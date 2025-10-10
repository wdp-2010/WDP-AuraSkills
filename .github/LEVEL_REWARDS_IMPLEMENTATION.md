# Level-Up SkillCoins Rewards Implementation

## Overview
Players now earn SkillCoins when leveling up skills. The reward amount scales exponentially based on the skill level, with per-skill override support.

## Configuration

### Location
`common/src/main/resources/shop_config.yml`

### Settings
```yaml
level_rewards:
  enabled: true                  # Enable/disable level-up rewards
  base_reward: 50.0             # Base coins awarded per level
  reward_multiplier: 1.2        # Exponential scaling factor
  
  skill_rewards:                # Per-skill overrides (optional)
    farming:
      base_reward: 60.0
      reward_multiplier: 1.15
    mining:
      base_reward: 55.0
      reward_multiplier: 1.18
```

### Calculation Formula
```
reward = baseReward × multiplier^(level / 10)
```

**Examples:**
- Level 1: 50 × 1.2^0.1 = ~51.84 coins
- Level 10: 50 × 1.2^1 = 60 coins
- Level 20: 50 × 1.2^2 = 72 coins
- Level 50: 50 × 1.2^5 = ~124.4 coins
- Level 100: 50 × 1.2^10 = ~309.6 coins

This exponential scaling rewards high-level progression without being exploitable at low levels.

## Implementation Details

### Modified Files

#### 1. LevelManager.java
**Location:** `common/src/main/java/dev/aurelium/auraskills/common/level/`

**Changes:**
- Added `calculateSkillCoinsReward(Skill skill, int level)` method
  - Dynamically loads `shop_config.yml` using ConfigurateLoader
  - Reads global settings and per-skill overrides
  - Returns calculated coins reward using exponential formula
  - Returns 0.0 if disabled or config error

- Modified `levelUpSkill(User user, Skill skill)` method
  - Calculates coins reward before processing custom rewards
  - Gives coins to player using `user.addSkillCoins(coinsReward)`
  - Passes reward amount to messenger via `setCoinsReward(coinsReward)`

**Code Flow:**
```
levelUpSkill() → calculateSkillCoinsReward() → user.addSkillCoins() → messenger.setCoinsReward()
```

#### 2. LevelUpMessenger.java
**Location:** `common/src/main/java/dev/aurelium/auraskills/common/level/`

**Changes:**
- Added field: `private double coinsReward = 0.0`
- Added setter: `setCoinsReward(double coinsReward)`
- Added method: `getCoinsRewardMessage()`
  - Formats coins reward with DecimalFormat("#") (no decimals)
  - Uses `LevelerFormat.COINS_REWARD` message template
  - Returns empty string if reward is 0 or less

- Modified `sendChatMessage()` method
  - Added `"coins_reward"` placeholder to message builder
  - Placeholder replaced with formatted coins reward message

**Message Integration:**
```java
builder.rawMessage(LevelerFormat.CHAT,
    "skill", displayName,
    "old", old + "",
    "new", level + "",
    "stat_level", getStatLevelMessage(),
    "ability_unlock", getAbilityUnlockMessage(),
    "ability_level_up", getAbilityLevelUpMessage(),
    "mana_ability_unlock", getManaAbilityUnlockMessage(),
    "mana_ability_level_up", getManaAbilityLevelUpMessage(),
    "money_reward", getMoneyRewardMessage(),
    "coins_reward", getCoinsRewardMessage());  // NEW
```

#### 3. LevelerFormat.java
**Location:** `common/src/main/java/dev/aurelium/auraskills/common/message/type/`

**Changes:**
- Added enum value: `COINS_REWARD`
- Maps to message path: `leveler_format.coins_reward`

**Complete Enum:**
```java
public enum LevelerFormat implements MessageKey {
    TITLE,
    SUBTITLE,
    CHAT,
    STAT_LEVEL,
    ABILITY_UNLOCK,
    ABILITY_LEVEL_UP,
    MANA_ABILITY_UNLOCK,
    MANA_ABILITY_LEVEL_UP,
    MONEY_REWARD,
    COINS_REWARD,        // NEW
    DESC_UPGRADE_VALUE,
    DESC_WRAP;
}
```

#### 4. global.yml
**Location:** `common/src/main/resources/messages/`

**Changes:**
- Added format template: `coins_reward: "\n  <gold>+{amount} SkillCoins"`
- Updated `chat` template to include `{coins_reward}` placeholder

