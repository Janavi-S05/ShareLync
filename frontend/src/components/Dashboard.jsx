import * as React from 'react';
import { UserAuth } from './UserAuth';
import { FileUpload } from './FileUpload';
import { FileList } from './FileList';
import { FileBrowser } from './FileBrowser';
import { ScheduleProvider } from './ScheduleContext';

export const Dashboard = ({ user, setIsLoggedIn }) => {
  const [fileUploadDone, setFileUploadDone] = React.useState(false);
  const [activeTab, setActiveTab] = React.useState(0);
  const [refreshTrigger, setRefreshTrigger] = React.useState(0);

  const triggerRefresh = () => setRefreshTrigger(prev => prev + 1);

  return (
    <div className="fade-in">
      <div style={{ marginBottom: 28 }}>
        <UserAuth userLoggedIn={true} setUserLoggedIn={setIsLoggedIn} />
      </div>

      <div className="sl-tabs">
        <button
          className={`sl-tab${activeTab === 0 ? ' active' : ''}`}
          onClick={() => setActiveTab(0)}
        >
          📁 &nbsp;My Files
        </button>
        <button
          className={`sl-tab${activeTab === 1 ? ' active' : ''}`}
          onClick={() => setActiveTab(1)}
        >
          🔍 &nbsp;Browse
        </button>
      </div>

      {activeTab === 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
          <div className="sl-card sl-card-accent fade-in">
            <FileUpload setFileUploaded={setFileUploadDone} onFileChanged={triggerRefresh} />
          </div>
          <div className="sl-card fade-in fade-in-delay-1">
            <ScheduleProvider>
              <FileList
                fileUploadDone={fileUploadDone}
                setFileUploadDone={setFileUploadDone}
                onFileChanged={triggerRefresh}
              />
            </ScheduleProvider>
          </div>
        </div>
      )}

      <div className="sl-card fade-in" style={{ display: activeTab === 1 ? 'block' : 'none' }}>
        <FileBrowser refreshTrigger={refreshTrigger} />
      </div>
    </div>
  );
};
