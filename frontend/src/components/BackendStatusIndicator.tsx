import React, { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { BackendHealth } from '../types/device';
import { RefreshCw, Server } from 'lucide-react';

export const BackendStatusIndicator: React.FC = () => {
  const [health, setHealth] = useState<BackendHealth | null>(null);
  const [isOnline, setIsOnline] = useState<boolean | null>(null);
  const [isChecking, setIsChecking] = useState<boolean>(false);

  const checkStatus = async () => {
    setIsChecking(true);
    try {
      const data = await apiService.getHealth();
      setHealth(data);
      setIsOnline(true);
    } catch {
      setHealth(null);
      setIsOnline(false);
    } finally {
      setIsChecking(false);
    }
  };

  useEffect(() => {
    checkStatus();
    const interval = setInterval(checkStatus, 15000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="backend-indicator" title={isOnline ? `Backend UP - Spring Boot (Port 8080)` : `Backend Offline - Port 8080 unreachable`}>
      <span className={`pulse-dot ${isOnline ? 'online' : 'offline'}`} />
      <Server size={14} color={isOnline ? '#38bdf8' : '#94a3b8'} />
      <span>{isOnline ? 'Backend Online' : 'Backend Offline'}</span>
      {health?.details?.javaVersion && (
        <span style={{ fontSize: '10px', color: '#64748b', fontFamily: 'var(--font-mono)' }}>
          (Java {health.details.javaVersion})
        </span>
      )}
      <button
        onClick={checkStatus}
        disabled={isChecking}
        className="btn-icon"
        style={{ padding: '2px', marginLeft: '4px' }}
        title="Refresh backend status"
      >
        <RefreshCw size={12} className={isChecking ? 'spin' : ''} />
      </button>
    </div>
  );
};
