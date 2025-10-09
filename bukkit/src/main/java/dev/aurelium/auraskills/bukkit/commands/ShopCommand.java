package dev.aurelium.auraskills.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import org.bukkit.entity.Player;

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
        plugin.getSlate().openMenu(player, "shop");
    }
}
