import React, { useContext, useEffect, useState } from 'react';
import { AlertDialog } from '../dialogs/AlertDialog';
import { ScheduleContext } from './ScheduleContext';
import { useCurrentUser } from '../hooks/useCurrentUser';
import { useFileActions, getFileIcon, VIEWABLE_EXTENSIONS } from '../hooks/useFileActions';
import { ShareModal } from './shared/ShareModal';
import { ScheduleModal } from './shared/ScheduleModal';
import { useScheduleActions } from '../hooks/useScheduleActions';

const TAG_COLORS = ['tag-0', 'tag-1', 'tag-2', 'tag-3', 'tag-4', 'tag-5'];

export const FileList = ({ fileUploadDone, setFileUploadDone, onFileChanged }) => {
  const user = useCurrentUser();
  const [userFiles, setUserFiles] = useState([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [fileToShare, setFileToShare] = useState('');
  const [showFiles, setShowFiles] = useState(false);
  const [alertOpen, setAlertOpen] = useState(false);
  const [notLoginErr, setNotLoginError] = useState(false);
  const [alertHeader, setAlertHeader] = useState('');
  const [alertMessage, setAlertMessage] = useState('');
  const scheduleContext = useContext(ScheduleContext);
  const [scheduleModalOpen, setScheduleModalOpen] = useState(false);
  const [fileToSchedule, setFileToSchedule] = useState('');
  const [existingSchedule, setExistingSchedule] = useState(null);
  const [scheduleMap, setScheduleMap] = useState(new Map());

  const { fetchAllSchedules } = useScheduleActions(user);

  // After delete — refresh FileList AND tell Dashboard something changed
  const handleFileChanged = () => {
    filesUploaded();
    if (onFileChanged) onFileChanged();
  };

  const { deleteFile, viewFile, shareFile, generatePublicLink, copyState } =
    useFileActions(user, handleFileChanged);

  useEffect(() => {
    if (fileUploadDone) {
      filesUploaded();
      setFileUploadDone(false);
      if (onFileChanged) onFileChanged(); // tell FileBrowser about upload too
    }
  }, [fileUploadDone]);

  async function filesUploaded() {
    if (!user) { setNotLoginError(true); return; }
    try {
      const resp = await fetch(`/api/file/by?username=${user.username}`, {
        headers: { Authorization: `Bearer ${user.token}` },
      });
      const data = await resp.json();
      if (data.status >= 400) {
        setAlertHeader('Error');
        setAlertMessage(data.data || 'Could not fetch files.');
        setAlertOpen(true);
      } else {
        const files = Array.isArray(data.data) ? data.data : [];
        setUserFiles(files);
        setShowFiles(true);
        // Build scheduleMap from the embedded schedule data already in the response.
        // Fall back to a dedicated API call only if the backend doesn't embed schedules.
        const embedded = new Map(
          files
            .filter((f) => f.schedule)
            .map((f) => [f.filename, f.schedule])
        );
        if (embedded.size > 0) {
          setScheduleMap(embedded);
        } else {
          // Fallback: fetch schedules separately (handles backends that don't embed them)
          const fetched = await fetchAllSchedules();
          setScheduleMap(fetched);
        }
      }
    } catch (err) {
      console.error('Failed to fetch files:', err);
      setAlertHeader('Network Error');
      setAlertMessage('Could not connect to the server.');
      setAlertOpen(true);
    }
  }

  const getSendDateStr = (date) => date.split('T')[0];

  const openScheduleModal = (filename) => {
    setFileToSchedule(filename);
    setExistingSchedule(scheduleMap.get(filename) || null);
    setScheduleModalOpen(true);
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 }}>
        <div className="sl-label" style={{ margin: 0, flex: 1 }}>Your Documents</div>
        <button
          className={`sl-btn ${showFiles ? 'sl-btn-ghost' : 'sl-btn-primary'} sl-btn-sm`}
          onClick={showFiles ? () => setShowFiles(false) : filesUploaded}
          disabled={!user}
        >
          {showFiles ? 'Hide' : 'Load files'}
        </button>
      </div>

      {showFiles && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {userFiles.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">📂</div>
              <div style={{ fontSize: '0.85rem' }}>No files uploaded yet</div>
            </div>
          ) : userFiles.map((file, rowIdx) => (
            <div
              key={file.filename}
              className="file-row"
              style={{ animationDelay: `${rowIdx * 0.05}s` }}
            >
              <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start', flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: '1.4rem', flexShrink: 0, marginTop: 2 }}>{getFileIcon(file.filename)}</div>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div className="file-name">{file.filename}</div>
                  {(() => {
                    const sched = scheduleMap.get(file.filename) || file.schedule;
                    return sched ? (
                      <div className="file-meta">
                        📅 Scheduled: {getSendDateStr(sched.sendDate)} → {sched.receivers?.[0] || ''}
                      </div>
                    ) : null;
                  })()}
                  <div style={{ marginTop: 6, display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                    {file.tags && file.tags.length > 0 ? (
                      file.tags.map((tag, i) => (
                        <span key={tag} className={`tag-chip ${TAG_COLORS[i % TAG_COLORS.length]}`}>{tag}</span>
                      ))
                    ) : (
                      <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontFamily: "'DM Mono', monospace", fontStyle: 'italic' }}>
                        AI tags generating…
                      </span>
                    )}
                  </div>
                </div>
              </div>
              <div className="file-actions">
                <button className="sl-btn sl-btn-ghost sl-btn-sm" onClick={() => viewFile(file.filename)}>
                  {VIEWABLE_EXTENSIONS.has(file.filename.split('.').pop().toLowerCase()) ? 'View' : 'Download'}
                </button>
                <button className="sl-btn sl-btn-ghost sl-btn-sm" onClick={() => { setFileToShare(file.filename); setDialogOpen(true); }}>Share</button>
                <button
                  className={`sl-btn sl-btn-sm ${scheduleMap.get(file.filename) ? 'sl-btn-primary' : 'sl-btn-ghost'}`}
                  onClick={() => openScheduleModal(file.filename)}
                >
                  {scheduleMap.get(file.filename) ? '📅 Scheduled' : 'Schedule'}
                </button>
                <button className="sl-btn sl-btn-ghost sl-btn-sm" onClick={() => generatePublicLink(file.filename)}>
                  {copyState[file.filename] ? 'Copied' : 'Link'}
                </button>
                <button className="sl-btn sl-btn-danger sl-btn-sm" onClick={() => deleteFile(file.filename)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {dialogOpen && (
        <ShareModal
          filename={fileToShare}
          onShare={shareFile}
          onClose={() => setDialogOpen(false)}
        />
      )}

      {scheduleModalOpen && (
        <ScheduleModal
          filename={fileToSchedule}
          user={user}
          existingSchedule={existingSchedule}
          onClose={() => setScheduleModalOpen(false)}
          onSaved={filesUploaded}
        />
      )}

      <AlertDialog open={alertOpen} handleClose={() => setAlertOpen(false)} title={alertHeader} content={alertMessage} />
      <AlertDialog open={notLoginErr} handleClose={() => setNotLoginError(false)} title="Not Signed In" content="Please sign in to manage your files." />
    </div>
  );
};
