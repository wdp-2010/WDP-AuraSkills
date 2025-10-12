package dev.aurelium.auraskills.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.economy.SkillPointsShop;
import dev.aurelium.auraskills.common.user.User;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

@CommandAlias("shop")
public class ShopCommand extends BaseCommand {

    private final AuraSkills plugin;

    public ShopCommand(AuraSkills plugin) {
        this.plugin = plugin;
    }

    @Default
    @CommandPermission("auraskills.command.shop")
    @Description("Opens the skill coins shop menu")
    public void onShop(Player player) {
        // Use main shop implementation
        plugin.getMainShopMenu().openMainMenu(player);
    }

    @Subcommand("debug")
    @CommandPermission("auraskills.command.shop.debug")
    @Description("Shows shop configuration debug information")
    public void onDebug(CommandSender sender) {
        SkillPointsShop shop = plugin.getShopManager().getShop();
        
        sender.sendMessage("§6§lShop Debug Information:");
        sender.sendMessage("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // Level purchase settings
        sender.sendMessage("§eLevel Purchase Settings:");
        sender.sendMessage("  §7Base Cost: §f" + shop.getLevelBaseCost());
        sender.sendMessage("  §7Cost Multiplier: §f" + shop.getLevelCostMultiplier());
        
        // Example level costs
        sender.sendMessage("\n§eExample Level Costs:");
        if (!plugin.getSkillManager().getEnabledSkills().isEmpty()) {
            Skill firstSkill = plugin.getSkillManager().getEnabledSkills().iterator().next();
            for (int level : new int[]{0, 10, 20, 50, 100}) {
                double cost = shop.calculateLevelCost(firstSkill, level);
                sender.sendMessage("  §7Level " + level + " → " + (level + 1) + ": §f" + String.format("%.2f", cost) + " §7Skill Coins");
            }
        }
        
        // Sellable items
        sender.sendMessage("\n§eSellable Items: §f" + shop.getSellableItems().size());
        for (Map.Entry<String, Double> entry : shop.getSellableItems().entrySet()) {
            sender.sendMessage("  §7" + entry.getKey() + ": §f" + entry.getValue() + " §7Skill Coins");
        }
        
        // Buyable abilities
        sender.sendMessage("\n§eBuyable Abilities: §f" + shop.getBuyableAbilities().size());
        for (Map.Entry<String, SkillPointsShop.BuyableAbility> entry : shop.getBuyableAbilities().entrySet()) {
            SkillPointsShop.BuyableAbility ability = entry.getValue();
            sender.sendMessage("  §7" + entry.getKey() + ":");
            sender.sendMessage("    §7Cost: §f" + ability.cost + " §7Skill Coins");
            if (!ability.requiredSkill.isEmpty()) {
                sender.sendMessage("    §7Requires: §f" + ability.requiredSkill + " §7Level §f" + ability.requiredLevel);
            }
        }
        
        // Player balance if sender is a player
        if (sender instanceof Player player) {
            User user = plugin.getUser(player);
            sender.sendMessage("\n§eYour Balance: §f" + String.format("%.2f", user.getSkillCoins()) + " §7Skill Coins");
        }
        
        sender.sendMessage("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§7Debug Mode: " + (shop.isDebugMode() ? "§aEnabled" : "§cDisabled"));
    }

    @Subcommand("reload")
    @CommandPermission("auraskills.command.shop.reload")
    @Description("Reloads the shop configuration")
    public void onReload(CommandSender sender) {
        try {
            // The shop configuration is loaded on each ShopMenu instantiation,
            // so we just need to notify the sender
            sender.sendMessage("§aShop configuration will be reloaded on next shop open.");
            sender.sendMessage("§7Note: Shop config is automatically loaded from shop_config.yml");
        } catch (Exception e) {
            sender.sendMessage("§cFailed to reload shop configuration: " + e.getMessage());
        }
    }

    @Subcommand("test")
    @CommandPermission("auraskills.command.shop.test")
    @Description("Opens the merged shop menu directly for testing")
    public void onTest(Player player) {
        plugin.getSlate().openMenu(player, "shop_merged");
        player.sendMessage("§aOpening merged shop menu...");
    }
}
