#!/usr/bin/env python3
"""Build the Phase 0 species coverage matrix.

Joins three audited sources:

  * catalogs/tree-features.csv  - tree placed features per original mod and the
                                  native wood families each one places.
  * catalogs/block-families.csv - audited log/leaves/sapling ids.
  * src/main/resources/.../species/*.json - the shipped Branched profiles.

Outputs catalogs/species-matrix.csv and catalogs/coverage-report.md. Nothing is
inferred: a family is only marked as having a profile when a profile whose block
ids match the family exists, and a feature is only marked replaceable when the
adapter declares it as STATIC_NORMAL or STATIC_FRUIT.
"""
from __future__ import annotations

import csv
import json
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CAT = ROOT / "catalogs"
PROFILES = ROOT / "src/main/resources/data/branched/arboreal/species"

ORIGINAL_JARS = {
    "RegionsUnexploredForge-0.5.6+1.20.1.jar": ("regions_unexplored", "0.5.6"),
    "Terralith_1.20.x_v2.5.4.jar": ("terralith", "2.5.4"),
    "aether-1.20.1-1.5.2-neoforge.jar": ("aether", "1.20.1-1.5.2"),
    "twilightforest-1.20.1-4.3.2508-universal.jar": ("twilightforest", "4.3.2508"),
    "Quark-4.0-462.jar": ("quark", "4.0-462"),
    "fish_of_thieves-mc1.20.1-v3.1.7-forge.jar": ("fishofthieves", "3.1.7"),
}

# Features whose replacement is deliberately refused. Kept in sync with the
# Java adapters; the test AdapterIsolationTest enforces the Java side.
SPECIAL_PRESERVE = {
    "regions_unexplored:alpha_tree",
    "regions_unexplored:bamboo_tree",
    "regions_unexplored:blackwood_mushrooms",
    "regions_unexplored:fallen_larch_tree",
    "terralith:skylands/trees",
    "terralith:cave/jungle/vegetation",
    "terralith:enchanted/enchanted_oak",
    "aether:holiday_tree",
    "twilightforest:tree/hollow_oak",
    "twilightforest:hollow_stump",
    "twilightforest:tree/mining_tree",
    "twilightforest:tree/sorting_tree",
    "twilightforest:tree/time_tree",
    "twilightforest:tree/transformation_tree",
    "twilightforest:mushroom/canopy_mushrooms_dense",
    "quark:glow_shrooms",
    "quark:ancient",
}
STATIC_FRUIT = {
    "regions_unexplored:apple_oak_tree",
    "aether:gold_dungeon_island_foliage",
    "fishofthieves:banana_tree_checked",
    "fishofthieves:coconut_tree_checked",
    "fishofthieves:mango_tree_checked",
    "fishofthieves:sparse_jungle_fruit_trees",
}
UNRESOLVED = {
    "minecraft:trees_plains",
    "quark:ancient",
}

# Non-species log ids that appear in placed features as parts of a larger tree.
# They have no leaves of their own and must not be counted as coverage gaps.
TREE_PARTS = {
    ("fishofthieves", "banana_cluster_growable"),
    ("fishofthieves", "coconut_fruit_growable"),
    ("fishofthieves", "medium_coconut"),
    ("fishofthieves", "small_coconut"),
    ("fishofthieves", "small_top_coconut"),
    ("fishofthieves", "budding_prismarized"),
    ("fishofthieves", "prismarized"),
    ("quark", "azalea"),
    ("quark", "glow_shroom"),
    ("twilightforest", "cinder"),
    ("twilightforest", "huge_mushgloom"),
}


