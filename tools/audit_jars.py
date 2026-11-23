#!/usr/bin/env python3
"""Branched - Phase 0 JAR auditor.

Reads the reference JARs, validates archive integrity, extracts metadata and
worldgen resources, and emits machine-readable catalogs. No Minecraft runtime
is required. Read-only with respect to the reference JARs.

Usage:
    python tools/audit_jars.py [--root .]
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import os
import re
import sys
import zipfile
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, Iterable

# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

TREE_TOKENS = re.compile(
    r"(tree|wood|log|leaves|sapling|branch|root|palm|baobab|mangrove|azalea|"
    r"mushroom|fungus|shroom|cactus|kapok|poplar|maple|jacaranda|sakura|"
    r"spruce|birch|oak|acacia|jungle|pine|cypress|larch|mahogany|redwood|"
    r"willow|aspen|fir|cedar|ebony|mirage|skyroot|glacia|roseroot|sunroot|"
    r"highsproot|fieldsproot|cruderoot|beladon|conberry|blightwillow|aerfin|"
    r"yagroot|mushgloom|darkwood|canopy|twilight|mining|sorting|time|"
    r"transformation|rainbow|holiday|gilded|crystal|enchanted|orange|purple)",
    re.IGNORECASE,
)

LOG_SUFFIXES = ("_log", "_wood", "_stem", "_hyphae")
LEAF_SUFFIXES = ("_leaves",)
SAPLING_SUFFIXES = ("_sapling",)


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def parse_simple_toml(text: str) -> dict[str, Any]:
    """Small TOML subset parser sufficient for mods.toml metadata.

    Handles modLoader/loaderVersion/license top level keys and [[mods]] /
    [[dependencies.*]] tables. Deliberately does not attempt full TOML.
    """
    out: dict[str, Any] = {"mods": [], "dependencies": defaultdict(list)}
    current: dict[str, Any] | None = None
    current_dep: str | None = None
    for raw in text.splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("[[dependencies.") and line.endswith("]]"):
            current = None
            current_dep = line[len("[[dependencies.") : -2].strip().strip('"')
            continue
        if line == "[[mods]]":
            current = {}
            out["mods"].append(current)
            current_dep = None
            continue
        if line.startswith("[[") or line.startswith("["):
            current = None
            current_dep = None
            continue
        if "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip().strip('"')
        value = value.strip()
        if value.startswith("'''") or value.startswith('"""'):
            delimiter = value[:3]
            if value.count(delimiter) >= 2 and len(value) > 3:
                value = value[3:-3].strip()
            else:
                value = value[3:]
        elif value.startswith('"') and value.endswith('"'):
            value = value[1:-1]
        if current is not None:
            current[key] = value
        elif current_dep is not None:
            out["dependencies"][current_dep].append({key: value})
        else:
            out[key] = value
    return out


def read_text(zf: zipfile.ZipFile, name: str) -> str | None:
    try:
        return zf.read(name).decode("utf-8", "replace")
    except KeyError:
        return None


def find_first(zf: zipfile.ZipFile, predicate) -> str | None:
    for name in zf.namelist():
        if predicate(name):
            return name
    return None


def collect_block_names(js: Any, acc: set[str]) -> None:
    if isinstance(js, dict):
        for k, v in js.items():
            if k in ("Name", "name", "block", "block_id", "log", "leaves") and isinstance(v, str):
                if ":" in v and " " not in v:
                    acc.add(v)
            collect_block_names(v, acc)
    elif isinstance(js, list):
        for v in js:
            collect_block_names(v, acc)


def collect_types(js: Any, acc: set[str]) -> None:
    if isinstance(js, dict):
        t = js.get("type")
        if isinstance(t, str):
            acc.add(t)
        for v in js.values():
            collect_types(v, acc)
    elif isinstance(js, list):
        for v in js:
            collect_types(v, acc)


def collect_refs(js: Any, acc: set[str]) -> None:
    """Collect string values that look like resource ids (namespace:path)."""
    if isinstance(js, dict):
        for v in js.values():
            collect_refs(v, acc)
    elif isinstance(js, list):
        for v in js:
            collect_refs(v, acc)
    elif isinstance(js, str) and re.fullmatch(r"[a-z0-9_.\-]+:[a-z0-9_./\-]+", js):
        acc.add(js)


