import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { apiService } from '../services/api';
import { NetworkDevice, NetworkInterface, NetworkLink } from '../types/device';
import { StatusBadge } from '../components/StatusBadge';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { EmptyState } from '../components/EmptyState';
import {
  Server,
  CheckCircle2,
  XCircle,
  HelpCircle,
  ArrowRight,
  RefreshCw,
  AlertTriangle,
  Plus,
  Network,
  GitBranch,
} from 'lucide-react';
import { DeviceModal } from '../components/DeviceModal';

export const DashboardPage: React.FC = () => {
  const [devices, setDevices] = useState<NetworkDevice[]>([]);
  const [interfaces, setInterfaces] = useState<NetworkInterface[]>([]);
  const [links, setLinks] = useState<NetworkLink[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  const fetchDashboardData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [devicesData, ifacesData, linksData] = await Promise.all([
        apiService.getDevices(),
        apiService.getInterfaces().catch(() => []),
        apiService.getLinks().catch(() => []),
      ]);
      setDevices(devicesData);
      setInterfaces(ifacesData);
      setLinks(linksData);
    } catch (err: any) {
      setError(err.message || 'Failed to load network dashboard data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  // Calculated Metrics from live API response
  const totalDevices = devices.length;
  const onlineDevices = devices.filter((d) => d.status === 'UP').length;
  const offlineDevices = devices.filter((d) => d.status === 'DOWN' || d.status === 'UNREACHABLE').length;
  const unknownDevices = devices.filter((d) => d.status === 'UNKNOWN' || d.status === 'MAINTENANCE').length;
  const totalInterfaces = interfaces.length;
  const activeLinks = links.filter((l) => l.status === 'UP').length;

  // Recent devices (sorted by updatedAt or createdAt desc, top 5)
  const recentDevices = [...devices]
    .sort((a, b) => new Date(b.updatedAt || b.createdAt).getTime() - new Date(a.updatedAt || a.createdAt).getTime())
    .slice(0, 5);

  return (
    <div>
      <div className="page-header">
        <div>
          <h2 className="page-title">Network Operations Center</h2>
          <p className="page-description">
            Real-time topology inventory, interface metrics, and link telemetry
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button onClick={fetchDashboardData} className="btn btn-secondary" title="Refresh data">
            <RefreshCw size={14} />
            <span>Refresh</span>
          </button>
          <button onClick={() => setIsAddModalOpen(true)} className="btn btn-primary">
            <Plus size={16} />
            <span>Add Device</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner" role="alert">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <AlertTriangle size={18} />
            <span>{error}</span>
          </div>
          <button onClick={fetchDashboardData} className="btn btn-secondary" style={{ padding: '4px 10px' }}>
            Retry
          </button>
        </div>
      )}

      {/* Metrics Statistics Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-blue">
            <Server size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Total Devices</span>
            <span className="stat-value">{loading ? '...' : totalDevices}</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-green">
            <CheckCircle2 size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Online Devices</span>
            <span className="stat-value">{loading ? '...' : onlineDevices}</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-purple">
            <Network size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Total Interfaces</span>
            <span className="stat-value">{loading ? '...' : totalInterfaces}</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-cyan">
            <GitBranch size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Active Links</span>
            <span className="stat-value">
              {loading ? '...' : `${activeLinks} / ${links.length}`}
            </span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-red">
            <XCircle size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Offline Devices</span>
            <span className="stat-value">{loading ? '...' : offlineDevices}</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-amber">
            <HelpCircle size={24} />
          </div>
          <div className="stat-content">
            <span className="stat-label">Unknown / Maint</span>
            <span className="stat-value">{loading ? '...' : unknownDevices}</span>
          </div>
        </div>
      </div>

      {/* Recent Devices Section */}
      <div className="section-card">
        <div className="section-header">
          <div className="section-title">
            <Server size={18} color="var(--accent-blue)" />
            <span>Recent Network Devices</span>
          </div>
          <Link to="/devices" className="btn btn-secondary" style={{ padding: '6px 12px', fontSize: '12px' }}>
            <span>Manage All Devices</span>
            <ArrowRight size={14} />
          </Link>
        </div>

        {loading ? (
          <LoadingSkeleton rows={4} />
        ) : devices.length === 0 ? (
          <EmptyState
            title="No network devices registered."
            description="Your inventory is empty. Register your first device (e.g. Cisco core router or edge switch) to populate the dashboard."
            onAction={() => setIsAddModalOpen(true)}
            actionText="Register Network Device"
          />
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Hostname</th>
                  <th>Management IP</th>
                  <th>Device Type</th>
                  <th>Vendor</th>
                  <th>Status</th>
                  <th>Last Modified</th>
                </tr>
              </thead>
              <tbody>
                {recentDevices.map((device) => (
                  <tr key={device.id}>
                    <td className="table-mono" style={{ fontWeight: 600 }}>
                      <Link to="/devices" style={{ color: 'var(--text-primary)', textDecoration: 'none' }}>
                        {device.hostname}
                      </Link>
                    </td>
                    <td className="table-mono" style={{ color: 'var(--accent-blue)' }}>
                      {device.managementIp}
                    </td>
                    <td>{device.deviceType}</td>
                    <td>{device.vendor}</td>
                    <td>
                      <StatusBadge status={device.status} />
                    </td>
                    <td style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                      {new Date(device.updatedAt || device.createdAt).toLocaleTimeString([], {
                        hour: '2-digit',
                        minute: '2-digit',
                        second: '2-digit',
                      })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <DeviceModal
        isOpen={isAddModalOpen}
        mode="CREATE"
        onClose={() => setIsAddModalOpen(false)}
        onSubmit={async (formData) => {
          await apiService.createDevice(formData);
          await fetchDashboardData();
        }}
      />
    </div>
  );
};
