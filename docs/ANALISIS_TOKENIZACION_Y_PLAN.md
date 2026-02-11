# Análisis Exhaustivo: Tokenización y Plan de Trabajo

**Proyecto:** Farmatodo - Reto Técnico Backend  
**Fecha:** 2025-02-11  
**Alcance:** Tokenización de tarjetas + Infraestructura base

---

## 1. REQUERIMIENTOS FUNCIONALES Y NO FUNCIONALES

### Del reto técnico (Requerimientos 1 y 2)

| ID | Requerimiento | Fuente |
|----|---------------|--------|
| RF-1.1 | Recibir datos de tarjeta (número, CVV, vencimiento, nombre) y devolver token único | Reto |
| RF-1.2 | API autenticada por API Key o Secret Key | Reto |
| RF-1.3 | Probabilidad de rechazo configurable (rechazar si random < porcentaje) | Reto |
| RF-2.1 | Endpoint `/ping` → `pong` 200 | Reto |
| RNF-S1 | Autenticación API Key / Secret / JWT | Reto |
| RNF-S2 | Encriptar datos sensibles de tarjetas (elegir algoritmo adecuado) | Reto |
| RNF-R1 | Control de errores y validaciones exhaustivas | Reto |
| RNF-R2 | Logs centralizados: eventos con UUID por transacción en BD | Reto |

### Decisiones técnicas explícitas (tuyas)

| Decisión | Valor |
|----------|--------|
| Algoritmo | AES-256-GCM |
| IV | Aleatorio por operación (12 bytes para GCM) |
| Persistir | ciphertext, iv, authTag |
| No persistir | CVV |
| Respuesta | masked PAN |
| Formato token | UUIDv4 |
| Rechazo probabilístico | Antes de persistir |
| tx_id | Del MDC en todo el flujo |
| Logs | Insertar evento en BD |
| Errores | GlobalExceptionHandler |
| Infra | docker-compose (DB vars), MailDev para mail |
| GCP | No por ahora |

---

## 2. FLUJO REQUERIDO (orden exacto)

```
[Request POST /tokens]

1. TxFilter
   └─ Generar UUID → MDC.put("tx_id") → response header X-Transaction-Id
   
2. ApiKeyFilter (solo /tokens)
   └─ Header X-API-KEY válido?
   └─ NO → 401, no continuar
   └─ SÍ → continuar

3. Controller
   └─ @Valid CreateTokenRequest
   └─ Validación falla → GlobalExceptionHandler → 400

4. TokenService.createToken()
   ├─ 4a. Obtener tx_id del MDC
   ├─ 4b. Aplicar rechazo probabilístico
   │     └─ v < rejectProbability → log → TokenRejectedException → 422
   ├─ 4c. Si aprobado:
   │     ├─ IV aleatorio (12 bytes)
   │     ├─ Encriptar PAN (AES-256-GCM) → ciphertext + authTag
   │     ├─ Generar token UUIDv4
   │     ├─ maskPan(pan) → "**** **** **** 1234"
   │     ├─ Persistir CardToken (ciphertext, iv, authTag, maskedPan, txId)
   │     ├─ Insertar evento en logs (con tx_id)
   │     └─ log.info("Token created", token, tx)
   └─ 4d. Retornar token + masked PAN

5. Controller
   └─ 200 OK { token, maskedPan }
```

**Punto crítico:** El rechazo probabilístico debe ocurrir **antes** de cifrar y persistir. Así evitamos escribir en BD operaciones que el negocio rechazó.

---

## 3. ARQUITECTURA ACTUAL

### Stack tecnológico

| Capa | Tecnología | Justificación |
|------|------------|---------------|
| Framework | Spring Boot 3.1.6 | Requerido por el reto, ecosistema maduro |
| JDK | Java 17 | LTS, records, mejoras de productividad |
| Persistencia | Spring Data JPA + PostgreSQL | Relacional, soporte JSON si se necesita |
| Validación | Jakarta Validation (@Valid) | Declarativa, integrada con Spring |
| Cifrado | AES-256-GCM (javax.crypto) | Autenticado, resistente a tampering |
| Configuración | YAML + env vars | Secretos fuera del código |
| Logging | SLF4J + MDC | Trazabilidad por request |

### Patrón arquitectónico: **Layered (en capas)**

