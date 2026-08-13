# BuyIt — Presentation Slides Outline

Suggested ~15 slides, 8–10 minutes. Each line under a slide = bullet point to put on the slide.

---

## 1. Title Slide
- **BuyIt** — Shop Management System
- Capstone Project
- Your Name · Your Class/Institution · Date

## 2. Agenda
- Problem & Goal
- Tech Stack
- System Architecture
- Database Design
- Backend (Models, Services, API)
- Frontend
- Key Workflow: Order Creation
- Demo
- Challenges & Improvements
- Q&A

## 3. Problem Statement
- Managing products, customers, and orders is messy with paper/spreadsheets
- Goal: one web app to browse products, place orders, and track stock & users
- Roles: Admin and Customer

## 4. Tech Stack
- **Backend:** Java 17+, JDBC, built-in HttpServer (no framework)
- **Database:** PostgreSQL on Supabase (cloud)
- **Frontend:** React 18 + Vite + React Router
- **Build/Run:** build.bat / run.bat, npm run build

## 5. System Architecture
- (Show diagram) Browser (React) ↔ Java WebServer (:8080) ↔ Service layer ↔ Supabase PostgreSQL
- Single server serves both the React app and JSON APIs

## 6. Database Design (ERD)
- (Show ERD)
- 4 tables: `users`, `products`, `orders`, `order_items`
- `order_items` = many-to-many bridge between orders and products
- Stores name/price snapshots to preserve order history

## 7. Backend — Models
- `User` (abstract) + `Customer` / `Admin` subclasses, `Role` enum
- `Product` with stock validation (`reduceQuantity`)
- `Order` with ≥1 item rule, `OrderItem` line totals
- Polymorphism: one `User` reference, two behaviours

## 8. Backend — Services
- `ProductService`: CRUD, stock check, atomic stock reduction
- `UserService`: CRUD, role-based mapping back to Admin/Customer
- `OrderService`: transactional order creation
- `Database`: auto-creates tables + seeds data on startup

## 9. REST API
- `/api/auth/login`, `/api/auth/register`
- `/api/products` (GET/POST/PUT/DELETE)
- `/api/orders` (GET/POST)
- `/api/users` (GET/POST/DELETE)

## 10. Frontend
- Pages: Login, Register, Home, Dashboard
- Dashboard tabs: Overview / Products / Orders / Users / Settings
- Modals for Add/Edit product, Create order, Add user
- localStorage holds the logged-in user

## 11. Key Workflow — Create Order
- (Show sequence diagram)
- Frontend collects customer + items → POST /api/orders
- One DB transaction: insert order → insert items → reduce stock
- Atomic stock check via `UPDATE ... WHERE quantity >= ?`
- Any failure → rollback (no partial orders)

## 12. Login Flow
- Backend matches email + password
- Frontend stores user, redirects to home page
- Both Admin and Customer access the same dashboard

## 13. Demo
- Run `backend/run.bat`, open http://localhost:8080
- Login as admin@example.com / adminpass
- Show: add a product, create an order, watch stock drop
- Login as asha@example.com / pass1234 to show customer view

## 14. Challenges Faced
- Building JSON parsing by hand (no library)
- Manual ID generation vs database sequences
- Keeping order items consistent when products change
- Serving SPA + API from a single Java server

## 15. Future Improvements
- Hash passwords (bcrypt), real auth tokens
- Role-based access control
- Proper JSON library, DB sequences, input validation
- Automated tests and deployment

## 16. Thank You / Q&A
- Recap: full-stack shop system, 4-table DB, transactional orders
- Questions welcome
