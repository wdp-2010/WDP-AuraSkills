<h1 style="text-align:center;">WDP AuraSkills</h1>

<p style="text-align:center;">
Enhanced RPG Skills Plugin with SkillCoins Economy
</p>

<p style="text-align: center;font-weight: bold;">
  🎮 <strong>Play on WDPServer:</strong> <code>play.wdpserver.com</code>
</p>

[![GitHub Release](https://img.shields.io/github/v/release/wdp-2010/WDP-AuraSkills?style=flat-square)](https://github.com/wdp-2010/WDP-AuraSkills/releases/latest)

---

## 🚀 About This Rework

**WDP AuraSkills** is a custom rework of the popular AuraSkills (formerly Aurelium Skills) plugin, specifically designed and enhanced for the **WDPServer** community at `play.wdpserver.com`. While maintaining all the core RPG features of the original plugin, this version introduces a revolutionary **SkillCoins economy system** that transforms how players interact with skills and progression.

### What Makes This Different?

This isn't just AuraSkills with a few tweaks - it's a comprehensive enhancement that adds:

- 💰 **Fully-Featured SkillCoins Economy** - A complete currency system integrated directly into the skills plugin
- 🛒 **Interactive Shop System** - Buy levels, XP, abilities, and items using SkillCoins
- 💸 **Player-to-Player Transfers** - Send coins to other players with the `/pay` command
- 🔗 **Vault Integration** - SkillCoins can replace your main server economy
- 📊 **Dynamic Pricing** - Exponential cost scaling ensures balanced progression
- 🎁 **Configurable Rewards** - Earn SkillCoins by leveling skills, selling items, or admin rewards

## 💎 SkillCoins Economy System

**SkillCoins** is the heart of this rework - a sophisticated economy that gives players meaningful choices in how they progress their skills. Unlike traditional skill plugins where you're locked into grinding specific activities, SkillCoins lets you earn currency through any skill and spend it however you want.

### How Players Earn SkillCoins

1. **Level Up Skills** - Automatic rewards based on level with exponential scaling
2. **Sell Rare Items** - Convert valuable items like Nether Stars, Dragon Eggs, and Netherite into coins
3. **Complete Challenges** - Admins can reward coins for quests and achievements
4. **Receive Transfers** - Other players can send you coins using `/pay`
5. **Vault Integration** - Earn through any Vault-compatible economy plugin

### How Players Spend SkillCoins

#### 🛍️ Shop System (`/shop`)

The interactive shop menu provides multiple ways to spend your hard-earned coins:

**1. Buy Skill Levels**
- Purchase individual levels for any skill
- Exponential pricing: Level 1 = ~100 coins, Level 40 = ~900 coins
- Formula: `Cost = 100 * (1.5 ^ (currentLevel / 10))`
- Skip the grind for skills you don't enjoy

**2. Buy XP Boosts**
- 100 XP for 10 coins (flat rate)
- Perfect for when you're just shy of leveling up
- More economical than buying full levels at higher tiers

**3. Buy Custom Abilities**
- Unlock special abilities not available through normal leveling
- Requires meeting skill level prerequisites
- Examples: Growth Aura (5000 coins), Revival (10000 coins)

**4. Buy/Sell Items**
- Market for rare items with dynamic pricing
- Sell prices typically 40-50% of buy prices
- Cooldown system prevents exploitation

**5. Stat Reset**
- Complete character respec for 500 coins
- All skills reset to starting level
- Get refunded for a fresh start

#### 💸 Player Transfers (`/pay`)

Send SkillCoins to other players:
```
/pay <player> <amount>
```
- Validates amounts and checks balances
- Prevents self-payment and negative amounts
- Instant transfers with confirmation messages

### Admin Management

Powerful admin commands for economy management:

```bash
/sk coins balance [player]           # Check any player's balance
/sk coins add <player> <amount>      # Add coins to a player
/sk coins set <player> <amount>      # Set exact balance
/sk coins remove <player> <amount>   # Remove coins from a player
/shop debug                          # View all shop prices and config
/shop reload                         # Reload shop configuration
```

All commands have proper permissions (`auraskills.command.coins.*`) and validation.

### Technical Features

- **Persistent Storage** - Balances saved with player data (YAML or MySQL)
- **Automatic Migration** - Existing databases get `skill_coins` column automatically
- **Vault Provider** - Register as economy provider for compatibility with other plugins
- **Transaction Safety** - Prevents negative balances and validates all operations
- **Performance Optimized** - In-memory caching with async database writes
- **Fully Configurable** - Starting balances, Vault integration, shop pricing

### Configuration

Control the entire economy system in `config.yml`:

```yaml
skillcoins:
  enabled: true                    # Enable/disable SkillCoins
  starting_balance: 0.0           # Starting coins for new players
  vault_provider_enabled: true    # Register as Vault economy provider
  shop:
    enabled: true                  # Enable the shop system
```

Customize messages in `messages_en.yml`:
- Balance displays and transactions
- Purchase confirmations and errors
- Admin command feedback
- All support color codes

For detailed configuration of shop pricing, item lists, and abilities, see `shop_config.yml`.

---

## 📚 Original AuraSkills Features

Beyond the SkillCoins economy, this plugin maintains all the classic AuraSkills features:

- **Skills** - 11 default skills: Farming, Foraging, Mining, Fishing, Excavation, Archery, Defense, Fighting, Agility, Enchanting, and Alchemy
- **Stats** - Get player buffs like increased health, damage, and speed
- **Abilities** - Passive and active abilities that add gameplay mechanics
- **Mana System** - Resource management for powerful abilities
- **Menus** - Fully-configurable inventory GUIs
- **Rewards** - Customizable rewards for leveling skills
- **Loot Tables** - Custom drops for fishing, blocks, and mobs
- **Leaderboards** - Compete with other players

See the [wiki documentation](wiki/) for complete details on all features.

## 🔧 Building from Source

WDP AuraSkills uses Gradle for dependencies and building.

#### Prerequisites

- Java 21 toolchain (API modules compile to Java 8 for compatibility)
- Git

#### Compiling

First, clone the WDP repository:

```bash
git clone https://github.com/wdp-2010/WDP-AuraSkills.git
cd WDP-AuraSkills/
```

Then build depending on your operating system:

**Linux / macOS**
```bash
./gradlew clean build
```

**Windows**
```bash
.\gradlew.bat clean build
```

The output jar can be found in `build/libs/AuraSkills-2.3.8.jar`.

#### Running Test Server

Test the plugin on a local Paper server:

```bash
./gradlew :bukkit:runServer
```

This downloads Paper 1.21.10 to `bukkit/run/` and starts a test server. First run requires EULA acceptance.

## 🔌 Developer API

WDP AuraSkills maintains full compatibility with the AuraSkills API, plus extends it with SkillCoins functionality.

### SkillCoins API

Interact with the economy system programmatically:

```java
// Get the plugin and SkillCoins manager
AuraSkills plugin = (AuraSkills) Bukkit.getPluginManager().getPlugin("AuraSkills");
SkillCoinsManager coinsManager = plugin.getSkillCoinsManager();

// Get a user
User user = plugin.getUser(player);

// Check balance
double balance = coinsManager.getBalance(user);

// Add coins (deposit)
coinsManager.deposit(user, 100.0);

// Remove coins (withdraw)
boolean success = coinsManager.withdraw(user, 50.0);

// Transfer between players
User sender = plugin.getUser(senderPlayer);
User receiver = plugin.getUser(receiverPlayer);
boolean transferred = coinsManager.transfer(sender, receiver, 25.0);
```

### Vault Integration

If `vault_provider_enabled: true`, SkillCoins registers as a Vault economy provider:

```java
import net.milkbowl.vault.economy.Economy;

// Get Vault economy (will be SkillCoins)
RegisteredServiceProvider<Economy> rsp = 
    getServer().getServicesManager().getRegistration(Economy.class);
Economy economy = rsp.getProvider();

// Use standard Vault methods
double balance = economy.getBalance(player);
EconomyResponse response = economy.depositPlayer(player, 100.0);
```

### Core AuraSkills API

For skills, stats, abilities, and other core features, see the [API documentation](wiki/api.md).

**Dependency Setup**

This rework uses the same namespace (`dev.aurelium.auraskills`) so existing plugins remain compatible:

**Maven**
```xml
<dependency>
    <groupId>dev.aurelium</groupId>
    <artifactId>auraskills-api-bukkit</artifactId>
    <version>2.3.8</version>
    <scope>provided</scope>
</dependency>
```

**Gradle (Kotlin DSL)**
```kotlin
dependencies { 
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.3.8")
}
```

## 📖 Documentation

- **[SkillCoins System Guide](SKILLCOINS_SYSTEM.md)** - Complete SkillCoins economy documentation
- **[Shop System Guide](SHOP_SYSTEM.md)** - Shop menu and pricing details
- **[Developer Guide](DEVELOPER_GUIDE.md)** - Contributing and development setup
- **[Wiki](wiki/)** - Full feature documentation
- **[API Reference](wiki/api.md)** - Developer API documentation

## 🤝 Contributing

We welcome contributions! This is a community-driven fork maintained for WDPServer. Please read the [contributing guide](CONTRIBUTING.md) for development setup and guidelines before submitting pull requests.

## 📜 License & Credits

This project is a fork of [AuraSkills](https://github.com/Archy-X/AuraSkills) by Archy-X. We maintain the original license and give full credit to the original developers.

**Original Project:** AuraSkills (formerly Aurelium Skills)  
**Original Author:** Archy-X  
**WDP Rework:** Enhanced for WDPServer with SkillCoins economy  
**Server:** `play.wdpserver.com`

See [LICENSE.md](LICENSE.md) for full license details.
