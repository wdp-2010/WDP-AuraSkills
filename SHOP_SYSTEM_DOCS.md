# AuraSkills Shop System Documentation

## Overview

The AuraSkills Shop System is a comprehensive economy implementation that allows players to buy and sell items, purchase skill levels, and acquire abilities using Skill Coins. The system is designed with modularity, extensibility, and performance in mind.

## Architecture

### Core Components

#### 1. SkillPointsShop (`common/economy/SkillPointsShop.java`)
The central backend class that manages all shop operations:
- **Item Trading**: Buy/sell operations with stock management
- **Level Purchasing**: Direct skill level purchases with configurable limits
- **Ability Purchasing**: Unlock powerful abilities with prerequisites
- **Configuration Management**: Dynamic loading from `shop_config.yml`
- **Stock System**: Automatic restocking with configurable intervals
- **Cooldown Management**: Per-player, per-item sell cooldowns

#### 2. ShopManager (`bukkit/economy/ShopManager.java`)
Coordinates shop initialization and menu management:
- **Lifecycle Management**: Initialize shop after config loading
- **Menu Coordination**: Manages all shop menu instances
- **Auto-restock**: Starts automatic restocking systems

#### 3. Menu System (`bukkit/menus/`)
User interface components with consistent design:
- **MainShopMenu**: Central hub with category selection
- **ItemShopMenu**: Buy/sell items with stock tracking
- **AbilityShopMenu**: Purchase abilities with requirement validation
- **LevelShopMenu**: Direct skill level purchasing with categorized layout

### Data Flow

```
Player Action → Menu Event → SkillPointsShop Backend → User Data Update → Menu Refresh
```

1. **Player Interaction**: Click events in inventory menus
2. **Validation**: Check requirements, cooldowns, stock, balance
3. **Transaction**: Execute purchase/sale with appropriate updates
4. **Feedback**: Audio/visual feedback and menu refresh
5. **Persistence**: Auto-save user data changes

## Configuration System

### Main Configuration (`shop_config.yml`)

#### Global Settings
```yaml
debug_mode: false                    # Enable detailed logging
```

#### Stock Management
```yaml
stock:
  restock_interval: 60              # Auto-restock every 60 minutes (0 = disabled)
  restock_amount: 10               # Default restock amount per item
```

#### Sell Items Configuration
```yaml
sell_items:
  global_cooldown: 300              # Global sell cooldown in seconds
  items:
    DIAMOND:
      price: 50.0                   # Sell price per item
      cooldown: 600                 # Individual cooldown in seconds
      max_amount: 5                 # Max sellable per cooldown period
    EMERALD:
      price: 30.0
      cooldown: 300
      max_amount: 10
```

#### Buy Items Configuration
```yaml
buy_items:
  items:
    GOLDEN_APPLE:
      display_name: "§6Golden Apple"
      amount: 1                     # Items given per purchase
      price: 100.0                  # Cost in skill coins
      max_stock: 5                  # Maximum stock (-1 for unlimited)
      restock_amount: 3             # Amount restored per restock
    ENCHANTED_GOLDEN_APPLE:
      display_name: "§6§lNotch Apple"
      amount: 1
      price: 500.0
      max_stock: 1
      restock_amount: 1
```

#### Level Purchase Configuration
```yaml
level_purchase:
  enabled: true                     # Enable level purchasing
  base_cost: 100.0                 # Base cost calculation
  cost_multiplier: 1.5             # Cost increase per level
  max_purchasable_levels: 50       # Global maximum purchasable levels
  
  # Skill-specific base costs
  skill_costs:
    farming: 80.0
    mining: 120.0
    fighting: 150.0
  
  # Skill-specific level limits
  skill_max_levels:
    farming: 75        # Can only purchase up to level 75
    fighting: 25       # Combat skills limited to level 25
    healing: 100       # Healing unlimited (up to global max)
```

