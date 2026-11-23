#!/usr/bin/env python3
"""Generate the Phase 3 static mirror for Regions Unexplored and Terralith.

Phase 3 extends the biome-level replacement proven in Phase 2 to the two large
biome mods. The generator reads the reference JARs (never committed), audits
every placed feature that resolves to a tree, and mirrors only the features
that pass every safety rule:

* every reachable configured node is a plain tree with a simple trunk and a
  simple foliage provider (no weighted variants, no air foliage);
* no decorator (beehive, vines, cocoa, alter_ground, ...) and no ``force_dirt``;
* the log/leaves pair has a shipped species profile whose compatibility level
  may replace;
* the placed feature is used in exactly one generation step, that step is
  ``vegetal_decoration``, and every biome that links it belongs to the mod
  namespace (so an absent mod turns the generated biome tag empty);
* every placement modifier is vanilla, so the generated placed feature still
  loads when the biome mod is absent;
* no foreign resource id is resolved by the placed feature at load time: a
  block/fluid predicate list is moved into a generated ``branched`` tag
  (tags ignore entries whose mod is absent), and a foreign ``BlockState``
  predicate cannot be rewritten, so the feature is preserved instead.

Everything else is preserved and recorded, with a reason, in
``catalogs/phase3-preserved.csv``. The result is a closed data graph:

    tools/generate_modded_phase3.py
        -> data/branched/worldgen/{configured_feature,placed_feature}/phase3
        -> data/branched/tags/worldgen/biome/phase3
        -> data/branched/tags/{blocks,fluids}/phase3
        -> data/branched/forge/biome_modifier/modded_*.json
        -> catalogs/phase3-replacements.csv
        -> catalogs/phase3-preserved.csv
        -> docs/phase3-audit.json

Run from the repository root:

    python tools/generate_modded_phase3.py
"""
from __future__ import annotations

import argparse
import csv
import json
import re
import zipfile
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
DATA = RES / "data/branched"
SPECIES_DIR = DATA / "arboreal/species"
CONFIGURED_DIR = DATA / "worldgen/configured_feature"
PLACED_DIR = DATA / "worldgen/placed_feature"
MODIFIER_DIR = DATA / "forge/biome_modifier"
TAG_DIR = DATA / "tags/worldgen/biome"
PLACED_TAG_DIR = DATA / "tags/worldgen/placed_feature"
BLOCK_TAG_DIR = DATA / "tags/blocks"
FLUID_TAG_DIR = DATA / "tags/fluids"
CATALOG_DIR = ROOT / "catalogs"

MOD_ID = "branched"
PHASE = "phase3"
VEGETAL_STEP_INDEX = 9
VEGETAL_STEP_NAME = "vegetal_decoration"

# Namespaces a generated resource may name directly. Anything else must be
# hidden behind a branched tag (tags ignore absent entries) or the
# feature is preserved.
OWN_NAMESPACES = {"minecraft", MOD_ID}
PLACEMENT_TAG_DIRS = {"blocks": BLOCK_TAG_DIR, "fluids": FLUID_TAG_DIR}

SELECTOR_TYPES = {
    "minecraft:random_selector",
    "minecraft:simple_random_selector",
    "minecraft:random_boolean_selector",
}

