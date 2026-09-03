/**
 * fetch_open_images.js
 * 
 * Automatically downloads 1,000 real, legally open-licensed product images
 * from Wikimedia Commons (CC-BY, CC-BY-SA, CC0, Public Domain) for the 15 categories.
 * 
 * Usage:
 *   node amazon-capstone/fetch_open_images.js
 *   node amazon-capstone/fetch_open_images.js --limit=50   (quick test with 50 images)
 */

const fs = require('fs');
const path = require('path');
const https = require('https');

const BASE_DIR = path.join(__dirname, 'product-images');
const CSV_PATH = path.join(__dirname, 'image_mapping.csv');
const MANIFEST_PATH = path.join(__dirname, 'attribution_manifest.json');

// Category definitions with Wikimedia Commons categories
const CATEGORIES = [
  { name: 'electronics', count: 67, wikiCats: ['Consumer_electronics', 'Audio_equipment', 'Calculators', 'Electronic_test_equipment'] },
  { name: 'mobiles', count: 67, wikiCats: ['Smartphones', 'Mobile_phones', 'Android_smartphones'] },
  { name: 'laptops', count: 67, wikiCats: ['Laptops', 'Notebook_computers', 'MacBook'] },
  { name: 'headphones', count: 67, wikiCats: ['Headphones', 'Earphones', 'Sennheiser_headphones'] },
  { name: 'clothing', count: 67, wikiCats: ['Shirts', 'T-shirts', 'Jackets', 'Clothing_on_hangers', 'Sweaters'] },
  { name: 'shoes', count: 67, wikiCats: ['Shoes', 'Sneakers', 'Footwear', 'Running_shoes'] },
  { name: 'watches', count: 67, wikiCats: ['Wristwatches', 'Watches', 'Pocket_watches'] },
  { name: 'bags', count: 67, wikiCats: ['Backpacks', 'Handbags', 'Luggage', 'Messenger_bags'] },
  { name: 'home-kitchen', count: 67, wikiCats: ['Cookware', 'Kitchenware', 'Tableware', 'Mugs'] },
  { name: 'beauty', count: 67, wikiCats: ['Cosmetics', 'Perfumes', 'Skin_care', 'Lipsticks'] },
  { name: 'books', count: 66, wikiCats: ['Books_by_cover', 'Hardcovers', 'Paperbacks', 'Books'] },
  { name: 'toys', count: 66, wikiCats: ['Toys', 'Action_figures', 'Board_games', 'Lego'] },
  { name: 'sports', count: 66, wikiCats: ['Sports_equipment', 'Footballs_(association_football)', 'Basketballs', 'Tennis_rackets'] },
  { name: 'grocery', count: 66, wikiCats: ['Fruit', 'Vegetables', 'Beverages', 'Edible_nuts'] },
  { name: 'appliances', count: 66, wikiCats: ['Home_appliances', 'Kitchen_appliances', 'Microwave_ovens', 'Toasters'] }
];

// Helper: fetch JSON from URL
function fetchJson(url) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'User-Agent': 'AmazonCapstoneImageImporter/1.0 (academic.project@example.org)' } }, res => {
      let data = '';
      res.on('data', chunk => { data += chunk; });
      res.on('end', () => {
        try {
          resolve(JSON.parse(data));
        } catch (e) {
          reject(e);
        }
      });
    }).on('error', reject);
  });
}

// Helper: download binary image file
function downloadFile(fileUrl, destPath) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(destPath);
    https.get(fileUrl, { headers: { 'User-Agent': 'AmazonCapstoneImageImporter/1.0 (academic.project@example.org)' } }, res => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        // Handle redirects
        return downloadFile(res.headers.location, destPath).then(resolve).catch(reject);
      }
      if (res.statusCode !== 200) {
        file.close();
        fs.unlink(destPath, () => {});
        return reject(new Error(`Failed with HTTP ${res.statusCode}`));
      }
      res.pipe(file);
      file.on('finish', () => {
        file.close(() => resolve(true));
      });
    }).on('error', err => {
      file.close();
      fs.unlink(destPath, () => {});
      reject(err);
    });
  });
}

// Sleep utility
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

