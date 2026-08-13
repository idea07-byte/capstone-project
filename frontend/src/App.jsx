import { useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';

const API_BASE = '/api';

async function getJson(path) {
  const res = await fetch(`${API_BASE}${path}`);
  if (!res.ok) throw new Error(`Request failed: ${res.status}`);
  return res.json();
}

async function sendJson(path, method, data) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`Request failed: ${res.status}`);
  return res.json();
}

function getStoredUser() {
  if (typeof window === 'undefined') {
    return null;
  }

  const storedUser = localStorage.getItem('shopCapstoneUser');
  return storedUser ? JSON.parse(storedUser) : null;
}

function App() {
  const [user, setUser] = useState(getStoredUser);
  const [alert, setAlert] = useState(null);
  const navigate = useNavigate();

  const showAlert = (message, type) => {
    setAlert({ message, type });
    window.clearTimeout(showAlert.timeout);
    showAlert.timeout = window.setTimeout(() => setAlert(null), 5000);
  };

  const handleLogin = async (email, password, rememberMe) => {
    try {
      const result = await sendJson('/auth/login', 'POST', { email, password });
      if (!result.success) {
        showAlert(result.message || 'Invalid email or password', 'error');
        return;
      }

      const nextUser = { ...result.user, rememberMe };
      localStorage.setItem('shopCapstoneUser', JSON.stringify(nextUser));
      localStorage.setItem('isLoggedIn', 'true');
      localStorage.setItem('userEmail', email);
      localStorage.setItem('userName', result.user.name);
      localStorage.setItem('userRole', result.user.role);
      localStorage.setItem('rememberMe', rememberMe ? 'true' : 'false');

      setUser(nextUser);
      showAlert('Login successful! Redirecting...', 'success');
      window.setTimeout(() => navigate('/'), 1000);
    } catch (error) {
      showAlert(error.message, 'error');
    }
  };

  const handleRegister = async (fullName, email, phone, password) => {
    try {
      const result = await sendJson('/auth/register', 'POST', { name: fullName, email, password });
      if (!result.success) {
        showAlert(result.message || 'Registration failed', 'error');
        return;
      }

      localStorage.setItem('newUserRegistration', JSON.stringify({
        name: fullName,
        email,
        phone,
        role: 'Customer',
        registeredDate: new Date().toISOString(),
      }));
      showAlert('Registration successful! Redirecting to login...', 'success');
      window.setTimeout(() => navigate('/'), 1000);
    } catch (error) {
      showAlert(error.message, 'error');
    }
  };

  return (
    <>
      <nav className="navbar">
        <div className="nav-container">
          <div className="logo">BuyIt</div>
          <ul className="nav-menu">
            <li><a href="#home">Home</a></li>
            <li><a href="#about">About</a></li>
            <li><a href="#services">Services</a></li>
            <li><a href="#contact">Contact</a></li>
            <li>
              {user ? (
                <Link to="/dashboard" className="login-btn">Dashboard</Link>
              ) : (
                <Link to="/" className="login-btn">Login</Link>
              )}
            </li>
          </ul>
          <div className="server-url" onClick={() => navigator.clipboard.writeText(window.location.origin)}>
            <span className="url-label">Server:</span>
            <span className="url-value">{window.location.origin}</span>
          </div>
        </div>
      </nav>

      <div className="forest-background" />

      <Routes>
        <Route path="/" element={user ? <HomePage user={user} /> : <LoginPage onLogin={handleLogin} alert={alert} showAlert={showAlert} />} />
        <Route path="/register" element={<RegisterPage onRegister={handleRegister} alert={alert} showAlert={showAlert} />} />
        <Route path="/dashboard" element={user ? <DashboardPage user={user} onLogout={() => { localStorage.clear(); setUser(null); navigate('/'); }} alert={alert} showAlert={showAlert} /> : <Navigate to="/" replace />} />
      </Routes>
    </>
  );
}

