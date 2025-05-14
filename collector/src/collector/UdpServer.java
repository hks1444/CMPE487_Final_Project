package collector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;

public class UdpServer implements Runnable {
    private final int port;
    private final MetricStore metricStore;
    private final WebSocketHandler webSocketHandler;
    private volatile boolean running;
    
    public UdpServer(int port, MetricStore metricStore, WebSocketHandler webSocketHandler) {
        this.port = port;
        this.metricStore = metricStore;
        this.webSocketHandler = webSocketHandler;
        this.running = true;
    }
    
    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP server listening on port " + port);
            byte[] buffer = new byte[4096];
            
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                handleMessage(message);
            }
        } catch (Exception e) {
            System.err.println("UDP server error: " + e.getMessage());
        }
    }
    
    private void handleMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String deviceId = json.getString("dev");
            long timestamp = json.getLong("ts") * 1000; // Convert to milliseconds
            
            if (json.has("m")) {
                JSONArray metrics = json.getJSONArray("m");
                for (int i = 0; i < metrics.length(); i++) {
                    JSONArray metric = metrics.getJSONArray(i);
                    String metricName = metric.getString(0);
                    double value = metric.getDouble(1);
                    metricStore.addMetric(deviceId, metricName, value, timestamp);
                }
            }
            
            webSocketHandler.broadcast(json);
            
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }
    
    public void stop() {
        running = false;
    }
}
