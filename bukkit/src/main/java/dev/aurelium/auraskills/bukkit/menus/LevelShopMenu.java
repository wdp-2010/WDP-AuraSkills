package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelShopMenu implements Listener {
    
    private final AuraSkills plugin;
    private final SkillPointsShop shop;
    
    public LevelShopMenu(AuraSkills plugin) {
        this.plugin = plugin;
        this.shop = plugin.getShopManager().getShop();
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openLevelShop(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, "§eLevel Shop");
        
        User user = plugin.getUser(player);
        
        // Fill with black glass panes like skills menu
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, filler);
        }
        
        // Balance info at top left (like skills menu)
        ItemStack balanceItem = createBalanceItem(user);
        inventory.setItem(0, balanceItem);
        
        // Add skill categories with organized layout
        addSkillCategories(inventory, user);
        
        // Add navigation items
        addNavigationItems(inventory);
        
        player.openInventory(inventory);
    }
    
    
    private void addSkillCategories(Inventory inventory, User user) {
        // Organize skills by categories for better UX
        
        // Combat Skills (Top Row - slots 10-16)
        Skill[] combatSkills = {Skills.FIGHTING, Skills.ARCHERY, Skills.DEFENSE};
        int[] combatSlots = {10, 11, 12};
        addSkillCategory(inventory, user, combatSkills, combatSlots, "⚔");
        
        // Gathering Skills (Middle-Left - slots 19-21)  
        Skill[] gatheringSkills = {Skills.MINING, Skills.FORAGING, Skills.FISHING};
        int[] gatheringSlots = {19, 20, 21};
        addSkillCategory(inventory, user, gatheringSkills, gatheringSlots, "⛏");
        
        // Production Skills (Middle-Right - slots 23-25)
        Skill[] productionSkills = {Skills.FARMING, Skills.EXCAVATION, Skills.FORGING};
        int[] productionSlots = {23, 24, 25};
        addSkillCategory(inventory, user, productionSkills, productionSlots, "🔨");
        
        // Magic Skills (Bottom-Left - slots 28-30)
        Skill[] magicSkills = {Skills.ALCHEMY, Skills.ENCHANTING, Skills.SORCERY};
        int[] magicSlots = {28, 29, 30};
        addSkillCategory(inventory, user, magicSkills, magicSlots, "✨");
        
        // Movement/Life Skills (Bottom-Right - slots 32-34)
        Skill[] lifeSkills = {Skills.AGILITY, Skills.ENDURANCE, Skills.HEALING};
        int[] lifeSlots = {32, 33, 34};
        addSkillCategory(inventory, user, lifeSkills, lifeSlots, "💚");
    }
    
    private void addSkillCategory(Inventory inventory, User user, Skill[] skills, int[] slots, String categoryIcon) {
        for (int i = 0; i < skills.length && i < slots.length; i++) {
            ItemStack skillItem = createSkillLevelItem(skills[i], user);
            inventory.setItem(slots[i], skillItem);
        }
    }
    
    private void addNavigationItems(Inventory inventory) {
        // Add back button (bottom-left, like skills menu)
        ItemStack backItem = createBackItem();
        inventory.setItem(45, backItem);
        
        // Add close button (bottom-right, like skills menu)
        ItemStack closeItem = createCloseItem();
        inventory.setItem(53, closeItem);
    }
    
    private ItemStack createBalanceItem(User user) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Skill Coins Balance");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Your current balance:");
        lore.add("§e" + String.format("%.0f", user.getSkillCoins()) + " ⛁");
        lore.add(" ");
        lore.add("§8Purchase skill levels below");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createSkillLevelItem(Skill skill, User user) {
        // Get skill icon material based on skill type
        Material material = getSkillMaterial(skill);
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        int currentLevel = user.getSkillLevel(skill);
        int maxLevel = shop.getMaxPurchasableLevel(skill);
        double nextLevelCost = shop.calculateLevelCost(skill, currentLevel);
        
        meta.setDisplayName("§a§l" + skill.getDisplayName(java.util.Locale.ENGLISH) + " Level");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Current Level: §e" + currentLevel);
        lore.add("§7Next Level: §e" + (currentLevel + 1));
        lore.add("");
        
        if (currentLevel >= maxLevel && maxLevel > 0) {
            lore.add("§c§lMaximum level reached!");
            lore.add("§c• Level limit: §e" + maxLevel);
            lore.add("§c• Cannot purchase more levels");
        } else {
            lore.add("§e§lNext Level Cost:");
            lore.add("§e• " + String.format("%.0f", nextLevelCost) + " skill coins");
            lore.add("");
            
            if (user.getSkillCoins() >= nextLevelCost) {
                lore.add("§a§lClick to purchase!");
            } else {
                lore.add("§c§lInsufficient coins!");
                lore.add("§c• Need " + String.format("%.0f", nextLevelCost - user.getSkillCoins()) + " more");
            }
            
            if (maxLevel > 0) {
                lore.add("");
                lore.add("§7Purchase limit: §e" + maxLevel);
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private Material getSkillMaterial(Skill skill) {
        // Return appropriate material based on skill
        return switch (skill.getId().getKey()) {
            case "farming" -> Material.WHEAT;
            case "foraging" -> Material.OAK_LOG;
            case "mining" -> Material.IRON_PICKAXE;
            case "fishing" -> Material.FISHING_ROD;
            case "excavation" -> Material.IRON_SHOVEL;
            case "archery" -> Material.BOW;
            case "defense" -> Material.SHIELD;
            case "fighting" -> Material.IRON_SWORD;
            case "endurance" -> Material.LEATHER_BOOTS;
            case "agility" -> Material.FEATHER;
            case "alchemy" -> Material.BREWING_STAND;
            case "enchanting" -> Material.ENCHANTING_TABLE;
            case "sorcery" -> Material.BLAZE_ROD;
            case "healing" -> Material.GOLDEN_APPLE;
            case "forging" -> Material.ANVIL;
            default -> Material.DIAMOND;
        };
    }
    
    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eBack to Shop");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Return to main menu");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cClose");
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§e§lLevel Shop")) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        int slot = event.getSlot();
        User user = plugin.getUser(player);
        
        // Create skill slot mapping for new categorized layout
        Map<Integer, Skill> skillSlotMap = new HashMap<>();
        
        // Combat Skills (slots 10-12)
        skillSlotMap.put(10, Skills.FIGHTING);
        skillSlotMap.put(11, Skills.ARCHERY);
        skillSlotMap.put(12, Skills.DEFENSE);
        
        // Gathering Skills (slots 19-21)
        skillSlotMap.put(19, Skills.MINING);
        skillSlotMap.put(20, Skills.FORAGING);
        skillSlotMap.put(21, Skills.FISHING);
        
        // Production Skills (slots 23-25)
        skillSlotMap.put(23, Skills.FARMING);
        skillSlotMap.put(24, Skills.EXCAVATION);
        skillSlotMap.put(25, Skills.FORGING);
        
        // Magic Skills (slots 28-30)
        skillSlotMap.put(28, Skills.ALCHEMY);
        skillSlotMap.put(29, Skills.ENCHANTING);
        skillSlotMap.put(30, Skills.SORCERY);
        
        // Life Skills (slots 32-34)
        skillSlotMap.put(32, Skills.AGILITY);
        skillSlotMap.put(33, Skills.ENDURANCE);
        skillSlotMap.put(34, Skills.HEALING);
        
        // Check if clicked slot is a skill slot
        if (skillSlotMap.containsKey(slot)) {
            Skill skill = skillSlotMap.get(slot);
            handleSkillLevelPurchase(player, user, skill);
            return;
        }
        
        // Handle navigation buttons
        switch (slot) {
            case 45: // Back
                player.closeInventory();
                plugin.getMainShopMenu().openMainMenu(player);
                break;
                
            case 53: // Close
                player.closeInventory();
                break;
        }
    }
    
    private void handleSkillLevelPurchase(Player player, User user, Skill skill) {
        SkillPointsShop.LevelPurchaseResult result = shop.purchaseLevel(user, skill);
        
        if (result.success) {
            player.sendMessage("§a§l[Shop] " + result.message);
            
            // Play success sound
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            
            // Refresh the menu to show updated levels and costs
            plugin.getScheduler().executeSync(() -> openLevelShop(player));
            
        } else {
            player.sendMessage("§c§l[Shop] " + result.message);
            
            // Play error sound
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Handle inventory close if needed
    }
}