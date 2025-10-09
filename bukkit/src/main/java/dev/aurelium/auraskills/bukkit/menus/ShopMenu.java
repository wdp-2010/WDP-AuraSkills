package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.shared.GlobalItems;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import dev.aurelium.auraskills.common.economy.SkillPointsShop;
import dev.aurelium.auraskills.common.message.type.CommandMessage;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.slate.builder.MenuBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

        // Shop header item showing balance
        menu.item("balance", item -> {
            item.replace("balance", p -> {
                User user = plugin.getUser(p.player());
                return String.format("%.2f", user.getSkillCoins());
            });
        });

        // Buy skill level template
        menu.template("buy_level", Skill.class, template -> {
            template.replace("skill", t -> t.value().getDisplayName(t.locale()));
            template.replace("current_level", t -> {
                User user = plugin.getUser(t.player());
                return String.valueOf(user.getSkillLevel(t.value()));
            });
            template.replace("cost", t -> {
                User user = plugin.getUser(t.player());
                int currentLevel = user.getSkillLevel(t.value());
                return String.format("%.2f", shop.calculateLevelCost(currentLevel));
            });
            template.replace("balance", t -> {
                User user = plugin.getUser(t.player());
                return String.format("%.2f", user.getSkillCoins());
            });

            template.onClick(c -> {
                User user = plugin.getUser(c.player());
                Player player = c.player();
                Locale locale = plugin.getLocale(player);

                int currentLevel = user.getSkillLevel(c.value());
                double cost = shop.calculateLevelCost(currentLevel);
                double balance = user.getSkillCoins();

                if (balance < cost) {
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

                    // Reopen menu to refresh
                    plugin.getSlate().openMenu(player, "shop");
                } else {
                    player.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(
                            CommandMessage.SHOP_PURCHASE_FAILED, locale));
                }
            });

            template.definedContexts(m -> new HashSet<>(plugin.getSkillManager().getEnabledSkills()));
        });

        // Buy 100 XP item
        menu.template("buy_xp", Skill.class, template -> {
            template.replace("skill", t -> t.value().getDisplayName(t.locale()));
            template.replace("xp_amount", t -> "100");
            template.replace("cost", t -> String.format("%.2f", shop.calculateXpCost(100)));
            template.replace("balance", t -> {
                User user = plugin.getUser(t.player());
                return String.format("%.2f", user.getSkillCoins());
            });

            template.onClick(c -> {
                User user = plugin.getUser(c.player());
                Player player = c.player();
                Locale locale = plugin.getLocale(player);

                double xpAmount = 100;
                double cost = shop.calculateXpCost(xpAmount);
                double balance = user.getSkillCoins();

                if (balance < cost) {
                    player.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(
                            CommandMessage.SHOP_INSUFFICIENT_FUNDS, locale));
                    return;
                }

                if (shop.purchaseXp(user, c.value(), xpAmount)) {
                    String message = TextUtil.replace(
                            plugin.getMsg(CommandMessage.SHOP_XP_PURCHASE_SUCCESS, locale),
                            "{skill}", c.value().getDisplayName(locale),
                            "{xp_amount}", String.valueOf((int) xpAmount),
                            "{cost}", String.format("%.2f", cost)
                    );
                    player.sendMessage(plugin.getPrefix(locale) + message);

                    // Reopen menu to refresh
                    plugin.getSlate().openMenu(player, "shop");
                } else {
                    player.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(
                            CommandMessage.SHOP_PURCHASE_FAILED, locale));
                }
            });

            template.definedContexts(m -> new HashSet<>(plugin.getSkillManager().getEnabledSkills()));
        });

        // Stat reset item
        menu.item("stat_reset", item -> {
            item.replace("cost", p -> String.format("%.2f", shop.getStatResetCost()));
            item.replace("balance", p -> {
                User user = plugin.getUser(p.player());
                return String.format("%.2f", user.getSkillCoins());
            });

            item.onClick(c -> {
                User user = plugin.getUser(c.player());
                Player player = c.player();
                Locale locale = plugin.getLocale(player);

                double cost = shop.getStatResetCost();
                double balance = user.getSkillCoins();

                if (balance < cost) {
                    player.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(
                            CommandMessage.SHOP_INSUFFICIENT_FUNDS, locale));
                    return;
                }

                if (shop.purchaseStatReset(user)) {
                    player.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(
                            CommandMessage.SHOP_STAT_RESET_SUCCESS, locale));

                    // Reopen menu to refresh
                    plugin.getSlate().openMenu(player, "shop");
                } else {
                    player.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(
                            CommandMessage.SHOP_PURCHASE_FAILED, locale));
                }
            });
        });
    }
}
