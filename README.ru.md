# Forbidden Magic

<img src="assets/branding/aop-logo.png" alt="Forbidden Magic" width="256" height="256">

**Проекты AOP:** [Thaumcraft 4](https://github.com/Salossov/AOP-Thaumcraft-4) · [Compact](https://github.com/Salossov/AOPTC4-Compact) · [Thaumic Tinkerer](https://github.com/Salossov/AOPTC4-Thaumic-Tinkerer) · [AOP UI](https://github.com/Salossov/AOP-UI) · [Forbidden Magic](https://github.com/Salossov/AOPTC4-Forbidden-Magic) · [Thaumic Energistics](https://github.com/Salossov/AOPTC4-Thaumic-Energistics)

[English](README.md) · [Русский](README.ru.md)

Неофициальный порт Forbidden Magic 0.575 от SpitefulFox на **Minecraft 1.21.1 / NeoForge**. Добавляет греховные аспекты, тёмные артефакты, зачарования, жезлы, клетку гнева и интеграции с совместимыми магическими модами.

Установите игровой JAR на клиент и сервер вместе с **NeoForge 21.1.252**, **Java 21**, **AOP: Thaumcraft 4 1.0.0**, **AOP UI 0.7.0** и **Curios 9.5.1+**. Для необязательных интеграций нужны совместимые порты; старые переключатели конфига не заменяют отсутствующие зависимости. Исходная интеграция EMC для EE3 адаптирована к ProjectE. JAR исходников и API разработчика в `mods/` не устанавливаются.

**0.575.0-alpha.1** · WTFPL-2.0

## Настройки

Откройте «Моды» → мод → «Настройки» либо выберите мод в AOP UI. Для всех существующих параметров добавлены русские и английские названия и подсказки. Параметры запуска требуют полного перезапуска; AOP UI показывает ожидающие применения изменения. Новые настройки баланса не добавлены.

## Сборка

Нужны Java 21, Python 3.11+ и архив закреплённых зависимостей. API и игровые JAR зависимостей не включаются в мод.

Задайте `AOP_DEPENDENCIES_URL` — HTTPS-адрес ZIP зависимостей, а `AOP_DEPENDENCIES_SHA256` — его точный SHA-256. Из корня репозитория выполните:

```powershell
python .github/scripts/ci.py prepare
python .github/scripts/ci.py build
```

Скрипт запускает Gradle wrapper напрямую через Java: `clean build sourcesJar` вместе с проверками репозитория. В `dist/` появятся игровой JAR, исходники, журнал и манифест с хешами исходников и зависимостей. Изменение исходников, зависимостей, Java или повреждение кеша требует пересборки.

Для ручной сборки `./gradlew.ps1 build sourcesJar` (PowerShell) и `./gradlew build sourcesJar` (POSIX) принимают параметры `-P<роль>=<абсолютный путь>` из списка ниже. Wrapper загружает закреплённую версию Gradle. Соседние локальные репозитории удобны при разработке; CI использует явные пути.

## CI

`.github/workflows/build.yml` запускает проверки и сборку на Java 21 при изменениях `main`, pull request и вручную. Укажите переменные репозитория `AOP_DEPENDENCIES_URL` и `AOP_DEPENDENCIES_SHA256` либо их значения при ручном запуске. Workflow сохраняет артефакты сборки; публикации релизов и отправки коммитов в нём нет.

ZIP должен содержать `manifest.json` и только перечисленные в нём JAR:

```json
{
  "format": 1,
  "artifacts": {
    "thaumcraftApiJar": {
      "file": "jars/thaumcraft-api-1.0.0.jar",
      "sha256": "<64 строчных шестнадцатеричных символа>"
    }
  }
}
```

Добавьте в `artifacts` все роли из списка ниже. У каждой роли — уникальный путь к JAR и SHA-256; для игровых зависимостей можно указать `mod_id` для проверки ID NeoForge. Не добавляйте отдельные записи каталогов ZIP. Перед распаковкой проверяется хеш архива, затем — каждого JAR. Размещайте только зависимости, которые вправе распространять; подойдёт закрытый архив. Адреса конкретной машины и архивы в Git не включены.

Для локальной проверки workflow используйте `act workflow_dispatch -j build` с двумя переменными зависимостей в env-файле. Локальный HTTP-источник допускается только при `ACT=true`; GitHub CI требует HTTPS. Для связанного worktree Windows перед `act` также выполните `python .github/scripts/ci.py inventory`: его `.git` ссылается на путь хоста. Игнорируемый инвентарь перечисляет отслеживаемые файлы, содержимое которых затем хешируется внутри CI.

## Графика и авторство

Редактируемый векторный логотип: [assets/branding/logo.svg](assets/branding/logo.svg). Прозрачные PNG 512 × 512 и 64 × 64 экспортированы из него для NeoForge и AOP UI, загружающих PNG-текстуры. Исходником графики остаётся SVG.

Авторы и условия сторонних материалов: [LICENSE](LICENSE), [NOTICE.md](NOTICE.md). Этот проект не меняет лицензии зависимостей и отдельного ядра Thaumcraft.

Обязательные роли зависимостей: `thaumcraftApiJar`, `thaumcraftRuntimeJar`, `aopUiJar`, `botaniaJar`, `bloodMagicJar`, `modopediaJar`, `tinkererJar`.

Набор совместимости для сборки: Botania 456 (совместимый бэкпорт AOP), Blood Magic 3.4.0-46-alpha, Modopedia 1.1.5 и Thaumic Tinkerer 2.5.164-port.1. Эти зависимости компиляции и проверок не становятся обязательными для игрока.
