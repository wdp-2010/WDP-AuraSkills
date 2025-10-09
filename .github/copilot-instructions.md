# AuraSkills Development Guide for AI Agents

## Project Overview

AuraSkills is a multi-module Minecraft RPG skills plugin for Spigot/Paper. The codebase is split into **4 distinct modules** with clear separation of concerns:

- **`api/`** - Platform-independent API (`dev.aurelium:auraskills-api`, Java 8 compatible)
- **`api-bukkit/`** - Bukkit-specific API extensions (`dev.aurelium:auraskills-api-bukkit`, depends on `api`)
- **`common/`** - Core skills systems (registries, config, messages, rewards, storage) - no Bukkit dependencies
- **`bukkit/`** - Implementation layer (sources, abilities, traits, loot, commands, menus) - requires Bukkit API

**Package namespace changed from `com.archyx.aureliumskills` to `dev.aurelium.auraskills` in version 2.0.**

## Architecture Patterns

### Registry System

All content (Skills, Stats, Abilities, Traits, ManaAbilities, SourceTypes) uses a `Registry<T, P>` pattern:
- Each registry extends `common/registry/Registry.java` with type parameters for the content type and its provider
- Content is identified by `NamespacedId` (format: `namespace/key`, defaults to `auraskills/key`)
- Registries call `registerDefaults()` during initialization to register built-in content
- Custom content registration via `NamespacedRegistry` obtained from `AuraSkillsApi.useRegistry(namespace, contentDirectory)`

Example: `SkillRegistry`, `StatRegistry`, `AbilityRegistry` in `common/` module.

### Configuration Loading

Uses Configurate (SpongePowered) library for YAML parsing:
- `ConfigurateLoader` in `common/config/` handles merging embedded defaults with user configs
- Pattern: Load embedded → Load user → Merge → Parse with loaders (e.g., `SkillLoader`, `StatLoader`, `AbilityLoader`)
- Main config options enum: `common/config/Option.java` with strongly-typed accessors via `ConfigProvider`
- Content files support multi-namespace merging (e.g., `yourplugin/` abilities can reference `auraskills/` skills)

### API Initialization Sequence (onEnable)

See `bukkit/AuraSkills.java` lines 195-300 for full lifecycle. Key phases:
1. API registration (`ApiRegistrationUtil.register(api)`)
2. Manager initialization (SkillManager, AbilityManager, StatManager, etc.)
3. Registry creation and `registerDefaults()`
4. Config generation and migration (`generateConfigs()`, `MigrationManager`)
5. Config loading (`configProvider.loadOptions()` - also loads hooks)
6. Storage initialization (`initStorageProvider()` - MySQL or YAML)
7. First-tick scheduler tasks (loadSkills, registerLevelers, loadRewards)

### Hook System

External plugin integrations in `bukkit/hooks/`:
- Enum `Hooks` implements `HookType` with registration controlled by `config.yml` hooks section
- Hooks loaded during `configProvider.loadOptions()` if plugin detected
- Some hooks (PlaceholderAPI, Nexo) set `requiresEnabledFirst=false` to allow async loading
- WorldGuard flags registered in `onLoad()` lifecycle method

### Scheduler Abstraction

`BukkitScheduler` wraps Folia-compatible scheduling via `FoliaLib`:
- Use `plugin.getScheduler().executeSync()/executeAsync()` for tasks
- Background tasks (e.g., StatisticLeveler) use `timerSync()` with proper TimeUnit
- Menu loading callbacks use scheduled delayed execution (50ms typical)

## Build & Test Workflow

### Building
```bash
./gradlew clean build          # Full build with shadowJar
./gradlew checkstyleMain checkstyleTest  # Run code style checks
```

Output: `build/libs/AuraSkills-<version>.jar` (shaded with relocations)

### Testing
```bash
./gradlew test                 # Run all tests
./gradlew :bukkit:runServer    # Start Paper test server (1.21.10)
```

Test server auto-downloads Paper to `bukkit/run/`. First run requires EULA acceptance.

### Code Style (Checkstyle)

Critical rules from `config/checkstyle/checkstyle.xml`:
- **No tabs** - use spaces (4-space indent)
- **No trailing whitespace**
- **No multiple consecutive blank lines**
- **LF line endings only** (no CRLF)
- **Import order**: `*`, `javax`, `java` (separated with blank lines)
- **Curly braces**: `{` on same line, `}` on new line except for `else`/`catch`
- **No static imports** for: `of`, `copyOf`, `valueOf`, `all`, `none`, `Optional.*`
- **One top-level class per file**

Use IntelliJ Checkstyle-IDEA plugin with `config/checkstyle/checkstyle.xml` for real-time validation.

## Key Technical Decisions

### Why Module Separation?
- **API stability**: External plugins depend only on `api`/`api-bukkit` (published to Maven Central)
- **Future portability**: `common` module enables potential Folia/Fabric ports without rewriting core logic
- **Clean contracts**: Implementation in `bukkit`/`common` can change without breaking API consumers

### Java Version Strategy
- **API modules**: Java 8 source/target for maximum compatibility (`options.release.set(8)`)
- **Implementation modules**: Java 21 toolchain for modern features
- **Compilation**: Uses Java 21 compiler with `-parameters` flag (required for ACF command framework)

### NBT API Integration
- Uses `tr7zw/item-nbt-api` shaded library for cross-version item NBT access
- Checks version compatibility in `initializeNbtApi()` - warns if unsupported
- Used for: legacy item conversion, item requirement/multiplier storage, custom loot data

### Storage Abstraction
- `StorageProvider` interface with MySQL and YAML implementations
- Configured via `config.yml` `sql.enabled` option
- Auto-saves enabled via `StorageProvider.startAutoSaving()`
- User data files in `userdata/` (YAML) or MySQL tables

## Common Pitfalls

1. **Don't use Bukkit APIs in `common/` module** - breaks architecture, causes compile errors
2. **Don't forget Checkstyle** - PRs require `./gradlew checkstyleMain checkstyleTest` to pass
3. **Test on real server** - Use `./gradlew :bukkit:runServer` before PRs
4. **Avoid `...existing code...` markers** - Always show full code in edits, never use placeholder comments
5. **NamespacedId case sensitivity** - Keys are forced lowercase, use `NamespacedId.of(namespace, key.toLowerCase())`
6. **ConfigurationNode null checks** - `.raw()` can return null, always validate before parsing

## Useful References

- **API Javadocs**: https://docs.aurelium.dev/auraskills-api-bukkit/
- **Wiki**: https://wiki.aurelium.dev/auraskills
- **Slate Menu Library**: Used for inventory menus (`plugin.getSlate()`)
- **ACF Commands**: Aikar's Command Framework handles command registration
- **Adventure API**: Text components (`net.kyori.adventure.*` shaded to `dev.aurelium.auraskills.kyori.*`)

## Testing Conventions

- Use JUnit 5 (`@Test`, `@BeforeEach`, `@AfterEach`)
- MockBukkit for Bukkit API mocking (`bukkit/src/test/`)
- Test fixtures in `common/src/testFixtures/` for shared test utilities
- Example: `LeaderboardManagerTest` uses MySQL container setup in `@BeforeEach`

## Commit Guidelines

- **Subject**: Imperative mood ("Add ...", not "Added ..."), max 72 chars, capitalized, no period
- **No commit prefixes**: Don't use "fix:", "feat:", etc.
- **Config paths**: Wrap in backticks like \`some.config.path\`
- **Multiple changes**: Separate with blank line + body paragraph
