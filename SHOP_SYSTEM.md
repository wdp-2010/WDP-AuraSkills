# Skill Coins Shop System

## Overview

The Skill Coins Shop system allows players to purchase skill levels, XP, and stat resets using skill coins. This creates a balanced economy where players can progress through the skill system using earned currency.

## Features

### 1. **Buy Skill Levels**

- Purchase individual skill levels for any enabled skill
- Dynamic pricing that scales with current level
- **Formula**: `Cost = 100 * (1.5 ^ (currentLevel / 10))`
- Higher levels cost exponentially more to maintain balance

### 2. **Buy XP**

- Purchase 100 XP for any skill
- Fixed cost: 10 skill coins per 100 XP
- Great for boosting skills close to leveling up

### 3. **Stat Reset**

- Reset all skills to starting level (default 0)
- Removes all XP from all skills
- Fixed cost: 500 skill coins
- Useful for players who want to respec their character

## Commands

### `/shop`

- **Permission**: `auraskills.command.shop`
- **Description**: Opens the Skill Coins Shop menu
- **Usage**: Simply type `/shop` to open the interactive menu

## Menu Layout

The shop menu is organized into rows:

- **Row 1**: Header with balance display
- **Row 2**: Buy level items for each skill (Archery, Fighting, Defense, Endurance, etc.)
- **Row 3**: More buy level items (Excavation, Agility, Alchemy, Enchanting, etc.)
- **Row 4**: Buy 100 XP items for each skill
- **Row 5**: More buy XP items + Stat Reset
- **Row 6**: Close button

## Pricing System

### Level Purchase Pricing
The cost to purchase a level increases exponentially:
- **Level 1-10**: ~100-173 coins per level
- **Level 11-20**: ~173-300 coins per level
- **Level 21-30**: ~300-520 coins per level
- **Level 31-40**: ~520-900 coins per level
- etc.

This ensures that higher levels remain challenging and valuable.

### XP Purchase Pricing
- **100 XP = 10 coins** (flat rate)
- More economical for lower levels
- Still useful for getting that last bit of XP needed to level up

### Stat Reset
- **Cost: 500 coins**
- Complete character reset
- All skills return to starting level
- All XP removed

## Configuration

The shop is enabled by default with the `skillcoins` section in `config.yml`:

```yaml
skillcoins:
  enabled: true
  starting_balance: 0.0
  vault_provider_enabled: true
  shop:
    enabled: true
```

## Messages

All shop messages can be customized in `messages_en.yml`:

```yaml
commands:
  shop:
    insufficient_funds: "<red>You don't have enough SkillCoins for this purchase!"
    purchase_success: "<green>Successfully purchased {skill} level {level} for <gold>{cost} SkillCoins<green>!"
    xp_purchase_success: "<green>Successfully purchased {xp_amount} XP for {skill} for <gold>{cost} SkillCoins<green>!"
    purchase_failed: "<red>Purchase failed! Please try again."
    stat_reset_success: "<green>Successfully reset all skills! Your skill coins have been refunded."
```

## Balance System

Players can earn skill coins through:
- Manual admin commands (`/sk coins add`)
- Economy integrations (if Vault is enabled)
- Custom reward systems you configure
- Pay command (`/pay`) to transfer coins between players

## Technical Details

### Files Added
1. **SkillPointsShop.java** - Core shop logic and pricing calculations
2. **ShopMenu.java** - Interactive menu implementation
3. **ShopCommand.java** - `/shop` command handler
4. **shop.yml** - Menu configuration and layout
5. Message keys added to **messages_en.yml**

### Integration Points
- Registered in `MenuRegistrar.java`
- Registered in `CommandRegistrar.java`
- Uses existing `SkillCoinsManager` for transactions
- Hooks into skill level and XP systems

## Build Location

After building, the plugin JAR is located at:
- **Main**: `/build/libs/AuraSkills-2.3.8.jar`
- **Bukkit**: `/bukkit/build/libs/AuraSkills-2.3.8.jar`

## Permissions

- `auraskills.command.shop` - Access to `/shop` command (default: true for all players)

## Tips for Server Admins

1. **Adjust Starting Balance**: Set `skillcoins.starting_balance` to give new players some coins
2. **Integrate with Economy**: Enable `vault_provider_enabled` to allow buying/selling coins
3. **Custom Rewards**: Add skill coins as quest/achievement rewards
4. **Balance Monitoring**: Watch the economy and adjust pricing in `SkillPointsShop.java` if needed

## Future Enhancements

Potential additions:
- Configurable pricing in config.yml
- XP boost multipliers (2x XP for 1 hour)
- Ability unlocks
- Temporary stat buffs
- Cosmetic rewards
