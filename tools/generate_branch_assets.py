#!/usr/bin/env python3
"""Generate 6-way multipart blockstates, baked templates, item models, loot tables, lang and tags."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/branched"
DATA_SA = RES / "data/branched"
DATA_MC = RES / "data/minecraft"

# (block_id, side_texture, end_texture, display_name)
BRANCHES = [
    # Vanilla + VanillaBackport
    ("oak_branch", "minecraft:block/oak_log", "minecraft:block/oak_log_top", "Oak Branch"),
    ("spruce_branch", "minecraft:block/spruce_log", "minecraft:block/spruce_log_top", "Spruce Branch"),
    ("birch_branch", "minecraft:block/birch_log", "minecraft:block/birch_log_top", "Birch Branch"),
    ("jungle_branch", "minecraft:block/jungle_log", "minecraft:block/jungle_log_top", "Jungle Branch"),
    ("acacia_branch", "minecraft:block/acacia_log", "minecraft:block/acacia_log_top", "Acacia Branch"),
    ("dark_oak_branch", "minecraft:block/dark_oak_log", "minecraft:block/dark_oak_log_top", "Dark Oak Branch"),
    ("mangrove_branch", "minecraft:block/mangrove_log", "minecraft:block/mangrove_log_top", "Mangrove Branch"),
    ("cherry_branch", "minecraft:block/cherry_log", "minecraft:block/cherry_log_top", "Cherry Branch"),
    ("crimson_branch", "minecraft:block/crimson_stem", "minecraft:block/crimson_stem_top", "Crimson Branch"),
    ("warped_branch", "minecraft:block/warped_stem", "minecraft:block/warped_stem_top", "Warped Branch"),
    ("pale_oak_branch", "minecraft:block/pale_oak_log", "minecraft:block/pale_oak_log_top", "Pale Oak Branch"),
    ("poplar_branch", "minecraft:block/poplar_log", "minecraft:block/poplar_log_top", "Poplar Branch"),

    # Regions Unexplored
    ("pine_branch", "regions_unexplored:block/pine_log", "regions_unexplored:block/pine_log_top", "Pine Branch"),
    ("larch_branch", "regions_unexplored:block/larch_log", "regions_unexplored:block/larch_log_top", "Larch Branch"),
    ("maple_branch", "regions_unexplored:block/maple_log", "regions_unexplored:block/maple_log_top", "Maple Branch"),
    ("magnolia_branch", "regions_unexplored:block/magnolia_log", "regions_unexplored:block/magnolia_log_top", "Magnolia Branch"),
    ("blackwood_branch", "regions_unexplored:block/blackwood_log", "regions_unexplored:block/blackwood_log_top", "Blackwood Branch"),
    ("redwood_branch", "regions_unexplored:block/redwood_log", "regions_unexplored:block/redwood_log_top", "Redwood Branch"),
    ("kapok_branch", "regions_unexplored:block/kapok_log", "regions_unexplored:block/kapok_log_top", "Kapok Branch"),
    ("eucalyptus_branch", "regions_unexplored:block/eucalyptus_log", "regions_unexplored:block/eucalyptus_log_top", "Eucalyptus Branch"),
    ("baobab_branch", "regions_unexplored:block/baobab_log", "regions_unexplored:block/baobab_log_top", "Baobab Branch"),
    ("cypress_branch", "regions_unexplored:block/cypress_log", "regions_unexplored:block/cypress_log_top", "Cypress Branch"),
    ("palm_branch", "regions_unexplored:block/palm_log", "regions_unexplored:block/palm_log_top", "Palm Branch"),
    ("willow_branch", "regions_unexplored:block/willow_log", "regions_unexplored:block/willow_log_top", "Willow Branch"),
    ("silver_birch_branch", "regions_unexplored:block/silver_birch_log", "minecraft:block/birch_log_top", "Silver Birch Branch"),
    ("ashen_branch", "regions_unexplored:block/ashen_log", "regions_unexplored:block/ashen_log_top", "Ashen Branch"),
    ("dead_branch", "regions_unexplored:block/dead_log", "regions_unexplored:block/dead_log_top", "Dead Branch"),
    ("mauve_branch", "regions_unexplored:block/mauve_log", "regions_unexplored:block/mauve_log_top", "Mauve Branch"),
    ("socotra_branch", "regions_unexplored:block/socotra_log", "regions_unexplored:block/socotra_log_top", "Socotra Branch"),
    ("joshua_branch", "regions_unexplored:block/joshua_log", "regions_unexplored:block/joshua_log_top", "Joshua Branch"),
    ("brimwood_branch", "regions_unexplored:block/brimwood_log", "regions_unexplored:block/brimwood_log_top", "Brimwood Branch"),
    ("cobalt_branch", "regions_unexplored:block/cobalt_log", "regions_unexplored:block/cobalt_log_top", "Cobalt Branch"),
    ("alpha_oak_branch", "regions_unexplored:block/alpha_oak_log", "regions_unexplored:block/alpha_oak_log_top", "Alpha Oak Branch"),
    ("small_oak_branch", "regions_unexplored:block/small_oak_log", "regions_unexplored:block/small_oak_log", "Small Oak Branch"),

    # Aether & Deep Aether
    ("skyroot_branch", "aether:block/natural/skyroot_log", "aether:block/natural/skyroot_log_top", "Skyroot Branch"),
    ("golden_oak_branch", "aether:block/natural/golden_oak_log", "aether:block/natural/skyroot_log_top", "Golden Oak Branch"),
    ("roseroot_branch", "deep_aether:block/roseroot_log", "deep_aether:block/roseroot_log_top", "Roseroot Branch"),
    ("yagroot_branch", "deep_aether:block/yagroot_log", "deep_aether:block/yagroot_log_top", "Yagroot Branch"),
    ("cruderoot_branch", "deep_aether:block/cruderoot_log", "deep_aether:block/cruderoot_log_top", "Cruderoot Branch"),
    ("conberry_branch", "deep_aether:block/conberry_log", "deep_aether:block/conberry_log_top", "Conberry Branch"),
    ("sunroot_branch", "deep_aether:block/sunroot_log", "deep_aether:block/sunroot_log_top", "Sunroot Branch"),

    # Quark
    ("blossom_branch", "quark:block/blossom_log", "quark:block/blossom_log_top", "Blossom Branch"),
    ("ancient_branch", "quark:block/ancient_log", "quark:block/ancient_log_top", "Ancient Branch"),
    ("azalea_branch", "quark:block/azalea_log", "quark:block/azalea_log_top", "Azalea Branch"),

    # Fish of Thieves
    ("banana_branch", "fishofthieves:block/banana_stem_side", "fishofthieves:block/banana_stem_top", "Banana Stem Branch"),
    ("fot_coconut_branch", "fishofthieves:block/coconut_log", "fishofthieves:block/coconut_log_top", "Tropical Coconut Branch"),

    # Twilight Forest
    ("twilight_oak_branch", "twilightforest:block/twilight_oak_log", "twilightforest:block/twilight_oak_log_top", "Twilight Oak Branch"),
    ("canopy_branch", "twilightforest:block/canopy_log", "twilightforest:block/canopy_log_top", "Canopy Branch"),
    ("darkwood_branch", "twilightforest:block/dark_log", "twilightforest:block/dark_log_top", "Darkwood Branch"),
    ("twilight_mangrove_branch", "twilightforest:block/mangrove_log", "twilightforest:block/mangrove_log_top", "Twilight Mangrove Branch"),
    ("mining_branch", "twilightforest:block/mining_log", "twilightforest:block/mining_log_top", "Minewood Branch"),
    ("time_branch", "twilightforest:block/time_log", "twilightforest:block/time_log_top", "Timewood Branch"),
    ("transformation_branch", "twilightforest:block/transformation_log", "twilightforest:block/transformation_log_top", "Transwood Branch"),
    ("sorting_branch", "twilightforest:block/sorting_log", "twilightforest:block/sorting_log_top", "Sortingwood Branch"),

    # Biome Makeover
    ("bm_ancient_oak_branch", "biomemakeover:block/ancient_oak_log", "biomemakeover:block/ancient_oak_log_top", "Ancient Oak Branch"),
    ("bm_blighted_balsa_branch", "biomemakeover:block/blighted_balsa_log", "biomemakeover:block/blighted_balsa_log_top", "Blighted Balsa Branch"),
    ("bm_swamp_cypress_branch", "biomemakeover:block/swamp_cypress_log", "biomemakeover:block/swamp_cypress_log_top", "Swamp Cypress Branch"),
    ("bm_willow_branch", "biomemakeover:block/willow_log", "biomemakeover:block/willow_log_top", "Swamp Willow Branch"),

    # Ecologics, Upgrade Aquatic, BaobabTree, Deeper & Darker, Alex's Caves
    ("walnut_branch", "ecologics:block/walnut_log", "ecologics:block/walnut_log_top", "Walnut Branch"),
    ("coconut_branch", "ecologics:block/coconut_log", "ecologics:block/coconut_log_top", "Coconut Branch"),
    ("ecologics_azalea_branch", "ecologics:block/azalea_log", "ecologics:block/azalea_log_top", "Flowering Azalea Branch"),
    ("river_branch", "upgrade_aquatic:block/river_log", "upgrade_aquatic:block/river_log_top", "River Branch"),
    ("baobabtree_branch", "baobabtree:block/baobab_log", "baobabtree:block/baobab_log_top", "Savanna Baobab Branch"),
    ("echo_branch", "deeperdarker:block/echo_log", "deeperdarker:block/echo_log_top", "Echo Branch"),
    ("pewen_branch", "alexscaves:block/pewen/pewen_log", "alexscaves:block/pewen/pewen_log_top", "Pewen Branch"),
    ("thornwood_branch", "alexscaves:block/thornwood/thornwood_log", "alexscaves:block/thornwood/thornwood_log_top", "Thornwood Branch"),
    ("licoroot_branch", "alexscaves:block/licoroot/licoroot", "alexscaves:block/licoroot/licoroot_top", "Licoroot Branch"),

    # Deep Aether & Twilight Delight additions
    ("rotten_roseroot_branch", "deep_aether:block/rotten_roseroot_log", "deep_aether:block/roseroot_log_top", "Rotten Roseroot Branch"),
    ("ironwood_branch", "twilightdelight:block/ironwood_log", "twilightdelight:block/ironwood_log_top", "Ironwood Branch"),

    # BetterEnd
    ("dragon_tree_branch", "betterend:block/dragon_tree_log_side", "betterend:block/dragon_tree_log_top", "Dragon Tree Branch"),
    ("helix_tree_branch", "betterend:block/helix_tree_log_side", "betterend:block/helix_tree_log_top", "Helix Tree Branch"),
    ("lacugrove_branch", "betterend:block/lacugrove_log_side", "betterend:block/lacugrove_log_top", "Lacugrove Branch"),
    ("lucernia_branch", "betterend:block/lucernia_log_side", "betterend:block/lucernia_log_top", "Lucernia Branch"),
    ("pythadendron_branch", "betterend:block/pythadendron_log_side", "betterend:block/pythadendron_log_top", "Pythadendron Branch"),
    ("tenanea_branch", "betterend:block/tenanea_log_side", "betterend:block/tenanea_log_top", "Tenanea Branch"),
    ("umbrella_tree_branch", "betterend:block/umbrella_tree_log_side", "betterend:block/umbrella_tree_log_top", "Umbrella Tree Branch"),

    # BetterNether
    ("anchor_tree_branch", "betternether:block/anchor_tree_log_side", "betternether:block/anchor_tree_log_top", "Anchor Tree Branch"),
    ("gloomwood_branch", "betternether:block/gloomwood_log_side", "betternether:block/gloomwood_log_top", "Gloomwood Branch"),
    ("gloomwood_dark_branch", "betternether:block/gloomwood_dark_log_side", "betternether:block/gloomwood_dark_log_top", "Dark Gloomwood Branch"),
    ("nether_sakura_branch", "betternether:block/nether_sakura_log_side", "betternether:block/nether_sakura_log_top", "Nether Sakura Branch"),
    ("rubeus_branch", "betternether:block/rubeus_log_side", "betternether:block/rubeus_log_top", "Rubeus Branch"),
    ("bn_willow_branch", "betternether:block/willow_bark", "betternether:block/willow_log_top", "Nether Willow Branch"),
    ("bn_wart_branch", "betternether:block/wart_bark", "betternether:block/wart_log_top", "Nether Wart Branch"),

    # Biomes O' Plenty
    ("bop_dead_branch", "biomesoplenty:block/dead_log", "biomesoplenty:block/dead_log_top", "Dead Branch"),
    ("bop_empyreal_branch", "biomesoplenty:block/empyreal_log", "biomesoplenty:block/empyreal_log_top", "Empyreal Branch"),
    ("bop_fir_branch", "biomesoplenty:block/fir_log", "biomesoplenty:block/fir_log_top", "Fir Branch"),
    ("bop_hellbark_branch", "biomesoplenty:block/hellbark_log", "biomesoplenty:block/hellbark_log_top", "Hellbark Branch"),
    ("bop_jacaranda_branch", "biomesoplenty:block/jacaranda_log", "biomesoplenty:block/jacaranda_log_top", "Jacaranda Branch"),
    ("bop_magic_branch", "biomesoplenty:block/magic_log", "biomesoplenty:block/magic_log_top", "Magic Branch"),
    ("bop_mahogany_branch", "biomesoplenty:block/mahogany_log", "biomesoplenty:block/mahogany_log_top", "Mahogany Branch"),
    ("bop_maple_branch", "biomesoplenty:block/maple_log", "biomesoplenty:block/maple_log_top", "BOP Maple Branch"),
    ("bop_palm_branch", "biomesoplenty:block/palm_log", "biomesoplenty:block/palm_log_top", "BOP Palm Branch"),
    ("bop_pine_branch", "biomesoplenty:block/pine_log", "biomesoplenty:block/pine_log_top", "BOP Pine Branch"),
    ("bop_redwood_branch", "biomesoplenty:block/redwood_log", "biomesoplenty:block/redwood_log_top", "BOP Redwood Branch"),
    ("bop_umbran_branch", "biomesoplenty:block/umbran_log", "biomesoplenty:block/umbran_log_top", "Umbran Branch"),
    ("bop_willow_branch", "biomesoplenty:block/willow_log", "biomesoplenty:block/willow_log_top", "BOP Willow Branch"),

    # Oh The Biomes We've Gone
    ("bwg_aspen_branch", "biomeswevegone:block/aspen/log", "biomeswevegone:block/aspen/log_top", "Aspen Branch"),
    ("bwg_baobab_branch", "biomeswevegone:block/baobab/log", "biomeswevegone:block/baobab/log_top", "BWG Baobab Branch"),
    ("bwg_blue_enchanted_branch", "biomeswevegone:block/blue_enchanted/log", "biomeswevegone:block/blue_enchanted/log_top", "Blue Enchanted Branch"),
    ("bwg_cika_branch", "biomeswevegone:block/cika/log", "biomeswevegone:block/cika/log_top", "Cika Branch"),
    ("bwg_cypress_branch", "biomeswevegone:block/cypress/log", "biomeswevegone:block/cypress/log_top", "BWG Cypress Branch"),
    ("bwg_ebony_branch", "biomeswevegone:block/ebony/log", "biomeswevegone:block/ebony/log_top", "Ebony Branch"),
    ("bwg_fir_branch", "biomeswevegone:block/fir/log", "biomeswevegone:block/fir/log_top", "BWG Fir Branch"),
    ("bwg_green_enchanted_branch", "biomeswevegone:block/green_enchanted/log", "biomeswevegone:block/green_enchanted/log_top", "Green Enchanted Branch"),
    ("bwg_holly_branch", "biomeswevegone:block/holly/log", "biomeswevegone:block/holly/log_top", "Holly Branch"),
    ("bwg_ironwood_branch", "biomeswevegone:block/ironwood/log", "biomeswevegone:block/ironwood/log_top", "BWG Ironwood Branch"),
    ("bwg_jacaranda_branch", "biomeswevegone:block/jacaranda/log", "biomeswevegone:block/jacaranda/log_top", "BWG Jacaranda Branch"),
    ("bwg_mahogany_branch", "biomeswevegone:block/mahogany/log", "biomeswevegone:block/mahogany/log_top", "BWG Mahogany Branch"),
    ("bwg_maple_branch", "biomeswevegone:block/maple/log", "biomeswevegone:block/maple/log_top", "BWG Maple Branch"),
    ("bwg_palm_branch", "biomeswevegone:block/palm/log", "biomeswevegone:block/palm/log_top", "BWG Palm Branch"),
    ("bwg_palo_verde_branch", "biomeswevegone:block/palo_verde/log", "biomeswevegone:block/palo_verde/log_top", "Palo Verde Branch"),
    ("bwg_pine_branch", "biomeswevegone:block/pine/log", "biomeswevegone:block/pine/log_top", "BWG Pine Branch"),
    ("bwg_rainbow_eucalyptus_branch", "biomeswevegone:block/rainbow_eucalyptus/log", "biomeswevegone:block/rainbow_eucalyptus/log_top", "Rainbow Eucalyptus Branch"),
    ("bwg_redwood_branch", "biomeswevegone:block/redwood/log", "biomeswevegone:block/redwood/log_top", "BWG Redwood Branch"),
    ("bwg_sakura_branch", "biomeswevegone:block/sakura/log", "biomeswevegone:block/sakura/log_top", "Sakura Branch"),
    ("bwg_skyris_branch", "biomeswevegone:block/skyris/log", "biomeswevegone:block/skyris/log_top", "Skyris Branch"),
    ("bwg_white_mangrove_branch", "biomeswevegone:block/white_mangrove/log", "biomeswevegone:block/white_mangrove/log_top", "White Mangrove Branch"),
    ("bwg_willow_branch", "biomeswevegone:block/willow/log", "biomeswevegone:block/willow/log_top", "BWG Willow Branch"),
    ("bwg_witch_hazel_branch", "biomeswevegone:block/witch_hazel/log", "biomeswevegone:block/witch_hazel/log_top", "Witch Hazel Branch"),
    ("bwg_zelkova_branch", "biomeswevegone:block/zelkova/log", "biomeswevegone:block/zelkova/log_top", "Zelkova Branch"),

    # Nature's Spirit
    ("ns_aspen_branch", "natures_spirit:block/aspen_log", "natures_spirit:block/aspen_log_top", "Nature's Spirit Aspen Branch"),
    ("ns_cedar_branch", "natures_spirit:block/cedar_log", "natures_spirit:block/cedar_log_top", "Cedar Branch"),
    ("ns_coconut_branch", "natures_spirit:block/coconut_log", "natures_spirit:block/coconut_log_top", "Nature's Spirit Coconut Branch"),
    ("ns_cypress_branch", "natures_spirit:block/cypress_log", "natures_spirit:block/cypress_log_top", "Nature's Spirit Cypress Branch"),
    ("ns_fir_branch", "natures_spirit:block/fir_log", "natures_spirit:block/fir_log_top", "Nature's Spirit Fir Branch"),
    ("ns_ghaf_branch", "natures_spirit:block/ghaf_log", "natures_spirit:block/ghaf_log_top", "Ghaf Branch"),
    ("ns_joshua_branch", "natures_spirit:block/joshua_log", "natures_spirit:block/joshua_log_top", "Nature's Spirit Joshua Branch"),
    ("ns_larch_branch", "natures_spirit:block/larch_log", "natures_spirit:block/larch_log_top", "Nature's Spirit Larch Branch"),
    ("ns_mahogany_branch", "natures_spirit:block/mahogany_log", "natures_spirit:block/mahogany_log_top", "Nature's Spirit Mahogany Branch"),
    ("ns_maple_branch", "natures_spirit:block/maple_log", "natures_spirit:block/maple_log_top", "Nature's Spirit Maple Branch"),
    ("ns_olive_branch", "natures_spirit:block/olive_log", "natures_spirit:block/olive_log_top", "Olive Branch"),
    ("ns_palo_verde_branch", "natures_spirit:block/palo_verde_log", "natures_spirit:block/palo_verde_log_top", "Nature's Spirit Palo Verde Branch"),
    ("ns_redwood_branch", "natures_spirit:block/redwood_log", "natures_spirit:block/redwood_log_top", "Nature's Spirit Redwood Branch"),
    ("ns_saxaul_branch", "natures_spirit:block/saxaul_log", "natures_spirit:block/saxaul_log_top", "Saxaul Branch"),
    ("ns_sugi_branch", "natures_spirit:block/sugi_log", "natures_spirit:block/sugi_log_top", "Sugi Branch"),
    ("ns_willow_branch", "natures_spirit:block/willow_log", "natures_spirit:block/willow_log_top", "Nature's Spirit Willow Branch"),
    ("ns_wisteria_branch", "natures_spirit:block/wisteria_log", "natures_spirit:block/wisteria_log_top", "Wisteria Branch"),

    # Atmospheric
    ("atm_aspen_branch", "atmospheric:block/aspen_log", "atmospheric:block/aspen_log_top", "Atmospheric Aspen Branch"),
    ("atm_crustose_branch", "atmospheric:block/crustose_log_side", "atmospheric:block/crustose_log_top", "Crustose Branch"),
    ("atm_grimwood_branch", "atmospheric:block/grimwood_log", "atmospheric:block/grimwood_log_top", "Grimwood Branch"),
    ("atm_kousa_branch", "atmospheric:block/kousa_log", "atmospheric:block/kousa_log_top", "Kousa Branch"),
    ("atm_laurel_branch", "atmospheric:block/laurel_log", "atmospheric:block/laurel_log_top", "Laurel Branch"),
    ("atm_morado_branch", "atmospheric:block/morado_log", "atmospheric:block/morado_log_top", "Morado Branch"),
    ("atm_rosewood_branch", "atmospheric:block/rosewood_log", "atmospheric:block/rosewood_log_top", "Rosewood Branch"),
    ("atm_yucca_branch", "atmospheric:block/yucca_log", "atmospheric:block/yucca_log_top", "Yucca Branch"),

    # Environmental
    ("env_pine_branch", "environmental:block/pine_log", "environmental:block/pine_log_top", "Environmental Pine Branch"),
    ("env_plum_branch", "environmental:block/plum_log", "environmental:block/plum_log_top", "Plum Branch"),
    ("env_willow_branch", "environmental:block/willow_log", "environmental:block/willow_log_top", "Environmental Willow Branch"),
    ("env_wisteria_branch", "environmental:block/wisteria_log", "environmental:block/wisteria_log_top", "Environmental Wisteria Branch"),

    # Autumnity
    ("aut_maple_branch", "autumnity:block/maple_log", "autumnity:block/maple_log_top", "Autumnity Maple Branch"),
]


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def write_gauge_templates(models_dir: Path, prefix: str, lo: int, hi: int) -> None:
    """Write baked core and 3 sleeve templates for a given pixel gauge [lo..hi]."""
    uv_arm = hi if lo >= 8 else lo  # lo..hi always symmetric around 8 (3..13 -> 3, 4..12 -> 4, 5..11 -> 5)
    up_span = 16 - hi

    # Core joint: bark (#side) on all 6 faces so elbows and gauge transitions never expose cut rings
    write_json(models_dir / f"branch_{prefix}core_template.json", {
        "parent": "minecraft:block/block",
        "elements": [{
            "from": [lo, lo, lo],
            "to": [hi, hi, hi],
            "faces": {
                "down":  {"uv": [lo, lo, hi, hi], "texture": "#side"},
                "up":    {"uv": [lo, lo, hi, hi], "texture": "#side"},
                "north": {"uv": [lo, lo, hi, hi], "texture": "#side"},
                "south": {"uv": [lo, lo, hi, hi], "texture": "#side"},
                "west":  {"uv": [lo, lo, hi, hi], "texture": "#side"},
                "east":  {"uv": [lo, lo, hi, hi], "texture": "#side"},
            }
        }]
    })

    # North sleeve [lo,lo,0]..[hi,hi,lo]: flush arm with bark grain aligned along Z and bark end cap
    write_json(models_dir / f"branch_{prefix}sleeve_north_template.json", {
        "parent": "minecraft:block/block",
        "elements": [{
            "from": [lo, lo, 0],
            "to": [hi, hi, lo],
            "faces": {
                "down":  {"uv": [lo, 0, hi, uv_arm], "texture": "#side"},
                "up":    {"uv": [lo, 0, hi, uv_arm], "texture": "#side"},
                "west":  {"uv": [lo, 0, hi, uv_arm], "rotation": 270, "texture": "#side"},
                "east":  {"uv": [lo, 0, hi, uv_arm], "rotation": 90, "texture": "#side"},
                "north": {"uv": [lo, lo, hi, hi], "texture": "#side", "cullface": "north"},
            }
        }]
    })

    # Up sleeve [lo,hi,lo]..[hi,16,hi]
    write_json(models_dir / f"branch_{prefix}sleeve_up_template.json", {
        "parent": "minecraft:block/block",
        "elements": [{
            "from": [lo, hi, lo],
            "to": [hi, 16, hi],
            "faces": {
                "north": {"uv": [lo, 0, hi, up_span], "texture": "#side"},
                "south": {"uv": [lo, 0, hi, up_span], "texture": "#side"},
                "west":  {"uv": [lo, 0, hi, up_span], "texture": "#side"},
                "east":  {"uv": [lo, 0, hi, up_span], "texture": "#side"},
                "up":    {"uv": [lo, lo, hi, hi], "texture": "#side", "cullface": "up"},
            }
        }]
    })

    # Down sleeve [lo,0,lo]..[hi,lo,hi]
    write_json(models_dir / f"branch_{prefix}sleeve_down_template.json", {
        "parent": "minecraft:block/block",
        "elements": [{
            "from": [lo, 0, lo],
            "to": [hi, lo, hi],
            "faces": {
                "north": {"uv": [lo, hi, hi, 16], "texture": "#side"},
                "south": {"uv": [lo, hi, hi, 16], "texture": "#side"},
                "west":  {"uv": [lo, hi, hi, 16], "texture": "#side"},
                "east":  {"uv": [lo, hi, hi, 16], "texture": "#side"},
                "down":  {"uv": [lo, lo, hi, hi], "texture": "#side", "cullface": "down"},
            }
        }]
    })


def write_templates() -> None:
    models_dir = ASSETS / "models/block"
    # Clean legacy per-direction JSONs before writing multipart templates
    for old in models_dir.glob("*.json"):
        old.unlink()
    for old in (ASSETS / "blockstates").glob("*.json"):
        old.unlink()
    for old in (ASSETS / "models/item").glob("*.json"):
        old.unlink()
    for old in (DATA_SA / "loot_tables/blocks").glob("*.json"):
        old.unlink()

    for prefix, lo, hi in [("", 4, 12), ("thin_", 5, 11), ("thick_", 3, 13)]:
        write_gauge_templates(models_dir, prefix, lo, hi)

    # Inventory item model showing vertical limb + horizontal fork
    write_json(models_dir / "branch_inventory_template.json", {
        "parent": "minecraft:block/block",
        "elements": [
            {
                "from": [4, 0, 4],
                "to": [12, 16, 12],
                "faces": {
                    "down":  {"uv": [4, 4, 12, 12], "texture": "#end"},
                    "up":    {"uv": [4, 4, 12, 12], "texture": "#end"},
                    "north": {"uv": [4, 0, 12, 16], "texture": "#side"},
                    "south": {"uv": [4, 0, 12, 16], "texture": "#side"},
                    "west":  {"uv": [4, 0, 12, 16], "texture": "#side"},
                    "east":  {"uv": [4, 0, 12, 16], "texture": "#side"},
                }
            },
            {
                "from": [4, 6, 0],
                "to": [12, 14, 4],
                "faces": {
                    "down":  {"uv": [4, 0, 12, 4], "texture": "#side"},
                    "up":    {"uv": [4, 0, 12, 4], "texture": "#side"},
                    "west":  {"uv": [4, 0, 12, 4], "rotation": 270, "texture": "#side"},
                    "east":  {"uv": [4, 0, 12, 4], "rotation": 90, "texture": "#side"},
                    "north": {"uv": [4, 4, 12, 12], "texture": "#end"},
                }
            }
        ]
    })


def main() -> None:
    write_templates()
    lang = {}
    tag_values = []
    gauges = [("1", "thin_"), ("2", ""), ("3", "thick_")]

    for block_id, side, end, name in BRANCHES:
        textures = {"particle": side, "side": side, "end": end}

        multipart = []
        for thickness_str, p in gauges:
            multipart.append({"when": {"thickness": thickness_str}, "apply": {"model": f"branched:block/{block_id}_{p}core"}})
            multipart.append({"when": {"thickness": thickness_str, "north": "true"}, "apply": {"model": f"branched:block/{block_id}_{p}sleeve_north"}})
            multipart.append({"when": {"thickness": thickness_str, "east": "true"},  "apply": {"model": f"branched:block/{block_id}_{p}sleeve_north", "y": 90}})
            multipart.append({"when": {"thickness": thickness_str, "south": "true"}, "apply": {"model": f"branched:block/{block_id}_{p}sleeve_north", "y": 180}})
            multipart.append({"when": {"thickness": thickness_str, "west": "true"},  "apply": {"model": f"branched:block/{block_id}_{p}sleeve_north", "y": 270}})
            multipart.append({"when": {"thickness": thickness_str, "up": "true"},    "apply": {"model": f"branched:block/{block_id}_{p}sleeve_up"}})
            multipart.append({"when": {"thickness": thickness_str, "down": "true"},  "apply": {"model": f"branched:block/{block_id}_{p}sleeve_down"}})
        write_json(ASSETS / f"blockstates/{block_id}.json", {"multipart": multipart})

        for _, p in gauges:
            for suffix, template in [
                (f"{p}core", f"branch_{p}core_template"),
                (f"{p}sleeve_north", f"branch_{p}sleeve_north_template"),
                (f"{p}sleeve_up", f"branch_{p}sleeve_up_template"),
                (f"{p}sleeve_down", f"branch_{p}sleeve_down_template"),
            ]:
                write_json(ASSETS / f"models/block/{block_id}_{suffix}.json", {
                    "parent": f"branched:block/{template}",
                    "textures": textures,
                })
        write_json(ASSETS / f"models/block/{block_id}_inventory.json", {
            "parent": "branched:block/branch_inventory_template",
            "textures": textures,
        })

        write_json(ASSETS / f"models/item/{block_id}.json", {
            "parent": f"branched:block/{block_id}_inventory"
        })

        write_json(DATA_SA / f"loot_tables/blocks/{block_id}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{
                    "type": "minecraft:item",
                    "name": f"branched:{block_id}"
                }],
                "conditions": [{"condition": "minecraft:survives_explosion"}]
            }]
        })

        lang[f"block.branched.{block_id}"] = name
        tag_values.append(f"branched:{block_id}")

    write_json(ASSETS / "lang/en_us.json", lang)
    write_json(DATA_SA / "tags/blocks/branches.json", {"replace": False, "values": tag_values})
    write_json(DATA_SA / "tags/items/branches.json", {"replace": False, "values": tag_values})
    write_json(DATA_MC / "tags/blocks/logs.json", {"replace": False, "values": tag_values})
    write_json(DATA_MC / "tags/blocks/mineable/axe.json", {"replace": False, "values": tag_values})
    print(f"Generated 6-way multipart assets, loot tables, lang and tags for {len(BRANCHES)} branch blocks.")


if __name__ == "__main__":
    main()
