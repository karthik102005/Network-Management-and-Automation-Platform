import React, { useState } from 'react';
import { NetworkDevice } from '../types/device';
import { AlertTriangle, Trash2, X } from 'lucide-react';

interface DeleteConfirmModalProps {
  isOpen: boolean;
  device: NetworkDevice | null;
  onClose: () => void;
  onConfirm: (id: number) => Promise<void>;
}

export const DeleteConfirmModal: React.FC<DeleteConfirmModalProps> = ({
  isOpen,
  device,
  onClose,
  onConfirm,
}) => {
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !device) return null;

  const handleConfirm = async () => {
    setIsDeleting(true);
    setError(null);
    try {
      await onConfirm(device.id);
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to delete device.');
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-content"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '440px' }}
        role="dialog"
        aria-modal="true"
        aria-labelledby="delete-confirm-title"
      >
        <div className="modal-header">
          <div className="section-title" style={{ color: '#ef4444' }}>
            <AlertTriangle size={18} />
            <span id="delete-confirm-title">Confirm Deletion</span>
          </div>
          <button onClick={onClose} className="btn-icon" aria-label="Close modal">
            <X size={18} />
          </button>
        </div>

        <div className="modal-body">
          {error && <div className="form-error" style={{ marginBottom: '12px' }}>{error}</div>}
          <p style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            Are you sure you want to remove this network device from active inventory?
          </p>
          <div
            style={{
              marginTop: '16px',
              padding: '12px',
              backgroundColor: 'rgba(239, 68, 68, 0.08)',
              border: '1px solid rgba(239, 68, 68, 0.2)',
              borderRadius: '6px',
            }}
          >
            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
              {device.hostname}
            </div>
            <div className="table-mono" style={{ fontSize: '12px', color: 'var(--accent-blue)', marginTop: '2px' }}>
              {device.managementIp} ({device.deviceType} / {device.vendor})
            </div>
          </div>
          <p style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '12px' }}>
            This action deletes the record from the database and stops Southbound polling.
          </p>
        </div>

        <div className="modal-footer">
          <button onClick={onClose} className="btn btn-secondary" disabled={isDeleting}>
            Cancel
          </button>
          <button onClick={handleConfirm} className="btn btn-danger" disabled={isDeleting}>
            <Trash2 size={16} />
            <span>{isDeleting ? 'Deleting...' : 'Delete Device'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
