package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.economy.SkillPointsShop;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemShopMenu implements Listener {
    
    private final AuraSkills plugin;
    private final SkillPointsShop shop;
    
    public ItemShopMenu(AuraSkills plugin) {
        this.plugin = plugin;
        this.shop = plugin.getShopManager().getShop();
    }
    
    public void openItemShop(Player player) {
        plugin.getLogger().info("ItemShopMenu - Opening item shop for " + player.getName());
        
        Inventory inventory = Bukkit.createInventory(null, 54, "§b§lItem Shop");
        
        // Add decorative border
        addDecorativeBorder(inventory);
        
        // Populate shop items with better organization
        populateItemShop(inventory, player);
        
        // Add navigation items
        addNavigationItems(inventory, player);
        
        player.openInventory(inventory);
    }
    
    private void addDecorativeBorder(Inventory inventory) {
        // Add decorative glass panes for visual organization
        ItemStack borderPane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        borderMeta.setDisplayName("§8");  // Empty name
        borderPane.setItemMeta(borderMeta);
        
        // Top and bottom borders
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inventory.setItem(slot, borderPane);
        }
    }
    
    private void populateItemShop(Inventory inventory, Player player) {
        plugin.getLogger().info("ItemShopMenu - Populating item shop");
        
        Map<String, Double> sellableItems = shop.getSellableItems();
        Map<String, SkillPointsShop.BuyableItem> buyableItems = shop.getBuyableItems();
        
        int slot = 0; // Start from top row
        
        // Add all unique items (combination of sellable and buyable)
        Set<String> allMaterials = new HashSet<>(sellableItems.keySet());
        allMaterials.addAll(buyableItems.keySet());
        
        plugin.getLogger().info("ItemShopMenu - Processing " + allMaterials.size() + " unique items");
        
        for (String materialName : allMaterials) {
            if (slot >= 45) break; // Stop if we run out of slots (leave bottom row for navigation)
            
            ItemStack item = createShopItem(materialName, player, sellableItems, buyableItems);
            if (item != null) {
                inventory.setItem(slot, item);
                slot++;
            }
        }
    }
    
    private ItemStack createShopItem(String materialName, Player player, 
                                   Map<String, Double> sellableItems, 
                                   Map<String, SkillPointsShop.BuyableItem> buyableItems) {
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            
            // Format the display name
            String displayName = formatMaterialName(materialName);
            meta.setDisplayName("§f§l" + displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7▸ Item: §f" + displayName);
            
            // Add selling info
            Double sellPrice = sellableItems.get(materialName);
            if (sellPrice != null) {
                lore.add("§7▸ Sell Price: §a" + String.format("%.0f", sellPrice) + " coins");
                
                // Add cooldown and amount info
                User user = plugin.getUser(player);
                long remainingCooldown = shop.getRemainingCooldown(user.getUuid().toString(), materialName);
                int remainingAmount = shop.getRemainingAmount(user.getUuid().toString(), materialName);
                
                if (remainingAmount < Integer.MAX_VALUE) {
                    // Amount-based system
                    if (remainingAmount > 0) {
                        lore.add("§7▸ Remaining: §e" + remainingAmount + " §7items this period");
                        if (remainingCooldown > 0) {
                            lore.add("§7▸ Reset in: §e" + formatTime(remainingCooldown));
                        }
                    } else {
                        lore.add("§7▸ Limit reached: §c" + formatTime(remainingCooldown) + " §7remaining");
                    }
                } else {
                    // Traditional cooldown system
                    if (remainingCooldown > 0) {
                        lore.add("§7▸ Sell Cooldown: §c" + formatTime(remainingCooldown));
                    } else {
                        lore.add("§7▸ Sell Status: §a§lReady!");
                    }
                }
            } else {
                lore.add("§7▸ Sell Price: §c§lNot sellable");
            }
            
            // Add buying info
            SkillPointsShop.BuyableItem buyableItem = buyableItems.get(materialName);
            if (buyableItem != null) {
                lore.add("§7▸ Buy Price: §b" + String.format("%.0f", buyableItem.getPrice()) + " coins");
                
                // Add stock info
                int stock = shop.getStock(materialName);
                if (stock == Integer.MAX_VALUE) {
                    lore.add("§7▸ Stock: §a§lUnlimited");
                } else if (stock > 0) {
                    lore.add("§7▸ Stock: §e" + stock + " remaining");
                } else {
                    lore.add("§7▸ Stock: §c§lOut of stock!");
                }
            } else {
                lore.add("§7▸ Buy Price: §c§lNot buyable");
            }
            
            // Add player inventory count
            int playerCount = getPlayerItemCount(player, materialName);
            lore.add("§7▸ You have: §e" + playerCount);
            
            lore.add("");
            
            // Add action instructions
            if (sellPrice != null) {
                lore.add("§a§l⬅ Left Click §7to §a§lSELL");
            }
            if (buyableItem != null) {
                lore.add("§b§l➡ Right Click §7to §b§lBUY");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
            
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("ItemShopMenu - Invalid material: " + materialName);
            return null;
        }
    }
    
    private void addNavigationItems(Inventory inventory, Player player) {
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§e§l← Back to Main Menu");
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to the main shop menu");
        backMeta.setLore(backLore);
        backItem.setItemMeta(backMeta);
        inventory.setItem(45, backItem);
        
        // Player balance
        User user = plugin.getUser(player);
        ItemStack balanceItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        balanceMeta.setDisplayName("§6§lYour Balance");
        List<String> balanceLore = new ArrayList<>();
        balanceLore.add("§7Skill Coins: §e§l" + String.format("%.0f", user.getSkillCoins()));
        balanceMeta.setLore(balanceLore);
        balanceItem.setItemMeta(balanceMeta);
        inventory.setItem(49, balanceItem);
        
        // Close button
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName("§c§lClose");
        List<String> closeLore = new ArrayList<>();
        closeLore.add("§7Close the shop menu");
        closeMeta.setLore(closeLore);
        closeItem.setItemMeta(closeMeta);
        inventory.setItem(53, closeItem);
    }
    
    private String formatMaterialName(String materialName) {
        return materialName.toLowerCase().replace("_", " ");
    }
    
    private String formatTime(long seconds) {
        if (seconds >= 3600) {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        } else if (seconds >= 60) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        } else {
            return seconds + "s";
        }
    }
    
    private int getPlayerItemCount(Player player, String materialName) {
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    count += item.getAmount();
                }
            }
            return count;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("§b§lItem Shop")) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        int slot = event.getSlot();
        
        // Handle navigation clicks
        if (slot == 45) { // Back button
            player.closeInventory();
            plugin.getMainShopMenu().openMainMenu(player);
            return;
        }
        
        if (slot == 53) { // Close button
            player.closeInventory();
            return;
        }
        
        if (slot == 49) { // Balance item - do nothing
            return;
        }
        
        // Handle shop item clicks
        if (slot >= 9 && slot < 45) {
            handleShopItemClick(player, clickedItem, event.getClick());
        }
    }
    
    private void handleShopItemClick(Player player, ItemStack clickedItem, ClickType clickType) {
        Material material = clickedItem.getType();
        String materialName = material.name();
        
        User user = plugin.getUser(player);
        
        if (clickType == ClickType.LEFT) {
            // Sell item
            handleSellItem(player, user, materialName);
        } else if (clickType == ClickType.RIGHT) {
            // Buy item
            handleBuyItem(player, user, materialName);
        }
    }
    
    private void handleSellItem(Player player, User user, String materialName) {
        Map<String, Double> sellableItems = shop.getSellableItems();
        Double sellPrice = sellableItems.get(materialName);
        
        if (sellPrice == null) {
            player.sendMessage("§c§lError: §7This item cannot be sold!");
            return;
        }
        
        // Check cooldown and amount limits
        long remainingCooldown = shop.getRemainingCooldown(user.getUuid().toString(), materialName);
        int remainingAmount = shop.getRemainingAmount(user.getUuid().toString(), materialName);
        
        if (remainingAmount < Integer.MAX_VALUE && remainingAmount <= 0) {
            player.sendMessage("§c§lLimit Reached: §7You've sold the maximum amount of this item. Wait " + formatTime(remainingCooldown) + " to sell more!");
            return;
        } else if (remainingAmount == Integer.MAX_VALUE && remainingCooldown > 0) {
            player.sendMessage("§c§lCooldown: §7You must wait " + formatTime(remainingCooldown) + " before selling this item again!");
            return;
        }
        
        // Check if player has the item
        int playerCount = getPlayerItemCount(player, materialName);
        if (playerCount == 0) {
            player.sendMessage("§c§lError: §7You don't have any " + formatMaterialName(materialName) + " to sell!");
            return;
        }
        
        // Remove one item from player inventory
        if (removeItemFromInventory(player, materialName, 1)) {
            // Use the shop's sellItem method to handle the transaction properly
            SkillPointsShop.SellResult sellResult = shop.sellItem(user, materialName, 1);
            
            if (sellResult.success) {
                player.sendMessage("§a§l✓ Sold: §7" + formatMaterialName(materialName) + " for §a§l" + String.format("%.0f", sellResult.totalPrice) + " coins§7!");
                
                // Refresh the inventory
                openItemShop(player);
            } else {
                // Return the item if the sale failed (this shouldn't happen since we already removed it)
                // But we'll handle it gracefully
                player.getInventory().addItem(new ItemStack(Material.valueOf(materialName), 1));
                
                if (sellResult.errorMessage.startsWith("cooldown:")) {
                    long cooldown = Long.parseLong(sellResult.errorMessage.substring(9));
                    player.sendMessage("§c§lCooldown: §7You must wait " + formatTime(cooldown) + " before selling this item again!");
                } else if (sellResult.errorMessage.startsWith("limit_reached:")) {
                    long cooldown = Long.parseLong(sellResult.errorMessage.substring(14));
                    player.sendMessage("§c§lLimit Reached: §7You've sold the maximum amount of this item. Wait " + formatTime(cooldown) + " to sell more!");
                } else {
                    player.sendMessage("§c§lError: §7" + sellResult.errorMessage);
                }
            }
        } else {
            player.sendMessage("§c§lError: §7Failed to remove item from inventory!");
        }
    }
    
    private void handleBuyItem(Player player, User user, String materialName) {
        Map<String, SkillPointsShop.BuyableItem> buyableItems = shop.getBuyableItems();
        SkillPointsShop.BuyableItem buyableItem = buyableItems.get(materialName);
        
        if (buyableItem == null) {
            player.sendMessage("§c§lError: §7This item cannot be bought!");
            return;
        }
        
        // Check stock
        int stock = shop.getStock(materialName);
        if (stock == 0) {
            player.sendMessage("§c§lError: §7This item is out of stock!");
            return;
        }
        
        // Check if player has enough coins
        double price = buyableItem.getPrice();
        if (user.getSkillCoins() < price) {
            player.sendMessage("§c§lError: §7You need §c§l" + String.format("%.0f", price) + " coins §7but only have §c§l" + String.format("%.0f", user.getSkillCoins()) + " coins§7!");
            return;
        }
        
        // Check if player has inventory space
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§c§lError: §7Your inventory is full!");
            return;
        }
        
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            ItemStack itemToGive = new ItemStack(material, buyableItem.getAmount());
            
            // Remove coins from player
            user.setSkillCoins(user.getSkillCoins() - price);
            
            // Give item to player
            player.getInventory().addItem(itemToGive);
            
            // Update stock (only if not unlimited)
            if (stock > 0 && stock != Integer.MAX_VALUE) {
                shop.setStock(materialName, stock - 1);
            }
            
            player.sendMessage("§b§l✓ Bought: §7" + buyableItem.getAmount() + "x " + formatMaterialName(materialName) + " for §b§l" + String.format("%.0f", price) + " coins§7!");
            
            // Refresh the inventory
            openItemShop(player);
            
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c§lError: §7Invalid item material!");
        }
    }
    
    private boolean removeItemFromInventory(Player player, String materialName, int amount) {
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    if (item.getAmount() >= amount) {
                        item.setAmount(item.getAmount() - amount);
                        return true;
                    }
                }
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Handle inventory close if needed
    }
}