```
┌─────────────────────────────────────────────────────────────┐
│  Filters (TxFilter, ApiKeyFilter)                            │  ← Cross-cutting
├─────────────────────────────────────────────────────────────┤
│  Controllers (TokenController, HealthController, Ping)       │  ← HTTP
├─────────────────────────────────────────────────────────────┤
│  Services (TokenService)                                     │  ← Lógica de negocio
├─────────────────────────────────────────────────────────────┤
│  Repositories (CardTokenRepository)                          │  ← Persistencia
├─────────────────────────────────────────────────────────────┤
│  Entities (CardToken)                                        │  ← Modelo de datos
└─────────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│  Utils / Config (CryptoUtils, GlobalExceptionHandler)        │
└─────────────────────────────────────────────────────────────┘
```

**Por qué esta arquitectura:**

1. **Separación clara:** Controladores delgados, lógica en servicios.
2. **Testeable:** Inyección de dependencias permite mocks.
3. **Escalable:** Fácil añadir capas (ej. eventos, auditoría).
4. **Spring Boot nativo:** Filtros, validación y excepciones globales ya integrados.

### Orden de filtros (crítico)

Para que `tx_id` exista en **todas** las respuestas (incluso 401), el orden debe ser:

| Orden | Filter | Responsabilidad |
|-------|--------|------------------|
| 1 | **TxFilter** | Asignar tx_id al MDC y header antes de cualquier otra lógica |
| 2 | **ApiKeyFilter** | Validar API Key en rutas protegidas |

**Estado actual:** No hay `@Order` explícito. Spring Boot ordena por nombre de bean (`apiKeyFilter` < `txFilter`), por lo que **ApiKeyFilter ejecuta antes** y las rechazos 401 no tendrían `tx_id`. Esto debe corregirse.

---

## 4. GAP ANALYSIS

### 4.1 Decisiones técnicas vs implementación actual

| Requerimiento | Estado | Detalle |
|---------------|--------|---------|
| AES-256-GCM | ✅ | `CryptoUtils` usa AES/GCM/NoPadding |
| IV aleatorio por operación | ✅ | `CryptoUtils.randomIv()` con SecureRandom |
| Persistir ciphertext, iv, authTag | ✅ | `CardToken` los tiene |
| No persistir CVV | ✅ | Solo se valida en request, nunca se guarda |
| Devolver masked PAN | ✅ | `CreateTokenResponse(token, maskedPan)` |
| Token UUIDv4 | ✅ | `UUID.randomUUID().toString()` |
| Rechazo antes de persistir | ✅ | Líneas 42–46 en TokenService, antes del try |
| tx_id del MDC | ✅ | Se usa en servicio y se persiste en CardToken |
| GlobalExceptionHandler | ✅ | TokenRejectedException → 422 |
| Orden de filtros | ⚠️ | Falta asegurar TxFilter antes que ApiKeyFilter |
| Insertar evento en logs (BD) | ❌ | Solo logs a consola, sin tabla de eventos |

### 4.2 Flujo actual vs flujo requerido

| Paso | Requerido | Actual | Coincide |
|------|-----------|--------|----------|
| 1 | Validar API Key | ApiKeyFilter | ⚠️ (orden con TxFilter) |
| 2 | Validar payload | @Valid en Controller | ✅ |
| 3 | Rechazo probabilístico | En TokenService, antes de cifrar | ✅ |
| 4 | Si rechazo → log + 4xx | TokenRejectedException → 422 | ✅ |
| 5 | Cifrar PAN | CryptoUtils.encrypt | ✅ |
| 6 | Generar UUID token | UUID.randomUUID() | ✅ |
| 7 | Persistir | repo.save(entity) | ✅ |
| 8 | Log con tx_id | log.info | ✅ |
| 9 | Evento en BD | — | ❌ |
| 10 | Retornar token + masked PAN | CreateTokenResponse | ✅ |

### 4.3 Pendientes de infraestructura

| Elemento | Estado |
|----------|--------|
| docker-compose.yml | ❌ No existe |
| Variables DB desde docker-compose | ❌ |
| MailDev config para mail | ❌ |
| Colección Postman/Bruno | ❌ |
| Tests unitarios | ❌ |
| .gitignore | ❌ |

### 4.4 Mejoras menores detectadas

1. **Charset:** `req.getCardNumber().getBytes()` usa charset por defecto. Preferible `StandardCharsets.UTF_8`.
2. **`Random` vs `SecureRandom`:** Para probabilidad de rechazo no importa, pero se podría documentar.
3. **Tabla de logs:** El reto pide “registrar eventos en BD”. Opciones: tabla `transaction_logs` o incluir `card_tokens` como registro de eventos de tokenización. La segunda opción cumple el mínimo; la primera da más flexibilidad para otros eventos.

---

## 5. RESUMEN DE CUMPLIMIENTO

