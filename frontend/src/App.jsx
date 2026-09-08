import { useState, useEffect, useCallback, createContext, useContext } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useParams, Link, useLocation, Outlet } from 'react-router-dom';

const TOKEN_KEY = 'buyit_token';
const USER_KEY  = 'buyit_user';

function getToken() { return localStorage.getItem(TOKEN_KEY); }
function clearAuth() { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY); }
function setAuth(token, user) { localStorage.setItem(TOKEN_KEY, token); localStorage.setItem(USER_KEY, JSON.stringify(user)); }
function getStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    const u = JSON.parse(raw);
    if (u && u.role && ['CUSTOMER','VENDOR','ADMIN'].includes(u.role)) return u;
    clearAuth(); return null;
  } catch { clearAuth(); return null; }
}

async function api(path, opts = {}) {
  const token = getToken();
  const headers = {};
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch('/api' + path, { method: opts.method || 'GET', headers, body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || 'Request failed (' + res.status + ')');
  return data;
}

function fmt(amount) {
  const n = Number(amount || 0);
  return '\u20B9' + n.toLocaleString('en-IN', { minimumFractionDigits: n % 1 === 0 ? 0 : 2, maximumFractionDigits: 2 });
}
function statusClass(s) { return 'status-badge status-' + (s || '').toLowerCase(); }

function Loader() { return <div className="loader">Loading...</div>; }
function EmptyState({ message }) { return <div className="empty-state">{message || 'No data found'}</div>; }

function Toast({ toasts, onRemove }) {
  if (!toasts.length) return null;
  return (
    <div style={{ position: 'fixed', top: 20, right: 20, zIndex: 9999, display: 'flex', flexDirection: 'column', gap: 8 }}>
      {toasts.map(t => <div key={t.id} className={'toast toast-' + t.type} onClick={() => onRemove(t.id)}>{t.message}</div>)}
    </div>
  );
}

function Modal({ title, children, onClose }) {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}><h3>{title}</h3>{children}</div>
    </div>
  );
}

function StarRating({ rating }) {
  const r = Math.round(rating || 0);
  return <span className="review-stars">{[1,2,3,4,5].map(i => <span key={i} style={{ color: i <= r ? '#f5a623' : '#ccc' }}>{i <= r ? '\u2605' : '\u2606'}</span>)}</span>;
}

let _cachedCats = null;
let _cachedBrds = null;

function ProductImage({ src, alt, style }) {
  const [err, setErr] = useState(false);
  useEffect(() => { setErr(false); }, [src]);
  if (src && !err) return <img src={src} alt={alt || ''} style={style} onError={() => setErr(true)} loading="lazy" decoding="async" />;
  return <div className="img-placeholder" style={style}>📦</div>;
}

const CartCtx = createContext({ cartCount: 0, refreshCart: () => {} });
function useCart() { return useContext(CartCtx); }

function BrandLogo({ size = 20, light = false }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: size, fontWeight: 800, color: light ? '#fff' : '#0f172a', letterSpacing: -0.5 }}>
      <span style={{
        width: size + 2,
        height: size + 2,
        borderRadius: '50%',
        background: 'conic-gradient(from 0deg, #f97316, #ef4444, #8b5cf6, #3b82f6, #10b981, #f97316)',
        display: 'inline-block',
        flexShrink: 0
      }} />
      <span>BuyIt</span>
    </div>
  );
}

