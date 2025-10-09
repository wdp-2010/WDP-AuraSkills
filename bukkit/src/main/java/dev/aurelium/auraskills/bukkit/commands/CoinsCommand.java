package dev.aurelium.auraskills.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.economy.SkillCoinsManager;
import dev.aurelium.auraskills.common.message.type.CommandMessage;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

@CommandAlias("%skills_alias")
@Subcommand("coins|skillcoins")
public class CoinsCommand extends BaseCommand {

    private final AuraSkills plugin;
    private final SkillCoinsManager coinsManager;

    public CoinsCommand(AuraSkills plugin) {
        this.plugin = plugin;
        this.coinsManager = plugin.getSkillCoinsManager();
    }

    @Subcommand("balance|bal")
    @CommandPermission("auraskills.command.coins.balance")
    @Description("Check skill coins balance")
    @CommandCompletion("@players")
    public void onBalance(CommandSender sender, @Optional Player target) {
        Player player = target != null ? target : (sender instanceof Player ? (Player) sender : null);
        if (player == null) {
            sender.sendMessage("Console must specify a player");
            return;
        }

        User user = plugin.getUser(player);
        Locale locale = plugin.getLocale(sender);
        
        double balance = coinsManager.getBalance(user);
        sender.sendMessage(plugin.getPrefix(locale) + TextUtil.replace(
                plugin.getMsg(CommandMessage.COINS_BALANCE, locale),
                "{player}", player.getName(),
                "{balance}", coinsManager.format(balance)
        ));
    }

    @Subcommand("add")
    @CommandPermission("auraskills.command.coins.add")
    @Description("Add skill coins to a player")
    @CommandCompletion("@players")
    public void onAdd(CommandSender sender, @Flags("other") Player target, double amount) {
        if (amount <= 0) {
            sender.sendMessage("Amount must be positive");
            return;
        }

        User user = plugin.getUser(target);
        Locale locale = plugin.getLocale(sender);
        
        coinsManager.deposit(user, amount);
        
        sender.sendMessage(plugin.getPrefix(locale) + TextUtil.replace(
                plugin.getMsg(CommandMessage.COINS_ADD_ADDED, locale),
                "{amount}", coinsManager.format(amount),
                "{player}", target.getName(),
                "{balance}", coinsManager.format(coinsManager.getBalance(user))
        ));
    }

    @Subcommand("set")
    @CommandPermission("auraskills.command.coins.set")
    @Description("Set skill coins balance of a player")
    @CommandCompletion("@players")
    public void onSet(CommandSender sender, @Flags("other") Player target, double amount) {
        if (amount < 0) {
            sender.sendMessage("Amount cannot be negative");
            return;
        }

        User user = plugin.getUser(target);
        Locale locale = plugin.getLocale(sender);
        
        coinsManager.setBalance(user, amount);
        
        sender.sendMessage(plugin.getPrefix(locale) + TextUtil.replace(
                plugin.getMsg(CommandMessage.COINS_SET_SET, locale),
                "{amount}", coinsManager.format(amount),
                "{player}", target.getName()
        ));
    }

    @Subcommand("remove")
    @CommandPermission("auraskills.command.coins.remove")
    @Description("Remove skill coins from a player")
    @CommandCompletion("@players")
    public void onRemove(CommandSender sender, @Flags("other") Player target, double amount) {
        if (amount <= 0) {
            sender.sendMessage("Amount must be positive");
            return;
        }

        User user = plugin.getUser(target);
        Locale locale = plugin.getLocale(sender);
        
        if (coinsManager.withdraw(user, amount)) {
            sender.sendMessage(plugin.getPrefix(locale) + TextUtil.replace(
                    plugin.getMsg(CommandMessage.COINS_REMOVE_REMOVED, locale),
                    "{amount}", coinsManager.format(amount),
                    "{player}", target.getName(),
                    "{balance}", coinsManager.format(coinsManager.getBalance(user))
            ));
        } else {
            sender.sendMessage(plugin.getPrefix(locale) + "Player does not have enough skill coins");
        }
    }
}
