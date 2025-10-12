# Shop Configuration Guide

## Overview

This guide covers all configuration options for the AuraSkills Shop System. The shop system is highly configurable, allowing server administrators to customize pricing, stock levels, purchase limits, and more.

## Configuration File Location

**Primary Config**: `plugins/AuraSkills/shop_config.yml`

The shop system uses a separate configuration file from the main AuraSkills config to keep shop-related settings organized and easily manageable.

## Global Settings

### Debug Mode
```yaml
debug_mode: false
```

**Purpose**: Enables detailed logging for troubleshooting
**Values**: `true` or `false`
**Default**: `false`

When enabled, the console will show:
- Transaction details and execution times
- Stock level changes and restock events  
- Configuration reload notifications
- Performance warnings for slow operations
- Detailed error information

## Stock Management

### Auto-Restock Configuration
```yaml
stock:
  restock_interval: 60    # Minutes between automatic restocks
  restock_amount: 10      # Default items to restock per cycle
```

**restock_interval**:
- **Purpose**: How often to automatically restock shop items
- **Values**: Number (minutes), `0` to disable
- **Default**: `60` (every hour)
- **Notes**: Set to `0` to disable automatic restocking

**restock_amount**:
- **Purpose**: Default number of items to add during restock
- **Values**: Positive integer
- **Default**: `10`
- **Notes**: Can be overridden per-item

## Sell Items Configuration

### Global Sell Settings
```yaml
sell_items:
  global_cooldown: 300    # Global cooldown in seconds
  items:
    # Individual item configurations...
```

**global_cooldown**:
- **Purpose**: Minimum time between any sell operations
- **Values**: Number (seconds), `0` to disable
- **Default**: `300` (5 minutes)

### Individual Sell Items
```yaml
sell_items:
  items:
    DIAMOND:
      price: 50.0           # Coins per item
      cooldown: 600         # Individual cooldown (seconds)
      max_amount: 5         # Max sellable per cooldown period
    
    EMERALD:
      price: 30.0
      cooldown: 300
      max_amount: 10
    
    GOLD_INGOT:
      price: 15.0
      cooldown: 180
      max_amount: 20
```

**Item Configuration Options**:

**price**:
- **Purpose**: Skill coins received per item sold
- **Values**: Positive decimal number
- **Required**: Yes

**cooldown**:
- **Purpose**: Time before this item can be sold again
- **Values**: Number (seconds), `0` for no cooldown
- **Default**: `0`

**max_amount**:
- **Purpose**: Maximum items sellable per cooldown period
- **Values**: Positive integer, `-1` for unlimited
- **Default**: `-1`

## Buy Items Configuration

### Buy Items Structure
```yaml
buy_items:
  items:
    GOLDEN_APPLE:
      display_name: "§6Golden Apple"
      amount: 1
      price: 100.0
      max_stock: 5
      restock_amount: 3
    
    ENCHANTED_GOLDEN_APPLE:
      display_name: "§6§lNotch Apple"
      amount: 1
      price: 500.0
      max_stock: 1
      restock_amount: 1
    
    DIAMOND_SWORD:
      display_name: "§bDiamond Sword"
      amount: 1
      price: 250.0
      max_stock: 3
      restock_amount: 2
```

**Item Configuration Options**:

**display_name**:
- **Purpose**: Name shown in shop menu
- **Values**: String with color codes
- **Required**: Yes
- **Format**: Use `§` color codes (e.g., `§6` for gold)

**amount**:
- **Purpose**: Number of items given per purchase
- **Values**: Positive integer
- **Required**: Yes

**price**:
- **Purpose**: Skill coins cost per purchase
- **Values**: Positive decimal number
- **Required**: Yes

**max_stock**:
- **Purpose**: Maximum items available in shop
- **Values**: Positive integer, `-1` for unlimited
- **Default**: `-1`

**restock_amount**:
- **Purpose**: Items added during restock cycles
- **Values**: Positive integer
- **Default**: Uses global `restock_amount`

## Level Purchase Configuration

