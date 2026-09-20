# 🌐 Deploying BuyIt Frontend to Netlify

This guide provides end-to-end instructions for deploying the **BuyIt React Frontend Single-Page Application (SPA)** to **[Netlify](https://www.netlify.com)** and connecting it to your **Render Backend**.

---

## 🌟 Why Netlify for the Frontend?
- **Global Edge CDN:** Instant global asset distribution with lightning-fast load times.
- **Continuous Deployment:** Automatically triggers a production build on every `git push`.
- **Automatic HTTPS/SSL:** Free TLS certificates.
- **Client-Side Routing Support:** Fully configured `_redirects` and `netlify.toml` so React Router paths (e.g. `/products`, `/cart`, `/admin`) work on direct navigation and refresh without 404s.
- **Zero-Config Environment Variables:** Set `VITE_API_URL` to point to your live Render backend API.

---

## 🚀 Option 1: Deploy via Netlify Web UI (Recommended - 3 Steps)

### Step 1: Push Your Project to GitHub
Ensure all your latest frontend changes are pushed:
```bash
git add .
git commit -m "feat: configure frontend for Netlify deployment"
git push origin main
```

### Step 2: Import Your Repository on Netlify
1. Log in to **[https://app.netlify.com](https://app.netlify.com)** (sign in with your GitHub account).
2. Click **"Add new site"** -> **"Import an existing project"**.
3. Choose **"GitHub"** and select your repository: `idea07-byte/capstone-project`.
4. Netlify will automatically detect [`netlify.toml`](netlify.toml). Verify the build settings:
   - **Base directory:** `frontend`
   - **Build command:** `npm run build`
   - **Publish directory:** `frontend/dist` (or `dist` if base is `frontend`)

### Step 3: Set Your Backend API URL Environment Variable
1. In the deployment configuration (or under **Site configuration** -> **Environment variables**), add:
   - **Key:** `VITE_API_URL`
   - **Value:** `https://<your-backend-service>.onrender.com` (your live Render backend URL)
2. Click **"Deploy BuyIt"** (or **"Deploy site"**).

---

## 💻 Option 2: Deploy via Netlify CLI (Direct Terminal Deploy)

If you want to build and deploy immediately from your computer without configuring Git on Netlify:

### 1. Build the Production Bundle
From the project root:
```bash
cd frontend
npm install
npm run build
```

### 2. Deploy Using Netlify CLI
```bash
# Login to Netlify (opens browser authentication)
npx netlify login

# Deploy production bundle directly
npx netlify deploy --dir=dist --prod
```
Netlify will prompt you to create or link a site, upload the pre-built `dist/` directory, and output your live production URL (e.g. `https://buyit-marketplace.netlify.app`).

---

## ⚙️ How Frontend Connects to the Render Backend

The frontend is programmed to dynamically resolve the API and image assets:

```javascript
// Automatically reads your Render backend URL from Netlify environment variables
const API_BASE = (import.meta.env.VITE_API_URL || '').replace(/\/+$/, '');

// All REST endpoints automatically route to Render backend:
// ${API_BASE}/api/products
// ${API_BASE}/api/auth/login
// ${API_BASE}/api/cart
// ${API_BASE}/product-images/...
```

---

## 🩺 Verifying Your Live Deployment

1. Open your Netlify site URL (e.g. `https://buyit-marketplace.netlify.app`).
2. Open Browser DevTools (`F12` -> **Network** tab).
3. Check the API requests:
   - `GET https://<your-backend>.onrender.com/api/products` (HTTP 200)
   - `GET https://<your-backend>.onrender.com/api/categories` (HTTP 200)
4. Verify refreshing dynamic routes:
   - Navigate to `/products`, `/categories`, `/cart`, or `/admin` and press **Refresh (F5)** to verify that Netlify loads the SPA without 404 errors.

---

## 🔑 Demo Login Accounts

| Role | Email | Password | Access Portal |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@buyit.com` | `Admin@123` | Full Admin Dashboard (`/admin`) |
| **Vendor** | `vendor1@buyit.com` | `Vendor@123` | Merchant Portal (`/vendor`) |
| **Customer** | `customer@buyit.com` | `Customer@123` | Shopping Experience |
