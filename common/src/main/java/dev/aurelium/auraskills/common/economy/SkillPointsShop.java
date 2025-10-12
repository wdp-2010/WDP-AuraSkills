package dev.aurelium.auraskills.common.economy;

import dev.aurelium.auraskills.api.ability.Ability;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.config.ConfigurateLoader;
import dev.aurelium.auraskills.common.scheduler.TaskRunnable;
import dev.aurelium.auraskills.common.user.User;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkillPointsShop {

    private final AuraSkillsPlugin plugin;
    private final Map<String, Double> sellableItems;
    private final Map<String, BuyableItem> buyableItems;
    private final Map<String, BuyableAbility> buyableAbilities;
    private final Map<String, Integer> sellCooldowns; // Material -> cooldown seconds
    private final Map<String, Integer> maxAmounts; // Material -> max sellable amount
    private final Map<String, Map<String, List<Long>>> playerCooldowns; // UUID -> (Material -> list of sell timestamps)
    private final Map<String, Integer> itemStock; // Item -> current stock
    private int globalSellCooldown;
    private double levelBaseCost;
    private double levelCostMultiplier;
    private int maxPurchasableLevels;
    private int restockInterval; // in minutes
    private final Map<String, Double> skillSpecificLevelCosts;
    private final Map<String, Integer> skillMaxLevels;
    private boolean debugMode;

    public SkillPointsShop(AuraSkillsPlugin plugin) {
        this.plugin = plugin;
        this.sellableItems = new ConcurrentHashMap<>();
        this.buyableItems = new ConcurrentHashMap<>();
        this.buyableAbilities = new ConcurrentHashMap<>();
        this.sellCooldowns = new ConcurrentHashMap<>();
        this.maxAmounts = new ConcurrentHashMap<>();
        this.playerCooldowns = new ConcurrentHashMap<>();
        this.itemStock = new ConcurrentHashMap<>();
        this.skillSpecificLevelCosts = new ConcurrentHashMap<>();
        this.skillMaxLevels = new ConcurrentHashMap<>();
        loadConfiguration();
    }

    public void loadConfiguration() {
        sellableItems.clear();
        buyableItems.clear();
        buyableAbilities.clear();
        sellCooldowns.clear();
        maxAmounts.clear();
        skillSpecificLevelCosts.clear();
        skillMaxLevels.clear();

        try {
            ConfigurateLoader loader = new ConfigurateLoader(plugin, TypeSerializerCollection.builder().build());
            
            // Update and load files like other content loaders do
            loader.updateUserFile("shop_config.yml");
            ConfigurationNode embedded = loader.loadEmbeddedFile("shop_config.yml");
            ConfigurationNode user = loader.loadUserFile("shop_config.yml");
            
            // Merge embedded and user configurations
            ConfigurationNode config = loader.loadContentAndMerge(null, "shop_config.yml", embedded, user);

            // Load debug mode
            debugMode = config.node("debug_mode").getBoolean(false);

            // Load global sell cooldown
            globalSellCooldown = config.node("sell_items", "global_cooldown").getInt(0);

            // Load stock restock interval
            restockInterval = config.node("stock", "restock_interval").getInt(0);

            // Load level purchase settings
            ConfigurationNode levelConfig = config.node("level_purchase");
            levelBaseCost = levelConfig.node("base_cost").getDouble(100.0);
            levelCostMultiplier = levelConfig.node("cost_multiplier").getDouble(1.5);
            maxPurchasableLevels = levelConfig.node("max_purchasable_levels").getInt(0);

            // Load skill-specific costs
            ConfigurationNode skillCostsNode = levelConfig.node("skill_costs");
            if (!skillCostsNode.virtual()) {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : skillCostsNode.childrenMap().entrySet()) {
                    String skillName = entry.getKey().toString();
                    double cost = entry.getValue().getDouble(levelBaseCost);
                    skillSpecificLevelCosts.put(skillName.toLowerCase(), cost);
                }
            }
            
            // Load skill-specific max levels
            ConfigurationNode skillMaxLevelsNode = levelConfig.node("skill_max_levels");
            if (!skillMaxLevelsNode.virtual()) {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : skillMaxLevelsNode.childrenMap().entrySet()) {
                    String skillName = entry.getKey().toString();
                    int maxLevel = entry.getValue().getInt(maxPurchasableLevels);
                    skillMaxLevels.put(skillName.toLowerCase(), maxLevel);
                }
            }

            // Load sellable items
            loadSellableItems(config.node("sell_items", "items"));

            // Load buyable items
            loadBuyableItems(config.node("buy_items", "items"));

            // Load buyable abilities
            loadBuyableAbilities(config.node("buyable_abilities", "abilities"));

            if (debugMode) {
                plugin.logger().info("Shop configuration loaded: " + sellableItems.size() + " sellable items, " + buyableItems.size() + " buyable items, " + buyableAbilities.size() + " buyable abilities");
            }

        } catch (IOException e) {
            plugin.logger().severe("Failed to load shop configuration: " + e.getMessage());
        }
    }

    private void loadSellableItems(ConfigurationNode node) {
        if (node.virtual()) return;

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String material = entry.getKey().toString();
            ConfigurationNode itemNode = entry.getValue();

            if (!itemNode.node("enabled").getBoolean(true)) continue;

            double price = itemNode.node("price").getDouble(0.0);
            if (price > 0) {
                sellableItems.put(material.toUpperCase(), price);
                
                // Load item-specific cooldown
                int cooldown = itemNode.node("cooldown").getInt(globalSellCooldown);
                if (cooldown > 0) {
                    sellCooldowns.put(material.toUpperCase(), cooldown);
                }
                
                // Load max_amount (how many items can be on cooldown at once)
                int maxAmount = itemNode.node("max_amount").getInt(1);
                maxAmounts.put(material.toUpperCase(), maxAmount);
                
                if (debugMode) {
                    plugin.logger().info("Loaded sellable item: " + material + " = " + price + " skill points (cooldown: " + cooldown + "s, max_amount: " + maxAmount + ")");
                }
            }
        }
    }

    private void loadBuyableItems(ConfigurationNode node) {
        if (node.virtual()) return;

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String material = entry.getKey().toString();
            ConfigurationNode itemNode = entry.getValue();

            if (!itemNode.node("enabled").getBoolean(true)) continue;

            String displayName = itemNode.node("display_name").getString(material);
            int amount = itemNode.node("amount").getInt(1);
            double price = itemNode.node("price").getDouble(0.0);
            int stock = itemNode.node("stock").getInt(-1); // -1 = unlimited
            
            if (price > 0) {
                BuyableItem buyableItem = new BuyableItem(material, displayName, amount, price, stock);
                buyableItems.put(material.toUpperCase(), buyableItem);
                if (debugMode) {
                    String stockStr = stock == -1 ? "unlimited" : String.valueOf(stock);
                    plugin.logger().info("Loaded buyable item: " + material + " = " + price + " skill coins (stock: " + stockStr + ")");
                }
            }
        }
    }

    private void loadBuyableAbilities(ConfigurationNode node) {
        if (node.virtual()) return;

        for (Map.Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
            String abilityKey = entry.getKey().toString();
            ConfigurationNode abilityNode = entry.getValue();

            if (!abilityNode.node("enabled").getBoolean(true)) continue;

            double cost = abilityNode.node("cost").getDouble(0.0);
            String requiredSkill = abilityNode.node("skill_requirement").getString("");
            int requiredLevel = abilityNode.node("required_level").getInt(0);
            String displayName = abilityNode.node("display_name").getString(abilityKey);
            
            // Load description as list of strings
            java.util.List<String> description = new java.util.ArrayList<>();
            ConfigurationNode descNode = abilityNode.node("description");
            if (descNode.isList()) {
                try {
                    description = descNode.getList(String.class, java.util.ArrayList::new);
                } catch (Exception e) {
                    plugin.logger().warn("Failed to load description for ability " + abilityKey + ": " + e.getMessage());
                }
            } else if (!descNode.virtual()) {
                description.add(descNode.getString(""));
            }

            if (cost > 0) {
                BuyableAbility buyableAbility = new BuyableAbility(abilityKey, cost, requiredSkill, requiredLevel, displayName, description);
                buyableAbilities.put(abilityKey.toLowerCase(), buyableAbility);
                if (debugMode) {
                    plugin.logger().info("Loaded buyable ability: " + abilityKey + " = " + cost + " skill points (requires " + requiredSkill + " level " + requiredLevel + ")");
                }
            }
        }
    }

    /**
     * Sells an item for the user with queue-based cooldown tracking.
     * Each sold item adds a timestamp to the queue, and items become available
     * again as their individual cooldowns expire.
     */
    public SellResult sellItem(User user, String material, int amount) {
        material = material.toUpperCase();
        
        // Check if item is sellable
        if (!sellableItems.containsKey(material)) {
            return new SellResult(false, 0, 0.0, "This item cannot be sold");
        }
        
        // Check how many items are currently available (not on cooldown)
        int available = getRemainingAmount(user.getUuid().toString(), material);
        if (available <= 0) {
            // All slots are on cooldown, show time until next item becomes available
            long remainingCooldown = getRemainingCooldown(user.getUuid().toString(), material);
            return new SellResult(false, 0, 0.0, "cooldown:" + remainingCooldown);
        }
        
        // Can only sell up to available amount
        int actualAmount = Math.min(amount, available);
        
        double pricePerItem = sellableItems.get(material);
        double totalPrice = pricePerItem * actualAmount;
        
        // Add coins to user
        user.addSkillCoins(totalPrice);
        
        // Add timestamps for each item sold
        addCooldownTimestamps(user.getUuid().toString(), material, actualAmount);
        
        if (debugMode) {
            plugin.logger().info("User " + user.getUsername() + " sold " + actualAmount + "x " + material + " for " + totalPrice + " skill coins (" + available + " -> " + (available - actualAmount) + " available)");
        }
        
        return new SellResult(true, actualAmount, totalPrice, null);
    }

    /**
     * Buys an item for the user with stock tracking
     */
    public BuyResult buyItem(User user, String material, int amount) {
        material = material.toUpperCase();
        
        // Check if item is buyable
        BuyableItem item = buyableItems.get(material);
        if (item == null) {
            return new BuyResult(BuyResult.BuyResultType.ITEM_NOT_FOUND, 0, 0.0, "This item is not available for purchase");
        }
        
        // Check stock
        if (item.getMaxStock() != -1 && getStock(material) < amount) {
            return new BuyResult(BuyResult.BuyResultType.OUT_OF_STOCK, 0, 0.0, "Insufficient stock (available: " + getStock(material) + ")");
        }
        
        double totalCost = item.getPrice() * amount;
        
        // Check if user has enough coins
        if (user.getSkillCoins() < totalCost) {
            return new BuyResult(BuyResult.BuyResultType.INSUFFICIENT_COINS, 0, 0.0, "Insufficient skill coins");
        }
        
        // Deduct coins
        user.setSkillCoins(user.getSkillCoins() - totalCost);
        
        // Update stock
        if (item.getMaxStock() != -1) {
            setStock(material, getStock(material) - amount);
        }
        
        if (debugMode) {
            plugin.logger().info("User " + user.getUsername() + " bought " + amount + "x " + material + " for " + totalCost + " skill coins");
        }
        
        return new BuyResult(BuyResult.BuyResultType.SUCCESS, amount, totalCost, null);
    }

    /**
     * Gets remaining cooldown for the NEXT item to become available (in seconds)
     * Returns the time until the oldest cooldown expires
     */
    public long getRemainingCooldown(String uuid, String material) {
        material = material.toUpperCase();
        
        Map<String, List<Long>> userCooldowns = playerCooldowns.get(uuid);
        if (userCooldowns == null) {
            return 0;
        }
        
        List<Long> timestamps = userCooldowns.get(material);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }
        
        int cooldownSeconds = sellCooldowns.getOrDefault(material, globalSellCooldown);
        long currentTime = System.currentTimeMillis();
        
        // Find the oldest timestamp (first one that will expire)
        long oldestTimestamp = timestamps.stream().min(Long::compareTo).orElse(currentTime);
        long timePassed = (currentTime - oldestTimestamp) / 1000;
        long remaining = cooldownSeconds - timePassed;
        
        return Math.max(0, remaining);
    }
    
    /**
     * Gets the remaining amount that can be sold (how many slots are NOT on cooldown)
     * Cleans up expired cooldowns and returns available slots
     */
    public int getRemainingAmount(String uuid, String material) {
        material = material.toUpperCase();
        
        int maxAmount = maxAmounts.getOrDefault(material, 1);
        int cooldownSeconds = sellCooldowns.getOrDefault(material, globalSellCooldown);
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        
        Map<String, List<Long>> userCooldowns = playerCooldowns.get(uuid);
        if (userCooldowns == null) {
            return maxAmount; // No cooldowns = all available
        }
        
        List<Long> timestamps = userCooldowns.get(material);
        if (timestamps == null) {
            timestamps = new ArrayList<>();
            userCooldowns.put(material, timestamps);
            return maxAmount;
        }
        
        // Remove expired timestamps (cooldown has passed)
        timestamps.removeIf(timestamp -> (currentTime - timestamp) >= cooldownMillis);
        
        // Available = max - currently on cooldown
        return Math.max(0, maxAmount - timestamps.size());
    }

    /**
     * Adds cooldown timestamps for each item sold
     */
    private void addCooldownTimestamps(String uuid, String material, int amount) {
        material = material.toUpperCase();
        long currentTime = System.currentTimeMillis();
        
        Map<String, List<Long>> userCooldowns = playerCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        List<Long> timestamps = userCooldowns.computeIfAbsent(material, k -> new ArrayList<>());
        
        // Add a timestamp for each item sold
        for (int i = 0; i < amount; i++) {
            timestamps.add(currentTime);
        }
    }

    /**
     * Calculates the cost to purchase a level for a specific skill
     */
    public double calculateLevelCost(Skill skill, int currentLevel) {
        String skillKey = skill.getId().getKey();
        double baseCost = skillSpecificLevelCosts.getOrDefault(skillKey.toLowerCase(), levelBaseCost);
        return baseCost * Math.pow(levelCostMultiplier, currentLevel);
    }

    /**
     * Gets current stock for an item
     */
    public int getStock(String itemKey) {
        BuyableItem item = buyableItems.get(itemKey.toUpperCase());
        if (item == null) return 0;
        
        if (item.getMaxStock() <= 0) return Integer.MAX_VALUE; // Unlimited stock
        
        return itemStock.getOrDefault(itemKey.toUpperCase(), item.getMaxStock());
    }

    /**
     * Sets stock for an item
     */
    public void setStock(String itemKey, int stock) {
        itemStock.put(itemKey.toUpperCase(), Math.max(0, stock));
    }

    /**
     * Restocks all items to their maximum stock levels
     */
    public void restockAll() {
        for (String itemKey : buyableItems.keySet()) {
            BuyableItem item = buyableItems.get(itemKey);
            if (item.getMaxStock() > 0) {
                setStock(itemKey, item.getMaxStock());
            }
        }
        
        if (debugMode) {
            plugin.logger().info("All shop items have been restocked to maximum levels");
        }
    }

    /**
     * Restocks a specific item to its maximum stock level
     */
    public void restockItem(String itemKey) {
        BuyableItem item = buyableItems.get(itemKey.toUpperCase());
        if (item != null && item.getMaxStock() > 0) {
            setStock(itemKey, item.getMaxStock());
            
            if (debugMode) {
                plugin.logger().info("Item " + itemKey + " has been restocked to " + item.getMaxStock());
            }
        }
    }

    /**
     * Starts the automatic restock timer based on configuration
     */
    public void startAutoRestock() {
        if (restockInterval <= 0) {
            return; // Auto-restock disabled
        }

        // Start a repeating task to restock items
        plugin.getScheduler().timerSync(new TaskRunnable() {
            @Override
            public void run() {
                restockAll();
                plugin.logger().info("Auto-restock: All items restocked");
            }
        }, restockInterval, restockInterval, java.util.concurrent.TimeUnit.MINUTES);
        
        if (debugMode) {
            plugin.logger().info("Auto-restock started: Every " + restockInterval + " minutes");
        }
    }

    /**
     * Purchases a level for the user if they have enough skill coins
     */
    public LevelPurchaseResult purchaseLevel(User user, Skill skill) {
        int currentLevel = user.getSkillLevel(skill);
        int nextLevel = currentLevel + 1;
        
        // Check if level purchasing is enabled
        ConfigurationNode levelConfig = null;
        try {
            ConfigurateLoader loader = new ConfigurateLoader(plugin, TypeSerializerCollection.builder().build());
            ConfigurationNode config = loader.loadEmbeddedFile("shop_config.yml");
            levelConfig = config.node("level_purchase");
        } catch (Exception e) {
            return new LevelPurchaseResult(false, "Configuration error");
        }
        
        if (!levelConfig.node("enabled").getBoolean(true)) {
            return new LevelPurchaseResult(false, "Level purchasing is disabled");
        }
        
        // Check max purchasable level for this skill
        String skillKey = skill.getId().getKey().toLowerCase();
        int maxPurchasableLevel = skillMaxLevels.getOrDefault(skillKey, maxPurchasableLevels);
        
        if (maxPurchasableLevel > 0 && currentLevel >= maxPurchasableLevel) {
            return new LevelPurchaseResult(false, "Already at maximum purchasable level (" + maxPurchasableLevel + ")");
        }
        
        // Check natural skill max level
        int skillMaxLevel = 100; // You might want to get this from skill configuration
        if (currentLevel >= skillMaxLevel) {
            return new LevelPurchaseResult(false, "Already at max skill level");
        }
        
        double cost = calculateLevelCost(skill, currentLevel);
        
        // Check if user has enough coins
        if (user.getSkillCoins() < cost) {
            return new LevelPurchaseResult(false, "Insufficient skill coins (need " + String.format("%.0f", cost - user.getSkillCoins()) + " more)");
        }
        
        // Purchase the level
        user.setSkillCoins(user.getSkillCoins() - cost);
        user.setSkillLevel(skill, nextLevel);
        
        if (debugMode) {
            plugin.logger().info("User " + user.getUsername() + " purchased level " + nextLevel + " of " + skill.getId().getKey() + " for " + cost + " skill coins");
        }
        
        return new LevelPurchaseResult(true, "Successfully purchased level " + nextLevel + "!");
    }
    
    /**
     * Gets the maximum purchasable level for a skill
     */
    public int getMaxPurchasableLevel(Skill skill) {
        String skillKey = skill.getId().getKey().toLowerCase();
        return skillMaxLevels.getOrDefault(skillKey, maxPurchasableLevels);
    }

    /**
     * Purchases an ability for the user if they meet requirements
     */
    public AbilityPurchaseResult purchaseAbility(User user, String abilityKey) {
        BuyableAbility buyableAbility = buyableAbilities.get(abilityKey.toLowerCase());
        
        if (buyableAbility == null) {
            return new AbilityPurchaseResult(false, "Ability not available for purchase");
        }

        // Check if user already has the ability
        NamespacedId abilityId = NamespacedId.fromDefault(abilityKey);
        Ability ability = plugin.getAbilityRegistry().getOrNull(abilityId);
        if (ability == null) {
            return new AbilityPurchaseResult(false, "Ability not found");
        }

        int currentAbilityLevel = user.getAbilityLevel(ability);
        if (currentAbilityLevel > 0) {
            return new AbilityPurchaseResult(false, "Ability already unlocked");
        }

        // Check skill level requirement
        if (!buyableAbility.requiredSkill.isEmpty()) {
            NamespacedId skillId = NamespacedId.fromDefault(buyableAbility.requiredSkill);
            Skill requiredSkill = plugin.getSkillRegistry().getOrNull(skillId);
            if (requiredSkill != null) {
                int userSkillLevel = user.getSkillLevel(requiredSkill);
                if (userSkillLevel < buyableAbility.requiredLevel) {
                    return new AbilityPurchaseResult(false, "Insufficient skill level (requires " + buyableAbility.requiredSkill + " level " + buyableAbility.requiredLevel + ")");
                }
            }
        }

        // Check skill coins
        if (user.getSkillCoins() < buyableAbility.cost) {
            return new AbilityPurchaseResult(false, "Insufficient skill coins");
        }

        // Purchase the ability - Set the skill level high enough to unlock it
        Skill abilitySkill = ability.getSkill();
        int requiredSkillLevel = ability.getUnlock();
        int currentSkillLevel = user.getSkillLevel(abilitySkill);
        
        // Only increase skill level if user doesn't meet the unlock requirement
        if (currentSkillLevel < requiredSkillLevel) {
            user.setSkillLevel(abilitySkill, requiredSkillLevel);
        }
        
        user.setSkillCoins(user.getSkillCoins() - buyableAbility.cost);

        if (debugMode) {
            plugin.logger().info("User " + user.getUsername() + " purchased ability " + abilityKey + " for " + buyableAbility.cost + " skill points");
        }

        return new AbilityPurchaseResult(true, null);
    }

    /**
     * Gets all sellable items with their prices
     */
    public Map<String, Double> getSellableItems() {
        return new HashMap<>(sellableItems);
    }

    /**
     * Gets all buyable items
     */
    public Map<String, BuyableItem> getBuyableItems() {
        return new HashMap<>(buyableItems);
    }

    /**
     * Gets all buyable abilities
     */
    public Map<String, BuyableAbility> getBuyableAbilities() {
        return new HashMap<>(buyableAbilities);
    }

    /**
     * Gets the price for a specific sellable item
     */
    public Double getSellPrice(String material) {
        return sellableItems.get(material.toUpperCase());
    }

    /**
     * Gets a buyable item
     */
    public BuyableItem getBuyableItem(String material) {
        return buyableItems.get(material.toUpperCase());
    }

    /**
     * Checks if an item is sellable
     */
    public boolean isSellable(String material) {
        return sellableItems.containsKey(material.toUpperCase());
    }

    /**
     * Checks if an item is buyable
     */
    public boolean isBuyable(String material) {
        return buyableItems.containsKey(material.toUpperCase());
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public double getLevelBaseCost() {
        return levelBaseCost;
    }

    public double getLevelCostMultiplier() {
        return levelCostMultiplier;
    }

    // Result classes
    public static class SellResult {
        public final boolean success;
        public final int amount;
        public final double totalPrice;
        public final String errorMessage;

        public SellResult(boolean success, int amount, double totalPrice, String errorMessage) {
            this.success = success;
            this.amount = amount;
            this.totalPrice = totalPrice;
            this.errorMessage = errorMessage;
        }
    }

    public static class BuyResult {
        public enum BuyResultType {
            SUCCESS,
            INSUFFICIENT_COINS,
            OUT_OF_STOCK,
            ITEM_NOT_FOUND,
            MAX_LEVEL_REACHED,
            ERROR
        }

        private final BuyResultType type;
        private final int amount;
        private final double totalCost;
        private final String errorMessage;

        public BuyResult(BuyResultType type, int amount, double totalCost, String errorMessage) {
            this.type = type;
            this.amount = amount;
            this.totalCost = totalCost;
            this.errorMessage = errorMessage;
        }

        public BuyResultType getType() {
            return type;
        }

        public boolean isSuccess() {
            return type == BuyResultType.SUCCESS;
        }

        public int getAmount() {
            return amount;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static class AbilityPurchaseResult {
        public final boolean success;
        public final String errorMessage;

        public AbilityPurchaseResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    public static class BuyableItem {
        private final String material;
        private final String displayName;
        private final int amount;
        private final double price;
        private final int maxStock;

        public BuyableItem(String material, String displayName, int amount, double price, int maxStock) {
            this.material = material;
            this.displayName = displayName;
            this.amount = amount;
            this.price = price;
            this.maxStock = maxStock;
        }

        public String getMaterial() {
            return material;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getAmount() {
            return amount;
        }

        public double getPrice() {
            return price;
        }

        public int getMaxStock() {
            return maxStock;
        }
    }

    public static class LevelPurchaseResult {
        public final boolean success;
        public final String message;

        public LevelPurchaseResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static class BuyableAbility {
        public final String abilityKey;
        public final double cost;
        public final String requiredSkill;
        public final int requiredLevel;
        public final String displayName;
        public final java.util.List<String> description;

        public BuyableAbility(String abilityKey, double cost, String requiredSkill, int requiredLevel, String displayName, java.util.List<String> description) {
            this.abilityKey = abilityKey;
            this.cost = cost;
            this.requiredSkill = requiredSkill;
            this.requiredLevel = requiredLevel;
            this.displayName = displayName;
            this.description = description;
        }
    }
}
