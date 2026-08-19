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

function ProductImage({ src, alt, style }) {
  const [err, setErr] = useState(false);
  if (src && !err) return <img src={src} alt={alt || ''} style={style} onError={() => setErr(true)} />;
  return <div className="img-placeholder" style={style}>\uD83D\uDCE6</div>;
}

const CartCtx = createContext({ cartCount: 0, refreshCart: () => {} });
function useCart() { return useContext(CartCtx); }

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
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await api('/auth/login', { method: 'POST', body: { email: email.trim(), password: password.trim() } });
      if (!res.success) { addToast(res.message || 'Login failed', 'error'); return; }
      onAuth(res.token, res.user);
      addToast('Login successful!', 'success');
      navigate(res.user.role === 'ADMIN' ? '/admin' : res.user.role === 'VENDOR' ? '/vendor' : '/store', { replace: true });
    } catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  };
  const fill = (e, em, pw) => { e.preventDefault(); setEmail(em); setPassword(pw); };
  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">BuyIt</div>
        <p className="auth-sub">Sign in to your account</p>
        <form onSubmit={handleSubmit}>
          <div className="field"><input type="email" placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} required /></div>
          <div className="field"><input type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} required /></div>
          <button type="submit" className="btn-full" disabled={loading}>{loading ? 'Signing in...' : 'Sign In'}</button>
        </form>
        <div className="demo-creds">
          <p><strong>Demo Credentials:</strong></p>
          <a href="#" onClick={e => fill(e, 'admin@buyit.com', 'Admin@123')}>Admin: admin@buyit.com / Admin@123</a>
          <a href="#" onClick={e => fill(e, 'vendor1@buyit.com', 'Vendor@123')}>Vendor: vendor1@buyit.com / Vendor@123</a>
          <a href="#" onClick={e => fill(e, 'customer@buyit.com', 'Customer@123')}>Customer: customer@buyit.com / Customer@123</a>
        </div>
        <p className="auth-link">Don't have an account? <Link to="/register">Register</Link></p>
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
    e.preventDefault(); setLoading(true);
    try {
      const body = { name: name.trim(), email: email.trim(), phone: phone.trim(), password: password.trim(), role };
      if (role === 'VENDOR') { body.businessName = businessName.trim(); body.description = description.trim(); body.city = city.trim(); body.state = state.trim(); body.pincode = pincode.trim(); }
      const res = await api('/auth/register', { method: 'POST', body });
      if (!res.success) { addToast(res.message || 'Registration failed', 'error'); return; }
      onAuth(res.token, res.user);
      addToast('Registration successful!', 'success');
      navigate(role === 'VENDOR' ? '/vendor' : '/store', { replace: true });
    } catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  };
  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">BuyIt</div>
        <p className="auth-sub">Create your account</p>
        <div className="field-row" style={{ marginBottom: 16 }}>
          <button type="button" className={role === 'CUSTOMER' ? 'btn-full' : 'btn-full btn-secondary'} onClick={() => setRole('CUSTOMER')}>Customer</button>
          <button type="button" className={role === 'VENDOR' ? 'btn-full' : 'btn-full btn-secondary'} onClick={() => setRole('VENDOR')}>Vendor</button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="field"><input type="text" placeholder="Full Name *" value={name} onChange={e => setName(e.target.value)} required /></div>
          <div className="field"><input type="email" placeholder="Email *" value={email} onChange={e => setEmail(e.target.value)} required /></div>
          <div className="field"><input type="tel" placeholder="Phone" value={phone} onChange={e => setPhone(e.target.value)} /></div>
          <div className="field"><input type="password" placeholder="Password *" value={password} onChange={e => setPassword(e.target.value)} required /></div>
          {role === 'VENDOR' && (<>
            <div className="field"><input type="text" placeholder="Business Name *" value={businessName} onChange={e => setBusinessName(e.target.value)} required /></div>
            <div className="field"><input type="text" placeholder="Description" value={description} onChange={e => setDescription(e.target.value)} /></div>
            <div className="field-row">
              <input type="text" placeholder="City *" value={city} onChange={e => setCity(e.target.value)} required style={{ flex: 1 }} />
              <input type="text" placeholder="State *" value={state} onChange={e => setState(e.target.value)} required style={{ flex: 1 }} />
            </div>
            <div className="field"><input type="text" placeholder="Pincode *" value={pincode} onChange={e => setPincode(e.target.value)} required /></div>
          </>)}
          <button type="submit" className="btn-full" disabled={loading}>{loading ? 'Creating Account...' : 'Create Account'}</button>
        </form>
        <p className="auth-link">Already have an account? <Link to="/login">Sign In</Link></p>
      </div>
    </div>
  );
}

