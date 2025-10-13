package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.economy.SkillPointsShop;
import dev.aurelium.auraskills.common.message.type.CommandMessage;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CooldownTrackerMenu implements Listener {

    private final AuraSkills plugin;
    private final SkillPointsShop shop;
    private final Set<UUID> openInventories = new HashSet<>();

    public CooldownTrackerMenu(AuraSkills plugin) {
        this.plugin = plugin;
        this.shop = plugin.getShopManager().getShop();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openCooldownTracker(Player player) {
        User user = plugin.getUser(player);
        String title = plugin.getMsg(CommandMessage.SHOP_COOLDOWN_TRACKER_TITLE, user.getLocale());
        
        Inventory inventory = Bukkit.createInventory(null, 54, title);
        openInventories.add(player.getUniqueId());
        
        Map<String, Long> activeCooldowns = shop.getActiveCooldowns(player.getUniqueId().toString());
        
        if (activeCooldowns.isEmpty()) {
            ItemStack noItemsItem = new ItemStack(Material.BARRIER);
            ItemMeta meta = noItemsItem.getItemMeta();
            meta.setDisplayName("§7No Active Cooldowns");
            
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getMsg(CommandMessage.SHOP_COOLDOWN_TRACKER_NO_COOLDOWNS, user.getLocale()));
            
            meta.setLore(lore);
            noItemsItem.setItemMeta(meta);
            inventory.setItem(22, noItemsItem);
        } else {
            int slot = 0;
            for (Map.Entry<String, Long> entry : activeCooldowns.entrySet()) {
                if (slot >= 45) break;
                
                String materialName = entry.getKey();
                long expiryTime = entry.getValue();
                
                Material material;
                try {
                    material = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    material = Material.STONE;
                }
                
                ItemStack cooldownItem = createCooldownItem(user, materialName, material, expiryTime);
                inventory.setItem(slot, cooldownItem);
                slot++;
            }
        }
        
        ItemStack backButton = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backButton.getItemMeta();
        backMeta.setDisplayName("§eBack to Shop");
        List<String> backLore = new ArrayList<>();
        backLore.add("§7Return to main menu");
        backMeta.setLore(backLore);
        backButton.setItemMeta(backMeta);
        inventory.setItem(49, backButton);
        
        player.openInventory(inventory);
    }

    private ItemStack createCooldownItem(User user, String materialName, Material material, long expiryTime) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String displayName = formatMaterialName(materialName);
        meta.setDisplayName("§b" + displayName);
        
        long currentTime = System.currentTimeMillis();
        long remainingMillis = expiryTime - currentTime;
        String timeRemaining = formatTime(remainingMillis);
        
        String uuid = user.getUuid().toString();
        int available = shop.getRemainingAmount(uuid, materialName);
        int maxAmount = shop.getMaxAmount(materialName);
        double price = shop.getSellPrice(materialName);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Available: §e" + available + "§7/§e" + maxAmount);
        lore.add("§7Reset in: §a" + timeRemaining);
        lore.add(" ");
        lore.add("§7Sell price: §e" + String.format("%.0f", price) + " ⛁");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatMaterialName(String material) {
        String[] words = material.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return formatted.toString();
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "Ready!";
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check if this player has cooldown tracker open using UUID tracking
        if (!openInventories.contains(player.getUniqueId())) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        // Handle back button
        if (event.getSlot() == 49 && clickedItem.getType() == Material.ARROW) {
            openInventories.remove(player.getUniqueId());
            player.closeInventory();
            plugin.getShopManager().getMainShopMenu().openMainMenu(player);
        }
    }
    
    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openInventories.remove(player.getUniqueId());
        }
    }
}
