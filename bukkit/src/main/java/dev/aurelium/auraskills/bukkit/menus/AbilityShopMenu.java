package dev.aurelium.auraskills.bukkit.menus;

import dev.aurelium.auraskills.api.ability.Ability;
import dev.aurelium.auraskills.api.registry.NamespacedId;
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
import java.util.stream.Collectors;

public class AbilityShopMenu {

    private final AuraSkills plugin;
    private final SkillPointsShop skillPointsShop;

    public AbilityShopMenu(AuraSkills plugin) {
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

        // Template for all buyable abilities
        menu.template("ability", String.class, template -> {
            template.replace("ability_name", p -> {
                SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(p.value());
                if (buyableAbility != null) {
                    Ability ability = plugin.getAbilityRegistry().getOrNull(NamespacedId.fromString(buyableAbility.abilityKey));
                    if (ability != null) {
                        return ability.getDisplayName(p.locale(), false);
                    }
                }
                return p.value().replace("_", " ");
            });
            template.replace("ability_desc", p -> {
                SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(p.value());
                if (buyableAbility != null) {
                    Ability ability = plugin.getAbilityRegistry().getOrNull(NamespacedId.fromString(buyableAbility.abilityKey));
                    if (ability != null) {
                        return plugin.getAbilityManager().getBaseDescription(ability, plugin.getUser(p.player()), false);
                    }
                }
                return "Custom ability";
            });
            template.replace("cost", p -> {
                SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(p.value());
                return buyableAbility != null ? String.valueOf((int) buyableAbility.cost) : "0";
            });
            template.replace("required_skill", p -> {
                SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(p.value());
                return buyableAbility != null ? buyableAbility.requiredSkill : "None";
            });
            template.replace("required_level", p -> {
                SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(p.value());
                return buyableAbility != null ? String.valueOf(buyableAbility.requiredLevel) : "1";
            });
            template.replace("afford_lore", p -> {
                User user = plugin.getUser(p.player());
                SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(p.value());
                double cost = buyableAbility != null ? buyableAbility.cost : 0;
                return user.getSkillCoins() >= cost ? "&aYou can afford this" : "&cNot enough Skill Coins";
            });
            template.replace("requirement_lore", p -> {
                User user = plugin.getUser(p.player());
                SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(p.value());
                if (buyableAbility != null && !buyableAbility.requiredSkill.equals("none")) {
                    // Check if user meets skill level requirement
                    var skill = plugin.getSkillRegistry().getOrNull(NamespacedId.fromString(buyableAbility.requiredSkill));
                    if (skill != null) {
                        int userLevel = user.getSkillLevel(skill);
                        if (userLevel >= buyableAbility.requiredLevel) {
                            return "&aRequirements met";
                        } else {
                            return "&cNeed " + buyableAbility.requiredSkill + " level " + buyableAbility.requiredLevel;
                        }
                    }
                }
                return "&aNo requirements";
            });
            template.replace("purchase_lore", p -> {
                User user = plugin.getUser(p.player());
                SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(p.value());
                if (buyableAbility == null) return "&cNot available";
                
                // Check if already purchased
                Ability ability = plugin.getAbilityRegistry().getOrNull(NamespacedId.fromString(buyableAbility.abilityKey));
                if (ability != null && user.getAbilityLevel(ability) > 0) {
                    return "&cAlready purchased";
                }
                
                // Check requirements
                if (!buyableAbility.requiredSkill.equals("none")) {
                    var skill = plugin.getSkillRegistry().getOrNull(NamespacedId.fromString(buyableAbility.requiredSkill));
                    if (skill != null && user.getSkillLevel(skill) < buyableAbility.requiredLevel) {
                        return "&cRequirements not met";
                    }
                }
                
                // Check balance
                if (user.getSkillCoins() < buyableAbility.cost) {
                    return "&cCannot afford";
                }
                
                return "&eClick to purchase!";
            });
            
            template.onClick(c -> {
                String abilityKey = c.value();
                Player player = c.player();
                User user = plugin.getUser(player);
                Locale locale = user.getLocale();
                
                SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(abilityKey);
                if (buyableAbility == null) {
                    player.sendMessage("§cThis ability is not available for purchase!");
                    return;
                }
                
                // Check if already purchased
                Ability ability = plugin.getAbilityRegistry().getOrNull(NamespacedId.fromString(buyableAbility.abilityKey));
                if (ability != null && user.getAbilityLevel(ability) > 0) {
                    player.sendMessage("§cYou have already purchased this ability!");
                    return;
                }
                
                // Check skill level requirements
                if (!buyableAbility.requiredSkill.equals("none")) {
                    var skill = plugin.getSkillRegistry().getOrNull(NamespacedId.fromString(buyableAbility.requiredSkill));
                    if (skill != null) {
                        int userLevel = user.getSkillLevel(skill);
                        if (userLevel < buyableAbility.requiredLevel) {
                            player.sendMessage("§cYou need " + buyableAbility.requiredSkill + " level " + buyableAbility.requiredLevel + " to purchase this ability!");
                            return;
                        }
                    }
                }
                
                // Check balance
                if (user.getSkillCoins() < buyableAbility.cost) {
                    player.sendMessage("§cYou don't have enough Skill Coins! You need " + (int) buyableAbility.cost + " but only have " + user.getSkillCoins() + ".");
                    return;
                }
                
                // Attempt to purchase ability
                SkillPointsShop.AbilityPurchaseResult result = skillPointsShop.purchaseAbility(user, abilityKey);
                
                if (result.success) {
                    String abilityName = ability != null ? ability.getDisplayName(locale, false) : abilityKey.replace("_", " ");
                    
                    String successMsg = plugin.getMsg(CommandMessage.SHOP_BUY_SUCCESS, locale);
                    successMsg = TextUtil.replace(successMsg,
                            "{amount}", "1",
                            "{item}", abilityName,
                            "{price}", String.valueOf((int) buyableAbility.cost),
                            "{balance}", String.valueOf(user.getSkillCoins()));
                    player.sendMessage(successMsg);
                    
                    // Refresh menu after 1 tick
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getSlate().openMenu(player, "shop_abilities");
                        }
                    }, 1L);
                } else {
                    player.sendMessage("§c" + result.errorMessage);
                }
            });
            
            template.definedContexts(m -> {
                // Only show abilities that the user hasn't purchased yet
                User user = plugin.getUser(m.player());
                return skillPointsShop.getBuyableAbilities().keySet().stream()
                        .filter(abilityKey -> {
                            SkillPointsShop.BuyableAbility buyableAbility = skillPointsShop.getBuyableAbilities().get(abilityKey);
                            if (buyableAbility == null) return false;
                            
                            Ability ability = plugin.getAbilityRegistry().getOrNull(NamespacedId.fromString(buyableAbility.abilityKey));
                            return ability == null || user.getAbilityLevel(ability) <= 0;
                        })
                        .collect(Collectors.toSet());
            });
        });
    }
}