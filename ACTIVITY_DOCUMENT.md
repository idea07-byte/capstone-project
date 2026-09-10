# ONLINE SHOPPING AND MULTI-VENDOR E-COMMERCE SYSTEM
## CAPSTONE PROJECT ACTIVITY DOCUMENT

**SUBMITTED BY:** SHYAM M  
**YEAR:** III  
**DEPARTMENT:** COMPUTER SCIENCE AND ENGINEERING  
**COLLEGE:** J.J COLLEGE OF ENGINEERING AND TECHNOLOGY  

---

## INDEX PAGE

| S.No | CONTENTS | PAGE NO. |
| :--- | :--- | :--- |
| 1 | Traditional ER Diagram | 2 |
| 2 | Traditional ER Diagram Explanation | 3 |
| 3 | Schema Mapping Rules | 5 |
| 4 | Data Dictionary | 6 onwards |

---

## 1. TRADITIONAL ER DIAGRAM (PAGE 2)

> The system ER diagram is modeled using Peter Chen's traditional geometric notation (Rectangles for Entities, Diamonds for Relationships, Ellipses for Attributes, and Underlines for Primary Keys).

See high-resolution vector diagram in [HTML report](docs/activity_document.html) or [PDF](BuyIt_Activity_Document.pdf).

---

## 2. TRADITIONAL ER DIAGRAM EXPLANATION (PAGE 3 - 4)

The Traditional Entity Relationship (ER) Diagram represents the overall database structure of the Online Shopping and Multi-Vendor E-Commerce System (BuyIt Marketplace). It illustrates the major entities, their attributes, relationships, and cardinalities used in the system. The ER Diagram helps in understanding how different entities are connected before converting the design into relational database tables.

The **USERS** entity is one of the main entities in the system. It stores information such as user ID, name, email, phone number, password, role, status, and account creation details. Users can act as customers, vendors, or system administrators depending on their assigned role. A customer can place multiple orders, maintain a personal cart and wishlist, save delivery addresses, receive notifications, and write product reviews.

The **VENDORS** entity stores business and merchant information for registered sellers on the marketplace. Each vendor record is associated with a user through the user_id in a one-to-one relationship. The entity contains business name, owner name, business description, address, city, state, pincode, and administrative approval status. A vendor can list and supply multiple products in the catalog.

The **CATEGORIES** entity organizes marketplace products into logical departments such as Electronics, Mobiles, Fashion, Home, and Books. It includes category name, description, image URL, and status. Each category can contain multiple products, establishing a one-to-many relationship with the products entity.

The **BRANDS** entity stores information about manufacturers and brands associated with marketplace items. It includes brand name, description, brand logo URL, and active status. Multiple products can belong to a specific brand.

The **PRODUCTS** entity stores important product catalog details such as product name, description, price, discount percentage, stock quantity, SKU code, display image, and availability status. Products form the central part of the e-commerce system. Each product belongs to a specific vendor, category, and brand, and can have multiple gallery images, reviews, and order items.

The **PRODUCT_IMAGES** entity stores multiple images related to products. A product can have multiple images, allowing the system to display different views and detailed gallery photos of the item to prospective buyers.

The **ADDRESSES** entity stores delivery address information created by customers. Each address record contains details such as recipient full name, phone number, address line, city, state, postal pincode, and default address flag. A user can store multiple addresses for shipping.

The **CART** entity manages active shopping sessions for customers. Each customer has one cart record connected via customer_id. The **CART_ITEMS** entity represents individual products placed in the cart, capturing the desired quantity and item price. Every cart item belongs to a specific cart and references a product.

The **WISHLIST** entity enables customers to save products of interest for future purchase. Each customer is associated with one wishlist through customer_id. The **WISHLIST_ITEMS** entity maintains the junction between the wishlist and individual products, allowing users to bookmark multiple items.

The **ORDERS** entity is responsible for managing customer checkout transactions and order fulfillment. It connects the USERS entity (customer) with the ADDRESSES entity (shipping destination). Order records maintain total amount, shipping charges, discount deductions, final payable amount, payment status, and order status. A user can place multiple orders over time.

The **ORDER_ITEMS** entity records the individual product line items contained within each order. It captures an immutable historical snapshot of the purchased item, including product ID, vendor ID, product name at the time of purchase, unit price, quantity, and line-item subtotal. Each order item connects to a specific order and product.

The **PAYMENTS** entity stores payment transaction records associated with customer orders. It includes details such as payment amount, payment method (Cash on Delivery, UPI, Card, or Net Banking), payment date, payment status, and transaction reference ID. Payments are associated with orders in a one-to-one relationship to maintain rigorous financial accounting records.

The **COUPONS** entity stores promotional discount information such as unique coupon code, discount type (percentage or fixed amount), discount value, minimum order requirement, maximum discount limit, usage limits, and validity dates. Coupons can be applied during checkout to provide promotional savings.

The **REVIEWS** entity stores feedback and ratings provided by customers for products they have purchased. It includes rating score (1 to 5 stars), review comments, moderation status, and creation date. A user can write reviews for multiple products, helping prospective buyers evaluate item quality.

