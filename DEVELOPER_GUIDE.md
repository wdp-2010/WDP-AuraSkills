# AuraSkills Shop System Developer Guide

## Quick Start for Contributors

### Development Environment Setup

1. **Clone Repository**
```bash
git clone https://github.com/wdp-2010/WDP-AuraSkills.git
cd WDP-AuraSkills
```

2. **Build Project**
```bash
./gradlew clean build
```

3. **Run Test Server**
```bash
./gradlew :bukkit:runServer
```

### Project Structure

```
WDP-AuraSkills/
├── api/                    # Platform-independent API (Java 8)
├── api-bukkit/            # Bukkit-specific API extensions  
├── common/                # Core systems (no Bukkit dependencies)
│   └── economy/           # Shop system backend
├── bukkit/                # Bukkit implementation
│   ├── economy/           # Shop managers
│   └── menus/             # Shop UI components
└── config/
    └── checkstyle/        # Code style configuration
```

## Shop System Architecture Deep Dive

### Backend (`common/economy/`)

#### SkillPointsShop.java
**Purpose**: Central shop logic and data management

**Key Methods**:
```java
// Core transactions
public BuyResult purchaseItem(User user, String itemKey, int amount)
public SellResult sellItem(User user, String materialName, int amount)
public LevelPurchaseResult purchaseLevel(User user, Skill skill)
public AbilityPurchaseResult purchaseAbility(User user, String abilityKey)

// Stock management
public int getStock(String itemKey)
public void setStock(String itemKey, int stock)
public void restockAll()
public void startAutoRestock()

// Configuration
public void loadConfiguration()
```

**Threading Considerations**:
- Uses `ConcurrentHashMap` for thread-safe operations
- Configuration loading is synchronized
- Auto-restock runs on background scheduler

#### Data Structures
```java
// Core data maps
Map<String, Double> sellableItems              // material -> price
Map<String, BuyableItem> buyableItems          // key -> item data
Map<String, BuyableAbility> buyableAbilities   // key -> ability data
Map<String, Integer> itemStock                 // item -> current stock

// Cooldown tracking
Map<String, Map<String, Long>> playerCooldowns     // uuid -> material -> timestamp
Map<String, Map<String, Integer>> playerSoldAmounts // uuid -> material -> amount
```

### Frontend (`bukkit/menus/`)

#### Menu Base Pattern
All shop menus follow consistent patterns:

```java
public class ShopMenu implements Listener {
    private final AuraSkills plugin;
    private final SkillPointsShop shop;
    
    // Constructor registers event listeners
    public ShopMenu(AuraSkills plugin) {
        this.plugin = plugin;
        this.shop = plugin.getShopManager().getShop();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    // Main menu opening method
    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, size, title);
        // Populate inventory
        player.openInventory(inventory);
    }
    
    // Event handler
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(menuTitle)) return;
        event.setCancelled(true);
        // Handle clicks
    }
}
```

#### Menu Design Principles

**Layout Standards**:
- 54-slot inventories for complex menus (6x9 grid)
- 27-slot for simple menus (3x9 grid)
- Decorative borders with colored glass panes
- Consistent navigation button placement

**Visual Hierarchy**:
```java
// Color coding by menu type
Material.GRAY_STAINED_GLASS_PANE        // Level Shop borders
Material.LIGHT_BLUE_STAINED_GLASS_PANE  // Item Shop borders  
Material.PURPLE_STAINED_GLASS_PANE      // Ability Shop borders

// Button placement standards
int BALANCE_SLOT = 4;          // Top center
int BACK_SLOT = 45;            // Bottom left
int CLOSE_SLOT = 53;           // Bottom right
int HELP_SLOT = 48;            // Bottom center-left
int REFRESH_SLOT = 50;         // Bottom center-right
```

## Development Guidelines

### Code Style Requirements

**Checkstyle Configuration**: `config/checkstyle/checkstyle.xml`

**Critical Rules**:
- **No tabs**: Use 4-space indentation
- **No trailing whitespace**
- **LF line endings only**
- **Import order**: `*`, `javax`, `java` (with blank line separation)
- **Brace style**: `{` on same line, `}` on new line
- **One top-level class per file**

**Example**:
```java
package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class ExampleMenu implements Listener {
    private final AuraSkills plugin;
    
    public ExampleMenu(AuraSkills plugin) {
        this.plugin = plugin;
    }
    
    public void openMenu(Player player) {
        // Implementation
    }
}
```

