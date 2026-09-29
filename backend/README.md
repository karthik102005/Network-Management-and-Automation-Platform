# Network Management and Automation Platform - Backend

Core backend service for the Network Management and Automation Platform (NMAP). Built with **Java JDK 25** and **Spring Boot 3.5.0**, providing centralized network device inventory management, interface tracking, point-to-point link connectivity, dynamic database-backed topology aggregation, device telemetry monitoring, fault alarm management, configuration snapshot versioning with diff comparisons, and simulated network change automation.

---

## 1. Prerequisites and Toolchain Versions

* **Java JDK**: Java JDK 25 (configured in `pom.xml` via `<java.version>25</java.version>` and `<maven.compiler.release>25</maven.compiler.release>`; tested on Oracle JDK 25.0.4)
* **Build Tool**: Apache Maven 3.9+ (tested on Apache Maven 3.9.16)
* **Database**: PostgreSQL 16+ (tested on PostgreSQL 18.6)
* **Operating System**: Windows 10/11, Linux, macOS

Verify installed versions before building:
```bash
java -version
mvn -version
```

---

## 2. PostgreSQL Configuration & Environment Variables

### Database Setup
The platform connects to a PostgreSQL database named `network_management`.

#### Option A: Automated Database Setup (Windows PowerShell)
Use the included setup script to verify and create the database automatically:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\setup-db.ps1
```

#### Option B: Manual Database Setup
1. Connect to PostgreSQL using `psql` or pgAdmin with your administrative account:
   ```sql
   CREATE DATABASE network_management;
   ```

2. **Database User Configuration**:
   * By default, the application connects using the administrative user `postgres` (`spring.datasource.username=${DB_USERNAME:postgres}`).
   * If using a dedicated non-admin database user (e.g., `nmap_admin`), create the role and grant schema permissions so Hibernate (`ddl-auto=update`) can inspect and update tables:
     ```sql
     -- Optional dedicated user example:
     CREATE USER nmap_admin WITH PASSWORD 'your_secure_password';
     GRANT ALL PRIVILEGES ON DATABASE network_management TO nmap_admin;
     -- On PostgreSQL 15+, grant schema usage and table creation permissions:
     \c network_management
     GRANT ALL ON SCHEMA public TO nmap_admin;
     ```
   * If using a dedicated user, set `DB_USERNAME=nmap_admin`.

### Environment Variables & Template
Connection parameters in `src/main/resources/application.properties` are bound to environment variables with fallback defaults:
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:network_management}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
```

| Variable | Description | Default Value |
|---|---|---|
| `DB_HOST` | PostgreSQL server hostname or IP | `localhost` |
| `DB_PORT` | PostgreSQL server port | `5432` |
| `DB_NAME` | Database catalog name | `network_management` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database user password | *(empty)* |

#### Setting Database Password via `.env` or Environment Variables
A template is provided at `.env.example`. Copy it to `.env` in the `backend/` directory:
```properties
DB_HOST=localhost
DB_PORT=5432
DB_NAME=network_management
DB_USERNAME=postgres
DB_PASSWORD=your_actual_password_here
```
*(The `.env` file is git-ignored and will not be committed.)*

**Or configure in PowerShell:**
```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="5432"
$env:DB_NAME="network_management"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_actual_password_here"
```

**Linux / macOS Bash:**
```bash
export DB_HOST="localhost"
export DB_PORT="5432"
export DB_NAME="network_management"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_actual_password_here"
```

---

## 3. Build, Test & Run Commands

All commands should be executed from the `backend/` directory.

### Run Automated Tests
Executes the JUnit 5 test suite against an isolated in-memory H2 database profile (`src/test/resources/application.properties`):
```powershell
mvn test
```
*Verification status: **169 automated tests** across **28 test classes** passing with **0 failures, 0 errors, and 0 skipped**.*

### Package Application JAR
Builds the executable Spring Boot artifact at `target/network-management-backend-1.0.0-SNAPSHOT.jar`:
* **Skip tests during packaging**:
  ```powershell
  mvn package -DskipTests
  ```
* **Execute test suite during packaging**:
  ```powershell
  mvn clean package
  ```

### Run Backend Application
* **Using the Startup Helper Script (Loads `.env` automatically)**:
  ```powershell
  powershell -ExecutionPolicy Bypass -File run-backend.ps1
  ```
* **Using Spring Boot Maven Plugin**:
  ```powershell
  mvn spring-boot:run
  ```
* **Using Packaged JAR**:
  ```powershell
  java -jar target/network-management-backend-1.0.0-SNAPSHOT.jar
  ```
The backend listens on **`http://localhost:8080`** by default (`server.port=8080`).

---

## 4. REST API Endpoints Reference

Base URL: `http://localhost:8080`

