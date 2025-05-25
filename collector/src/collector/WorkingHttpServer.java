package collector;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class WorkingHttpServer {
    private final int port;
    private final MetricStore metricStore;
    private HttpServer server;
    
    public WorkingHttpServer(int port, MetricStore metricStore) {
        this.port = port;
        this.metricStore = metricStore;
    }
    
    public void start() throws IOException {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Create contexts - the path must start with "/"
            server.createContext("/", new RootHandler());
            server.createContext("/api/metrics", new MetricsHandler());
            server.createContext("/api/devices", new DevicesHandler()); 
            server.createContext("/api/device", new DeviceHandler());
            
            server.setExecutor(null);
            server.start();
            
            System.out.println("HTTP API Server started successfully on port " + port);
            System.out.println("Test endpoints:");
            System.out.println("  curl http://localhost:" + port + "/");
            System.out.println("  curl http://localhost:" + port + "/api/metrics");
            System.out.println("  curl http://localhost:" + port + "/api/devices");
            
        } catch (Exception e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP server stopped");
        }
    }
    
    // Root handler for basic connectivity test
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            String path = exchange.getRequestURI().getPath();
            System.out.println("Root handler: " + exchange.getRequestMethod() + " " + path);
            
            if (path.equals("/")) {
                JSONObject response = new JSONObject();
                response.put("status", "running");
                response.put("message", "Metric Collector API");
                response.put("endpoints", new String[]{
                    "/api/metrics", "/api/devices", "/api/device/{deviceId}"
                });
                sendJsonResponse(exchange, response);
            } else {
                sendError(exchange, "Not found", 404);
            }
        }
    }
    
    // Metrics handler
    private class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            System.out.println("Metrics handler: " + exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }
            
            try {
                JSONObject response = new JSONObject();
                JSONObject devicesData = new JSONObject();
                
                Set<String> devices = metricStore.getDevices();
                System.out.println("Found " + devices.size() + " devices in store");
                
                for (String deviceId : devices) {
                    JSONObject deviceData = new JSONObject();
                    Set<String> metricNames = metricStore.getMetricNames(deviceId);
                    
                    for (String metricName : metricNames) {
                        List<MetricStore.MetricPoint> points = metricStore.getMetrics(deviceId, metricName);
                        JSONArray pointsArray = new JSONArray();
                        
                        for (MetricStore.MetricPoint point : points) {
                            JSONObject pointData = new JSONObject();
                            pointData.put("timestamp", point.timestamp);
                            pointData.put("value", point.value);
                            pointsArray.put(pointData);
                        }
                        
                        deviceData.put(metricName, pointsArray);
                    }
                    
                    devicesData.put(deviceId, deviceData);
                }
                
                response.put("devices", devicesData);
                response.put("timestamp", System.currentTimeMillis());
                response.put("deviceCount", devices.size());
                
                sendJsonResponse(exchange, response);
                
            } catch (Exception e) {
                System.err.println("Error in metrics handler: " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, "Internal server error", 500);
            }
        }
    }
    
    // Devices handler
    private class DevicesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            System.out.println("Devices handler: " + exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }
            
            try {
                JSONObject response = new JSONObject();
                JSONArray devicesArray = new JSONArray();
                
                Set<String> devices = metricStore.getDevices();
                for (String deviceId : devices) {
                    JSONObject deviceInfo = new JSONObject();
                    deviceInfo.put("deviceId", deviceId);
                    deviceInfo.put("metrics", new JSONArray(metricStore.getMetricNames(deviceId)));
                    devicesArray.put(deviceInfo);
                }
                
                response.put("devices", devicesArray);
                response.put("count", devices.size());
                
                sendJsonResponse(exchange, response);
                
            } catch (Exception e) {
                System.err.println("Error in devices handler: " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, "Internal server error", 500);
            }
        }
    }
    
    // Device-specific handler
    private class DeviceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            String path = exchange.getRequestURI().getPath();
            System.out.println("Device handler: " + exchange.getRequestMethod() + " " + path);
            
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }
            
            // Extract device ID from path like /api/device/deviceId
            if (!path.startsWith("/api/device/")) {
                sendError(exchange, "Invalid device path", 400);
                return;
            }
            
            String deviceId = path.substring("/api/device/".length());
            if (deviceId.isEmpty()) {
                sendError(exchange, "Device ID required", 400);
                return;
            }
            
            try {
                Set<String> devices = metricStore.getDevices();
                if (!devices.contains(deviceId)) {
                    sendError(exchange, "Device not found", 404);
                    return;
                }
                
                JSONObject response = new JSONObject();
                JSONObject deviceData = new JSONObject();
                Set<String> metricNames = metricStore.getMetricNames(deviceId);
                
                for (String metricName : metricNames) {
                    List<MetricStore.MetricPoint> points = metricStore.getMetrics(deviceId, metricName);
                    JSONArray pointsArray = new JSONArray();
                    
                    for (MetricStore.MetricPoint point : points) {
                        JSONObject pointData = new JSONObject();
                        pointData.put("timestamp", point.timestamp);
                        pointData.put("value", point.value);
                        pointsArray.put(pointData);
                    }
                    
                    deviceData.put(metricName, pointsArray);
                }
                
                response.put("deviceId", deviceId);
                response.put("metrics", deviceData);
                response.put("timestamp", System.currentTimeMillis());
                
                sendJsonResponse(exchange, response);
                
            } catch (Exception e) {
                System.err.println("Error in device handler: " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, "Internal server error", 500);
            }
        }
    }
    
    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
    }
    
    private void sendJsonResponse(HttpExchange exchange, JSONObject response) throws IOException {
        String responseStr = response.toString();
        byte[] bytes = responseStr.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
    
    private void sendError(HttpExchange exchange, String message, int code) throws IOException {
        JSONObject error = new JSONObject();
        error.put("error", message);
        error.put("code", code);
        String responseStr = error.toString();
        byte[] bytes = responseStr.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}