**Chat Template:**
```yaml
leveler_format:
  chat: |-
    <dark_aqua><strikethrough>----------------------------------------
    <reset> <dark_aqua>{{leveler.skill_level_up}} <aqua><bold>{skill}</bold> <dark_gray>{old}➜<dark_aqua><bold>{new}</bold>
    
    <aqua> {{leveler.rewards}}:{stat_level}{ability_unlock}{ability_level_up}{mana_ability_unlock}{mana_ability_level_up}{money_reward}{coins_reward}<reset>
    <dark_aqua><strikethrough>----------------------------------------
```

**Format Template:**
```yaml
leveler_format:
  money_reward: "\n  <green>${amount}"
  coins_reward: "\n  <gold>+{amount} SkillCoins"  # NEW
```

## Player Experience

### In-Game Display
When a player levels up, they see:

```
----------------------------------------
 Skill Level Up! Mining 10➜11

 Rewards:
  +1❤ Health
  +50 SkillCoins                  ← NEW
----------------------------------------
```

The coins reward appears alongside other rewards (stat increases, ability unlocks, money rewards).

### Reward Visibility
- **Chat message:** Shows "+X SkillCoins" in gold color
- **Title/Subtitle:** Not displayed (follows same pattern as money rewards)
- **Balance update:** Instant - coins added before message sent
- **Scaling indicator:** Higher levels = more coins (visible progression)

## Balance Considerations

### Why Exponential Scaling?
1. **Low-level protection:** Levels 1-10 give ~50-60 coins (not exploitable)
2. **Mid-level reward:** Levels 20-50 give 72-124 coins (good progression)
3. **High-level incentive:** Levels 50-100 give 124-310 coins (rewards dedication)
4. **Smooth curve:** No sudden jumps, feels natural

### Preventing Exploitation
- **Configuration-based:** Admins can adjust base and multiplier
- **Per-skill tuning:** Easy skills can have lower rewards
- **Dynamic loading:** Changes apply without restart (first level-up)
- **No hardcoding:** All values in config, no magic numbers

### Recommended Settings
```yaml
# Conservative (slower economy)
base_reward: 30.0
reward_multiplier: 1.15

# Default (balanced)
base_reward: 50.0
reward_multiplier: 1.2

# Generous (faster economy)
base_reward: 75.0
reward_multiplier: 1.25
```

## Testing Checklist

### Functional Tests
- [ ] Level up from 1→2: Receive ~52 coins
- [ ] Level up from 10→11: Receive ~60 coins
- [ ] Level up from 50→51: Receive ~124 coins
- [ ] Disable rewards (`enabled: false`): Receive 0 coins
- [ ] Per-skill override: Farming gives different amount than Mining
- [ ] Multiple level-ups: Each gives correct coins

### Display Tests
- [ ] Chat message shows "+X SkillCoins" in gold
- [ ] Coins appear below other rewards (stat levels, abilities)
- [ ] Amount formatted without decimals (50, not 50.0)
- [ ] No message shown if reward is 0

### Edge Cases
- [ ] Config file missing: Defaults to no reward
- [ ] Invalid values: Logs warning, defaults to 0
- [ ] Negative values: Config validation should prevent
- [ ] Very high levels (100+): Calculation doesn't overflow

## Debug Commands

### Check Player Balance
```
/sk coins balance <player>
```

### Give Coins Manually (Testing)
```
/sk coins give <player> <amount>
```

### Reload Shop Config
```
/sk shop reload
```
Reloads `shop_config.yml` including level_rewards settings.

## Troubleshooting

### Problem: No coins awarded on level-up
**Possible Causes:**
1. `enabled: false` in shop_config.yml
2. Config file corrupted or missing
3. SkillCoins system not initialized

**Solution:**
1. Check console for warnings about shop_config.yml loading
2. Verify `level_rewards.enabled: true`
3. Use `/sk shop reload` to reload config
4. Check player balance before/after: `/sk coins balance <player>`

### Problem: Wrong amount awarded
**Possible Causes:**
1. Per-skill override active
2. Multiplier too high/low
3. Calculation error (unlikely)

**Solution:**
1. Check `skill_rewards` section for skill-specific settings
2. Verify formula: reward = base × multiplier^(level/10)
3. Use calculator to verify expected amount
4. Add debug logging to `calculateSkillCoinsReward()`

### Problem: Message not displayed
**Possible Causes:**
1. Message template missing in global.yml
2. Placeholder not added to chat format
3. LevelerFormat.COINS_REWARD not found

