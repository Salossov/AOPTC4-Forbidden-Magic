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

13 JVM suites pass 14,649 checks of rules and data snapshots. Independent legacy recipe/research parity remains open:
1. `foundationRules`: ContentGates cross-product configurations (768 profiles, 9,216 checks).
2. `semanticDataCheck`: Recipe/research snapshots and negative mutations (2,706 checks).
3. `componentCodecCheck`: Data component persistence and packet serialization codecs (12 checks).
4. `p01ContentNormal`: Standard profile registration and content mechanics (322 checks).
5. `p01ContentNoLust`: Lust-disabled profile gating (322 checks).
6. `p01ContentNoCage`: Cage-disabled profile gating (320 checks).
7. `p02ToolsCheck`: Morph tools, Taint tools, accessories mechanics (40 checks).
8. `p02WandCheck`: Wands, caps, rods, Blink focus behavior (258 checks).
9. `p03WrathCageCheck`: Wrath Cage logic, essentia suction, mob limits (224 checks).
10. `p03VillagerCheck`: Heretic Villager profession, trades, POI (65 checks).
11. `p04CompatCheck`: Thaumic Tinkerer, Twilight Forest, Viewers integration (163 checks).
12. `p04BloodMagicCheck`: Blood Magic integration (443 checks).
13. `p04BotaniaCheck`: Botania integration (558 checks).

*Gameplay acceptance is incomplete. On 2026-10-25 the user authorized dedicated-server runs and EULA acceptance in isolated test worlds. Live runs have exposed defects that the offline snapshots missed. Client/rendering and real two-player acceptance have not been performed.*

### Automated dedicated-server acceptance

```powershell
.\tools\acceptance\Invoke-LiveServerAcceptance.ps1 -AcceptEula -Profiles @('base','no-lust','no-cage','no-enchantments','gluttony-off','gluttony-hard','botania','bloodmagic','tinkerer','integrations','free-cage') -Lifecycle -ContinueOnFailure
```

Each invocation uses a fresh `build/acceptance/<runId>` directory, two server processes for persistence checks, `--nogui`, the packaged gameplay JAR, nonce-tagged assertion reports and graceful shutdown. No client, desktop interaction, screenshots or audio are involved. The private `src/acceptance` mod never ships in the gameplay JAR. `base` also exercises actual world interactions and reports known failures rather than suppressing them. These checks do not establish visual or two-player network parity.

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

## Dependency staging

`tools/acceptance/Install-ProductionJar.ps1` requires Python 3.11+ with standard-library `tomllib` to parse actual TOML tables. The audit distinguishes `[[mods]]` declarations from `[[dependencies.*]]` references, detects missing required dependencies and duplicate mod IDs, and honors optional/mandatory=false. It writes `dependency-audit.json`. NeoForge validates version ranges during live startup.

Server matrix on 2026-10-25: 11 profiles, 22 write/read processes, 1460 assertions PASS. This proves the named scenarios, not complete gameplay acceptance.

## Release Artifacts & Licensing

- **License:** WTFPL-2.0 (see `LICENSE`).
- **Attribution:** Original Forbidden Magic 0.575 by SpitefulFox and contributors (see `NOTICE.md`).
- **JAR Contents:** Release gameplay JAR contains strictly `main` and `client` classes and resources. All `rules`, `nativeVerification`, test classes, and core leaks are strictly audited and excluded.


### Tested optional dependency set (2026-10-26)

NeoForge21.1.252 / Java21; local Thaumcraft1.0.0 and AOP UI0.7.0, Curios9.5.1+1.21.1. The isolated combined test set also contains Botania456 (snapshot20260912), Blood Magic3.4.0-46-alpha, Thaumic Tinkerer1.0.0, AE2 19.2.18, Compact0.7.0 and EMI1.1.24. Optional integrations remain optional; this list records the tested versions, not full gameplay acceptance or availability of the historical blocked integrations.
