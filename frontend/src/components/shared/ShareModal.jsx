import React, { useState } from 'react';

export const ShareModal = ({ filename, onShare, onClose }) => {
  const [shareEmail, setShareEmail] = useState('');
  const [busy, setBusy] = useState(false);

  const handleShare = async (e) => {
    e.preventDefault();
    if (!shareEmail) return;
    setBusy(true);
    try {
      await onShare({ filename, shareEmail });
    } finally {
      setBusy(false);
      onClose();
    }
  };

  return (
    <div
      className="sl-modal-overlay"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="sl-modal">
        <div className="sl-modal-title">Share file</div>
        <div style={{ fontFamily: "'DM Mono', monospace", fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: 20 }}>
          {filename}
        </div>
        <div className="sl-alert sl-alert-info" style={{ marginBottom: 20 }}>
          ℹ &nbsp;Only 1 email address supported at this time
        </div>
        <div className="sl-input-group">
          <label className="sl-input-label">Recipient email</label>
          <input
            className="sl-input"
            type="email"
            placeholder="colleague@example.com"
            autoFocus
            value={shareEmail}
            onChange={(e) => setShareEmail(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleShare(e)}
          />
        </div>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 8 }}>
          <button className="sl-btn sl-btn-ghost" onClick={onClose}>Cancel</button>
          <button
            className="sl-btn sl-btn-primary"
            disabled={busy || !shareEmail}
            onClick={handleShare}
          >
            {busy ? 'Sharing…' : 'Share'}
          </button>
        </div>
      </div>
    </div>
  );
};
