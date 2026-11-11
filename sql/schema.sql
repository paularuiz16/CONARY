-- =====================================================================
-- SISTEMA DE GESTION DE LA PAPELERIA DE ROSITA (PROYECTO CONARY)
-- Base de datos para MySQL (XAMPP) / MariaDB
-- Diseno optimizado con patron de movimiento unificado y vistas
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `papeleria_rosita`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `papeleria_rosita`;

-- Desactivar revision de claves foraneas durante la creacion/reinicio
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `pago_proveedor`;
DROP TABLE IF EXISTS `abono_cliente`;
DROP TABLE IF EXISTS `detalle_compra`;
DROP TABLE IF EXISTS `compra`;
DROP TABLE IF EXISTS `detalle_venta`;
DROP TABLE IF EXISTS `venta`;
DROP TABLE IF EXISTS `movimiento`;
DROP TABLE IF EXISTS `caja`;
DROP TABLE IF EXISTS `producto`;
DROP TABLE IF EXISTS `proveedor`;
DROP TABLE IF EXISTS `cliente`;
DROP TABLE IF EXISTS `usuario`;

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 1. TABLA: usuario
-- =====================================================================
CREATE TABLE `usuario` (
  `id_usuario` INT AUTO_INCREMENT PRIMARY KEY,
  `nombre` VARCHAR(100) NOT NULL,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password_hash` VARCHAR(255) NOT NULL,
  `rol` ENUM('ADMINISTRADOR', 'VENDEDOR') NOT NULL DEFAULT 'VENDEDOR',
  `activo` BOOLEAN NOT NULL DEFAULT TRUE,
  INDEX `idx_usuario_username` (`username`),
  INDEX `idx_usuario_activo` (`activo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 2. TABLA: cliente
-- =====================================================================
CREATE TABLE `cliente` (
  `id_cliente` INT AUTO_INCREMENT PRIMARY KEY,
  `nombre` VARCHAR(120) NOT NULL,
  `telefono` VARCHAR(30) NULL,
  `direccion` VARCHAR(200) NULL,
  `activo` BOOLEAN NOT NULL DEFAULT TRUE,
  `fiado_autorizado` BOOLEAN NOT NULL DEFAULT FALSE,
  `limite_credito` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  INDEX `idx_cliente_nombre` (`nombre`),
  INDEX `idx_cliente_activo` (`activo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 3. TABLA: proveedor
-- =====================================================================
CREATE TABLE `proveedor` (
  `id_proveedor` INT AUTO_INCREMENT PRIMARY KEY,
  `nombre` VARCHAR(120) NOT NULL,
  `telefono` VARCHAR(30) NULL,
  `direccion` VARCHAR(200) NULL,
  `productos` TEXT NULL,
  `activo` BOOLEAN NOT NULL DEFAULT TRUE,
  INDEX `idx_proveedor_nombre` (`nombre`),
  INDEX `idx_proveedor_activo` (`activo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 4. TABLA: producto
-- =====================================================================
CREATE TABLE `producto` (
  `id_producto` INT AUTO_INCREMENT PRIMARY KEY,
  `nombre` VARCHAR(150) NOT NULL UNIQUE,
  `categoria` VARCHAR(60) NOT NULL DEFAULT 'GENERAL',
  `precio_compra` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `precio_venta` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `stock_actual` INT NOT NULL DEFAULT 0,
  `stock_minimo` INT NOT NULL DEFAULT 5,
  `activo` BOOLEAN NOT NULL DEFAULT TRUE,
  INDEX `idx_producto_categoria` (`categoria`),
  INDEX `idx_producto_activo` (`activo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 5. TABLA: caja (Arqueos y turnos)
-- =====================================================================
CREATE TABLE `caja` (
  `id_caja` INT AUTO_INCREMENT PRIMARY KEY,
  `fecha_apertura` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fecha_cierre` DATETIME NULL,
  `monto_inicial` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `total_tirilla` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `efectivo_contado` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `diferencia` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  INDEX `idx_caja_fechas` (`fecha_apertura`, `fecha_cierre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 6. TABLA: movimiento (Tabla transaccional unificada para trazabilidad)
-- =====================================================================
CREATE TABLE `movimiento` (
  `id_movimiento` INT AUTO_INCREMENT PRIMARY KEY,
  `fecha` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `descripcion` VARCHAR(255) NULL,
  `tipo` ENUM('VENTA', 'COMPRA', 'ABONO_CLIENTE', 'PAGO_PROVEEDOR') NOT NULL,
  `id_usuario` INT NOT NULL,
  `id_caja` INT NULL,
  CONSTRAINT `fk_movimiento_usuario` FOREIGN KEY (`id_usuario`)
    REFERENCES `usuario` (`id_usuario`) ON UPDATE CASCADE,
  CONSTRAINT `fk_movimiento_caja` FOREIGN KEY (`id_caja`)
    REFERENCES `caja` (`id_caja`) ON UPDATE CASCADE ON DELETE SET NULL,
  INDEX `idx_movimiento_fecha` (`fecha`),
  INDEX `idx_movimiento_tipo` (`tipo`),
  INDEX `idx_movimiento_caja` (`id_caja`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 7. TABLA: venta (Subtipo de movimiento)
-- =====================================================================
CREATE TABLE `venta` (
  `id_movimiento` INT PRIMARY KEY,
  `id_cliente` INT NULL,
  `es_fiado` BOOLEAN NOT NULL DEFAULT FALSE,
  `anulada` BOOLEAN NOT NULL DEFAULT FALSE,
  `motivo_anulacion` VARCHAR(255) NULL,
  `importada` BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT `fk_venta_movimiento` FOREIGN KEY (`id_movimiento`)
    REFERENCES `movimiento` (`id_movimiento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_venta_cliente` FOREIGN KEY (`id_cliente`)
    REFERENCES `cliente` (`id_cliente`) ON UPDATE CASCADE ON DELETE SET NULL,
  INDEX `idx_venta_cliente` (`id_cliente`),
  INDEX `idx_venta_fiado` (`es_fiado`),
  INDEX `idx_venta_anulada` (`anulada`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 8. TABLA: detalle_venta
-- =====================================================================
CREATE TABLE `detalle_venta` (
  `id_detalle` INT AUTO_INCREMENT PRIMARY KEY,
  `id_movimiento` INT NOT NULL,
  `id_producto` INT NOT NULL,
  `cantidad` INT NOT NULL,
  `valor_unitario` DECIMAL(12,2) NOT NULL,
  `subtotal` DECIMAL(12,2) NOT NULL,
  CONSTRAINT `fk_detalle_venta_venta` FOREIGN KEY (`id_movimiento`)
    REFERENCES `venta` (`id_movimiento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_detalle_venta_producto` FOREIGN KEY (`id_producto`)
    REFERENCES `producto` (`id_producto`) ON UPDATE CASCADE,
  INDEX `idx_detalle_venta_mov` (`id_movimiento`),
  INDEX `idx_detalle_venta_prod` (`id_producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 9. TABLA: compra (Subtipo de movimiento)
-- =====================================================================
CREATE TABLE `compra` (
  `id_movimiento` INT PRIMARY KEY,
  `id_proveedor` INT NOT NULL,
  `numero_factura_proveedor` VARCHAR(50) NULL,
  `fecha_pago_acordada` DATE NULL,
  CONSTRAINT `fk_compra_movimiento` FOREIGN KEY (`id_movimiento`)
    REFERENCES `movimiento` (`id_movimiento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_compra_proveedor` FOREIGN KEY (`id_proveedor`)
    REFERENCES `proveedor` (`id_proveedor`) ON UPDATE CASCADE,
  INDEX `idx_compra_proveedor` (`id_proveedor`),
  INDEX `idx_compra_fecha_pago` (`fecha_pago_acordada`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 10. TABLA: detalle_compra
-- =====================================================================
CREATE TABLE `detalle_compra` (
  `id_detalle` INT AUTO_INCREMENT PRIMARY KEY,
  `id_movimiento` INT NOT NULL,
  `id_producto` INT NOT NULL,
  `cantidad` INT NOT NULL,
  `valor_unitario` DECIMAL(12,2) NOT NULL,
  `subtotal` DECIMAL(12,2) NOT NULL,
  CONSTRAINT `fk_detalle_compra_compra` FOREIGN KEY (`id_movimiento`)
    REFERENCES `compra` (`id_movimiento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_detalle_compra_producto` FOREIGN KEY (`id_producto`)
    REFERENCES `producto` (`id_producto`) ON UPDATE CASCADE,
  INDEX `idx_detalle_compra_mov` (`id_movimiento`),
  INDEX `idx_detalle_compra_prod` (`id_producto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 11. TABLA: abono_cliente (Subtipo de movimiento)
-- =====================================================================
CREATE TABLE `abono_cliente` (
  `id_movimiento` INT PRIMARY KEY,
  `id_cliente` INT NOT NULL,
  `monto` DECIMAL(12,2) NOT NULL,
  CONSTRAINT `fk_abono_movimiento` FOREIGN KEY (`id_movimiento`)
    REFERENCES `movimiento` (`id_movimiento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_abono_cliente` FOREIGN KEY (`id_cliente`)
    REFERENCES `cliente` (`id_cliente`) ON UPDATE CASCADE,
  INDEX `idx_abono_cliente` (`id_cliente`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 12. TABLA: pago_proveedor (Subtipo de movimiento)
-- =====================================================================
CREATE TABLE `pago_proveedor` (
  `id_movimiento` INT PRIMARY KEY,
  `id_proveedor` INT NOT NULL,
  `monto` DECIMAL(12,2) NOT NULL,
  CONSTRAINT `fk_pago_movimiento` FOREIGN KEY (`id_movimiento`)
    REFERENCES `movimiento` (`id_movimiento`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_pago_proveedor` FOREIGN KEY (`id_proveedor`)
    REFERENCES `proveedor` (`id_proveedor`) ON UPDATE CASCADE,
  INDEX `idx_pago_proveedor` (`id_proveedor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- VISTAS DEL SISTEMA
-- =====================================================================

-- Vista 1: v_total_venta
-- Calcula el total sumado de cada venta, incluyendo sus datos principales
CREATE OR REPLACE VIEW `v_total_venta` AS
SELECT 
    m.id_movimiento,
    m.fecha,
    m.descripcion,
    m.id_usuario,
    u.nombre AS nombre_usuario,
    m.id_caja,
    v.id_cliente,
    COALESCE(c.nombre, 'CLIENTE GENERAL') AS nombre_cliente,
    v.es_fiado,
    v.anulada,
    v.motivo_anulacion,
    v.importada,
    COALESCE(SUM(dv.subtotal), 0.00) AS total_venta
FROM `venta` v
INNER JOIN `movimiento` m ON v.id_movimiento = m.id_movimiento
INNER JOIN `usuario` u ON m.id_usuario = u.id_usuario
LEFT JOIN `cliente` c ON v.id_cliente = c.id_cliente
LEFT JOIN `detalle_venta` dv ON v.id_movimiento = dv.id_movimiento
GROUP BY 
    m.id_movimiento, m.fecha, m.descripcion, m.id_usuario, u.nombre,
    m.id_caja, v.id_cliente, c.nombre, v.es_fiado, v.anulada,
    v.motivo_anulacion, v.importada;

-- Vista 2: v_total_compra
-- Calcula el total sumado de cada compra a proveedores
CREATE OR REPLACE VIEW `v_total_compra` AS
SELECT 
    m.id_movimiento,
    m.fecha,
    m.descripcion,
    m.id_usuario,
    u.nombre AS nombre_usuario,
    m.id_caja,
    cp.id_proveedor,
    p.nombre AS nombre_proveedor,
    cp.numero_factura_proveedor,
    cp.fecha_pago_acordada,
    COALESCE(SUM(dc.subtotal), 0.00) AS total_compra
FROM `compra` cp
INNER JOIN `movimiento` m ON cp.id_movimiento = m.id_movimiento
INNER JOIN `usuario` u ON m.id_usuario = u.id_usuario
INNER JOIN `proveedor` p ON cp.id_proveedor = p.id_proveedor
LEFT JOIN `detalle_compra` dc ON cp.id_movimiento = dc.id_movimiento
GROUP BY 
    m.id_movimiento, m.fecha, m.descripcion, m.id_usuario, u.nombre,
    m.id_caja, cp.id_proveedor, p.nombre, cp.numero_factura_proveedor,
    cp.fecha_pago_acordada;

-- Vista 3: v_saldo_cliente
-- Saldo de cuentas por cobrar: Ventas fiadas activas menos abonos recibidos
CREATE OR REPLACE VIEW `v_saldo_cliente` AS
SELECT 
    c.id_cliente,
    c.nombre,
    c.telefono,
    c.direccion,
    c.activo,
    c.fiado_autorizado,
    c.limite_credito,
    COALESCE(vf.total_fiado, 0.00) AS total_fiado,
    COALESCE(ab.total_abonos, 0.00) AS total_abonos,
    (COALESCE(vf.total_fiado, 0.00) - COALESCE(ab.total_abonos, 0.00)) AS saldo_pendiente
FROM `cliente` c
LEFT JOIN (
    SELECT 
        v.id_cliente,
        SUM(dv.subtotal) AS total_fiado
    FROM `venta` v
    INNER JOIN `detalle_venta` dv ON v.id_movimiento = dv.id_movimiento
    WHERE v.es_fiado = TRUE AND v.anulada = FALSE
    GROUP BY v.id_cliente
) vf ON c.id_cliente = vf.id_cliente
LEFT JOIN (
    SELECT 
        ac.id_cliente,
        SUM(ac.monto) AS total_abonos
    FROM `abono_cliente` ac
    GROUP BY ac.id_cliente
) ab ON c.id_cliente = ab.id_cliente;

-- Vista 4: v_saldo_proveedor
-- Saldo de cuentas por pagar: Compras realizadas menos pagos efectuados
CREATE OR REPLACE VIEW `v_saldo_proveedor` AS
SELECT 
    p.id_proveedor,
    p.nombre,
    p.telefono,
    p.direccion,
    p.activo,
    COALESCE(cp.total_compras, 0.00) AS total_compras,
    COALESCE(pg.total_pagos, 0.00) AS total_pagos,
    (COALESCE(cp.total_compras, 0.00) - COALESCE(pg.total_pagos, 0.00)) AS saldo_pendiente
FROM `proveedor` p
LEFT JOIN (
    SELECT 
        c.id_proveedor,
        SUM(dc.subtotal) AS total_compras
    FROM `compra` c
    INNER JOIN `detalle_compra` dc ON c.id_movimiento = dc.id_movimiento
    GROUP BY c.id_proveedor
) cp ON p.id_proveedor = cp.id_proveedor
LEFT JOIN (
    SELECT 
        pp.id_proveedor,
        SUM(pp.monto) AS total_pagos
    FROM `pago_proveedor` pp
    GROUP BY pp.id_proveedor
) pg ON p.id_proveedor = pg.id_proveedor;

-- =====================================================================
-- DATOS SEMILLA (Usuarios iniciales)
-- Contrasenas encriptadas con SHA-256:
-- Rosita:  'admin123'    -> 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa82280f1a8230e88
-- Gladys:  'vendedor123' -> 8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918
-- =====================================================================
INSERT INTO `usuario` (`nombre`, `username`, `password_hash`, `rol`, `activo`) VALUES
('Rosita', 'rosita', SHA2('admin123', 256), 'ADMINISTRADOR', TRUE),
('Gladys', 'gladys', SHA2('vendedor123', 256), 'VENDEDOR', TRUE);

