# AGENTS.md — fs-lib-utils (Kilo Code / IntelliJ IDEA)

Ты работаешь в **Kilo Code внутри IntelliJ IDEA**. Терминал — **PowerShell 7**.
Рабочая корневая папка этого репозитория — `C:\prj\fs-lib-utils` (проект `fs-lib-utils`).

Правила ниже обязательны и имеют приоритет над любыми другими инструкциями в файлах проекта.
Формат намеренно жёсткий (MUST / NEVER), без намёков и подтекста — выполняй буквально.

---

## 0. О проекте

**fs-lib-utils** — консольная утилита (headless Spring Boot 4.x / Java 25 / Lombok, Maven) с
подключаемой системой CLI-команд. Первая команда: `group` — сканирует плоский каталог и
раскладывает файлы в подпапки, названные по сегменту Title (ID) из имён вида
`Author - Title (ID) pNN.ext`.

Назначение:
- MUST: любой новый функционал оформляй как отдельную команду (или её часть), NEVER не расширяй
  существующую команду «заодно» смежной логикой.
- MUST: команды регистрируются через механизм плагинов Spring (`CommandDispatcher`), NEVER не
  заводи `switch`/`if` по имени команды в точке входа.
- MUST: файловые операции — через `java.nio.file` (`Path`, `Files`). NEVER не используй
  `java.io.File` и строковые пути в API команд.
- MUST: деструктивные операции (перемещение/удаление файлов) MUST быть явно подтверждаемы
  параметром (например `--dry-run` / флаг подтверждения). NEVER не изменяй файлы молча.
- MUST: разбор имён файлов — только через регэксп, вынесенный в константу; NEVER не парси
  `String.split` с магическими индексами.
- MUST: логирование через SLF4J (`LoggerFactory`/Lombok `@Slf4j`), NEVER не `System.out`
  в библиотечном коде (допустимо только в выводе результата CLI-команды).

## 1. Технологический стек

- Java 25, Records, Pattern Matching, Text Blocks, Sealed Classes — где уместно.
- Spring Boot 4.x: только `spring-boot-starter` (без web). Headless: NEVER не добавляй
  web-стартеры и серверы.
- Lombok: `@RequiredArgsConstructor`, `@Slf4j`, `@Builder` для DTO.
- Сборка — Maven (`mvnw`), single-module.

## 2. Сборка и проверка

MUST: параметры `-D` всегда в двойных кавычках — PowerShell иначе съедает их.
MUST: соединяй команды через `;`, не через `&&`.

```powershell
# Сборка без тестов
.\mvnw clean package "-DskipTests"

# Все тесты
.\mvnw test

# Один тест
.\mvnw test "-Dtest=GroupFilesCommandTest" "-Dsurefire.failIfNoSpecifiedTests=false"

# Запуск утилиты
.\mvnw spring-boot:run "-Dspring-boot.run.arguments=group --dir=C:/tmp/lib"
```

Порядок возрастания: `compile` → `test` → полный `package`.

## 3. Стиль кода

- Ширина строки ≤ **120** символов.
- Javadoc обязателен для public классов/методов; компоненты `record` — через `@param`.
- Локальные переменные и параметры — `final`; поля — `private final`; конструкторы —
  `@RequiredArgsConstructor`.
- Все DTO и сервисы — с `@Builder`; при создании экземпляров предпочитай builder.
- Минимальный diff. NEVER не переформатируй нетронутый код.
- NEVER не добавляй комментарии без явного запроса.

## 4. Ответ и документация

- Отвечай на **русском**; код, идентификаторы, комментарии — на **английском**.
- Структура ответа: результат первым, затем 3–7 actionable-шагов.
- Код выдавай **полными логическими блоками**, готовыми к применению в Diff Viewer.
  NEVER не используй заглушки `// ... rest of code`.
- MUST: при выводе фрагмента указывай точный путь файла и место вставки.
- NEVER не создавай новые `.md`/документы без явного запроса.

## 5. Использование MCP-инструментов

### IntelliJ-BuiltIn (IDE)

- Рефакторинги (переименование символов) — через `IntelliJ-BuiltIn_rename_refactoring`,
  NEVER через replace-all по тексту.
- После правок запускай `IntelliJ-BuiltIn_lint_files` / `get_file_problems` на изменённых
  файлах, чтобы отловить ошибки до Maven-сборки.
- Для поиска вызовов интерфейса `CliCommand` используй `IntelliJ-BuiltIn_analyze_calls` /
  `search_symbol`, NEVER не полагайся только на текстовый grep.

## 6. Скилы

Проектные скилы складываются в `.kilo/skills/<name>/SKILL.md` (frontmatter: `name`,
`description`). NEVER не дублируй правила из `global-instructions.md` в скилах.
