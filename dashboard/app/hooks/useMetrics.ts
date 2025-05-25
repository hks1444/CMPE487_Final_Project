import { useState, useEffect } from "react";

export interface Metric {
  metric: string;
  timestamp: number;
  value: number;
}

export interface MetricHistory {
  [key: string]: {
    [key: string]: Metric[];
  };
}

const WEBSOCKET_URL = "ws://localhost:8080";
const HTTP_API_URL = "http://localhost:8082"; // Updated to use the exposed port

export function useMetrics() {
  const [metrics, setMetrics] = useState<MetricHistory>({});
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Load historical data from HTTP API
  const loadHistoricalData = async () => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await fetch(`${HTTP_API_URL}/api/metrics`);

      if (!response.ok) {
        if (response.status === 404) {
          console.warn(
            "HTTP API not available - will rely on WebSocket data only"
          );
          return; // Don't set error for 404, just skip historical data
        }
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data = await response.json();

      if (data.devices) {
        const historicalMetrics: MetricHistory = {};

        Object.entries(data.devices).forEach(
          ([deviceId, deviceData]: [string, any]) => {
            historicalMetrics[deviceId] = {};

            Object.entries(deviceData).forEach(
              ([metricName, points]: [string, any]) => {
                if (Array.isArray(points)) {
                  historicalMetrics[deviceId][metricName] = points.map(
                    (point: any) => ({
                      metric: metricName,
                      timestamp: point.timestamp,
                      value: point.value,
                    })
                  );
                }
              }
            );
          }
        );

        setMetrics(historicalMetrics);
        console.log(
          "Loaded historical data for",
          Object.keys(historicalMetrics).length,
          "devices"
        );
      }
    } catch (err) {
      console.error("Failed to load historical data:", err);
      if (err instanceof Error && !err.message.includes("Failed to fetch")) {
        setError(`HTTP API unavailable - using real-time data only`);
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    // Load historical data on component mount (page load/refresh)
    loadHistoricalData();

    // Then connect to WebSocket for real-time updates
    const ws = new WebSocket(WEBSOCKET_URL);

    ws.onopen = () => {
      console.log("Connected to WebSocket");
      setError(null);
      setIsLoading(false); // Stop loading when WebSocket connects
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const deviceId = data.dev;
        const timestamp = data.ts * 1000; // Convert to milliseconds

        if (!deviceId || !timestamp) {
          console.warn("Invalid message format:", data);
          return;
        }

        setMetrics((prevMetrics) => {
          const newMetrics = { ...prevMetrics };

          if (!newMetrics[deviceId]) {
            newMetrics[deviceId] = {};
          }

          // Handle metric array format
          if (data.m) {
            data.m.forEach(([metricName, value]: [string, number]) => {
              if (!newMetrics[deviceId][metricName]) {
                newMetrics[deviceId][metricName] = [];
              }

              // Add new data point
              newMetrics[deviceId][metricName] = [
                ...newMetrics[deviceId][metricName],
                { metric: metricName, timestamp, value },
              ];

              // Remove data points older than 5 minutes and limit to 3000 points
              const fiveMinutesAgo = Date.now() - 5 * 60 * 1000;
              newMetrics[deviceId][metricName] = newMetrics[deviceId][
                metricName
              ]
                .filter((point) => point.timestamp >= fiveMinutesAgo)
                .slice(-3000);
            });
          }

          // Handle alert format
          if (data.alert) {
            data.alert.forEach((alert: { metric: string; value: number }) => {
              if (!newMetrics[deviceId][alert.metric]) {
                newMetrics[deviceId][alert.metric] = [];
              }

              newMetrics[deviceId][alert.metric] = [
                ...newMetrics[deviceId][alert.metric],
                { metric: alert.metric, timestamp, value: alert.value },
              ].slice(-300); // Keep last 300 alert points
            });
          }

          return newMetrics;
        });
      } catch (e) {
        console.error("Error parsing WebSocket message:", e);
      }
    };

    ws.onerror = (event) => {
      setError("WebSocket error occurred");
      console.error("WebSocket error:", event);
    };

    ws.onclose = () => {
      console.log("WebSocket connection closed");
      // Only set error if we haven't loaded historical data
      if (Object.keys(metrics).length === 0) {
        setError("WebSocket connection closed");
      }
    };

    return () => {
      ws.close();
    };
  }, []); // Empty dependency array ensures this only runs on mount

  return {
    metrics,
    error,
    isLoading,
  };
}
