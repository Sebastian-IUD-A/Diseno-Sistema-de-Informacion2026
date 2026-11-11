# Autos Colombia · Sistema de parqueadero (Iteración 1)

Sistema web para administrar un parqueadero que trabaja por **mensualidad**: registra entradas y salidas de vehículos, asigna celdas, muestra la ocupación en tiempo real, guarda el historial y permite anotar novedades (daños, objetos olvidados, observaciones) sobre cada permanencia.

|Capa|Tecnología|
|-|-|
|Backend|Java 17 · Spring Boot 3.5 (Web, Data JPA, Security, Validation) · JWT|
|Frontend|React 19 · Vite · TypeScript|
|Base de datos|MySQL 8.4 · migraciones con Flyway|
|Pruebas|JUnit 5 · Mockito · Testcontainers (MySQL real) · Vitest · React Testing Library|
|Despliegue local|Docker Compose (un solo comando)|

\---

## 1\. Inicio rápido (recomendado): un solo comando con Docker

**Requisito único:** tener instalado [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows, macOS o Linux) y abierto. No necesita instalar Java, Node ni MySQL.

1. Descomprima el proyecto y abra una terminal **dentro de la carpeta `autos-colombia`** (la que contiene `docker-compose.yml`).

   * Windows: en el Explorador, entre a la carpeta, escriba `cmd` en la barra de direcciones y presione Enter.
   * macOS/Linux: `cd ruta/a/autos-colombia`
2. Ejecute:

```bash
   docker compose up --build
   ```

   La primera vez tarda varios minutos (descarga imágenes y dependencias, compila el backend y el frontend). Está listo cuando en la terminal aparece una línea del backend parecida a `Started ParqueaderoApplication`.

3. Abra en el navegador: **http://localhost:3000**

### Credenciales de demostración

|Rol|Correo|Contraseña|
|-|-|-|
|Operador|`operador@autoscolombia.co`|`Operador123\*`|
|Administrador|`admin@autoscolombia.co`|`Admin123\*`|

> Son datos de prueba creados por la migración `V2\_\_datos\_demo.sql`. Las contraseñas se guardan como hash BCrypt, nunca en texto plano.

### Direcciones útiles

|Qué|URL|
|-|-|
|Aplicación web|http://localhost:3000|
|API REST|http://localhost:8080/api|
|Documentación interactiva (Swagger / OpenAPI)|http://localhost:8080/swagger-ui.html|
|Especificación OpenAPI en JSON|http://localhost:8080/v3/api-docs|
|Estado del backend|http://localhost:8080/actuator/health|
|MySQL (desde su equipo, p. ej. con Workbench)|`localhost:3307` · usuario `autos` · clave `autos\_dev` · BD `autos\_colombia`|

### Detener, reiniciar y borrar datos

```bash
# Detener (en la misma terminal): Ctrl + C, y luego:
docker compose down

# Volver a iniciar (más rápido que la primera vez):
docker compose up

# Borrar la base de datos y empezar con los datos demo otra vez:
docker compose down -v
docker compose up --build
```

\---

## 2\. Guion de demostración (5 minutos)

Datos demo: `ABC123` (vigente), `DEF456` (vigente), `GHI78D` (moto vigente), `JKL012` (ya está dentro en la celda A-02), `MNO345` (**mensualidad vencida**), `PQR67E` (**vehículo inactivo**), `STU901` (vence en 5 días). La celda `A-08` está fuera de servicio.

1. **Operador** → *Registrar entrada* → placa `abc123` (con espacios y minúsculas: se normaliza) → elija una celda → confirme.
2. Intente la entrada de `MNO345` → el sistema muestra *“La mensualidad … venció”* y no ofrece celdas.
3. Intente la entrada de `JKL012` → *“ya se encuentra dentro”* con enlace a registrar la salida.
4. *Ocupación actual* → filtre por `AB` → *Novedades* → registre una novedad (intente enviarla vacía: se rechaza).
5. *Registrar salida* → busque `ABC123` → confirme → la celda vuelve a *Disponible*.
6. Cierre sesión e ingrese como **Administrador** → *Historial* (filtre por `ABC123` y fechas) → *Administrar celdas*: cree una celda, ponga una disponible *Fuera de servicio* y compruebe que una celda **ocupada** no se puede modificar (RN-09).
7. Seguridad: el administrador no ve *Registrar entrada*; aunque llame la API directamente, el backend responde **403**.