#### Buyable Abilities Configuration
```yaml
buyable_abilities:
  abilities:
    auraskills/farming/bountiful_harvest:
      cost: 1000.0
      required_skill: "farming"
      required_level: 25
      display_name: "§aBountiful Harvest"
      description:
        - "§7Chance to get extra crops"
        - "§7when harvesting fully grown plants"
        - "§eRequired: Farming Level 25"
    
    auraskills/mining/lucky_miner:
      cost: 1500.0
      required_skill: "mining"
      required_level: 30
      display_name: "§6Lucky Miner"
      description:
        - "§7Chance to find rare gems"
        - "§7while mining stone blocks"
        - "§eRequired: Mining Level 30"
```

## Menu Design Guidelines

### Visual Consistency

#### Color Schemes
- **Main Shop**: `§6` (Gold) - Central hub theme
- **Item Shop**: `§b` (Aqua) - Commerce/trading theme
- **Ability Shop**: `§d` (Light Purple) - Magic/abilities theme  
- **Level Shop**: `§e` (Yellow) - Experience/progression theme

#### Layout Standards
- **54-slot inventory** for all sub-menus (6x9 grid)
- **27-slot inventory** for main menu (3x9 grid)
- **Decorative borders** with colored glass panes
- **Consistent navigation** (back button slot 45, close button slot 53)
- **Balance display** prominently positioned
- **Category organization** with logical grouping

#### Typography Conventions
- **§l** for headers and important elements
- **§7** for secondary text and descriptions
- **§e** for prices and values
- **§a** for success states
- **§c** for error states
- **§7▸** for bullet points

### Level Shop Layout

The Level Shop uses a categorized approach for better UX:

```
┌─────────────────────────────────────────────────────────┐
│ [Border] [Border] [Border] [Balance] [Border] [Border]  │
│ [Border] [Border] [Border] [Border] [Border] [Border]   │
│ [Border] [Combat] [Combat] [Combat] [Border] [Border]   │
│ [Border] [Border] [Border] [Border] [Border] [Border]   │
│ [Border] [Gather] [Gather] [Info] [Prod] [Prod] [Prod] │
│ [Border] [Border] [Border] [Border] [Border] [Border]   │
│ [Border] [Magic] [Magic] [Magic] [Border] [Life] [Life] │
│ [Border] [Border] [Border] [Border] [Border] [Border]   │
│ [Back] [Border] [Help] [Border] [Refresh] [Border] [Close]│
└─────────────────────────────────────────────────────────┘
```

**Categories:**
- **Combat** (slots 10-12): Fighting, Archery, Defense
- **Gathering** (slots 19-21): Mining, Foraging, Fishing  
- **Production** (slots 23-25): Farming, Excavation, Forging
- **Magic** (slots 28-30): Alchemy, Enchanting, Sorcery
- **Life** (slots 32-34): Agility, Endurance, Healing

## API Usage

### For Plugin Developers

#### Accessing the Shop System
```java
// Get the shop instance
AuraSkills plugin = AuraSkills.getInstance();
SkillPointsShop shop = plugin.getShopManager().getShop();

// Check if user can afford an item
User user = plugin.getUser(player);
boolean canAfford = user.getSkillCoins() >= itemPrice;

// Purchase a level programmatically
SkillPointsShop.LevelPurchaseResult result = shop.purchaseLevel(user, Skills.FARMING);
if (result.success) {
    player.sendMessage("Level purchased successfully!");
}
```

#### Opening Shop Menus
```java
// Open main shop menu
plugin.getMainShopMenu().openMainMenu(player);

// Open specific shop sections
plugin.getShopManager().getLevelShopMenu().openLevelShop(player);
```

#### Custom Shop Extensions
```java
// Add custom sellable items
shop.addSellableItem("CUSTOM_ITEM", 75.0);

// Add custom buyable items
shop.addBuyableItem("CUSTOM_REWARD", "§aCustom Reward", 1, 200.0, 10);

// Monitor shop events
@EventHandler
public void onShopPurchase(InventoryClickEvent event) {
    if (event.getView().getTitle().contains("Shop")) {
        // Handle custom shop logic
    }
}
```

## Performance Considerations