### 4.1 System & Health Endpoints (`HealthController`)
| Method | Endpoint | Description | Status Code |
|---|---|---|---|
| `GET` | `/api/health` | Application status, JVM stats, memory telemetry, uptime, and OS details | `200 OK` |
| `GET` | `/actuator/health` | Spring Boot Actuator health status (`UP`) | `200 OK` |
| `GET` | `/actuator/info` | Application metadata and build info | `200 OK` |
| `GET` | `/actuator/metrics` | Actuator metrics catalog (JVM, memory, HTTP request metrics) | `200 OK` |

### 4.2 Network Devices API (`NetworkDeviceController`)
| Method | Endpoint | Description | Supported Status Codes |
|---|---|---|---|
| `GET` | `/api/devices` | List all registered network devices | `200 OK` |
| `GET` | `/api/devices/{id}` | Retrieve device by ID | `200 OK`, `404 Not Found`, `400 Bad Request` |
| `POST` | `/api/devices` | Register a new network device | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `PUT` | `/api/devices/{id}` | Update existing device details | `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/devices/{id}` | Delete device; transactionally cascades to attached links and interfaces | `204 No Content`, `404 Not Found` |
| `POST` | `/api/devices/{id}/reachability` | Probe device reachability (ICMP/TCP); creates alert on failure | `200 OK`, `404 Not Found` |

### 4.3 Network Interfaces API (`NetworkInterfaceController`)
| Method | Endpoint | Description | Supported Status Codes |
|---|---|---|---|
| `GET` | `/api/interfaces` | List all interfaces across all devices | `200 OK` |
| `GET` | `/api/interfaces/{id}` | Retrieve interface by ID | `200 OK`, `404 Not Found` |
| `GET` | `/api/devices/{deviceId}/interfaces` | List all interfaces belonging to a specific device | `200 OK`, `404 Not Found` |
| `POST` | `/api/interfaces` | Create an interface for a device | `201 Created`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `PUT` | `/api/interfaces/{id}` | Update interface attributes | `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/interfaces/{id}` | Delete an interface | `204 No Content`, `404 Not Found` |

### 4.4 Network Links API (`NetworkLinkController`)
| Method | Endpoint | Description | Supported Status Codes |
|---|---|---|---|
| `GET` | `/api/links` | List all point-to-point network links | `200 OK` |
| `GET` | `/api/links/{id}` | Retrieve link by ID | `200 OK`, `404 Not Found` |
| `POST` | `/api/links` | Create a point-to-point link between two distinct interfaces | `201 Created`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `PUT` | `/api/links/{id}` | Update link status, speed, or description | `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/links/{id}` | Delete a link | `204 No Content`, `404 Not Found` |

### 4.5 Dynamic Topology API (`TopologyController`)
| Method | Endpoint | Description | Supported Status Codes |
|---|---|---|---|
| `GET` | `/api/topology` | Aggregated topology graph containing device nodes and interconnect links | `200 OK` |

### 4.6 Device Telemetry & Metrics API (`DeviceMetricsController`)
| Method | Endpoint | Description | Supported Status Codes |
|---|---|---|---|
| `GET` | `/api/devices/{deviceId}/metrics` | Retrieve current simulated CPU, memory, and interface metrics | `200 OK`, `404 Not Found` |
| `GET` | `/api/devices/{deviceId}/metrics/history` | Retrieve historical telemetry points (default 20 points) | `200 OK`, `404 Not Found` |

### 4.7 Fault Management & Alerts API (`AlertController`)
| Method | Endpoint | Description | Supported Status Codes |
|---|---|---|---|
| `GET` | `/api/alerts` | List alerts (supports optional `status`, `severity`, `deviceId` filters) | `200 OK`, `400 Bad Request` |
| `GET` | `/api/alerts/{id}` | Retrieve alert by ID | `200 OK`, `404 Not Found` |
| `POST` | `/api/alerts/{id}/acknowledge` | Transition alert from `ACTIVE` to `ACKNOWLEDGED` | `200 OK`, `404 Not Found`, `409 Conflict` |
| `POST` | `/api/alerts/{id}/resolve` | Transition alert to `RESOLVED` status | `200 OK`, `404 Not Found` |

