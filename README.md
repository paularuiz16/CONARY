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

## ☕ Versiones de Java y Compatibilidad

> [!IMPORTANT]
> **Versión requerida:** **Java SE 8 (JDK/JRE 1.8.0_xxx)** de 32 o 64 bits (ej. `jre1.8.0_511` o `jdk1.8.0_xxx`).

### ¿Por qué Java 8?
1. **JavaFX Integrado:** En Java 8 (Oracle JDK/JRE), **JavaFX viene preinstalado de forma nativa** (`jfxrt.jar`), por lo que no requiere descargas pesadas de SDKs externos ni configuración de módulos `module-info.java`.
2. **Consumo de Memoria Reducido:** Diseñado para correr con un consumo base menor a 120 MB de RAM, ideal para equipos modestos de 2 GB de RAM.
3. **Incompatibilidad con Java 11 / 17 / 21:** Versiones posteriores eliminaron JavaFX del núcleo de Java. Si intentas compilar o ejecutar este proyecto directamente con Java 11+, fallará con errores como `ClassNotFoundException: javafx.application.Application` a menos que se mantenga el runtime en Java 8.

---

## 📥 Clonar el Repositorio

Abre una terminal (PowerShell, Git Bash o CMD) en la carpeta donde desees ubicar el proyecto y ejecuta:

```bash
git clone https://github.com/paularuiz16/CONARY.git
cd CONARY
```

---

## 🤖 Configuración para Antigravity e IDEs con Agentes de IA

Si vas a abrir este proyecto en **Antigravity**, **VS Code**, **Cursor** u otros entornos que usan extensiones de Java y asistentes de IA, sigue estas recomendaciones para evitar advertencias y conflictos de versiones:

### 1. Configuración de `.vscode/settings.json` (Ya incluida en el repositorio)
El proyecto incluye la configuración para que el servidor de lenguaje reconozca Java 8 sin alterar el JDK moderno que el IDE use internamente:

```json
{
    "java.configuration.runtimes": [
        {
            "name": "JavaSE-1.8",
            "path": "C:\\Program Files\\Java\\jre1.8.0_511",
            "default": true
        }
    ]
}
```
*(Ajusta la ruta `path` si tu JRE/JDK 8 está instalado en otra ubicación, como `C:\Program Files (x86)\Java\jre1.8.0_xxx`)*.

### 2. Instrucción para Agentes de IA (Prompt de Contexto)
Si trabajas con un asistente o agente de IA en el proyecto, indícale siempre este contexto:
> *"Este proyecto está desarrollado estrictamente en Java 8 con JavaFX nativo y JDBC puro para MySQL. No introduzcas módulos de Java 9+ (module-info.java) ni dependencias externas de JavaFX 11+ para preservar la compatibilidad con Windows 8 y 2 GB de RAM."*

---

## 🛠️ Puesta en Marcha

### 1. Base de Datos (MySQL / XAMPP)
1. Iniciar **MySQL** en el Panel de XAMPP.
2. Importar el script `sql/schema.sql` (en phpMyAdmin o por consola):
   ```bash
   mysql -u root -p < sql/schema.sql
   ```
3. Usuarios creados:
   - **Administrador:** `rosita` | Clave: `admin123`
   - **Vendedor:** `gladys` | Clave: `vendedor123`

### 2. Compilación (Maven)
```bash
mvn clean package
```

### 3. Ejecución
- **Lanzador normal:** Doble clic en `ejecutar.bat`.
- **Modo consola (muestra logs):** Doble clic en `ejecutar_consola.bat`.
- **Desde el IDE:** Ejecutar la clase `com.conary.rosita.MainLauncher`.

---

## 📂 Estructura del Proyecto

```
papeleria-rosita/
├── .vscode/                 # Configuración de runtime Java 8 para IDEs
├── lib/                     # Dependencias locales (driver JDBC)
├── sql/
│   └── schema.sql           # Script de base de datos y datos iniciales
├── src/
│   └── main/
│       ├── java/com/conary/rosita/
│       │   ├── controller/  # Controladores JavaFX
│       │   ├── dao/         # Capa de persistencia JDBC
│       │   ├── db/          # Conexión Singleton
│       │   ├── model/       # Entidades del sistema
│       │   └── util/        # Utilidades y seguridad
│       └── resources/
│           ├── css/         # Estilos visuales (styles.css)
│           └── fxml/        # Vistas de interfaz JavaFX
├── pom.xml                  # Configuración de Maven
├── ejecutar.bat             # Lanzador optimizado para Windows
├── ejecutar_consola.bat     # Lanzador con salida de consola
└── README.md
```
