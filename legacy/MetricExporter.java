package legacy;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.io.*;
import java.util.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class MetricExporter {
    private static final String COLLECTOR_HOST = "172.20.10.9";
    private static final int COLLECTOR_PORT = 9090;
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/load", exchange -> {
            performCpuTask();
            sendMetrics();
            String response = "OK";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });
        server.setExecutor(null);
        server.start();
        System.out.println("Metric Exporter running on port " + PORT);
    }

    private static void performCpuTask() {
        double[][] matA = new double[200][200];
        double[][] matB = new double[200][200];
        double[][] result = new double[200][200];
        Random rand = new Random();

        for (int i = 0; i < 200; i++)
            for (int j = 0; j < 200; j++) {
                matA[i][j] = rand.nextDouble();
                matB[i][j] = rand.nextDouble();
            }

        for (int i = 0; i < 200; i++)
            for (int j = 0; j < 200; j++)
                for (int k = 0; k < 200; k++)
                    result[i][j] += matA[i][k] * matB[k][j];
    }

    private static void sendMetrics() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();

        String hostname = "vm-01";  // Replace or get from env
        long timestamp = System.currentTimeMillis();

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("cpu", osBean.getSystemCpuLoad() * 100);
        metrics.put("mem", (1 - osBean.getFreePhysicalMemorySize() /
                (double) osBean.getTotalPhysicalMemorySize()) * 100);
        metrics.put("disk", new File("/").getFreeSpace() /
                (double) new File("/").getTotalSpace() * 100);

        try (Socket socket = new Socket(COLLECTOR_HOST, COLLECTOR_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                String metric = String.format("%s,%d,%s,%.2f",
                        hostname, timestamp, entry.getKey(), entry.getValue());
                out.println(metric);
            }
        } catch (IOException e) {
            System.err.println("Error sending metrics: " + e.getMessage());
        }
    }
}
