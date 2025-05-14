import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import type { Metric } from '../hooks/useMetrics';

interface MetricChartProps {
  title: string;
  data: Metric[];
  isAlert?: boolean;
}

const formatMetricName = (name: string): string => {
  if (!name) {
    console.warn('Received undefined or empty metric name');
    return 'Unknown';
  }

  const nameMap: { [key: string]: string } = {
    cpu_usage: 'CPU Usage',
    mem_free: 'Free Memory',
    mem_total: 'Total Memory',
    threads: 'Thread Count',
    process_cpu: 'Process CPU',
    swap_total: 'Total Swap',
    swap_free: 'Free Swap'
  };
  const formattedName = nameMap[name];
  if (!formattedName) {
    console.warn('Unknown metric name:', name);
  }
  return formattedName || name;
};

export function MetricChart({ title, data, isAlert }: MetricChartProps) {
  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp);
    return `${date.getHours()}:${date.getMinutes()}:${date.getSeconds()}`;
  };

  const formatValue = (value: number) => {
    if (title.includes('cpu')) {
      return `${(value * 100).toFixed(1)}%`;
    }
    if (title.includes('mem')) {
      return `${(value / 1024 / 1024).toFixed(1)}MB`;
    }
    return value.toString();
  };

  return (
    <div style={{ 
      width: '100%', 
      height: 300, 
      padding: '0.75rem',
      backgroundColor: '#f8f9fa',
      borderRadius: '8px',
      boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
      display: 'flex',
      flexDirection: 'column'
    }}>
      <div style={{ marginBottom: '0.5rem' }}>
        <h3 style={{ 
          color: isAlert ? '#dc3545' : '#333',
          marginBottom: '0.5rem',
          fontSize: '1.1rem',
          fontWeight: 600
        }}>
          {formatMetricName(title)}
        </h3>
        {data.length > 0 && (
          <div style={{ fontSize: '0.9rem', color: '#666', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            Current: {formatValue(data[data.length - 1].value)}
            {isAlert && <span style={{ color: 'red', marginLeft: '0.5rem' }}>⚠️ Alert Threshold Exceeded</span>}
          </div>
        )}
      </div>
      <div style={{ flex: 1, minHeight: 0 }}>
        <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e9ecef" />
          <XAxis 
            dataKey="timestamp"
            tickFormatter={formatTimestamp}
            tick={false}
            axisLine={false}
          />
          <YAxis 
            tickFormatter={formatValue}
            axisLine={false}
            tickLine={false}
            width={80}
            tick={{ fontSize: 12 }}
          />
          <Tooltip 
            labelFormatter={formatTimestamp}
            formatter={(value: number) => [formatValue(value), formatMetricName(title)]}
            contentStyle={{
              backgroundColor: 'rgba(255, 255, 255, 0.95)',
              border: '1px solid #e9ecef',
              borderRadius: '4px',
              boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
            }}
          />
          <Line
            type="monotone"
            dataKey="value"
            stroke={isAlert ? '#dc3545' : '#0d6efd'}
            strokeWidth={2}
            dot={false}
          />
        </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