### Basic Level Settings
```yaml
level_purchase:
  enabled: true             # Enable/disable level purchasing
  base_cost: 100.0         # Base cost for level calculations
  cost_multiplier: 1.5     # Cost increase per level
  max_purchasable_levels: 50   # Global level limit
```

**enabled**:
- **Purpose**: Master switch for level purchasing
- **Values**: `true` or `false`
- **Default**: `true`

**base_cost**:
- **Purpose**: Starting cost for level calculations
- **Values**: Positive decimal number
- **Default**: `100.0`

**cost_multiplier**:
- **Purpose**: How much cost increases per level
- **Values**: Decimal number ≥ 1.0
- **Default**: `1.5`
- **Formula**: `cost = base_cost × level × cost_multiplier`

**max_purchasable_levels**:
- **Purpose**: Maximum levels purchasable for any skill
- **Values**: Positive integer, `0` for unlimited
- **Default**: `50`

### Skill-Specific Costs
```yaml
level_purchase:
  skill_costs:
    farming: 80.0      # Cheaper than default
    mining: 120.0      # More expensive than default
    fighting: 150.0    # Combat skills cost more
    magic: 200.0       # Magical skills are premium
```

**Purpose**: Override base_cost for specific skills
**Format**: `skill_name: cost_value`
**Skill Names**: Use lowercase skill identifiers

### Skill-Specific Level Limits
```yaml
level_purchase:
  skill_max_levels:
    farming: 75        # Can purchase up to level 75
    fighting: 25       # Combat limited to level 25
    healing: 100       # Healing gets higher limit
    mining: 0          # 0 = uses global limit
```

**Purpose**: Set different level limits per skill
**Format**: `skill_name: max_level`
**Values**: 
- Positive integer: Specific limit for this skill
- `0`: Use global `max_purchasable_levels`

## Buyable Abilities Configuration

### Ability Structure
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

**Ability Configuration Options**:

**Ability Key Format**: `namespace/skill/ability_name`
- Use lowercase with underscores
- Namespace typically `auraskills`

**cost**:
- **Purpose**: Skill coins required to purchase
- **Values**: Positive decimal number
- **Required**: Yes

**required_skill**:
- **Purpose**: Skill that must be leveled
- **Values**: Lowercase skill name
- **Required**: Yes

**required_level**:
- **Purpose**: Minimum level needed in required skill
- **Values**: Positive integer
- **Required**: Yes

**display_name**:
- **Purpose**: Name shown in ability shop
- **Values**: String with color codes
- **Required**: Yes

**description**:
- **Purpose**: Lore lines describing the ability
- **Values**: List of strings with color codes
- **Required**: Yes
- **Format**: Each line is a separate list item

## Advanced Configuration

### Performance Tuning

For high-traffic servers, consider these optimizations:

```yaml
# Reduce restock frequency for better performance
stock:
  restock_interval: 120    # Every 2 hours instead of 1

# Increase cooldowns to reduce database writes
sell_items:
  global_cooldown: 600     # 10 minutes instead of 5

# Limit concurrent operations
max_concurrent_transactions: 50
```

### Economy Integration

```yaml
# Optional external economy integration
economy:
  vault_integration: false    # Use Vault for transactions
  conversion_rate: 1.0        # Rate between skill coins and vault economy
```

### Permission-Based Pricing

```yaml
# Different prices based on permissions (advanced)
pricing_tiers:
  vip:
    multiplier: 0.8    # 20% discount
    permission: "auraskills.shop.vip"
  
  premium:
    multiplier: 0.6    # 40% discount  
    permission: "auraskills.shop.premium"
```

## Configuration Examples

### Balanced Server Setup
```yaml
# Good for most servers
debug_mode: false

stock:
  restock_interval: 60
  restock_amount: 10

sell_items:
  global_cooldown: 300
  items:
    DIAMOND: {price: 50.0, cooldown: 600, max_amount: 5}
    EMERALD: {price: 30.0, cooldown: 300, max_amount: 10}
    GOLD_INGOT: {price: 15.0, cooldown: 180, max_amount: 20}

buy_items:
  items:
    GOLDEN_APPLE: {display_name: "§6Golden Apple", amount: 1, price: 100.0, max_stock: 5}
    DIAMOND_SWORD: {display_name: "§bDiamond Sword", amount: 1, price: 250.0, max_stock: 3}

level_purchase:
  enabled: true
  base_cost: 100.0
  cost_multiplier: 1.5
  max_purchasable_levels: 50
```

