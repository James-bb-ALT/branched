#!/usr/bin/env python3
"""Generate the Phase 2 static mirror of the vanilla tree aggregates.

The mod replaces a small set of vanilla PlacedFeatures at biome level. Each
replacement must keep the original feature graph: the same placement modifiers,
the same random_selector chances and the same biome links. This script reads the
1.20.1 client jar, mirrors every tree/selector node with a static equivalent and
writes only the generated JSON into ``src/main/resources``.

Run from the repository root:

    python tools/generate_vanilla_phase2.py
"""
from __future__ import annotations

import json
import os
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
DATA = RES / "data/branched"
SPECIES_DIR = DATA / "arboreal/species"
CONFIGURED_DIR = DATA / "worldgen/configured_feature"
PLACED_DIR = DATA / "worldgen/placed_feature"
MODIFIER_DIR = DATA / "forge/biome_modifier"

MOD_ID = "branched"

# Vanilla PlacedFeature -> (biomes, species). Placement and selector chances
# are copied from the jar; this table only states the intent that must stay
# stable. Any drift between the jar and this table aborts the generation.
REPLACE = {
    "trees_badlands": {
        "biomes": ["minecraft:wooded_badlands"],
        "species": ["minecraft:oak"],
    },
    "trees_grove": {
        "biomes": ["minecraft:grove"],
        "species": ["minecraft:spruce", "minecraft:pine"],
    },
    "trees_savanna": {
        "biomes": ["minecraft:savanna", "minecraft:savanna_plateau"],
        "species": ["minecraft:oak", "minecraft:acacia"],
    },
    "trees_snowy": {
        "biomes": ["minecraft:ice_spikes", "minecraft:snowy_plains"],
        "species": ["minecraft:spruce"],
    },
    "trees_taiga": {
        "biomes": ["minecraft:taiga", "minecraft:snowy_taiga"],
        "species": ["minecraft:spruce", "minecraft:pine"],
    },
    "trees_windswept_forest": {
        "biomes": ["minecraft:windswept_forest"],
        "species": ["minecraft:oak", "minecraft:spruce", "minecraft:fancy_oak"],
    },
    "trees_windswept_hills": {
        "biomes": ["minecraft:windswept_gravelly_hills", "minecraft:windswept_hills"],
        "species": ["minecraft:oak", "minecraft:spruce", "minecraft:fancy_oak"],
    },
    "trees_windswept_savanna": {
        "biomes": ["minecraft:windswept_savanna"],
        "species": ["minecraft:oak", "minecraft:acacia"],
    },
}

# Vanilla configured tree feature -> shipped morphology profile.
TREE_SPECIES = {
    "minecraft:oak": "minecraft:oak",
    "minecraft:acacia": "minecraft:acacia",
    "minecraft:spruce": "minecraft:spruce",
    "minecraft:pine": "minecraft:pine",
    "minecraft:fancy_oak": "minecraft:fancy_oak",
}

PILOT_FILES = [
    PLACED_DIR / "static_spruce_taiga.json",
    MODIFIER_DIR / "static_spruce_taiga.json",
]


def jar_path() -> Path:
    override = os.environ.get("BRANCHED_CLIENT_JAR")
    if override:
        return Path(override)
    return Path.home() / (
        ".gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar"
    )


def path_of(resource_id: str) -> str:
    return resource_id.split(":", 1)[1]


def static_id(resource_id: str) -> str:
    return f"{MOD_ID}:static_{path_of(resource_id)}"


