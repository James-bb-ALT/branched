#!/usr/bin/env python3
"""Audit the Phase 4 mods and decide, feature by feature, what may be replaced.

Phase 4 covers the remaining integrations named by the plan: Aether, Fish of
Thieves, Quark and Twilight Forest, plus the Dynamic Trees add-ons that have no
reference JAR in this repository. Unlike Phase 3, these mods do not offer plain
overworld trees:

* their tree features attach fruit, roots, vines or other decoration;
* Twilight Forest's magic trees use custom feature types;
* every candidate adds at least one non-vanilla placement modifier, which a
  generated placed feature cannot load when the mod is absent;
* Aether 1.20.1 1.5.2 only ships a NeoForge build, so it cannot run on the
  Forge target of this project.

The generator therefore writes a full audit and a preservation decision. It only
writes replacements if every safety rule passes, which keeps the tool honest:
rerunning it after a mod changes can only *add* replacements that are provably
safe, never silently drop one.

Outputs:

    catalogs/phase4-preserved.csv
    catalogs/phase4-replacements.csv
    docs/phase4-audit.json
    docs/phase4-tree-audit.csv

Run from the repository root:

    python tools/generate_modded_phase4.py
    python tools/generate_modded_phase4.py --dry-run
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

MOD_ID = "branched"
PHASE = "phase4"
VEGETAL_STEP_INDEX = 9
VEGETAL_STEP_NAME = "vegetal_decoration"

SELECTOR_TYPES = {
    "minecraft:random_selector",
    "minecraft:simple_random_selector",
    "minecraft:random_boolean_selector",
}

# Reference JARs are git-ignored; the Forge build is preferred when both exist.
MODS: dict[str, dict[str, Any]] = {
    "aether": {
        "jars": ["aether-1.20.1-1.5.2-forge.jar", "aether-1.20.1-1.5.2-neoforge.jar"],
        "namespace": "aether",
        # The audited 1.20.1 build is NeoForge-only, so no Forge server can load
        # it; the decision is preservation regardless of the graph rules.
        "forge_runtime": False,
    },
    "fishofthieves": {
        "jars": ["fish_of_thieves-mc1.20.1-v3.1.7-forge.jar"],
        "namespace": "fishofthieves",
        "forge_runtime": True,
    },
    "quark": {
        "jars": ["Quark-4.0-462.jar"],
        "namespace": "quark",
        "forge_runtime": True,
    },
    "twilightforest": {
        "jars": ["twilightforest-1.20.1-4.3.2508-universal.jar"],
        "namespace": "twilightforest",
        "forge_runtime": True,
    },
}

# Plan §8 Phase 4 names three Dynamic Trees add-ons. The repository holds no
# reference JAR for them, and Dynamic Trees is never a runtime dependency, so
# they cannot be audited or wired here.
UNAVAILABLE_MODS: dict[str, str] = {
    "dynamictrees_baobab": "no_reference_jar",
    "dynamictrees_addon_lib": "no_reference_jar",
    "dynamictrees_plus": "no_reference_jar",
}

PRESERVE_WORDS: list[tuple[str, str]] = [
    ("fallen", "fallen_log_feature"),
    ("hollow", "hollow_tree_feature"),
    ("stump", "hollow_tree_feature"),
    ("mushroom", "fungus_feature"),
    ("bioshroom", "fungus_feature"),
    ("shroom", "fungus_feature"),
    ("cactus", "cactus_feature"),
    ("bamboo", "bamboo_feature"),
    ("joshua", "joshua_shrub_feature"),
    ("giant", "landmark_scale_feature"),
    ("mega", "landmark_scale_feature"),
    ("large", "landmark_scale_feature"),
    ("canopy", "custom_canopy_feature"),
    ("shrub", "shrub_silhouette"),
    ("bush", "shrub_silhouette"),
    ("small", "shrub_silhouette"),
    ("tiny", "shrub_silhouette"),
]

# Feature types known to build their own trunk/foliage layout, so the generic
# trunk/foliage predicates do not describe them.
SPECIAL_TREE_TYPES = {
    "twilightforest:dark_canopy_tree",
    "twilightforest:canopy_oak",
    "twilightforest:hollow_tree",
    "twilightforest:tree_of_time",
    "twilightforest:tree_of_transformation",
    "twilightforest:tree_of_mining",
    "twilightforest:tree_of_sorting",
}


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def write_csv(path: Path, fieldnames: list[str], rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in fieldnames})


def state_name(provider: dict | None) -> str | None:
    if not provider:
        return None
    if provider.get("type") == "minecraft:weighted_state_provider":
        return None
    return (provider.get("state") or {}).get("Name")


class Profiles:
    """Shipped species profiles, indexed by log/leaves pair."""

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
        # Spruce and pine share vanilla blocks; keep the choice explicit.
        self.by_pair[("minecraft:oak_log", "minecraft:oak_leaves")] = "minecraft:oak"
        self.by_pair[("minecraft:spruce_log", "minecraft:spruce_leaves")] = "minecraft:spruce"


class JarAudit:
    """Parsed worldgen resources of one reference JAR."""

    def __init__(self, mod: str, jar_path: Path) -> None:
        self.mod = mod
        self.jar_path = jar_path
        self.configured: dict[str, dict] = {}
        self.placed: dict[str, dict] = {}
        self.biomes: dict[str, dict] = {}
        self.biome_tags: set[str] = set()
        self.biome_modifiers: dict[str, dict] = {}
        self._read()

    def _read(self) -> None:
        with zipfile.ZipFile(self.jar_path) as jar:
            for name in jar.namelist():
                if not name.startswith("data/") or not name.endswith(".json"):
                    continue
                parts = name.split("/")
                namespace = parts[1]
                if "/tags/worldgen/biome/" in name:
                    marker = "/tags/worldgen/biome/"
                    self.biome_tags.add(f"#{namespace}:" + name.split(marker, 1)[1][:-5])
                    continue
                if "/forge/biome_modifier/" in name:
                    marker = "/forge/biome_modifier/"
                    key = f"{namespace}:" + name.split(marker, 1)[1][:-5]
                    self.biome_modifiers[key] = json.loads(jar.read(name))
                    continue
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
        """Biome/step links declared by biome JSON and by add_features modifiers.

        A placed feature can be reached through another placed feature (a
        selector), so the link is propagated to every descendant. That mirror the
        runtime: the biome runs the root, and the selector decides the leaf.
        """
        biomes: dict[str, set[str]] = defaultdict(set)
        steps: dict[str, set[int]] = defaultdict(set)
        roots: list[tuple[str, str, int]] = []
        for biome_id, payload in self.biomes.items():
            for index, step in enumerate(payload.get("features") or []):
                for placed_id in step:
                    roots.append((placed_id, biome_id, index))
        for payload in self.biome_modifiers.values():
            if payload.get("type") not in ("forge:add_features", "forge:remove_features"):
                continue
            targets = payload.get("biomes")
            if isinstance(targets, str):
                targets = [targets]
            features = payload.get("features")
            if isinstance(features, str):
                features = [features]
            step = step_index(payload["step"]) if payload.get("step") else 0
            for placed_id in features or []:
                for biome in targets or []:
                    roots.append((placed_id, biome, step))

        for placed_id, biome, index in roots:
            for descendant in self.descendants(placed_id):
                biomes[descendant].add(biome)
                steps[descendant].add(index)
        return biomes, steps

    def descendants(self, placed_id: str) -> set[str]:
        """Every placed feature id reachable from this one, including itself."""
        found: set[str] = set()
        queue = [placed_id]
        while queue:
            current = queue.pop()
            if current in found:
                continue
            payload = self.placed.get(current)
            if payload is None:
                continue
            found.add(current)
            for child in self.selector_children(payload.get("feature")):
                queue.append(child)
        return found

    def selector_children(self, ref: Any) -> list[str]:
        """Placed ids referenced by a selector configured feature, if any."""
        if not isinstance(ref, str):
            return []
        payload = self.configured.get(ref)
        if payload is None:
            return []
        kind = payload.get("type") or ""
        config = payload.get("config") or {}
        if kind == "minecraft:random_selector":
            children = [config.get("default")] + [
                entry.get("feature") for entry in config.get("features") or []
            ]
        elif kind == "minecraft:simple_random_selector":
            children = config.get("features") or []
            if isinstance(children, str):
                children = [children]
        elif kind == "minecraft:random_boolean_selector":
            children = [config.get("feature_true"), config.get("feature_false")]
        else:
            return []
        return [child for child in children if isinstance(child, str)]

    def resolve_placed(self, ref: Any, seen: set[str] | None = None) -> list[tuple[str, dict, str]]:
        """Expand a placed feature (id or inline object) into configured nodes."""
        if seen is None:
            seen = set()
        if isinstance(ref, dict):
            payload = ref
            source = "<inline>"
        else:
            source = ref
            key = f"p:{ref}"
            if key in seen:
                return []
            seen.add(key)
            payload = self.placed.get(ref)
            if payload is None:
                return [("missing_placed", {"id": ref}, source)]
        return self.resolve_feature(payload.get("feature"), source, seen)

    def resolve_feature(self, ref: Any, source: str, seen: set[str]) -> list[tuple[str, dict, str]]:
        if isinstance(ref, dict):
            payload = ref
        else:
            key = f"c:{ref}"
            if key in seen:
                return []
            seen.add(key)
            payload = self.configured.get(ref)
            if payload is None:
                return [("missing_configured", {"id": ref}, source)]
        kind = payload.get("type") or ""
        if kind in SELECTOR_TYPES:
            config = payload.get("config") or {}
            if kind == "minecraft:random_selector":
                children = [config.get("default")] + [
                    entry.get("feature") for entry in config.get("features") or []
                ]
            elif kind == "minecraft:simple_random_selector":
                children = config.get("features") or []
                if isinstance(children, str):
                    children = [children]
            else:
                children = [config.get("feature_true"), config.get("feature_false")]
            nodes: list[tuple[str, dict, str]] = []
            for child in children:
                nodes.extend(self.resolve_placed(child, seen))
            return nodes
        return [(kind, payload.get("config") or {}, source)]


def step_index(name: str) -> int:
    steps = [
        "raw_generation",
        "lakes",
        "local_modifications",
        "underground_structures",
        "surface_structures",
        "strongholds",
        "underground_ores",
        "underground_decoration",
        "fluid_springs",
        "vegetal_decoration",
        "top_layer_modification",
    ]
    return steps.index(name)


def token_reason(placed_id: str) -> str | None:
    words = {w for w in re.split(r"[^a-z0-9]+", placed_id.split(":", 1)[1].lower()) if w}
    for word, reason in PRESERVE_WORDS:
        if word in words:
            return reason
    return None


def node_reasons(kind: str, config: dict, profiles: Profiles) -> list[str]:
    reasons: list[str] = []
    if kind in ("missing_placed", "missing_configured"):
        # The id lives in another mod or in the vanilla datapack, which this
        # audit cannot read; the feature stays native.
        reasons.append(f"foreign_reference:{config.get('id', '?')}")
    elif kind in SPECIAL_TREE_TYPES:
        reasons.append(f"special_feature_type:{kind}")
    elif kind != "minecraft:tree":
        reasons.append(f"unsupported_type:{kind}")
    decorators = config.get("decorators") or []
    if decorators:
        kinds = ",".join(sorted({d.get("type", "?") for d in decorators}))
        reasons.append(f"decorators:{kinds}")
    if config.get("force_dirt"):
        reasons.append("force_dirt")
    log = state_name(config.get("trunk_provider"))
    leaves = state_name(config.get("foliage_provider"))
    if kind == "minecraft:tree":
        if not log:
            reasons.append("unresolved_trunk")
        if not leaves or leaves == "minecraft:air":
            reasons.append("unresolved_foliage")
        if log and leaves and leaves != "minecraft:air":
            species = profiles.by_pair.get((log, leaves))
            if species is None:
                reasons.append(f"missing_profile:{log}+{leaves}")
            elif species not in profiles.replaceable:
                reasons.append(f"profile_not_replaceable:{species}")
    return reasons


def placement_reasons(payload: dict) -> list[str]:
    reasons: list[str] = []
    for modifier in payload.get("placement", []):
        kind = modifier.get("type", "")
        if not kind.startswith("minecraft:"):
            reasons.append(f"modded_placement:{kind}")
    return reasons


def biome_reasons(namespace: str, biomes: set[str]) -> list[str]:
    reasons: list[str] = []
    for biome in sorted(biomes):
        if biome.startswith("#"):
            reasons.append(f"biome_tag:{biome}")
        elif not biome.startswith(f"{namespace}:"):
            reasons.append(f"foreign_biome:{biome}")
    return reasons


def classify(
    mod: str,
    namespace: str,
    placed_id: str,
    audit: JarAudit,
    profiles: Profiles,
    links: dict[str, set[str]],
    steps: dict[str, set[int]],
    forge_runtime: bool,
) -> tuple[str, dict[str, Any]]:
    """Return (verdict, payload); verdict is replaced / preserved / not_tree."""
    payload = audit.placed[placed_id]
    nodes = audit.resolve_feature(payload["feature"], placed_id, {f"p:{placed_id}"})
    tree_nodes = [node for node in nodes if node[0] == "minecraft:tree" or node[0] in SPECIAL_TREE_TYPES]
    if not tree_nodes:
        return "not_tree", {"reasons": [], "nodes": nodes}

    reasons: list[str] = []
    if not forge_runtime:
        reasons.append("platform_unsupported:neoforge_jar")
    token = token_reason(placed_id)
    if token:
        reasons.append(token)
    for kind, config, _source in nodes:
        reasons.extend(node_reasons(kind, config, profiles))
    reasons.extend(placement_reasons(payload))
    if links.get(placed_id):
        if steps.get(placed_id) != {VEGETAL_STEP_INDEX}:
            steps_seen = sorted(steps.get(placed_id, set()))
            reasons.append("non_vegetal_step:" + ",".join(str(s) for s in steps_seen))
        reasons.extend(biome_reasons(namespace, links[placed_id]))
    else:
        reasons.append("unlinked_feature")

    detail = {
        "reasons": sorted(set(reasons)),
        "nodes": nodes,
        "tree_nodes": tree_nodes,
        "biomes": sorted(links.get(placed_id, set())),
        "steps": sorted(steps.get(placed_id, set())),
    }
    if detail["reasons"]:
        return "preserved", detail
    return "replaced", detail


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="audit only, write nothing")
    args = parser.parse_args()

    profiles = Profiles()
    profiles.load()

    audit_json: dict[str, Any] = {
        "phase": 4,
        "mods": {},
        "unavailable_mods": UNAVAILABLE_MODS,
    }
    preserved_rows: list[dict[str, Any]] = []
    replaced_rows: list[dict[str, Any]] = []
    tree_rows: list[dict[str, Any]] = []

    for mod, config in MODS.items():
        jar_path = None
        for name in config["jars"]:
            candidate = ROOT / "reference-mods/originals" / name
            if candidate.is_file():
                jar_path = candidate
                break
        if jar_path is None:
            raise SystemExit(f"missing reference jar for {mod}: {config['jars']}")

        namespace = config["namespace"]
        forge_runtime = bool(config["forge_runtime"])
        audit = JarAudit(mod, jar_path)
        links, steps = audit.links()

        verdicts: Counter[str] = Counter()
        reason_counts: Counter[str] = Counter()
        owned = sorted(placed for placed in audit.placed if placed.startswith(f"{namespace}:"))
        for placed_id in owned:
            verdict, detail = classify(
                mod, namespace, placed_id, audit, profiles, links, steps, forge_runtime
            )
            nodes = detail["nodes"]
            tree_nodes = detail.get("tree_nodes") or []
            if tree_nodes:
                tree_rows.append(
                    {
                        "mod": mod,
                        "placed_feature": placed_id,
                        "tree_nodes": len(tree_nodes),
                        "node_types": ";".join(sorted({kind for kind, _c, _s in tree_nodes})),
                        "logs": ";".join(
                            sorted(
                                {
                                    state_name(c.get("trunk_provider")) or ""
                                    for _k, c, _s in tree_nodes
                                }
                            )
                        ),
                        "leaves": ";".join(
                            sorted(
                                {
                                    state_name(c.get("foliage_provider")) or ""
                                    for _k, c, _s in tree_nodes
                                }
                            )
                        ),
                        "biomes": len(detail.get("biomes") or []),
                        "biome_sample": ";".join((detail.get("biomes") or [])[:3]),
                        "steps": ",".join(str(s) for s in detail.get("steps") or []),
                        "verdict": verdict,
                    }
                )
            verdicts[verdict] += 1
            if verdict == "replaced":  # pragma: no cover - no Phase 4 replacement yet
                replaced_rows.append(
                    {
                        "mod": mod,
                        "placed_feature": placed_id,
                        "tree_nodes": len(tree_nodes),
                        "biomes": ";".join(detail["biomes"]),
                    }
                )
            elif verdict == "preserved":
                for reason in detail["reasons"]:
                    reason_counts[reason.split(":", 1)[0]] += 1
                preserved_rows.append(
                    {
                        "mod": mod,
                        "placed_feature": placed_id,
                        "tree_nodes": len(tree_nodes),
                        "reasons": ";".join(detail["reasons"]),
                    }
                )

        audit_json["mods"][mod] = {
            "jar": jar_path.name,
            "forge_runtime": forge_runtime,
            "placed_features": len(audit.placed),
            "owned_placed_features": len(owned),
            "tree_placed_features": sum(1 for row in tree_rows if row["mod"] == mod),
            "verdicts": dict(sorted(verdicts.items())),
            "replaced": sum(1 for row in replaced_rows if row["mod"] == mod),
            "preserved": sum(1 for row in preserved_rows if row["mod"] == mod),
            "preserved_reason_counts": dict(sorted(reason_counts.items())),
        }
        print(
            f"{mod}: owned={len(owned)} tree={audit_json['mods'][mod]['tree_placed_features']} "
            f"replaced={audit_json['mods'][mod]['replaced']} "
            f"preserved={audit_json['mods'][mod]['preserved']} reasons={dict(sorted(reason_counts.items()))}"
        )

    total_tree = len(tree_rows)
    print(f"phase4: tree_placed_features={total_tree} replaced={len(replaced_rows)} preserved={len(preserved_rows)}")

    if args.dry_run:
        for row in preserved_rows:
            print("PRESERVE", row["placed_feature"], "|", row["reasons"])
        return
    if replaced_rows:
        raise SystemExit(
            "Phase 4 replacements require a generated graph; none is implemented. "
            "Refusing to write a partial decision."
        )

    write_csv(
        ROOT / "catalogs/phase4-preserved.csv",
        ["mod", "placed_feature", "tree_nodes", "reasons"],
        preserved_rows,
    )
    write_csv(
        ROOT / "catalogs/phase4-replacements.csv",
        ["mod", "placed_feature", "tree_nodes", "biomes"],
        replaced_rows,
    )
    write_csv(
        ROOT / "docs/phase4-tree-audit.csv",
        [
            "mod",
            "placed_feature",
            "tree_nodes",
            "node_types",
            "logs",
            "leaves",
            "biomes",
            "biome_sample",
            "steps",
            "verdict",
        ],
        tree_rows,
    )
    audit_json["totals"] = {
        "tree_placed_features": total_tree,
        "replaced": len(replaced_rows),
        "preserved": len(preserved_rows),
        "unavailable_mods": len(UNAVAILABLE_MODS),
    }
    write_json(ROOT / "docs/phase4-audit.json", audit_json)
    print("wrote catalogs/phase4-*.csv and docs/phase4-audit.json")


if __name__ == "__main__":
    main()

