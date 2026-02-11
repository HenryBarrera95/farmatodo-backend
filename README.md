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

| Método | Ruta    | Auth        | Descripción        |
|--------|---------|-------------|--------------------|
| GET    | /ping   | No          | Disponibilidad     |
| GET    | /health | No          | Estado del servicio|
| POST   | /tokens | X-API-KEY   | Crear token de TC  |

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

## Variables de entorno

| Variable                 | Requerido | Descripción                    |
|--------------------------|-----------|--------------------------------|
| APP_API_KEY              | Sí        | API Key para /tokens           |
| ENCRYPTION_KEY           | Sí        | Clave AES-256 en Base64        |
| SPRING_DATASOURCE_URL    | No        | Default: `jdbc:postgresql://localhost:5432/farmatodo` |
| SPRING_DATASOURCE_USERNAME | No      | Default: farmatodo             |
| SPRING_DATASOURCE_PASSWORD | No      | Default: farmatodo_pwd         |
| TOKEN_REJECT_PROBABILITY | No        | 0.0–1.0, default 0.0          |

## Validaciones manuales

1. **Token OK:** `POST /tokens` con body válido y `X-API-KEY` → 200, registro en `card_tokens` y `transaction_logs`.
2. **401 sin API Key:** `POST /tokens` sin header → 401 con header `X-Transaction-Id`.
3. **422 rechazo:** `TOKEN_REJECT_PROBABILITY=1` → 422 constante, evento `token_rejected` en `transaction_logs`.
