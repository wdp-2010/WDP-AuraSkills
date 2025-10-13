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
        Inventory inventory = Bukkit.createInventory(null, 54, "§bItem Shop");
        
        addDecorativeBorder(inventory);
        populateItemShop(inventory, player);
        addNavigationItems(inventory, player);
        
        player.openInventory(inventory);
    }
    
    private void addDecorativeBorder(Inventory inventory) {
        ItemStack borderPane = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        borderMeta.setDisplayName(" ");
        borderPane.setItemMeta(borderMeta);
        
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inventory.setItem(slot, borderPane);
        }
    }
    
    private void populateItemShop(Inventory inventory, Player player) {
        Map<String, Double> sellableItems = shop.getSellableItems();
        Map<String, SkillPointsShop.BuyableItem> buyableItems = shop.getBuyableItems();
        
        int slot = 0;
        
        Set<String> allMaterials = new HashSet<>(sellableItems.keySet());
        allMaterials.addAll(buyableItems.keySet());
        
        for (String materialName : allMaterials) {
            if (slot >= 45) break;
            
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
            
            String displayName = formatMaterialName(materialName);
            meta.setDisplayName("§f" + displayName);
            
            List<String> lore = new ArrayList<>();
            User user = plugin.getUser(player);
            
            Double sellPrice = sellableItems.get(materialName);
            if (sellPrice != null) {
                lore.add("§7Sell: §a" + String.format("%.0f", sellPrice) + " ⛁");
                
                long remainingCooldown = shop.getRemainingCooldown(user.getUuid().toString(), materialName);
                int remainingAmount = shop.getRemainingAmount(user.getUuid().toString(), materialName);
                
                if (remainingAmount < Integer.MAX_VALUE) {
                    if (remainingAmount > 0) {
                        lore.add("§7Remaining: §e" + remainingAmount);
                    } else {
                        lore.add("§7Cooldown: §c" + formatTime(remainingCooldown));
                    }
                } else {
                    if (remainingCooldown > 0) {
                        lore.add("§7Cooldown: §c" + formatTime(remainingCooldown));
                    }
                }
            }
            
            SkillPointsShop.BuyableItem buyableItem = buyableItems.get(materialName);
            if (buyableItem != null) {
                lore.add("§7Buy: §b" + String.format("%.0f", buyableItem.getPrice()) + " ⛁");
                
                int stock = shop.getStock(materialName);
                if (stock != Integer.MAX_VALUE && stock <= 0) {
                    lore.add("§7Stock: §cOut of stock");
                }
            }
            
            int playerCount = getPlayerItemCount(player, materialName);
            lore.add(" ");
            lore.add("§7You have: §e" + playerCount);
            lore.add(" ");
            
            if (sellPrice != null && buyableItem != null) {
                lore.add("§eLeft-click to sell • Right-click to buy");
            } else if (sellPrice != null) {
                lore.add("§eLeft-click to sell");
            } else if (buyableItem != null) {
                lore.add("§eRight-click to buy");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
            
        } catch (IllegalArgumentException e) {
            return null;
        }
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
        
        User user = plugin.getUser(player);
        ItemStack balanceItem = new ItemStack(Material.SUNFLOWER);
        ItemMeta balanceMeta = balanceItem.getItemMeta();
        balanceMeta.setDisplayName("§6Your Balance");
        List<String> balanceLore = new ArrayList<>();
        balanceLore.add("§e" + String.format("%.0f", user.getSkillCoins()) + " ⛁");
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
        if (!event.getView().getTitle().equals("§bItem Shop")) return;
        
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
            player.sendMessage("§cThis item cannot be sold");
            return;
        }
        
        long remainingCooldown = shop.getRemainingCooldown(user.getUuid().toString(), materialName);
        int remainingAmount = shop.getRemainingAmount(user.getUuid().toString(), materialName);
        
        if (remainingAmount < Integer.MAX_VALUE && remainingAmount <= 0) {
            player.sendMessage("§cLimit reached - wait " + formatTime(remainingCooldown));
            return;
        } else if (remainingAmount == Integer.MAX_VALUE && remainingCooldown > 0) {
            player.sendMessage("§cCooldown active - " + formatTime(remainingCooldown) + " remaining");
            return;
        }
        
        int playerCount = getPlayerItemCount(player, materialName);
        if (playerCount == 0) {
            player.sendMessage("§cYou don't have any " + formatMaterialName(materialName));
            return;
        }
        
        if (removeItemFromInventory(player, materialName, 1)) {
            SkillPointsShop.SellResult sellResult = shop.sellItem(user, materialName, 1);
            
            if (sellResult.success) {
                player.sendMessage("§aSold " + formatMaterialName(materialName) + " for " + String.format("%.0f", sellResult.totalPrice) + " ⛁");
                openItemShop(player);
            } else {
                player.getInventory().addItem(new ItemStack(Material.valueOf(materialName), 1));
                
                if (sellResult.errorMessage.startsWith("cooldown:")) {
                    long cooldown = Long.parseLong(sellResult.errorMessage.substring(9));
                    player.sendMessage("§cCooldown active - " + formatTime(cooldown) + " remaining");
                } else if (sellResult.errorMessage.startsWith("limit_reached:")) {
                    long cooldown = Long.parseLong(sellResult.errorMessage.substring(14));
                    player.sendMessage("§cLimit reached - wait " + formatTime(cooldown));
                } else {
                    player.sendMessage("§c" + sellResult.errorMessage);
                }
            }
        } else {
            player.sendMessage("§cFailed to remove item from inventory");
        }
    }
    
    private void handleBuyItem(Player player, User user, String materialName) {
        Map<String, SkillPointsShop.BuyableItem> buyableItems = shop.getBuyableItems();
        SkillPointsShop.BuyableItem buyableItem = buyableItems.get(materialName);
        
        if (buyableItem == null) {
            player.sendMessage("§cThis item cannot be bought");
            return;
        }
        
        int stock = shop.getStock(materialName);
        if (stock == 0) {
            player.sendMessage("§cOut of stock");
            return;
        }
        
        double price = buyableItem.getPrice();
        if (user.getSkillCoins() < price) {
            player.sendMessage("§cNot enough coins - need " + String.format("%.0f", price) + " ⛁");
            return;
        }
        
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§cInventory full");
            return;
        }
        
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            ItemStack itemToGive = new ItemStack(material, buyableItem.getAmount());
            
            user.setSkillCoins(user.getSkillCoins() - price);
            player.getInventory().addItem(itemToGive);
            
            if (stock > 0 && stock != Integer.MAX_VALUE) {
                shop.setStock(materialName, stock - 1);
            }
            
            player.sendMessage("§bBought " + buyableItem.getAmount() + "x " + formatMaterialName(materialName) + " for " + String.format("%.0f", price) + " ⛁");
            

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