class Generator:
    def __init__(self, jar: zipfile.ZipFile):
        self.jar = jar
        self.configured: dict[str, dict] = {}
        self.placed: dict[str, dict] = {}
        self.used_species: set[str] = set()

    # -- vanilla access -------------------------------------------------
    def vanilla_configured(self, resource_id: str) -> dict:
        key = path_of(resource_id)
        return json.loads(
            self.jar.read(f"data/minecraft/worldgen/configured_feature/{key}.json")
        )

    def vanilla_placed(self, resource_id: str) -> dict:
        key = path_of(resource_id)
        return json.loads(
            self.jar.read(f"data/minecraft/worldgen/placed_feature/{key}.json")
        )

    def biome_links(self, placed_id: str) -> list[str]:
        links = []
        for name in self.jar.namelist():
            if not (
                name.startswith("data/minecraft/worldgen/biome/")
                and name.endswith(".json")
            ):
                continue
            biome = "minecraft:" + name.split("/")[-1][:-5]
            data = json.loads(self.jar.read(name))
            for step in data.get("features") or []:
                if placed_id in step:
                    links.append(biome)
        return sorted(links)

    # -- mirroring ------------------------------------------------------
    def mirror_placed(self, ref, inline_id: str | None = None) -> str:
        if isinstance(ref, dict):
            if inline_id is None:
                raise SystemExit("inline placed feature without an id anchor")
            target = inline_id
            payload = ref
        else:
            target = static_id(ref)
            if target in self.placed:
                return target
            payload = self.vanilla_placed(ref)

        feature = self.mirror_configured(payload["feature"])
        self.placed[target] = {
            "feature": feature,
            "placement": payload.get("placement", []),
        }
        return target

    def mirror_configured(self, ref) -> str:
        if isinstance(ref, dict):
            raise SystemExit("inline configured feature is not supported")
        if static_id(ref) in self.configured:
            return static_id(ref)

        payload = self.vanilla_configured(ref)
        kind = payload.get("type")
        target = static_id(ref)

        if kind == "minecraft:tree":
            if ref not in TREE_SPECIES:
                raise SystemExit(f"no species profile mapping for tree {ref}")
            species = TREE_SPECIES[ref]
            self.used_species.add(species)
            self.configured[target] = {
                "type": f"{MOD_ID}:static_tree",
                "config": {"species": species, "attempts": 3},
            }
        elif kind == "minecraft:random_selector":
            config = payload["config"]
            default_id = self.mirror_placed(
                config["default"], f"{MOD_ID}:static_{path_of(ref)}_default"
            )
            entries = []
            for index, entry in enumerate(config["features"]):
                placed_id = self.mirror_placed(
                    entry["feature"], f"{MOD_ID}:static_{path_of(ref)}_feature_{index}"
                )
                entries.append({"chance": entry["chance"], "feature": placed_id})
            self.configured[target] = {
                "type": "minecraft:random_selector",
                "config": {"default": default_id, "features": entries},
            }
        elif kind == "minecraft:simple_random_selector":
            entries = [
                self.mirror_placed(
                    entry, f"{MOD_ID}:static_{path_of(ref)}_feature_{index}"
                )
                for index, entry in enumerate(payload["config"]["features"])
            ]
            self.configured[target] = {
                "type": "minecraft:simple_random_selector",
                "config": {"features": entries},
            }
        elif kind == "minecraft:random_boolean_selector":
            config = payload["config"]
            self.configured[target] = {
                "type": "minecraft:random_boolean_selector",
                "config": {
                    "feature_true": self.mirror_placed(
                        config["feature_true"], f"{MOD_ID}:static_{path_of(ref)}_true"
                    ),
                    "feature_false": self.mirror_placed(
                        config["feature_false"], f"{MOD_ID}:static_{path_of(ref)}_false"
                    ),
                },
            }
        else:
            raise SystemExit(f"unexpected configured feature type {kind} for {ref}")

        return target


def write_json(path: Path, payload) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print("wrote", path.relative_to(ROOT).as_posix())


def check_profiles(species: set[str]) -> None:
    for resource_id in sorted(species):
        namespace, relative = resource_id.split(":", 1)
        if not (SPECIES_DIR / namespace / f"{relative}.json").is_file():
            raise SystemExit(f"missing species profile for {resource_id}")


def main() -> None:
    with zipfile.ZipFile(jar_path()) as jar:
        generator = Generator(jar)

        aggregates = []
        for aggregate, spec in REPLACE.items():
            placed_id = f"minecraft:{aggregate}"
            actual = generator.biome_links(placed_id)
            expected = sorted(spec["biomes"])
            if actual != expected:
                raise SystemExit(
                    f"biome links changed for {placed_id}: jar={actual} table={expected}"
                )
            aggregates.append((aggregate, spec, generator.mirror_placed(placed_id)))

        check_profiles(generator.used_species)

        for aggregate, spec, add in aggregates:
            write_json(
                MODIFIER_DIR / f"vanilla_{aggregate}.json",
                {
                    "type": f"{MOD_ID}:replace_trees",
                    "biomes": spec["biomes"],
                    "add": add,
                    "replace": [f"minecraft:{aggregate}"],
                    "species": spec["species"],
                    "step": "vegetal_decoration",
                },
            )

        for resource_id, payload in sorted(generator.configured.items()):
            write_json(CONFIGURED_DIR / f"{path_of(resource_id)}.json", payload)

        for resource_id, payload in sorted(generator.placed.items()):
            write_json(PLACED_DIR / f"{path_of(resource_id)}.json", payload)

    for pilot in PILOT_FILES:
        if pilot.is_file():
            pilot.unlink()
            print("removed", pilot.relative_to(ROOT).as_posix())

    print(
        f"generated {len(REPLACE)} biome modifiers, "
        f"{len(generator.configured)} configured and "
        f"{len(generator.placed)} placed features"
    )


if __name__ == "__main__":
    main()
