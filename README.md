# BuyIt — Multi-Vendor E-Commerce Marketplace

> Capstone Project: Multi-vendor E-Commerce Marketplace with Seller Dashboard, Admin Monitoring, Customer Storefront, and Secure Transactional Order Management (Amazon-style).

---

## 🌟 Key Features

### 🛒 Customer Storefront
- **1,000 Products & 3,000 Product Images:** Browse across 15 consumer categories (Electronics, Mobiles, Laptops, Headphones, Clothing, Shoes, Watches, Bags, Home & Kitchen, Beauty, Books, Toys, Sports, Grocery, Appliances).
- **Advanced Search & Filtering:** Filter by category, brand, minimum/maximum price, and sort by price, rating, or newest.
- **Product Details & Galleries:** Multi-image carousel, stock indicator, dynamic star ratings, and authentic customer reviews.
- **Cart & Wishlist:** Persistent shopping cart with real-time totals, quantity controls, and one-click wishlist saving.
- **Transactional Checkout:** Multi-item orders with address selection, promo coupon discounts (e.g. `WELCOME10`), COD/Online payment methods, and atomic stock deductions.
- **Notifications & History:** Live notification bell and order history with status tracking.

### 🏪 Vendor Portal
- **Seller Dashboard:** Real-time revenue metrics, product counts, and order statistics.
- **Catalog Management:** Add, edit, and manage products with image URLs, pricing, discounts, and inventory tracking.
- **Order Fulfillment:** Review incoming customer orders containing vendor products and update processing states.
- **Vendor Profile:** Store branding, business name, address, and operational details.

### 🛡️ Admin Dashboard
- **Platform Analytics:** Live KPI cards for total customers, active vendors, products catalog, order volumes, and platform gross revenue.
- **Customer & Vendor Oversight:** Manage users, toggle active/inactive account status, approve or suspend vendor merchants.
- **Category & Brand Management:** Create, update, and manage marketplace categories and brand registries.
- **Order Monitoring:** Platform-wide order review, line item breakdowns, and lifecycle status management.

### 💻 Dual-Mode Architecture (Web & CLI)
- **Single Java Web Server:** Built on JDK `HttpServer` (zero external framework bloat), serving both the production React SPA and JSON REST APIs on port 8080.
- **Interactive CLI Management:** Built-in console menu in `Main` for terminal-based operations (products, users, orders) alongside the web server.
- **Automated Test Suite:** Built-in standalone automated test runner `TestRunner` verifying database connectivity, models, cart/wishlist, transactional stock safety, and REST APIs.

---

## 🚀 Quick Start

### Prerequisites
- **Java:** JDK 17 or higher (tested on JDK 21 and 26)
- **Node.js:** Node 18+ (for frontend building, optional if using pre-built `frontend/dist`)
- **Database:** Supabase PostgreSQL cloud database (pre-configured in `backend/resources/database.properties`)

### 1. One-Click Build & Run (Root Directory)

