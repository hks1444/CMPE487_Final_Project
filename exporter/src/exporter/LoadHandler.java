package exporter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

public class LoadHandler implements HttpHandler {
    private final ExecutorService executorService;
    
    public LoadHandler() {
        this.executorService = Executors.newCachedThreadPool();
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            int duration = parseDuration(query);
            
            String response = "Load test started";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            
            // Execute load test in background
            if (path.endsWith("/cpu")) {
                executorService.submit(() -> generateCpuLoad(duration));
            } else if (path.endsWith("/memory")) {
                executorService.submit(() -> generateMemoryLoad(duration));
            } else if (path.endsWith("/thread")) {
                executorService.submit(() -> generateThreadLoad(duration));
            }
            
        } catch (Exception e) {
            String response = "Error: " + e.getMessage();
            exchange.sendResponseHeaders(400, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    private int parseDuration(String query) {
        if (query == null) return 30; // default duration
        String[] params = query.split("=");
        return params.length == 2 ? Integer.parseInt(params[1]) : 30;
    }
    
    private void generateCpuLoad(int durationSeconds) {
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
        while (System.currentTimeMillis() < endTime) {
            // Matrix multiplication to generate CPU load
            double[][] m1 = new double[100][100];
            double[][] m2 = new double[100][100];
            double[][] result = new double[100][100];
            
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    for (int k = 0; k < 100; k++) {
                        result[i][j] += m1[i][k] * m2[k][j];
                    }
                }
            }
        }
    }
    
    private void generateMemoryLoad(int durationSeconds) {
        List<byte[]> memoryBlocks = new ArrayList<>();
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
        
        try {
            while (System.currentTimeMillis() < endTime) {
                memoryBlocks.add(new byte[1024 * 1024]); // Allocate 1MB
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            memoryBlocks.clear(); // Release memory
        }
    }
    
    private void generateThreadLoad(int durationSeconds) {
        List<Thread> threads = new ArrayList<>();
        try {
            for (int i = 0; i < 100; i++) {
                Thread t = new Thread(() -> {
                    try {
                        Thread.sleep(durationSeconds * 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                t.start();
                threads.add(t);
            }
            
            // Wait for all threads to complete
            for (Thread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
