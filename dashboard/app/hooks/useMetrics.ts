import { useState, useEffect } from 'react';

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

const WEBSOCKET_URL = 'ws://localhost:8080/ws';

export function useMetrics() {
  const [metrics, setMetrics] = useState<MetricHistory>({});
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const ws = new WebSocket(WEBSOCKET_URL);
    
    ws.onopen = () => {
      console.log('Connected to WebSocket');
      setError(null);
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const deviceId = data.dev;
        const timestamp = data.ts * 1000; // Convert to milliseconds

        if (!deviceId || !timestamp) {
          console.warn('Invalid message format:', data);
          return;
        }

        setMetrics(prevMetrics => {
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

              // Keep 5 minutes of data at 10Hz = 3000 points
              newMetrics[deviceId][metricName] = [
                ...newMetrics[deviceId][metricName].slice(-2999),
                { metric: metricName, timestamp, value }
              ];
              
              // Remove data points older than 5 minutes
              const fiveMinutesAgo = Date.now() - 5 * 60 * 1000;
              newMetrics[deviceId][metricName] = newMetrics[deviceId][metricName]
                .filter(point => point.timestamp >= fiveMinutesAgo);
            });
          }

          // Handle alert format
          if (data.alert) {
            data.alert.forEach((alert: { metric: string, value: number }) => {
              if (!newMetrics[deviceId][alert.metric]) {
                newMetrics[deviceId][alert.metric] = [];
              }

              newMetrics[deviceId][alert.metric] = [
                ...newMetrics[deviceId][alert.metric].slice(-299),
                { metric: alert.metric, timestamp, value: alert.value }
              ];
            });
          }

          return newMetrics;
        });
      } catch (e) {
        console.error('Error parsing WebSocket message:', e);
      }
    };

    ws.onerror = (event) => {
      setError('WebSocket error occurred');
      console.error('WebSocket error:', event);
    };

    ws.onclose = () => {
      setError('WebSocket connection closed');
    };

    return () => {
      ws.close();
    };
  }, []);

  return { metrics, error };
}