\---

## 3\. Ejecutar sin Docker (modo desarrollo)

Útil para programar con recarga automática. Requisitos: **Java 17+**, **Maven 3.9+**, **Node 20.19+ (o 22)** y **MySQL 8**.

**a) Base de datos.** Puede usar solo el contenedor de MySQL del proyecto:

```bash
docker compose up mysql
```

o su propio MySQL, creando la base y el usuario:

```sql
CREATE DATABASE autos\_colombia CHARACTER SET utf8mb4 COLLATE utf8mb4\_0900\_ai\_ci;
CREATE USER 'autos'@'localhost' IDENTIFIED BY 'autos\_dev';
GRANT ALL PRIVILEGES ON autos\_colombia.\* TO 'autos'@'localhost';
```

**b) Backend** (Flyway crea las tablas y los datos demo al arrancar):

```bash
cd backend
# Si usa el MySQL del docker compose (puerto 3307):
#   Linux/macOS:  export DB\_URL="jdbc:mysql://localhost:3307/autos\_colombia?useSSL=false\&allowPublicKeyRetrieval=true"
#   Windows (PowerShell):  $env:DB\_URL="jdbc:mysql://localhost:3307/autos\_colombia?useSSL=false\&allowPublicKeyRetrieval=true"
mvn spring-boot:run
```

**c) Frontend** (en otra terminal):

```bash
cd frontend
npm install
npm run dev
```

Abra **http://localhost:5173**. Vite reenvía `/api` al backend en `:8080`.

### Variables de entorno

|Variable|Para qué sirve|Valor por defecto|
|-|-|-|
|`DB\_URL`|Conexión JDBC a MySQL|`jdbc:mysql://localhost:3306/autos\_colombia?...`|
|`DB\_USER` / `DB\_PASSWORD`|Usuario y clave de MySQL|`autos` / `autos\_dev` (solo demo)|
|`JWT\_SECRET`|Secreto para firmar tokens (mín. 32 caracteres)|vacío → se genera uno temporal en cada arranque|
|`JWT\_EXPIRACION\_MINUTOS`|Duración de la sesión|`480` (un turno de 8 h)|
|`CORS\_ORIGENES`|Orígenes web permitidos, separados por coma|`http://localhost:5173,http://localhost:3000`|
|`VITE\_API\_PROXY`|(Frontend dev) a dónde reenviar `/api`|`http://localhost:8080`|

Con Docker, copie `.env.example` como `.env` para cambiarlos. El archivo `.env` está en `.gitignore`: **nunca se suben secretos al repositorio**.

\---

## 4\. Pruebas

```bash
# Backend: unitarias (siempre) + integración contra MySQL real (si Docker está abierto)
cd backend
mvn test

# Frontend
cd frontend
npm test
```

Las pruebas de integración del backend usan **Testcontainers**, que inicia un MySQL 8.4 temporal en Docker. Si Docker no está disponible, esas pruebas se **omiten** (no fallan) y solo corren las unitarias.

### Casos obligatorios y dónde se prueban

|Caso|Prueba unitaria (Mockito)|Prueba de integración (HTTP + MySQL)|
|-|-|-|
|Placa inexistente|`MovimientoServiceTest` · placaInexistente|`EntradaSalidaIntegrationTest` · placaInexistente|
|Autorización vencida|`MovimientoServiceTest` · autorizacionVencida|`EntradaSalidaIntegrationTest` · autorizacionVencida|
|Vehículo que ya está dentro|`MovimientoServiceTest` · vehiculoYaDentro|`EntradaSalidaIntegrationTest` · vehiculoYaDentro|
|Celda ocupada o fuera de servicio|`MovimientoServiceTest` · celdaOcupada / celdaFueraDeServicio|`EntradaSalidaIntegrationTest` · celdaOcupadaOFueraDeServicio|
|Entrada correcta|`MovimientoServiceTest` · entradaCorrecta|`EntradaSalidaIntegrationTest` · entradaCorrecta|
|Salida correcta y liberación de celda|`MovimientoServiceTest` · salidaCorrecta|`EntradaSalidaIntegrationTest` · salidaCorrecta|
|Fallo intermedio con rollback|—|`TransaccionesConcurrenciaIntegrationTest` · falloIntermedioHaceRollback, indiceUnicoProtegeLaCelda|
|Usuario sin permisos|`MovimientoServiceTest` · usuarioInactivo|`SeguridadIntegrationTest` (401, 403 por rol, userId ignorado)|
|Novedad sin descripción|`ReglasDominioTest`, `ValidacionSolicitudesTest`|`NovedadesHistorialCeldasIntegrationTest` · novedades|
|Dos solicitudes concurrentes por la misma celda|—|`TransaccionesConcurrenciaIntegrationTest` · dosSolicitudesConcurrentesMismaCelda|
|Historial con filtros|`MovimientoServiceTest` · historialRangoInvertido|`NovedadesHistorialCeldasIntegrationTest` · historialConFiltros|

