package dev.aurelium.auraskills.common.economy;

import dev.aurelium.auraskills.api.ability.Ability;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.config.ConfigurateLoader;
import dev.aurelium.auraskills.common.user.User;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkillPointsShop {

    private final AuraSkillsPlugin plugin;
    private final Map<String, Double> sellableItems;
    private final Map<String, BuyableItem> buyableItems;
    private final Map<String, BuyableAbility> buyableAbilities;
    private final Map<String, Integer> sellCooldowns; // Material -> cooldown seconds
    private final Map<String, Map<String, Long>> playerCooldowns; // UUID -> (Material -> lastSellTime)
    private final Map<String, Integer> itemStock; // Item -> current stock
    private int globalSellCooldown;
    private double levelBaseCost;
    private double levelCostMultiplier;
    private final Map<String, Double> skillSpecificLevelCosts;
    private boolean debugMode;

    public SkillPointsShop(AuraSkillsPlugin plugin) {
        this.plugin = plugin;
        this.sellableItems = new ConcurrentHashMap<>();
        this.buyableItems = new ConcurrentHashMap<>();
        this.buyableAbilities = new ConcurrentHashMap<>();
        this.sellCooldowns = new ConcurrentHashMap<>();
        this.playerCooldowns = new ConcurrentHashMap<>();
        this.itemStock = new ConcurrentHashMap<>();
        this.skillSpecificLevelCosts = new ConcurrentHashMap<>();
        loadConfiguration();
    }

    public void loadConfiguration() {
        sellableItems.clear();
        buyableItems.clear();
        buyableAbilities.clear();
        sellCooldowns.clear();
        skillSpecificLevelCosts.clear();

        try {
            ConfigurateLoader loader = new ConfigurateLoader(plugin, TypeSerializerCollection.builder().build());
            ConfigurationNode config = loader.loadEmbeddedFile("shop_config.yml");

            // Load debug mode
            debugMode = config.node("debug_mode").getBoolean(false);

            // Load global sell cooldown
            globalSellCooldown = config.node("sell_items", "global_cooldown").getInt(0);

            // Load level purchase settings
            ConfigurationNode levelConfig = config.node("level_purchase");
            levelBaseCost = levelConfig.node("base_cost").getDouble(100.0);
            levelCostMultiplier = levelConfig.node("cost_multiplier").getDouble(1.5);

            // Load skill-specific costs
            ConfigurationNode skillCostsNode = levelConfig.node("skill_specific_costs");
            if (!skillCostsNode.virtual()) {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : skillCostsNode.childrenMap().entrySet()) {
                    String skillName = entry.getKey().toString();
                    double cost = entry.getValue().getDouble(levelBaseCost);
                    skillSpecificLevelCosts.put(skillName.toLowerCase(), cost);
                }
            }

            // Load sellable items
            loadSellableItems(config.node("sell_items", "items"));

            // Load buyable items
            loadBuyableItems(config.node("buy_items"));

            // Load buyable abilities
            loadBuyableAbilities(config.node("buyable_abilities"));

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
                
                if (debugMode) {
                    plugin.logger().info("Loaded sellable item: " + material + " = " + price + " skill points (cooldown: " + cooldown + "s)");
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
            String requiredSkill = abilityNode.node("required_skill").getString("");
            int requiredLevel = abilityNode.node("required_level").getInt(0);

            if (cost > 0) {
                BuyableAbility buyableAbility = new BuyableAbility(abilityKey, cost, requiredSkill, requiredLevel);
                buyableAbilities.put(abilityKey.toLowerCase(), buyableAbility);
                if (debugMode) {
                    plugin.logger().info("Loaded buyable ability: " + abilityKey + " = " + cost + " skill points (requires " + requiredSkill + " level " + requiredLevel + ")");
                }
            }
        }
    }

    /**
     * Sells an item for the user with cooldown checking
     */
    public SellResult sellItem(User user, String material, int amount) {
        material = material.toUpperCase();
        
        // Check if item is sellable
        if (!sellableItems.containsKey(material)) {
            return new SellResult(false, 0, 0.0, "This item cannot be sold");
        }
        
        // Check cooldown
        long remainingCooldown = getRemainingCooldown(user.getUuid().toString(), material);
        if (remainingCooldown > 0) {
            return new SellResult(false, 0, 0.0, "cooldown:" + remainingCooldown);
        }
        
        double pricePerItem = sellableItems.get(material);
        double totalPrice = pricePerItem * amount;
        
        // Add coins to user
        user.addSkillCoins(totalPrice);
        
        // Set cooldown
        setCooldown(user.getUuid().toString(), material);
        
        if (debugMode) {
            plugin.logger().info("User " + user.getUsername() + " sold " + amount + "x " + material + " for " + totalPrice + " skill coins");
        }
        
        return new SellResult(true, amount, totalPrice, null);
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
     * Gets remaining cooldown for selling an item in seconds
     */
    public long getRemainingCooldown(String uuid, String material) {
        material = material.toUpperCase();
        
        Map<String, Long> userCooldowns = playerCooldowns.get(uuid);
        if (userCooldowns == null) {
            return 0;
        }
        
        Long lastSellTime = userCooldowns.get(material);
        if (lastSellTime == null) {
            return 0;
        }
        
        int cooldownSeconds = sellCooldowns.getOrDefault(material, globalSellCooldown);
        long timePassed = (System.currentTimeMillis() - lastSellTime) / 1000;
        long remaining = cooldownSeconds - timePassed;
        
        return Math.max(0, remaining);
    }

    /**
     * Sets a cooldown for selling an item
     */
    private void setCooldown(String uuid, String material) {
        material = material.toUpperCase();
        playerCooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(material, System.currentTimeMillis());
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
     * Purchases a level for the user if they have enough skill coins
     */
    public BuyResult purchaseLevel(User user, Skill skill) {
        int currentLevel = user.getSkillLevel(skill);
        int nextLevel = currentLevel + 1;
        
        // Check if already at max level
        int maxLevel = 100; // Default max level
        if (currentLevel >= maxLevel) {
            return new BuyResult(BuyResult.BuyResultType.MAX_LEVEL_REACHED, 0, 0.0, "Already at max level");
        }
        
        double cost = calculateLevelCost(skill, nextLevel);

        if (user.getSkillCoins() < cost) {
            return new BuyResult(BuyResult.BuyResultType.INSUFFICIENT_COINS, 0, cost, "Insufficient skill coins");
        }

        user.setSkillCoins(user.getSkillCoins() - cost);
        user.setSkillLevel(skill, nextLevel);

        if (debugMode) {
            plugin.logger().info("User " + user.getUsername() + " purchased level for " + skill.getId().getKey() + " (cost: " + cost + ")");
        }

        return new BuyResult(BuyResult.BuyResultType.SUCCESS, 1, cost, null);
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

    public static class BuyableAbility {
        public final String abilityKey;
        public final double cost;
        public final String requiredSkill;
        public final int requiredLevel;

        public BuyableAbility(String abilityKey, double cost, String requiredSkill, int requiredLevel) {
            this.abilityKey = abilityKey;
            this.cost = cost;
            this.requiredSkill = requiredSkill;
            this.requiredLevel = requiredLevel;
        }
    }
}
