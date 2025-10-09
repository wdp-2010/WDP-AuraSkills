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
    private final Map<String, BuyableAbility> buyableAbilities;
    private double levelBaseCost;
    private double levelCostMultiplier;
    private final Map<String, Double> skillSpecificLevelCosts;
    private boolean debugMode;

    public SkillPointsShop(AuraSkillsPlugin plugin) {
        this.plugin = plugin;
        this.sellableItems = new ConcurrentHashMap<>();
        this.buyableAbilities = new ConcurrentHashMap<>();
        this.skillSpecificLevelCosts = new ConcurrentHashMap<>();
        loadConfiguration();
    }

    public void loadConfiguration() {
        sellableItems.clear();
        buyableAbilities.clear();
        skillSpecificLevelCosts.clear();

        try {
            ConfigurateLoader loader = new ConfigurateLoader(plugin, TypeSerializerCollection.builder().build());
            ConfigurationNode config = loader.loadEmbeddedFile("shop_config.yml");

            // Load debug mode
            debugMode = config.node("debug_mode").getBoolean(false);

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
            loadSellableItems(config.node("sellable_items"));

            // Load buyable abilities
            loadBuyableAbilities(config.node("buyable_abilities"));

            if (debugMode) {
                plugin.logger().info("Shop configuration loaded: " + sellableItems.size() + " sellable items, " + buyableAbilities.size() + " buyable abilities");
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
                if (debugMode) {
                    plugin.logger().info("Loaded sellable item: " + material + " = " + price + " skill points");
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
     * Calculates the cost to purchase a level for a specific skill
     */
    public double calculateLevelCost(Skill skill, int currentLevel) {
        String skillKey = skill.getId().getKey();
        double baseCost = skillSpecificLevelCosts.getOrDefault(skillKey.toLowerCase(), levelBaseCost);
        return baseCost * Math.pow(levelCostMultiplier, currentLevel);
    }

    /**
     * Purchases a level for the user if they have enough skill coins
     */
    public boolean purchaseLevel(User user, Skill skill) {
        int currentLevel = user.getSkillLevel(skill);
        double cost = calculateLevelCost(skill, currentLevel);

        if (user.getSkillCoins() >= cost) {
            user.setSkillCoins(user.getSkillCoins() - cost);
            user.setSkillLevel(skill, currentLevel + 1);

            if (debugMode) {
                plugin.logger().info("User " + user.getUsername() + " purchased level for " + skill.getId().getKey() + " (cost: " + cost + ")");
            }

            return true;
        }

        return false;
    }

    /**
     * Sells an item stack for skill coins
     */
    public SellResult sellItem(User user, String material, int amount) {
        Double pricePerItem = sellableItems.get(material.toUpperCase());
        
        if (pricePerItem == null) {
            return new SellResult(false, 0, 0, "Item not sellable");
        }

        double totalPrice = pricePerItem * amount;
        user.setSkillCoins(user.getSkillCoins() + totalPrice);

        if (debugMode) {
            plugin.logger().info("User " + user.getUsername() + " sold " + amount + "x " + material + " for " + totalPrice + " skill points");
        }

        return new SellResult(true, amount, totalPrice, null);
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
     * Checks if an item is sellable
     */
    public boolean isSellable(String material) {
        return sellableItems.containsKey(material.toUpperCase());
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

    public static class AbilityPurchaseResult {
        public final boolean success;
        public final String errorMessage;

        public AbilityPurchaseResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
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