# Species variants that reuse the morphology of a base profile but write a
# different log/leaf block. They are emitted as normal profile files so the
# generated worldgen graph keeps referring to shipped data.
PROFILE_VARIANTS: list[dict[str, str]] = [
    {
        "species": "regions_unexplored:red_maple",
        "base": "regions_unexplored:maple",
        "logs": "regions_unexplored:maple_log",
        "leaves": "regions_unexplored:red_maple_leaves",
        "wood": "regions_unexplored:maple_wood",
        "sapling": "regions_unexplored:red_maple_sapling",
        "source": "regions_unexplored:red_maple_tree_on_grass",
    },
    {
        "species": "regions_unexplored:orange_maple",
        "base": "regions_unexplored:maple",
        "logs": "regions_unexplored:maple_log",
        "leaves": "regions_unexplored:orange_maple_leaves",
        "wood": "regions_unexplored:maple_wood",
        "sapling": "regions_unexplored:orange_maple_sapling",
        "source": "regions_unexplored:orange_maple_tree_on_grass",
    },
    {
        "species": "regions_unexplored:blue_magnolia",
        "base": "regions_unexplored:magnolia",
        "logs": "regions_unexplored:magnolia_log",
        "leaves": "regions_unexplored:blue_magnolia_leaves",
        "wood": "regions_unexplored:magnolia_wood",
        "sapling": "regions_unexplored:blue_magnolia_sapling",
        "source": "regions_unexplored:blue_magnolia_tree",
    },
    {
        "species": "regions_unexplored:pink_magnolia",
        "base": "regions_unexplored:magnolia",
        "logs": "regions_unexplored:magnolia_log",
        "leaves": "regions_unexplored:pink_magnolia_leaves",
        "wood": "regions_unexplored:magnolia_wood",
        "sapling": "regions_unexplored:pink_magnolia_sapling",
        "source": "regions_unexplored:pink_magnolia_tree",
    },
    {
        "species": "regions_unexplored:white_magnolia",
        "base": "regions_unexplored:magnolia",
        "logs": "regions_unexplored:magnolia_log",
        "leaves": "regions_unexplored:white_magnolia_leaves",
        "wood": "regions_unexplored:magnolia_wood",
        "sapling": "regions_unexplored:white_magnolia_sapling",
        "source": "regions_unexplored:white_magnolia_tree",
    },
    {
        "species": "regions_unexplored:enchanted_birch",
        "base": "regions_unexplored:silver_birch",
        "logs": "regions_unexplored:silver_birch_log",
        "leaves": "regions_unexplored:enchanted_birch_leaves",
        "wood": "regions_unexplored:silver_birch_wood",
        "sapling": "regions_unexplored:enchanted_birch_sapling",
        "source": "regions_unexplored:enchanted_birch",
    },
    {
        "species": "regions_unexplored:golden_larch",
        "base": "regions_unexplored:larch",
        "logs": "regions_unexplored:larch_log",
        "leaves": "regions_unexplored:golden_larch_leaves",
        "wood": "regions_unexplored:larch_wood",
        "sapling": "regions_unexplored:golden_larch_sapling",
        "source": "regions_unexplored:golden_larch_tree_dense",
    },
    {
        "species": "regions_unexplored:dead_pine",
        "base": "regions_unexplored:pine",
        "logs": "regions_unexplored:pine_log",
        "leaves": "regions_unexplored:dead_pine_leaves",
        "wood": "regions_unexplored:pine_wood",
        "sapling": "regions_unexplored:dead_pine_sapling",
        "source": "regions_unexplored:dead_scotts_pine_tree",
    },
    {
        "species": "regions_unexplored:ashen_pine",
        "base": "regions_unexplored:pine",
        "logs": "regions_unexplored:ashen_log",
        "leaves": "regions_unexplored:pine_leaves",
        "wood": "regions_unexplored:ashen_wood",
        "sapling": "regions_unexplored:ashen_sapling",
        "source": "regions_unexplored:ashen_pine_tree",
    },
]

# A log/leaves pair can match more than one profile (spruce and pine share
# vanilla blocks). These preferences keep the choice stable and explicit.
PAIR_PREFERENCE = {
    ("minecraft:oak_log", "minecraft:oak_leaves"): "minecraft:oak",
    ("minecraft:spruce_log", "minecraft:spruce_leaves"): "minecraft:spruce",
}

# Tokens that describe a silhouette or a feature class this project does not
# mirror. They are checked before the graph rules so the audit reason is the
# shape/class, not a downstream symptom.
PRESERVE_WORDS: list[tuple[str, str]] = [
    ("fallen", "fallen_log_feature"),
    ("mushroom", "fungus_feature"),
    ("bioshroom", "fungus_feature"),
    ("cactus", "cactus_feature"),
    ("bamboo", "bamboo_feature"),
    ("joshua", "joshua_shrub_feature"),
    ("socotra", "socotra_landmark"),
    ("giant", "landmark_scale_feature"),
    ("mega", "landmark_scale_feature"),
    ("ultra", "landmark_scale_feature"),
    ("shrub", "shrub_silhouette"),
    ("bush", "shrub_silhouette"),
    ("small", "shrub_silhouette"),
    ("tiny", "shrub_silhouette"),
]


def path_of(resource_id: str) -> str:
    return resource_id.split(":", 1)[1]


