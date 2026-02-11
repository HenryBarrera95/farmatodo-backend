# Farmatodo - API de Tokenización

Sistema de tokenización de tarjetas de crédito con Spring Boot, PostgreSQL y trazabilidad por transacción.

## Requisitos

- Java 17
- Maven 3.8+
- Docker y Docker Compose (para ejecución con contenedores)

## Ejecución con Docker Compose (recomendado)

```bash
# Copiar secrets (si no existe .env)
cp .env.example .env
# Editar .env con APP_API_KEY y ENCRYPTION_KEY reales

# Levantar todo
docker-compose up -d

# Ver logs
docker-compose logs -f app
```

**Servicios:**

| Servicio  | Puerto | Descripción                    |
|-----------|--------|--------------------------------|
| app       | 8080   | API Spring Boot                |
| postgres  | 5432   | Base de datos                  |
| maildev   | 1025 SMTP, 1080 UI | Servidor de correo de desarrollo |

**Validaciones tras `docker-compose up`:**
- Hibernate crea tablas en Postgres (`ddl-auto: update`)
- GET http://localhost:8080/ping → `pong`
- GET http://localhost:8080/health → `{"status":"UP",...}`

## Ejecución local (sin Docker)

```bash
# Postgres en localhost:5432, DB farmatodo
# Crear .env con APP_API_KEY y ENCRYPTION_KEY

mvn clean package
java -jar target/farmatodo-0.0.1-SNAPSHOT.jar
```

## Endpoints

| Método | Ruta           | Auth      | Descripción                  |
|--------|----------------|-----------|------------------------------|
| GET    | /ping          | No        | Disponibilidad               |
| GET    | /health        | No        | Estado del servicio (custom) |
| GET    | /actuator/health | No     | Health completo (Actuator)   |
| GET    | /actuator/health/liveness | No | Liveness (K8s)  |
| GET    | /actuator/health/readiness | No | Readiness (K8s) |
| POST   | /tokens        | X-API-KEY | Crear token de TC            |
| POST   | /clients       | X-API-KEY | Registrar cliente            |
| GET    | /products      | X-API-KEY | Listar productos             |
| POST   | /carts/items   | X-API-KEY | Agregar ítem al carrito      |
| GET    | /carts         | X-API-KEY | Obtener carrito activo       |
| POST   | /orders        | X-API-KEY | Crear pedido (incluye pago)  |

Todos los endpoints protegidos tienen **rate limiting** (60 req/min por IP por defecto). Exceder el límite devuelve **429 Too Many Requests**.

---

### POST /tokens

**Headers:**
- `X-API-KEY`: API Key válida (requerido)

**Body:**
```json
{
  "cardNumber": "4111111111111111",
  "cvv": "123",
  "expiryMonth": "12",
  "expiryYear": "2028",
  "cardHolderName": "JOHN DOE"
}
```

**Respuesta 200:**
```json
{
  "token": "uuid-del-token",
  "maskedPan": "**** **** **** 1111"
}
```

**Respuestas de error:**
- 401: API Key inválida o faltante (incluye `X-Transaction-Id`)
- 400: Validación fallida (payload incorrecto)
- 422: Token rechazado por probabilidad configurable (`TOKEN_REJECT_PROBABILITY`)
- 429: Rate limit excedido

---

### POST /clients

**Headers:**
- `X-API-KEY`: API Key válida (requerido)

**Body:**
```json
{
  "name": "Juan Pérez",
  "email": "juan@ejemplo.com",
  "phone": "+573001234567",
  "address": "Calle 10 #5-20, Bogotá"
}
```

**Respuesta 201:**
```json
{
  "id": "uuid-del-cliente",
  "name": "Juan Pérez",
  "email": "juan@ejemplo.com",
  "phone": "+573001234567",
  "address": "Calle 10 #5-20, Bogotá"
}
```

**Respuestas de error:**
- 401: API Key inválida
- 400: Validación fallida (email inválido, campos vacíos)
- 409: Email o teléfono duplicado

---

### GET /products

**Headers:**
- `X-API-KEY`: API Key válida (requerido)

**Query params (opcional):**
- `minStock`: productos con stock >= valor (ej: `?minStock=5`)

**Respuesta 200:**
```json
[
  {
    "id": "uuid-producto",
    "name": "Paracetamol 500mg",
    "description": "Analgésico y antipirético",
    "price": 3500.00,
    "stock": 100
  }
]
```

**Respuestas de error:**
- 401: API Key inválida
- 429: Rate limit excedido

---

### POST /carts/items

