# 🛍️ BuyIt — Multi-Vendor E-Commerce Marketplace

<div align="center">

![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2026-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![React](https://img.shields.io/badge/React-18.3-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-5.4-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Supabase](https://img.shields.io/badge/Database-Supabase%20Cloud-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

<p align="center">
  <strong>Production-Grade Multi-Vendor E-Commerce Platform</strong><br>
  Featuring Seller Portals, Platform Admin Telemetry, Customer VIP Storefront, Real-Time Cart Synchronization, Multi-Image Galleries, and ACID Transactional Inventory Management.
</p>

[🎓 Academic Report (PDF)](BuyIt_Database_Report.pdf) •
[📑 Activity Document (PDF)](BuyIt_Activity_Document.pdf) •
[📕 Technical Documentation (PDF)](BuyIt_Project_Documentation.pdf) •
[📖 Full Markdown Spec](PROJECT_DOCUMENTATION.md) •
[🌐 Cloud Deployment Guide](DEPLOYMENT.md)

</div>

---

## 📑 Table of Contents
1. [Executive Overview](#-executive-overview)
2. [Key Architecture & Technical Highlights](#-key-architecture--technical-highlights)
3. [Portal & Core Features](#-portal--core-features)
   - [Customer VIP Storefront](#1-customer-vip-storefront)
   - [Vendor Merchant Portal](#2-vendor-merchant-portal)
   - [Super Admin Platform Console](#3-super-admin-platform-console)
4. [Live Demo Credentials](#-live-demo-credentials)
5. [System Architecture Diagram](#-system-architecture-diagram)
6. [Database Schema (15 Tables)](#-database-schema-15-tables)
7. [Getting Started (Local Setup)](#-getting-started-local-setup)
8. [Cloud Deployment](#-cloud-deployment)
9. [Automated Test Suite](#-automated-test-suite)
10. [REST API Documentation](#-rest-api-documentation)
11. [Project Directory Map](#-project-directory-map)

---

## 🌟 Executive Overview

**BuyIt** is an Amazon-style, full-stack multi-vendor e-commerce marketplace engineered for high performance, transactional safety, and seamless customer experiences:

* **Unified Lightweight Java Backend:** Powered by JDK's embedded high-throughput `HttpServer` with non-blocking connection routing, custom connection pooling, and zero framework bloat.
* **Modern React 18 SPA:** Responsive luxury UI powered by Vite, featuring fluid glassmorphism aesthetics, dynamic dark/light surfaces, animated carousels, and instant real-time client routing.
* **1,000 Products & 3,000 Studio Photos:** Seeded enterprise catalog across 15 consumer categories (Electronics, Laptops, Mobiles, Sneakers, Luxury Apparel, Watches, Appliances, and more).
* **ACID Transactional Safety:** Atomic order placement with row-level stock locking, automated inventory deduction, rollback guarantees on failure, and multi-coupon calculation engines.
* **Hybrid Guest & Cloud Cart Engine:** Offline-first guest cart in `localStorage` that seamlessly auto-merges into the user's PostgreSQL database cart upon login.

---

## ⚡ Key Architecture & Technical Highlights

```mermaid
flowchart TD
    Client["🌐 React 18 Single-Page Application (Vite CDN / Local :3000)"]
    
    subgraph Backend ["⚡ High-Throughput Java WebServer (:8080)"]
        Router["HttpServer Router & CORS Interceptor"]
        AuthHandler["Auth Handler (Bearer Token / UUID)"]
        ProductHandler["Product & Gallery Service"]
        CartHandler["Hybrid Cart & Wishlist Service"]
        OrderHandler["Transactional Order & Stock Engine"]
        AdminHandler["Admin Telemetry & User Control"]
    end
    
    subgraph Database ["🐘 Cloud PostgreSQL Database (Supabase / Neon / Railway)"]
        UsersTbl[("users & vendors")]
        CatalogTbl[("categories, brands, products, product_images")]
        OrdersTbl[("orders, order_items & addresses")]
        CartTbl[("cart, cart_items & wishlist")]
        ReviewsTbl[("reviews, coupons & notifications")]
    end

    Client -->|REST JSON APIs| Router
    Router --> AuthHandler & ProductHandler & CartHandler & OrderHandler & AdminHandler
    AuthHandler & ProductHandler & CartHandler & OrderHandler & AdminHandler -->|Connection Pool (12 Max)| Database
```

---

## 🎯 Portal & Core Features

### 1. Customer VIP Storefront
- **Enterprise Catalog:** Browse 1,000 products with instant multi-facet filtering (categories, brands, price range sliders, rating filters, and sorting).
- **Interactive Multi-Image Galleries:** High-resolution studio photography with category-intelligent fallback pipelines and interactive variant/size pickers.
- **Hybrid Guest & Cloud Cart:** Add items to cart as a guest without friction; cart automatically synchronizes to the cloud on login with zero data loss.
- **Express Transactional Checkout:** Multi-item orders with saved addresses, coupon discounts (e.g. `WELCOME10`), COD/Card payment simulations, and instant stock reservations.
- **Customer Account & Tracking:** Order history, tracking status badges (`PENDING`, `SHIPPED`, `DELIVERED`), wishlist toggling, and verified product reviews.

### 2. Vendor Merchant Portal
- **Real-Time Analytics:** Live KPI metrics for gross merchant volume (GMV), active product counts, inventory warnings, and order counts.
- **Product Catalog Management:** Add and edit items with multi-image URLs, MSRP pricing, discount percentages, SKUs, and stock quantities.
- **Fulfillment Management:** Filter incoming customer orders by vendor line items and transition processing states.
- **Store Branding:** Custom store banner, business details, owner identity, and contact information.

### 3. Super Admin Platform Console
- **Executive KPI Dashboard:** Real-time platform revenue, total user registrations, vendor metrics, order volumes, and catalog size.
- **User & Merchant Governance:** Search users by role, toggle account status (`ACTIVE` / `INACTIVE`), approve or ban vendors.
- **Category & Brand Engine:** Declarative management of top-level product taxonomies and brand directories.
- **Platform Order Auditing:** Audit and manage every transactional order across all sellers.

---

## 🔑 Live Demo Credentials

Use the **Quick Demo Access** buttons on the login page or sign in with these credentials:

| Role | Email | Password | Default Portal Route | Privileges |
| :--- | :--- | :--- | :--- | :--- |
| 👑 **Super Admin** | `admin@buyit.com` | `Admin@123` | `/admin` | Complete platform analytics, user moderation, taxonomy controls |
| 🏪 **Vendor Merchant** | `vendor1@buyit.com` | `Vendor@123` | `/vendor` | Store catalog management, order fulfillment, revenue stats |
| 🛍️ **Customer** | `customer@buyit.com` | `Customer@123` | `/store` | Full storefront, hybrid cart, wishlist, transactional checkout |

---

## 🗄️ Database Schema (15 Tables)

The system runs on a normalized 3NF relational schema engineered for PostgreSQL & MySQL:

| # | Table Name | Purpose | Key Constraints |
| :--- | :--- | :--- | :--- |
| 1 | `users` | Core user identity & role polymorphism | `role IN ('CUSTOMER', 'VENDOR', 'ADMIN')` |
| 2 | `categories` | Product taxonomy hierarchy | `slug UNIQUE`, `name NOT NULL` |
| 3 | `brands` | Official brand registries | `name NOT NULL`, `logo TEXT` |
| 4 | `vendors` | Merchant business profiles | `FOREIGN KEY (user_id) REFERENCES users(id)` |
| 5 | `products` | Core catalog listings & inventory | `FOREIGN KEY (vendor_id)`, `price > 0`, `stock >= 0` |
| 6 | `product_images` | Multi-image gallery URLs | `FOREIGN KEY (product_id) ON DELETE CASCADE` |
| 7 | `cart` | Customer active shopping basket | `FOREIGN KEY (customer_id) UNIQUE` |
| 8 | `cart_items` | Individual line items in shopping cart | `UNIQUE (cart_id, product_id)`, `quantity > 0` |
| 9 | `wishlist` | Customer saved favorites container | `FOREIGN KEY (customer_id) UNIQUE` |
| 10 | `wishlist_items` | Individual saved wishlist products | `UNIQUE (wishlist_id, product_id)` |
| 11 | `orders` | Transactional order headers | `status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')` |
| 12 | `order_items` | Immutable snapshot of purchased items | `FOREIGN KEY (order_id)`, `unit_price DECIMAL(10,2)` |
| 13 | `addresses` | Customer shipping address book | `FOREIGN KEY (user_id) REFERENCES users(id)` |
| 14 | `reviews` | Customer ratings & feedback | `rating INT CHECK (rating BETWEEN 1 AND 5)` |
| 15 | `coupons` | Promo code discount engine | `code UNIQUE`, `discount_percent DECIMAL(5,2)` |

---

## 🚀 Getting Started (Local Setup)

### Prerequisites
* **Java:** JDK 17+ (tested on JDK 21 and 26)
* **Node.js:** v18+ (for building React frontend)
* **Git:** Version control

### Option A: One-Click Launch (Windows)

```bat
:: 1. Clone repository
git clone https://github.com/idea07-byte/capstone-project.git
cd capstone-project

:: 2. Build full-stack application (compiles React SPA and Java backend)
build.bat

:: 3. Start BuyIt server
run.bat
```
* Access the web platform at: **[http://localhost:8080](http://localhost:8080)**

---

### Option B: Linux / macOS / Unix Setup

```bash
# 1. Grant execution permissions
chmod +x build.sh run.sh

# 2. Build both frontend and backend
./build.sh

# 3. Launch the platform
./run.sh
```

---

### Option C: Frontend Hot-Reload Development (Vite Dev Server)

```bash
cd frontend
npm install
npm run dev
```
* Vite will run at **[http://localhost:3000](http://localhost:3000)** with proxy routing configured to the backend on `:8080`.

---

## ☁️ Cloud Deployment

BuyIt is 12-factor cloud compliant and containerized for zero-friction cloud deployment.

* 🌐 **Frontend (Netlify):** Push to GitHub, connect to Netlify, set Base Directory to `frontend`, Build Command to `npm run build`, Publish Directory to `frontend/dist`, and set `VITE_API_URL` to your backend URL. (See [NETLIFY_DEPLOYMENT.md](NETLIFY_DEPLOYMENT.md))
* 🚀 **Backend (Render / Railway / Cloud Run):** One-click container deployment via Dockerfile. (See [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md) and [DEPLOYMENT.md](DEPLOYMENT.md))
* 🐳 **Docker Compose:**
  ```bash
  docker compose up -d --build
  ```
* 🏥 **Health Endpoint:** `GET /health` and `GET /api/health` return live service and database connectivity telemetry.

---

## 🧪 Automated Test Suite

A standalone test suite validates database health, polymorphic user models, catalog search, atomic inventory locks, and REST endpoints:

```bat
# Run test suite
java -cp "out;backend/lib/postgresql-42.7.4.jar;backend/resources" TestRunner
```

**Validated Test Cases:**
1. ✅ Database connection & 15-table integrity check
2. ✅ Catalog search, category resolution & stock validation
3. ✅ Polymorphic user roles (`Customer`, `VendorUser`, `Admin`)
4. ✅ Cart & Wishlist persistence and calculations
5. ✅ Transactional order placement with atomic rollback on out-of-stock
6. ✅ JSON serializer, null-safety guards & escape routines

---

## 📡 REST API Documentation

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | User authentication & bearer token issuance | No |
| `POST` | `/api/auth/register` | Register new customer or vendor merchant | No |
| `POST` | `/api/auth/logout` | Invalidate active user session | Bearer Token |
| `GET` | `/api/auth/me` | Fetch active authenticated profile | Bearer Token |
| `GET` | `/api/products` | Search catalog with category/brand/price filters | No |
| `GET` | `/api/products/{id}` | Product details with multi-image gallery | No |
| `POST` | `/api/products` | Create product (vendor or admin) | Vendor / Admin |
| `PUT` | `/api/products/{id}` | Update product details, pricing, and stock | Vendor / Admin |
| `DELETE` | `/api/products/{id}` | Remove product from catalog | Admin |
| `GET` | `/api/categories` | List all product categories | No |
| `POST` | `/api/categories` | Create new catalog category | Admin |
| `GET` | `/api/brands` | List registered brands | No |
| `POST` | `/api/brands` | Create new brand | Admin |
| `GET` | `/api/cart` | Retrieve customer cart items and totals | Customer Token |
| `POST` | `/api/cart` | Add product to cart | Customer Token |
| `PUT` | `/api/cart` | Update item quantity in cart | Customer Token |
| `DELETE` | `/api/cart` | Remove single item or clear entire cart | Customer Token |
| `GET` | `/api/wishlist` | Retrieve saved customer wishlist | Customer Token |
| `POST` | `/api/wishlist` | Add product to wishlist | Customer Token |
| `DELETE` | `/api/wishlist` | Remove product from wishlist | Customer Token |
| `GET` | `/api/orders` | List customer orders / vendor line items | Bearer Token |
| `POST` | `/api/orders` | Place transactional order with stock lock | Customer Token |
| `PUT` | `/api/orders/{id}/status` | Update fulfillment state (`SHIPPED`, `DELIVERED`) | Vendor / Admin |
| `GET` | `/api/admin/stats` | Platform KPIs (Revenue, Users, Orders, Stock) | Admin Token |
| `GET` | `/api/notifications` | Unread user alerts & notifications | Bearer Token |
| `GET` | `/api/health` | Service uptime and database connection state | No |

---

## 📁 Project Directory Map

```
capstone-project/
├── build.bat / build.sh         # Top-level full-stack build scripts
├── run.bat / run.sh             # Top-level application runners
├── Dockerfile                   # Multi-stage production container
├── docker-compose.yml           # Unified local/cloud stack runner
├── DEPLOYMENT.md                # Cloud deployment master guide
├── NETLIFY_DEPLOYMENT.md        # Netlify frontend deployment guide
├── RENDER_DEPLOYMENT.md         # Render backend blueprint guide
├── backend/
│   ├── Main.java                # Web server bootstrap & interactive CLI console
│   ├── WebServer.java           # Embedded HTTP server, REST endpoints & static host
│   ├── TestRunner.java          # Automated unit & integration test suite
│   ├── db/                      # Database pool manager & seed orchestrators
│   ├── model/                   # Domain entities (User, Product, Order, CartItem, etc.)
│   ├── service/                 # Business logic & ACID SQL transaction layer
│   ├── lib/                     # JDBC drivers (PostgreSQL 42.7.4)
│   └── resources/               # database.properties configuration
├── frontend/
│   ├── src/App.jsx              # Complete React Single-Page Application
│   ├── css/style.css            # Custom glassmorphic styles & design system
│   ├── dist/                    # Compiled SPA bundle served by Java backend
│   ├── package.json             # React 18, Vite 5, React Router 6 dependencies
│   └── vite.config.js           # Vite dev proxy configuration
├── amazon-capstone/             # 1,000-product image dataset pipeline & SQL seeds
└── docs/                        # Architecture diagrams, ERD, PDF reports & guides
```

---

## 📄 Academic & Submission Documents

- 🎓 **[BuyIt Database Report (PDF)](BuyIt_Database_Report.pdf)** — Academic report with Page 1 ER diagram, table schemas, row counts, and JOIN execution outputs.
- 📕 **[BuyIt Complete Technical Documentation (PDF)](BuyIt_Project_Documentation.pdf)** — Full 15-chapter publication-ready report.
- 📑 **[BuyIt Activity Document (PDF)](BuyIt_Activity_Document.pdf)** — Capstone development activity log & milestone breakdown.

---

## ⚖️ License

Distributed under the **MIT License**. See `LICENSE` for more information.

<div align="center">
  <sub>Built with ❤️ for the Capstone Project • 2026</sub>
</div>