def phase_path(resource_id: str) -> str:
    """Namespace-qualified path used for generated ids and files."""
    namespace, path = resource_id.split(":", 1)
    return f"{namespace}/{path}"


def static_id(resource_id: str) -> str:
    return f"{MOD_ID}:{PHASE}/{phase_path(resource_id)}"


def slug_of(resource_id: str) -> str:
    return f"{resource_id.split(':', 1)[0]}_{path_of(resource_id).replace('/', '_')}"


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def foreign_references(value: Any) -> list[str]:
    """Resource ids in ``value`` that a datapack load would resolve in another mod."""
    found: list[str] = []
    if isinstance(value, str):
        raw = value[1:] if value.startswith("#") else value
        if ":" in raw and raw.split(":", 1)[0] not in OWN_NAMESPACES:
            found.append(raw)
    elif isinstance(value, list):
        for item in value:
            found.extend(foreign_references(item))
    elif isinstance(value, dict):
        for item in value.values():
            found.extend(foreign_references(item))
    return found


def placement_foreign(
    node: Any,
    state_refs: list[str],
    other_refs: list[str],
    *,
    inside_state: bool = False,
    inside_block_set: bool = False,
) -> None:
    """Sort the foreign ids of a placement payload by how they can be handled."""
    if isinstance(node, dict):
        for name, value in node.items():
            placement_foreign(
                value,
                state_refs,
                other_refs,
                inside_state=inside_state or name == "state",
                inside_block_set=inside_block_set or name in ("blocks", "fluids"),
            )
    elif isinstance(node, list):
        for item in node:
            placement_foreign(
                item,
                state_refs,
                other_refs,
                inside_state=inside_state,
                inside_block_set=inside_block_set,
            )
    elif isinstance(node, str):
        raw = node[1:] if node.startswith("#") else node
        if ":" not in raw or raw.split(":", 1)[0] in OWN_NAMESPACES:
            return
        if inside_block_set:
            return
        if inside_state:
            state_refs.append(raw)
        else:
            other_refs.append(raw)


def state_name(provider: dict | None) -> str | None:
    if not provider:
        return None
    if provider.get("type") == "minecraft:weighted_state_provider":
        return None
    return (provider.get("state") or {}).get("Name")


class Profiles:
    """Profile index by log/leaves pair, plus compatibility lookup."""

    def __init__(self) -> None:
        self.by_pair: dict[tuple[str, str], str] = {}
        self.by_species: dict[str, dict] = {}
        self.replaceable: set[str] = set()

    def load(self) -> None:
        for path in sorted(SPECIES_DIR.rglob("*.json")):
            data = load_json(path)
            species = data["species"]
            self.by_species[species] = data
            if data.get("compatibility") in ("STATIC_NORMAL", "STATIC_FRUIT"):
                self.replaceable.add(species)
            self.by_pair.setdefault((data["logs"], data["leaves"]), species)
        for pair, species in PAIR_PREFERENCE.items():
            self.by_pair[pair] = species

    def emit_variants(self) -> list[Path]:
        written: list[Path] = []
        for variant in PROFILE_VARIANTS:
            species = variant["species"]
            if species in self.by_species:
                continue
            base = self.by_species.get(variant["base"])
            if base is None:
                raise SystemExit(f"missing base profile {variant['base']} for {species}")
            payload = json.loads(json.dumps(base))
            payload["species"] = species
            payload["logs"] = variant["logs"]
            payload["leaves"] = variant["leaves"]
            payload["wood"] = variant["wood"]
            payload["sapling"] = variant["sapling"]
            payload.setdefault("source", {})["feature"] = variant["source"]
            namespace, name = species.split(":", 1)
            path = SPECIES_DIR / namespace / f"{name}.json"
            write_json(path, payload)
            written.append(path)
            self.by_species[species] = payload
            self.by_pair[(payload["logs"], payload["leaves"])] = species
            self.replaceable.add(species)
        return written