### High-Economy Server
```yaml
# For servers with lots of currency circulation
level_purchase:
  base_cost: 500.0      # Higher base cost
  cost_multiplier: 2.0  # Steeper progression
  max_purchasable_levels: 30  # Lower limits

buy_items:
  items:
    GOLDEN_APPLE: {price: 500.0, max_stock: 2}  # Expensive, rare items
    DIAMOND_SWORD: {price: 1500.0, max_stock: 1}
```

### Beginner-Friendly Server
```yaml
# Easier progression for new players
level_purchase:
  base_cost: 50.0       # Lower costs
  cost_multiplier: 1.2  # Gentler progression
  max_purchasable_levels: 75  # Higher limits

sell_items:
  global_cooldown: 120  # Shorter cooldowns
  items:
    DIAMOND: {price: 75.0, max_amount: 10}  # Better prices, higher limits
```

## Validation and Testing

### Configuration Validation

The plugin automatically validates configuration on startup:

**Common Validation Errors**:
```
Invalid price value: must be positive number
Unknown skill name: check spelling and case
Missing required field: display_name
Invalid YAML syntax: check indentation
```

### Testing Configuration Changes

1. **Backup Current Config**:
   ```bash
   cp shop_config.yml shop_config.yml.backup
   ```

2. **Enable Debug Mode**:
   ```yaml
   debug_mode: true
   ```

3. **Reload Configuration**:
   ```
   /auraskills reload
   ```

4. **Monitor Console**: Check for validation errors or warnings

5. **Test In-Game**: Verify changes work as expected

### Configuration Reload

Changes take effect immediately with:
```
/auraskills reload
```

**Note**: Some changes (like adding new items) may require players to reopen menus to see updates.

## Troubleshooting

### Common Configuration Issues

**Problem**: Items not appearing in shop
```yaml
# Solution: Check item key spelling and YAML formatting
buy_items:
  items:
    GOLDEN_APPLE:  # Correct: Valid material name
      display_name: "§6Golden Apple"
    # Wrong: GOLDEN_APPLES (with S)
```

**Problem**: Levels not purchasable
```yaml
# Solution: Ensure level purchasing is enabled
level_purchase:
  enabled: true  # Must be true
  max_purchasable_levels: 50  # Must be > 0
```

**Problem**: Stock not restocking
```yaml
# Solution: Check restock configuration
stock:
  restock_interval: 60  # Must be > 0
  
buy_items:
  items:
    ITEM_NAME:
      max_stock: 10  # Must be > 0 for restocking
      restock_amount: 5  # Must be > 0
```

### Debug Information

With debug mode enabled, check console for:
```
[AuraSkills] Shop configuration loaded: X sellable items, Y buyable items
[AuraSkills] Auto-restock started: Every X minutes
[AuraSkills] Configuration validation passed
```

### Performance Issues

If experiencing lag:

1. **Increase restock interval**:
   ```yaml
   restock_interval: 120  # Reduce frequency
   ```

2. **Reduce concurrent operations**:
   ```yaml
   global_cooldown: 600  # Increase cooldowns
   ```

3. **Monitor debug output**:
   ```
   [AuraSkills] Slow shop operation: purchase took 150ms
   ```

## Migration Guide

### Updating from Previous Versions

**Backup First**: Always backup configuration before updating

**Version 2.3.x to 2.4.x**:
- No breaking changes
- New features available immediately

**Version 2.2.x to 2.3.x**:
- Added stock management system
- Added level purchasing
- Configuration auto-migrates

### Custom Configuration Preservation

When updating, your custom settings are preserved:
1. Plugin generates new default configuration
2. Merges with existing user configuration  
3. Validates combined result
4. Reports any conflicts or issues

---

*For additional support, consult the main documentation or create an issue in the project repository.*