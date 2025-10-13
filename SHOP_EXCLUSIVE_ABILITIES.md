# Shop-Exclusive Abilities System - Extensibility Guide

## Overview

The shop-exclusive abilities system is **fully configuration-driven** and extensible. You can easily add new shop-exclusive abilities without modifying core code by simply adding the `shop_exclusive: true` flag to any ability in `abilities.yml`.

## Quick Start: Adding a New Shop-Exclusive Ability

### 1. Mark Ability as Shop-Exclusive in Config

Edit `common/src/main/resources/abilities.yml`:

```yaml
auraskills/your_new_ability:
  enabled: true
  shop_exclusive: true  # Must be purchased from shop to use
  base_value: 10.0
  value_per_level: 5.0
  unlock: '{start}+5'
  level_up: 5
  max_level: 0
```

**That's it!** The ability will now:
- Show in skill progression menus (because `enabled: true`)
- Require purchase from shop before effects apply (because `shop_exclusive: true`)
- Be automatically tracked in the purchase system

### 2. Add Purchase Check in Ability Implementation

In your ability's event handler, add the purchase check:

```java
@EventHandler
public void yourAbility(SomeEvent event) {
    var ability = Abilities.YOUR_NEW_ABILITY;
    
    if (failsChecks(player, ability)) return;
    
    User user = plugin.getUser(player);
    
    // Check if shop-exclusive ability has been purchased
    if (!plugin.getShopManager().getShop().canUseAbility(user, "auraskills/your_new_ability")) return;
    
    // Apply ability effect here...
}
```

### 3. Configure Shop Pricing

Edit `common/src/main/resources/shop_config.yml`:

```yaml
abilities:
  buyable:
    auraskills/your_new_ability:
      cost: 50
      requirement_skill: farming  # Skill that must be trained to unlock
      requirement_level: 20       # Minimum skill level required
      display_name: "Your New Ability"
      display_lore:
        - "Description of what this ability does"
        - "Cost: 50 Skill Points"
```

## System Architecture

### Configuration Layer (`abilities.yml`)

Each ability has a `shop_exclusive` boolean flag:
- `shop_exclusive: true` → Ability requires purchase from shop
- `shop_exclusive: false` or omitted → Normal ability, unlocks via skill progression

```yaml
abilities:
  auraskills/growth_aura:
    enabled: true
    shop_exclusive: true  # Configuration-driven flag
    base_value: 12.0
    # ... other properties
```

### API Layer

**AbilityConfig.java** - Reads configuration
```java
public boolean shopExclusive() {
    return getBoolean("shop_exclusive", false);
}
```

**AbilityManager.java** - Checks if ability is shop-exclusive
```java
public boolean isShopExclusive(Ability ability) {
    LoadedAbility loadedAbility = abilityMap.get(ability);
    if (loadedAbility == null) return false;
    return loadedAbility.config().shopExclusive();
}
```

**SkillPointsShop.java** - Validates purchases
```java
public boolean canUseAbility(User user, String abilityKey) {
    // If it's not a shop-exclusive ability, they can use it normally
    if (!isShopExclusiveAbility(abilityKey)) {
        return true;
    }
    // For shop-exclusive abilities, check if they've purchased it
    return user.hasPurchasedAbility(abilityKey);
}
```

### Storage Layer

**User.java** - Tracks purchased abilities
```java
private final Set<String> purchasedAbilities;

public boolean hasPurchasedAbility(String abilityKey) {
    return purchasedAbilities.contains(abilityKey.toLowerCase());
}

public void addPurchasedAbility(String abilityKey) {
    purchasedAbilities.add(abilityKey.toLowerCase());
    blank = false; // Mark user data as modified
}
```

**User.metadata** - Persists to database
```
Key: "shop.purchased_abilities"
Value: "auraskills/growth_aura,auraskills/stun,auraskills/bleed"
Format: Comma-separated list of purchased ability keys
Storage: MySQL or YAML (configured via config.yml)
```

