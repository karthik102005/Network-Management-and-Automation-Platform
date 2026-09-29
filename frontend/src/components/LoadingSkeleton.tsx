import React from 'react';

export const LoadingSkeleton: React.FC<{ rows?: number }> = ({ rows = 5 }) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', padding: '16px' }}>
      {Array.from({ length: rows }).map((_, i) => (
        <div
          key={i}
          style={{
            height: '42px',
            backgroundColor: 'rgba(255, 255, 255, 0.03)',
            borderRadius: '6px',
            border: '1px solid rgba(255, 255, 255, 0.05)',
            animation: 'pulse 1.5s infinite ease-in-out',
          }}
        />
      ))}
      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 0.4; }
          50% { opacity: 0.8; }
        }
      `}</style>
    </div>
  );
};