### 4.8 Configuration Management & Diffs API (`DeviceConfigurationController`)
| Method | Endpoint | Description | Supported Status Codes |
|---|---|---|---|
| `GET` | `/api/devices/{deviceId}/configurations` | List configuration snapshots for a device (version descending) | `200 OK`, `404 Not Found` |
| `GET` | `/api/devices/{deviceId}/configurations/latest` | Retrieve latest configuration snapshot for a device | `200 OK`, `404 Not Found` |
| `GET` | `/api/devices/{deviceId}/configurations/{version}` | Retrieve specific configuration version | `200 OK`, `404 Not Found` |
| `POST` | `/api/devices/{deviceId}/configurations` | Create new configuration snapshot (auto-increments version) | `201 Created`, `400 Bad Request`, `404 Not Found` |
| `POST` | `/api/devices/{deviceId}/configurations/{version}/restore` | Restore (rollback) to a previous configuration version | `200 OK`, `404 Not Found` |
| `GET` | `/api/devices/{deviceId}/configurations/compare` | Compare two versions (`?v1=1&v2=2`), returning line-by-line diff | `200 OK`, `400 Bad Request`, `404 Not Found` |

### 4.9 Configuration Templates API (`ConfigurationTemplateController`)
| Method | Endpoint | Description | Supported Status Codes |
|---|---|---|---|
| `GET` | `/api/configuration-templates` | List pre-defined provisioning configuration templates | `200 OK` |
| `GET` | `/api/configuration-templates/{id}` | Retrieve specific configuration template by ID | `200 OK`, `404 Not Found` |

### 4.10 Network Automation API (`AutomationController`)
| Method | Endpoint | Description | Supported Status Codes |
|---|---|---|---|
| `GET` | `/api/automation/playbooks` | List available pre-defined automation playbooks | `200 OK` |
| `GET` | `/api/automation/playbooks/{id}` | Retrieve specific playbook with execution steps | `200 OK`, `404 Not Found` |
| `GET` | `/api/automation/requests` | List change requests (supports optional `deviceId` filter) | `200 OK` |
| `GET` | `/api/automation/requests/{id}` | Retrieve specific change request with execution status | `200 OK`, `404 Not Found` |
| `POST` | `/api/automation/requests` | Submit a new automation change request | `201 Created`, `400 Bad Request`, `404 Not Found` |
| `POST` | `/api/automation/requests/{id}/execute` | Execute change request (simulated multi-step execution) | `200 OK`, `404 Not Found`, `409 Conflict` |
| `POST` | `/api/automation/requests/{id}/rollback` | Rollback executed change request to pre-change configuration | `200 OK`, `404 Not Found`, `409 Conflict` |

---

## 5. Domain Models, Validation & Enums

### 5.1 Enumeration Reference