class JarAudit:
    """Parsed worldgen resources of one reference JAR."""

    def __init__(self, mod: str, jar_path: Path) -> None:
        self.mod = mod
        self.jar_path = jar_path
        self.configured: dict[str, dict] = {}
        self.placed: dict[str, dict] = {}
        self.biomes: dict[str, dict] = {}
        self._read()

    def _read(self) -> None:
        with zipfile.ZipFile(self.jar_path) as jar:
            for name in jar.namelist():
                if not name.startswith("data/") or not name.endswith(".json"):
                    continue
                namespace = name.split("/")[1]
                for kind, target in (
                    ("configured_feature", self.configured),
                    ("placed_feature", self.placed),
                    ("biome", self.biomes),
                ):
                    marker = f"/worldgen/{kind}/"
                    if marker in name:
                        key = f"{namespace}:" + name.split(marker, 1)[1][:-5]
                        target[key] = json.loads(jar.read(name))
                        break

    def links(self) -> tuple[dict[str, set[str]], dict[str, set[int]]]:
        biomes: dict[str, set[str]] = defaultdict(set)
        steps: dict[str, set[int]] = defaultdict(set)
        for biome_id, payload in self.biomes.items():
            for index, step in enumerate(payload.get("features") or []):
                for placed_id in step:
                    biomes[placed_id].add(biome_id)
                    steps[placed_id].add(index)
        return biomes, steps

    def resolve(self, ref: Any, seen: set[str] | None = None) -> list[tuple[str, dict]]:
        """Expand selectors into the ordered list of (type, config) leaves."""
        if seen is None:
            seen = set()
        if isinstance(ref, dict):
            payload = ref
        else:
            if ref in seen:
                return []
            seen.add(ref)
            payload = self.configured.get(ref)
            if payload is None:
                return [("missing", {})]
        kind = payload.get("type") or ""
        if kind in SELECTOR_TYPES:
            config = payload["config"]
            if kind == "minecraft:random_selector":
                children = [config["default"]] + [e["feature"] for e in config["features"]]
            elif kind == "minecraft:simple_random_selector":
                children = config["features"]
            else:
                children = [config["feature_true"], config["feature_false"]]
            nodes: list[tuple[str, dict]] = []
            for child in children:
                nodes.extend(self.resolve(child, seen))
            return nodes
        return [(kind, payload.get("config") or {})]


class Mirror:
    """Builds the static worldgen graph for one accepted placed feature."""

    def __init__(self, audit: JarAudit, profiles: Profiles) -> None:
        self.audit = audit
        self.profiles = profiles
        self.configured: dict[str, dict] = {}
        self.placed: dict[str, dict] = {}
        self.placement_tags: dict[tuple[str, str], dict] = {}

    def tree_species(self, config: dict) -> str:
        log = state_name(config.get("trunk_provider"))
        leaves = state_name(config.get("foliage_provider"))
        if not log or not leaves:
            raise SystemExit(f"unresolved trunk/foliage in {self.audit.mod}: {log} {leaves}")
        species = self.profiles.by_pair.get((log, leaves))
        if species is None:
            raise SystemExit(f"missing profile for {log} + {leaves}")
        return species

    def rewrite_block_sets(self, target: str, placement: list) -> list:
        """Move every foreign block/fluid id of a predicate into a generated tag.

        A generated placed feature is parsed even when the mod that owns the
        blocks is absent, and a list element is resolved at load time. A tag is
        not: unknown entries are dropped, so the file always parses. Keeping the
        list intact under an own tag also preserves the original selection when
        the mod is present.
        """
        base = target.split(":", 1)[1]
        counter = 0

        def visit(node: Any) -> Any:
            nonlocal counter
            if isinstance(node, dict):
                rewritten: dict[str, Any] = {}
                for name, value in node.items():
                    if name in PLACEMENT_TAG_DIRS and foreign_references(value):
                        counter += 1
                        suffix = "" if counter == 1 else f"_{counter}"
                        tag_id = f"{MOD_ID}:{base}_{name}{suffix}"
                        entries = value if isinstance(value, list) else [value]
                        self.placement_tags[(name, tag_id)] = {
                            "replace": False,
                            "values": [str(entry) for entry in entries],
                        }
                        rewritten[name] = f"#{tag_id}"
                    else:
                        rewritten[name] = visit(value)
                return rewritten
            if isinstance(node, list):
                return [visit(item) for item in node]
            return node

        return visit(placement)

    def mirror_placed(self, placed_ref: str) -> str:
        target = static_id(placed_ref)
        if target in self.placed:
            return target
        payload = self.audit.placed[placed_ref]
        configured = self.mirror_feature(payload["feature"], target)
        self.placed[target] = {
            "feature": configured,
            "placement": self.rewrite_block_sets(target, payload.get("placement", [])),
        }
        return target

    def mirror_feature(self, ref: Any, anchor: str) -> str:
        if isinstance(ref, dict):
            target = f"{anchor}/inline"
            payload = ref
        else:
            target = static_id(ref)
            payload = self.audit.configured[ref]
        if target in self.configured:
            return target

        kind = payload.get("type") or ""
        if kind == "missing":
            raise SystemExit(f"missing configured feature in {self.audit.mod}: {ref}")
        if kind in SELECTOR_TYPES:
            config = payload["config"]
            if kind == "minecraft:random_selector":
                default = self.mirror_placed(config["default"])
                entries = [
                    {
                        "chance": entry["chance"],
                        "feature": self.mirror_placed(entry["feature"]),
                    }
                    for entry in config["features"]
                ]
                self.configured[target] = {
                    "type": kind,
                    "config": {"default": default, "features": entries},
                }
            elif kind == "minecraft:simple_random_selector":
                entries = [self.mirror_placed(entry) for entry in config["features"]]
                self.configured[target] = {
                    "type": kind,
                    "config": {"features": entries},
                }
            else:
                self.configured[target] = {
                    "type": kind,
                    "config": {
                        "feature_true": self.mirror_placed(config["feature_true"]),
                        "feature_false": self.mirror_placed(config["feature_false"]),
                    },
                }
            return target

        species = self.tree_species(payload["config"])
        self.configured[target] = {
            "type": f"{MOD_ID}:static_tree",
            "config": {"species": species, "attempts": 3},
        }
        return target


