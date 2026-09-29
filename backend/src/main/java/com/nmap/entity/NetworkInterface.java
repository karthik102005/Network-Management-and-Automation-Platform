package com.nmap.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "network_interfaces",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_device_interface", columnNames = {"device_id", "interface_name"})
    },
    indexes = {
        @Index(name = "idx_interface_device", columnList = "device_id"),
        @Index(name = "idx_interface_ip", columnList = "ip_address"),
        @Index(name = "idx_interface_status", columnList = "operational_status")
    }
)
public class NetworkInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false, foreignKey = @ForeignKey(name = "fk_interface_device"))
    private NetworkDevice device;

    @Column(name = "interface_name", nullable = false, length = 100)
    private String interfaceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "interface_type", nullable = false, length = 50)
    private InterfaceType interfaceType = InterfaceType.GIGABIT_ETHERNET;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "subnet_prefix")
    private Integer subnetPrefix;

    @Column(name = "mac_address", length = 20)
    private String macAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status", nullable = false, length = 30)
    private AdminStatus adminStatus = AdminStatus.UP;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 30)
    private OperationalStatus operationalStatus = OperationalStatus.UNKNOWN;

    @Column(name = "speed_mbps")
    private Long speedMbps;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NetworkInterface() {
    }

    public NetworkInterface(NetworkDevice device, String interfaceName, InterfaceType interfaceType,
                            String ipAddress, Integer subnetPrefix, String macAddress,
                            AdminStatus adminStatus, OperationalStatus operationalStatus,
                            Long speedMbps, String description) {
        this.device = device;
        this.interfaceName = interfaceName;
        this.interfaceType = interfaceType != null ? interfaceType : InterfaceType.GIGABIT_ETHERNET;
        this.ipAddress = ipAddress;
        this.subnetPrefix = subnetPrefix;
        this.macAddress = macAddress;
        this.adminStatus = adminStatus != null ? adminStatus : AdminStatus.UP;
        this.operationalStatus = operationalStatus != null ? operationalStatus : OperationalStatus.UNKNOWN;
        this.speedMbps = speedMbps;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.adminStatus == null) this.adminStatus = AdminStatus.UP;
        if (this.operationalStatus == null) this.operationalStatus = OperationalStatus.UNKNOWN;
        if (this.interfaceType == null) this.interfaceType = InterfaceType.GIGABIT_ETHERNET;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NetworkDevice getDevice() {
        return device;
    }

    public void setDevice(NetworkDevice device) {
        this.device = device;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public InterfaceType getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(InterfaceType interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getSubnetPrefix() {
        return subnetPrefix;
    }

    public void setSubnetPrefix(Integer subnetPrefix) {
        this.subnetPrefix = subnetPrefix;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public AdminStatus getAdminStatus() {
        return adminStatus;
    }

    public void setAdminStatus(AdminStatus adminStatus) {
        this.adminStatus = adminStatus;
    }

    public OperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(OperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public Long getSpeedMbps() {
        return speedMbps;
    }

    public void setSpeedMbps(Long speedMbps) {
        this.speedMbps = speedMbps;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkInterface that = (NetworkInterface) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(interfaceName, that.interfaceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, interfaceName);
    }

    @Override
    public String toString() {
        return "NetworkInterface{" +
                "id=" + id +
                ", interfaceName='" + interfaceName + '\'' +
                ", interfaceType=" + interfaceType +
                ", ipAddress='" + ipAddress + '\'' +
                ", operationalStatus=" + operationalStatus +
                '}';
    }
}
