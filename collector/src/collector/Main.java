package collector;

public class Main {
    private static final int UDP_PORT = Integer.parseInt(System.getenv().getOrDefault("UDP_PORT", "4000"));
    private static final int WS_PORT = Integer.parseInt(System.getenv().getOrDefault("WS_PORT", "8080"));
    private static final int HTTP_PORT = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "8081"));
    
    public static void main(String[] args) throws Exception {
        MetricStore metricStore = new MetricStore();
        WebSocketHandler webSocketHandler = new WebSocketHandler(WS_PORT);
        UdpServer udpServer = new UdpServer(UDP_PORT, metricStore, webSocketHandler);
        WorkingHttpServer httpServer = new WorkingHttpServer(HTTP_PORT, metricStore);
        
        // Start HTTP server first
        httpServer.start();
        
        // Start WebSocket server
        webSocketHandler.start();
        
        // Start UDP server in a separate thread
        Thread udpThread = new Thread(udpServer);
        udpThread.start();
        
        System.out.println("Metric Collector started:");
        System.out.println("- UDP server listening on port " + UDP_PORT);
        System.out.println("- WebSocket server listening on port " + WS_PORT);
        System.out.println("- HTTP API server listening on port " + HTTP_PORT);
        System.out.println("\nAPI Endpoints:");
        System.out.println("- GET http://localhost:" + HTTP_PORT + "/api/metrics");
        System.out.println("- GET http://localhost:" + HTTP_PORT + "/api/devices");
        System.out.println("- GET http://localhost:" + HTTP_PORT + "/api/device/{deviceId}");
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Metric Collector...");
            udpServer.stop();
            httpServer.stop();
            try {
                webSocketHandler.stop();
            } catch (InterruptedException e) {
                System.err.println("Error stopping WebSocket server: " + e.getMessage());
            }
        }));
    }
}