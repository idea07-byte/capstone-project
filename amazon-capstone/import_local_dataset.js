/**
 * import_local_dataset.js
 * 
 * Imports and organizes images from an already downloaded local dataset (e.g. from Kaggle,
 * Hugging Face, or Roboflow) into the 15 category folders of amazon-capstone/product-images/.
 * 
 * It renames files sequentially (product_0001.jpg ... product_1000.jpg), places them in
 * their appropriate category folder, and generates/updates image_mapping.csv.
 * 
 * Usage:
 *   node amazon-capstone/import_local_dataset.js --source="C:/Users/Downloads/my_kaggle_dataset"
 *   node amazon-capstone/import_local_dataset.js --source="./temp_download" --copy
 */

const fs = require('fs');
const path = require('path');

const TARGET_DIR = path.join(__dirname, 'product-images');
const CSV_PATH = path.join(__dirname, 'image_mapping.csv');

const CATEGORIES = [
  'electronics', 'mobiles', 'laptops', 'headphones',
  'clothing', 'shoes', 'watches', 'bags',
  'home-kitchen', 'beauty', 'books', 'toys',
  'sports', 'grocery', 'appliances'
];

// Fuzzy alias map for folder/category names found in common datasets
const ALIAS_MAP = {
  'electronics': ['electronics', 'gadgets', 'electronic', 'audio', 'cables', 'chargers', 'accessories'],
  'mobiles': ['mobiles', 'mobile', 'smartphones', 'smartphone', 'phones', 'phone', 'cellphone'],
  'laptops': ['laptops', 'laptop', 'computers', 'notebooks', 'computer', 'pc'],
  'headphones': ['headphones', 'headphone', 'earphones', 'earbuds', 'headset'],
  'clothing': ['clothing', 'apparel', 'clothes', 'shirts', 't-shirts', 'tshirts', 'pants', 'jackets', 'dresses', 'tops'],
  'shoes': ['shoes', 'footwear', 'sneakers', 'boots', 'sandals', 'heels'],
  'watches': ['watches', 'watch', 'wristwatches', 'timepieces'],
  'bags': ['bags', 'bag', 'backpacks', 'backpack', 'handbags', 'wallets', 'luggage'],
  'home-kitchen': ['home-kitchen', 'home', 'kitchen', 'cookware', 'utensils', 'tableware', 'dining'],
  'beauty': ['beauty', 'cosmetics', 'makeup', 'skincare', 'perfumes', 'fragrances', 'personal-care'],
  'books': ['books', 'book', 'textbooks', 'novels', 'literature'],
  'toys': ['toys', 'toy', 'games', 'boardgames', 'puzzles', 'figures'],
  'sports': ['sports', 'sport', 'fitness', 'exercise', 'outdoor', 'gym'],
  'grocery': ['grocery', 'groceries', 'food', 'produce', 'fruits', 'vegetables', 'beverages', 'snacks'],
  'appliances': ['appliances', 'appliance', 'home-appliances', 'kitchen-appliances', 'microwave', 'refrigerator']
};

function resolveCategory(name) {
  const lower = name.toLowerCase().replace(/[^a-z0-9]/g, '');
  for (const [targetCat, aliases] of Object.entries(ALIAS_MAP)) {
    for (const alias of aliases) {
      if (lower.includes(alias.replace(/[^a-z0-9]/g, ''))) {
        return targetCat;
      }
    }
  }
  return null;
}

function getAllFiles(dirPath, arrayOfFiles = []) {
  if (!fs.existsSync(dirPath)) return arrayOfFiles;
  const files = fs.readdirSync(dirPath);

  files.forEach(file => {
    const fullPath = path.join(dirPath, file);
    if (fs.statSync(fullPath).isDirectory()) {
      arrayOfFiles = getAllFiles(fullPath, arrayOfFiles);
    } else {
      const ext = path.extname(file).toLowerCase();
      if (['.jpg', '.jpeg', '.png', '.webp'].includes(ext)) {
        arrayOfFiles.push(fullPath);
      }
    }
  });

  return arrayOfFiles;
}

