import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './layouts/AppLayout';
import { DashboardPage } from './pages/DashboardPage';
import { DevicesPage } from './pages/DevicesPage';
import { TopologyPage } from './pages/TopologyPage';
import { MonitoringPage } from './pages/MonitoringPage';
import { ConfigurationPage } from './pages/ConfigurationPage';
import { AlertsPage } from './pages/AlertsPage';
import { AutomationPage } from './pages/AutomationPage';

export const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="devices" element={<DevicesPage />} />
          <Route path="topology" element={<TopologyPage />} />
          <Route path="monitoring" element={<MonitoringPage />} />
          <Route path="configuration" element={<ConfigurationPage />} />
          <Route path="alerts" element={<AlertsPage />} />
          <Route path="automation" element={<AutomationPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};