### Optimization Features

#### 1. Lazy Loading
- Configuration loaded only when needed
- Menu population on-demand
- Skill calculations cached appropriately

#### 2. Efficient Data Structures
- `ConcurrentHashMap` for thread-safe operations
- Optimized lookup tables for skill mappings
- Minimal object creation in hot paths

#### 3. Background Operations
- Auto-restock runs asynchronously
- Non-blocking menu refreshes
- Batched database operations

#### 4. Memory Management
- Weak references for temporary data
- Automatic cleanup of expired cooldowns
- Efficient string interning for repeated text

### Monitoring and Debugging

#### Debug Mode Features
```yaml
debug_mode: true
```

Enables detailed logging:
- Transaction details and timing
- Stock level changes
- Configuration reload events
- Performance metrics
- Error stack traces

#### Performance Metrics
- Shop operation execution times
- Menu render performance
- Database query optimization
- Memory usage tracking

## Security and Validation

### Input Validation
- **Amount Bounds**: Prevent integer overflow/underflow
- **Price Validation**: Ensure positive values only
- **Permission Checks**: Verify player access rights
- **State Validation**: Check inventory space, requirements

### Anti-Exploitation
- **Cooldown Enforcement**: Prevent rapid-fire selling
- **Stock Limits**: Prevent unlimited item generation
- **Transaction Atomicity**: All-or-nothing operations
- **Audit Logging**: Track all significant transactions

### Error Handling
- **Graceful Degradation**: Continue operation despite minor errors
- **User Feedback**: Clear error messages
- **Recovery Mechanisms**: Automatic retry for transient failures
- **Failsafe Defaults**: Safe fallback values

## Extension Points

### Custom Shop Types
Extend the system with new shop categories:

1. **Create Menu Class**
```java
public class CustomShopMenu implements Listener {
    // Implement menu logic
}
```

2. **Register with ShopManager**
```java
// Add to ShopManager initialization
customShopMenu = new CustomShopMenu(plugin);
```

3. **Add Navigation Integration**
```java
// Update MainShopMenu with new category
```

### Custom Transaction Types
Add new types of purchases/sales:

1. **Extend SkillPointsShop**
2. **Add Configuration Support**
3. **Implement Validation Logic**
4. **Create Result Classes**

### Integration Hooks
- **Economy Plugins**: Vault integration for external economy
- **Permission Systems**: Hook into LuckPerms/GroupManager
- **Statistics Tracking**: Integration with PlaceholderAPI
- **External APIs**: REST endpoints for web integration

## Troubleshooting

### Common Issues

#### Menu Not Opening
```
Check console for errors
Verify player permissions
Ensure shop is initialized
Confirm configuration validity
```

#### Items Not Stocking
```
Check max_stock values in config
Verify restock_interval setting
Confirm auto-restock started
Check debug logs for restock events
```

#### Purchase Failures
```
Verify player balance
Check skill requirements
Confirm stock availability
Validate cooldown status
```

#### Performance Issues
```
Enable debug mode temporarily
Monitor console for slow operations
Check database connection health
Review configuration complexity
```

### Debug Commands

#### Shop Status
```
/auraskills shop debug
```
Shows current shop state, player balances, and system status.

#### Force Restock
```
/auraskills shop restock
```
Manually triggers restocking of all items.

#### Reload Configuration
```
/auraskills reload
```
Reloads shop configuration without restart.

## Future Enhancements

### Planned Features
- **Dynamic Pricing**: Supply and demand based pricing
- **Auction House**: Player-to-player trading
- **Shop NPCs**: Physical shop locations
- **Trade Routes**: Multi-step trading chains
- **Seasonal Sales**: Time-based discounts
- **Achievement Integration**: Unlock shops through progression

### API Expansion
- **Shop Events**: Comprehensive event system
- **External Integration**: REST API for web management
- **Plugin Hooks**: More extension points
- **Database Support**: Multiple storage backends

---

*This documentation is maintained with the AuraSkills Shop System. For the latest updates, see the project repository.*