# Network Management and Automation Platform - Frontend

React single-page application for the Network Management and Automation Platform (NMAP). Built with **React 18**, **TypeScript**, and **Vite**, featuring a high-contrast dark theme tailored for Network Operations Centers (NOC).

The application provides a comprehensive operations dashboard for network device inventory, interactive topology visualization, real-time telemetry monitoring, alert management, configuration snapshot versioning with visual diffs, and network change automation workflows.

---

## 1. Prerequisites and Toolchain

* **Node.js**: 18.x or newer (tested on Node 22+)
* **npm**: 9.x or newer
* **Backend Dependency**: Spring Boot backend service running on `http://localhost:8080` (required for all live inventory, interface, link, topology, metrics, alert, configuration, and automation operations).

Verify installed Node and npm versions:
```bash
node -v
npm -v
```

---

## 2. Setup & Development Commands

All commands must be executed from the `frontend/` directory.

### Install Dependencies
Installs application and development dependencies declared in `package.json`:
```bash
npm install
```

### Run Development Server
Starts the local Vite development server:
```bash
npm run dev
```
Listens on **`http://localhost:5173/`** by default.

### Build for Production
Typechecks using the TypeScript compiler (`tsc`) and generates optimized production assets in `dist/`:
```bash
npm run build
```

### Preview Production Build
Locally serves the built `dist/` directory to inspect production output:
```bash
npm run preview
```

---

## 3. Backend Proxy Configuration & Connectivity

