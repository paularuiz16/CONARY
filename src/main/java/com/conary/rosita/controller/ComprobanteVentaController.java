package com.conary.rosita.controller;

import com.conary.rosita.dao.VentaDAO;
import com.conary.rosita.model.DetalleVenta;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.model.Venta;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista "Comprobante de venta" (Tanda 3 del Modulo 1: Vender).
 * Actualiza en tiempo real la vista previa del recibo interno y persiste el comprobante en MySQL.
 */
public class ComprobanteVentaController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final DateTimeFormatter FMT_HORA_CORTA = DateTimeFormatter.ofPattern("HH:mm");
    private static Venta ventaPendienteComprobante;

    public static void setVentaPendienteComprobante(Venta venta) {
        ventaPendienteComprobante = venta;
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverTop;
    @FXML private Label lblFechaHeader;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRolBadge;

    // Columna Izquierda: 1. Datos del cliente (opcional)
    @FXML private TextField txtNombreClienteComprobante;
    @FXML private TextField txtCedulaNit;
    @FXML private TextField txtNumeroFacturaExterna;
    @FXML private Label lblResumenVentaHoraTotal;
    @FXML private Label lblResumenVentaItemsModo;
    @FXML private Button btnGenerarComprobante;
    @FXML private Button btnVolverInferior;

    // Columna Derecha: 2. Asi quedara el comprobante (Vista previa en tiempo real)
    @FXML private Label lblNumeroComprobantePill;
    @FXML private Label lblFechaHoraVentaRecibo;
    @FXML private Label lblClienteEnVivo;
    @FXML private Label lblFacturaExternaEnVivo;
    @FXML private VBox boxItemsComprobante;
    @FXML private Label lblTotalRecibo;

    private final VentaDAO ventaDAO = new VentaDAO();
    private Venta ventaActual;

    @FXML
    public void initialize() {
        configurarEncabezado();
        configurarListenersVistaPrevia();
        if (ventaPendienteComprobante != null) {
            setVenta(ventaPendienteComprobante);
        }
    }

    private void configurarEncabezado() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        String nombre = "Doña Rosita";
        String rolBadge = "ADMINISTRADORA";

        if (u != null) {
            String raw = u.getNombre() != null ? u.getNombre().trim() : "Usuario";
            if (raw.toLowerCase(LOCALE_ES_CO).contains("rosita")
                    && !raw.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
                nombre = "Doña " + raw;
            } else {
                nombre = raw;
            }
            rolBadge = u.esAdministrador() ? "ADMINISTRADORA" : "VENDEDORA";
        }

        if (lblUsuarioNombre != null) {
            lblUsuarioNombre.setText(nombre);
        }
        if (lblUsuarioRolBadge != null) {
            lblUsuarioRolBadge.setText(rolBadge);
        }
        if (lblFechaHeader != null) {
            lblFechaHeader.setText(formatearFechaLarga(LocalDate.now()));
        }
    }

    private void configurarListenersVistaPrevia() {
        if (txtNombreClienteComprobante != null) {
            txtNombreClienteComprobante.textProperty().addListener((obs, oldVal, newVal) -> actualizarVistaPreviaCliente());
        }
        if (txtCedulaNit != null) {
            txtCedulaNit.textProperty().addListener((obs, oldVal, newVal) -> actualizarVistaPreviaCliente());
        }
        if (txtNumeroFacturaExterna != null) {
            txtNumeroFacturaExterna.textProperty().addListener((obs, oldVal, newVal) -> actualizarVistaPreviaCliente());
        }
    }

    public void setVenta(Venta venta) {
        this.ventaActual = venta;
        if (venta == null) {
            return;
        }

        List<DetalleVenta> detalles = venta.getDetalles();
        if (detalles == null || detalles.isEmpty()) {
            try {
                detalles = ventaDAO.obtenerDetallesVenta(venta.getIdMovimiento());
                venta.setDetalles(detalles);
            } catch (SQLException ignored) {
            }
        }

        // Precargar nombre del cliente si la venta ya tenia uno distinto de Publico
        String clienteExistente = obtenerNombreClienteReal(venta);
        if (txtNombreClienteComprobante != null) {
            txtNombreClienteComprobante.setText(clienteExistente);
        }

        String horaCorta = venta.getFecha() != null ? venta.getFecha().format(FMT_HORA_CORTA) : "--:--";
        String totalFormateado = "$ " + formatearMoneda(venta.getTotalVenta());
        int numProductos = detalles != null ? detalles.size() : 0;
        String modoPago = venta.isEsFiado() ? "Fiado" : "Contado";

        if (lblResumenVentaHoraTotal != null) {
            lblResumenVentaHoraTotal.setText("Venta de las " + horaCorta + " · " + totalFormateado);
        }
        if (lblResumenVentaItemsModo != null) {
            lblResumenVentaItemsModo.setText((numProductos == 1 ? "1 producto cobrado" : numProductos + " productos cobrados")
                    + " en mostrador (" + modoPago + ").");
        }

        if (lblNumeroComprobantePill != null) {
            int numComp = Math.max(1, venta.getIdMovimiento());
            lblNumeroComprobantePill.setText(String.format("Comprobante No. %04d", Integer.valueOf(numComp)));
        }

        if (lblFechaHoraVentaRecibo != null) {
            LocalDateTime dt = venta.getFecha() != null ? venta.getFecha() : LocalDateTime.now();
            lblFechaHoraVentaRecibo.setText(formatearFechaSinDiaSemana(dt.toLocalDate()) + " · " + formatearHoraAmPm(dt));
        }

        if (boxItemsComprobante != null) {
            boxItemsComprobante.getChildren().clear();
            if (detalles != null) {
                for (DetalleVenta d : detalles) {
                    HBox fila = new HBox(10);
                    fila.setAlignment(Pos.CENTER_LEFT);

                    Label lblProd = new Label(d.getNombreProducto() + "  × " + d.getCantidad());
                    lblProd.getStyleClass().add("comprobante-receipt-item-name");
                    lblProd.setWrapText(true);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label lblSub = new Label("$ " + formatearMoneda(d.getSubtotal()));
                    lblSub.getStyleClass().add("comprobante-receipt-item-price");

                    fila.getChildren().addAll(lblProd, spacer, lblSub);
                    boxItemsComprobante.getChildren().add(fila);
                }
            }
        }

        if (lblTotalRecibo != null) {
            lblTotalRecibo.setText(totalFormateado);
        }

        actualizarVistaPreviaCliente();
    }

    private void actualizarVistaPreviaCliente() {
        String nombre = txtNombreClienteComprobante != null && txtNombreClienteComprobante.getText() != null
                ? txtNombreClienteComprobante.getText().trim()
                : "";
        String cedula = txtCedulaNit != null && txtCedulaNit.getText() != null
                ? txtCedulaNit.getText().trim()
                : "";
        String factura = txtNumeroFacturaExterna != null && txtNumeroFacturaExterna.getText() != null
                ? txtNumeroFacturaExterna.getText().trim()
                : "";

        String textoCliente = nombre.isEmpty() ? "Público" : nombre;
        if (!cedula.isEmpty()) {
            textoCliente = textoCliente + "  (C.C./NIT: " + cedula + ")";
        }

        if (lblClienteEnVivo != null) {
            lblClienteEnVivo.setText(textoCliente);
        }

        if (lblFacturaExternaEnVivo != null) {
            boolean hayFactura = !factura.isEmpty();
            lblFacturaExternaEnVivo.setVisible(hayFactura);
            lblFacturaExternaEnVivo.setManaged(hayFactura);
            if (hayFactura) {
                lblFacturaExternaEnVivo.setText("Factura anotada: " + factura);
            }
        }
    }

    @FXML
    public void handleGenerarComprobante() {
        if (ventaActual == null) {
            return;
        }

        String nombre = txtNombreClienteComprobante != null && txtNombreClienteComprobante.getText() != null
                ? txtNombreClienteComprobante.getText().trim()
                : "";
        String cedula = txtCedulaNit != null && txtCedulaNit.getText() != null
                ? txtCedulaNit.getText().trim()
                : "";
        String factura = txtNumeroFacturaExterna != null && txtNumeroFacturaExterna.getText() != null
                ? txtNumeroFacturaExterna.getText().trim()
                : "";

        StringBuilder metaComprobante = new StringBuilder("[COMPROBANTE]");
        if (!nombre.isEmpty()) {
            metaComprobante.append(" Cliente: ").append(nombre);
        } else {
            metaComprobante.append(" Cliente: Público");
        }
        if (!cedula.isEmpty()) {
            metaComprobante.append(" | Doc: ").append(cedula);
        }
        if (!factura.isEmpty()) {
            metaComprobante.append(" | Factura: ").append(factura);
        }

        try {
            ventaDAO.guardarComprobanteInterno(ventaActual.getIdMovimiento(), metaComprobante.toString());
            VentasHoyController.registrarComprobanteEmitido(ventaActual.getIdMovimiento());
            ventaPendienteComprobante = null;

            MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
            if (menu != null) {
                menu.cargarVistaEnContenido("/fxml/ventas_hoy.fxml");
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Guardar Comprobante",
                    "No se pudo guardar el comprobante en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleVolverVentasHoy() {
        ventaPendienteComprobante = null;
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/ventas_hoy.fxml");
        }
    }

    private String obtenerNombreClienteReal(Venta v) {
        if (v == null || v.getNombreCliente() == null) {
            return "";
        }
        String raw = v.getNombreCliente().trim();
        if (raw.isEmpty()
                || "CLIENTE GENERAL".equalsIgnoreCase(raw)
                || "Público".equalsIgnoreCase(raw)
                || raw.toLowerCase(LOCALE_ES_CO).contains("público")) {
            return "";
        }
        return raw;
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(monto);
    }

    private String formatearFechaLarga(LocalDate fecha) {
        LocalDate f = fecha != null ? fecha : LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", LOCALE_ES_CO);
        String txt = f.format(fmt);
        if (txt == null || txt.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(txt.charAt(0)) + txt.substring(1);
    }

    private String formatearFechaSinDiaSemana(LocalDate fecha) {
        LocalDate f = fecha != null ? fecha : LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", LOCALE_ES_CO);
        return f.format(fmt);
    }

    private String formatearHoraAmPm(LocalDateTime fechaHora) {
        LocalDateTime dt = fechaHora != null ? fechaHora : LocalDateTime.now();
        int h24 = dt.getHour();
        int min = dt.getMinute();
        int h12 = h24 % 12;
        if (h12 == 0) {
            h12 = 12;
        }
        String sufijo = h24 < 12 ? "a. m." : "p. m.";
        return String.format("%d:%02d %s", Integer.valueOf(h12), Integer.valueOf(min), sufijo);
    }
}
