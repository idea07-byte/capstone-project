# Usage

## Run

```powershell
cd d:\java\capstone
javac -d out src\Main.java src\model\Product.java src\model\User.java src\model\Customer.java src\service\ProductService.java src\service\UserService.java
java -cp out Main
```

## CLI Features

- View available products
- Add a product
- Find a product by ID
- Remove a product by ID
- View seeded users
- Add a user
- Find a user by ID

## Documentation

- ER diagram: `docs/erd.md`
- Generated ER diagram image: `docs/erd.png`
- Generated class diagram image: `docs/class-diagram.png`