function NotificationBell({ addToast }) {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const fetchNotifications = useCallback(async () => {
    try {
      const data = await api('/notifications');
      setNotifications(data.notifications || []);
      setUnreadCount(data.unreadCount || 0);
    } catch { }
  }, []);

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 25000);
    return () => clearInterval(interval);
  }, [fetchNotifications]);

  const markAllRead = async () => {
    try {
      await api('/notifications/read', { method: 'POST' });
      setUnreadCount(0);
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
      if (addToast) addToast('All notifications marked as read', 'info');
    } catch (err) {
      if (addToast) addToast(err.message, 'error');
    }
  };

  return (
    <div className="notif-bell-container">
      <button
        type="button"
        className="notif-bell-btn"
        onClick={() => setOpen(!open)}
        title="Notifications"
        aria-label="Notifications"
      >
        🔔{unreadCount > 0 && <span className="notif-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
      </button>
      {open && (
        <div className="notif-popover">
          <div className="notif-header">
            <h4>Notifications</h4>
            {unreadCount > 0 && (
              <button type="button" className="notif-mark-read" onClick={markAllRead}>
                Mark all read
              </button>
            )}
          </div>
          <div className="notif-list">
            {notifications.length === 0 ? (
              <div className="notif-empty">No notifications yet</div>
            ) : (
              notifications.map(n => (
                <div key={n.id} className={`notif-item ${n.isRead ? 'read' : 'unread'}`}>
                  <div className="notif-title">{n.title}</div>
                  <div className="notif-msg">{n.message}</div>
                  <div className="notif-time">
                    {n.createdAt ? new Date(n.createdAt).toLocaleDateString() + ' ' + new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function RequireRole({ user, role, children }) {
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) {
    const home = user.role === 'ADMIN' ? '/admin' : user.role === 'VENDOR' ? '/vendor' : '/store';
    return <Navigate to={home} replace />;
  }
  return children;
}

function LoginPage({ addToast, onAuth }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [loading, setLoading] = useState(false);
  const [focusEmail, setFocusEmail] = useState(false);
  const [focusPass, setFocusPass] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await api('/auth/login', { method: 'POST', body: { email: email.trim(), password: password.trim() } });
      if (!res.success) { addToast(res.message || 'Login failed', 'error'); return; }
      onAuth(res.token, res.user);
      addToast('Welcome back! 🎉', 'success');
      navigate(res.user.role === 'ADMIN' ? '/admin' : res.user.role === 'VENDOR' ? '/vendor' : '/store', { replace: true });
    } catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  };

  const fill = (e, em, pw) => { e.preventDefault(); setEmail(em); setPassword(pw); };

  const s = {
    page: { display: 'flex', minHeight: '100vh', background: '#f0f0f0', alignItems: 'center', justifyContent: 'center', fontFamily: "'Inter', 'Segoe UI', sans-serif", padding: '20px' },
    wrapper: { display: 'flex', width: '100%', maxWidth: 960, minHeight: 580, borderRadius: 24, overflow: 'hidden', boxShadow: '0 32px 80px rgba(0,0,0,0.25)', background: '#fff' },
    left: { flex: 1, background: 'linear-gradient(150deg, #1a1008 0%, #2d1a08 40%, #1c1209 100%)', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: '36px 40px', position: 'relative', overflow: 'hidden', minWidth: 0 },
    leftGlow: { position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', width: 340, height: 340, borderRadius: '50%', border: '1px solid rgba(255,255,255,0.06)', pointerEvents: 'none' },
    leftGlow2: { position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', width: 520, height: 520, borderRadius: '50%', border: '1px solid rgba(255,255,255,0.03)', pointerEvents: 'none' },
    tagline: { fontSize: 13, color: 'rgba(255,255,255,0.45)', letterSpacing: 0.3, zIndex: 1 },
    heroText: { zIndex: 1 },
    h1: { fontSize: 46, fontWeight: 800, color: '#fff', lineHeight: 1.1, margin: '0 0 8px 0', letterSpacing: -1 },
    heroSub: { fontSize: 14, color: 'rgba(255,255,255,0.4)', margin: 0 },
    phoneWrap: { position: 'absolute', bottom: -20, left: '50%', transform: 'translateX(-50%)', width: 220, zIndex: 1 },
    phone: { width: '100%', borderRadius: 20, display: 'block', filter: 'drop-shadow(0 20px 40px rgba(0,0,0,0.6))' },
    right: { flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: '36px 48px', background: '#fff', minWidth: 320 },
    rightTop: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
    signUpBtn: { display: 'flex', alignItems: 'center', gap: 6, fontSize: 14, color: '#555', cursor: 'pointer', background: 'none', border: 'none', fontFamily: 'inherit', textDecoration: 'none' },
    formArea: { flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', gap: 0, paddingTop: 16 },
    heading: { fontSize: 36, fontWeight: 700, color: '#111', margin: '0 0 28px 0', letterSpacing: -0.8 },
    inputWrap: { position: 'relative', marginBottom: 14 },
    input: (focused) => ({ width: '100%', padding: '14px 16px', fontSize: 15, border: `1.5px solid ${focused ? '#f97316' : '#e5e7eb'}`, borderRadius: 10, outline: 'none', background: '#fafafa', color: '#111', boxSizing: 'border-box', transition: 'border-color 0.2s', fontFamily: 'inherit' }),
    inputPassStyle: (focused) => ({ width: '100%', padding: '14px 44px 14px 16px', fontSize: 15, border: `1.5px solid ${focused ? '#f97316' : '#e5e7eb'}`, borderRadius: 10, outline: 'none', background: '#fafafa', color: '#111', boxSizing: 'border-box', transition: 'border-color 0.2s', fontFamily: 'inherit' }),
    eyeBtn: { position: 'absolute', right: 14, top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: '#9ca3af', padding: 0, fontSize: 18, lineHeight: 1, display: 'flex' },
    forgotRow: { textAlign: 'right', marginBottom: 22 },
    forgot: { fontSize: 13, color: '#f97316', cursor: 'pointer', background: 'none', border: 'none', fontFamily: 'inherit', fontWeight: 500 },
    signInBtn: (loading) => ({ width: '100%', padding: '15px', fontSize: 16, fontWeight: 600, color: '#fff', background: loading ? '#f9a96e' : 'linear-gradient(135deg, #f97316 0%, #ef4444 100%)', border: 'none', borderRadius: 12, cursor: loading ? 'not-allowed' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, transition: 'opacity 0.2s, transform 0.1s', fontFamily: 'inherit', boxShadow: '0 4px 20px rgba(249,115,22,0.35)' }),
    demoBox: { marginTop: 22, padding: '14px 16px', background: '#fafafa', borderRadius: 10, border: '1px solid #f3f4f6' },
    demoTitle: { fontSize: 11, fontWeight: 700, color: '#9ca3af', textTransform: 'uppercase', letterSpacing: 1, marginBottom: 8 },
    demoLink: { display: 'block', fontSize: 12, color: '#6366f1', cursor: 'pointer', marginBottom: 4, background: 'none', border: 'none', fontFamily: 'inherit', textAlign: 'left', padding: 0, textDecoration: 'underline' },
    footer: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: 16, borderTop: '1px solid #f3f4f6' },
    footerNote: { fontSize: 12, color: '#9ca3af' },
    footerLink: { fontSize: 12, color: '#6b7280' },
    registerLink: { fontSize: 14, color: '#6b7280', textAlign: 'center', marginTop: 16 },
  };

  return (
    <div style={s.page}>
      <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
      <div style={s.wrapper}>
        {/* LEFT HERO PANEL */}
        <div style={s.left}>
          <div style={s.leftGlow} />
          <div style={s.leftGlow2} />
          <p style={s.tagline}>Your one-stop shop — quality products delivered fast.</p>
          <div style={s.heroText}>
            <h1 style={s.h1}>Shop<br />smarter,<br />live better</h1>
            <p style={s.heroSub}>Thousands of products, unbeatable prices.</p>
          </div>
          <div style={s.phoneWrap}>
            <img src="/login-hero.jpg" alt="BuyIt App" style={s.phone} />
          </div>
          {/* Bottom spacer so phone doesn't overlap text */}
          <div style={{ height: 180 }} />
        </div>

        {/* RIGHT FORM PANEL */}
        <div style={s.right}>
          <div style={s.rightTop}>
            <BrandLogo size={20} />
            <Link to="/register" style={s.signUpBtn}>
              <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
              Sign Up
            </Link>
          </div>

          <div style={s.formArea}>
            <h2 style={s.heading}>Sign In</h2>
            <form onSubmit={handleSubmit} autoComplete="on">
              <div style={s.inputWrap}>
                <input
                  id="login-email"
                  type="email"
                  placeholder="Email or Username"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  onFocus={() => setFocusEmail(true)}
                  onBlur={() => setFocusEmail(false)}
                  style={s.input(focusEmail)}
                  required
                />
              </div>
              <div style={s.inputWrap}>
                <input
                  id="login-password"
                  type={showPass ? 'text' : 'password'}
                  placeholder="Password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  onFocus={() => setFocusPass(true)}
                  onBlur={() => setFocusPass(false)}
                  style={s.inputPassStyle(focusPass)}
                  required
                />
                <button type="button" style={s.eyeBtn} onClick={() => setShowPass(p => !p)} tabIndex={-1} aria-label="Toggle password">
                  {showPass
                    ? <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    : <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  }
                </button>
              </div>
              <div style={s.forgotRow}>
                <button type="button" style={s.forgot}>Forgot password?</button>
              </div>
              <button
                id="login-submit"
                type="submit"
                style={s.signInBtn(loading)}
                disabled={loading}
                onMouseEnter={e => { if (!loading) e.currentTarget.style.opacity = '0.9'; }}
                onMouseLeave={e => { e.currentTarget.style.opacity = '1'; }}
                onMouseDown={e => { e.currentTarget.style.transform = 'scale(0.98)'; }}
                onMouseUp={e => { e.currentTarget.style.transform = 'scale(1)'; }}
              >
                {loading
                  ? <><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ animation: 'spin 1s linear infinite' }}><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg> Signing in...</>
                  : <><svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2.5" viewBox="0 0 24 24"><path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg> Sign In</>
                }
              </button>
            </form>

            <div style={s.demoBox}>
              <div style={s.demoTitle}>Quick Demo Access</div>
              <button style={s.demoLink} onClick={e => fill(e, 'admin@buyit.com', 'Admin@123')}>👑 Admin — admin@buyit.com</button>
              <button style={s.demoLink} onClick={e => fill(e, 'vendor1@buyit.com', 'Vendor@123')}>🏪 Vendor — vendor1@buyit.com</button>
              <button style={s.demoLink} onClick={e => fill(e, 'customer@buyit.com', 'Customer@123')}>🛍️ Customer — customer@buyit.com</button>
            </div>

            <p style={s.registerLink}>
              Don&apos;t have an account?{' '}
              <Link to="/register" style={{ color: '#f97316', fontWeight: 600, textDecoration: 'none' }}>Create one →</Link>
            </p>
          </div>

          <div style={s.footer}>
            <span style={s.footerNote}>© 2025 BuyIt Inc.</span>
            <span style={s.footerLink}>Contact Us</span>
          </div>
        </div>
      </div>
      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

function RegisterPage({ addToast, onAuth }) {
  const [role, setRole] = useState('CUSTOMER');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [businessName, setBusinessName] = useState('');
  const [description, setDescription] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [pincode, setPincode] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const body = { name: name.trim(), email: email.trim(), phone: phone.trim(), password: password.trim(), role };
      if (role === 'VENDOR') {
        body.businessName = businessName.trim();
        body.description = description.trim();
        body.city = city.trim();
        body.state = state.trim();
        body.pincode = pincode.trim();
      }
      const res = await api('/auth/register', { method: 'POST', body });
      if (!res.success) { addToast(res.message || 'Registration failed', 'error'); return; }
      onAuth(res.token, res.user);
      addToast('Account created successfully! 🎉', 'success');
      navigate(role === 'VENDOR' ? '/vendor' : '/store', { replace: true });
    } catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  };

  const s = {
    page: { display: 'flex', minHeight: '100vh', background: '#f0f0f0', alignItems: 'center', justifyContent: 'center', fontFamily: "'Inter', 'Segoe UI', sans-serif", padding: '20px' },
    wrapper: { display: 'flex', width: '100%', maxWidth: 1020, minHeight: 620, borderRadius: 24, overflow: 'hidden', boxShadow: '0 32px 80px rgba(0,0,0,0.25)', background: '#fff' },
    left: { flex: 1, background: 'linear-gradient(150deg, #1a1008 0%, #2d1a08 40%, #1c1209 100%)', display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: '36px 40px', position: 'relative', overflow: 'hidden', minWidth: 0 },
    leftGlow: { position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', width: 340, height: 340, borderRadius: '50%', border: '1px solid rgba(255,255,255,0.06)', pointerEvents: 'none' },
    leftGlow2: { position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)', width: 520, height: 520, borderRadius: '50%', border: '1px solid rgba(255,255,255,0.03)', pointerEvents: 'none' },
    tagline: { fontSize: 13, color: 'rgba(255,255,255,0.45)', letterSpacing: 0.3, zIndex: 1 },
    heroText: { zIndex: 1 },
    h1: { fontSize: 44, fontWeight: 800, color: '#fff', lineHeight: 1.15, margin: '0 0 8px 0', letterSpacing: -1 },
    heroSub: { fontSize: 14, color: 'rgba(255,255,255,0.45)', margin: 0 },
    phoneWrap: { position: 'absolute', bottom: -20, left: '50%', transform: 'translateX(-50%)', width: 220, zIndex: 1 },
    phone: { width: '100%', borderRadius: 20, display: 'block', filter: 'drop-shadow(0 20px 40px rgba(0,0,0,0.6))' },
    right: { flex: 1.15, display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: '36px 44px', background: '#fff', minWidth: 320, maxHeight: '90vh', overflowY: 'auto' },
    rightTop: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
    signUpBtn: { display: 'flex', alignItems: 'center', gap: 6, fontSize: 14, color: '#555', cursor: 'pointer', background: 'none', border: 'none', fontFamily: 'inherit', textDecoration: 'none' },
    formArea: { flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' },
    heading: { fontSize: 32, fontWeight: 800, color: '#111', margin: '0 0 16px 0', letterSpacing: -0.8 },
    roleTabs: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, background: '#f1f5f9', padding: 4, borderRadius: 12, marginBottom: 16 },
    roleBtn: (active) => ({ padding: '10px', fontSize: 14, fontWeight: 700, border: 'none', borderRadius: 10, cursor: 'pointer', transition: 'all 0.2s', background: active ? 'linear-gradient(135deg, #f97316 0%, #ef4444 100%)' : 'transparent', color: active ? '#fff' : '#64748b', boxShadow: active ? '0 2px 10px rgba(249,115,22,0.3)' : 'none' }),
    inputWrap: { marginBottom: 10 },
    input: { width: '100%', padding: '12px 16px', fontSize: 14, border: '1.5px solid #e5e7eb', borderRadius: 10, outline: 'none', background: '#fafafa', color: '#111', boxSizing: 'border-box', transition: 'border-color 0.2s', fontFamily: 'inherit' },
    submitBtn: (loading) => ({ width: '100%', padding: '14px', fontSize: 15, fontWeight: 700, color: '#fff', background: loading ? '#f9a96e' : 'linear-gradient(135deg, #f97316 0%, #ef4444 100%)', border: 'none', borderRadius: 12, cursor: loading ? 'not-allowed' : 'pointer', transition: 'all 0.2s', fontFamily: 'inherit', boxShadow: '0 4px 20px rgba(249,115,22,0.35)', marginTop: 8 }),
    footer: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: 16, borderTop: '1px solid #f3f4f6', marginTop: 16 },
    footerNote: { fontSize: 12, color: '#9ca3af' },
    footerLink: { fontSize: 12, color: '#6b7280' },
    registerLink: { fontSize: 13, color: '#6b7280', textAlign: 'center', marginTop: 14 },
  };

  return (
    <div style={s.page}>
      <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
      <div style={s.wrapper}>
        {/* LEFT HERO PANEL */}
        <div style={s.left}>
          <div style={s.leftGlow} />
          <div style={s.leftGlow2} />
          <p style={s.tagline}>Join thousands of buyers & merchants.</p>
          <div style={s.heroText}>
            <h1 style={s.h1}>Start<br />selling &<br />shopping today</h1>
            <p style={s.heroSub}>Seamless marketplace experience built for speed.</p>
          </div>
          <div style={s.phoneWrap}>
            <img src="/login-hero.jpg" alt="BuyIt App" style={s.phone} />
          </div>
          <div style={{ height: 160 }} />
        </div>

        {/* RIGHT FORM PANEL */}
        <div style={s.right}>
          <div style={s.rightTop}>
            <BrandLogo size={20} />
            <Link to="/login" style={s.signUpBtn}>
              <svg width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path d="M15 3h4a2 2 0 012 2v14a2 2 0 01-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>
              Sign In
            </Link>
          </div>

          <div style={s.formArea}>
            <h2 style={s.heading}>Create Account</h2>

            <div style={s.roleTabs}>
              <button type="button" style={s.roleBtn(role === 'CUSTOMER')} onClick={() => setRole('CUSTOMER')}>🛍️ Customer</button>
              <button type="button" style={s.roleBtn(role === 'VENDOR')} onClick={() => setRole('VENDOR')}>🏪 Vendor / Seller</button>
            </div>

            <form onSubmit={handleSubmit} autoComplete="on">
              <div style={s.inputWrap}><input style={s.input} type="text" placeholder="Full Name *" value={name} onChange={e => setName(e.target.value)} required /></div>
              <div style={s.inputWrap}><input style={s.input} type="email" placeholder="Email Address *" value={email} onChange={e => setEmail(e.target.value)} required /></div>
              <div style={s.inputWrap}><input style={s.input} type="tel" placeholder="Phone Number" value={phone} onChange={e => setPhone(e.target.value)} /></div>
              <div style={s.inputWrap}><input style={s.input} type="password" placeholder="Password *" value={password} onChange={e => setPassword(e.target.value)} required /></div>

              {role === 'VENDOR' && (
                <>
                  <div style={s.inputWrap}><input style={s.input} type="text" placeholder="Business / Store Name *" value={businessName} onChange={e => setBusinessName(e.target.value)} required /></div>
                  <div style={s.inputWrap}><input style={s.input} type="text" placeholder="Short Description" value={description} onChange={e => setDescription(e.target.value)} /></div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 10 }}>
                    <input style={s.input} type="text" placeholder="City *" value={city} onChange={e => setCity(e.target.value)} required />
                    <input style={s.input} type="text" placeholder="State *" value={state} onChange={e => setState(e.target.value)} required />
                  </div>
                  <div style={s.inputWrap}><input style={s.input} type="text" placeholder="Pincode *" value={pincode} onChange={e => setPincode(e.target.value)} required /></div>
                </>
              )}

              <button type="submit" style={s.submitBtn(loading)} disabled={loading}>
                {loading ? 'Creating Account...' : (role === 'VENDOR' ? 'Register as Merchant' : 'Create Customer Account')}
              </button>
            </form>

            <p style={s.registerLink}>
              Already have an account?{' '}
              <Link to="/login" style={{ color: '#f97316', fontWeight: 600, textDecoration: 'none' }}>Sign In here →</Link>
            </p>
          </div>

          <div style={s.footer}>
            <span style={s.footerNote}>© 2025 BuyIt Inc.</span>
            <span style={s.footerLink}>Terms & Privacy</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function CustomerLayout({ user, onLogout }) {
  const [cartCount, setCartCount] = useState(0);
  const [wishlistCount, setWishlistCount] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  const refreshCart = useCallback(async () => {
    try { const d = await api('/cart'); setCartCount(d.count || (d.items || []).length || 0); } catch { }
  }, []);

  const refreshWishlist = useCallback(async () => {
    try { const d = await api('/wishlist'); setWishlistCount(d.count || (d.items || []).length || 0); } catch { }
  }, []);

  useEffect(() => {
    refreshCart();
    refreshWishlist();
  }, [refreshCart, refreshWishlist, location.pathname]);

  const handleSearch = (e) => { e.preventDefault(); navigate('/store?q=' + encodeURIComponent(searchQuery)); };

  return (
    <CartCtx.Provider value={{ cartCount, refreshCart, wishlistCount, refreshWishlist }}>
      <div className="app-layout">
        <header className="top-bar">
          <div className="top-bar-left">
            <Link to="/store" style={{ textDecoration: 'none' }}><BrandLogo size={22} /></Link>
            <form onSubmit={handleSearch} style={{ display: 'flex', flex: 1, maxWidth: 400 }}>
              <input className="search-box" type="text" placeholder="Search products, brands..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} />
            </form>
            <Link to="/store" className={'nav-pill' + (location.pathname === '/store' ? ' active' : '')}>Store</Link>
            <Link to="/orders" className={'nav-pill' + (location.pathname === '/orders' ? ' active' : '')}>Orders</Link>
            <Link to="/wishlist" className={'nav-pill' + (location.pathname === '/wishlist' ? ' active' : '')}>
              ❤️ Wishlist{wishlistCount > 0 && <span className="badge">{wishlistCount}</span>}
            </Link>
          </div>
          <div className="top-bar-right" style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Link to="/cart" className={'nav-pill' + (location.pathname === '/cart' ? ' active' : '')}>
              🛒 Cart{cartCount > 0 && <span className="badge">{cartCount}</span>}
            </Link>
            <NotificationBell />
            <span className="user-pill">👤 {user?.name}</span>
            <button className="btn-logout" onClick={onLogout}>Logout</button>
          </div>
        </header>
        <main className="main-area"><Outlet /></main>
      </div>
    </CartCtx.Provider>
  );
}

function StorePage({ addToast }) {
  const { refreshCart, refreshWishlist } = useCart();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [wishlistIds, setWishlistIds] = useState(new Set());
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [brand, setBrand] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [sort, setSort] = useState('');
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 24;
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    api('/wishlist').then(d => {
      setWishlistIds(new Set((d.items || []).map(x => x.id)));
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (_cachedCats && _cachedBrds) {
      setCategories(_cachedCats);
      setBrands(_cachedBrds);
      return;
    }
    Promise.all([
      _cachedCats ? Promise.resolve(_cachedCats) : api('/categories').then(d => { const a = Array.isArray(d) ? d : []; _cachedCats = a; return a; }).catch(() => []),
      _cachedBrds ? Promise.resolve(_cachedBrds) : api('/brands').then(d => { const a = Array.isArray(d) ? d : []; _cachedBrds = a; return a; }).catch(() => []),
    ]).then(([cats, brds]) => {
      setCategories(cats);
      setBrands(brds);
    });
  }, []);

  const loadProducts = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      const q = new URLSearchParams(location.search).get('q');
      if (q) params.set('search', q); else if (search) params.set('search', search);
      if (category) params.set('category', category);
      if (brand) params.set('brand', brand);
      if (minPrice) params.set('minPrice', minPrice);
      if (maxPrice) params.set('maxPrice', maxPrice);
      if (sort) params.set('sort', sort);
      const data = await api('/products?' + params.toString());
      setProducts(Array.isArray(data) ? data : []);
    } catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [category, brand, minPrice, maxPrice, sort, location.search, search, addToast]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadProducts();
    }, 150);
    return () => clearTimeout(timer);
  }, [loadProducts]);

  const addToCart = async (e, productId) => {
    e.stopPropagation();
    try { await api('/cart', { method: 'POST', body: { productId, quantity: 1 } }); addToast('Added to cart! 🛍️', 'success'); refreshCart(); }
    catch (err) { addToast(err.message, 'error'); }
  };

  const toggleWishlist = async (e, productId) => {
    e.stopPropagation();
    try {
      if (wishlistIds.has(productId)) {
        await api('/wishlist?productId=' + productId, { method: 'DELETE' });
        setWishlistIds(prev => {
          const next = new Set(prev);
          next.delete(productId);
          return next;
        });
        addToast('Removed from Wishlist', 'info');
      } else {
        await api('/wishlist', { method: 'POST', body: { productId } });
        setWishlistIds(prev => new Set(prev).add(productId));
        addToast('Added to Wishlist ❤️', 'success');
      }
      if (refreshWishlist) refreshWishlist();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const discountPct = (p) => (!p.discount || p.discount <= 0) ? 0 : Math.round(p.discount);
  const finalPrice = (p) => (!p.discount || p.discount <= 0) ? p.price : Math.max(0, p.price * (1 - p.discount / 100));

  return (
    <div className="store">
      {/* HERO BANNER */}
      <div style={{
        background: 'linear-gradient(150deg, #1a1008 0%, #2d1a08 50%, #1c1209 100%)',
        borderRadius: 24,
        padding: '2.5rem 3rem',
        color: '#fff',
        marginBottom: '2rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        position: 'relative',
        overflow: 'hidden',
        boxShadow: '0 20px 40px rgba(0,0,0,0.12)'
      }}>
        <div style={{
          position: 'absolute',
          right: -40,
          top: -40,
          width: 250,
          height: 250,
          borderRadius: '50%',
          border: '1px solid rgba(255,255,255,0.06)',
          pointerEvents: 'none'
        }} />
        <div style={{ zIndex: 1, maxWidth: 540 }}>
          <span style={{ fontSize: 13, fontWeight: 700, color: '#f97316', textTransform: 'uppercase', letterSpacing: 1 }}>Exclusive Marketplace</span>
          <h1 style={{ fontSize: 36, fontWeight: 800, margin: '8px 0', letterSpacing: -0.5, lineHeight: 1.2 }}>Discover Top Deals & Trending Products</h1>
          <p style={{ color: 'rgba(255,255,255,0.7)', fontSize: 15, margin: 0 }}>Shop verified vendor collections at unbeatable prices with fast door-step delivery.</p>
        </div>
        <div style={{ zIndex: 1, display: 'flex', gap: 12 }}>
          <span style={{
            background: 'rgba(255,255,255,0.1)',
            border: '1px solid rgba(255,255,255,0.15)',
            backdropFilter: 'blur(10px)',
            padding: '12px 20px',
            borderRadius: 16,
            textAlign: 'center'
          }}>
            <strong style={{ display: 'block', fontSize: 22, color: '#f97316' }}>100%</strong>
            <small style={{ fontSize: 12, color: 'rgba(255,255,255,0.7)' }}>Verified</small>
          </span>
          <span style={{
            background: 'rgba(255,255,255,0.1)',
            border: '1px solid rgba(255,255,255,0.15)',
            backdropFilter: 'blur(10px)',
            padding: '12px 20px',
            borderRadius: 16,
            textAlign: 'center'
          }}>
            <strong style={{ display: 'block', fontSize: 22, color: '#ef4444' }}>Fast</strong>
            <small style={{ fontSize: 12, color: 'rgba(255,255,255,0.7)' }}>Shipping</small>
          </span>
        </div>
      </div>

      <div className="store-filters">
        <input className="filter-search" type="text" placeholder="Search products..." value={search} onChange={e => setSearch(e.target.value)} onKeyDown={e => e.key === 'Enter' && loadProducts()} style={{ flex: 1.5 }} />
        <select value={category} onChange={e => setCategory(e.target.value)}><option value="">All Categories</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
        <select value={brand} onChange={e => setBrand(e.target.value)}><option value="">All Brands</option>{brands.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}</select>
        <input type="number" placeholder="Min ₹" value={minPrice} onChange={e => setMinPrice(e.target.value)} style={{ width: 100 }} />
        <input type="number" placeholder="Max ₹" value={maxPrice} onChange={e => setMaxPrice(e.target.value)} style={{ width: 100 }} />
        <select value={sort} onChange={e => setSort(e.target.value)}>
          <option value="">Sort By</option>
          <option value="price_asc">Price: Low to High</option>
          <option value="price_desc">Price: High to Low</option>
          <option value="name_asc">Name: A-Z</option>
          <option value="name_desc">Name: Z-A</option>
          <option value="rating">Top Rated</option>
        </select>
        <button className="btn-primary" onClick={loadProducts}>Search</button>
      </div>

      {loading ? <Loader /> : products.length === 0 ? <EmptyState message="No products found matching your filters" /> : (() => {
        const totalPages = Math.ceil(products.length / PAGE_SIZE) || 1;
        const startIndex = (page - 1) * PAGE_SIZE;
        const paginated = products.slice(startIndex, startIndex + PAGE_SIZE);

        const handlePageChange = (newPage) => {
          setPage(newPage);
          window.scrollTo({ top: 380, behavior: 'smooth' });
        };

        return (
          <>
            <div className="product-grid">
              {paginated.map(p => (
                <div key={p.id} className="product-card" onClick={() => navigate('/store/product/' + p.id)}>
                  <div className="product-img">
                    <button
                      type="button"
                      className={`product-wishlist-btn ${wishlistIds.has(p.id) ? 'active' : ''}`}
                      onClick={(e) => toggleWishlist(e, p.id)}
                      title={wishlistIds.has(p.id) ? "Remove from wishlist" : "Add to wishlist"}
                    >
                      {wishlistIds.has(p.id) ? '❤️' : '🤍'}
                    </button>
                    <ProductImage src={p.image} alt={p.name} />
                    {discountPct(p) > 0 && <span className="discount-badge">-{discountPct(p)}%</span>}
                  </div>
                  <div className="product-info">
                    <span className="p-category">{p.categoryName}</span>
                    <h3>{p.name}</h3>
                    <span className="p-vendor">Sold by {p.vendorName}</span>
                    {p.averageRating > 0 && <span className="p-rating"><StarRating rating={p.averageRating} /> ({p.reviewCount})</span>}
                    <div className="p-price">
                      <span className="final">{fmt(finalPrice(p))}</span>
                      {discountPct(p) > 0 && <span className="original">{fmt(p.price)}</span>}
                      {discountPct(p) > 0 && <span className="disc-tag">-{discountPct(p)}%</span>}
                    </div>
                    <button className="btn-add-cart" onClick={e => addToCart(e, p.id)}>Add to Cart</button>
                  </div>
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="store-pagination-wrapper">
                <span className="pagination-stats">
                  Showing <strong>{startIndex + 1}</strong> – <strong>{Math.min(startIndex + PAGE_SIZE, products.length)}</strong> of <strong>{products.length}</strong> products
                </span>
                <div className="store-pagination">
                  <button
                    type="button"
                    className="page-btn"
                    onClick={() => handlePageChange(page - 1)}
                    disabled={page <= 1}
                  >
                    ← Prev
                  </button>
                  {Array.from({ length: totalPages }, (_, i) => i + 1)
                    .filter(pNum => pNum === 1 || pNum === totalPages || Math.abs(pNum - page) <= 2)
                    .reduce((acc, pNum, idx, arr) => {
                      if (idx > 0 && pNum - arr[idx - 1] > 1) acc.push(-1 * pNum);
                      acc.push(pNum);
                      return acc;
                    }, [])
                    .map(pNum => pNum < 0 ? (
                      <span key={pNum} className="page-dots">…</span>
                    ) : (
                      <button
                        key={pNum}
                        type="button"
                        className={`page-btn ${pNum === page ? 'active' : ''}`}
                        onClick={() => handlePageChange(pNum)}
                      >
                        {pNum}
                      </button>
                    ))}
                  <button
                    type="button"
                    className="page-btn"
                    onClick={() => handlePageChange(page + 1)}
                    disabled={page >= totalPages}
                  >
                    Next →
                  </button>
                </div>
              </div>
            )}
          </>
        );
      })()}
    </div>
  );
}

function ProductDetailsPage({ addToast }) {
  const { refreshCart, refreshWishlist } = useCart();
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [related, setRelated] = useState([]);
  const [loading, setLoading] = useState(true);
  const [qty, setQty] = useState(1);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewHover, setReviewHover] = useState(0);
  const [reviewComment, setReviewComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [addingCart, setAddingCart] = useState(false);
  const [wishlisted, setWishlisted] = useState(false);
  const [activeImgIndex, setActiveImgIndex] = useState(0);

  useEffect(() => {
    setActiveImgIndex(0);
  }, [id]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [pRes, rRes] = await Promise.all([
        api('/products/' + id),
        api('/reviews/' + id).catch(() => []),
        api('/wishlist').then(d => (d.items || []).some(x => x.id === Number(id))).catch(() => false)
      ]);
      const rawProd = pRes.product !== undefined ? pRes.product : pRes;
      const pObj = typeof rawProd === 'string' ? JSON.parse(rawProd) : rawProd;
      setProduct(pObj);
      setReviews(Array.isArray(rRes) ? rRes : []);

      // Wishlist check
      api('/wishlist').then(d => {
        setWishlisted((d.items || []).some(x => x.id === Number(id)));
      }).catch(() => {});

      if (pObj && pObj.categoryId) {
        api('/products?category=' + pObj.categoryId)
          .then(list => {
            if (Array.isArray(list)) {
              setRelated(list.filter(item => item.id !== pObj.id).slice(0, 4));
            }
          })
          .catch(() => {});
      }
    } catch (err) {
      addToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [id, addToast]);

  useEffect(() => {
    load();
    window.scrollTo(0, 0);
  }, [load]);

  const addToCart = async (redirect = false) => {
    setAddingCart(true);
    try {
      await api('/cart', { method: 'POST', body: { productId: Number(id), quantity: qty } });
      addToast('Added ' + qty + ' item' + (qty > 1 ? 's' : '') + ' to cart 🛒', 'success');
      refreshCart();
      if (redirect) navigate('/cart');
    } catch (err) {
      addToast(err.message, 'error');
    } finally {
      setAddingCart(false);
    }
  };

  const toggleWishlist = async () => {
    try {
      if (wishlisted) {
        await api('/wishlist?productId=' + id, { method: 'DELETE' });
        setWishlisted(false);
        addToast('Removed from Wishlist', 'info');
      } else {
        await api('/wishlist', { method: 'POST', body: { productId: Number(id) } });
        setWishlisted(true);
        addToast('Added to your Wishlist ❤️', 'success');
      }
      if (refreshWishlist) refreshWishlist();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const submitReview = async (e) => {
    e.preventDefault();
    if (!reviewComment.trim()) {
      addToast('Please enter a review comment', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await api('/reviews', {
        method: 'POST',
        body: { productId: Number(id), rating: reviewRating, comment: reviewComment.trim() }
      });
      addToast('Thank you for your review! ⭐', 'success');
      setReviewComment('');
      setReviewRating(5);
      load();
    } catch (err) {
      addToast(err.message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <Loader />;
  if (!product) return <EmptyState message="Product not found" />;

  const fp = (!product.discount || product.discount <= 0)
    ? product.price
    : Math.max(0, product.price * (1 - product.discount / 100));
  const dp = (!product.discount || product.discount <= 0) ? 0 : Math.round(product.discount);
  const savings = product.price - fp;
  const isOutOfStock = !product.stockQuantity || product.stockQuantity <= 0;
  const galleryImages = (product?.images && product.images.length > 0)
    ? product.images
    : (product?.image ? [product.image] : []);
  const currentImage = galleryImages[activeImgIndex] || product?.image;

  return (
    <div className="product-details-container">
      {/* Top Breadcrumb Bar */}
      <div className="pd-breadcrumb-bar">
        <button className="btn-back-link" onClick={() => navigate(-1)}>
          <span className="back-arrow">←</span> Back to Store
        </button>
        <div className="pd-breadcrumbs">
          <Link to="/store">Store</Link>
          <span className="sep">/</span>
          <span>{product.categoryName || 'Products'}</span>
          {product.brandName && (
            <>
              <span className="sep">/</span>
              <span>{product.brandName}</span>
            </>
          )}
          <span className="sep">/</span>
          <span className="current">{product.name}</span>
        </div>
      </div>

      {/* Main Two-Column Showcase */}
      <div className="pd-main-grid">
        {/* Left Column: Image Gallery & Guarantees */}
        <div className="pd-gallery-column">
          <div className="pd-image-card">
            {dp > 0 && <div className="pd-discount-badge">-{dp}% OFF</div>}
            {product.categoryName && <div className="pd-category-pill">{product.categoryName}</div>}
            <button
              className={`pd-wishlist-btn ${wishlisted ? 'active' : ''}`}
              onClick={toggleWishlist}
              title="Save to wishlist"
            >
              {wishlisted ? '❤️' : '🤍'}
            </button>
            <div className="pd-image-wrapper" style={{ position: 'relative' }}>
              <ProductImage
                src={currentImage}
                alt={product.name}
              />
              {galleryImages.length > 1 && (
                <>
                  <button
                    type="button"
                    className="gallery-nav-btn prev"
                    onClick={(e) => { e.stopPropagation(); setActiveImgIndex(i => (i - 1 + galleryImages.length) % galleryImages.length); }}
                    title="Previous Image"
                  >
                    ‹
                  </button>
                  <button
                    type="button"
                    className="gallery-nav-btn next"
                    onClick={(e) => { e.stopPropagation(); setActiveImgIndex(i => (i + 1) % galleryImages.length); }}
                    title="Next Image"
                  >
                    ›
                  </button>
                </>
              )}
            </div>

            {/* Gallery Thumbnails Strip */}
            {galleryImages.length > 1 && (
              <div className="pd-thumbnails-strip">
                {galleryImages.map((imgUrl, idx) => (
                  <button
                    key={idx}
                    type="button"
                    className={`pd-thumbnail-item ${idx === activeImgIndex ? 'active' : ''}`}
                    onClick={() => setActiveImgIndex(idx)}
                    onMouseEnter={() => setActiveImgIndex(idx)}
                    title={`View photo ${idx + 1}`}
                  >
                    <ProductImage src={imgUrl} alt={`${product.name} view ${idx + 1}`} />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Value Props & Trust Badges */}
          <div className="pd-trust-card">
            <div className="trust-item">
              <div className="trust-icon">🚚</div>
              <div>
                <strong>Free Express Shipping</strong>
                <p>Delivery across India within 2-4 business days</p>
              </div>
            </div>
            <div className="trust-item">
              <div className="trust-icon">🔄</div>
              <div>
                <strong>7-Day Replacement</strong>
                <p>Hassle-free replacement for damaged items</p>
              </div>
            </div>
            <div className="trust-item">
              <div className="trust-icon">🛡️</div>
              <div>
                <strong>100% Genuine Warranty</strong>
                <p>Authentic brand item with warranty support</p>
              </div>
            </div>
            <div className="trust-item">
              <div className="trust-icon">💳</div>
              <div>
                <strong>Secure Payment Options</strong>
                <p>UPI, Cards, Net Banking & Cash on Delivery</p>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Product Info & Purchase Actions */}
        <div className="pd-info-column">
          {product.brandName && (
            <div className="pd-brand-badge">
              <span>🏷️ Brand: <strong>{product.brandName}</strong></span>
            </div>
          )}

          <h1 className="pd-title">{product.name}</h1>

          <div className="pd-meta-row">
            <div className="pd-rating-box">
              <StarRating rating={product.averageRating || 5} />
              <span className="pd-rating-num">
                {product.averageRating > 0 ? Number(product.averageRating).toFixed(1) : '5.0'}
              </span>
              <a href="#reviews" className="pd-reviews-link">
                ({product.reviewCount || reviews.length} {(product.reviewCount || reviews.length) === 1 ? 'review' : 'reviews'})
              </a>
            </div>
            <div className="pd-vendor-chip">
              <span>🏪 Sold by <strong>{product.vendorName || 'TechHub Electronics'}</strong></span>
              <span className="verified-check" title="Verified Seller">✓</span>
            </div>
          </div>

          <div className="pd-divider" />

          {/* Pricing Box */}
          <div className="pd-price-box">
            <div className="pd-price-main">
              <span className="pd-final-price">{fmt(fp)}</span>
              {dp > 0 && <span className="pd-old-price">{fmt(product.price)}</span>}
            </div>
            {dp > 0 && (
              <div className="pd-savings-badge">
                You Save {fmt(savings)} ({dp}% off)
              </div>
            )}
            <span className="pd-tax-text">Inclusive of all applicable taxes & duties</span>
          </div>

          {/* Stock Status Indicator */}
          <div className="pd-stock-row">
            {isOutOfStock ? (
              <span className="stock-pill stock-out">🔴 Currently Out of Stock</span>
            ) : product.stockQuantity <= 5 ? (
              <span className="stock-pill stock-low">🟠 Only {product.stockQuantity} items left in stock - order soon!</span>
            ) : (
              <span className="stock-pill stock-in">🟢 In Stock ({product.stockQuantity} available)</span>
            )}
            {product.sku && <span className="pd-sku-chip">SKU: {product.sku}</span>}
          </div>

          {/* Product Description */}
          {product.description && (
            <div className="pd-desc-block">
              <h3>About this item</h3>
              <p>{product.description}</p>
            </div>
          )}

          {/* Specifications Matrix */}
          <div className="pd-specs-table">
            <div className="spec-row">
              <span className="spec-label">Brand</span>
              <span className="spec-val">{product.brandName || 'Standard'}</span>
            </div>
            <div className="spec-row">
              <span className="spec-label">Category</span>
              <span className="spec-val">{product.categoryName || 'General'}</span>
            </div>
            <div className="spec-row">
              <span className="spec-label">Item SKU</span>
              <span className="spec-val">{product.sku || 'N/A'}</span>
            </div>
            <div className="spec-row">
              <span className="spec-label">Seller</span>
              <span className="spec-val">{product.vendorName || 'TechHub Electronics'}</span>
            </div>
          </div>

          {/* Purchase Actions Panel */}
          <div className="pd-actions-panel">
            {!isOutOfStock && (
              <div className="pd-qty-group">
                <label>Quantity</label>
                <div className="pd-qty-stepper">
                  <button type="button" onClick={() => setQty(Math.max(1, qty - 1))} disabled={qty <= 1}>-</button>
                  <span className="qty-number">{qty}</span>
                  <button type="button" onClick={() => setQty(Math.min(product.stockQuantity, qty + 1))} disabled={qty >= product.stockQuantity}>+</button>
                </div>
              </div>
            )}

            <div className="pd-btn-group">
              <button
                className="btn-add-cart-main"
                onClick={() => addToCart(false)}
                disabled={isOutOfStock || addingCart}
              >
                🛒 {addingCart ? 'Adding...' : 'Add to Cart'}
              </button>
              <button
                className="btn-buy-now-main"
                onClick={() => addToCart(true)}
                disabled={isOutOfStock}
              >
                ⚡ Buy Now
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Customer Reviews Section */}
      <div className="pd-reviews-container" id="reviews">
        <div className="pd-section-header">
          <div>
            <h2>Customer Reviews & Ratings</h2>
            <p className="section-sub">Real feedback from verified buyers across India</p>
          </div>
          <div className="pd-rating-summary-pill">
            <span className="avg-num">{product.averageRating > 0 ? Number(product.averageRating).toFixed(1) : '5.0'}</span>
            <StarRating rating={product.averageRating || 5} />
            <span className="count-label">Based on {reviews.length} {reviews.length === 1 ? 'review' : 'reviews'}</span>
          </div>
        </div>

        <div className="pd-reviews-grid">
          {/* Write a Review Card */}
          <div className="pd-write-review-card">
            <h3>Write a Review</h3>
            <p className="write-hint">Share your thoughts with other shoppers</p>
            <form onSubmit={submitReview}>
              <div className="rating-select-row">
                <label>Your Rating:</label>
                <div className="star-picker">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      type="button"
                      key={star}
                      className={`star-btn ${(reviewHover || reviewRating) >= star ? 'active' : ''}`}
                      onMouseEnter={() => setReviewHover(star)}
                      onMouseLeave={() => setReviewHover(0)}
                      onClick={() => setReviewRating(star)}
                    >
                      ★
                    </button>
                  ))}
                  <span className="star-label">
                    {['Poor', 'Fair', 'Good', 'Very Good', 'Excellent'][(reviewHover || reviewRating) - 1]}
                  </span>
                </div>
              </div>

              <div className="field-block">
                <label>Your Experience / Comments</label>
                <textarea
                  placeholder="What did you like or dislike? How was the build quality, performance, and packaging?"
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                  rows={4}
                  required
                />
              </div>

              <button type="submit" className="btn-submit-review" disabled={submitting}>
                {submitting ? 'Submitting...' : 'Post Review'}
              </button>
            </form>
          </div>

          {/* Customer Reviews List */}
          <div className="pd-reviews-list-card">
            <h3>Buyer Feedback ({reviews.length})</h3>
            {reviews.length === 0 ? (
              <div className="no-reviews-box">
                <div className="no-rev-icon">💬</div>
                <h4>No reviews yet</h4>
                <p>Be the first customer to review <strong>{product.name}</strong>!</p>
              </div>
            ) : (
              <div className="reviews-scroll-list">
                {reviews.map((r) => {
                  const initial = (r.customerName || 'Customer').charAt(0).toUpperCase();
                  return (
                    <div key={r.id} className="review-item-card">
                      <div className="rev-user-header">
                        <div className="user-avatar-circle">{initial}</div>
                        <div className="user-info-text">
                          <div className="user-name-line">
                            <strong>{r.customerName || 'Verified Shopper'}</strong>
                            <span className="verified-badge">✓ Verified Buyer</span>
                          </div>
                          <div className="user-meta-sub">
                            <StarRating rating={r.rating} />
                            <span className="rev-date">
                              {r.createdAt ? new Date(r.createdAt).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' }) : 'Recently'}
                            </span>
                          </div>
                        </div>
                      </div>
                      <p className="rev-comment-body">{r.comment}</p>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Related Products Carousel / Grid */}
      {related.length > 0 && (
        <div className="pd-related-container">
          <div className="pd-section-header">
            <div>
              <h2>Similar Products in {product.categoryName || 'Store'}</h2>
              <p className="section-sub">Customers who viewed this item also looked at</p>
            </div>
          </div>
          <div className="related-products-grid">
            {related.map((item) => {
              const itemFp = (!item.discount || item.discount <= 0)
                ? item.price
                : Math.max(0, item.price * (1 - item.discount / 100));
              return (
                <div key={item.id} className="related-card" onClick={() => { navigate('/store/product/' + item.id); window.scrollTo(0, 0); }}>
                  <div className="related-img-box">
                    <ProductImage src={item.image} alt={item.name} />
                  </div>
                  <div className="related-content">
                    <span className="related-cat">{item.categoryName}</span>
                    <h4 className="related-title">{item.name}</h4>
                    <div className="related-price-row">
                      <span className="rel-final">{fmt(itemFp)}</span>
                      {item.discount > 0 && <span className="rel-old">{fmt(item.price)}</span>}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

function CartPage({ addToast }) {
  const { refreshCart } = useCart();
  const [cart, setCart] = useState({ items: [], total: 0, count: 0 });
  const [loading, setLoading] = useState(true);
  const [clearing, setClearing] = useState(false);
  const navigate = useNavigate();
  const loadCart = useCallback(async () => {
    setLoading(true);
    try { setCart(await api('/cart')); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);
  useEffect(() => { loadCart(); }, [loadCart]);

  const updateQty = async (productId, quantity) => {
    try { await api('/cart', { method: 'PUT', body: { productId, quantity } }); loadCart(); refreshCart(); }
    catch (err) { addToast(err.message, 'error'); }
  };

  const removeItem = async (productId) => {
    try { await api('/cart', { method: 'DELETE', body: { productId } }); addToast('Item removed from cart', 'success'); loadCart(); refreshCart(); }
    catch (err) { addToast(err.message, 'error'); }
  };

  const handleClearCart = async () => {
    if (!window.confirm('Are you sure you want to remove all items from your cart?')) return;
    setClearing(true);
    try {
      await api('/cart', { method: 'DELETE', body: { productId: 0 } });
      addToast('Cart cleared', 'success');
      loadCart();
      refreshCart();
    } catch (err) { addToast(err.message, 'error'); }
    finally { setClearing(false); }
  };

  if (loading) return <Loader />;
  const items = cart.items || [];
  const totalItemCount = items.reduce((acc, i) => acc + (i.quantity || 1), 0);

  return (
    <div className="cart-page">
      <div className="cart-page-header">
        <div>
          <h1>Shopping Cart <span className="cart-badge-count">{totalItemCount} {totalItemCount === 1 ? 'item' : 'items'}</span></h1>
          <p className="cart-subtitle">Review items in your cart and proceed to express checkout</p>
        </div>
        {items.length > 0 && (
          <button className="btn-clear-cart" onClick={handleClearCart} disabled={clearing}>
            🗑️ Clear Cart
          </button>
        )}
      </div>

      {items.length === 0 ? (
        <div className="cart-empty-box">
          <div className="cart-empty-icon">🛒</div>
          <h2>Your Cart is Empty</h2>
          <p>Explore our wide collection of trending products and add items to your cart.</p>
          <button className="btn-primary btn-lg" onClick={() => navigate('/store')}>
            Explore Store Catalog →
          </button>
        </div>
      ) : (
        <div className="cart-main-layout">
          <div className="cart-items-container">
            <div className="cart-free-shipping-banner">
              <span className="banner-icon">⚡</span>
              <div>
                <strong>FREE Express Shipping Unlocked!</strong>
                <span>All items in your bag qualify for free door-step expedited delivery.</span>
              </div>
            </div>

            <div className="cart-items">
              {items.map(item => (
                <div key={item.id} className="cart-item">
                  <div className="ci-img" onClick={() => navigate('/store/product/' + item.productId)} style={{ cursor: 'pointer' }} title="View product details">
                    <ProductImage src={item.productImage} alt={item.productName} />
                  </div>
                  <div className="ci-info">
                    <h3 onClick={() => navigate('/store/product/' + item.productId)} style={{ cursor: 'pointer' }}>
                      {item.productName}
                    </h3>
                    <span className="ci-vendor">Sold by: <strong>{item.vendorName || 'BuyIt Verified Merchant'}</strong></span>
                    <div className="ci-price-wrap">
                      <span className="ci-unit-price">{fmt(item.price)} each</span>
                      {item.stockQuantity > 0 && item.stockQuantity < 10 && (
                        <span className="ci-low-stock">Only {item.stockQuantity} left in stock</span>
                      )}
                    </div>
                  </div>
                  <div className="ci-qty">
                    <button onClick={() => updateQty(item.productId, Math.max(1, item.quantity - 1))} title="Decrease quantity">-</button>
                    <span>{item.quantity}</span>
                    <button onClick={() => updateQty(item.productId, item.quantity + 1)} title="Increase quantity">+</button>
                  </div>
                  <div className="ci-subtotal-box">
                    <span className="ci-subtotal-label">Subtotal</span>
                    <span className="ci-subtotal">{fmt(item.subtotal)}</span>
                  </div>
                  <button className="btn-remove" onClick={() => removeItem(item.productId)} title="Remove item from cart" aria-label="Remove item">
                    <svg width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                      <path d="M3 6h18M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2M10 11v6M14 11v6"/>
                    </svg>
                  </button>
                </div>
              ))}
            </div>

            <div className="cart-perks">
              <div className="perk-item">
                <span>🛡️</span>
                <div><strong>100% Genuine</strong><p>Authentic brand guarantee</p></div>
              </div>
              <div className="perk-item">
                <span>🔄</span>
                <div><strong>7 Days Return</strong><p>Hassle-free easy returns</p></div>
              </div>
              <div className="perk-item">
                <span>🔒</span>
                <div><strong>Secure Checkout</strong><p>256-bit SSL encrypted</p></div>
              </div>
            </div>
          </div>

          <div className="cart-summary">
            <h2>Order Summary</h2>
            <div className="summary-row">
              <span>Items Subtotal ({totalItemCount} pcs)</span>
              <span>{fmt(cart.total)}</span>
            </div>
            <div className="summary-row">
              <span>Delivery Charges</span>
              <span style={{ color: '#10b981', fontWeight: 700 }}>FREE</span>
            </div>
            <div className="summary-row">
              <span>Estimated Taxes & Packaging</span>
              <span>₹0.00</span>
            </div>
            <div className="total">
              <span>Estimated Total</span>
              <span>{fmt(cart.total)}</span>
            </div>
            <button className="btn-primary btn-lg btn-full" style={{ marginTop: 20 }} onClick={() => navigate('/checkout')}>
              Proceed to Checkout →
            </button>
            <button className="btn-secondary btn-full" style={{ marginTop: 10 }} onClick={() => navigate('/store')}>
              ← Continue Shopping
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function CheckoutPage({ addToast }) {
  const { refreshCart } = useCart();
  const navigate = useNavigate();
  const [addresses, setAddresses] = useState([]);
  const [selectedAddress, setSelectedAddress] = useState(null);
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [cart, setCart] = useState({ items: [], total: 0 });
  const [paymentMethod, setPaymentMethod] = useState('COD');
  const [couponCode, setCouponCode] = useState('');
  const [discountAmount, setDiscountAmount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [placing, setPlacing] = useState(false);
  const [addrForm, setAddrForm] = useState({ fullName: '', phone: '', addressLine: '', city: '', state: '', pincode: '', isDefault: false });
  useEffect(() => {
    Promise.all([
      api('/addresses').then(d => { const a = Array.isArray(d) ? d : []; setAddresses(a); if (a.length) setSelectedAddress(a.find(x => x.isDefault)?.id || a[0].id); }).catch(() => {}),
      api('/cart').then(d => setCart(d)).catch(() => {}),
    ]).finally(() => setLoading(false));
  }, []);
  const saveAddress = async (e) => {
    e.preventDefault();
    try {
      const res = await api('/addresses', { method: 'POST', body: addrForm });
      addToast('Address saved', 'success'); setShowAddressForm(false);
      setAddrForm({ fullName: '', phone: '', addressLine: '', city: '', state: '', pincode: '', isDefault: false });
      const list = await api('/addresses'); setAddresses(Array.isArray(list) ? list : []);
      if (res.id) setSelectedAddress(res.id);
    } catch (err) { addToast(err.message, 'error'); }
  };
  const applyCoupon = async () => {
    if (!couponCode.trim()) return;
    try {
      const res = await api('/coupons', { method: 'POST', body: { code: couponCode.trim(), amount: cart.total } });
      if (res.success) { setDiscountAmount(res.discount || 0); addToast('Coupon applied! ' + fmt(res.discount) + ' off', 'success'); }
      else addToast(res.message || 'Invalid coupon', 'error');
    } catch (err) { addToast(err.message, 'error'); }
  };
  const placeOrder = async () => {
    if (!selectedAddress) { addToast('Please select a shipping address', 'error'); return; }
    const items = (cart.items || []).map(i => ({ productId: i.productId, quantity: i.quantity }));
    if (!items.length) { addToast('Cart is empty', 'error'); return; }
    setPlacing(true);
    try { await api('/orders', { method: 'POST', body: { items, addressId: selectedAddress, paymentMethod, discountAmount } }); addToast('Order placed successfully! 🎉', 'success'); refreshCart(); navigate('/orders'); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setPlacing(false); }
  };
  if (loading) return <Loader />;
  const finalTotal = Math.max(0, (cart.total || 0) - discountAmount);
  return (
    <div className="checkout">
      <h1>Checkout</h1>
      <div className="checkout-layout">
        <div className="checkout-left">
          <h2>Shipping Address</h2>
          {addresses.map(a => (
            <div key={a.id} className={'address-card' + (selectedAddress === a.id ? ' selected' : '')} onClick={() => setSelectedAddress(a.id)}>
              <strong>{a.fullName}</strong> - {a.phone}<br />{a.addressLine}, {a.city}, {a.state} - {a.pincode}
            </div>
          ))}
          {showAddressForm ? (
            <form className="address-card" onSubmit={saveAddress} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <input placeholder="Full Name *" value={addrForm.fullName} onChange={e => setAddrForm({ ...addrForm, fullName: e.target.value })} required />
              <input placeholder="Phone Number *" value={addrForm.phone} onChange={e => setAddrForm({ ...addrForm, phone: e.target.value })} required />
              <input placeholder="Address Line (Street, Flat/House No.) *" value={addrForm.addressLine} onChange={e => setAddrForm({ ...addrForm, addressLine: e.target.value })} required />
              <div className="field-row">
                <input placeholder="City *" value={addrForm.city} onChange={e => setAddrForm({ ...addrForm, city: e.target.value })} required style={{ flex: 1 }} />
                <input placeholder="State *" value={addrForm.state} onChange={e => setAddrForm({ ...addrForm, state: e.target.value })} style={{ flex: 1 }} />
              </div>
              <input placeholder="Pincode *" value={addrForm.pincode} onChange={e => setAddrForm({ ...addrForm, pincode: e.target.value })} required />
              <div className="field-row">
                <button type="submit" className="btn-primary">Save Address</button>
                <button type="button" className="btn-secondary" onClick={() => setShowAddressForm(false)}>Cancel</button>
              </div>
            </form>
          ) : <button className="btn-secondary" onClick={() => setShowAddressForm(true)}>+ Add New Address</button>}
          <h2 style={{ marginTop: 28 }}>Payment Method</h2>
          {['COD', 'UPI', 'CARD', 'NETBANKING'].map(m => (
            <label key={m} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 14px', background: paymentMethod === m ? '#fff7ed' : '#fff', border: `1.5px solid ${paymentMethod === m ? '#f97316' : '#e2e8f0'}`, borderRadius: 10, marginBottom: 8, cursor: 'pointer', transition: 'all 0.2s' }}>
              <input type="radio" name="payment" value={m} checked={paymentMethod === m} onChange={() => setPaymentMethod(m)} />
              <strong style={{ fontSize: 14 }}>{m === 'COD' ? '💵 Cash on Delivery' : m === 'UPI' ? '⚡ Instant UPI (GPay / PhonePe / Paytm)' : m === 'CARD' ? '💳 Credit / Debit Card' : '🏦 Net Banking'}</strong>
            </label>
          ))}
        </div>
        <div className="checkout-right">
          <h2>Order Summary</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 16 }}>
            {(cart.items || []).map(i => (
              <div key={i.id} style={{ display: 'flex', alignItems: 'center', gap: 12, paddingBottom: 10, borderBottom: '1px solid #f1f5f9' }}>
                <div style={{ width: 46, height: 46, borderRadius: 8, overflow: 'hidden', flexShrink: 0, background: '#f8fafc', border: '1px solid #e2e8f0' }}>
                  <ProductImage src={i.productImage} alt={i.productName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 600, fontSize: 13, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{i.productName}</div>
                  <div style={{ fontSize: 12, color: '#64748b' }}>Qty: {i.quantity} × {fmt(i.price)}</div>
                </div>
                <span style={{ fontWeight: 700, fontSize: 13 }}>{fmt(i.subtotal)}</span>
              </div>
            ))}
          </div>
          <div className="coupon-row">
            <input type="text" placeholder="Coupon code (e.g. WELCOME10)" value={couponCode} onChange={e => setCouponCode(e.target.value)} />
            <button className="btn-secondary" onClick={applyCoupon}>Apply</button>
          </div>
          <div className="summary-row"><span>Subtotal</span><span>{fmt(cart.total)}</span></div>
          {discountAmount > 0 && <div className="summary-row discount"><span>Discount Applied</span><span>-{fmt(discountAmount)}</span></div>}
          <div className="summary-row"><span>Shipping</span><span style={{ color: '#10b981', fontWeight: 700 }}>FREE</span></div>
          <div className="total"><span>Total</span><span>{fmt(finalTotal)}</span></div>
          <div className="checkout-actions" style={{ marginTop: 20 }}>
            <button className="btn-secondary" onClick={() => navigate('/cart')}>Back to Cart</button>
            <button className="btn-primary btn-lg" onClick={placeOrder} disabled={placing}>{placing ? 'Placing Order...' : 'Place Order'}</button>
          </div>
        </div>
      </div>
    </div>
  );
}

function OrdersPage({ addToast }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/orders'); setOrders(Array.isArray(d) ? d : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);
  useEffect(() => { load(); }, [load]);
  const cancelOrder = async (orderId) => {
    if (!window.confirm('Cancel this order?')) return;
    try { await api('/orders/' + orderId + '/cancel', { method: 'POST' }); addToast('Order cancelled', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  if (loading) return <Loader />;
  return (
    <div className="orders-page">
      <h1>My Orders</h1>
      {orders.length === 0 ? <EmptyState message="No orders yet" /> : orders.map(order => (
        <div key={order.id} className="order-card">
          <div className="order-header">
            <span>Order #{order.id}</span>
            <span className={statusClass(order.orderStatus)}>{order.orderStatus}</span>
            <span className="note">{order.createdAt ? new Date(order.createdAt).toLocaleDateString() : ''}</span>
          </div>
          {(order.items || []).map(item => (
            <div key={item.id} className="order-item">
              <span>{item.productName}</span><span className="oi-vendor">by {item.vendorName}</span>
              <span>{item.quantity} x {fmt(item.price)}</span><span>{fmt(item.subtotal)}</span>
              <span className={statusClass(item.itemStatus)}>{item.itemStatus}</span>
            </div>
          ))}
          <div className="order-footer">
            <span>Total: {fmt(order.finalAmount)}</span><span>Payment: {order.paymentStatus}</span>
            {order.orderStatus === 'PLACED' && <button className="btn-secondary" onClick={() => cancelOrder(order.id)}>Cancel Order</button>}
          </div>
        </div>
      ))}
    </div>
  );
}


function WishlistPage({ addToast }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const { refreshCart, refreshWishlist } = useCart();
  const navigate = useNavigate();

  const loadWishlist = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api('/wishlist');
      setItems(data.items || []);
    } catch (err) {
      addToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => { loadWishlist(); }, [loadWishlist]);

  const removeFromWishlist = async (productId) => {
    try {
      await api('/wishlist?productId=' + productId, { method: 'DELETE' });
      setItems(prev => prev.filter(i => i.id !== productId));
      if (refreshWishlist) refreshWishlist();
      addToast('Item removed from wishlist', 'info');
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const moveToCart = async (product) => {
    try {
      await api('/cart', { method: 'POST', body: { productId: product.id, quantity: 1 } });
      await api('/wishlist?productId=' + product.id, { method: 'DELETE' });
      setItems(prev => prev.filter(i => i.id !== product.id));
      refreshCart();
      if (refreshWishlist) refreshWishlist();
      addToast(`Moved "${product.name}" to cart! 🛒`, 'success');
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  if (loading) return <Loader />;

  return (
    <div className="wishlist-container">
      <div className="wishlist-header">
        <div>
          <h1>My Wishlist</h1>
          <p style={{ color: '#64748b', fontSize: '0.9rem', marginTop: 4 }}>Saved items you love</p>
        </div>
        <span className="wishlist-count-pill">{items.length} {items.length === 1 ? 'item' : 'items'}</span>
      </div>

      {items.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '60px 20px', background: '#fff', borderRadius: 16, border: '1px solid #e2e8f0' }}>
          <div style={{ fontSize: '3rem', marginBottom: 16 }}>🤍</div>
          <h2 style={{ fontSize: '1.4rem', color: '#0f172a', marginBottom: 8 }}>Your Wishlist is Empty</h2>
          <p style={{ color: '#64748b', marginBottom: 24 }}>Explore our store to find and save products you love!</p>
          <button className="btn-primary" onClick={() => navigate('/store')}>Browse Store</button>
        </div>
      ) : (
        <div className="wishlist-grid">
          {items.map(p => {
            const fp = (!p.discount || p.discount <= 0) ? p.price : Math.max(0, p.price * (1 - p.discount / 100));
            return (
              <div key={p.id} className="wishlist-card">
                <div className="wishlist-card-img" onClick={() => navigate('/store/product/' + p.id)}>
                  <ProductImage src={p.image} alt={p.name} />
                  <button
                    type="button"
                    className="wishlist-remove-btn"
                    onClick={(e) => { e.stopPropagation(); removeFromWishlist(p.id); }}
                    title="Remove from wishlist"
                  >
                    ✕
                  </button>
                </div>
                <div className="wishlist-card-body">
                  <span className="wishlist-card-category">{p.categoryName || 'Product'}</span>
                  <div className="wishlist-card-title" onClick={() => navigate('/store/product/' + p.id)} title={p.name}>
                    {p.name}
                  </div>
                  <div className="wishlist-card-price">
                    <span className="final">{fmt(fp)}</span>
                    {p.discount > 0 && <span className="orig">{fmt(p.price)}</span>}
                  </div>
                  <div className="wishlist-card-actions">
                    <button
                      type="button"
                      className="btn-primary btn-add-cart"
                      onClick={() => moveToCart(p)}
                      disabled={p.stockQuantity <= 0}
                    >
                      {p.stockQuantity <= 0 ? 'Out of Stock' : 'Move to Cart'}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

function VendorLayout({ user, onLogout }) {
  const location = useLocation();
  const [vendorInfo, setVendorInfo] = useState(null);
  useEffect(() => { api('/vendors/me').then(d => setVendorInfo(d.vendor || d)).catch(() => {}); }, []);
  const nav = [
    { path: '/vendor', label: '📊 Dashboard' },
    { path: '/vendor/products', label: '📦 Products' },
    { path: '/vendor/orders', label: '🛍️ Orders' },
    { path: '/vendor/profile', label: '⚙️ Profile' },
  ];
  return (
    <div className="dashboard">
      <aside className="dash-sidebar">
        <div className="dash-sidebar-header">
          <Link to="/vendor" style={{ textDecoration: 'none' }}>
            <BrandLogo size={20} light={true} />
          </Link>
        </div>
        <div className="dash-welcome" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <p>Merchant Portal</p>
            <strong>{vendorInfo?.businessName || user?.name}</strong>
          </div>
          <NotificationBell />
        </div>
        <nav className="sidebar-nav">
          {nav.map(n => <Link key={n.path} to={n.path} className={'sidebar-nav-item' + (location.pathname === n.path ? ' active' : '')}>{n.label}</Link>)}
        </nav>
        <button className="btn-logout" style={{ margin: '16px' }} onClick={onLogout}>Logout</button>
      </aside>
      <main className="dash-main"><Outlet /></main>
    </div>
  );
}

function VendorDashboard({ addToast }) {
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const vInfo = await api('/vendors/me').catch(() => null);
        const v = vInfo?.vendor || vInfo;
        const vId = v?.id;
        const [pData, oData] = await Promise.all([
          api('/products' + (vId ? '?vendorId=' + vId : '')).catch(() => []),
          api('/orders').catch(() => [])
        ]);
        setProducts(Array.isArray(pData) ? pData : []);
        setOrders(Array.isArray(oData) ? oData : []);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) return <Loader />;
  const totalRevenue = orders.reduce((s, o) => s + Number(o.finalAmount || 0), 0);
  const pendingOrders = orders.filter(o => o.orderStatus === 'PLACED' || o.orderStatus === 'CONFIRMED').length;

  return (
    <div>
      <h1>Dashboard</h1>
      <div className="dash-cards">
        <div className="dash-card"><span className="stat-icon">📦</span><h3>Products</h3><p>{products.length}</p></div>
        <div className="dash-card"><span className="stat-icon">💳</span><h3>Total Orders</h3><p>{orders.length}</p></div>
        <div className="dash-card"><span className="stat-icon">💰</span><h3>Revenue</h3><p>{fmt(totalRevenue)}</p></div>
        <div className="dash-card"><span className="stat-icon">⏳</span><h3>Pending</h3><p>{pendingOrders}</p></div>
      </div>
      <h2 style={{ marginTop: 24 }}>Recent Orders</h2>
      {orders.length === 0 ? <EmptyState message="No orders yet" /> : (
        <table className="data-table">
          <thead><tr><th>Order ID</th><th>Customer</th><th>Amount</th><th>Status</th><th>Date</th></tr></thead>
          <tbody>{orders.slice(0, 10).map(o => (
            <tr key={o.id}><td>#{o.id}</td><td>{o.customerName}</td><td>{fmt(o.finalAmount)}</td><td><span className={statusClass(o.orderStatus)}>{o.orderStatus}</span></td><td>{o.createdAt ? new Date(o.createdAt).toLocaleDateString() : ''}</td></tr>
          ))}</tbody>
        </table>
      )}
    </div>
  );
}

function VendorProducts({ addToast, user }) {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modal, setModal] = useState(null);
  const [saving, setSaving] = useState(false);
  const [vendorInfo, setVendorInfo] = useState(null);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 15;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const vInfo = await api('/vendors/me').catch(() => null);
      const v = vInfo?.vendor || vInfo;
      setVendorInfo(v);
      const vId = v?.id || user?.vendorId || user?.id;

      const [pData, cData, bData] = await Promise.all([
        api('/products' + (vId ? '?vendorId=' + vId : '')).catch(() => []),
        api('/categories').catch(() => []),
        api('/brands').catch(() => []),
      ]);
      setProducts(Array.isArray(pData) ? pData : []);
      setCategories(Array.isArray(cData) ? cData : []);
      setBrands(Array.isArray(bData) ? bData : []);
    } finally { setLoading(false); }
  }, [user]);

  useEffect(() => { load(); }, [load]);

  const deleteProduct = async (id) => {
    if (!window.confirm('Delete this product?')) return;
    try { await api('/products/' + id, { method: 'DELETE' }); addToast('Deleted successfully', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };

  const saveProduct = async (form) => {
    setSaving(true);
    try {
      const vId = vendorInfo?.id || user?.vendorId || user?.id;
      if (modal?.editing) await api('/products/' + modal.editing.id, { method: 'PUT', body: form });
      else await api('/products', { method: 'POST', body: { ...form, vendorId: vId } });
      addToast('Product saved successfully', 'success');
      setModal(null);
      load();
    } catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };

  const filtered = products.filter(p => {
    const matchSearch = !search || p.name.toLowerCase().includes(search.toLowerCase()) || (p.sku && p.sku.toLowerCase().includes(search.toLowerCase()));
    const matchCat = !categoryFilter || String(p.categoryId) === String(categoryFilter);
    return matchSearch && matchCat;
  });

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE) || 1;
  const curPage = Math.min(page, totalPages);
  const paginated = filtered.slice((curPage - 1) * PAGE_SIZE, curPage * PAGE_SIZE);

  if (loading) return <Loader />;

  return (
    <div>
      <div className="section-header">
        <h1>My Products ({products.length})</h1>
        <button className="btn-add" onClick={() => setModal({ editing: null })}>+ Add Product</button>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <input
          type="text"
          placeholder="Filter by name or SKU..."
          value={search}
          onChange={e => { setSearch(e.target.value); setPage(1); }}
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #cbd5e1', flex: 1, minWidth: 200 }}
        />
        <select
          value={categoryFilter}
          onChange={e => { setCategoryFilter(e.target.value); setPage(1); }}
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #cbd5e1' }}
        >
          <option value="">All Categories</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </div>

      {filtered.length === 0 ? <EmptyState message="No products found" /> : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Thumbnail</th>
                <th>Name</th>
                <th>Category</th>
                <th>Brand</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {paginated.map(p => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>
                    <img src={p.image} alt="" style={{ width: 36, height: 36, objectFit: 'cover', borderRadius: 4 }} onError={e => e.target.style.display = 'none'} />
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{p.name}</div>
                    <small style={{ color: '#64748b' }}>{p.sku}</small>
                  </td>
                  <td>{p.categoryName}</td>
                  <td>{p.brandName}</td>
                  <td>{fmt(p.price)}</td>
                  <td>
                    <span style={{ color: p.stockQuantity < 10 ? '#ef4444' : '#16a34a', fontWeight: 600 }}>
                      {p.stockQuantity}
                    </span>
                  </td>
                  <td>
                    <button className="btn-primary" style={{ marginRight: 6 }} onClick={() => setModal({ editing: p })}>Edit</button>
                    <button className="btn-cancel" onClick={() => deleteProduct(p.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div className="table-pagination">
              <span>Showing {(curPage - 1) * PAGE_SIZE + 1} to {Math.min(curPage * PAGE_SIZE, filtered.length)} of {filtered.length} products</span>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="page-btn" disabled={curPage <= 1} onClick={() => setPage(curPage - 1)}>← Prev</button>
                <span style={{ padding: '6px 12px', fontWeight: 600 }}>Page {curPage} of {totalPages}</span>
                <button className="page-btn" disabled={curPage >= totalPages} onClick={() => setPage(curPage + 1)}>Next →</button>
              </div>
            </div>
          )}
        </>
      )}
      {modal && <ProductModal product={modal.editing} categories={categories} brands={brands} saving={saving} onSave={saveProduct} onClose={() => setModal(null)} />}
    </div>
  );
}

function ProductModal({ product, categories, brands, saving, onSave, onClose }) {
  const [form, setForm] = useState({ name: product?.name || '', description: product?.description || '', categoryId: product?.categoryId || '', brandId: product?.brandId || '', price: product?.price || '', discount: product?.discount || 0, stockQuantity: product?.stockQuantity || '', sku: product?.sku || '', image: product?.image || '' });
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const handleSubmit = (e) => { e.preventDefault(); onSave({ ...form, price: Number(form.price), discount: Number(form.discount), stockQuantity: Number(form.stockQuantity), categoryId: Number(form.categoryId), brandId: Number(form.brandId) }); };
  return (
    <Modal title={product ? 'Edit Product' : 'Add Product'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div className="form-group"><label>Name</label><input value={form.name} onChange={e => set('name', e.target.value)} required /></div>
        <div className="form-group"><label>Description</label><textarea value={form.description} onChange={e => set('description', e.target.value)} rows={3} /></div>
        <div className="form-row">
          <div className="form-group" style={{ flex: 1 }}><label>Category</label><select value={form.categoryId} onChange={e => set('categoryId', e.target.value)} required><option value="">Select</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
          <div className="form-group" style={{ flex: 1 }}><label>Brand</label><select value={form.brandId} onChange={e => set('brandId', e.target.value)} required><option value="">Select</option>{brands.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}</select></div>
        </div>
        <div className="form-row">
          <div className="form-group" style={{ flex: 1 }}><label>Price</label><input type="number" step="0.01" min="0" value={form.price} onChange={e => set('price', e.target.value)} required /></div>
          <div className="form-group" style={{ flex: 1 }}><label>Discount %</label><input type="number" step="0.01" min="0" value={form.discount} onChange={e => set('discount', e.target.value)} /></div>
        </div>
        <div className="form-row">
          <div className="form-group" style={{ flex: 1 }}><label>Stock</label><input type="number" min="0" value={form.stockQuantity} onChange={e => set('stockQuantity', e.target.value)} required /></div>
          <div className="form-group" style={{ flex: 1 }}><label>SKU</label><input value={form.sku} onChange={e => set('sku', e.target.value)} /></div>
        </div>
        <div className="form-group"><label>Image URL</label><input value={form.image} onChange={e => set('image', e.target.value)} placeholder="https://..." /></div>
        <div className="modal-actions"><button type="button" className="btn-cancel" onClick={onClose}>Cancel</button><button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button></div>
      </form>
    </Modal>
  );
}

function CategoryModal({ category, saving, onSave, onClose }) {
  const [name, setName] = useState(category?.name || '');
  const [description, setDescription] = useState(category?.description || '');
  const handleSubmit = (e) => {
    e.preventDefault();
    onSave({ name: name.trim(), description: description.trim() });
  };
  return (
    <Modal title={category?.id ? 'Edit Category' : 'Add Category'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div className="form-group"><label>Category Name</label><input value={name} onChange={e => setName(e.target.value)} required /></div>
        <div className="form-group"><label>Description</label><textarea value={description} onChange={e => setDescription(e.target.value)} rows={3} /></div>
        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
        </div>
      </form>
    </Modal>
  );
}

function BrandModal({ brand, saving, onSave, onClose }) {
  const [name, setName] = useState(brand?.name || '');
  const [description, setDescription] = useState(brand?.description || '');
  const handleSubmit = (e) => {
    e.preventDefault();
    onSave({ name: name.trim(), description: description.trim() });
  };
  return (
    <Modal title={brand?.id ? 'Edit Brand' : 'Add Brand'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <div className="form-group"><label>Brand Name</label><input value={name} onChange={e => setName(e.target.value)} required /></div>
        <div className="form-group"><label>Description</label><textarea value={description} onChange={e => setDescription(e.target.value)} rows={3} /></div>
        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
        </div>
      </form>
    </Modal>
  );
}

function OrderDetailsModal({ order, onClose, onStatusUpdate }) {
  if (!order) return null;
  const items = order.items || [];
  const addr = order.shippingAddress || {};
  return (
    <Modal title={`Order Details #${order.id}`} onClose={onClose}>
      <div className="order-details-modal">
        <div className="order-details-summary">
          <div>
            <strong>Status: </strong>
            <span className={statusClass(order.orderStatus)}>{order.orderStatus}</span>
          </div>
          <div>
            <strong>Date: </strong>
            {order.createdAt ? new Date(order.createdAt).toLocaleString() : '-'}
          </div>
          <div>
            <strong>Payment: </strong>
            <span className={statusClass(order.paymentStatus)}>{order.paymentStatus || 'COMPLETED'}</span>
          </div>
        </div>

        <div className="order-details-customer">
          <h4>Customer & Delivery Info</h4>
          <p><strong>Name:</strong> {order.customerName || '-'}</p>
          {order.customerEmail && <p><strong>Email:</strong> {order.customerEmail}</p>}
          {order.customerPhone && <p><strong>Phone:</strong> {order.customerPhone}</p>}
          {order.shippingAddressString ? (
            <p><strong>Address:</strong> {order.shippingAddressString}</p>
          ) : addr.addressLine ? (
            <p><strong>Address:</strong> {addr.addressLine}, {addr.city}, {addr.state} - {addr.pincode}</p>
          ) : null}
        </div>

        <div className="order-details-items">
          <h4>Ordered Items ({items.length})</h4>
          <table className="data-table" style={{ marginTop: 8 }}>
            <thead>
              <tr>
                <th>Item</th>
                <th>Price</th>
                <th>Qty</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              {items.map((it, idx) => (
                <tr key={idx}>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      {it.productImage && <img src={it.productImage} alt="" style={{ width: 32, height: 32, objectFit: 'cover', borderRadius: 4 }} onError={e => e.target.style.display = 'none'} />}
                      <span>{it.productName || `Product #${it.productId}`}</span>
                    </div>
                  </td>
                  <td>{fmt(it.unitPrice || it.price)}</td>
                  <td>{it.quantity}</td>
                  <td>{fmt(it.totalPrice || it.subtotal || ((it.unitPrice || it.price) * it.quantity))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="order-details-totals" style={{ marginTop: 16, padding: '12px', background: '#f8fafc', borderRadius: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
            <span>Subtotal:</span>
            <span>{fmt(order.totalAmount)}</span>
          </div>
          {Number(order.discountAmount) > 0 && (
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, color: '#16a34a' }}>
              <span>Discount ({order.couponCode || 'Promo'}):</span>
              <span>-{fmt(order.discountAmount)}</span>
            </div>
          )}
          {Number(order.shippingFee) > 0 && (
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span>Shipping:</span>
              <span>{fmt(order.shippingFee)}</span>
            </div>
          )}
          <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 800, fontSize: 16, borderTop: '1px solid #e2e8f0', paddingTop: 8, marginTop: 8 }}>
            <span>Final Amount:</span>
            <span>{fmt(order.finalAmount)}</span>
          </div>
        </div>

        {onStatusUpdate && (
          <div style={{ marginTop: 20, display: 'flex', alignItems: 'center', gap: 12 }}>
            <label style={{ fontWeight: 600 }}>Update Status:</label>
            <select
              value={order.orderStatus}
              onChange={e => onStatusUpdate(order.id, e.target.value)}
              style={{ padding: '6px 12px', borderRadius: 6, border: '1px solid #cbd5e1' }}
            >
              {['PLACED','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED'].map(s => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
        )}
      </div>
    </Modal>
  );
}

function VendorOrders({ addToast }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedOrder, setSelectedOrder] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/orders'); setOrders(Array.isArray(d) ? d : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);

  useEffect(() => { load(); }, [load]);

  const updateStatus = async (orderId, status) => {
    try {
      await api('/orders/' + orderId + '/status', { method: 'PUT', body: { status } });
      addToast('Status updated to ' + status, 'success');
      load();
      if (selectedOrder && selectedOrder.id === orderId) {
        setSelectedOrder(prev => ({ ...prev, orderStatus: status }));
      }
    } catch (err) { addToast(err.message, 'error'); }
  };

  if (loading) return <Loader />;

  return (
    <div>
      <div className="section-header">
        <h1>Orders ({orders.length})</h1>
      </div>
      {orders.length === 0 ? <EmptyState message="No orders received yet" /> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Order</th>
              <th>Date</th>
              <th>Customer</th>
              <th>Amount</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.map(o => (
              <tr key={o.id}>
                <td><strong>#{o.id}</strong></td>
                <td>{o.createdAt ? new Date(o.createdAt).toLocaleDateString() : '-'}</td>
                <td>{o.customerName}</td>
                <td><strong>{fmt(o.finalAmount)}</strong></td>
                <td><span className={statusClass(o.orderStatus)}>{o.orderStatus}</span></td>
                <td>
                  <button className="btn-secondary" style={{ marginRight: 8, padding: '4px 10px', fontSize: '0.85rem' }} onClick={() => setSelectedOrder(o)}>
                    View Details
                  </button>
                  <select
                    value={o.orderStatus}
                    onChange={e => updateStatus(o.id, e.target.value)}
                    style={{ padding: '4px 8px', borderRadius: 6, border: '1px solid #cbd5e1' }}
                  >
                    {['PLACED','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED'].map(s => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {selectedOrder && (
        <OrderDetailsModal
          order={selectedOrder}
          onClose={() => setSelectedOrder(null)}
          onStatusUpdate={updateStatus}
        />
      )}
    </div>
  );
}

function VendorProfile({ addToast }) {
  const [form, setForm] = useState({ businessName: '', description: '', city: '', state: '', pincode: '', address: '' });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  useEffect(() => {
    api('/vendors/me').then(d => { const v = d.vendor || d; setForm({ businessName: v.businessName || '', description: v.description || '', city: v.city || '', state: v.state || '', pincode: v.pincode || '', address: v.address || '' }); }).catch(() => {}).finally(() => setLoading(false));
  }, []);
  const save = async (e) => {
    e.preventDefault(); setSaving(true);
    try { await api('/vendors/me', { method: 'PUT', body: form }); addToast('Profile updated', 'success'); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };
  if (loading) return <Loader />;
  return (
    <div className="profile-section">
      <h1>Vendor Profile</h1>
      <div className="profile-card">
        <form onSubmit={save}>
          <div className="form-group"><label>Business Name</label><input value={form.businessName} onChange={e => setForm({ ...form, businessName: e.target.value })} required /></div>
          <div className="form-group"><label>Description</label><textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} rows={3} /></div>
          <div className="form-group"><label>Address</label><input value={form.address} onChange={e => setForm({ ...form, address: e.target.value })} /></div>
          <div className="form-row">
            <div className="form-group" style={{ flex: 1 }}><label>City</label><input value={form.city} onChange={e => setForm({ ...form, city: e.target.value })} /></div>
            <div className="form-group" style={{ flex: 1 }}><label>State</label><input value={form.state} onChange={e => setForm({ ...form, state: e.target.value })} /></div>
          </div>
          <div className="form-group"><label>Pincode</label><input value={form.pincode} onChange={e => setForm({ ...form, pincode: e.target.value })} /></div>
          <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save Profile'}</button>
        </form>
      </div>
    </div>
  );
}

function AdminLayout({ user, onLogout }) {
  const location = useLocation();
  const nav = [
    { path: '/admin', label: '📊 Dashboard' },
    { path: '/admin/customers', label: '👥 Customers' },
    { path: '/admin/vendors', label: '🏪 Vendors' },
    { path: '/admin/products', label: '📦 Products' },
    { path: '/admin/categories', label: '🏷️ Categories' },
    { path: '/admin/brands', label: '✨ Brands' },
    { path: '/admin/orders', label: '🛍️ Orders' },
  ];
  return (
    <div className="dashboard">
      <aside className="dash-sidebar">
        <div className="dash-sidebar-header">
          <Link to="/admin" style={{ textDecoration: 'none' }}>
            <BrandLogo size={20} light={true} />
          </Link>
        </div>
        <div className="dash-welcome" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <p>Administrator</p>
            <strong>{user?.name}</strong>
          </div>
          <NotificationBell />
        </div>
        <nav className="sidebar-nav">
          {nav.map(n => <Link key={n.path} to={n.path} className={'sidebar-nav-item' + (location.pathname === n.path ? ' active' : '')}>{n.label}</Link>)}
        </nav>
        <button className="btn-logout" style={{ margin: '16px' }} onClick={onLogout}>Logout</button>
      </aside>
      <main className="dash-main"><Outlet /></main>
    </div>
  );
}

function AdminDashboard({ addToast }) {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  useEffect(() => { api('/admin/stats').then(d => setStats(d)).catch(err => addToast(err.message, 'error')).finally(() => setLoading(false)); }, [addToast]);
  if (loading) return <Loader />;
  if (!stats) return <EmptyState message="Could not load stats" />;
  return (
    <div>
      <h1>Admin Dashboard</h1>
      <div className="dash-cards">
        <div className="dash-card"><span className="stat-icon">👤</span><h3>Customers</h3><p>{stats.totalCustomers || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">🏬</span><h3>Vendors</h3><p>{stats.approvedVendors || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">⏳</span><h3>Pending Vendors</h3><p>{stats.pendingVendors || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">📦</span><h3>Products</h3><p>{stats.totalProducts || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">💳</span><h3>Orders</h3><p>{stats.totalOrders || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">💰</span><h3>Revenue</h3><p>{fmt(stats.totalRevenue || 0)}</p></div>
        <div className="dash-card"><span className="stat-icon">✅</span><h3>Delivered</h3><p>{stats.deliveredOrders || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">❌</span><h3>Cancelled</h3><p>{stats.cancelledOrders || 0}</p></div>
      </div>
    </div>
  );
}

function AdminCustomers({ addToast }) {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/admin/users'); setUsers(Array.isArray(d) ? d.filter(u => u.role === 'CUSTOMER') : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);
  useEffect(() => { load(); }, [load]);
  const toggleStatus = async (id, s) => {
    try { await api('/admin/users/' + id + '/status', { method: 'PUT', body: { status: s === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' } }); addToast('Updated', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  const deleteUser = async (id) => {
    if (!window.confirm('Delete this user?')) return;
    try { await api('/admin/users/' + id, { method: 'DELETE' }); addToast('Deleted', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  if (loading) return <Loader />;
  return (
    <div>
      <div className="section-header"><h1>Customers</h1></div>
      {users.length === 0 ? <EmptyState message="No customers" /> : (
        <table className="data-table">
          <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Phone</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>{users.map(u => (
            <tr key={u.id}><td>{u.id}</td><td>{u.name}</td><td>{u.email}</td><td>{u.phone || '-'}</td>
              <td><span className={(u.status || 'ACTIVE') === 'ACTIVE' ? 'status-badge status-delivered' : 'status-badge status-cancelled'}>{u.status || 'ACTIVE'}</span></td>
              <td><button className="btn-primary" style={{ marginRight: 4 }} onClick={() => toggleStatus(u.id, u.status || 'ACTIVE')}>{(u.status || 'ACTIVE') === 'ACTIVE' ? 'Disable' : 'Enable'}</button><button className="btn-cancel" onClick={() => deleteUser(u.id)}>Delete</button></td>
            </tr>
          ))}</tbody>
        </table>
      )}
    </div>
  );
}

function AdminVendors({ addToast }) {
  const [vendors, setVendors] = useState([]);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/vendors'); setVendors(Array.isArray(d) ? d : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);
  useEffect(() => { load(); }, [load]);
  const updateVendorStatus = async (id, status) => {
    try { await api('/vendors/' + id + '/status', { method: 'PUT', body: { status } }); addToast('Vendor ' + status.toLowerCase(), 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  if (loading) return <Loader />;
  return (
    <div>
      <div className="section-header"><h1>Vendors</h1></div>
      {vendors.length === 0 ? <EmptyState message="No vendors" /> : (
        <table className="data-table">
          <thead><tr><th>ID</th><th>Business</th><th>Owner</th><th>Email</th><th>City</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>{vendors.map(v => (
            <tr key={v.id}><td>{v.id}</td><td>{v.businessName}</td><td>{v.ownerName}</td><td>{v.ownerEmail}</td><td>{v.city}</td>
              <td><span className={v.approvalStatus === 'APPROVED' ? 'status-badge status-delivered' : v.approvalStatus === 'REJECTED' ? 'status-badge status-cancelled' : 'status-badge status-placed'}>{v.approvalStatus || 'PENDING'}</span></td>
              <td>{v.approvalStatus !== 'APPROVED' && <button className="btn-primary" style={{ marginRight: 4 }} onClick={() => updateVendorStatus(v.id, 'APPROVED')}>Approve</button>}{v.approvalStatus !== 'REJECTED' && <button className="btn-cancel" onClick={() => updateVendorStatus(v.id, 'REJECTED')}>Reject</button>}</td>
            </tr>
          ))}</tbody>
        </table>
      )}
    </div>
  );
}

function AdminProducts({ addToast }) {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 20;
  const [modal, setModal] = useState(null);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [prods, cats, brds] = await Promise.all([
        api('/products').catch(() => []),
        api('/categories').catch(() => []),
        api('/brands').catch(() => [])
      ]);
      setProducts(Array.isArray(prods) ? prods : []);
      setCategories(Array.isArray(cats) ? cats : []);
      setBrands(Array.isArray(brds) ? brds : []);
    }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);

  useEffect(() => { load(); }, [load]);

  const deleteProduct = async (id) => {
    if (!window.confirm('Delete this product from catalog?')) return;
    try { await api('/products/' + id, { method: 'DELETE' }); addToast('Product deleted', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };

  const saveProduct = async (form) => {
    setSaving(true);
    try {
      if (modal?.editing) {
        await api('/products/' + modal.editing.id, { method: 'PUT', body: form });
        addToast('Product updated successfully', 'success');
      }
      setModal(null);
      load();
    } catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };

  const filtered = products.filter(p => {
    const matchSearch = !search || p.name.toLowerCase().includes(search.toLowerCase()) || (p.sku && p.sku.toLowerCase().includes(search.toLowerCase()));
    const matchCat = !categoryFilter || String(p.categoryId) === String(categoryFilter);
    return matchSearch && matchCat;
  });

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE) || 1;
  const curPage = Math.min(page, totalPages);
  const paginated = filtered.slice((curPage - 1) * PAGE_SIZE, curPage * PAGE_SIZE);

  if (loading) return <Loader />;

  return (
    <div>
      <div className="section-header">
        <h1>Catalog Management ({products.length} Products)</h1>
      </div>

      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
        <input
          type="text"
          placeholder="Search by name or SKU..."
          value={search}
          onChange={e => { setSearch(e.target.value); setPage(1); }}
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #cbd5e1', flex: 1, minWidth: 200 }}
        />
        <select
          value={categoryFilter}
          onChange={e => { setCategoryFilter(e.target.value); setPage(1); }}
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #cbd5e1' }}
        >
          <option value="">All Categories</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </div>

      {filtered.length === 0 ? <EmptyState message="No products match your filter" /> : (
        <>
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Thumbnail</th>
                <th>Name</th>
                <th>Vendor</th>
                <th>Category</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {paginated.map(p => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>
                    <img src={p.image} alt="" style={{ width: 36, height: 36, objectFit: 'cover', borderRadius: 4 }} onError={e => e.target.style.display = 'none'} />
                  </td>
                  <td>
                    <div style={{ fontWeight: 600 }}>{p.name}</div>
                    <small style={{ color: '#64748b' }}>{p.sku}</small>
                  </td>
                  <td>{p.vendorName || '-'}</td>
                  <td>{p.categoryName || '-'}</td>
                  <td>{fmt(p.price)}</td>
                  <td>
                    <span style={{ color: p.stockQuantity < 10 ? '#ef4444' : '#16a34a', fontWeight: 600 }}>
                      {p.stockQuantity}
                    </span>
                  </td>
                  <td>
                    <button className="btn-primary" style={{ marginRight: 6 }} onClick={() => setModal({ editing: p })}>Edit</button>
                    <button className="btn-cancel" onClick={() => deleteProduct(p.id)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div className="table-pagination">
              <span>Showing {(curPage - 1) * PAGE_SIZE + 1} to {Math.min(curPage * PAGE_SIZE, filtered.length)} of {filtered.length} products</span>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="page-btn" disabled={curPage <= 1} onClick={() => setPage(curPage - 1)}>← Prev</button>
                <span style={{ padding: '6px 12px', fontWeight: 600 }}>Page {curPage} of {totalPages}</span>
                <button className="page-btn" disabled={curPage >= totalPages} onClick={() => setPage(curPage + 1)}>Next →</button>
              </div>
            </div>
          )}
        </>
      )}

      {modal && (
        <ProductModal
          product={modal.editing}
          categories={categories}
          brands={brands}
          saving={saving}
          onSave={saveProduct}
          onClose={() => setModal(null)}
        />
      )}
    </div>
  );
}

function AdminCategories({ addToast }) {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [desc, setDesc] = useState('');
  const [saving, setSaving] = useState(false);
  const [editModal, setEditModal] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/categories'); setCategories(Array.isArray(d) ? d : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);
  useEffect(() => { load(); }, [load]);

  const addCategory = async (e) => {
    e.preventDefault(); if (!name.trim()) { addToast('Name required', 'error'); return; }
    setSaving(true);
    try { await api('/categories', { method: 'POST', body: { name: name.trim(), description: desc.trim() } }); addToast('Category added', 'success'); setName(''); setDesc(''); setShowForm(false); load(); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };

  const updateCategory = async (form) => {
    setSaving(true);
    try {
      await api('/categories/' + editModal.id, { method: 'PUT', body: form });
      addToast('Category updated', 'success');
      setEditModal(null);
      load();
    } catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };

  const deleteCategory = async (id) => {
    if (!window.confirm('Delete category?')) return;
    try { await api('/categories/' + id, { method: 'DELETE' }); addToast('Deleted', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };

  if (loading) return <Loader />;
  return (
    <div>
      <div className="section-header"><h1>Categories</h1><button className="btn-add" onClick={() => setShowForm(!showForm)}>{showForm ? 'Cancel' : '+ Add'}</button></div>
      {showForm && (
        <form className="profile-card" onSubmit={addCategory} style={{ marginBottom: 16, maxWidth: 500 }}>
          <div className="form-group"><label>Name</label><input value={name} onChange={e => setName(e.target.value)} required /></div>
          <div className="form-group"><label>Description</label><input value={desc} onChange={e => setDesc(e.target.value)} /></div>
          <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Add Category'}</button>
        </form>
      )}
      {categories.length === 0 ? <EmptyState message="No categories" /> : (
        <table className="data-table">
          <thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Actions</th></tr></thead>
          <tbody>{categories.map(c => (
            <tr key={c.id}>
              <td>{c.id}</td>
              <td><strong>{c.name}</strong></td>
              <td>{c.description || '-'}</td>
              <td>
                <button className="btn-primary" style={{ marginRight: 6 }} onClick={() => setEditModal(c)}>Edit</button>
                <button className="btn-cancel" onClick={() => deleteCategory(c.id)}>Delete</button>
              </td>
            </tr>
          ))}</tbody>
        </table>
      )}
      {editModal && <CategoryModal category={editModal} saving={saving} onSave={updateCategory} onClose={() => setEditModal(null)} />}
    </div>
  );
}

function AdminBrands({ addToast }) {
  const [brands, setBrands] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [desc, setDesc] = useState('');
  const [saving, setSaving] = useState(false);
  const [editModal, setEditModal] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/brands'); setBrands(Array.isArray(d) ? d : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);
  useEffect(() => { load(); }, [load]);

  const addBrand = async (e) => {
    e.preventDefault(); if (!name.trim()) { addToast('Name required', 'error'); return; }
    setSaving(true);
    try { await api('/brands', { method: 'POST', body: { name: name.trim(), description: desc.trim() } }); addToast('Brand added', 'success'); setName(''); setDesc(''); setShowForm(false); load(); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };

  const updateBrand = async (form) => {
    setSaving(true);
    try {
      await api('/brands/' + editModal.id, { method: 'PUT', body: form });
      addToast('Brand updated', 'success');
      setEditModal(null);
      load();
    } catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };

  const deleteBrand = async (id) => {
    if (!window.confirm('Delete brand?')) return;
    try { await api('/brands/' + id, { method: 'DELETE' }); addToast('Deleted', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };

  if (loading) return <Loader />;
  return (
    <div>
      <div className="section-header"><h1>Brands</h1><button className="btn-add" onClick={() => setShowForm(!showForm)}>{showForm ? 'Cancel' : '+ Add'}</button></div>
      {showForm && (
        <form className="profile-card" onSubmit={addBrand} style={{ marginBottom: 16, maxWidth: 500 }}>
          <div className="form-group"><label>Name</label><input value={name} onChange={e => setName(e.target.value)} required /></div>
          <div className="form-group"><label>Description</label><input value={desc} onChange={e => setDesc(e.target.value)} /></div>
          <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Add Brand'}</button>
        </form>
      )}
      {brands.length === 0 ? <EmptyState message="No brands" /> : (
        <table className="data-table">
          <thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Actions</th></tr></thead>
          <tbody>{brands.map(b => (
            <tr key={b.id}>
              <td>{b.id}</td>
              <td><strong>{b.name}</strong></td>
              <td>{b.description || '-'}</td>
              <td>
                <button className="btn-primary" style={{ marginRight: 6 }} onClick={() => setEditModal(b)}>Edit</button>
                <button className="btn-cancel" onClick={() => deleteBrand(b.id)}>Delete</button>
              </td>
            </tr>
          ))}</tbody>
        </table>
      )}
      {editModal && <BrandModal brand={editModal} saving={saving} onSave={updateBrand} onClose={() => setEditModal(null)} />}
    </div>
  );
}

function AdminOrders({ addToast }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedOrder, setSelectedOrder] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/orders'); setOrders(Array.isArray(d) ? d : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);

  useEffect(() => { load(); }, [load]);

  const updateStatus = async (orderId, status) => {
    try {
      await api('/orders/' + orderId + '/status', { method: 'PUT', body: { status } });
      addToast('Order status updated to ' + status, 'success');
      load();
      if (selectedOrder && selectedOrder.id === orderId) {
        setSelectedOrder(prev => ({ ...prev, orderStatus: status }));
      }
    } catch (err) { addToast(err.message, 'error'); }
  };

  if (loading) return <Loader />;

  return (
    <div>
      <div className="section-header"><h1>Orders Management ({orders.length})</h1></div>
      {orders.length === 0 ? <EmptyState message="No orders in marketplace" /> : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Order</th>
              <th>Date</th>
              <th>Customer</th>
              <th>Amount</th>
              <th>Payment</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.map(o => (
              <tr key={o.id}>
                <td><strong>#{o.id}</strong></td>
                <td>{o.createdAt ? new Date(o.createdAt).toLocaleDateString() : '-'}</td>
                <td>{o.customerName}</td>
                <td><strong>{fmt(o.finalAmount)}</strong></td>
                <td><span className={statusClass(o.paymentStatus)}>{o.paymentStatus || 'COMPLETED'}</span></td>
                <td><span className={statusClass(o.orderStatus)}>{o.orderStatus}</span></td>
                <td>
                  <button className="btn-secondary" style={{ marginRight: 8, padding: '4px 10px', fontSize: '0.85rem' }} onClick={() => setSelectedOrder(o)}>
                    View Details
                  </button>
                  <select
                    value={o.orderStatus}
                    onChange={e => updateStatus(o.id, e.target.value)}
                    style={{ padding: '4px 8px', borderRadius: 6, border: '1px solid #cbd5e1' }}
                  >
                    {['PLACED','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED'].map(s => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {selectedOrder && (
        <OrderDetailsModal
          order={selectedOrder}
          onClose={() => setSelectedOrder(null)}
          onStatusUpdate={updateStatus}
        />
      )}
    </div>
  );
}

function App() {
  const [user, setUser] = useState(getStoredUser);
  const [toasts, setToasts] = useState([]);
  const addToast = useCallback((message, type = 'success') => {
    const id = Date.now() + Math.random();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4000);
  }, []);
  const removeToast = useCallback((id) => setToasts(prev => prev.filter(t => t.id !== id)), []);
  const handleLogout = useCallback(async () => {
    try { await api('/auth/logout', { method: 'POST' }); } catch { }
    clearAuth(); setUser(null);
  }, []);
  const handleAuth = useCallback((token, loggedUser) => { setAuth(token, loggedUser); setUser(loggedUser); }, []);
  const home = user ? (user.role === 'ADMIN' ? '/admin' : user.role === 'VENDOR' ? '/vendor' : '/store') : '/login';

  return (
    <BrowserRouter>
      <Toast toasts={toasts} onRemove={removeToast} />
      <Routes>
        <Route path="/login"    element={user ? <Navigate to={home} replace /> : <LoginPage    addToast={addToast} onAuth={handleAuth} />} />
        <Route path="/register" element={user ? <Navigate to={home} replace /> : <RegisterPage addToast={addToast} onAuth={handleAuth} />} />

        <Route element={<RequireRole user={user} role="CUSTOMER"><CustomerLayout user={user} onLogout={handleLogout} /></RequireRole>}>
          <Route path="/store"             element={<StorePage          addToast={addToast} />} />
          <Route path="/store/product/:id" element={<ProductDetailsPage addToast={addToast} />} />
          <Route path="/products/:id"       element={<ProductDetailsPage addToast={addToast} />} />
          <Route path="/cart"              element={<CartPage           addToast={addToast} />} />
          <Route path="/checkout"          element={<CheckoutPage       addToast={addToast} />} />
          <Route path="/orders"            element={<OrdersPage         addToast={addToast} />} />
          <Route path="/wishlist"          element={<WishlistPage       addToast={addToast} />} />
        </Route>

        <Route element={<RequireRole user={user} role="VENDOR"><VendorLayout user={user} onLogout={handleLogout} /></RequireRole>}>
          <Route path="/vendor"          element={<VendorDashboard addToast={addToast} />} />
          <Route path="/vendor/products" element={<VendorProducts  addToast={addToast} user={user} />} />
          <Route path="/vendor/orders"   element={<VendorOrders    addToast={addToast} />} />
          <Route path="/vendor/profile"  element={<VendorProfile   addToast={addToast} />} />
        </Route>

        <Route element={<RequireRole user={user} role="ADMIN"><AdminLayout user={user} onLogout={handleLogout} /></RequireRole>}>
          <Route path="/admin"            element={<AdminDashboard  addToast={addToast} />} />
          <Route path="/admin/customers"  element={<AdminCustomers  addToast={addToast} />} />
          <Route path="/admin/vendors"    element={<AdminVendors    addToast={addToast} />} />
          <Route path="/admin/products"   element={<AdminProducts   addToast={addToast} />} />
          <Route path="/admin/categories" element={<AdminCategories addToast={addToast} />} />
          <Route path="/admin/brands"     element={<AdminBrands     addToast={addToast} />} />
          <Route path="/admin/orders"     element={<AdminOrders     addToast={addToast} />} />
        </Route>

        <Route path="*" element={<Navigate to={home} replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;

