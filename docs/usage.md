# BuyIt Marketplace — Usage Guide

## Table of Contents
- [Quick Start](#quick-start)
- [Web Application Access](#web-application-access)
- [CLI Features & Menu](#cli-features--menu)
- [Automated Testing](#automated-testing)
- [Manual Compilation & Execution](#manual-compilation--execution)
- [Test Accounts](#test-accounts)
- [Documentation Index](#documentation-index)

---

## Quick Start

### Option A: Using Root Batch Scripts (Recommended)

From the project root (`d:\java\capstone`):
```bat
:: Build React frontend and compile Java backend
build.bat

:: Start the application
run.bat
```
The server starts at `http://localhost:8080` and automatically opens your default browser.

### Option B: Using Backend Scripts

```bat
cd backend
build.bat
run.bat
```

---

## Web Application Access

Once the server is running, navigate to:
**`http://localhost:8080`**

The single Java server serves both:
1. The built React 18 single-page application (storefront, vendor portal, admin dashboard).
2. All `/api/*` REST endpoints and `/product-images/*` static assets.

---

## CLI Features & Menu

When running `java -cp "out;backend\lib\postgresql-42.7.4.jar" Main`, you can interact with the CLI management menu directly in the console:

```
--------------------------------------------------
              BUYIT CLI MANAGEMENT MENU            
--------------------------------------------------
 1. View available products
 2. Add a product
 3. Find a product by ID
 4. Remove a product by ID
 5. View users
 6. Add a user
 7. Find a user by ID
 8. Create a customer order with multiple items
 9. View order history
 10. Find an order by ID
 0. Exit CLI
--------------------------------------------------
```

### CLI Operations Summary
- **1. View products:** Lists first 25 products from the 1,000-product catalog with live pricing, stock, and vendor name.
- **2. Add product:** Prompts for vendor ID, product name, price, stock, and SKU; inserts into the database.
- **3. Find product by ID:** Displays full details including rating, category, brand, and discounts.
- **4. Remove product by ID:** Removes product from the catalog.
- **5. View users:** Displays all registered users with their roles (Customer, Vendor, Admin) and active statuses.
- **6. Add user:** Creates a new customer, vendor, or administrator.
- **7. Find user by ID:** Displays user contact and role profile.
- **8. Create order with multiple items:** Prompts for Customer ID, lets you add multiple products and quantities with stock validation, and creates an order atomically within a database transaction.
- **9. View order history:** Lists placed orders with timestamps, status, item counts, and totals.
- **10. Find order by ID:** Shows order breakdown, customer details, and individual line items.

---

## Automated Testing

Run the automated test suite anytime to verify database connectivity, models, services, transactions, and REST helpers:

```bat
java -cp "out;backend\lib\postgresql-42.7.4.jar" TestRunner
```

Expected output:
```
==================================================
     BuyIt Marketplace Automated Test Suite       
==================================================
[TEST] Database Connection & Table Integrity ... PASSED ✓
[TEST] Product Catalog & Search Operations ... PASSED ✓
[TEST] User Roles, Authentication & Polymorphism ... PASSED ✓
[TEST] Cart & Wishlist Operations ... PASSED ✓
[TEST] Transactional Order Placement & Atomic Stock ... PASSED ✓
[TEST] JSON Serialization & Parsing Helpers ... PASSED ✓
==================================================
Test Results: 6 Passed, 0 Failed (Total: 6)
==================================================
```

---

## Manual Compilation & Execution

If you prefer to compile manually using `javac`:

```powershell
cd d:\java\capstone

# 1. Compile backend classes into out/
javac -cp "backend\lib\postgresql-42.7.4.jar" -d out backend\*.java backend\model\*.java backend\service\*.java backend\db\*.java

# 2. Copy configuration files to out/
if (!(Test-Path out\resources)) { New-Item -ItemType Directory -Path out\resources }
Copy-Item backend\resources\* out\resources\ -Recurse -Force
Copy-Item backend\resources\database.properties out\database.properties -Force

# 3. Run the application
java -cp "out;backend\lib\postgresql-42.7.4.jar" Main
```

---

## Test Accounts

| Role | Email | Password | Features |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@buyit.com` | `Admin@123` | Platform KPIs, manage all users, products, categories, brands, orders |
| **Vendor** | `vendor1@buyit.com` | `Vendor@123` | Merchant dashboard, manage store inventory, process store orders |
| **Customer** | `customer@buyit.com` | `Customer@123` | Browse catalog, product details, cart, wishlist, checkout, orders |

---

## Documentation Index

- [Project README](../README.md)
- [System Architecture & UML Diagrams](diagrams.md)
- [Entity-Relationship Diagram (ERD)](erd.md)
- [Comprehensive Study Guide](study-guide.md)
- [Presentation Slides Outline](slides.md)
