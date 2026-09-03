/**
 * generate_mysql_seed.js
 * Generates seed_products.sql to populate MySQL products and product_images tables.
 */
const fs = require('fs');
const path = require('path');

const csvPath = path.join(__dirname, 'image_mapping.csv');
const outSqlPath = path.join(__dirname, 'seed_products.sql');

if (!fs.existsSync(csvPath)) {
  console.error('image_mapping.csv not found!');
  process.exit(1);
}

const lines = fs.readFileSync(csvPath, 'utf8').trim().split('\n');
const records = lines.slice(1).map(line => {
  const parts = line.split(',');
  return {
    imageId: parseInt(parts[0], 10),
    productId: parseInt(parts[1], 10),
    category: parts[2].trim(),
    filename: parts[3].trim()
  };
});

const categories = [
  { id: 1, name: 'electronics', display: 'Electronics', desc: 'Audio, gadgets, and consumer electronics' },
  { id: 2, name: 'mobiles', display: 'Mobiles & Accessories', desc: 'Smartphones, cases, and mobile gear' },
  { id: 3, name: 'laptops', display: 'Laptops & Computers', desc: 'Notebooks, ultrabooks, and PC accessories' },
  { id: 4, name: 'headphones', display: 'Headphones & Audio', desc: 'Over-ear, in-ear, and wireless earbuds' },
  { id: 5, name: 'clothing', display: 'Fashion & Clothing', desc: 'Men and women apparel, shirts, and jackets' },
  { id: 6, name: 'shoes', display: 'Footwear & Shoes', desc: 'Sneakers, formal shoes, and boots' },
  { id: 7, name: 'watches', display: 'Watches', desc: 'Analog, digital, and smart watches' },
  { id: 8, name: 'bags', display: 'Bags & Luggage', desc: 'Backpacks, handbags, travel duffels' },
  { id: 9, name: 'home-kitchen', display: 'Home & Kitchen', desc: 'Cookware, dining, kitchen essentials' },
  { id: 10, name: 'beauty', display: 'Beauty & Personal Care', desc: 'Skincare, perfumes, and cosmetics' },
  { id: 11, name: 'books', display: 'Books', desc: 'Fiction, non-fiction, textbooks, and bestsellers' },
  { id: 12, name: 'toys', display: 'Toys & Games', desc: 'Action figures, board games, and puzzles' },
  { id: 13, name: 'sports', display: 'Sports & Fitness', desc: 'Sporting goods, fitness equipment, activewear' },
  { id: 14, name: 'grocery', display: 'Grocery & Gourmet', desc: 'Pantry essentials, organic foods, snacks' },
  { id: 15, name: 'appliances', display: 'Appliances', desc: 'Kitchen and home electrical appliances' }
];

const catMap = {};
categories.forEach(c => { catMap[c.name] = c; });

const adjectives = ['Premium', 'Ultra', 'Pro', 'Classic', 'Modern', 'Compact', 'Elite', 'Essential', 'Wireless', 'Smart', 'Organic', 'Ergonomic', 'Durable'];
const baseNames = {
  'electronics': ['Bluetooth Speaker', 'Power Bank 20000mAh', 'USB-C Fast Hub', 'Streaming Mic', 'Digital Voice Recorder', 'Wireless Charger Pad'],
  'mobiles': ['5G Smartphone 128GB', 'Dual SIM Flagship Phone', 'Rugged Smartphone', 'Foldable Mobile Phone', 'Pro Camera Phone'],
  'laptops': ['15.6 Inch IPS Laptop', 'Slim Ultrabook 16GB RAM', 'Gaming Laptop RTX', 'Convertible 2-in-1 Laptop', 'Business Notebook'],
  'headphones': ['Noise Cancelling Headphones', 'True Wireless Earbuds', 'Studio Monitor Headphones', 'Sport Neckband Earphones'],
  'clothing': ['Cotton Slim Fit Shirt', 'Comfort Fleece Hoodie', 'Casual Denim Jacket', 'Breathable Polo T-Shirt', 'Formal Chino Trousers'],
  'shoes': ['Cushioned Running Shoes', 'Classic Leather Loafers', 'High-Top Canvas Sneakers', 'Lightweight Training Shoes'],
  'watches': ['Chronograph Stainless Watch', 'Minimalist Leather Watch', 'Sports GPS Smartwatch', 'Automatic Mechanical Watch'],
  'bags': ['Water-Resistant Backpack', 'Canvas Messenger Bag', 'Leather Crossbody Bag', 'Carry-On Travel Duffle'],
  'home-kitchen': ['Non-Stick Frying Pan 10 Inch', 'Stainless Steel Chef Knife', 'Cast Iron Dutch Oven', 'Insulated Thermal Flask 1L'],
  'beauty': ['Hydrating Face Serum', 'Gentle Foaming Cleanser', 'Eau De Parfum 100ml', 'Natural Botanical Moisture Cream'],
  'books': ['Complete Guide to Modern Computing', 'Chronicles of Adventure Novel', 'Data Structures & Algorithms Handbook'],
  'toys': ['Modular Building Blocks Set', 'Remote Control High-Speed Car', 'Strategic Wooden Board Game', 'Classic Wooden Puzzle Box'],
  'sports': ['Non-Slip Exercise Yoga Mat', 'Adjustable Dumbbell Set', 'Professional Match Football', 'Carbon Fiber Badminton Racket'],
  'grocery': ['Organic Roasted Almonds 500g', 'Pure Raw Honey 1kg', 'Extra Virgin Cold Pressed Olive Oil', 'Artisanal Dark Roast Coffee Beans'],
  'appliances': ['Digital Air Fryer 4.5L', 'Electric Variable Temp Kettle', 'High-Speed Smoothie Blender', 'Compact Countertop Microwave Oven']
};