```bat
:: Build both React frontend and Java backend
build.bat

:: Launch the application
run.bat
```
Then open your browser at **[http://localhost:8080](http://localhost:8080)**.

### 2. Running via Backend Directory

```bat
cd backend
build.bat
run.bat
```

### 3. Running Frontend in Vite Dev Server (Optional)

```bash
cd frontend
npm install
npm run dev
```
Accessible at `http://localhost:3000` (API requests automatically proxied to `:8080`).

---

## 🔑 Demo & Test Credentials

| Role | Email | Password | Access Portal |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@buyit.com` | `Admin@123` | Full Admin Dashboard (`/admin`) |
| **Vendor** | `vendor1@buyit.com` | `Vendor@123` | Merchant Portal (`/vendor`) |
| **Customer** | `customer@buyit.com` | `Customer@123` | Storefront & Checkout (`/store`) |

---

## 🧪 Automated Testing

The project includes an automated test suite verifying database integrity, models, polymorphism, stock atomicity, and REST helpers:

```bat
:: Run tests from root
java -cp "out;backend\lib\postgresql-42.7.4.jar" TestRunner

:: Or from backend directory
cd backend
java -cp "..\out;lib\postgresql-42.7.4.jar" TestRunner
```

**Test Coverage:**
1. Database Connection & 15-Table Integrity
2. Product Catalog, Search & Stock Checking
3. User Roles, Authentication & Polymorphism (`Customer`, `VendorUser`, `Admin`)
4. Cart & Wishlist Operations
5. Transactional Order Placement & Atomic Stock Deduction (with rollbacks)
6. JSON Serialization & Helper Routines

---

## 📡 REST API Reference

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | User authentication | No |
| `POST` | `/api/auth/register` | User / vendor registration | No |
| `POST` | `/api/auth/logout` | Session invalidation | Yes |
| `GET` | `/api/auth/me` | Current authenticated user profile | Yes |
| `GET` | `/api/products` | Search & list catalog products | No |
| `GET` | `/api/products/{id}` | Product details with image gallery | No |
| `POST` | `/api/products` | Add new product (vendor/admin) | Yes (Vendor) |
| `PUT` | `/api/products/{id}` | Update product details or stock | Yes (Owner/Admin) |
| `DELETE` | `/api/products/{id}` | Remove product | Yes (Admin) |
| `GET` | `/api/categories` | List all catalog categories | No |
| `POST` | `/api/categories` | Create product category | Yes (Admin) |
| `GET` | `/api/brands` | List all registered brands | No |
| `POST` | `/api/brands` | Create new brand entry | Yes (Admin) |
| `GET` | `/api/users` | List users (optional `?role=...`) | Yes (Admin) |
| `GET` | `/api/users/{id}` | User details by ID | Yes |
| `POST` | `/api/users` | Create customer/vendor/admin | Yes (Admin) |
| `DELETE` | `/api/users/{id}` | Remove user | Yes (Admin) |
| `GET` | `/api/cart` | Retrieve customer cart items & totals | Yes (Customer) |
| `POST` | `/api/cart` | Add product to cart | Yes (Customer) |
| `PUT` | `/api/cart` | Update item quantity | Yes (Customer) |
| `DELETE` | `/api/cart` | Remove item or clear cart | Yes (Customer) |
| `GET` | `/api/wishlist` | Retrieve customer saved wishlist | Yes (Customer) |
| `POST` | `/api/wishlist` | Add product to wishlist | Yes (Customer) |
| `DELETE` | `/api/wishlist` | Remove product from wishlist | Yes (Customer) |
| `GET` | `/api/orders` | List user orders (role-filtered) | Yes |
| `GET` | `/api/orders/{id}` | Single order details with line items | Yes |
| `POST` | `/api/orders` | Place multi-item order (transactional) | Yes (Customer) |
| `PUT` | `/api/orders/{id}/status` | Update order status | Yes (Vendor/Admin) |
| `POST` | `/api/orders/{id}/cancel` | Cancel order & restore inventory | Yes (Customer) |
| `GET` | `/api/admin/stats` | Admin platform metrics & KPIs | Yes (Admin) |
| `GET` | `/api/notifications` | User notifications list & unread count | Yes |
| `POST` | `/api/notifications/read`| Mark all user notifications as read | Yes |

---

## 📁 Repository Structure

```
capstone/
├── build.bat                    # Top-level build script (Frontend + Backend)
├── run.bat                      # Top-level run script
├── backend/
│   ├── Main.java                # Web server + Interactive CLI launcher
│   ├── WebServer.java           # Built-in HttpServer REST API & SPA host
│   ├── TestRunner.java          # Standalone automated test suite
│   ├── build.bat / run.bat      # Backend standalone scripts
│   ├── model/                   # Product, User, Customer, Admin, Vendor, Order, etc.
│   ├── service/                 # ProductService, UserService, OrderService, Cart, etc.
│   ├── db/                      # Database connection pool, SeedPostgresRunner
│   ├── lib/                     # JDBC drivers (PostgreSQL, MySQL)
│   └── resources/               # database.properties configuration
├── frontend/
│   ├── src/App.jsx              # Full React single-page application (Storefront, Portals)
│   ├── css/style.css            # Stylesheets (glassmorphism, theme, components)
│   ├── dist/                    # Production bundle served by Java WebServer
│   ├── package.json             # React 18, Vite 5, React Router 6
│   └── vite.config.js           # Vite dev proxy configuration
├── amazon-capstone/             # 1000-product image dataset pipeline & mapping
└── docs/                        # Architecture diagrams, ERD, study guides, slides
```

---

## 📚 Documentation

- [Usage Guide](docs/usage.md)
- [System Architecture & UML Diagrams](docs/diagrams.md)
- [Entity-Relationship Diagram (ERD)](docs/erd.md)
- [Comprehensive Study Guide](docs/study-guide.md)
- [Presentation Slides Outline](docs/slides.md)
