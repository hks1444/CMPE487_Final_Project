package collector;

public class Main {
    private static final int UDP_PORT = Integer.parseInt(System.getenv().getOrDefault("UDP_PORT", "4000"));
    private static final int WS_PORT = Integer.parseInt(System.getenv().getOrDefault("WS_PORT", "8080"));
    
    public static void main(String[] args) throws Exception {
        MetricStore metricStore = new MetricStore();
        WebSocketHandler webSocketHandler = new WebSocketHandler(WS_PORT);
        UdpServer udpServer = new UdpServer(UDP_PORT, metricStore, webSocketHandler);
        
        // Start WebSocket server
        webSocketHandler.start();
        
        // Start UDP server in a separate thread
        Thread udpThread = new Thread(udpServer);
        udpThread.start();
        
        System.out.println("Metric Collector started:");
        System.out.println("- UDP server listening on port " + UDP_PORT);
        System.out.println("- WebSocket server listening on port " + WS_PORT);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Metric Collector...");
            udpServer.stop();
            try {
                webSocketHandler.stop();
            } catch (InterruptedException e) {
                System.err.println("Error stopping WebSocket server: " + e.getMessage());
            }
        }));
    }
}
