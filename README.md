# BuyIt

A shop system with products, users, and orders, split into a Java backend and a React frontend.

## Structure

- `backend/` - Java backend (models, services, CLI, and web server)
- `frontend/` - React frontend (Vite build)

## Prerequisites

- Java 17+ (tested on Java 25)
- Supabase (PostgreSQL) project
- PostgreSQL JDBC Driver `postgresql-42.7.4.jar` (included in `backend/lib/`)

## Database Setup

1. Point `backend/resources/database.properties` at your Supabase project:
   - URL: `jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require`
   - User: `postgres`
   - Password: your Supabase database password
2. Create the tables and seed data. Run the schema in the Supabase SQL Editor (Dashboard -> SQL Editor), or from the project:
   ```sql
   -- paste contents of backend/database/schema.sql
   ```
   The backend also auto-creates the tables on startup via `db/Database.java`.

## Running the backend

### Windows
```bash
cd backend
build.bat
run.bat
```

### Manual
```bash
cd backend
javac -cp lib\postgresql-42.7.4.jar -d ../out Main.java WebServer.java model\*.java service\*.java db\*.java
java -cp ../out;lib\postgresql-42.7.4.jar Main
```

The web server serves the built React app from `frontend/dist` at http://localhost:8080.

## Running the frontend

```bash
cd frontend
npm install
npm run dev      # development server on http://localhost:3000
npm run build    # production build served by the Java web server
```

## Features

- Manage products with stock tracking
- Manage customers and admin users
- Create customer orders with multiple items
- Find products, users, and orders by ID

## Documentation

- [Usage guide](docs/usage.md)
- [System diagrams](docs/diagrams.md)
- [ER diagram reference](docs/erd.md)
