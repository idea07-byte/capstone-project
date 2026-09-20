# ☁️ BuyIt Cloud Deployment Guide

This guide provides end-to-end instructions for deploying the **BuyIt Multi-Vendor E-Commerce Marketplace** to modern cloud hosting platforms.

---

## 🏛️ Cloud Architecture Overview

BuyIt is engineered with a high-performance **unified cloud architecture**:
- **Single Containerized Service:** The Java HTTP server serves the compiled React Single-Page Application (`frontend/dist`), product image galleries (`amazon-capstone/product-images`), and the JSON REST APIs on a single port.
- **Zero CORS Issues:** Everything operates under the same origin domain.
- **Pre-Configured Cloud Database:** Connects to a cloud-hosted PostgreSQL database on **Supabase** (`db.wcoivrmtfvlcpwerhjwn.supabase.co`) with automatic connection pooling and reconnect retries.
- **12-Factor Cloud Compliant:** Accepts dynamic cloud environment variables (`PORT`, `DATABASE_URL`, `DB_URL`, `DB_USER`, `DB_PASSWORD`) while falling back seamlessly to default configurations.
- **Built-in Health Checks:** Serves `/api/health` and `/health` for zero-downtime rolling deploys and cloud orchestrator monitoring.

---

## 🚀 Recommended Platforms