Frontend (Vitest + Testing Library): login, entrada (vencida, inexistente, correcta, conflicto 409), salida, diálogo de confirmación accesible, cliente HTTP y menú por rol.

Las evidencias están en `docs/evidencias/` (ver sección 8).

\---

## 5\. Arquitectura y estructura

```
autos-colombia/
├── docker-compose.yml          ← levanta MySQL + backend + frontend
├── .env.example                ← plantilla de variables (copiar a .env)
├── backend/                    ← API REST Spring Boot
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/co/autoscolombia/parqueadero/
│       │   ├── presentation/   ← controladores REST, DTOs de entrada, manejo de errores
│       │   ├── application/    ← casos de uso (servicios) y DTOs de respuesta
│       │   ├── domain/         ← entidades, enums, reglas de negocio y excepciones
│       │   ├── infrastructure/ ← repositorios JPA (acceso a MySQL)
│       │   ├── security/       ← JWT, BCrypt, permisos por rol
│       │   └── config/         ← reloj (zona Colombia) y OpenAPI
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── db/migration/   ← V1 esquema · V2 datos demo (Flyway)
│       └── test/               ← unitarias + integración (Testcontainers)
├── frontend/                   ← React + Vite + TypeScript
│   ├── Dockerfile, nginx.conf
│   └── src/
│       ├── api/                ← cliente HTTP, endpoints y tipos
│       ├── auth/               ← sesión y rutas protegidas
│       ├── components/         ← componentes reutilizables (diálogo, alertas, ficha…)
│       ├── layout/             ← menú lateral y encabezado
│       ├── features/           ← una carpeta por pantalla
│       └── styles.css
└── docs/
    ├── arquitectura.md         ← decisiones técnicas explicadas, modelo de datos, concurrencia
    ├── registro.md             ← registro de decisiones, tareas, pendientes y errores
    ├── api/                    ← colección Postman
    └── evidencias/             ← resultados de pruebas y capturas de pantalla
```

**Flujo de una petición** (ej. registrar entrada):
`EntradaPage (React)` → `api.registrarEntrada()` → `POST /api/movements/entries` → filtro JWT de Spring Security (¿token válido? ¿rol OPERADOR?) → `MovimientoController` (valida el JSON con `@Valid`) → `MovimientoService.registrarEntrada()` (reglas + transacción) → repositorios JPA → MySQL.

La explicación detallada de cada decisión (por qué JWT, por qué capas, cómo se garantiza que una celda no tenga dos vehículos, manejo de zona horaria, etc.) está en [**docs/arquitectura.md**](docs/arquitectura.md).

\---

## 6\. API REST

Todas las respuestas son JSON. Los errores siguen un formato único:

```json
{ "timestamp": "2026-11-08T15:00:00Z", "status": 409, "codigo": "CELDA\_OCUPADA",
  "mensaje": "La celda A-01 ya está ocupada. Seleccione otra celda.", "ruta": "/api/movements/entries" }
```

