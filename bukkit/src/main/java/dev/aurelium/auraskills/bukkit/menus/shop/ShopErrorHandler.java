package dev.aurelium.auraskills.bukkit.menus.shop;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import dev.aurelium.auraskills.common.message.type.CommandMessage;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Enhanced error handling and user experience manager for shop operations.
 * Provides graceful error recovery, user-friendly messaging, and operation validation.
 */
public class ShopErrorHandler {

    private final AuraSkills plugin;
    private final ConcurrentHashMap<String, Integer> playerErrorCounts;
    private final ConcurrentHashMap<String, Long> lastErrorTimes;
    
    // Error thresholds and limits
    private static final int MAX_ERRORS_PER_PLAYER = 10;
    private static final long ERROR_RESET_TIME = 300000; // 5 minutes
    private static final long RATE_LIMIT_WINDOW = 1000; // 1 second
    
    public ShopErrorHandler(AuraSkills plugin) {
        this.plugin = plugin;
        this.playerErrorCounts = new ConcurrentHashMap<>();
        this.lastErrorTimes = new ConcurrentHashMap<>();
    }
    
    /**
     * Handle transaction errors with graceful recovery
     */
    public boolean handleTransactionError(Player player, String operation, Exception error) {
        String playerId = player.getUniqueId().toString();
        
        // Check if player is hitting error limits
        if (isPlayerBlocked(playerId)) {
            player.sendMessage("§cShop temporarily unavailable due to too many errors. Please try again later.");
            return false;
        }
        
        // Increment error count
        incrementErrorCount(playerId);
        
        // Log error with context
        plugin.getLogger().log(Level.WARNING, String.format(
            "Shop transaction error for player %s during %s: %s", 
            player.getName(), operation, error.getMessage()
        ), error);
        
        // Provide user-friendly error messages based on error type
        if (error instanceof IllegalArgumentException) {
            player.sendMessage("§cInvalid shop operation. Please try again.");
        } else if (error instanceof IllegalStateException) {
            player.sendMessage("§cShop is temporarily unavailable. Please try again later.");
        } else if (error.getMessage() != null && error.getMessage().contains("insufficient")) {
            sendMessage(player, CommandMessage.SHOP_INSUFFICIENT_FUNDS);
        } else {
            sendMessage(player, CommandMessage.SHOP_PURCHASE_FAILED);
        }
        
        return true; // Error handled gracefully
    }
    
    /**
     * Validate shop operation before execution
     */
    public ValidationResult validateOperation(Player player, String operation, Object... parameters) {
        String playerId = player.getUniqueId().toString();
        
        // Check rate limiting
        if (isRateLimited(playerId)) {
            return ValidationResult.failure("Rate limit exceeded. Please wait before trying again.");
        }
        
        // Check if player is blocked due to errors
        if (isPlayerBlocked(playerId)) {
            return ValidationResult.failure("Too many errors. Shop temporarily unavailable.");
        }
        
        // Validate based on operation type
        switch (operation.toLowerCase()) {
            case "purchase_level":
                return validateLevelPurchase(player, parameters);
            case "sell_item":
                return validateItemSale(player, parameters);
            case "buy_item":
                return validateItemPurchase(player, parameters);
            case "buy_ability":
                return validateAbilityPurchase(player, parameters);
            default:
                return ValidationResult.failure("Unknown operation type");
        }
    }
    
    /**
     * Validate level purchase operation
     */
    private ValidationResult validateLevelPurchase(Player player, Object... parameters) {
        if (parameters.length < 1) {
            return ValidationResult.failure("Missing skill parameter");
        }
        
        String skillName = parameters[0].toString();
        if (skillName == null || skillName.trim().isEmpty()) {
            return ValidationResult.failure("Invalid skill name");
        }
        
        // Additional validation could be added here
        // - Check if skill exists
        // - Check if player can purchase levels for this skill
        // - Check if player has sufficient funds
        
        return ValidationResult.success();
    }
    
