import { useEffect, useState } from 'react';
import { useCurrentUser } from '../hooks/useCurrentUser';
import { useFileActions, getFileIcon } from '../hooks/useFileActions';
import { ShareModal } from './shared/ShareModal';
import { ScheduleModal } from './shared/ScheduleModal';
import { useScheduleActions } from '../hooks/useScheduleActions';
import { API_FILE_PATH } from '../Constants';

export const FileBrowser = ({ refreshTrigger }) => {
  const user = useCurrentUser();
  const [userFiles, setUserFiles] = useState([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [fileToShare, setFileToShare] = useState('');
  const [filesLoading, setFilesLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [scheduleModalOpen, setScheduleModalOpen] = useState(false);
  const [fileToSchedule, setFileToSchedule] = useState('');
  const [existingSchedule, setExistingSchedule] = useState(null);
  const [scheduleMap, setScheduleMap] = useState(new Map());

  const { fetchAllSchedules } = useScheduleActions(user);

  const { deleteFile, downloadFile, shareFile, generatePublicLink, copyState } =
    useFileActions(user, fetchFiles);

  useEffect(() => {
    if (user?.username) fetchFiles();
  }, [user?.username, refreshTrigger]);

  async function fetchFiles() {
    if (!user) return;
    setFilesLoading(true);
    try {
      const resp = await fetch(`${API_FILE_PATH}/list?userId=${user.username}`, {
        headers: { Authorization: `Bearer ${user.token}` },
      });
      const data = await resp.json();
      if (data.status !== 401 && data.status !== 403) {
        setUserFiles(Array.isArray(data.data) ? data.data : []);
      }
      // Load schedule map for badge display
      const map = await fetchAllSchedules();
      setScheduleMap(map);
    } catch (err) {
      console.error('Error fetching files:', err);
    } finally {
      setFilesLoading(false);
    }
  }

  const openScheduleModal = (filename) => {
    setFileToSchedule(filename);
    setExistingSchedule(scheduleMap.get(filename) || null);
    setScheduleModalOpen(true);
  };

  const filteredFiles = userFiles.filter(f =>
    f.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div>
      <div className="sl-label">Browse All Files</div>

      <div className="sl-input-group" style={{ marginBottom: 20 }}>
        <input
          className="sl-input"
          type="text"
          placeholder="Search files…"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      {filesLoading ? (
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', color: 'var(--text-muted)', fontSize: '0.82rem', fontFamily: "'DM Mono', monospace" }}>
          <span style={{ display: 'inline-block', width: 12, height: 12, borderRadius: '50%', border: '2px solid var(--border)', borderTopColor: 'var(--accent)', animation: 'spin 0.7s linear infinite' }} />
          Loading files…
        </div>
      ) : filteredFiles.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">📂</div>
          <div style={{ fontSize: '0.85rem' }}>{searchQuery ? 'No files match your search' : 'No files found'}</div>
        </div>
      ) : (
        <div className="browser-grid">
          {filteredFiles.map((file, i) => (
            <div key={file} className="browser-item" style={{ animationDelay: `${i * 0.04}s` }}>
              <div className="browser-item-icon">{getFileIcon(file)}</div>
              <div className="browser-item-name">{file}</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 12 }}>
                <button className="sl-btn sl-btn-ghost sl-btn-sm" style={{ width: '100%' }} onClick={() => downloadFile(file)}>
                  Download
                </button>
                <button className="sl-btn sl-btn-ghost sl-btn-sm" style={{ width: '100%' }} onClick={() => { setFileToShare(file); setDialogOpen(true); }}>
                  Share
                </button>
                <button
                  className={`sl-btn sl-btn-sm ${scheduleMap.get(file) ? 'sl-btn-primary' : 'sl-btn-ghost'}`}
                  style={{ width: '100%' }}
                  onClick={() => openScheduleModal(file)}
                >
                  {scheduleMap.get(file) ? '📅 Scheduled' : 'Schedule'}
                </button>
                <button className="sl-btn sl-btn-ghost sl-btn-sm" style={{ width: '100%' }} onClick={() => generatePublicLink(file)}>
                  {copyState[file] ? '✓ Copied' : 'Public link'}
                </button>
                <button className="sl-btn sl-btn-danger sl-btn-sm" style={{ width: '100%' }} onClick={() => deleteFile(file)}>
                  Delete
                </button>
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
          onSaved={fetchFiles}
        />
      )}

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
};
