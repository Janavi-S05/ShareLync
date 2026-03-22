import React, { useState, useRef } from 'react';
import { AlertDialog } from '../dialogs/AlertDialog';
import { useCurrentUser } from '../hooks/useCurrentUser';

const SUPPORTED = ['.pdf', '.doc', '.docx', '.txt', '.md', '.json'];

function getFileIcon(name) {
  const ext = name ? name.split('.').pop().toLowerCase() : '';
  const icons = { pdf: '📄', doc: '📝', docx: '📝', txt: '📃', md: '📋', json: '🔧' };
  return icons[ext] || '📎';
}

export const FileUpload = ({ setFileUploaded , onFileChanged }) => {
  const user = useCurrentUser();
  const [file, setFile] = useState(null);
  const [alertMessage, setAlertMessage] = useState('');
  const [alertHeader, setAlertHeader] = useState('');
  const [alertOpen, setAlertOpen] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [dragging, setDragging] = useState(false);
  const fileInputRef = useRef(null);

  const handleDrop = (e) => {
    e.preventDefault();
    setDragging(false);
    const dropped = e.dataTransfer.files[0];
    if (dropped) setFile(dropped);
  };

  const uploadFile = async (e) => {
    e.preventDefault();
    if (!user || !file) return;
    setUploading(true);
    const formData = new FormData();
    formData.append('file', file);
    formData.append('username', user.username);
    try {
      const resp = await fetch('/api/file/upload', {
        method: 'POST',
        headers: { Authorization: `Bearer ${user.token}` },
        body: formData,
      });
      const data = await resp.json();
      setFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
      if (data.status > 299) {
        setAlertHeader('Upload failed');
        setAlertMessage(data.message);
        setAlertOpen(true);
      } else {
        setFileUploaded(true);
        await new Promise(resolve => setTimeout(resolve, 500));
        if (onFileChanged) onFileChanged();
      }
    } catch (err) {
      console.error('Upload error', err);
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <div className="sl-label">Upload Document</div>
      {!user ? (
        <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem', fontFamily: "'DM Mono', monospace" }}>
          Please sign in to upload files.
        </div>
      ) : (
        <div>
          <div
            className={`upload-zone${file ? ' has-file' : ''}${dragging ? ' has-file' : ''}`}
            onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
            onDragLeave={() => setDragging(false)}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            style={{ cursor: 'pointer' }}
          >
            <input
              ref={fileInputRef}
              type="file"
              onChange={(e) => setFile(e.target.files[0])}
              style={{ display: 'none' }}
              accept={SUPPORTED.join(',')}
            />
            {file ? (
              <div>
                <div style={{ fontSize: '2rem', marginBottom: 8 }}>{getFileIcon(file.name)}</div>
                <div style={{ fontFamily: "'DM Mono', monospace", fontSize: '0.82rem', color: 'var(--text-primary)', marginBottom: 4 }}>{file.name}</div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)' }}>{(file.size / 1024).toFixed(1)} KB · click to change</div>
              </div>
            ) : (
              <div>
                <div style={{ fontSize: '2rem', marginBottom: 8, opacity: 0.4 }}>☁</div>
                <div style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', marginBottom: 4 }}>
                  Drop a file here, or <span style={{ color: 'var(--accent)' }}>browse</span>
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: "'DM Mono', monospace" }}>
                  {SUPPORTED.join(' · ')}
                </div>
              </div>
            )}
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
            <button className="sl-btn sl-btn-primary" disabled={uploading || !file} onClick={uploadFile}>
              {uploading ? (
                <>
                  <span style={{ display: 'inline-block', width: 12, height: 12, borderRadius: '50%', border: '2px solid rgba(10,12,20,0.3)', borderTopColor: '#0a0c14', animation: 'spin 0.7s linear infinite' }} />
                  Uploading…
                </>
              ) : 'Upload file'}
            </button>
          </div>
        </div>
      )}
      <AlertDialog open={alertOpen} handleClose={() => setAlertOpen(false)} title={alertHeader} content={alertMessage} />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
};
