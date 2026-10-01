import {
  BackendHealth,
  DeviceFormData,
  NetworkDevice,
  NetworkInterface,
  InterfaceFormData,
  NetworkLink,
  LinkFormData,
  TopologyData,
  ApiErrorResponse,
  DeviceReachabilityResult,
  NetworkAlert,
  AlertStatus,
  AlertSeverity,
  DeviceConfiguration,
  ConfigurationRequestData,
  ConfigurationDiff,
  ConfigurationTemplate,
  DeviceMetrics,
  DeviceMetricsHistoryPoint,
  AutomationPlaybook,
  AutomationChangeRequest,
  AutomationChangeRequestCreateData,
} from '../types/device';

const resolveApiBase = (): string => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (!envUrl || typeof envUrl !== 'string' || !envUrl.trim()) {
    return '/api';
  }
  const cleanUrl = envUrl.trim().replace(/\/+$/, '');
  return cleanUrl.endsWith('/api') ? cleanUrl : `${cleanUrl}/api`;
};

const API_BASE = resolveApiBase();

export class ApiError extends Error {
  public status?: number;
  public validationErrors?: Record<string, string>;

  constructor(message: string, status?: number, validationErrors?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.validationErrors = validationErrors;
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return {} as T;
  }

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');

  if (!response.ok) {
    let errorMessage = `HTTP error ${response.status}: ${response.statusText}`;
    let validationErrors: Record<string, string> | undefined;

    if (isJson) {
      try {
        const errorData = (await response.json()) as ApiErrorResponse;
        errorMessage = errorData.message || errorMessage;
        validationErrors = errorData.validationErrors;
      } catch {
        // Fall back to status text
      }
    }

    throw new ApiError(errorMessage, response.status, validationErrors);
  }

  if (isJson) {
    return response.json() as Promise<T>;
  }

  return response.text() as unknown as Promise<T>;
}

