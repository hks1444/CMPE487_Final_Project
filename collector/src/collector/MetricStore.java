package collector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetricStore {
    private static final long RETENTION_PERIOD_MS = 5 * 60 * 1000; // 5 minutes
    private final Map<String, Map<String, Deque<MetricPoint>>> store;
    
    public MetricStore() {
        this.store = new ConcurrentHashMap<>();
    }
    
    public void addMetric(String deviceId, String metricName, double value, long timestamp) {
        store.computeIfAbsent(deviceId, k -> new ConcurrentHashMap<>())
             .computeIfAbsent(metricName, k -> new LinkedList<>())
             .addLast(new MetricPoint(timestamp, value));
        
        pruneOldData(deviceId, metricName);
    }
    
    public void clearMetrics(String deviceId) {
        Map<String, Deque<MetricPoint>> deviceData = store.get(deviceId);
        if (deviceData != null) {
            deviceData.clear();
            System.out.println("Cleared all metrics for device: " + deviceId);
        }
    }
    
    public void clearAllMetrics() {
        for (String deviceId : store.keySet()) {
            clearMetrics(deviceId);
        }
        store.clear();
        System.out.println("Cleared all metrics for all devices");
    }
    
    private void pruneOldData(String deviceId, String metricName) {
        Deque<MetricPoint> points = store.get(deviceId).get(metricName);
        long cutoffTime = System.currentTimeMillis() - RETENTION_PERIOD_MS;
        
        while (!points.isEmpty() && points.getFirst().timestamp < cutoffTime) {
            points.removeFirst();
        }
    }
    
    public List<MetricPoint> getMetrics(String deviceId, String metricName) {
        Map<String, Deque<MetricPoint>> deviceMetrics = store.get(deviceId);
        if (deviceMetrics == null) return Collections.emptyList();
        
        Deque<MetricPoint> metrics = deviceMetrics.get(metricName);
        if (metrics == null) return Collections.emptyList();
        
        return new ArrayList<>(metrics);
    }
    
    public Set<String> getDevices() {
        return new HashSet<>(store.keySet());
    }
    
    public Set<String> getMetricNames(String deviceId) {
        Map<String, Deque<MetricPoint>> deviceMetrics = store.get(deviceId);
        if (deviceMetrics == null) return Collections.emptySet();
        return new HashSet<>(deviceMetrics.keySet());
    }
    
    public static class MetricPoint {
        public final long timestamp;
        public final double value;
        
        public MetricPoint(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}
