import type { Route } from "./+types/home";
import { useMetrics } from "../hooks/useMetrics";
import { MetricChart } from "../components/MetricChart";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "System Metrics Dashboard" },
    { name: "description", content: "Real-time system metrics monitoring" },
  ];
}

const ALERT_THRESHOLDS = {
  cpu_usage: 0.8,
  mem_free: 300_000_000,
  threads: 250,
};

export default function Home() {
  const { metrics, error, isLoading } = useMetrics();

  if (error && Object.keys(metrics).length === 0) {
    return (
      <div style={{ padding: "2rem", color: "red" }}>
        <h2>Connection Error</h2>
        <p>{error}</p>
        <p>
          Please check that the collector service is running and try refreshing
          the page.
        </p>
      </div>
    );
  }

  return (
    <main
      style={{ padding: "1.5rem", background: "white", minHeight: "100vh" }}
    >
      {/* Header with status */}
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "1.5rem",
          flexWrap: "wrap",
          gap: "1rem",
        }}
      >
        <h1 style={{ margin: 0, color: "#333" }}>System Metrics Dashboard</h1>

        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
          {isLoading && (
            <span style={{ color: "#666", fontSize: "0.9rem" }}>
              Loading historical data...
            </span>
          )}
          {error && (
            <span style={{ color: "#dc3545", fontSize: "0.9rem" }}>
              ⚠️ {error}
            </span>
          )}
          {!isLoading && !error && Object.keys(metrics).length > 0 && (
            <span style={{ color: "#28a745", fontSize: "0.9rem" }}>
              ✅ Connected
            </span>
          )}
        </div>
      </div>

      {/* Load test buttons */}
      <div
        style={{
          display: "flex",
          gap: "1rem",
          marginBottom: "1.5rem",
          flexWrap: "wrap",
        }}
      >
        <button
          onClick={() => fetch("http://localhost:8081/load/cpu?duration=10")}
          style={{
            padding: "0.5rem 1rem",
            backgroundColor: "#dc3545",
            color: "white",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          Load CPU
        </button>
        <button
          onClick={() => fetch("http://localhost:8081/load/memory?duration=20")}
          style={{
            padding: "0.5rem 1rem",
            backgroundColor: "#198754",
            color: "white",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          Load Memory
        </button>
        <button
          onClick={() => fetch("http://localhost:8081/load/thread?duration=10")}
          style={{
            padding: "0.5rem 1rem",
            backgroundColor: "#ffc107",
            color: "black",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          Load Threads
        </button>
        <button
          onClick={() => fetch("http://localhost:8081/load/free")}
          style={{
            padding: "0.5rem 1rem",
            backgroundColor: "#28a745",
            color: "white",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          Free Resources
        </button>
        <button
          onClick={() => {
            fetch("http://localhost:8082/api/metrics", {
              method: "DELETE",
            });

            window.location.reload();
          }}
          style={{
            padding: "0.5rem 1rem",
            backgroundColor: "#6f42c1",
            color: "white",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          Clear Data
        </button>
      </div>
      {Object.entries(metrics)
        .sort(([deviceA], [deviceB]) => deviceA.localeCompare(deviceB)) // Sort devices alphabetically
        .map(([device, deviceMetrics]) => (
          <div key={device} style={{ marginBottom: "2rem" }}>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(600px, 1fr))",
                gap: "1.5rem",
              }}
            >
              {Object.entries(deviceMetrics)
                .sort(([metricA], [metricB]) => metricA.localeCompare(metricB)) // Sort metrics alphabetically
                .map(([metricName, metricData]) => {
                  const isAlert = (() => {
                    const lastValue = metricData[metricData.length - 1]?.value;
                    if (lastValue === undefined) return false;

                    switch (metricName) {
                      case "cpu_usage":
                        return lastValue > ALERT_THRESHOLDS.cpu_usage;
                      case "mem_free":
                        return lastValue < ALERT_THRESHOLDS.mem_free;
                      case "threads":
                        return lastValue > ALERT_THRESHOLDS.threads;
                      default:
                        return false;
                    }
                  })();

                  return (
                    <MetricChart
                      key={metricName}
                      title={metricName}
                      data={metricData}
                      isAlert={isAlert}
                    />
                  );
                })}
            </div>
          </div>
        ))}

      {/* Loading/empty state */}
      {Object.keys(metrics).length === 0 && !isLoading && (
        <div
          style={{
            textAlign: "center",
            padding: "3rem",
            color: "#666",
            backgroundColor: "#f8f9fa",
            borderRadius: "8px",
            border: "2px dashed #dee2e6",
          }}
        >
          <h3>No metrics data available</h3>
          <p>Waiting for data from devices...</p>
          <p style={{ fontSize: "0.9rem", marginTop: "1rem" }}>
            Make sure your metric exporters are running and sending data to the
            collector.
          </p>
        </div>
      )}

      {isLoading && Object.keys(metrics).length === 0 && (
        <div
          style={{
            textAlign: "center",
            padding: "3rem",
            color: "#666",
          }}
        >
          <h3>Loading historical data...</h3>
          <p>Please wait while we fetch your metrics history.</p>
        </div>
      )}
    </main>
  );
}
