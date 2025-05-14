package collector;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public class WebSocketHandler extends WebSocketServer {
    private final Set<WebSocket> connections;
    
    public WebSocketHandler(int port) {
        super(new InetSocketAddress(port));
        this.connections = new HashSet<>();
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connections.add(conn);
        System.out.println("New WebSocket connection from " + conn.getRemoteSocketAddress());
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("WebSocket connection closed: " + reason);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        // We don't expect any messages from clients
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        if (conn != null) {
            connections.remove(conn);
        }
    }
    
    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port " + getPort());
    }
    
    public void broadcast(JSONObject data) {
        String message = data.toString();
        for (WebSocket conn : connections) {
            conn.send(message);
        }
    }
}
