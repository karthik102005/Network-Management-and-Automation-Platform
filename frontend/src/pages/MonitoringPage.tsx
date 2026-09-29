import React, { useEffect, useState, useCallback } from 'react';
import { apiService } from '../services/api';
import {
  NetworkDevice,
  DeviceReachabilityResult,
  DeviceMetrics,
  DeviceMetricsHistoryPoint,
  DeviceHealthState,
} from '../types/device';
import { StatusBadge } from '../components/StatusBadge';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { EmptyState } from '../components/EmptyState';
import {
  Activity,
  Server,
  RefreshCw,
  Search,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  Zap,
  Info,
  Radio,
  Cpu,
  HardDrive,
  TrendingUp,
  BarChart2,
  ShieldCheck,
  AlertCircle,
} from 'lucide-react';

export const MonitoringPage: React.FC = () => {
  // Navigation tabs
  const [activeTab, setActiveTab] = useState<'reachability' | 'telemetry'>('reachability');

  // Device inventory
  const [devices, setDevices] = useState<NetworkDevice[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Reachability Probes state
  const [probingDeviceIds, setProbingDeviceIds] = useState<Record<number, boolean>>({});
  const [probeResults, setProbeResults] = useState<Record<number, DeviceReachabilityResult>>({});

  // Simulated Telemetry state
  const [selectedDeviceId, setSelectedDeviceId] = useState<number | null>(null);
  const [currentMetrics, setCurrentMetrics] = useState<DeviceMetrics | null>(null);
  const [metricsHistory, setMetricsHistory] = useState<DeviceMetricsHistoryPoint[]>([]);
  const [historyPoints, setHistoryPoints] = useState<number>(20);
  const [loadingTelemetry, setLoadingTelemetry] = useState<boolean>(false);
  const [telemetryError, setTelemetryError] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState<boolean>(false);

  // Load device inventory
  const fetchDevices = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiService.getDevices();
      setDevices(data);
      if (data.length > 0 && selectedDeviceId === null) {
        setSelectedDeviceId(data[0].id);
      }
    } catch (err: unknown) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to load network devices from backend.';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  }, [selectedDeviceId]);

  useEffect(() => {
    fetchDevices();
  }, [fetchDevices]);

  // Load simulated telemetry for selected device
  const fetchTelemetry = useCallback(async () => {
    if (!selectedDeviceId) return;
    setLoadingTelemetry(true);
    setTelemetryError(null);
    try {
      const [metricsData, historyData] = await Promise.all([
        apiService.getDeviceMetrics(selectedDeviceId),
        apiService.getDeviceMetricsHistory(selectedDeviceId, historyPoints),
      ]);
      setCurrentMetrics(metricsData);
      setMetricsHistory(historyData);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch simulated telemetry.';
      setTelemetryError(msg);
    } finally {
      setLoadingTelemetry(false);
    }
  }, [selectedDeviceId, historyPoints]);

  useEffect(() => {
    if (activeTab === 'telemetry' && selectedDeviceId) {
      fetchTelemetry();
    }
  }, [activeTab, selectedDeviceId, historyPoints, fetchTelemetry]);

  // Auto-refresh interval
  useEffect(() => {
    if (!autoRefresh || activeTab !== 'telemetry' || !selectedDeviceId) return;
    const timer = setInterval(() => {
      fetchTelemetry();
    }, 5000);
    return () => clearInterval(timer);
  }, [autoRefresh, activeTab, selectedDeviceId, fetchTelemetry]);

  // Handle manual reachability probe
  const handleProbeDevice = async (device: NetworkDevice) => {
    const deviceId = device.id;
    setProbingDeviceIds((prev) => ({ ...prev, [deviceId]: true }));
    try {
      const result = await apiService.checkDeviceReachability(deviceId, 2000);
      setProbeResults((prev) => ({ ...prev, [deviceId]: result }));
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error occurred while executing probe.';
      setProbeResults((prev) => ({
        ...prev,
        [deviceId]: {
          deviceId,
          hostname: device.hostname,
          managementIp: device.managementIp,
          reachable: false,
          reachabilityStatus: 'UNREACHABLE',
          probeMethod: 'NONE',
          checkedAt: new Date().toISOString(),
          details: msg,
        },
      }));
    } finally {
      setProbingDeviceIds((prev) => ({ ...prev, [deviceId]: false }));
    }
  };

  const filteredDevices = devices.filter((d) => {
    const query = searchQuery.toLowerCase();
    return (
      d.hostname.toLowerCase().includes(query) ||
      d.managementIp.toLowerCase().includes(query) ||
      d.vendor.toLowerCase().includes(query) ||
      d.deviceType.toLowerCase().includes(query)
    );
  });

  const selectedDevice = devices.find((d) => d.id === selectedDeviceId);

  // Statistics for Reachability
  const testedCount = Object.keys(probeResults).length;
  const reachableCount = Object.values(probeResults).filter((r) => r.reachable).length;
  const unreachableCount = Object.values(probeResults).filter((r) => !r.reachable).length;

  // Helper for health state badge
  const renderHealthStateBadge = (state?: DeviceHealthState) => {
    switch (state) {
      case 'HEALTHY':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 10px',
              borderRadius: '16px',
              fontSize: '12px',
              fontWeight: 600,
              background: 'var(--status-up-bg)',
              color: 'var(--status-up)',
              border: '1px solid var(--status-up-border)',
            }}
          >
            <ShieldCheck size={14} />
            HEALTHY
          </span>
        );
      case 'WARNING':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 10px',
              borderRadius: '16px',
              fontSize: '12px',
              fontWeight: 600,
              background: 'var(--status-unknown-bg)',
              color: 'var(--status-unknown)',
              border: '1px solid var(--status-unknown-border)',
            }}
          >
            <AlertTriangle size={14} />
            WARNING
          </span>
        );
      case 'DEGRADED':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 10px',
              borderRadius: '16px',
              fontSize: '12px',
              fontWeight: 600,
              background: 'var(--status-down-bg)',
              color: 'var(--status-down)',
              border: '1px solid var(--status-down-border)',
            }}
          >
            <AlertCircle size={14} />
            DEGRADED
          </span>
        );
      default:
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              padding: '4px 10px',
              borderRadius: '16px',
              fontSize: '12px',
              fontWeight: 600,
              background: 'rgba(100, 116, 139, 0.15)',
              color: 'var(--text-muted)',
            }}
          >
            UNKNOWN
          </span>
        );
    }
  };

  // Helper for progress bar color
  const getProgressColor = (percent: number) => {
    if (percent >= 85) return 'var(--status-down)';
    if (percent >= 70) return 'var(--status-unknown)';
    return 'var(--accent-blue)';
  };

  // SVG Chart helpers
  const svgWidth = 680;
  const svgHeight = 180;
  const padLeft = 40;
  const padRight = 20;
  const padTop = 15;
  const padBottom = 25;
  const innerWidth = svgWidth - padLeft - padRight;
  const innerHeight = svgHeight - padTop - padBottom;

  const getChartX = (idx: number, total: number) => {
    if (total <= 1) return padLeft + innerWidth / 2;
    return padLeft + (idx / (total - 1)) * innerWidth;
  };

  const getPercentY = (val: number) => {
    const clamped = Math.max(0, Math.min(100, val));
    return padTop + innerHeight - (clamped / 100) * innerHeight;
  };

  // Build SVG path for history points
  const cpuLine = metricsHistory
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${getChartX(i, metricsHistory.length).toFixed(1)} ${getPercentY(p.cpuPercent).toFixed(1)}`)
    .join(' ');

  const cpuArea = metricsHistory.length > 0
    ? `${cpuLine} L ${getChartX(metricsHistory.length - 1, metricsHistory.length).toFixed(1)} ${padTop + innerHeight} L ${getChartX(0, metricsHistory.length).toFixed(1)} ${padTop + innerHeight} Z`
    : '';

  const memLine = metricsHistory
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${getChartX(i, metricsHistory.length).toFixed(1)} ${getPercentY(p.memoryPercent).toFixed(1)}`)
    .join(' ');

  const memArea = metricsHistory.length > 0
    ? `${memLine} L ${getChartX(metricsHistory.length - 1, metricsHistory.length).toFixed(1)} ${padTop + innerHeight} L ${getChartX(0, metricsHistory.length).toFixed(1)} ${padTop + innerHeight} Z`
    : '';

  // Max throughput for bandwidth chart
  const maxThroughput = Math.max(
    50,
    ...(metricsHistory.length > 0
      ? metricsHistory.map((p) => Math.max(p.inboundMbps, p.outboundMbps) * 1.2)
      : [100])
  );

  const getThroughputY = (val: number) => {
    const clamped = Math.max(0, Math.min(maxThroughput, val));
    return padTop + innerHeight - (clamped / maxThroughput) * innerHeight;
  };

  const inLine = metricsHistory
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${getChartX(i, metricsHistory.length).toFixed(1)} ${getThroughputY(p.inboundMbps).toFixed(1)}`)
    .join(' ');

  const outLine = metricsHistory
    .map((p, i) => `${i === 0 ? 'M' : 'L'} ${getChartX(i, metricsHistory.length).toFixed(1)} ${getThroughputY(p.outboundMbps).toFixed(1)}`)
    .join(' ');

  return (
    <div style={{ paddingBottom: '40px' }}>
      {/* Page Header */}
      <div className="page-header" style={{ marginBottom: '16px' }}>
        <div>
          <h2 className="page-title">Monitoring & Telemetry</h2>
          <p className="page-description">
            Live reachability diagnostics, Layer-3/4 probes, and Phase 5 simulated performance telemetry
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => {
              if (activeTab === 'reachability') {
                fetchDevices();
              } else {
                fetchTelemetry();
              }
            }}
            className="btn btn-secondary"
            title="Refresh current view"
          >
            <RefreshCw size={14} className={loading || loadingTelemetry ? 'spin' : ''} />
            <span>{activeTab === 'reachability' ? 'Refresh Inventory' : 'Refresh Telemetry'}</span>
          </button>
        </div>
      </div>

      {/* Navigation Sub-Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '8px',
          borderBottom: '1px solid var(--border-color)',
          marginBottom: '20px',
        }}
      >
        <button
          onClick={() => setActiveTab('reachability')}
          style={{
            padding: '10px 18px',
            fontSize: '13px',
            fontWeight: 600,
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'reachability' ? '2px solid var(--accent-blue)' : '2px solid transparent',
            color: activeTab === 'reachability' ? 'var(--accent-blue)' : 'var(--text-secondary)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <Activity size={16} />
          Reachability Probes (ICMP / TCP)
        </button>

        <button
          onClick={() => setActiveTab('telemetry')}
          style={{
            padding: '10px 18px',
            fontSize: '13px',
            fontWeight: 600,
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'telemetry' ? '2px solid var(--accent-blue)' : '2px solid transparent',
            color: activeTab === 'telemetry' ? 'var(--accent-blue)' : 'var(--text-secondary)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <BarChart2 size={16} />
          Device Telemetry & Metrics (Simulated)
          <span
            style={{
              padding: '1px 6px',
              borderRadius: '8px',
              fontSize: '10px',
              fontWeight: 700,
              background: 'rgba(245, 158, 11, 0.2)',
              color: 'var(--status-unknown)',
              border: '1px solid rgba(245, 158, 11, 0.4)',
            }}
          >
            SIMULATED
          </span>
        </button>
      </div>

      {/* ========================================================================= */}
      {/* TAB 1: REACHABILITY PROBES */}
      {/* ========================================================================= */}
      {activeTab === 'reachability' && (
        <div>
          {error && (
            <div className="error-banner" role="alert" style={{ marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <AlertTriangle size={18} />
                <span>{error}</span>
              </div>
              <button onClick={fetchDevices} className="btn btn-secondary" style={{ padding: '4px 10px' }}>
                Retry
              </button>
            </div>
          )}

          {/* Information Banner */}
          <div
            style={{
              background: 'rgba(56, 189, 248, 0.08)',
              border: '1px solid rgba(56, 189, 248, 0.25)',
              borderRadius: '8px',
              padding: '14px 18px',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '12px',
              fontSize: '13px',
              lineHeight: '1.5',
              color: 'var(--text-secondary)',
            }}
          >
            <Info size={18} color="var(--accent-blue)" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong style={{ color: 'var(--text-primary)' }}>Layer-3 / Layer-4 Reachability Probe Engine: </strong>
              Executes bounded ICMP echo requests with automatic concurrent fallback to TCP management ports (SSH 22, HTTP 80, HTTPS 443, Telnet 23).
              Probes are executed on demand per device and do not mutate persistent inventory records in PostgreSQL.
            </div>
          </div>

          {/* Session Metrics Grid */}
          <div className="stats-grid" style={{ marginBottom: '24px' }}>
            <div className="stat-card">
              <div className="stat-header">
                <span className="stat-title">Registered Targets</span>
                <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.1)', color: 'var(--accent-blue)' }}>
                  <Server size={18} />
                </div>
              </div>
              <div className="stat-value">{devices.length}</div>
              <div className="stat-subtitle">Configured management endpoints</div>
            </div>

            <div className="stat-card">
              <div className="stat-header">
                <span className="stat-title">Probed This Session</span>
                <div className="stat-icon" style={{ background: 'rgba(99, 102, 241, 0.1)', color: 'var(--accent-indigo)' }}>
                  <Activity size={18} />
                </div>
              </div>
              <div className="stat-value">{testedCount}</div>
              <div className="stat-subtitle">On-demand verification passes</div>
            </div>

            <div className="stat-card">
              <div className="stat-header">
                <span className="stat-title">Confirmed Reachable</span>
                <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.1)', color: 'var(--status-up)' }}>
                  <CheckCircle2 size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: 'var(--status-up)' }}>
                {reachableCount}
              </div>
              <div className="stat-subtitle">ICMP echo or TCP connection established</div>
            </div>

            <div className="stat-card">
              <div className="stat-header">
                <span className="stat-title">Unreachable / Inconclusive</span>
                <div className="stat-icon" style={{ background: 'rgba(239, 68, 68, 0.1)', color: 'var(--status-down)' }}>
                  <XCircle size={18} />
                </div>
              </div>
              <div className="stat-value" style={{ color: unreachableCount > 0 ? 'var(--status-down)' : 'var(--text-muted)' }}>
                {unreachableCount}
              </div>
              <div className="stat-subtitle">Probes timed out or target invalid</div>
            </div>
          </div>

          {/* Main Device Probe Table Card */}
          <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
            <div
              style={{
                padding: '16px 20px',
                borderBottom: '1px solid var(--border-color)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '12px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Radio size={16} color="var(--accent-blue)" />
                <h3 style={{ fontSize: '15px', fontWeight: 600 }}>Device Management Endpoints</h3>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>({filteredDevices.length} items)</span>
              </div>

              <div style={{ position: 'relative', width: '280px' }}>
                <Search size={14} style={{ position: 'absolute', left: '10px', top: '10px', color: 'var(--text-muted)' }} />
                <input
                  type="text"
                  className="form-input"
                  placeholder="Filter by hostname, IP, vendor..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  style={{ paddingLeft: '32px', height: '34px', fontSize: '13px' }}
                />
              </div>
            </div>

            {loading ? (
              <div style={{ padding: '24px' }}>
                <LoadingSkeleton rows={4} />
              </div>
            ) : filteredDevices.length === 0 ? (
              <div style={{ padding: '40px 20px' }}>
                <EmptyState
                  title={devices.length === 0 ? 'No registered devices found' : 'No matching devices'}
                  description={
                    devices.length === 0
                      ? 'Add devices in the Devices Inventory to enable reachability monitoring.'
                      : 'Try adjusting your search filter.'
                  }
                />
              </div>
            ) : (
              <div className="table-responsive">
                <table className="table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr
                      style={{
                        background: 'var(--bg-secondary)',
                        borderBottom: '1px solid var(--border-color)',
                        textAlign: 'left',
                        fontSize: '12px',
                        color: 'var(--text-muted)',
                      }}
                    >
                      <th style={{ padding: '12px 16px' }}>Device Hostname</th>
                      <th style={{ padding: '12px 16px' }}>Management IP</th>
                      <th style={{ padding: '12px 16px' }}>Type & Vendor</th>
                      <th style={{ padding: '12px 16px' }}>Inventory Status</th>
                      <th style={{ padding: '12px 16px' }}>Live Probe Status</th>
                      <th style={{ padding: '12px 16px' }}>Latency</th>
                      <th style={{ padding: '12px 16px' }}>Probe Diagnostic Details</th>
                      <th style={{ padding: '12px 16px', textAlign: 'right' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredDevices.map((device) => {
                      const isProbing = !!probingDeviceIds[device.id];
                      const result = probeResults[device.id];

                      return (
                        <tr
                          key={device.id}
                          style={{
                            borderBottom: '1px solid var(--border-color)',
                            transition: 'background-color 0.15s ease',
                          }}
                        >
                          <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                              <Server size={14} color="var(--accent-blue)" />
                              <span>{device.hostname}</span>
                            </div>
                          </td>

                          <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', fontSize: '13px' }}>
                            {device.managementIp}
                          </td>

                          <td style={{ padding: '14px 16px', fontSize: '12px' }}>
                            <span style={{ color: 'var(--text-secondary)' }}>{device.deviceType}</span>
                            <span style={{ color: 'var(--text-muted)', margin: '0 4px' }}>•</span>
                            <span style={{ color: 'var(--text-muted)' }}>{device.vendor}</span>
                          </td>

                          <td style={{ padding: '14px 16px' }}>
                            <StatusBadge status={device.status} />
                          </td>

                          <td style={{ padding: '14px 16px' }}>
                            {isProbing ? (
                              <span
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '6px',
                                  padding: '3px 8px',
                                  borderRadius: '12px',
                                  fontSize: '11px',
                                  fontWeight: 600,
                                  background: 'rgba(56, 189, 248, 0.15)',
                                  color: 'var(--accent-blue)',
                                }}
                              >
                                <RefreshCw size={10} className="spin" />
                                Probing...
                              </span>
                            ) : result ? (
                              result.reachable ? (
                                <span
                                  style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '6px',
                                    padding: '3px 8px',
                                    borderRadius: '12px',
                                    fontSize: '11px',
                                    fontWeight: 600,
                                    background: 'var(--status-up-bg)',
                                    color: 'var(--status-up)',
                                    border: '1px solid var(--status-up-border)',
                                  }}
                                >
                                  <CheckCircle2 size={12} />
                                  REACHABLE
                                </span>
                              ) : (
                                <span
                                  style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '6px',
                                    padding: '3px 8px',
                                    borderRadius: '12px',
                                    fontSize: '11px',
                                    fontWeight: 600,
                                    background: 'var(--status-down-bg)',
                                    color: 'var(--status-down)',
                                    border: '1px solid var(--status-down-border)',
                                  }}
                                >
                                  <XCircle size={12} />
                                  {result.reachabilityStatus || 'UNREACHABLE'}
                                </span>
                              )
                            ) : (
                              <span
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '6px',
                                  padding: '3px 8px',
                                  borderRadius: '12px',
                                  fontSize: '11px',
                                  background: 'rgba(100, 116, 139, 0.15)',
                                  color: 'var(--text-muted)',
                                }}
                              >
                                <Clock size={11} />
                                Not Tested
                              </span>
                            )}
                          </td>

                          <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                            {result && result.responseTimeMs != null ? (
                              <span
                                style={{
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '4px',
                                  color: result.responseTimeMs < 50 ? 'var(--status-up)' : 'var(--status-unknown)',
                                }}
                              >
                                <Zap size={11} />
                                {result.responseTimeMs} ms
                              </span>
                            ) : (
                              <span style={{ color: 'var(--text-muted)' }}>—</span>
                            )}
                          </td>

                          <td style={{ padding: '14px 16px', fontSize: '12px', maxWidth: '300px' }}>
                            {result ? (
                              <div>
                                <div style={{ color: 'var(--text-secondary)', lineHeight: 1.4 }}>
                                  {result.details}
                                </div>
                                <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>
                                  Method: {result.probeMethod}
                                  {result.port ? ` (Port ${result.port})` : ''} • Checked:{' '}
                                  {new Date(result.checkedAt).toLocaleTimeString()}
                                </div>
                              </div>
                            ) : (
                              <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>
                                Click &quot;Check Reachability&quot; to probe target
                              </span>
                            )}
                          </td>

                          <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                            <button
                              onClick={() => handleProbeDevice(device)}
                              disabled={isProbing}
                              className="btn btn-secondary"
                              style={{
                                padding: '6px 12px',
                                fontSize: '12px',
                                cursor: isProbing ? 'not-allowed' : 'pointer',
                              }}
                              title={`Probe reachability of ${device.hostname} (${device.managementIp})`}
                            >
                              <Activity size={13} className={isProbing ? 'spin' : ''} />
                              <span>{isProbing ? 'Probing...' : 'Check Reachability'}</span>
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* TAB 2: DEVICE TELEMETRY & METRICS (SIMULATED) */}
      {/* ========================================================================= */}
      {activeTab === 'telemetry' && (
        <div>
          {/* Explicit Simulated Telemetry Disclaimer Notice */}
          <div
            style={{
              background: 'rgba(245, 158, 11, 0.08)',
              border: '1px solid rgba(245, 158, 11, 0.35)',
              borderRadius: '8px',
              padding: '14px 18px',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '12px',
              fontSize: '13px',
              lineHeight: '1.5',
              color: 'var(--text-secondary)',
            }}
          >
            <AlertTriangle size={20} color="var(--status-unknown)" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <div style={{ color: 'var(--status-unknown)', fontWeight: 700, marginBottom: '2px' }}>
                SIMULATED TELEMETRY ACTIVE — PHASE 5 DEMONSTRATION PROVIDER
              </div>
              <div>
                All CPU utilization, memory consumption, latency, and interface throughput measurements shown below are
                algorithmically calculated in real-time by the simulated telemetry provider.
                <strong style={{ color: 'var(--text-primary)' }}> No physical SNMP poller is configured</strong>, and no
                persistent database records or reachability alert states are mutated.
              </div>
            </div>
          </div>

          {/* Telemetry Control Bar */}
          <div
            className="card"
            style={{
              padding: '16px 20px',
              marginBottom: '20px',
              display: 'flex',
              flexWrap: 'wrap',
              gap: '16px',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Server size={16} color="var(--accent-blue)" />
                <label style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Target Device:
                </label>
              </div>

              <select
                className="form-input"
                style={{ width: '220px', height: '36px', fontSize: '13px' }}
                value={selectedDeviceId ?? ''}
                onChange={(e) => setSelectedDeviceId(Number(e.target.value))}
              >
                {devices.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.hostname} ({d.managementIp})
                  </option>
                ))}
              </select>

              {selectedDevice && (
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                  Vendor: {selectedDevice.vendor} • Type: {selectedDevice.deviceType}
                </span>
              )}
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
              {/* History points selector */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Samples:</span>
                {[10, 20, 30, 60].map((pts) => (
                  <button
                    key={pts}
                    onClick={() => setHistoryPoints(pts)}
                    className="btn btn-secondary"
                    style={{
                      padding: '4px 8px',
                      fontSize: '11px',
                      fontWeight: historyPoints === pts ? 700 : 400,
                      borderColor: historyPoints === pts ? 'var(--accent-blue)' : undefined,
                      color: historyPoints === pts ? 'var(--accent-blue)' : undefined,
                    }}
                  >
                    {pts} pts
                  </button>
                ))}
              </div>

              {/* Auto Refresh Toggle */}
              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                  fontSize: '12px',
                  color: 'var(--text-secondary)',
                  cursor: 'pointer',
                  userSelect: 'none',
                }}
              >
                <input
                  type="checkbox"
                  checked={autoRefresh}
                  onChange={(e) => setAutoRefresh(e.target.checked)}
                />
                Auto-poll (5s)
              </label>

              <button
                onClick={fetchTelemetry}
                disabled={loadingTelemetry}
                className="btn btn-primary"
                style={{ padding: '6px 12px', fontSize: '12px' }}
              >
                <RefreshCw size={13} className={loadingTelemetry ? 'spin' : ''} />
                <span>Poll Metrics</span>
              </button>
            </div>
          </div>

          {telemetryError && (
            <div className="error-banner" role="alert" style={{ marginBottom: '20px' }}>
              <AlertTriangle size={18} />
              <span>{telemetryError}</span>
            </div>
          )}

          {loadingTelemetry && !currentMetrics ? (
            <div className="card" style={{ padding: '24px' }}>
              <LoadingSkeleton rows={4} />
            </div>
          ) : !currentMetrics ? (
            <div className="card" style={{ padding: '40px 20px' }}>
              <EmptyState
                title="No telemetry available"
                description="Select a device above and click Poll Metrics to compute simulated telemetry."
              />
            </div>
          ) : (
            <div>
              {/* Telemetry KPI Cards */}
              <div className="stats-grid" style={{ marginBottom: '20px' }}>
                {/* Health State */}
                <div className="stat-card">
                  <div className="stat-header">
                    <span className="stat-title">Health State</span>
                    <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.1)', color: 'var(--accent-blue)' }}>
                      <Activity size={18} />
                    </div>
                  </div>
                  <div style={{ marginTop: '8px', marginBottom: '8px' }}>
                    {renderHealthStateBadge(currentMetrics.healthState)}
                  </div>
                  <div className="stat-subtitle">Calculated from composite telemetry</div>
                </div>

                {/* CPU Utilization */}
                <div className="stat-card">
                  <div className="stat-header">
                    <span className="stat-title">CPU Utilization</span>
                    <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.1)', color: 'var(--accent-blue)' }}>
                      <Cpu size={18} />
                    </div>
                  </div>
                  <div className="stat-value" style={{ color: getProgressColor(currentMetrics.cpuUtilizationPercent) }}>
                    {currentMetrics.cpuUtilizationPercent.toFixed(1)}%
                  </div>
                  <div
                    style={{
                      height: '6px',
                      background: 'rgba(255, 255, 255, 0.1)',
                      borderRadius: '3px',
                      overflow: 'hidden',
                      marginTop: '8px',
                    }}
                  >
                    <div
                      style={{
                        width: `${Math.min(100, Math.max(0, currentMetrics.cpuUtilizationPercent))}%`,
                        height: '100%',
                        background: getProgressColor(currentMetrics.cpuUtilizationPercent),
                        transition: 'width 0.4s ease',
                      }}
                    />
                  </div>
                </div>

                {/* Memory Utilization */}
                <div className="stat-card">
                  <div className="stat-header">
                    <span className="stat-title">Memory Utilization</span>
                    <div className="stat-icon" style={{ background: 'rgba(99, 102, 241, 0.1)', color: 'var(--accent-indigo)' }}>
                      <HardDrive size={18} />
                    </div>
                  </div>
                  <div className="stat-value" style={{ color: getProgressColor(currentMetrics.memoryUtilizationPercent) }}>
                    {currentMetrics.memoryUtilizationPercent.toFixed(1)}%
                  </div>
                  <div
                    style={{
                      height: '6px',
                      background: 'rgba(255, 255, 255, 0.1)',
                      borderRadius: '3px',
                      overflow: 'hidden',
                      marginTop: '8px',
                    }}
                  >
                    <div
                      style={{
                        width: `${Math.min(100, Math.max(0, currentMetrics.memoryUtilizationPercent))}%`,
                        height: '100%',
                        background: getProgressColor(currentMetrics.memoryUtilizationPercent),
                        transition: 'width 0.4s ease',
                      }}
                    />
                  </div>
                </div>

                {/* Latency & Loss */}
                <div className="stat-card">
                  <div className="stat-header">
                    <span className="stat-title">Simulated Latency</span>
                    <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.1)', color: 'var(--status-up)' }}>
                      <Zap size={18} />
                    </div>
                  </div>
                  <div className="stat-value" style={{ color: 'var(--text-primary)' }}>
                    {currentMetrics.latencyMs.toFixed(1)}{' '}
                    <span style={{ fontSize: '14px', color: 'var(--text-muted)' }}>ms</span>
                  </div>
                  <div className="stat-subtitle">
                    Loss: {currentMetrics.packetLossPercent.toFixed(1)}% • Source:{' '}
                    <span style={{ fontFamily: 'var(--font-mono)' }}>SIMULATED</span>
                  </div>
                </div>
              </div>

              {/* Historical SVG Charts */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(480px, 1fr))', gap: '20px', marginBottom: '20px' }}>
                {/* Chart 1: CPU & Memory History */}
                <div className="card" style={{ padding: '20px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <TrendingUp size={16} color="var(--accent-blue)" />
                      <h3 style={{ fontSize: '14px', fontWeight: 600 }}>CPU & Memory History (%)</h3>
                    </div>
                    {/* Legend */}
                    <div style={{ display: 'flex', gap: '14px', fontSize: '11px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <span style={{ width: '10px', height: '10px', background: 'var(--accent-blue)', borderRadius: '2px' }} />
                        <span style={{ color: 'var(--text-secondary)' }}>CPU %</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <span style={{ width: '10px', height: '10px', background: 'var(--accent-indigo)', borderRadius: '2px' }} />
                        <span style={{ color: 'var(--text-secondary)' }}>Memory %</span>
                      </div>
                    </div>
                  </div>

                  {/* SVG Chart */}
                  <div style={{ width: '100%', overflowX: 'auto' }}>
                    <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} style={{ width: '100%', height: 'auto', display: 'block' }}>
                      <defs>
                        <linearGradient id="cpuGrad" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="#38bdf8" stopOpacity="0.35" />
                          <stop offset="100%" stopColor="#38bdf8" stopOpacity="0.0" />
                        </linearGradient>
                        <linearGradient id="memGrad" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stopColor="#6366f1" stopOpacity="0.25" />
                          <stop offset="100%" stopColor="#6366f1" stopOpacity="0.0" />
                        </linearGradient>
                      </defs>

                      {/* Horizontal Gridlines & Y-Axis Labels */}
                      {[100, 75, 50, 25, 0].map((val) => {
                        const y = getPercentY(val);
                        return (
                          <g key={val}>
                            <line
                              x1={padLeft}
                              y1={y}
                              x2={padLeft + innerWidth}
                              y2={y}
                              stroke="rgba(255,255,255,0.07)"
                              strokeDasharray="4 4"
                            />
                            <text
                              x={padLeft - 6}
                              y={y + 3}
                              fill="var(--text-muted)"
                              fontSize="10"
                              textAnchor="end"
                              fontFamily="var(--font-mono)"
                            >
                              {val}%
                            </text>
                          </g>
                        );
                      })}

                      {/* Area Fills */}
                      {cpuArea && <path d={cpuArea} fill="url(#cpuGrad)" />}
                      {memArea && <path d={memArea} fill="url(#memGrad)" />}

                      {/* Lines */}
                      {cpuLine && <path d={cpuLine} fill="none" stroke="#38bdf8" strokeWidth="2" />}
                      {memLine && <path d={memLine} fill="none" stroke="#6366f1" strokeWidth="2" />}

                      {/* Dots on line */}
                      {metricsHistory.map((p, i) => (
                        <circle
                          key={`cpu-${i}`}
                          cx={getChartX(i, metricsHistory.length)}
                          cy={getPercentY(p.cpuPercent)}
                          r="2.5"
                          fill="#38bdf8"
                        />
                      ))}

                      {/* Time Labels on X-Axis */}
                      {metricsHistory.length > 0 && (
                        <g fill="var(--text-muted)" fontSize="10" fontFamily="var(--font-mono)">
                          <text x={padLeft} y={svgHeight - 6} textAnchor="start">
                            {new Date(metricsHistory[0].timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </text>
                          <text x={padLeft + innerWidth / 2} y={svgHeight - 6} textAnchor="middle">
                            {new Date(metricsHistory[Math.floor(metricsHistory.length / 2)].timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </text>
                          <text x={padLeft + innerWidth} y={svgHeight - 6} textAnchor="end">
                            {new Date(metricsHistory[metricsHistory.length - 1].timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </text>
                        </g>
                      )}
                    </svg>
                  </div>
                </div>

                {/* Chart 2: Network Throughput History */}
                <div className="card" style={{ padding: '20px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <Activity size={16} color="var(--accent-blue)" />
                      <h3 style={{ fontSize: '14px', fontWeight: 600 }}>Throughput History (Mbps)</h3>
                    </div>
                    {/* Legend */}
                    <div style={{ display: 'flex', gap: '14px', fontSize: '11px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <span style={{ width: '10px', height: '10px', background: 'var(--status-up)', borderRadius: '2px' }} />
                        <span style={{ color: 'var(--text-secondary)' }}>Inbound</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <span style={{ width: '10px', height: '10px', background: 'var(--accent-cyan)', borderRadius: '2px' }} />
                        <span style={{ color: 'var(--text-secondary)' }}>Outbound</span>
                      </div>
                    </div>
                  </div>

                  {/* SVG Chart */}
                  <div style={{ width: '100%', overflowX: 'auto' }}>
                    <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} style={{ width: '100%', height: 'auto', display: 'block' }}>
                      {/* Horizontal Gridlines & Y-Axis Labels */}
                      {[1.0, 0.75, 0.5, 0.25, 0].map((factor) => {
                        const val = Math.round(maxThroughput * factor);
                        const y = padTop + innerHeight - factor * innerHeight;
                        return (
                          <g key={factor}>
                            <line
                              x1={padLeft}
                              y1={y}
                              x2={padLeft + innerWidth}
                              y2={y}
                              stroke="rgba(255,255,255,0.07)"
                              strokeDasharray="4 4"
                            />
                            <text
                              x={padLeft - 6}
                              y={y + 3}
                              fill="var(--text-muted)"
                              fontSize="10"
                              textAnchor="end"
                              fontFamily="var(--font-mono)"
                            >
                              {val}M
                            </text>
                          </g>
                        );
                      })}

                      {/* Throughput Lines */}
                      {inLine && <path d={inLine} fill="none" stroke="var(--status-up)" strokeWidth="2" />}
                      {outLine && <path d={outLine} fill="none" stroke="var(--accent-cyan)" strokeWidth="2" strokeDasharray="3 3" />}

                      {/* Dots on line */}
                      {metricsHistory.map((p, i) => (
                        <circle
                          key={`in-${i}`}
                          cx={getChartX(i, metricsHistory.length)}
                          cy={getThroughputY(p.inboundMbps)}
                          r="2.5"
                          fill="var(--status-up)"
                        />
                      ))}

                      {/* Time Labels on X-Axis */}
                      {metricsHistory.length > 0 && (
                        <g fill="var(--text-muted)" fontSize="10" fontFamily="var(--font-mono)">
                          <text x={padLeft} y={svgHeight - 6} textAnchor="start">
                            {new Date(metricsHistory[0].timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </text>
                          <text x={padLeft + innerWidth / 2} y={svgHeight - 6} textAnchor="middle">
                            {new Date(metricsHistory[Math.floor(metricsHistory.length / 2)].timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </text>
                          <text x={padLeft + innerWidth} y={svgHeight - 6} textAnchor="end">
                            {new Date(metricsHistory[metricsHistory.length - 1].timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                          </text>
                        </g>
                      )}
                    </svg>
                  </div>
                </div>
              </div>

              {/* Interface Telemetry Breakdown Table */}
              <div className="card" style={{ padding: '0', overflow: 'hidden', marginBottom: '20px' }}>
                <div style={{ padding: '14px 20px', borderBottom: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Radio size={15} color="var(--accent-blue)" />
                  <h3 style={{ fontSize: '14px', fontWeight: 600 }}>Interface Telemetry Breakdown</h3>
                  <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                    ({currentMetrics.interfaceMetrics?.length || 0} interfaces)
                  </span>
                </div>

                {!currentMetrics.interfaceMetrics || currentMetrics.interfaceMetrics.length === 0 ? (
                  <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' }}>
                    No interfaces registered on this device. Interface telemetry requires configured interfaces.
                  </div>
                ) : (
                  <div className="table-responsive">
                    <table className="table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                      <thead>
                        <tr
                          style={{
                            background: 'var(--bg-secondary)',
                            borderBottom: '1px solid var(--border-color)',
                            textAlign: 'left',
                            fontSize: '12px',
                            color: 'var(--text-muted)',
                          }}
                        >
                          <th style={{ padding: '10px 16px' }}>Interface Name</th>
                          <th style={{ padding: '10px 16px' }}>Inbound (Mbps)</th>
                          <th style={{ padding: '10px 16px' }}>Outbound (Mbps)</th>
                          <th style={{ padding: '10px 16px' }}>Capacity</th>
                          <th style={{ padding: '10px 16px' }}>Utilization %</th>
                          <th style={{ padding: '10px 16px', minWidth: '160px' }}>Load Visual</th>
                        </tr>
                      </thead>
                      <tbody>
                        {currentMetrics.interfaceMetrics.map((intf) => (
                          <tr key={intf.interfaceName} style={{ borderBottom: '1px solid var(--border-color)' }}>
                            <td style={{ padding: '12px 16px', fontWeight: 600, fontFamily: 'var(--font-mono)', fontSize: '13px' }}>
                              {intf.interfaceName}
                            </td>
                            <td style={{ padding: '12px 16px', fontFamily: 'var(--font-mono)', fontSize: '12px', color: 'var(--status-up)' }}>
                              {intf.inboundMbps.toFixed(1)} Mbps
                            </td>
                            <td style={{ padding: '12px 16px', fontFamily: 'var(--font-mono)', fontSize: '12px', color: 'var(--accent-cyan)' }}>
                              {intf.outboundMbps.toFixed(1)} Mbps
                            </td>
                            <td style={{ padding: '12px 16px', fontSize: '12px', color: 'var(--text-secondary)' }}>
                              {intf.capacityMbps} Mbps
                            </td>
                            <td style={{ padding: '12px 16px', fontWeight: 600, fontSize: '12px', color: getProgressColor(intf.utilizationPercent) }}>
                              {intf.utilizationPercent.toFixed(1)}%
                            </td>
                            <td style={{ padding: '12px 16px' }}>
                              <div
                                style={{
                                  height: '8px',
                                  background: 'rgba(255, 255, 255, 0.08)',
                                  borderRadius: '4px',
                                  overflow: 'hidden',
                                }}
                              >
                                <div
                                  style={{
                                    width: `${Math.min(100, Math.max(0, intf.utilizationPercent))}%`,
                                    height: '100%',
                                    background: getProgressColor(intf.utilizationPercent),
                                    borderRadius: '4px',
                                  }}
                                />
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Telemetry Provider Metadata Footer */}
              <div
                style={{
                  fontSize: '11px',
                  color: 'var(--text-muted)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '8px 4px',
                  flexWrap: 'wrap',
                  gap: '8px',
                }}
              >
                <div>
                  Provider Source: <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)' }}>{currentMetrics.telemetrySource}</span>
                  {' • '}
                  Sample Time: <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--text-secondary)' }}>{new Date(currentMetrics.timestamp).toLocaleTimeString()}</span>
                </div>
                <div>
                  State: <span style={{ color: 'var(--status-unknown)' }}>Demonstration / Synthetic</span> (Zero DB mutations)
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
