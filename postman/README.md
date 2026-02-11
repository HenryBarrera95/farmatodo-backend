# Colección Postman - Farmatodo API

## Importar

1. Abre Postman.
2. **Import** → arrastra o selecciona `Farmatodo API.postman_collection.json`.

## Variables de colección

| Variable   | Default        | Descripción                          |
|------------|----------------|--------------------------------------|
| baseUrl    | http://localhost:8080 | URL base de la API            |
| apiKey     | changeme       | API Key (debe coincidir con `APP_API_KEY` en `.env`) |
| customerId | (se rellena)   | Se guarda al crear un cliente        |
| tokenId    | (se rellena)   | Se guarda al crear un token           |
| productId  | (se rellena)   | Se guarda al listar productos         |

Edita `apiKey` en las variables de la colección si usas otra clave en `.env`.

## Pruebas automatizadas

Cada request incluye **scripts de test** (`pm.test()`) que validan la respuesta:

| Request              | Validaciones                                           |
|----------------------|--------------------------------------------------------|
| Ping                 | Status 200, body contiene "pong"                       |
| Health / Actuator   | Status 200, JSON con status                           |
| Create Customer     | 201, tiene id y email (email único por ejecución)     |
| Create Token        | 200, tiene token y maskedPan con formato correcto    |
| Get All Products    | 200, array de productos                               |
| Add Item / Get Cart | 201/200, estructura esperada                          |
| Create Order        | 201, orderId, status PAID o PAYMENT_FAILED            |
| 401 Sin API Key     | Status 401                                            |
| 400 Validación      | Status 400                                            |

## Collection Runner (pruebas automatizadas)

1. Clic derecho en la colección → **Run collection**
2. Selecciona todas las carpetas o las que quieras (orden recomendado: Health → 1. Clientes → 2. Tokens → 3. Productos → 4. Carrito → 5. Pedidos → Errores)
3. **Run** → verás ✅/❌ por cada test

## Newman (CLI, para CI/CD)

```bash
npm install -g newman
newman run "postman/Farmatodo API.postman_collection.json" --variable apiKey=tu-api-key
```
