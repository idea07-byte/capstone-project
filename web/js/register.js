// Register Form Handler
document.addEventListener('DOMContentLoaded', function() {
    // Display current server URL
    displayServerUrl();

    const registerForm = document.getElementById('registerForm');
    const registerModal = document.getElementById('registerModal');
    const closeBtn = document.querySelector('.close');

    // Close modal when X is clicked
    closeBtn.addEventListener('click', function() {
        window.location.href = 'index.html';
    });

    // Handle form submission
    registerForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const fullName = document.getElementById('fullName').value.trim();
        const email = document.getElementById('registerEmail').value.trim();
        const phone = document.getElementById('phone').value.trim();
        const password = document.getElementById('registerPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        const agreeTerms = document.getElementById('agreeTerms').checked;

        // Validation
        if (!fullName || !email || !phone || !password || !confirmPassword) {
            showAlert('Please fill in all fields', 'error');
            return;
        }

        if (!isValidEmail(email)) {
            showAlert('Please enter a valid email address', 'error');
            return;
        }

        if (password.length < 6) {
            showAlert('Password must be at least 6 characters long', 'error');
            return;
        }

        if (password !== confirmPassword) {
            showAlert('Passwords do not match', 'error');
            return;
        }

        if (!agreeTerms) {
            showAlert('You must agree to the Terms & Conditions', 'error');
            return;
        }

        if (!isValidPhone(phone)) {
            showAlert('Please enter a valid phone number', 'error');
            return;
        }

        // Register user
        registerUser(fullName, email, phone, password);
    });
});

// Validate email format
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Validate phone number
function isValidPhone(phone) {
    const phoneRegex = /^[\d\s\-\+\(\)]+$/;
    return phoneRegex.test(phone) && phone.replace(/\D/g, '').length >= 10;
}

// Register user
function registerUser(fullName, email, phone, password) {
    // Mock registration - in production, send to backend
    const newUser = {
        name: fullName,
        email: email,
        phone: phone,
        role: 'Customer',
        registeredDate: new Date().toISOString()
    };

    // Store new user temporarily
    localStorage.setItem('newUserRegistration', JSON.stringify(newUser));

    showAlert('Registration successful! Redirecting to login...', 'success');
    setTimeout(() => {
        window.location.href = 'index.html';
    }, 2000);
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

    const form = document.getElementById('registerForm');
    form.insertBefore(alert, form.firstChild);

    // Remove alert after 5 seconds
    setTimeout(() => {
        alert.remove();
    }, 5000);
}

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
