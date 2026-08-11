// Dashboard JavaScript
document.addEventListener('DOMContentLoaded', function() {
    // Check if user is logged in
    if (!localStorage.getItem('isLoggedIn')) {
        window.location.href = 'index.html';
        return;
    }

    // Update user info in dashboard
    const userName = localStorage.getItem('userName');
    const userRole = localStorage.getItem('userRole');
    const userEmail = localStorage.getItem('userEmail');

    document.querySelectorAll('.user-name').forEach(el => {
        el.textContent = userName;
    });

    document.querySelectorAll('.user-role').forEach(el => {
        el.textContent = userRole;
    });

    document.getElementById('settingsName').value = userName;
    document.getElementById('settingsEmail').value = userEmail;

    // Handle logout
    document.querySelector('.logout-btn').addEventListener('click', logout);

    // Setup sidebar navigation
    setupSidebarNavigation();

    // Load initial data
    loadDashboardData();
});

// Setup sidebar navigation
function setupSidebarNavigation() {
    const menuItems = document.querySelectorAll('.sidebar-menu .menu-item');
    const sections = document.querySelectorAll('.content-section');

    menuItems.forEach(item => {
        item.addEventListener('click', function(e) {
            e.preventDefault();

            // Remove active class from all
            menuItems.forEach(m => m.classList.remove('active'));
            sections.forEach(s => s.classList.remove('active'));

            // Add active class to clicked item
            this.classList.add('active');

            // Show corresponding section
            const href = this.getAttribute('href').substring(1);
            const section = document.getElementById(href);
            if (section) {
                section.classList.add('active');
            }
        });
    });
}

// Load dashboard data
function loadDashboardData() {
    // Mock data - replace with actual API calls
    const mockStats = {
        totalProducts: 45,
        totalOrders: 128,
        totalUsers: 256,
        totalRevenue: 125750
    };

    document.getElementById('totalProducts').textContent = mockStats.totalProducts;
    document.getElementById('totalOrders').textContent = mockStats.totalOrders;
    document.getElementById('totalUsers').textContent = mockStats.totalUsers;
    document.getElementById('totalRevenue').textContent = '$' + mockStats.totalRevenue.toLocaleString();

    loadProducts();
    loadOrders();
    loadUsers();
}

// Load products
function loadProducts() {
    const mockProducts = [
        { id: 1, name: 'Laptop', price: 999.99, quantity: 15 },
        { id: 2, name: 'Mouse', price: 29.99, quantity: 150 },
        { id: 3, name: 'Keyboard', price: 79.99, quantity: 75 },
        { id: 4, name: 'Monitor', price: 349.99, quantity: 30 },
        { id: 5, name: 'Headphones', price: 149.99, quantity: 50 }
    ];

    const tbody = document.getElementById('productsTable');
    tbody.innerHTML = mockProducts.map(product => `
        <tr>
            <td>${product.id}</td>
            <td>${product.name}</td>
            <td>$${product.price}</td>
            <td>${product.quantity}</td>
            <td>
                <button class="btn-edit" onclick="editProduct(${product.id})">Edit</button>
                <button class="btn-delete" onclick="deleteProduct(${product.id})">Delete</button>
            </td>
        </tr>
    `).join('');
}

// Load orders
function loadOrders() {
    const mockOrders = [
        { id: 101, customer: 'John Doe', date: '2024-08-01', total: 1200.00, status: 'Delivered' },
        { id: 102, customer: 'Jane Smith', date: '2024-08-02', total: 450.50, status: 'Processing' },
        { id: 103, customer: 'Mike Johnson', date: '2024-08-03', total: 825.75, status: 'Shipped' },
        { id: 104, customer: 'Sarah Williams', date: '2024-08-04', total: 320.00, status: 'Pending' },
        { id: 105, customer: 'Tom Brown', date: '2024-08-05', total: 1050.25, status: 'Delivered' }
    ];

    const tbody = document.getElementById('ordersTable');
    tbody.innerHTML = mockOrders.map(order => `
        <tr>
            <td>#${order.id}</td>
            <td>${order.customer}</td>
            <td>${order.date}</td>
            <td>$${order.total.toFixed(2)}</td>
            <td><span class="status-${order.status.toLowerCase()}">${order.status}</span></td>
            <td>
                <button class="btn-view" onclick="viewOrder(${order.id})">View</button>
                <button class="btn-edit" onclick="editOrder(${order.id})">Edit</button>
            </td>
        </tr>
    `).join('');
}

// Load users
function loadUsers() {
    const mockUsers = [
        { id: 1, name: 'Admin User', email: 'admin@shop.com', role: 'Admin' },
        { id: 2, name: 'John Doe', email: 'customer@shop.com', role: 'Customer' },
        { id: 3, name: 'Jane Smith', email: 'jane@shop.com', role: 'Customer' },
        { id: 4, name: 'Mike Johnson', email: 'mike@shop.com', role: 'Customer' },
        { id: 5, name: 'Sarah Williams', email: 'sarah@shop.com', role: 'Customer' }
    ];

    const tbody = document.getElementById('usersTable');
    tbody.innerHTML = mockUsers.map(user => `
        <tr>
            <td>${user.id}</td>
            <td>${user.name}</td>
            <td>${user.email}</td>
            <td>${user.role}</td>
            <td>
                <button class="btn-edit" onclick="editUser(${user.id})">Edit</button>
                <button class="btn-delete" onclick="deleteUser(${user.id})">Delete</button>
            </td>
        </tr>
    `).join('');
}

// Modal functions
function openAddProductModal() {
    alert('Add Product Modal - To be implemented');
}

function openCreateOrderModal() {
    alert('Create Order Modal - To be implemented');
}

function openAddUserModal() {
    alert('Add User Modal - To be implemented');
}

// Action functions
function editProduct(id) {
    alert('Edit Product #' + id + ' - To be implemented');
}

function deleteProduct(id) {
    if (confirm('Are you sure you want to delete this product?')) {
        alert('Product deleted - To be implemented');
    }
}

function viewOrder(id) {
    alert('View Order #' + id + ' - To be implemented');
}

function editOrder(id) {
    alert('Edit Order #' + id + ' - To be implemented');
}

function editUser(id) {
    alert('Edit User #' + id + ' - To be implemented');
}

function deleteUser(id) {
    if (confirm('Are you sure you want to delete this user?')) {
        alert('User deleted - To be implemented');
    }
}

// Save settings
function saveSettings() {
    const name = document.getElementById('settingsName').value;
    const email = document.getElementById('settingsEmail').value;

    if (!name || !email) {
        alert('Please fill in all fields');
        return;
    }

    localStorage.setItem('userName', name);
    localStorage.setItem('userEmail', email);

    // Update display
    document.querySelectorAll('.user-name').forEach(el => {
        el.textContent = name;
    });

    alert('Settings saved successfully!');
}

// Logout function
function logout() {
    if (confirm('Are you sure you want to logout?')) {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('userName');
        localStorage.removeItem('userEmail');
        localStorage.removeItem('userRole');
        window.location.href = 'index.html';
    }
}