### Vite Reverse Proxy (`vite.config.ts`)
During development, Vite forwards API calls and Actuator requests to the Spring Boot backend service:
```typescript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

### Backend Connectivity Monitoring
The application header includes a `BackendStatusIndicator` component:
* Automatically polls `GET /api/health` every **15 seconds** (`15000` ms).
* Displays a visual status indicator (`Backend Online` in green or `Backend Offline` in red).
* Includes a manual refresh button to trigger an immediate health check.
* Shows backend runtime metadata (Java version and uptime) when connected.

---

## 4. Implemented Pages & Features

The frontend implements seven complete, interactive operational modules:

### 4.1 Dashboard (`/`)
* **Inventory Metrics**: Summary metric cards calculating Total Devices, Online Devices, Offline/Warning Devices, Total Configured Interfaces, and Total Interconnect Links based on persisted database state.
* **Breakdown Statistics**: Visual distribution charts of devices categorized by device type (Router, Switch, Firewall, Server, etc.) and hardware vendor (Cisco, Juniper, Arista, etc.).
* **Recent Devices Overview**: Summary table listing recently registered devices with links to manage them in the device inventory.

### 4.2 Devices Inventory (`/devices`)
* **Inventory Table**: Full listing of registered devices showing hostname, management IP, device type, vendor, operational status, and last seen timestamp.
* **Search & Multi-Filter**: Real-time filtering by hostname/IP substring and dropdown filters by status, device type, and vendor.
* **Device & Interface Modals**:
  * **Add Device**: Modal dialog (`DeviceModal`) with IPv4 format validation, hostname requirements, and enum selectors.
  * **Edit Device**: Updates persisted device metadata.
  * **Interface Management Tab**: Embedded interface viewer inside `DeviceModal` showing all interfaces configured on the selected device, with actions to add, edit, or delete interfaces via `InterfaceModal`.
  * **Safe Delete Confirmation**: Confirmation dialog (`DeleteConfirmModal`) notifying the operator that deleting a device transactionally cascades to remove all associated interfaces and connected links.

### 4.3 Network Topology Visualization (`/topology`)
* **Database-Backed Topology Canvas**: SVG-rendered network graph displaying registered devices as nodes and configured point-to-point links as connecting lines.
* **Interactive Node Controls**:
  * Automatically computes circular positions for newly added nodes.
  * Supports click-and-drag repositioning of device nodes within the canvas session.
  * Displays device abbreviations (`RTR`, `SW`, `FW`, `SRV`) and configured interface counts.
* **Link Visualization**:
  * Renders line connections between linked device interfaces.
  * Color-coded status representation (green for `UP`, amber for `DEGRADED`, red for `DOWN`).
  * Displays link capacity badge pills (e.g., `1000M`).
* **Inspector Sidebar**:
  * Clicking any node displays device details, management IP, and configured interfaces.
  * Clicking any link displays source and destination endpoints, bandwidth, and status, with an action to delete the link.
* **Create Link Action**: Opens `LinkModal` allowing the operator to select source device/interface and destination device/interface to establish a new link.
* **Reset Layout**: Restores default circular node positioning.

### 4.4 Telemetry & Metrics Monitoring (`/monitoring`)
* **Real-Time Telemetry Gauges**: Visual circular and bar gauges displaying CPU load percentage, memory utilization, and device operating temperature.
* **Interface Bandwidth Throughput**: Table of device interfaces displaying inbound/outbound Mbps traffic and packet error counters.
* **Historical Trend Charts**: Interactive historical trend visualization tracking telemetry points over time.
* **On-Demand Reachability Probe**: "Ping Device" action triggering an on-demand ICMP/TCP reachability check via the backend, displaying response latency and reachability state.

### 4.5 Fault Alarms & Alert Center (`/alerts`)
* **Alarm Console**: Centralized alert management interface listing active, acknowledged, and resolved network faults.
* **Filtering & Search**: Multi-criteria filtering by alert severity (`CRITICAL`, `WARNING`, `INFO`), lifecycle status (`ACTIVE`, `ACKNOWLEDGED`, `RESOLVED`), and keyword search across error descriptions.
* **Operational Actions**:
  * **Acknowledge Alert**: Single-click action transitioning active alarms to acknowledged state.
  * **Resolve Alert**: Closes the alarm once the underlying fault condition is remediated.
* **Automated Failure Correlation**: Displays alarms automatically generated when device reachability probes fail.

### 4.6 Configuration Management & Diffs (`/configuration`)
* **Snapshot History**: Version-controlled list of configuration snapshots for each device, tracking version numbers, timestamps, configuration formats (Cisco IOS, JunOS, JSON, YAML), and descriptions.
* **Visual Diff Comparison**: Side-by-side split diff viewer highlighting added lines (green), removed lines (red), and unchanged context.
* **Configuration Restoration (Rollback)**: Restore action allowing operators to safely revert a device's running configuration to any historical snapshot version.
* **Snapshot Creation**: Modal dialog allowing manual configuration snapshot capture with format and syntax labeling.
* **Template Catalog**: Browser for standardized network configuration templates.

### 4.7 Automation Playbooks & Workflows (`/automation`)
* **Playbook Catalog**: Grid of available automation playbooks (e.g., VLAN configuration, interface hardening, BGP neighbor provisioning) with descriptions and estimated run times.
* **Change Request Creation**: Workflow for submitting structured change requests targeting specific network devices with custom parameters.
* **Execution & Step Progress**: Simulated multi-step execution visualizer showing real-time step progress (validation, pre-check, deployment, post-check) and output logs.
* **Automated Rollback**: Single-click rollback action that reverts an executed change request to its recorded pre-change configuration version.

---

## 5. Responsive Design & Accessibility Features

### Responsive Layout
* **Collapsible Navigation Sidebar**: The navigation layout (`AppLayout.tsx`) supports narrow and mobile viewports. On smaller screens, the fixed sidebar collapses into a slide-out drawer accessible via a header menu toggle button, complete with backdrop click-to-dismiss.
* **Adaptive Data Tables**: Tables provide horizontal overflow scrolling and priority column visibility on compact screens.

### Accessibility Enhancements
* **Modal Dialog Semantics**: All modals (`DeviceModal`, `InterfaceModal`, `LinkModal`, `DeleteConfirmModal`) implement `role="dialog"`, `aria-modal="true"`, and `aria-labelledby` referencing dialog titles.
* **Form Controls**: Form inputs across modals have explicit `id` and `htmlFor` label associations.
* **Visual Feedback**: Interactive elements provide clear focus rings, high-contrast text ratios for dark mode, animated pulse loading skeletons (`LoadingSkeleton`), and descriptive empty states (`EmptyState`).

---

## 6. Frontend API Client Reference (`src/services/api.ts`)

The unified API client exposes 34 typed methods communicating with the backend REST endpoints:

| Category | Method | HTTP & Endpoint | Description |
|---|---|---|---|
| **Health** | `getHealth()` | `GET /api/health` | Application health and system telemetry |
| **Devices** | `getDevices()` | `GET /api/devices` | Retrieve all registered devices |
| | `getDeviceById(id)` | `GET /api/devices/{id}` | Retrieve device by ID |
| | `createDevice(data)` | `POST /api/devices` | Register a new device |
| | `updateDevice(id, data)` | `PUT /api/devices/{id}` | Update existing device details |
| | `deleteDevice(id)` | `DELETE /api/devices/{id}` | Delete device and cascade-delete links/interfaces |
| | `checkDeviceReachability(id, timeoutMs?)` | `POST /api/devices/{id}/reachability` | Trigger on-demand reachability probe |
| **Interfaces**| `getInterfaces()` | `GET /api/interfaces` | List all interfaces across devices |
| | `getInterfacesByDeviceId(deviceId)` | `GET /api/devices/{deviceId}/interfaces`| List interfaces for a specific device |
| | `getInterfaceById(id)` | `GET /api/interfaces/{id}` | Retrieve interface by ID |
| | `createInterface(data)` | `POST /api/interfaces` | Create interface for a device |
| | `updateInterface(id, data)` | `PUT /api/interfaces/{id}` | Update interface attributes |
| | `deleteInterface(id)` | `DELETE /api/interfaces/{id}` | Delete an interface |
| **Links** | `getLinks()` | `GET /api/links` | List all point-to-point links |
| | `getLinkById(id)` | `GET /api/links/{id}` | Retrieve link by ID |
| | `createLink(data)` | `POST /api/links` | Create link between two interfaces |
| | `updateLink(id, data)` | `PUT /api/links/{id}` | Update link properties |
| | `deleteLink(id)` | `DELETE /api/links/{id}` | Delete a link |
| **Topology** | `getTopology()` | `GET /api/topology` | Aggregated topology graph data |
| **Alerts** | `getAlerts(filters?)` | `GET /api/alerts` | List alerts with status/severity filters |
| | `getAlertById(id)` | `GET /api/alerts/{id}` | Retrieve alert by ID |
| | `acknowledgeAlert(id)` | `POST /api/alerts/{id}/acknowledge` | Acknowledge active alert |
| | `resolveAlert(id)` | `POST /api/alerts/{id}/resolve` | Mark alert as resolved |
| **Config** | `getDeviceConfigurations(deviceId)` | `GET /api/devices/{deviceId}/configurations` | List configuration snapshots |
| | `getDeviceConfigurationByVersion(dId, v)` | `GET /api/devices/{deviceId}/configurations/{v}` | Retrieve specific configuration version |
| | `createDeviceConfiguration(deviceId, data)`| `POST /api/devices/{deviceId}/configurations` | Create new configuration snapshot |
| | `restoreConfigurationVersion(deviceId, v)` | `POST /api/devices/{deviceId}/configurations/{v}/restore` | Restore past configuration version |
| | `compareConfigurations(deviceId, v1, v2)` | `GET /api/devices/{deviceId}/configurations/compare` | Line-by-line diff between versions |
| | `getConfigurationTemplates()` | `GET /api/configuration-templates` | List pre-defined config templates |
| **Metrics** | `getDeviceMetrics(deviceId)` | `GET /api/devices/{deviceId}/metrics` | Current CPU, memory, and interface metrics |
| | `getDeviceMetricsHistory(deviceId, pts?)` | `GET /api/devices/{deviceId}/metrics/history` | Historical telemetry time series |
| **Automation**| `getPlaybooks()` | `GET /api/automation/playbooks` | List available automation playbooks |
| | `getAutomationRequests(deviceId?)` | `GET /api/automation/requests` | List change requests |
| | `getAutomationRequestById(id)` | `GET /api/automation/requests/{id}` | Retrieve change request status |
| | `createAutomationRequest(data)` | `POST /api/automation/requests` | Submit new change request |
| | `executeAutomationRequest(id)` | `POST /api/automation/requests/{id}/execute` | Execute change request |
| | `rollbackAutomationRequest(id)` | `POST /api/automation/requests/{id}/rollback` | Rollback executed change request |

---

## 7. Project Structure

```
frontend/
├── package.json
├── package-lock.json
├── tsconfig.json
├── vite.config.ts
├── index.html
├── src/
│   ├── types/
│   │   └── device.ts              # TypeScript interfaces (Device, Interface, Link, Topology, Form, Alerts, Config, Automation)
│   ├── services/
│   │   └── api.ts                 # Unified REST client with 34 methods
│   ├── styles/
│   │   └── index.css              # Dark theme styling, badges, tables, modals, diff colors, and SVG canvas
│   ├── components/
│   │   ├── BackendStatusIndicator.tsx # Health polling indicator with manual refresh
│   │   ├── StatusBadge.tsx        # Color-coded operational and administrative badges
│   │   ├── DeviceModal.tsx        # Device create/edit modal with nested interface manager
│   │   ├── InterfaceModal.tsx     # Interface create/edit modal
│   │   ├── LinkModal.tsx          # Point-to-point link creation modal
│   │   ├── DeleteConfirmModal.tsx # Safe cascading deletion dialog
│   │   ├── EmptyState.tsx         # Descriptive empty states
│   │   └── LoadingSkeleton.tsx    # Pulse skeleton loaders
│   ├── layouts/
│   │   └── AppLayout.tsx          # Responsive navigation sidebar, header, and outlet
│   ├── pages/
│   │   ├── DashboardPage.tsx      # Overview metrics and distribution breakdown
│   │   ├── DevicesPage.tsx        # Device inventory table and management
│   │   ├── TopologyPage.tsx       # Interactive SVG topology visualization
│   │   ├── MonitoringPage.tsx     # Telemetry gauges, interface throughput, and ping
│   │   ├── AlertsPage.tsx         # Alert management console with acknowledge/resolve
│   │   ├── ConfigurationPage.tsx  # Versioned snapshots, side-by-side diff, and rollback
│   │   └── AutomationPage.tsx     # Playbook browser, change request execution, and rollback
│   ├── App.tsx                    # React Router configuration for all 7 routes
│   └── main.tsx                   # React DOM root entry point
└── dist/                          # Production build output (git-ignored)
```

---

## 8. Simulation Notice

> [!NOTE]
> **Operational Telemetry & Automation Behavior**:
> The metrics and automation workflows displayed in the UI communicate with backend simulated providers. Telemetry numbers (CPU/RAM/Temp) and change request execution steps demonstrate full operational behavior and state changes without connecting to physical network hardware.