function HomePage({ user }) {
  return (
    <div className="home-page">
      <div className="home-background" />
      <section id="home" className="home-hero">
        <h1>Welcome, <span className="home-name">{user?.name}</span>!</h1>
        <p>Browse products, manage orders, and run your shop from one place.</p>
        <div className="home-actions">
          <Link to="/dashboard" className="home-btn">Go to Dashboard</Link>
        </div>
      </section>

      <section id="about" className="home-section">
        <h2>About BuyIt</h2>
        <p>BuyIt is a full-stack shop management system with a Java backend and a React frontend, backed by a PostgreSQL (Supabase) database.</p>
      </section>

      <section id="services" className="home-section">
        <h2>Our Services</h2>
        <div className="home-cards">
          <div className="home-card"><div className="home-card-icon">📦</div><h3>Products</h3><p>Track your inventory with full stock control.</p></div>
          <div className="home-card"><div className="home-card-icon">🛒</div><h3>Orders</h3><p>Create and manage customer orders with multiple items.</p></div>
          <div className="home-card"><div className="home-card-icon">👥</div><h3>Users</h3><p>Manage customers and admin accounts in one place.</p></div>
        </div>
      </section>

      <section id="contact" className="home-section">
        <h2>Contact Us</h2>
        <p>Reach out at <a href="mailto:support@buyit.example.com">support@buyit.example.com</a></p>
      </section>
    </div>
  );
}

function LoginPage({ onLogin, alert, showAlert }) {
  const [form, setForm] = useState({ email: '', password: '', rememberMe: false });
  const navigate = useNavigate();

  useEffect(() => {
    const rememberedEmail = localStorage.getItem('userEmail') || '';
    const rememberMe = localStorage.getItem('rememberMe') === 'true';
    setForm((current) => ({ ...current, email: rememberMe ? rememberedEmail : current.email, rememberMe }));
  }, []);

  const handleSubmit = (event) => {
    event.preventDefault();
    const email = form.email.trim();
    const password = form.password.trim();

    if (!email || !password) {
      showAlert('Please fill in all fields', 'error');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      showAlert('Please enter a valid email address', 'error');
      return;
    }

    onLogin(email, password, form.rememberMe);
  };

  return (
    <div className="login-page">
      <div className="login-background" />
      <div className="login-container">
        <div className="login-logo">BuyIt</div>
        <h1 className="login-title">Welcome Back</h1>
        <p className="login-subtitle">Sign in to continue shopping</p>
        {alert && alert.type === 'error' ? <div className={`alert alert-${alert.type}`}>{alert.message}</div> : null}
        {alert && alert.type === 'success' ? <div className={`alert alert-${alert.type}`}>{alert.message}</div> : null}
        <form id="loginForm" onSubmit={handleSubmit}>
          <div className="form-group">
            <input type="email" id="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="Email" required />
            <span className="icon">✉</span>
          </div>
          <div className="form-group">
            <input type="password" id="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="Password" required />
            <span className="icon">🔒</span>
          </div>
          <div className="form-options">
            <label className="checkbox">
              <input type="checkbox" id="rememberMe" checked={form.rememberMe} onChange={() => setForm({ ...form, rememberMe: !form.rememberMe })} />
              Remember me
            </label>
            <a href="#" className="forgot-link" onClick={(event) => { event.preventDefault(); showAlert('Password reset link sent to your email', 'success'); }}>Forgot Password?</a>
          </div>
          <button type="submit" className="login-button">Sign In</button>
          <p className="register-link">
            Don't have an account? <Link to="/register">Register</Link>
          </p>
        </form>
      </div>
    </div>
  );
}

