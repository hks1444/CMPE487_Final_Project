import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MetricCollector {
    private static final int PORT = 9090;
    private static final Map<String, List<String>> timeSeries = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Metric Collector listening on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleConnection(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleConnection(Socket socket) {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);
                String[] parts = line.split(",");
                if (parts.length != 4) continue;

                String key = parts[0] + "-" + parts[2];  // deviceID-metricName
                timeSeries.putIfAbsent(key, new ArrayList<>());
                timeSeries.get(key).add(parts[1] + ":" + parts[3]);  // timestamp:value
            }
        } catch (IOException e) {
            System.err.println("Error handling connection: " + e.getMessage());
        }
    }
}
