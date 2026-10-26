# Forbidden Magic — неофициальный порт на 1.21.1

Неофициальный порт модификации Forbidden Magic 0.575 (автор SpitefulFox) для Minecraft 1.21.1,
NeoForge 21.1.252, Java 21, локального ядра Thaumcraft/API 1.0.0 и Curios 9.5.1+1.21.1.

## Статус миграции и реализованные пачки

- **P01 Базовый контент и аспекты:** 7 греховных аспектов (`infernus`, `superbia`, `gula`, `invidia`, `desidia`, `ira`, `luxuria`), осколки Незера, ресурсы порчи, дерево порчи, темные растения, пирог порчи, базовые стержни жезлов, зачарования.
- **P02 Инструменты, жезлы и фокусы:** Хамелеон-инструменты (Morph) с 3 фазами и Impact 3x3, инструменты порчи, Топор черепов, Вилка диаболиста, Кнут, Кольцо еды, Ошейник послушания, Primewell и Crystalwell, Фокус мерцания (Blink: транзакционное списание 3 perditio, дальность 128 блоков, улучшения Hellfire и Pandemonium).
- **P03 Клетка гнева и житель:** Клетка гнева (потребление эссенции с suction 256, приоритет особого/ira/desidia топлива, редстоун-контроль, лимит 6 мобов вокруг, каталог 30+ мобов), импринтинг кристалла моба, житель-еретик и рабочий блок Heretic Study с 9 сделками.
- **P04 Кросс-модовые интеграции:**
  - **Thaumic Tinkerer (T01):** 8 стоимостей зачарований FM зарегистрированы напрямую в каталоге TT `EnchanterCatalog` через IMC `RegisterEnchantmentCost`; сохранение KAMI аспектов подтверждено.
  - **Botania:** Стержни из жизнедерева и древесины снов, конверсия маны в вис, 4 цветка Botania (Euclidaisy, Whisperweed, Tainthistle, Manathistle).
  - **Blood Magic:** Стержень крови, кровавый колпачок, Рапира сангвиника, синергия жертвенного кинжала с LP, цветок Bloodthorn.
  - **Twilight Forest:** Мобы Сумеречного леса в клетке гнева (Mini Ghast, Tower Ghast и др.) и их аспекты.
  - **Viewers (JEI / EMI):** Все 5 категорий рецептов (Arcane Shaped, Arcane Shapeless, Crucible, Infusion, Infernal) автоматически индексируются через `thaumcraft_viewers`.

## Верификация и архитектура

13 JVM-наборов проходят 14 649 проверок правил и снимков данных. Независимая legacy-сверка всех рецептов/исследований остаётся открытой:
1. `foundationRules`: Проверка комбинаций ContentGates (768 профилей, 9 216 проверок).
2. `semanticDataCheck`: Снимки рецептов/исследований и отрицательные мутации (2 706 проверок).
3. `componentCodecCheck`: Кодеки компонентов и сетевая сериализация (12 проверок).
4. `p01ContentNormal`: Стандартный профиль регистрации (322 проверки).
5. `p01ContentNoLust`: Профиль с отключенной похотью (322 проверки).
6. `p01ContentNoCage`: Профиль с отключенной клеткой гнева (320 проверок).
7. `p02ToolsCheck`: Механики инструментов и аксессуаров (40 проверок).
8. `p02WandCheck`: Жезлы, колпачки, стержни и логика фокуса Blink (258 проверок).
9. `p03WrathCageCheck`: Логика клетки гнева, suction и спавнера (224 проверки).
10. `p03VillagerCheck`: Профессия жителя, сделки, POI (65 проверок).
11. `p04CompatCheck`: Интеграции Thaumic Tinkerer, Twilight Forest, Viewers (163 проверки).
12. `p04BloodMagicCheck`: Интеграция Blood Magic (443 проверки).
13. `p04BotaniaCheck`: Интеграция Botania (558 проверок).

*Игровая приёмка не завершена. Запуск выделенного сервера и EULA для отдельных тестовых миров разрешены пользователем 25.10.2026; клиентская приёмка ещё не выполнена. В реальном мире найдены ошибки, которых не обнаружили офлайн-снимки данных. Успешные JVM-наборы не доказывают полное соответствие оригиналу.*

### Автоматическая серверная приёмка

```powershell
.\tools\acceptance\Invoke-LiveServerAcceptance.ps1 -AcceptEula -Profiles @('base','no-lust','no-cage','no-enchantments','gluttony-off','gluttony-hard','botania','bloodmagic','tinkerer','integrations','free-cage') -Lifecycle -ContinueOnFailure
```

Каждый запуск создаёт новый `build/acceptance/<runId>`, принимает EULA только в нём и использует два последовательных процесса для записи/перезапуска/чтения мира. Запускается выделенный сервер `--nogui`; клиент, окно и звук отсутствуют. Проверяется происхождение кода из gameplay JAR, свежесть отчёта, ненулевое число утверждений, отсутствие ошибок загрузки и штатная остановка.

`base` дополнительно проверяет действия в мире: дерево/куст/торт/краситель/лопату/печь. Он сохраняет неуспешный результат, если обнаружена ошибка; `-ContinueOnFailure` позволяет выполнить независимые профили. Отчёты не заменяют двухклиентский мультиплеер или визуальные проверки. Приёмочный мод находится в `src/acceptance` и не включается в выпускной JAR.

## Сборка и верификация

Запуск лаунчера офлайн-проверок:

```powershell
.\tools\verification\Invoke-FmVerification.ps1 -Phase Offline -VerifyArtifacts
```

Запуск регрессионных тестов парсера и артефактов:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verification\Test-FmVerificationParser.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\verification\Test-FmArtifactVerification.ps1
```

## Подготовка комплекта зависимостей

`tools/acceptance/Install-ProductionJar.ps1` требует Python 3.11+ со стандартным `tomllib` для разбора настоящих TOML-таблиц. Аудит отличает объявления `[[mods]]` от ссылок `[[dependencies.*]]`, проверяет обязательные зависимости и дубликаты mod ID; optional/mandatory=false не считаются обязательными. Результат сохраняется в `dependency-audit.json`. Диапазоны версий проверяются NeoForge при реальном запуске.

Серверная матрица 25.10.2026: 11 профилей, 22 процесса write/read, 1460 assertions PASS. Это покрытие перечисленных сценариев, а не завершение полной приёмки.

## Выпускные артефакты и лицензия

- **Лицензия:** WTFPL-2.0 (см. `LICENSE`).
- **Авторы:** Исходный мод Forbidden Magic 0.575 создан SpitefulFox при участии контрибьюторов (см. `NOTICE.md`).
- **Состав JAR:** Выпускной JAR содержит исключительно классы и ресурсы `main` и `client`. Все тестовые пакеты (`rules`, `nativeVerification`) и классы ядра строго отфильтрованы.


### Tested optional dependency set (2026-10-26)

NeoForge21.1.252 / Java21; local Thaumcraft1.0.0 and AOP UI0.7.0, Curios9.5.1+1.21.1. The isolated combined test set also contains Botania456 (snapshot20260912), Blood Magic3.4.0-46-alpha, Thaumic Tinkerer1.0.0, AE2 19.2.18, Compact0.7.0 and EMI1.1.24. Optional integrations remain optional; this list records the tested versions, not full gameplay acceptance or availability of the historical blocked integrations.
