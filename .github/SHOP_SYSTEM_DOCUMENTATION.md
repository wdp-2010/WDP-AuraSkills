# AuraSkills Shop System - Complete Documentation for AI Agents

## Project Context

**Repository**: WDP-AuraSkills (fork of AuraSkills 2.3.8)  
**Module Architecture**: Multi-module Gradle project (api, api-bukkit, common, bukkit)  
**Build System**: Gradle 8.x with shadow plugin for shaded dependencies  
**Java Version**: API modules use Java 8, implementation uses Java 21  
**Package Namespace**: `dev.aurelium.auraskills.*`

## System Overview

The Shop System is a comprehensive economy feature built on top of the SkillCoins currency system. It provides three main functionalities:
1. **Sell Rare Items** - Convert valuable items into SkillCoins with cooldown protection
2. **Buy Skill Levels** - Purchase levels for any skill using SkillCoins
3. **Buy/Sell Special Abilities** - Trade rare abilities with requirement checks

### Core Design Principles
- ✅ **Configuration-Driven** - Zero hardcoded values, everything in YAML
- ✅ **Balanced Economy** - Exponential scaling, cooldowns, and requirement checks
- ✅ **Multi-Language Support** - All messages use Locale system
- ✅ **Module Separation** - Clean separation between common (logic) and bukkit (UI)

## File Structure

```
common/
├── src/main/java/dev/aurelium/auraskills/common/
│   ├── economy/
│   │   └── SkillPointsShop.java          # Core shop logic
│   ├── message/type/
│   │   └── CommandMessage.java           # Message enum keys
│   └── user/
│       └── User.java                     # User model with skillCoins
├── src/main/resources/
│   ├── shop_config.yml                   # Shop configuration
│   └── messages/messages_en.yml          # Localized messages

bukkit/
├── src/main/java/dev/aurelium/auraskills/bukkit/
│   ├── commands/
│   │   └── ShopCommand.java              # /shop command + /shop debug
│   ├── menus/
│   │   ├── ShopMenu.java                 # Main shop menu builder
│   │   └── MenuRegistrar.java            # Menu registration
│   └── managers/
│       └── SkillCoinsManager.java        # Transaction manager
└── src/main/resources/menus/
    └── shop.yml                          # Menu layout config
```

## Architecture Deep Dive

### 1. SkillPointsShop Class (`common/economy/SkillPointsShop.java`)

**Purpose**: Core business logic for all shop transactions

**Key Methods**:
```java
// Configuration loading
public void loadConfiguration()  // Loads shop_config.yml via ConfigurateLoader
private void loadSellableItems(ConfigurationNode node)
private void loadBuyableItems(ConfigurationNode node)
private void loadBuyableAbilities(ConfigurationNode node)

// Level purchasing
public double calculateLevelCost(Skill skill, int currentLevel)
public boolean purchaseLevel(User user, Skill skill)

// Item selling with cooldown
public SellResult sellItem(User user, String material, int amount)
public boolean canSellItem(User user, String material)  // Checks cooldown
public long getSellCooldownRemaining(User user, String material)

// Item buying
public BuyResult buyItem(User user, String material, int amount)

// Ability transactions
public AbilityPurchaseResult purchaseAbility(User user, String abilityKey)

// Getters for menu display
public Map<String, Double> getSellableItems()
public Map<String, BuyableItem> getBuyableItems()
public Map<String, BuyableAbility> getBuyableAbilities()
```

**Inner Classes**:
- `SellResult` - Contains success status, amount, total price, error message, cooldown info
- `BuyResult` - Contains success status, amount, total price, error message
- `BuyableItem` - Stores material, buy price, display info
- `BuyableAbility` - Stores ability key, cost, skill requirements

**Cooldown System**:
- Stored in `User.abilityData` map with key format: `"shop_sell_cooldown_{MATERIAL}"`
- Cooldown duration configurable per item in `shop_config.yml`
- Checked before every sell transaction

### 2. ShopMenu Class (`bukkit/menus/ShopMenu.java`)

**Purpose**: Slate menu builder for shop GUI

**Menu Structure**:
- Uses Slate library (`plugin.getSlate()`)
- Template system for dynamic item generation
- Context-based rendering (Skill contexts for level buying)
- onClick handlers with transaction logic

