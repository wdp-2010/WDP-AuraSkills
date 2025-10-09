package dev.aurelium.auraskills.common.economy;

import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.user.User;

import java.util.UUID;

public class SkillCoinsManager {

    private final AuraSkillsPlugin plugin;

    public SkillCoinsManager(AuraSkillsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Deposits skill coins to a user's balance
     *
     * @param user The user to deposit to
     * @param amount The amount to deposit
     */
    public void deposit(User user, double amount) {
        if (amount <= 0) return;
        user.addSkillCoins(amount);
    }

    /**
     * Withdraws skill coins from a user's balance
     *
     * @param user The user to withdraw from
     * @param amount The amount to withdraw
     * @return true if the transaction was successful, false if insufficient funds
     */
    public boolean withdraw(User user, double amount) {
        if (amount <= 0) return false;
        return user.withdrawSkillCoins(amount);
    }

    /**
     * Gets the balance of a user
     *
     * @param user The user to get the balance of
     * @return The user's skill coins balance
     */
    public double getBalance(User user) {
        return user.getSkillCoins();
    }

    /**
     * Sets the balance of a user
     *
     * @param user The user to set the balance of
     * @param amount The new balance
     */
    public void setBalance(User user, double amount) {
        user.setSkillCoins(amount);
    }

    /**
     * Checks if a user has a certain amount of skill coins
     *
     * @param user The user to check
     * @param amount The amount to check for
     * @return true if the user has enough skill coins
     */
    public boolean has(User user, double amount) {
        return user.hasSkillCoins(amount);
    }

    /**
     * Transfers skill coins from one user to another
     *
     * @param from The user sending coins
     * @param to The user receiving coins
     * @param amount The amount to transfer
     * @return true if the transaction was successful
     */
    public boolean transfer(User from, User to, double amount) {
        if (amount <= 0) return false;
        if (!from.hasSkillCoins(amount)) return false;
        
        if (from.withdrawSkillCoins(amount)) {
            to.addSkillCoins(amount);
            return true;
        }
        return false;
    }

    /**
     * Formats a skill coins amount to a string
     *
     * @param amount The amount to format
     * @return The formatted string
     */
    public String format(double amount) {
        if (amount == Math.floor(amount)) {
            return String.format("%,d", (long) amount);
        } else {
            return String.format("%,.2f", amount);
        }
    }
}
