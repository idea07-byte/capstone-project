# BuyIt Marketplace — Entity-Relationship Diagram (ERD)

This document provides both the **Traditional Geometric ER Diagram (Peter Chen Notation using Shapes)** and the **Relational Crow's Foot Schema** for the BuyIt Multi-Vendor E-commerce Marketplace.

> **Visual Artifacts:**
> - 🌐 **[Interactive ER Diagram Web App (Pan & Zoom)](er_diagram_traditional.html)**
> - 🖼️ **[Standalone High-Res Vector SVG](er_diagram_traditional.svg)**
> - 📖 **[Comprehensive Traditional ER Documentation](ER_DIAGRAM_TRADITIONAL.md)**

---

## 1. Traditional ER Diagram (Peter Chen Notation Using Shapes)

In traditional Peter Chen ER modeling, the system data architecture is drawn strictly with geometric shapes:
- **Rectangles `[ ]`**: Strong Entities (`User`, `Vendor`, `Category`, `Brand`, `Product`, `Address`, `Order`, `Coupon`)
- **Double Rectangles `[[ ]]`**: Weak Entities (`ProductImage`, `Cart`, `CartItem`, `Wishlist`, `WishlistItem`, `OrderItem`, `Payment`, `Review`, `Notification`)
- **Diamonds `{ }`**: Relationships (`Operates`, `Supplies`, `Classifies`, `Manufactures`, `Places`, `Delivered_To`, `Writes`, `Receives`, `Applies`)
- **Double Diamonds `{{ }}`**: Identifying Relationships (`Has_Gallery`, `Owns_Cart`, `Holds_Items`, `Consists_Of`, `Settled_With`, `Notified_By`)
- **Ovals / Ellipses `( )`**: Attributes (`name`, `price`, `status`, `discount`, `rating`, `payment_method`)
- **Underlined Ovals `( <u>...</u> )`**: Key Attributes (`<u>id (PK)</u>`, `<u>email (UK)</u>`, `<u>code (UK)</u>`)
- **Dashed Ovals `(- - ... - -)`**: Derived Attributes (`discounted_price`, `final_amount`, `subtotal`)

