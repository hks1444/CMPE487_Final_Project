package exporter;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

public class Main {
    private static final String DEVICE_ID = System.getenv().getOrDefault("DEVICE_ID", "exporter1");
    private static final String COLLECTOR_HOST = System.getenv().getOrDefault("COLLECTOR_HOST", "collector");
    private static final int COLLECTOR_PORT = Integer.parseInt(System.getenv().getOrDefault("COLLECTOR_PORT", "4000"));
    private static final int HTTP_PORT = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "8080"));
    
    public static void main(String[] args) throws Exception {
        // Initialize components
        MetricCollector metricCollector = new MetricCollector(DEVICE_ID);
        UdpSender udpSender = new UdpSender(COLLECTOR_HOST, COLLECTOR_PORT);
        
        // Setup HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(HTTP_PORT), 0);
        server.createContext("/load", new LoadHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        
        System.out.println("Metric Exporter started on port " + HTTP_PORT);
        System.out.println("Sending metrics to " + COLLECTOR_HOST + ":" + COLLECTOR_PORT);
        
        // Schedule metric collection and sending
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        
        // Schedule metric collection every 100ms
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Collect and send metrics
                udpSender.sendMetrics(metricCollector.collectMetrics());
                
                // Check and send alerts if any
                var alerts = metricCollector.checkAlerts();
                if (!alerts.isEmpty()) {
                    udpSender.sendAlert(DEVICE_ID, alerts);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Metric Exporter...");
            scheduler.shutdown();
            server.stop(0);
            udpSender.close();
        }));
    }
}
