# Shop-Exclusive Abilities System - Complete Fix

## Issues Identified

### 1. **Level Requirements Mismatch**
- **Problem**: `shop_config.yml` had requirements of levels 25-50, but `abilities.yml` shows all shop-exclusive abilities unlock at `{start}+5` (level 5)
- **Impact**: Players reaching level 5 in skills would see abilities as "unlocked" in progression menus, but couldn't purchase them from shop until much higher levels
- **Root Cause**: Arbitrary high level requirements not aligned with ability configuration

### 2. **Purchase Status Check Bug**
- **Problem**: `AbilityShopMenu.java` checked `user.getAbilityLevel(ability) > 0` instead of `user.hasPurchasedAbility(abilityKey)`
- **Impact**: Menu showed abilities as "purchased" if player had ANY level in them (from skill progression), not if they actually bought them from shop
- **Root Cause**: Confusion between ability unlock level and shop purchase status

### 3. **Poor Display Quality**
- **Problem**: Menu items didn't show detailed ability stats, just generic descriptions
- **Impact**: Players couldn't see what they're buying (base values, per-level increases, special effects)
- **Root Cause**: Display logic didn't leverage detailed descriptions from shop_config.yml

### 4. **Unbalanced Pricing**
- **Problem**: Flat pricing structure (3000-8000 coins) didn't reflect ability power
- **Impact**: Some weak abilities cost too much, some powerful abilities cost too little
- **Root Cause**: No analysis of ability values and utility when setting prices

## Solutions Implemented

### 1. Fixed shop_config.yml - Balanced Requirements & Pricing

**New Pricing Tiers:**
```yaml
TIER 1 (1500-2000 coins) - Essential abilities, early game access
  - Stun (1500): 2% + 1%/lvl, crowd control
  - Hardened Armor (1800): 3% + 3%/lvl damage reduction

TIER 2 (2500-3500 coins) - Powerful abilities, mid-game power spike
  - Growth Aura (2500): 12% + 12%/lvl, 30 block radius AOE
  - Lucky Spades (2800): 0.05% + 0.03%/lvl treasure chance
  - Bleed (3000): 3% + 3%/lvl chance, DOT damage

TIER 3 (4000-5000 coins) - Elite abilities, late-game advantages
  - Epic Catch (4000): 0.2% + 0.2%/lvl epic loot
  - No Debuff (4500): 5% + 5%/lvl immunity
  - Lucky Table (5000): 5% + 1%/lvl enchanting luck
```

**All requirements now level 5** - aligned with `abilities.yml` unlock pattern

**Enhanced descriptions** with:
- Ability mechanics explanation
- Base value statistics
- Per-level scaling info
- Special effects (radius, duration, etc.)
- Clear requirement display

### 2. Fixed AbilityShopMenu.java - Proper Purchase Tracking

**Changes made:**

1. **Purchase Status Check**
```java
// OLD (WRONG):
boolean hasAbility = ability != null && user.getAbilityLevel(ability) > 0;

// NEW (CORRECT):
boolean hasPurchased = user.hasPurchasedAbility(abilityKey);
```

2. **Visual Indicators**
- Purchased abilities: `Material.BOOK` with green checkmark `§a✓` prefix
- Unpurchased abilities: `Material.ENCHANTED_BOOK` with purple color
- Menu title shows balance: `§dAbility Shop §8| §e1234 ⛁`

3. **Better Purchase Feedback**
```
PURCHASED:
  § a✓ Purchased
  §8Current Level: §7X
  §8Level up your skill to unlock!

NOT PURCHASED:
  §7Status:
    §a✓ All requirements met
    §e▶ Click to purchase!
  
  OR
  
  §7Status:
    §c✗ Need X more coins
    §c✗ Skill level too low
```

4. **Fixed Grid Layout**
- Corrected row/column calculations (was off by 1)
- Abilities now properly arranged in 7-column grid
- Click detection matches visual positions

### 3. Pricing Balance Rationale

**Tier 1 (Cheap) - Essential Utility:**
- **Stun**: Low base chance (2%), requires hits to be useful, good for beginners
- **Hardened Armor**: Defensive, percentage-based, scales with player skill

**Tier 2 (Medium) - Power Spikes:**
- **Growth Aura**: AOE effect, powerful but stationary, great for farmers
- **Bleed**: Combat-focused DOT, mid-tier damage, scales well
- **Lucky Spades**: Very low base chance, requires grinding, treasure-focused

**Tier 3 (Expensive) - Game Changers:**
- **No Debuff**: Immunity to debuffs, powerful defensive tool, high utility
- **Epic Catch**: Legendary loot access, rare but valuable rewards
- **Lucky Table**: Enchanting boost, affects all enchantments, late-game focused

## Testing Checklist

- [x] Code compiles without errors
- [ ] Server starts without errors  
- [ ] Shop menu opens correctly
- [ ] Abilities display with proper stats
- [ ] Purchase flow works (deducts coins, adds to user)
- [ ] Purchased abilities show checkmark
- [ ] Level requirements enforced
- [ ] Balance feels appropriate (not too easy/hard to buy)
- [ ] Ability effects work after purchase

## Files Modified

1. **common/src/main/resources/shop_config.yml**
   - Rewritten `buyable_abilities` section
   - Level requirements: 25-50 → 5 (all abilities)
   - Pricing: 3000-8000 → 1500-5000 (tiered by power)
   - Descriptions: Added detailed stats and mechanics

2. **bukkit/src/main/java/.../menus/AbilityShopMenu.java**
   - Fixed purchase status check (`hasPurchasedAbility` instead of `getAbilityLevel`)
   - Enhanced item display with visual indicators
   - Corrected grid layout calculations
   - Added balance to menu title
   - Improved requirement display

## Backward Compatibility

✅ **Fully compatible** - existing purchases persist
✅ **Config hot-reloadable** - changes apply after `/sk reload`
✅ **No database changes** - uses existing purchase tracking system

## Future Enhancements

- [ ] Add 🔒 LOCKED indicators in skill progression menus for unpurchased shop abilities
- [ ] Permission-based free access (`auraskills.ability.<name>.free`)