def deep_get(js: Any, *keys: str, default=None):
    cur = js
    for key in keys:
        if not isinstance(cur, dict) or key not in cur:
            return default
        cur = cur[key]
    return cur


# ---------------------------------------------------------------------------
# per-jar audit
# ---------------------------------------------------------------------------

def audit_jar(path: Path) -> dict[str, Any]:
    entry: dict[str, Any] = {
        "file": path.name,
        "path": str(path),
        "size": path.stat().st_size,
        "sha256": sha256_file(path),
        "zip_valid": False,
        "entry_count": 0,
        "error": None,
    }
    try:
        zf = zipfile.ZipFile(path)
        bad = zf.testzip()
        entry["zip_valid"] = bad is None
        names = zf.namelist()
        entry["entry_count"] = len(names)
        if bad:
            entry["error"] = f"corrupt entry: {bad}"
            return entry
    except Exception as exc:  # noqa: BLE001
        entry["error"] = f"{type(exc).__name__}: {exc}"
        return entry

    # --- metadata -----------------------------------------------------------
    meta: dict[str, Any] = {}
    toml_name = find_first(zf, lambda n: n.lower().endswith("meta-inf/mods.toml"))
    if toml_name:
        parsed = parse_simple_toml(read_text(zf, toml_name) or "")
        meta["mods_toml"] = toml_name
        meta["modLoader"] = parsed.get("modLoader")
        meta["loaderVersion"] = parsed.get("loaderVersion")
        meta["license"] = parsed.get("license")
        meta["mods"] = parsed.get("mods", [])
        meta["dependencies"] = {
            k: v for k, v in parsed.get("dependencies", {}).items()
        }
    manifest = read_text(zf, "META-INF/MANIFEST.MF")
    if manifest:
        meta["manifest"] = {
            line.split(":", 1)[0].strip(): line.split(":", 1)[1].strip()
            for line in manifest.splitlines()
            if ":" in line
        }
    meta["modid"] = (meta.get("mods") or [{}])[0].get("modId") if meta.get("mods") else None
    meta["version"] = (meta.get("mods") or [{}])[0].get("version") if meta.get("mods") else None
    entry["metadata"] = meta

    license_files = [
        n
        for n in names
        if re.search(r"(^|/)(LICEN[CS]E|COPYING|NOTICE)(\.[A-Za-z0-9]+)?$", n, re.IGNORECASE)
    ]
    entry["license_files"] = license_files

    # --- resource indexes ---------------------------------------------------
    data_entries = [n for n in names if n.startswith("data/") and n.endswith(".json")]
    asset_entries = [n for n in names if n.startswith("assets/") and n.endswith(".json")]
    entry["counts"] = {
        "data_json": len(data_entries),
        "asset_json": len(asset_entries),
        "classes": sum(1 for n in names if n.endswith(".class")),
        "blockstates": sum(1 for n in names if n.startswith("assets/") and "/blockstates/" in n),
        "models": sum(1 for n in names if n.startswith("assets/") and "/models/" in n),
    }

    def kind(name: str) -> str:
        if "/worldgen/configured_feature/" in name:
            return "configured_feature"
        if "/worldgen/placed_feature/" in name:
            return "placed_feature"
        if "/worldgen/biome_modifier/" in name:
            return "biome_modifier"
        if "/tags/worldgen/biome/" in name:
            return "biome_tag"
        if "/tags/blocks/" in name:
            return "block_tag"
        if "/tags/items/" in name:
            return "item_tag"
        if "/worldgen/structure/" in name:
            return "structure"
        return "other"

    buckets: dict[str, list[str]] = defaultdict(list)
    for n in data_entries:
        buckets[kind(n)].append(n)

    block_ids: set[str] = set()
    for n in names:
        if n.startswith("assets/") and "/blockstates/" in n and n.endswith(".json"):
            ns = n.split("/")[1]
            rel = n.split("/blockstates/", 1)[1][: -len(".json")]
            block_ids.add(f"{ns}:{rel}")
    entry["block_count"] = len(block_ids)

    lang = {}
    lang_name = find_first(zf, lambda n: re.fullmatch(r"assets/[^/]+/lang/en_us\.json", n))
    if lang_name:
        try:
            lang = json.loads(read_text(zf, lang_name) or "{}")
        except json.JSONDecodeError:
            lang = {}
    entry["lang_file"] = lang_name

    # --- block families -----------------------------------------------------
    families: dict[str, dict[str, Any]] = {}
    for log_ns_path in sorted(block_ids):
        ns, p = log_ns_path.split(":", 1)
        if p.startswith("stripped_") or p.startswith("hollow_"):
            continue
        if p.endswith("_stripped_log") or p.endswith("_stripped_wood"):
            continue
        base = None
        # Prefer the canonical *_log id over *_wood: a family may expose both.
        for suf in ("_log", "_wood", "_stem", "_hyphae"):
            if p.endswith(suf):
                base = p[: -len(suf)]
                break
        if base is None:
            continue
        log_id = f"{ns}:{base}_log"
        if log_id not in block_ids:
            log_id = log_ns_path
        leaves = f"{ns}:{base}_leaves"
        sapling = f"{ns}:{base}_sapling"
        families[base] = {
            "namespace": ns,
            "base": base,
            "log": log_id,
            "wood": f"{ns}:{base}_wood" if f"{ns}:{base}_wood" in block_ids else "",
            "stem": f"{ns}:{base}_stem" if f"{ns}:{base}_stem" in block_ids else "",
            "leaves": leaves if leaves in block_ids else "",
            "sapling": sapling if sapling in block_ids else "",
            "stripped_log": f"{ns}:stripped_{base}_log" if f"{ns}:stripped_{base}_log" in block_ids else "",
            "planks": f"{ns}:{base}_planks" if f"{ns}:{base}_planks" in block_ids else "",
            "display": lang.get(f"block.{ns}.{base}_log")
            or lang.get(f"block.{ns}.{base}_sapling")
            or base.replace("_", " ").title(),
        }
    entry["tree_block_families"] = families

    # --- worldgen payloads --------------------------------------------------
    configured: dict[str, dict[str, Any]] = {}
    for n in buckets["configured_feature"]:
        try:
            js = json.loads(zf.read(n))
        except Exception:  # noqa: BLE001
            continue
        ns = n.split("/")[1]
        rel = n.split("/configured_feature/", 1)[1][: -len(".json")]
        ftypes: set[str] = set()
        blocks: set[str] = set()
        refs: set[str] = set()
        collect_types(js, ftypes)
        collect_block_names(js, blocks)
        collect_refs(js, refs)
        configured[f"{ns}:{rel}"] = {
            "file": n,
            "types": sorted(ftypes),
            "blocks": sorted(blocks),
            "refs": sorted(refs),
        }
    entry["configured_features"] = configured

    placed: dict[str, dict[str, Any]] = {}
    for n in buckets["placed_feature"]:
        try:
            js = json.loads(zf.read(n))
        except Exception:  # noqa: BLE001
            continue
        ns = n.split("/")[1]
        rel = n.split("/placed_feature/", 1)[1][: -len(".json")]
        feature = js.get("feature") if isinstance(js, dict) else None
        if isinstance(feature, list):
            feature = ",".join(str(x) for x in feature)
        refs: set[str] = set()
        collect_refs(js, refs)
        placed[f"{ns}:{rel}"] = {
            "file": n,
            "feature": feature,
            "refs": sorted(refs),
        }
    entry["placed_features"] = placed

    biome_modifiers: dict[str, dict[str, Any]] = {}
    for n in buckets["biome_modifier"]:
        try:
            js = json.loads(zf.read(n))
        except Exception:  # noqa: BLE001
            continue
        ns = n.split("/")[1]
        rel = n.split("/biome_modifier/", 1)[1][: -len(".json")]
        biome_modifiers[f"{ns}:{rel}"] = {
            "file": n,
            "type": js.get("type") if isinstance(js, dict) else None,
            "biomes": js.get("biomes") if isinstance(js, dict) else None,
            "features": js.get("features") if isinstance(js, dict) else None,
        }
    entry["biome_modifiers"] = biome_modifiers

    biome_tags: dict[str, dict[str, Any]] = {}
    for n in buckets["biome_tag"]:
        try:
            js = json.loads(zf.read(n))
        except Exception:  # noqa: BLE001
            continue
        ns = n.split("/")[1]
        rel = n.split("/tags/worldgen/biome/", 1)[1][: -len(".json")]
        biome_tags[f"{ns}:{rel}"] = {
            "file": n,
            "values": js.get("values", []) if isinstance(js, dict) else [],
        }
    entry["biome_tags"] = biome_tags

    # --- DT species (development reference only) ---------------------------
    species: dict[str, dict[str, Any]] = {}
    for n in names:
        if re.search(r"/species/[^/]+\.json$", n):
            try:
                js = json.loads(zf.read(n))
            except Exception:  # noqa: BLE001
                continue
            ns = n.split("/")[1]
            rel = n.split("/species/", 1)[1][: -len(".json")]
            species[f"{ns}:{rel}"] = {"file": n, "json": js}
    entry["dt_species"] = species

    entry["tree_related_assets"] = sorted(
        n for n in asset_entries if TREE_TOKENS.search(n) and "/lang/" not in n
    )[:400]
    return entry


