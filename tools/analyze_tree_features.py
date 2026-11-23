#!/usr/bin/env python3
"""Branched - tree feature / biome audit for the original mods.

Resolves placed_feature -> configured_feature chains, classifies tree features,
extracts the native wood families they place and links them to biome files.
Outputs catalogs used by the Phase 0 compatibility matrix.
"""
from __future__ import annotations

import csv
import json
import re
import zipfile
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JARS = sorted(
    list((ROOT / "reference-mods" / "originals").glob("*.jar"))
    + list((ROOT / "reference-mods" / "dynamic-trees").glob("*.jar"))
)
OUT = ROOT / "catalogs"

TREE_TYPES = {
    "minecraft:tree",
    "minecraft:fungus",
    "minecraft:huge_fungus",
    "minecraft:mushroom",
    "minecraft:huge_brown_mushroom",
    "minecraft:huge_red_mushroom",
    "minecraft:random_selector",
    "minecraft:simple_random_selector",
    "minecraft:random_patch",
    "minecraft:noise_based_count",
    "dynamictrees:species",
    "dynamictrees:tree",
}
WOODISH = re.compile(r"(_log|_wood|_stem|_hyphae)$")


def is_treeish(types: set[str], blocks: set[str]) -> bool:
    if any(t in TREE_TYPES for t in types):
        return True
    if any(WOODISH.search(b.split(":")[-1]) for b in blocks) and any(
        b.endswith("_leaves") or b.endswith("_wart_block") for b in blocks
    ):
        return True
    return False


def walk_collect(node, types: set[str], blocks: set[str], refs: set[str]) -> None:
    if isinstance(node, dict):
        t = node.get("type")
        if isinstance(t, str):
            types.add(t)
        for k, v in node.items():
            if k in ("Name", "name") and isinstance(v, str) and re.fullmatch(r"[a-z0-9_.\-]+:[a-z0-9_./\-]+", v):
                blocks.add(v)
            walk_collect(v, types, blocks, refs)
    elif isinstance(node, list):
        for v in node:
            walk_collect(v, types, blocks, refs)
    elif isinstance(node, str):
        if re.fullmatch(r"[a-z0-9_.\-]+:[a-z0-9_./\-]+", node):
            refs.add(node)


def family_of(block: str) -> str | None:
    p = block.split(":", 1)[-1]
    p = re.sub(r"^(stripped|hollow)_", "", p)
    for suf in ("_log", "_wood", "_stem", "_hyphae"):
        if p.endswith(suf):
            return p[: -len(suf)]
    return None


def audit(jar: Path) -> dict:
    zf = zipfile.ZipFile(jar)
    names = zf.namelist()
    cfg: dict[str, dict] = {}
    placed: dict[str, dict] = {}
    biomes: dict[str, dict] = {}

    for n in names:
        if not n.endswith(".json"):
            continue
        if "/worldgen/configured_feature/" in n:
            ns, rel = n.split("/")[1], n.split("/configured_feature/", 1)[1][:-5]
            try:
                js = json.loads(zf.read(n))
            except Exception:
                continue
            t, b, r = set(), set(), set()
            walk_collect(js, t, b, r)
            cfg[f"{ns}:{rel}"] = {"types": t, "blocks": b, "refs": r, "json": js}
        elif "/worldgen/placed_feature/" in n:
            ns, rel = n.split("/")[1], n.split("/placed_feature/", 1)[1][:-5]
            try:
                js = json.loads(zf.read(n))
            except Exception:
                continue
            feat = js.get("feature")
            if isinstance(feat, dict):
                feat = "inline"
            placed[f"{ns}:{rel}"] = {"feature": feat, "json": js}
        elif "/worldgen/biome/" in n:
            ns, rel = n.split("/")[1], n.split("/worldgen/biome/", 1)[1][:-5]
            try:
                js = json.loads(zf.read(n))
            except Exception:
                continue
            biomes[f"{ns}:{rel}"] = js

    def expand(fid: str, seen: set[str]) -> tuple[set[str], set[str], set[str], set[str]]:
        types, blocks, refs, used = set(), set(), set(), set()
        if fid in cfg and fid not in seen:
            seen.add(fid)
            c = cfg[fid]
            types |= c["types"]
            blocks |= c["blocks"]
            used.add(fid)
            for r in c["refs"]:
                if r in cfg:
                    t2, b2, r2, u2 = expand(r, seen)
                    types |= t2
                    blocks |= b2
                    refs |= r2
                    used |= u2
        return types, blocks, refs, used

    rows = []
    resolved: dict[str, dict] = {}
    for pid, p in placed.items():
        feat = p["feature"]
        if not isinstance(feat, str) or feat.startswith("#"):
            continue
        types, blocks, refs, used = expand(feat, set())
        if not is_treeish(types, blocks):
            continue
        fams = sorted({f for b in blocks if (f := family_of(b))})
        resolved[pid] = {
            "feature": feat,
            "types": sorted(types),
            "blocks": sorted(blocks),
            "families": fams,
            "used_configs": sorted(used),
        }

    biome_rows = []
    for bid, bj in biomes.items():
        for step, feature_list in enumerate(bj.get("features", []) or []):
            if not isinstance(feature_list, list):
                continue
            for entry in feature_list:
                if isinstance(entry, str) and entry in resolved:
                    biome_rows.append({"biome": bid, "step": step, "placed_feature": entry})
                elif isinstance(entry, dict):
                    # inline placed feature: classify on the spot
                    t, bl, _, _ = set(), set(), set(), set()
                    walk_collect(entry, t, bl, set())
                    if is_treeish(t, bl):
                        biome_rows.append({"biome": bid, "step": step, "placed_feature": "inline"})

    return {"jar": jar.name, "resolved": resolved, "biomes": biome_rows,
            "cfg_count": len(cfg), "placed_count": len(placed), "biome_count": len(biomes)}


def main() -> int:
    all_rows, biome_rows, summaries = [], [], []
    for jar in JARS:
        print(f"[tree-audit] {jar.name}", flush=True)
        a = audit(jar)
        tree_b = defaultdict(list)
        for br in a["biomes"]:
            tree_b[br["placed_feature"]].append(br["biome"])
        for pid, r in sorted(a["resolved"].items()):
            all_rows.append(
                {
                    "jar": a["jar"],
                    "placed_feature": pid,
                    "configured_feature": r["feature"],
                    "types": ";".join(r["types"]),
                    "families": ";".join(r["families"]),
                    "blocks": ";".join(r["blocks"]),
                    "biome_count": len(set(tree_b.get(pid, []))),
                    "biomes": ";".join(sorted(set(tree_b.get(pid, [])))[:12]),
                }
            )
        biome_rows.extend(dict(jar=a["jar"], **br) for br in a["biomes"])
        summaries.append(
            {
                "jar": a["jar"],
                "configured_features": a["cfg_count"],
                "placed_features": a["placed_count"],
                "biomes": a["biome_count"],
                "tree_placed_features": len(a["resolved"]),
                "tree_biome_links": len(a["biomes"]),
            }
        )

    OUT.mkdir(exist_ok=True)
    with (OUT / "tree-features.csv").open("w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=list(all_rows[0].keys()))
        w.writeheader()
        w.writerows(all_rows)
    with (OUT / "biome-tree-mapping.csv").open("w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=["jar", "biome", "step", "placed_feature"])
        w.writeheader()
        w.writerows(biome_rows)
    (OUT / "tree-feature-summary.json").write_text(
        json.dumps(summaries, indent=2), encoding="utf-8"
    )
    for s in summaries:
        print(s)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
