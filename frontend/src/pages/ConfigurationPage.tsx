import React, { useEffect, useState, useCallback } from 'react';
import { apiService } from '../services/api';
import {
  NetworkDevice,
  DeviceConfiguration,
  ConfigurationTemplate,
  ConfigurationDiff,
  ConfigFormat,
} from '../types/device';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { EmptyState } from '../components/EmptyState';
import {
  CheckCircle,
  Clock,
  History,
  GitCompare,
  RotateCcw,
  Copy,
  Plus,
  RefreshCw,
  AlertTriangle,
  Server,
  Hash,
  User,
  Check,
  X,
  BookOpen,
} from 'lucide-react';

export const ConfigurationPage: React.FC = () => {
  const [devices, setDevices] = useState<NetworkDevice[]>([]);
  const [selectedDeviceId, setSelectedDeviceId] = useState<number | null>(null);
  const [configurations, setConfigurations] = useState<DeviceConfiguration[]>([]);
  const [templates, setTemplates] = useState<ConfigurationTemplate[]>([]);

  const [loadingDevices, setLoadingDevices] = useState<boolean>(true);
  const [loadingConfigs, setLoadingConfigs] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Modals & Active Views
  const [viewingConfig, setViewingConfig] = useState<DeviceConfiguration | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);
  const [isTemplatesModalOpen, setIsTemplatesModalOpen] = useState<boolean>(false);
  const [isDiffModalOpen, setIsDiffModalOpen] = useState<boolean>(false);
  const [restoringConfig, setRestoringConfig] = useState<DeviceConfiguration | null>(null);

  // Snapshot Form State
  const [formData, setFormData] = useState<{
    configText: string;
    configFormat: ConfigFormat;
    author: string;
    description: string;
  }>({
    configText: '',
    configFormat: 'CISCO_IOS',
    author: 'operator',
    description: '',
  });
  const [formSubmitting, setFormSubmitting] = useState<boolean>(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Diff State
  const [diffV1, setDiffV1] = useState<number | ''>('');
  const [diffV2, setDiffV2] = useState<number | ''>('');
  const [diffResult, setDiffResult] = useState<ConfigurationDiff | null>(null);
  const [diffLoading, setDiffLoading] = useState<boolean>(false);
  const [diffError, setDiffError] = useState<string | null>(null);

  // Restore State
  const [restoreSubmitting, setRestoreSubmitting] = useState<boolean>(false);

  // Copy feedback state
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const copyToClipboard = (text: string, key: string) => {
    navigator.clipboard.writeText(text);
    setCopiedKey(key);
    setTimeout(() => setCopiedKey(null), 2000);
  };

  // Fetch initial devices & templates
  useEffect(() => {
    const initData = async () => {
      setLoadingDevices(true);
      setError(null);
      try {
        const [devList, tmplList] = await Promise.all([
          apiService.getDevices(),
          apiService.getConfigurationTemplates().catch(() => []),
        ]);
        setDevices(devList);
        setTemplates(tmplList);
        if (devList.length > 0) {
          setSelectedDeviceId(devList[0].id);
        }
      } catch (err: any) {
        setError(err.message || 'Failed to load network devices.');
      } finally {
        setLoadingDevices(false);
      }
    };
    initData();
  }, []);

  // Fetch configurations whenever selected device changes
  const fetchConfigs = useCallback(async (deviceId: number) => {
    setLoadingConfigs(true);
    setError(null);
    try {
      const data = await apiService.getDeviceConfigurations(deviceId);
      setConfigurations(data);
    } catch (err: any) {
      setError(err.message || 'Failed to fetch configurations.');
    } finally {
      setLoadingConfigs(false);
    }
  }, []);

  useEffect(() => {
    if (selectedDeviceId !== null) {
      fetchConfigs(selectedDeviceId);
    } else {
      setConfigurations([]);
    }
  }, [selectedDeviceId, fetchConfigs]);

  const selectedDevice = devices.find((d) => d.id === selectedDeviceId);
  const activeConfig = configurations.find((c) => c.active);

  // Handle Snapshot Creation
  const handleCreateSnapshot = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDeviceId) return;
    if (!formData.configText.trim()) {
      setFormError('Configuration text cannot be empty.');
      return;
    }

    setFormSubmitting(true);
    setFormError(null);
    try {
      await apiService.createDeviceConfiguration(selectedDeviceId, {
        configText: formData.configText.trim(),
        configFormat: formData.configFormat,
        author: formData.author.trim() || 'operator',
        description: formData.description.trim() || undefined,
      });
      setIsCreateModalOpen(false);
      setFormData({
        configText: '',
        configFormat: 'CISCO_IOS',
        author: 'operator',
        description: '',
      });
      setSuccessMessage('New configuration snapshot successfully recorded.');
      setTimeout(() => setSuccessMessage(null), 4000);
      await fetchConfigs(selectedDeviceId);
    } catch (err: any) {
      setFormError(err.message || 'Failed to save configuration snapshot.');
    } finally {
      setFormSubmitting(false);
    }
  };

  // Handle Restore Confirmation
  const handleConfirmRestore = async () => {
    if (!selectedDeviceId || !restoringConfig) return;
    setRestoreSubmitting(true);
    try {
      const restored = await apiService.restoreConfigurationVersion(selectedDeviceId, restoringConfig.version);
      setRestoringConfig(null);
      setSuccessMessage(`Successfully restored version ${restoringConfig.version} as new version ${restored.version}.`);
      setTimeout(() => setSuccessMessage(null), 4000);
      await fetchConfigs(selectedDeviceId);
    } catch (err: any) {
      setError(err.message || 'Failed to restore configuration version.');
    } finally {
      setRestoreSubmitting(false);
    }
  };

  // Handle Diff Execution
  const handleRunDiff = async () => {
    if (!selectedDeviceId || diffV1 === '' || diffV2 === '') {
      setDiffError('Please select both versions to compare.');
      return;
    }
    setDiffLoading(true);
    setDiffError(null);
    setDiffResult(null);
    try {
      const result = await apiService.compareConfigurations(selectedDeviceId, Number(diffV1), Number(diffV2));
      setDiffResult(result);
    } catch (err: any) {
      setDiffError(err.message || 'Failed to compare configuration versions.');
    } finally {
      setDiffLoading(false);
    }
  };

  const openDiffModal = (v1?: number, v2?: number) => {
    if (configurations.length >= 2) {
      const defaultV1 = v1 ?? configurations[1]?.version ?? configurations[0]?.version;
      const defaultV2 = v2 ?? configurations[0]?.version;
      setDiffV1(defaultV1);
      setDiffV2(defaultV2);
    } else if (configurations.length === 1) {
      setDiffV1(configurations[0].version);
      setDiffV2(configurations[0].version);
    }
    setDiffResult(null);
    setDiffError(null);
    setIsDiffModalOpen(true);
  };

  // Populate editor with template content
  const handleApplyTemplate = (tmpl: ConfigurationTemplate) => {
    let populatedText = tmpl.templateText;
    if (selectedDevice) {
      populatedText = populatedText
        .replace(/\{\{HOSTNAME\}\}/g, selectedDevice.hostname)
        .replace(/\{\{MGMT_IP\}\}/g, selectedDevice.managementIp);
    }
    setFormData((prev) => ({
      ...prev,
      configText: populatedText,
      configFormat: tmpl.format,
      description: `Baseline from ${tmpl.name}`,
    }));
    setIsTemplatesModalOpen(false);
    setIsCreateModalOpen(true);
  };

  return (
    <div style={{ paddingBottom: '40px' }}>
      {/* Header */}
      <div className="page-header" style={{ marginBottom: '20px' }}>
        <div>
          <h2 className="page-title">Configuration Management</h2>
          <p className="page-description">
            Immutable device snapshots, cryptographic verification, versioned diffs, and non-destructive rollbacks
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button
            className="btn btn-secondary"
            onClick={() => setIsTemplatesModalOpen(true)}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <BookOpen size={16} />
            <span>Templates Library</span>
          </button>

          <button
            className="btn btn-secondary"
            onClick={() => selectedDeviceId && fetchConfigs(selectedDeviceId)}
            disabled={loadingConfigs || !selectedDeviceId}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <RefreshCw size={16} className={loadingConfigs ? 'spin' : ''} />
            <span>Refresh</span>
          </button>

          <button
            className="btn btn-primary"
            onClick={() => {
              setFormError(null);
              setIsCreateModalOpen(true);
            }}
            disabled={!selectedDeviceId}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Plus size={16} />
            <span>Take Snapshot</span>
          </button>
        </div>
      </div>

      {/* Notifications */}
      {successMessage && (
        <div
          style={{
            backgroundColor: 'var(--status-up-bg)',
            border: '1px solid var(--status-up-border)',
            borderRadius: '8px',
            padding: '12px 16px',
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            color: 'var(--status-up)',
            fontSize: '14px',
          }}
        >
          <CheckCircle size={18} />
          <span>{successMessage}</span>
        </div>
      )}

      {error && (
        <div
          role="alert"
          style={{
            backgroundColor: 'var(--status-down-bg)',
            border: '1px solid var(--status-down-border)',
            borderRadius: '8px',
            padding: '12px 16px',
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            color: 'var(--status-down)',
            fontSize: '14px',
          }}
        >
          <AlertTriangle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Device Selector & Summary Bar */}
      <div
        style={{
          backgroundColor: 'var(--bg-card)',
          border: '1px solid var(--border-color)',
          borderRadius: '10px',
          padding: '16px 20px',
          marginBottom: '24px',
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '8px',
              backgroundColor: 'rgba(56, 189, 248, 0.12)',
              color: 'var(--accent-blue)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Server size={20} />
          </div>
          <div>
            <label
              htmlFor="device-select"
              style={{
                display: 'block',
                fontSize: '11px',
                fontWeight: 600,
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
                color: 'var(--text-secondary)',
                marginBottom: '4px',
              }}
            >
              Target Network Device
            </label>
            <select
              id="device-select"
              value={selectedDeviceId ?? ''}
              onChange={(e) => setSelectedDeviceId(Number(e.target.value))}
              disabled={loadingDevices || devices.length === 0}
              style={{
                backgroundColor: 'var(--bg-primary)',
                border: '1px solid var(--border-color)',
                borderRadius: '6px',
                color: 'var(--text-primary)',
                padding: '6px 12px',
                fontSize: '14px',
                minWidth: '220px',
              }}
            >
              {devices.length === 0 ? (
                <option value="">No devices registered</option>
              ) : (
                devices.map((dev) => (
                  <option key={dev.id} value={dev.id}>
                    {dev.hostname} ({dev.managementIp}) — {dev.vendor} {dev.deviceType}
                  </option>
                ))
              )}
            </select>
          </div>
        </div>

        {selectedDevice && (
          <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>Status / Vendor</div>
              <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>
                <span
                  style={{
                    display: 'inline-block',
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    backgroundColor: selectedDevice.status === 'UP' ? 'var(--status-up)' : 'var(--status-down)',
                    marginRight: '6px',
                  }}
                />
                {selectedDevice.status} • {selectedDevice.vendor}
              </div>
            </div>

            <div style={{ height: '30px', width: '1px', backgroundColor: 'var(--border-color)' }} />

            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>Total Snapshots</div>
              <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)' }}>
                {configurations.length} {configurations.length === 1 ? 'version' : 'versions'}
              </div>
            </div>

            <div style={{ height: '30px', width: '1px', backgroundColor: 'var(--border-color)' }} />

            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>Active Version</div>
              <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--accent-blue)' }}>
                {activeConfig ? `v${activeConfig.version}` : 'None'}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Main Content Area */}
      {loadingDevices || loadingConfigs ? (
        <LoadingSkeleton />
      ) : devices.length === 0 ? (
        <EmptyState
          title="No Devices Available"
          description="Register network devices in Inventory before managing configurations."
        />
      ) : configurations.length === 0 ? (
        <div
          style={{
            backgroundColor: 'var(--bg-card)',
            border: '1px dashed var(--border-color)',
            borderRadius: '10px',
            padding: '48px 24px',
            textAlign: 'center',
          }}
        >
          <div
            style={{
              width: '48px',
              height: '48px',
              borderRadius: '50%',
              backgroundColor: 'rgba(56, 189, 248, 0.1)',
              color: 'var(--accent-blue)',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              marginBottom: '16px',
            }}
          >
            <History size={24} />
          </div>
          <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '8px' }}>
            No Configuration Snapshots for {selectedDevice?.hostname}
          </h3>
          <p
            style={{
              fontSize: '13px',
              color: 'var(--text-secondary)',
              maxWidth: '460px',
              margin: '0 auto 20px auto',
              lineHeight: 1.6,
            }}
          >
            Create the initial baseline configuration snapshot for this device, or choose from our built-in golden
            templates to get started.
          </p>
          <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
            <button className="btn btn-secondary" onClick={() => setIsTemplatesModalOpen(true)}>
              <BookOpen size={16} style={{ marginRight: '6px' }} />
              Browse Example Templates
            </button>
            <button className="btn btn-primary" onClick={() => setIsCreateModalOpen(true)}>
              <Plus size={16} style={{ marginRight: '6px' }} />
              Create Version 1 Snapshot
            </button>
          </div>
        </div>
      ) : (
        <div>
          {/* Action Toolbar above table */}
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '12px',
            }}
          >
            <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-primary)' }}>
              Configuration Snapshot History
            </div>

            {configurations.length >= 2 && (
              <button
                className="btn btn-secondary"
                onClick={() => openDiffModal()}
                style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px' }}
              >
                <GitCompare size={14} />
                <span>Compare Versions</span>
              </button>
            )}
          </div>

          {/* Configuration History Table */}
          <div
            style={{
              backgroundColor: 'var(--bg-card)',
              border: '1px solid var(--border-color)',
              borderRadius: '10px',
              overflow: 'hidden',
            }}
          >
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13px' }}>
              <thead>
                <tr
                  style={{
                    borderBottom: '1px solid var(--border-color)',
                    backgroundColor: 'rgba(255, 255, 255, 0.02)',
                    color: 'var(--text-secondary)',
                    fontSize: '11px',
                    textTransform: 'uppercase',
                    letterSpacing: '0.05em',
                  }}
                >
                  <th style={{ padding: '12px 16px', width: '90px' }}>Version</th>
                  <th style={{ padding: '12px 16px', width: '100px' }}>Status</th>
                  <th style={{ padding: '12px 16px', width: '120px' }}>Format</th>
                  <th style={{ padding: '12px 16px', width: '150px' }}>SHA-256</th>
                  <th style={{ padding: '12px 16px', width: '120px' }}>Author</th>
                  <th style={{ padding: '12px 16px', width: '160px' }}>Timestamp</th>
                  <th style={{ padding: '12px 16px' }}>Description</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right', width: '220px' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {configurations.map((cfg) => (
                  <tr
                    key={cfg.id}
                    style={{
                      borderBottom: '1px solid var(--border-color)',
                      backgroundColor: cfg.active ? 'rgba(56, 189, 248, 0.03)' : 'transparent',
                    }}
                  >
                    {/* Version */}
                    <td style={{ padding: '14px 16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                      <span
                        style={{
                          fontFamily: 'var(--font-mono)',
                          padding: '3px 8px',
                          borderRadius: '4px',
                          backgroundColor: cfg.active ? 'rgba(56, 189, 248, 0.15)' : 'rgba(255, 255, 255, 0.05)',
                          color: cfg.active ? 'var(--accent-blue)' : 'var(--text-primary)',
                        }}
                      >
                        v{cfg.version}
                      </span>
                    </td>

                    {/* Status Badge */}
                    <td style={{ padding: '14px 16px' }}>
                      {cfg.active ? (
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '5px',
                            padding: '3px 8px',
                            borderRadius: '12px',
                            fontSize: '11px',
                            fontWeight: 600,
                            backgroundColor: 'var(--status-up-bg)',
                            color: 'var(--status-up)',
                            border: '1px solid var(--status-up-border)',
                          }}
                        >
                          <CheckCircle size={12} />
                          Active
                        </span>
                      ) : (
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '5px',
                            padding: '3px 8px',
                            borderRadius: '12px',
                            fontSize: '11px',
                            fontWeight: 500,
                            backgroundColor: 'rgba(148, 163, 184, 0.1)',
                            color: 'var(--text-secondary)',
                            border: '1px solid rgba(148, 163, 184, 0.2)',
                          }}
                        >
                          Archived
                        </span>
                      )}
                    </td>

                    {/* Format */}
                    <td style={{ padding: '14px 16px', color: 'var(--text-secondary)' }}>
                      <span
                        style={{
                          fontSize: '11px',
                          padding: '2px 6px',
                          borderRadius: '4px',
                          backgroundColor: 'var(--bg-primary)',
                          border: '1px solid var(--border-color)',
                          fontFamily: 'var(--font-mono)',
                        }}
                      >
                        {cfg.configFormat}
                      </span>
                    </td>

                    {/* Checksum */}
                    <td style={{ padding: '14px 16px' }}>
                      <button
                        onClick={() => copyToClipboard(cfg.checksum, `chk-${cfg.id}`)}
                        title="Click to copy full SHA-256 checksum"
                        style={{
                          background: 'none',
                          border: 'none',
                          cursor: 'pointer',
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '6px',
                          fontFamily: 'var(--font-mono)',
                          fontSize: '11px',
                          color: copiedKey === `chk-${cfg.id}` ? 'var(--status-up)' : 'var(--text-secondary)',
                          padding: 0,
                        }}
                      >
                        <Hash size={12} />
                        <span>{cfg.checksum.substring(0, 8)}...</span>
                        {copiedKey === `chk-${cfg.id}` ? <Check size={12} /> : <Copy size={12} />}
                      </button>
                    </td>

                    {/* Author */}
                    <td style={{ padding: '14px 16px', color: 'var(--text-secondary)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <User size={13} color="var(--text-muted)" />
                        <span>{cfg.author || 'operator'}</span>
                      </div>
                    </td>

                    {/* Created At */}
                    <td style={{ padding: '14px 16px', color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <Clock size={13} color="var(--text-muted)" />
                        <span>{new Date(cfg.createdAt).toLocaleString()}</span>
                      </div>
                    </td>

                    {/* Description */}
                    <td style={{ padding: '14px 16px', color: 'var(--text-primary)', maxWidth: '240px' }}>
                      <span style={{ fontSize: '12px', color: cfg.description ? 'var(--text-primary)' : 'var(--text-muted)' }}>
                        {cfg.description || '—'}
                      </span>
                    </td>

                    {/* Actions */}
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '8px' }}>
                        <button
                          className="btn btn-secondary"
                          onClick={() => setViewingConfig(cfg)}
                          style={{ padding: '4px 10px', fontSize: '12px' }}
                        >
                          View
                        </button>

                        {configurations.length >= 2 && (
                          <button
                            className="btn btn-secondary"
                            onClick={() => openDiffModal(cfg.version, activeConfig?.version)}
                            style={{ padding: '4px 10px', fontSize: '12px' }}
                            title="Compare against active version"
                          >
                            Diff
                          </button>
                        )}

                        {!cfg.active && (
                          <button
                            className="btn btn-secondary"
                            onClick={() => setRestoringConfig(cfg)}
                            style={{
                              padding: '4px 10px',
                              fontSize: '12px',
                              color: 'var(--accent-blue)',
                              borderColor: 'rgba(56, 189, 248, 0.3)',
                            }}
                          >
                            Restore
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* --- MODAL 1: VIEW CONFIGURATION --- */}
      {viewingConfig && (
        <div className="modal-overlay" onClick={() => setViewingConfig(null)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '800px', width: '90%', maxHeight: '85vh', display: 'flex', flexDirection: 'column' }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                paddingBottom: '16px',
                borderBottom: '1px solid var(--border-color)',
                marginBottom: '16px',
              }}
            >
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Configuration v{viewingConfig.version} — {viewingConfig.deviceHostname || selectedDevice?.hostname}
                </h3>
                <div style={{ display: 'flex', gap: '12px', fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px' }}>
                  <span>Format: <strong>{viewingConfig.configFormat}</strong></span>
                  <span>•</span>
                  <span>Author: <strong>{viewingConfig.author}</strong></span>
                  <span>•</span>
                  <span>{new Date(viewingConfig.createdAt).toLocaleString()}</span>
                </div>
              </div>
              <button
                onClick={() => setViewingConfig(null)}
                style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Checksum Bar */}
            <div
              style={{
                backgroundColor: 'var(--bg-primary)',
                padding: '8px 12px',
                borderRadius: '6px',
                fontSize: '12px',
                fontFamily: 'var(--font-mono)',
                color: 'var(--text-secondary)',
                marginBottom: '12px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <span>SHA-256: {viewingConfig.checksum}</span>
              <button
                className="btn btn-secondary"
                onClick={() => copyToClipboard(viewingConfig.configText, 'modal-view')}
                style={{ padding: '3px 8px', fontSize: '11px', display: 'flex', alignItems: 'center', gap: '4px' }}
              >
                {copiedKey === 'modal-view' ? <Check size={12} /> : <Copy size={12} />}
                <span>{copiedKey === 'modal-view' ? 'Copied' : 'Copy All'}</span>
              </button>
            </div>

            {/* Code Box */}
            <div
              style={{
                backgroundColor: 'var(--bg-primary)',
                border: '1px solid var(--border-color)',
                borderRadius: '6px',
                padding: '16px',
                overflowY: 'auto',
                flex: 1,
                fontFamily: 'var(--font-mono)',
                fontSize: '12px',
                lineHeight: 1.6,
                color: 'var(--text-primary)',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-all',
              }}
            >
              {viewingConfig.configText}
            </div>

            <div style={{ marginTop: '16px', textAlign: 'right' }}>
              <button className="btn btn-secondary" onClick={() => setViewingConfig(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- MODAL 2: CREATE SNAPSHOT --- */}
      {isCreateModalOpen && (
        <div className="modal-overlay" onClick={() => !formSubmitting && setIsCreateModalOpen(false)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '750px', width: '90%', maxHeight: '90vh', display: 'flex', flexDirection: 'column' }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                paddingBottom: '14px',
                borderBottom: '1px solid var(--border-color)',
                marginBottom: '16px',
              }}
            >
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Take Configuration Snapshot
                </h3>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  Target: <strong>{selectedDevice?.hostname}</strong> ({selectedDevice?.managementIp})
                </p>
              </div>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                disabled={formSubmitting}
                style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            {formError && (
              <div
                style={{
                  backgroundColor: 'var(--status-down-bg)',
                  border: '1px solid var(--status-down-border)',
                  color: 'var(--status-down)',
                  borderRadius: '6px',
                  padding: '10px 14px',
                  fontSize: '13px',
                  marginBottom: '14px',
                }}
              >
                {formError}
              </div>
            )}

            <form onSubmit={handleCreateSnapshot} style={{ display: 'flex', flexDirection: 'column', flex: 1, gap: '14px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                    Configuration Format *
                  </label>
                  <select
                    value={formData.configFormat}
                    onChange={(e) => setFormData({ ...formData, configFormat: e.target.value as ConfigFormat })}
                    style={{
                      width: '100%',
                      backgroundColor: 'var(--bg-primary)',
                      border: '1px solid var(--border-color)',
                      borderRadius: '6px',
                      color: 'var(--text-primary)',
                      padding: '8px 12px',
                      fontSize: '13px',
                    }}
                  >
                    <option value="CISCO_IOS">Cisco IOS</option>
                    <option value="JUNOS">Juniper JunOS</option>
                    <option value="JSON">JSON / RESTCONF</option>
                    <option value="TEXT">Generic Text / UNIX</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                    Author
                  </label>
                  <input
                    type="text"
                    value={formData.author}
                    onChange={(e) => setFormData({ ...formData, author: e.target.value })}
                    placeholder="operator"
                    maxLength={100}
                    style={{
                      width: '100%',
                      backgroundColor: 'var(--bg-primary)',
                      border: '1px solid var(--border-color)',
                      borderRadius: '6px',
                      color: 'var(--text-primary)',
                      padding: '8px 12px',
                      fontSize: '13px',
                    }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                  Description / Change Summary
                </label>
                <input
                  type="text"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="e.g. Added OSPF area 0 routing statements"
                  maxLength={500}
                  style={{
                    width: '100%',
                    backgroundColor: 'var(--bg-primary)',
                    border: '1px solid var(--border-color)',
                    borderRadius: '6px',
                    color: 'var(--text-primary)',
                    padding: '8px 12px',
                    fontSize: '13px',
                  }}
                />
              </div>

              <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                  <label style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                    Configuration Text *
                  </label>
                  <button
                    type="button"
                    onClick={() => {
                      setIsCreateModalOpen(false);
                      setIsTemplatesModalOpen(true);
                    }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'var(--accent-blue)',
                      cursor: 'pointer',
                      fontSize: '11px',
                    }}
                  >
                    Load from Template
                  </button>
                </div>
                <textarea
                  value={formData.configText}
                  onChange={(e) => setFormData({ ...formData, configText: e.target.value })}
                  placeholder="Paste or write device configuration commands here..."
                  rows={14}
                  required
                  style={{
                    width: '100%',
                    flex: 1,
                    backgroundColor: 'var(--bg-primary)',
                    border: '1px solid var(--border-color)',
                    borderRadius: '6px',
                    color: 'var(--text-primary)',
                    fontFamily: 'var(--font-mono)',
                    fontSize: '12px',
                    padding: '12px',
                    lineHeight: 1.5,
                    resize: 'vertical',
                  }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setIsCreateModalOpen(false)}
                  disabled={formSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={formSubmitting}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                >
                  {formSubmitting && <RefreshCw size={14} className="spin" />}
                  <span>{formSubmitting ? 'Saving Snapshot...' : 'Save Snapshot'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* --- MODAL 3: DIFF VIEWER --- */}
      {isDiffModalOpen && (
        <div className="modal-overlay" onClick={() => setIsDiffModalOpen(false)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '900px', width: '95%', maxHeight: '90vh', display: 'flex', flexDirection: 'column' }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                paddingBottom: '14px',
                borderBottom: '1px solid var(--border-color)',
                marginBottom: '14px',
              }}
            >
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Configuration Diff Analysis
                </h3>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  Device: <strong>{selectedDevice?.hostname}</strong>
                </p>
              </div>
              <button
                onClick={() => setIsDiffModalOpen(false)}
                style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Version Selectors */}
            <div
              style={{
                display: 'flex',
                gap: '16px',
                alignItems: 'center',
                backgroundColor: 'var(--bg-primary)',
                padding: '12px 16px',
                borderRadius: '8px',
                marginBottom: '14px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Version A (Old):</span>
                <select
                  value={diffV1}
                  onChange={(e) => setDiffV1(Number(e.target.value))}
                  style={{
                    backgroundColor: 'var(--bg-card)',
                    border: '1px solid var(--border-color)',
                    color: 'var(--text-primary)',
                    borderRadius: '4px',
                    padding: '4px 8px',
                    fontSize: '12px',
                  }}
                >
                  {configurations.map((c) => (
                    <option key={c.id} value={c.version}>
                      v{c.version} {c.active ? '(Active)' : ''}
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Version B (New):</span>
                <select
                  value={diffV2}
                  onChange={(e) => setDiffV2(Number(e.target.value))}
                  style={{
                    backgroundColor: 'var(--bg-card)',
                    border: '1px solid var(--border-color)',
                    color: 'var(--text-primary)',
                    borderRadius: '4px',
                    padding: '4px 8px',
                    fontSize: '12px',
                  }}
                >
                  {configurations.map((c) => (
                    <option key={c.id} value={c.version}>
                      v{c.version} {c.active ? '(Active)' : ''}
                    </option>
                  ))}
                </select>
              </div>

              <button
                className="btn btn-primary"
                onClick={handleRunDiff}
                disabled={diffLoading || diffV1 === '' || diffV2 === ''}
                style={{ padding: '5px 14px', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                {diffLoading ? <RefreshCw size={14} className="spin" /> : <GitCompare size={14} />}
                <span>Compare</span>
              </button>
            </div>

            {diffError && (
              <div
                style={{
                  backgroundColor: 'var(--status-down-bg)',
                  border: '1px solid var(--status-down-border)',
                  color: 'var(--status-down)',
                  borderRadius: '6px',
                  padding: '10px 14px',
                  fontSize: '13px',
                  marginBottom: '14px',
                }}
              >
                {diffError}
              </div>
            )}

            {/* Diff Stats Banner */}
            {diffResult && (
              <div
                style={{
                  display: 'flex',
                  gap: '16px',
                  alignItems: 'center',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  backgroundColor: diffResult.identical ? 'rgba(16, 185, 129, 0.08)' : 'rgba(56, 189, 248, 0.08)',
                  border: `1px solid ${diffResult.identical ? 'var(--status-up-border)' : 'rgba(56, 189, 248, 0.2)'}`,
                  fontSize: '12px',
                  marginBottom: '12px',
                }}
              >
                {diffResult.identical ? (
                  <span style={{ color: 'var(--status-up)', fontWeight: 600 }}>
                    ✓ Identical: No line differences between v{diffResult.v1} and v{diffResult.v2}.
                  </span>
                ) : (
                  <>
                    <span style={{ color: 'var(--status-up)', fontWeight: 600 }}>
                      +{diffResult.addedCount} added
                    </span>
                    <span style={{ color: 'var(--status-down)', fontWeight: 600 }}>
                      -{diffResult.removedCount} removed
                    </span>
                    <span style={{ color: 'var(--text-secondary)' }}>
                      {diffResult.unchangedCount} unchanged
                    </span>
                  </>
                )}
              </div>
            )}

            {/* Diff Output Lines */}
            <div
              style={{
                backgroundColor: 'var(--bg-primary)',
                border: '1px solid var(--border-color)',
                borderRadius: '6px',
                overflowY: 'auto',
                flex: 1,
                fontFamily: 'var(--font-mono)',
                fontSize: '12px',
              }}
            >
              {!diffResult && !diffLoading && (
                <div style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)' }}>
                  Select two versions above and click "Compare" to view unified diff.
                </div>
              )}

              {diffLoading && (
                <div style={{ padding: '32px', textAlign: 'center', color: 'var(--text-secondary)' }}>
                  <RefreshCw size={20} className="spin" style={{ display: 'inline-block', marginBottom: '8px' }} />
                  <div>Computing line-by-line diff...</div>
                </div>
              )}

              {diffResult && (
                <div>
                  {diffResult.diffLines.map((line, idx) => {
                    const isAdded = line.type === 'ADDED';
                    const isRemoved = line.type === 'REMOVED';
                    const bgColor = isAdded
                      ? 'rgba(16, 185, 129, 0.12)'
                      : isRemoved
                      ? 'rgba(239, 68, 68, 0.12)'
                      : 'transparent';
                    const textColor = isAdded
                      ? 'var(--status-up)'
                      : isRemoved
                      ? 'var(--status-down)'
                      : 'var(--text-primary)';
                    const prefix = isAdded ? '+' : isRemoved ? '-' : ' ';

                    return (
                      <div
                        key={idx}
                        style={{
                          display: 'flex',
                          backgroundColor: bgColor,
                          color: textColor,
                          lineHeight: '22px',
                          borderBottom: '1px solid rgba(255, 255, 255, 0.02)',
                        }}
                      >
                        <div
                          style={{
                            width: '45px',
                            textAlign: 'right',
                            paddingRight: '8px',
                            color: 'var(--text-muted)',
                            userSelect: 'none',
                            borderRight: '1px solid var(--border-color)',
                          }}
                        >
                          {line.oldLineNumber ?? ''}
                        </div>
                        <div
                          style={{
                            width: '45px',
                            textAlign: 'right',
                            paddingRight: '8px',
                            color: 'var(--text-muted)',
                            userSelect: 'none',
                            borderRight: '1px solid var(--border-color)',
                          }}
                        >
                          {line.newLineNumber ?? ''}
                        </div>
                        <div style={{ width: '20px', textAlign: 'center', userSelect: 'none', fontWeight: 'bold' }}>
                          {prefix}
                        </div>
                        <div style={{ flex: 1, paddingLeft: '6px', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                          {line.content}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <div style={{ marginTop: '14px', textAlign: 'right' }}>
              <button className="btn btn-secondary" onClick={() => setIsDiffModalOpen(false)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- MODAL 4: RESTORE CONFIRMATION --- */}
      {restoringConfig && (
        <div className="modal-overlay" onClick={() => !restoreSubmitting && setRestoringConfig(null)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '520px', width: '90%' }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
              <div
                style={{
                  width: '40px',
                  height: '40px',
                  borderRadius: '50%',
                  backgroundColor: 'rgba(56, 189, 248, 0.12)',
                  color: 'var(--accent-blue)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <RotateCcw size={20} />
              </div>
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Restore Configuration v{restoringConfig.version}?
                </h3>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  Device: {selectedDevice?.hostname}
                </p>
              </div>
            </div>

            <div
              style={{
                backgroundColor: 'rgba(56, 189, 248, 0.06)',
                border: '1px solid rgba(56, 189, 248, 0.2)',
                borderRadius: '8px',
                padding: '14px',
                fontSize: '13px',
                color: 'var(--text-primary)',
                lineHeight: 1.5,
                marginBottom: '16px',
              }}
            >
              <div style={{ fontWeight: 600, color: 'var(--accent-blue)', marginBottom: '4px' }}>
                Non-Destructive Versioning
              </div>
              Restoring this snapshot will <strong>NOT</strong> overwrite or delete any historical records. A new version{' '}
              <strong style={{ color: 'var(--accent-blue)' }}>
                v{(configurations[0]?.version ?? 0) + 1}
              </strong>{' '}
              will be created with this snapshot's exact configuration text, marked as active, and logged in the audit trail.
            </div>

            <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '20px' }}>
              <div>Snapshot Date: {new Date(restoringConfig.createdAt).toLocaleString()}</div>
              <div>Author: {restoringConfig.author || 'operator'}</div>
              <div>Format: {restoringConfig.configFormat}</div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button
                className="btn btn-secondary"
                onClick={() => setRestoringConfig(null)}
                disabled={restoreSubmitting}
              >
                Cancel
              </button>
              <button
                className="btn btn-primary"
                onClick={handleConfirmRestore}
                disabled={restoreSubmitting}
                style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
              >
                {restoreSubmitting && <RefreshCw size={14} className="spin" />}
                <span>{restoreSubmitting ? 'Restoring...' : `Confirm & Create Version ${(configurations[0]?.version ?? 0) + 1}`}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- MODAL 5: TEMPLATES LIBRARY --- */}
      {isTemplatesModalOpen && (
        <div className="modal-overlay" onClick={() => setIsTemplatesModalOpen(false)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '850px', width: '95%', maxHeight: '85vh', display: 'flex', flexDirection: 'column' }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                paddingBottom: '14px',
                borderBottom: '1px solid var(--border-color)',
                marginBottom: '16px',
              }}
            >
              <div>
                <h3 style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                  Golden Configuration Templates
                </h3>
                <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                  Standard baseline templates with dynamic variable substitution
                </p>
              </div>
              <button
                onClick={() => setIsTemplatesModalOpen(false)}
                style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Disclaimer Banner */}
            <div
              style={{
                backgroundColor: 'var(--status-unknown-bg)',
                border: '1px solid var(--status-unknown-border)',
                borderRadius: '8px',
                padding: '10px 14px',
                fontSize: '12px',
                color: 'var(--status-unknown)',
                marginBottom: '16px',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <AlertTriangle size={16} />
              <span>
                <strong>Educational Reference Notice:</strong> These templates are generic baseline examples and have not
                been validated against real physical hardware.
              </span>
            </div>

            {/* Templates List */}
            <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {templates.map((tmpl) => (
                <div
                  key={tmpl.id}
                  style={{
                    backgroundColor: 'var(--bg-primary)',
                    border: '1px solid var(--border-color)',
                    borderRadius: '8px',
                    padding: '16px',
                  }}
                >
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'flex-start',
                      marginBottom: '10px',
                    }}
                  >
                    <div>
                      <h4 style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '4px' }}>
                        {tmpl.name}
                      </h4>
                      <p style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>{tmpl.description}</p>
                    </div>

                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                      <span
                        style={{
                          fontSize: '11px',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          backgroundColor: 'var(--bg-card)',
                          color: 'var(--text-secondary)',
                          border: '1px solid var(--border-color)',
                        }}
                      >
                        {tmpl.vendor} • {tmpl.deviceType}
                      </span>
                      <button
                        className="btn btn-primary"
                        onClick={() => handleApplyTemplate(tmpl)}
                        style={{ padding: '4px 12px', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '4px' }}
                      >
                        <Copy size={12} />
                        <span>Use Template</span>
                      </button>
                    </div>
                  </div>

                  {/* Variables */}
                  <div style={{ display: 'flex', gap: '6px', alignItems: 'center', marginBottom: '10px' }}>
                    <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Variables:</span>
                    {tmpl.variables.map((v) => (
                      <span
                        key={v}
                        style={{
                          fontSize: '10px',
                          fontFamily: 'var(--font-mono)',
                          padding: '1px 6px',
                          borderRadius: '3px',
                          backgroundColor: 'rgba(56, 189, 248, 0.1)',
                          color: 'var(--accent-blue)',
                        }}
                      >
                        {`{{${v}}}`}
                      </span>
                    ))}
                  </div>

                  {/* Preview Code Box */}
                  <pre
                    style={{
                      backgroundColor: 'var(--bg-card)',
                      border: '1px solid var(--border-color)',
                      borderRadius: '6px',
                      padding: '10px 12px',
                      fontFamily: 'var(--font-mono)',
                      fontSize: '11px',
                      color: 'var(--text-secondary)',
                      maxHeight: '120px',
                      overflowY: 'auto',
                      whiteSpace: 'pre-wrap',
                      lineHeight: 1.4,
                      margin: 0,
                    }}
                  >
                    {tmpl.templateText}
                  </pre>
                </div>
              ))}
            </div>

            <div style={{ marginTop: '16px', textAlign: 'right' }}>
              <button className="btn btn-secondary" onClick={() => setIsTemplatesModalOpen(false)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