**Key Templates**:
```java
menu.template("buy_level", Skill.class, template -> {
    // Dynamic skill level purchasing
    template.replace("skill", t -> t.value().getDisplayName(t.locale()));
    template.replace("cost", t -> calculateCost());
    template.onClick(c -> handlePurchase());
});

menu.item("sell_items", item -> {
    // Sell items button - opens sell submenu (future)
});

menu.item("buy_abilities", item -> {
    // Buy abilities button - opens ability submenu (future)
});
```

### 3. Configuration System

#### shop_config.yml Structure
```yaml
# Feature toggles
enabled: true
debug_mode: false

# Level purchasing
level_purchase:
  enabled: true
  base_cost: 100.0
  cost_multiplier: 1.5
  skill_costs: {}  # Skill-specific overrides

# Skill coins rewards (NEW)
level_rewards:
  enabled: true
  base_reward: 50.0
  reward_multiplier: 1.2
  skill_rewards: {}  # Skill-specific overrides

# Item selling (with cooldowns)
sell_items:
  enabled: true
  global_cooldown: 300  # 5 minutes in seconds
  items:
    DIAMOND:
      enabled: true
      price: 60.0
      cooldown: 300  # Per-item cooldown override
      display_name: "<aqua>Diamond"

# Item buying (NEW)
buy_items:
  enabled: true
  items:
    DIAMOND:
      enabled: true
      price: 150.0  # Typically higher than sell price
      stock: -1  # -1 = unlimited, >0 = limited stock
      display_name: "<aqua>Diamond"

# Buyable abilities
buyable_abilities:
  enabled: true
  abilities:
    growth_aura:
      enabled: true
      cost: 5000.0
      required_skill: "farming"
      required_level: 50
```

#### messages_en.yml Keys
```yaml
commands:
  shop:
    insufficient_funds: "<red>You don't have enough SkillCoins! <gray>(Balance: <gold>{balance}<gray>)"
    purchase_success: "<green>Successfully purchased {skill} level {level} for <gold>{cost} SkillCoins<green>!"
    purchase_failed: "<red>Purchase failed! Please try again."
    sell_success: "<green>Sold {amount}x {item} for <gold>{total} SkillCoins<green>! <gray>(New balance: <gold>{balance}<gray>)"
    sell_cooldown: "<red>You must wait {time} before selling {item} again!"
    buy_success: "<green>Bought {amount}x {item} for <gold>{total} SkillCoins<green>!"
    buy_out_of_stock: "<red>This item is out of stock!"
    ability_purchase_success: "<green>Successfully unlocked {ability} ability for <gold>{cost} SkillCoins<green>!"
    ability_insufficient_level: "<red>You need {skill} level {level} to purchase this ability!"
leveler:
  level_up: "{skill} Level Up"
  coins_reward: "<gold>+{amount} SkillCoins"  # NEW
```

## Implementation Guide for AI Agents

### Step 1: Understanding the Module System

**Critical Rules**:
1. **NO Bukkit APIs in `common/` module** - This breaks architecture
2. **Use ConfigurateLoader properly** - Pass TypeSerializerCollection to constructor
3. **Registry pattern** - All content uses `NamespacedId.fromDefault("key")`
4. **User API** - Use `user.getSkillLevel()`, `user.setSkillLevel()`, `user.getSkillCoins()`, `user.setSkillCoins()`

### Step 2: Adding New Shop Features

#### Adding a New Sellable Item
1. Add to `shop_config.yml`:
```yaml
sell_items:
  items:
    NEW_MATERIAL:
      enabled: true
      price: 100.0
      cooldown: 600  # 10 minutes
      display_name: "<gold>New Material"
      lore:
        - "<gray>Description"
```

2. No code changes needed - SkillPointsShop reads dynamically

#### Adding a New Message
1. Add to CommandMessage.java:
```java
SHOP_NEW_MESSAGE("shop.new_message");
```

2. Add to messages_en.yml:
```yaml
commands:
  shop:
    new_message: "<green>Your message with {placeholder}"
```

3. Use in code:
```java
String msg = plugin.getMsg(CommandMessage.SHOP_NEW_MESSAGE, locale);
msg = TextUtil.replace(msg, "{placeholder}", "value");
```

