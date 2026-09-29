import React, { useEffect, useState } from 'react';
import {
  LinkFormData,
  LinkStatus,
  LinkType,
  NetworkDevice,
  NetworkInterface,
} from '../types/device';
import { apiService } from '../services/api';
import { X, GitBranch, Check, AlertCircle } from 'lucide-react';

interface LinkModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: LinkFormData) => Promise<void>;
  devices?: NetworkDevice[];
}

const LINK_TYPES: LinkType[] = [
  'ETHERNET',
  'FIBER',
  'SERIAL',
  'VIRTUAL',
  'AGGREGATE',
  'WIRELESS',
  'OTHER',
];

const LINK_STATUSES: LinkStatus[] = ['UP', 'DOWN', 'DEGRADED', 'UNKNOWN'];

export const LinkModal: React.FC<LinkModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  devices: initialDevices,
}) => {
  const [deviceList, setDeviceList] = useState<NetworkDevice[]>(initialDevices || []);
  const [sourceDeviceId, setSourceDeviceId] = useState<number | ''>('');
  const [destinationDeviceId, setDestinationDeviceId] = useState<number | ''>('');

  const [sourceInterfaces, setSourceInterfaces] = useState<NetworkInterface[]>([]);
  const [destInterfaces, setDestInterfaces] = useState<NetworkInterface[]>([]);

  const [loadingSrcIfaces, setLoadingSrcIfaces] = useState(false);
  const [loadingDstIfaces, setLoadingDstIfaces] = useState(false);

  const [sourceInterfaceId, setSourceInterfaceId] = useState<number | ''>('');
  const [destinationInterfaceId, setDestinationInterfaceId] = useState<number | ''>('');
  const [linkType, setLinkType] = useState<LinkType>('ETHERNET');
  const [status, setStatus] = useState<LinkStatus>('UP');
  const [bandwidthMbps, setBandwidthMbps] = useState<number | ''>(1000);
  const [description, setDescription] = useState('');

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  // Fetch devices if not passed
  useEffect(() => {
    if (isOpen) {
      if (!initialDevices || initialDevices.length === 0) {
        apiService.getDevices().then(setDeviceList).catch(console.error);
      } else {
        setDeviceList(initialDevices);
      }

      // Reset form
      setSourceDeviceId('');
      setDestinationDeviceId('');
      setSourceInterfaces([]);
      setDestInterfaces([]);
      setSourceInterfaceId('');
      setDestinationInterfaceId('');
      setLinkType('ETHERNET');
      setStatus('UP');
      setBandwidthMbps(1000);
      setDescription('');
      setErrors({});
      setApiError(null);
    }
  }, [isOpen, initialDevices]);

  // When sourceDeviceId changes, load its interfaces
  useEffect(() => {
    if (sourceDeviceId !== '') {
      setLoadingSrcIfaces(true);
      apiService
        .getInterfacesByDeviceId(Number(sourceDeviceId))
        .then((ifaces) => {
          setSourceInterfaces(ifaces);
          if (ifaces.length > 0) {
            setSourceInterfaceId(ifaces[0].id);
          } else {
            setSourceInterfaceId('');
          }
        })
        .catch((err) => {
          console.error(err);
          setSourceInterfaces([]);
          setSourceInterfaceId('');
        })
        .finally(() => setLoadingSrcIfaces(false));
    } else {
      setSourceInterfaces([]);
      setSourceInterfaceId('');
    }
  }, [sourceDeviceId]);

  // When destinationDeviceId changes, load its interfaces
  useEffect(() => {
    if (destinationDeviceId !== '') {
      setLoadingDstIfaces(true);
      apiService
        .getInterfacesByDeviceId(Number(destinationDeviceId))
        .then((ifaces) => {
          setDestInterfaces(ifaces);
          if (ifaces.length > 0) {
            setDestinationInterfaceId(ifaces[0].id);
          } else {
            setDestinationInterfaceId('');
          }
        })
        .catch((err) => {
          console.error(err);
          setDestInterfaces([]);
          setDestinationInterfaceId('');
        })
        .finally(() => setLoadingDstIfaces(false));
    } else {
      setDestInterfaces([]);
      setDestinationInterfaceId('');
    }
  }, [destinationDeviceId]);

  if (!isOpen) return null;

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    if (!sourceDeviceId) {
      errs.sourceDeviceId = 'Select a source device';
    }

    if (!sourceInterfaceId) {
      errs.sourceInterfaceId = 'Select a source interface';
    }

    if (!destinationDeviceId) {
      errs.destinationDeviceId = 'Select a destination device';
    }

    if (!destinationInterfaceId) {
      errs.destinationInterfaceId = 'Select a destination interface';
    }

    if (sourceDeviceId && destinationDeviceId && sourceDeviceId === destinationDeviceId) {
      errs.destinationDeviceId = 'Source and destination cannot be the same device';
    }

    if (
      sourceInterfaceId &&
      destinationInterfaceId &&
      sourceInterfaceId === destinationInterfaceId
    ) {
      errs.destinationInterfaceId = 'Source and destination cannot be the same interface';
    }

    if (bandwidthMbps !== '' && Number(bandwidthMbps) <= 0) {
      errs.bandwidthMbps = 'Bandwidth must be a positive number';
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
        sourceInterfaceId: Number(sourceInterfaceId),
        destinationInterfaceId: Number(destinationInterfaceId),
        linkType,
        status,
        bandwidthMbps: bandwidthMbps !== '' ? Number(bandwidthMbps) : undefined,
        description: description.trim() || undefined,
      });
      onClose();
    } catch (err: any) {
      if (err.validationErrors) {
        setErrors(err.validationErrors);
      }
      setApiError(err.message || 'Failed to establish network link.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 60 }}>
      <div
        className="modal-content"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '620px' }}
        role="dialog"
        aria-modal="true"
        aria-labelledby="link-modal-title"
      >
        <div className="modal-header">
          <div className="section-title">
            <GitBranch size={18} color="var(--accent-blue)" />
            <span id="link-modal-title">Establish Network Interconnect Link</span>
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

            {/* Source Device & Interface */}
            <div
              style={{
                backgroundColor: 'rgba(30, 41, 59, 0.4)',
                border: '1px solid var(--border-color)',
                borderRadius: '8px',
                padding: '14px',
                marginBottom: '16px',
              }}
            >
              <div
                style={{
                  fontSize: '12px',
                  fontWeight: 600,
                  color: 'var(--accent-blue)',
                  marginBottom: '10px',
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                }}
              >
                Endpoint A (Source Node)
              </div>
              <div className="form-grid-2">
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label htmlFor="link-source-device" className="form-label">Source Device *</label>
                  <select
                    id="link-source-device"
                    className="form-select"
                    value={sourceDeviceId}
                    onChange={(e) => {
                      const val = e.target.value === '' ? '' : Number(e.target.value);
                      setSourceDeviceId(val);
                    }}
                    disabled={isSubmitting}
                    aria-invalid={!!errors.sourceDeviceId}
                    aria-describedby={errors.sourceDeviceId ? "link-source-device-error" : undefined}
                  >
                    <option value="">-- Select Source Device --</option>
                    {deviceList.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.hostname} ({d.managementIp}) - {d.deviceType}
                      </option>
                    ))}
                  </select>
                  {errors.sourceDeviceId && (
                    <div id="link-source-device-error" className="form-error" role="alert">
                      {errors.sourceDeviceId}
                    </div>
                  )}
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label htmlFor="link-source-interface" className="form-label">Source Interface *</label>
                  <select
                    id="link-source-interface"
                    className="form-select mono"
                    value={sourceInterfaceId}
                    onChange={(e) => {
                      const val = e.target.value === '' ? '' : Number(e.target.value);
                      setSourceInterfaceId(val);
                    }}
                    disabled={isSubmitting || loadingSrcIfaces || sourceDeviceId === ''}
                    aria-invalid={!!errors.sourceInterfaceId}
                    aria-describedby={errors.sourceInterfaceId ? "link-source-interface-error" : undefined}
                  >
                    {sourceDeviceId === '' ? (
                      <option value="">Select device first</option>
                    ) : loadingSrcIfaces ? (
                      <option value="">Loading interfaces...</option>
                    ) : sourceInterfaces.length === 0 ? (
                      <option value="">No interfaces on device</option>
                    ) : (
                      sourceInterfaces.map((iface) => (
                        <option key={iface.id} value={iface.id}>
                          {iface.interfaceName} {iface.ipAddress ? `(${iface.ipAddress})` : ''} [{iface.interfaceType}]
                        </option>
                      ))
                    )}
                  </select>
                  {errors.sourceInterfaceId && (
                    <div id="link-source-interface-error" className="form-error" role="alert">
                      {errors.sourceInterfaceId}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Destination Device & Interface */}
            <div
              style={{
                backgroundColor: 'rgba(30, 41, 59, 0.4)',
                border: '1px solid var(--border-color)',
                borderRadius: '8px',
                padding: '14px',
                marginBottom: '16px',
              }}
            >
              <div
                style={{
                  fontSize: '12px',
                  fontWeight: 600,
                  color: 'var(--status-up)',
                  marginBottom: '10px',
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                }}
              >
                Endpoint B (Destination Node)
              </div>
              <div className="form-grid-2">
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label htmlFor="link-dest-device" className="form-label">Destination Device *</label>
                  <select
                    id="link-dest-device"
                    className="form-select"
                    value={destinationDeviceId}
                    onChange={(e) => {
                      const val = e.target.value === '' ? '' : Number(e.target.value);
                      setDestinationDeviceId(val);
                    }}
                    disabled={isSubmitting}
                    aria-invalid={!!errors.destinationDeviceId}
                    aria-describedby={errors.destinationDeviceId ? "link-dest-device-error" : undefined}
                  >
                    <option value="">-- Select Destination Device --</option>
                    {deviceList.map((d) => (
                      <option
                        key={d.id}
                        value={d.id}
                        disabled={d.id === sourceDeviceId}
                      >
                        {d.hostname} ({d.managementIp}) - {d.deviceType} {d.id === sourceDeviceId ? '(Same as Source)' : ''}
                      </option>
                    ))}
                  </select>
                  {errors.destinationDeviceId && (
                    <div id="link-dest-device-error" className="form-error" role="alert">
                      {errors.destinationDeviceId}
                    </div>
                  )}
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label htmlFor="link-dest-interface" className="form-label">Destination Interface *</label>
                  <select
                    id="link-dest-interface"
                    className="form-select mono"
                    value={destinationInterfaceId}
                    onChange={(e) => {
                      const val = e.target.value === '' ? '' : Number(e.target.value);
                      setDestinationInterfaceId(val);
                    }}
                    disabled={isSubmitting || loadingDstIfaces || destinationDeviceId === ''}
                    aria-invalid={!!errors.destinationInterfaceId}
                    aria-describedby={errors.destinationInterfaceId ? "link-dest-interface-error" : undefined}
                  >
                    {destinationDeviceId === '' ? (
                      <option value="">Select device first</option>
                    ) : loadingDstIfaces ? (
                      <option value="">Loading interfaces...</option>
                    ) : destInterfaces.length === 0 ? (
                      <option value="">No interfaces on device</option>
                    ) : (
                      destInterfaces.map((iface) => (
                        <option key={iface.id} value={iface.id}>
                          {iface.interfaceName} {iface.ipAddress ? `(${iface.ipAddress})` : ''} [{iface.interfaceType}]
                        </option>
                      ))
                    )}
                  </select>
                  {errors.destinationInterfaceId && (
                    <div id="link-dest-interface-error" className="form-error" role="alert">
                      {errors.destinationInterfaceId}
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Link Properties */}
            <div className="form-grid-2">
              <div className="form-group">
                <label htmlFor="link-type" className="form-label">Link Medium / Type *</label>
                <select
                  id="link-type"
                  className="form-select"
                  value={linkType}
                  onChange={(e) => setLinkType(e.target.value as LinkType)}
                  disabled={isSubmitting}
                >
                  {LINK_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label htmlFor="link-status" className="form-label">Link Status *</label>
                <select
                  id="link-status"
                  className="form-select"
                  value={status}
                  onChange={(e) => setStatus(e.target.value as LinkStatus)}
                  disabled={isSubmitting}
                >
                  {LINK_STATUSES.map((s) => (
                    <option key={s} value={s}>
                      {s}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-grid-2">
              <div className="form-group">
                <label htmlFor="link-bandwidth" className="form-label">Link Bandwidth (Mbps)</label>
                <input
                  id="link-bandwidth"
                  type="number"
                  className="form-input mono"
                  placeholder="e.g. 1000"
                  value={bandwidthMbps}
                  onChange={(e) => {
                    const val = e.target.value === '' ? '' : parseInt(e.target.value, 10);
                    setBandwidthMbps(isNaN(val as number) ? '' : val);
                  }}
                  disabled={isSubmitting}
                  aria-invalid={!!errors.bandwidthMbps}
                  aria-describedby={errors.bandwidthMbps ? "link-bandwidth-error" : undefined}
                />
                {errors.bandwidthMbps && (
                  <div id="link-bandwidth-error" className="form-error" role="alert">
                    {errors.bandwidthMbps}
                  </div>
                )}
              </div>

              <div className="form-group">
                <label htmlFor="link-description" className="form-label">Description / Tag</label>
                <input
                  id="link-description"
                  type="text"
                  className="form-input"
                  placeholder="e.g. Core Backbone Uplink"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  disabled={isSubmitting}
                />
              </div>
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" onClick={onClose} className="btn btn-secondary" disabled={isSubmitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              <Check size={16} />
              <span>{isSubmitting ? 'Connecting...' : 'Establish Link'}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
