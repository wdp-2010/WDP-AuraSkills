package dev.aurelium.auraskills.bukkit.economy;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.CooldownTrackerMenu;
import dev.aurelium.auraskills.bukkit.menus.LevelShopMenu;
import dev.aurelium.auraskills.bukkit.menus.MainShopMenu;
import dev.aurelium.auraskills.common.economy.SkillPointsShop;

/**
 * Manager for the centralized shop system.
 * Provides a single SkillPointsShop instance to all menus.
 */
public class ShopManager {
    
    private final AuraSkills plugin;
    private SkillPointsShop shop;
    private LevelShopMenu levelShopMenu;
    private MainShopMenu mainShopMenu;
    private CooldownTrackerMenu cooldownTrackerMenu;
    
    public ShopManager(AuraSkills plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Initialize the shop instance with configuration.
     * Should be called after config loading but before menu registration.
     */
    public void initialize() {
        if (shop == null) {
            shop = new SkillPointsShop(plugin);
            levelShopMenu = new LevelShopMenu(plugin);
            mainShopMenu = new MainShopMenu(plugin);
            cooldownTrackerMenu = new CooldownTrackerMenu(plugin);
            
            // Start auto-restock system if configured
            shop.startAutoRestock();
            
            plugin.getLogger().info("Shop system initialized");
        }
    }
    
    /**
     * Get the shop instance.
     * @return the centralized SkillPointsShop instance
     * @throws IllegalStateException if shop is not initialized
     */
    public SkillPointsShop getShop() {
        if (shop == null) {
            throw new IllegalStateException("Shop not initialized. Call initialize() first.");
        }
        return shop;
    }
    
    /**
     * Get the level shop menu instance.
     * @return the LevelShopMenu instance
     * @throws IllegalStateException if shop is not initialized
     */
    public LevelShopMenu getLevelShopMenu() {
        if (levelShopMenu == null) {
            throw new IllegalStateException("Shop not initialized. Call initialize() first.");
        }
        return levelShopMenu;
    }

    /**
     * Get the main shop menu instance.
     * @return the MainShopMenu instance
     * @throws IllegalStateException if shop is not initialized
     */
    public MainShopMenu getMainShopMenu() {
        if (mainShopMenu == null) {
            throw new IllegalStateException("Shop not initialized. Call initialize() first.");
        }
        return mainShopMenu;
    }

    /**
     * Get the cooldown tracker menu instance.
     * @return the CooldownTrackerMenu instance
     * @throws IllegalStateException if shop is not initialized
     */
    public CooldownTrackerMenu getCooldownTrackerMenu() {
        if (cooldownTrackerMenu == null) {
            throw new IllegalStateException("Shop not initialized. Call initialize() first.");
        }
        return cooldownTrackerMenu;
    }
    
    /**
     * Check if the shop is initialized.
     * @return true if shop is available
     */
    public boolean isInitialized() {
        return shop != null;
    }
    
    /**
     * Reload the shop configuration.
     */
    public void reload() {
        if (shop != null) {
            shop.loadConfiguration();
            plugin.getLogger().info("Shop configuration reloaded");
        }
    }
}