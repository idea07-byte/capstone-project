import { useEffect, useState } from 'react';
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';

const mockProducts = [
  { id: 1, name: 'Laptop', price: 999.99, quantity: 15 },
  { id: 2, name: 'Mouse', price: 29.99, quantity: 150 },
  { id: 3, name: 'Keyboard', price: 79.99, quantity: 75 },
  { id: 4, name: 'Monitor', price: 349.99, quantity: 30 },
  { id: 5, name: 'Headphones', price: 149.99, quantity: 50 },
];

const mockOrders = [
  { id: 101, customer: 'John Doe', date: '2024-08-01', total: 1200.0, status: 'Delivered' },
  { id: 102, customer: 'Jane Smith', date: '2024-08-02', total: 450.5, status: 'Processing' },
  { id: 103, customer: 'Mike Johnson', date: '2024-08-03', total: 825.75, status: 'Shipped' },
  { id: 104, customer: 'Sarah Williams', date: '2024-08-04', total: 320.0, status: 'Pending' },
  { id: 105, customer: 'Tom Brown', date: '2024-08-05', total: 1050.25, status: 'Delivered' },
];

const mockUsers = [
  { id: 1, name: 'Admin User', email: 'admin@shop.com', role: 'Admin' },
  { id: 2, name: 'John Doe', email: 'customer@shop.com', role: 'Customer' },
  { id: 3, name: 'Jane Smith', email: 'jane@shop.com', role: 'Customer' },
  { id: 4, name: 'Mike Johnson', email: 'mike@shop.com', role: 'Customer' },
  { id: 5, name: 'Sarah Williams', email: 'sarah@shop.com', role: 'Customer' },
];

const mockStats = {
  totalProducts: 45,
  totalOrders: 128,
  totalUsers: 256,
  totalRevenue: 125750,
};

function getStoredUser() {
  if (typeof window === 'undefined') {
    return null;
  }

  const storedUser = localStorage.getItem('shopCapstoneUser');
  return storedUser ? JSON.parse(storedUser) : null;
}

function App() {
  const [user, setUser] = useState(getStoredUser);
  const [serverUrl, setServerUrl] = useState('');
  const [alert, setAlert] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const currentUrl = `${window.location.protocol}//${window.location.hostname}:${window.location.port}`;
    setServerUrl(currentUrl);
  }, []);

  const showAlert = (message, type) => {
    setAlert({ message, type });
    window.clearTimeout(showAlert.timeout);
    showAlert.timeout = window.setTimeout(() => setAlert(null), 5000);
  };

  const handleLogin = (email, password, rememberMe) => {
    const mockAccounts = [
      { email: 'admin@shop.com', password: 'admin123', role: 'Admin', name: 'Admin User' },
      { email: 'customer@shop.com', password: 'customer123', role: 'Customer', name: 'John Doe' },
      { email: 'test@shop.com', password: 'test123', role: 'Customer', name: 'Test User' },
    ];

    const matchedUser = mockAccounts.find((account) => account.email === email && account.password === password);

    if (!matchedUser) {
      showAlert('Invalid email or password', 'error');
      return;
    }

    const nextUser = {
      ...matchedUser,
      email,
      rememberMe,
    };

    localStorage.setItem('shopCapstoneUser', JSON.stringify(nextUser));
    localStorage.setItem('isLoggedIn', 'true');
    localStorage.setItem('userEmail', email);
    localStorage.setItem('userName', matchedUser.name);
    localStorage.setItem('userRole', matchedUser.role);
    localStorage.setItem('rememberMe', rememberMe ? 'true' : 'false');

    setUser(nextUser);
    showAlert('Login successful! Redirecting...', 'success');
    window.setTimeout(() => navigate('/dashboard'), 1000);
  };

  const handleRegister = (fullName, email, phone, password) => {
    const newUser = {
      name: fullName,
      email,
      phone,
      role: 'Customer',
      registeredDate: new Date().toISOString(),
    };

    localStorage.setItem('newUserRegistration', JSON.stringify(newUser));
    showAlert('Registration successful! Redirecting to login...', 'success');
    window.setTimeout(() => navigate('/'), 1000);
  };

  return (
    <>
      <nav className="navbar">
        <div className="nav-container">
          <div className="logo">Shop</div>
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
          <div className="server-url" onClick={() => navigator.clipboard.writeText(serverUrl)}>
            <span className="url-label">Server:</span>
            <span className="url-value">{serverUrl || 'http://localhost:3000'}</span>
          </div>
        </div>
      </nav>

      <div className="forest-background" />

      <Routes>
        <Route path="/" element={<LoginPage onLogin={handleLogin} serverUrl={serverUrl} alert={alert} showAlert={showAlert} />} />
        <Route path="/register" element={<RegisterPage onRegister={handleRegister} serverUrl={serverUrl} alert={alert} showAlert={showAlert} />} />
        <Route path="/dashboard" element={user ? <DashboardPage user={user} onLogout={() => { localStorage.clear(); setUser(null); navigate('/'); }} serverUrl={serverUrl} alert={alert} showAlert={showAlert} /> : <Navigate to="/" replace />} />
      </Routes>
    </>
  );
}

