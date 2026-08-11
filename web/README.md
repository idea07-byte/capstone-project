# Shop Capstone - Web Interface

## Overview
This folder contains the complete web-based login and dashboard interface for the Shop Capstone project.

## File Structure
```
web/
├── index.html              # Login page
├── register.html           # Registration page
├── dashboard.html          # Admin/User dashboard
├── css/
│   ├── style.css          # Main styles (login, navbar, modals)
│   └── dashboard.css      # Dashboard specific styles
└── js/
    ├── app.js             # Main application logic & login handler
    ├── dashboard.js       # Dashboard functionality
    └── register.js        # Registration form handler
```

## Features

### 1. Login Page (index.html)
- Modern forest-themed UI with dark blue color scheme
- Email and password authentication
- Remember me functionality
- Forgot password link
- Register link for new users
- Form validation and error handling

### 2. Registration Page (register.html)
- Full name, email, phone, and password fields
- Password confirmation validation
- Terms & Conditions checkbox
- Email format validation
- Phone number validation

### 3. Dashboard (dashboard.html)
- User profile sidebar with role display
- Navigation menu for different sections:
  - **Overview**: Dashboard statistics and cards
  - **Products**: Product management table
  - **Orders**: Order management and tracking
  - **Users**: User management
  - **Settings**: User profile settings
- Responsive design for mobile and desktop

## Test Users (Mock)

### Demo Credentials:
```
Email: admin@shop.com
Password: admin123
Role: Admin

Email: customer@shop.com
Password: customer123
Role: Customer

Email: test@shop.com
Password: test123
Role: Customer
```

## How to Use

### Option 1: Run Locally with a Simple HTTP Server
```bash
# Using Python 3
python -m http.server 8000

# Using Python 2
python -m SimpleHTTPServer 8000

# Using Node.js (install http-server globally first)
npx http-server
```

Then open: `http://localhost:8000`

### Option 2: Integrate with Java Backend
To integrate with your Java services, update the `js/app.js` file:

1. Modify the `loginUser()` function to call your Java backend API:
```javascript
fetch('http://your-server:port/api/auth/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify({
        email: email,
        password: password
    })
})
.then(response => response.json())
.then(data => {
    // Handle authentication response
})
```

2. Update mock data in `js/dashboard.js` with real API calls to:
   - `/api/products`
   - `/api/orders`
   - `/api/users`

## Color Scheme
- **Primary**: #1a3a52 (Dark Blue)
- **Secondary**: #64b5f6 (Light Blue)
- **Accent**: #2d5a7b (Medium Blue)
- **Background**: Forest gradient (custom SVG)

## Styling Notes
- All forms use glassmorphism effect with backdrop blur
- Smooth transitions and hover effects throughout
- Fully responsive design (mobile, tablet, desktop)
- Dark mode friendly color palette

## Browser Compatibility
- Chrome/Edge: Full support
- Firefox: Full support
- Safari: Full support
- IE11: Limited support (consider modernizing)

## Future Enhancements
- [ ] Backend API integration
- [ ] JWT token authentication
- [ ] Real database connectivity
- [ ] Email verification
- [ ] Two-factor authentication
- [ ] User profile avatars
- [ ] Notification system
- [ ] Search and filter functionality
- [ ] Data export (PDF, CSV)
- [ ] Dark/Light theme toggle

## Security Considerations
⚠️ **Note**: This is a frontend mockup. For production:
1. Never store passwords in localStorage
2. Use HTTP-only cookies for session tokens
3. Implement proper server-side validation
4. Use HTTPS only
5. Implement rate limiting on login attempts
6. Add CSRF protection
7. Sanitize all user inputs

## Customization

### Change Colors
Edit `css/style.css` and update the color variables:
- `.navbar` background
- `.modal-content` styling
- `.login-button` background
- `.card` styling

### Change Branding
Update the logo text "Shop" in `index.html`, `register.html`, and `dashboard.html`

### Add More Menu Items
Add new `<li>` items to `.sidebar-menu` in `dashboard.html` and create corresponding sections with `.content-section` class.

## Support
For integration with your Java backend services (UserService, ProductService, OrderService), ensure your Java application serves this web folder or configure CORS appropriately.