function CustomerLayout({ user, onLogout }) {
  const [cartCount, setCartCount] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const navigate = useNavigate();
  const location = useLocation();
  const refreshCart = useCallback(async () => {
    try { const d = await api('/cart'); setCartCount(d.count || (d.items || []).length || 0); } catch { }
  }, []);
  useEffect(() => { refreshCart(); }, [refreshCart, location.pathname]);
  const handleSearch = (e) => { e.preventDefault(); navigate('/store?q=' + encodeURIComponent(searchQuery)); };
  return (
    <CartCtx.Provider value={{ cartCount, refreshCart }}>
      <div className="app-layout">
        <header className="top-bar">
          <div className="top-bar-left">
            <Link to="/store" className="brand">BuyIt</Link>
            <form onSubmit={handleSearch} style={{ display: 'flex' }}>
              <input className="search-box" type="text" placeholder="Search products..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} />
            </form>
            <Link to="/store" className={'nav-pill' + (location.pathname === '/store' ? ' active' : '')}>Store</Link>
            <Link to="/orders" className={'nav-pill' + (location.pathname === '/orders' ? ' active' : '')}>Orders</Link>
          </div>
          <div className="top-bar-right">
            <Link to="/cart" className={'nav-pill' + (location.pathname === '/cart' ? ' active' : '')}>
              \uD83D\uDED2 Cart{cartCount > 0 && <span className="badge">{cartCount}</span>}
            </Link>
            <span className="user-pill">{user?.name}</span>
            <button className="btn-logout" onClick={onLogout}>Logout</button>
          </div>
        </header>
        <main className="main-area"><Outlet /></main>
      </div>
    </CartCtx.Provider>
  );
}