| Categoría | Cumplido | Pendiente |
|-----------|----------|-----------|
| Cifrado y persistencia | 100% | — |
| Flujo de negocio | 95% | Orden filtros, logs en BD |
| Seguridad | 100% | — |
| Manejo de errores | 100% | — |
| Infraestructura | 0% | docker-compose, MailDev |
| Tests y colección | 0% | — |

---

## 6. PLAN DE TRABAJO

### Fase 0: Correcciones críticas (antes de docker-compose)

| # | Tarea | Esfuerzo | Prioridad |
|---|-------|----------|-----------|
| 0.1 | Garantizar orden de filtros (TxFilter antes que ApiKeyFilter) | Bajo | Alta |
| 0.2 | Definir si habrá tabla `transaction_logs` o si `card_tokens.tx_id` basta para el reto | Bajo | Media |
| 0.3 | Opcional: usar `StandardCharsets.UTF_8` al cifrar | Bajo | Baja |

### Fase 1: Tokenización estable y probada localmente

| # | Tarea | Esfuerzo | Prioridad |
|---|-------|----------|-----------|
| 1.1 | Crear `docker-compose.yml`: Postgres, app, MailDev (puerto 1025) | Medio | Alta |
| 1.2 | Configurar variables de entorno desde docker-compose (DB, ENCRYPTION_KEY, APP_API_KEY) | Bajo | Alta |
| 1.3 | Probar arranque completo: `docker-compose up` | Bajo | Alta |
| 1.4 | Verificar que `card_tokens` se crea y se persisten registros correctamente | Bajo | Alta |
| 1.5 | Verificar que `tx_id` se guarda en `card_tokens` y aparece en header de respuesta | Bajo | Alta |
| 1.6 | Probar rechazo probabilístico (configurar 1.0, confirmar 422 constante) | Bajo | Alta |
| 1.7 | Incluir `.env.example` sin secretos reales | Bajo | Media |

### Fase 2: Documentación y pruebas manuales

| # | Tarea | Esfuerzo | Prioridad |
|---|-------|----------|-----------|
| 2.1 | Actualizar README: cómo ejecutar con docker-compose, endpoints, variables | Medio | Alta |
| 2.2 | Crear colección Bruno/Postman para `POST /tokens` y `GET /ping`, `GET /health` | Medio | Alta |
| 2.3 | Documentar prompts de IA usados (si aplica) | Bajo | Según reto |

### Fase 3: Tests (alineado con reto)

| # | Tarea | Esfuerzo | Prioridad |
|---|-------|----------|-----------|
| 3.1 | Tests unitarios TokenService (≥80% cobertura en tokenización) | Alto | Alta |
| 3.2 | Tests de integración del endpoint `POST /tokens` | Medio | Alta |

### Fase 4: Siguientes módulos (después de tokenización validada)

| # | Tarea | Dependencias |
|---|-------|--------------|
| 4.1 | Gestión de clientes | Tokenización OK |
| 4.2 | Productos y búsqueda asíncrona | Clientes (opcional) |
| 4.3 | Carrito | Productos |
| 4.4 | Pedidos y pagos con reintentos | Carrito, Mail |
| 4.5 | Notificaciones por correo (MailDev) | Pedidos |

---

## 7. ORDEN DE EJECUCIÓN INMEDIATO

```
1. Fase 0.1 → Orden de filtros (@Order)
2. Fase 1.1–1.2 → docker-compose + variables
3. Fase 1.3–1.6 → Validaciones manuales
4. Fase 2.1–2.2 → README + colección
5. Fase 3 → Tests

Luego → Clientes (Fase 4.1)
```

---

## 8. DECISIONES TOMADAS (post-implementación Fase 0)

1. **Tabla transaction_logs:** ✅ Implementada en Fase 0. Estructura: id (UUID), tx_id, event_type, level, message, payload (jsonb), created_at.
2. **MailDev:** Incluido en el mismo docker-compose (pendiente implementación).
3. **Tests:** TokenService antes de avanzar a clientes. No full cobertura aún.
4. **LogService:** Opción A — servicio explícito llamado desde cada Service.

---

## 9. FASE 0 IMPLEMENTADA (2025-02-11)

- [x] @Order(1) en TxFilter
- [x] @Order(2) en ApiKeyFilter
- [x] Entidad TransactionLog + TransactionLogRepository
- [x] LogService con @Transactional(REQUIRES_NEW) para commits independientes
- [x] token_rejected → insert en transaction_logs antes de lanzar excepción
- [x] token_created → insert en transaction_logs después de repo.save
- [x] @Transactional en createToken (card + log en mismo commit para éxito)