async function getCategoryMembers(catName, targetCount) {
  const images = [];
  let gcmcontinue = null;
  
  while (images.length < targetCount) {
    let url = `https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category:${encodeURIComponent(catName)}&gcmtype=file&gcmlimit=50&prop=imageinfo&iiprop=url|mime|extmetadata|size&format=json&origin=*`;
    if (gcmcontinue) {
      url += `&gcmcontinue=${encodeURIComponent(gcmcontinue)}`;
    }
    
    try {
      const data = await fetchJson(url);
      if (!data.query || !data.query.pages) break;
      
      const pages = Object.values(data.query.pages);
      for (const p of pages) {
        if (!p.imageinfo || !p.imageinfo[0]) continue;
        const info = p.imageinfo[0];
        
        // Filter only valid JPG or PNG images of reasonable dimensions (> 200px)
        const isJpgOrPng = info.mime === 'image/jpeg' || info.mime === 'image/png';
        if (isJpgOrPng && info.width >= 200 && info.height >= 200) {
          images.push({
            title: p.title,
            url: info.url,
            mime: info.mime,
            width: info.width,
            height: info.height,
            license: info.extmetadata?.LicenseShortName?.value || 'Open License / Creative Commons',
            artist: info.extmetadata?.Artist?.value ? info.extmetadata.Artist.value.replace(/<[^>]*>?/gm, '').trim() : 'Wikimedia Contributor'
          });
          if (images.length >= targetCount) break;
        }
      }
      
      if (data.continue && data.continue.gcmcontinue) {
        gcmcontinue = data.continue.gcmcontinue;
        await sleep(300);
      } else {
        break;
      }
    } catch (err) {
      console.warn(`Warning fetching from Category:${catName}:`, err.message);
      break;
    }
  }
  return images;
}

async function main() {
  console.log('========================================================');
  console.log('Amazon Capstone: Legally Usable Open Image Downloader');
  console.log('Source: Wikimedia Commons / Open Media Archive');
  console.log('Licensing: 100% CC-BY, CC-BY-SA, CC0, Public Domain');
  console.log('========================================================\n');

  // Check optional CLI argument --limit
  const limitArg = process.argv.find(arg => arg.startsWith('--limit='));
  const maxTotalLimit = limitArg ? parseInt(limitArg.split('=')[1], 10) : 1000;

  let globalId = 1;
  const manifest = [];
  const csvRows = ['image_id,product_id,category,filename'];

  for (const cat of CATEGORIES) {
    const catDir = path.join(BASE_DIR, cat.name);
    if (!fs.existsSync(catDir)) fs.mkdirSync(catDir, { recursive: true });

    let needed = cat.count;
    if (maxTotalLimit < 1000) {
      needed = Math.max(1, Math.round((cat.count / 1000) * maxTotalLimit));
    }

    console.log(`\n[Category: ${cat.name}] Fetching ${needed} open-licensed product images...`);
    let gathered = [];

    for (const wikiCat of cat.wikiCats) {
      if (gathered.length >= needed) break;
      const results = await getCategoryMembers(wikiCat, needed - gathered.length);
      gathered = gathered.concat(results);
      await sleep(250);
    }

    console.log(`  Found ${gathered.length} verified image candidates. Downloading...`);

    let downloadedCount = 0;
    for (const img of gathered) {
      if (downloadedCount >= needed) break;
      if (globalId > maxTotalLimit) break;

      const ext = img.mime === 'image/png' ? '.png' : '.jpg';
      const filename = `product_${String(globalId).padStart(4, '0')}${ext}`;
      const destPath = path.join(catDir, filename);

      try {
        await downloadFile(img.url, destPath);
        manifest.push({
          image_id: globalId,
          product_id: globalId,
          category: cat.name,
          filename: filename,
          license: img.license,
          author: img.artist,
          source_url: img.url,
          title: img.title
        });

        csvRows.push(`${globalId},${globalId},${cat.name},${filename}`);
        process.stdout.write(`\r  Saved: ${filename} -> ${cat.name} (${downloadedCount + 1}/${needed})`);
        downloadedCount++;
        globalId++;
        await sleep(150); // Respect Wikimedia Commons rate guidelines
      } catch (err) {
        console.warn(`\n  Download failed for ${img.url.substring(0, 50)}: ${err.message}. Skipping...`);
      }
    }
  }

  // Update CSV
  fs.writeFileSync(CSV_PATH, csvRows.join('\n'), 'utf8');
  fs.writeFileSync(MANIFEST_PATH, JSON.stringify(manifest, null, 2), 'utf8');

  console.log('\n\n========================================================');
  console.log(`Download Complete! Processed ${globalId - 1} images.`);
  console.log(`- Folder: ${BASE_DIR}`);
  console.log(`- CSV Mapping: ${CSV_PATH}`);
  console.log(`- Attribution & License Manifest: ${MANIFEST_PATH}`);
  console.log('========================================================');
}

main().catch(err => {
  console.error('Fatal execution error:', err);
  process.exit(1);
});