| Enum | Valid Values |
|---|---|
| `DeviceType` | `ROUTER`, `SWITCH`, `FIREWALL`, `SERVER`, `ACCESS_POINT`, `LOAD_BALANCER`, `OTHER` |
| `DeviceVendor` | `CISCO`, `JUNIPER`, `ARISTA`, `HUAWEI`, `FORTINET`, `PALO_ALTO`, `MIKROTIK`, `LINUX`, `GENERIC` |
| `DeviceStatus` | `UP`, `DOWN`, `UNKNOWN`, `WARNING`, `CRITICAL`, `MAINTENANCE` |
| `InterfaceType` | `ETHERNET`, `FAST_ETHERNET`, `GIGABIT_ETHERNET`, `TEN_GIGABIT_ETHERNET`, `LOOPBACK`, `VLAN`, `SERIAL`, `MANAGEMENT`, `WIRELESS`, `OTHER` |
| `AdminStatus` | `UP`, `DOWN`, `TESTING` |
| `OperationalStatus`| `UP`, `DOWN`, `TESTING`, `UNKNOWN`, `DORMANT`, `NOT_PRESENT`, `LOWER_LAYER_DOWN` |
| `LinkType` | `ETHERNET`, `FIBER`, `SERIAL`, `VIRTUAL`, `AGGREGATE`, `WIRELESS`, `OTHER` |
| `LinkStatus` | `UP`, `DOWN`, `DEGRADED`, `UNKNOWN` |
| `AlertSeverity` | `CRITICAL`, `WARNING`, `INFO` |
| `AlertStatus` | `ACTIVE`, `ACKNOWLEDGED`, `RESOLVED` |
| `AlertType` | `DEVICE_DOWN`, `LINK_DOWN`, `HIGH_UTILIZATION`, `HIGH_LATENCY`, `CONFIG_DRIFT`, `SECURITY_WARNING`, `REACHABILITY_LOST`, `SYSTEM_ERROR` |
| `ConfigFormat` | `CISCO_IOS`, `JUNIPER_JUNOS`, `JSON`, `YAML`, `RAW_TEXT` |
| `DiffLineType` | `ADDED`, `REMOVED`, `UNCHANGED` |
| `AutomationStatus` | `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `ROLLED_BACK` |
| `DeviceHealthState`| `HEALTHY`, `DEGRADED`, `CRITICAL` |

### 5.2 Validation Rules
* **DeviceRequestDto**: `hostname` (1–100 chars, required), `managementIp` (IPv4 regex, required, unique), `deviceType`, `vendor`, `status`, `description` (max 500 chars).
* **InterfaceRequestDto**: `deviceId` (required, existing device), `interfaceName` (1–100 chars, required, unique per device), `macAddress` (standard MAC regex formats), `speedMbps` (non-negative).
* **LinkRequestDto**: `sourceInterfaceId` and `destinationInterfaceId` (required, existing interfaces, distinct devices), `bandwidthMbps` (non-negative).
* **DeviceConfigurationRequestDto**: `rawConfig` (required, non-blank), `format` (required `ConfigFormat`), `description` (optional).
* **AutomationChangeRequestCreateDto**: `deviceId` (required), `playbookId` (required), `parameters` (optional map).

---

## 6. Business Logic, Validation & Exception Handling

1. **Transactional Safe Device Deletion**:
   * Deleting a device cascades transactionally to remove attached links and interfaces before removing the device, avoiding foreign key violations.
2. **Interface & Link Integrity**:
   * Interfaces must have unique names on a given device.
   * Links cannot connect an interface to itself, nor connect two interfaces on the same device.
   * Bidirectional uniqueness is enforced (cannot link A $\to$ B if B $\to$ A already exists).
3. **Configuration Version Immutability**:
   * Configuration snapshots are version-stamped monotonically (v1, v2, ...).
   * Restoring a past version creates a new snapshot recording the restore event rather than overwriting historical records.
   * Checksums (SHA-256) are computed automatically to verify content integrity.
4. **Alert Lifecycle & Correlation**:
   * Reachability probes that fail automatically raise a `REACHABILITY_LOST` alert.
   * Alerts progress through `ACTIVE` $\to$ `ACKNOWLEDGED` $\to$ `RESOLVED`.
5. **Automation State Machine**:
   * Change requests execute step-by-step with status transitioning `PENDING` $\to$ `RUNNING` $\to$ `COMPLETED` (or `FAILED`).
   * Rollback restores the pre-change configuration snapshot and marks the request `ROLLED_BACK`.
6. **Global Exception Mapping (`GlobalExceptionHandler`)**:
   * `MethodArgumentNotValidException` $\to$ **`400 Bad Request`** with detailed field-level errors.
   * `MethodArgumentTypeMismatchException` $\to$ **`400 Bad Request`** with clear type and parameter guidance.
   * `HttpMessageNotReadableException` $\to$ **`400 Bad Request`** for malformed JSON or invalid enum strings.
   * `IllegalArgumentException` $\to$ **`400 Bad Request`** for business validation rejections.
   * `ResourceNotFoundException` $\to$ **`404 Not Found`** when a referenced resource is absent.
   * `DuplicateResourceException` $\to$ **`409 Conflict`** when unique constraints are violated.
   * `Exception` $\to$ **`500 Internal Server Error`** for unexpected server faults.

---

## 7. Project Structure

```
backend/
├── pom.xml
├── README.md
├── run-backend.ps1
├── scripts/
│   └── setup-db.ps1
└── src/
    ├── main/
    │   ├── java/com/nmap/
    │   │   ├── NetworkManagementApplication.java
    │   │   ├── config/WebConfig.java
    │   │   ├── controller/
    │   │   │   ├── AlertController.java
    │   │   │   ├── AutomationController.java
    │   │   │   ├── ConfigurationTemplateController.java
    │   │   │   ├── DeviceConfigurationController.java
    │   │   │   ├── DeviceMetricsController.java
    │   │   │   ├── HealthController.java
    │   │   │   ├── NetworkDeviceController.java
    │   │   │   ├── NetworkInterfaceController.java
    │   │   │   ├── NetworkLinkController.java
    │   │   │   └── TopologyController.java
    │   │   ├── dto/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── network/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/
    │       └── application.properties
    └── test/
        ├── java/com/nmap/
        │   ├── NetworkManagementApplicationTests.java
        │   ├── controller/             # 9 controller test suites
        │   ├── exception/              # GlobalExceptionHandlerTest
        │   ├── integration/            # 2 end-to-end integration test suites
        │   ├── network/                # DeviceReachabilityServiceTest
        │   ├── repository/             # 6 repository test suites
        │   └── service/                # 8 service unit test suites
        └── resources/
            └── application.properties  # In-memory H2 configuration
```

---

## 8. Simulation Notice & Scope Boundaries

> [!NOTE]
> **Telemetry & Automation Simulation**:
> The metrics engine (`SimulatedDeviceMetricsProvider`) and the automation engine (`AutomationServiceImpl`) currently operate in software simulation mode. Telemetry data (CPU load, memory allocation, temperature, interface throughput) is generated algorithmically, and automation change request steps are simulated rather than pushed to physical network equipment via live SSH/NETCONF/SNMP sessions. This enables full end-to-end functional testing and UI workflows without requiring dedicated lab hardware.
