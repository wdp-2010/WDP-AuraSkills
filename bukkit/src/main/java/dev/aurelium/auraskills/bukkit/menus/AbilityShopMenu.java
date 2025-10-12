package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.ability.Ability;
import dev.aurelium.auraskills.api.registry.NamespacedId;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.economy.SkillPointsShop;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AbilityShopMenu implements Listener {
    
    private final AuraSkills plugin;
    private final SkillPointsShop shop;
    
    public AbilityShopMenu(AuraSkills plugin) {
        this.plugin = plugin;
        this.shop = plugin.getShopManager().getShop();
    }
    
    public void openAbilityShop(Player player) {
        plugin.getLogger().info("AbilityShopMenu - Opening ability shop for " + player.getName());
        
        Inventory inventory = Bukkit.createInventory(null, 54, "§d§lAbility Shop");
        
        // Add decorative border
        addDecorativeBorder(inventory);
        
        populateAbilityShop(inventory, player);
        
        // Add navigation items
        addNavigationItems(inventory, player);
        
        player.openInventory(inventory);
    }
    
    private void addDecorativeBorder(Inventory inventory) {
        // Add decorative glass panes for visual organization
        ItemStack borderPane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        borderMeta.setDisplayName("§8");  // Empty name
        borderPane.setItemMeta(borderMeta);
        
        // Top and bottom borders
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inventory.setItem(slot, borderPane);
        }
    }
    
    private void populateAbilityShop(Inventory inventory, Player player) {
        plugin.getLogger().info("AbilityShopMenu - Populating ability shop");
        
        Map<String, SkillPointsShop.BuyableAbility> buyableAbilities = shop.getBuyableAbilities();
        User user = plugin.getUser(player);
        
        int row = 0;
        int col = 0;
        int maxCols = 7; // Leave slots 7 and 8 for navigation/decoration
        int maxRows = 5; // Rows 0-4, leaving row 5 for navigation
        
        for (Map.Entry<String, SkillPointsShop.BuyableAbility> entry : buyableAbilities.entrySet()) {
            String abilityKey = entry.getKey();
            SkillPointsShop.BuyableAbility buyableAbility = entry.getValue();
            
            // Calculate slot position (row * 9 + col)
            int slot = row * 9 + col;
            
            if (row >= maxRows) break; // Don't overflow into navigation area
            
            ItemStack abilityItem = createAbilityItem(abilityKey, buyableAbility, user);
            inventory.setItem(slot, abilityItem);
            
            // Move to next position
            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }
    }
    
    private ItemStack createAbilityItem(String abilityKey, SkillPointsShop.BuyableAbility buyableAbility, User user) {
        // Try to get the actual ability object for checking if player owns it
        NamespacedId abilityId = NamespacedId.fromDefault(abilityKey);
        Ability ability = plugin.getAbilityRegistry().getOrNull(abilityId);
        
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        // Use display name from shop config or fallback to ability key
        String displayName = buyableAbility.displayName != null && !buyableAbility.displayName.isEmpty() 
            ? convertColorCodes(buyableAbility.displayName)
            : (ability != null ? ability.getDisplayName(user.getLocale()) : abilityKey);
        meta.setDisplayName("§d§l" + displayName);
        
        List<String> lore = new ArrayList<>();
        
        // Add custom description from shop config
        if (buyableAbility.description != null && !buyableAbility.description.isEmpty()) {
            for (String descLine : buyableAbility.description) {
                lore.add("§7" + convertColorCodes(descLine));
            }
        } else if (ability != null) {
            // Fallback to API description
            lore.add("§7" + ability.getDescription(user.getLocale()));
        }
        lore.add("");
        
        // Check if player already has this ability
        boolean hasAbility = ability != null && user.getAbilityLevel(ability) > 0;
        
        if (hasAbility) {
            lore.add("§a§l✓ Already Purchased");
            item.setType(Material.BOOK); // Different icon for owned abilities
        } else {
            // Price
            lore.add("§6Price: §e" + buyableAbility.cost + " §7Skill Coins");
            lore.add("");
            
            // Requirements
            if (buyableAbility.requiredSkill != null && !buyableAbility.requiredSkill.isEmpty()) {
                NamespacedId skillId = NamespacedId.fromDefault(buyableAbility.requiredSkill);
                Skill requiredSkill = plugin.getSkillRegistry().getOrNull(skillId);
                String skillName = requiredSkill != null ? requiredSkill.getDisplayName(user.getLocale()) : buyableAbility.requiredSkill;
                
                int playerLevel = requiredSkill != null ? user.getSkillLevel(requiredSkill) : 0;
                boolean meetsRequirement = playerLevel >= buyableAbility.requiredLevel;
                
                if (meetsRequirement) {
                    lore.add("§7▸ Requirement: §a§l✓ " + skillName + " Level " + buyableAbility.requiredLevel);
                } else {
                    lore.add("§7▸ Requirement: §c§l✗ " + skillName + " Level " + buyableAbility.requiredLevel);
                    lore.add("§c  (You have level " + playerLevel + ")");
                }
            }
            
            lore.add("");
            
            // Check if player can afford
            double playerBalance = user.getSkillCoins();
            if (playerBalance >= buyableAbility.cost) {
                lore.add("§a§lClick to purchase!");
            } else {
                lore.add("§c§lInsufficient Skill Coins!");
                lore.add("§c(Need " + (buyableAbility.cost - playerBalance) + " more)");
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private void addNavigationItems(Inventory inventory, Player player) {
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§c§l← Back to Main Shop");
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to the main shop menu");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inventory.setItem(45, backItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c§lClose");
        List<String> closeLore = new ArrayList<>();
        closeLore.add("§7Close the shop menu");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(53, closeItem);
        
        // Player balance info
        User user = plugin.getUser(player);
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        balanceMeta.setDisplayName("§6§lYour Balance");
        List<String> balanceLore = new ArrayList<>();
        balanceLore.add("§7Skill Coins: §e§l" + String.format("%.0f", user.getSkillCoins()));
        balanceLore.add("");
        balanceLore.add("§7Use skill coins to purchase");
        balanceLore.add("§7new abilities and upgrades!");
        balanceMeta.setLore(balanceLore);
        balanceItem.setItemMeta(balanceMeta);
        inventory.setItem(49, balanceItem);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("Ability Shop")) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        int slot = event.getSlot();
        
        // Handle navigation
        if (slot == 45) { // Back button
            plugin.getMainShopMenu().openMainMenu(player);
            return;
        }
        
        if (slot == 53) { // Close button
            player.closeInventory();
            return;
        }
        
        if (slot == 49) { // Balance info - do nothing
            return;
        }
        
        // Handle ability purchase
        handleAbilityPurchase(player, slot);
    }
    
    private void handleAbilityPurchase(Player player, int slot) {
        Map<String, SkillPointsShop.BuyableAbility> buyableAbilities = shop.getBuyableAbilities();
        
        // Calculate which ability this slot corresponds to
        int abilityIndex = getAbilityIndexFromSlot(slot);
        if (abilityIndex < 0 || abilityIndex >= buyableAbilities.size()) {
            return; // Invalid slot
        }
        
        // Get the ability at this index
        String[] abilityKeys = buyableAbilities.keySet().toArray(new String[0]);
        String abilityKey = abilityKeys[abilityIndex];
        SkillPointsShop.BuyableAbility buyableAbility = buyableAbilities.get(abilityKey);
        
        purchaseAbility(player, abilityKey, buyableAbility);
    }
    
    private int getAbilityIndexFromSlot(int slot) {
        // Convert slot position back to ability index for grid layout
        // Abilities are arranged in a 7x5 grid (7 columns, 5 rows)
        int row = slot / 9 + 1;
        int col = slot % 9 + 1;
        
        // Check if slot is in valid ability area (columns 0-6, rows 0-4)
        if (row >= 5 || col >= 7) return -1;
        
        // Calculate ability index based on grid position
        return row * 7 + col;
    }
    
    private void purchaseAbility(Player player, String abilityKey, SkillPointsShop.BuyableAbility buyableAbility) {
        User user = plugin.getUser(player);
        
        try {
            // Use the shop's purchase method
            SkillPointsShop.AbilityPurchaseResult result = shop.purchaseAbility(user, abilityKey);
            
            if (result.success) {
                String displayName = buyableAbility.displayName != null && !buyableAbility.displayName.isEmpty() 
                    ? convertColorCodes(buyableAbility.displayName) : abilityKey;
                player.sendMessage("§a§lPurchase Successful! §7You learned the " + displayName + " ability!");
                // Refresh the menu to show updated state
                openAbilityShop(player);
            } else {
                player.sendMessage("§c§lError: §7" + result.errorMessage);
                
                // Provide more specific feedback based on common error types
                if (result.errorMessage.contains("Insufficient skill coins")) {
                    double needed = buyableAbility.cost - user.getSkillCoins();
                    player.sendMessage("§c§lYou need " + needed + " more skill coins.");
                } else if (result.errorMessage.contains("requirements")) {
                    if (buyableAbility.requiredSkill != null) {
                        NamespacedId skillId = NamespacedId.fromDefault(buyableAbility.requiredSkill);
                        Skill requiredSkill = plugin.getSkillRegistry().getOrNull(skillId);
                        String skillName = requiredSkill != null ? requiredSkill.getDisplayName(user.getLocale()) : buyableAbility.requiredSkill;
                        player.sendMessage("§c§lRequired: " + skillName + " Level " + buyableAbility.requiredLevel);
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error purchasing ability " + abilityKey + " for player " + player.getName() + ": " + e.getMessage());
            player.sendMessage("§c§lError: §7An error occurred during purchase. Please try again.");
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Nothing special needed for cleanup
    }
    
    // Method for MenuRegistrar compatibility
    public void build(Object menu) {
        // This method exists for compatibility with the MenuRegistrar pattern
        // The actual menu building is handled by openAbilityShop()
        plugin.getLogger().info("AbilityShopMenu - build() method called (compatibility mode)");
    }
    
    /**
     * Converts MiniMessage format color codes to legacy Minecraft color codes
     */
    private String convertColorCodes(String text) {
        if (text == null) return "";
        
        return text
            .replace("<black>", "§0")
            .replace("<dark_blue>", "§1")
            .replace("<dark_green>", "§2")
            .replace("<dark_aqua>", "§3")
            .replace("<dark_red>", "§4")
            .replace("<dark_purple>", "§5")
            .replace("<gold>", "§6")
            .replace("<gray>", "§7")
            .replace("<dark_gray>", "§8")
            .replace("<blue>", "§9")
            .replace("<green>", "§a")
            .replace("<aqua>", "§b")
            .replace("<red>", "§c")
            .replace("<light_purple>", "§d")
            .replace("<yellow>", "§e")
            .replace("<white>", "§f")
            .replace("<bold>", "§l")
            .replace("<italic>", "§o")
            .replace("<underlined>", "§n")
            .replace("<strikethrough>", "§m")
            .replace("<obfuscated>", "§k")
            .replace("<reset>", "§r");
    }
}
