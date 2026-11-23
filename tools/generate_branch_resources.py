#!/usr/bin/env python3
"""Generate blockstates, models, item models and loot tables for the branches.

The branch blocks are plain baked models: a central joint plus one beam per
cardinal/vertical direction. Geometry is kept in sync with the cached VoxelShapes
in ``StaticBranchBlock``. Textures are referenced from the vanilla log, never
copied into this repository.

Run from the repository root:

    python tools/generate_branch_resources.py
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/branched"
DATA = RES / "data/branched"

MOD_ID = "branched"

# shape -> (direction name or None, texture used on the outward end face)
SHAPES = {
    "core": None,
    "up": "up",
    "down": "down",
    "north": "north",
    "south": "south",
    "east": "east",
    "west": "west",
}

# Materials: branch id -> (log texture, log top texture, block display name, wood)
MATERIALS = {
    "oak": {
        "side": "minecraft:block/oak_log",
        "end": "minecraft:block/oak_log_top",
        "name": "Oak Branch",
    },
    "spruce": {
        "side": "minecraft:block/spruce_log",
        "end": "minecraft:block/spruce_log_top",
        "name": "Spruce Branch",
    },
    "acacia": {
        "side": "minecraft:block/acacia_log",
        "end": "minecraft:block/acacia_log_top",
        "name": "Acacia Branch",
    },
}

# Element boxes matching StaticBranchBlock's VoxelShapes (in 1/16 block units).
CORE_BOX = (4, 4, 4, 12, 12, 12)
BEAMS = {
    "up": (5, 5, 5, 11, 16, 11),
    "down": (5, 0, 5, 11, 11, 11),
    "north": (5, 5, 0, 11, 11, 11),
    "south": (5, 5, 5, 11, 11, 16),
    "east": (5, 5, 5, 16, 11, 11),
    "west": (0, 5, 5, 11, 11, 11),
}

FACES = ("down", "up", "north", "south", "west", "east")


def element(box, end_faces=()):
    faces = {}
    for face in FACES:
        texture = "#end" if face in end_faces else "#side"
        faces[face] = {"texture": texture}
    return {
        "from": [box[0], box[1], box[2]],
        "to": [box[3], box[4], box[5]],
        "faces": faces,
    }


def block_model(material, shape):
    textures = MATERIALS[material]
    beam = BEAMS.get(shape)
    if beam is None:
        # Centred joint: log rings on the top and bottom faces.
        elements = [element(CORE_BOX, end_faces=("up", "down"))]
    else:
        # Core joint plus the beam; the outward face of the beam shows log rings.
        elements = [element(CORE_BOX, end_faces=("up", "down")),
                    element(beam, end_faces=(shape,))]
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": textures["side"],
            "side": textures["side"],
            "end": textures["end"],
        },
        "elements": elements,
    }


def write_json(path: Path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
    print("wrote", path.relative_to(ROOT).as_posix())


def main():
    for material in MATERIALS:
        block = f"{material}_branch"

        # Blockstates: one variant per shape value.
        variants = {
            f"shape={shape}": {"model": f"{MOD_ID}:block/{block}_{shape}"}
            for shape in SHAPES
        }
        write_json(ASSETS / "blockstates" / f"{block}.json", {"variants": variants})

        # Block models: one per shape.
        for shape in SHAPES:
            write_json(
                ASSETS / "models/block" / f"{block}_{shape}.json",
                block_model(material, shape),
            )

        # Item model reuses the core joint.
        write_json(
            ASSETS / "models/item" / f"{block}.json",
            {"parent": f"{MOD_ID}:block/{block}_core"},
        )

        # Self-dropping loot table.
        write_json(
            DATA / "loot_tables/blocks" / f"{block}.json",
            {
                "type": "minecraft:block",
                "pools": [
                    {
                        "rolls": 1,
                        "bonus_rolls": 0,
                        "entries": [
                            {"type": "minecraft:item", "name": f"{MOD_ID}:{block}"}
                        ],
                        "conditions": [
                            {"condition": "minecraft:survives_explosion"}
                        ],
                    }
                ],
            },
        )


if __name__ == "__main__":
    main()
