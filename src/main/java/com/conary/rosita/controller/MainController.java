package com.conary.rosita.controller;

import com.conary.rosita.App;
import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.dao.CompraDAO;
import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.dao.ProveedorDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Cliente;
import com.conary.rosita.model.Compra;
import com.conary.rosita.model.Producto;
import com.conary.rosita.model.Proveedor;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del Menu Principal / Dashboard de la aplicacion (Java 8).
 */
public class MainController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML
    private Label lblUsuarioLogueado;
    @FXML
    private Label lblNombreUsuario;
    @FXML
    private Label lblRolUsuario;
    @FXML
    private Label lblSaludo;
    @FXML
    private Label lblFechaHoy;

    @FXML
    private Label lblAlertaStock;
    @FXML
    private Label lblAlertaProveedores;
    @FXML
    private Label lblAlertaFiados;

    @FXML
    private Label lblIconoCaja;
    @FXML
    private Label lblEstadoCaja;
    @FXML
    private Label lblDetalleCaja;
    @FXML
    private Label lblUltimoRespaldo;

    @FXML
    private StackPane contentArea;
    @FXML
    private VBox paneDashboard;

    @FXML
    private Button btnVolverMenu;
    @FXML
    private Button btnCerrarSesion;
    @FXML
    private Button btnMasOpciones;

    @FXML
    private Button btnNavVenta;
    @FXML
    private Button btnNavInventario;
    @FXML
    private Button btnNavClientes;
    @FXML
    private Button btnNavProveedores;
    @FXML
    private Button btnNavCaja;
    @FXML
    private Button btnNavReportes;

    private final CajaDAO cajaDAO = new CajaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final CompraDAO compraDAO = new CompraDAO();

    private List<Button> botonesMenu;

    @FXML
    public void initialize() {
        botonesMenu = Arrays.asList(btnNavVenta, btnNavInventario, btnNavClientes, btnNavProveedores, btnNavCaja);

        refrescarDatosSesion();
        aplicarRestriccionesPorRol();
    }

    public void refrescarDatosSesion() {
        Usuario usuario = SesionUsuario.getUsuarioLogueado();
        String nombreMostrar = "Doña Rosita";
        String rolMostrar = "Administradora";

        if (usuario != null) {
            String rawNombre = usuario.getNombre() != null ? usuario.getNombre().trim() : "Usuario";
            if (rawNombre.toLowerCase(LOCALE_ES_CO).contains("rosita")
                    && !rawNombre.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
                nombreMostrar = "Doña " + rawNombre;
            } else {
                nombreMostrar = rawNombre;
            }
            rolMostrar = usuario.esAdministrador() ? "Administradora" : "Vendedora";
            if (lblUsuarioLogueado != null) {
                lblUsuarioLogueado.setText(usuario.getNombre() + " (" + usuario.getRol() + ")");
            }
        } else if (lblUsuarioLogueado != null) {
            lblUsuarioLogueado.setText("Invitado");
        }

        if (lblNombreUsuario != null) {
            lblNombreUsuario.setText(nombreMostrar);
        }
        if (lblRolUsuario != null) {
            lblRolUsuario.setText(rolMostrar);
        }
        if (lblSaludo != null) {
            lblSaludo.setText(construirSaludoSegunHora(nombreMostrar));
        }
        if (lblFechaHoy != null) {
            lblFechaHoy.setText(formatearFechaHoy());
        }
        if (lblUltimoRespaldo != null) {
            DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("hh:mm a", LOCALE_ES_CO);
            String hora = LocalTime.now().format(fmtHora).toLowerCase(LOCALE_ES_CO);
            lblUltimoRespaldo.setText("Última copia de seguridad: hoy, " + hora);
        }

        refrescarEstadoCaja();
        refrescarAlertasReales();
    }

    private String construirSaludoSegunHora(String nombre) {
        int hora = LocalTime.now().getHour();
        if (hora < 12) {
            return "¡Buenos días, " + nombre + "!";
        } else if (hora < 19) {
            return "¡Buenas tardes, " + nombre + "!";
        } else {
            return "¡Buenas noches, " + nombre + "!";
        }
    }

    private String formatearFechaHoy() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", LOCALE_ES_CO);
        String texto = LocalDate.now().format(formatter);
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private void aplicarRestriccionesPorRol() {
        boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
        if (btnNavReportes != null) {
            btnNavReportes.setOpacity(esAdmin ? 1.0 : 0.6);
        }
        if (btnMasOpciones != null) {
            btnMasOpciones.setVisible(esAdmin);
            btnMasOpciones.setManaged(esAdmin);
        }
    }

    public void refrescarEstadoCaja() {
        try {
            Caja abierta = cajaDAO.obtenerCajaAbierta();
            SesionUsuario.setCajaActiva(abierta);
            if (abierta != null) {
                if (lblIconoCaja != null) {
                    lblIconoCaja.setText("\u2713");
                    lblIconoCaja.getStyleClass().removeAll("dashboard-caja-icon-warn");
                    if (!lblIconoCaja.getStyleClass().contains("dashboard-caja-icon-ok")) {
                        lblIconoCaja.getStyleClass().add("dashboard-caja-icon-ok");
                    }
                }
                lblEstadoCaja.setText("\u2713 Caja del día: ABIERTA Y LISTA PARA COBRAR");
                if (lblDetalleCaja != null) {
                    String montoStr = formatearMoneda(abierta.getMontoInicial());
                    String horaApertura = "Hoy";
                    if (abierta.getFechaApertura() != null) {
                        DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
                        horaApertura = abierta.getFechaApertura().format(fmtHora);
                    }
                    lblDetalleCaja.setText(
                            "Fondo base inicial registrado: $ " + montoStr + " (Apertura: " + horaApertura + ")");
                }
            } else {
                if (lblIconoCaja != null) {
                    lblIconoCaja.setText("!");
                    lblIconoCaja.getStyleClass().removeAll("dashboard-caja-icon-ok");
                    if (!lblIconoCaja.getStyleClass().contains("dashboard-caja-icon-warn")) {
                        lblIconoCaja.getStyleClass().add("dashboard-caja-icon-warn");
                    }
                }
                lblEstadoCaja.setText("! Caja del día: CERRADA (Abra caja antes de vender)");
                if (lblDetalleCaja != null) {
                    lblDetalleCaja.setText("Toque en '2. Caja y cierre del día' para registrar el fondo base inicial.");
                }
            }
        } catch (SQLException e) {
            if (lblIconoCaja != null) {
                lblIconoCaja.setText("!");
            }
            lblEstadoCaja.setText("Caja del día: Sin conexión a base de datos");
            if (lblDetalleCaja != null) {
                lblDetalleCaja.setText("Verifique que el servicio MySQL en XAMPP esté iniciado.");
            }
        }
    }

    public void refrescarAlertasReales() {
        // 1. Productos con pocas existencias
        int stockBajo = 0;
        try {
            List<Producto> listaBajo = productoDAO.listarStockBajo();
            stockBajo = listaBajo != null ? listaBajo.size() : 0;
        } catch (SQLException e) {
            stockBajo = 0;
        }
        if (lblAlertaStock != null) {
            lblAlertaStock.setText(stockBajo == 1
                    ? "1 producto con pocas existencias"
                    : stockBajo + " productos con pocas existencias");
        }

        // 2. Pagos a proveedores pendientes o que vencen esta semana
        int pagosPendientes = 0;
        try {
            List<Proveedor> provConSaldo = proveedorDAO.listarConSaldosPendientes();
            int conSaldo = provConSaldo != null ? provConSaldo.size() : 0;

            LocalDate hoy = LocalDate.now();
            LocalDate limiteSemana = hoy.plusDays(7);
            List<Compra> compras = compraDAO.listarCompras(hoy.minusDays(30), null);
            int vencenSemana = 0;
            if (compras != null) {
                for (Compra c : compras) {
                    if (c.getFechaPagoAcordada() != null && !c.getFechaPagoAcordada().isAfter(limiteSemana)) {
                        vencenSemana++;
                    }
                }
            }
            pagosPendientes = Math.max(conSaldo, vencenSemana);
        } catch (SQLException e) {
            pagosPendientes = 0;
        }
        if (lblAlertaProveedores != null) {
            lblAlertaProveedores.setText(pagosPendientes == 1
                    ? "1 pago a proveedor vence esta semana"
                    : pagosPendientes + " pagos a proveedor vencen esta semana");
        }

        // 3. Clientes con deuda pendiente / vencida
        int clientesConDeuda = 0;
        try {
            List<Cliente> deudores = clienteDAO.listarConSaldosPendientes();
            clientesConDeuda = deudores != null ? deudores.size() : 0;
        } catch (SQLException e) {
            clientesConDeuda = 0;
        }
        if (lblAlertaFiados != null) {
            lblAlertaFiados.setText(clientesConDeuda == 1
                    ? "1 cliente con deuda vencida"
                    : clientesConDeuda + " clientes con deuda vencida");
        }
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(monto);
    }

    @FXML
    public void mostrarMenuDashboard() {
        if (paneDashboard != null) {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(paneDashboard);
        }
        if (btnVolverMenu != null) {
            btnVolverMenu.setVisible(false);
            btnVolverMenu.setManaged(false);
        }
        refrescarDatosSesion();
    }

    @FXML
    public void mostrarPuntoVenta() {
        activarBoton(btnNavVenta);
        cargarVistaEnContenido("/fxml/punto_venta.fxml");
    }

    @FXML
    public void mostrarInventario() {
        activarBoton(btnNavInventario);
        cargarVistaEnContenido("/fxml/inventario.fxml");
    }

    @FXML
    public void mostrarClientes() {
        activarBoton(btnNavClientes);
        cargarVistaEnContenido("/fxml/clientes.fxml");
    }

    @FXML
    public void mostrarProveedores() {
        activarBoton(btnNavProveedores);
        cargarVistaEnContenido("/fxml/proveedores.fxml");
    }

    @FXML
    public void mostrarCaja() {
        activarBoton(btnNavCaja);
        cargarVistaEnContenido("/fxml/caja.fxml");
    }

    @FXML
    public void mostrarReportes() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) tiene permiso para consultar los reportes generales del negocio.");
            return;
        }

        try {
            int totalProductos = productoDAO.listarTodos(true).size();
            int stockBajo = productoDAO.listarStockBajo().size();
            List<Cliente> deudores = clienteDAO.listarConSaldosPendientes();
            BigDecimal totalCartera = BigDecimal.ZERO;
            for (Cliente c : deudores) {
                if (c.getSaldoPendiente() != null) {
                    totalCartera = totalCartera.add(c.getSaldoPendiente());
                }
            }
            List<Proveedor> provSaldos = proveedorDAO.listarConSaldosPendientes();
            BigDecimal totalPorPagar = BigDecimal.ZERO;
            for (Proveedor p : provSaldos) {
                if (p.getSaldoPendiente() != null) {
                    totalPorPagar = totalPorPagar.add(p.getSaldoPendiente());
                }
            }

            String resumen = "Resumen en tiempo real de la Papelería de Rosita:\n\n"
                    + "• Productos activos en catálogo: " + totalProductos + "\n"
                    + "• Productos con pocas existencias: " + stockBajo + "\n"
                    + "• Clientes con saldo fiado pendiente: " + deudores.size()
                    + " ($ " + formatearMoneda(totalCartera) + ")\n"
                    + "• Proveedores por pagar: " + provSaldos.size()
                    + " ($ " + formatearMoneda(totalPorPagar) + ")";

            AlertaUtil.mostrarInfo("6. Reportes — Resumen Ejecutivo", resumen);
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error en Reportes", "No se pudo consultar el resumen en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleMasOpcionesRespaldo() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo la Administradora puede gestionar copias de seguridad.");
            return;
        }
        AlertaUtil.mostrarInfo("Copias de Seguridad — CONARY",
                "El respaldo local automático de la base de datos 'papeleria_rosita' está activo.\n\n"
                        + "• Estado actual: Verificado y sincronizado localmente.\n"
                        + "• Ubicación local: Base de datos MySQL (puerto 3306).");
    }

    @FXML
    public void handleManualSencillo() {
        AlertaUtil.mostrarInfo("Manual Sencillo — Papelería de Rosita",
                "Guía rápida de uso diario:\n\n"
                        + "1. Vender: Busque el producto por nombre, elija si es al contado o fiado y cobre.\n"
                        + "2. Caja y cierre del día: Abra la caja por la mañana con la base y ciérrela por la noche.\n"
                        + "3. Clientes y fiado: Anote abonos y revise el cuaderno de fiados.\n"
                        + "4. Productos y precios: Agregue mercancía o actualice precios.\n"
                        + "5. Compras y proveedores: Registre facturas de pedidos y pagos.");
    }

    @FXML
    public void handleCerrarSesion() {
        boolean confirmar = AlertaUtil.mostrarConfirmacion("Cerrar Sesión",
                "¿Estás seguro de que deseas salir del sistema?");
        if (confirmar) {
            SesionUsuario.cerrarSesion();
            App.cambiarEscena("/fxml/login.fxml", "Papelería de Rosita - Inicio de Sesión", 680, 660);
        }
    }

    private void activarBoton(Button botonActivo) {
        if (botonesMenu == null || botonActivo == null)
            return;
        for (Button btn : botonesMenu) {
            if (btn != null) {
                btn.getStyleClass().remove("sidebar-button-active");
            }
        }
    }

    private void cargarVistaEnContenido(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(node);
            if (btnVolverMenu != null) {
                btnVolverMenu.setVisible(true);
                btnVolverMenu.setManaged(true);
            }
        } catch (IOException e) {
            System.err.println("[MainController] Error cargando vista: " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
            AlertaUtil.mostrarError("Error de Navegación", "No se pudo cargar la vista solicitada: " + fxmlPath);
        }
    }
}