| Platform | Free Tier / Cost | Setup Difficulty | Build Type | Best For |
| :--- | :--- | :--- | :--- | :--- |
| **[Netlify (Frontend)](#1-deploying-frontend-to-netlify-recommended)** | Free Tier | ⭐ Very Easy | React SPA / Static | **High-Speed Global Edge Frontend** |
| **[Render (Backend / Full-Stack)](#2-deploying-to-render-recommended)** | Free / Low cost | ⭐ Very Easy | Docker (Automated) | **Capstone Projects & Portfolios (Recommended)** |
| **[Google Cloud Run](#3-deploying-to-google-cloud-run)** | Generous Free Tier | ⭐⭐ Medium | Serverless Container | Enterprise Scalability |
| **[Fly.io](#4-deploying-to-flyio)** | Free / Low cost | ⭐⭐ Medium | Docker (`flyctl`) | Global Edge Hosting |
| **[Self-Hosted VPS](#5-deploying-to-a-vps-with-docker-compose)** | $4–$6/month VPS | ⭐⭐ Medium | Docker Compose | Complete Control (Ubuntu/Debian) |

---

## 1. Deploying Frontend to Netlify (Recommended)

Netlify hosts the React Single-Page Application on global CDN edge nodes with instant HTTPS and client-side routing. See [NETLIFY_DEPLOYMENT.md](NETLIFY_DEPLOYMENT.md).

### Quick Steps:
1. Go to **[https://app.netlify.com](https://app.netlify.com)** and import your GitHub repository: `idea07-byte/capstone-project`.
2. Set **Base directory:** `frontend`, **Build command:** `npm run build`, **Publish directory:** `frontend/dist`.
3. Add Environment Variable: `VITE_API_URL` = `https://<your-backend>.onrender.com`
4. Click **"Deploy site"**.

---

## 2. Deploying to Render (Recommended)

Render offers the easiest way to deploy Docker containers directly from GitHub with automatic HTTPS certificates and continuous deployment. See also [RENDER_DEPLOYMENT.md](RENDER_DEPLOYMENT.md).

### Step-by-Step Instructions:

1. **Commit and push your project to GitHub:**
   ```bash
   git add .
   git commit -m "feat: configure cloud deployment readiness, Dockerfile, and blueprints"
   git push origin main
   ```

2. **Log in to Render:**
   - Go to **[https://render.com](https://render.com)** and sign in (using your GitHub account).

3. **Deploy via Blueprint (One-Click):**
   - Click **"New +"** in the top navigation bar.
   - Select **"Blueprint"**.
   - Connect your GitHub repository: `idea07-byte/capstone-project`.
   - Render will detect [`render.yaml`](render.yaml) automatically.
   - Click **"Apply"**.

4. **Alternative: Deploy as a Web Service:**
   - Click **"New +"** -> **"Web Service"**.
   - Choose your repository `idea07-byte/capstone-project`.
   - Select **Docker** as the Runtime.
   - Set **Instance Type** to **Free**.
   - In **Health Check Path**, enter: `/api/health`.
   - Click **"Create Web Service"**.

5. **Access Your Live App:**
   - Render will build the multi-stage Docker image (~2 minutes) and deploy it.
   - Once the build log shows `BuyIt Marketplace Server Started`, your app will be live at:
     `https://buyit-marketplace-xxxx.onrender.com`

---

## 2. Deploying to Google Cloud Run

Google Cloud Run provides fully managed serverless containers with automatic SSL and zero-cost scaling when idle.

### Prerequisites:
- Google Cloud SDK (`gcloud` CLI) installed and authenticated:
  ```bash
  gcloud auth login
  gcloud config set project YOUR_GCP_PROJECT_ID
  ```

### Deploy Command:
Run the following single command from the project root:
```bash
gcloud run deploy buyit-marketplace \
  --source . \
  --region us-central1 \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --memory 512Mi \
  --cpu 1
```

Once the deployment finishes, Cloud Run outputs the live HTTPS service URL (e.g., `https://buyit-marketplace-xyz-uc.a.run.app`).

---

## 3. Deploying to Fly.io

1. Install the `flyctl` CLI:
   - Windows: `pwsh -Command "iwr https://fly.io/install.ps1 -useb | iex"`
   - Mac/Linux: `curl -L https://fly.io/install.sh | sh`
2. Authenticate:
   ```bash
   fly auth login
   ```
3. Launch and deploy using the included [`fly.toml`](fly.toml):
   ```bash
   fly launch --no-deploy
   fly deploy
   ```
4. Open the application:
   ```bash
   fly open
   ```

---

## 4. Deploying to a VPS with Docker Compose

If you have a Linux VPS (DigitalOcean Droplet, AWS EC2, Linode, or Hetzner):

1. **Install Docker and Docker Compose on your server:**
   ```bash
   sudo apt-get update && sudo apt-get install -y docker.io docker-compose-v2
   ```

2. **Clone the repository:**
   ```bash
   git clone https://github.com/idea07-byte/capstone-project.git
   cd capstone-project
   ```

3. **Start the container:**
   ```bash
   docker compose up -d --build
   ```

4. **Verify running status:**
   ```bash
   docker ps
   curl http://localhost:8080/api/health
   ```

---

## 🔧 Environment Variables Reference

All environment variables are optional. If omitted, the server uses standard defaults:

| Variable | Description | Default Value | Example |
| :--- | :--- | :--- | :--- |
| `PORT` | Listening HTTP port for the web server | `8080` | `10000` |
| `DATABASE_URL` | Standard PostgreSQL connection URI | Supabase DB | `postgresql://user:pass@host:5432/dbname` |
| `DB_URL` | JDBC-formatted database URL | Supabase DB | `jdbc:postgresql://host:5432/dbname?sslmode=require` |
| `DB_USER` | Database username | `postgres` | `postgres` |
| `DB_PASSWORD`| Database password | *(Pre-configured)* | `your_secure_password` |
| `WEB_ROOT` | Custom path to compiled React `dist/` | `./frontend/dist` | `/app/frontend/dist` |
| `IMAGE_ROOT`| Custom path to product images catalog | `./amazon-capstone/product-images` | `/app/amazon-capstone/product-images` |

---

## 🧪 Verifying the Deployment

### 1. Health Check Endpoint
Query the `/api/health` endpoint:
```bash
curl -i https://YOUR_DEPLOYED_URL/api/health
```
Expected Response:
```json
{
  "status": "UP",
  "timestamp": 1726329600000,
  "port": 8080,
  "database": "connected",
  "service": "BuyIt Marketplace"
}
```

### 2. Login & Test Credentials
Verify the three core roles on your live deployment:

| Role | Email | Password | What to Verify |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@buyit.com` | `Admin@123` | Analytics cards, user management, category controls (`/admin`) |
| **Vendor** | `vendor1@buyit.com` | `Vendor@123` | Vendor dashboard, catalog items, order fulfillment (`/vendor`) |
| **Customer** | `customer@buyit.com` | `Customer@123` | Browsing 1,000 products, cart, wishlist, checkout (`/store`) |

---

## 🛠️ Troubleshooting

### Cold Starts / First Load
- On free tiers (like Render Free), services spin down after 15 minutes of inactivity. The first request may take ~30–45 seconds to wake up the container.

### Database Connection Timeout
- The backend features built-in 3-attempt exponential backoff retry and a connection pool of 12 connections. If your database provider enforces SSL, ensure `?sslmode=require` is appended to the connection string.

### Resetting or Re-seeding Catalog
- The database auto-detects existing products and preserves them. If you ever need to reset to fresh catalog data, simply run:
  ```bash
  java -cp "out;backend/lib/postgresql-42.7.4.jar" db.SeedPostgresRunner
  ```
