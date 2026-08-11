// Login Form Handler
document.addEventListener('DOMContentLoaded', function() {
    // Display current server URL
    displayServerUrl();

    const loginForm = document.getElementById('loginForm');
    const loginModal = document.getElementById('loginModal');
    const closeBtn = document.querySelector('.close');
    const loginNavBtn = document.querySelector('.login-btn');

    // Close modal when X is clicked
    closeBtn.addEventListener('click', function() {
        loginModal.classList.remove('active');
    });

    // Open modal when Login button is clicked
    loginNavBtn.addEventListener('click', function() {
        loginModal.classList.add('active');
    });

    // Close modal when clicking outside of it
    window.addEventListener('click', function(e) {
        if (e.target === loginModal) {
            loginModal.classList.remove('active');
        }
    });

    // Handle form submission
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value.trim();
        const rememberMe = document.getElementById('rememberMe').checked;

        // Validation
        if (!email || !password) {
            showAlert('Please fill in all fields', 'error');
            return;
        }

        if (!isValidEmail(email)) {
            showAlert('Please enter a valid email address', 'error');
            return;
        }

        // Send login request to backend
        loginUser(email, password, rememberMe);
    });

    // Remember me functionality
    const rememberMe = localStorage.getItem('rememberMe');
    if (rememberMe === 'true') {
        document.getElementById('rememberMe').checked = true;
        document.getElementById('email').value = localStorage.getItem('userEmail') || '';
    }
});

// Validate email format
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Send login request to backend
function loginUser(email, password, rememberMe) {
    // For demonstration, using mock data
    // In production, this should connect to your Java backend

    const mockUsers = [
        { email: 'admin@shop.com', password: 'admin123', role: 'Admin', name: 'Admin User' },
        { email: 'customer@shop.com', password: 'customer123', role: 'Customer', name: 'John Doe' },
        { email: 'test@shop.com', password: 'test123', role: 'Customer', name: 'Test User' }
    ];

    const user = mockUsers.find(u => u.email === email && u.password === password);

    if (user) {
        // Store login info
        localStorage.setItem('isLoggedIn', 'true');
        localStorage.setItem('userEmail', email);
        localStorage.setItem('userName', user.name);
        localStorage.setItem('userRole', user.role);

        if (rememberMe) {
            localStorage.setItem('rememberMe', 'true');
        } else {
            localStorage.setItem('rememberMe', 'false');
        }

        showAlert('Login successful! Redirecting...', 'success');
        setTimeout(() => {
            window.location.href = 'dashboard.html';
        }, 1500);
    } else {
        showAlert('Invalid email or password', 'error');
    }
}

// Show alert messages
function showAlert(message, type) {
    // Remove existing alerts
    const existingAlert = document.querySelector('.alert');
    if (existingAlert) {
        existingAlert.remove();
    }

    // Create new alert
    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.textContent = message;

    const modal = document.getElementById('loginModal');
    const form = document.getElementById('loginForm');
    form.insertBefore(alert, form.firstChild);

    // Remove alert after 5 seconds
    setTimeout(() => {
        alert.remove();
    }, 5000);
}

// Forgot password handler
document.addEventListener('DOMContentLoaded', function() {
    const forgotLink = document.querySelector('.forgot-link');
    if (forgotLink) {
        forgotLink.addEventListener('click', function(e) {
            e.preventDefault();
            const email = document.getElementById('email').value.trim();
            if (!email) {
                showAlert('Please enter your email first', 'error');
            } else {
                showAlert('Password reset link sent to ' + email, 'success');
                // In production, this should send a reset email
            }
        });
    }
});

// Display server URL in navbar
function displayServerUrl() {
    const serverUrlElement = document.getElementById('serverUrl');
    if (serverUrlElement) {
        const currentUrl = window.location.protocol + '//' + window.location.hostname + ':' + window.location.port;
        serverUrlElement.textContent = currentUrl;
        
        // Make URL clickable
        serverUrlElement.style.cursor = 'pointer';
        serverUrlElement.addEventListener('click', function() {
            navigator.clipboard.writeText(currentUrl);
            alert('Server URL copied to clipboard:\n' + currentUrl);
        });
    }
}
