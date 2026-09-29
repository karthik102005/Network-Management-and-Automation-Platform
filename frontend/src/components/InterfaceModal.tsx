import React, { useEffect, useState } from 'react';
import { AdminStatus, InterfaceFormData, InterfaceType, NetworkInterface, OperationalStatus } from '../types/device';
import { X, Network, Check, AlertCircle } from 'lucide-react';

interface InterfaceModalProps {
  isOpen: boolean;
  mode: 'CREATE' | 'EDIT';
  deviceId: number;
  deviceHostname?: string;
  initialInterface?: NetworkInterface | null;
  onClose: () => void;
  onSubmit: (data: InterfaceFormData) => Promise<void>;
}

const INTERFACE_TYPES: InterfaceType[] = [
  'GIGABIT_ETHERNET',
  'TEN_GIGABIT_ETHERNET',
  'FAST_ETHERNET',
  'ETHERNET',
  'LOOPBACK',
  'VLAN',
  'SERIAL',
  'MANAGEMENT',
  'WIRELESS',
  'OTHER',
];

const ADMIN_STATUSES: AdminStatus[] = ['UP', 'DOWN', 'TESTING'];
const OPER_STATUSES: OperationalStatus[] = [
  'UP',
  'DOWN',
  'UNKNOWN',
  'TESTING',
  'DORMANT',
  'NOT_PRESENT',
  'LOWER_LAYER_DOWN',
];

const IPV4_REGEX = /^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.){3}(25[0-5]|(2[0-4]|1\d|[1-9]|)\d)$/;
const MAC_REGEX = /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$|^([0-9A-Fa-f]{4}\.){2}[0-9A-Fa-f]{4}$/;

