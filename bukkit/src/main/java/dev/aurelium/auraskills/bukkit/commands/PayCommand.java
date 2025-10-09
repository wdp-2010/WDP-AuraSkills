package dev.aurelium.auraskills.bukkit.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.economy.SkillCoinsManager;
import dev.aurelium.auraskills.common.message.type.CommandMessage;
import dev.aurelium.auraskills.common.user.User;
import dev.aurelium.auraskills.common.util.text.TextUtil;
import org.bukkit.entity.Player;

import java.util.Locale;

@CommandAlias("pay|skillpay")
public class PayCommand extends BaseCommand {

    private final AuraSkills plugin;
    private final SkillCoinsManager coinsManager;

    public PayCommand(AuraSkills plugin) {
        this.plugin = plugin;
        this.coinsManager = plugin.getSkillCoinsManager();
    }

    @Default
    @CommandPermission("auraskills.command.pay")
    @Description("Pay skill coins to another player")
    @CommandCompletion("@players")
    @Syntax("<player> <amount>")
    public void onPay(Player sender, @Flags("other") Player target, double amount) {
        User senderUser = plugin.getUser(sender);
        Locale locale = senderUser.getLocale();

        // Validate amount
        if (amount <= 0) {
            sender.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(CommandMessage.PAY_INVALID_AMOUNT, locale));
            return;
        }

        // Check if trying to pay self
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(CommandMessage.PAY_CANNOT_PAY_SELF, locale));
            return;
        }

        // Get target user
        User targetUser = plugin.getUser(target);
        if (targetUser == null) {
            sender.sendMessage(plugin.getPrefix(locale) + plugin.getMsg(CommandMessage.PAY_TARGET_NOT_FOUND, locale));
            return;
        }

        // Check if sender has enough funds
        if (!coinsManager.has(senderUser, amount)) {
            sender.sendMessage(plugin.getPrefix(locale) + TextUtil.replace(
                    plugin.getMsg(CommandMessage.PAY_INSUFFICIENT_FUNDS, locale),
                    "{balance}", coinsManager.format(coinsManager.getBalance(senderUser))
            ));
            return;
        }

        // Perform transfer
        if (coinsManager.transfer(senderUser, targetUser, amount)) {
            // Send success messages
            sender.sendMessage(plugin.getPrefix(locale) + TextUtil.replace(
                    plugin.getMsg(CommandMessage.PAY_SENT, locale),
                    "{amount}", coinsManager.format(amount),
                    "{player}", target.getName()
            ));

            Locale targetLocale = targetUser.getLocale();
            target.sendMessage(plugin.getPrefix(targetLocale) + TextUtil.replace(
                    plugin.getMsg(CommandMessage.PAY_RECEIVED, targetLocale),
                    "{amount}", coinsManager.format(amount),
                    "{player}", sender.getName()
            ));
        }
    }
}
