package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.bukkit.menus.shared.GlobalItems;
import dev.aurelium.auraskills.common.economy.SkillPointsShop;
import dev.aurelium.auraskills.common.message.type.CommandMessage;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import dev.aurelium.slate.builder.MenuBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;

public class LevelShopMenu {

    private final AuraSkills plugin;
    private final SkillPointsShop skillPointsShop;

    public LevelShopMenu(AuraSkills plugin) {
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

        // Template for all skills
        menu.template("skill", Skill.class, template -> {
            template.replace("skill_name", p -> p.value().getDisplayName(p.locale(), false));
            template.replace("current_level", p -> {
                User user = plugin.getUser(p.player());
                return String.valueOf(user.getSkillLevel(p.value()));
            });
            template.replace("next_level", p -> {
                User user = plugin.getUser(p.player());
                return String.valueOf(user.getSkillLevel(p.value()) + 1);
            });
            template.replace("max_level", p -> String.valueOf(p.value().getMaxLevel()));
            template.replace("cost", p -> {
                User user = plugin.getUser(p.player());
                int currentLevel = user.getSkillLevel(p.value());
                double cost = skillPointsShop.calculateLevelCost(p.value(), currentLevel + 1);
                return String.valueOf((int) cost);
            });
            template.replace("afford_lore", p -> {
                User user = plugin.getUser(p.player());
                int currentLevel = user.getSkillLevel(p.value());
                double cost = skillPointsShop.calculateLevelCost(p.value(), currentLevel + 1);
                return user.getSkillCoins() >= cost ? "&aYou can afford this" : "&cNot enough Skill Coins";
            });
            template.replace("level_lore", p -> {
                User user = plugin.getUser(p.player());
                int currentLevel = user.getSkillLevel(p.value());
                int maxLevel = p.value().getMaxLevel();
                return currentLevel >= maxLevel ? "&cAlready at max level" : "&aLevel available";
            });
            template.replace("click_lore", p -> {
                User user = plugin.getUser(p.player());
                int currentLevel = user.getSkillLevel(p.value());
                int maxLevel = p.value().getMaxLevel();
                if (currentLevel >= maxLevel) {
                    return "&cAlready at max level!";
                }
                double cost = skillPointsShop.calculateLevelCost(p.value(), currentLevel + 1);
                return user.getSkillCoins() >= cost ? "&eClick to buy level!" : "&cCannot afford!";
            });
            template.replace("material", p -> {
                // Use skill-specific material or default
                return p.value().getId().toString().toUpperCase() + "_SWORD";
            });
            
            template.onClick(c -> {
                Skill skill = c.value();
                Player player = c.player();
                User user = plugin.getUser(player);
                Locale locale = user.getLocale();
                
                int currentLevel = user.getSkillLevel(skill);
                int maxLevel = skill.getMaxLevel();
                
                // Check if already at max level
                if (currentLevel >= maxLevel) {
                    player.sendMessage("§cYou are already at the maximum level for " + skill.getDisplayName(locale, false) + "!");
                    return;
                }
                
                double cost = skillPointsShop.calculateLevelCost(skill, currentLevel + 1);
                
                // Check balance
                if (user.getSkillCoins() < cost) {
                    player.sendMessage("§cYou don't have enough Skill Coins! You need " + (int) cost + " but only have " + user.getSkillCoins() + ".");
                    return;
                }
                
                // Attempt to purchase level
                SkillPointsShop.BuyResult result = skillPointsShop.purchaseLevel(user, skill);
                
                if (result.isSuccess()) {
                    String successMsg = plugin.getMsg(CommandMessage.SHOP_LEVEL_SUCCESS, locale);
                    successMsg = TextUtil.replace(successMsg,
                            "{skill}", skill.getDisplayName(locale, false),
                            "{level}", String.valueOf(currentLevel + 1),
                            "{cost}", String.valueOf((int) result.getTotalCost()),
                            "{balance}", String.valueOf(user.getSkillCoins()));
                    player.sendMessage(successMsg);
                    
                    // Refresh menu after 1 tick
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getSlate().openMenu(player, "shop_levels");
                        }
                    }, 1L);
                } else {
                    player.sendMessage("§c" + result.getErrorMessage());
                }
            });
            
            template.definedContexts(m -> plugin.getSkillManager().getEnabledSkills());
        });
    }
}
