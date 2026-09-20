# 🚀 Deploying BuyIt Backend & Marketplace to Render

This guide provides step-by-step instructions to deploy the **BuyIt Multi-Vendor E-Commerce Backend** (and full-stack application) to **[Render.com](https://render.com)**.

---

## 🌟 Why Render?
- **Automated Git Deployments:** Every push to `main` triggers a zero-downtime rebuild.
- **Blueprint Support (`render.yaml`):** 1-click infrastructure as code deployment.
- **Docker Multi-Stage Build:** Automatically builds both the high-performance Java backend & Vite React SPA.
- **Built-in Free Tier:** Free web service with automatic HTTPS/TLS certificates.
- **Health Check Monitoring:** Automatically pings `/api/health` to verify server status.

---

## 📋 Option 1: 1-Click Blueprint Deploy (Recommended)

### Step 1: Push Your Code to GitHub
Make sure your latest code is committed and pushed:
```bash
git add .
git commit -m "feat: configure backend for Render deployment"
git push origin main
```

### Step 2: Create Blueprint on Render
1. Go to **[https://render.com](https://render.com)** and sign in with GitHub.
2. In the top-right header, click **"New +"** and select **"Blueprint"**.
3. Connect your repository: `idea07-byte/capstone-project` (or your repo name).
4. Render will automatically read [`render.yaml`](render.yaml) and populate the service settings:
   - **Service Name:** `buyit-marketplace`
   - **Runtime:** `Docker`
   - **Plan:** `Free`
   - **Region:** `Oregon (US West)` (or your preferred region)
   - **Health Check Path:** `/api/health`
5. Click **"Apply"** to start the build and deployment.

---

## 🛠️ Option 2: Deploy as a Manual Web Service

If you prefer setting up the Web Service manually:

1. In Render, click **"New +"** -> **"Web Service"**.
2. Select **"Build and deploy from a Git repository"** and connect your repo.
3. Configure the following fields:
   - **Name:** `buyit-marketplace`
   - **Region:** Any (e.g. `Oregon`, `Frankfurt`, `Singapore`)
   - **Branch:** `main`
   - **Language / Runtime:** **`Docker`**
   - **Dockerfile Path:** `./Dockerfile` (or `backend/Dockerfile` if deploying backend-only)
   - **Instance Type:** **Free**
4. Under **Advanced Settings**:
   - **Health Check Path:** `/api/health`
   - **Auto-Deploy:** `Yes`
5. Click **"Create Web Service"**.

---

## 🗄️ Database Configuration (Optional)

BuyIt includes a pre-configured, live cloud PostgreSQL database on Supabase out of the box.

If you wish to attach a **Render Managed PostgreSQL** database instead:
1. In Render, click **"New +"** -> **"PostgreSQL"**.
2. Give it a name (e.g. `buyit-db`) and click **"Create Database"**.
3. Copy the **Internal Database URL** (or External Database URL).
4. In your Web Service settings -> **"Environment"** tab:
   - Add environment variable:
     - `DATABASE_URL` = `<pasted PostgreSQL URL>`
5. Click **"Save Changes"**. BuyIt will automatically detect the database, create all required tables, and seed initial catalog data on startup!

---

## 🩺 Verifying Your Live Deployment

Once the build finishes (typically ~2-3 minutes), Render will assign a public HTTPS URL (e.g. `https://buyit-marketplace-xxxx.onrender.com`).

You can verify the backend endpoints:

1. **System & Database Health Check:**
   ```
   GET https://<your-app>.onrender.com/api/health
   ```
   *Expected Response (HTTP 200):*
   ```json
   {
     "status": "UP",
     "timestamp": 1740000000000,
     "port": 10000,
     "database": "connected",
     "service": "BuyIt Marketplace"
   }
   ```

2. **Products Catalog API:**
   ```
   GET https://<your-app>.onrender.com/api/products
   ```

3. **Categories API:**
   ```
   GET https://<your-app>.onrender.com/api/categories
   ```

4. **Default Admin Login Credentials:**
   - **Email:** `admin@buyit.com`
   - **Password:** `Admin@123`

---

## ⚙️ Environment Variables Reference

| Variable | Description | Default / Render Setting |
| :--- | :--- | :--- |
| `PORT` | Web server listening port | `10000` (Render default) |
| `DATABASE_URL` | PostgreSQL connection URL | Supabase default cloud instance |
| `JAVA_OPTS` | JVM performance flags | `-Djava.awt.headless=true -XX:+UseG1GC -XX:MaxRAMPercentage=75.0` |
| `WEB_ROOT` | Optional custom frontend assets path | `./frontend/dist` |
| `IMAGE_ROOT` | Optional custom images directory | `./amazon-capstone/product-images` |
