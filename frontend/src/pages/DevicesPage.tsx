import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { DeviceFormData, NetworkDevice } from '../types/device';
import { StatusBadge } from '../components/StatusBadge';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { EmptyState } from '../components/EmptyState';
import { DeviceModal } from '../components/DeviceModal';
import { DeleteConfirmModal } from '../components/DeleteConfirmModal';
import {
  Server,
  Plus,
  RefreshCw,
  Search,
  Eye,
  Edit2,
  Trash2,
  AlertTriangle,
  Filter,
} from 'lucide-react';

export const DevicesPage: React.FC = () => {
  const [devices, setDevices] = useState<NetworkDevice[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Search and filter
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modal states
  const [modalMode, setModalMode] = useState<'CREATE' | 'EDIT' | 'VIEW'>('CREATE');
  const [selectedDevice, setSelectedDevice] = useState<NetworkDevice | null>(null);
  const [isDeviceModalOpen, setIsDeviceModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  const fetchDevices = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await apiService.getDevices();
      setDevices(data);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch network devices from backend.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDevices();
  }, []);

  const handleOpenCreate = () => {
    setSelectedDevice(null);
    setModalMode('CREATE');
    setIsDeviceModalOpen(true);
  };

  const handleOpenView = (device: NetworkDevice) => {
    setSelectedDevice(device);
    setModalMode('VIEW');
    setIsDeviceModalOpen(true);
  };

  const handleOpenEdit = (device: NetworkDevice) => {
    setSelectedDevice(device);
    setModalMode('EDIT');
    setIsDeviceModalOpen(true);
  };

  const handleOpenDelete = (device: NetworkDevice) => {
    setSelectedDevice(device);
    setIsDeleteModalOpen(true);
  };

  const handleDeviceSubmit = async (formData: DeviceFormData) => {
    if (modalMode === 'CREATE') {
      await apiService.createDevice(formData);
    } else if (modalMode === 'EDIT' && selectedDevice) {
      await apiService.updateDevice(selectedDevice.id, formData);
    }
    await fetchDevices();
  };

  const handleDeleteConfirm = async (id: number) => {
    await apiService.deleteDevice(id);
    await fetchDevices();
  };

  // Filtered devices
  const filteredDevices = devices.filter((d) => {
    const matchesSearch =
      d.hostname.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.managementIp.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.vendor.toLowerCase().includes(searchQuery.toLowerCase()) ||
      d.deviceType.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesStatus = statusFilter === 'ALL' || d.status === statusFilter;

    return matchesSearch && matchesStatus;
  });

  return (
    <div>
      <div className="page-header">
        <div>
          <h2 className="page-title">Device Inventory</h2>
          <p className="page-description">
            Manage physical and virtual routers, switches, and firewalls
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button onClick={fetchDevices} className="btn btn-secondary" title="Refresh inventory">
            <RefreshCw size={14} />
            <span>Refresh</span>
          </button>
          <button onClick={handleOpenCreate} className="btn btn-primary">
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
          <button onClick={fetchDevices} className="btn btn-secondary" style={{ padding: '4px 10px' }}>
            Retry
          </button>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div
        className="section-card"
        style={{
          padding: '16px 20px',
          marginBottom: '20px',
          display: 'flex',
          gap: '16px',
          alignItems: 'center',
          flexWrap: 'wrap',
        }}
      >
        <div style={{ position: 'relative', flexGrow: 1, minWidth: '240px' }}>
          <Search
            size={16}
            color="var(--text-muted)"
            style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}
          />
          <input
            type="text"
            className="form-input"
            style={{ paddingLeft: '36px' }}
            placeholder="Search by hostname, IP address, vendor, or device type..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Filter size={15} color="var(--text-muted)" />
          <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Status:</span>
          <select
            className="form-select"
            style={{ width: 'auto', padding: '8px 12px' }}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="UP">Online (UP)</option>
            <option value="DOWN">Offline (DOWN)</option>
            <option value="UNKNOWN">Unknown</option>
            <option value="MAINTENANCE">Maintenance</option>
          </select>
        </div>
      </div>

      {/* Main Device Inventory Table */}
      <div className="section-card">
        <div className="section-header">
          <div className="section-title">
            <Server size={18} color="var(--accent-blue)" />
            <span>Registered Devices ({filteredDevices.length})</span>
          </div>
        </div>

        {loading ? (
          <LoadingSkeleton rows={5} />
        ) : filteredDevices.length === 0 ? (
          <EmptyState
            title={searchQuery ? 'No matching network devices' : 'No network devices registered.'}
            description={
              searchQuery
                ? `No devices matched query "${searchQuery}". Try adjusting your filters.`
                : 'Your platform has no managed nodes registered. Click Add Device to get started.'
            }
            onAction={searchQuery ? () => setSearchQuery('') : handleOpenCreate}
            actionText={searchQuery ? 'Clear Filter' : 'Add Device'}
          />
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Hostname</th>
                  <th>Management IP</th>
                  <th>Device Type</th>
                  <th>Vendor</th>
                  <th>Status</th>
                  <th>Description</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredDevices.map((device) => (
                  <tr key={device.id}>
                    <td className="table-mono" style={{ color: 'var(--text-muted)' }}>
                      #{device.id}
                    </td>
                    <td className="table-mono" style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                      {device.hostname}
                    </td>
                    <td className="table-mono" style={{ color: 'var(--accent-blue)' }}>
                      {device.managementIp}
                    </td>
                    <td>{device.deviceType}</td>
                    <td>{device.vendor}</td>
                    <td>
                      <StatusBadge status={device.status} />
                    </td>
                    <td
                      style={{
                        maxWidth: '220px',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                      title={device.description}
                    >
                      {device.description || '—'}
                    </td>
                    <td>
                      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '6px' }}>
                        <button
                          onClick={() => handleOpenView(device)}
                          className="btn-icon"
                          title="View device details"
                          aria-label="View device"
                        >
                          <Eye size={15} />
                        </button>
                        <button
                          onClick={() => handleOpenEdit(device)}
                          className="btn-icon"
                          title="Edit device configuration"
                          aria-label="Edit device"
                        >
                          <Edit2 size={15} />
                        </button>
                        <button
                          onClick={() => handleOpenDelete(device)}
                          className="btn-icon danger"
                          title="Delete device"
                          aria-label="Delete device"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create / Edit / View Modal */}
      <DeviceModal
        isOpen={isDeviceModalOpen}
        mode={modalMode}
        device={selectedDevice}
        onClose={() => setIsDeviceModalOpen(false)}
        onSubmit={handleDeviceSubmit}
        onDeviceUpdated={fetchDevices}
      />

      {/* Delete Confirmation Modal */}
      <DeleteConfirmModal
        isOpen={isDeleteModalOpen}
        device={selectedDevice}
        onClose={() => setIsDeleteModalOpen(false)}
        onConfirm={handleDeleteConfirm}
      />
    </div>
  );
};
