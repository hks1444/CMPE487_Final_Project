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
            
            // Create contexts
            server.createContext("/", new RootHandler());
            server.createContext("/api/metrics", new MetricsHandler());
            server.createContext("/api/devices", new DevicesHandler()); 
            server.createContext("/api/device", new DeviceHandler());
            
            server.setExecutor(null);
            server.start();
            
            System.out.println("HTTP API Server started successfully on port " + port);
            System.out.println("Available endpoints:");
            System.out.println("  GET    http://localhost:" + port + "/");
            System.out.println("  GET    http://localhost:" + port + "/api/metrics");
            System.out.println("  GET    http://localhost:" + port + "/api/devices");
            System.out.println("  GET    http://localhost:" + port + "/api/device/{deviceId}");
            System.out.println("  DELETE http://localhost:" + port + "/api/metrics");
            System.out.println("  DELETE http://localhost:" + port + "/api/device/{deviceId}");
            
        } catch (Exception e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP API server stopped");
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
                    "GET /api/metrics", "GET /api/devices", "GET /api/device/{deviceId}",
                    "DELETE /api/metrics", "DELETE /api/device/{deviceId}"
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
            
            String method = exchange.getRequestMethod();
            System.out.println("Metrics handler: " + method + " " + exchange.getRequestURI().getPath());
            
            if ("GET".equals(method)) {
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
            } else if ("DELETE".equals(method)) {
                try {
                    // Clear all metrics for all devices
                    clearAllMetrics();
                    
                    JSONObject response = new JSONObject();
                    response.put("message", "All metrics history cleared");
                    response.put("timestamp", System.currentTimeMillis());
                    
                    sendJsonResponse(exchange, response);
                    System.out.println("All metrics history cleared via API");
                    
                } catch (Exception e) {
                    System.err.println("Error clearing metrics: " + e.getMessage());
                    e.printStackTrace();
                    sendError(exchange, "Internal server error", 500);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
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
            String method = exchange.getRequestMethod();
            System.out.println("Device handler: " + method + " " + path);
            
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
            
            if ("GET".equals(method)) {
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
            } else if ("DELETE".equals(method)) {
                try {
                    boolean cleared = clearDeviceMetrics(deviceId);
                    
                    if (cleared) {
                        JSONObject response = new JSONObject();
                        response.put("message", "Metrics cleared for device: " + deviceId);
                        response.put("deviceId", deviceId);
                        response.put("timestamp", System.currentTimeMillis());
                        
                        sendJsonResponse(exchange, response);
                        System.out.println("Metrics cleared for device: " + deviceId);
                    } else {
                        sendError(exchange, "Device not found: " + deviceId, 404);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error clearing device metrics: " + e.getMessage());
                    e.printStackTrace();
                    sendError(exchange, "Internal server error", 500);
                }
            } else {
                sendError(exchange, "Method not allowed", 405);
            }
        }
    }
    
    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
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
    
    private void clearAllMetrics() {
        Set<String> devices = metricStore.getDevices();
        for (String deviceId : devices) {
            metricStore.clearMetrics(deviceId);
        }
        System.out.println("Cleared metrics for " + devices.size() + " devices");
    }
    
    private boolean clearDeviceMetrics(String deviceId) {
        Set<String> devices = metricStore.getDevices();
        if (devices.contains(deviceId)) {
            metricStore.clearMetrics(deviceId);
            System.out.println("Cleared metrics for device: " + deviceId);
            return true;
        }
        return false;
    }
}