|Método y ruta|Descripción|Admin|Operador|
|-|-|:-:|:-:|
|`POST /api/auth/login`|Inicia sesión, devuelve token JWT|público|público|
|`GET /api/auth/me` · `POST /api/auth/logout`|Usuario actual · cierre de sesión|✔|✔|
|`GET /api/dashboard/summary`|Resumen de ocupación|✔|✔|
|`GET /api/vehicles/{placa}`|Vehículo, cliente, vigencia y si puede ingresar|✔|✔|
|`GET /api/cells`|Todas las celdas (con placa si está ocupada)|✔|✔|
|`GET /api/cells/available?type=CARRO`|Celdas disponibles por tipo|✔|✔|
|`POST /api/cells` · `PUT /api/cells/{id}`|Crear / editar celda|✔|✘|
|`POST /api/movements/entries`|Registrar entrada `{placa, celdaId}`|✘|✔|
|`POST /api/movements/{id}/exit`|Registrar salida|✘|✔|
|`GET /api/movements/active?plate=`|Ocupación actual (filtro parcial por placa)|✔|✔|
|`GET /api/movements/{id}`|Detalle de un movimiento|✔|✔|
|`GET /api/movements?plate=\&from=\&to=`|Historial (placa exacta, fechas `AAAA-MM-DD`)|✔|✘|
|`POST /api/movements/{id}/incidents`|Registrar novedad `{tipo, descripcion}`|✘|✔|
|`GET /api/movements/{id}/incidents`|Novedades del movimiento|✔|✔|

Códigos HTTP: `200/201` éxito · `400` datos inválidos · `401` sin sesión · `403` sin permiso · `404` no existe · `409` conflicto (ya dentro, celda ocupada, carrera concurrente) · `422` regla de negocio (autorización vencida, celda fuera de servicio o incompatible).

El usuario que registra entradas, salidas y novedades **siempre se toma del token**; si el frontend envía un `usuarioId`, se ignora.

Colección Postman lista para importar: `docs/api/autos-colombia.postman\_collection.json`.

\---

## 7\. Solución de problemas

|Síntoma|Solución|
|-|-|
|`port is already allocated` (3000, 8080 o 3307)|Cree un `.env` (desde `.env.example`) y cambie `FRONTEND\_PUERTO\_HOST`, `BACKEND\_PUERTO\_HOST` o `DB\_PUERTO\_HOST`.|
|`Cannot connect to the Docker daemon`|Abra Docker Desktop y espere a que diga *Running*.|
|La web carga pero muestra *“No fue posible conectar con el servidor”*|El backend aún está arrancando (espere `Started ParqueaderoApplication`) o falló: revise `docker compose logs backend`.|
|Error de Flyway por *checksum* tras modificar una migración|Las migraciones aplicadas no se editan. Para desarrollo: `docker compose down -v` y vuelva a levantar.|
|Al reiniciar el backend me saca de la sesión|Normal si `JWT\_SECRET` está vacío (se genera uno nuevo). Defina un `JWT\_SECRET` fijo en `.env`.|
|Las pruebas de integración aparecen como *skipped*|Docker no estaba abierto al ejecutar `mvn test`. Ábralo y repita.|

\---

## 8\. Estado de verificación (honesto)

Lo que se ejecutó y verificó al construir esta entrega:

* ✅ **Migraciones SQL** (V1 + V2) ejecutadas en un MySQL 8 real; todas las restricciones (placa única/normalizada, un movimiento abierto por vehículo y por celda, salida con usuario, novedad no vacía, no borrar movimientos con novedades) y el bloqueo `SELECT … FOR UPDATE` ante dos transacciones simultáneas fueron comprobados → `docs/evidencias/evidencia-restricciones-mysql.txt`.
* ✅ **Frontend**: compila sin errores de TypeScript, build de producción correcto y **23/23 pruebas** Vitest en verde → `docs/evidencias/evidencia-pruebas-frontend.txt`. Capturas de todas las pantallas (incluida vista móvil) en `docs/evidencias/capturas/`.
* ⚠️ **Backend Java**: el código fuente pasó revisión de sintaxis con `javac`, pero el entorno donde se generó el proyecto **no tenía acceso al repositorio Maven Central**, así que no fue posible descargar dependencias, compilarlo ni ejecutar `mvn test` allí. Las pruebas están escritas y listas; la primera ejecución real ocurre en su equipo con `docker compose up --build` (compila) y `mvn test` (ejecuta). Si aparece cualquier error, el registro en `docs/registro.md` explica cómo reportarlo.

\---

## 9\. Fuera del alcance de esta iteración

Cobro por tiempo, pagos y facturación, gestión de clientes/vehículos/usuarios desde la interfaz (en esta iteración se cargan por migración) y reportes. La arquitectura deja el espacio preparado: ver *“Cómo crecer”* en `docs/arquitectura.md`.