### Testing Strategy

**Build and Test**:
```bash
# Full build with tests
./gradlew clean build

# Run specific tests
./gradlew test --tests "*ShopTest*"

# Integration testing with test server
./gradlew :bukkit:runServer
```

**Test Server Usage**:
- Auto-downloads Paper 1.21.10
- Located in `bukkit/run/`
- First run requires EULA acceptance
- Plugin auto-loads for testing

### Adding New Features

#### 1. New Shop Menu Type

**Step 1**: Create Menu Class
```java
// bukkit/src/main/java/dev/aurelium/auraskills/bukkit/menus/CustomShopMenu.java
public class CustomShopMenu implements Listener {
    // Follow existing menu patterns
}
```

**Step 2**: Register in ShopManager
```java
// bukkit/src/main/java/dev/aurelium/auraskills/bukkit/economy/ShopManager.java
public void initialize() {
    if (shop == null) {
        shop = new SkillPointsShop(plugin);
        levelShopMenu = new LevelShopMenu(plugin);
        customShopMenu = new CustomShopMenu(plugin);  // Add this
        shop.startAutoRestock();
        plugin.getLogger().info("Shop system initialized");
    }
}
```

**Step 3**: Add Navigation
```java
// Update MainShopMenu.java to include new category
```

#### 2. New Transaction Type

**Step 1**: Extend SkillPointsShop
```java
// Add to common/src/main/java/dev/aurelium/auraskills/common/economy/SkillPointsShop.java
public CustomPurchaseResult purchaseCustomItem(User user, String itemKey) {
    // Implement custom logic
    return new CustomPurchaseResult(success, message);
}
```

**Step 2**: Add Configuration Support
```yaml
# Add to common/src/main/resources/shop_config.yml
custom_items:
  enabled: true
  items:
    special_item:
      cost: 1000.0
      requirements:
        level: 50
        skill: "farming"
```

**Step 3**: Create Result Class
```java
public static class CustomPurchaseResult {
    public final boolean success;
    public final String message;
    
    public CustomPurchaseResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
```

### Configuration Best Practices

#### YAML Structure
```yaml
# Use consistent naming conventions
section_name:
  enabled: true           # Boolean settings first
  setting_value: 100      # Numeric settings
  
  subsection:             # Nested objects
    item_key:
      property: value
      
  list_items:             # Arrays
    - item1
    - item2
```

#### Loading Pattern
```java
// Always provide defaults
ConfigurationNode config = loader.loadContentAndMerge(null, "shop_config.yml", embedded, user);
boolean enabled = config.node("section", "enabled").getBoolean(true);
int value = config.node("section", "value").getInt(100);

// Handle missing nodes gracefully
if (!config.node("optional_section").virtual()) {
    // Process optional configuration
}
```

### Performance Optimization

#### Common Hotspots
1. **Menu Population**: Cache frequently accessed data
2. **Event Handling**: Minimize work in click handlers  
3. **Configuration Access**: Load once, cache results
4. **Database Operations**: Batch updates when possible

#### Optimization Techniques
```java
// Cache expensive calculations
private final Map<Skill, Material> skillMaterials = new HashMap<>();

// Use efficient data structures
private final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();

// Minimize object creation in hot paths
private static final ItemStack BORDER_ITEM = createBorderItem();

// Batch database operations
scheduler.executeAsync(() -> {
    // Batch multiple user updates
});
```

### Debugging and Monitoring

#### Debug Mode
```java
if (debugMode) {
    plugin.logger().info("Debug: " + operation + " took " + duration + "ms");
}
```

#### Error Handling
```java
try {
    // Risky operation
} catch (Exception e) {
    plugin.logger().error("Failed to process shop operation", e);
    player.sendMessage("§c§lError: §7Shop temporarily unavailable");
    return false;
}
```

#### Performance Monitoring
```java
long startTime = System.nanoTime();
// Operation
long duration = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms

if (duration > 50) { // Log slow operations
    plugin.logger().warn("Slow shop operation: " + operation + " took " + duration + "ms");
}
```

## API Reference

### Public Interfaces

#### Getting Shop Instance
```java
AuraSkills plugin = AuraSkills.getInstance();
SkillPointsShop shop = plugin.getShopManager().getShop();
```

