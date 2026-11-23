# Dependency and license register

## Runtime dependencies of Branched

| Dependency | Required | Version range | Notes |
| --- | --- | --- | --- |
| Minecraft | yes | 1.20.1 | — |
| Forge | yes | `[47,)` | built against 47.4.29 |
| Regions Unexplored | no | `[0.5.6,1.0)` | optional integration |
| Terralith | no | `[2.5.4,3.0)` | optional integration |
| The Aether | no | `[1.5.2,2.0)` | optional integration |
| Twilight Forest | no | `[4.3.2508,5.0)` | optional integration |
| Quark | no | `[4.0,5.0)` | optional integration |
| Fish of Thieves | no | `[3.1.7,4.0)` | optional integration |
| **Dynamic Trees** | **never** | — | development reference only |

`src/main/resources/META-INF/mods.toml` declares the optional mods with
`mandatory = false` and contains no `dynamictrees` entry.
`NoDynamicTreesDependencyTest` asserts both the TOML and the absence of
`dynamictrees` imports.

Optional mods' own transitive requirements (relevant when testing):

- Quark 4.0-462 requires **Zeta** `[1.0-28,2.0)` (mandatory) and Forge `[43.0.8,)`.
- Regions Unexplored 0.5.6 requires **TerraBlender** `>=3.0.0.163` (mandatory).
- Aether 1.5.2 declares Forge `[47.1.0,)`; no Extra dependency observed.
- Twilight Forest 4.3.2508, Terralith 2.5.4 and Fish of Thieves are standalone.

## Audited reference archives

None of these files is redistributed or committed. `reference-mods/` is
git-ignored; this table is the verifiable record instead.

| File | Bytes | SHA-256 | License (declared in jar) |
| --- | ---: | --- | --- |
| `RegionsUnexploredForge-0.5.6+1.20.1.jar` | 5,726,781 | `5ba96a83d12c0e2eb3b22c0fa11262ff6d49ba8d2ebd03e1c59ab47ea7dcc2c6` | All rights reserved |
| `Terralith_1.20.x_v2.5.4.jar` | 3,156,901 | `8f65f309d8f2723754bf4b60c7b5763d3ab6ed04b01c172109ba6564e981b95f` | Stardust Labs License |
| `aether-1.20.1-1.5.2-neoforge.jar` | 39,746,319 | `5cb64ab1f27e78e8338fe67f9c766578904aad9bc1f8bcc90f306ea52aea3fc5` | Assets: All Rights Reserved; Code: LGPL-3.0 |
| `twilightforest-1.20.1-4.3.2508-universal.jar` | 23,332,091 | `0bdc89263616d1b35c32ef82c5e9c14cbd20368e2fe8b468c72a28320be7a778` | LGPL-2.1 |
| `Quark-4.0-462.jar` | 14,935,230 | `c90b96f049d92825c4a103721988c80d72e5ad380d17be46a0f53e114874c478` | Quark license (link in mods.toml) |
| `Zeta-1.0-31.jar` | 1,006,518 | `cd1786902a79434f8fd762d741019d9c8a79725fef7e30a89c153c934e1e6a9e` | Zeta license (link in mods.toml) |
| `fish_of_thieves-mc1.20.1-v3.1.7-forge.jar` | 2,140,464 | `8a99c6060a8e8deaa38e912a649b72fde37427b41ff0eaf70c3f0ca9dc7f2c45` | MIT |
| `fish_of_thieves-mc1.20.1-v3.4.0-forge.jar` | 2,423,875 | `deed9982f30f591164a185efb6c0e8fec1d22d175ff967f933ac673256ece34a` | MIT |
| `TerraBlender-forge-1.20.1-3.0.1.10.jar` | 107,533 | `c77a13e3879f9f991cf592abac60ad0c6d2ed17f5ff697ec2db3872b22902d4a` | LGPLv3 |
| `DynamicTrees-1.20.1-1.4.11.jar` | 3,761,924 | `24885db0f7550055f628c02f0d274bbdd495cd8c6c3bc682b2545beadaf9c020` | MIT |
| `DynamicTreesRU-1.20.1-1.0.1.jar` | 763,194 | `0620f83e80333217dbd567883e8830a367e41085aacaa13b2f916e0e84b8110b` | MIT |
| `DynamicTreesTerralith-1.20.1-1.2.6.jar` | 514,225 | `178db557af0da2c3aedfda38437065aebf9fc0765e7db51340491b20908ace60` | MIT |
| `DynamicTreesAether-1.20.1-1.3.3.jar` | 538,364 | `fad05c708538cad4e787c8b2d179d653fbd0eaf482873c731457415fe8d170d0` | MIT |
| `DynamicTreesTheTwilightForest-1.20.1-2.2.0.jar` | 295,107 | `303598487bc3ef8378fb57d5652111bbef6048a0fb3e67fb67d142602916c3e5` | MIT |
| `DynamicTreesQuark-1.20.1-2.5.3.jar` | 464,224 | `1bdc83a523fd9b3d46e0d80eafbba8bb03c148133cd1f96c5b94b7058cec8344` | MIT |
| `DynamicTreesFishofThieves-1.20.1-1.0.4.jar` | 864,882 | `76b08f044ab2c314f2e2f06e396048551e2344a205d5c4b29ff71df636339556` | MIT |
| `DynamicTreesPlus-1.20.1-1.2.2.jar` | 534,711 | `2860d999e169783f026dd333f30730e1cc07a4bcb7afb0ee76e13fc2bb6334da` | MIT |
| `DynamicTreesAddonLib-1.20.1-0.1.0-BETA5.jar` | 1,335,605 | `7b6e45a196312dfaa8238e97510ec5c8bdfb138e6a609d65dad88461ba27ada8` | MIT |
| `DT_Baobab_Forge_1.20.1_v015.jar` (local recovery) | 47,141 | `3e430959a6b2675758a91e6e943a4a43bbc8f1eaf947a3e975e5ef9614675792` | MIT |

## Redistribution policy

- Branched contains **no** upstream code, assets, textures, models,
  lang files, loot tables or NBT templates.
- Profiles reference block and feature ids by name only. Those ids and the
  blocks they resolve to remain under their authors' licenses.
- Reference jars stay on the developer machine and are excluded from git
  (`.gitignore`: `reference-mods/**/*.jar`).
- The mod's own license (`mod_license` in `gradle.properties`) is set to
  *All Rights Reserved* until the project owner picks a final license.

## Verification status

| Check | Status |
| --- | --- |
| All 19 archives are valid ZIPs | verified (`zipfile.testzip()` per jar) |
| SHA-256 recorded for all archives | verified |
| `mods.toml` declares no Dynamic Trees | verified (test) |
| No Dynamic Trees imports in source | verified (test) |
| Optional integrations marked non-mandatory | verified (test) |
| Aether loads on Forge 47.4.29 | metadata verified; runtime **pending** |
| Datapack override of profiles | code path implemented; runtime **pending** |
| Client / dedicated server launch with all optional mods | **pending** |
