---
description: Complete guide to the SkillCoins economy system
---

# SkillCoins Economy

**SkillCoins** is the integrated currency system in WDP AuraSkills that powers the entire progression economy. Unlike external economy plugins, SkillCoins is deeply integrated with the skills system, providing seamless transactions, persistent storage, and powerful admin tools.

> **For WDPServer Players:** This economy system is exclusive to our custom AuraSkills rework. Join us at `play.wdpserver.com` to experience it!

## Overview

SkillCoins transforms AuraSkills from a traditional skill plugin into a full RPG economy where:
- Players earn coins by leveling skills and selling rare items
- Coins can be spent on skill levels, XP, abilities, and items in the shop
- Player-to-player transfers enable trading and cooperation
- Admins have full control over balances and economy settings
- Vault integration allows use with other economy-dependent plugins

## For Players

### Checking Your Balance

View your current SkillCoins balance:
- Open the shop menu (`/shop`) - your balance appears in the top-left corner
- Use `/sk coins balance` if you have permission
- Check via PlaceholderAPI: `%auraskills_skillcoins%`

### Earning SkillCoins

#### 1. **Leveling Skills** (Primary Method)

Every time you level up any skill, you automatically earn SkillCoins! The amount scales with your level:

**Formula:** `Base Reward × (1.1 ^ (level / 10))`

**Example Rewards (with 100 base reward):**
- Level 1 → 2: 100 coins
- Level 10 → 11: 110 coins
- Level 20 → 21: 121 coins
- Level 50 → 51: 161 coins
- Level 100 → 101: 259 coins

**Configuration:** Server admins set the base reward in `shop_config.yml`. Higher levels give exponentially more coins to reward dedication.

#### 2. **Selling Items** (Quick Cash)

Access the shop (`/shop`) and navigate to the **Sell Items** section. Sell rare items you've collected:

**Legendary Tier:**
- Dragon Egg: 10,000 coins
- Nether Star: 5,000 coins
- Elytra: 3,000 coins

**Epic Tier:**
- Netherite Ingot: 500 coins
- Trident: 2,000 coins
- Totem of Undying: 1,500 coins

**Rare Tier:**
- Enchanted Golden Apple: 200 coins
- Diamond: 50 coins
- Emerald: 100 coins

**Valuable Tier:**
- Gold Ingot: 10 coins
- Iron Ingot: 5 coins
- Lapis Lazuli: 3 coins

**Special Tier:**
- Music Discs: 50-100 coins each
- Specialized items with unique values

**Cooldown System:** Some items have selling cooldowns to prevent exploitation (e.g., Dragon Egg has a 24-hour cooldown).

#### 3. **Receiving from Other Players**

Players can send you SkillCoins using:
```
/pay YourUsername <amount>
```

You'll receive a confirmation message when coins arrive.

#### 4. **Admin Rewards**

Server admins can award SkillCoins for:
- Completing quests or challenges
- Winning events or competitions
- Contributing to the community
- Special achievements

### Spending SkillCoins

#### The Shop System (`/shop`)

Open an interactive menu with multiple shopping categories:

**1. Buy Skill Levels**
- Purchase individual levels for any skill
- Exponential pricing ensures higher levels are more expensive
- **Cost Formula:** `100 × (1.5 ^ (currentLevel / 10))`

**Example Costs:**
- Level 0 → 1: 100 coins
- Level 10 → 11: 173 coins
- Level 20 → 21: 300 coins
- Level 30 → 31: 520 coins
- Level 40 → 41: 900 coins

**Strategy Tip:** It's more economical to grind lower levels and buy higher ones!

**2. Buy XP Boosts**
- Purchase 100 XP for any skill
- **Fixed Price:** 10 coins per 100 XP
- Perfect for when you're just shy of leveling up
- More cost-effective than buying levels at lower tiers

**3. Buy Custom Abilities**
- Unlock special abilities not available through normal progression
- Each ability has skill level requirements
- **Examples:**
  - **Growth Aura** (5,000 coins): Requires Farming 50, crops grow faster around you
  - **Revival** (10,000 coins): Requires Healing 75, chance to revive with 50% health

**4. Buy Items from Market**
- Purchase rare items if you need them
- Typically 2-3× the sell price
- Optional stock limits (configurable by admins)

