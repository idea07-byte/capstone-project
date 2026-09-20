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

const API_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');

function resolveMediaUrl(url) {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url;
  if (url.startsWith('/product-images/')) {
    return API_BASE ? `${API_BASE}${url}` : url;
  }
  return url;
}

async function api(path, opts = {}) {
  const token = getToken();
  const headers = {};
  if (token) headers['Authorization'] = 'Bearer ' + token;
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
  const url = API_BASE ? `${API_BASE}/api${path}` : `/api${path}`;
  const res = await fetch(url, { method: opts.method || 'GET', headers, body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || 'Request failed (' + res.status + ')');
  return data;
}

function fmt(amount) {
  const n = Number(amount || 0);
  return '$' + n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

const MOCK_FALLBACK_PRODUCTS = [
  {
    id: 'puma',
    name: "Puma Xetic Sculpt",
    categoryName: "Shoes",
    categoryId: 6,
    brandName: "PUMA",
    price: 149.99,
    discount: 50,
    stockQuantity: 12,
    averageRating: 4.9,
    reviewCount: 32000,
    image: "/puma_xetic_sculpt.png",
    description: "The Puma Xetic Sculpt blends revolutionary 3D mechanical cushioning with premium athletic street styling. Engineered with sculpted honeycomb lattices and breathable knit mesh upper for supreme comfort.",
    vendorName: "PUMA Official Flagship"
  },
  {
    id: 2,
    name: "Regular Fit Cotton Crew Tee",
    categoryName: "Clothing",
    categoryId: 2,
    brandName: "H&M",
    price: 34.99,
    discount: 12,
    stockQuantity: 12,
    averageRating: 4.6,
    reviewCount: 124,
    image: "/product-images/clothing/product_0150.jpg",
    description: "Premium breathable organic cotton tee tailored in a relaxed, modern silhouette for all-day comfort.",
    vendorName: "H&M Global"
  },
  {
    id: 3,
    name: "Asics Gel-Nimbus 26",
    categoryName: "Shoes",
    categoryId: 1,
    brandName: "Asics",
    price: 159.99,
    discount: 15,
    stockQuantity: 9,
    averageRating: 4.8,
    reviewCount: 88,
    image: "/puma_xetic_sculpt.png",
    description: "Experience plush cloud-like landings with PureGEL technology and lightweight FF BLAST PLUS ECO cushioning.",
    vendorName: "Asics Running"
  },
  {
    id: 4,
    name: "Adidas Originals Trefoil Hoodie",
    categoryName: "Clothing",
    categoryId: 2,
    brandName: "Adidas",
    price: 79.99,
    discount: 20,
    stockQuantity: 18,
    averageRating: 4.7,
    reviewCount: 215,
    image: "/product-images/clothing/product_0155.jpg",
    description: "Iconic athletic pullover fleece hoodie featuring the classic trefoil graphic and ultra-soft brushed interior.",
    vendorName: "Adidas Originals"
  },
  {
    id: 5,
    name: "Minimalist Nordic Desk Lamp",
    categoryName: "Lamp",
    categoryId: 3,
    brandName: "Nordic Deco",
    price: 49.99,
    discount: 10,
    stockQuantity: 24,
    averageRating: 4.5,
    reviewCount: 42,
    image: "/product-images/home-kitchen/product_0201.jpg",
    description: "Warm champagne brushed brass desk lamp with touch dimming, 3000K warm LED illumination, and architectural silhouette.",
    vendorName: "Studio Light"
  },
  {
    id: 6,
    name: "All-Day Urban Leather Backpack",
    categoryName: "Bag",
    categoryId: 4,
    brandName: "Samsonite",
    price: 129.00,
    discount: 18,
    stockQuantity: 7,
    averageRating: 4.9,
    reviewCount: 96,
    image: "/product-images/bags/product_0101.jpg",
    description: "Water-resistant commuter backpack with padded 16-inch laptop chamber, quick-access magnetic flap, and ergonomic strap system.",
    vendorName: "Samsonite Store"
  }
];

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

function getCategoryFallbackImage(categoryName = '', productName = '') {
  const c = (categoryName || '').toLowerCase();
  const n = (productName || '').toLowerCase();
  if (c.includes('shoe') || n.includes('sneaker') || n.includes('shoe') || n.includes('boot') || n.includes('puma') || n.includes('nike')) {
    return 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('cloth') || c.includes('apparel') || c.includes('fashion') || n.includes('shirt') || n.includes('jacket') || n.includes('dress') || n.includes('hoodie')) {
    return 'https://images.unsplash.com/photo-1523381210434-271e8be1f52b?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('watch') || n.includes('watch') || n.includes('timepiece')) {
    return 'https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('bag') || n.includes('bag') || n.includes('backpack') || n.includes('tote')) {
    return 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('headphone') || c.includes('audio') || n.includes('headphone') || n.includes('earbuds') || n.includes('airpods')) {
    return 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('laptop') || c.includes('computer') || n.includes('laptop') || n.includes('macbook')) {
    return 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('mobile') || c.includes('phone') || n.includes('iphone') || n.includes('smartphone')) {
    return 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('beauty') || c.includes('perfume') || c.includes('cosmetic')) {
    return 'https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('appliance') || c.includes('kitchen') || c.includes('home')) {
    return 'https://images.unsplash.com/photo-1588854337236-6889d631faa8?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('sport') || c.includes('fitness')) {
    return 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=900&auto=format&fit=crop&q=85';
  }
  if (c.includes('toy') || c.includes('game')) {
    return 'https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?w=900&auto=format&fit=crop&q=85';
  }
  return 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=900&auto=format&fit=crop&q=85';
}

function ProductImage({ src, alt, category, style, className }) {
  const [imgSrc, setImgSrc] = useState(() => resolveMediaUrl(src) || getCategoryFallbackImage(category, alt));
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    const resolved = resolveMediaUrl(src);
    setImgSrc(resolved || getCategoryFallbackImage(category, alt));
    setHasError(false);
  }, [src, category, alt]);

  const handleError = () => {
    if (!hasError) {
      setHasError(true);
      setImgSrc(getCategoryFallbackImage(category, alt));
    }
  };

  return (
    <img
      src={imgSrc}
      alt={alt || 'Product Image'}
      style={style}
      className={className}
      onError={handleError}
      loading="lazy"
      decoding="async"
    />
  );
}

const GUEST_CART_KEY = 'buyit_guest_cart';

function getGuestCart() {
  try {
    const raw = localStorage.getItem(GUEST_CART_KEY);
    if (!raw) return { items: [], total: 0, count: 0 };
    const parsed = JSON.parse(raw);
    const items = Array.isArray(parsed.items) ? parsed.items : [];
    const total = items.reduce((acc, i) => acc + ((Number(i.price) || 0) * (Number(i.quantity) || 1)), 0);
    const count = items.reduce((acc, i) => acc + (Number(i.quantity) || 1), 0);
    return { items, total, count };
  } catch {
    return { items: [], total: 0, count: 0 };
  }
}

function saveGuestCart(cart) {
  try {
    const items = Array.isArray(cart.items) ? cart.items : [];
    const total = items.reduce((acc, i) => acc + ((Number(i.price) || 0) * (Number(i.quantity) || 1)), 0);
    const count = items.reduce((acc, i) => acc + (Number(i.quantity) || 1), 0);
    const normalized = { items, total, count };
    localStorage.setItem(GUEST_CART_KEY, JSON.stringify(normalized));
    return normalized;
  } catch {
    return { items: [], total: 0, count: 0 };
  }
}

function clearGuestCart() {
  try { localStorage.removeItem(GUEST_CART_KEY); } catch {}
}

async function getCartData() {
  const token = getToken();
  if (token) {
    try {
      const data = await api('/cart');
      if (data && Array.isArray(data.items)) return data;
    } catch (err) {
      if (err.message && err.message.toLowerCase().includes('unauthorized')) {
        clearAuth();
      }
    }
  }
  return getGuestCart();
}

async function addToCartItem(product, quantity = 1) {
  const token = getToken();
  const prodId = Number(product.id || product.productId || 1);
  const qty = Number(quantity) || 1;
  const price = Number(product.finalPrice || product.price || 0);

  if (token) {
    try {
      await api('/cart', { method: 'POST', body: { productId: prodId, quantity: qty } });
      return;
    } catch (err) {
      if (!err.message || !err.message.toLowerCase().includes('unauthorized')) {
        throw err;
      }
      clearAuth();
    }
  }

  // Fallback to guest cart
  const cart = getGuestCart();
  const existingIdx = cart.items.findIndex(i => Number(i.productId) === prodId);
  if (existingIdx >= 0) {
    cart.items[existingIdx].quantity += qty;
    cart.items[existingIdx].subtotal = cart.items[existingIdx].quantity * cart.items[existingIdx].price;
  } else {
    cart.items.push({
      id: Date.now() + Math.floor(Math.random() * 1000),
      productId: prodId,
      productName: product.name || product.productName || 'Product #' + prodId,
      productImage: product.image || product.productImage || '',
      quantity: qty,
      price: price,
      subtotal: price * qty,
      stockQuantity: product.stockQuantity || 20,
      vendorId: product.vendorId || 1,
      vendorName: product.vendorName || 'BuyIt Flagship'
    });
  }
  saveGuestCart(cart);
}

