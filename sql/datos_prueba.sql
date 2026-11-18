-- =====================================================================
-- DATOS DE PRUEBA REALISTAS - PAPELERIA DE ROSITA (PROYECTO CONARY)
-- Ejecutar en MySQL / phpMyAdmin en la base de datos 'papeleria_rosita'
-- =====================================================================

USE `papeleria_rosita`;

-- 1. PROVEEDORES DE PRUEBA
INSERT INTO `proveedor` (`nombre`, `telefono`, `direccion`, `productos`, `activo`) VALUES
('Distribuidora Papelera Nacional SAS', '3104567890', 'Cra. 10 # 18-45, Bogotá', 'Resmas, Cuadernos, Blocks, Cartulinas', TRUE),
('Comercializadora Escolar Pelikan Ltda', '3157891234', 'Calle 13 # 68-20, Bogotá', 'Marcadores, Resaltadores, Tintas, Vinilos', TRUE),
('Industrias Norma de Colombia', '3209876543', 'Av. Las Américas # 42-10, Bogotá', 'Cuadernos Norma, Colores, Carpetas', TRUE),
('Pegantes y Adhesivos Pritt S.A.', '3123456789', 'Zona Industrial Cazucá, Soacha', 'Pegantes en barra, Líquidos, Cintas de embalaje', TRUE);

-- 2. CLIENTES DE PRUEBA (Mostrador habituales y cuentas de crédito/fiado)
INSERT INTO `cliente` (`nombre`, `telefono`, `direccion`, `activo`, `fiado_autorizado`, `limite_credito`) VALUES
('Consumidor Final (Mostrador)', '0000000', 'Mostrador Local', TRUE, FALSE, 0.00),
('Prof. Carlos Alberto Mendoza', '3112233445', 'Colegio Departamental San José', TRUE, TRUE, 150000.00),
('Mariana Gómez (Madre de Familia)', '3138899001', 'Barrio Centro, Calle 5 # 4-12', TRUE, TRUE, 80000.00),
('Academia de Artes y Dibujo Fusagasugá', '3187766554', 'Cra. 6 # 9-30', TRUE, TRUE, 300000.00),
('Julián David Pérez (Estudiante)', '3145566778', 'Av. Manuel Humberto Cárdenas', TRUE, FALSE, 0.00);

-- 3. PRODUCTOS DE PRUEBA (Con stock normal y stock bajo para alertas)
INSERT INTO `producto` (`nombre`, `categoria`, `precio_compra`, `precio_venta`, `stock_actual`, `stock_minimo`, `activo`) VALUES
('Cuaderno Cuadriculado 100H Norma', 'CUADERNOS', 3200.00, 4800.00, 45, 10, TRUE),
('Cuaderno Rayado 100H Norma', 'CUADERNOS', 3200.00, 4800.00, 38, 10, TRUE),
('Resma Papel Carta Reprograf (500 hojas)', 'PAPEL', 14500.00, 19500.00, 20, 5, TRUE),
('Block Carta Rayado sin Fin', 'PAPEL', 2800.00, 4200.00, 25, 8, TRUE),
('Bolígrafo BIC Cristal Negro (Unidad)', 'ESCRITURA', 800.00, 1400.00, 120, 20, TRUE),
('Bolígrafo BIC Cristal Azul (Unidad)', 'ESCRITURA', 800.00, 1400.00, 110, 20, TRUE),
('Bolígrafo BIC Cristal Rojo (Unidad)', 'ESCRITURA', 800.00, 1400.00, 60, 15, TRUE),
('Lápiz Grafito Mirado #2', 'ESCRITURA', 900.00, 1600.00, 75, 15, TRUE),
('Borrador de Nata Grande Pelikan', 'ESCRITURA', 700.00, 1200.00, 50, 10, TRUE),
('Tajalápiz Metálico con Depósito', 'ESCRITURA', 1200.00, 2200.00, 30, 8, TRUE),
('Caja de Colores Norma Kiut x 24', 'ARTE', 16000.00, 23000.00, 15, 4, TRUE),
('Pegante en Barra Pritt 40g', 'ADHESIVOS', 4200.00, 6500.00, 18, 6, TRUE),
('Cinta Transparente Gruesa 48mm x 40m', 'ADHESIVOS', 2800.00, 4500.00, 14, 5, TRUE),
('Marcador Permanente Sharpie Negro', 'ESCRITURA', 2800.00, 4500.00, 4, 10, TRUE),
('Corrector Líquido Liquid Paper Tipo Lápiz', 'CORRECCION', 3100.00, 4900.00, 3, 8, TRUE),
('Juego Geométrico Técnico 30cm', 'GEOMETRIA', 3800.00, 6200.00, 12, 5, TRUE),
('Tijera Punta Roma Escolar 5 Pulgadas', 'ESCOLAR', 1900.00, 3300.00, 22, 6, TRUE),
('Carpeta Legajadora Plástica Tamaño Carta', 'OFICINA', 1500.00, 2600.00, 40, 10, TRUE);
