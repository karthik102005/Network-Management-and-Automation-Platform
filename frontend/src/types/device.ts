export type DeviceType =
  | 'ROUTER'
  | 'SWITCH'
  | 'FIREWALL'
  | 'ACCESS_POINT'
  | 'SERVER'
  | 'GATEWAY'
  | 'LOAD_BALANCER'
  | 'OTHER';

export type DeviceVendor =
  | 'CISCO'
  | 'JUNIPER'
  | 'ARISTA'
  | 'MIKROTIK'
  | 'HUAWEI'
  | 'FORTINET'
  | 'LINUX'
  | 'GENERIC';

export type DeviceStatus = 'UP' | 'DOWN' | 'UNREACHABLE' | 'MAINTENANCE' | 'UNKNOWN';

export interface NetworkDevice {
  id: number;
  hostname: string;
  managementIp: string;
  deviceType: DeviceType;
  vendor: DeviceVendor;
  status: DeviceStatus;
  description?: string;
  createdAt: string;
  updatedAt: string;
  lastSeen?: string;
}

export interface DeviceFormData {
  hostname: string;
  managementIp: string;
  deviceType: DeviceType;
  vendor: DeviceVendor;
  status: DeviceStatus;
  description?: string;
}

// Network Interface Types
export type InterfaceType =
  | 'ETHERNET'
  | 'FAST_ETHERNET'
  | 'GIGABIT_ETHERNET'
  | 'TEN_GIGABIT_ETHERNET'
  | 'LOOPBACK'
  | 'VLAN'
  | 'SERIAL'
  | 'MANAGEMENT'
  | 'WIRELESS'
  | 'OTHER';

export type AdminStatus = 'UP' | 'DOWN' | 'TESTING';

export type OperationalStatus =
  | 'UP'
  | 'DOWN'
  | 'TESTING'
  | 'UNKNOWN'
  | 'DORMANT'
  | 'NOT_PRESENT'
  | 'LOWER_LAYER_DOWN';