## Currently Implemented Shop-Exclusive Abilities

| Ability | Skill | Effect | Implementation Status |
|---------|-------|--------|----------------------|
| Growth Aura | Farming | Speeds up crop growth nearby | ✅ Fully implemented with purchase check |
| Hardened Armor | Mining | Chance to prevent armor damage | ✅ Fully implemented with purchase check |
| Epic Catch | Fishing | Increased chance for rare catches | ⚠️ Defined but not yet implemented |
| Lucky Spades | Excavation | Increased rare loot from digging | ⚠️ Defined but not yet implemented |
| Stun | Archery | Chance to slow enemies with arrows | ✅ Fully implemented with purchase check |
| No Debuff | Defense | Chance to resist negative potion effects | ✅ Fully implemented with purchase check |
| Bleed | Fighting | Chance to make enemies bleed over time | ✅ Fully implemented with purchase check |
| Lucky Table | Enchanting | Chance for higher enchantment levels | ✅ Fully implemented with purchase check |

## Purchase Flow

### 1. Player Purchases Ability
```
Player clicks "Buy" in shop menu
  ↓
ShopMenu validates purchase requirements
  ↓
SkillPointsShop.purchaseAbility() called
  ↓
Checks: already purchased? sufficient points? meets requirements?
  ↓
Deducts skill points
  ↓
user.addPurchasedAbility(abilityKey)
  ↓
savePurchasedAbilities() → writes to User.metadata
  ↓
Returns success message
```

### 2. Player Uses Ability
```
Ability event triggers (e.g., BlockGrowEvent for Growth Aura)
  ↓
failsChecks() validates basic conditions
  ↓
canUseAbility() checks if purchased
  ↓
If not shop-exclusive OR purchased → Apply effect
  ↓
If shop-exclusive AND not purchased → Return early (no effect)
```

### 3. Data Persistence
```
Player joins server
  ↓
UserLoadEvent fires
  ↓
loadPurchasedAbilities() reads from User.metadata
  ↓
Parses comma-separated string into Set<String>
  ↓
Player quits server
  ↓
User.metadata auto-saves via StorageProvider
```

## Testing Your New Shop-Exclusive Ability

### 1. Verify Configuration
```bash
# Check abilities.yml loaded correctly
/sk debug shop
```

### 2. Test Purchase Flow
```bash
# Give yourself skill points
/sk modifier add <player> skill_points 100

# Try purchasing ability
/sk shop buy ability auraskills/your_new_ability
```

### 3. Verify Effect Requires Purchase
```bash
# Test without purchasing - should NOT apply effect
# Purchase ability
/sk shop buy ability auraskills/your_new_ability
# Test with purchase - should apply effect
```

### 4. Check Persistence
```bash
# Purchase ability
/sk shop buy ability auraskills/your_new_ability
# Quit and rejoin server
# Verify ability still works
```

## Common Patterns

### Check Purchase Before Effect
```java
// Pattern 1: Early return after failsChecks
if (failsChecks(player, ability)) return;
User user = plugin.getUser(player);
if (!plugin.getShopManager().getShop().canUseAbility(user, "auraskills/ability_key")) return;

// Pattern 2: Check within probability roll
if (rand.nextDouble() < (getValue(ability, user) / 100)) {
    if (!plugin.getShopManager().getShop().canUseAbility(user, "auraskills/ability_key")) return;
    // Apply effect
}
```

### Add Refund Support (Optional)
```java
// In your refund command/menu handler
user.removePurchasedAbility("auraskills/ability_key");
plugin.getShopManager().getShop().savePurchasedAbilities(user.getUuid().toString());
// Return skill points to user
```

### Check Multiple Abilities
```java
// If you have multiple shop-exclusive abilities in one handler
for (Ability ability : abilities) {
    if (plugin.getAbilityManager().isShopExclusive(ability)) {
        if (!user.hasPurchasedAbility(ability.getId().toString())) {
            continue; // Skip this ability
        }
    }
    // Apply ability effect
}
```

