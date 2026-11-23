#!/usr/bin/env python3
"""Update pine archetype metadata and generate expanded vanilla + modpack species & worldgen data."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src/main/resources/data/branched"
SPECIES_DIR = DATA / "arboreal/species"
CF_DIR = DATA / "worldgen/configured_feature"
PF_DIR = DATA / "worldgen/placed_feature"
BM_DIR = DATA / "forge/biome_modifier"
BIOME_TAG_DIR = DATA / "tags/worldgen/biome"
PF_TAG_DIR = DATA / "tags/worldgen/placed_feature"


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def profile(species: str, archetype: str, logs: str, leaves: str, wood: str | None,
            sapling: str | None, height: list[int], radius: list[float], bend: list[float],
            primary: list[int], depth: list[int], angle: list[int], len_ratio: list[float],
            asym: list[float], pattern: str, width: list[float], density: list[float],
            clusters: list[int], max_seg: int, max_leaves: int, max_quads: int,
            compat: str, source_feat: str, biomes: list[str]) -> dict:
    data = {
        "schema": 1,
        "species": species,
        "archetype": archetype,
        "logs": logs,
        "leaves": leaves,
    }
    if wood:
        data["wood"] = wood
    if sapling:
        data["sapling"] = sapling
    data.update({
        "trunk": {"height": height, "radius": radius, "bend": bend},
        "branches": {
            "primaryCount": primary,
            "depth": depth,
            "angleDegrees": angle,
            "lengthRatio": len_ratio,
            "asymmetry": asym,
            "pattern": pattern,
        },
        "crown": {"widthRatio": width, "density": density, "clusterCount": clusters},
        "generation": {"mode": "semiprocedural_once", "attempts": 3, "version": 3},
        "budget": {"maxSegments": max_seg, "maxLeaves": max_leaves, "maxQuads": max_quads},
        "compatibility": compat,
        "source": {"feature": source_feat},
        "biomes": biomes,
        "fruitBearing": False,
    })
    return data


def static_tree_cf(species: str) -> dict:
    return {"type": "branched:static_tree", "config": {"species": species, "attempts": 3}}


def checked_pf(feature: str, sapling: str) -> dict:
    return {
        "feature": feature,
        "placement": [{
            "type": "minecraft:block_predicate_filter",
            "predicate": {
                "type": "minecraft:would_survive",
                "state": {"Name": sapling, "Properties": {"stage": "0"}}
            }
        }]
    }


def weighted_count_pf(feature: str, data0: int, w0: int, data1: int, w1: int,
                      max_water_depth: int = 0, extra_sapling: str | None = None) -> dict:
    placement = [
        {
            "type": "minecraft:count",
            "count": {
                "type": "minecraft:weighted_list",
                "distribution": [
                    {"data": data0, "weight": w0},
                    {"data": data1, "weight": w1},
                ]
            }
        },
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": max_water_depth},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
        {"type": "minecraft:biome"},
    ]
    if extra_sapling:
        placement.append({
            "type": "minecraft:block_predicate_filter",
            "predicate": {
                "type": "minecraft:would_survive",
                "state": {"Name": extra_sapling, "Properties": {"stage": "0"}}
            }
        })
    return {"feature": feature, "placement": placement}


def update_pine_profiles() -> None:
    for name in ("pine.json", "ashen_pine.json", "dead_pine.json"):
        p = SPECIES_DIR / "regions_unexplored" / name
        d = json.loads(p.read_text(encoding="utf-8"))
        d["archetype"] = "columnar"
        write_json(p, d)

    # Make vanilla jungle tall enough for satisfying lateral parkour boughs
    jp = SPECIES_DIR / "minecraft/jungle.json"
    jd = json.loads(jp.read_text(encoding="utf-8"))
    jd["trunk"]["height"] = [10, 19]
    jd["branches"]["primaryCount"] = [7, 13]
    jd["budget"]["maxSegments"] = 240
    jd["budget"]["maxLeaves"] = 720
    jd["budget"]["maxQuads"] = 17000
    write_json(jp, jd)


def write_new_profiles() -> None:
    profiles = [
        ("ecologics/walnut.json", profile(
            "ecologics:walnut", "broadleaf_round",
            "ecologics:walnut_log", "ecologics:walnut_leaves", "ecologics:walnut_wood", "ecologics:walnut_sapling",
            [5, 8], [0.6, 0.85], [0.02, 0.14], [4, 7], [1, 2], [25, 55], [0.28, 0.48], [0.1, 0.35],
            "alternate", [0.95, 1.45], [0.55, 0.8], [6, 12], 128, 420, 9000,
            "STATIC_NORMAL", "ecologics:walnut", ["minecraft:plains"])),
        ("ecologics/coconut.json", profile(
            "ecologics:coconut", "palm",
            "ecologics:coconut_log", "ecologics:coconut_leaves", "ecologics:coconut_wood", "ecologics:coconut_seedling",
            [7, 11], [0.45, 0.7], [0.12, 0.3], [4, 6], [1, 1], [55, 78], [0.22, 0.36], [0.1, 0.3],
            "terminal", [0.9, 1.4], [0.45, 0.75], [5, 8], 96, 320, 7500,
            "SPECIAL_PRESERVE", "ecologics:coconut", ["minecraft:beach"])),
        ("upgrade_aquatic/river.json", profile(
            "upgrade_aquatic:river", "broadleaf_irregular",
            "upgrade_aquatic:river_log", "upgrade_aquatic:river_leaves", "upgrade_aquatic:river_wood", "upgrade_aquatic:river_sapling",
            [6, 10], [0.6, 0.9], [0.05, 0.2], [4, 7], [1, 3], [28, 55], [0.28, 0.48], [0.15, 0.4],
            "alternate", [0.95, 1.45], [0.5, 0.8], [6, 12], 136, 440, 9500,
            "STATIC_NORMAL", "upgrade_aquatic:river_tree", ["#upgrade_aquatic:has_feature/river_tree"])),
        ("deep_aether/roseroot.json", profile(
            "deep_aether:roseroot", "broadleaf_round",
            "deep_aether:roseroot_log", "deep_aether:roseroot_leaves", "deep_aether:roseroot_wood", "deep_aether:roseroot_sapling",
            [6, 10], [0.6, 0.9], [0.02, 0.14], [4, 7], [1, 2], [25, 52], [0.26, 0.45], [0.1, 0.35],
            "alternate", [0.95, 1.45], [0.55, 0.85], [6, 12], 128, 420, 9500,
            "STATIC_NORMAL", "deep_aether:roseroot_tree_small", ["deep_aether:aerglow_forest"])),
        ("deep_aether/blue_roseroot.json", profile(
            "deep_aether:blue_roseroot", "broadleaf_round",
            "deep_aether:roseroot_log", "deep_aether:blue_roseroot_leaves", "deep_aether:roseroot_wood", "deep_aether:blue_roseroot_sapling",
            [6, 10], [0.6, 0.9], [0.02, 0.14], [4, 7], [1, 2], [25, 52], [0.26, 0.45], [0.1, 0.35],
            "alternate", [0.95, 1.45], [0.55, 0.85], [6, 12], 128, 420, 9500,
            "STATIC_NORMAL", "deep_aether:blue_roseroot_tree_small", ["deep_aether:blue_aerglow_forest"])),
        ("deep_aether/yagroot.json", profile(
            "deep_aether:yagroot", "mangrove",
            "deep_aether:yagroot_log", "deep_aether:yagroot_leaves", "deep_aether:yagroot_wood", "deep_aether:yagroot_sapling",
            [6, 10], [0.65, 1.0], [0.08, 0.28], [4, 8], [2, 3], [32, 68], [0.3, 0.5], [0.2, 0.5],
            "irregular", [1.2, 1.9], [0.5, 0.8], [7, 14], 152, 480, 11000,
            "STATIC_NORMAL", "deep_aether:yagroot_tree", ["deep_aether:yagroot_swamp"])),
        ("deep_aether/cruderoot.json", profile(
            "deep_aether:cruderoot", "conifer",
            "deep_aether:cruderoot_log", "deep_aether:cruderoot_leaves", "deep_aether:cruderoot_wood", "deep_aether:cruderoot_sapling",
            [6, 11], [0.55, 0.85], [0.0, 0.1], [4, 8], [1, 2], [10, 30], [0.18, 0.34], [0.05, 0.2],
            "whorled", [0.55, 0.85], [0.6, 0.9], [8, 14], 140, 440, 9500,
            "STATIC_NORMAL", "deep_aether:cruderoot_tree", ["deep_aether:yagroot_swamp"])),
        ("deep_aether/conberry.json", profile(
            "deep_aether:conberry", "birch",
            "deep_aether:conberry_log", "deep_aether:conberry_leaves", "deep_aether:conberry_wood", "deep_aether:conberry_sapling",
            [7, 13], [0.55, 0.8], [0.02, 0.12], [4, 7], [1, 2], [22, 45], [0.2, 0.35], [0.08, 0.25],
            "alternate", [0.65, 1.05], [0.5, 0.8], [6, 12], 120, 420, 9000,
            "STATIC_NORMAL", "deep_aether:conberry_tree", ["deep_aether:golden_grove"])),
        ("deep_aether/sunroot.json", profile(
            "deep_aether:sunroot", "mangrove",
            "deep_aether:sunroot_log", "deep_aether:sunroot_leaves", "deep_aether:sunroot_wood", "deep_aether:sunroot_sapling",
            [6, 11], [0.6, 0.95], [0.06, 0.24], [4, 7], [1, 3], [30, 62], [0.28, 0.48], [0.15, 0.45],
            "irregular", [1.0, 1.6], [0.5, 0.8], [7, 14], 144, 460, 10000,
            "STATIC_NORMAL", "deep_aether:sunroot_tree", ["deep_aether:golden_grove"])),
        ("deeperdarker/echo.json", profile(
            "deeperdarker:echo", "broadleaf_irregular",
            "deeperdarker:echo_log", "deeperdarker:echo_leaves", "deeperdarker:echo_wood", "deeperdarker:echo_sapling",
            [6, 11], [0.65, 1.0], [0.04, 0.18], [4, 7], [1, 3], [28, 58], [0.28, 0.48], [0.12, 0.38],
            "alternate", [0.95, 1.5], [0.5, 0.8], [6, 13], 144, 460, 10000,
            "STATIC_NORMAL", "deeperdarker:echo_tree", ["deeperdarker:echoing_forest"])),
        ("alexscaves/pewen.json", profile(
            "alexscaves:pewen", "conifer",
            "alexscaves:pewen_log", "alexscaves:pewen_pines", "alexscaves:pewen_wood", "alexscaves:pewen_sapling",
            [8, 16], [0.65, 1.0], [0.0, 0.08], [5, 9], [1, 2], [10, 28], [0.2, 0.36], [0.05, 0.2],
            "whorled", [0.55, 0.9], [0.55, 0.85], [8, 15], 160, 480, 10500,
            "SPECIAL_PRESERVE", "alexscaves:pewen_tree", ["alexscaves:primordial_caves"])),
        ("alexscaves/thornwood.json", profile(
            "alexscaves:thornwood", "acacia",
            "alexscaves:thornwood_log", "alexscaves:pewen_pines", "alexscaves:thornwood_wood", "alexscaves:thornwood_sapling",
            [6, 12], [0.6, 0.9], [0.08, 0.26], [3, 6], [1, 2], [32, 65], [0.3, 0.5], [0.2, 0.5],
            "irregular", [0.95, 1.5], [0.4, 0.65], [5, 10], 128, 360, 8500,
            "SPECIAL_PRESERVE", "alexscaves:thornwood_tree", ["alexscaves:primordial_caves"])),
        ("baobabtree/baobab.json", profile(
            "baobabtree:baobab", "baobab",
            "baobabtree:baobab_log", "baobabtree:baobab_leaves", None, "baobabtree:baobab_sapling",
            [9, 15], [1.25, 1.8], [0.02, 0.1], [4, 7], [2, 3], [40, 72], [0.28, 0.46], [0.15, 0.35],
            "alternate", [1.4, 2.2], [0.55, 0.85], [7, 15], 192, 600, 14000,
            "STATIC_NORMAL", "baobabtree:mature_baobab_placed", ["#baobabtree:has_baobab_generation"])),
        ("minecraft/pale_oak.json", profile(
            "minecraft:pale_oak", "broadleaf_round",
            "minecraft:pale_oak_log", "minecraft:pale_oak_leaves", "minecraft:pale_oak_wood", "minecraft:pale_oak_sapling",
            [6, 10], [1.0, 1.35], [0.03, 0.14], [5, 8], [1, 3], [32, 62], [0.28, 0.48], [0.18, 0.45],
            "alternate", [1.3, 1.9], [0.55, 0.85], [8, 16], 160, 520, 12000,
            "STATIC_NORMAL", "minecraft:pale_oak", ["minecraft:pale_garden"])),
        ("minecraft/poplar.json", profile(
            "minecraft:poplar", "birch",
            "minecraft:poplar_log", "minecraft:poplar_leaves", "minecraft:poplar_wood", "minecraft:poplar_sapling",
            [8, 14], [0.45, 0.7], [0.0, 0.06], [4, 7], [1, 2], [18, 36], [0.16, 0.28], [0.05, 0.2],
            "alternate", [0.48, 0.78], [0.5, 0.8], [7, 13], 120, 420, 9000,
            "STATIC_NORMAL", "minecraft:red_poplar", ["minecraft:forest"])),
    ]
    for rel, data in profiles:
        write_json(SPECIES_DIR / rel, data)


def write_vanilla_expansions() -> None:
    # Single species static trees & checked placements needed by newly replaced vanilla aggregates
    singles = [
        ("static_birch", "minecraft:birch", "minecraft:birch_sapling"),
        ("static_cherry", "minecraft:cherry", "minecraft:cherry_sapling"),
        ("static_jungle", "minecraft:jungle", "minecraft:jungle_sapling"),
    ]
    for name, species, sapling in singles:
        write_json(CF_DIR / f"{name}.json", static_tree_cf(species))
        write_json(PF_DIR / f"{name}_checked.json", checked_pf(f"branched:{name}", sapling))

    # Aggregates mapping 1:1 to vanilla placed feature names
    aggs = [
        ("trees_birch", ["minecraft:birch_forest", "minecraft:old_growth_birch_forest"],
         ["minecraft:birch"],
         static_tree_cf("minecraft:birch"),
         weighted_count_pf("branched:static_trees_birch", 10, 9, 11, 1, 0, "minecraft:birch_sapling")),

        ("trees_birch_and_oak", ["minecraft:forest"],
         ["minecraft:oak", "minecraft:birch", "minecraft:fancy_oak"],
         {
             "type": "minecraft:random_selector",
             "config": {
                 "default": "branched:static_oak_checked",
                 "features": [
                     {"chance": 0.2, "feature": "branched:static_birch_checked"},
                     {"chance": 0.1, "feature": "branched:static_fancy_oak_checked"},
                 ]
             }
         },
         weighted_count_pf("branched:static_trees_birch_and_oak", 10, 9, 11, 1)),

        ("trees_cherry", ["minecraft:cherry_grove"],
         ["minecraft:cherry"],
         {
             "type": "minecraft:random_selector",
             "config": {
                 "default": "branched:static_cherry_checked",
                 "features": []
             }
         },
         weighted_count_pf("branched:static_trees_cherry", 10, 9, 11, 1)),

        ("trees_flower_forest", ["minecraft:flower_forest"],
         ["minecraft:oak", "minecraft:birch", "minecraft:fancy_oak"],
         {
             "type": "minecraft:random_selector",
             "config": {
                 "default": "branched:static_oak_checked",
                 "features": [
                     {"chance": 0.2, "feature": "branched:static_birch_checked"},
                     {"chance": 0.1, "feature": "branched:static_fancy_oak_checked"},
                 ]
             }
         },
         weighted_count_pf("branched:static_trees_flower_forest", 6, 9, 7, 1)),

        ("trees_meadow", ["minecraft:meadow"],
         ["minecraft:birch", "minecraft:fancy_oak"],
         {
             "type": "minecraft:random_selector",
             "config": {
                 "default": "branched:static_birch_checked",
                 "features": [
                     {"chance": 0.5, "feature": "branched:static_fancy_oak_checked"},
                 ]
             }
         },
         {
             "feature": "branched:static_trees_meadow",
             "placement": [
                 {"type": "minecraft:rarity_filter", "chance": 100},
                 {"type": "minecraft:in_square"},
                 {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
                 {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
                 {"type": "minecraft:biome"},
             ]
         }),

        ("trees_jungle", ["minecraft:jungle", "minecraft:bamboo_jungle"],
         ["minecraft:jungle", "minecraft:fancy_oak"],
         {
             "type": "minecraft:random_selector",
             "config": {
                 "default": "branched:static_jungle_checked",
                 "features": [
                     {"chance": 0.12, "feature": "branched:static_fancy_oak_checked"},
                 ]
             }
         },
         weighted_count_pf("branched:static_trees_jungle", 22, 9, 25, 1)),

        ("trees_sparse_jungle", ["minecraft:sparse_jungle"],
         ["minecraft:jungle", "minecraft:fancy_oak"],
         {
             "type": "minecraft:random_selector",
             "config": {
                 "default": "branched:static_jungle_checked",
                 "features": [
                     {"chance": 0.15, "feature": "branched:static_fancy_oak_checked"},
                 ]
             }
         },
         weighted_count_pf("branched:static_trees_sparse_jungle", 2, 9, 3, 1)),

        ("trees_swamp", ["minecraft:swamp"],
         ["minecraft:oak"],
         static_tree_cf("minecraft:oak"),
         weighted_count_pf("branched:static_trees_swamp", 2, 9, 3, 1, 0, "minecraft:oak_sapling")),

        ("trees_old_growth_pine_taiga", ["minecraft:old_growth_pine_taiga"],
         ["minecraft:pine", "minecraft:spruce"],
         {
             "type": "minecraft:random_selector",
             "config": {
                 "default": "branched:static_pine_checked",
                 "features": [
                     {"chance": 0.35, "feature": "branched:static_spruce_checked"},
                 ]
             }
         },
         weighted_count_pf("branched:static_trees_old_growth_pine_taiga", 10, 9, 11, 1)),

        ("trees_old_growth_spruce_taiga", ["minecraft:old_growth_spruce_taiga"],
         ["minecraft:spruce", "minecraft:pine"],
         {
             "type": "minecraft:random_selector",
             "config": {
                 "default": "branched:static_spruce_checked",
                 "features": [
                     {"chance": 0.33333334, "feature": "branched:static_pine_checked"},
                 ]
             }
         },
         weighted_count_pf("branched:static_trees_old_growth_spruce_taiga", 10, 9, 11, 1)),
    ]

    for name, biomes, species_list, cf, pf in aggs:
        write_json(CF_DIR / f"static_{name}.json", cf)
        write_json(PF_DIR / f"static_{name}.json", pf)
        write_json(BM_DIR / f"vanilla_{name}.json", {
            "type": "branched:replace_trees",
            "biomes": biomes,
            "add": f"branched:static_{name}",
            "replace": [f"minecraft:{name}"],
            "species": species_list,
            "step": "vegetal_decoration",
        })


def write_modpack_integrations() -> None:
    # Tag-safe integrations under branched:modpack/<mod>/<name>
    # Never reference foreign block/biome/feature ids inline outside optional tags!
    entries = [
        ("ecologics", "trees_walnut", "ecologics:walnut", ["ecologics:walnut"],
         ["minecraft:plains"], "ecologics:trees_walnut",
         weighted_count_pf("branched:modpack/ecologics/trees_walnut", 0, 19, 1, 1)),

        ("upgrade_aquatic", "river_tree", "upgrade_aquatic:river", ["upgrade_aquatic:river"],
         [{"id": "#upgrade_aquatic:has_feature/river_tree", "required": False}],
         "upgrade_aquatic:river_tree",
         weighted_count_pf("branched:modpack/upgrade_aquatic/river_tree", 1, 1, 3, 1, 0, "minecraft:oak_sapling")),

        ("deeperdarker", "echo_tree", "deeperdarker:echo", ["deeperdarker:echo"],
         [{"id": "deeperdarker:echoing_forest", "required": False}],
         "deeperdarker:echo_tree",
         {
             "feature": "branched:modpack/deeperdarker/echo_tree",
             "placement": [
                 {"type": "minecraft:count_on_every_layer", "count": 8},
                 {"type": "minecraft:biome"},
             ]
         }),

        ("baobabtree", "mature_baobab_placed", "baobabtree:baobab", ["baobabtree:baobab"],
         [{"id": "#baobabtree:has_baobab_generation", "required": False}],
         "baobabtree:mature_baobab_placed",
         {
             "feature": "branched:modpack/baobabtree/mature_baobab_placed",
             "placement": [
                 {"type": "minecraft:rarity_filter", "chance": 90},
                 {"type": "minecraft:in_square"},
                 {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
                 {"type": "minecraft:biome"},
             ]
         }),

        ("baobabtree", "young_baobab_placed", "baobabtree:baobab", ["baobabtree:baobab"],
         [{"id": "#baobabtree:has_baobab_generation", "required": False}],
         "baobabtree:young_baobab_placed",
         {
             "feature": "branched:modpack/baobabtree/young_baobab_placed",
             "placement": [
                 {"type": "minecraft:rarity_filter", "chance": 30},
                 {"type": "minecraft:in_square"},
                 {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
                 {"type": "minecraft:biome"},
             ]
         }),

        ("deep_aether", "aerglow_forest_trees_placement", "deep_aether:roseroot", ["deep_aether:roseroot"],
         [{"id": "deep_aether:aerglow_forest", "required": False}],
         "deep_aether:aerglow_forest_trees_placement",
         weighted_count_pf("branched:modpack/deep_aether/aerglow_forest_trees_placement", 10, 9, 11, 1)),

        ("deep_aether", "blue_aerglow_forest_trees_placement", "deep_aether:blue_roseroot", ["deep_aether:blue_roseroot"],
         [{"id": "deep_aether:blue_aerglow_forest", "required": False}],
         "deep_aether:blue_aerglow_forest_trees_placement",
         weighted_count_pf("branched:modpack/deep_aether/blue_aerglow_forest_trees_placement", 10, 9, 11, 1)),

        ("deep_aether", "mystic_aerglow_forest_trees_placement", "deep_aether:roseroot", ["deep_aether:roseroot"],
         [{"id": "deep_aether:mystic_aerglow_forest", "required": False}],
         "deep_aether:mystic_aerglow_forest_trees_placement",
         weighted_count_pf("branched:modpack/deep_aether/mystic_aerglow_forest_trees_placement", 10, 9, 11, 1)),

        ("deep_aether", "yagroot_swamp_trees_placement", "deep_aether:yagroot", ["deep_aether:yagroot"],
         [{"id": "deep_aether:yagroot_swamp", "required": False}],
         "deep_aether:yagroot_swamp_trees_placement",
         weighted_count_pf("branched:modpack/deep_aether/yagroot_swamp_trees_placement", 16, 9, 18, 1)),

        ("deep_aether", "golden_grove_trees", "deep_aether:conberry", ["deep_aether:conberry"],
         [{"id": "deep_aether:golden_grove", "required": False}],
         "deep_aether:golden_grove_trees",
         weighted_count_pf("branched:modpack/deep_aether/golden_grove_trees", 22, 9, 25, 1)),

        ("deep_aether", "golden_heights_trees", "deep_aether:conberry", ["deep_aether:conberry"],
         [{"id": "deep_aether:golden_heights", "required": False}],
         "deep_aether:golden_heights_trees",
         weighted_count_pf("branched:modpack/deep_aether/golden_heights_trees", 5, 1, 7, 1)),
    ]

    for mod, name, species, species_gate, biomes, orig_pf, pf_json in entries:
        key = f"modpack/{mod}/{name}"
        write_json(CF_DIR / f"{key}.json", static_tree_cf(species))
        write_json(PF_DIR / f"{key}.json", pf_json)
        write_json(BIOME_TAG_DIR / f"{key}.json", {"replace": False, "values": biomes})
        write_json(PF_TAG_DIR / f"{key}.json", {"replace": False, "values": [{"id": orig_pf, "required": False}]})
        write_json(BM_DIR / f"pack_{mod}_{name}.json", {
            "type": "branched:replace_trees",
            "biomes": f"#branched:{key}",
            "add": f"branched:{key}",
            "replace": f"#branched:{key}",
            "species": species_gate,
            "step": "vegetal_decoration",
        })


def main() -> None:
    update_pine_profiles()
    write_new_profiles()
    write_vanilla_expansions()
    write_modpack_integrations()
    print("Updated pine archetypes, wrote 15 new profiles, 10 vanilla aggregates, and 11 modpack integrations.")


if __name__ == "__main__":
    main()