export interface NetworkInterface {
  id: number;
  deviceId: number;
  deviceHostname?: string;
  interfaceName: string;
  interfaceType: InterfaceType;
  ipAddress?: string;
  subnetPrefix?: number;
  macAddress?: string;
  adminStatus: AdminStatus;
  operationalStatus: OperationalStatus;
  speedMbps?: number;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface InterfaceFormData {
  deviceId: number;
  interfaceName: string;
  interfaceType: InterfaceType;
  ipAddress?: string;
  subnetPrefix?: number;
  macAddress?: string;
  adminStatus: AdminStatus;
  operationalStatus: OperationalStatus;
  speedMbps?: number;
  description?: string;
}

// Network Link Types
export type LinkStatus = 'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN';

export type LinkType =
  | 'ETHERNET'
  | 'FIBER'
  | 'SERIAL'
  | 'VIRTUAL'
  | 'AGGREGATE'
  | 'WIRELESS'
  | 'OTHER';

export interface NetworkLink {
  id: number;
  sourceInterfaceId: number;
  sourceInterfaceName: string;
  sourceDeviceId: number;
  sourceHostname: string;
  destinationInterfaceId: number;
  destinationInterfaceName: string;
  destinationDeviceId: number;
  destinationHostname: string;
  status: LinkStatus;
  linkType: LinkType;
  bandwidthMbps?: number;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LinkFormData {
  sourceInterfaceId: number;
  destinationInterfaceId: number;
  status: LinkStatus;
  linkType: LinkType;
  bandwidthMbps?: number;
  description?: string;
}

// Topology Types
export interface TopologyNode {
  id: number;
  hostname: string;
  managementIp: string;
  deviceType: DeviceType;
  vendor: DeviceVendor;
  status: DeviceStatus;
  interfaceCount: number;
}

export interface TopologyLink {
  id: number;
  sourceDeviceId: number;
  sourceHostname: string;
  sourceInterfaceId: number;
  sourceInterfaceName: string;
  destinationDeviceId: number;
  destinationHostname: string;
  destinationInterfaceId: number;
  destinationInterfaceName: string;
  status: LinkStatus;
  linkType: LinkType;
  bandwidthMbps?: number;
}

export interface TopologyData {
  nodes: TopologyNode[];
  links: TopologyLink[];
  totalNodes: number;
  totalLinks: number;
}

export interface BackendHealth {
  status: string;
  application: string;
  version: string;
  timestamp: string;
  uptimeMillis: number;
  details?: {
    javaVersion?: string;
    javaVendor?: string;
    osName?: string;
    availableProcessors?: number;
    freeMemoryBytes?: number;
    totalMemoryBytes?: number;
  };
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

export interface DeviceReachabilityResult {
  deviceId: number;
  hostname: string;
  managementIp: string;
  reachable: boolean;
  reachabilityStatus: 'REACHABLE' | 'UNREACHABLE' | 'INVALID_TARGET' | 'INCONCLUSIVE';
  probeMethod: 'ICMP_ECHO' | 'TCP_CONNECTION' | 'TCP_RESET' | 'NONE';
  port?: number;
  responseTimeMs?: number;
  checkedAt: string;
  details: string;
}

export type AlertSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
export type AlertStatus = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';
export type AlertType = 'DEVICE_UNREACHABLE' | 'INTERFACE_DOWN' | 'LINK_DOWN' | 'HIGH_LATENCY' | 'SYSTEM';

export interface NetworkAlert {
  id: number;
  deviceId?: number;
  deviceHostname?: string;
  deviceIp?: string;
  alertType: AlertType;
  severity: AlertSeverity;
  status: AlertStatus;
  message: string;
  source: string;
  createdAt: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
}

// Configuration Management Types
export type ConfigFormat = 'CISCO_IOS' | 'JUNOS' | 'JSON' | 'TEXT';

export interface DeviceConfiguration {
  id: number;
  deviceId: number;
  deviceHostname?: string;
  version: number;
  configText: string;
  configFormat: ConfigFormat;
  checksum: string;
  active: boolean;
  author?: string;
  description?: string;
  createdAt: string;
}

export interface ConfigurationRequestData {
  configText: string;
  configFormat: ConfigFormat;
  author?: string;
  description?: string;
}

export type DiffLineType = 'ADDED' | 'REMOVED' | 'UNCHANGED';

export interface ConfigurationDiffLine {
  type: DiffLineType;
  oldLineNumber?: number;
  newLineNumber?: number;
  content: string;
}

export interface ConfigurationDiff {
  deviceId: number;
  deviceHostname?: string;
  v1: number;
  v2: number;
  identical: boolean;
  totalLinesV1: number;
  totalLinesV2: number;
  addedCount: number;
  removedCount: number;
  unchangedCount: number;
  diffLines: ConfigurationDiffLine[];
}

export interface ConfigurationTemplate {
  id: string;
  name: string;
  vendor: string;
  deviceType: string;
  format: ConfigFormat;
  description: string;
  templateText: string;
  exampleWarning: string;
  variables: string[];
}

// Simulated Device Metrics Types
export type DeviceHealthState = 'HEALTHY' | 'WARNING' | 'DEGRADED';

export interface InterfaceMetrics {
  interfaceName: string;
  inboundMbps: number;
  outboundMbps: number;
  capacityMbps: number;
  utilizationPercent: number;
}

export interface DeviceMetrics {
  deviceId: number;
  deviceHostname: string;
  timestamp: string;
  cpuUtilizationPercent: number;
  memoryUtilizationPercent: number;
  latencyMs: number;
  packetLossPercent: number;
  healthState: DeviceHealthState;
  interfaceMetrics: InterfaceMetrics[];
  simulated: boolean;
  telemetrySource: string;
  disclaimer: string;
}

export interface DeviceMetricsHistoryPoint {
  timestamp: string;
  cpuPercent: number;
  memoryPercent: number;
  latencyMs: number;
  inboundMbps: number;
  outboundMbps: number;
}

// Network Automation Types
export type AutomationStatus =
  | 'DRAFT'
  | 'VALIDATING'
  | 'EXECUTING'
  | 'COMPLETED'
  | 'FAILED'
  | 'ROLLED_BACK';

export interface AutomationPlaybook {
  id: string;
  name: string;
  category: string;
  vendor: string;
  description: string;
  variables: string[];
  commandTemplate: string;
}

export interface AutomationExecutionStep {
  stepNumber: number;
  stepName: string;
  status: 'PASSED' | 'FAILED' | 'SKIPPED';
  details: string;
  timestamp: string;
}

export interface AutomationChangeRequest {
  id: number;
  deviceId: number;
  deviceHostname?: string;
  title: string;
  description?: string;
  playbookId?: string;
  status: AutomationStatus;
  configCommands: string;
  preChangeVersion?: number;
  postChangeVersion?: number;
  rollbackVersion?: number;
  author?: string;
  executionSteps: AutomationExecutionStep[];
  terminalTranscript?: string;
  errorMessage?: string;
  simulated: boolean;
  disclaimer: string;
  createdAt: string;
  executedAt?: string;
  completedAt?: string;
}

export interface AutomationChangeRequestCreateData {
  deviceId: number;
  title: string;
  description?: string;
  playbookId?: string;
  configCommands: string;
  author?: string;
}