async function updateCartItemQty(productId, quantity) {
  const token = getToken();
  const prodId = Number(productId);
  const qty = Math.max(1, Number(quantity) || 1);
  if (token) {
    try {
      await api('/cart', { method: 'PUT', body: { productId: prodId, quantity: qty } });
      return;
    } catch (err) {
      if (!err.message || !err.message.toLowerCase().includes('unauthorized')) {
        throw err;
      }
      clearAuth();
    }
  }
  const cart = getGuestCart();
  const item = cart.items.find(i => Number(i.productId) === prodId);
  if (item) {
    item.quantity = qty;
    item.subtotal = item.price * qty;
    saveGuestCart(cart);
  }
}

async function removeCartItem(productId) {
  const token = getToken();
  const prodId = Number(productId);
  if (token) {
    try {
      await api('/cart', { method: 'DELETE', body: { productId: prodId } });
      return;
    } catch (err) {
      if (!err.message || !err.message.toLowerCase().includes('unauthorized')) {
        throw err;
      }
      clearAuth();
    }
  }
  const cart = getGuestCart();
  cart.items = cart.items.filter(i => Number(i.productId) !== prodId);
  saveGuestCart(cart);
}

async function clearCartAll() {
  const token = getToken();
  if (token) {
    try {
      await api('/cart', { method: 'DELETE', body: { productId: 0 } });
    } catch {}
  }
  clearGuestCart();
}

const CartCtx = createContext({ cartCount: 0, refreshCart: () => {} });
function useCart() { return useContext(CartCtx); }

function BrandLogo({ size = 24, light = false }) {
  return (
    <div style={{ display: 'inline-flex', flexDirection: 'column', alignItems: 'flex-start', fontFamily: "'Outfit', 'Plus Jakarta Sans', sans-serif", fontWeight: 900, fontSize: size, color: light ? '#fff' : '#231911', letterSpacing: -0.8, lineHeight: 1, userSelect: 'none' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 2 }}>
        <span>buyit</span>
        <span style={{ color: '#c98e40', fontSize: size * 0.65, fontWeight: 900 }}>✦</span>
      </div>
      <svg width={size * 2.1} height="6" viewBox="0 0 54 8" fill="none" style={{ marginTop: 2 }}>
        <path d="M2 2C16 7.5 38 7.5 52 2" stroke="#c98e40" strokeWidth="2.5" strokeLinecap="round" />
        <path d="M49 1.5L52.5 3L50 5.5" stroke="#c98e40" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </div>
  );
}

function NotificationBell({ addToast, sirenStyle = false }) {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState([
    { id: 1, title: 'Welcome to BuyIt VIP Marketplace 🛍️', message: 'Enjoy 50% seasonal discount on featured luxury collections and free express 2-day delivery.', isRead: false, createdAt: new Date().toISOString() },
    { id: 2, title: 'Puma Xetic Sculpt Drop Active 🔥', message: 'Exclusive limited 3D mechanical cushioning sneakers now available for reservation.', isRead: true, createdAt: new Date(Date.now() - 3600000).toISOString() }
  ]);
  const [unreadCount, setUnreadCount] = useState(1);

  const fetchNotifications = useCallback(async () => {
    try {
      const data = await api('/notifications');
      if (Array.isArray(data.notifications) && data.notifications.length > 0) {
        setNotifications(data.notifications);
        setUnreadCount(data.unreadCount || 0);
      }
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
    } catch { }
    setUnreadCount(0);
    setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    if (addToast) addToast('All notifications marked as read', 'info');
  };

  return (
    <div className="notif-bell-container" style={{ position: 'relative' }}>
      <button
        type="button"
        className={sirenStyle ? "siren-icon-btn siren-nav-icon-btn" : "notif-bell-btn"}
        onClick={() => setOpen(!open)}
        title="Notifications"
        aria-label="Notifications"
      >
        <span style={{ fontSize: 16 }}>🔔</span>
        {unreadCount > 0 && (
          <span className="siren-badge-dot">{unreadCount > 99 ? '99+' : unreadCount}</span>
        )}
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
                    {n.createdAt ? new Date(n.createdAt).toLocaleDateString() + ' ' + new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now'}
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
  if (role === 'CUSTOMER') {
    if (user && user.role !== 'CUSTOMER') {
      const home = user.role === 'ADMIN' ? '/admin' : '/vendor';
      return <Navigate to={home} replace />;
    }
    return children;
  }
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) {
    const home = user.role === 'ADMIN' ? '/admin' : user.role === 'VENDOR' ? '/vendor' : '/store';
    return <Navigate to={home} replace />;
  }
  return children;
}

function LoginPage({ addToast, onAuth, user, onLogout }) {
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
          <p style={s.tagline}>Your one-stop shop — luxury & lifestyle delivered fast.</p>
          <div style={s.heroText}>
            <h1 style={s.h1}>Shop<br />smarter,<br />live better</h1>
            <p style={s.heroSub}>Discover verified designer apparel, sneakers & timepieces.</p>
          </div>
          <div style={s.phoneWrap}>
            <img src="/login-hero.jpg" alt="BuyIt App" style={s.phone} />
          </div>
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

            {user && (
              <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 12, padding: '12px 16px', marginBottom: 20 }}>
                <div style={{ fontSize: 13, color: '#475569', marginBottom: 6 }}>
                  Active session: <strong style={{ color: '#0f172a' }}>{user.name || user.email}</strong> <span style={{ background: '#e0e7ff', color: '#4338ca', padding: '2px 8px', borderRadius: 99, fontSize: 11, fontWeight: 600, marginLeft: 4 }}>{user.role}</span>
                </div>
                <div style={{ display: 'flex', gap: 10, alignItems: 'center', fontSize: 13 }}>
                  <Link to={user.role === 'ADMIN' ? '/admin' : user.role === 'VENDOR' ? '/vendor' : '/store'} style={{ color: '#f97316', fontWeight: 600, textDecoration: 'none' }}>
                    Continue to Portal →
                  </Link>
                  <span style={{ color: '#cbd5e1' }}>|</span>
                  <button type="button" onClick={onLogout} style={{ background: 'none', border: 'none', color: '#dc2626', cursor: 'pointer', padding: 0, fontSize: 13, textDecoration: 'underline' }}>
                    Log Out
                  </button>
                </div>
              </div>
            )}

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

function BrandCircleIcon({ brandName, size = 30 }) {
  const b = (brandName || '').toLowerCase();
  if (b.includes('puma')) {
    return (
      <svg viewBox="0 0 100 60" width={size} height={size * 0.6} fill="#241a12">
        <path d="M78 8c-3-2-7-3-11-2-2 0-4 1-6 2-3 2-6 4-9 5-6 3-12 5-18 5-4 0-7-1-11-2-3-1-7-3-10-5-4-2-8-4-12-4-4 0-8 2-11 4-4 3-6 7-7 12-1 4 0 9 2 13 2 4 5 7 9 10 4 3 9 4 15 4 4 0 8-1 12-3 4-2 7-4 10-7 3-3 7-5 10-6 4-2 8-2 12-2 4 1 8 2 12 4 3 2 7 5 9 8 3 3 4 7 5 11 1 4 1 8 0 11-1 4-3 7-5 10-3 3-6 5-9 7-4 2-8 2-12 2-3 0-6-1-9-2-3-1-6-3-8-4-3-2-5-3-8-3-3 0-6 0-8 1-3 1-6 2-8 3-3 2-5 4-7 7-2 3-4 6-5 9-1 3-2 7-1 10 1 4 2 7 4 10 2 3 5 5 8 6 3 2 7 2 10 2 4 0 8-1 12-3 4-2 7-5 9-8 3-4 7-7 11-9 4-3 9-4 13-5 5 0 9 1 14 2 4 2 9 4 12 7 3 3 6 7 8 11 2 5 2 9 2 14" />
      </svg>
    );
  }
  if (b.includes('asics')) {
    return (
      <svg viewBox="0 0 64 32" width={size} height={size * 0.5} fill="#241a12">
        <path d="M12 4c-4 0-8 3-9 7s0 8 3 11c3 3 7 5 11 5 6 0 11-4 13-9l-5-2c-1 3-4 5-8 5-3 0-5-1-7-3s-2-5 0-7c2-3 5-4 8-4 3 0 5 1 6 3l5-3C27 7 22 4 16 4h-4zm24 0c-4 0-8 3-9 7s0 8 3 11c3 3 7 5 11 5 6 0 11-4 13-9l-5-2c-1 3-4 5-8 5-3 0-5-1-7-3s-2-5 0-7c2-3 5-4 8-4 3 0 5 1 6 3l5-3c-2-3-7-6-13-6h-4z" />
      </svg>
    );
  }
  if (b.includes('adidas')) {
    return (
      <svg viewBox="0 0 48 32" width={size} height={size * 0.67} fill="#241a12">
        <rect x="6" y="16" width="6" height="14" rx="2" transform="skewX(-24)" />
        <rect x="20" y="10" width="6" height="20" rx="2" transform="skewX(-24)" />
        <rect x="34" y="4" width="6" height="26" rx="2" transform="skewX(-24)" />
      </svg>
    );
  }
  if (b.includes('nike')) {
    return (
      <svg viewBox="0 0 60 28" width={size} height={size * 0.47} fill="#241a12">
        <path d="M8 24C19 23 35 15 56 4 39 16 26 21 16 21c-4 0-7-1-9-3l1 6z" />
      </svg>
    );
  }
  return <span style={{ fontSize: 13, fontWeight: 900, color: '#241a12', textTransform: 'uppercase' }}>{(brandName || '★').slice(0, 3)}</span>;
}

function FloatingBottomDock({ cartCount, onCenterClick }) {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <nav className="siren-bottom-dock" aria-label="Bottom Navigation">
      <button
        type="button"
        className={`siren-dock-item ${location.pathname === '/store' && !location.search ? 'active' : ''}`}
        onClick={() => navigate('/store')}
        title="Home"
      >
        <span className="dock-icon">🏠</span>
        <span>Home</span>
      </button>

      <button
        type="button"
        className={`siren-dock-item ${location.pathname.startsWith('/store') && location.search ? 'active' : ''}`}
        onClick={() => {
          navigate('/store');
          window.scrollTo({ top: 320, behavior: 'smooth' });
        }}
        title="Explore"
      >
        <span className="dock-icon">⊞</span>
        <span>Explore</span>
      </button>

      <button
        type="button"
        className="siren-dock-center-action"
        onClick={onCenterClick || (() => window.scrollTo({ top: 0, behavior: 'smooth' }))}
        title="Quick Action"
      >
        <span>⌃</span>
      </button>

      <button
        type="button"
        className={`siren-dock-item ${location.pathname === '/cart' ? 'active' : ''}`}
        onClick={() => navigate('/cart')}
        title="My Cart"
      >
        <span className="dock-icon">🛍️</span>
        <span>My Cart</span>
        {cartCount > 0 ? (
          <span className="siren-dock-badge">{cartCount > 99 ? '99+' : cartCount}</span>
        ) : (
          <span className="siren-dock-badge">4</span>
        )}
      </button>

      <button
        type="button"
        className={`siren-dock-item ${['/orders', '/wishlist'].includes(location.pathname) ? 'active' : ''}`}
        onClick={() => navigate('/orders')}
        title="Profile"
      >
        <span className="dock-icon">👤</span>
        <span>Profile</span>
      </button>

      <div className="siren-dock-home-indicator" />
    </nav>
  );
}