function RegisterPage({ onRegister, alert, showAlert }) {
  const [form, setForm] = useState({ fullName: '', registerEmail: '', phone: '', registerPassword: '', confirmPassword: '', agreeTerms: false });

  const handleSubmit = (event) => {
    event.preventDefault();
    const { fullName, registerEmail, phone, registerPassword, confirmPassword, agreeTerms } = form;

    if (!fullName || !registerEmail || !phone || !registerPassword || !confirmPassword) {
      showAlert('Please fill in all fields', 'error');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerEmail)) {
      showAlert('Please enter a valid email address', 'error');
      return;
    }

    if (registerPassword.length < 6) {
      showAlert('Password must be at least 6 characters long', 'error');
      return;
    }

    if (registerPassword !== confirmPassword) {
      showAlert('Passwords do not match', 'error');
      return;
    }

    if (!agreeTerms) {
      showAlert('You must agree to the Terms & Conditions', 'error');
      return;
    }

    if (!/^[\d\s\-\+\(\)]+$/.test(phone) || phone.replace(/\D/g, '').length < 10) {
      showAlert('Please enter a valid phone number', 'error');
      return;
    }

    onRegister(fullName, registerEmail, phone, registerPassword);
  };

  return (
    <div className="register-page">
      <div className="register-background" />
      <div className="register-container">
        <div className="register-logo">BuyIt</div>
        <h1 className="register-title">Create Account</h1>
        <p className="register-subtitle">Sign up to start shopping</p>
        {alert && alert.type === 'error' ? <div className={`alert alert-${alert.type}`}>{alert.message}</div> : null}
        {alert && alert.type === 'success' ? <div className={`alert alert-${alert.type}`}>{alert.message}</div> : null}
        <form id="registerForm" onSubmit={handleSubmit}>
          <div className="form-group">
            <input type="text" id="fullName" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} placeholder="Full Name" required />
            <span className="icon">👤</span>
          </div>
          <div className="form-group">
            <input type="email" id="registerEmail" value={form.registerEmail} onChange={(event) => setForm({ ...form, registerEmail: event.target.value })} placeholder="Email" required />
            <span className="icon">✉</span>
          </div>
          <div className="form-group">
            <input type="tel" id="phone" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} placeholder="Phone Number" required />
            <span className="icon">📱</span>
          </div>
          <div className="form-group">
            <input type="password" id="registerPassword" value={form.registerPassword} onChange={(event) => setForm({ ...form, registerPassword: event.target.value })} placeholder="Password" required />
            <span className="icon">🔒</span>
          </div>
          <div className="form-group">
            <input type="password" id="confirmPassword" value={form.confirmPassword} onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })} placeholder="Confirm Password" required />
            <span className="icon">🔒</span>
          </div>
          <div className="form-options">
            <label className="checkbox">
              <input type="checkbox" id="agreeTerms" checked={form.agreeTerms} onChange={() => setForm({ ...form, agreeTerms: !form.agreeTerms })} required />
              <span className="checkmark"></span>
              I agree to the Terms & Conditions
            </label>
          </div>
          <button type="submit" className="login-button">Register</button>
          <p className="register-link">
            Already have an account? <Link to="/">Login</Link>
          </p>
        </form>
      </div>
    </div>
  );
}