#### User Operations
```java
User user = plugin.getUser(player);

// Check balance
double balance = user.getSkillCoins();

// Modify balance
user.setSkillCoins(balance + amount);
user.addSkillCoins(amount);  // Alternative method

// Get skill levels
int level = user.getSkillLevel(Skills.FARMING);
```

#### Shop Operations
```java
// Purchase item
BuyResult result = shop.purchaseItem(user, "DIAMOND", 1);
if (result.getType() == BuyResult.BuyResultType.SUCCESS) {
    // Handle success
}

// Sell item
SellResult result = shop.sellItem(user, "EMERALD", 5);

// Purchase level
LevelPurchaseResult result = shop.purchaseLevel(user, Skills.MINING);
```

### Events Integration

#### Custom Event Listening
```java
@EventHandler
public void onShopPurchase(InventoryClickEvent event) {
    String title = event.getView().getTitle();
    
    if (title.contains("Shop")) {
        // Custom shop logic
        Player player = (Player) event.getWhoClicked();
        // Process custom behavior
    }
}
```

#### Menu State Management
```java
// Track menu state
private final Set<UUID> activeMenus = ConcurrentHashMap.newKeySet();

public void openMenu(Player player) {
    activeMenus.add(player.getUniqueId());
    // Open menu
}

@EventHandler
public void onMenuClose(InventoryCloseEvent event) {
    activeMenus.remove(event.getPlayer().getUniqueId());
}
```

## Troubleshooting Common Issues

### Build Problems

**Checkstyle Failures**:
```bash
# Run checkstyle check
./gradlew checkstyleMain checkstyleTest

# Common fixes
- Remove trailing whitespace
- Fix indentation (4 spaces, no tabs)
- Correct import order
- Add missing blank lines
```

**Compilation Errors**:
```bash
# Clean build
./gradlew clean

# Check for missing imports
# Verify API compatibility
# Ensure proper module dependencies
```

### Runtime Issues

**Menu Not Opening**:
```java
// Check event registration
Bukkit.getPluginManager().registerEvents(this, plugin);

// Verify menu title matching
if (!event.getView().getTitle().equals(expectedTitle)) return;

// Confirm shop initialization
if (!plugin.getShopManager().isInitialized()) {
    // Handle uninitialized state
}
```

**Transaction Failures**:
```java
// Add debug logging
if (debugMode) {
    plugin.logger().info("Transaction: " + user.getUsername() + 
                        " tried to buy " + itemKey + 
                        " for " + cost + 
                        " (balance: " + user.getSkillCoins() + ")");
}

// Check all prerequisites
- User balance sufficient
- Item in stock
- No active cooldowns
- Valid permissions
```

### Performance Issues

**Slow Menu Loading**:
```java
// Profile menu population
long start = System.nanoTime();
populateMenu(inventory, player);
long duration = (System.nanoTime() - start) / 1_000_000;

if (duration > 100) {
    plugin.logger().warn("Slow menu population: " + duration + "ms");
}

// Optimization strategies:
- Cache item creation
- Reduce database queries
- Simplify lore generation
- Use async loading where possible
```

**Memory Leaks**:
```java
// Clean up resources
@EventHandler
public void onPlayerQuit(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    
    // Clear cached data
    playerMenuStates.remove(uuid);
    activeCooldowns.remove(uuid.toString());
}
```

## Contributing Guidelines

### Pull Request Process

1. **Fork Repository**
2. **Create Feature Branch**: `feature/shop-enhancement`
3. **Follow Code Style**: Run checkstyle before committing
4. **Add Tests**: Include unit tests for new functionality
5. **Update Documentation**: Modify this guide and API docs
6. **Test Integration**: Verify with test server
7. **Submit PR**: Include detailed description

### Code Review Checklist

- [ ] Follows established patterns
- [ ] Includes error handling
- [ ] Has appropriate logging
- [ ] Performance considerations addressed
- [ ] Configuration documented
- [ ] User experience tested
- [ ] No breaking changes to API

### Release Process

1. **Version Bump**: Update `gradle.properties`
2. **Changelog**: Update `Changelog.md`
3. **Build Verification**: Full clean build
4. **Integration Testing**: Test server validation
5. **Documentation**: Update all relevant docs
6. **Tag Release**: Git tag with version
7. **Artifact Publishing**: Deploy to repositories

---

*For questions or support, create an issue in the project repository.*