export const apiService = {
  // --- Health Diagnostics ---
  async getHealth(): Promise<BackendHealth> {
    try {
      const response = await fetch(`${API_BASE}/health`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<BackendHealth>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  // --- Network Devices API ---
  async getDevices(): Promise<NetworkDevice[]> {
    try {
      const response = await fetch(`${API_BASE}/devices`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkDevice[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async getDeviceById(id: number): Promise<NetworkDevice> {
    try {
      const response = await fetch(`${API_BASE}/devices/${id}`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkDevice>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async createDevice(deviceData: DeviceFormData): Promise<NetworkDevice> {
    try {
      const response = await fetch(`${API_BASE}/devices`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(deviceData),
      });
      return await handleResponse<NetworkDevice>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async updateDevice(id: number, deviceData: DeviceFormData): Promise<NetworkDevice> {
    try {
      const response = await fetch(`${API_BASE}/devices/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(deviceData),
      });
      return await handleResponse<NetworkDevice>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async deleteDevice(id: number): Promise<void> {
    try {
      const response = await fetch(`${API_BASE}/devices/${id}`, {
        method: 'DELETE',
      });
      await handleResponse<void>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  // --- Network Interfaces API ---
  async getInterfaces(): Promise<NetworkInterface[]> {
    try {
      const response = await fetch(`${API_BASE}/interfaces`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkInterface[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async getInterfacesByDeviceId(deviceId: number): Promise<NetworkInterface[]> {
    try {
      const response = await fetch(`${API_BASE}/devices/${deviceId}/interfaces`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkInterface[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async getInterfaceById(id: number): Promise<NetworkInterface> {
    try {
      const response = await fetch(`${API_BASE}/interfaces/${id}`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkInterface>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async createInterface(interfaceData: InterfaceFormData): Promise<NetworkInterface> {
    try {
      const response = await fetch(`${API_BASE}/interfaces`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(interfaceData),
      });
      return await handleResponse<NetworkInterface>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async updateInterface(id: number, interfaceData: InterfaceFormData): Promise<NetworkInterface> {
    try {
      const response = await fetch(`${API_BASE}/interfaces/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(interfaceData),
      });
      return await handleResponse<NetworkInterface>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async deleteInterface(id: number): Promise<void> {
    try {
      const response = await fetch(`${API_BASE}/interfaces/${id}`, {
        method: 'DELETE',
      });
      await handleResponse<void>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  // --- Network Links API ---
  async getLinks(): Promise<NetworkLink[]> {
    try {
      const response = await fetch(`${API_BASE}/links`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkLink[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async getLinkById(id: number): Promise<NetworkLink> {
    try {
      const response = await fetch(`${API_BASE}/links/${id}`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkLink>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async createLink(linkData: LinkFormData): Promise<NetworkLink> {
    try {
      const response = await fetch(`${API_BASE}/links`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(linkData),
      });
      return await handleResponse<NetworkLink>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async updateLink(id: number, linkData: LinkFormData): Promise<NetworkLink> {
    try {
      const response = await fetch(`${API_BASE}/links/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(linkData),
      });
      return await handleResponse<NetworkLink>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  async deleteLink(id: number): Promise<void> {
    try {
      const response = await fetch(`${API_BASE}/links/${id}`, {
        method: 'DELETE',
      });
      await handleResponse<void>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  // --- Topology Graph API ---
  async getTopology(): Promise<TopologyData> {
    try {
      const response = await fetch(`${API_BASE}/topology`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<TopologyData>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable. Please start the Spring Boot backend on port 8080.');
    }
  },

  // --- Device Reachability API ---
  async checkDeviceReachability(id: number, timeoutMs?: number): Promise<DeviceReachabilityResult> {
    try {
      const url = timeoutMs != null
        ? `${API_BASE}/devices/${id}/reachability?timeoutMs=${timeoutMs}`
        : `${API_BASE}/devices/${id}/reachability`;
      const response = await fetch(url, {
        method: 'POST',
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<DeviceReachabilityResult>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error checking device reachability.');
    }
  },

  // --- Fault & Alert Management API ---
  async getAlerts(filters?: { status?: AlertStatus; severity?: AlertSeverity; deviceId?: number }): Promise<NetworkAlert[]> {
    try {
      const params = new URLSearchParams();
      if (filters?.status) params.append('status', filters.status);
      if (filters?.severity) params.append('severity', filters.severity);
      if (filters?.deviceId) params.append('deviceId', filters.deviceId.toString());
      const query = params.toString() ? `?${params.toString()}` : '';

      const response = await fetch(`${API_BASE}/alerts${query}`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkAlert[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error fetching network alerts.');
    }
  },

  async getAlertById(id: number): Promise<NetworkAlert> {
    try {
      const response = await fetch(`${API_BASE}/alerts/${id}`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkAlert>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error fetching alert details.');
    }
  },

  async acknowledgeAlert(id: number): Promise<NetworkAlert> {
    try {
      const response = await fetch(`${API_BASE}/alerts/${id}/acknowledge`, {
        method: 'PATCH',
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkAlert>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error acknowledging alert.');
    }
  },

  async resolveAlert(id: number): Promise<NetworkAlert> {
    try {
      const response = await fetch(`${API_BASE}/alerts/${id}/resolve`, {
        method: 'PATCH',
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<NetworkAlert>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error resolving alert.');
    }
  },

  // --- Device Configuration Management API ---
  async getDeviceConfigurations(deviceId: number): Promise<DeviceConfiguration[]> {
    try {
      const response = await fetch(`${API_BASE}/devices/${deviceId}/configurations`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<DeviceConfiguration[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error fetching device configurations.');
    }
  },

  async getDeviceConfigurationByVersion(deviceId: number, version: number): Promise<DeviceConfiguration> {
    try {
      const response = await fetch(`${API_BASE}/devices/${deviceId}/configurations/${version}`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<DeviceConfiguration>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(`Backend unavailable or error fetching configuration version ${version}.`);
    }
  },

  async createDeviceConfiguration(deviceId: number, configData: ConfigurationRequestData): Promise<DeviceConfiguration> {
    try {
      const response = await fetch(`${API_BASE}/devices/${deviceId}/configurations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(configData),
      });
      return await handleResponse<DeviceConfiguration>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error saving configuration snapshot.');
    }
  },

  async restoreConfigurationVersion(deviceId: number, version: number): Promise<DeviceConfiguration> {
    try {
      const response = await fetch(`${API_BASE}/devices/${deviceId}/configurations/${version}/restore`, {
        method: 'POST',
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<DeviceConfiguration>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(`Backend unavailable or error restoring configuration version ${version}.`);
    }
  },

  async compareConfigurations(deviceId: number, v1: number, v2: number): Promise<ConfigurationDiff> {
    try {
      const response = await fetch(`${API_BASE}/devices/${deviceId}/configurations/diff?v1=${v1}&v2=${v2}`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<ConfigurationDiff>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(`Backend unavailable or error comparing configuration versions ${v1} and ${v2}.`);
    }
  },

  async getConfigurationTemplates(): Promise<ConfigurationTemplate[]> {
    try {
      const response = await fetch(`${API_BASE}/configurations/templates`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<ConfigurationTemplate[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error fetching configuration templates.');
    }
  },

  // --- Simulated Device Metrics & Telemetry API ---
  async getDeviceMetrics(deviceId: number): Promise<DeviceMetrics> {
    try {
      const response = await fetch(`${API_BASE}/devices/${deviceId}/metrics`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<DeviceMetrics>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(`Backend unavailable or error fetching metrics for device ${deviceId}.`);
    }
  },

  async getDeviceMetricsHistory(deviceId: number, points: number = 20): Promise<DeviceMetricsHistoryPoint[]> {
    try {
      const response = await fetch(`${API_BASE}/devices/${deviceId}/metrics/history?points=${points}`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<DeviceMetricsHistoryPoint[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(`Backend unavailable or error fetching metrics history for device ${deviceId}.`);
    }
  },

  // --- Network Automation API ---
  async getPlaybooks(): Promise<AutomationPlaybook[]> {
    try {
      const response = await fetch(`${API_BASE}/automation/playbooks`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<AutomationPlaybook[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error fetching automation playbooks.');
    }
  },

  async getAutomationRequests(deviceId?: number): Promise<AutomationChangeRequest[]> {
    try {
      const url = deviceId ? `${API_BASE}/automation/requests?deviceId=${deviceId}` : `${API_BASE}/automation/requests`;
      const response = await fetch(url, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<AutomationChangeRequest[]>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error fetching automation change requests.');
    }
  },

  async getAutomationRequestById(id: number): Promise<AutomationChangeRequest> {
    try {
      const response = await fetch(`${API_BASE}/automation/requests/${id}`, {
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<AutomationChangeRequest>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(`Backend unavailable or error fetching change request ${id}.`);
    }
  },

  async createAutomationRequest(data: AutomationChangeRequestCreateData): Promise<AutomationChangeRequest> {
    try {
      const response = await fetch(`${API_BASE}/automation/requests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(data),
      });
      return await handleResponse<AutomationChangeRequest>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError('Backend unavailable or error creating automation change request.');
    }
  },

  async executeAutomationRequest(id: number): Promise<AutomationChangeRequest> {
    try {
      const response = await fetch(`${API_BASE}/automation/requests/${id}/execute`, {
        method: 'POST',
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<AutomationChangeRequest>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(`Backend unavailable or error executing change request ${id}.`);
    }
  },

  async rollbackAutomationRequest(id: number): Promise<AutomationChangeRequest> {
    try {
      const response = await fetch(`${API_BASE}/automation/requests/${id}/rollback`, {
        method: 'POST',
        headers: { Accept: 'application/json' },
      });
      return await handleResponse<AutomationChangeRequest>(response);
    } catch (err: unknown) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(`Backend unavailable or error rolling back change request ${id}.`);
    }
  },
};
