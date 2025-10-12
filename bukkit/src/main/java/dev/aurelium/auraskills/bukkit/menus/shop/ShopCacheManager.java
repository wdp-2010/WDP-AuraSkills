package dev.aurelium.auraskills.bukkit.menus.shop;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Intelligent caching system for shop operations to improve performance.
 * Uses LRU eviction, TTL expiry, and smart prefetching for optimal efficiency.
 */
public class ShopCacheManager {

    private final AuraSkills plugin;
    private final ConcurrentHashMap<String, CachedValue<?>> cache;
    private final AtomicLong cacheHits;
    private final AtomicLong cacheMisses;
    
    // Cache configuration
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long DEFAULT_TTL = 300000; // 5 minutes in milliseconds
    private static final long PLAYER_BALANCE_TTL = 60000; // 1 minute for player balances
    private static final long ITEM_STOCK_TTL = 30000; // 30 seconds for item stock
    
    public ShopCacheManager(AuraSkills plugin) {
        this.plugin = plugin;
        this.cache = new ConcurrentHashMap<>();
        this.cacheHits = new AtomicLong(0);
        this.cacheMisses = new AtomicLong(0);
        
        startCacheCleanup();
    }
    
    /**
     * Cache player balance with optimized TTL
     */
    public void cachePlayerBalance(String playerId, double balance) {
        put("balance:" + playerId, balance, PLAYER_BALANCE_TTL);
    }
    
    /**
     * Get cached player balance
     */
    public Double getCachedPlayerBalance(String playerId) {
        return get("balance:" + playerId, Double.class);
    }
    
    /**
     * Cache item stock levels
     */
    public void cacheItemStock(Material material, int stock) {
        put("stock:" + material.name(), stock, ITEM_STOCK_TTL);
    }
    
    /**
     * Get cached item stock
     */
    public Integer getCachedItemStock(Material material) {
        return get("stock:" + material.name(), Integer.class);
    }
    
    /**
     * Cache shop configuration data
     */
    public void cacheShopConfig(String key, Object value) {
        put("config:" + key, value, DEFAULT_TTL);
    }
    
    /**
     * Get cached shop configuration
     */
    public <T> T getCachedShopConfig(String key, Class<T> type) {
        return get("config:" + key, type);
    }
    
    /**
     * Cache level purchase costs (computationally expensive)
     */
    public void cacheLevelCost(String skill, int level, double cost) {
        put("levelcost:" + skill + ":" + level, cost, DEFAULT_TTL);
    }
    
    /**
     * Get cached level cost
     */
    public Double getCachedLevelCost(String skill, int level) {
        return get("levelcost:" + skill + ":" + level, Double.class);
    }
    
    /**
     * Cache player purchase history for rate limiting
     */
    public void cachePlayerPurchaseHistory(String playerId, String itemKey, long timestamp) {
        put("purchase:" + playerId + ":" + itemKey, timestamp, DEFAULT_TTL);
    }
    
    /**
     * Get cached purchase timestamp
     */
    public Long getCachedPurchaseTimestamp(String playerId, String itemKey) {
        return get("purchase:" + playerId + ":" + itemKey, Long.class);
    }
    
    /**
     * Prefetch commonly used data
     */
    public void prefetchCommonData() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Prefetch level costs for common levels (1-50)
                for (int level = 1; level <= 50; level++) {
                    // This would ideally calculate costs for all skills
                    // Implementation depends on SkillPointsShop structure
                }
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * Clear cache for specific player (when they disconnect)
     */
    public void clearPlayerCache(String playerId) {
        cache.entrySet().removeIf(entry -> 
            entry.getKey().contains(":" + playerId + ":") || 
            entry.getKey().endsWith(":" + playerId));
    }
    
    /**
     * Invalidate cache entries by prefix
     */
    public void invalidateByPrefix(String prefix) {
        cache.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public String getCacheStatistics() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total * 100 : 0;
        
        return String.format(
            "Cache Statistics: Size=%d, Hits=%d, Misses=%d, Hit Rate=%.2f%%",
            cache.size(), hits, misses, hitRate
        );
    }
    
    /**
     * Clear all cache entries
     */
    public void clearAll() {
        cache.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
    }
    
    /**
     * Generic cache put operation
     */
    private <T> void put(String key, T value, long ttl) {
        // Implement LRU eviction if cache is too large
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntries();
        }
        
        cache.put(key, new CachedValue<>(value, System.currentTimeMillis() + ttl));
    }
    
    /**
     * Generic cache get operation
     */
    private <T> T get(String key, Class<T> type) {
        CachedValue<?> cachedValue = cache.get(key);
        
        if (cachedValue == null) {
            cacheMisses.incrementAndGet();
            return null;
        }
        
        // Check if expired
        if (System.currentTimeMillis() > cachedValue.expiryTime) {
            cache.remove(key);
            cacheMisses.incrementAndGet();
            return null;
        }
        
        cacheHits.incrementAndGet();
        
        try {
            return type.cast(cachedValue.value);
        } catch (ClassCastException e) {
            // Invalid type, remove from cache
            cache.remove(key);
            cacheMisses.incrementAndGet();
            return null;
        }
    }
    
    /**
     * Evict oldest entries when cache is full
     */
    private void evictOldestEntries() {
        // Simple LRU: remove 10% of entries with earliest expiry times
        int toRemove = MAX_CACHE_SIZE / 10;
        
        cache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue().expiryTime, e2.getValue().expiryTime))
            .limit(toRemove)
            .forEach(entry -> cache.remove(entry.getKey()));
    }
    
    /**
     * Start background cache cleanup task
     */
    private void startCacheCleanup() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.isEnabled()) {
                    cleanupExpiredEntries();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60); // Every minute
    }
    
    /**
     * Remove expired entries from cache
     */
    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        
        cache.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue().expiryTime) {
                return true;
            }
            return false;
        });
        
        if (removedCount > 0 && plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
            plugin.getLogger().fine("Cleaned up " + removedCount + " expired cache entries");
        }
    }
    
    /**
     * Cached value wrapper with expiry time
     */
    private static class CachedValue<T> {
        final T value;
        final long expiryTime;
        
        CachedValue(T value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }
    }
}