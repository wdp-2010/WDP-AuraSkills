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
        Inventory inventory = Bukkit.createInventory(null, 54, "§dAbility Shop");
        
        User user = plugin.getUser(player);
        
        // Fill with black glass panes (consistent with MainShopMenu and skills menu)
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        
        // Balance info at top left (consistent with MainShopMenu)
        ItemStack balanceItem = createBalanceItem(user);
        inventory.setItem(0, balanceItem);
        
        // Populate abilities in organized layout
        populateAbilityShop(inventory, user);
        
        // Navigation items at bottom
        addNavigationItems(inventory);
        
        player.openInventory(inventory);
    }
    
    private ItemStack createBalanceItem(User user) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Skill Coins Balance");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Your current balance:");
        lore.add("§e" + String.format("%.0f", user.getSkillCoins()) + " ⛁");
        lore.add(" ");
        lore.add("§8Purchase exclusive abilities below");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private void populateAbilityShop(Inventory inventory, User user) {
        Map<String, SkillPointsShop.BuyableAbility> buyableAbilities = shop.getBuyableAbilities();
        
        // Organized layout: abilities arranged by skill category
        // Using 3 columns, centered in the inventory
        int[] slots = {
            10, 11, 12,  // Row 1 - Farming, Mining, Fishing abilities
            19, 20, 21,  // Row 2 - Excavation, Archery, Defense abilities  
            28, 29, 30   // Row 3 - Fighting, Enchanting abilities
        };
        
        int slotIndex = 0;
        for (Map.Entry<String, SkillPointsShop.BuyableAbility> entry : buyableAbilities.entrySet()) {
            if (slotIndex >= slots.length) break; // Don't overflow available slots
            
            String abilityKey = entry.getKey();
            SkillPointsShop.BuyableAbility buyableAbility = entry.getValue();
            
            ItemStack abilityItem = createAbilityItem(abilityKey, buyableAbility, user);
            inventory.setItem(slots[slotIndex], abilityItem);
            
            slotIndex++;
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
    
    private void addNavigationItems(Inventory inventory) {
        // Back button at bottom left (slot 45 - consistent with MainShopMenu)
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§eBack to Shop");
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to main menu");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inventory.setItem(45, backItem);
        
        // Close button at bottom right (slot 53 - consistent with MainShopMenu)
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§cClose");
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(53, closeItem);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§dAbility Shop")) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        int slot = event.getSlot();
        
        // Handle navigation buttons
        if (slot == 45) { // Back button
            player.closeInventory();
            plugin.getMainShopMenu().openMainMenu(player);
            return;
        }
        
        if (slot == 53) { // Close button
            player.closeInventory();
            return;
        }
        
        // Handle ability purchase
        handleAbilityPurchase(player, slot);
    }
    
    private void handleAbilityPurchase(Player player, int slot) {
        Map<String, SkillPointsShop.BuyableAbility> buyableAbilities = shop.getBuyableAbilities();
        
        // Map slots to ability indices
        int[] validSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        int abilityIndex = -1;
        
        for (int i = 0; i < validSlots.length; i++) {
            if (validSlots[i] == slot) {
                abilityIndex = i;
                break;
            }
        }
        
        if (abilityIndex < 0 || abilityIndex >= buyableAbilities.size()) {
            return; // Invalid slot or no ability in this position
        }
        
        // Get the ability at this index
        String[] abilityKeys = buyableAbilities.keySet().toArray(new String[0]);
        String abilityKey = abilityKeys[abilityIndex];
        SkillPointsShop.BuyableAbility buyableAbility = buyableAbilities.get(abilityKey);
        
        purchaseAbility(player, abilityKey, buyableAbility);
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
