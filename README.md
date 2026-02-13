# Farmatodo - API de Tokenización

Sistema de tokenización de tarjetas de crédito con Spring Boot, PostgreSQL y trazabilidad por transacción.

## Diagrama de arquitectura

```
                    ┌─────────────────────────────────────────────────────────┐
                    │                      Cliente HTTP                        │
                    └────────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│  TxFilter (1) → RateLimitFilter (2) → ApiKeyFilter (3)                                            │
│  tx_id en MDC │ 60 req/min por IP    │ X-API-KEY válido                                            │
└────────────────────────────────────────────┬───────────────────────────────────────────────────────┘
                                             │
                                             ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│  Controllers (Token, Client, Product, Cart, Order)                                                  │
│  @Valid │ ResponseEntity │ DTOs                                                                     │
└────────────────────────────────────────────┬───────────────────────────────────────────────────────┘
                                             │
                                             ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│  Services (Token, Customer, Product, Cart, Order, Payment)                                           │
│  Lógica de negocio │ @Transactional │ Eventos (ProductSearch)                                      │
└────────────────────────────────────────────┬───────────────────────────────────────────────────────┘
                                             │
         ┌──────────────────────────────────┼──────────────────────────────────┐
         ▼                                  ▼                                  ▼
┌─────────────────┐              ┌──────────────────┐              ┌─────────────────┐
│  JPA Repos      │              │  LogService       │              │  EmailService    │
│  PostgreSQL     │              │  transaction_logs │              │  Resend SMTP     │
└─────────────────┘              └──────────────────┘              └─────────────────┘
```

## Decisiones de diseño

| Decisión | Elección | Justificación |
|----------|----------|---------------|
| Encriptación | AES-256-GCM | Algoritmo autenticado, resistente a manipulación; IV aleatorio por operación |
| Formato token | UUIDv4 | Único, no correlacionable con el PAN |
| Autenticación | API Key (header X-API-KEY) | Simple, suficiente para el alcance; extensible a JWT si se requiere |
| Persistencia token | ciphertext + iv + authTag; no CVV | Cumple requerimientos PCI: datos sensibles cifrados, CVV nunca persiste |
| Rechazo probabilístico | Antes de cifrar | Evita escribir en BD operaciones rechazadas por negocio |
| Trazabilidad | tx_id en MDC + transaction_logs | UUID por request en logs y respuestas; trazabilidad end-to-end |
| Pago | Simulador con probabilidad configurable | Sin pasarela real; permite probar flujo completo y reintentos |
| Rate limiting | Bucket4j + Caffeine (TTL 15 min) | Límite por IP; buckets con expiración para evitar memory leak |

## Entorno de despliegue utilizado para esta entrega

| Componente | Tecnología |
|------------|------------|
| Compute | Cloud Run |
| Base de datos | Supabase (PostgreSQL) |
| Email | Resend |

*Nota: Si el alcance no exige deploy real, este stack puede considerarse infraestructura de entrega y no una decisión de diseño del sistema.*

## Supuestos

- **Uso interno / MVP:** La API está pensada para integración con frontends o sistemas internos, no como API pública masiva.
- **API Key estática:** Se asume una o pocas API keys por entorno; rotación manual si aplica.
- **Simulador de pago:** No hay integración con pasarelas reales; el pago es simulado para demostrar flujo.
- **Un carrito activo por cliente:** Un cliente tiene como máximo un carrito en estado ACTIVE a la vez.
- **Token de tarjeta previamente creado:** Para crear un pedido se requiere un token vigente del flujo de tokenización.
- **Email operacional:** Los emails (éxito/fallo de pago) se envían a la dirección del cliente registrada; no hay plantillas HTML avanzadas.
- **Productos precargados:** El seeder crea productos de ejemplo; en producción se cargarían desde un proceso o admin.

## Limitaciones

- **Sin búsqueda por texto:** Los productos se filtran solo por `minStock`; no hay búsqueda por nombre o criterios.
- **Un solo carrito activo:** No se soportan múltiples carritos en paralelo por cliente.
- **CVV no validado contra token:** El token guarda solo el PAN cifrado; no se valida relación token–CVV en el pedido.
- **Sin control de concurrencia de stock:** No hay optimistic locking; bajo alta carga concurrente, múltiples pedidos podrían comprometer stock (race condition).
- **Rate limit por IP:** En NAT/proxies, múltiples usuarios pueden compartir la misma IP y el límite.
- **ddl-auto: update:** Hibernate gestiona el esquema; para producción estricta se recomiendan migraciones explícitas (Flyway/Liquibase).
- **Logs de búsqueda minimalistas:** Solo se registra minStock y tx_id; no se guarda texto de búsqueda.

