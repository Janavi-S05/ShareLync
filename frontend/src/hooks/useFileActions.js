import { useState } from 'react';
import { API_FILE_PATH } from '../Constants';

export const VIEWABLE_EXTENSIONS = new Set(['pdf', 'png', 'jpg', 'jpeg', 'gif', 'webp', 'txt', 'md', 'json']);

export function useFileActions(user, onFileListChanged) {
  const [copyState, setCopyState] = useState({});

  async function deleteFile(filename) {
    const bareFilename = filename.includes('/')
      ? filename.split('/').slice(1).join('/')
      : filename;

    const url = `${API_FILE_PATH}/delete/${bareFilename}?userId=${user.username}`;
    await fetch(url, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${user.token}` },
    });
    await new Promise(resolve => setTimeout(resolve, 500));
    if (onFileListChanged) onFileListChanged();
  }

  async function downloadFile(file) {
    const bareFile = file.includes('/')
      ? file.split('/').slice(1).join('/')
      : file;

    const url = `${API_FILE_PATH}/${bareFile}/download?userId=${user.username}`;
    try {
      const resp = await fetch(url, { headers: { Authorization: `Bearer ${user.token}` } });
      const presignedUrl = await resp.text();
      window.open(presignedUrl, '_blank', 'noopener');
    } catch (err) {
      console.error('Download error', err);
    }
  }

  async function viewFile(file) {
    const bareFile = file.includes('/')
      ? file.split('/').slice(1).join('/')
      : file;

    const ext = bareFile.split('.').pop().toLowerCase();
    if (!VIEWABLE_EXTENSIONS.has(ext)) {
      return downloadFile(file);
    }

    const url = `${API_FILE_PATH}/${bareFile}/view?userId=${user.username}`;
    try {
      const resp = await fetch(url, { headers: { Authorization: `Bearer ${user.token}` } });
      const presignedUrl = await resp.text();
      window.open(presignedUrl, '_blank', 'noopener,noreferrer');
    } catch (err) {
      console.error('View error', err);
    }
  }

  async function generatePublicLink(filename) {
    try {
      const resp = await fetch(
        `/api/link/generate?username=${user.username}&filename=${filename}`,
        { method: 'POST', headers: { Authorization: `Bearer ${user.token}` } }
      );
      const link = await resp.text();
      await navigator.clipboard.writeText(link);
      setCopyState(prev => ({ ...prev, [filename]: true }));
      setTimeout(() => setCopyState(prev => ({ ...prev, [filename]: false })), 2000);
    } catch (error) {
      console.error('Error generating public link', error);
    }
  }

  async function shareFile({ filename, shareEmail }) {
    const emailBody = {
      to: shareEmail,
      cc: [], bcc: [],
      subject: `${user.name} shared ${filename} with you`,
      body: `Dear ${shareEmail}, kindly download the attachment`,
      filesToAttach: [`${user.username}/${filename}`],
    };
    await fetch('/api/social/sendMail', {
      method: 'POST',
      headers: { Authorization: `Bearer ${user.token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify(emailBody),
    });
  }

  return { deleteFile, downloadFile, viewFile, generatePublicLink, shareFile, copyState };
}

export function getFileIcon(name) {
  const ext = (name || '').split('.').pop().toLowerCase();
  const icons = { pdf: '📄', doc: '📝', docx: '📝', txt: '📃', md: '📋', json: '🔧' };
  return icons[ext] || '📎';
}