### Step 3: Working with Cooldowns

**Cooldown Storage**:
```java
// Store cooldown
user.getAbilityDataMap().put(
    "shop_sell_cooldown_" + material.toUpperCase(), 
    System.currentTimeMillis()
);

// Check cooldown
long lastSell = user.getAbilityData("shop_sell_cooldown_" + material).getInt("timestamp");
long elapsed = System.currentTimeMillis() - lastSell;
if (elapsed < cooldownMillis) {
    // Still on cooldown
}
```

### Step 4: Skill Coins Rewards on Level Up

**Location**: `bukkit/levelers/SkillLeveler.java` or equivalent

**Pattern**:
```java
// In level up handler
public void onLevelUp(User user, Skill skill, int newLevel) {
    // Calculate reward
    double baseReward = config.node("level_rewards", "base_reward").getDouble(50.0);
    double multiplier = config.node("level_rewards", "reward_multiplier").getDouble(1.2);
    double reward = baseReward * Math.pow(multiplier, newLevel / 10.0);
    
    // Give coins
    user.setSkillCoins(user.getSkillCoins() + reward);
    
    // Send message
    String msg = TextUtil.replace(
        plugin.getMsg(CommandMessage.LEVEL_UP_COINS_REWARD, locale),
        "{amount}", String.format("%.0f", reward)
    );
    player.sendMessage(msg);
}
```

### Step 5: Testing Checklist

**Before Committing**:
1. ✅ Run `./gradlew checkstyleMain checkstyleTest` - Must pass
2. ✅ Run `./gradlew build -x test` - Must compile
3. ✅ Check console for config load errors
4. ✅ Test in-game with `/shop debug`
5. ✅ Verify cooldowns work correctly
6. ✅ Test level up rewards display
7. ✅ Check balance updates in real-time

## Common Pitfalls for AI Agents

### ❌ DON'T DO THIS:
```java
// Using Bukkit in common module
import org.bukkit.Material;  // WRONG MODULE!

// Hardcoding values
double cost = 100.0;  // Should be from config!

// Wrong ConfigurateLoader constructor
new ConfigurateLoader(plugin);  // Missing TypeSerializerCollection!

// Using placeholder comments
// ...existing code...  // NEVER use in replace_string_in_file!

// Wrong User API
user.addSkillLevel(skill, 1);  // Method doesn't exist!
```

### ✅ DO THIS INSTEAD:
```java
// Keep Bukkit in bukkit module
// Use String for materials in common module

// Load from config
double cost = config.node("level_purchase", "base_cost").getDouble(100.0);

// Correct constructor
new ConfigurateLoader(plugin, TypeSerializerCollection.builder().build());

// Full code context in edits
// Show actual code, never use markers

// Correct User API
user.setSkillLevel(skill, currentLevel + 1);
```

## Debug Commands

**Usage**:
```bash
/shop debug          # Shows all config, prices, cooldowns, balance
/shop reload         # Reloads shop_config.yml
/sk coins add <player> <amount>  # Give coins for testing
```

**Debug Output Includes**:
- Level purchase settings (base cost, multiplier)
- Example costs for levels 0, 10, 20, 50, 100
- All sellable items with prices and cooldowns
- All buyable items with prices and stock
- All buyable abilities with requirements
- Player balance (if player runs command)
- Debug mode status

## Performance Considerations

1. **Config Caching** - Shop config loaded once per menu open, cached in memory
2. **Cooldown Storage** - Uses existing User.abilityData map (no extra queries)
3. **Async Transactions** - User data auto-saved async by StorageProvider
4. **Menu Rendering** - Slate library handles efficient item updates

## Future Enhancement Hooks

**Prepared but Not Implemented**:
- Sell items submenu (button exists, opens "coming soon")
- Buy items submenu (button exists, opens "coming soon")  
- Buy abilities submenu (button exists, opens "coming soon")
- Shop history tracking
- Transaction logs
- Item stock system (buy_items.stock field ready)
- Dynamic pricing (demand-based)

**Implementation Pattern**:
```java
menu.item("sell_items", item -> {
    item.onClick(c -> {
        // Instead of "coming soon":
        plugin.getSlate().openMenu(c.player(), "shop_sell");
    });
});
```

## Integration Points