def tree_like(mod: str, kind: str) -> bool:
    if kind == "minecraft:tree":
        return True
    if mod == "regions_unexplored":
        return kind.startswith("regions_unexplored:") and (
            "tree" in kind or "willow" in kind
        )
    return False


def _stem(word: str) -> str:
    if word.endswith("us") or word.endswith("ss"):
        return word
    if word.endswith("es"):
        return word[:-2]
    if word.endswith("s"):
        return word[:-1]
    return word


def token_reason(placed_id: str) -> str | None:
    words = {_stem(w) for w in re.split(r"[^a-z0-9]+", path_of(placed_id).lower()) if w}
    for word, reason in PRESERVE_WORDS:
        if word in words:
            return reason
    return None


def node_reasons(mod: str, kind: str, config: dict, profiles: Profiles) -> list[str]:
    reasons: list[str] = []
    if not tree_like(mod, kind):
        return [f"unsupported_type:{kind}"]
    decorators = config.get("decorators") or []
    if decorators:
        kinds = ",".join(sorted({d.get("type", "?") for d in decorators}))
        reasons.append(f"decorators:{kinds}")
    if config.get("force_dirt"):
        reasons.append("force_dirt")
    log = state_name(config.get("trunk_provider"))
    leaves = state_name(config.get("foliage_provider"))
    if not log:
        reasons.append("unresolved_trunk")
    if not leaves or leaves == "minecraft:air":
        reasons.append("unresolved_foliage")
    if log and leaves and log != "minecraft:air" and leaves != "minecraft:air":
        species = profiles.by_pair.get((log, leaves))
        if species is None:
            reasons.append(f"missing_profile:{log}+{leaves}")
        elif species not in profiles.replaceable:
            reasons.append(f"profile_not_replaceable:{species}")
    return reasons


def placement_reasons(mod: str, payload: dict) -> list[str]:
    reasons: list[str] = []
    for modifier in payload.get("placement", []):
        kind = modifier.get("type", "")
        if not kind.startswith("minecraft:"):
            reasons.append(f"modded_placement:{kind}")
    state_refs: list[str] = []
    other_refs: list[str] = []
    placement_foreign(payload.get("placement", []), state_refs, other_refs)
    # A BlockState predicate cannot be turned into a tag, so a foreign state
    # keeps the native feature.
    for reference in sorted(set(state_refs)):
        reasons.append(f"placement_block_state:{reference}")
    for reference in sorted(set(other_refs)):
        reasons.append(f"foreign_placement_reference:{reference}")
    return reasons


