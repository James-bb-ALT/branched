#!/usr/bin/env python3
"""Validate all Branched species profiles (Phase 5/6 tooling)."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SPECIES_DIR = ROOT / "src/main/resources/data/branched/arboreal/species"

VALID_ARCHETYPES = {
    "BROADLEAF_IRREGULAR",
    "BROADLEAF_ROUND",
    "CONIFER",
    "COLUMNAR",
    "BIRCH",
    "ACACIA",
    "MANGROVE",
    "PALM",
    "BAOBAB",
    "CYPRESS",
    "BONSAI",
    "FANTASTIC",
}
VALID_COMPAT = {"STATIC_NORMAL", "STATIC_FRUIT", "SPECIAL_PRESERVE", "UNRESOLVED"}


def check_range(obj: dict, key: str, lo: float, hi: float, errors: list[str], label: str) -> None:
    val = obj.get(key)
    if not isinstance(val, list) or len(val) != 2:
        errors.append(f"{label}.{key}: expected [min, max] list")
        return
    a, b = val[0], val[1]
    if not isinstance(a, (int, float)) or not isinstance(b, (int, float)):
        errors.append(f"{label}.{key}: non-numeric bounds")
        return
    if a > b:
        errors.append(f"{label}.{key}: inverted range [{a}, {b}]")
    if a < lo or b > hi:
        errors.append(f"{label}.{key}: [{a}, {b}] outside [{lo}, {hi}]")


def main() -> int:
    files = sorted(SPECIES_DIR.rglob("*.json"))
    if not files:
        print(f"No species profiles found in {SPECIES_DIR}", file=sys.stderr)
        return 1

    errors: list[str] = []
    seen_ids: set[str] = set()
    replaceable = 0
    preserved = 0

    for path in files:
        rel = path.relative_to(ROOT).as_posix()
        data = json.loads(path.read_text(encoding="utf-8"))
        if data.get("schema") != 1:
            errors.append(f"{rel}: expected schema=1")
        species = data.get("species", "")
        if ":" not in species:
            errors.append(f"{rel}: invalid species id '{species}'")
        elif species in seen_ids:
            errors.append(f"{rel}: duplicate species id '{species}'")
        seen_ids.add(species)

        archetype = str(data.get("archetype", "")).upper()
        if archetype not in VALID_ARCHETYPES:
            errors.append(f"{rel}: invalid archetype '{archetype}'")

        compat = str(data.get("compatibility", "")).upper()
        if compat not in VALID_COMPAT:
            errors.append(f"{rel}: invalid compatibility '{compat}'")
        elif compat in {"STATIC_NORMAL", "STATIC_FRUIT"}:
            replaceable += 1
        else:
            preserved += 1

        if data.get("logs") == data.get("leaves"):
            errors.append(f"{rel}: logs and leaves must differ")

        gen = data.get("generation", {})
        if gen.get("mode") != "semiprocedural_once":
            errors.append(f"{rel}: generation.mode must be semiprocedural_once")
        if gen.get("version", 0) < 2:
            errors.append(f"{rel}: generation.version must be >= 2")

        trunk = data.get("trunk", {})
        check_range(trunk, "height", 2.0, 64.0, errors, f"{rel}:trunk")
        check_range(trunk, "radius", 0.2, 6.0, errors, f"{rel}:trunk")
        check_range(trunk, "bend", 0.0, 1.0, errors, f"{rel}:trunk")

        branches = data.get("branches", {})
        check_range(branches, "primaryCount", 0, 64, errors, f"{rel}:branches")
        check_range(branches, "depth", 1, 6, errors, f"{rel}:branches")
        check_range(branches, "angleDegrees", 0.0, 110.0, errors, f"{rel}:branches")
        check_range(branches, "lengthRatio", 0.05, 1.0, errors, f"{rel}:branches")
        check_range(branches, "asymmetry", 0.0, 1.0, errors, f"{rel}:branches")

        crown = data.get("crown", {})
        check_range(crown, "widthRatio", 0.2, 3.0, errors, f"{rel}:crown")
        check_range(crown, "density", 0.0, 1.0, errors, f"{rel}:crown")
        check_range(crown, "clusterCount", 1, 96, errors, f"{rel}:crown")

        budget = data.get("budget", {})
        if not (8 <= budget.get("maxSegments", 0) <= 4096):
            errors.append(f"{rel}: maxSegments out of bounds")
        if not (8 <= budget.get("maxLeaves", 0) <= 8192):
            errors.append(f"{rel}: maxLeaves out of bounds")
        if not (64 <= budget.get("maxQuads", 0) <= 200000):
            errors.append(f"{rel}: maxQuads out of bounds")

    if errors:
        for err in errors:
            print("ERROR:", err, file=sys.stderr)
        return 1

    print(
        f"Validated {len(files)} profiles ({replaceable} replaceable, {preserved} preserved): "
        "0 errors, 0 warnings"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
