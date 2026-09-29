import React, { useEffect, useState, useCallback } from 'react';
import { apiService } from '../services/api';
import { NetworkAlert, AlertStatus, AlertSeverity } from '../types/device';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { EmptyState } from '../components/EmptyState';
import {
  Bell,
  AlertOctagon,
  AlertTriangle,
  CheckCircle2,
  RefreshCw,
  Search,
  Server,
  Clock,
  Check,
  ShieldAlert,
  Info,
} from 'lucide-react';

export const AlertsPage: React.FC = () => {
  const [alerts, setAlerts] = useState<NetworkAlert[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [actionInProgress, setActionInProgress] = useState<Record<number, boolean>>({});

  // Filters
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const fetchAlerts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const filters: { status?: AlertStatus; severity?: AlertSeverity } = {};
      if (statusFilter !== 'ALL') {
        filters.status = statusFilter as AlertStatus;
      }
      if (severityFilter !== 'ALL') {
        filters.severity = severityFilter as AlertSeverity;
      }

      const data = await apiService.getAlerts(filters);
      setAlerts(data);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch network alerts.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, severityFilter]);

  useEffect(() => {
    fetchAlerts();
  }, [fetchAlerts]);

  const handleAcknowledge = async (alertId: number) => {
    setActionInProgress((prev) => ({ ...prev, [alertId]: true }));
    try {
      await apiService.acknowledgeAlert(alertId);
      await fetchAlerts();
    } catch (err: any) {
      setError(err.message || 'Failed to acknowledge alert.');
    } finally {
      setActionInProgress((prev) => ({ ...prev, [alertId]: false }));
    }
  };

  const handleResolve = async (alertId: number) => {
    setActionInProgress((prev) => ({ ...prev, [alertId]: true }));
    try {
      await apiService.resolveAlert(alertId);
      await fetchAlerts();
    } catch (err: any) {
      setError(err.message || 'Failed to resolve alert.');
    } finally {
      setActionInProgress((prev) => ({ ...prev, [alertId]: false }));
    }
  };

  // Local search filter
  const filteredAlerts = alerts.filter((alert) => {
    const query = searchQuery.toLowerCase();
    const matchesQuery =
      (alert.deviceHostname && alert.deviceHostname.toLowerCase().includes(query)) ||
      (alert.deviceIp && alert.deviceIp.toLowerCase().includes(query)) ||
      alert.message.toLowerCase().includes(query) ||
      alert.alertType.toLowerCase().includes(query);

    return matchesQuery;
  });

  // Calculate statistics across all loaded alerts
  const totalActive = alerts.filter((a) => a.status === 'OPEN' || a.status === 'ACKNOWLEDGED').length;
  const criticalCount = alerts.filter((a) => a.severity === 'CRITICAL' && a.status !== 'RESOLVED').length;
  const highCount = alerts.filter((a) => a.severity === 'HIGH' && a.status !== 'RESOLVED').length;
  const resolvedCount = alerts.filter((a) => a.status === 'RESOLVED').length;

  const renderSeverityBadge = (severity: AlertSeverity) => {
    switch (severity) {
      case 'CRITICAL':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: '12px',
              fontSize: '11px',
              fontWeight: 700,
              background: 'rgba(239, 68, 68, 0.15)',
              color: 'var(--status-down)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
            }}
          >
            <AlertOctagon size={11} />
            CRITICAL
          </span>
        );
      case 'HIGH':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: '12px',
              fontSize: '11px',
              fontWeight: 600,
              background: 'rgba(245, 158, 11, 0.15)',
              color: 'var(--status-unknown)',
              border: '1px solid rgba(245, 158, 11, 0.3)',
            }}
          >
            <AlertTriangle size={11} />
            HIGH
          </span>
        );
      default:
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '4px',
              padding: '3px 8px',
              borderRadius: '12px',
              fontSize: '11px',
              fontWeight: 500,
              background: 'rgba(56, 189, 248, 0.15)',
              color: 'var(--accent-blue)',
              border: '1px solid rgba(56, 189, 248, 0.3)',
            }}
          >
            <Info size={11} />
            {severity}
          </span>
        );
    }
  };

  const renderStatusBadge = (status: AlertStatus) => {
    switch (status) {
      case 'OPEN':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '5px',
              padding: '2px 8px',
              borderRadius: '10px',
              fontSize: '11px',
              fontWeight: 600,
              background: 'rgba(239, 68, 68, 0.12)',
              color: 'var(--status-down)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
            }}
          >
            <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--status-down)' }} />
            OPEN
          </span>
        );
      case 'ACKNOWLEDGED':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '5px',
              padding: '2px 8px',
              borderRadius: '10px',
              fontSize: '11px',
              fontWeight: 600,
              background: 'rgba(245, 158, 11, 0.12)',
              color: 'var(--status-unknown)',
              border: '1px solid rgba(245, 158, 11, 0.3)',
            }}
          >
            <Clock size={11} />
            ACKNOWLEDGED
          </span>
        );
      case 'RESOLVED':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '5px',
              padding: '2px 8px',
              borderRadius: '10px',
              fontSize: '11px',
              fontWeight: 600,
              background: 'rgba(16, 185, 129, 0.12)',
              color: 'var(--status-up)',
              border: '1px solid rgba(16, 185, 129, 0.3)',
            }}
          >
            <CheckCircle2 size={11} />
            RESOLVED
          </span>
        );
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h2 className="page-title">Fault & Alarm Management Center</h2>
          <p className="page-description">
            Live reachability alarms, anomaly alerts, operator lifecycle acknowledgement, and automated recovery
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button onClick={fetchAlerts} className="btn btn-secondary" title="Refresh alerts">
            <RefreshCw size={14} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner" role="alert">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <AlertTriangle size={18} />
            <span>{error}</span>
          </div>
          <button onClick={fetchAlerts} className="btn btn-secondary" style={{ padding: '4px 10px' }}>
            Retry
          </button>
        </div>
      )}

      {/* Summary Metrics Grid */}
      <div className="stats-grid" style={{ marginBottom: '24px' }}>
        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-title">Active Alarms</span>
            <div className="stat-icon" style={{ background: 'rgba(239, 68, 68, 0.1)', color: 'var(--status-down)' }}>
              <Bell size={18} />
            </div>
          </div>
          <div className="stat-value" style={{ color: totalActive > 0 ? 'var(--status-down)' : 'var(--text-primary)' }}>
            {totalActive}
          </div>
          <div className="stat-subtitle">Open & acknowledged faults</div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-title">Critical Severity</span>
            <div className="stat-icon" style={{ background: 'rgba(239, 68, 68, 0.15)', color: 'var(--status-down)' }}>
              <AlertOctagon size={18} />
            </div>
          </div>
          <div className="stat-value" style={{ color: criticalCount > 0 ? 'var(--status-down)' : 'var(--text-muted)' }}>
            {criticalCount}
          </div>
          <div className="stat-subtitle">Unreachable target devices</div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-title">High Severity</span>
            <div className="stat-icon" style={{ background: 'rgba(245, 158, 11, 0.1)', color: 'var(--status-unknown)' }}>
              <ShieldAlert size={18} />
            </div>
          </div>
          <div className="stat-value" style={{ color: highCount > 0 ? 'var(--status-unknown)' : 'var(--text-muted)' }}>
            {highCount}
          </div>
          <div className="stat-subtitle">Degraded state warnings</div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-title">Resolved History</span>
            <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.1)', color: 'var(--status-up)' }}>
              <CheckCircle2 size={18} />
            </div>
          </div>
          <div className="stat-value" style={{ color: 'var(--status-up)' }}>
            {resolvedCount}
          </div>
          <div className="stat-subtitle">Recovered or closed alarms</div>
        </div>
      </div>

      {/* Main Alerts Card */}
      <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
        {/* Controls Toolbar */}
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
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
            {/* Status Filter */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Status:</span>
              {(['ALL', 'OPEN', 'ACKNOWLEDGED', 'RESOLVED'] as const).map((st) => (
                <button
                  key={st}
                  onClick={() => setStatusFilter(st)}
                  className={`btn ${statusFilter === st ? 'btn-primary' : 'btn-secondary'}`}
                  style={{ padding: '4px 10px', fontSize: '11px', height: '28px' }}
                >
                  {st}
                </button>
              ))}
            </div>

            {/* Severity Filter */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Severity:</span>
              {(['ALL', 'CRITICAL', 'HIGH'] as const).map((sev) => (
                <button
                  key={sev}
                  onClick={() => setSeverityFilter(sev)}
                  className={`btn ${severityFilter === sev ? 'btn-primary' : 'btn-secondary'}`}
                  style={{ padding: '4px 10px', fontSize: '11px', height: '28px' }}
                >
                  {sev}
                </button>
              ))}
            </div>
          </div>

          {/* Search Box */}
          <div style={{ position: 'relative', width: '260px' }}>
            <Search size={14} style={{ position: 'absolute', left: '10px', top: '10px', color: 'var(--text-muted)' }} />
            <input
              type="text"
              className="form-input"
              placeholder="Search alerts by device, message..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ paddingLeft: '32px', height: '34px', fontSize: '13px' }}
            />
          </div>
        </div>

        {/* Content Body */}
        {loading ? (
          <div style={{ padding: '24px' }}>
            <LoadingSkeleton rows={4} />
          </div>
        ) : filteredAlerts.length === 0 ? (
          <div style={{ padding: '40px 20px' }}>
            <EmptyState
              title={alerts.length === 0 ? 'No network alerts recorded' : 'No matching alerts'}
              description={
                alerts.length === 0
                  ? 'Active alerts will be generated when on-demand reachability probes detect unreachable target devices.'
                  : 'Try adjusting your status or severity filter.'
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
                  <th style={{ padding: '12px 16px' }}>Severity</th>
                  <th style={{ padding: '12px 16px' }}>Status</th>
                  <th style={{ padding: '12px 16px' }}>Device</th>
                  <th style={{ padding: '12px 16px' }}>Alert Type</th>
                  <th style={{ padding: '12px 16px' }}>Diagnostic Message</th>
                  <th style={{ padding: '12px 16px' }}>Timestamp</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredAlerts.map((alert) => {
                  const isBusy = !!actionInProgress[alert.id];

                  return (
                    <tr
                      key={alert.id}
                      style={{
                        borderBottom: '1px solid var(--border-color)',
                        transition: 'background-color 0.15s ease',
                      }}
                    >
                      {/* Severity */}
                      <td style={{ padding: '14px 16px' }}>{renderSeverityBadge(alert.severity)}</td>

                      {/* Status */}
                      <td style={{ padding: '14px 16px' }}>{renderStatusBadge(alert.status)}</td>

                      {/* Device */}
                      <td style={{ padding: '14px 16px' }}>
                        {alert.deviceHostname ? (
                          <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 600 }}>
                              <Server size={13} color="var(--accent-blue)" />
                              <span>{alert.deviceHostname}</span>
                            </div>
                            <div style={{ fontSize: '11px', fontFamily: 'var(--font-mono)', color: 'var(--text-muted)', marginTop: '2px' }}>
                              {alert.deviceIp}
                            </div>
                          </div>
                        ) : (
                          <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>System Alert</span>
                        )}
                      </td>

                      {/* Type */}
                      <td style={{ padding: '14px 16px', fontSize: '12px', fontFamily: 'var(--font-mono)' }}>
                        <span style={{ background: 'rgba(255, 255, 255, 0.05)', padding: '2px 6px', borderRadius: '4px' }}>
                          {alert.alertType}
                        </span>
                      </td>

                      {/* Message */}
                      <td style={{ padding: '14px 16px', fontSize: '12px', maxWidth: '340px' }}>
                        <div style={{ color: 'var(--text-primary)', lineHeight: 1.4 }}>{alert.message}</div>
                        <div style={{ fontSize: '10px', color: 'var(--text-muted)', marginTop: '3px' }}>
                          Source: {alert.source}
                          {alert.resolvedAt && (
                            <span> • Resolved: {new Date(alert.resolvedAt).toLocaleTimeString()}</span>
                          )}
                        </div>
                      </td>

                      {/* Timestamp */}
                      <td style={{ padding: '14px 16px', fontSize: '11px', color: 'var(--text-muted)', whiteSpace: 'nowrap' }}>
                        <div>{new Date(alert.createdAt).toLocaleDateString()}</div>
                        <div style={{ marginTop: '2px' }}>{new Date(alert.createdAt).toLocaleTimeString()}</div>
                      </td>

                      {/* Actions */}
                      <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '6px' }}>
                          {alert.status === 'OPEN' && (
                            <button
                              onClick={() => handleAcknowledge(alert.id)}
                              disabled={isBusy}
                              className="btn btn-secondary"
                              style={{ padding: '4px 10px', fontSize: '11px', height: '28px' }}
                              title="Acknowledge fault"
                            >
                              <Check size={12} />
                              <span>Ack</span>
                            </button>
                          )}

                          {alert.status !== 'RESOLVED' && (
                            <button
                              onClick={() => handleResolve(alert.id)}
                              disabled={isBusy}
                              className="btn btn-secondary"
                              style={{
                                padding: '4px 10px',
                                fontSize: '11px',
                                height: '28px',
                                borderColor: 'var(--status-up-border)',
                                color: 'var(--status-up)',
                              }}
                              title="Resolve and close alert"
                            >
                              <CheckCircle2 size={12} />
                              <span>Resolve</span>
                            </button>
                          )}

                          {alert.status === 'RESOLVED' && (
                            <span style={{ fontSize: '11px', color: 'var(--status-up)', display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                              <CheckCircle2 size={12} />
                              Closed
                            </span>
                          )}
                        </div>
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
  );
};
