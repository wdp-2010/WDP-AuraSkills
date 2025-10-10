package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.shared.GlobalItems;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import dev.aurelium.auraskills.common.economy.SkillPointsShop;
import dev.aurelium.auraskills.common.message.type.CommandMessage;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.slate.builder.MenuBuilder;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

public class ShopMenu {

    private final AuraSkills plugin;
    private final SkillPointsShop shop;

    public ShopMenu(AuraSkills plugin) {
        this.plugin = plugin;
        this.shop = new SkillPointsShop(plugin);
    }

    public void build(MenuBuilder menu) {
        try {
            menu.defaultOptions(Map.of(
                    "percent_format", "#.##"));

            var globalItems = new GlobalItems(plugin);
            menu.item("close", globalItems::close);
            menu.fillItem(globalItems::fill);

            // Balance display
            menu.item("balance", item -> {
                item.replace("balance", p -> {
                    User user = plugin.getUser(p.player());
                    return String.format("%.2f", user.getSkillCoins());
            });
        });

        // Sell items button        // Sell items button
        menu.item("sell_items", item -> {
            item.replace("sellable_count", p -> String.valueOf(shop.getSellableItems().size()));
            item.onClick(c -> {
                plugin.getSlate().openMenu(c.player(), "shop_sell");
            });
        });

        // Buy levels button
        menu.item("buy_levels", item -> {
            item.replace("skill_count", p -> String.valueOf(plugin.getSkillManager().getEnabledSkills().size()));
            item.onClick(c -> {
                plugin.getSlate().openMenu(c.player(), "shop_levels");
            });
        });

        // Buy items button  
        menu.item("buy_items", item -> {
            item.replace("buyable_count", p -> String.valueOf(shop.getBuyableItems().size()));
            item.onClick(c -> {
                plugin.getSlate().openMenu(c.player(), "shop_buy");
            });
        });

        // Buy abilities button
        menu.item("buy_abilities", item -> {
            item.replace("ability_count", p -> String.valueOf(shop.getBuyableAbilities().size()));
            item.onClick(c -> {
                c.player().sendMessage(plugin.getPrefix(plugin.getLocale(c.player())) + "§cBuy abilities feature coming in future update!");
            });
        });
        } catch (Exception e) {
            plugin.getLogger().severe("Error building shop menu: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
