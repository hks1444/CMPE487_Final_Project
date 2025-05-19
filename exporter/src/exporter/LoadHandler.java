package exporter;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

public class LoadHandler implements HttpHandler {
    private ExecutorService executorService;
    private List<byte[]> memoryBlocks; // Added as class field
    private List<Thread> threads;      // Added as class field
    private volatile boolean stopCpuLoad = false;
    
    public LoadHandler() {
        this.executorService = Executors.newCachedThreadPool();
        this.memoryBlocks = new ArrayList<>();
        this.threads = new ArrayList<>();
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
            } else if (path.endsWith("/free")) {
                executorService.submit(() -> freeResources());
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
    private void freeResources() {
        // Signal CPU load tasks to stop
        stopCpuLoad = true;
        
        // Clear CPU load - force shutdown of executor service
        executorService.shutdownNow();
        try {
            // Wait for termination with timeout
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("Executor service did not terminate in the specified time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executorService = Executors.newCachedThreadPool(); // Restart executor service
        
        // Clear memory load
        memoryBlocks.clear();
        System.gc(); // Request garbage collection to free memory
        
        // Clear thread load
        for (Thread thread : threads) {
            thread.interrupt(); // Interrupt thread
            try {
                thread.join(1000); // Wait for thread to finish with timeout
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        threads.clear();
        
        System.out.println("Resources freed successfully");
        stopCpuLoad = false; // Reset the flag for future CPU load tests
    }
    
    private void generateCpuLoad(int durationSeconds) {
    long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
    while (System.currentTimeMillis() < endTime && !stopCpuLoad) {
        // Matrix multiplication to generate CPU load
        double[][] m1 = new double[100][100];
        double[][] m2 = new double[100][100];
        double[][] result = new double[100][100];
        
        for (int i = 0; i < 100; i++) {
            if (stopCpuLoad) break; // Check stop flag in the outer loops too
            for (int j = 0; j < 100; j++) {
                for (int k = 0; k < 100; k++) {
                    result[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }
        
        // Add a small sleep to allow interruption
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        }
    }
}
    
    private void generateMemoryLoad(int durationSeconds) {
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);
        
        try {
            while (System.currentTimeMillis() < endTime) {
                memoryBlocks.add(new byte[1024 * 1024]); // Allocate 1MB
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void generateThreadLoad(int durationSeconds) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}