**5. Stat Reset**
- Complete character respec
- **Cost:** 500 coins
- All skills reset to starting level (usually 0)
- All XP removed from all skills
- **Use Case:** Realize you want a different build? Reset and start fresh!

#### Player-to-Player Transfers

Send SkillCoins to other players:

```
/pay <player> <amount>
```

**Features:**
- Instant transfers with confirmation messages
- Both sender and receiver get notifications
- Prevents sending to yourself
- Validates amounts (must be positive)
- Checks your balance before sending

**Example:**
```
/pay Steve 500
```
You: "You sent 500 SkillCoins to Steve"  
Steve: "You received 500 SkillCoins from [YourName]"

**Use Cases:**
- Pay players for services or items
- Help friends get started
- Trade coins for in-game favors
- Pool resources for group purchases

### Tips for Players

1. **Focus on High-Value Skills**: Some skills are easier to level than others. Find your efficient grind!
2. **Sell Smart**: Check item prices before farming - some items are worth more per time invested
3. **Balance Grinding and Buying**: Use coins to skip skills you don't enjoy, grind the ones you do
4. **Watch for Cooldowns**: High-value items have selling cooldowns, plan accordingly
5. **Don't Rush Resets**: 500 coins is significant - make sure you really want to respec
6. **Save for Abilities**: Custom abilities are expensive but game-changing

## For Admins

### Commands

All admin commands require appropriate permissions (`auraskills.command.coins.*`).

#### Balance Management

**Check Any Player's Balance:**
```
/sk coins balance <player>
```
Shows the exact SkillCoins balance for any player.

**Add Coins:**
```
/sk coins add <player> <amount>
```
Adds the specified amount to a player's balance. Use for quest rewards, event prizes, or compensating bugs.

**Set Exact Balance:**
```
/sk coins set <player> <amount>
```
Sets a player's balance to an exact value. Useful for fixing issues or admin testing.

**Remove Coins:**
```
/sk coins remove <player> <amount>
```
Removes coins from a player's balance. Use for punishments or balance corrections.

#### Shop Management

**Debug Shop Configuration:**
```
/shop debug
```
Displays all current shop prices, settings, and configuration values. Essential for troubleshooting pricing issues.

**Reload Shop Config:**
```
/shop reload
```
Reloads `shop_config.yml` without restarting the server. Use after making configuration changes.

### Configuration

#### Main Config (`config.yml`)

```yaml
skillcoins:
  # Enable/disable the entire SkillCoins system
  enabled: true
  
  # Starting balance for new players
  # Set to 0 for no starting coins, or give a welcome bonus
  starting_balance: 0.0
  
  # Register as a Vault economy provider
  # Allows other plugins to interact with SkillCoins through Vault
  vault_provider_enabled: true
  
  # Shop system toggle
  shop:
    enabled: true
```

**Options Explained:**
- `enabled`: Master switch for SkillCoins. Set to `false` to disable economy entirely.
- `starting_balance`: Give new players a head start. Recommended: 0-1000 coins.
- `vault_provider_enabled`: Enable Vault integration for compatibility with shop plugins, permission plugins, etc.
- `shop.enabled`: Toggle the `/shop` command and menu system.

#### Shop Config (`shop_config.yml`)

Complete configuration for shop pricing, sellable items, and purchasable abilities:

```yaml
# Level Purchase Costs
level_costs:
  base: 100              # Starting cost for level 1
  multiplier: 1.5        # Exponential growth factor
  
  # Per-skill cost overrides (optional)
  skill_overrides:
    farming: 80          # Make farming cheaper
    combat: 150          # Make combat more expensive

# Level Rewards (earned when leveling up)
level_rewards:
  base: 100              # Base coins per level
  multiplier: 1.1        # Growth per 10 levels

# Sellable Items
sellable_items:
  dragon_egg:
    material: DRAGON_EGG
    price: 10000
    cooldown_hours: 24   # Can only sell once per day
    tier: legendary
    
  nether_star:
    material: NETHER_STAR
    price: 5000
    cooldown_hours: 0    # No cooldown
    tier: legendary

# Buyable Items (Market)
buyable_items:
  diamond:
    material: DIAMOND
    price: 150           # 3x the sell price
    stock: -1            # -1 = unlimited

# Custom Abilities
buyable_abilities:
  growth_aura:
    cost: 5000
    required_skill: farming
    required_level: 50
    description: "Crops grow faster around you"
```

