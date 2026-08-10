# Multi-Vendor E-commerce Marketplace Diagrams

Project statement: Multi-vendor E-commerce Marketplace with Seller Dashboard and Secure Order Management (Amazon-style).

This document models a marketplace where buyers can browse and purchase products, sellers can manage inventory and sales, and admins can oversee platform security and order processing.

## 1. Architecture Diagram

```mermaid
flowchart LR
    B[Buyer] --> A[Marketplace Web App]
    S[Seller] --> A
    AD[Admin] --> A
    A --> AU[Authentication & Authorization]
    A --> PC[Product Catalog Service]
    A --> SD[Seller Dashboard Service]
    A --> OM[Order Management Service]
    A --> PM[Payment & Security Service]
    PC --> DB[(Product Database)]
    SD --> DB
    OM --> DB
    PM --> PAY[(Payment Gateway)]
    OM --> SH[(Shipping / Fulfillment)]
```

## 2. Use-Case Diagram

```mermaid
flowchart TD
    Buyer[Buyer] --> UC1[Browse products]
    Buyer --> UC2[Place order]
    Buyer --> UC3[Track order]
    Buyer --> UC4[Make secure payment]

    Seller[Seller] --> US1[Manage products]
    Seller --> US2[Update inventory]
    Seller --> US3[View sales dashboard]
    Seller --> US4[Process orders]

    Admin[Admin] --> UA1[Approve sellers]
    Admin --> UA2[Monitor orders]
    Admin --> UA3[Manage disputes]
    Admin --> UA4[Secure platform access]
```

## 3. ER Diagram

```mermaid
erDiagram
    USER {
        int id PK
        string name
        string email
        string password
        string role
    }

    SELLER {
        int id PK
        int user_id FK
        string shop_name
    }

    BUYER {
        int id PK
        int user_id FK
        string address
    }

    PRODUCT {
        int id PK
        int seller_id FK
        string name
        double price
        int stock
    }

    ORDER {
        int id PK
        int buyer_id FK
        int seller_id FK
        datetime created_at
        string status
    }

    ORDER_ITEM {
        int id PK
        int order_id FK
        int product_id FK
        int quantity
        double unit_price
    }

    PAYMENT {
        int id PK
        int order_id FK
        string method
        string status
    }

    USER ||--o| SELLER : "has"
    USER ||--o| BUYER : "has"
    SELLER ||--o{ PRODUCT : "offers"
    BUYER ||--o{ ORDER : "places"
    SELLER ||--o{ ORDER : "fulfills"
    ORDER ||--|{ ORDER_ITEM : "contains"
    PRODUCT ||--o{ ORDER_ITEM : "appears in"
    ORDER ||--|| PAYMENT : "has"
```

## 4. Class Diagram

```mermaid
classDiagram
    class User {
      +int id
      +String name
      +String email
      +String password
      +String role
    }

    class Buyer {
      +String address
      +placeOrder()
      +trackOrder()
    }

    class Seller {
      +String shopName
      +manageInventory()
      +viewDashboard()
      +processOrder()
    }

    class Admin {
      +approveSeller()
      +monitorOrders()
      +manageDisputes()
    }

    class Product {
      +int id
      +String name
      +double price
      +int stock
      +int sellerId
    }

    class Order {
      +int id
      +int buyerId
      +int sellerId
      +String status
      +createOrder()
      +updateStatus()
    }

    class OrderItem {
      +int quantity
      +double unitPrice
    }

    class Payment {
      +String method
      +String status
      +processPayment()
    }

    class SellerDashboard {
      +viewSalesReport()
      +viewOrders()
    }

    User <|-- Buyer
    User <|-- Seller
    User <|-- Admin
    Seller --> Product
    Buyer --> Order
    Seller --> Order
    Order --> OrderItem
    Order --> Payment
    Seller --> SellerDashboard
```

## 5. Sequence Diagram: Secure Checkout Flow

```mermaid
sequenceDiagram
    actor Buyer
    participant App as Marketplace App
    participant Auth as Auth Service
    participant Catalog as Catalog Service
    participant Order as Order Service
    participant Pay as Payment Service
    participant Seller as Seller Dashboard

    Buyer->>Auth: Login
    Auth-->>Buyer: Access granted
    Buyer->>Catalog: Browse products
    Catalog-->>Buyer: Product list
    Buyer->>Order: Place order
    Order->>Pay: Authorize payment
    Pay-->>Order: Payment success
    Order->>Seller: Notify seller
    Seller-->>Buyer: Order accepted
    Buyer-->>Buyer: Track order status
```

## 6. Activity Diagram: Seller Dashboard Workflow

```mermaid
flowchart TD
    A[Start] --> B[Seller logs in]
    B --> C{Valid credentials?}
    C -- No --> D[Show access error]
    C -- Yes --> E[Open dashboard]
    E --> F[View products]
    F --> G[Update stock or price]
    G --> H[Review incoming orders]
    H --> I[Approve / ship order]
    I --> J[Send status update]
    J --> K[End]
    D --> K
```

## 7. Data-Flow Diagram

```mermaid
flowchart LR
    A[Buyer actions] --> B[Marketplace App]
    B --> C[Authentication]
    B --> D[Catalog Service]
    B --> E[Order Management]
    B --> F[Payment Security]
    D --> G[(Product DB)]
    E --> H[(Order DB)]
    F --> I[(Payment Records)]
    E --> J[Seller Dashboard]
    E --> K[Admin Monitoring]
    B --> L[Order Status Updates]
```

## 8. Proposed Architecture Summary

The proposed system follows a layered architecture for a multi-vendor e-commerce marketplace:

- Presentation Layer: buyer, seller, and admin interfaces for browsing products, managing inventory, placing orders, and monitoring transactions.
- Application Layer: services for authentication, catalog management, seller dashboard operations, order processing, and payment security.
- Data Layer: databases for users, products, orders, payments, and inventory records.
- Integration Layer: secure payment gateways, shipping/fulfillment services, and notification modules.

This architecture supports scalability, modular development, and secure order handling. It also allows multiple vendors to operate independently while maintaining centralized control for platform administration and transaction monitoring.
