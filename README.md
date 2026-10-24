# Forbidden Magic — Unofficial 1.21.1 Port

Unofficial modern port of Forbidden Magic 0.575 by SpitefulFox for Minecraft 1.21.1,
NeoForge 21.1.252, Java 21, local Thaumcraft/API 1.0.0 and Curios 9.5.1+1.21.1.

## Migration Status & Implemented Batches

- **P01 Foundation & Content:** 7 sinful aspects (`infernus`, `superbia`, `gula`, `invidia`, `desidia`, `ira`, `luxuria`), Nether shards, Taint resources, Taint tree/plants, Arcane Cake, standard wand parts, base enchantments.
- **P02 Tools, Wands & Foci:** Chameleon (Morph) tools with 3-phase cycling and 3x3 Impact mining, Taint tools, Skull Axe, Diabolist Fork, Riding Crop, Ring of Nutrition, Collar of Pain, Primewell & Crystalwell, Blink Focus (transactional vis deduction, 128-block range, Hellfire & Pandemonium upgrades).
- **P03 Wrath Cage & Villager:** Wrath Cage (essentia consumption with suction 256, special/ira/desidia fuel priority, redstone pause, 6 nearby mob limit, catalog of 30+ mobs), Mob Crystal imprinting, Heretic Villager profession and custom Heretic Study POI with 9 tiered trades.
- **P04 Cross-Mod Integrations:**
  - **Thaumic Tinkerer (T01):** 8 enchantment costs registered directly with TT `EnchanterCatalog` via `RegisterEnchantmentCost` IMC; KAMI aspects preservation verified.
  - **Botania:** Livingwood & Dreamwood wand parts, Mana Vis conversion, 4 Botania flowers (Euclidaisy, Whisperweed, Tainthistle, Manathistle).
  - **Blood Magic:** Blood wand core & cap, Sanguine Rapier, Sacrificial Dagger LP synergy, Bloodthorn flower.
  - **Twilight Forest:** Twilight cage mobs (Mini Ghast, Tower Ghast, etc.) and mob aspects.
  - **Viewers (JEI / EMI):** All 5 recipe categories (Arcane Shaped, Arcane Shapeless, Crucible, Infusion, Infernal) automatically indexed via `thaumcraft_viewers`.

## Verification & Architecture

All code and data mechanics are verified offline via 13 dedicated JVM test suites (12,555 assertions):
1. `foundationRules`: ContentGates cross-product configurations (768 profiles, 9,216 checks).
2. `semanticDataCheck`: 33 recipe JSONs and 31 research JSONs semantic integrity (1,107 checks).
3. `componentCodecCheck`: Data component persistence and packet serialization codecs (12 checks).
4. `p01ContentNormal`: Standard profile registration and content mechanics (322 checks).
5. `p01ContentNoLust`: Lust-disabled profile gating (322 checks).
6. `p01ContentNoCage`: Cage-disabled profile gating (320 checks).
7. `p02ToolsCheck`: Morph tools, Taint tools, accessories mechanics (40 checks).
8. `p02WandCheck`: Wands, caps, rods, Blink focus behavior (258 checks).
9. `p03WrathCageCheck`: Wrath Cage logic, essentia suction, mob limits (224 checks).
10. `p03VillagerCheck`: Heretic Villager profession, trades, POI (65 checks).
11. `p04CompatCheck`: Thaumic Tinkerer, Twilight Forest, Viewers integration (139 checks).
12. `p04BloodMagicCheck`: Blood Magic integration (268 checks).
13. `p04BotaniaCheck`: Botania integration (262 checks).

*Live server and client acceptance tests remain strictly gated until explicit user approval.*

## Building & Verification

Run PowerShell verification launcher:

```powershell
.\tools\verification\Invoke-FmVerification.ps1 -Phase Offline -VerifyArtifacts
```

Run parser and artifact regression suites:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verification\Test-FmVerificationParser.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verification\Test-FmArtifactVerification.ps1
```

## Release Artifacts & Licensing

- **License:** WTFPL-2.0 (see `LICENSE`).
- **Attribution:** Original Forbidden Magic 0.575 by SpitefulFox and contributors (see `NOTICE.md`).
- **JAR Contents:** Release gameplay JAR contains strictly `main` and `client` classes and resources. All `rules`, `nativeVerification`, test classes, and core leaks are strictly audited and excluded.