**Configuration Tips:**
1. **Balance is Key**: Test pricing with actual players before going live
2. **Reward Grinding**: Make sure leveling skills is the primary income source
3. **Don't Overvalue Selling**: Item selling should be supplementary, not primary income
4. **Consider Cooldowns**: High-value items should have cooldowns to prevent farming exploits
5. **Adjust for Your Economy**: Scale prices based on your server's progression speed

#### Messages (`messages_en.yml`)

All SkillCoins messages support color codes and placeholders:

```yaml
commands:
  pay:
    sent: "<gray>You sent <gold>{amount} SkillCoins <gray>to <aqua>{player}"
    received: "<gray>You received <gold>{amount} SkillCoins <gray>from <aqua>{player}"
    insufficient_funds: "<red>You don't have enough SkillCoins! <gray>(Balance: <gold>{balance}<gray>)"
    invalid_amount: "<red>Amount must be positive!"
    cannot_pay_self: "<red>You cannot pay yourself!"
    target_not_found: "<red>Target player not found!"
    
  coins:
    balance: "<gray>{player}'s balance: <gold>{balance} SkillCoins"
    add:
      added: "<gray>Added <gold>{amount} SkillCoins <gray>to <aqua>{player}<gray>. New balance: <gold>{balance}"
    set:
      set: "<gray>Set <aqua>{player}<gray>'s balance to <gold>{amount} SkillCoins"
    remove:
      removed: "<gray>Removed <gold>{amount} SkillCoins <gray>from <aqua>{player}<gray>. New balance: <gold>{balance}"
      
  shop:
    insufficient_funds: "<red>You don't have enough SkillCoins for this purchase!"
    purchase_success: "<green>Successfully purchased {skill} level {level} for <gold>{cost} SkillCoins<green>!"
    xp_purchase_success: "<green>Successfully purchased {xp_amount} XP for {skill} for <gold>{cost} SkillCoins<green>!"
    purchase_failed: "<red>Purchase failed! Please try again."
    stat_reset_success: "<green>Successfully reset all skills! Your skill coins have been refunded."
```

**Available Placeholders:**
- `{amount}` - Coin amount
- `{player}` - Player name
- `{balance}` - Current balance
- `{skill}` - Skill name
- `{level}` - Level number
- `{cost}` - Purchase cost
- `{xp_amount}` - XP amount

### Permissions

```
auraskills.command.shop           # Access /shop menu (default: true)
auraskills.command.pay            # Use /pay command (default: true)
auraskills.command.coins.balance  # Check any player's balance
auraskills.command.coins.add      # Add coins to players
auraskills.command.coins.set      # Set player balances
auraskills.command.coins.remove   # Remove coins from players
```

**Recommended Setup:**
- Give all players `shop` and `pay` permissions
- Restrict `coins.*` permissions to admins only
- Consider separate permission for `coins.balance` for moderators

### Storage & Performance

#### Storage Options

SkillCoins balances are stored alongside player skill data:

**YAML Storage** (`playerdata/<uuid>.yml`):
```yaml
skill_coins: 1234.56
```

**SQL Storage** (MySQL/MariaDB):
- Automatic column `skill_coins DOUBLE` added to `auraskills_users` table
- Migration runs on first startup
- No manual database changes required

#### Performance Characteristics

- **In-Memory Caching**: Balances loaded with user data, no extra queries
- **Async Writes**: Database operations happen asynchronously
- **No Additional Load**: Uses existing storage infrastructure
- **Transaction Safety**: Prevents race conditions and negative balances

#### Backup Recommendations

Since SkillCoins uses the same storage as skills:
- Your existing backup strategy covers coin balances
- SQL: Regular database dumps
- YAML: Backup `playerdata/` directory
- Consider more frequent backups if running a large economy

### Vault Integration

When `vault_provider_enabled: true`, SkillCoins registers as a Vault economy provider.

**What This Enables:**
- Shop plugins (ChestShop, EssentialsX, etc.) can use SkillCoins
- Permission plugins can charge SkillCoins for rank purchases
- Any Vault-compatible plugin can interact with the economy

**Setup:**
1. Install Vault plugin
2. Set `vault_provider_enabled: true` in config.yml
3. Restart server
4. SkillCoins auto-registers as economy provider

**Priority:** If multiple economy plugins are installed, SkillCoins registers but may not be the default. Configure Vault's priority settings if needed.

