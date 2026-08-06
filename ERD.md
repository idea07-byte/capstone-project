# Entity-Relationship Diagram

This repository models a simple shop domain with products and users.

```mermaid
erDiagram
    PRODUCT {
        int id PK
        string name
        double price
        int quantity
    }

    USER {
        int id PK
        string name
        string email
        string password
        string role
    }

    CUSTOMER {
        int id PK
        string name
        string email
        string password
    }

    USER ||--|| CUSTOMER : extends
```