function DashboardPage({ user, onLogout, showAlert }) {
  const [activeSection, setActiveSection] = useState('overview');
  const [settings, setSettings] = useState({ name: user?.name || '', email: user?.email || '' });
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [productModal, setProductModal] = useState(null);
  const [orderModal, setOrderModal] = useState(false);
  const [userModal, setUserModal] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [productData, orderData, userData] = await Promise.all([
        getJson('/products'),
        getJson('/orders'),
        getJson('/users'),
      ]);
      setProducts(productData);
      setOrders(orderData);
      setUsers(userData);
    } catch (error) {
      showAlert(error.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const stats = {
    totalProducts: products.length,
    totalOrders: orders.length,
    totalUsers: users.length,
    totalRevenue: orders.reduce((sum, order) => sum + Number(order.total || 0), 0),
  };

  const handleSaveSettings = (event) => {
    event.preventDefault();
    if (!settings.name || !settings.email) {
      showAlert('Please fill in all fields', 'error');
      return;
    }

    localStorage.setItem('userName', settings.name);
    localStorage.setItem('userEmail', settings.email);
    showAlert('Settings saved successfully!', 'success');
  };

  const handleSaveProduct = async (form) => {
    setSaving(true);
    try {
      const isEdit = Boolean(productModal.editing);
      const path = isEdit ? `/products/${productModal.editing.id}` : '/products';
      const method = isEdit ? 'PUT' : 'POST';
      const result = await sendJson(path, method, form);
      if (!result.success) {
        showAlert(result.message, 'error');
      } else {
        showAlert(result.message, 'success');
        setProductModal(null);
        await loadData();
      }
    } catch (error) {
      showAlert(error.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteProduct = async (id) => {
    if (!window.confirm(`Delete product #${id}?`)) return;
    try {
      const result = await sendJson(`/products/${id}`, 'DELETE');
      showAlert(result.message, result.success ? 'success' : 'error');
      await loadData();
    } catch (error) {
      showAlert(error.message, 'error');
    }
  };

  const handleSaveUser = async (form) => {
    setSaving(true);
    try {
      const result = await sendJson('/users', 'POST', form);
      if (!result.success) {
        showAlert(result.message, 'error');
      } else {
        showAlert(result.message, 'success');
        setUserModal(false);
        await loadData();
      }
    } catch (error) {
      showAlert(error.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteUser = async (id) => {
    if (!window.confirm(`Delete user #${id}?`)) return;
    try {
      const result = await sendJson(`/users/${id}`, 'DELETE');
      showAlert(result.message, result.success ? 'success' : 'error');
      await loadData();
    } catch (error) {
      showAlert(error.message, 'error');
    }
  };

  const handleCreateOrder = async (customerId, items) => {
    setSaving(true);
    try {
      const result = await sendJson('/orders', 'POST', { customer_id: customerId, items });
      if (!result.success) {
        showAlert(result.message, 'error');
      } else {
        showAlert(result.message, 'success');
        setOrderModal(false);
        await loadData();
      }
    } catch (error) {
      showAlert(error.message, 'error');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="dashboard-container">
      <aside className="sidebar">
        <div className="user-profile">
          <div className="avatar">👤</div>
          <h3 className="user-name">{user?.name}</h3>
          <p className="user-role">{user?.role}</p>
        </div>
        <ul className="sidebar-menu">
          <li><a href="#overview" className={`menu-item ${activeSection === 'overview' ? 'active' : ''}`} onClick={(event) => { event.preventDefault(); setActiveSection('overview'); }}>📊 Overview</a></li>
          <li><a href="#products" className={`menu-item ${activeSection === 'products' ? 'active' : ''}`} onClick={(event) => { event.preventDefault(); setActiveSection('products'); }}>📦 Products</a></li>
          <li><a href="#orders" className={`menu-item ${activeSection === 'orders' ? 'active' : ''}`} onClick={(event) => { event.preventDefault(); setActiveSection('orders'); }}>🛒 Orders</a></li>
          <li><a href="#users" className={`menu-item ${activeSection === 'users' ? 'active' : ''}`} onClick={(event) => { event.preventDefault(); setActiveSection('users'); }}>👥 Users</a></li>
          <li><a href="#settings" className={`menu-item ${activeSection === 'settings' ? 'active' : ''}`} onClick={(event) => { event.preventDefault(); setActiveSection('settings'); }}>⚙️ Settings</a></li>
        </ul>
        <div style={{ padding: '1.5rem' }}>
          <button className="logout-btn" onClick={onLogout}>Logout</button>
        </div>
      </aside>

      <main className="main-content">
        <section id="overview" className={`content-section ${activeSection === 'overview' ? 'active' : ''}`}>
          <h1>Welcome, <span className="user-name" style={{ color: '#1a3a52' }}>{user?.name}</span>!</h1>
          {loading ? <p>Loading...</p> : (
            <div className="dashboard-cards">
              <div className="card"><div className="card-header">📦</div><h3>Total Products</h3><p className="card-value">{stats.totalProducts}</p></div>
              <div className="card"><div className="card-header">🛒</div><h3>Total Orders</h3><p className="card-value">{stats.totalOrders}</p></div>
              <div className="card"><div className="card-header">👥</div><h3>Total Users</h3><p className="card-value">{stats.totalUsers}</p></div>
              <div className="card"><div className="card-header">💰</div><h3>Revenue</h3><p className="card-value">${stats.totalRevenue.toLocaleString(undefined, { minimumFractionDigits: 2 })}</p></div>
            </div>
          )}
        </section>

        <section id="products" className={`content-section ${activeSection === 'products' ? 'active' : ''}`}>
          <div className="section-header">
            <h2>Products</h2>
            <button className="btn-primary" onClick={() => setProductModal({ editing: null })}>+ Add Product</button>
          </div>
          {loading ? <p>Loading...</p> : (
            <table className="data-table">
              <thead>
                <tr><th>ID</th><th>Name</th><th>Price</th><th>Quantity</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {products.map((product) => (
                  <tr key={product.id}>
                    <td>{product.id}</td>
                    <td>{product.name}</td>
                    <td>${Number(product.price).toFixed(2)}</td>
                    <td>{product.quantity}</td>
                    <td>
                      <button className="btn-edit" onClick={() => setProductModal({ editing: product })}>Edit</button>
                      <button className="btn-delete" onClick={() => handleDeleteProduct(product.id)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section id="orders" className={`content-section ${activeSection === 'orders' ? 'active' : ''}`}>
          <div className="section-header">
            <h2>Orders</h2>
            <button className="btn-primary" onClick={() => setOrderModal(true)}>+ Create Order</button>
          </div>
          {loading ? <p>Loading...</p> : (
            <table className="data-table">
              <thead>
                <tr><th>Order ID</th><th>Customer</th><th>Items</th><th>Total</th><th>Status</th></tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td>#{order.id}</td>
                    <td>{order.customer}</td>
                    <td>{order.items}</td>
                    <td>${Number(order.total).toFixed(2)}</td>
                    <td><span className={`status-${order.status.toLowerCase()}`}>{order.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section id="users" className={`content-section ${activeSection === 'users' ? 'active' : ''}`}>
          <div className="section-header">
            <h2>Users</h2>
            <button className="btn-primary" onClick={() => setUserModal(true)}>+ Add User</button>
          </div>
          {loading ? <p>Loading...</p> : (
            <table className="data-table">
              <thead>
                <tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {users.map((entry) => (
                  <tr key={entry.id}>
                    <td>{entry.id}</td>
                    <td>{entry.name}</td>
                    <td>{entry.email}</td>
                    <td>{entry.role}</td>
                    <td>
                      <button className="btn-delete" onClick={() => handleDeleteUser(entry.id)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>

        <section id="settings" className={`content-section ${activeSection === 'settings' ? 'active' : ''}`}>
          <h2>Settings</h2>
          <form className="settings-form" onSubmit={handleSaveSettings}>
            <div className="form-group">
              <label>Full Name</label>
              <input type="text" id="settingsName" value={settings.name} onChange={(event) => setSettings({ ...settings, name: event.target.value })} placeholder="Enter your full name" />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input type="email" id="settingsEmail" value={settings.email} onChange={(event) => setSettings({ ...settings, email: event.target.value })} placeholder="Enter your email" />
            </div>
            <div className="form-group">
              <label>Current Password</label>
              <input type="password" placeholder="Enter your current password" />
            </div>
            <div className="form-group">
              <label>New Password</label>
              <input type="password" placeholder="Enter new password" />
            </div>
            <button className="btn-primary" type="submit">Save Settings</button>
          </form>
        </section>
      </main>

      {productModal && (
        <ProductModal
          product={productModal.editing}
          onCancel={() => setProductModal(null)}
          onSave={handleSaveProduct}
          saving={saving}
        />
      )}

      {orderModal && (
        <OrderModal
          products={products}
          customers={users.filter((u) => u.role === 'CUSTOMER')}
          onCancel={() => setOrderModal(false)}
          onSubmit={handleCreateOrder}
          saving={saving}
        />
      )}

      {userModal && (
        <UserModal
          onCancel={() => setUserModal(false)}
          onSave={handleSaveUser}
          saving={saving}
        />
      )}
    </div>
  );
}

function ProductModal({ product, onCancel, onSave, saving }) {
  const [form, setForm] = useState({
    name: product?.name || '',
    price: product ? Number(product.price).toFixed(2) : '',
    quantity: product?.quantity ?? '',
  });

  const handleSubmit = (event) => {
    event.preventDefault();
    const name = form.name.trim();
    const price = Number(form.price);
    const quantity = Number(form.quantity);

    if (!name) {
      window.alert('Please enter a product name');
      return;
    }
    if (Number.isNaN(price) || price < 0) {
      window.alert('Please enter a valid price');
      return;
    }
    if (!Number.isInteger(quantity) || quantity < 0) {
      window.alert('Please enter a valid quantity');
      return;
    }

    onSave({ name, price, quantity });
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <h3>{product ? `Edit Product #${product.id}` : 'Add Product'}</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Name</label>
            <input type="text" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Product name" required />
          </div>
          <div className="form-group">
            <label>Price</label>
            <input type="number" step="0.01" min="0" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} placeholder="0.00" required />
          </div>
          <div className="form-group">
            <label>Quantity</label>
            <input type="number" step="1" min="0" value={form.quantity} onChange={(event) => setForm({ ...form, quantity: event.target.value })} placeholder="0" required />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn-cancel" onClick={onCancel}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

function UserModal({ onCancel, onSave, saving }) {
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'CUSTOMER' });

  const handleSubmit = (event) => {
    event.preventDefault();
    const name = form.name.trim();
    const email = form.email.trim();
    const password = form.password.trim();

    if (!name || !email || !password) {
      window.alert('Please fill in all fields');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      window.alert('Please enter a valid email address');
      return;
    }
    if (password.length < 6) {
      window.alert('Password must be at least 6 characters long');
      return;
    }

    onSave({ name, email, password, role: form.role });
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <h3>Add User</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Name</label>
            <input type="text" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} placeholder="Full name" required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} placeholder="Email" required />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} placeholder="Password" required />
          </div>
          <div className="form-group">
            <label>Role</label>
            <select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value })}>
              <option value="CUSTOMER">Customer</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn-cancel" onClick={onCancel}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

function OrderModal({ products, customers, onCancel, onSubmit, saving }) {
  const [customerId, setCustomerId] = useState('');
  const [productId, setProductId] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [items, setItems] = useState([]);

  const addItem = () => {
    const product = products.find((p) => p.id === Number(productId));
    if (!product) {
      window.alert('Please select a product');
      return;
    }
    const qty = Number(quantity);
    if (!Number.isInteger(qty) || qty <= 0) {
      window.alert('Please enter a valid quantity');
      return;
    }
    if (qty > Number(product.quantity)) {
      window.alert(`Only ${product.quantity} in stock for ${product.name}`);
      return;
    }
    setItems((current) => [...current, { product_id: product.id, quantity: qty }]);
    setQuantity(1);
  };

  const removeItem = (index) => {
    setItems((current) => current.filter((_, i) => i !== index));
  };

  const total = items.reduce((sum, item) => {
    const product = products.find((p) => p.id === item.product_id);
    return sum + (product ? Number(product.price) * item.quantity : 0);
  }, 0);

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!customerId) {
      window.alert('Please select a customer');
      return;
    }
    if (items.length === 0) {
      window.alert('Please add at least one item');
      return;
    }
    onSubmit(Number(customerId), items);
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <h3>Create Order</h3>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Customer</label>
            <select value={customerId} onChange={(event) => setCustomerId(event.target.value)} required>
              <option value="">Select a customer</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>{customer.name} ({customer.email})</option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Add Item</label>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <select value={productId} onChange={(event) => setProductId(event.target.value)} style={{ flex: 1 }}>
                <option value="">Select product</option>
                {products.map((product) => (
                  <option key={product.id} value={product.id}>{product.name} - ${Number(product.price).toFixed(2)}</option>
                ))}
              </select>
              <input type="number" min="1" value={quantity} onChange={(event) => setQuantity(event.target.value)} style={{ width: '70px' }} />
              <button type="button" className="btn-primary" onClick={addItem}>+ Add</button>
            </div>
          </div>
          <div className="form-group">
            <label>Items</label>
            <div className="order-items-preview">
              {items.length === 0 ? <p style={{ color: '#999', margin: 0 }}>No items added yet</p> : items.map((item, index) => {
                const product = products.find((p) => p.id === item.product_id);
                return (
                  <div className="preview-row" key={index}>
                    <span>{product?.name}</span>
                    <span>× {item.quantity}</span>
                    <button type="button" className="btn-delete" style={{ padding: '0.25rem 0.5rem' }} onClick={() => removeItem(index)}>Remove</button>
                  </div>
                );
              })}
            </div>
          </div>
          <div className="order-total">Total: ${total.toFixed(2)}</div>
          <div className="modal-actions">
            <button type="button" className="btn-cancel" onClick={onCancel}>Cancel</button>
            <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Creating...' : 'Create Order'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default App;