**Testing:**
```
/vault-info
```
Should show SkillCoins as registered economy provider.

### Troubleshooting

#### Players Can't Access Shop
- Check permission: `auraskills.command.shop`
- Verify `skillcoins.enabled: true` in config.yml
- Check console for errors on startup

#### Transactions Failing
- Check player balance is sufficient
- Verify amounts are positive numbers
- Check console logs for errors
- Try `/shop reload` to refresh config

#### Vault Not Working
- Ensure Vault plugin is installed
- Check `vault_provider_enabled: true`
- Restart server after config changes
- Use `/vault-info` to verify registration

#### Balances Reset/Lost
- Check storage type matches configuration
- For SQL: Verify database connection
- For YAML: Check `playerdata/` directory exists
- Review server logs for storage errors

#### Wrong Prices
- Use `/shop debug` to see current pricing
- Edit `shop_config.yml` for custom prices
- Use `/shop reload` after changes
- Verify formula matches expectations

## For Developers

### API Integration

SkillCoins provides a full API for plugin developers:

```java
// Get the SkillCoins manager
AuraSkills plugin = (AuraSkills) Bukkit.getPluginManager().getPlugin("AuraSkills");
SkillCoinsManager manager = plugin.getSkillCoinsManager();

// Get a user
User user = plugin.getUser(player);

// Check balance
double balance = manager.getBalance(user);

// Add coins
manager.deposit(user, 100.0);

// Remove coins (returns false if insufficient)
boolean success = manager.withdraw(user, 50.0);

// Transfer between users
User sender = plugin.getUser(senderPlayer);
User receiver = plugin.getUser(receiverPlayer);
boolean transferred = manager.transfer(sender, receiver, 25.0);
```

### Vault API

Use standard Vault methods:

```java
Economy economy = getServer().getServicesManager()
    .getRegistration(Economy.class)
    .getProvider();

economy.depositPlayer(player, 100.0);
economy.withdrawPlayer(player, 50.0);
double balance = economy.getBalance(player);
```

### Events

Listen for economy transactions (uses existing AuraSkills events):

```java
@EventHandler
public void onSkillLevel(SkillLevelUpEvent event) {
    // Player just earned coins from leveling
    User user = event.getUser();
    double coins = coinsManager.getBalance(user);
}
```

### Database Schema

For SQL storage, the schema extension:

```sql
ALTER TABLE auraskills_users
    ADD COLUMN skill_coins DOUBLE NOT NULL DEFAULT 0.0;
```

Migration happens automatically on first load.

## Advanced Topics

### Economy Balancing

Tips for maintaining a healthy economy:

**1. Control Inflation**
- Don't give away free coins excessively
- Make earning coins require effort
- Remove coins from economy via sinks (shop purchases)

**2. Progression Curve**
- Early levels should be affordable (players can buy 1-2 levels)
- Mid levels should require some grinding
- High levels should be expensive but achievable

**3. Monitor the Economy**
- Track average player balances
- Watch for exploits or farming patterns
- Adjust prices based on player behavior

**4. Sinks vs Sources**
- **Sources:** Leveling, selling items
- **Sinks:** Buying levels, abilities, resets
- Balance should slightly favor sinks to prevent inflation

### Custom Rewards

Integrate SkillCoins with other plugins:

**Quest Rewards:**
```yaml
# In your quest plugin config
reward:
  commands:
    - "sk coins add %player% 500"
```

**Vote Rewards:**
```yaml
# In vote plugin
reward:
  - "sk coins add %player% 100"
```

**Achievement Rewards:**
```yaml
# In achievement plugin
on_complete:
  - "sk coins add %player% 1000"
```

### PlaceholderAPI

Use SkillCoins in other plugins:

```
%auraskills_skillcoins%        # Current balance
%auraskills_skillcoins_formatted%  # Formatted with commas
```

Use in scoreboards, chat prefixes, holograms, etc.

## Related Documentation

- [Shop System Guide](../SHOP_SYSTEM.md) - Detailed shop menu documentation
- [Main README](../README.md) - Overview and quick start
- [Developer Guide](../DEVELOPER_GUIDE.md) - Contributing to the project
- [Configuration](main-config/) - Full config.yml reference

---

**Join WDPServer at `play.wdpserver.com` to experience the SkillCoins economy!**
