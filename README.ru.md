# Forbidden Magic — неофициальный порт 1.21.1

Оригинальный Forbidden Magic 0.575 создан SpitefulFox. Цель: Minecraft 1.21.1,
NeoForge 21.1.252, Java 21, локальный Thaumcraft/API 1.0.0 и Curios 9.5.1+1.21.1.

Первая пачка реализации добавляет аспекты, осколки/ресурсы, заражённые растения
и дерево, торт, определения стандартных частей жезлов и зачарований, клетку
и обмен кристаллами. Инструменты, обновления жезлов, эффекты зачарований,
спавн/питание клетки, исследования и интеграции ещё не завершены.
Игровая серверная/клиентская приёмка не проводилась.

Нужна соответствующая локальная сборка Thaumcraft/API с перегрузкой blend
у Aspect.registerCompound и entity_variant_aspect_additions. Старые артефакты
1.0.0 без этих дополнений несовместимы, несмотря на тот же номер версии.

Сначала собрать совместимое ядро/API, затем в PowerShell:

```powershell
.\gradlew.ps1 build --offline
```

Путь API можно задать через `-PthaumcraftApiJar=C:/dependencies/thaumcraft-api-1.0.0.jar`.
Для игровых профилей также нужны runtime JAR ядра и AOP: UI 0.7.x (`-PaopUiJar=...`).
Runtime ядра переопределяется `-PthaumcraftRuntimeJar=...`.

Build включает 768 профилей gates, native codec checks и три отдельных JVM
с фикстурами регистрации normal/No Lust/disabled cage. Они не запускают игру
и не заменяют приёмку. В игровой JAR входят только main/client; проверочные
source sets остаются отдельно. Профили запуска используют `runs/private-*`,
EULA автоматически не принимается. Авторство и лицензии — в NOTICE.md/LICENSE.
