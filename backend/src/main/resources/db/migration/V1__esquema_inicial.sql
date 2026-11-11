-- =====================================================================
-- Autos Colombia - Iteración 1
-- V1: esquema inicial
--
-- Flyway ejecuta este archivo UNA sola vez y registra que ya lo aplicó en
-- la tabla flyway_schema_history. Si necesitas cambiar el esquema, crea un
-- archivo nuevo (V3__..., V4__...) en lugar de editar este.
-- =====================================================================

-- ---------------------------------------------------------------------
-- CLIENTE: dueño de la mensualidad
-- ---------------------------------------------------------------------
CREATE TABLE cliente (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    tipo_documento   VARCHAR(10)  NOT NULL,
    numero_documento VARCHAR(20)  NOT NULL,
    nombres          VARCHAR(80)  NOT NULL,
    apellidos        VARCHAR(80)  NOT NULL,
    telefono         VARCHAR(20)  NULL,
    correo           VARCHAR(120) NULL,
    estado           VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT pk_cliente PRIMARY KEY (id),
    CONSTRAINT uk_cliente_documento UNIQUE (numero_documento),
    CONSTRAINT ck_cliente_tipo_documento CHECK (tipo_documento IN ('CC', 'CE', 'NIT', 'PASAPORTE', 'TI')),
    CONSTRAINT ck_cliente_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- USUARIO: personas que inician sesión (administradores y operadores)
-- password_hash guarda un hash BCrypt, NUNCA la contraseña en texto plano.
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(100) NOT NULL,
    correo        VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_correo UNIQUE (correo),
    CONSTRAINT ck_usuario_rol CHECK (rol IN ('ADMINISTRADOR', 'OPERADOR'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- VEHICULO
-- RN-01: placa única y normalizada (mayúsculas, sin espacios externos).
--   * La aplicación normaliza antes de guardar.
--   * El CHECK lo vuelve a exigir en la base de datos (defensa en profundidad).
--     Se compara con COLLATE utf8mb4_bin porque la intercalación por defecto
--     no distingue mayúsculas/minúsculas ('abc' = 'ABC' sería verdadero).
--   * La intercalación _ai_ci del UNIQUE impide 'ABC123' y 'abc123' a la vez.
-- ---------------------------------------------------------------------
CREATE TABLE vehiculo (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    placa            VARCHAR(10) NOT NULL,
    tipo             VARCHAR(10) NOT NULL,
    marca            VARCHAR(40) NULL,
    modelo           VARCHAR(40) NULL,
    color            VARCHAR(30) NULL,
    estado           VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    autorizado_hasta DATE        NOT NULL,
    cliente_id       BIGINT      NOT NULL,
    CONSTRAINT pk_vehiculo PRIMARY KEY (id),
    CONSTRAINT uk_vehiculo_placa UNIQUE (placa),
    CONSTRAINT fk_vehiculo_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id) ON DELETE RESTRICT,
    CONSTRAINT ck_vehiculo_placa_normalizada CHECK (placa = UPPER(TRIM(placa)) COLLATE utf8mb4_bin AND CHAR_LENGTH(placa) > 0),
    CONSTRAINT ck_vehiculo_tipo CHECK (tipo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_vehiculo_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_vehiculo_cliente ON vehiculo (cliente_id);

-- ---------------------------------------------------------------------
-- CELDA: espacio físico de parqueo
-- version: columna para bloqueo optimista (JPA @Version) en ediciones del admin.
-- ---------------------------------------------------------------------
CREATE TABLE celda (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    codigo        VARCHAR(20) NOT NULL,
    zona          VARCHAR(40) NOT NULL,
    tipo_vehiculo VARCHAR(10) NOT NULL,
    estado        VARCHAR(15) NOT NULL DEFAULT 'DISPONIBLE',
    version       BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_celda PRIMARY KEY (id),
    CONSTRAINT uk_celda_codigo UNIQUE (codigo),
    CONSTRAINT ck_celda_tipo CHECK (tipo_vehiculo IN ('CARRO', 'MOTO')),
    CONSTRAINT ck_celda_estado CHECK (estado IN ('DISPONIBLE', 'OCUPADA', 'FUERA_SERVICIO'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_celda_estado_tipo ON celda (estado, tipo_vehiculo);

-- ---------------------------------------------------------------------
-- MOVIMIENTO_PARQUEO: una permanencia (entrada -> salida)
--
-- RN-02 y RN-04: como máximo UN movimiento abierto por vehículo y por celda.
-- MySQL no tiene "índices únicos parciales" (UNIQUE ... WHERE salida IS NULL),
-- así que usamos columnas generadas:
--
--   vehiculo_abierto_id = vehiculo_id   si el movimiento está abierto
--                       = NULL          si ya tiene fecha de salida
--
-- Un índice UNIQUE en MySQL permite muchos NULL pero no valores repetidos.
-- Resultado: muchos movimientos CERRADOS del mismo vehículo (NULL, NULL, ...)
-- pero nunca dos ABIERTOS (el segundo INSERT falla con error de duplicado).
-- Lo mismo para celda_abierta_id.
-- ---------------------------------------------------------------------
CREATE TABLE movimiento_parqueo (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    vehiculo_id         BIGINT      NOT NULL,
    celda_id            BIGINT      NOT NULL,
    fecha_hora_entrada  DATETIME(6) NOT NULL,
    fecha_hora_salida   DATETIME(6) NULL,
    usuario_entrada_id  BIGINT      NOT NULL,
    usuario_salida_id   BIGINT      NULL,
    vehiculo_abierto_id BIGINT GENERATED ALWAYS AS (CASE WHEN fecha_hora_salida IS NULL THEN vehiculo_id END) STORED,
    celda_abierta_id    BIGINT GENERATED ALWAYS AS (CASE WHEN fecha_hora_salida IS NULL THEN celda_id END) STORED,
    CONSTRAINT pk_movimiento PRIMARY KEY (id),
    CONSTRAINT fk_mov_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculo (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mov_celda FOREIGN KEY (celda_id) REFERENCES celda (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mov_usuario_entrada FOREIGN KEY (usuario_entrada_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mov_usuario_salida FOREIGN KEY (usuario_salida_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT uk_mov_vehiculo_abierto UNIQUE (vehiculo_abierto_id),
    CONSTRAINT uk_mov_celda_abierta UNIQUE (celda_abierta_id),
    -- Salida y usuario de salida van juntos (RN-06): o ambos nulos o ambos con valor.
    CONSTRAINT ck_mov_salida_coherente CHECK (
        (fecha_hora_salida IS NULL AND usuario_salida_id IS NULL)
        OR (fecha_hora_salida IS NOT NULL AND usuario_salida_id IS NOT NULL)
    ),
    CONSTRAINT ck_mov_fechas CHECK (fecha_hora_salida IS NULL OR fecha_hora_salida >= fecha_hora_entrada)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Índices para las consultas de historial y ocupación.
CREATE INDEX idx_mov_entrada ON movimiento_parqueo (fecha_hora_entrada);
CREATE INDEX idx_mov_vehiculo_entrada ON movimiento_parqueo (vehiculo_id, fecha_hora_entrada);
CREATE INDEX idx_mov_celda ON movimiento_parqueo (celda_id);

-- ---------------------------------------------------------------------
-- NOVEDAD: incidente u observación ligada a UN movimiento (RN-07)
-- ---------------------------------------------------------------------
CREATE TABLE novedad (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    movimiento_id BIGINT       NOT NULL,
    tipo          VARCHAR(25)  NOT NULL,
    descripcion   VARCHAR(500) NOT NULL,
    fecha_hora    DATETIME(6)  NOT NULL,
    usuario_id    BIGINT       NOT NULL,
    CONSTRAINT pk_novedad PRIMARY KEY (id),
    CONSTRAINT fk_novedad_movimiento FOREIGN KEY (movimiento_id) REFERENCES movimiento_parqueo (id) ON DELETE RESTRICT,
    CONSTRAINT fk_novedad_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT ck_novedad_tipo CHECK (tipo IN ('DANO_VEHICULO', 'OBJETO_OLVIDADO', 'INCIDENTE_SEGURIDAD', 'OBSERVACION', 'OTRO')),
    CONSTRAINT ck_novedad_descripcion CHECK (CHAR_LENGTH(TRIM(descripcion)) > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_novedad_movimiento ON novedad (movimiento_id, fecha_hora);