**Headers:**
- `X-API-KEY`: API Key válida (requerido)

**Body:**
```json
{
  "customerId": "uuid-del-cliente",
  "productId": "uuid-del-producto",
  "quantity": 2
}
```

**Respuesta 201:**
```json
{
  "cartId": "uuid-del-carrito",
  "customerId": "uuid-del-cliente",
  "items": [
    {
      "productId": "uuid-producto",
      "quantity": 2,
      "unitPrice": 3500.00,
      "subtotal": 7000.00
    }
  ],
  "total": 7000.00
}
```

**Respuestas de error:**
- 401: API Key inválida
- 400: Cliente o producto inexistente, stock insuficiente
- 429: Rate limit excedido

---

### GET /carts

**Headers:**
- `X-API-KEY`: API Key válida (requerido)

**Query params:**
- `customerId`: UUID del cliente (requerido)

**Respuesta 200:**
```json
{
  "cartId": "uuid-del-carrito",
  "customerId": "uuid-del-cliente",
  "items": [
    {
      "productId": "uuid-producto",
      "quantity": 2,
      "unitPrice": 3500.00,
      "subtotal": 7000.00
    }
  ],
  "total": 7000.00
}
```

**Respuestas de error:**
- 401: API Key inválida
- 400: No hay carrito activo para el cliente
- 429: Rate limit excedido

---

### POST /orders

**Headers:**
- `X-API-KEY`: API Key válida (requerido)

**Body:**
```json
{
  "customerId": "uuid-del-cliente",
  "deliveryAddress": "Calle 10 #5-20, Bogotá",
  "token": "uuid-del-token"
}
```

**Respuesta 201 (pago exitoso):**
```json
{
  "orderId": "uuid-del-pedido",
  "customerId": "uuid-del-cliente",
  "cartId": "uuid-del-carrito",
  "status": "PAID",
  "items": [
    {
      "productId": "uuid-producto",
      "quantity": 2,
      "unitPrice": 3500.00,
      "subtotal": 7000.00
    }
  ],
  "totalAmount": 7000.00,
  "deliveryAddress": "Calle 10 #5-20, Bogotá",
  "tokenId": "uuid-del-token"
}
```

**Respuesta 201 (pago fallido tras reintentos):**
```json
{
  "orderId": "uuid-del-pedido",
  "status": "PAYMENT_FAILED",
  ...
}
```

**Respuestas de error:**
- 401: API Key inválida
- 400: Carrito vacío, token inválido, stock insuficiente, no hay carrito activo
- 429: Rate limit excedido

## Tests

```bash
mvn test
```

Incluye:
- **TokenServiceTest**: unitario de tokenización y rechazo.
- **TokenIntegrationTest**: integración con Testcontainers (Postgres), POST /tokens, persistencia y logs.
- **PaymentIntegrationTest**: flujo completo cliente→token→carrito→pedido con pago fallando; verifica retry, recover, order PAYMENT_FAILED, envío de email y logs (`payment_failed`, `email_sent_payment_failed`).

## Variables de entorno

| Variable                     | Requerido | Descripción                                       |
|------------------------------|-----------|---------------------------------------------------|
| APP_API_KEY                  | Sí        | API Key para endpoints protegidos                 |
| ENCRYPTION_KEY               | Sí        | Clave AES-256 en Base64                           |
| SPRING_DATASOURCE_URL        | No        | Default: `jdbc:postgresql://localhost:5432/farmatodo` |
| SPRING_DATASOURCE_USERNAME   | No        | Default: farmatodo                                |
| SPRING_DATASOURCE_PASSWORD   | No        | Default: farmatodo_pwd                            |
| TOKEN_REJECT_PROBABILITY     | No        | 0.0–1.0, default 0.0                              |
| RATE_LIMIT_REQUESTS_PER_MINUTE | No      | Límite por IP, default 60                         |
| PAYMENT_APPROVE_PROBABILITY  | No        | 0.0–1.0, default 0.7 (simulador de pago)          |

## Validaciones manuales

1. **Token OK:** `POST /tokens` con body válido y `X-API-KEY` → 200, registro en `card_tokens` y `transaction_logs`.
2. **401 sin API Key:** `POST /tokens` sin header → 401 con header `X-Transaction-Id`.
3. **422 rechazo:** `TOKEN_REJECT_PROBABILITY=1` → 422 constante, evento `token_rejected` en `transaction_logs`.
4. **429 Rate limit:** Exceder 60 peticiones/min al mismo endpoint → 429.
5. **Health Actuator:** `GET /actuator/health` → status, db, diskSpace, liveness/readiness.
