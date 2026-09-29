import React, { useEffect, useState, useCallback } from 'react';
import { apiService } from '../services/api';
import {
  AutomationPlaybook,
  AutomationChangeRequest,
  AutomationChangeRequestCreateData,
  AutomationStatus,
  NetworkDevice,
} from '../types/device';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { EmptyState } from '../components/EmptyState';
import {
  Cpu,
  Terminal,
  RefreshCw,
  Play,
  RotateCcw,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  Server,
  Plus,
  X,
  FileCode,
  ShieldCheck,
  Layers,
  ArrowRight,
  ExternalLink,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const AutomationPage: React.FC = () => {
  const [playbooks, setPlaybooks] = useState<AutomationPlaybook[]>([]);
  const [requests, setRequests] = useState<AutomationChangeRequest[]>([]);
  const [devices, setDevices] = useState<NetworkDevice[]>([]);

  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Modals & Active Selections
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);
  const [selectedPlaybook, setSelectedPlaybook] = useState<AutomationPlaybook | null>(null);
  const [viewingRequest, setViewingRequest] = useState<AutomationChangeRequest | null>(null);

  // Action Spinners
  const [executingId, setExecutingId] = useState<number | null>(null);
  const [rollingBackId, setRollingBackId] = useState<number | null>(null);
  const [submittingCreate, setSubmittingCreate] = useState<boolean>(false);

  // Form State
  const [formData, setFormData] = useState<AutomationChangeRequestCreateData>({
    deviceId: 1,
    title: '',
    description: '',
    playbookId: '',
    configCommands: '',
    author: 'operator',
  });
  const [variableValues, setVariableValues] = useState<Record<string, string>>({});

  // Fetch all initial data
  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [playbooksData, requestsData, devicesData] = await Promise.all([
        apiService.getPlaybooks(),
        apiService.getAutomationRequests(),
        apiService.getDevices(),
      ]);
      setPlaybooks(playbooksData);
      setRequests(requestsData);
      setDevices(devicesData);

      if (devicesData.length > 0 && !formData.deviceId) {
        setFormData((prev) => ({ ...prev, deviceId: devicesData[0].id }));
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load automation data from backend.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [formData.deviceId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Open Create Modal from a specific Playbook
  const handleLaunchPlaybook = (playbook: AutomationPlaybook) => {
    setSelectedPlaybook(playbook);

    // Initial variable values
    const initialVars: Record<string, string> = {};
    playbook.variables.forEach((v) => {
      initialVars[v] = '';
    });

    // Sensible defaults
    if (playbook.id === 'playbook-vlan-provision') {
      initialVars['VLAN_ID'] = '100';
      initialVars['VLAN_NAME'] = 'Engineering';
      initialVars['IP_ADDRESS'] = '10.100.1.1';
      initialVars['SUBNET_MASK'] = '255.255.255.0';
    } else if (playbook.id === 'playbook-acl-hardening') {
      initialVars['ACL_NAME'] = 'MANAGEMENT-ACCESS';
      initialVars['MANAGEMENT_SUBNET'] = '192.168.10.0';
      initialVars['WILDCARD_MASK'] = '0.0.0.255';
    } else if (playbook.id === 'playbook-ntp-syslog') {
      initialVars['NTP_SERVER'] = 'time.google.com';
      initialVars['SYSLOG_HOST'] = '192.168.10.254';
    } else if (playbook.id === 'playbook-interface-turnup') {
      initialVars['INTERFACE_NAME'] = 'GigabitEthernet0/0/1';
      initialVars['DESCRIPTION'] = 'Uplink to Core-Distribution';
      initialVars['IP_ADDRESS'] = '10.254.1.2';
      initialVars['SUBNET_MASK'] = '255.255.255.252';
    }

    setVariableValues(initialVars);

    // Generate commands with initial values
    let cmds = playbook.commandTemplate;
    Object.entries(initialVars).forEach(([k, v]) => {
      cmds = cmds.replace(new RegExp(`\\{\\{${k}\\}\\}`, 'g'), v);
    });

    setFormData({
      deviceId: devices.length > 0 ? devices[0].id : 1,
      title: `${playbook.name} - Automated Deployment`,
      description: playbook.description,
      playbookId: playbook.id,
      configCommands: cmds,
      author: 'operator',
    });

    setIsCreateModalOpen(true);
  };

  // Update playbook variable live
  const handleVariableChange = (varName: string, val: string) => {
    const updatedVars = { ...variableValues, [varName]: val };
    setVariableValues(updatedVars);

    if (selectedPlaybook) {
      let cmds = selectedPlaybook.commandTemplate;
      Object.entries(updatedVars).forEach(([k, v]) => {
        cmds = cmds.replace(new RegExp(`\\{\\{${k}\\}\\}`, 'g'), v || `{{${k}}}`);
      });
      setFormData((prev) => ({ ...prev, configCommands: cmds }));
    }
  };

  // Create Change Request Handler
  const handleCreateRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title.trim() || !formData.configCommands.trim()) {
      setError('Title and configuration commands are required.');
      return;
    }

    setSubmittingCreate(true);
    setError(null);
    try {
      const created = await apiService.createAutomationRequest(formData);
      setSuccessMessage(`Change Request #${created.id} ("${created.title}") created in DRAFT status.`);
      setIsCreateModalOpen(false);
      await fetchData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error creating change request.';
      setError(msg);
    } finally {
      setSubmittingCreate(false);
    }
  };

  // Execute Change Request Handler
  const handleExecuteRequest = async (id: number) => {
    setExecutingId(id);
    setError(null);
    setSuccessMessage(null);
    try {
      const executed = await apiService.executeAutomationRequest(id);
      setSuccessMessage(`Change Request #${id} executed successfully! Configuration snapshot v${executed.postChangeVersion} created.`);
      await fetchData();
      setViewingRequest(executed);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error executing change request.';
      setError(msg);
    } finally {
      setExecutingId(null);
    }
  };

  // Rollback Change Request Handler
  const handleRollbackRequest = async (id: number) => {
    if (!window.confirm(`Are you sure you want to roll back Change Request #${id}? This will create a non-destructive restoration snapshot reverting to the pre-change configuration.`)) {
      return;
    }

    setRollingBackId(id);
    setError(null);
    setSuccessMessage(null);
    try {
      const rolledBack = await apiService.rollbackAutomationRequest(id);
      setSuccessMessage(`Change Request #${id} rolled back successfully! Restored to v${rolledBack.preChangeVersion} as new active snapshot v${rolledBack.rollbackVersion}.`);
      await fetchData();
      setViewingRequest(rolledBack);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error rolling back change request.';
      setError(msg);
    } finally {
      setRollingBackId(null);
    }
  };

  // Status Badge Helper
  const renderStatusBadge = (status: AutomationStatus) => {
    switch (status) {
      case 'DRAFT':
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
              background: 'rgba(100, 116, 139, 0.15)',
              color: 'var(--text-secondary)',
              border: '1px solid rgba(100, 116, 139, 0.3)',
            }}
          >
            <Clock size={11} />
            DRAFT
          </span>
        );
      case 'COMPLETED':
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
              background: 'var(--status-up-bg)',
              color: 'var(--status-up)',
              border: '1px solid var(--status-up-border)',
            }}
          >
            <CheckCircle2 size={11} />
            COMPLETED
          </span>
        );
      case 'ROLLED_BACK':
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
              background: 'var(--status-unknown-bg)',
              color: 'var(--status-unknown)',
              border: '1px solid var(--status-unknown-border)',
            }}
          >
            <RotateCcw size={11} />
            ROLLED_BACK
          </span>
        );
      case 'FAILED':
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
              background: 'var(--status-down-bg)',
              color: 'var(--status-down)',
              border: '1px solid var(--status-down-border)',
            }}
          >
            <XCircle size={11} />
            FAILED
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
              fontWeight: 600,
              background: 'rgba(56, 189, 248, 0.15)',
              color: 'var(--accent-blue)',
            }}
          >
            {status}
          </span>
        );
    }
  };

  // Counts
  const completedCount = requests.filter((r) => r.status === 'COMPLETED').length;
  const rolledBackCount = requests.filter((r) => r.status === 'ROLLED_BACK').length;

  return (
    <div style={{ paddingBottom: '40px' }}>
      {/* Header */}
      <div className="page-header" style={{ marginBottom: '16px' }}>
        <div>
          <h2 className="page-title">Network Automation & Orchestration</h2>
          <p className="page-description">
            Playbook execution, simulated CLI staging, version-controlled commits, and non-destructive rollbacks
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button onClick={fetchData} className="btn btn-secondary" title="Refresh data">
            <RefreshCw size={14} className={loading ? 'spin' : ''} />
            <span>Refresh</span>
          </button>
          <button
            onClick={() => {
              setSelectedPlaybook(null);
              setFormData({
                deviceId: devices.length > 0 ? devices[0].id : 1,
                title: '',
                description: '',
                playbookId: '',
                configCommands: '!\nEnter configuration commands here\n',
                author: 'operator',
              });
              setVariableValues({});
              setIsCreateModalOpen(true);
            }}
            className="btn btn-primary"
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <Plus size={14} />
            <span>New Change Request</span>
          </button>
        </div>
      </div>

      {/* Prominent Amber Simulated Disclaimer Banner */}
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
            SIMULATED AUTOMATION RUNTIME ACTIVE — EDUCATIONAL STAGING
          </div>
          <div>
            All change requests, CLI staging commands, and verification checks are executed against an in-memory network device simulator.
            <strong style={{ color: 'var(--text-primary)' }}> No live SSH, NETCONF, or physical commands are issued to real hardware.</strong>
            {' '}Committed changes generate persistent, versioned configuration snapshots in PostgreSQL with full audit logging and safe rollback capability.
          </div>
        </div>
      </div>

      {/* Error & Success Banners */}
      {error && (
        <div className="error-banner" role="alert" style={{ marginBottom: '16px' }}>
          <AlertTriangle size={18} />
          <span>{error}</span>
        </div>
      )}

      {successMessage && (
        <div
          style={{
            background: 'rgba(16, 185, 129, 0.1)',
            border: '1px solid var(--status-up-border)',
            color: 'var(--status-up)',
            borderRadius: '8px',
            padding: '12px 16px',
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            fontSize: '13px',
          }}
        >
          <CheckCircle2 size={16} />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Stats Cards */}
      <div className="stats-grid" style={{ marginBottom: '24px' }}>
        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-title">Catalog Playbooks</span>
            <div className="stat-icon" style={{ background: 'rgba(56, 189, 248, 0.1)', color: 'var(--accent-blue)' }}>
              <Layers size={18} />
            </div>
          </div>
          <div className="stat-value">{playbooks.length}</div>
          <div className="stat-subtitle">Pre-validated network workflow templates</div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-title">Total Requests</span>
            <div className="stat-icon" style={{ background: 'rgba(99, 102, 241, 0.1)', color: 'var(--accent-indigo)' }}>
              <Terminal size={18} />
            </div>
          </div>
          <div className="stat-value">{requests.length}</div>
          <div className="stat-subtitle">Drafted, executed, and archived jobs</div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-title">Successfully Completed</span>
            <div className="stat-icon" style={{ background: 'rgba(16, 185, 129, 0.1)', color: 'var(--status-up)' }}>
              <CheckCircle2 size={18} />
            </div>
          </div>
          <div className="stat-value" style={{ color: 'var(--status-up)' }}>{completedCount}</div>
          <div className="stat-subtitle">Configuration committed with snapshot version</div>
        </div>

        <div className="stat-card">
          <div className="stat-header">
            <span className="stat-title">Rolled Back</span>
            <div className="stat-icon" style={{ background: 'rgba(245, 158, 11, 0.1)', color: 'var(--status-unknown)' }}>
              <RotateCcw size={18} />
            </div>
          </div>
          <div className="stat-value" style={{ color: 'var(--status-unknown)' }}>{rolledBackCount}</div>
          <div className="stat-subtitle">Reverted to pre-change snapshot state</div>
        </div>
      </div>

      {/* Playbook Catalog Cards */}
      <div style={{ marginBottom: '28px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
          <FileCode size={16} color="var(--accent-blue)" />
          <h3 style={{ fontSize: '15px', fontWeight: 600 }}>Automation Playbook Catalog</h3>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>({playbooks.length} templates)</span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
          {playbooks.map((pb) => (
            <div
              key={pb.id}
              className="card"
              style={{
                padding: '18px',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                transition: 'border-color 0.2s',
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                  <span
                    style={{
                      fontSize: '11px',
                      fontWeight: 700,
                      padding: '2px 8px',
                      borderRadius: '10px',
                      background: 'rgba(56, 189, 248, 0.15)',
                      color: 'var(--accent-blue)',
                    }}
                  >
                    {pb.category}
                  </span>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{pb.vendor}</span>
                </div>

                <h4 style={{ fontSize: '14px', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '6px' }}>
                  {pb.name}
                </h4>

                <p style={{ fontSize: '12px', color: 'var(--text-secondary)', lineHeight: 1.5, marginBottom: '14px' }}>
                  {pb.description}
                </p>
              </div>

              <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                  {pb.variables.length} parameters
                </span>
                <button
                  onClick={() => handleLaunchPlaybook(pb)}
                  className="btn btn-secondary"
                  style={{ padding: '4px 10px', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '4px' }}
                >
                  <Play size={12} />
                  <span>Launch Playbook</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Change Requests Audit Table */}
      <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid var(--border-color)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Cpu size={16} color="var(--accent-blue)" />
            <h3 style={{ fontSize: '15px', fontWeight: 600 }}>Automation Change Requests</h3>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>({requests.length} records)</span>
          </div>
        </div>

        {loading ? (
          <div style={{ padding: '24px' }}>
            <LoadingSkeleton rows={4} />
          </div>
        ) : requests.length === 0 ? (
          <div style={{ padding: '40px 20px' }}>
            <EmptyState
              title="No automation change requests"
              description="Launch a playbook above or click 'New Change Request' to stage and execute a simulated network change."
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
                  <th style={{ padding: '12px 16px' }}>Request ID & Title</th>
                  <th style={{ padding: '12px 16px' }}>Target Device</th>
                  <th style={{ padding: '12px 16px' }}>Playbook</th>
                  <th style={{ padding: '12px 16px' }}>Status</th>
                  <th style={{ padding: '12px 16px' }}>Config Versions</th>
                  <th style={{ padding: '12px 16px' }}>Author & Created</th>
                  <th style={{ padding: '12px 16px', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((req) => {
                  const isExecuting = executingId === req.id;
                  const isRollingBack = rollingBackId === req.id;

                  return (
                    <tr
                      key={req.id}
                      style={{
                        borderBottom: '1px solid var(--border-color)',
                        transition: 'background-color 0.15s ease',
                      }}
                    >
                      {/* ID & Title */}
                      <td style={{ padding: '14px 16px' }}>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)', fontSize: '13px' }}>
                          #{req.id} — {req.title}
                        </div>
                        {req.description && (
                          <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '2px' }}>
                            {req.description}
                          </div>
                        )}
                      </td>

                      {/* Device */}
                      <td style={{ padding: '14px 16px', fontSize: '13px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          <Server size={13} color="var(--accent-blue)" />
                          <span style={{ fontWeight: 600 }}>{req.deviceHostname || `Device #${req.deviceId}`}</span>
                        </div>
                      </td>

                      {/* Playbook */}
                      <td style={{ padding: '14px 16px', fontSize: '12px', color: 'var(--text-secondary)' }}>
                        {req.playbookId ? (
                          <span style={{ fontFamily: 'var(--font-mono)', fontSize: '11px' }}>{req.playbookId}</span>
                        ) : (
                          <span style={{ color: 'var(--text-muted)' }}>Custom / Manual</span>
                        )}
                      </td>

                      {/* Status */}
                      <td style={{ padding: '14px 16px' }}>
                        {renderStatusBadge(req.status)}
                      </td>

                      {/* Config Versions */}
                      <td style={{ padding: '14px 16px', fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                        {req.preChangeVersion != null ? (
                          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                            <span>v{req.preChangeVersion}</span>
                            {req.postChangeVersion != null && (
                              <>
                                <ArrowRight size={11} color="var(--text-muted)" />
                                <span style={{ color: 'var(--accent-blue)', fontWeight: 600 }}>v{req.postChangeVersion}</span>
                              </>
                            )}
                            {req.rollbackVersion != null && (
                              <span style={{ color: 'var(--status-unknown)', fontSize: '11px' }}>
                                {' '}(Rollback: v{req.rollbackVersion})
                              </span>
                            )}
                          </span>
                        ) : (
                          <span style={{ color: 'var(--text-muted)' }}>—</span>
                        )}
                      </td>

                      {/* Author & Timestamp */}
                      <td style={{ padding: '14px 16px', fontSize: '12px', color: 'var(--text-secondary)' }}>
                        <div>{req.author || 'operator'}</div>
                        <div style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                          {new Date(req.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </div>
                      </td>

                      {/* Actions */}
                      <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                        <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                          {/* View Log / Terminal */}
                          <button
                            onClick={() => setViewingRequest(req)}
                            className="btn btn-secondary"
                            style={{ padding: '4px 10px', fontSize: '12px' }}
                            title="Inspect execution log and simulated CLI transcript"
                          >
                            <Terminal size={12} />
                            <span>Details</span>
                          </button>

                          {/* Execute (if Draft) */}
                          {req.status === 'DRAFT' && (
                            <button
                              onClick={() => handleExecuteRequest(req.id)}
                              disabled={isExecuting}
                              className="btn btn-primary"
                              style={{ padding: '4px 10px', fontSize: '12px' }}
                            >
                              <Play size={12} className={isExecuting ? 'spin' : ''} />
                              <span>{isExecuting ? 'Executing...' : 'Execute'}</span>
                            </button>
                          )}

                          {/* Rollback (if Completed) */}
                          {req.status === 'COMPLETED' && (
                            <button
                              onClick={() => handleRollbackRequest(req.id)}
                              disabled={isRollingBack}
                              className="btn btn-secondary"
                              style={{
                                padding: '4px 10px',
                                fontSize: '12px',
                                color: 'var(--status-unknown)',
                                borderColor: 'rgba(245, 158, 11, 0.4)',
                              }}
                              title="Revert configuration to pre-change snapshot"
                            >
                              <RotateCcw size={12} className={isRollingBack ? 'spin' : ''} />
                              <span>{isRollingBack ? 'Rolling back...' : 'Rollback'}</span>
                            </button>
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

      {/* ========================================================================= */}
      {/* MODAL 1: CREATE CHANGE REQUEST */}
      {/* ========================================================================= */}
      {isCreateModalOpen && (
        <div className="modal-overlay" onClick={() => setIsCreateModalOpen(false)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '750px', width: '90%', maxHeight: '90vh', overflowY: 'auto' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid var(--border-color)', paddingBottom: '12px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Terminal size={18} color="var(--accent-blue)" />
                <h3 style={{ fontSize: '16px', fontWeight: 600 }}>Stage Network Change Request</h3>
              </div>
              <button
                onClick={() => setIsCreateModalOpen(false)}
                style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateRequest}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px', marginBottom: '14px' }}>
                {/* Target Device */}
                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '6px' }}>
                    Target Network Device *
                  </label>
                  <select
                    className="form-input"
                    value={formData.deviceId}
                    onChange={(e) => setFormData({ ...formData, deviceId: Number(e.target.value) })}
                    required
                  >
                    {devices.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.hostname} ({d.managementIp}) — {d.vendor} {d.deviceType}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Author */}
                <div>
                  <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '6px' }}>
                    Operator / Author
                  </label>
                  <input
                    type="text"
                    className="form-input"
                    value={formData.author}
                    onChange={(e) => setFormData({ ...formData, author: e.target.value })}
                    required
                  />
                </div>
              </div>

              {/* Title */}
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '6px' }}>
                  Change Request Title *
                </label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Provision VLAN 100 for Engineering Subnet"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  required
                />
              </div>

              {/* Description */}
              <div style={{ marginBottom: '14px' }}>
                <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, marginBottom: '6px' }}>
                  Description / Change Reason
                </label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Approved ticket NET-4092: Add Layer 2/3 engineering boundary"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>

              {/* Dynamic Variables if Playbook is selected */}
              {selectedPlaybook && selectedPlaybook.variables.length > 0 && (
                <div
                  style={{
                    background: 'var(--bg-secondary)',
                    padding: '14px',
                    borderRadius: '8px',
                    border: '1px solid var(--border-color)',
                    marginBottom: '16px',
                  }}
                >
                  <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--accent-blue)', marginBottom: '10px' }}>
                    Playbook Parameters ({selectedPlaybook.name})
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '10px' }}>
                    {selectedPlaybook.variables.map((v) => (
                      <div key={v}>
                        <label style={{ display: 'block', fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                          {v}
                        </label>
                        <input
                          type="text"
                          className="form-input"
                          style={{ height: '32px', fontSize: '12px', fontFamily: 'var(--font-mono)' }}
                          value={variableValues[v] || ''}
                          onChange={(e) => handleVariableChange(v, e.target.value)}
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Commands Editor */}
              <div style={{ marginBottom: '16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                  <label style={{ fontSize: '12px', fontWeight: 600 }}>
                    Staged Configuration Commands *
                  </label>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>
                    Cisco IOS Directives (Educational Simulation)
                  </span>
                </div>
                <textarea
                  className="form-input"
                  rows={8}
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: '12px',
                    lineHeight: '1.45',
                    background: '#090d16',
                    color: '#e2e8f0',
                  }}
                  value={formData.configCommands}
                  onChange={(e) => setFormData({ ...formData, configCommands: e.target.value })}
                  required
                />
              </div>

              {/* Notice */}
              <div
                style={{
                  background: 'rgba(56, 189, 248, 0.06)',
                  border: '1px solid rgba(56, 189, 248, 0.2)',
                  borderRadius: '6px',
                  padding: '10px 14px',
                  fontSize: '11px',
                  color: 'var(--text-secondary)',
                  marginBottom: '18px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <ShieldCheck size={16} color="var(--accent-blue)" style={{ flexShrink: 0 }} />
                <span>
                  Staging creates a DRAFT record and snapshots the pre-change configuration version. No persistent configuration snapshots or device changes occur until you click Execute.
                </span>
              </div>

              {/* Modal Actions */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                <button
                  type="button"
                  onClick={() => setIsCreateModalOpen(false)}
                  className="btn btn-secondary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingCreate}
                  className="btn btn-primary"
                >
                  {submittingCreate ? 'Staging...' : 'Save Draft Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL 2: EXECUTION DETAILS & SIMULATED CLI TERMINAL */}
      {/* ========================================================================= */}
      {viewingRequest && (
        <div className="modal-overlay" onClick={() => setViewingRequest(null)}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: '850px', width: '92%', maxHeight: '90vh', overflowY: 'auto' }}
          >
            {/* Modal Header */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                borderBottom: '1px solid var(--border-color)',
                paddingBottom: '14px',
                marginBottom: '16px',
              }}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
                  <h3 style={{ fontSize: '16px', fontWeight: 600 }}>
                    Change Request #{viewingRequest.id}: {viewingRequest.title}
                  </h3>
                  {renderStatusBadge(viewingRequest.status)}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                  <span>Target: <strong>{viewingRequest.deviceHostname || `Device #${viewingRequest.deviceId}`}</strong></span>
                  <span>•</span>
                  <span>Author: <strong>{viewingRequest.author}</strong></span>
                  <span>•</span>
                  <span>Created: {new Date(viewingRequest.createdAt).toLocaleString()}</span>
                </div>
              </div>
              <button
                onClick={() => setViewingRequest(null)}
                style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer' }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Version Transition Bar */}
            <div
              style={{
                background: 'var(--bg-secondary)',
                borderRadius: '8px',
                padding: '12px 16px',
                marginBottom: '18px',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '12px',
                fontSize: '13px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ color: 'var(--text-muted)' }}>Configuration Version Transition:</span>
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    padding: '2px 8px',
                    borderRadius: '4px',
                    background: 'rgba(255, 255, 255, 0.05)',
                  }}
                >
                  v{viewingRequest.preChangeVersion ?? '—'}
                </span>
                <ArrowRight size={14} color="var(--accent-blue)" />
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    padding: '2px 8px',
                    borderRadius: '4px',
                    background: 'rgba(56, 189, 248, 0.15)',
                    color: 'var(--accent-blue)',
                    fontWeight: 700,
                  }}
                >
                  v{viewingRequest.postChangeVersion ?? 'Pending'}
                </span>
                {viewingRequest.rollbackVersion && (
                  <span
                    style={{
                      fontFamily: 'var(--font-mono)',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      background: 'rgba(245, 158, 11, 0.15)',
                      color: 'var(--status-unknown)',
                    }}
                  >
                    (Reverted to v{viewingRequest.rollbackVersion})
                  </span>
                )}
              </div>

              {viewingRequest.postChangeVersion && (
                <Link
                  to="/configuration"
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                    fontSize: '12px',
                    color: 'var(--accent-blue)',
                    textDecoration: 'none',
                  }}
                >
                  <span>View in Configuration Page</span>
                  <ExternalLink size={12} />
                </Link>
              )}
            </div>

            {/* Error Message if Failed */}
            {viewingRequest.errorMessage && (
              <div className="error-banner" style={{ marginBottom: '16px' }}>
                <AlertTriangle size={18} />
                <span>{viewingRequest.errorMessage}</span>
              </div>
            )}

            {/* Stepper Pipeline */}
            {viewingRequest.executionSteps && viewingRequest.executionSteps.length > 0 && (
              <div style={{ marginBottom: '20px' }}>
                <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '10px' }}>
                  Execution Pipeline Step Log
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {viewingRequest.executionSteps.map((step) => (
                    <div
                      key={step.stepNumber}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '10px',
                        padding: '10px 14px',
                        borderRadius: '6px',
                        background: step.status === 'PASSED' ? 'rgba(16, 185, 129, 0.05)' : 'rgba(239, 68, 68, 0.05)',
                        border: `1px solid ${step.status === 'PASSED' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`,
                      }}
                    >
                      {step.status === 'PASSED' ? (
                        <CheckCircle2 size={16} color="var(--status-up)" style={{ marginTop: '2px', flexShrink: 0 }} />
                      ) : (
                        <XCircle size={16} color="var(--status-down)" style={{ marginTop: '2px', flexShrink: 0 }} />
                      )}
                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                          <span style={{ fontSize: '12px', fontWeight: 700, fontFamily: 'var(--font-mono)' }}>
                            Step {step.stepNumber}: {step.stepName}
                          </span>
                          <span style={{ fontSize: '10px', color: 'var(--text-muted)' }}>
                            {new Date(step.timestamp).toLocaleTimeString()}
                          </span>
                        </div>
                        <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '2px' }}>
                          {step.details}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Simulated CLI Terminal Window */}
            {viewingRequest.terminalTranscript && (
              <div style={{ marginBottom: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                  <div style={{ fontSize: '13px', fontWeight: 600 }}>Simulated CLI Execution Session</div>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>Interactive Console Log</span>
                </div>

                <div
                  style={{
                    background: '#090d16',
                    border: '1px solid var(--border-color)',
                    borderRadius: '8px',
                    overflow: 'hidden',
                  }}
                >
                  <div
                    style={{
                      background: 'rgba(255, 255, 255, 0.03)',
                      padding: '6px 12px',
                      borderBottom: '1px solid var(--border-color)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                      fontSize: '11px',
                      color: 'var(--text-muted)',
                      fontFamily: 'var(--font-mono)',
                    }}
                  >
                    <span style={{ display: 'inline-flex', gap: '4px' }}>
                      <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#ef4444' }} />
                      <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#f59e0b' }} />
                      <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10b981' }} />
                    </span>
                    <span>cli-session@{viewingRequest.deviceHostname || 'target'}:~</span>
                  </div>

                  <pre
                    style={{
                      padding: '14px 16px',
                      margin: 0,
                      fontFamily: 'var(--font-mono)',
                      fontSize: '12px',
                      lineHeight: '1.5',
                      color: '#38bdf8',
                      whiteSpace: 'pre-wrap',
                      maxHeight: '320px',
                      overflowY: 'auto',
                    }}
                  >
                    {viewingRequest.terminalTranscript}
                  </pre>
                </div>
              </div>
            )}

            {/* Modal Actions */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--border-color)', paddingTop: '14px' }}>
              <div>
                {viewingRequest.status === 'COMPLETED' && (
                  <button
                    onClick={() => handleRollbackRequest(viewingRequest.id)}
                    disabled={rollingBackId === viewingRequest.id}
                    className="btn btn-secondary"
                    style={{
                      color: 'var(--status-unknown)',
                      borderColor: 'rgba(245, 158, 11, 0.4)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                    }}
                  >
                    <RotateCcw size={13} className={rollingBackId === viewingRequest.id ? 'spin' : ''} />
                    <span>{rollingBackId === viewingRequest.id ? 'Rolling back...' : 'Rollback to Pre-Change State'}</span>
                  </button>
                )}

                {viewingRequest.status === 'DRAFT' && (
                  <button
                    onClick={() => handleExecuteRequest(viewingRequest.id)}
                    disabled={executingId === viewingRequest.id}
                    className="btn btn-primary"
                    style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
                  >
                    <Play size={13} className={executingId === viewingRequest.id ? 'spin' : ''} />
                    <span>{executingId === viewingRequest.id ? 'Executing...' : 'Execute Change Request'}</span>
                  </button>
                )}
              </div>

              <button onClick={() => setViewingRequest(null)} className="btn btn-secondary">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
