# Amazon Capstone Product Image Dataset & MySQL Integration Guide

This directory provides an organized, legally open-licensed product image pipeline with 1,000 target products for an Amazon-style capstone project.

---

## 1. Directory Structure

```
amazon-capstone/
├── README.md                      # Comprehensive guide and documentation
├── image_mapping.csv              # CSV mapping (image_id, product_id, category, filename)
├── seed_products.sql              # Ready-to-import MySQL seed script (1000 products & images)
├── fetch_open_images.js           # Automated fetcher from Wikimedia Commons Open Archive
├── import_local_dataset.js        # Importer/organizer for downloaded Kaggle/HuggingFace datasets
├── generate_mysql_seed.js         # Script to regenerate seed_products.sql from CSV
└── product-images/
    ├── electronics/               # product_0001.jpg - product_0067.jpg
    ├── mobiles/                   # product_0068.jpg - product_0134.jpg
    ├── laptops/                   # product_0135.jpg - product_0201.jpg
    ├── headphones/                # product_0202.jpg - product_0268.jpg
    ├── clothing/                  # product_0269.jpg - product_0335.jpg
    ├── shoes/                     # product_0336.jpg - product_0402.jpg
    ├── watches/                   # product_0403.jpg - product_0469.jpg
    ├── bags/                      # product_0470.jpg - product_0536.jpg
    ├── home-kitchen/              # product_0537.jpg - product_0603.jpg
    ├── beauty/                    # product_0604.jpg - product_0670.jpg
    ├── books/                     # product_0671.jpg - product_0736.jpg
    ├── toys/                      # product_0737.jpg - product_0802.jpg
    ├── sports/                    # product_0803.jpg - product_0868.jpg
    ├── grocery/                   # product_0869.jpg - product_0934.jpg
    └── appliances/                # product_0935.jpg - product_1000.jpg
```

---

## 2. Top Recommended Open-Licensed Datasets

All datasets listed below are **100% legal to use**, have verified open licenses, and avoid any copyrighted Amazon scraping:

| Dataset Name | Source Platform | License | Image Count & Format | Categories Covered |
| :--- | :--- | :--- | :--- | :--- |
| **Fashion Product Images (Small)** | [Kaggle](https://www.kaggle.com/datasets/paramaggarwal/fashion-product-images-small) / [Hugging Face](https://huggingface.co/datasets/ceyda/fashion-products-small) | **CC BY-SA 4.0 / Public Domain** | 44,441 images (JPG) | Clothing, Shoes, Watches, Bags, Beauty, Accessories |
| **ecommerce_product_images_18K** | [Kaggle](https://www.kaggle.com/datasets/theblackmamba31/ecommerce-product-images-18k) | **Apache 2.0** | 18,175 images (JPG, 224x224) | Electronics, Footwear, Household, Clothing |
| **Grocery Store Dataset** | [GitHub (Marcus Klasson et al.)](https://github.com/marcklas/GroceryStoreDataset) | **CC BY 4.0** | 5,125 images (JPG) | 81 fine-grained grocery classes (Fruit, Veg, Pantry) |
| **Wikimedia Commons Product Archive** | [Wikimedia Commons](https://commons.wikimedia.org) | **CC0 / CC-BY / CC-BY-SA** | Millions (JPG/PNG) | All 15 categories (Mobiles, Laptops, Appliances, etc.) |
| **Qdrant H&M E-Commerce Products** | [Hugging Face](https://huggingface.co/datasets/Qdrant/hm_ecommerce_products) | **CC BY 4.0** | 105,100 products (JPG URLs) | Fashion, Footwear, Accessories, Bags |
| **Google Open Images V7** | [Google Open Images](https://storage.googleapis.com/openimages/web/index.html) | **CC BY 4.0 / CC BY 2.0** | 9M+ annotated images | All 15 consumer goods categories |

---

## 3. How to Populate the 1,000 Images

### Method A: Automated Download (One Command)
You can directly download all 1,000 images using the included fetcher script that queries Wikimedia Commons:
```bash
# Download 1,000 images across all 15 categories
node amazon-capstone/fetch_open_images.js

# Or test with 50 images first
node amazon-capstone/fetch_open_images.js --limit=50
```
This script automatically organizes images into the 15 category folders, names them `product_0001.jpg` to `product_1000.jpg`, and writes an `attribution_manifest.json` recording the exact CC license and author for compliance.

### Method B: Importing from a Kaggle/HuggingFace Zip
If you download a dataset archive from Kaggle or Hugging Face:
1. Extract the `.zip` archive to any temporary folder (e.g. `C:/temp/kaggle_dataset`).
2. Run the local importer script:
```bash
node amazon-capstone/import_local_dataset.js --source="C:/temp/kaggle_dataset" --count=1000
```
The importer scans the source images, maps them into the 15 categories, renames them sequentially from `product_0001.jpg` to `product_1000.jpg`, copies them into `product-images/<category>/`, and updates `image_mapping.csv`.

---

## 4. MySQL Products Table Integration

### Database Schema (DDL)
```sql
CREATE DATABASE IF NOT EXISTS capstone_db;
USE capstone_db;

-- 1. Categories Table
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Products Table
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    discount_percentage DECIMAL(5, 2) DEFAULT 0.00,
    stock_quantity INT NOT NULL DEFAULT 0,
    sku VARCHAR(50) UNIQUE,
    primary_image VARCHAR(500),
    status ENUM('ACTIVE', 'INACTIVE', 'OUT_OF_STOCK') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- 3. Product Images Table (1:Many relationship)
CREATE TABLE IF NOT EXISTS product_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
```

### Loading Data into MySQL

#### Option 1: Run the Generated Seed Script
```bash
mysql -u root -p capstone_db < amazon-capstone/seed_products.sql
```

#### Option 2: Bulk Import via `LOAD DATA INFILE`
You can import `image_mapping.csv` directly:
```sql
-- Temporary staging table
CREATE TEMPORARY TABLE temp_image_mapping (
    image_id INT,
    product_id INT,
    category VARCHAR(100),
    filename VARCHAR(255)
);

LOAD DATA INFILE 'd:/java/capstone/amazon-capstone/image_mapping.csv'
INTO TABLE temp_image_mapping
FIELDS TERMINATED BY ',' 
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(image_id, product_id, category, filename);

-- Populate product_images table
INSERT INTO product_images (id, product_id, image_url, is_primary)
SELECT 
    image_id, 
    product_id, 
    CONCAT('/product-images/', category, '/', filename),
    TRUE
FROM temp_image_mapping;
```

---

## 5. Connecting Frontend & Backend to Serve Images

In your Java backend or static web server, expose the `amazon-capstone/product-images` directory as a static resource path:

### Java Web Server route:
```java
// Map requests matching /product-images/* to the local directory
File imageFile = new File("amazon-capstone/product-images/" + category + "/" + filename);
if (imageFile.exists()) {
    // Send 200 OK with Content-Type: image/jpeg or image/png
}
```

### React / Frontend display:
```jsx
<img 
  src={`http://localhost:8080${product.primary_image}`} 
  alt={product.name} 
  className="product-card-img"
/>
```