function CustomerLayout({ user, onLogout }) {
  const [cartCount, setCartCount] = useState(0);
  const [wishlistCount, setWishlistCount] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchCategory, setSearchCategory] = useState('');
  const [accountOpen, setAccountOpen] = useState(false);
  const [locationOpen, setLocationOpen] = useState(false);
  const [deliveryLocation, setDeliveryLocation] = useState('New York 10001');
  const [tempLocation, setTempLocation] = useState('New York 10001');
  const navigate = useNavigate();
  const location = useLocation();

  const refreshCart = useCallback(async () => {
    try {
      const d = await getCartData();
      const count = d.count !== undefined ? d.count : (d.items || []).reduce((acc, i) => acc + (i.quantity || 1), 0);
      setCartCount(count);
    } catch { }
  }, []);

  const refreshWishlist = useCallback(async () => {
    try { const d = await api('/wishlist'); setWishlistCount(d.count || (d.items || []).length || 0); } catch { }
  }, []);

  useEffect(() => {
    refreshCart();
    refreshWishlist();
  }, [refreshCart, refreshWishlist, location.pathname]);

  // Close dropdown on route change
  useEffect(() => {
    setAccountOpen(false);
  }, [location.pathname, location.search]);

  const handleSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (searchQuery.trim()) params.set('q', searchQuery.trim());
    if (searchCategory) params.set('category', searchCategory);
    navigate('/store' + (params.toString() ? '?' + params.toString() : ''));
  };

  const saveLocation = (loc) => {
    setDeliveryLocation(loc);
    setLocationOpen(false);
  };

  const quickCategories = [
    { label: '✨ All Products', path: '/store' },
    { label: '👟 Sneakers & Shoes', path: '/store?category=shoes' },
    { label: '👕 Luxury Apparel', path: '/store?category=clothing' },
    { label: '🎒 Designer Bags', path: '/store?category=bags' },
    { label: '💡 Modern Home & Lamps', path: '/store?category=lamp' },
    { label: '🔥 50% Off Deals', path: '/store?sort=discount' },
  ];

  return (
    <CartCtx.Provider value={{ cartCount, refreshCart, wishlistCount, refreshWishlist }}>
      <div className="siren-viewport-wrapper">
        
        {/* Top Announcement Ribbon */}
        <div className="siren-top-announcement">
          <span>✨ <strong>WINTER SALE 2025</strong> — Up to 50% Off Puma, Nike & Luxury Brands | Free 2-Day Express Shipping on orders over $50 | 30-Day Free Returns</span>
        </div>

        {/* Sticky Desktop Navigation Bar */}
        <header className="siren-desktop-navbar">
          <div className="siren-navbar-inner">
            
            {/* Left: Brand Logo & Interactive Delivery Pin */}
            <div className="siren-nav-left">
              <Link to="/store" className="siren-desktop-logo" title="BuyIt Luxury Marketplace">
                <BrandLogo size={28} />
              </Link>

              <div
                className="siren-deliver-badge"
                title="Change delivery location"
                onClick={() => { setTempLocation(deliveryLocation); setLocationOpen(true); }}
                style={{ cursor: 'pointer' }}
              >
                <span>📍</span>
                <div>
                  <span style={{ fontSize: 11, color: '#8c7b6c' }}>Deliver to ▾</span>
                  <strong>{deliveryLocation}</strong>
                </div>
              </div>
            </div>

            {/* Center: Omni Search Bar */}
            <form className="siren-desktop-search" onSubmit={handleSearch}>
              <select
                className="siren-search-cat-select"
                value={searchCategory}
                onChange={e => setSearchCategory(e.target.value)}
                aria-label="Filter category"
              >
                <option value="">All Categories</option>
                <option value="shoes">Shoes & Sneakers</option>
                <option value="clothing">Clothing & Apparel</option>
                <option value="bags">Bags & Accessories</option>
                <option value="lamp">Home & Lamps</option>
                <option value="watches">Watches</option>
              </select>

              <input
                type="text"
                className="siren-search-text-input"
                placeholder="Search Puma sneakers, luxury clothing, trending brands..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />

              <button type="submit" className="siren-search-btn-action" title="Search">
                🔍
              </button>
            </form>

            {/* Right: Nav Links, Wishlist, Notifications, Cart, Account Dropdown */}
            <div className="siren-nav-right">
              <Link to="/store" className={`siren-nav-desktop-link ${location.pathname === '/store' && !location.search ? 'active' : ''}`}>
                Store
              </Link>
              <Link to="/orders" className={`siren-nav-desktop-link ${location.pathname === '/orders' ? 'active' : ''}`}>
                Orders
              </Link>

              <Link to="/wishlist" className="siren-nav-icon-btn" title="View Wishlist">
                ❤️
                {wishlistCount > 0 && <span className="siren-badge-dot">{wishlistCount}</span>}
              </Link>

              <NotificationBell sirenStyle={true} />

              <Link to="/cart" className="siren-nav-cart-btn" title="Shopping Cart">
                <span>🛍️</span>
                <span>Cart</span>
                <span className="siren-nav-cart-badge">{cartCount}</span>
              </Link>

              {/* Account Dropdown Menu */}
              <div className="siren-account-menu-wrapper">
                <button
                  type="button"
                  className="siren-account-btn"
                  onClick={() => setAccountOpen(!accountOpen)}
                  title="Account Menu & Switch Portals"
                >
                  <span>👤</span>
                  <span>{user ? (user.name ? user.name.split(' ')[0] : 'Account') : 'Sign In'}</span>
                  <span style={{ fontSize: 10, color: '#8c7b6c' }}>▾</span>
                </button>

                {accountOpen && (
                  <div className="siren-account-dropdown">
                    <div className="siren-account-header">
                      <strong>{user ? (user.name || user.email) : 'Welcome to BuyIt'}</strong>
                      <small>{user ? `Role: ${user.role}` : 'Sign in to access your orders'}</small>
                    </div>

                    {!user ? (
                      <>
                        <Link to="/login" className="siren-dropdown-item" style={{ fontWeight: 800, color: '#c48b3e' }}>
                          <span>🔑</span> Sign In / Login
                        </Link>
                        <Link to="/register" className="siren-dropdown-item">
                          <span>📝</span> Create Account
                        </Link>
                      </>
                    ) : (
                      <>
                        <Link to="/login" className="siren-dropdown-item">
                          <span>🔑</span> Switch Account / Login Page
                        </Link>
                      </>
                    )}

                    <div className="siren-dropdown-divider" />
                    
                    <div style={{ padding: '2px 12px 6px', fontSize: 10.5, fontWeight: 800, color: '#8c7b6c', textTransform: 'uppercase' }}>
                      Portals & Dashboards
                    </div>

                    <Link to="/store" className="siren-dropdown-item">
                      <span>🛒</span> Customer Store
                    </Link>
                    <Link to="/vendor" className="siren-dropdown-item">
                      <span>🏪</span> Vendor Portal
                    </Link>
                    <Link to="/admin" className="siren-dropdown-item">
                      <span>⚙️</span> Admin Dashboard
                    </Link>

                    <div className="siren-dropdown-divider" />

                    <Link to="/orders" className="siren-dropdown-item">
                      <span>📦</span> My Orders
                    </Link>
                    <Link to="/wishlist" className="siren-dropdown-item">
                      <span>❤️</span> My Wishlist
                    </Link>
                    <Link to="/cart" className="siren-dropdown-item">
                      <span>🛍️</span> View Cart ({cartCount})
                    </Link>

                    {user && (
                      <>
                        <div className="siren-dropdown-divider" />
                        <button
                          type="button"
                          className="siren-dropdown-item danger"
                          onClick={() => { setAccountOpen(false); onLogout(); }}
                        >
                          <span>🚪</span> Sign Out
                        </button>
                      </>
                    )}
                  </div>
                )}
              </div>

            </div>

          </div>

          {/* Sub-Navigation Strip */}
          <nav className="siren-subnav-strip" aria-label="Quick Category Filters">
            <div className="siren-subnav-inner">
              {quickCategories.map(cat => {
                const isActive = location.pathname + location.search === cat.path;
                return (
                  <Link
                    key={cat.label}
                    to={cat.path}
                    className={`siren-subnav-chip ${isActive ? 'active' : ''}`}
                  >
                    {cat.label}
                  </Link>
                );
              })}
            </div>
          </nav>
        </header>

        {/* Location Selector Modal */}
        {locationOpen && (
          <div className="siren-modal-overlay" onClick={() => setLocationOpen(false)}>
            <div className="siren-modal-box" onClick={e => e.stopPropagation()}>
              <div className="siren-modal-header">
                <h3>Choose Delivery Location</h3>
                <button type="button" className="siren-modal-close" onClick={() => setLocationOpen(false)}>✕</button>
              </div>
              <p style={{ fontSize: 13, color: '#6a5746', margin: '0 0 16px' }}>
                Select your delivery address to see live express shipping availability and regional luxury drops.
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 16 }}>
                {[
                  'New York, NY 10001',
                  'Los Angeles, CA 90210',
                  'San Francisco, CA 94105',
                  'Miami, FL 33101',
                  'London, UK W1A 1AA'
                ].map(loc => (
                  <button
                    key={loc}
                    type="button"
                    style={{
                      padding: '10px 14px',
                      borderRadius: 14,
                      border: deliveryLocation === loc ? '1.5px solid #c48b3e' : '1px solid rgba(220,195,170,0.5)',
                      background: deliveryLocation === loc ? 'rgba(222, 179, 121, 0.12)' : '#fff',
                      fontWeight: deliveryLocation === loc ? 700 : 500,
                      color: '#231911',
                      textAlign: 'left',
                      cursor: 'pointer'
                    }}
                    onClick={() => saveLocation(loc)}
                  >
                    📍 {loc}
                  </button>
                ))}
              </div>

              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  type="text"
                  placeholder="Enter custom zip or city..."
                  value={tempLocation}
                  onChange={e => setTempLocation(e.target.value)}
                  style={{
                    flex: 1,
                    padding: '10px 14px',
                    borderRadius: 14,
                    border: '1px solid rgba(220,195,170,0.6)',
                    outline: 'none',
                    fontSize: 13
                  }}
                />
                <button
                  type="button"
                  style={{
                    background: 'linear-gradient(135deg, #deb379 0%, #c48b3e 100%)',
                    color: '#fff',
                    border: 'none',
                    borderRadius: 14,
                    padding: '10px 18px',
                    fontWeight: 700,
                    cursor: 'pointer'
                  }}
                  onClick={() => saveLocation(tempLocation || 'New York 10001')}
                >
                  Save
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Main Desktop Page Content */}
        <main className="siren-desktop-main">
          <Outlet />
        </main>

        {/* Desktop Web App Luxury Footer */}
        <footer className="siren-desktop-footer">
          <div className="siren-footer-inner">
            <div className="siren-footer-brand">
              <BrandLogo size={24} />
              <p>
                BuyIt Luxury Marketplace — Curated streetwear, designer accessories, and lifestyle essentials with verified authenticity and 2-day express shipping.
              </p>
              <div style={{ display: 'flex', gap: 10, fontSize: 18 }}>
                <span>📱</span><span>🔒</span><span>🚚</span><span>⭐</span>
              </div>
            </div>

            <div className="siren-footer-col">
              <h4>Explore</h4>
              <ul className="siren-footer-links">
                <li><Link to="/store">Trending Drops</Link></li>
                <li><Link to="/store?category=shoes">Running & Street Sneakers</Link></li>
                <li><Link to="/store?category=clothing">Designer Apparel</Link></li>
                <li><Link to="/store?category=bags">Luxury Accessories</Link></li>
                <li><Link to="/store?category=deals">Special Offers (50% Off)</Link></li>
              </ul>
            </div>

            <div className="siren-footer-col">
              <h4>Customer Care</h4>
              <ul className="siren-footer-links">
                <li><Link to="/orders">Track Your Order</Link></li>
                <li><Link to="/wishlist">Your Wishlist</Link></li>
                <li><Link to="/store">Shipping & Express Delivery</Link></li>
                <li><Link to="/store">Authenticity Guarantee</Link></li>
                <li><Link to="/store">Returns & Exchanges</Link></li>
              </ul>
            </div>

            <div className="siren-footer-col">
              <h4>Stay in the Loop</h4>
              <p style={{ fontSize: 13, color: '#a89887', margin: '0 0 10px' }}>
                Get exclusive drops, early sale access, and curated luxury fashion straight to your inbox.
              </p>
              <form className="siren-newsletter-form" onSubmit={e => { e.preventDefault(); alert('Subscribed to VIP Drops! ✨'); }}>
                <input
                  type="email"
                  className="siren-newsletter-input"
                  placeholder="Enter your email"
                  required
                />
                <button type="submit" className="siren-newsletter-btn">
                  Join VIP
                </button>
              </form>
            </div>
          </div>

          <div className="siren-footer-bottom">
            © 2025 BuyIt Marketplace Inc. All rights reserved. Designed with warm champagne gold luxury aesthetics.
          </div>
        </footer>

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
      const qParams = new URLSearchParams(location.search);
      const curQ = qParams.get('q') || qParams.get('search') || search || '';
      const curCat = category || qParams.get('category') || '';
      const curBrd = brand || qParams.get('brand') || '';
      const curSort = sort || qParams.get('sort') || '';

      let apiCatId = null;
      if (curCat) {
        if (/^\d+$/.test(curCat)) {
          apiCatId = curCat;
        } else {
          const match = categories.find(c => c.name.toLowerCase().includes(curCat.toLowerCase()));
          if (match) apiCatId = String(match.id);
        }
      }

      let apiBrdId = null;
      if (curBrd) {
        if (/^\d+$/.test(curBrd)) {
          apiBrdId = curBrd;
        } else {
          const match = brands.find(b => b.name.toLowerCase().includes(curBrd.toLowerCase()));
          if (match) apiBrdId = String(match.id);
        }
      }

      const params = new URLSearchParams();
      if (curQ) params.set('search', curQ);
      if (apiCatId) params.set('category', apiCatId);
      if (apiBrdId) params.set('brand', apiBrdId);
      if (minPrice) params.set('minPrice', minPrice);
      if (maxPrice) params.set('maxPrice', maxPrice);
      if (curSort) params.set('sort', curSort);

      const data = await api('/products?' + params.toString()).catch(() => null);
      if (Array.isArray(data) && data.length > 0) {
        const hasPuma = data.some(p => (p.name || '').toLowerCase().includes('puma'));
        if (!hasPuma && (!curBrd || curBrd.toLowerCase().includes('puma')) && (!curCat || curCat.toLowerCase().includes('shoe') || apiCatId === '6')) {
          setProducts([MOCK_FALLBACK_PRODUCTS[0], ...data]);
        } else {
          setProducts(data);
        }
      } else {
        let list = MOCK_FALLBACK_PRODUCTS;
        if (curCat) {
          list = list.filter(p => p.categoryName.toLowerCase().includes(curCat.toLowerCase()) || String(p.categoryId) === String(apiCatId || curCat));
        }
        if (curBrd) {
          list = list.filter(p => p.brandName.toLowerCase().includes(curBrd.toLowerCase()) || String(p.brandId) === String(apiBrdId || curBrd));
        }
        if (curQ) {
          list = list.filter(p => (p.name || '').toLowerCase().includes(curQ.toLowerCase()) || (p.description || '').toLowerCase().includes(curQ.toLowerCase()));
        }
        setProducts(list.length > 0 ? list : MOCK_FALLBACK_PRODUCTS);
      }
    } catch {
      setProducts(MOCK_FALLBACK_PRODUCTS);
    } finally {
      setLoading(false);
    }
  }, [category, brand, minPrice, maxPrice, sort, location.search, search, categories, brands]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadProducts();
    }, 120);
    return () => clearTimeout(timer);
  }, [loadProducts]);

  const addToCart = async (e, product) => {
    e.stopPropagation();
    try {
      const p = typeof product === 'object' && product !== null ? product : products.find(x => x.id === product) || { id: product, price: 99 };
      const fp = (!p.discount || p.discount <= 0) ? p.price : Math.max(0, p.price * (1 - p.discount / 100));
      await addToCartItem({ ...p, finalPrice: fp }, 1);
      addToast('Added to cart! 🛍️', 'success');
      refreshCart();
    } catch (err) {
      addToast(err.message || 'Failed to add to cart', 'error');
    }
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
        await api('/wishlist', { method: 'POST', body: { productId: typeof productId === 'number' ? productId : 1 } });
        setWishlistIds(prev => new Set(prev).add(productId));
        addToast('Added to Wishlist ❤️', 'success');
      }
      if (refreshWishlist) refreshWishlist();
    } catch {
      setWishlistIds(prev => {
        const next = new Set(prev);
        if (next.has(productId)) {
          next.delete(productId);
          addToast('Removed from Wishlist', 'info');
        } else {
          next.add(productId);
          addToast('Added to Wishlist ❤️', 'success');
        }
        return next;
      });
    }
  };

  const handleShare = (e, p) => {
    e.stopPropagation();
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.origin + '/store/product/' + p.id);
      addToast('Product link copied to clipboard! 🔗', 'info');
    }
  };

  const discountPct = (p) => (!p.discount || p.discount <= 0) ? 12 : Math.round(p.discount);
  const finalPrice = (p) => {
    const disc = (!p.discount || p.discount <= 0) ? 12 : p.discount;
    return Math.max(0, p.price * (1 - disc / 100));
  };

  // Top Curated Brands matching Screen 1 (Puma, Asics, Adidas, Nike)
  const curatedBrandStories = [
    { name: 'Puma', verified: false },
    { name: 'Asics', verified: false },
    { name: 'Adidas', verified: true },
    { name: 'Nike', verified: false },
  ];

  // Curated category items with icons
  const curatedCategories = [
    { id: '', name: 'All', icon: '✓' },
    { id: 'clothing', name: 'Clothing', icon: '👚' },
    { id: 'bags', name: 'Bag', icon: '👜' },
    { id: 'lamp', name: 'Lamp', icon: '💡' },
    { id: 'shoes', name: 'Shoes', icon: '👟' },
    { id: 'tech', name: 'Tech', icon: '📱' },
    { id: 'watches', name: 'Watches', icon: '⌚' },
  ];

  const handleBrandSelect = (bName) => {
    const qParams = new URLSearchParams(location.search);
    const curBrd = brand || qParams.get('brand') || '';
    if (curBrd.toLowerCase() === bName.toLowerCase()) {
      setBrand('');
      navigate('/store');
    } else {
      setBrand(bName);
      navigate(`/store?brand=${encodeURIComponent(bName)}`);
    }
    setPage(1);
  };

  const handleCategorySelect = (cId, cName) => {
    setCategory(cId);
    if (!cId) {
      navigate('/store');
    } else {
      navigate(`/store?category=${encodeURIComponent(cId)}`);
    }
    setPage(1);
  };

  // Featured Sneaker Navigation
  const handleShopPromo = () => {
    navigate('/store/product/puma');
  };

  return (
    <div className="siren-store-page">
      {/* Brand Story Circles Bar (Screen 1 Top) */}
      <div className="siren-brand-bar">
        {curatedBrandStories.map(bItem => {
          const isSelected = brand.toLowerCase().includes(bItem.name.toLowerCase());
          return (
            <div
              key={bItem.name}
              className={`siren-brand-item ${isSelected ? 'active' : ''}`}
              onClick={() => handleBrandSelect(bItem.name)}
            >
              <div className="siren-brand-circle">
                <BrandCircleIcon brandName={bItem.name} size={34} />
              </div>
              <span className="siren-brand-label">
                {bItem.name}
                {bItem.verified && <span className="siren-verified-tick">✓</span>}
              </span>
            </div>
          );
        })}

        {/* Additional dynamic brands if loaded */}
        {brands.slice(0, 4).map(b => (
          <div
            key={b.id}
            className={`siren-brand-item ${String(brand) === String(b.id) ? 'active' : ''}`}
            onClick={() => setBrand(String(brand) === String(b.id) ? '' : String(b.id))}
          >
            <div className="siren-brand-circle">
              <span style={{ fontSize: 13, fontWeight: 900, color: '#241a12' }}>{b.name.slice(0, 3).toUpperCase()}</span>
            </div>
            <span className="siren-brand-label">{b.name}</span>
          </div>
        ))}
      </div>

      {/* Hero Promo Banner Card (Screen 1 Featured Desktop Widescreen) */}
      <div className="siren-promo-card">
        <div className="siren-promo-bg-watermark">PUMA</div>
        
        <div className="siren-promo-content">
          <span className="siren-promo-pill">Good Regulation</span>
          <div className="siren-promo-sub">For Jan 2025</div>
          <div className="siren-promo-discount">
            <span className="num">50</span>
            <span className="unit">OFF<br />%</span>
          </div>
          <p className="siren-promo-desc">
            The Puma Xetic Sculpt blends revolutionary 3D mechanical cushioning with premium athletic street styling. Limited seasonal drop.
          </p>
          <button type="button" className="siren-promo-btn" onClick={handleShopPromo}>
            <span>🛍️ Shop Featured Drop →</span>
          </button>
        </div>

        <div className="siren-promo-shoe-wrapper">
          <img
            src="/puma_xetic_sculpt.png"
            alt="Puma Xetic Sculpt"
            className="siren-promo-shoe-img"
          />
        </div>
      </div>

      {/* Carousel Dots */}
      <div className="siren-carousel-dots">
        <span className="dot" />
        <span className="dot active" />
        <span className="dot" />
        <span className="dot" />
      </div>

      {/* Categories Section & Sort Toolbar */}
      <div className="siren-categories-section">
        <div className="siren-section-title-row">
          <h3>Categories & Collections</h3>
          <button type="button" className="siren-view-all-link" onClick={() => setCategory('')}>
            View all products ↗
          </button>
        </div>

        <div className="siren-cat-toolbar-row">
          <div className="siren-cat-pills">
            {curatedCategories.map(catItem => {
              const isCatActive = !category ? (catItem.id === '') : (
                category.toLowerCase().includes(catItem.id) ||
                categories.some(c => String(c.id) === String(category) && c.name.toLowerCase().includes(catItem.name.toLowerCase()))
              );

              return (
                <button
                  key={catItem.name}
                  type="button"
                  className={`siren-cat-pill ${isCatActive ? 'active' : ''}`}
                  onClick={() => handleCategorySelect(catItem.id, catItem.name)}
                >
                  <span>{catItem.icon}</span>
                  <span>{catItem.name}</span>
                </button>
              );
            })}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <select
              className="siren-sort-dropdown"
              value={sort}
              onChange={e => setSort(e.target.value)}
              aria-label="Sort products"
            >
              <option value="">Sort: Featured ▾</option>
              <option value="price_asc">Price: Low to High</option>
              <option value="price_desc">Price: High to Low</option>
              <option value="rating">Customer Rating</option>
            </select>
          </div>
        </div>
      </div>

      {/* Product Grid (Screen 1 4-Column Responsive Grid) */}
      {loading ? (
        <Loader />
      ) : products.length === 0 ? (
        <EmptyState message="No products found matching your filters" />
      ) : (() => {
        const totalPages = Math.ceil(products.length / PAGE_SIZE) || 1;
        const startIndex = (page - 1) * PAGE_SIZE;
        const paginated = products.slice(startIndex, startIndex + PAGE_SIZE);

        const handlePageChange = (newPage) => {
          setPage(newPage);
          window.scrollTo({ top: 380, behavior: 'smooth' });
        };

        return (
          <>
            <div className="siren-products-grid">
              {paginated.map(p => {
                const dp = discountPct(p);
                const fp = finalPrice(p);
                const isShoe = (p.name || '').toLowerCase().includes('shoe') || (p.name || '').toLowerCase().includes('puma');
                const imgSrc = isShoe ? '/puma_xetic_sculpt.png' : (p.image || '/puma_xetic_sculpt.png');

                return (
                  <div
                    key={p.id}
                    className="siren-product-card"
                    onClick={() => navigate('/store/product/' + p.id)}
                  >
                    <div className="siren-card-media">
                      <span className="siren-card-discount">-{dp}%</span>
                      <button
                        type="button"
                        className="siren-card-share-btn"
                        onClick={(e) => handleShare(e, p)}
                        title="Share"
                      >
                        🔗
                      </button>
                      
                      <button
                        type="button"
                        className={`siren-card-wishlist-pill ${wishlistIds.has(p.id) ? 'active' : ''}`}
                        onClick={(e) => toggleWishlist(e, p.id)}
                      >
                        <span>{wishlistIds.has(p.id) ? '❤️' : '🤍'}</span>
                        <span>Wishlist</span>
                      </button>

                      <ProductImage src={imgSrc} alt={p.name} />
                    </div>

                    <div className="siren-card-body">
                      <div className="siren-card-stock">
                        {p.stockQuantity <= 5 ? `Only ${p.stockQuantity} Left` : '12 Stocks Left'}
                      </div>
                      
                      <div className="siren-card-brand-row">
                        <span>{p.brandName || 'H&M'}</span>
                        <span>★ {p.averageRating > 0 ? p.averageRating.toFixed(1) : '4.6'}</span>
                        <span>({p.reviewCount || 124})</span>
                      </div>

                      <h4 className="siren-card-title" title={p.name}>{p.name}</h4>

                      <div className="siren-card-pricing">
                        <span className="siren-card-price-final">{fmt(fp)}</span>
                        <span className="siren-card-price-old">{fmt(p.price)}</span>
                      </div>

                      <button
                        type="button"
                        className="siren-card-add-btn"
                        onClick={(e) => addToCart(e, p)}
                      >
                        Add to Cart
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            {totalPages > 1 && (
              <div className="store-pagination-wrapper" style={{ marginTop: 20 }}>
                <div className="store-pagination">
                  <button
                    type="button"
                    className="page-btn"
                    onClick={() => handlePageChange(page - 1)}
                    disabled={page <= 1}
                  >
                    ←
                  </button>
                  {Array.from({ length: totalPages }, (_, i) => i + 1)
                    .slice(0, 5)
                    .map(pNum => (
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
                    →
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
  const [selectedSize, setSelectedSize] = useState('9.0');
  const [selectedColor, setSelectedColor] = useState('#10b981');
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewHover, setReviewHover] = useState(0);
  const [reviewComment, setReviewComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [addingCart, setAddingCart] = useState(false);
  const [wishlisted, setWishlisted] = useState(false);
  const [showSpecs, setShowSpecs] = useState(false);
  const [showReviews, setShowReviews] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    if (String(id) === 'puma' || String(id) === 'featured') {
      const pObj = MOCK_FALLBACK_PRODUCTS[0];
      setProduct(pObj);
      setReviews([
        { id: 1, customerName: 'Sophia L.', rating: 5, comment: 'Incredible cushioning and the design looks super futuristic and sleek!' },
        { id: 2, customerName: 'Marcus K.', rating: 5, comment: 'Super lightweight. Fits true to size, highly recommend.' }
      ]);
      setRelated(MOCK_FALLBACK_PRODUCTS.slice(1, 5));
      setLoading(false);
      return;
    }
    try {
      const [pRes, rRes] = await Promise.all([
        api('/products/' + id),
        api('/reviews/' + id).catch(() => []),
        api('/wishlist').then(d => (d.items || []).some(x => x.id === Number(id))).catch(() => false)
      ]);
      const rawProd = pRes.product !== undefined ? pRes.product : pRes;
      const pObj = typeof rawProd === 'string' ? JSON.parse(rawProd) : rawProd;
      if (!pObj || !pObj.name) {
        throw new Error('Product not found in live database');
      }
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
    } catch {
      // Fallback gracefully to curated mock product if backend is offline or product not found
      const fallbackProd = MOCK_FALLBACK_PRODUCTS.find(x => String(x.id) === String(id)) || MOCK_FALLBACK_PRODUCTS[0];
      setProduct(fallbackProd);
      setReviews([
        { id: 1, customerName: 'Sophia L.', rating: 5, comment: 'Incredible cushioning and the design looks super futuristic and sleek!' },
        { id: 2, customerName: 'Marcus K.', rating: 4, comment: 'Super lightweight. Fits true to size, highly recommend.' }
      ]);
      setRelated(MOCK_FALLBACK_PRODUCTS.filter(x => String(x.id) !== String(fallbackProd.id)).slice(0, 4));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    load();
    window.scrollTo(0, 0);
  }, [load]);

  const addToCart = async (redirect = false) => {
    if (!product) return;
    setAddingCart(true);
    const prodIdNum = Number(id) || Number(product.id) || 1;
    try {
      await addToCartItem({
        id: prodIdNum,
        productId: prodIdNum,
        name: product.name,
        productName: product.name,
        image: currentRawImg || product.image,
        productImage: currentRawImg || product.image,
        price: fp,
        finalPrice: fp,
        stockQuantity: product.stockQuantity,
        vendorId: product.vendorId,
        vendorName: product.vendorName
      }, qty);
      addToast(`Added ${qty} item${qty > 1 ? 's' : ''} (${isShoe ? `Size: US ${selectedSize}` : `Option: ${selectedSize}`}) to cart! 🛍️`, 'success');
      refreshCart();
      if (redirect) navigate('/cart');
    } catch (err) {
      addToast(err.message || 'Failed to add item to cart', 'error');
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
    } catch {
      setWishlisted(prev => !prev);
      addToast(wishlisted ? 'Removed from Wishlist' : 'Added to your Wishlist ❤️', 'success');
    }
  };

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href);
      addToast('Product link copied to clipboard! 🔗', 'info');
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
  const isOutOfStock = !product.stockQuantity || product.stockQuantity <= 0;
  const isShoe = String(id) === 'puma' || String(id) === 'featured' || (product.name || '').toLowerCase().includes('shoe') || (product.name || '').toLowerCase().includes('puma') || (product.brandName || '').toLowerCase().includes('puma') || (product.categoryName || '').toLowerCase().includes('shoe');
  
  const defaultFallback = isShoe ? '/puma_xetic_sculpt.png' : getCategoryFallbackImage(product.categoryName, product.name);
  const rawImages = (Array.isArray(product.images) && product.images.length > 0)
    ? product.images
    : (product.image ? [product.image] : [defaultFallback]);
  
  const [selectedImgIdx, setSelectedImgIdx] = useState(0);
  const currentRawImg = rawImages[selectedImgIdx] || rawImages[0] || defaultFallback;
  const heroImage = resolveMediaUrl(currentRawImg) || defaultFallback;
  const brandNameDisplay = (String(id) === 'puma' || String(id) === 'featured') ? 'PUMA' : (product.brandName || 'BuyIt Flagship');

  return (
    <div className="pd-siren-page">
      {/* Desktop Breadcrumb navigation */}
      <div className="pd-desktop-breadcrumb">
        <Link to="/">Home</Link>
        <span>›</span>
        <Link to="/store">Store</Link>
        <span>›</span>
        <Link to={`/store?category=${encodeURIComponent(product.categoryName || 'Shoes')}`}>{product.categoryName || 'Shoes'}</Link>
        <span>›</span>
        <span style={{ color: '#231911', fontWeight: 700 }}>{product.name}</span>
      </div>

      {/* 2-Column Desktop Grid Layout */}
      <div className="pd-desktop-layout">
        {/* Left Column: Interactive Showcase Stage */}
        <div className="pd-showcase-stage">
          {/* Background Oversized Faint Watermark Text */}
          <div className="pd-brand-watermark-text">
            {brandNameDisplay.toUpperCase()}
          </div>

          {/* Left Vertical Size / Variant Selector */}
          <div className="pd-vertical-sizes">
            <span className="pd-sizes-label">{isShoe ? 'Size' : 'Opt'}</span>
            {(isShoe ? ['9.0', '9.5', '10.0', '10.5'] : ['Std', 'Pro', 'Max']).map((opt, idx) => {
              const fullVal = isShoe ? opt : (['Standard', 'Pro Edition', 'Deluxe Bundle'][idx] || opt);
              return (
                <button
                  type="button"
                  key={opt}
                  className={`pd-size-pill-btn ${selectedSize === fullVal || (isShoe && selectedSize === opt) ? 'active' : ''}`}
                  onClick={() => setSelectedSize(fullVal)}
                >
                  {opt}
                </button>
              );
            })}
          </div>

          {/* Floating Vertical Color Dots Capsule */}
          <div className="pd-floating-color-picker">
            <span style={{ fontSize: 9, fontWeight: 700, color: '#8c7b6c' }}>Colors</span>
            {[
              { color: '#10b981', label: 'Lime Green' },
              { color: '#ef4444', label: 'Crimson Red' },
              { color: '#3b82f6', label: 'Royal Blue' },
              { color: '#1e293b', label: 'Jet Black' },
            ].map(c => (
              <div
                key={c.color}
                className={`pd-color-dot ${selectedColor === c.color ? 'active' : ''}`}
                style={{ background: c.color }}
                onClick={() => setSelectedColor(c.color)}
                title={c.label}
              />
            ))}
          </div>

          {/* Hero Center Sneaker / Product Image */}
          <img
            src={heroImage}
            alt={product.name}
            className={`pd-hero-product-img ${isShoe ? 'is-shoe' : 'is-generic'}`}
            onError={(e) => {
              e.currentTarget.src = getCategoryFallbackImage(product.categoryName, product.name);
            }}
          />

          {/* Stage Bottom Carousel Dots */}
          {rawImages.length > 1 && (
            <div className="pd-stage-dots" style={{ display: 'flex', gap: 8, justifyContent: 'center', marginTop: 16 }}>
              {rawImages.map((img, idx) => (
                <button
                  key={idx}
                  type="button"
                  className={`dot ${selectedImgIdx === idx ? 'active' : ''}`}
                  onClick={() => setSelectedImgIdx(idx)}
                  style={{ cursor: 'pointer', border: 'none' }}
                  aria-label={`Show image ${idx + 1}`}
                />
              ))}
            </div>
          )}
        </div>

        {/* Right Column: Buy Box Container */}
        <div className="pd-buybox-container">
          {/* Brand Row & Actions */}
          <div className="pd-siren-brand-bar">
            <div className="pd-siren-brand-info">
              <div className="pd-siren-brand-avatar">
                <BrandCircleIcon brandName={brandNameDisplay} size={28} />
              </div>
              <div className="pd-siren-brand-name-group">
                <h4>
                  <span>{brandNameDisplay}</span>
                  <span className="siren-verified-tick">✓</span>
                </h4>
                <small>{product.vendorName || 'Official Flagship Store'}</small>
              </div>
            </div>

            <div className="pd-siren-brand-actions">
              <button
                type="button"
                className="pd-siren-circle-action"
                onClick={handleShare}
                title="Share this product"
              >
                🔗
              </button>
              <button
                type="button"
                className={`pd-siren-circle-action ${wishlisted ? 'active' : ''}`}
                onClick={toggleWishlist}
                title="Save to Wishlist"
              >
                {wishlisted ? '❤️' : '🤍'}
              </button>
            </div>
          </div>

          {/* Title & Category */}
          <div className="pd-siren-title-block">
            <h1>{product.name}</h1>
            <p>{product.categoryName || 'Performance & Luxury Footwear'}</p>
          </div>

          {/* Rating Badge */}
          <div className="pd-siren-rating-badge">
            <span style={{ color: '#f59e0b', fontSize: 14 }}>★</span>
            <span>{product.averageRating > 0 ? Number(product.averageRating).toFixed(1) : '4.5'}</span>
            <span style={{ color: '#8c7b6c', fontWeight: 500 }}>
              ({reviews.length > 0 ? `${reviews.length} reviews` : '32k verified reviews'})
            </span>
          </div>

          {/* Pricing Block */}
          <div className="pd-siren-pricing-block">
            <div className="pd-siren-price-display">
              <span className="currency">$</span>
              <span className="amount">{(fp || 89.14).toFixed(2)}</span>
              {product.price && <span className="old-amount">${Number(product.price).toFixed(2)}</span>}
            </div>
            {dp > 0 ? (
              <span className="pd-siren-discount-tag">Save {dp}% OFF</span>
            ) : (
              <span className="pd-siren-discount-tag" style={{ color: '#10b981', background: 'rgba(16, 185, 129, 0.12)' }}>In Stock & Ready to Ship</span>
            )}
          </div>

          {/* Horizontal Size / Variant Selection Chips */}
          <div className="pd-option-group">
            <div className="pd-option-label">
              {isShoe ? 'Select US Shoe Size:' : 'Select Edition:'} <strong>{isShoe ? `US ${selectedSize}` : selectedSize}</strong>
            </div>
            <div className="pd-horizontal-size-chips">
              {(isShoe ? ['9.0', '9.5', '10.0', '10.5'] : ['Standard', 'Pro Edition', 'Deluxe Bundle']).map(sz => (
                <button
                  key={sz}
                  type="button"
                  className={`pd-chip-btn ${selectedSize === sz ? 'active' : ''}`}
                  onClick={() => setSelectedSize(sz)}
                >
                  {isShoe ? `US ${sz}` : sz}
                </button>
              ))}
            </div>
          </div>

          {/* Stock Status Line */}
          <div className="pd-stock-status-line">
            <span>●</span>
            <span>{isOutOfStock ? 'Currently Sold Out' : `In Stock — Usually ships within 24 hours (${product.stockQuantity || 12} pairs left)`}</span>
          </div>

          {/* Quantity Stepper & Main CTA Button */}
          <div className="pd-actions-row">
            <div className="pd-siren-stepper">
              <button
                type="button"
                onClick={() => setQty(Math.max(1, qty - 1))}
                disabled={qty <= 1}
                title="Decrease quantity"
              >
                −
              </button>
              <span className="count">{qty}</span>
              <button
                type="button"
                onClick={() => setQty(qty + 1)}
                disabled={product.stockQuantity && qty >= product.stockQuantity}
                title="Increase quantity"
              >
                +
              </button>
            </div>

            <button
              type="button"
              className="pd-siren-cta-btn"
              onClick={() => addToCart(false)}
              disabled={isOutOfStock || addingCart}
            >
              <span>🛍️</span>
              <span>{addingCart ? 'Booking Order...' : `Book Order • $${((fp || 89.14) * qty).toFixed(2)}`}</span>
            </button>
          </div>

          {/* Trust Perks Bar */}
          <div className="pd-trust-perks">
            <div className="pd-perk-item">
              <span className="pd-perk-icon">⚡</span>
              <strong>Complimentary Express</strong>
              <span>2-Day Courier Delivery</span>
            </div>
            <div className="pd-perk-item">
              <span className="pd-perk-icon">🛡️</span>
              <strong>100% Authentic</strong>
              <span>Verified Direct from Brand</span>
            </div>
            <div className="pd-perk-item">
              <span className="pd-perk-icon">🔄</span>
              <strong>Hassle-Free Returns</strong>
              <span>30-Day Money Back</span>
            </div>
          </div>

          {/* Expandable Specifications & About Accordion */}
          <div className="pd-siren-accordion">
            <div
              className="pd-siren-accordion-header"
              onClick={() => setShowSpecs(prev => !prev)}
            >
              <span>Specifications & Craftsmanship</span>
              <span>{showSpecs ? '▲' : '▼'}</span>
            </div>
            {showSpecs && (
              <div className="pd-siren-accordion-content">
                <p style={{ marginBottom: 12 }}>
                  {product.description || 'Modern athletic sneaker engineered with 3D sculpted honeycomb cushioning, breathable knit mesh upper, and high-traction performance outsole for everyday luxury comfort.'}
                </p>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, fontSize: 13 }}>
                  <div><strong>Brand:</strong> {brandNameDisplay}</div>
                  <div><strong>Category:</strong> {product.categoryName || 'Footwear'}</div>
                  <div><strong>Selected Size:</strong> US {selectedSize}</div>
                  <div><strong>Upper Material:</strong> Engineered Breathable Mesh</div>
                  <div><strong>Midsole:</strong> Mechanical Xetic Cushioning</div>
                  <div><strong>Stock Status:</strong> {isOutOfStock ? 'Out of Stock' : `${product.stockQuantity || 12} In Stock`}</div>
                </div>
              </div>
            )}
          </div>

          {/* Expandable Customer Reviews Accordion */}
          <div className="pd-siren-accordion">
            <div
              className="pd-siren-accordion-header"
              onClick={() => setShowReviews(prev => !prev)}
            >
              <span>Verified Customer Reviews ({reviews.length})</span>
              <span>{showReviews ? '▲' : '▼'}</span>
            </div>
            {showReviews && (
              <div className="pd-siren-accordion-content">
                {/* Write Review */}
                <form onSubmit={submitReview} style={{ marginBottom: 16 }}>
                  <div style={{ display: 'flex', gap: 6, marginBottom: 8 }}>
                    {[1, 2, 3, 4, 5].map(s => (
                      <button
                        type="button"
                        key={s}
                        style={{
                          border: 'none',
                          background: 'none',
                          cursor: 'pointer',
                          fontSize: 18,
                          color: (reviewHover || reviewRating) >= s ? '#f59e0b' : '#d1c7bc'
                        }}
                        onMouseEnter={() => setReviewHover(s)}
                        onMouseLeave={() => setReviewHover(0)}
                        onClick={() => setReviewRating(s)}
                      >
                        ★
                      </button>
                    ))}
                  </div>
                  <input
                    type="text"
                    placeholder="Share your experience with this item..."
                    value={reviewComment}
                    onChange={e => setReviewComment(e.target.value)}
                    style={{
                      width: '100%',
                      padding: '10px 14px',
                      borderRadius: 14,
                      border: '1px solid rgba(220, 195, 170, 0.5)',
                      background: 'rgba(255, 255, 255, 0.9)',
                      fontSize: 13,
                      outline: 'none',
                      marginBottom: 8
                    }}
                  />
                  <button
                    type="submit"
                    disabled={submitting}
                    style={{
                      background: 'linear-gradient(135deg, #deb379 0%, #c48b3e 100%)',
                      color: '#fff',
                      border: 'none',
                      borderRadius: 14,
                      padding: '8px 18px',
                      fontSize: 13,
                      fontWeight: 700,
                      cursor: 'pointer'
                    }}
                  >
                    {submitting ? 'Submitting...' : 'Post Review'}
                  </button>
                </form>

                {/* Existing Reviews List */}
                {reviews.length === 0 ? (
                  <p style={{ color: '#8c7b6c', fontStyle: 'italic' }}>No reviews yet. Be the first to leave one!</p>
                ) : (
                  reviews.map(r => (
                    <div key={r.id} style={{ borderTop: '1px solid rgba(220, 195, 170, 0.3)', paddingTop: 10, marginTop: 10 }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                        <strong>{r.customerName || 'Verified Buyer'}</strong>
                        <span style={{ color: '#f59e0b' }}>{'★'.repeat(r.rating || 5)}</span>
                      </div>
                      <p style={{ margin: '4px 0 0', fontSize: 13, color: '#554433' }}>{r.comment}</p>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Similar Products Recommendation */}
      {related.length > 0 && (
        <div style={{ marginTop: 40 }}>
          <h3 style={{ fontSize: 20, fontWeight: 800, color: '#231911', marginBottom: 18 }}>
            Complete the Look & Similar Drops
          </h3>
          <div className="siren-products-grid">
            {related.slice(0, 4).map(item => {
              const itemFp = (!item.discount || item.discount <= 0)
                ? item.price
                : Math.max(0, item.price * (1 - item.discount / 100));
              return (
                <div
                  key={item.id}
                  className="siren-product-card"
                  onClick={() => { navigate('/store/product/' + item.id); window.scrollTo({ top: 0, behavior: 'smooth' }); }}
                >
                  <div className="siren-card-media" style={{ height: 180 }}>
                    <ProductImage src={item.image || '/puma_xetic_sculpt.png'} alt={item.name} />
                  </div>
                  <div className="siren-card-body">
                    <h4 className="siren-card-title">{item.name}</h4>
                    <div className="siren-card-pricing">
                      <span className="siren-card-price-final">{fmt(itemFp)}</span>
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
    try {
      const data = await getCartData();
      setCart(data);
    } catch (err) {
      addToast(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);
  useEffect(() => { loadCart(); }, [loadCart]);

  const updateQty = async (productId, quantity) => {
    try {
      await updateCartItemQty(productId, quantity);
      await loadCart();
      refreshCart();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const removeItem = async (productId) => {
    try {
      await removeCartItem(productId);
      addToast('Item removed from cart', 'success');
      await loadCart();
      refreshCart();
    } catch (err) {
      addToast(err.message, 'error');
    }
  };

  const handleClearCart = async () => {
    if (!window.confirm('Are you sure you want to remove all items from your cart?')) return;
    setClearing(true);
    try {
      await clearCartAll();
      addToast('Cart cleared', 'success');
      await loadCart();
      refreshCart();
    } catch (err) {
      addToast(err.message, 'error');
    } finally {
      setClearing(false);
    }
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
            <button
              className="btn-primary btn-lg btn-full"
              style={{ marginTop: 20 }}
              onClick={() => {
                if (!getToken()) {
                  addToast('Please sign in or create an account to proceed to checkout 🔒', 'info');
                  navigate('/login');
                } else {
                  navigate('/checkout');
                }
              }}
            >
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
      getCartData().then(d => setCart(d)).catch(() => {}),
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

const CATEGORY_ANGLE_SUGGESTIONS = {
  'electronics': [
    'https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80'
  ],
  'mobiles': [
    'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=800&auto=format&fit=crop&q=80'
  ],
  'laptops': [
    'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=800&auto=format&fit=crop&q=80'
  ],
  'headphones': [
    'https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1484704849700-f032a568e944?w=800&auto=format&fit=crop&q=80'
  ],
  'clothing': [
    'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1581655353564-df123a1eb820?w=800&auto=format&fit=crop&q=80'
  ],
  'shoes': [
    'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=800&auto=format&fit=crop&q=80'
  ],
  'watches': [
    'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=800&auto=format&fit=crop&q=80'
  ],
  'bags': [
    'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1622560480605-d83c853bc5c3?w=800&auto=format&fit=crop&q=80'
  ],
  'home-kitchen': [
    'https://images.unsplash.com/photo-1584990347449-a2a51f33f679?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=800&auto=format&fit=crop&q=80'
  ],
  'beauty': [
    'https://images.unsplash.com/photo-1620916566398-39f1143ab7be?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1556228720-195a672e8a03?w=800&auto=format&fit=crop&q=80'
  ],
  'books': [
    'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800&auto=format&fit=crop&q=80'
  ],
  'toys': [
    'https://images.unsplash.com/photo-1566576912321-d58ddd7a6088?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1596461404969-9ae70f2830c1?w=800&auto=format&fit=crop&q=80'
  ],
  'sports': [
    'https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=800&auto=format&fit=crop&q=80'
  ],
  'grocery': [
    'https://images.unsplash.com/photo-1540420773420-3366772f4999?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1610832958506-aa56368176cf?w=800&auto=format&fit=crop&q=80'
  ],
  'appliances': [
    'https://images.unsplash.com/photo-1590794056226-79ef3a8147e1?w=800&auto=format&fit=crop&q=80',
    'https://images.unsplash.com/photo-1556911220-e15b29be8c8f?w=800&auto=format&fit=crop&q=80'
  ]
};

function ProductModal({ product, categories, brands, saving, onSave, onClose }) {
  const [form, setForm] = useState({
    name: product?.name || '',
    description: product?.description || '',
    categoryId: product?.categoryId || '',
    brandId: product?.brandId || '',
    price: product?.price || '',
    discount: product?.discount || 0,
    stockQuantity: product?.stockQuantity || '',
    sku: product?.sku || '',
    image: product?.image || ''
  });

  const [imagesList, setImagesList] = useState(() => {
    if (Array.isArray(product?.images) && product.images.length > 0) {
      return product.images;
    }
    return product?.image ? [product.image] : [''];
  });

  const [loadingDetails, setLoadingDetails] = useState(false);

  // If editing an existing product, fetch full details to ensure all 3 gallery images are loaded
  useEffect(() => {
    if (product?.id) {
      setLoadingDetails(true);
      api('/products/' + product.id)
        .then(res => {
          const p = res?.product || res;
          if (p && Array.isArray(p.images) && p.images.length > 0) {
            setImagesList(p.images);
            if (p.image) setForm(f => ({ ...f, image: p.image }));
          }
        })
        .catch(() => {})
        .finally(() => setLoadingDetails(false));
    }
  }, [product?.id]);

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const updateImageUrl = (index, url) => {
    setImagesList(prev => {
      const next = [...prev];
      next[index] = url;
      return next;
    });
  };

  const addImageSlot = () => {
    setImagesList(prev => [...prev, '']);
  };

  const removeImageSlot = (index) => {
    setImagesList(prev => {
      if (prev.length <= 1) return [''];
      return prev.filter((_, i) => i !== index);
    });
  };

  const setAsPrimary = (index) => {
    if (index === 0) return;
    setImagesList(prev => {
      const item = prev[index];
      const remaining = prev.filter((_, i) => i !== index);
      return [item, ...remaining];
    });
  };

  const autoFillAngles = () => {
    const selCat = categories.find(c => String(c.id) === String(form.categoryId));
    const catKey = selCat ? selCat.name.toLowerCase().replace(/ & /g, '-').replace(/ /g, '-') : '';
    const suggested = CATEGORY_ANGLE_SUGGESTIONS[catKey] || [
      'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop&q=80',
      'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&auto=format&fit=crop&q=80'
    ];

    setImagesList(prev => {
      const valid = prev.filter(u => u && u.trim().length > 0);
      const current = valid.length > 0 ? valid[0] : (form.image || '');
      const base = current ? [current] : [];
      for (const s of suggested) {
        if (!base.includes(s) && base.length < 3) {
          base.push(s);
        }
      }
      return base.length > 0 ? base : [''];
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const validImages = imagesList.map(u => u.trim()).filter(u => u.length > 0);
    const primary = validImages.length > 0 ? validImages[0] : (form.image || '');
    onSave({
      ...form,
      image: primary,
      images: validImages,
      price: Number(form.price),
      discount: Number(form.discount),
      stockQuantity: Number(form.stockQuantity),
      categoryId: Number(form.categoryId),
      brandId: Number(form.brandId)
    });
  };

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

        {/* Multi-Image Product Gallery Manager */}
        <div className="product-images-manager">
          <div className="pim-header">
            <div>
              <label className="pim-title">Product Image Gallery ({imagesList.filter(u => u && u.trim()).length} Images)</label>
              <span className="pim-subtitle">First photo serves as the storefront cover. Add alternate angles & lifestyle views.</span>
            </div>
            <div className="pim-actions">
              {form.categoryId && (
                <button
                  type="button"
                  className="btn-autofill-angles"
                  onClick={autoFillAngles}
                  title="Autofill alternate angles based on selected category"
                >
                  ✨ Complete 3 Gallery Angles
                </button>
              )}
              <button
                type="button"
                className="btn-add-slot"
                onClick={addImageSlot}
                title="Add another photo URL slot"
              >
                + Add Angle
              </button>
            </div>
          </div>

          {loadingDetails && <div className="pim-loading">Loading full gallery images...</div>}

          {/* Visual Thumbnail Preview Strip */}
          {imagesList.some(u => u && u.trim()) && (
            <div className="pim-thumbnails-row">
              {imagesList.map((url, idx) => {
                if (!url || !url.trim()) return null;
                return (
                  <div key={idx} className={`pim-thumb-card ${idx === 0 ? 'is-primary' : ''}`}>
                    <img
                      src={url}
                      alt={`View ${idx + 1}`}
                      onError={(e) => { e.target.onerror = null; e.target.src = 'https://placehold.co/100x100?text=Invalid+Image'; }}
                    />
                    <span className="pim-thumb-badge">
                      {idx === 0 ? '★ Primary' : `Angle ${idx + 1}`}
                    </span>
                  </div>
                );
              })}
            </div>
          )}

          {/* Image URL Inputs */}
          <div className="pim-inputs-list">
            {imagesList.map((url, idx) => (
              <div key={idx} className="pim-input-row">
                <div className="pim-slot-label">
                  <span className={`pim-slot-num ${idx === 0 ? 'primary' : ''}`}>
                    {idx === 0 ? '★ Primary' : `Angle ${idx + 1}`}
                  </span>
                </div>
                <div className="pim-input-wrapper">
                  <input
                    type="text"
                    value={url}
                    onChange={(e) => updateImageUrl(idx, e.target.value)}
                    placeholder={idx === 0 ? "Primary image URL (e.g. /product-images/... or https://...)" : `Angle ${idx + 1} URL (detail, side view, lifestyle)`}
                    required={idx === 0}
                  />
                </div>
                <div className="pim-row-actions">
                  {idx !== 0 && url && url.trim() && (
                    <button
                      type="button"
                      className="btn-set-primary"
                      onClick={() => setAsPrimary(idx)}
                      title="Make this the Primary Storefront Image"
                    >
                      ★ Make Primary
                    </button>
                  )}
                  {imagesList.length > 1 && (
                    <button
                      type="button"
                      className="btn-remove-slot"
                      onClick={() => removeImageSlot(idx)}
                      title="Remove this image slot"
                    >
                      ✕
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="modal-actions">
          <button type="button" className="btn-cancel" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save Product'}</button>
        </div>
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
  const handleAuth = useCallback(async (token, u) => {
    setAuth(token, u);
    setUser(u);
    // Automatically merge any guest cart items into user's cloud cart
    try {
      const gCart = getGuestCart();
      if (gCart.items && gCart.items.length > 0) {
        for (const it of gCart.items) {
          await api('/cart', { method: 'POST', body: { productId: it.productId, quantity: it.quantity } });
        }
        clearGuestCart();
      }
    } catch {}
  }, []);
  const handleLogout = useCallback(async () => {
    try { await api('/auth/logout', { method: 'POST' }); } catch { }
    clearAuth();
    setUser(null);
  }, []);
  const home = user ? (user.role === 'ADMIN' ? '/admin' : user.role === 'VENDOR' ? '/vendor' : '/store') : '/store';

  return (
    <BrowserRouter>
      <Toast toasts={toasts} onRemove={removeToast} />
      <Routes>
        <Route path="/login"    element={<LoginPage    addToast={addToast} onAuth={handleAuth} user={user} onLogout={handleLogout} />} />
        <Route path="/register" element={<RegisterPage addToast={addToast} onAuth={handleAuth} user={user} />} />

        <Route element={<CustomerLayout user={user} onLogout={handleLogout} />}>
          <Route path="/"                  element={<Navigate to="/store" replace />} />
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

