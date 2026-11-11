-- =====================================================================
-- V2: datos de demostración (NO usar en producción)
--
-- Credenciales de demo (contraseñas guardadas como hash BCrypt):
--   admin@autoscolombia.co     / Admin123*     (ADMINISTRADOR)
--   operador@autoscolombia.co  / Operador123*  (OPERADOR)
--
-- Las fechas son relativas a "hoy" (CURDATE / NOW) para que la demo
-- siempre tenga mensualidades vigentes y una vencida.
-- =====================================================================

INSERT INTO usuario (id, nombre, correo, password_hash, rol, activo) VALUES
    (1, 'Laura Administradora', 'admin@autoscolombia.co',
     '$2a$10$wgJJ07KGHsCBI3qiPeeqR.3u985WzGrd4sqS5/2eXn7GdiEL3S7Qu', 'ADMINISTRADOR', TRUE),
    (2, 'Carlos Operador', 'operador@autoscolombia.co',
     '$2a$10$FJLVAnOFjBmnoHPSkO8lXu4mnzZDy7azBZfNmWXV8LKcdluhRXAWK', 'OPERADOR', TRUE);

INSERT INTO cliente (id, tipo_documento, numero_documento, nombres, apellidos, telefono, correo, estado) VALUES
    (1, 'CC', '1020304050', 'Andrea',   'Gómez Restrepo',  '3001234567', 'andrea.gomez@example.com',  'ACTIVO'),
    (2, 'CC', '79856321',   'Jorge',    'Martínez Ruiz',   '3109876543', 'jorge.martinez@example.com', 'ACTIVO'),
    (3, 'CE', 'E-456789',   'Valentina','Pérez Silva',     '3205554433', 'valentina.perez@example.com','ACTIVO'),
    (4, 'NIT','900123456',  'Logística','Andina S.A.S.',   '6015551234', 'flota@logandina.example.com','ACTIVO'),
    (5, 'CC', '52741963',   'Camilo',   'Rodríguez Peña',  '3157778899', 'camilo.rodriguez@example.com','ACTIVO');

INSERT INTO vehiculo (id, placa, tipo, marca, modelo, color, estado, autorizado_hasta, cliente_id) VALUES
    (1, 'ABC123', 'CARRO', 'Mazda',     '3',       'Gris',   'ACTIVO',   DATE_ADD(CURDATE(), INTERVAL 1 YEAR),   1),
    (2, 'DEF456', 'CARRO', 'Renault',   'Logan',   'Blanco', 'ACTIVO',   DATE_ADD(CURDATE(), INTERVAL 3 MONTH),  2),
    (3, 'GHI78D', 'MOTO',  'Yamaha',    'NMAX',    'Negro',  'ACTIVO',   DATE_ADD(CURDATE(), INTERVAL 6 MONTH),  3),
    (4, 'JKL012', 'CARRO', 'Chevrolet', 'Onix',    'Rojo',   'ACTIVO',   DATE_ADD(CURDATE(), INTERVAL 2 MONTH),  4),
    (5, 'MNO345', 'CARRO', 'Kia',       'Picanto', 'Azul',   'ACTIVO',   DATE_SUB(CURDATE(), INTERVAL 10 DAY),   5),
    (6, 'PQR67E', 'MOTO',  'AKT',       'NKD 125', 'Verde',  'INACTIVO', DATE_ADD(CURDATE(), INTERVAL 1 MONTH),  2),
    (7, 'STU901', 'CARRO', 'Toyota',    'Corolla', 'Plata',  'ACTIVO',   DATE_ADD(CURDATE(), INTERVAL 5 DAY),    1);

INSERT INTO celda (codigo, zona, tipo_vehiculo, estado) VALUES
    ('A-01', 'Zona A - Cubierta', 'CARRO', 'DISPONIBLE'),
    ('A-02', 'Zona A - Cubierta', 'CARRO', 'OCUPADA'),
    ('A-03', 'Zona A - Cubierta', 'CARRO', 'DISPONIBLE'),
    ('A-04', 'Zona A - Cubierta', 'CARRO', 'DISPONIBLE'),
    ('A-05', 'Zona A - Cubierta', 'CARRO', 'DISPONIBLE'),
    ('A-06', 'Zona A - Cubierta', 'CARRO', 'DISPONIBLE'),
    ('A-07', 'Zona A - Cubierta', 'CARRO', 'DISPONIBLE'),
    ('A-08', 'Zona A - Cubierta', 'CARRO', 'FUERA_SERVICIO'),
    ('B-01', 'Zona B - Descubierta', 'CARRO', 'DISPONIBLE'),
    ('B-02', 'Zona B - Descubierta', 'CARRO', 'DISPONIBLE'),
    ('B-03', 'Zona B - Descubierta', 'CARRO', 'DISPONIBLE'),
    ('B-04', 'Zona B - Descubierta', 'CARRO', 'DISPONIBLE'),
    ('M-01', 'Zona M - Motos', 'MOTO', 'DISPONIBLE'),
    ('M-02', 'Zona M - Motos', 'MOTO', 'DISPONIBLE'),
    ('M-03', 'Zona M - Motos', 'MOTO', 'DISPONIBLE'),
    ('M-04', 'Zona M - Motos', 'MOTO', 'DISPONIBLE'),
    ('M-05', 'Zona M - Motos', 'MOTO', 'DISPONIBLE'),
    ('M-06', 'Zona M - Motos', 'MOTO', 'DISPONIBLE');

-- Historial: tres permanencias ya cerradas y una abierta (JKL012 en A-02).
INSERT INTO movimiento_parqueo
    (id, vehiculo_id, celda_id, fecha_hora_entrada, fecha_hora_salida, usuario_entrada_id, usuario_salida_id)
VALUES
    (1, 1, (SELECT id FROM (SELECT id FROM celda WHERE codigo = 'A-01') c),
        DATE_SUB(NOW(6), INTERVAL 3 DAY), DATE_SUB(NOW(6), INTERVAL 70 HOUR), 2, 2),
    (2, 2, (SELECT id FROM (SELECT id FROM celda WHERE codigo = 'B-01') c),
        DATE_SUB(NOW(6), INTERVAL 2 DAY), DATE_SUB(NOW(6), INTERVAL 40 HOUR), 2, 2),
    (3, 3, (SELECT id FROM (SELECT id FROM celda WHERE codigo = 'M-01') c),
        DATE_SUB(NOW(6), INTERVAL 1 DAY), DATE_SUB(NOW(6), INTERVAL 20 HOUR), 2, 2),
    (4, 4, (SELECT id FROM (SELECT id FROM celda WHERE codigo = 'A-02') c),
        DATE_SUB(NOW(6), INTERVAL 2 HOUR), NULL, 2, NULL);

INSERT INTO novedad (movimiento_id, tipo, descripcion, fecha_hora, usuario_id) VALUES
    (1, 'OBSERVACION', 'El cliente informa que recogerá el vehículo un familiar autorizado.',
        DATE_SUB(NOW(6), INTERVAL 71 HOUR), 2),
    (4, 'DANO_VEHICULO', 'Rayón visible en la puerta trasera izquierda al momento del ingreso.',
        DATE_SUB(NOW(6), INTERVAL 2 HOUR), 2);