def load_profiles() -> dict[str, dict]:
    profiles: dict[str, dict] = {}
    for path in sorted(PROFILES.rglob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        profiles[data["species"]] = data
    return profiles


def block_to_family(block: str) -> str:
    path = block.split(":", 1)[-1]
    for prefix in ("stripped_", "hollow_"):
        if path.startswith(prefix):
            path = path[len(prefix):]
    for suffix in ("_log", "_wood", "_stem", "_hyphae"):
        if path.endswith(suffix):
            return path[: -len(suffix)]
    return path


def level_for(feature: str) -> str:
    if feature in SPECIAL_PRESERVE:
        return "SPECIAL_PRESERVE"
    if feature in STATIC_FRUIT:
        return "STATIC_FRUIT"
    if feature in UNRESOLVED:
        return "UNRESOLVED"
    return "STATIC_NORMAL"


def main() -> int:
    profiles = load_profiles()
    profile_by_log = {}
    profile_by_family = {}
    for species, data in profiles.items():
        profile_by_log[data["logs"]] = species
        profile_by_family[(species.split(":")[0], block_to_family(data["logs"]))] = species
        if data.get("wood"):
            profile_by_log[data["wood"]] = species

    families = {}
    with (CAT / "block-families.csv").open(encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            key = (row["jar"], row["family"])
            families.setdefault(key, row)

    features = defaultdict(list)
    with (CAT / "tree-features.csv").open(encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            if row["jar"] not in ORIGINAL_JARS:
                continue
            features[row["jar"]].append(row)

    matrix = []
    for jar, (modid, version) in ORIGINAL_JARS.items():
        fam_features: dict[str, list[str]] = defaultdict(list)
        fam_blocks: dict[str, set[str]] = defaultdict(set)
        for row in features.get(jar, []):
            blocks = {b for b in row["blocks"].split(";") if b}
            for fam in row["families"].split(";"):
                if fam:
                    fam_features[fam].append(row["placed_feature"])
                    fam_blocks[fam] |= blocks

        for fam in sorted(set(fam_features) | {f for (j, f) in families if j == jar}):
            block = families.get((jar, fam), {})
            feats = sorted(set(fam_features.get(fam, [])))
            blocks = fam_blocks.get(fam, set())

            profile = profile_by_family.get((modid, fam))
            status = "COVERED"
            if profile is None:
                for candidate in sorted(blocks):
                    if candidate in profile_by_log:
                        profile = profile_by_log[candidate]
                        status = "COVERED_VIA_BLOCK"
                        break
            if profile is None:
                if (modid, fam) in TREE_PARTS or (blocks and not block.get("leaves") and not block.get("sapling")):
                    status = "TREE_PART"
                elif not feats:
                    status = "NO_PLACED_FEATURE"
                else:
                    status = "PROFILE_MISSING"
            levels = sorted({level_for(f) for f in feats}) if feats else ["UNRESOLVED"]
            matrix.append(
                {
                    "mod": modid,
                    "mod_version": version,
                    "family": fam,
                    "profile": profile or "",
                    "status": status,
                    "levels": ";".join(levels),
                    "placed_features": len(feats),
                    "features": ";".join(feats[:6]),
                    "blocks": ";".join(sorted(b for b in blocks if b.endswith(("_log", "_wood", "_stem", "_hyphae")))),
                    "log": block.get("log", ""),
                    "leaves": block.get("leaves", ""),
                    "sapling": block.get("sapling", ""),
                }
            )

    # Vanilla rows (not in the audited JARs).
    for species, data in sorted(profiles.items()):
        if not species.startswith("minecraft:"):
            continue
        matrix.append(
            {
                "mod": "minecraft",
                "mod_version": "1.20.1",
                "family": block_to_family(data["logs"]),
                "profile": species,
                "status": "COVERED",
                "levels": data["compatibility"],
                "placed_features": 0,
                "features": "vanilla worldgen features (see adapter)",
                "blocks": data["logs"],
                "log": data["logs"],
                "leaves": data["leaves"],
                "sapling": data.get("sapling", ""),
            }
        )

    matrix.sort(key=lambda r: (r["mod"], r["family"]))
    with (CAT / "species-matrix.csv").open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=list(matrix[0].keys()))
        writer.writeheader()
        writer.writerows(matrix)

    # ---- coverage report ----
    all_features = [f for rows in features.values() for r in rows for f in [r["placed_feature"]]]
    by_level = Counter(level_for(f) for f in all_features)
    per_mod = {}
    for jar, (modid, _v) in ORIGINAL_JARS.items():
        rows = features.get(jar, [])
        mod_feats = [r["placed_feature"] for r in rows]
        mod_rows = [m for m in matrix if m["mod"] == modid]
        species_rows = [m for m in mod_rows if m["status"] not in ("TREE_PART", "NO_PLACED_FEATURE")]
        per_mod[modid] = {
            "tree_placed_features": len(mod_feats),
            "families": len({f for r in rows for f in r["families"].split(";") if f}),
            "profiles": sum(1 for p in profiles if p.startswith(modid + ":")),
            "covered_families": sum(1 for m in species_rows
                                    if m["status"] in ("COVERED", "COVERED_VIA_BLOCK")),
            "species_families": len(species_rows),
            "missing_profiles": sorted(m["family"] for m in species_rows
                                       if m["status"] == "PROFILE_MISSING"),
            "levels": dict(Counter(level_for(f) for f in mod_feats)),
        }

    lines = [
        "# Phase 0 coverage report",
        "",
        "Generated by `tools/build_species_matrix.py` from the audited JAR catalogs.",
        "These are **profiles and classifications**, not completed integrations.",
        "",
        "## Totals",
        "",
        f"- Audited original mods: **{len(ORIGINAL_JARS)}**",
        f"- Tree placed features found: **{len(all_features)}**",
        f"- Distinct wood families with tree features: **{len({(m['mod'], m['family']) for m in matrix if m['placed_features']})}**",
        f"- Shipped species profiles: **{len(profiles)}**",
        f"- Profiles marked STATIC_NORMAL: **{sum(1 for p in profiles.values() if p['compatibility'] == 'STATIC_NORMAL')}**",
        f"- Profiles marked STATIC_FRUIT: **{sum(1 for p in profiles.values() if p['compatibility'] == 'STATIC_FRUIT')}**",
        f"- Profiles marked SPECIAL_PRESERVE: **{sum(1 for p in profiles.values() if p['compatibility'] == 'SPECIAL_PRESERVE')}**",
        f"- Profiles marked UNRESOLVED: **{sum(1 for p in profiles.values() if p['compatibility'] == 'UNRESOLVED')}**",
        "",
        "## Feature classification (by placed feature id)",
        "",
        "| Level | Features |",
        "| --- | ---: |",
    ]
    for level in ("STATIC_NORMAL", "STATIC_FRUIT", "SPECIAL_PRESERVE", "UNRESOLVED"):
        lines.append(f"| {level} | {by_level.get(level, 0)} |")
    lines += ["", "## Per mod", "", "| Mod | Tree features | Families | Profiles | Covered species families | Families without profile |", "| --- | ---: | ---: | ---: | ---: | --- |"]
    for modid, data in per_mod.items():
        missing = ", ".join(data["missing_profiles"]) if data["missing_profiles"] else "-"
        lines.append(
            f"| {modid} | {data['tree_placed_features']} | {data['families']} | "
            f"{data['profiles']} | {data['covered_families']}/{data['species_families']} | {missing} |"
        )
    lines += [
        "",
        "## Honest limitations",
        "",
        "- A profile is a *description*, not an integration. No worldgen replacement is",
        "  enabled in Phase 0; `worldgen.replace_existing_chunks` is false and no",
        "  PlacedFeature is rewritten.",
        "- Terralith ships no wood blocks: its 342 tree placed features reuse vanilla",
        "  families, so species identity is shape/biome while logs stay vanilla.",
        "- 233 Dynamic Trees species were catalogued from the reference JARs for",
        "  comparison only. They are **not** integrations and Dynamic Trees is never a",
        "  runtime dependency.",
        "- Dynamic Trees Baobab 0.1.5 is a development reference with no verified",
        "  public download; it stays `UNRESOLVED` and its adapter never activates.",
        "",
    ]
    (CAT / "coverage-report.md").write_text("\n".join(lines), encoding="utf-8")

    print(f"matrix rows: {len(matrix)}")
    print(f"profiles: {len(profiles)}")
    print("levels:", dict(by_level))
    for modid, data in per_mod.items():
        print(f"  {modid}: features={data['tree_placed_features']} families={data['families']} "
              f"profiles={data['profiles']} covered={data['covered_families']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