```mermaid
flowchart TD
    %% Styling Classes
    classDef strongEntity fill:#0284c7,stroke:#38bdf8,stroke-width:2.5px,color:#ffffff,font-weight:bold;
    classDef weakEntity fill:#0d9488,stroke:#2dd4bf,stroke-width:3px,color:#ffffff,font-weight:bold;
    classDef relDiamond fill:#b45309,stroke:#fde047,stroke-width:2px,color:#ffffff,font-weight:bold;
    classDef idRelDiamond fill:#c2410c,stroke:#fdba74,stroke-width:3px,color:#ffffff,font-weight:bold;

    %% Strong Entities (Rectangles)
    User["User"]:::strongEntity
    Vendor["Vendor"]:::strongEntity
    Category["Category"]:::strongEntity
    Brand["Brand"]:::strongEntity
    Product["Product"]:::strongEntity
    Address["Address"]:::strongEntity
    Order["Order"]:::strongEntity
    Coupon["Coupon"]:::strongEntity

    %% Weak Entities (Double Rectangles)
    ProdImg[["ProductImage"]]:::weakEntity
    Cart[["Cart"]]:::weakEntity
    CartItem[["CartItem"]]:::weakEntity
    Wishlist[["Wishlist"]]:::weakEntity
    WishlistItem[["WishlistItem"]]:::weakEntity
    OrderItem[["OrderItem"]]:::weakEntity
    Payment[["Payment"]]:::weakEntity
    Review[["Review"]]:::weakEntity
    Notification[["Notification"]]:::weakEntity

    %% Relationships (Diamonds)
    R_Operates{"Operates"}:::relDiamond
    R_Saves{"Has_Saved"}:::relDiamond
    R_Supplies{"Supplies"}:::relDiamond
    R_Classifies{"Classifies"}:::relDiamond
    R_Manufactures{"Manufactures"}:::relDiamond
    R_Places{"Places"}:::relDiamond
    R_DeliveredTo{"Delivered_To"}:::relDiamond
    R_Writes{"Writes"}:::relDiamond
    R_Receives{"Receives"}:::relDiamond
    R_Applies{"Applies"}:::relDiamond
    R_Fulfills{"Fulfills"}:::relDiamond
    R_CartProduct{"References"}:::relDiamond
    R_WishProduct{"References"}:::relDiamond
    R_OrderProduct{"References"}:::relDiamond

    %% Identifying Relationships (Double Diamonds)
    R_Gallery{{"Has_Gallery"}}:::idRelDiamond
    R_OwnsCart{{"Owns_Cart"}}:::idRelDiamond
    R_HoldsCart{{"Holds_Items"}}:::idRelDiamond
    R_OwnsWish{{"Owns_Wishlist"}}:::idRelDiamond
    R_HoldsWish{{"Stores_Items"}}:::idRelDiamond
    R_ConsistsOf{{"Consists_Of"}}:::idRelDiamond
    R_SettledWith{{"Settled_With"}}:::idRelDiamond
    R_NotifiedBy{{"Notified_By"}}:::idRelDiamond

    %% User & Vendor & Address
    User ---|"1"| R_Operates ---|"1"| Vendor
    User ---|"1"| R_Saves ---|"N"| Address
    User ===|"1"| R_NotifiedBy ===|"N"| Notification

    %% Product & Catalog
    Vendor ---|"1"| R_Supplies ---|"N"| Product
    Category ---|"1"| R_Classifies ---|"N"| Product
    Brand ---|"1"| R_Manufactures ---|"N"| Product
    Product ===|"1"| R_Gallery ===|"N"| ProdImg

    %% Cart & Wishlist Subsystem
    User ===|"1"| R_OwnsCart ===|"1"| Cart
    Cart ===|"1"| R_HoldsCart ===|"N"| CartItem
    Product ---|"1"| R_CartProduct ---|"N"| CartItem

    User ===|"1"| R_OwnsWish ===|"1"| Wishlist
    Wishlist ===|"1"| R_HoldsWish ===|"N"| WishlistItem
    Product ---|"1"| R_WishProduct ---|"N"| WishlistItem

    %% Orders & Fulfillment
    User ---|"1"| R_Places ---|"N"| Order
    Address ---|"1"| R_DeliveredTo ---|"N"| Order
    Order ---|"N"| R_Applies ---|"1"| Coupon
    Order ===|"1"| R_ConsistsOf ===|"N"| OrderItem
    Product ---|"1"| R_OrderProduct ---|"N"| OrderItem
    Vendor ---|"1"| R_Fulfills ---|"N"| OrderItem
    Order ===|"1"| R_SettledWith ===|"1"| Payment

    %% Reviews
    User ---|"1"| R_Writes ---|"N"| Review
    Product ---|"1"| R_Receives ---|"N"| Review
```

---

## 2. Core Relational Schema (Crow's Foot Notation)

```mermaid
erDiagram
    users ||--o| vendors : operates
    users ||--o{ addresses : registers
    users ||--|| cart : owns
    users ||--|| wishlist : owns
    users ||--o{ orders : places
    users ||--o{ reviews : writes
    users ||--o{ notifications : receives

    vendors ||--o{ products : supplies
    vendors ||--o{ order_items : fulfills
    categories ||--o{ products : classifies
    brands ||--o{ products : manufactures

    products ||--o{ product_images : contains
    products ||--o{ cart_items : referenced_in
    products ||--o{ wishlist_items : saved_in
    products ||--o{ order_items : ordered_in
    products ||--o{ reviews : receives

    cart ||--o{ cart_items : holds
    wishlist ||--o{ wishlist_items : holds

    addresses ||--o{ orders : delivers_to
    coupons ||--o{ orders : discounts
    orders ||--|{ order_items : includes
    orders ||--|| payments : settles

    users {
        int id PK
        string name
        string email
        string password
        string role
        string status
    }

    vendors {
        int id PK
        int user_id FK
        string business_name
        string owner_name
        string approval_status
    }

    products {
        int id PK
        int vendor_id FK
        int category_id FK
        int brand_id FK
        string name
        decimal price
        decimal discount
        int stock_quantity
    }

    orders {
        int id PK
        int customer_id FK
        int address_id FK
        decimal total_amount
        decimal final_amount
        string order_status
        string payment_status
    }

    order_items {
        int id PK
        int order_id FK
        int product_id FK
        int vendor_id FK
        int quantity
        decimal price
        decimal subtotal
    }

    payments {
        int id PK
        int order_id FK
        string payment_method
        string transaction_id
        decimal amount
        string payment_status
    }
```
