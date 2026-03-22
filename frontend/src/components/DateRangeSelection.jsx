import { useContext, useEffect, useState } from 'react';
import './components.css';
import { ScheduleContext } from './ScheduleContext';

export const DateTimeRange = ({ fileId, savedSchedule }) => {
  const [emailToSendTo, setEmailToSendTo] = useState('');
  const [selDate, setSelDate] = useState('');
  const { schedule, setSchedule } = useContext(ScheduleContext);

  // Fixed: dependency array prevents running on every render
  useEffect(() => {
    if (!savedSchedule) return;
    const email = savedSchedule.receivers[0] || '';
    const dateStr = savedSchedule.sendDate ? savedSchedule.sendDate.split('T')[0] : '';
    setEmailToSendTo(email);
    setSelDate(dateStr);
    // Update context — do not mutate schedule object directly
    setSchedule(prev => ({ ...prev, to: email, date: dateStr }));
  }, [savedSchedule]); // eslint-disable-line react-hooks/exhaustive-deps

  const today = new Date().toISOString().split('T')[0];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', marginTop: 8, padding: 0 }}>
      <div className="sl-alert sl-alert-info" style={{ marginBottom: 12 }}>
        Date &amp; time when you want to share it
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div className="sl-input-group">
          <label className="sl-input-label">Recipient email</label>
          <input
            className="sl-input"
            autoFocus
            required
            type="email"
            value={emailToSendTo}
            placeholder="Enter the receiver's email address"
            onChange={(e) => {
              setEmailToSendTo(e.target.value);
              setSchedule(prev => ({ ...prev, to: e.target.value }));
            }}
          />
        </div>

        <div className="sl-input-group">
          <label className="sl-input-label">Scheduled date</label>
          <input
            className="sl-input"
            type="date"
            value={selDate}
            min={today}
            onChange={(e) => {
              setSelDate(e.target.value);
              setSchedule(prev => ({ ...prev, date: e.target.value }));
            }}
          />
        </div>
      </div>
    </div>
  );
};
