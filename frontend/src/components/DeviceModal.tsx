import React, { useEffect, useState } from 'react';
import {
  DeviceFormData,
  DeviceStatus,
  DeviceType,
  DeviceVendor,
  InterfaceFormData,
  NetworkDevice,
  NetworkInterface,
} from '../types/device';
import { apiService } from '../services/api';
import { StatusBadge } from './StatusBadge';
import { InterfaceModal } from './InterfaceModal';
import {
  X,
  Server,
  Network,
  Check,
  AlertCircle,
  Plus,
  Edit2,
  Trash2,
  RefreshCw,
} from 'lucide-react';

interface DeviceModalProps {
  isOpen: boolean;
  mode: 'CREATE' | 'EDIT' | 'VIEW';
  device?: NetworkDevice | null;
  onClose: () => void;
  onSubmit: (formData: DeviceFormData) => Promise<void>;
  onDeviceUpdated?: () => void;
}

const DEVICE_TYPES: DeviceType[] = [
  'ROUTER',
  'SWITCH',
  'FIREWALL',
  'ACCESS_POINT',
  'SERVER',
  'GATEWAY',
  'LOAD_BALANCER',
  'OTHER',
];

const DEVICE_VENDORS: DeviceVendor[] = [
  'CISCO',
  'JUNIPER',
  'ARISTA',
  'MIKROTIK',
  'HUAWEI',
  'FORTINET',
  'LINUX',
  'GENERIC',
];

const DEVICE_STATUSES: DeviceStatus[] = [
  'UP',
  'DOWN',
  'UNREACHABLE',
  'MAINTENANCE',
  'UNKNOWN',
];

const IPV4_REGEX = /^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.){3}(25[0-5]|(2[0-4]|1\d|[1-9]|)\d)$/;
const HOSTNAME_REGEX = /^[a-zA-Z0-9_.-]+$/;

