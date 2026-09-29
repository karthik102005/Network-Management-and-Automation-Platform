# Network Management and Automation Platform (NMAP)

A unified, full-stack network management and automation platform designed for modern Network Operations Centers (NOC). The platform provides centralized network device inventory management, interactive point-to-point network topology visualization, simulated real-time telemetry monitoring, fault alarm management, device configuration snapshot versioning with visual diffs, and automated change request execution with rollback capabilities.

---

## 1. Platform Architecture

The platform follows a decoupled, three-tier architecture:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Client Web Browser                            │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ HTTP / REST (Port 5173 / Proxy)
┌────────────────────────────────────▼────────────────────────────────────┐
│                        React 18 Single-Page App                         │
│   • TypeScript, Vite, Tailwind/CSS NOC Dark Theme                       │
│   • 7 Full Feature Pages: Dashboard, Devices, Topology,                 │
│     Monitoring, Alerts, Configuration, Automation                       │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │ Reverse Proxy (/api, /actuator)
┌────────────────────────────────────▼────────────────────────────────────┐
│                     Spring Boot 3.5.0 Backend                           │
│   • Java JDK 25, RESTful Controllers, Spring Data JPA                   │
│   • 10 REST Controllers, Global Exception Handling (RFC 7807/Custom)   │
│   • Simulation Providers: Telemetry & Automated Playbook Engine         │
└───────────────────┬─────────────────────────────────┬───────────────────┘
                    │ JDBC (Port 5432)                │ Topology Descriptors
┌───────────────────▼─────────────┐   ┌───────────────▼───────────────────┐
│     PostgreSQL 16+ Database     │   │      GNS3 Network Simulator       │
│  • Catalog: network_management  │   │  • Topology JSON & VPCS Scripts   │
│  • Automated Schema Management  │   │  • Lab Emulation Reference        │
└─────────────────────────────────┘   └───────────────────────────────────┘
```

* **Frontend**: React 18 single-page application built with TypeScript, Vite, and Lucide icons, providing a high-contrast dark theme tailored for operations.
* **Backend**: Spring Boot 3.5.0 application running on Java JDK 25, exposing RESTful endpoints, Actuator diagnostics, and service layer validation.
* **Persistence**: PostgreSQL database storing device inventories, interface configurations, interconnect links, alert records, configuration snapshots, and automation audit logs. (H2 in-memory profile used for automated test suites).
* **Simulation Layer**: Built-in simulated telemetry metric generator and multi-step playbook execution engine supporting safe operational testing without hardware dependency.

---

## 2. Repository Structure

```
Network-Management-and-Automation-Platform/
├── README.md                           # Top-level repository documentation
├── .gitignore                          # Hardened Git exclusions (build artifacts, credentials, logs)
├── backend/                            # Spring Boot backend service
│   ├── README.md                       # Comprehensive backend documentation and API reference
│   ├── pom.xml                         # Maven dependencies and build configuration (Java 25)
│   ├── .env.example                    # Safe template for local database environment variables
│   ├── run-backend.ps1                 # PowerShell helper script to start the backend
│   ├── scripts/
│   │   └── setup-db.ps1                # PowerShell script to verify and create PostgreSQL database
│   └── src/
│       ├── main/java/com/nmap/         # Application source (controllers, services, repositories)
│       ├── main/resources/             # application.properties (PostgreSQL profile)
│       ├── test/java/com/nmap/         # Full automated test suite (169 JUnit 5 tests)
│       └── test/resources/             # application.properties (H2 in-memory test profile)
├── frontend/                           # React + TypeScript single-page application
│   ├── README.md                       # Comprehensive frontend documentation and UI workflows
│   ├── package.json                    # npm project dependencies and scripts
│   ├── package-lock.json               # Deterministic dependency lockfile
│   ├── tsconfig.json                   # TypeScript configuration
│   ├── vite.config.ts                  # Vite build configuration and backend reverse proxy
│   ├── index.html                      # HTML entrypoint
│   └── src/
│       ├── components/                 # Modals, status badges, skeletons, health indicator
│       ├── layouts/                    # AppLayout with responsive sidebar and header
│       ├── pages/                      # 7 Core UI pages (Dashboard, Devices, Topology, etc.)
│       ├── services/                   # Unified api.ts REST client (34 methods)
│       ├── styles/                     # NOC dark theme CSS
│       └── types/                      # TypeScript domain models and DTO interfaces
└── network/                            # Network simulation artifacts
    └── Network Management and Automation Platform/
        ├── Network Management and Automation Platform.gns3 # GNS3 topology descriptor
        └── project-files/vpcs/         # VPCS startup scripts for lab emulation