### With Vault Economy
SkillCoins integrates with Vault, so shop purchases work with:
- ChestShop
- QuickShop
- Essentials economy commands
- Any Vault-compatible plugin

### With Leaderboards
Shop earnings can affect leaderboards if configured:
- Track richest players
- Most items sold
- Biggest spenders

### With Rewards System
Level rewards tie into existing reward system:
- Configure in `rewards.yml`
- Money rewards now give SkillCoins
- Pattern: `type: money` → `amount: {coins}`

## Code Style Requirements

**Checkstyle Rules** (enforced at build time):
- 4 spaces indent (NO TABS)
- LF line endings only
- No trailing whitespace
- No multiple consecutive blank lines
- Import order: `*`, `javax`, `java` (blank line separated)
- Opening brace `{` on same line
- Closing brace `}` on new line (except else/catch)
- One top-level class per file

**Commit Message Format**:
```
Subject: Imperative mood, max 72 chars, capitalized, no period
Add shop cooldown system for item selling

Body: Explain what and why, wrap at 72 chars
- Prevents exploiting sell system
- Configurable per-item cooldowns
- Stored in User.abilityData map
```

## Troubleshooting Guide

### Shop Menu Won't Open
1. Check `MenuFileManager.MENU_NAMES` includes "shop"
2. Verify `shop.yml` exists in resources/menus/
3. Check console for YAML parse errors
4. Run `/shop debug` to test backend

### Items Not Selling
1. Check `shop_config.yml` has item enabled
2. Verify material name matches Bukkit Material enum
3. Check cooldown hasn't blocked transaction
4. Look for insufficient inventory space

### Level Purchase Fails
1. Verify sufficient SkillCoins balance
2. Check skill is enabled in skills.yml
3. Ensure level_purchase.enabled: true
4. Check for max level cap

### Cooldowns Not Working
1. Verify cooldown stored in User.abilityData
2. Check System.currentTimeMillis() timestamp
3. Test with `/shop debug` (shows cooldown status)
4. Ensure config uses seconds, code uses milliseconds

## API Examples for External Plugins

```java
// Get shop instance
AuraSkills plugin = (AuraSkills) Bukkit.getPluginManager().getPlugin("AuraSkills");
// Note: SkillPointsShop is created per-operation, config-driven

// Give coins on custom event
User user = plugin.getUser(player);
user.setSkillCoins(user.getSkillCoins() + 100.0);

// Check if player can afford custom purchase
double balance = user.getSkillCoins();
if (balance >= customPrice) {
    user.setSkillCoins(balance - customPrice);
    // Give custom item/service
}

// Track sell cooldown for custom item
long lastSell = 0;
Object data = user.getAbilityData("shop_sell_cooldown_CUSTOM_ITEM");
if (data != null) {
    lastSell = ((Number) data).longValue();
}
long elapsed = System.currentTimeMillis() - lastSell;
if (elapsed >= 300000) { // 5 minutes
    // Allow sell
    user.getAbilityDataMap().put("shop_sell_cooldown_CUSTOM_ITEM", System.currentTimeMillis());
}
```

## Version Compatibility

**Current Version**: 2.3.8  
**Minecraft**: 1.20.x - 1.21.x  
**Java**: 8+ (API), 21 (Implementation)  
**Dependencies**:
- Spigot/Paper API 1.21.x
- Configurate 4.x (SpongePowered)
- Slate (menu library, shaded)
- ACF (Aikar's Command Framework, shaded)
- Adventure API (text components, shaded)

## Summary for AI Agents

**When modifying the shop system**:
1. Always load values from `shop_config.yml`
2. Use SkillPointsShop for business logic (common module)
3. Use ShopMenu for UI (bukkit module)
4. Store cooldowns in User.abilityData map
5. Add messages to CommandMessage enum + messages_en.yml
6. Test with `/shop debug` before committing
7. Run checkstyle to validate code style
8. Update this documentation when adding features

**Architecture Memory Map**:
- **common/economy** = Logic (no Bukkit)
- **bukkit/menus** = UI (Slate + Bukkit)
- **bukkit/commands** = Commands (ACF)
- **common/user** = Data model
- **common/config** = YAML loading (Configurate)

This system is production-ready, fully configurable, and extensible. Happy coding! 🚀
