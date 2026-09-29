import React from 'react';
import { ServerOff } from 'lucide-react';

interface EmptyStateProps {
  title?: string;
  description?: string;
  onAction?: () => void;
  actionText?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title = 'No network devices registered.',
  description = 'Add network devices such as routers, switches, and firewalls to begin monitoring and automation.',
  onAction,
  actionText,
}) => {
  return (
    <div className="empty-state">
      <ServerOff className="empty-icon" />
      <div className="empty-title">{title}</div>
      <div className="empty-desc">{description}</div>
      {onAction && actionText && (
        <button onClick={onAction} className="btn btn-primary" style={{ marginTop: '12px' }}>
          {actionText}
        </button>
      )}
    </div>
  );
};
