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
import com.conary.rosita.util.RespaldoService;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

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
 * Gestiona el estado dinamico de Caja Abierta vs Caja Cerrada, alertas reales y permisos por rol.
 */
public class MenuPrincipalController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static MenuPrincipalController instanciaActiva;

    public static MenuPrincipalController getInstanciaActiva() {
        return instanciaActiva;
    }

    private static final String SVG_CARRITO = "M 4.5 14 C 3.67 14 3.01 14.67 3.01 15.5 C 3.01 16.33 3.67 17 4.5 17 C 5.33 17 6 16.33 6 15.5 C 6 14.67 5.33 14 4.5 14 Z M 0 1 L 0 2.5 L 1.5 2.5 L 4.2 8.2 L 3.2 10 C 3.07 10.22 3 10.48 3 10.75 C 3 11.58 3.67 12.25 4.5 12.25 L 13.5 12.25 L 13.5 10.75 L 4.82 10.75 C 4.71 10.75 4.63 10.67 4.63 10.56 L 4.65 10.47 L 5.33 9.25 L 10.92 9.25 C 11.48 9.25 11.97 8.94 12.23 8.48 L 14.91 3.61 C 14.97 3.5 15 3.38 15 3.25 C 15 2.83 14.66 2.5 14.25 2.5 L 3.16 2.5 L 2.45 1 L 0 1 Z M 12 14 C 11.17 14 10.51 14.67 10.51 15.5 C 10.51 16.33 11.17 17 12 17 C 12.83 17 13.5 16.33 13.5 15.5 C 13.5 14.67 12.83 14 12 14 Z";
    private static final String SVG_CANDADO = "M 11 5 L 10 5 L 10 3.5 C 10 1.57 8.43 0 6.5 0 C 4.57 0 3 1.57 3 3.5 L 3 5 L 2 5 C 1.17 5 0.5 5.67 0.5 6.5 L 0.5 12.5 C 0.5 13.33 1.17 14 2 14 L 11 14 C 11.83 14 12.5 13.33 12.5 12.5 L 12.5 6.5 C 12.5 5.67 11.83 5 11 5 Z M 6.5 10.5 C 5.67 10.5 5 9.83 5 9 C 5 8.17 5.67 7.5 6.5 7.5 C 7.33 7.5 8 8.17 8 9 C 8 9.83 7.33 10.5 6.5 10.5 Z M 8.3 5 L 4.7 5 L 4.7 3.5 C 4.7 2.51 5.51 1.7 6.5 1.7 C 7.49 1.7 8.3 2.51 8.3 3.5 L 8.3 5 Z";

    @FXML protected HBox headerPrincipal;
    @FXML protected Label lblUsuarioLogueado;
    @FXML protected Label lblNombreUsuario;
    @FXML protected Label lblRolUsuario;
    @FXML protected Label lblSaludo;
    @FXML protected Label lblFechaHoy;

    // Elementos dinamicos de la tarjeta 1. Vender
    @FXML protected Label lblBadgeVenta;
    @FXML protected StackPane circleIconoVenta;
    @FXML protected SVGPath svgIconoVenta;
    @FXML protected Label lblTituloVenta;
    @FXML protected Label lblSubtituloVenta;
    @FXML protected HBox boxAvisoAbraCaja;

    // Alertas rapidas
    @FXML protected Label lblAlertaStock;
    @FXML protected Label lblAlertaProveedores;
    @FXML protected Label lblAlertaFiados;

    // Franja de estado de caja y respaldo
    @FXML protected Label lblIconoCaja;
    @FXML protected StackPane paneIconoCajaCerrada;
    @FXML protected Label lblEstadoCaja;
    @FXML protected Label lblBadgeApertura;
    @FXML protected Label lblDetalleCaja;
    @FXML protected HBox boxRespaldo;
    @FXML protected Label lblUltimoRespaldo;
    @FXML protected Button btnAbrirCajaDirecto;

    @FXML protected StackPane contentArea;
    @FXML protected VBox paneDashboard;
    @FXML protected GridPane gridModulos;
    @FXML protected RowConstraints rowModulos1;
    @FXML protected RowConstraints rowModulos2;
    @FXML protected GridPane gridAlertasRapidas;

    @FXML protected Button btnVolverMenu;
    @FXML protected Button btnCerrarSesion;
    @FXML protected Button btnMasOpciones;

    @FXML protected Button btnNavVenta;
    @FXML protected Button btnNavInventario;
    @FXML protected Label lblTituloModuloProductos;
    @FXML protected Label lblSubtituloModuloProductos;
    @FXML protected Button btnNavClientes;
    @FXML protected Button btnNavProveedores;
    @FXML protected Button btnNavCaja;
    @FXML protected Label lblTituloCaja;
    @FXML protected Label lblSubtituloCaja;
    @FXML protected Button btnNavReportes;
    @FXML protected Label lblTextoAyuda;

    protected final CajaDAO cajaDAO = new CajaDAO();
    protected final ProductoDAO productoDAO = new ProductoDAO();
    protected final ClienteDAO clienteDAO = new ClienteDAO();
    protected final ProveedorDAO proveedorDAO = new ProveedorDAO();
    protected final CompraDAO compraDAO = new CompraDAO();
    protected final RespaldoService respaldoService = new RespaldoService();

    protected List<Button> botonesMenu;

    @FXML
    public void initialize() {
        instanciaActiva = this;
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
            String textoCopia = respaldoService.obtenerTextoUltimaCopia()
                    .replace("🕒  ", "")
                    .replace("Última copia:", "Última copia de seguridad:");
            lblUltimoRespaldo.setText(textoCopia);
        }

        refrescarEstadoCaja();
        refrescarAlertasReales();
        aplicarRestriccionesPorRol();
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

        if (lblSubtituloVenta != null) {
            lblSubtituloVenta.setText(esAdmin ? "(Mostrador principal)" : "Mostrador principal");
        }
        if (lblTituloModuloProductos != null) {
            lblTituloModuloProductos.setText(esAdmin ? "4. Productos y precios" : "2. Productos");
        }
        if (lblSubtituloModuloProductos != null) {
            lblSubtituloModuloProductos.setText(esAdmin ? "(Ver y modificar)" : "Buscar y consultar");
        }
        if (lblTituloCaja != null) {
            lblTituloCaja.setText(esAdmin ? "2. Caja y cierre del día" : "3. Caja");
        }
        if (lblSubtituloCaja != null) {
            lblSubtituloCaja.setText(esAdmin ? "(Entradas y salidas)" : "Abrir la caja del día");
        }
        if (lblTextoAyuda != null) {
            lblTextoAyuda.setText(esAdmin
                    ? "¿Necesita ayuda con el sistema? Pídasela a Gladys en el mostrador o abra el manual."
                    : "¿Necesita ayuda con el sistema? Pídasela a Doña Rosita o abra el manual.");
        }

        configurarVisibilidadNodo(btnNavClientes, esAdmin);
        configurarVisibilidadNodo(btnNavProveedores, esAdmin);
        configurarVisibilidadNodo(btnNavReportes, esAdmin);
        configurarVisibilidadNodo(gridAlertasRapidas, esAdmin);
        configurarVisibilidadNodo(btnMasOpciones, esAdmin);
        if (boxRespaldo != null && SesionUsuario.getCajaActiva() != null) {
            configurarVisibilidadNodo(boxRespaldo, esAdmin);
        }

        if (rowModulos1 != null && rowModulos2 != null) {
            if (esAdmin) {
                rowModulos1.setPercentHeight(50.0);
                rowModulos2.setPercentHeight(50.0);
                rowModulos2.setMinHeight(Region.USE_COMPUTED_SIZE);
                rowModulos2.setPrefHeight(Region.USE_COMPUTED_SIZE);
                rowModulos2.setMaxHeight(Region.USE_COMPUTED_SIZE);
            } else {
                rowModulos1.setPercentHeight(100.0);
                rowModulos2.setPercentHeight(0.0);
                rowModulos2.setMinHeight(0.0);
                rowModulos2.setPrefHeight(0.0);
                rowModulos2.setMaxHeight(0.0);
            }
        }

        if (btnNavVenta != null && btnNavInventario != null && btnNavCaja != null) {
            if (esAdmin) {
                GridPane.setColumnIndex(btnNavVenta, 0);
                GridPane.setRowIndex(btnNavVenta, 0);
                GridPane.setColumnIndex(btnNavCaja, 1);
                GridPane.setRowIndex(btnNavCaja, 0);
                GridPane.setColumnIndex(btnNavInventario, 0);
                GridPane.setRowIndex(btnNavInventario, 1);
            } else {
                GridPane.setColumnIndex(btnNavVenta, 0);
                GridPane.setRowIndex(btnNavVenta, 0);
                GridPane.setColumnIndex(btnNavInventario, 1);
                GridPane.setRowIndex(btnNavInventario, 0);
                GridPane.setColumnIndex(btnNavCaja, 2);
                GridPane.setRowIndex(btnNavCaja, 0);
            }
        }

        if (gridModulos != null) {
            gridModulos.setVgap(esAdmin ? 14.0 : 0.0);
            gridModulos.requestLayout();
        }
    }

    private void configurarVisibilidadNodo(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    public void refrescarEstadoCaja() {
        try {
            Caja abierta = cajaDAO.obtenerCajaAbierta();
            SesionUsuario.setCajaActiva(abierta);
            if (abierta != null) {
                configurarVistaCajaAbierta(abierta);
            } else {
                configurarVistaCajaCerrada();
            }
        } catch (SQLException e) {
            SesionUsuario.setCajaActiva(null);
            configurarVistaCajaCerrada();
        }
    }

    private void configurarVistaCajaAbierta(Caja abierta) {
        // 1. Tarjeta 1. Vender en estado activo (PRINCIPAL)
        if (btnNavVenta != null) {
            btnNavVenta.getStyleClass().removeAll("dashboard-module-card-locked");
            if (!btnNavVenta.getStyleClass().contains("dashboard-module-card-primary")) {
                btnNavVenta.getStyleClass().add("dashboard-module-card-primary");
            }
        }
        if (lblBadgeVenta != null) {
            lblBadgeVenta.setText("PRINCIPAL");
            lblBadgeVenta.getStyleClass().removeAll("dashboard-badge-inactivo");
            if (!lblBadgeVenta.getStyleClass().contains("dashboard-badge-principal")) {
                lblBadgeVenta.getStyleClass().add("dashboard-badge-principal");
            }
        }
        if (circleIconoVenta != null) {
            circleIconoVenta.getStyleClass().removeAll("dashboard-module-icon-circle-locked");
            if (!circleIconoVenta.getStyleClass().contains("dashboard-module-icon-circle")) {
                circleIconoVenta.getStyleClass().add("dashboard-module-icon-circle");
            }
        }
        if (svgIconoVenta != null) {
            svgIconoVenta.setContent(SVG_CARRITO);
            svgIconoVenta.setFill(Color.web("#704214"));
        }
        if (lblTituloVenta != null) {
            lblTituloVenta.getStyleClass().removeAll("dashboard-module-title-locked");
            if (!lblTituloVenta.getStyleClass().contains("dashboard-module-title")) {
                lblTituloVenta.getStyleClass().add("dashboard-module-title");
            }
        }
        if (lblSubtituloVenta != null) {
            lblSubtituloVenta.getStyleClass().removeAll("dashboard-module-subtitle-locked");
            if (!lblSubtituloVenta.getStyleClass().contains("dashboard-module-subtitle")) {
                lblSubtituloVenta.getStyleClass().add("dashboard-module-subtitle");
            }
        }
        if (boxAvisoAbraCaja != null) {
            boxAvisoAbraCaja.setVisible(false);
            boxAvisoAbraCaja.setManaged(false);
        }

        // 2. Franja inferior de estado de caja abierta
        if (lblIconoCaja != null) {
            lblIconoCaja.setText("\u2713");
            lblIconoCaja.setVisible(true);
            lblIconoCaja.setManaged(true);
        }
        if (paneIconoCajaCerrada != null) {
            paneIconoCajaCerrada.setVisible(false);
            paneIconoCajaCerrada.setManaged(false);
        }
        if (lblEstadoCaja != null) {
            lblEstadoCaja.setText("\u2713 Caja del día: ABIERTA Y LISTA PARA COBRAR");
        }
        if (lblBadgeApertura != null) {
            lblBadgeApertura.setVisible(false);
            lblBadgeApertura.setManaged(false);
        }
        if (lblDetalleCaja != null) {
            String montoStr = formatearMoneda(abierta.getMontoInicial());
            String horaApertura = "Hoy";
            if (abierta.getFechaApertura() != null) {
                DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
                horaApertura = abierta.getFechaApertura().format(fmtHora);
            }
            lblDetalleCaja.setText("Fondo base inicial registrado: $ " + montoStr + " (Apertura: " + horaApertura + ")");
        }
        if (boxRespaldo != null) {
            boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
            boxRespaldo.setVisible(esAdmin);
            boxRespaldo.setManaged(esAdmin);
        }
        if (btnAbrirCajaDirecto != null) {
            btnAbrirCajaDirecto.setVisible(false);
            btnAbrirCajaDirecto.setManaged(false);
        }
    }

    private void configurarVistaCajaCerrada() {
        // 1. Tarjeta 1. Vender en estado bloqueado (INACTIVO)
        if (btnNavVenta != null) {
            btnNavVenta.getStyleClass().removeAll("dashboard-module-card-primary");
            if (!btnNavVenta.getStyleClass().contains("dashboard-module-card-locked")) {
                btnNavVenta.getStyleClass().add("dashboard-module-card-locked");
            }
        }
        if (lblBadgeVenta != null) {
            lblBadgeVenta.setText("\uD83D\uDD12 INACTIVO");
            lblBadgeVenta.getStyleClass().removeAll("dashboard-badge-principal");
            if (!lblBadgeVenta.getStyleClass().contains("dashboard-badge-inactivo")) {
                lblBadgeVenta.getStyleClass().add("dashboard-badge-inactivo");
            }
        }
        if (circleIconoVenta != null) {
            circleIconoVenta.getStyleClass().removeAll("dashboard-module-icon-circle");
            if (!circleIconoVenta.getStyleClass().contains("dashboard-module-icon-circle-locked")) {
                circleIconoVenta.getStyleClass().add("dashboard-module-icon-circle-locked");
            }
        }
        if (svgIconoVenta != null) {
            svgIconoVenta.setContent(SVG_CANDADO);
            svgIconoVenta.setFill(Color.web("#8D7B6A"));
        }
        if (lblTituloVenta != null) {
            lblTituloVenta.getStyleClass().removeAll("dashboard-module-title");
            if (!lblTituloVenta.getStyleClass().contains("dashboard-module-title-locked")) {
                lblTituloVenta.getStyleClass().add("dashboard-module-title-locked");
            }
        }
        if (lblSubtituloVenta != null) {
            lblSubtituloVenta.getStyleClass().removeAll("dashboard-module-subtitle");
            if (!lblSubtituloVenta.getStyleClass().contains("dashboard-module-subtitle-locked")) {
                lblSubtituloVenta.getStyleClass().add("dashboard-module-subtitle-locked");
            }
        }
        if (boxAvisoAbraCaja != null) {
            boxAvisoAbraCaja.setVisible(true);
            boxAvisoAbraCaja.setManaged(true);
        }

        // 2. Franja inferior de estado de caja cerrada
        if (lblIconoCaja != null) {
            lblIconoCaja.setVisible(false);
            lblIconoCaja.setManaged(false);
        }
        if (paneIconoCajaCerrada != null) {
            paneIconoCajaCerrada.setVisible(true);
            paneIconoCajaCerrada.setManaged(true);
        }
        if (lblEstadoCaja != null) {
            lblEstadoCaja.setText("Caja cerrada");
        }
        if (lblBadgeApertura != null) {
            lblBadgeApertura.setVisible(true);
            lblBadgeApertura.setManaged(true);
        }
        if (lblDetalleCaja != null) {
            lblDetalleCaja.setText("Para vender, primero abra la caja e ingrese el fondo de inicio del día.");
        }
        if (boxRespaldo != null) {
            boxRespaldo.setVisible(false);
            boxRespaldo.setManaged(false);
        }
        if (btnAbrirCajaDirecto != null) {
            btnAbrirCajaDirecto.setVisible(true);
            btnAbrirCajaDirecto.setManaged(true);
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
        if (headerPrincipal != null) {
            headerPrincipal.setVisible(true);
            headerPrincipal.setManaged(true);
        }
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
        refrescarEstadoCaja();
        Caja abierta = SesionUsuario.getCajaActiva();
        if (abierta == null
                || (abierta.getFechaApertura() != null
                    && abierta.getFechaApertura().toLocalDate().isBefore(LocalDate.now()))) {
            mostrarAbrirCajaDirecto();
            return;
        }
        activarBoton(btnNavVenta);
        cargarVistaEnContenido("/fxml/vender.fxml");
    }

    @FXML
    public void mostrarInventario() {
        activarBoton(btnNavInventario);
        cargarVistaEnContenido("/fxml/productos.fxml");
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
    public void mostrarAbrirCajaDirecto() {
        activarBoton(btnNavCaja);
        cargarVistaEnContenido("/fxml/abrir_caja.fxml");
    }

    @FXML
    public void mostrarCaja() {
        activarBoton(btnNavCaja);
        try {
            Caja abierta = cajaDAO.obtenerCajaAbierta();
            SesionUsuario.setCajaActiva(abierta);
            if (abierta == null
                    || (abierta.getFechaApertura() != null
                        && abierta.getFechaApertura().toLocalDate().isBefore(LocalDate.now()))) {
                cargarVistaEnContenido("/fxml/abrir_caja.fxml");
                return;
            }
        } catch (SQLException e) {
            cargarVistaEnContenido("/fxml/abrir_caja.fxml");
            return;
        }
        cargarVistaEnContenido("/fxml/cierre_caja.fxml");
    }

    @FXML
    public void mostrarReportes() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) tiene permiso para consultar los reportes generales del negocio.");
            return;
        }
        activarBoton(btnNavReportes);
        cargarVistaEnContenido("/fxml/reportes.fxml");
    }

    @FXML
    public void handleMasOpcionesRespaldo() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) tiene acceso a Más opciones.");
            return;
        }
        cargarVistaEnContenido("/fxml/mas_opciones.fxml");
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
        boolean confirmar = AlertaUtil.mostrarConfirmacion("Cerrar Sesión", "¿Estás seguro de que deseas salir del sistema?");
        if (confirmar) {
            SesionUsuario.cerrarSesion();
            App.cambiarEscena("/fxml/login.fxml", "Papelería de Rosita - Inicio de Sesión", 680, 660);
        }
    }

    protected void activarBoton(Button botonActivo) {
        if (botonesMenu == null || botonActivo == null) return;
        for (Button btn : botonesMenu) {
            if (btn != null) {
                btn.getStyleClass().remove("sidebar-button-active");
            }
        }
    }

    /** Carga una vista FXML en el area principal de contenido. */
    public void cargarVistaEnContenido(String fxmlPath) {
        cargarVistaConControlador(fxmlPath);
    }

    public <T> T cargarVistaConControlador(String fxmlPath) {
        try {
            java.net.URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                java.io.File fallback = new java.io.File("src/main/resources" + fxmlPath);
                if (fallback.exists()) {
                    url = fallback.toURI().toURL();
                }
            }
            if (url == null) {
                throw new IOException("No se encontró el recurso FXML: " + fxmlPath);
            }
            FXMLLoader loader = new FXMLLoader(url);
            Node node = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(node);

            boolean usaHeaderPropio = "/fxml/vender.fxml".equals(fxmlPath)
                    || "/fxml/venta_registrada.fxml".equals(fxmlPath)
                    || "/fxml/ventas_hoy.fxml".equals(fxmlPath)
                    || "/fxml/comprobante_venta.fxml".equals(fxmlPath)
                    || "/fxml/clientes.fxml".equals(fxmlPath)
                    || "/fxml/registrar_cliente.fxml".equals(fxmlPath)
                    || "/fxml/modificar_cliente.fxml".equals(fxmlPath)
                    || "/fxml/estado_cuenta_cliente.fxml".equals(fxmlPath)
                    || "/fxml/caja.fxml".equals(fxmlPath)
                    || "/fxml/abrir_caja.fxml".equals(fxmlPath)
                    || "/fxml/cierre_caja.fxml".equals(fxmlPath)
                    || "/fxml/productos.fxml".equals(fxmlPath)
                    || "/fxml/registrar_producto.fxml".equals(fxmlPath)
                    || "/fxml/modificar_producto.fxml".equals(fxmlPath)
                    || "/fxml/proveedores.fxml".equals(fxmlPath)
                    || "/fxml/registrar_proveedor.fxml".equals(fxmlPath)
                    || "/fxml/registrar_compra.fxml".equals(fxmlPath)
                    || "/fxml/cuenta_proveedor.fxml".equals(fxmlPath)
                    || "/fxml/modificar_proveedor.fxml".equals(fxmlPath)
                    || "/fxml/reportes.fxml".equals(fxmlPath)
                    || "/fxml/resultado_reporte_ventas.fxml".equals(fxmlPath)
                    || "/fxml/resultado_reporte_inventario.fxml".equals(fxmlPath)
                    || "/fxml/resultado_reporte_general.fxml".equals(fxmlPath)
                    || "/fxml/mas_opciones.fxml".equals(fxmlPath)
                    || "/fxml/usuarios.fxml".equals(fxmlPath)
                    || "/fxml/registrar_usuario.fxml".equals(fxmlPath)
                    || "/fxml/copias_seguridad.fxml".equals(fxmlPath)
                    || "/fxml/pasar_datos_excel.fxml".equals(fxmlPath)
                    || "/fxml/resultado_pasar_excel.fxml".equals(fxmlPath);
            if (headerPrincipal != null) {
                headerPrincipal.setVisible(!usaHeaderPropio);
                headerPrincipal.setManaged(!usaHeaderPropio);
            }
            if (btnVolverMenu != null) {
                btnVolverMenu.setVisible(!usaHeaderPropio);
                btnVolverMenu.setManaged(!usaHeaderPropio);
            }
            return loader.getController();
        } catch (IOException e) {
            System.err.println("[MenuPrincipalController] Error cargando vista: " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
            AlertaUtil.mostrarError("Error de Navegación", "No se pudo cargar la vista solicitada: " + fxmlPath);
            return null;
        }
    }
}
