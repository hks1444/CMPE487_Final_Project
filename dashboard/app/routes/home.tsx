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
  mem_free: 500_000_000,
  threads: 200,
};

export default function Home() {
  const { metrics, error } = useMetrics();

  if (error) {
    return (
      <div style={{ padding: "2rem", color: "red" }}>
        <h2>Error</h2>
        <p>{error}</p>
      </div>
    );
  }

  return (
    <main
      style={{ padding: "1.5rem", background: "white", minHeight: "100vh" }}
    >
      <div
        style={{
          display: "flex",
          gap: "1rem",
          marginBottom: "1.5rem",
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
          onClick={() => fetch("http://localhost:8081/load/memory?duration=10")}
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
      </div>
      {Object.entries(metrics).map(([device, deviceMetrics]) => (
        <div key={device} style={{ marginBottom: "2rem" }}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(600px, 1fr))",
              gap: "1.5rem",
              marginTop: "1rem",
            }}
          >
            {Object.entries(deviceMetrics).map(([metricName, metricData]) => {
              const isAlert = (() => {
                const lastValue = metricData[metricData.length - 1]?.value;
                if (!lastValue) return false;

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

      {Object.keys(metrics).length === 0 && <p>Waiting for metrics data...</p>}
    </main>
  );
}