export const DeviceModal: React.FC<DeviceModalProps> = ({
  isOpen,
  mode,
  device,
  onClose,
  onSubmit,
  onDeviceUpdated,
}) => {
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'INTERFACES'>('OVERVIEW');
  const [formData, setFormData] = useState<DeviceFormData>({
    hostname: '',
    managementIp: '',
    deviceType: 'ROUTER',
    vendor: 'CISCO',
    status: 'UNKNOWN',
    description: '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  // Interfaces state
  const [interfaces, setInterfaces] = useState<NetworkInterface[]>([]);
  const [loadingInterfaces, setLoadingInterfaces] = useState(false);
  const [interfaceError, setInterfaceError] = useState<string | null>(null);

  // Nested interface modal state
  const [isInterfaceModalOpen, setIsInterfaceModalOpen] = useState(false);
  const [interfaceModalMode, setInterfaceModalMode] = useState<'CREATE' | 'EDIT'>('CREATE');
  const [selectedInterface, setSelectedInterface] = useState<NetworkInterface | null>(null);

  const fetchInterfaces = async (devId: number) => {
    setLoadingInterfaces(true);
    setInterfaceError(null);
    try {
      const data = await apiService.getInterfacesByDeviceId(devId);
      setInterfaces(data);
    } catch (err: any) {
      setInterfaceError(err.message || 'Failed to load device interfaces.');
    } finally {
      setLoadingInterfaces(false);
    }
  };

  useEffect(() => {
    if (device && (mode === 'EDIT' || mode === 'VIEW')) {
      setFormData({
        hostname: device.hostname,
        managementIp: device.managementIp,
        deviceType: device.deviceType,
        vendor: device.vendor,
        status: device.status,
        description: device.description || '',
      });
      fetchInterfaces(device.id);
    } else {
      setFormData({
        hostname: '',
        managementIp: '',
        deviceType: 'ROUTER',
        vendor: 'CISCO',
        status: 'UNKNOWN',
        description: '',
      });
      setInterfaces([]);
    }
    setActiveTab('OVERVIEW');
    setErrors({});
    setApiError(null);
  }, [device, mode, isOpen]);

  if (!isOpen) return null;

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.hostname.trim()) {
      newErrors.hostname = 'Hostname is required';
    } else if (!HOSTNAME_REGEX.test(formData.hostname.trim())) {
      newErrors.hostname = 'Only alphanumeric characters, dots, hyphens, and underscores are allowed';
    }

    if (!formData.managementIp.trim()) {
      newErrors.managementIp = 'Management IP is required';
    } else if (!IPV4_REGEX.test(formData.managementIp.trim())) {
      newErrors.managementIp = 'Enter a valid IPv4 address (e.g., 192.168.1.1)';
    }

    if (!formData.deviceType) {
      newErrors.deviceType = 'Device type is required';
    }

    if (!formData.vendor) {
      newErrors.vendor = 'Vendor is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (mode === 'VIEW') {
      onClose();
      return;
    }

    if (!validate()) return;

    setIsSubmitting(true);
    setApiError(null);

    try {
      await onSubmit({
        ...formData,
        hostname: formData.hostname.trim(),
        managementIp: formData.managementIp.trim(),
        description: formData.description?.trim(),
      });
      onClose();
    } catch (err: any) {
      if (err.validationErrors) {
        setErrors(err.validationErrors);
      }
      setApiError(err.message || 'Failed to save network device.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Interface Actions
  const handleOpenAddInterface = () => {
    setSelectedInterface(null);
    setInterfaceModalMode('CREATE');
    setIsInterfaceModalOpen(true);
  };

  const handleOpenEditInterface = (iface: NetworkInterface) => {
    setSelectedInterface(iface);
    setInterfaceModalMode('EDIT');
    setIsInterfaceModalOpen(true);
  };

  const handleDeleteInterface = async (iface: NetworkInterface) => {
    if (window.confirm(`Are you sure you want to delete interface ${iface.interfaceName}? Associated links will also be removed.`)) {
      try {
        await apiService.deleteInterface(iface.id);
        if (device) {
          await fetchInterfaces(device.id);
        }
        if (onDeviceUpdated) onDeviceUpdated();
      } catch (err: any) {
        alert(err.message || 'Failed to delete interface.');
      }
    }
  };

  const handleInterfaceSubmit = async (data: InterfaceFormData) => {
    if (interfaceModalMode === 'CREATE') {
      await apiService.createInterface(data);
    } else if (interfaceModalMode === 'EDIT' && selectedInterface) {
      await apiService.updateInterface(selectedInterface.id, data);
    }
    if (device) {
      await fetchInterfaces(device.id);
    }
    if (onDeviceUpdated) onDeviceUpdated();
  };

  const modalTitle =
    mode === 'CREATE'
      ? 'Register New Network Device'
      : mode === 'EDIT'
      ? `Edit Device: ${device?.hostname}`
      : `Device Details: ${device?.hostname}`;

  return (
    <>
      <div className="modal-overlay" onClick={onClose}>
        <div
          className={`modal-content ${mode !== 'CREATE' ? 'modal-lg' : ''}`}
          onClick={(e) => e.stopPropagation()}
          role="dialog"
          aria-modal="true"
          aria-labelledby="device-modal-title"
        >
          {/* Header */}
          <div className="modal-header">
            <div className="section-title">
              <Server size={18} color="var(--accent-blue)" />
              <span id="device-modal-title">{modalTitle}</span>
            </div>
            <button onClick={onClose} className="btn-icon" aria-label="Close modal">
              <X size={18} />
            </button>
          </div>

          {/* Tabs for VIEW and EDIT modes */}
          {mode !== 'CREATE' && device && (
            <div className="modal-tabs" role="tablist" aria-label="Device modal views">
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'OVERVIEW'}
                className={`modal-tab ${activeTab === 'OVERVIEW' ? 'active' : ''}`}
                onClick={() => setActiveTab('OVERVIEW')}
              >
                <Server size={14} />
                <span>Overview</span>
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'INTERFACES'}
                className={`modal-tab ${activeTab === 'INTERFACES' ? 'active' : ''}`}
                onClick={() => setActiveTab('INTERFACES')}
              >
                <Network size={14} />
                <span>Interfaces</span>
                <span className="tab-badge">{interfaces.length}</span>
              </button>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              {apiError && (
                <div className="error-banner" role="alert" style={{ marginBottom: '16px', padding: '10px 14px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <AlertCircle size={16} />
                    <span>{apiError}</span>
                  </div>
                </div>
              )}

              {/* OVERVIEW TAB / CREATE MODE */}
              {activeTab === 'OVERVIEW' && (
                <>
                  {mode === 'VIEW' && device ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                        <div>
                          <span className="form-label">Hostname</span>
                          <div className="table-mono" style={{ fontSize: '15px', fontWeight: 600 }}>
                            {device.hostname}
                          </div>
                        </div>
                        <div>
                          <span className="form-label">Management IP</span>
                          <div className="table-mono" style={{ fontSize: '15px', color: 'var(--accent-blue)' }}>
                            {device.managementIp}
                          </div>
                        </div>
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px' }}>
                        <div>
                          <span className="form-label">Type</span>
                          <div>{device.deviceType}</div>
                        </div>
                        <div>
                          <span className="form-label">Vendor</span>
                          <div>{device.vendor}</div>
                        </div>
                        <div>
                          <span className="form-label">Status</span>
                          <div>
                            <StatusBadge status={device.status} />
                          </div>
                        </div>
                      </div>

                      <div>
                        <span className="form-label">Description</span>
                        <div style={{ color: 'var(--text-secondary)', fontSize: '13px' }}>
                          {device.description || 'No description provided.'}
                        </div>
                      </div>

                      <div
                        style={{
                          borderTop: '1px solid var(--border-color)',
                          paddingTop: '14px',
                          display: 'grid',
                          gridTemplateColumns: '1fr 1fr',
                          gap: '12px',
                          fontSize: '11px',
                          color: 'var(--text-muted)',
                        }}
                      >
                        <div>
                          <span>Created: </span>
                          <span className="table-mono">{new Date(device.createdAt).toLocaleString()}</span>
                        </div>
                        <div>
                          <span>Last Updated: </span>
                          <span className="table-mono">{new Date(device.updatedAt).toLocaleString()}</span>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <>
                      <div className="form-grid-2">
                        <div className="form-group">
                          <label htmlFor="device-hostname" className="form-label">Hostname *</label>
                          <input
                            id="device-hostname"
                            type="text"
                            className="form-input mono"
                            placeholder="e.g. core-rtr01"
                            value={formData.hostname}
                            onChange={(e) => setFormData({ ...formData, hostname: e.target.value })}
                            disabled={isSubmitting}
                            aria-invalid={!!errors.hostname}
                            aria-describedby={errors.hostname ? "device-hostname-error" : undefined}
                          />
                          {errors.hostname && (
                            <div id="device-hostname-error" className="form-error" role="alert">
                              {errors.hostname}
                            </div>
                          )}
                        </div>

                        <div className="form-group">
                          <label htmlFor="device-management-ip" className="form-label">Management IP *</label>
                          <input
                            id="device-management-ip"
                            type="text"
                            className="form-input mono"
                            placeholder="e.g. 192.168.1.1"
                            value={formData.managementIp}
                            onChange={(e) => setFormData({ ...formData, managementIp: e.target.value })}
                            disabled={isSubmitting}
                            aria-invalid={!!errors.managementIp}
                            aria-describedby={errors.managementIp ? "device-management-ip-error" : undefined}
                          />
                          {errors.managementIp && (
                            <div id="device-management-ip-error" className="form-error" role="alert">
                              {errors.managementIp}
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="form-grid-2">
                        <div className="form-group">
                          <label htmlFor="device-type" className="form-label">Device Type *</label>
                          <select
                            id="device-type"
                            className="form-select"
                            value={formData.deviceType}
                            onChange={(e) => setFormData({ ...formData, deviceType: e.target.value as DeviceType })}
                            disabled={isSubmitting}
                            aria-invalid={!!errors.deviceType}
                            aria-describedby={errors.deviceType ? "device-type-error" : undefined}
                          >
                            {DEVICE_TYPES.map((t) => (
                              <option key={t} value={t}>
                                {t}
                              </option>
                            ))}
                          </select>
                          {errors.deviceType && (
                            <div id="device-type-error" className="form-error" role="alert">
                              {errors.deviceType}
                            </div>
                          )}
                        </div>

                        <div className="form-group">
                          <label htmlFor="device-vendor" className="form-label">Hardware Vendor *</label>
                          <select
                            id="device-vendor"
                            className="form-select"
                            value={formData.vendor}
                            onChange={(e) => setFormData({ ...formData, vendor: e.target.value as DeviceVendor })}
                            disabled={isSubmitting}
                            aria-invalid={!!errors.vendor}
                            aria-describedby={errors.vendor ? "device-vendor-error" : undefined}
                          >
                            {DEVICE_VENDORS.map((v) => (
                              <option key={v} value={v}>
                                {v}
                              </option>
                            ))}
                          </select>
                          {errors.vendor && (
                            <div id="device-vendor-error" className="form-error" role="alert">
                              {errors.vendor}
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="form-group">
                        <label htmlFor="device-status" className="form-label">Operational Status</label>
                        <select
                          id="device-status"
                          className="form-select"
                          value={formData.status}
                          onChange={(e) => setFormData({ ...formData, status: e.target.value as DeviceStatus })}
                          disabled={isSubmitting}
                        >
                          {DEVICE_STATUSES.map((s) => (
                            <option key={s} value={s}>
                              {s}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="form-group">
                        <label htmlFor="device-description" className="form-label">Description / Remarks</label>
                        <textarea
                          id="device-description"
                          rows={3}
                          className="form-textarea"
                          placeholder="Role, location, interface notes..."
                          value={formData.description}
                          onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                          disabled={isSubmitting}
                        />
                      </div>
                    </>
                  )}
                </>
              )}

              {/* INTERFACES TAB */}
              {activeTab === 'INTERFACES' && device && (
                <div>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      marginBottom: '16px',
                    }}
                  >
                    <div>
                      <span style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>
                        Port Interfaces ({interfaces.length})
                      </span>
                      <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: 0 }}>
                        Physical and logical interfaces assigned to {device.hostname}
                      </p>
                    </div>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        type="button"
                        onClick={() => fetchInterfaces(device.id)}
                        className="btn btn-secondary"
                        style={{ padding: '6px 10px', fontSize: '12px' }}
                        title="Reload interfaces"
                      >
                        <RefreshCw size={13} />
                      </button>
                      <button
                        type="button"
                        onClick={handleOpenAddInterface}
                        className="btn btn-primary"
                        style={{ padding: '6px 12px', fontSize: '12px' }}
                      >
                        <Plus size={14} />
                        <span>Add Interface</span>
                      </button>
                    </div>
                  </div>

                  {interfaceError && (
                    <div className="error-banner" style={{ marginBottom: '12px', padding: '8px 12px' }}>
                      <AlertCircle size={14} />
                      <span style={{ fontSize: '12px' }}>{interfaceError}</span>
                    </div>
                  )}

                  {loadingInterfaces ? (
                    <div style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                      Loading interface list...
                    </div>
                  ) : interfaces.length === 0 ? (
                    <div
                      style={{
                        padding: '36px 20px',
                        textAlign: 'center',
                        backgroundColor: 'var(--bg-secondary)',
                        borderRadius: '8px',
                        border: '1px dashed var(--border-color)',
                      }}
                    >
                      <Network size={28} color="var(--text-muted)" style={{ margin: '0 auto 8px' }} />
                      <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-primary)' }}>
                        No Interfaces Configured
                      </div>
                      <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px', marginBottom: '14px' }}>
                        Configure GigabitEthernet, Loopback, or Management ports for this device.
                      </p>
                      <button
                        type="button"
                        onClick={handleOpenAddInterface}
                        className="btn btn-primary"
                        style={{ padding: '6px 12px', fontSize: '12px' }}
                      >
                        <Plus size={14} />
                        <span>Add First Interface</span>
                      </button>
                    </div>
                  ) : (
                    <div className="table-container" style={{ maxHeight: '340px', overflowY: 'auto' }}>
                      <table className="data-table" style={{ fontSize: '12px' }}>
                        <thead>
                          <tr>
                            <th>Interface</th>
                            <th>Type</th>
                            <th>IP Address</th>
                            <th>Admin</th>
                            <th>Oper</th>
                            <th>Speed</th>
                            <th style={{ textAlign: 'right' }}>Actions</th>
                          </tr>
                        </thead>
                        <tbody>
                          {interfaces.map((iface) => (
                            <tr key={iface.id}>
                              <td className="table-mono" style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                                {iface.interfaceName}
                              </td>
                              <td style={{ color: 'var(--text-secondary)' }}>{iface.interfaceType}</td>
                              <td className="table-mono" style={{ color: 'var(--accent-blue)' }}>
                                {iface.ipAddress ? `${iface.ipAddress}/${iface.subnetPrefix ?? 24}` : '-'}
                              </td>
                              <td>
                                <span
                                  className={`badge ${
                                    iface.adminStatus === 'UP' ? 'badge-online' : 'badge-offline'
                                  }`}
                                  style={{ fontSize: '10px', padding: '1px 6px' }}
                                >
                                  {iface.adminStatus}
                                </span>
                              </td>
                              <td>
                                <span
                                  className={`badge ${
                                    iface.operationalStatus === 'UP'
                                      ? 'badge-online'
                                      : iface.operationalStatus === 'DOWN'
                                      ? 'badge-offline'
                                      : 'badge-unknown'
                                  }`}
                                  style={{ fontSize: '10px', padding: '1px 6px' }}
                                >
                                  {iface.operationalStatus}
                                </span>
                              </td>
                              <td className="table-mono" style={{ color: 'var(--text-muted)' }}>
                                {iface.speedMbps ? `${iface.speedMbps}M` : '-'}
                              </td>
                              <td style={{ textAlign: 'right' }}>
                                <div style={{ display: 'flex', gap: '4px', justifyContent: 'flex-end' }}>
                                  <button
                                    type="button"
                                    onClick={() => handleOpenEditInterface(iface)}
                                    className="btn-icon"
                                    title="Edit Interface"
                                    style={{ width: '26px', height: '26px' }}
                                  >
                                    <Edit2 size={12} />
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => handleDeleteInterface(iface)}
                                    className="btn-icon danger"
                                    title="Delete Interface"
                                    style={{ width: '26px', height: '26px' }}
                                  >
                                    <Trash2 size={12} />
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
              )}
            </div>

            <div className="modal-footer">
              <button type="button" onClick={onClose} className="btn btn-secondary" disabled={isSubmitting}>
                {mode === 'VIEW' ? 'Close' : 'Cancel'}
              </button>
              {mode !== 'VIEW' && activeTab === 'OVERVIEW' && (
                <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
                  <Check size={16} />
                  <span>{isSubmitting ? 'Saving...' : mode === 'CREATE' ? 'Register Device' : 'Update Device'}</span>
                </button>
              )}
            </div>
          </form>
        </div>
      </div>

      {/* Nested Interface Modal */}
      {device && (
        <InterfaceModal
          isOpen={isInterfaceModalOpen}
          mode={interfaceModalMode}
          deviceId={device.id}
          deviceHostname={device.hostname}
          initialInterface={selectedInterface}
          onClose={() => setIsInterfaceModalOpen(false)}
          onSubmit={handleInterfaceSubmit}
        />
      )}
    </>
  );
};