The **NOTIFICATIONS** entity stores system alerts and operational messages sent to users. A user can receive multiple notifications related to order confirmations, shipping updates, payment receipts, or promotional offers. Notification details include message title, body text, type, read status, and timestamp.

Overall, the Traditional ER Diagram provides a clear representation of the entities and relationships used in the Online Shopping and Multi-Vendor E-Commerce System. Primary Keys are used to uniquely identify records, while Foreign Keys establish relationships between related entities. The ER Diagram was later converted into relational tables using schema mapping rules, ensuring data integrity, consistency, and proper database organization.

---

## 3. SCHEMA MAPPING RULES (PAGE 5)

### 1. Entity to Table Mapping
Each entity in the ER Diagram was converted into a separate relational table. For example, USERS, VENDORS, CATEGORIES, BRANDS, PRODUCTS, ORDERS, and PAYMENTS were converted into database tables.

### 2. Attribute to Column Mapping
Each attribute of an entity was converted into a column in the corresponding table, retaining standard descriptive identifiers such as user_id, price, stock_quantity, and order_status.

### 3. Primary Key Mapping
Each table was assigned a Primary Key to uniquely identify every record, implemented using auto-incrementing SERIAL integer sequences.

### 4. Relationship Mapping
Relationships between entities were implemented using Foreign Keys. For example, the customer_id in the ORDERS table references the USERS table, and vendor_id in the PRODUCTS table references the VENDORS table.

### 5. One-to-Many Relationship Mapping
For a one-to-many relationship, the Primary Key of the parent table was added as a Foreign Key in the child table. For example, category_id is added to PRODUCTS to reference the parent CATEGORIES table.

### 6. One-to-One Relationship Mapping
For a one-to-one relationship, a Foreign Key with a UNIQUE constraint was used. For example, user_id in the VENDORS table and customer_id in the CART table uniquely reference the USERS table.

### 7. Many-to-Many Relationship Mapping
Many-to-many relationships were represented using an intermediate or junction table when required. For example, the relationship between CART and PRODUCTS is represented via CART_ITEMS, and between ORDERS and PRODUCTS via ORDER_ITEMS, avoiding data redundancy.

### 8. Foreign Key Mapping
Foreign Keys were used to establish relationships between related tables. For example, customer_id connects USERS with ORDERS, ADDRESSES, CART, WISHLIST, REVIEWS, and NOTIFICATIONS.

### 9. Referential Integrity
Referential integrity was maintained by using Foreign Key constraints with appropriate cascading actions (ON DELETE CASCADE, SET NULL, or RESTRICT). This ensures that a record in a child table cannot reference a non-existing record in a parent table.

### 10. Constraint Mapping
Constraints such as NOT NULL, UNIQUE, PRIMARY KEY, FOREIGN KEY, CHECK (e.g. price >= 0, stock_quantity >= 0, rating between 1 and 5), and DEFAULT were applied to maintain data accuracy, consistency, and integrity.

### 11. Data Type Mapping
Appropriate data types were selected based on the type of information stored: VARCHAR for text values, INT for numerical values, DATE and TIMESTAMP for date-related information, and DECIMAL(10,2) for exact monetary and currency values.

---

## 4. DATA DICTIONARY (PAGE 6 - 10)

The following data dictionary describes the tables, columns, data types, keys, and constraints used in the Online Shopping and Multi-Vendor E-Commerce System database.

### USERS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| user_id | SERIAL | PRIMARY KEY |
| name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(100) | UNIQUE, NOT NULL |
| phone | VARCHAR(20) | - |
| password | VARCHAR(255) | NOT NULL |
| role | VARCHAR(20) | DEFAULT 'CUSTOMER' |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### VENDORS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| vendor_id | SERIAL | PRIMARY KEY |
| user_id | INT | NOT NULL, UNIQUE, FOREIGN KEY |
| business_name | VARCHAR(200) | NOT NULL |
| owner_name | VARCHAR(100) | NOT NULL |
| description | TEXT | - |
| address | TEXT | - |
| city | VARCHAR(100) | - |
| state | VARCHAR(100) | - |
| pincode | VARCHAR(10) | - |
| approval_status | VARCHAR(20) | DEFAULT 'PENDING' |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### CATEGORIES Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| category_id | SERIAL | PRIMARY KEY |
| name | VARCHAR(100) | NOT NULL |
| description | TEXT | - |
| image | VARCHAR(500) | - |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### BRANDS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| brand_id | SERIAL | PRIMARY KEY |
| name | VARCHAR(100) | NOT NULL |
| description | TEXT | - |
| logo | VARCHAR(500) | - |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### PRODUCTS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| product_id | SERIAL | PRIMARY KEY |
| vendor_id | INT | NOT NULL, FOREIGN KEY |
| category_id | INT | FOREIGN KEY |
| brand_id | INT | FOREIGN KEY |
| name | VARCHAR(255) | NOT NULL |
| description | TEXT | - |
| price | DECIMAL(10,2) | NOT NULL |
| discount | DECIMAL(5,2) | DEFAULT 0 |
| stock_quantity | INT | NOT NULL, DEFAULT 0 |
| sku | VARCHAR(50) | - |
| image | VARCHAR(500) | - |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### PRODUCT_IMAGES Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| image_id | SERIAL | PRIMARY KEY |
| product_id | INT | NOT NULL, FOREIGN KEY |
| image_url | VARCHAR(500) | NOT NULL |

