package exporter;

import java.net.*;
import java.util.*;
import org.json.*;

public class UdpSender {
    private final DatagramSocket socket;
    private final InetAddress address;
    private final int port;
    
    public UdpSender(String host, int port) throws Exception {
        this.socket = new DatagramSocket();
        this.address = InetAddress.getByName(host);
        this.port = port;
    }
    
    public void sendMetrics(Map<String, Object> metrics) throws Exception {
        JSONObject json = new JSONObject(metrics);
        byte[] buffer = json.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }
    
    public void sendAlert(String deviceId, List<Map<String, Object>> alerts) throws Exception {
        JSONObject alertPacket = new JSONObject();
        alertPacket.put("dev", deviceId);
        alertPacket.put("ts", System.currentTimeMillis() / 1000);
        alertPacket.put("alert", new JSONArray(alerts));
        
        byte[] buffer = alertPacket.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
    }
    
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
