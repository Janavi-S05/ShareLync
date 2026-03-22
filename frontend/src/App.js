import './App.css';
import React, { useState } from 'react';
import { UserAuth } from './components/UserAuth';
import { Dashboard } from './components/Dashboard';
import { useCurrentUser } from './hooks/useCurrentUser';

function App() {
  const user = useCurrentUser();
  const [isLoggedIn, setIsLoggedIn] = useState(!!user);

  return (
    <div className="app-shell">
      <header className="app-header fade-in">
        <div>
          <div className="app-wordmark">Share<span>Lync</span></div>
          <div className="app-tagline">Document sharing effortlessly</div>
        </div>
      </header>

      {isLoggedIn && user ? (
        <Dashboard user={user} setIsLoggedIn={setIsLoggedIn} />
      ) : (
        <UserAuth setUserLoggedIn={setIsLoggedIn} />
      )}
    </div>
  );
}

export default App;
