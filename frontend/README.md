# Shop Capstone - React Web Interface

## Overview
This folder contains the React-based login and dashboard interface for the Shop Capstone project.

## File Structure
```
frontend/
├── index.html              # Vite entry point
├── package.json            # Dependencies and scripts
├── vite.config.js          # Vite configuration with API proxy
├── css/
│   ├── style.css          # Main styles (login, navbar, modals)
│   └── dashboard.css      # Dashboard specific styles
└── src/
    ├── main.jsx           # React entry point
    └── App.jsx            # Main application logic, routes, and API handlers
```

## Features

### 1. Login Page
- Modern dark theme UI with blue gradient scheme
- Email and password authentication
- Remember me functionality
- Forgot password link
- Register link for new users
- Form validation and error handling

### 2. Registration Page
- Full name, email, phone, and password fields
- Password confirmation validation
- Terms & Conditions checkbox
- Email format validation
- Phone number validation

### 3. Dashboard
- User profile sidebar with role display
- Navigation menu for different sections:
  - **Overview**: Dashboard statistics and cards
  - **Products**: Product management table with add/edit/delete
  - **Orders**: Order management and creation
  - **Users**: User management with add/delete
  - **Settings**: User profile settings
- Responsive design for mobile and desktop

## Test Credentials

### Pre-seeded Users:
```
Email: admin@example.com
Password: adminpass
Role: Admin

Email: asha@example.com
Password: pass1234
Role: Customer
```

## How to Use

### Development
```bash
cd frontend
npm install
npm run dev
```

Then open: `http://localhost:3000`

### Production Build
```bash
cd frontend
npm run build
```

The built app is served by the Java backend at `http://localhost:8080`.

## API Endpoints

The frontend communicates with the Java backend via REST API:

- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `GET /api/products` - List all products
- `POST /api/products` - Create product
- `PUT /api/products/:id` - Update product
- `DELETE /api/products/:id` - Delete product
- `GET /api/orders` - List all orders
- `POST /api/orders` - Create order
- `GET /api/users` - List all users
- `POST /api/users` - Create user
- `DELETE /api/users/:id` - Delete user

## Color Scheme
- **Primary**: #1a3a52 (Dark Blue)
- **Secondary**: #64b5f6 (Light Blue)
- **Accent**: #2d5a7b (Medium Blue)
- **Background**: Dark gradient theme

## Styling Notes
- All forms use glassmorphism effect with backdrop blur
- Smooth transitions and hover effects throughout
- Fully responsive design (mobile, tablet, desktop)
- Dark mode friendly color palette

## Browser Compatibility
- Chrome/Edge: Full support
- Firefox: Full support
- Safari: Full support