def biome_reasons(mod: str, biomes: set[str]) -> list[str]:
    reasons = []
    for biome in sorted(biomes):
        if not biome.startswith(f"{mod}:"):
            reasons.append(f"foreign_biome:{biome}")
    return reasons


def classify(
    mod: str,
    placed_id: str,
    audit: JarAudit,
    profiles: Profiles,
    links: dict[str, set[str]],
    steps: dict[str, set[int]],
) -> tuple[str, Any]:
    """Return (verdict, payload) where verdict is replaced / preserved / not_tree."""
    token = token_reason(placed_id)
    payload = audit.placed[placed_id]
    nodes = audit.resolve(payload["feature"])

    if token:
        return "preserved", {"reasons": [token]}

    tree_nodes = [node for node in nodes if tree_like(mod, node[0])]
    if not tree_nodes:
        return "not_tree", {"reasons": []}

    reasons: list[str] = []
    for kind, config in nodes:
        reasons.extend(node_reasons(mod, kind, config, profiles))
    reasons.extend(placement_reasons(mod, payload))
    if steps.get(placed_id) != {VEGETAL_STEP_INDEX}:
        reasons.append("non_vegetal_step:" + ",".join(str(s) for s in sorted(steps.get(placed_id, set()))))
    reasons.extend(biome_reasons(mod, links.get(placed_id, set())))

    if reasons:
        return "preserved", {"reasons": sorted(set(reasons))}

    species = []
    logs: set[str] = set()
    leaves: set[str] = set()
    kinds: set[str] = set()
    for kind, config in nodes:
        kinds.add(kind)
        species.append(profiles.by_pair[
            (state_name(config["trunk_provider"]), state_name(config["foliage_provider"]))
        ])
        logs.add(state_name(config["trunk_provider"]))
        leaves.add(state_name(config["foliage_provider"]))
    return "replaced", {
        "species": sorted(set(species)),
        "nodes": sorted(kinds),
        "logs": sorted(logs),
        "leaves": sorted(leaves),
        "biomes": sorted(links[placed_id]),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="audit only, write nothing")
    args = parser.parse_args()

    config = {
        "regions_unexplored": ROOT / "reference-mods/originals/RegionsUnexploredForge-0.5.6+1.20.1.jar",
        "terralith": ROOT / "reference-mods/originals/Terralith_1.20.x_v2.5.4.jar",
    }

    profiles = Profiles()
    profiles.load()
    variants = [] if args.dry_run else profiles.emit_variants()

    replaced_rows: list[dict[str, str]] = []
    preserved_rows: list[dict[str, str]] = []
    audit_json: dict[str, Any] = {"phase": 3, "mods": {}, "variants": [v["species"] for v in PROFILE_VARIANTS]}

    if not args.dry_run:
        for directory in (
            CONFIGURED_DIR / PHASE,
            PLACED_DIR / PHASE,
            TAG_DIR / PHASE,
            PLACED_TAG_DIR / PHASE,
            BLOCK_TAG_DIR / PHASE,
            FLUID_TAG_DIR / PHASE,
        ):
            if directory.exists():
                for path in sorted(directory.rglob("*.json"), reverse=True):
                    path.unlink()
                for path in sorted(directory.rglob("*"), reverse=True):
                    if path.is_dir() and not any(path.iterdir()):
                        path.rmdir()
        for path in sorted(MODIFIER_DIR.glob("modded_*.json")):
            path.unlink()

    for mod, jar_path in config.items():
        if not jar_path.is_file():
            raise SystemExit(f"missing reference jar {jar_path}")
        audit = JarAudit(mod, jar_path)
        links, steps = audit.links()
        mirror = Mirror(audit, profiles)
        replaced: list[dict[str, Any]] = []
        preserved: list[dict[str, str]] = []
        not_tree = 0
        for placed_id in sorted(audit.placed):
            if not links.get(placed_id):
                continue
            verdict, detail = classify(mod, placed_id, audit, profiles, links, steps)
            if verdict == "not_tree":
                not_tree += 1
                continue
            if verdict == "preserved":
                preserved.append({"placed": placed_id, "reasons": detail["reasons"]})
                preserved_rows.append(
                    {
                        "mod": mod,
                        "placed_feature": placed_id,
                        "reasons": ";".join(detail["reasons"]),
                    }
                )
                continue

            target = mirror.mirror_placed(placed_id)
            biomes = detail["biomes"]
            tag_values = sorted(biomes)
            tag_id = f"{MOD_ID}:{PHASE}/{phase_path(placed_id)}"
            if not args.dry_run:
                write_json(
                    TAG_DIR / PHASE / (phase_path(placed_id) + ".json"),
                    {"replace": False, "values": tag_values},
                )
                # The original id is referenced through a tag, never as an
                # inline list: a HolderSet built from a list binds every entry
                # while the datapack loads, so an absent mod would abort the
                # load. A tag drops unknown entries instead.
                write_json(
                    PLACED_TAG_DIR / PHASE / (phase_path(placed_id) + ".json"),
                    {"replace": False, "values": [placed_id]},
                )
                write_json(
                    MODIFIER_DIR / f"modded_{slug_of(placed_id)}.json",
                    {
                        "type": f"{MOD_ID}:replace_trees",
                        # A HolderSet tag reference is a bare string, not a list.
                        "biomes": f"#{tag_id}",
                        "add": target,
                        "replace": f"#{tag_id}",
                        "species": detail["species"],
                        "step": VEGETAL_STEP_NAME,
                    },
                )
            replaced.append(
                {
                    "placed": placed_id,
                    "static": target,
                    "species": detail["species"],
                    "nodes": detail["nodes"],
                    "logs": detail["logs"],
                    "leaves": detail["leaves"],
                    "biomes": tag_values,
                    "step": VEGETAL_STEP_NAME,
                }
            )
            replaced_rows.append(
                {
                    "mod": mod,
                    "placed_feature": placed_id,
                    "static_placed": target,
                    "species": ";".join(detail["species"]),
                    "tree_types": ";".join(detail["nodes"]),
                    "logs": ";".join(detail["logs"]),
                    "leaves": ";".join(detail["leaves"]),
                    "biomes": ";".join(tag_values),
                    "biome_count": str(len(tag_values)),
                }
            )

        if not args.dry_run:
            for configured_id, payload in sorted(mirror.configured.items()):
                write_json(CONFIGURED_DIR / (path_of(configured_id) + ".json"), payload)
            for placed_id, payload in sorted(mirror.placed.items()):
                leftover = sorted(set(foreign_references(payload)))
                if leftover:
                    raise SystemExit(f"foreign reference survived in {placed_id}: {leftover}")
                write_json(PLACED_DIR / (path_of(placed_id) + ".json"), payload)
            for (registry, tag_id), payload in sorted(mirror.placement_tags.items()):
                write_json(PLACEMENT_TAG_DIRS[registry] / (path_of(tag_id) + ".json"), payload)

        reason_counts = Counter(
            reason.split(":", 1)[0] for entry in preserved for reason in entry["reasons"]
        )
        audit_json["mods"][mod] = {
            "replaced": replaced,
            "preserved": preserved,
            "preserved_reason_counts": dict(sorted(reason_counts.items())),
            "referenced_placed_features": len(links),
            "audited_placed_features": len(replaced) + len(preserved) + not_tree,
            "foreign_referenced_features": len(links) - (len(replaced) + len(preserved) + not_tree),
            "not_tree_features": not_tree,
        }
        print(
            f"{mod}: replaced={len(replaced)} preserved={len(preserved)} "
            f"not_tree={not_tree} configured={len(mirror.configured)} placed={len(mirror.placed)}"
        )

    if args.dry_run:
        for row in replaced_rows:
            print("REPLACE", row["placed_feature"], "->", row["species"], f"({row['biome_count']} biomes)")
        return

    CATALOG_DIR.mkdir(parents=True, exist_ok=True)
    with (CATALOG_DIR / "phase3-replacements.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(replaced_rows[0].keys()))
        writer.writeheader()
        writer.writerows(replaced_rows)
    with (CATALOG_DIR / "phase3-preserved.csv").open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(preserved_rows[0].keys()))
        writer.writeheader()
        writer.writerows(preserved_rows)
    write_json(ROOT / "docs/phase3-audit.json", audit_json)

    print(f"profile variants written: {len(variants)}")
    print(f"replacements: {len(replaced_rows)} preserved: {len(preserved_rows)}")


if __name__ == "__main__":
    main()
