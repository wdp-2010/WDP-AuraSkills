package dev.aurelium.auraskills.bukkit.menus;

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

public class MainShopMenu implements Listener {
    
    private final AuraSkills plugin;
    private final SkillPointsShop shop;
    private final ItemShopMenu itemShopMenu;
    private final AbilityShopMenu abilityShopMenu;
    
    public MainShopMenu(AuraSkills plugin) {
        this.plugin = plugin;
        this.shop = plugin.getShopManager().getShop();
        this.itemShopMenu = new ItemShopMenu(plugin);
        this.abilityShopMenu = new AbilityShopMenu(plugin);
        
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(itemShopMenu, plugin);
        Bukkit.getPluginManager().registerEvents(abilityShopMenu, plugin);
    }
    
    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, "§6Skill Coins Shop");
        
        User user = plugin.getUser(player);
        
        // Fill with black glass panes like skills menu
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, filler);
        }
        
        // Balance at top left (like "Your Skills" in skills menu)
        ItemStack balanceItem = createBalanceItem(user);
        inventory.setItem(0, balanceItem);
        
        // Main shop items centered in row 2 (like skills are displayed)
        ItemStack itemShopItem = createItemShopItem();
        inventory.setItem(11, itemShopItem);
        
        ItemStack levelShopItem = createLevelShopItem();
        inventory.setItem(13, levelShopItem);
        
        ItemStack abilityShopItem = createAbilityShopItem();
        inventory.setItem(15, abilityShopItem);
        
        // Cooldown tracker in row 3
        ItemStack cooldownTrackerItem = createCooldownTrackerItem(player);
        inventory.setItem(22, cooldownTrackerItem);
        
        // Close button at bottom right (like skills menu)
        ItemStack closeItem = createCloseItem();
        inventory.setItem(44, closeItem);
        
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
        lore.add("§8Earn coins by leveling skills");
        lore.add("§8Spend in the shop categories below");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createItemShopItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bItem Shop");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Buy and sell items for");
        lore.add("§7skill coins");
        lore.add(" ");
        lore.add("§eClick to open");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createLevelShopItem() {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eLevel Shop");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Purchase skill levels");
        lore.add("§7directly with coins");
        lore.add(" ");
        lore.add("§eClick to open");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAbilityShopItem() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§dAbility Shop");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Purchase exclusive abilities");
        lore.add("§7not available through leveling");
        lore.add(" ");
        lore.add("§eClick to open");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createCooldownTrackerItem(Player player) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Cooldown Tracker");
        
        List<String> lore = new ArrayList<>();
        int cooldownCount = shop.getActiveCooldowns(player.getUniqueId().toString()).size();
        
        if (cooldownCount > 0) {
            lore.add("§7View active sell cooldowns");
            lore.add(" ");
            lore.add("§e" + cooldownCount + " §7item" + (cooldownCount == 1 ? "" : "s") + " on cooldown");
            lore.add(" ");
            lore.add("§eClick to view");
        } else {
            lore.add("§7View active sell cooldowns");
            lore.add(" ");
            lore.add("§7No active cooldowns");
        }
        
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
        if (!event.getView().getTitle().equals("§6Skill Coins Shop")) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 11: // Item Shop
                player.closeInventory();
                itemShopMenu.openItemShop(player);
                break;
                
            case 13: // Level Shop
                player.closeInventory();
                plugin.getShopManager().getLevelShopMenu().openLevelShop(player);
                break;
                
            case 15: // Ability Shop
                player.closeInventory();
                abilityShopMenu.openAbilityShop(player);
                break;
                
            case 22: // Cooldown Tracker
                player.closeInventory();
                plugin.getShopManager().getCooldownTrackerMenu().openCooldownTracker(player);
                break;
                
            case 44: // Close
                player.closeInventory();
                break;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Handle inventory close if needed
    }
    
    public ItemShopMenu getItemShopMenu() {
        return itemShopMenu;
    }
    
    public AbilityShopMenu getAbilityShopMenu() {
        return abilityShopMenu;
    }
}