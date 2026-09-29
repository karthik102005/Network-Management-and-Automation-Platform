import React, { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import {
  LayoutDashboard,
  Server,
  Network,
  Activity,
  Sliders,
  Bell,
  Cpu,
  ShieldAlert,
  Menu,
  X,
} from 'lucide-react';
import { BackendStatusIndicator } from '../components/BackendStatusIndicator';

export const AppLayout: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const navItems = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/devices', label: 'Devices', icon: Server },
    { to: '/topology', label: 'Topology', icon: Network },
    { to: '/monitoring', label: 'Monitoring', icon: Activity },
    { to: '/configuration', label: 'Configuration', icon: Sliders },
    { to: '/alerts', label: 'Alerts', icon: Bell },
    { to: '/automation', label: 'Automation', icon: Cpu },
  ];

  return (
    <div className="app-container">
      {/* Mobile Backdrop Overlay */}
      {sidebarOpen && (
        <div
          className="sidebar-backdrop open"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Sidebar */}
      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : ''}`} aria-label="Sidebar Navigation">
        <div className="sidebar-header">
          <div className="brand-badge">NM</div>
          <div className="brand-info">
            <span className="brand-title">NMAP Platform</span>
            <span className="brand-sub">Network Management</span>
          </div>
          <button
            type="button"
            className="sidebar-close-btn"
            onClick={() => setSidebarOpen(false)}
            aria-label="Close navigation sidebar"
          >
            <X size={18} />
          </button>
        </div>

        <nav className="sidebar-nav" aria-label="Main Navigation">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                end={item.to === '/'}
                onClick={() => setSidebarOpen(false)}
              >
                <Icon size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="sidebar-footer">
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <ShieldAlert size={14} color="var(--accent-blue)" />
            <span style={{ fontWeight: 600, color: 'var(--text-secondary)' }}>CSE Capstone</span>
          </div>
          <div>Protocols: SNMP · NETCONF · RESTCONF</div>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="main-wrapper">
        <header className="header">
          <div className="header-title-container">
            <button
              type="button"
              className="mobile-menu-btn"
              onClick={() => setSidebarOpen((prev) => !prev)}
              aria-label="Toggle navigation menu"
              aria-expanded={sidebarOpen}
            >
              <Menu size={20} />
            </button>
            <h1 className="header-title">Network Management and Automation Platform</h1>
            <span className="header-badge">v1.0 Core</span>
          </div>
          <div className="header-right">
            <BackendStatusIndicator />
          </div>
        </header>

        <main className="content-body">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
