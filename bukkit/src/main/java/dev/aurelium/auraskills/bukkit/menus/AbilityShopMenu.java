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
        User user = plugin.getUser(player);
        String title = "§dAbility Shop §8| §e" + String.format("%.0f", user.getSkillCoins()) + " ⛁";
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        
        addDecorativeBorder(inventory);
        populateAbilityShop(inventory, player);
        addNavigationItems(inventory, player);
        
        player.openInventory(inventory);
    }
    
    private void addDecorativeBorder(Inventory inventory) {
        ItemStack borderPane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderPane.setItemMeta(borderMeta);
        
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inventory.setItem(slot, borderPane);
        }
    }
    
    private void populateAbilityShop(Inventory inventory, Player player) {
        Map<String, SkillPointsShop.BuyableAbility> buyableAbilities = shop.getBuyableAbilities();
        User user = plugin.getUser(player);
        
        int row = 1; // Start at row 1 (skip top border row 0)
        int col = 1; // Start at column 1 (skip left border column 0)
        int maxCols = 7; // Columns 1-7 (leave 8 for right border)
        int maxRows = 4; // Rows 1-4 (leave row 5 for navigation)
        
        for (Map.Entry<String, SkillPointsShop.BuyableAbility> entry : buyableAbilities.entrySet()) {
            String abilityKey = entry.getKey();
            SkillPointsShop.BuyableAbility buyableAbility = entry.getValue();
            
            if (row > maxRows) break; // Don't overflow into navigation area
            
            // Calculate slot position (row * 9 + col)
            int slot = row * 9 + col;
            
            ItemStack abilityItem = createAbilityItem(abilityKey, buyableAbility, user);
            inventory.setItem(slot, abilityItem);
            
            // Move to next position
            col++;
            if (col > maxCols) {
                col = 1; // Reset to first content column
                row++;
            }
        }
    }
    
    private ItemStack createAbilityItem(String abilityKey, SkillPointsShop.BuyableAbility buyableAbility, User user) {
        NamespacedId abilityId = NamespacedId.fromDefault(abilityKey);
        Ability ability = plugin.getAbilityRegistry().getOrNull(abilityId);
        
        // Check if player has PURCHASED the ability (not just unlocked it)
        boolean hasPurchased = user.hasPurchasedAbility(abilityKey);
        
        ItemStack item = new ItemStack(hasPurchased ? Material.BOOK : Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        
        String displayName = buyableAbility.displayName != null && !buyableAbility.displayName.isEmpty() 
            ? convertColorCodes(buyableAbility.displayName)
            : (ability != null ? ability.getDisplayName(user.getLocale()) : abilityKey);
        
        // Add purchase indicator to display name
        meta.setDisplayName((hasPurchased ? "§a✓ " : "§d") + displayName);
        
        List<String> lore = new ArrayList<>();
        
        // Add description from shop_config.yml (cleaner, more detailed)
        if (buyableAbility.description != null && !buyableAbility.description.isEmpty()) {
            for (String descLine : buyableAbility.description) {
                lore.add(convertColorCodes(descLine));
            }
        } else if (ability != null) {
            lore.add("§7" + ability.getDescription(user.getLocale()));
        }
        
        lore.add("");
        
        if (hasPurchased) {
            // Show purchase status and current level
            lore.add("§a✓ Purchased");
            if (ability != null) {
                int abilityLevel = user.getAbilityLevel(ability);
                if (abilityLevel > 0) {
                    lore.add("§8Current Level: §7" + abilityLevel);
                } else {
                    lore.add("§8Level up your skill to unlock!");
                }
            }
        } else {
            // Show requirements and purchase info
            boolean canAfford = user.getSkillCoins() >= buyableAbility.cost;
            boolean meetsLevelReq = true;
            
            if (buyableAbility.requiredSkill != null && !buyableAbility.requiredSkill.isEmpty()) {
                NamespacedId skillId = NamespacedId.fromDefault(buyableAbility.requiredSkill);
                Skill requiredSkill = plugin.getSkillRegistry().getOrNull(skillId);
                int playerLevel = requiredSkill != null ? user.getSkillLevel(requiredSkill) : 0;
                meetsLevelReq = playerLevel >= buyableAbility.requiredLevel;
            }
            
            lore.add("§7Status:");
            if (canAfford && meetsLevelReq) {
                lore.add("  §a✓ All requirements met");
                lore.add("");
                lore.add("§e▶ Click to purchase!");
            } else {
                if (!canAfford) {
                    double needed = buyableAbility.cost - user.getSkillCoins();
                    lore.add("  §c✗ Need " + String.format("%.0f", needed) + " more coins");
                } else {
                    lore.add("  §a✓ Sufficient coins");
                }
                
                if (!meetsLevelReq) {
                    lore.add("  §c✗ Skill level too low");
                } else {
                    lore.add("  §a✓ Level requirement met");
                }
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private void addNavigationItems(Inventory inventory, Player player) {
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§eBack to Shop");
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to main menu");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inventory.setItem(45, backItem);
        
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§cClose");
        List<String> closeLore = new ArrayList<>();
        closeLore.add("§7Close the menu");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(53, closeItem);
        
        User user = plugin.getUser(player);
        ItemStack balanceItem = new ItemStack(Material.SUNFLOWER);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        balanceMeta.setDisplayName("§6Your Balance");
        List<String> balanceLore = new ArrayList<>();
        balanceLore.add("§e" + String.format("%.0f", user.getSkillCoins()) + " ⛁");
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
        // Abilities are arranged in a 7-column grid starting at row 1, column 1
        int row = slot / 9;
        int col = slot % 9;
        
        // Check if slot is in valid ability area (rows 1-4, columns 1-7)
        if (row < 1 || row > 4 || col < 1 || col > 7) {
            return -1;
        }
        
        // Calculate ability index based on grid position
        // Row 1 col 1 = index 0, row 1 col 2 = index 1, etc.
        return (row - 1) * 7 + (col - 1);
    }
    
    private void purchaseAbility(Player player, String abilityKey, SkillPointsShop.BuyableAbility buyableAbility) {
        User user = plugin.getUser(player);
        
        try {
            SkillPointsShop.AbilityPurchaseResult result = shop.purchaseAbility(user, abilityKey);
            
            if (result.success) {
                String displayName = buyableAbility.displayName != null && !buyableAbility.displayName.isEmpty() 
                    ? convertColorCodes(buyableAbility.displayName) : abilityKey;
                player.sendMessage("§aPurchased " + displayName);
                openAbilityShop(player);
            } else {
                player.sendMessage("§c" + result.errorMessage);
            }
            
        } catch (Exception e) {
            player.sendMessage("§cPurchase failed - please try again");
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Nothing special needed for cleanup
    }
    
    public void build(Object menu) {
        // Compatibility method for MenuRegistrar pattern
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