export const InterfaceModal: React.FC<InterfaceModalProps> = ({
  isOpen,
  mode,
  deviceId,
  deviceHostname,
  initialInterface,
  onClose,
  onSubmit,
}) => {
  const [formData, setFormData] = useState<InterfaceFormData>({
    deviceId,
    interfaceName: '',
    interfaceType: 'GIGABIT_ETHERNET',
    ipAddress: '',
    subnetPrefix: 24,
    macAddress: '',
    adminStatus: 'UP',
    operationalStatus: 'UNKNOWN',
    speedMbps: 1000,
    description: '',
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  useEffect(() => {
    if (initialInterface && mode === 'EDIT') {
      setFormData({
        deviceId: initialInterface.deviceId,
        interfaceName: initialInterface.interfaceName,
        interfaceType: initialInterface.interfaceType,
        ipAddress: initialInterface.ipAddress || '',
        subnetPrefix: initialInterface.subnetPrefix !== undefined ? initialInterface.subnetPrefix : 24,
        macAddress: initialInterface.macAddress || '',
        adminStatus: initialInterface.adminStatus,
        operationalStatus: initialInterface.operationalStatus,
        speedMbps: initialInterface.speedMbps || 1000,
        description: initialInterface.description || '',
      });
    } else {
      setFormData({
        deviceId,
        interfaceName: '',
        interfaceType: 'GIGABIT_ETHERNET',
        ipAddress: '',
        subnetPrefix: 24,
        macAddress: '',
        adminStatus: 'UP',
        operationalStatus: 'UNKNOWN',
        speedMbps: 1000,
        description: '',
      });
    }
    setErrors({});
    setApiError(null);
  }, [initialInterface, mode, deviceId, isOpen]);

  if (!isOpen) return null;

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    if (!formData.interfaceName.trim()) {
      errs.interfaceName = 'Interface name is required (e.g., GigabitEthernet0/0)';
    }

    if (formData.ipAddress && formData.ipAddress.trim()) {
      if (!IPV4_REGEX.test(formData.ipAddress.trim())) {
        errs.ipAddress = 'Must be a valid IPv4 address (e.g., 192.168.10.1)';
      }
    }

    if (formData.macAddress && formData.macAddress.trim()) {
      if (!MAC_REGEX.test(formData.macAddress.trim())) {
        errs.macAddress = 'Must be in format AA:BB:CC:DD:EE:FF or aabb.ccdd.eeff';
      }
    }

    if (formData.subnetPrefix !== undefined && (formData.subnetPrefix < 0 || formData.subnetPrefix > 32)) {
      errs.subnetPrefix = 'Subnet prefix must be between 0 and 32';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    setApiError(null);

    try {
      await onSubmit({
        ...formData,
        deviceId,
        interfaceName: formData.interfaceName.trim(),
        ipAddress: formData.ipAddress?.trim() || undefined,
        macAddress: formData.macAddress?.trim() || undefined,
        description: formData.description?.trim() || undefined,
      });
      onClose();
    } catch (err: any) {
      if (err.validationErrors) {
        setErrors(err.validationErrors);
      }
      setApiError(err.message || 'Failed to save network interface.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 60 }}>
      <div
        className="modal-content"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '600px' }}
        role="dialog"
        aria-modal="true"
        aria-labelledby="interface-modal-title"
      >
        <div className="modal-header">
          <div className="section-title">
            <Network size={18} color="var(--accent-blue)" />
            <span id="interface-modal-title">
              {mode === 'CREATE' ? 'Add Interface' : 'Edit Interface'}{' '}
              {deviceHostname && <span style={{ color: 'var(--text-muted)' }}>({deviceHostname})</span>}
            </span>
          </div>
          <button onClick={onClose} className="btn-icon" aria-label="Close modal">
            <X size={18} />
          </button>
        </div>

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

            <div className="form-grid-2">
              <div className="form-group">
                <label htmlFor="interface-name" className="form-label">Interface Name *</label>
                <input
                  id="interface-name"
                  type="text"
                  className="form-input mono"
                  placeholder="e.g. GigabitEthernet0/0/0"
                  value={formData.interfaceName}
                  onChange={(e) => setFormData({ ...formData, interfaceName: e.target.value })}
                  disabled={isSubmitting}
                  aria-invalid={!!errors.interfaceName}
                  aria-describedby={errors.interfaceName ? "interface-name-error" : undefined}
                />
                {errors.interfaceName && (
                  <div id="interface-name-error" className="form-error" role="alert">
                    {errors.interfaceName}
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="interface-type" className="form-label">Interface Type *</label>
                <select
                  id="interface-type"
                  className="form-select"
                  value={formData.interfaceType}
                  onChange={(e) => setFormData({ ...formData, interfaceType: e.target.value as InterfaceType })}
                  disabled={isSubmitting}
                >
                  {INTERFACE_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-grid-2">
              <div className="form-group">
                <label htmlFor="interface-ip" className="form-label">IP Address</label>
                <input
                  id="interface-ip"
                  type="text"
                  className="form-input mono"
                  placeholder="e.g. 192.168.10.1"
                  value={formData.ipAddress}
                  onChange={(e) => setFormData({ ...formData, ipAddress: e.target.value })}
                  disabled={isSubmitting}
                  aria-invalid={!!errors.ipAddress}
                  aria-describedby={errors.ipAddress ? "interface-ip-error" : undefined}
                />
                {errors.ipAddress && (
                  <div id="interface-ip-error" className="form-error" role="alert">
                    {errors.ipAddress}
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="interface-subnet" className="form-label">Subnet Prefix (CIDR)</label>
                <input
                  id="interface-subnet"
                  type="number"
                  min="0"
                  max="32"
                  className="form-input mono"
                  placeholder="e.g. 24"
                  value={formData.subnetPrefix !== undefined ? formData.subnetPrefix : ''}
                  onChange={(e) => setFormData({ ...formData, subnetPrefix: e.target.value ? parseInt(e.target.value) : undefined })}
                  disabled={isSubmitting}
                  aria-invalid={!!errors.subnetPrefix}
                  aria-describedby={errors.subnetPrefix ? "interface-subnet-error" : undefined}
                />
                {errors.subnetPrefix && (
                  <div id="interface-subnet-error" className="form-error" role="alert">
                    {errors.subnetPrefix}
                  </div>
                )}
              </div>
            </div>

            <div className="form-grid-2">
              <div className="form-group">
                <label htmlFor="interface-mac" className="form-label">MAC Address</label>
                <input
                  id="interface-mac"
                  type="text"
                  className="form-input mono"
                  placeholder="e.g. 00:1A:2B:3C:4D:5E"
                  value={formData.macAddress}
                  onChange={(e) => setFormData({ ...formData, macAddress: e.target.value })}
                  disabled={isSubmitting}
                  aria-invalid={!!errors.macAddress}
                  aria-describedby={errors.macAddress ? "interface-mac-error" : undefined}
                />
                {errors.macAddress && (
                  <div id="interface-mac-error" className="form-error" role="alert">
                    {errors.macAddress}
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="interface-speed" className="form-label">Speed (Mbps)</label>
                <input
                  id="interface-speed"
                  type="number"
                  className="form-input mono"
                  placeholder="e.g. 1000"
                  value={formData.speedMbps !== undefined ? formData.speedMbps : ''}
                  onChange={(e) => setFormData({ ...formData, speedMbps: e.target.value ? parseInt(e.target.value) : undefined })}
                  disabled={isSubmitting}
                />
              </div>
            </div>

            <div className="form-grid-2">
              <div className="form-group">
                <label htmlFor="interface-admin-status" className="form-label">Admin Status</label>
                <select
                  id="interface-admin-status"
                  className="form-select"
                  value={formData.adminStatus}
                  onChange={(e) => setFormData({ ...formData, adminStatus: e.target.value as AdminStatus })}
                  disabled={isSubmitting}
                >
                  {ADMIN_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="interface-oper-status" className="form-label">Operational Status</label>
                <select
                  id="interface-oper-status"
                  className="form-select"
                  value={formData.operationalStatus}
                  onChange={(e) => setFormData({ ...formData, operationalStatus: e.target.value as OperationalStatus })}
                  disabled={isSubmitting}
                >
                  {OPER_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="interface-desc" className="form-label">Description / VLAN Notes</label>
              <input
                id="interface-desc"
                type="text"
                className="form-input"
                placeholder="Uplink to Core SW / VLAN 10 Gateway..."
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                disabled={isSubmitting}
              />
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={isSubmitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              <Check size={16} />
              <span>{isSubmitting ? 'Saving...' : mode === 'CREATE' ? 'Add Interface' : 'Update Interface'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