function StorePage({ addToast }) {
  const { refreshCart } = useCart();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [brands, setBrands] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [brand, setBrand] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [sort, setSort] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    Promise.all([
      api('/categories').then(d => setCategories(Array.isArray(d) ? d : [])).catch(() => {}),
      api('/brands').then(d => setBrands(Array.isArray(d) ? d : [])).catch(() => {}),
    ]);
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
  }, [category, brand, minPrice, maxPrice, sort, location.search, addToast]);

  useEffect(() => { loadProducts(); }, [loadProducts]);

  const addToCart = async (e, productId) => {
    e.stopPropagation();
    try { await api('/cart', { method: 'POST', body: { productId, quantity: 1 } }); addToast('Added to cart', 'success'); refreshCart(); }
    catch (err) { addToast(err.message, 'error'); }
  };

  const discountPct = (p) => (!p.discount || p.discount <= 0) ? 0 : Math.round(p.discount);
  const finalPrice = (p) => (!p.discount || p.discount <= 0) ? p.price : Math.max(0, p.price * (1 - p.discount / 100));

  return (
    <div className="store">
      <div className="store-filters">
        <input className="filter-search" type="text" placeholder="Search products..." value={search} onChange={e => setSearch(e.target.value)} onKeyDown={e => e.key === 'Enter' && loadProducts()} />
        <select value={category} onChange={e => setCategory(e.target.value)}><option value="">All Categories</option>{categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select>
        <select value={brand} onChange={e => setBrand(e.target.value)}><option value="">All Brands</option>{brands.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}</select>
        <input type="number" placeholder="Min Price" value={minPrice} onChange={e => setMinPrice(e.target.value)} style={{ width: 100 }} />
        <input type="number" placeholder="Max Price" value={maxPrice} onChange={e => setMaxPrice(e.target.value)} style={{ width: 100 }} />
        <select value={sort} onChange={e => setSort(e.target.value)}>
          <option value="">Sort By</option><option value="price_asc">Price: Low to High</option><option value="price_desc">Price: High to Low</option>
          <option value="name_asc">Name: A-Z</option><option value="name_desc">Name: Z-A</option><option value="rating">Rating</option>
        </select>
        <button className="btn-primary" onClick={loadProducts}>Search</button>
      </div>
      {loading ? <Loader /> : products.length === 0 ? <EmptyState message="No products found" /> : (
        <div className="product-grid">
          {products.map(p => (
            <div key={p.id} className="product-card" onClick={() => navigate('/store/product/' + p.id)}>
              <div className="product-img">
                <ProductImage src={p.image} alt={p.name} />
                {discountPct(p) > 0 && <span className="discount-badge">-{discountPct(p)}%</span>}
              </div>
              <div className="product-info">
                <span className="p-category">{p.categoryName}</span>
                <h3>{p.name}</h3>
                <span className="p-vendor">by {p.vendorName}</span>
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
      )}
    </div>
  );
}

function ProductDetailsPage({ addToast }) {
  const { refreshCart } = useCart();
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [qty, setQty] = useState(1);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [pRes, rRes] = await Promise.all([api('/products/' + id), api('/reviews/' + id).catch(() => [])]);
      setProduct(pRes.product || pRes); setReviews(Array.isArray(rRes) ? rRes : []);
    } catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [id, addToast]);
  useEffect(() => { load(); }, [load]);
  const addToCart = async () => {
    try { await api('/cart', { method: 'POST', body: { productId: Number(id), quantity: qty } }); addToast('Added to cart', 'success'); refreshCart(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  const submitReview = async (e) => {
    e.preventDefault(); if (!reviewComment.trim()) { addToast('Please enter a comment', 'error'); return; }
    setSubmitting(true);
    try { await api('/reviews', { method: 'POST', body: { productId: Number(id), rating: reviewRating, comment: reviewComment.trim() } }); addToast('Review submitted', 'success'); setReviewComment(''); setReviewRating(5); load(); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setSubmitting(false); }
  };
  if (loading) return <Loader />;
  if (!product) return <EmptyState message="Product not found" />;
  const fp = (!product.discount || product.discount <= 0) ? product.price : Math.max(0, product.price * (1 - product.discount / 100));
  const dp = (!product.discount || product.discount <= 0) ? 0 : Math.round(product.discount);
  return (
    <div className="product-details">
      <button className="btn-back" onClick={() => navigate(-1)}>\u2190 Back</button>
      <div className="pd-layout">
        <div className="pd-image"><ProductImage src={product.image} alt={product.name} style={{ width: '100%', maxHeight: 400, objectFit: 'contain' }} /></div>
        <div className="pd-info">
          <span className="pd-cat">{product.categoryName}</span>
          <h1>{product.name}</h1>
          <span className="pd-vendor">Sold by {product.vendorName}</span>
          {product.averageRating > 0 && <div className="pd-rating"><StarRating rating={product.averageRating} /> <span>({product.reviewCount} reviews)</span></div>}
          <div className="pd-price">
            <span className="big-price">{fmt(fp)}</span>
            {dp > 0 && <span className="old-price">{fmt(product.price)}</span>}
            {dp > 0 && <span className="off-tag">{dp}% off</span>}
          </div>
          <p className="pd-stock">{product.stockQuantity > 0 ? 'In Stock (' + product.stockQuantity + ' available)' : 'Out of Stock'}</p>
          {product.sku && <p className="note">SKU: {product.sku}</p>}
          {product.description && <p className="pd-desc">{product.description}</p>}
          <div className="pd-actions">
            <div className="qty-ctrl">
              <button onClick={() => setQty(Math.max(1, qty - 1))}>-</button>
              <span>{qty}</span>
              <button onClick={() => setQty(Math.min(product.stockQuantity, qty + 1))}>+</button>
            </div>
            <button className="btn-primary btn-lg" onClick={addToCart} disabled={product.stockQuantity <= 0}>Add to Cart</button>
          </div>
        </div>
      </div>
      <div className="reviews-section">
        <h2>Reviews</h2>
        <form className="review-form" onSubmit={submitReview}>
          <div className="field-row">
            <select value={reviewRating} onChange={e => setReviewRating(Number(e.target.value))}>
              {[5,4,3,2,1].map(r => <option key={r} value={r}>{r} Star{r > 1 ? 's' : ''}</option>)}
            </select>
          </div>
          <div className="field"><textarea placeholder="Write your review..." value={reviewComment} onChange={e => setReviewComment(e.target.value)} rows={3} /></div>
          <button type="submit" className="btn-primary" disabled={submitting}>{submitting ? 'Submitting...' : 'Submit Review'}</button>
        </form>
        <div className="reviews-list">
          {reviews.length === 0 ? <p className="note">No reviews yet</p> : reviews.map(r => (
            <div key={r.id} className="review-card">
              <div className="review-header"><strong>{r.customerName}</strong><StarRating rating={r.rating} /><span className="note">{r.createdAt ? new Date(r.createdAt).toLocaleDateString() : ''}</span></div>
              <p>{r.comment}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function CartPage({ addToast }) {
  const { refreshCart } = useCart();
  const [cart, setCart] = useState({ items: [], total: 0, count: 0 });
  const [loading, setLoading] = useState(true);
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
    try { await api('/cart', { method: 'DELETE', body: { productId } }); addToast('Removed', 'success'); loadCart(); refreshCart(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  if (loading) return <Loader />;
  const items = cart.items || [];
  return (
    <div className="cart-page">
      <h1>Shopping Cart</h1>
      {items.length === 0 ? <EmptyState message="Your cart is empty" /> : (
        <div style={{ display: 'flex', gap: 32, alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <div className="cart-items" style={{ flex: 2, minWidth: 300 }}>
            {items.map(item => (
              <div key={item.id} className="cart-item">
                <div className="ci-img"><ProductImage src={item.productImage} alt={item.productName} style={{ width: 80, height: 80 }} /></div>
                <div className="ci-info"><h3>{item.productName}</h3><span className="ci-vendor">by {item.vendorName}</span></div>
                <span className="ci-price">{fmt(item.price)}</span>
                <div className="ci-qty">
                  <button onClick={() => updateQty(item.productId, Math.max(1, item.quantity - 1))}>-</button>
                  <span>{item.quantity}</span>
                  <button onClick={() => updateQty(item.productId, item.quantity + 1)}>+</button>
                </div>
                <span className="ci-subtotal">{fmt(item.subtotal)}</span>
                <button className="btn-remove" onClick={() => removeItem(item.productId)}>\u00D7</button>
              </div>
            ))}
          </div>
          <div className="cart-summary" style={{ flex: 1, minWidth: 250 }}>
            <h2>Order Summary</h2>
            <div className="summary-row"><span>Items ({cart.count || items.length})</span><span>{fmt(cart.total)}</span></div>
            <div className="summary-row"><span>Shipping</span><span>Free</span></div>
            <div className="total"><span>Total</span><span>{fmt(cart.total)}</span></div>
            <button className="btn-primary btn-lg" style={{ width: '100%', marginTop: 16 }} onClick={() => navigate('/checkout')}>Proceed to Checkout</button>
            <button className="btn-secondary" style={{ width: '100%', marginTop: 8 }} onClick={() => navigate('/store')}>Continue Shopping</button>
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
    try { await api('/orders', { method: 'POST', body: { items, addressId: selectedAddress, paymentMethod, discountAmount } }); addToast('Order placed!', 'success'); refreshCart(); navigate('/orders'); }
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
              <input placeholder="Full Name" value={addrForm.fullName} onChange={e => setAddrForm({ ...addrForm, fullName: e.target.value })} required />
              <input placeholder="Phone" value={addrForm.phone} onChange={e => setAddrForm({ ...addrForm, phone: e.target.value })} />
              <input placeholder="Address Line" value={addrForm.addressLine} onChange={e => setAddrForm({ ...addrForm, addressLine: e.target.value })} required />
              <div className="field-row">
                <input placeholder="City" value={addrForm.city} onChange={e => setAddrForm({ ...addrForm, city: e.target.value })} required style={{ flex: 1 }} />
                <input placeholder="State" value={addrForm.state} onChange={e => setAddrForm({ ...addrForm, state: e.target.value })} style={{ flex: 1 }} />
              </div>
              <input placeholder="Pincode" value={addrForm.pincode} onChange={e => setAddrForm({ ...addrForm, pincode: e.target.value })} />
              <div className="field-row">
                <button type="submit" className="btn-primary">Save Address</button>
                <button type="button" className="btn-secondary" onClick={() => setShowAddressForm(false)}>Cancel</button>
              </div>
            </form>
          ) : <button className="btn-secondary" onClick={() => setShowAddressForm(true)}>+ Add New Address</button>}
          <h2 style={{ marginTop: 24 }}>Payment Method</h2>
          {['COD', 'UPI', 'CARD', 'NETBANKING'].map(m => (
            <label key={m} style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 0' }}>
              <input type="radio" name="payment" value={m} checked={paymentMethod === m} onChange={() => setPaymentMethod(m)} />
              {m === 'COD' ? 'Cash on Delivery' : m === 'UPI' ? 'UPI' : m === 'CARD' ? 'Credit/Debit Card' : 'Net Banking'}
            </label>
          ))}
        </div>
        <div className="checkout-right">
          <h2>Order Summary</h2>
          {(cart.items || []).map(i => <div key={i.id} className="summary-row"><span>{i.productName} x{i.quantity}</span><span>{fmt(i.subtotal)}</span></div>)}
          <div className="coupon-row">
            <input type="text" placeholder="Coupon code" value={couponCode} onChange={e => setCouponCode(e.target.value)} />
            <button className="btn-secondary" onClick={applyCoupon}>Apply</button>
          </div>
          <div className="summary-row"><span>Subtotal</span><span>{fmt(cart.total)}</span></div>
          {discountAmount > 0 && <div className="summary-row discount"><span>Discount</span><span>-{fmt(discountAmount)}</span></div>}
          <div className="summary-row"><span>Shipping</span><span>Free</span></div>
          <div className="total"><span>Total</span><span>{fmt(finalTotal)}</span></div>
          <div className="checkout-actions">
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

function VendorLayout({ user, onLogout }) {
  const location = useLocation();
  const [vendorInfo, setVendorInfo] = useState(null);
  useEffect(() => { api('/vendors/me').then(d => setVendorInfo(d.vendor || d)).catch(() => {}); }, []);
  const nav = [
    { path: '/vendor', label: 'Dashboard' },
    { path: '/vendor/products', label: 'Products' },
    { path: '/vendor/orders', label: 'Orders' },
    { path: '/vendor/profile', label: 'Profile' },
  ];
  return (
    <div className="dashboard">
      <aside className="dash-sidebar">
        <div className="dash-sidebar-header"><Link to="/vendor" className="brand" style={{ color: '#fff', textDecoration: 'none' }}>BuyIt</Link></div>
        <div className="dash-welcome"><p>Welcome,</p><strong>{vendorInfo?.businessName || user?.name}</strong></div>
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
    Promise.all([
      api('/products').then(d => setProducts(Array.isArray(d) ? d : [])).catch(() => {}),
      api('/orders').then(d => setOrders(Array.isArray(d) ? d : [])).catch(() => {}),
    ]).finally(() => setLoading(false));
  }, []);
  if (loading) return <Loader />;
  const totalRevenue = orders.reduce((s, o) => s + Number(o.finalAmount || 0), 0);
  const pendingOrders = orders.filter(o => o.orderStatus === 'PLACED' || o.orderStatus === 'CONFIRMED').length;
  return (
    <div>
      <h1>Dashboard</h1>
      <div className="dash-cards">
        <div className="dash-card"><span className="stat-icon">\uD83D\uDCE6</span><h3>Products</h3><p>{products.length}</p></div>
        <div className="dash-card"><span className="stat-icon">\uD83D\uDCB3</span><h3>Total Orders</h3><p>{orders.length}</p></div>
        <div className="dash-card"><span className="stat-icon">\uD83D\uDCB0</span><h3>Revenue</h3><p>{fmt(totalRevenue)}</p></div>
        <div className="dash-card"><span className="stat-icon">\u23F3</span><h3>Pending</h3><p>{pendingOrders}</p></div>
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
  const load = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([
        api('/products').then(d => setProducts(Array.isArray(d) ? d : [])).catch(() => setProducts([])),
        api('/categories').then(d => setCategories(Array.isArray(d) ? d : [])).catch(() => setCategories([])),
        api('/brands').then(d => setBrands(Array.isArray(d) ? d : [])).catch(() => setBrands([])),
      ]);
    } finally { setLoading(false); }
  }, []);
  useEffect(() => { load(); }, [load]);
  const deleteProduct = async (id) => {
    if (!window.confirm('Delete this product?')) return;
    try { await api('/products/' + id, { method: 'DELETE' }); addToast('Deleted', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  const saveProduct = async (form) => {
    setSaving(true);
    try {
      if (modal?.editing) await api('/products/' + modal.editing.id, { method: 'PUT', body: form });
      else await api('/products', { method: 'POST', body: { ...form, vendorId: user?.vendorId || user?.id } });
      addToast('Product saved', 'success'); setModal(null); load();
    } catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };
  if (loading) return <Loader />;
  return (
    <div>
      <div className="section-header"><h1>Products</h1><button className="btn-add" onClick={() => setModal({ editing: null })}>+ Add Product</button></div>
      {products.length === 0 ? <EmptyState message="No products yet" /> : (
        <table className="data-table">
          <thead><tr><th>ID</th><th>Name</th><th>Category</th><th>Brand</th><th>Price</th><th>Stock</th><th>Actions</th></tr></thead>
          <tbody>{products.map(p => (
            <tr key={p.id}><td>{p.id}</td><td>{p.name}</td><td>{p.categoryName}</td><td>{p.brandName}</td><td>{fmt(p.price)}</td><td>{p.stockQuantity}</td>
              <td><button className="btn-primary" style={{ marginRight: 4 }} onClick={() => setModal({ editing: p })}>Edit</button><button className="btn-cancel" onClick={() => deleteProduct(p.id)}>Delete</button></td>
            </tr>
          ))}</tbody>
        </table>
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

function VendorOrders({ addToast }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/orders'); setOrders(Array.isArray(d) ? d : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);
  useEffect(() => { load(); }, [load]);
  const updateStatus = async (orderId, status) => {
    try { await api('/orders/' + orderId + '/status', { method: 'PUT', body: { status } }); addToast('Status updated', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  if (loading) return <Loader />;
  return (
    <div>
      <h1>Orders</h1>
      {orders.length === 0 ? <EmptyState message="No orders" /> : (
        <table className="data-table">
          <thead><tr><th>Order</th><th>Customer</th><th>Items</th><th>Amount</th><th>Status</th><th>Actions</th></tr></thead>
          <tbody>{orders.map(o => (
            <tr key={o.id}><td>#{o.id}</td><td>{o.customerName}</td><td>{(o.items || []).map(i => i.productName).join(', ')}</td><td>{fmt(o.finalAmount)}</td><td><span className={statusClass(o.orderStatus)}>{o.orderStatus}</span></td>
              <td><select value={o.orderStatus} onChange={e => updateStatus(o.id, e.target.value)} style={{ padding: '4px 8px' }}>{['PLACED','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED'].map(s => <option key={s} value={s}>{s}</option>)}</select></td>
            </tr>
          ))}</tbody>
        </table>
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
    { path: '/admin', label: 'Dashboard' },
    { path: '/admin/customers', label: 'Customers' },
    { path: '/admin/vendors', label: 'Vendors' },
    { path: '/admin/products', label: 'Products' },
    { path: '/admin/categories', label: 'Categories' },
    { path: '/admin/brands', label: 'Brands' },
    { path: '/admin/orders', label: 'Orders' },
  ];
  return (
    <div className="dashboard">
      <aside className="dash-sidebar">
        <div className="dash-sidebar-header"><Link to="/admin" className="brand" style={{ color: '#fff', textDecoration: 'none' }}>BuyIt Admin</Link></div>
        <div className="dash-welcome"><p>Welcome,</p><strong>{user?.name}</strong></div>
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
        <div className="dash-card"><span className="stat-icon">\uD83D\uDC64</span><h3>Customers</h3><p>{stats.totalCustomers || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">\uD83C\uDFED</span><h3>Vendors</h3><p>{stats.approvedVendors || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">\u23F3</span><h3>Pending Vendors</h3><p>{stats.pendingVendors || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">\uD83D\uDCE6</span><h3>Products</h3><p>{stats.totalProducts || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">\uD83D\uDCB3</span><h3>Orders</h3><p>{stats.totalOrders || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">\uD83D\uDCB0</span><h3>Revenue</h3><p>{fmt(stats.totalRevenue || 0)}</p></div>
        <div className="dash-card"><span className="stat-icon">\u2705</span><h3>Delivered</h3><p>{stats.deliveredOrders || 0}</p></div>
        <div className="dash-card"><span className="stat-icon">\u274C</span><h3>Cancelled</h3><p>{stats.cancelledOrders || 0}</p></div>
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
    if (!window.confirm('Delete?')) return;
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
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/products'); setProducts(Array.isArray(d) ? d : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);
  useEffect(() => { load(); }, [load]);
  const deleteProduct = async (id) => {
    if (!window.confirm('Delete?')) return;
    try { await api('/products/' + id, { method: 'DELETE' }); addToast('Deleted', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  if (loading) return <Loader />;
  return (
    <div>
      <div className="section-header"><h1>Products</h1></div>
      {products.length === 0 ? <EmptyState message="No products" /> : (
        <table className="data-table">
          <thead><tr><th>ID</th><th>Name</th><th>Vendor</th><th>Category</th><th>Price</th><th>Stock</th><th>Actions</th></tr></thead>
          <tbody>{products.map(p => (
            <tr key={p.id}><td>{p.id}</td><td>{p.name}</td><td>{p.vendorName}</td><td>{p.categoryName}</td><td>{fmt(p.price)}</td><td>{p.stockQuantity}</td><td><button className="btn-cancel" onClick={() => deleteProduct(p.id)}>Delete</button></td></tr>
          ))}</tbody>
        </table>
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
    try { await api('/categories', { method: 'POST', body: { name: name.trim(), description: desc.trim() } }); addToast('Added', 'success'); setName(''); setDesc(''); setShowForm(false); load(); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };
  const deleteCategory = async (id) => {
    if (!window.confirm('Delete?')) return;
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
          <tbody>{categories.map(c => <tr key={c.id}><td>{c.id}</td><td>{c.name}</td><td>{c.description || '-'}</td><td><button className="btn-cancel" onClick={() => deleteCategory(c.id)}>Delete</button></td></tr>)}</tbody>
        </table>
      )}
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
    try { await api('/brands', { method: 'POST', body: { name: name.trim(), description: desc.trim() } }); addToast('Added', 'success'); setName(''); setDesc(''); setShowForm(false); load(); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setSaving(false); }
  };
  const deleteBrand = async (id) => {
    if (!window.confirm('Delete?')) return;
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
          <tbody>{brands.map(b => <tr key={b.id}><td>{b.id}</td><td>{b.name}</td><td>{b.description || '-'}</td><td><button className="btn-cancel" onClick={() => deleteBrand(b.id)}>Delete</button></td></tr>)}</tbody>
        </table>
      )}
    </div>
  );
}

function AdminOrders({ addToast }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const load = useCallback(async () => {
    setLoading(true);
    try { const d = await api('/orders'); setOrders(Array.isArray(d) ? d : []); }
    catch (err) { addToast(err.message, 'error'); }
    finally { setLoading(false); }
  }, [addToast]);
  useEffect(() => { load(); }, [load]);
  const updateStatus = async (orderId, status) => {
    try { await api('/orders/' + orderId + '/status', { method: 'PUT', body: { status } }); addToast('Updated', 'success'); load(); }
    catch (err) { addToast(err.message, 'error'); }
  };
  if (loading) return <Loader />;
  return (
    <div>
      <div className="section-header"><h1>Orders</h1></div>
      {orders.length === 0 ? <EmptyState message="No orders" /> : (
        <table className="data-table">
          <thead><tr><th>Order</th><th>Customer</th><th>Amount</th><th>Payment</th><th>Status</th><th>Update</th></tr></thead>
          <tbody>{orders.map(o => (
            <tr key={o.id}><td>#{o.id}</td><td>{o.customerName}</td><td>{fmt(o.finalAmount)}</td><td><span className={statusClass(o.paymentStatus)}>{o.paymentStatus}</span></td><td><span className={statusClass(o.orderStatus)}>{o.orderStatus}</span></td>
              <td><select value={o.orderStatus} onChange={e => updateStatus(o.id, e.target.value)} style={{ padding: '4px 8px' }}>{['PLACED','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED'].map(s => <option key={s} value={s}>{s}</option>)}</select></td>
            </tr>
          ))}</tbody>
        </table>
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
          <Route path="/cart"              element={<CartPage           addToast={addToast} />} />
          <Route path="/checkout"          element={<CheckoutPage       addToast={addToast} />} />
          <Route path="/orders"            element={<OrdersPage         addToast={addToast} />} />
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
