# Registro del proyecto

Bitácora de decisiones, tareas completadas, pendientes y errores encontrados durante la construcción de la Iteración 1.

## Decisiones

| # | Decisión | Alternativa descartada | Motivo |
|---|---|---|---|
| D1 | Spring Boot **3.5.15** con Java **17** | Spring Boot 4 | El enunciado exige Spring Boot 3; 3.5 es la última línea 3.x. Java 17 es LTS y compatible. |
| D2 | **MySQL 8.4** (LTS) en Docker | MySQL 8.0 | 8.0 terminó su soporte en 2026; 8.4 es la versión LTS actual de MySQL 8. Las migraciones se ejecutaron y validaron en MySQL 8.0.46 y solo usan SQL compatible con 8.4. |
| D3 | **Flyway** | Liquibase | SQL plano: más fácil de leer para aprender y permite columnas generadas e índices exactos. |
| D4 | `ddl-auto: none` | `update`/`validate` | El esquema lo controla solo Flyway; Hibernate no lo modifica. |
| D5 | **JWT HS256** con `spring-boot-starter-oauth2-resource-server` | Sesiones con cookie; librería jjwt | Usa componentes oficiales de Spring Security, sin dependencias extra. |
| D6 | Doble protección de concurrencia: **índice único sobre columna generada** + **`SELECT … FOR UPDATE`** | Solo validación en Java | RN-10 exige protección en backend **y** base de datos. |
| D7 | `MovimientoRepository` y `NovedadRepository` extienden `Repository` (sin `delete`) | `JpaRepository` | RN-08: no existe forma de borrar históricos desde la aplicación. |
| D8 | Códigos HTTP: 409 conflicto de estado, 422 regla de negocio, 400 formato | Todo 400 | El frontend puede reaccionar distinto (p. ej. recargar celdas ante un 409). |
| D9 | Pruebas de integración con **Testcontainers + MySQL real** | H2 en memoria | H2 no reproduce columnas generadas, bloqueos ni índices de MySQL; las pruebas de concurrencia no serían reales. |
| D10 | Docker Compose con Nginx como proxy de `/api` | Servir el frontend desde Spring | Mismo origen (sin CORS), separación clara frontend/backend. |
| D11 | `JWT_SECRET` vacío → secreto aleatorio temporal | Secreto fijo en el repositorio | Funciona "descargar y ejecutar" sin subir secretos. |
| D12 | Entrega completa en una sola vez | Trabajo por fases con aprobación | Lo pidió así el usuario; el contenido de cada fase quedó documentado aquí y en `arquitectura.md`. |

## Tareas completadas

- [x] Fase 1 — Análisis, supuestos, arquitectura, modelo de datos (`arquitectura.md`).
- [x] Fase 2 — Proyecto Maven, configuración, migraciones V1/V2 validadas en MySQL real.
- [x] Fase 3 — Backend: dominio, servicios, seguridad, controladores, manejo de errores, OpenAPI; pruebas unitarias e integración escritas.
- [x] Fase 4 — Frontend: 8 pantallas + login, conexión a la API, estados de carga/vacío/éxito/error, confirmaciones, responsive.
- [x] Fase 5 — Pruebas de frontend ejecutadas (23/23). Docker Compose y guía de arranque.
- [x] Fase 6 — README, migraciones SQL, colección Postman, OpenAPI (Swagger), credenciales demo, evidencias.

## Pendientes

- [ ] **Ejecutar `mvn test` y `docker compose up --build` por primera vez en un equipo con acceso a Maven Central** (ver "Errores y limitaciones").
- [ ] Pantallas para gestionar clientes, vehículos y usuarios (fuera de alcance de la Iteración 1).
- [ ] Pagos y facturación (Iteración 2).
- [ ] Paginación real en el historial (hoy se limita a 500 filas).

## Errores y limitaciones encontrados

| Fecha | Situación | Qué se hizo |
|---|---|---|
| 2026-11-08 | El entorno de construcción **no tenía acceso a Maven Central** (bloqueado por red) ni a Docker Hub. | El backend se revisó con `javac` (sintaxis y referencias internas) y todo el SQL se ejecutó en MySQL real. La compilación y las pruebas Java deben correrse en su equipo. |
| 2026-11-08 | Hibernate 6 en MySQL espera columnas `ENUM` nativas para `@Enumerated(STRING)` y la validación de esquema podría fallar. | Se usa `@JdbcTypeCode(SqlTypes.VARCHAR)` y `ddl-auto: none`. |
| 2026-11-08 | Docker Engine 29 dejó de aceptar la API antigua que usa Testcontainers 1.x. | Se agregó `src/test/resources/docker-java.properties` con `api.version=1.44`. |
| 2026-11-08 | Con `serverTimezone` en la URL JDBC y JVM en UTC, las horas `LocalDateTime` se desplazarían 5 horas. | Se eliminó `serverTimezone`; la hora se calcula con `Clock` en zona de Colombia. |
| 2026-11-08 | Tipado de pruebas del frontend (`error` de tipo `unknown`) rompía `tsc`. | Se corrigió con conversión explícita a `ApiError`. |

### Si algo falla al compilar o probar

1. Copie el mensaje de error completo (`docker compose logs backend` o la salida de `mvn test`).
2. Indique su sistema operativo y versiones (`docker --version`, `java -version`, `mvn -v`).
3. La mayoría de problemas de esta iteración serían de versiones de dependencias: se corrigen en `backend/pom.xml` sin tocar el resto del código.