function parseArgs() {
  const args = process.argv.slice(2);
  const result = { source: null, copy: true, targetCount: 1000 };

  for (const arg of args) {
    if (arg.startsWith('--source=')) {
      result.source = arg.split('=')[1].replace(/^["']|["']$/g, '');
    } else if (arg === '--move') {
      result.copy = false;
    } else if (arg.startsWith('--count=')) {
      result.targetCount = parseInt(arg.split('=')[1], 10);
    }
  }
  return result;
}

function main() {
  const config = parseArgs();

  console.log('========================================================');
  console.log('Amazon Capstone: Local Dataset Importer & Organizer');
  console.log('Target: amazon-capstone/product-images/ (15 categories)');
  console.log('========================================================\n');

  if (!config.source) {
    console.log('Usage:');
    console.log('  node amazon-capstone/import_local_dataset.js --source="<path_to_extracted_dataset>"');
    console.log('\nOptions:');
    console.log('  --source=<path>    Path to the extracted dataset directory (Required)');
    console.log('  --move             Move files instead of copying (Default is copy)');
    console.log('  --count=1000       Target total images (Default is 1000)');
    console.log('\nExample:');
    console.log('  node amazon-capstone/import_local_dataset.js --source="C:/Datasets/fashion-product-images-small"');
    process.exit(0);
  }

  const sourceDir = path.resolve(config.source);
  if (!fs.existsSync(sourceDir)) {
    console.error(`Error: Source directory "${sourceDir}" does not exist.`);
    process.exit(1);
  }

  console.log(`Scanning files in: ${sourceDir}`);
  const allImages = getAllFiles(sourceDir);
  console.log(`Found ${allImages.length} image files.\n`);

  // Bucket images by category
  const bucketed = {};
  CATEGORIES.forEach(c => { bucketed[c] = []; });
  const unclassified = [];

  for (const imgPath of allImages) {
    const parentFolder = path.basename(path.dirname(imgPath));
    const fileName = path.basename(imgPath);
    const cat = resolveCategory(parentFolder) || resolveCategory(fileName);
    if (cat) {
      bucketed[cat].push(imgPath);
    } else {
      unclassified.push(imgPath);
    }
  }

  // Distribute unclassified evenly if needed
  if (unclassified.length > 0) {
    let catIdx = 0;
    for (const imgPath of unclassified) {
      bucketed[CATEGORIES[catIdx % CATEGORIES.length]].push(imgPath);
      catIdx++;
    }
  }

  // Target count per category
  const perCatTarget = Math.ceil(config.targetCount / CATEGORIES.length);
  console.log(`Targeting approx ${perCatTarget} images per category (Total: ${config.targetCount})\n`);

  let globalId = 1;
  const csvRows = ['image_id,product_id,category,filename'];

  CATEGORIES.forEach(cat => {
    const catTargetDir = path.join(TARGET_DIR, cat);
    if (!fs.existsSync(catTargetDir)) fs.mkdirSync(catTargetDir, { recursive: true });

    const available = bucketed[cat];
    const take = Math.min(available.length, perCatTarget);
    console.log(`[${cat}] Processing ${take} images (available: ${available.length})...`);

    for (let i = 0; i < take; i++) {
      if (globalId > config.targetCount) break;
      const srcFile = available[i];
      const ext = path.extname(srcFile).toLowerCase() === '.png' ? '.png' : '.jpg';
      const destFilename = `product_${String(globalId).padStart(4, '0')}${ext}`;
      const destFile = path.join(catTargetDir, destFilename);

      if (config.copy) {
        fs.copyFileSync(srcFile, destFile);
      } else {
        fs.renameSync(srcFile, destFile);
      }

      csvRows.push(`${globalId},${globalId},${cat},${destFilename}`);
      globalId++;
    }
  });

  fs.writeFileSync(CSV_PATH, csvRows.join('\n'), 'utf8');
  console.log('\n========================================================');
  console.log(`Import Complete! Successfully organized ${globalId - 1} images.`);
  console.log(`- Folder: ${TARGET_DIR}`);
  console.log(`- Mapping CSV updated: ${CSV_PATH}`);
  console.log('========================================================');
}

main();