**Solution:**
1. Verify `leveler_format.coins_reward` exists in global.yml
2. Check chat template includes `{coins_reward}` placeholder
3. Rebuild project to ensure enum changes compiled
4. Check for compilation errors in LevelUpMessenger.java

## Future Enhancements

### Potential Features
1. **Bonus multipliers:** Extra coins on milestones (every 10 levels)
2. **Time-based bonuses:** 2x coins during events/weekends
3. **Prestige integration:** Higher prestige = higher multiplier
4. **Party bonuses:** Shared coins when leveling in party
5. **Leaderboard rewards:** Top levelers get bonus coins
6. **Custom messages:** Per-skill level-up coin messages
7. **Animation effects:** Particles/sounds when coins awarded

### Configuration Expansion
```yaml
level_rewards:
  enabled: true
  base_reward: 50.0
  reward_multiplier: 1.2
  
  # Milestone bonuses (future)
  milestone_bonus:
    enabled: true
    levels: [10, 25, 50, 75, 100]
    multiplier: 2.0
  
  # Event bonuses (future)
  event_multiplier:
    enabled: false
    times:
      - "FRIDAY 18:00-23:59"
      - "SATURDAY 00:00-23:59"
    multiplier: 1.5
```

## Related Systems

### Shop System
See `.github/SHOP_SYSTEM_DOCUMENTATION.md` for complete shop implementation.

**Integration Points:**
- Level rewards → Player balance increase
- Shop menu → Display current balance
- Sell items → Additional coins income
- Buy levels/abilities → Spend accumulated coins

### Leaderboards
**Potential Integration:**
- Most coins earned from leveling
- Highest average coins per level
- Coins earned this week/month

### Rewards System
**Integration:**
- Custom rewards can also give coins
- Level rewards processed after coin rewards
- Both appear in same level-up message

## Developer Notes

### Code Style
- ✅ No hardcoded values (all from config)
- ✅ Proper error handling (missing config = 0 reward)
- ✅ No Bukkit APIs in common/ module
- ✅ Follows existing reward pattern (money_reward)
- ✅ Enum-based message system (no string literals)

### Performance
- Config loaded per level-up (not cached)
  - Trade-off: Allows dynamic changes without restart
  - Impact: Negligible (level-ups are infrequent)
- Calculation is O(1) with simple math
- No database queries (user balance in memory)

### Maintainability
- Follows existing reward system patterns
- Easy to add per-skill overrides
- Clear separation of concerns:
  - LevelManager: Calculation + giving coins
  - LevelUpMessenger: Formatting + display
  - shop_config.yml: All tunable values

## API Usage (External Plugins)

### Listen for Level-Up Rewards
```java
@EventHandler
public void onSkillLevelUp(SkillLevelUpEvent event) {
    Player player = event.getPlayer();
    Skill skill = event.getSkill();
    int newLevel = event.getNewLevel();
    
    // Coins are already given at this point
    AuraSkillsApi api = AuraSkillsApi.get();
    AuraSkillsUser user = api.getUser(player.getUniqueId());
    double balance = user.getSkillCoinsBalance();
    
    // Custom logic based on new balance
}
```

### Modify Rewards (Custom Plugin)
```java
public class CustomRewardsPlugin extends JavaPlugin implements Listener {
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onLevelUp(SkillLevelUpEvent event) {
        if (event.getSkill().getId().toString().equals("auraskills/farming")) {
            AuraSkillsUser user = AuraSkillsApi.get().getUser(event.getPlayer().getUniqueId());
            
            // Double farming rewards during harvest season
            if (isHarvestSeason()) {
                user.addSkillCoins(calculateLevelReward(event.getNewLevel()));
                event.getPlayer().sendMessage("§6✦ Harvest Season Bonus!");
            }
        }
    }
}
```

## Summary

The level-up SkillCoins reward system is now fully implemented and integrated into the existing level-up flow. Players earn coins based on skill level with exponential scaling, all values are configurable, and rewards are displayed clearly in level-up messages alongside other rewards (stats, abilities, money).

**Key Files Modified:**
1. `LevelManager.java` - Calculation and giving logic
2. `LevelUpMessenger.java` - Display formatting
3. `LevelerFormat.java` - Message enum addition
4. `global.yml` - Message templates
5. `shop_config.yml` - Configuration values

**Result:** Players now have a clear progression incentive through SkillCoins rewards that scale appropriately with their level investment, creating a more engaging economy system.
