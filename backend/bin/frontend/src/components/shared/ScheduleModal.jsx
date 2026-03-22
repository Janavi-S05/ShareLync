import React, { useState, useEffect } from 'react';

export const ScheduleModal = ({ filename, user, existingSchedule, onClose, onSaved }) => {
  const today = new Date().toISOString().split('T')[0];

  const [recipientEmail, setRecipientEmail] = useState('');
  const [sendDate, setSendDate] = useState(today);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(false);

  // Pre-fill when editing an existing schedule
  useEffect(() => {
    if (existingSchedule) {
      setRecipientEmail(existingSchedule.receivers?.[0] || '');
      setSendDate(
        existingSchedule.sendDate
          ? existingSchedule.sendDate.split('T')[0]
          : today
      );
    }
  }, [existingSchedule]);

  const isEditing = !!existingSchedule;

  const validate = () => {
    if (!recipientEmail) return 'Recipient email is required.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(recipientEmail))
      return 'Please enter a valid email address.';
    if (!sendDate) return 'A send date is required.';
    return '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationError = validate();
    if (validationError) { setError(validationError); return; }

    setBusy(true);
    setError('');

    const payload = {
      senderName: user.name || user.username,
      senderEmail: user.username,
      receivers: [recipientEmail],
      filename,
      sendDate,
      isRecurring: false,
    };

    try {
      let resp;
      if (isEditing) {
        resp = await fetch(`/api/schedule/${existingSchedule.id}`, {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${user.token}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(payload),
        });
      } else {
        resp = await fetch('/api/schedule/', {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${user.token}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(payload),
        });
      }

      if (!resp.ok) {
        const data = await resp.json().catch(() => ({}));
        setError(data.message || `Request failed (${resp.status})`);
        return;
      }

      onSaved && onSaved();
      onClose();
    } catch (err) {
      setError('Network error — please try again.');
    } finally {
      setBusy(false);
    }
  };

  const handleDelete = async () => {
    if (!existingSchedule?.id) return;
    setBusy(true);
    setError('');
    try {
      const resp = await fetch(`/api/schedule/${existingSchedule.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${user.token}` },
      });
      if (!resp.ok && resp.status !== 204) {
        setError(`Delete failed (${resp.status})`);
        return;
      }
      onSaved && onSaved();
      onClose();
    } catch (err) {
      setError('Network error — could not delete schedule.');
    } finally {
      setBusy(false);
      setConfirmDelete(false);
    }
  };

  return (
    <div
      className="sl-modal-overlay"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="sl-modal">
        {/* Header */}
        <div className="sl-modal-title">
          {isEditing ? 'Edit schedule' : 'Schedule file share'}
        </div>
        <div
          style={{
            fontFamily: "'DM Mono', monospace",
            fontSize: '0.75rem',
            color: 'var(--text-muted)',
            marginBottom: 20,
          }}
        >
          {filename}
        </div>

        {/* Info */}
        <div className="sl-alert sl-alert-info" style={{ marginBottom: 20 }}>
          📅 &nbsp;The file will be emailed to the recipient on the scheduled date.
        </div>

        {/* Error */}
        {error && (
          <div
            className="sl-alert"
            style={{
              marginBottom: 16,
              background: 'rgba(255,80,80,0.08)',
              border: '1px solid rgba(255,80,80,0.25)',
              color: '#ff6b6b',
            }}
          >
            &nbsp;{error}
          </div>
        )}

        {/* Form */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div className="sl-input-group">
            <label className="sl-input-label">Recipient email</label>
            <input
              className="sl-input"
              type="email"
              placeholder="colleague@example.com"
              autoFocus
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
            />
          </div>

          <div className="sl-input-group">
            <label className="sl-input-label">Send date</label>
            <input
              className="sl-input"
              type="date"
              min={today}
              value={sendDate}
              onChange={(e) => setSendDate(e.target.value)}
            />
          </div>
        </div>

        {/* Actions */}
        <div
          style={{
            display: 'flex',
            gap: 8,
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: 24,
          }}
        >
          {/* Delete side */}
          <div>
            {isEditing && !confirmDelete && (
              <button
                className="sl-btn sl-btn-danger sl-btn-sm"
                disabled={busy}
                onClick={() => setConfirmDelete(true)}
              >
                Delete schedule
              </button>
            )}
            {confirmDelete && (
              <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                <span
                  style={{
                    fontSize: '0.72rem',
                    color: '#ff6b6b',
                    fontFamily: "'DM Mono', monospace",
                  }}
                >
                  Confirm?
                </span>
                <button
                  className="sl-btn sl-btn-danger sl-btn-sm"
                  disabled={busy}
                  onClick={handleDelete}
                >
                  {busy ? 'Deleting…' : 'Yes, delete'}
                </button>
                <button
                  className="sl-btn sl-btn-ghost sl-btn-sm"
                  disabled={busy}
                  onClick={() => setConfirmDelete(false)}
                >
                  Cancel
                </button>
              </div>
            )}
          </div>

          {/* Save / Cancel side */}
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="sl-btn sl-btn-ghost" onClick={onClose} disabled={busy}>
              Cancel
            </button>
            <button
              className="sl-btn sl-btn-primary"
              disabled={busy || !recipientEmail || !sendDate}
              onClick={handleSubmit}
            >
              {busy
                ? isEditing
                  ? 'Saving…'
                  : 'Scheduling…'
                : isEditing
                ? 'Save changes'
                : 'Schedule'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};