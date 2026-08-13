-- ============================================
-- buyit Capstone - PostgreSQL Schema (Supabase)
-- Run this in the Supabase SQL Editor (Dashboard -> SQL Editor)
-- ============================================

-- ============================================
-- Users Table
-- Stores both customers and admins
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);

-- ============================================
-- Products Table
-- ============================================
CREATE TABLE IF NOT EXISTS products (
    id INT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);

-- ============================================
-- Orders Table
-- ============================================
CREATE TABLE IF NOT EXISTS orders (
    id INT PRIMARY KEY,
    customer_id INT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at);

-- ============================================
-- Order Items Table
-- ============================================
CREATE TABLE IF NOT EXISTS order_items (
    id INT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items (product_id);

-- ============================================
-- Seed Initial Data
-- ============================================

-- Insert admin user (password: adminpass)
INSERT INTO users (id, name, email, password, role) VALUES
(900, 'Admin', 'admin@example.com', 'adminpass', 'ADMIN')
ON CONFLICT (id) DO NOTHING;

-- Insert customer (password: pass1234)
INSERT INTO users (id, name, email, password, role) VALUES
(1, 'Asha', 'asha@example.com', 'pass1234', 'CUSTOMER')
ON CONFLICT (id) DO NOTHING;

-- Insert products
INSERT INTO products (id, name, price, quantity) VALUES
(101, 'Wireless Mouse', 799.00, 12),
(102, 'USB-C Cable', 299.00, 25),
(103, 'Mechanical Keyboard', 2499.00, 8)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- Useful Queries
-- ============================================

-- View all users with their roles
-- SELECT id, name, email, role FROM users;

-- View all products with stock
-- SELECT id, name, price, quantity FROM products;

-- View all orders with customer info
-- SELECT o.id, u.name AS customer, o.total_amount, o.status, o.created_at
-- FROM orders o
-- JOIN users u ON o.customer_id = u.id;

-- View order items with product info
-- SELECT oi.id, o.order_id, p.name AS product, oi.quantity, oi.unit_price, (oi.quantity * oi.unit_price) AS total
-- FROM order_items oi
-- JOIN orders o ON oi.order_id = o.id
-- JOIN products p ON oi.product_id = p.id;

-- Get customer order history
-- SELECT o.id, o.total_amount, o.status, o.created_at
-- FROM orders o
-- WHERE o.customer_id = 1;

-- Get low stock products
-- SELECT id, name, price, quantity FROM products WHERE quantity < 10;