let sql = '-- ========================================================\n';
sql += '-- MySQL Seed Script for Amazon Capstone Project\n';
sql += '-- Generated from image_mapping.csv (1000 Products & Images)\n';
sql += '-- ========================================================\n\n';

sql += 'USE capstone_db;\n\n';

sql += '-- 1. Insert Categories\n';
sql += 'INSERT INTO categories (id, name, display_name, description)\nVALUES\n';
const catValues = categories.map(c => '  (' + c.id + ', \'' + c.name + '\', \'' + c.display + '\', \'' + c.desc + '\')').join(',\n');
sql += catValues + '\nON DUPLICATE KEY UPDATE display_name=VALUES(display_name);\n\n';

sql += '-- 2. Insert Products (1000 items)\n';
sql += 'INSERT INTO products (id, category_id, name, description, price, stock_quantity, sku, primary_image)\nVALUES\n';

const productValues = records.map((rec, idx) => {
  const cat = catMap[rec.category] || { id: 1 };
  const adj = adjectives[idx % adjectives.length];
  const pool = baseNames[rec.category] || ['Product Item'];
  const nameBase = pool[idx % pool.length];
  const prodName = adj + ' ' + nameBase + ' (Model ' + String(rec.productId).padStart(4, '0') + ')';
  const desc = 'High quality ' + rec.category + ' product featuring durable build, optimal performance, and official warranty.';
  
  let basePrice = 29.99;
  if (rec.category === 'laptops') basePrice = 699.99;
  else if (rec.category === 'mobiles') basePrice = 499.99;
  else if (rec.category === 'appliances') basePrice = 129.99;
  else if (rec.category === 'electronics') basePrice = 59.99;
  else if (rec.category === 'watches') basePrice = 89.99;
  else if (rec.category === 'headphones') basePrice = 79.99;
  else if (rec.category === 'shoes') basePrice = 64.99;
  else if (rec.category === 'clothing') basePrice = 34.99;
  else if (rec.category === 'bags') basePrice = 44.99;
  else if (rec.category === 'home-kitchen') basePrice = 39.99;
  else if (rec.category === 'sports') basePrice = 35.99;
  else if (rec.category === 'toys') basePrice = 24.99;
  else if (rec.category === 'beauty') basePrice = 22.99;
  else if (rec.category === 'books') basePrice = 18.99;
  else if (rec.category === 'grocery') basePrice = 12.99;

  const price = (basePrice + ((idx * 7) % 40)).toFixed(2);
  const stock = 15 + ((idx * 13) % 185);
  const sku = 'SKU-' + rec.category.substring(0, 3).toUpperCase() + '-' + String(rec.productId).padStart(5, '0');
  const imgPath = '/product-images/' + rec.category + '/' + rec.filename;

  return '  (' + rec.productId + ', ' + cat.id + ', \'' + prodName + '\', \'' + desc + '\', ' + price + ', ' + stock + ', \'' + sku + '\', \'' + imgPath + '\')';
});

sql += productValues.join(',\n');
sql += '\nON DUPLICATE KEY UPDATE name=VALUES(name), price=VALUES(price), primary_image=VALUES(primary_image);\n\n';

sql += '-- 3. Insert Product Images (1000 items mapped to products table)\n';
sql += 'INSERT INTO product_images (id, product_id, image_url, is_primary)\nVALUES\n';

const imgValues = records.map(rec => {
  const imgPath = '/product-images/' + rec.category + '/' + rec.filename;
  return '  (' + rec.imageId + ', ' + rec.productId + ', \'' + imgPath + '\', TRUE)';
});

sql += imgValues.join(',\n');
sql += '\nON DUPLICATE KEY UPDATE image_url=VALUES(image_url);\n';

fs.writeFileSync(outSqlPath, sql, 'utf8');
console.log('Generated ' + outSqlPath + ' with ' + records.length + ' products and images.');
