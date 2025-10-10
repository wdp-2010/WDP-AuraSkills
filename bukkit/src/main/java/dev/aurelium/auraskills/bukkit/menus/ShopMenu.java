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

        // Buy skill levels
        menu.template("buy_level", Skill.class, template -> {
            template.replace("skill", t -> t.value().getDisplayName(t.locale()));
            template.replace("current_level", t -> {
                User user = plugin.getUser(t.player());
                return String.valueOf(user.getSkillLevel(t.value()));
            });
            template.replace("cost", t -> {
                User user = plugin.getUser(t.player());
                int currentLevel = user.getSkillLevel(t.value());
                return String.format("%.2f", shop.calculateLevelCost(t.value(), currentLevel));
            });

            template.onClick(c -> {
                User user = plugin.getUser(c.player());
                Player player = c.player();
                Locale locale = plugin.getLocale(player);

                int currentLevel = user.getSkillLevel(c.value());
                double cost = shop.calculateLevelCost(c.value(), currentLevel);

                if (user.getSkillCoins() < cost) {
                    player.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(
                            CommandMessage.SHOP_INSUFFICIENT_FUNDS, locale));
                    return;
                }

                if (shop.purchaseLevel(user, c.value())) {
                    String message = TextUtil.replace(
                            plugin.getMsg(CommandMessage.SHOP_PURCHASE_SUCCESS, locale),
                            "{skill}", c.value().getDisplayName(locale),
                            "{level}", String.valueOf(currentLevel + 1),
                            "{cost}", String.format("%.2f", cost)
                    );
                    player.sendMessage(plugin.getPrefix(locale) + message);
                    plugin.getSlate().openMenu(player, "shop");
                } else {
                    player.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(
                            CommandMessage.SHOP_PURCHASE_FAILED, locale));
                }
            });

            template.definedContexts(m -> new HashSet<>(plugin.getSkillManager().getEnabledSkills()));
        });

        // Sell items button - will be implemented with submenu later
        menu.item("sell_items", item -> {
            item.replace("sellable_count", p -> String.valueOf(shop.getSellableItems().size()));
            item.onClick(c -> {
                // TODO: Open sell items submenu when implemented
                c.player().sendMessage(plugin.getPrefix(plugin.getLocale(c.player())) + "§cSell items feature coming soon!");
            });
        });

        // Buy abilities button - will be implemented with submenu later
        menu.item("buy_abilities", item -> {
            item.replace("ability_count", p -> String.valueOf(shop.getBuyableAbilities().size()));
            item.onClick(c -> {
                // TODO: Open buy abilities submenu when implemented
                c.player().sendMessage(plugin.getPrefix(plugin.getLocale(c.player())) + "§cBuy abilities feature coming soon!");
            });
        });
    }
}
