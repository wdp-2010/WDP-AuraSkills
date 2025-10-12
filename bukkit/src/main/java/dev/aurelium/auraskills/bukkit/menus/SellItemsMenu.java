package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.shared.GlobalItems;
import dev.aurelium.auraskills.common.economy.SkillPointsShop;
import dev.aurelium.auraskills.common.message.type.CommandMessage;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import dev.aurelium.slate.builder.MenuBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class SellItemsMenu {

    private final AuraSkills plugin;
    private final SkillPointsShop skillPointsShop;

    public SellItemsMenu(AuraSkills plugin) {
        this.plugin = plugin;
        this.skillPointsShop = new SkillPointsShop(plugin);
    }

    public void build(MenuBuilder menu) {
        var globalItems = new GlobalItems(plugin);
        menu.item("close", globalItems::close);
        menu.item("back", item -> {
            item.onClick(c -> plugin.getSlate().openMenu(c.player(), "shop"));
        });
        menu.fillItem(globalItems::fill);

        // Balance display with real SkillCoins
        menu.item("balance", item -> {
            item.replace("skill_coins", p -> {
                User user = plugin.getUser(p.player());
                return String.valueOf(user.getSkillCoins());
            });
        });

        // Template for all sellable items
        menu.template("item", String.class, template -> {
            template.replace("display_name", p -> p.value().toLowerCase().replace("_", " "));
            template.replace("price", p -> String.valueOf(skillPointsShop.getSellableItems().get(p.value())));
            template.replace("inventory_count", p -> {
                int inventoryCount = 0;
                try {
                    Material mat = Material.valueOf(p.value().toUpperCase());
                    for (ItemStack item : p.player().getInventory().getContents()) {
                        if (item != null && item.getType() == mat) {
                            inventoryCount += item.getAmount();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in sellable items: " + p.value());
                }
                return String.valueOf(inventoryCount);
            });
            template.replace("cooldown_lore", p -> {
                long remainingCooldown = skillPointsShop.getRemainingCooldown(p.player().getUniqueId().toString(), p.value());
                return remainingCooldown > 0 ? "&cCooldown: " + remainingCooldown + "s" : "&aReady to sell";
            });
            
            template.onClick(c -> {
                String material = c.value();
                Player player = c.player();
                User user = plugin.getUser(player);
                Locale locale = user.getLocale();
                
                // Check cooldown
                long remainingCooldown = skillPointsShop.getRemainingCooldown(player.getUniqueId().toString(), material);
                if (remainingCooldown > 0) {
                    String cooldownMsg = plugin.getMsg(CommandMessage.SHOP_SELL_COOLDOWN, locale);
                    cooldownMsg = TextUtil.replace(cooldownMsg, 
                            "{time}", String.valueOf(remainingCooldown),
                            "{item}", material.toLowerCase().replace("_", " "));
                    player.sendMessage(cooldownMsg);
                    return;
                }
                
                // Count items in player's inventory
                int inventoryCount = 0;
                try {
                    Material mat = Material.valueOf(material.toUpperCase());
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == mat) {
                            inventoryCount += item.getAmount();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in sellable items: " + material);
                    return;
                }
                
                // Check if player has the item
                if (inventoryCount <= 0) {
                    player.sendMessage("§cYou don't have any " + material.toLowerCase().replace("_", " ") + " to sell!");
                    return;
                }
                
                // Attempt to sell
                SkillPointsShop.SellResult result = skillPointsShop.sellItem(user, material, 1);
                
                if (result.success) {
                    // Remove item from inventory
                    try {
                        Material mat = Material.valueOf(material.toUpperCase());
                        ItemStack toRemove = new ItemStack(mat, 1);
                        player.getInventory().removeItem(toRemove);
                        
                        String successMsg = plugin.getMsg(CommandMessage.SHOP_SELL_SUCCESS, locale);
                        successMsg = TextUtil.replace(successMsg,
                                "{amount}", "1",
                                "{item}", material.toLowerCase().replace("_", " "),
                                "{price}", String.valueOf(result.totalPrice),
                                "{balance}", String.valueOf(user.getSkillCoins()));
                        player.sendMessage(successMsg);
                        
                        // Refresh menu after 1 tick
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                plugin.getSlate().openMenu(player, "shop_sell");
                            }
                        }, 1L);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cError: Invalid item material!");
                    }
                } else {
                    player.sendMessage("§c" + result.errorMessage);
                }
            });
            
            template.definedContexts(m -> skillPointsShop.getSellableItems().keySet());
        });
    }
}
