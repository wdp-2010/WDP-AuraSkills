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

public class BuyItemsMenu {

    private final AuraSkills plugin;
    private final SkillPointsShop skillPointsShop;

    public BuyItemsMenu(AuraSkills plugin) {
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

        // Template for all buyable items
        menu.template("item", String.class, template -> {
            template.replace("display_name", p -> p.value().toLowerCase().replace("_", " "));
            template.replace("price", p -> {
                SkillPointsShop.BuyableItem item = skillPointsShop.getBuyableItems().get(p.value());
                return item != null ? String.valueOf((int) item.getPrice()) : "0";
            });
            template.replace("amount", p -> {
                SkillPointsShop.BuyableItem item = skillPointsShop.getBuyableItems().get(p.value());
                return item != null ? String.valueOf(item.getAmount()) : "1";
            });
            template.replace("stock", p -> {
                int stock = skillPointsShop.getStock(p.value());
                return stock >= 0 ? String.valueOf(stock) : "∞";
            });
            template.replace("afford_lore", p -> {
                User user = plugin.getUser(p.player());
                SkillPointsShop.BuyableItem item = skillPointsShop.getBuyableItems().get(p.value());
                double price = item != null ? item.getPrice() : 0;
                return user.getSkillCoins() >= price ? "&aYou can afford this" : "&cNot enough Skill Coins";
            });
            template.replace("stock_lore", p -> {
                int stock = skillPointsShop.getStock(p.value());
                return stock > 0 || stock == -1 ? "&aIn stock" : "&cOut of stock";
            });
            template.replace("click_lore", p -> {
                int stock = skillPointsShop.getStock(p.value());
                User user = plugin.getUser(p.player());
                SkillPointsShop.BuyableItem item = skillPointsShop.getBuyableItems().get(p.value());
                double price = item != null ? item.getPrice() : 0;
                
                if (stock == 0) {
                    return "&cOut of stock!";
                } else if (user.getSkillCoins() < price) {
                    return "&cCannot afford!";
                } else {
                    return "&eClick to buy!";
                }
            });
            
            template.onClick(c -> {
                String material = c.value();
                Player player = c.player();
                User user = plugin.getUser(player);
                Locale locale = user.getLocale();
                
                SkillPointsShop.BuyableItem item = skillPointsShop.getBuyableItems().get(material);
                if (item == null) {
                    player.sendMessage("§cThis item is not available for purchase!");
                    return;
                }
                
                // Check stock
                int stock = skillPointsShop.getStock(material);
                if (stock == 0) {
                    player.sendMessage("§cThis item is out of stock!");
                    return;
                }
                
                // Check balance
                double price = item.getPrice();
                if (user.getSkillCoins() < price) {
                    player.sendMessage("§cYou don't have enough Skill Coins! You need " + (int) price + " but only have " + user.getSkillCoins() + ".");
                    return;
                }
                
                // Check inventory space
                if (player.getInventory().firstEmpty() == -1) {
                    player.sendMessage("§cYour inventory is full!");
                    return;
                }
                
                // Attempt to buy
                SkillPointsShop.BuyResult result = skillPointsShop.buyItem(user, material, item.getAmount());
                
                if (result.isSuccess()) {
                    // Give item to player
                    try {
                        Material mat = Material.valueOf(material.toUpperCase());
                        ItemStack itemStack = new ItemStack(mat, item.getAmount());
                        player.getInventory().addItem(itemStack);
                        
                        String successMsg = plugin.getMsg(CommandMessage.SHOP_BUY_SUCCESS, locale);
                        successMsg = TextUtil.replace(successMsg,
                                "{amount}", String.valueOf(item.getAmount()),
                                "{item}", material.toLowerCase().replace("_", " "),
                                "{price}", String.valueOf((int) result.getTotalCost()),
                                "{balance}", String.valueOf(user.getSkillCoins()));
                        player.sendMessage(successMsg);
                        
                        // Refresh menu after 1 tick
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                plugin.getSlate().openMenu(player, "shop_buy");
                            }
                        }, 1L);
                    } catch (IllegalArgumentException e) {
                        player.sendMessage("§cError: Invalid item material!");
                    }
                } else {
                    player.sendMessage("§c" + result.getErrorMessage());
                }
            });
            
            template.definedContexts(m -> skillPointsShop.getBuyableItems().keySet());
        });
    }
}