### ADDRESSES Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| address_id | SERIAL | PRIMARY KEY |
| customer_id | INT | NOT NULL, FOREIGN KEY |
| full_name | VARCHAR(100) | NOT NULL |
| phone | VARCHAR(20) | - |
| address_line | TEXT | NOT NULL |
| city | VARCHAR(100) | NOT NULL |
| state | VARCHAR(100) | NOT NULL |
| pincode | VARCHAR(10) | NOT NULL |
| is_default | BOOLEAN | DEFAULT FALSE |

### CART Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| cart_id | SERIAL | PRIMARY KEY |
| customer_id | INT | NOT NULL, UNIQUE, FOREIGN KEY |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### CART_ITEMS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| cart_item_id | SERIAL | PRIMARY KEY |
| cart_id | INT | NOT NULL, FOREIGN KEY |
| product_id | INT | NOT NULL, FOREIGN KEY |
| quantity | INT | NOT NULL, DEFAULT 1 |
| price | DECIMAL(10,2) | NOT NULL |

### WISHLIST Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| wishlist_id | SERIAL | PRIMARY KEY |
| customer_id | INT | NOT NULL, UNIQUE, FOREIGN KEY |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### WISHLIST_ITEMS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| wishlist_item_id | SERIAL | PRIMARY KEY |
| wishlist_id | INT | NOT NULL, FOREIGN KEY |
| product_id | INT | NOT NULL, FOREIGN KEY |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### ORDERS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| order_id | SERIAL | PRIMARY KEY |
| customer_id | INT | NOT NULL, FOREIGN KEY |
| address_id | INT | FOREIGN KEY |
| total_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0.00 |
| shipping_amount | DECIMAL(10,2) | DEFAULT 0.00 |
| discount_amount | DECIMAL(10,2) | DEFAULT 0.00 |
| final_amount | DECIMAL(10,2) | NOT NULL, DEFAULT 0.00 |
| payment_status | VARCHAR(20) | DEFAULT 'PENDING' |
| order_status | VARCHAR(30) | DEFAULT 'PLACED' |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### ORDER_ITEMS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| order_item_id | SERIAL | PRIMARY KEY |
| order_id | INT | NOT NULL, FOREIGN KEY |
| product_id | INT | NOT NULL, FOREIGN KEY |
| vendor_id | INT | NOT NULL, FOREIGN KEY |
| product_name | VARCHAR(255) | NOT NULL |
| price | DECIMAL(10,2) | NOT NULL |
| quantity | INT | NOT NULL |
| subtotal | DECIMAL(10,2) | NOT NULL |
| item_status | VARCHAR(30) | DEFAULT 'PLACED' |

### PAYMENTS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| payment_id | SERIAL | PRIMARY KEY |
| order_id | INT | NOT NULL, FOREIGN KEY |
| payment_method | VARCHAR(20) | NOT NULL |
| transaction_id | VARCHAR(100) | - |
| amount | DECIMAL(10,2) | NOT NULL |
| payment_status | VARCHAR(20) | DEFAULT 'PENDING' |
| payment_date | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### COUPONS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| coupon_id | SERIAL | PRIMARY KEY |
| coupon_code | VARCHAR(50) | UNIQUE, NOT NULL |
| discount_type | VARCHAR(20) | NOT NULL |
| discount_value | DECIMAL(10,2) | NOT NULL |
| minimum_amount | DECIMAL(10,2) | DEFAULT 0 |
| maximum_discount | DECIMAL(10,2) | - |
| start_date | TIMESTAMP | - |
| expiry_date | TIMESTAMP | - |
| usage_limit | INT | DEFAULT 0 |
| used_count | INT | DEFAULT 0 |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### REVIEWS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| review_id | SERIAL | PRIMARY KEY |
| product_id | INT | NOT NULL, FOREIGN KEY |
| customer_id | INT | NOT NULL, FOREIGN KEY |
| rating | INT | NOT NULL |
| comment | TEXT | - |
| status | VARCHAR(20) | DEFAULT 'ACTIVE' |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |

### NOTIFICATIONS Table
| Column Name | Data Type | Key / Constraint |
| :--- | :--- | :--- |
| notification_id | SERIAL | PRIMARY KEY |
| user_id | INT | NOT NULL, FOREIGN KEY |
| title | VARCHAR(200) | NOT NULL |
| message | TEXT | NOT NULL |
| type | VARCHAR(50) | - |
| is_read | BOOLEAN | DEFAULT FALSE |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP |
