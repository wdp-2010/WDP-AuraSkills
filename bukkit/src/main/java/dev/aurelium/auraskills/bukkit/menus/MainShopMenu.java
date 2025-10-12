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
        plugin.getLogger().info("MainShopMenu - Opening main shop menu for " + player.getName());
        
        Inventory inventory = Bukkit.createInventory(null, 27, "§6§lSkill Coins Shop");
        
        // Add player balance info
        User user = plugin.getUser(player);
        ItemStack balanceItem = createBalanceItem(user);
        inventory.setItem(4, balanceItem);
        
        // Add shop categories
        ItemStack itemShopItem = createItemShopItem();
        inventory.setItem(10, itemShopItem);
        
        ItemStack levelShopItem = createLevelShopItem();
        inventory.setItem(13, levelShopItem);
        
        ItemStack abilityShopItem = createAbilityShopItem();
        inventory.setItem(16, abilityShopItem);
        
        // Add close button
        ItemStack closeItem = createCloseItem();
        inventory.setItem(22, closeItem);
        
        player.openInventory(inventory);
    }
    
    private ItemStack createBalanceItem(User user) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lYour Balance");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Skill Coins: §e§l" + String.format("%.0f", user.getSkillCoins()));
        lore.add("");
        lore.add("§a§lHow to earn:");
        lore.add("§a• Complete skill leveling tasks");
        lore.add("§a• Sell valuable items");
        lore.add("§a• Trade with other players");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createItemShopItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lItem Shop");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Buy and sell valuable items");
        lore.add("");
        lore.add("§e" + shop.getSellableItems().size() + " §7sellable items");
        lore.add("§e" + shop.getBuyableItems().size() + " §7buyable items");
        lore.add("");
        lore.add("§a§lClick to browse!");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createLevelShopItem() {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lLevel Shop");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Purchase skill levels directly");
        lore.add("");
        lore.add("§e15 §7purchasable skills");
        lore.add("§7Configurable level limits");
        lore.add("");
        lore.add("§a§lClick to browse!");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createAbilityShopItem() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lAbility Shop");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Purchase powerful abilities");
        lore.add("");
        lore.add("§e" + shop.getBuyableAbilities().size() + " §7available abilities");
        lore.add("");
        lore.add("§a§lClick to browse!");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lClose");
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Close the shop menu");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§6§lSkill Coins Shop")) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        int slot = event.getSlot();
        
        switch (slot) {
            case 10: // Item Shop
                plugin.getLogger().info("MainShopMenu - Opening item shop for " + player.getName());
                player.closeInventory();
                itemShopMenu.openItemShop(player);
                break;
                
            case 13: // Level Shop
                plugin.getLogger().info("MainShopMenu - Opening level shop for " + player.getName());
                player.closeInventory();
                plugin.getShopManager().getLevelShopMenu().openLevelShop(player);
                break;
                
            case 16: // Ability Shop
                plugin.getLogger().info("MainShopMenu - Opening ability shop for " + player.getName());
                player.closeInventory();
                abilityShopMenu.openAbilityShop(player);
                break;
                
            case 22: // Close
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