function LoginPage({ onLogin, serverUrl, alert, showAlert }) {
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
    <div id="loginModal" className="modal active">
      <div className="modal-content">
        <span className="close" onClick={() => navigate('/')}>×</span>
        <h1>Login</h1>
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
          <button type="submit" className="login-button">Login</button>
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
  const navigate = useNavigate();

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
    <div id="registerModal" className="modal active">
      <div className="modal-content">
        <span className="close" onClick={() => navigate('/')}>×</span>
        <h1>Register</h1>
        {alert ? <div className={`alert alert-${alert.type}`}>{alert.message}</div> : null}
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
  const [products] = useState(mockProducts);
  const [orders] = useState(mockOrders);
  const [users] = useState(mockUsers);
  const [stats] = useState(mockStats);

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
      </aside>

      <main className="main-content">
        <section id="overview" className={`content-section ${activeSection === 'overview' ? 'active' : ''}`}>
          <h1>Welcome, <span className="user-name">{user?.name}</span>!</h1>
          <div className="dashboard-cards">
            <div className="card"><div className="card-header">📦</div><h3>Total Products</h3><p className="card-value">{stats.totalProducts}</p></div>
            <div className="card"><div className="card-header">🛒</div><h3>Total Orders</h3><p className="card-value">{stats.totalOrders}</p></div>
            <div className="card"><div className="card-header">👥</div><h3>Total Users</h3><p className="card-value">{stats.totalUsers}</p></div>
            <div className="card"><div className="card-header">💰</div><h3>Revenue</h3><p className="card-value">${stats.totalRevenue.toLocaleString()}</p></div>
          </div>
        </section>

        <section id="products" className={`content-section ${activeSection === 'products' ? 'active' : ''}`}>
          <div className="section-header">
            <h2>Products</h2>
            <button className="btn-primary" onClick={() => showAlert('Add Product modal is ready to be implemented', 'success')}>+ Add Product</button>
          </div>
          <table className="data-table">
            <thead>
              <tr><th>ID</th><th>Name</th><th>Price</th><th>Quantity</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id}>
                  <td>{product.id}</td>
                  <td>{product.name}</td>
                  <td>${product.price}</td>
                  <td>{product.quantity}</td>
                  <td>
                    <button className="btn-edit" onClick={() => showAlert(`Edit Product #${product.id}`, 'success')}>Edit</button>
                    <button className="btn-delete" onClick={() => showAlert(`Delete Product #${product.id}`, 'success')}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section id="orders" className={`content-section ${activeSection === 'orders' ? 'active' : ''}`}>
          <div className="section-header">
            <h2>Orders</h2>
            <button className="btn-primary" onClick={() => showAlert('Create Order modal is ready to be implemented', 'success')}>+ Create Order</button>
          </div>
          <table className="data-table">
            <thead>
              <tr><th>Order ID</th><th>Customer</th><th>Date</th><th>Total</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {orders.map((order) => (
                <tr key={order.id}>
                  <td>#{order.id}</td>
                  <td>{order.customer}</td>
                  <td>{order.date}</td>
                  <td>${order.total.toFixed(2)}</td>
                  <td><span className={`status-${order.status.toLowerCase()}`}>{order.status}</span></td>
                  <td>
                    <button className="btn-view" onClick={() => showAlert(`View Order #${order.id}`, 'success')}>View</button>
                    <button className="btn-edit" onClick={() => showAlert(`Edit Order #${order.id}`, 'success')}>Edit</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>

        <section id="users" className={`content-section ${activeSection === 'users' ? 'active' : ''}`}>
          <div className="section-header">
            <h2>Users</h2>
            <button className="btn-primary" onClick={() => showAlert('Add User modal is ready to be implemented', 'success')}>+ Add User</button>
          </div>
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
                    <button className="btn-edit" onClick={() => showAlert(`Edit User #${entry.id}`, 'success')}>Edit</button>
                    <button className="btn-delete" onClick={() => showAlert(`Delete User #${entry.id}`, 'success')}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
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
    </div>
  );
}

export default App;
