package exporter;

import java.util.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import com.sun.management.OperatingSystemMXBean;

public class MetricCollector {
    private final String deviceId;
    private final com.sun.management.OperatingSystemMXBean osBean;
    private final ThreadMXBean threadBean;
    
    public MetricCollector(String deviceId) {
        this.deviceId = deviceId;
        this.threadBean = ManagementFactory.getThreadMXBean();
        
        Object tempBean = ManagementFactory.getOperatingSystemMXBean();
        if (!(tempBean instanceof com.sun.management.OperatingSystemMXBean)) {
            throw new RuntimeException("com.sun.management.OperatingSystemMXBean not available");
        }
        this.osBean = (com.sun.management.OperatingSystemMXBean) tempBean;
    }
    
    public Map<String, Object> collectMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("dev", deviceId);
        metrics.put("ts", System.currentTimeMillis() / 1000);
        
        List<Object[]> metricList = new ArrayList<>();
        
        // Cast to com.sun.management.OperatingSystemMXBean (we checked in constructor)
        com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
        
        // Get CPU usage
        double cpuUsage = sunOsBean.getCpuLoad();
        if (cpuUsage < 0) {
            cpuUsage = 0.0; // Default to 0 if not available
        }
        metricList.add(new Object[]{"cpu_usage", cpuUsage}); // 0-1 scale
        
        // Memory metrics
        long totalMemory = sunOsBean.getTotalMemorySize();
        long freeMemory = sunOsBean.getFreeMemorySize();
        metricList.add(new Object[]{"mem_total", totalMemory});
        metricList.add(new Object[]{"mem_free", freeMemory});
        
        // Thread count
        int threadCount = threadBean.getThreadCount();
        metricList.add(new Object[]{"threads", threadCount});
        
        metrics.put("m", metricList);
        return metrics;
    }
    
    public List<Map<String, Object>> checkAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();
        
        try {
            // Check CPU usage
            double cpuUsage = osBean.getCpuLoad();
            if (cpuUsage > 0.8) { // CPU usage > 80%
                Map<String, Object> alert = new HashMap<>();
                alert.put("metric", "cpu_usage");
                alert.put("value", cpuUsage);
                alerts.add(alert);
            }
            
            // Check memory
            long freeMemory = osBean.getFreeMemorySize();
            if (freeMemory < 100_000_000) { // Free memory < 100MB
                Map<String, Object> alert = new HashMap<>();
                alert.put("metric", "mem_free");
                alert.put("value", freeMemory);
                alerts.add(alert);
            }
            
            // Check thread count
            int threadCount = threadBean.getThreadCount();
            if (threadCount > 100) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("metric", "threads");
                alert.put("value", threadCount);
                alerts.add(alert);
            }
            
        } catch (Exception e) {
            System.err.println("Error checking alerts: " + e.getMessage());
        }
        
        return alerts;
    }
}