```

---

## 3. Technology Stack & Prerequisites

### Prerequisites

| Component | Minimum Version | Tested Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Java JDK** | JDK 25 | Oracle JDK 25.0.4 | Backend runtime and compilation |
| **Apache Maven** | 3.9+ | Apache Maven 3.9.16 | Backend build tool and dependency management |
| **PostgreSQL** | 16+ | PostgreSQL 18.6 | Relational database (runtime persistence) |
| **Node.js** | 18.x+ | Node.js 22.x+ | Frontend toolchain |
| **npm** | 9.x+ | npm 10.x+ | Frontend package management |
| **Operating System** | Windows 10/11, Linux, macOS | Windows 11 | Host environment |

---

## 4. Local Setup and Startup Instructions (Windows)

All instructions assume PowerShell executed from the repository root:
`C:\Users\R R V KARTHIK\Network-Management-and-Automation-Platform`

### Step 1: Database Setup
The platform requires a PostgreSQL database named `network_management`.

1. You can run the automated PowerShell setup script:
   ```powershell
   powershell -ExecutionPolicy Bypass -File backend\scripts\setup-db.ps1
   ```
2. Or configure credentials manually:
   * Copy `backend/.env.example` to `backend/.env`.
   * Set your database credentials in `backend/.env` (e.g., `DB_PASSWORD=your_password`).
   * The `.env` file is git-ignored and will not be committed.

### Step 2: Start the Backend Service
From the repository root or `backend/` directory:

* **Using the startup helper script (recommended)**:
  ```powershell
  powershell -ExecutionPolicy Bypass -File backend\run-backend.ps1
  ```
* **Or using Maven directly**:
  ```powershell
  cd backend
  mvn spring-boot:run
  ```

The backend starts on **`http://localhost:8080`**. Verify health at **`http://localhost:8080/api/health`**.

### Step 3: Start the Frontend Application
From a separate PowerShell terminal:

```powershell
cd frontend
npm install
npm run dev
```

The frontend Vite dev server will start on **`http://localhost:5173`**. Open this URL in your web browser.

---

## 5. Seven Implemented Frontend Modules

The platform features seven fully implemented and interactive user interface modules:

1. **Dashboard (`/`)**:
   * Summary metric cards for total devices, online/offline counts, total interfaces, and active links.
   * Visual device breakdown charts by device type and hardware vendor.
   * Recent devices inventory overview with direct links to management.
2. **Device Inventory (`/devices`)**:
   * Searchable and filterable data table (by hostname, IP, vendor, status, and device type).
   * Device creation and editing modal dialogs with IPv4 regex validation.
   * Nested interface management tab within the device modal.
   * Safe transactional deletion with cascading link/interface warnings.
3. **Network Topology (`/topology`)**:
   * Interactive SVG topology canvas displaying registered devices as nodes and interconnects as links.
   * Dynamic circular node positioning with interactive drag-and-drop layout adjustments.
   * Status-colored links (green for UP, amber for DEGRADED, red for DOWN) with bandwidth badge pills.
   * Node and link inspector sidebar with real-time deletion and new link creation modal.
4. **Monitoring & Telemetry (`/monitoring`)**:
   * Live device resource telemetry gauges (CPU utilization, Memory usage, Temperature).
   * Interface throughput telemetry (Inbound/Outbound Mbps, packet error counters).
   * Metric history timeline charts showing telemetry trends over time.
   * On-demand ICMP ping reachability probing with live status feedback.
5. **Alert Center (`/alerts`)**:
   * Fault and alarm management with status filtering (`ACTIVE`, `ACKNOWLEDGED`, `RESOLVED`) and severity filters (`CRITICAL`, `WARNING`, `INFO`).
   * Alarm lifecycle actions: single-click alert acknowledgment and resolution.
   * Reachability loss alarms automatically correlated when device reachability fails.
6. **Configuration Management (`/configuration`)**:
   * Version-controlled device configuration snapshots (Cisco IOS, Juniper JunOS, JSON, YAML).
   * Visual side-by-side configuration diff viewer highlighting additions, removals, and unchanged lines.
   * Snapshot creation and single-click configuration restoration (rollback) to any previous version.
   * Configuration template catalog for standardized device provisioning.
7. **Automation & Change Requests (`/automation`)**:
   * Pre-defined network automation playbook catalog (e.g., VLAN provisioning, interface hardening, BGP configuration).
   * Change request creation workflow with pre-execution validation.
   * Simulated multi-step execution with real-time progress indicators and execution logs.
   * Single-click automated change request rollback restoring previous configuration versions.

---

## 6. Testing & Quality Verification

* **Backend Unit & Integration Tests**:
  * **169 automated JUnit 5 tests** across **28 test classes** covering all controllers, repositories, services, and integration workflows.
  * Verified **0 failures, 0 errors, 0 skipped**.
  * Executes in complete isolation using an in-memory H2 database with PostgreSQL compatibility mode:
    ```powershell
    cd backend
    mvn test
    ```
* **Frontend Quality & Build Verification**:
  * Full TypeScript strict typechecking and production build verification:
    ```powershell
    cd frontend
    npm run build
    ```

---

## 7. Component Documentation Links

For deeper technical documentation, API specifications, and architectural details:

* [**Backend Documentation (`backend/README.md`)**](file:///C:/Users/R%20R%20V%20KARTHIK/Network-Management-and-Automation-Platform/backend/README.md): Detailed database configuration, complete REST API endpoint reference (10 controllers), entity-relationship models, validation rules, and error handling.
* [**Frontend Documentation (`frontend/README.md`)**](file:///C:/Users/R%20R%20V%20KARTHIK/Network-Management-and-Automation-Platform/frontend/README.md): Detailed UI component architecture, state management, Vite reverse proxy setup, responsive design features, accessibility implementation, and complete API service reference (34 client methods).

---

## 8. Simulation Notice & Scope Boundaries

> [!NOTE]
> **Simulation Notice**:
> Network telemetry metrics (CPU, memory, packet rates, temperature) and network automation change request execution steps are currently **simulated in software** via backend service providers (`SimulatedDeviceMetricsProvider` and `AutomationServiceImpl`). They demonstrate realistic operational behavior, fault recovery, and rollback mechanisms without requiring live hardware connectivity or physical changes to lab devices.
