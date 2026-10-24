# Forbidden Magic — unofficial 1.21.1 port

Unofficial port of Forbidden Magic 0.575 by SpitefulFox. Minecraft 1.21.1,
NeoForge 21.1.252, Java 21, local Thaumcraft/API 1.0.0 and Curios 9.5.1+1.21.1.
The first implementation batch adds aspects, shards/resources, tainted plants and
trees, cake, standard wand-part definitions, enchantment data, and the cage/crystal
exchange. Tools, wand updates, enchantment effects, cage spawning/essentia,
research and optional integrations remain incomplete. A successful build is not
server/client acceptance. No game server or client acceptance has been performed.

Use the matching local Thaumcraft/API build with the explicit aspect-blend
overload and `entity_variant_aspect_additions`. Earlier 1.0.0 artifacts without
these additions are incompatible despite sharing the same version string.

Build the matching local Thaumcraft API first, then run PowerShell:

```powershell
.\gradlew.ps1 build
```

Override the local API location with `-PthaumcraftApiJar=C:/dependencies/thaumcraft-api-1.0.0.jar`.
Runtime profiles also require the matching Thaumcraft gameplay JAR and its AOP: UI 0.7.x
dependency (`-PaopUiJar=...`). `-PthaumcraftRuntimeJar=...` overrides the runtime core.

`foundationRules` checks config gates without Minecraft. `componentCodecCheck` checks
persistence and network codec round trips without starting a server. Both run during `build`.
`src/client` contains client presentation; `src/nativeVerification` contains private native
checks; `src/rules` contains platform-independent checks. Three P01 registry/JVM
fixtures cover normal, No Lust and disabled-cage profiles. Only main/client ship
in the gameplay JAR.

`runClient`, `runServer` and `runGameTestServer` use isolated `runs/private-*` directories.
The project never creates `eula.txt` automatically. No gameplay or worlds are supplied.
Optional Botania/Blood Magic integration targets follow the locally pinned Tinkerer builds;
their gameplay integration will be implemented separately.