    /**
     * Validate item sale operation
     */
    private ValidationResult validateItemSale(Player player, Object... parameters) {
        if (parameters.length < 2) {
            return ValidationResult.failure("Missing material or amount parameter");
        }
        
        try {
            int amount = Integer.parseInt(parameters[1].toString());
            
            if (amount <= 0) {
                return ValidationResult.failure("Amount must be positive");
            }
            
            if (amount > 64) {
                return ValidationResult.failure("Cannot sell more than 64 items at once");
            }
            
            // Check if player has the items
            // This would need integration with inventory checking
            // String materialName = parameters[0].toString(); - used for inventory validation
            
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Invalid amount specified");
        }
    }
    
    /**
     * Validate item purchase operation
     */
    private ValidationResult validateItemPurchase(Player player, Object... parameters) {
        if (parameters.length < 2) {
            return ValidationResult.failure("Missing item or amount parameter");
        }
        
        try {
            int amount = Integer.parseInt(parameters[1].toString());
            
            if (amount <= 0) {
                return ValidationResult.failure("Amount must be positive");
            }
            
            // Check inventory space
            if (player.getInventory().firstEmpty() == -1) {
                return ValidationResult.failure("Inventory is full");
            }
            
            // String itemName = parameters[0].toString(); - used for item validation
            
            return ValidationResult.success();
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Invalid amount specified");
        }
    }
    
    /**
     * Validate ability purchase operation
     */
    private ValidationResult validateAbilityPurchase(Player player, Object... parameters) {
        if (parameters.length < 1) {
            return ValidationResult.failure("Missing ability parameter");
        }
        
        String abilityName = parameters[0].toString();
        if (abilityName == null || abilityName.trim().isEmpty()) {
            return ValidationResult.failure("Invalid ability name");
        }
        
        // Additional validation:
        // - Check if ability exists
        // - Check if player meets requirements
        // - Check if player already has the ability
        
        return ValidationResult.success();
    }
    
    /**
     * Send user-friendly error message
     */
    private void sendMessage(Player player, CommandMessage message) {
        // This would integrate with the plugin's message system
        // For now, we'll send a simple message
        player.sendMessage("§c" + message.toString());
    }
    
    /**
     * Check if player is rate limited
     */
    private boolean isRateLimited(String playerId) {
        Long lastError = lastErrorTimes.get(playerId);
        if (lastError == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastError < RATE_LIMIT_WINDOW;
    }
    
    /**
     * Check if player is blocked due to too many errors
     */
    private boolean isPlayerBlocked(String playerId) {
        Integer errorCount = playerErrorCounts.get(playerId);
        if (errorCount == null) {
            return false;
        }
        
        Long lastError = lastErrorTimes.get(playerId);
        if (lastError == null) {
            return false;
        }
        
        // Reset error count if enough time has passed
        if (System.currentTimeMillis() - lastError > ERROR_RESET_TIME) {
            playerErrorCounts.remove(playerId);
            lastErrorTimes.remove(playerId);
            return false;
        }
        
        return errorCount >= MAX_ERRORS_PER_PLAYER;
    }
    
    /**
     * Increment error count for player
     */
    private void incrementErrorCount(String playerId) {
        playerErrorCounts.merge(playerId, 1, Integer::sum);
        lastErrorTimes.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Clear error history for a player (e.g., when they disconnect)
     */
    public void clearPlayerErrors(String playerId) {
        playerErrorCounts.remove(playerId);
        lastErrorTimes.remove(playerId);
    }
    
    /**
     * Get error statistics for monitoring
     */
    public String getErrorStatistics() {
        int totalPlayers = playerErrorCounts.size();
        int blockedPlayers = (int) playerErrorCounts.entrySet().stream()
            .filter(entry -> entry.getValue() >= MAX_ERRORS_PER_PLAYER)
            .count();
        
        return String.format("Error Statistics: %d players with errors, %d blocked", 
            totalPlayers, blockedPlayers);
    }
    
    /**
     * Validation result wrapper
     */
    public static class ValidationResult {
        private final boolean success;
        private final String errorMessage;
        
        private ValidationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public boolean isFailure() {
            return !success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}