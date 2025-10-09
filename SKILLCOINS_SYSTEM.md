# SkillCoins Economy System

## Overview
SkillCoins is a fully-featured economy system integrated into AuraSkills that can replace your main server economy. It provides a complete currency system with player-to-player transfers, admin management, and Vault compatibility.

## Features

### Core Features
- **Persistent Balance Storage**: Player balances are saved in the same storage system as skills (YAML or MySQL/MariaDB)
- **Automatic Migration**: SQL migration included to add skill_coins column to existing databases
- **Vault Integration**: Full Vault Economy provider for compatibility with other economy plugins
- **Configurable**: All features can be enabled/disabled and configured

### Commands

#### Player Commands
- `/pay <player> <amount>` - Send SkillCoins to another player
  - Permission: `auraskills.command.pay`
  - Validates amount, checks balance, prevents self-payment

#### Admin Commands (under `/sk coins`)
- `/sk coins balance [player]` - Check a player's balance
  - Permission: `auraskills.command.coins.balance`
  
- `/sk coins add <player> <amount>` - Add SkillCoins to a player
  - Permission: `auraskills.command.coins.add`
  
- `/sk coins set <player> <amount>` - Set a player's balance
  - Permission: `auraskills.command.coins.set`
  
- `/sk coins remove <player> <amount>` - Remove SkillCoins from a player
  - Permission: `auraskills.command.coins.remove`

## Configuration

### config.yml
```yaml
skillcoins:
  # Enable/disable the SkillCoins system
  enabled: true
  
  # Starting balance for new players
  starting_balance: 0.0
  
  # Register as a Vault economy provider
  # This allows other plugins to use SkillCoins through Vault
  vault_provider_enabled: true
  
  # Shop system (prepared for future implementation)
  shop:
    enabled: true
```

### Messages (messages_en.yml)
All SkillCoins messages are fully customizable and support color codes:

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
```

## API Usage

### For Plugin Developers

#### Using the SkillCoins Manager
```java
AuraSkills plugin = (AuraSkills) Bukkit.getPluginManager().getPlugin("AuraSkills");
SkillCoinsManager coinsManager = plugin.getSkillCoinsManager();

// Get a user
User user = plugin.getUser(player);

// Check balance
double balance = coinsManager.getBalance(user);

// Add coins
coinsManager.deposit(user, 100.0);

// Remove coins
boolean success = coinsManager.withdraw(user, 50.0);

// Transfer between players
User sender = plugin.getUser(senderPlayer);
User receiver = plugin.getUser(receiverPlayer);
boolean transferred = coinsManager.transfer(sender, receiver, 25.0);
```

#### Using Vault Integration
If `vault_provider_enabled: true` in config:

```java
import net.milkbowl.vault.economy.Economy;

// Get Vault economy
RegisteredServiceProvider<Economy> rsp = 
    getServer().getServicesManager().getRegistration(Economy.class);
Economy economy = rsp.getProvider(); // This will be SkillCoins

// Use standard Vault methods
double balance = economy.getBalance(player);
EconomyResponse response = economy.depositPlayer(player, 100.0);
```

## Storage

### File Storage (YAML)
Coin balances are stored in player files:
```yaml
# playerdata/<uuid>.yml
skill_coins: 1234.56
```

### SQL Storage
A new column `skill_coins` is automatically added to the `users` table:
```sql
ALTER TABLE auraskills_users
    ADD COLUMN skill_coins DOUBLE
        NOT NULL
        DEFAULT 0.0;
```

## Integration with Other Plugins

With Vault integration enabled, SkillCoins can be used by:
- Shop plugins (ChestShop, QuickShop, etc.)
- Permission plugins for rank purchases
- Any plugin that supports Vault economy

## Future Enhancements

The system is designed to support future additions:
- **Shop GUI**: Buy experience and items with SkillCoins
- **Rewards**: Earn SkillCoins from leveling up skills
- **Trading**: Enhanced player-to-player trading
- **Banks**: Store coins securely
- **Interest**: Passive coin generation

## Technical Details

### Architecture
- **SkillCoinsManager**: Core transaction handler in common module
- **SkillCoinsEconomyProvider**: Vault Economy implementation in Bukkit module
- **User class**: Extended with coin balance field and methods
- **Storage**: Integrated with existing FileStorageProvider and SqlStorageProvider
- **Commands**: Standard ACF command structure with validation

### Security
- All transactions validate amounts (positive, sufficient balance)
- Player-to-player transfers check both sender and receiver
- Admin commands require proper permissions
- Balances cannot go negative (enforced at the User level)

### Performance
- In-memory balance storage (loaded with user data)
- Async database writes (same as skill data)
- No additional queries for balance checks
- Efficient transfer operations (single transaction)

## Support

For issues or questions about the SkillCoins system:
1. Check this documentation
2. Review the config.yml options
3. Check the plugin logs for any errors
4. Report issues on the GitHub repository
