package dev.aurelium.auraskills.bukkit.menus.shop;

import dev.aurelium.auraskills.bukkit.AuraSkills;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Performance monitoring and optimization utility for the shop system.
 * Tracks performance metrics and provides automated optimization recommendations.
 */
public class ShopPerformanceMonitor {

    private final AuraSkills plugin;
    private final ConcurrentHashMap<String, PerformanceMetric> metrics;
    private final AtomicInteger activeTransactions;
    private final long startTime;
    
    // Performance thresholds (configurable)
    private static final long SLOW_TRANSACTION_THRESHOLD = 100; // milliseconds
    private static final int HIGH_CONCURRENT_TRANSACTION_THRESHOLD = 50;
    private static final long MEMORY_WARNING_THRESHOLD = 500 * 1024 * 1024; // 500MB
    
    public ShopPerformanceMonitor(AuraSkills plugin) {
        this.plugin = plugin;
        this.metrics = new ConcurrentHashMap<>();
        this.activeTransactions = new AtomicInteger(0);
        this.startTime = System.currentTimeMillis();
        
        // Start monitoring task
        startPerformanceMonitoring();
    }
    
    /**
     * Record a shop transaction for performance tracking
     */
    public void recordTransaction(String operation, long executionTime) {
        activeTransactions.incrementAndGet();
        
        PerformanceMetric metric = metrics.computeIfAbsent(operation, 
            k -> new PerformanceMetric());
        metric.addSample(executionTime);
        
        // Log slow transactions
        if (executionTime > SLOW_TRANSACTION_THRESHOLD) {
            plugin.getLogger().warning(String.format(
                "Slow shop operation: %s took %dms (threshold: %dms)",
                operation, executionTime, SLOW_TRANSACTION_THRESHOLD
            ));
        }
        
        activeTransactions.decrementAndGet();
    }
    
    /**
     * Start a performance monitoring session for an operation
     */
    public PerformanceSession startSession(String operation) {
        return new PerformanceSession(operation);
    }
    
    /**
     * Get performance statistics for debugging
     */
    public String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Shop Performance Report ===\n");
        report.append(String.format("Uptime: %d minutes\n", 
            (System.currentTimeMillis() - startTime) / 60000));
        report.append(String.format("Active transactions: %d\n", 
            activeTransactions.get()));
        report.append(String.format("Memory usage: %.2f MB\n", 
            getMemoryUsage() / (1024.0 * 1024.0)));
        
        report.append("\n--- Operation Statistics ---\n");
        for (String operation : metrics.keySet()) {
            PerformanceMetric metric = metrics.get(operation);
            report.append(String.format("%s: avg=%.2fms, max=%dms, count=%d\n",
                operation, metric.getAverageTime(), metric.getMaxTime(), 
                metric.getCount()));
        }
        
        return report.toString();
    }
    
    /**
     * Get optimization recommendations based on performance data
     */
    public String getOptimizationRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("=== Performance Optimization Recommendations ===\n");
        
        // Check for slow operations
        for (String operation : metrics.keySet()) {
            PerformanceMetric metric = metrics.get(operation);
            if (metric.getAverageTime() > SLOW_TRANSACTION_THRESHOLD) {
                recommendations.append(String.format(
                    "⚠ SLOW OPERATION: %s (avg: %.2fms) - Consider caching or optimization\n",
                    operation, metric.getAverageTime()
                ));
            }
        }
        
        // Check memory usage
        long memoryUsage = getMemoryUsage();
        if (memoryUsage > MEMORY_WARNING_THRESHOLD) {
            recommendations.append(String.format(
                "⚠ HIGH MEMORY: %.2f MB - Consider reducing cache sizes or garbage collection\n",
                memoryUsage / (1024.0 * 1024.0)
            ));
        }
        
        // Check concurrent transactions
        if (activeTransactions.get() > HIGH_CONCURRENT_TRANSACTION_THRESHOLD) {
            recommendations.append(String.format(
                "⚠ HIGH CONCURRENCY: %d active transactions - Consider rate limiting\n",
                activeTransactions.get()
            ));
        }
        
        // General recommendations
        recommendations.append("\n--- General Recommendations ---\n");
        recommendations.append("• Use async operations for database writes\n");
        recommendations.append("• Implement transaction batching for high-volume operations\n");
        recommendations.append("• Cache frequently accessed data (player balances, shop items)\n");
        recommendations.append("• Consider lazy loading for expensive menu operations\n");
        recommendations.append("• Use object pooling for frequently created objects\n");
        
        return recommendations.toString();
    }
    
    /**
     * Start automated performance monitoring
     */
    private void startPerformanceMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Log performance summary every 10 minutes
                if (plugin.isEnabled()) {
                    checkPerformanceThresholds();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60 * 10, 20L * 60 * 10); // Every 10 minutes
    }
    
    /**
     * Check performance thresholds and log warnings
     */
    private void checkPerformanceThresholds() {
        // Check for consistently slow operations
        for (String operation : metrics.keySet()) {
            PerformanceMetric metric = metrics.get(operation);
            if (metric.getCount() > 10 && metric.getAverageTime() > SLOW_TRANSACTION_THRESHOLD) {
                plugin.getLogger().log(Level.WARNING, String.format(
                    "Consistently slow operation detected: %s (avg: %.2fms over %d samples)",
                    operation, metric.getAverageTime(), metric.getCount()
                ));
            }
        }
        
        // Check memory usage
        long memoryUsage = getMemoryUsage();
        if (memoryUsage > MEMORY_WARNING_THRESHOLD) {
            plugin.getLogger().log(Level.WARNING, String.format(
                "High memory usage detected: %.2f MB - Consider optimization",
                memoryUsage / (1024.0 * 1024.0)
            ));
        }
    }
    
    /**
     * Get current memory usage
     */
    private long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Performance metric tracking class
     */
    private static class PerformanceMetric {
        private long totalTime = 0;
        private long maxTime = 0;
        private int count = 0;
        
        public synchronized void addSample(long time) {
            totalTime += time;
            maxTime = Math.max(maxTime, time);
            count++;
        }
        
        public synchronized double getAverageTime() {
            return count > 0 ? (double) totalTime / count : 0;
        }
        
        public synchronized long getMaxTime() {
            return maxTime;
        }
        
        public synchronized int getCount() {
            return count;
        }
    }
    
    /**
     * Performance session for tracking individual operations
     */
    public class PerformanceSession implements AutoCloseable {
        private final String operation;
        private final long startTime;
        
        public PerformanceSession(String operation) {
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
        }
        
        @Override
        public void close() {
            long executionTime = System.currentTimeMillis() - startTime;
            recordTransaction(operation, executionTime);
        }
    }
}