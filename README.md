# Forbidden Magic

<img src="assets/branding/aop-logo.png" alt="Forbidden Magic" width="256" height="256">

**AOP projects:** [Thaumcraft 4](https://github.com/Salossov/AOP-Thaumcraft-4) · [Compact](https://github.com/Salossov/AOPTC4-Compact) · [Thaumic Tinkerer](https://github.com/Salossov/AOPTC4-Thaumic-Tinkerer) · [AOP UI](https://github.com/Salossov/AOP-UI) · [Forbidden Magic](https://github.com/Salossov/AOPTC4-Forbidden-Magic) · [Thaumic Energistics](https://github.com/Salossov/AOPTC4-Thaumic-Energistics)

[English](README.md) · [Русский](README.ru.md)

Unofficial port of SpitefulFox’s Forbidden Magic 0.575 to **Minecraft 1.21.1 / NeoForge**. Adds sinful aspects, dark artifacts, enchantments, wands, the Wrath Cage and integrations with compatible magic mods.

Install the gameplay JAR on both client and server with **NeoForge 21.1.166**, **Java 21**, **AOP: Thaumcraft 4 1.1.0**, **AOP UI 0.8.0** and **Curios 9.5.0+**. Use compatible local ports for optional integrations; legacy configuration switches do not supply absent dependencies. The original EE3 EMC integration is adapted to ProjectE. Source and development API JARs do not belong in `mods/`.

**1.0.0** · WTFPL-2.0 · [Changelog](CHANGELOG.md)

NeoForge mod ID: **`forbidden_aop`**. Registry and resource namespace: **`forbidden`**. Existing item/block IDs and configuration filenames are retained.

## Settings

Open the mod through the Mods screen → Config, or select it in AOP UI. All existing settings have English and Russian labels and tooltips. Startup settings require a full restart; AOP UI shows pending changes. No additional balance settings are introduced.

## Build

Requires Java 21, Python 3.11+ and compatible AOP UI / Thaumcraft API and runtime JARs. Forbidden Magic also needs its optional-integration compile inputs. Dependencies are never embedded in the gameplay JAR.

Build locally with `./gradlew.ps1 build sourcesJar` (PowerShell) or `./gradlew build sourcesJar` (POSIX), passing `-P<role>=<absolute path>` for the dependency roles listed below. The wrapper downloads the pinned Gradle distribution. CI builds our dependencies from their source repositories automatically.

## Continuous integration

In **Actions → Build and release → Run workflow**, enable **release** for an ordinary `v<version>` release; leave it unchecked for the single `nightly` prerelease. Blank **version** uses `mod_version`; an override must be `major.minor.patch`. Pull requests build without publishing. Existing ordinary tags are never overwritten; ordinary release notes list commits since the preceding release. Local `act` runs only validate publication plans and cannot publish.

Dependencies are built automatically from the AOP UI and Thaumcraft source repositories; Forbidden Magic also builds Thaumic Tinkerer. Repository/ref defaults can be overridden with `AOPUI_REPOSITORY` / `AOPUI_REF`, `THAUMCRAFT_REPOSITORY` / `THAUMCRAFT_REF`, and `TINKERER_REPOSITORY` / `TINKERER_REF` where applicable. `DEPENDENCY_TOKEN` needs Contents read access when a dependency repository is private; public source repositories need no additional token. No dependency archive URL or SHA-256 repository variables are required. Cached JARs are reused only when source revisions, content, toolchain and dependency checksums match. Dependencies are never published by this workflow.

The workflow generates and verifies its dependency manifest and SHA-256 automatically. Third-party FM compile inputs are downloaded from the fixed URLs in [.github/ci-external.json](.github/ci-external.json), with checksum and mod-ID verification. Artifacts contain gameplay/sources JARs, `SHA256SUMS.txt` and build provenance.

For local `act` verification, export compatible source checkouts to `.dependencies/aopui`, `.dependencies/thaumcraft` and (FM only) `.dependencies/tinkerer`, or set the corresponding `SUITE_*_ROOT` variables. Windows linked worktrees need ignored `.suite-source.json` inventories for exported dependency checkouts and `python .github/scripts/ci.py inventory` for the current component. Local publication is always a dry run.

## Artwork and credits

See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md) for authors and third-party terms. Third-party dependencies and the separate Thaumcraft runtime are not relicensed by this project.

Required dependency roles: `thaumcraftApiJar`, `thaumcraftRuntimeJar`, `aopUiJar`, `botaniaJar`, `bloodMagicJar`, `modopediaJar`, `tinkererJar`.

Local build compatibility set: Botania 456 (pinned public Maven build), Blood Magic 3.4.0-46-alpha, Modopedia 1.1.5 and Thaumic Tinkerer 1.1.0. These compile/test inputs are not automatically required in a player installation.

- SpitefulFox: original Forbidden Magic.
- [Salossov](https://github.com/Salossov): port and maintenance.
- NeoForge, AOP: Thaumcraft 4, AOP UI and the integrated mod projects: platform and APIs.