# ---------------------------------------------------------------------------
# reports
# ---------------------------------------------------------------------------

def write_csv(path: Path, headers: list[str], rows: Iterable[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=headers, extrasaction="ignore")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=".")
    args = ap.parse_args()
    root = Path(args.root).resolve()
    jars = sorted(
        list((root / "reference-mods" / "originals").glob("*.jar"))
        + list((root / "reference-mods" / "dynamic-trees").glob("*.jar"))
    )
    if not jars:
        print("No JARs found", file=sys.stderr)
        return 1

    audits = []
    for jar in jars:
        print(f"[audit] {jar.name}", flush=True)
        audits.append(audit_jar(jar))

    out = root / "catalogs"
    out.mkdir(exist_ok=True)
    (out / "jar-audit.json").write_text(
        json.dumps(audits, indent=2, ensure_ascii=False, default=str), encoding="utf-8"
    )

    inv_rows = []
    for a in audits:
        m = a.get("metadata", {})
        mods = m.get("mods") or [{}]
        deps = m.get("dependencies", {}) or {}
        dep_flat = []
        for modid, entries in deps.items():
            for d in entries:
                dep_flat.append(
                    f"{d.get('modId','?')}:{d.get('versionRange','?')}:{'M' if d.get('mandatory')=='true' else 'O'}"
                )
        inv_rows.append(
            {
                "file": a["file"],
                "size": a["size"],
                "sha256": a["sha256"],
                "zip_valid": a["zip_valid"],
                "entry_count": a["entry_count"],
                "modid": mods[0].get("modId") if mods else "",
                "mod_version": mods[0].get("version") if mods else "",
                "display_name": mods[0].get("displayName") if mods else "",
                "modLoader": m.get("modLoader", ""),
                "loaderVersion": m.get("loaderVersion", ""),
                "license": m.get("license", ""),
                "dependencies": "; ".join(dep_flat),
                "license_files": "; ".join(a.get("license_files", [])),
                "classes": a["counts"]["classes"],
                "block_families": len(a.get("tree_block_families", {})),
                "placed_features": len(a.get("placed_features", {})),
                "configured_features": len(a.get("configured_features", {})),
                "biome_modifiers": len(a.get("biome_modifiers", {})),
                "dt_species": len(a.get("dt_species", {})),
                "error": a.get("error") or "",
            }
        )
    write_csv(
        out / "jar-inventory.csv",
        list(inv_rows[0].keys()) if inv_rows else ["file"],
        inv_rows,
    )

    fam_rows = []
    for a in audits:
        ns = (a.get("metadata", {}).get("mods") or [{}])[0].get("modId", "")
        for base, f in sorted(a.get("tree_block_families", {}).items()):
            fam_rows.append(
                {
                    "jar": a["file"],
                    "modid": ns,
                    "family": base,
                    "display": f["display"],
                    "log": f["log"],
                    "wood": f["wood"],
                    "stem": f["stem"],
                    "leaves": f["leaves"],
                    "sapling": f["sapling"],
                    "stripped_log": f["stripped_log"],
                    "planks": f["planks"],
                }
            )
    write_csv(out / "block-families.csv", list(fam_rows[0].keys()) if fam_rows else ["family"], fam_rows)

    cf_rows, pf_rows, bm_rows = [], [], []
    for a in audits:
        for fid, f in sorted(a.get("configured_features", {}).items()):
            cf_rows.append(
                {
                    "jar": a["file"],
                    "feature": fid,
                    "types": ";".join(f["types"]),
                    "blocks": ";".join(f["blocks"]),
                    "refs": ";".join(f["refs"]),
                    "file": f["file"],
                }
            )
        for pid, p in sorted(a.get("placed_features", {}).items()):
            pf_rows.append(
                {
                    "jar": a["file"],
                    "placed_feature": pid,
                    "configured_feature": p["feature"] or "",
                    "refs": ";".join(p["refs"]),
                    "file": p["file"],
                }
            )
        for bid, b in sorted(a.get("biome_modifiers", {}).items()):
            bm_rows.append(
                {
                    "jar": a["file"],
                    "biome_modifier": bid,
                    "type": b["type"] or "",
                    "biomes": json.dumps(b["biomes"], ensure_ascii=False) if b["biomes"] is not None else "",
                    "features": json.dumps(b["features"], ensure_ascii=False) if b["features"] is not None else "",
                    "file": b["file"],
                }
            )
    write_csv(out / "configured-features.csv", ["jar", "feature", "types", "blocks", "refs", "file"], cf_rows)
    write_csv(out / "placed-features.csv", ["jar", "placed_feature", "configured_feature", "refs", "file"], pf_rows)
    write_csv(out / "biome-modifiers.csv", ["jar", "biome_modifier", "type", "biomes", "features", "file"], bm_rows)

    # --- DT species catalog -------------------------------------------------
    sp_rows = []
    for a in audits:
        for sid, s in sorted(a.get("dt_species", {}).items()):
            js = s["json"]
            families = js.get("family") or js.get("families") or ""
            if isinstance(families, list):
                families = ";".join(map(str, families))
            sp_rows.append(
                {
                    "jar": a["file"],
                    "species": sid,
                    "family": families,
                    "type": js.get("type", ""),
                    "tapered": js.get("tapered", ""),
                    "signal_energy": js.get("signal_energy", ""),
                    "growth_logic_kit": json.dumps(js.get("growth_logic_kit", ""), ensure_ascii=False),
                    "has_fruits": "fruits" in js,
                    "has_pods": "pods" in js,
                    "has_mega": "mega_species" in js or bool(js.get("mega_species")),
                    "file": s["file"],
                }
            )
    write_csv(
        out / "dt-species.csv",
        [
            "jar",
            "species",
            "family",
            "type",
            "tapered",
            "signal_energy",
            "growth_logic_kit",
            "has_fruits",
            "has_pods",
            "has_mega",
            "file",
        ],
        sp_rows,
    )

    # --- summary ------------------------------------------------------------
    summary = {
        "jars": len(audits),
        "valid_zips": sum(1 for a in audits if a["zip_valid"]),
        "total_bytes": sum(a["size"] for a in audits),
        "total_block_families": sum(len(a.get("tree_block_families", {})) for a in audits),
        "total_configured_features": sum(len(a.get("configured_features", {})) for a in audits),
        "total_placed_features": sum(len(a.get("placed_features", {})) for a in audits),
        "total_biome_modifiers": sum(len(a.get("biome_modifiers", {})) for a in audits),
        "total_dt_species": sum(len(a.get("dt_species", {})) for a in audits),
        "per_jar": {
            a["file"]: {
                "modid": ((a.get("metadata", {}).get("mods") or [{}])[0].get("modId")),
                "block_families": len(a.get("tree_block_families", {})),
                "configured_features": len(a.get("configured_features", {})),
                "placed_features": len(a.get("placed_features", {})),
                "biome_modifiers": len(a.get("biome_modifiers", {})),
                "dt_species": len(a.get("dt_species", {})),
            }
            for a in audits
        },
    }
    (out / "audit-summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