## Consideraciones para producción

| Aspecto | Recomendación |
|---------|---------------|
| **ddl-auto** | Cambiar a `validate` o `none`; usar Flyway/Liquibase para cambios de esquema |
| **Secrets** | Usar Secret Manager (GCP) en lugar de variables de entorno para claves sensibles |
| **API Key** | Implementar rotación; considerar JWT para múltiples clientes |
| **Pasarela de pago** | Integrar un proveedor real (Stripe, Mercado Pago, etc.) reemplazando el simulador |
| **Monitoreo** | Configurar alertas en Cloud Run; revisar métricas de latencia y errores |
| **Backups** | Supabase ofrece backups; definir política de retención según SLA |
| **CORS** | Configurar orígenes permitidos si hay frontend en otro dominio |
| **Logs** | Reducir nivel a INFO o WARN en producción; considerar envío a sistema centralizado |
| **Health checks** | Cloud Run usa `/actuator/health/liveness` y `readiness`; verificar umbrales |

## Módulos principales

| Módulo | Descripción |
|--------|-------------|
| **Tokenización** | Encriptación de datos de tarjeta, token UUID, masked PAN, rechazo por probabilidad |
| **Orders** | Creación de pedidos desde carrito; estados: `PAYMENT_PENDING`, `PAID`, `PAYMENT_FAILED` |
| **Payment** | Simulador de pago con probabilidad configurable; integrado en el flujo del pedido |
| **Retry** | Reintentos automáticos en pago fallido (backoff exponencial); email al fallar definitivamente |
| **Rate limiting** | Bucket por IP (Bucket4j); 60 req/min por defecto en endpoints protegidos |

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

## Despliegue en Cloud Run

```powershell
.\deploy.ps1
```

Requisitos: Docker Desktop, gcloud CLI autenticado, `.env` configurado. El script construye la imagen, la sube a Artifact Registry y despliega en Cloud Run. Ver `deploy.ps1` para el flujo detallado.

## Endpoints

| Método | Ruta           | Auth      | Descripción                  |
|--------|----------------|-----------|------------------------------|
| GET    | /ping          | No        | Disponibilidad               |
| GET    | /health        | No        | Estado del servicio (liviano, para balanceadores) |
| GET    | /actuator/health | No     | Health detallado (db, mail, disk, probes)         |
| GET    | /actuator/health/liveness | No | Liveness (K8s)  |
| GET    | /actuator/health/readiness | No | Readiness (K8s) |
| POST   | /tokens        | X-API-KEY | Crear token de TC            |

*`/health` se mantiene como endpoint liviano para balanceadores simples; `/actuator/health` expone información detallada (db, mail, disk, probes).*
| POST   | /clients       | X-API-KEY | Registrar cliente            |
| GET    | /products      | X-API-KEY | Listar productos             |
| POST   | /carts/items   | X-API-KEY | Agregar ítem al carrito      |
| GET    | /carts         | X-API-KEY | Obtener carrito activo       |
| POST   | /orders        | X-API-KEY | Crear pedido (incluye pago)  |

---

### Rate limiting

Los endpoints protegidos (`/tokens`, `/clients`, `/products`, `/carts`, `/orders`) aplican **rate limiting por IP** con Bucket4j:

- **Límite por defecto:** 60 peticiones por minuto por IP
- **Clave:** IP del cliente (`X-Forwarded-For` si existe, si no `RemoteAddr`)
- **Respuesta 429:** `{"error":"Too many requests. Rate limit exceeded."}`
- **Variable:** `RATE_LIMIT_REQUESTS_PER_MINUTE` (default 60)

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

---

### Payment y Retry

Al crear un pedido (`POST /orders`), el pago se procesa automáticamente con un **simulador** configurable:

| Aspecto | Configuración |
|----------|---------------|
| **Probabilidad de aprobación** | `PAYMENT_APPROVE_PROBABILITY` (0.0–1.0, default 0.7) |
| **Reintentos** | `PAYMENT_RETRY_MAX_ATTEMPTS` (default 3) |
| **Delay inicial** | `PAYMENT_RETRY_DELAY` ms (default 1000) |
| **Multiplicador backoff** | `PAYMENT_RETRY_MULTIPLIER` (default 2) |

