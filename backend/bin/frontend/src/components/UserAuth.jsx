import React, { useEffect, useState } from 'react';
import { useCookies } from 'react-cookie';
import { RegistrationForm } from '../RegistrationForm';
import { useGoogleLogin, googleLogout } from '@react-oauth/google';
import { useCurrentUser } from '../hooks/useCurrentUser';

export const UserAuth = ({ userLoggedIn, setUserLoggedIn }) => {
  const user = useCurrentUser();
  const [, setCookie, removeCookie] = useCookies(['user']);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loginError, setLoginError] = useState(false);
  const [showRegForm, setShowRegForm] = useState(false);
  const [profileImage, setProfileImage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (userLoggedIn && user?.google?.pictureLink) {
      setProfileImage(user.google.pictureLink);
    }
  }, [user, userLoggedIn]);

  const handleLoginSuccess = (userObject) => {
    setCookie('user', userObject, { path: '/' });
    setUserLoggedIn(true);
  };

  const fetchUserGoogleProfile = async (token) => {
    try {
      const resp = await fetch(`https://www.googleapis.com/oauth2/v1/userinfo?access_token=${token}`);
      const data = await resp.json();
      const newUserInfo = {
        google: { token, email: data.email, pictureLink: data.picture, name: data.name },
        username: data.email,
        name: data.name,
      };
      const bePost = await fetch('/api/auth/login/google', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newUserInfo.google),
      });
      const beData = await bePost.json();
      newUserInfo.token = beData.data.accessToken;
      handleLoginSuccess(newUserInfo);
    } catch (error) {
      console.error('Google login failed', error);
    }
  };

  const glogin = useGoogleLogin({
    onSuccess: (tokenResponse) => fetchUserGoogleProfile(tokenResponse.access_token),
    onError: (e) => console.error(e),
  });

  const login = async () => {
    setIsLoading(true);
    try {
      const resp = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });
      const data = await resp.json();
      if (data.isError) {
        setLoginError(true);
      } else {
        handleLoginSuccess({ name: data.data.name, username, token: data.data.access_token });
      }
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    removeCookie('user', { path: '/' });
    setUserLoggedIn(false);
    googleLogout();
  };

  if (userLoggedIn && user) {
    return (
      <div className="user-badge fade-in">
        <div className="user-avatar">
          {profileImage
            ? <img src={profileImage} alt={user.username} />
            : (user.name || user.username || '?')[0].toUpperCase()}
        </div>
        <div style={{ flex: 1 }}>
          <div className="user-name">{user.name || user.username}</div>
          <div className="user-email">{user.username}</div>
        </div>
        <button className="sl-btn sl-btn-ghost sl-btn-sm" onClick={logout} data-test="logout">
          Sign out
        </button>
      </div>
    );
  }

  if (showRegForm) {
    return (
      <div style={{ maxWidth: 440, margin: '40px auto' }} className="fade-in">
        <div className="sl-card">
          <div className="sl-modal-title">Create Account</div>
          <RegistrationForm closeForm={() => setShowRegForm(false)} />
          <div className="sl-divider" />
          <button className="sl-btn sl-btn-ghost" style={{ width: '100%' }} onClick={() => setShowRegForm(false)}>
            ← Back to sign in
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="login-layout">
      <div className="login-visual fade-in">
        <div className="login-hero-title">
          Share documents<br /><em>To anyone, anytime.</em>
        </div>
        <p className="login-hero-sub">
          Upload your files, keep them organised with AI-generated tags, and share them with anyone simply and fast
        </p>
        <div>
          {['Drop it in', 'Find anything, fast', 'Read without downloading','Share in a tap', 'Tagged before you blink'].map(f => (
            <div className="login-feature" key={f}>
              <div className="login-feature-dot" />
              {f}
            </div>
          ))}
        </div>
      </div>

      <div className="sl-card fade-in fade-in-delay-1" style={{ maxWidth: 380, width: '100%', justifySelf: 'end' }}>
        <div style={{ marginBottom: 24 }}>
          <div style={{ fontFamily: "'DM Serif Display', serif", fontSize: '1.3rem', color: 'var(--text-primary)', marginBottom: 4 }}>Sign in</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', fontFamily: "'DM Mono', monospace" }}>Welcome back</div>
        </div>

        {loginError && (
          <div className="sl-alert sl-alert-error" style={{ marginBottom: 16 }}>
            ✕ &nbsp;Invalid username or password
          </div>
        )}

        <div className="sl-input-group">
          <label className="sl-input-label">Email</label>
          <input
            className="sl-input"
            type="email"
            placeholder="you@example.com"
            autoComplete="email"
            autoFocus
            onChange={(e) => setUsername(e.target.value)}
          />
        </div>

        <div className="sl-input-group">
          <label className="sl-input-label">Password</label>
          <input
            className="sl-input"
            type="password"
            placeholder="••••••••"
            onChange={(e) => { setPassword(e.target.value); setLoginError(false); }}
            onKeyDown={(e) => e.key === 'Enter' && login()}
          />
        </div>

        <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
          <button className="sl-btn sl-btn-primary" style={{ flex: 1 }} onClick={login} disabled={isLoading}>
            {isLoading ? 'Signing in…' : 'Sign in'}
          </button>
          <button className="sl-btn sl-btn-ghost" onClick={() => setShowRegForm(true)}>Register</button>
        </div>

        <div className="sl-or">or</div>

        <button className="sl-btn sl-btn-google" onClick={() => glogin()}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z" fill="#FBBC05"/>
            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
          </svg>
          Continue with Google
        </button>
      </div>
    </div>
  );
};
