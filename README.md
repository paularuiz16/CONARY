# CONARY - Sistema de Gestión Papelería de Rosita

Sistema de escritorio ligero para la gestión integral de papelería, desarrollado con **Java 8 (JavaFX nativo)** y **MySQL / MariaDB (XAMPP)**, diseñado y optimizado para funcionar con alta fluidez y bajo consumo de recursos (Windows 8 / 2 GB de RAM).

---

## 🚀 Características Principales

- **Control de Acceso y Seguridad:** Autenticación por roles (Administrador y Vendedor) con contraseñas cifradas en SHA-256.
- **Punto de Venta (POS):** Facturación rápida con escaneo de código de barras o búsqueda manual, cálculo automático de cambio y generación de comprobantes.
- **Inventario:** Control de catálogo, alertas de stock mínimo, gestión de categorías y precios de compra/venta.
- **Gestión de Caja:** Control estricto de apertura, arqueo, ingresos, egresos y cierre de caja por turno.
- **Clientes y Proveedores:** Historial de compras, créditos, cuentas por cobrar y gestión de compras a proveedores.
- **Rendimiento Ultraligero:** Conexión JDBC directa y parámetros JVM optimizados (`-Xms64m -Xmx256m -XX:+UseSerialGC`).

---

## 📋 Requisitos del Sistema

- **Java Runtime:** Java SE 8 (JRE 1.8) con soporte nativo de JavaFX.
- **Base de Datos:** MySQL 5.7+ o MariaDB 10.4+ (ej. XAMPP).
- **Herramienta de Construcción:** Apache Maven 3.6+.

---

## 🛠️ Instalación y Configuración

### 1. Base de Datos
1. Iniciar **MySQL** desde el Panel de Control de XAMPP.
2. Importar el script SQL ubicado en `sql/schema.sql` (en phpMyAdmin o por consola):
   ```bash
   mysql -u root -p < sql/schema.sql
   ```
3. Credenciales predeterminadas:
   - **Administrador:** `rosita` | Clave: `admin123`
   - **Vendedor:** `gladys` | Clave: `vendedor123`

### 2. Compilación
Compilar y empaquetar el proyecto usando Maven:
```bash
mvn clean package
```

### 3. Ejecución
- **Modo silencioso / Producción:** Ejecutar `ejecutar.bat`.
- **Modo consola / Depuración:** Ejecutar `ejecutar_consola.bat`.
- **Desde IDE (VS Code / Eclipse / IntelliJ):** Ejecutar la clase principal `com.conary.rosita.MainLauncher`.

---

## 📂 Estructura del Proyecto

```
papeleria-rosita/
├── lib/                     # Dependencias locales (driver JDBC)
├── sql/
│   └── schema.sql           # Estructura de base de datos y datos semilla
├── src/
│   └── main/
│       ├── java/com/conary/rosita/
│       │   ├── controller/  # Controladores JavaFX
│       │   ├── dao/         # Data Access Objects (JDBC)
│       │   ├── db/          # Conexión Singleton
│       │   ├── model/       # Entidades de dominio
│       │   └── util/        # Utilidades y formateadores
│       └── resources/
│           ├── css/         # Hojas de estilo CSS
│           └── fxml/        # Vistas de interfaz JavaFX
├── pom.xml                  # Configuración de Maven
├── ejecutar.bat             # Lanzador optimizado para Windows
└── README.md
```