**Flujo:**
1. Orden creada → estado `PAYMENT_PENDING`
2. Simulador intenta pago (aprueba/rechaza según probabilidad)
3. Si falla: retry con backoff (delay × multiplier^intento)
4. Si todos fallan: orden `PAYMENT_FAILED`, email al cliente
5. Si aprueba: orden `PAID`, descuenta stock, email de confirmación

**Para pruebas:** `PAYMENT_APPROVE_PROBABILITY=0` fuerza fallo y permite ver retry + email.

## Colección Postman

En `postman/Farmatodo API.postman_collection.json` tienes una colección para probar todos los endpoints. Importarla en Postman y configurar `apiKey` (igual que `APP_API_KEY` en `.env`). Ver `postman/README.md` para el flujo recomendado.

## Tests

```bash
mvn test
```

### Cobertura (JaCoCo)

- **Umbral:** 80% líneas cubiertas (el build falla si no se cumple)
- **Reporte:** `target/site/jacoco/index.html`

**Importante:** `docker compose up -d` levanta la app en producción, **no ejecuta tests**.

| Comando | Uso |
|--------|-----|
| `mvn test` | Todos los tests + verificación de cobertura (requiere Docker para integración) |
| `mvn test -Pno-docker` | Solo tests unitarios (excluye `@Tag("integration")`) |
| `mvn test -Pno-docker "-Djacoco.check.skip=true"` | Sin fallar si cobertura &lt; 80% |

**Sin Maven local** (con Docker):
```powershell
docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn test -Pno-docker "-Djacoco.check.skip=true"
```

### Tests incluidos

| Clase | Tipo | Descripción |
|-------|------|-------------|
| CustomerServiceTest, CustomerControllerTest | Unit / WebMvc | Cliente: create, conflictos email/teléfono |
| ProductServiceTest, ProductControllerTest | Unit / WebMvc | Productos: search, minStock |
| CartServiceTest, CartControllerTest | Unit / WebMvc | Carrito: addItem, getCart |
| OrderServiceTest, OrderControllerTest | Unit / WebMvc | Pedidos: createOrderAndCart, toResponse |
| PaymentServiceTest | Unit | Pago: process, recover, retry |
| TokenServiceTest, TokenControllerTest | Unit / WebMvc | Tokenización: éxito, rechazo, maskedPan |
| GlobalExceptionHandlerTest | WebMvc | Excepciones: Token, Order, Cart, Conflict, Validation, 500 |
| LogServiceTest, EmailServiceTest | Unit | Log y correo |
| AesEncryptionServiceTest | Unit | Encriptación AES |
| HealthControllerTest | WebMvc | /ping, /health |
| TokenIntegrationTest | Integración (@Tag) | Postgres + Testcontainers, POST /tokens |
| PaymentIntegrationTest | Integración (@Tag) | Flujo completo, retry, email en fallo |

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
| PAYMENT_RETRY_MAX_ATTEMPTS   | No        | Reintentos antes de fallar, default 3              |
| PAYMENT_RETRY_DELAY          | No        | Delay inicial en ms, default 1000                  |
| PAYMENT_RETRY_MULTIPLIER     | No        | Multiplicador backoff exponencial, default 2      |
| SPRING_MAIL_HOST             | No        | SMTP host (ej. smtp.resend.com, localhost para MailDev) |
| SPRING_MAIL_PORT             | No        | SMTP port (465 Resend, 1025 MailDev)              |
| SPRING_MAIL_USERNAME         | No        | Usuario SMTP (resend para Resend)                  |
| SPRING_MAIL_PASSWORD         | No        | API key Resend o vacío para MailDev                |
| SPRING_MAIL_SSL_ENABLE       | No        | true para Resend (puerto 465)                      |

## Validaciones manuales

1. **Token OK:** `POST /tokens` con body válido y `X-API-KEY` → 200, registro en `card_tokens` y `transaction_logs`.
2. **401 sin API Key:** `POST /tokens` sin header → 401 con header `X-Transaction-Id`.
3. **422 rechazo:** `TOKEN_REJECT_PROBABILITY=1` → 422 constante, evento `token_rejected` en `transaction_logs`.
4. **429 Rate limit:** Exceder 60 peticiones/min al mismo endpoint → 429.
5. **Health Actuator:** `GET /actuator/health` → status, db, diskSpace, liveness/readiness.
6. **Payment retry:** Flujo cliente→token→carrito→pedido con `PAYMENT_APPROVE_PROBABILITY=0` → orden `PAYMENT_FAILED`, email recibido en Maildev.
