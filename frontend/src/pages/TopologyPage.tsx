import React, { useEffect, useState, useRef, useCallback } from 'react';
import { apiService } from '../services/api';
import {
  DeviceStatus,
  LinkFormData,
  LinkStatus,
  NetworkDevice,
  TopologyData,
  TopologyLink,
  TopologyNode,
} from '../types/device';
import { StatusBadge } from '../components/StatusBadge';
import { LinkModal } from '../components/LinkModal';
import { DeviceModal } from '../components/DeviceModal';
import {
  Network,
  GitBranch,
  RefreshCw,
  Plus,
  Server,
  Layers,
  Trash2,
  X,
  ExternalLink,
  Info,
  Maximize2,
  AlertTriangle,
} from 'lucide-react';

interface NodePosition {
  x: number;
  y: number;
}

export const TopologyPage: React.FC = () => {
  const [topologyData, setTopologyData] = useState<TopologyData>({
    nodes: [],
    links: [],
    totalNodes: 0,
    totalLinks: 0,
  });
  const [devices, setDevices] = useState<NetworkDevice[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Selection
  const [selectedNode, setSelectedNode] = useState<TopologyNode | null>(null);
  const [selectedLink, setSelectedLink] = useState<TopologyLink | null>(null);

  // Modals
  const [isLinkModalOpen, setIsLinkModalOpen] = useState(false);
  const [isDeviceModalOpen, setIsDeviceModalOpen] = useState(false);
  const [deviceModalMode, setDeviceModalMode] = useState<'VIEW' | 'CREATE'>('VIEW');
  const [deviceForModal, setDeviceForModal] = useState<NetworkDevice | null>(null);

  // Canvas & Drag State
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [positions, setPositions] = useState<Record<number, NodePosition>>({});
  const [draggingNodeId, setDraggingNodeId] = useState<number | null>(null);
  const [dragOffset, setDragOffset] = useState<{ x: number; y: number }>({ x: 0, y: 0 });

  // Load topology and device inventory
  const fetchTopology = async () => {
    setLoading(true);
    setError(null);
    try {
      const [topData, devList] = await Promise.all([
        apiService.getTopology(),
        apiService.getDevices(),
      ]);
      setTopologyData(topData);
      setDevices(devList);

      // Keep positions for existing nodes, compute layout for new nodes
      setPositions((prev) => calculateLayout(topData.nodes, prev));
    } catch (err: any) {
      setError(err.message || 'Failed to load network topology graph.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTopology();
  }, []);

  // Compute circular or grid layout for nodes
  const calculateLayout = (
    nodes: TopologyNode[],
    currentPositions: Record<number, NodePosition> = {}
  ): Record<number, NodePosition> => {
    const newPositions: Record<number, NodePosition> = { ...currentPositions };
    const unpositionedNodes = nodes.filter((n) => !newPositions[n.id]);

    if (unpositionedNodes.length === 0) return newPositions;

    const centerX = 420;
    const centerY = 280;
    const radius = Math.min(220, Math.max(140, nodes.length * 40));

    nodes.forEach((node, index) => {
      if (!newPositions[node.id]) {
        if (nodes.length === 1) {
          newPositions[node.id] = { x: centerX, y: centerY };
        } else {
          const angle = (2 * Math.PI * index) / nodes.length - Math.PI / 2;
          newPositions[node.id] = {
            x: Math.round(centerX + radius * Math.cos(angle)),
            y: Math.round(centerY + radius * Math.sin(angle)),
          };
        }
      }
    });

    return newPositions;
  };

  const handleResetLayout = () => {
    if (topologyData.nodes.length === 0) return;
    const centerX = 420;
    const centerY = 280;
    const radius = Math.min(220, Math.max(140, topologyData.nodes.length * 40));
    const resetPositions: Record<number, NodePosition> = {};

    topologyData.nodes.forEach((node, index) => {
      if (topologyData.nodes.length === 1) {
        resetPositions[node.id] = { x: centerX, y: centerY };
      } else {
        const angle = (2 * Math.PI * index) / topologyData.nodes.length - Math.PI / 2;
        resetPositions[node.id] = {
          x: Math.round(centerX + radius * Math.cos(angle)),
          y: Math.round(centerY + radius * Math.sin(angle)),
        };
      }
    });

    setPositions(resetPositions);
  };

  // Node Drag Handlers
  const handleNodeMouseDown = (e: React.MouseEvent, nodeId: number) => {
    e.stopPropagation();
    if (!svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    const nodePos = positions[nodeId] || { x: 0, y: 0 };
    setDraggingNodeId(nodeId);
    setDragOffset({
      x: e.clientX - rect.left - nodePos.x,
      y: e.clientY - rect.top - nodePos.y,
    });
  };

  const handleMouseMove = useCallback(
    (e: React.MouseEvent<SVGSVGElement>) => {
      if (draggingNodeId !== null && svgRef.current) {
        const rect = svgRef.current.getBoundingClientRect();
        const mouseX = e.clientX - rect.left - dragOffset.x;
        const mouseY = e.clientY - rect.top - dragOffset.y;

        // Keep inside bounds
        const boundedX = Math.max(50, Math.min(rect.width - 50, mouseX));
        const boundedY = Math.max(50, Math.min(rect.height - 50, mouseY));

        setPositions((prev) => ({
          ...prev,
          [draggingNodeId]: { x: boundedX, y: boundedY },
        }));
      }
    },
    [draggingNodeId, dragOffset]
  );

  const handleMouseUp = () => {
    setDraggingNodeId(null);
  };

  // Link deletion
  const handleDeleteLink = async (linkId: number) => {
    if (window.confirm('Are you sure you want to delete this network link connection?')) {
      try {
        await apiService.deleteLink(linkId);
        setSelectedLink(null);
        await fetchTopology();
      } catch (err: any) {
        alert(err.message || 'Failed to delete network link.');
      }
    }
  };

  // Open device modal for selected node
  const handleOpenDeviceDetails = (node: TopologyNode) => {
    const fullDevice = devices.find((d) => d.id === node.id);
    if (fullDevice) {
      setDeviceForModal(fullDevice);
      setDeviceModalMode('VIEW');
      setIsDeviceModalOpen(true);
    }
  };

  // Link status color mapping
  const getLinkColor = (status: LinkStatus): string => {
    switch (status) {
      case 'UP':
        return '#10b981'; // green
      case 'DOWN':
        return '#ef4444'; // red
      case 'DEGRADED':
        return '#f59e0b'; // amber
      case 'UNKNOWN':
      default:
        return '#64748b'; // slate
    }
  };

  const getNodeBorderColor = (status: DeviceStatus): string => {
    switch (status) {
      case 'UP':
        return '#10b981';
      case 'DOWN':
      case 'UNREACHABLE':
        return '#ef4444';
      case 'MAINTENANCE':
        return '#f59e0b';
      case 'UNKNOWN':
      default:
        return '#64748b';
    }
  };

  // Connected links for selected node
  const connectedLinks = selectedNode
    ? topologyData.links.filter(
        (l) => l.sourceDeviceId === selectedNode.id || l.destinationDeviceId === selectedNode.id
      )
    : [];

  return (
    <div>
      {/* Page Header */}
      <div className="page-header">
        <div>
          <h2 className="page-title">Network Topology Map</h2>
          <p className="page-description">
            Interactive Layer 2 / Layer 3 dynamic topology graph and link telemetry
          </p>
        </div>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button onClick={fetchTopology} className="btn btn-secondary" title="Refresh topology data">
            <RefreshCw size={14} />
            <span>Refresh</span>
          </button>
          <button onClick={() => setIsLinkModalOpen(true)} className="btn btn-primary">
            <GitBranch size={16} />
            <span>Create Link</span>
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner" role="alert" style={{ marginBottom: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <AlertTriangle size={18} />
            <span>{error}</span>
          </div>
          <button onClick={fetchTopology} className="btn btn-secondary" style={{ padding: '4px 10px' }}>
            Retry
          </button>
        </div>
      )}

      {/* Main Layout Container */}
      <div className="topology-container">
        {/* SVG Canvas Area */}
        <div className="topology-canvas-wrapper">
          {/* Canvas Toolbar */}
          <div className="topology-toolbar">
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontWeight: 600 }}>
                <Layers size={15} color="var(--accent-blue)" />
                <span>Topology Mesh</span>
              </div>
              <span style={{ color: 'var(--text-muted)', fontSize: '12px' }}>
                Nodes: <strong style={{ color: 'var(--text-primary)' }}>{topologyData.totalNodes}</strong> | Links:{' '}
                <strong style={{ color: 'var(--text-primary)' }}>{topologyData.totalLinks}</strong>
              </span>
            </div>

            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                onClick={handleResetLayout}
                className="btn btn-secondary"
                style={{ padding: '4px 10px', fontSize: '12px' }}
                title="Reset node layout positions"
              >
                <Maximize2 size={13} />
                <span>Reset Layout</span>
              </button>
            </div>
          </div>

          {/* SVG Graph Viewport */}
          {loading ? (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--text-muted)' }}>
              Loading network topology graph...
            </div>
          ) : topologyData.nodes.length === 0 ? (
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100%',
                padding: '40px',
                textAlign: 'center',
              }}
            >
              <Network size={48} color="var(--text-muted)" style={{ marginBottom: '16px' }} />
              <div style={{ fontSize: '16px', fontWeight: 600, color: 'var(--text-primary)' }}>
                No Network Devices Registered
              </div>
              <p style={{ maxWidth: '400px', fontSize: '13px', color: 'var(--text-secondary)', marginTop: '6px', marginBottom: '20px' }}>
                Register devices into the inventory to visualize nodes, ports, and interconnecting links in this topology map.
              </p>
              <button
                type="button"
                onClick={() => {
                  setDeviceForModal(null);
                  setDeviceModalMode('CREATE');
                  setIsDeviceModalOpen(true);
                }}
                className="btn btn-primary"
              >
                <Plus size={16} />
                <span>Add First Device</span>
              </button>
            </div>
          ) : (
            <svg
              ref={svgRef}
              className="topology-svg"
              onMouseMove={handleMouseMove}
              onMouseUp={handleMouseUp}
              onClick={() => {
                setSelectedNode(null);
                setSelectedLink(null);
              }}
            >
              {/* SVG Definitions */}
              <defs>
                <pattern id="topo-grid" width="36" height="36" patternUnits="userSpaceOnUse">
                  <path d="M 36 0 L 0 0 0 36" fill="none" stroke="rgba(255, 255, 255, 0.04)" strokeWidth="1" />
                </pattern>
                <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
                  <feGaussianBlur stdDeviation="3" result="blur" />
                  <feComposite in="SourceGraphic" in2="blur" operator="over" />
                </filter>
              </defs>

              {/* Background Grid */}
              <rect width="100%" height="100%" fill="url(#topo-grid)" />

              {/* Render Links (Edges) */}
              <g className="topology-links-layer">
                {topologyData.links.map((link) => {
                  const srcPos = positions[link.sourceDeviceId];
                  const dstPos = positions[link.destinationDeviceId];
                  if (!srcPos || !dstPos) return null;

                  const isSelected = selectedLink?.id === link.id;
                  const isNodeConnected =
                    selectedNode &&
                    (link.sourceDeviceId === selectedNode.id || link.destinationDeviceId === selectedNode.id);

                  const strokeColor = getLinkColor(link.status);
                  const isDashed = link.status === 'DOWN' || link.status === 'DEGRADED';
                  const midX = (srcPos.x + dstPos.x) / 2;
                  const midY = (srcPos.y + dstPos.y) / 2;

                  return (
                    <g
                      key={`link-${link.id}`}
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedLink(link);
                        setSelectedNode(null);
                      }}
                      style={{ cursor: 'pointer' }}
                    >
                      {/* Invisible thicker line for easier clicking */}
                      <line
                        x1={srcPos.x}
                        y1={srcPos.y}
                        x2={dstPos.x}
                        y2={dstPos.y}
                        stroke="transparent"
                        strokeWidth={16}
                      />

                      {/* Visible Link Line */}
                      <line
                        x1={srcPos.x}
                        y1={srcPos.y}
                        x2={dstPos.x}
                        y2={dstPos.y}
                        stroke={strokeColor}
                        strokeWidth={isSelected ? 4 : isNodeConnected ? 3 : 2.5}
                        strokeDasharray={isDashed ? '6 4' : undefined}
                        filter={isSelected || isNodeConnected ? 'url(#glow)' : undefined}
                      />

                      {/* Link Bandwidth / Medium Badge */}
                      <g transform={`translate(${midX}, ${midY})`}>
                        <rect
                          x={-24}
                          y={-10}
                          width={48}
                          height={20}
                          rx={10}
                          fill="#0f172a"
                          stroke={isSelected ? 'var(--accent-blue)' : strokeColor}
                          strokeWidth={1}
                        />
                        <text
                          x={0}
                          y={3}
                          fill="var(--text-primary)"
                          fontSize="9"
                          fontFamily="monospace"
                          fontWeight="600"
                          textAnchor="middle"
                        >
                          {link.bandwidthMbps ? `${link.bandwidthMbps}M` : link.linkType.slice(0, 4)}
                        </text>
                      </g>
                    </g>
                  );
                })}
              </g>

              {/* Render Nodes */}
              <g className="topology-nodes-layer">
                {topologyData.nodes.map((node) => {
                  const pos = positions[node.id] || { x: 100, y: 100 };
                  const isSelected = selectedNode?.id === node.id;
                  const borderColor = getNodeBorderColor(node.status);

                  return (
                    <g
                      key={`node-${node.id}`}
                      transform={`translate(${pos.x}, ${pos.y})`}
                      className="topology-node"
                      onMouseDown={(e) => handleNodeMouseDown(e, node.id)}
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedNode(node);
                        setSelectedLink(null);
                      }}
                    >
                      {/* Selection Halo */}
                      {isSelected && (
                        <circle
                          r={38}
                          fill="none"
                          stroke="var(--accent-blue)"
                          strokeWidth={2.5}
                          strokeDasharray="4 3"
                          filter="url(#glow)"
                        />
                      )}

                      {/* Node Outer Circle */}
                      <circle
                        r={28}
                        fill="#1e293b"
                        stroke={borderColor}
                        strokeWidth={isSelected ? 3 : 2}
                        filter="drop-shadow(0 4px 6px rgba(0,0,0,0.4))"
                      />

                      {/* Node Inner Symbol / Type Abbreviation */}
                      <text
                        x={0}
                        y={5}
                        fill="var(--text-primary)"
                        fontSize="13"
                        fontWeight="700"
                        fontFamily="monospace"
                        textAnchor="middle"
                        style={{ pointerEvents: 'none' }}
                      >
                        {node.deviceType === 'ROUTER'
                          ? 'RTR'
                          : node.deviceType === 'SWITCH'
                          ? 'SW'
                          : node.deviceType === 'FIREWALL'
                          ? 'FW'
                          : 'DEV'}
                      </text>

                      {/* Status indicator dot */}
                      <circle
                        cx={20}
                        cy={-20}
                        r={6}
                        fill={borderColor}
                        stroke="#0f172a"
                        strokeWidth={2}
                      />

                      {/* Hostname Label */}
                      <text
                        x={0}
                        y={46}
                        fill="var(--text-primary)"
                        fontSize="12"
                        fontWeight="600"
                        fontFamily="monospace"
                        textAnchor="middle"
                        style={{ pointerEvents: 'none' }}
                      >
                        {node.hostname}
                      </text>

                      {/* IP Sub-Label */}
                      <text
                        x={0}
                        y={60}
                        fill="var(--accent-blue)"
                        fontSize="10"
                        fontFamily="monospace"
                        textAnchor="middle"
                        style={{ pointerEvents: 'none' }}
                      >
                        {node.managementIp}
                      </text>

                      {/* Interface Count Badge */}
                      <g transform="translate(0, 72)">
                        <rect
                          x={-28}
                          y={-7}
                          width={56}
                          height={14}
                          rx={7}
                          fill="#0f172a"
                          stroke="var(--border-color)"
                          strokeWidth={1}
                        />
                        <text
                          x={0}
                          y={3}
                          fill="var(--text-muted)"
                          fontSize="8"
                          fontWeight="500"
                          textAnchor="middle"
                          style={{ pointerEvents: 'none' }}
                        >
                          {node.interfaceCount} {node.interfaceCount === 1 ? 'iface' : 'ifaces'}
                        </text>
                      </g>
                    </g>
                  );
                })}
              </g>
            </svg>
          )}
        </div>

        {/* Details & Telemetry Sidebar */}
        <div className="topology-sidebar">
          {selectedNode ? (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Server size={18} color="var(--accent-blue)" />
                  <span style={{ fontSize: '15px', fontWeight: 600 }}>Node Telemetry</span>
                </div>
                <button onClick={() => setSelectedNode(null)} className="btn-icon" title="Close details">
                  <X size={16} />
                </button>
              </div>

              <div style={{ backgroundColor: 'var(--bg-secondary)', padding: '14px', borderRadius: '8px', marginBottom: '16px' }}>
                <div style={{ fontSize: '16px', fontWeight: 700, fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>
                  {selectedNode.hostname}
                </div>
                <div style={{ fontSize: '13px', color: 'var(--accent-blue)', fontFamily: 'var(--font-mono)', marginTop: '2px' }}>
                  {selectedNode.managementIp}
                </div>
                <div style={{ marginTop: '10px' }}>
                  <StatusBadge status={selectedNode.status} />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '16px' }}>
                <div>
                  <span className="form-label">Type</span>
                  <div style={{ fontSize: '13px', fontWeight: 500 }}>{selectedNode.deviceType}</div>
                </div>
                <div>
                  <span className="form-label">Vendor</span>
                  <div style={{ fontSize: '13px', fontWeight: 500 }}>{selectedNode.vendor}</div>
                </div>
                <div>
                  <span className="form-label">Interfaces</span>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--accent-blue)' }}>
                    {selectedNode.interfaceCount} configured
                  </div>
                </div>
                <div>
                  <span className="form-label">Connected Links</span>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--status-up)' }}>
                    {connectedLinks.length} active
                  </div>
                </div>
              </div>

              {/* Connected Links List */}
              <div style={{ marginBottom: '20px' }}>
                <span className="form-label" style={{ marginBottom: '8px' }}>
                  Connected Links
                </span>
                {connectedLinks.length === 0 ? (
                  <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                    No links connected to this device.
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                    {connectedLinks.map((link) => {
                      const peerHostname =
                        link.sourceDeviceId === selectedNode.id
                          ? link.destinationHostname
                          : link.sourceHostname;
                      const localIface =
                        link.sourceDeviceId === selectedNode.id
                          ? link.sourceInterfaceName
                          : link.destinationInterfaceName;
                      const peerIface =
                        link.sourceDeviceId === selectedNode.id
                          ? link.destinationInterfaceName
                          : link.sourceInterfaceName;

                      return (
                        <div
                          key={link.id}
                          style={{
                            padding: '10px',
                            backgroundColor: 'var(--bg-secondary)',
                            borderRadius: '6px',
                            border: '1px solid var(--border-color)',
                            fontSize: '12px',
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                              Peer: {peerHostname}
                            </span>
                            <span
                              className={`badge ${
                                link.status === 'UP' ? 'badge-online' : 'badge-offline'
                              }`}
                              style={{ fontSize: '9px', padding: '1px 6px' }}
                            >
                              {link.status}
                            </span>
                          </div>
                          <div style={{ color: 'var(--text-muted)', fontSize: '11px', marginTop: '4px', fontFamily: 'var(--font-mono)' }}>
                            {localIface} &harr; {peerIface}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              <button
                type="button"
                onClick={() => handleOpenDeviceDetails(selectedNode)}
                className="btn btn-primary"
                style={{ width: '100%', justifyContent: 'center' }}
              >
                <ExternalLink size={14} />
                <span>Manage Ports & Details</span>
              </button>
            </div>
          ) : selectedLink ? (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <GitBranch size={18} color="var(--accent-blue)" />
                  <span style={{ fontSize: '15px', fontWeight: 600 }}>Link Telemetry</span>
                </div>
                <button onClick={() => setSelectedLink(null)} className="btn-icon" title="Close details">
                  <X size={16} />
                </button>
              </div>

              {/* Endpoint A */}
              <div
                style={{
                  backgroundColor: 'var(--bg-secondary)',
                  padding: '12px',
                  borderRadius: '6px',
                  marginBottom: '10px',
                  border: '1px solid var(--border-color)',
                }}
              >
                <span className="form-label" style={{ fontSize: '10px', color: 'var(--accent-blue)' }}>
                  Endpoint A (Source)
                </span>
                <div style={{ fontWeight: 600, fontFamily: 'var(--font-mono)', fontSize: '13px' }}>
                  {selectedLink.sourceHostname}
                </div>
                <div style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                  Port: {selectedLink.sourceInterfaceName}
                </div>
              </div>

              {/* Endpoint B */}
              <div
                style={{
                  backgroundColor: 'var(--bg-secondary)',
                  padding: '12px',
                  borderRadius: '6px',
                  marginBottom: '16px',
                  border: '1px solid var(--border-color)',
                }}
              >
                <span className="form-label" style={{ fontSize: '10px', color: 'var(--status-up)' }}>
                  Endpoint B (Destination)
                </span>
                <div style={{ fontWeight: 600, fontFamily: 'var(--font-mono)', fontSize: '13px' }}>
                  {selectedLink.destinationHostname}
                </div>
                <div style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-mono)', fontSize: '12px' }}>
                  Port: {selectedLink.destinationInterfaceName}
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '16px' }}>
                <div>
                  <span className="form-label">Link Type</span>
                  <div style={{ fontSize: '13px', fontWeight: 500 }}>{selectedLink.linkType}</div>
                </div>
                <div>
                  <span className="form-label">Status</span>
                  <div>
                    <span
                      className={`badge ${
                        selectedLink.status === 'UP' ? 'badge-online' : 'badge-offline'
                      }`}
                      style={{ fontSize: '10px', padding: '2px 8px' }}
                    >
                      {selectedLink.status}
                    </span>
                  </div>
                </div>
                <div>
                  <span className="form-label">Bandwidth</span>
                  <div style={{ fontSize: '13px', fontFamily: 'var(--font-mono)', fontWeight: 600 }}>
                    {selectedLink.bandwidthMbps ? `${selectedLink.bandwidthMbps} Mbps` : 'Unspecified'}
                  </div>
                </div>
              </div>

              <button
                type="button"
                onClick={() => handleDeleteLink(selectedLink.id)}
                className="btn btn-secondary danger"
                style={{ width: '100%', justifyContent: 'center', marginTop: '16px' }}
              >
                <Trash2 size={14} />
                <span>Delete Link</span>
              </button>
            </div>
          ) : (
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '14px' }}>
                <Info size={18} color="var(--accent-blue)" />
                <span style={{ fontSize: '15px', fontWeight: 600 }}>Network Mesh Overview</span>
              </div>

              <p style={{ fontSize: '13px', color: 'var(--text-secondary)', lineHeight: 1.6, marginBottom: '20px' }}>
                Click any device node to inspect configured interfaces, check link connectivity, or manage device ports.
              </p>

              <div
                style={{
                  backgroundColor: 'var(--bg-secondary)',
                  padding: '16px',
                  borderRadius: '8px',
                  border: '1px solid var(--border-color)',
                  marginBottom: '20px',
                }}
              >
                <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '10px' }}>
                  CANVAS CONTROLS
                </div>
                <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '12px', color: 'var(--text-secondary)', lineHeight: 1.8 }}>
                  <li>Click and drag nodes to arrange topology map</li>
                  <li>Click any link to inspect bandwidth & status</li>
                  <li>Click &quot;Create Link&quot; to bridge two interfaces</li>
                  <li>Use &quot;Reset Layout&quot; to restore circular mesh</li>
                </ul>
              </div>

              <div
                style={{
                  backgroundColor: 'rgba(56, 189, 248, 0.05)',
                  border: '1px solid rgba(56, 189, 248, 0.15)',
                  borderRadius: '8px',
                  padding: '14px',
                  fontSize: '12px',
                  color: 'var(--text-secondary)',
                }}
              >
                <div style={{ fontWeight: 600, color: 'var(--accent-blue)', marginBottom: '4px' }}>
                  Live Link Telemetry
                </div>
                <span>
                  Green solid lines indicate active UP links. Red or amber dashed lines denote DOWN or degraded links.
                </span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Link Creation Modal */}
      <LinkModal
        isOpen={isLinkModalOpen}
        onClose={() => setIsLinkModalOpen(false)}
        devices={devices}
        onSubmit={async (linkData: LinkFormData) => {
          await apiService.createLink(linkData);
          await fetchTopology();
        }}
      />

      {/* Device View & Port Management Modal */}
      <DeviceModal
        isOpen={isDeviceModalOpen}
        mode={deviceModalMode}
        device={deviceForModal}
        onClose={() => setIsDeviceModalOpen(false)}
        onSubmit={async (formData) => {
          if (deviceModalMode === 'CREATE') {
            await apiService.createDevice(formData);
          }
          await fetchTopology();
        }}
        onDeviceUpdated={fetchTopology}
      />
    </div>
  );
};
