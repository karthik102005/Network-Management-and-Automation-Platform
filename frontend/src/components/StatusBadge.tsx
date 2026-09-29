import React from 'react';
import { DeviceStatus } from '../types/device';

interface StatusBadgeProps {
  status: DeviceStatus;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  let badgeClass = 'status-badge-unknown';

  switch (status) {
    case 'UP':
      badgeClass = 'status-badge-up';
      break;
    case 'DOWN':
    case 'UNREACHABLE':
      badgeClass = 'status-badge-down';
      break;
    case 'MAINTENANCE':
      badgeClass = 'status-badge-maintenance';
      break;
    case 'UNKNOWN':
    default:
      badgeClass = 'status-badge-unknown';
      break;
  }

  return (
    <span className={`status-badge ${badgeClass}`}>
      <span className="status-dot"></span>
      {status}
    </span>
  );
};