## Migration Notes

### Old System (Hardcoded)
```java
// BAD - Hardcoded ability keys
public boolean isShopExclusiveAbility(String abilityKey) {
    return buyableAbilities.containsKey(abilityKey.toLowerCase());
}
```

### New System (Configuration-Driven)
```java
// GOOD - Reads from abilities.yml
public boolean isShopExclusiveAbility(String abilityKey) {
    Ability ability = plugin.getAbilityRegistry().getOrNull(NamespacedId.fromString(abilityKey));
    if (ability == null) return false;
    return plugin.getAbilityManager().isShopExclusive(ability);
}
```

## Benefits of Extensible Design

✅ **No Code Changes Required** - Add abilities via configuration only
✅ **Custom Abilities Support** - Works with custom abilities from other plugins
✅ **Namespace-Aware** - Supports `yourplugin/custom_ability` format
✅ **Hot-Reloadable** - Config changes apply after `/sk reload`
✅ **Type-Safe** - Compile-time checks via AbilityConfig API
✅ **Future-Proof** - Easy to add new shop-exclusive abilities in updates

## Troubleshooting

**Problem**: Ability works even without purchase
**Solution**: Check that you added the `canUseAbility()` check in the ability implementation

**Problem**: Ability doesn't show in shop
**Solution**: Add to `shop_config.yml` under `abilities.buyable`

**Problem**: Purchase not persisting across restarts
**Solution**: Verify `loadPurchasedAbilities()` is called in `PlayerJoinQuit.onUserLoad()`

**Problem**: Custom ability not recognized
**Solution**: Ensure ability is properly registered with `NamespacedRegistry` before config load

## Future Enhancements

Potential additions to the system:
- [ ] Visual 🔒 LOCKED indicators in skills menus for unpurchased abilities
- [ ] Refund system with cooldown
- [ ] Bulk purchase discounts
- [ ] Ability rental system (temporary unlock)
- [ ] Achievement rewards (free unlock on achievement)
- [ ] Permission-based free access (`auraskills.ability.growth_aura.free`)

## Example: Full Implementation

Here's a complete example of adding a new shop-exclusive ability from scratch:

### 1. Define in abilities.yml
```yaml
auraskills/super_harvest:
  enabled: true
  shop_exclusive: true  # NEW: Mark as shop-exclusive
  base_value: 15.0
  value_per_level: 5.0
  unlock: '{start}+10'
  level_up: 5
  max_level: 20
```

### 2. Add to shop_config.yml
```yaml
abilities:
  buyable:
    auraskills/super_harvest:
      cost: 75
      requirement_skill: farming
      requirement_level: 25
      display_name: "Super Harvest"
      display_lore:
        - "Chance to harvest 2x crops"
        - "Cost: 75 Skill Points"
```

### 3. Implement Effect
```java
@EventHandler
public void superHarvest(BlockBreakEvent event) {
    var ability = Abilities.SUPER_HARVEST;
    
    if (isDisabled(ability)) return;
    
    Player player = event.getPlayer();
    
    if (failsChecks(player, ability)) return;
    
    User user = plugin.getUser(player);
    
    // NEW: Check if shop-exclusive ability has been purchased
    if (!plugin.getShopManager().getShop().canUseAbility(user, "auraskills/super_harvest")) return;
    
    if (rand.nextDouble() < (getValue(ability, user) / 100)) {
        // Double the crop drops
        Collection<ItemStack> drops = event.getBlock().getDrops(player.getInventory().getItemInMainHand());
        Location loc = event.getBlock().getLocation();
        for (ItemStack drop : drops) {
            loc.getWorld().dropItemNaturally(loc, drop);
        }
    }
}
```

Done! Your new shop-exclusive ability is now fully integrated and extensible.
