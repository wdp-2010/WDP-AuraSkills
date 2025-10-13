# Main Shop Menu Redesign - Skills Menu Style

## Overview

Complete redesign of the main shop menu to match the professional layout and aesthetic of the Skills menu (`skills.yml`).

## Design Changes

### Layout Structure (Following skills.yml pattern)

**Skills Menu Pattern:**
```
[Your Skills] [          Skills in centered rows          ] [Close]
[Stats]       [                                            ]
[Skill Coins] [                                            ]
```

**New Shop Menu Pattern:**
```
Row 0: [Balance]    [■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■]
Row 1: [■■■■■■■■■]  [Item]  [■]  [Level]  [■]  [Ability]  [■■■]
Row 2: [■■■■■■■■■■■■■■■■■■■]  [Cooldown]  [■■■■■■■■■■■■]
Row 3: [■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■]
Row 4: [■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■] [Close]

■ = Black stained glass pane (like skills menu)
```

### Specific Slot Positions

| Item | Slot | Reason |
|------|------|--------|
| **Balance** | 0 | Top-left, like "Your Skills" in skills menu |
| **Item Shop** | 11 | Row 1, centered group of 3 |
| **Level Shop** | 13 | Row 1, center of 3-item group |
| **Ability Shop** | 15 | Row 1, centered group of 3 |
| **Cooldown Tracker** | 22 | Row 2, center position |
| **Close** | 44 | Bottom-right, matching skills menu |
| **Filler** | All others | Black stained glass pane |

## Key Design Elements Matching Skills Menu

### 1. Background Fill
- **Material**: BLACK_STAINED_GLASS_PANE (not gray, not light blue)
- **Display Name**: Single space `" "` (not empty string)
- **Purpose**: Clean, professional background like skills menu

### 2. Item Positioning
- **Top-left info item** (slot 0): Balance/stats display
- **Centered content rows**: Main interactive items
- **Bottom-right close** (slot 44): Consistent exit button placement

### 3. Color Scheme (from skills.yml)
| Color | Code | Usage |
|-------|------|-------|
| Aqua | `§b` | Item Shop title |
| Yellow/Gold | `§e`/`§6` | Level Shop, Balance, highlights |
| Light Purple | `§d` | Ability Shop title |
| Gray | `§7` | Descriptions |
| Dark Gray | `§8` | Subtle hints |
| Red | `§c` | Close button |

### 4. Text Format
**Balance Item:**
```
§6Skill Coins Balance
§7Your current balance:
§e1000 ⛁
 
§8Earn coins by leveling skills
§8Spend in the shop categories below
```

**Shop Category Items:**
```
§bItem Shop
§7Buy and sell items for
§7skill coins
 
§eClick to open
```

### 5. Lore Structure
Following skills.yml format:
1. Description (2 lines max, §7 gray)
2. Blank line for spacing
3. Additional info if needed (§7 or §8)
4. Blank line
5. Action hint (§e yellow, "Click to open")

## Before vs After

### Before (Broken Design)
❌ Small 27-slot inventory (3 rows)
❌ Empty slots with no background
❌ Random positioning
❌ Broken color codes (`§yellow`, `§dark_gray`)
❌ Cluttered text
❌ Close at slot 22 (middle)

### After (Skills Menu Style)
✅ 45-slot inventory (5 rows, matches skills menu)
✅ Black stained glass pane fill throughout
✅ Centered, balanced positioning
✅ Proper Minecraft color codes (§e, §8)
✅ Clean, concise descriptions
✅ Close at slot 44 (bottom-right)
✅ Balance at slot 0 (top-left)
✅ Professional spacing and alignment

## Technical Implementation

### Slot Calculation Logic
```java
// Row 0: slot 0 (balance)
// Row 1: slots 11, 13, 15 (3 items centered)
// Row 2: slot 22 (1 item centered)
// Row 4: slot 44 (close)

// All other slots: black glass pane fill
for (int i = 0; i < 45; i++) {
    inventory.setItem(i, filler);
}
```

### Click Handler
```java
case 11: // Item Shop (row 1, left)
case 13: // Level Shop (row 1, center)
case 15: // Ability Shop (row 1, right)
case 22: // Cooldown Tracker (row 2, center)
case 44: // Close (row 4, right)
```

## Visual Consistency Points

1. ✅ **Sunflower for balance** - Matches skills.yml skill_coins_balance
2. ✅ **Black glass pane fill** - Matches skills.yml fill.material
3. ✅ **Top-left info slot** - Matches "Your Skills" position
4. ✅ **Bottom-right close** - Matches skills menu close button
5. ✅ **Centered content rows** - Matches skill display pattern
6. ✅ **Clean spacing** - Single blank lines, no clutter
7. ✅ **Proper color codes** - §e for yellow, §8 for dark_gray
8. ✅ **Concise text** - 2-line descriptions like skills menu

## Files Modified
- `bukkit/src/main/java/.../menus/MainShopMenu.java`
  - `openMainMenu()` - Complete layout redesign
  - `onInventoryClick()` - Updated slot positions
  - `createBalanceItem()` - Matching skills.yml format
  - All shop item creators - Clean, professional text

## Result
The main shop menu now perfectly matches the aesthetic and layout philosophy of the skills menu, providing a consistent, professional user experience throughout the plugin.
