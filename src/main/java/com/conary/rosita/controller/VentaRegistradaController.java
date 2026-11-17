package com.conary.rosita.controller;

import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.model.Cliente;
import com.conary.rosita.model.DetalleVenta;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.model.Venta;
import com.conary.rosita.util.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista A de la Tanda 2 del Modulo 1: Confirmacion "¡Venta registrada!".
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class VentaRegistradaController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    private static Venta ultimaVentaPendiente;
    private static BigDecimal ultimoValorRecibidoPendiente = BigDecimal.ZERO;
    private static BigDecimal ultimoCambioPendiente = BigDecimal.ZERO;

    public static void prepararDatosConfirmacion(Venta venta, BigDecimal valorRecibido, BigDecimal cambio) {
        ultimaVentaPendiente = venta;
        ultimoValorRecibidoPendiente = valorRecibido != null ? valorRecibido : BigDecimal.ZERO;
        ultimoCambioPendiente = cambio != null ? cambio : BigDecimal.ZERO;
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverMenu;
    @FXML private Button btnHistorialHoyHeader;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRolBadge;

    @FXML private Label lblSubtituloHoraProductos;
    @FXML private VBox boxDetalleItems;
    @FXML private Label lblTotalVenta;

    @FXML private VBox boxResumenContado;
    @FXML private Label lblValorRecibido;
    @FXML private Label lblCambioDevolver;

    @FXML private VBox boxResumenFiado;
    @FXML private Label lblClienteFiado;
    @FXML private Label lblNuevoSaldoFiado;

    @FXML private Button btnNuevaVenta;
    @FXML private Button btnVentasHoy;

    private final ClienteDAO clienteDAO = new ClienteDAO();

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarAtajosTeclado();
        if (ultimaVentaPendiente != null) {
            mostrarDatosVenta(ultimaVentaPendiente, ultimoValorRecibidoPendiente, ultimoCambioPendiente);
        }
        Platform.runLater(() -> {
            if (btnNuevaVenta != null) {
                btnNuevaVenta.requestFocus();
            }
        });
    }

    private void configurarEncabezadoUsuario() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        String nombre = "Doña Rosita";
        String badgeRol = "ADMIN";

        if (u != null) {
            String raw = u.getNombre() != null ? u.getNombre().trim() : "Usuario";
            if (raw.toLowerCase(LOCALE_ES_CO).contains("rosita")
                    && !raw.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
                nombre = "Doña " + raw;
            } else {
                nombre = raw;
            }
            badgeRol = u.esAdministrador() ? "ADMIN" : "VENDEDORA";
        }

        if (lblUsuarioNombre != null) {
            lblUsuarioNombre.setText(nombre);
        }
        if (lblUsuarioRolBadge != null) {
            lblUsuarioRolBadge.setText(badgeRol);
        }
    }

    private void configurarAtajosTeclado() {
        if (rootPane != null) {
            rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    handleNuevaVenta();
                }
            });
        }
    }

    public void mostrarDatosVenta(Venta venta, BigDecimal valorRecibido, BigDecimal cambio) {
        if (venta == null) {
            return;
        }

        List<DetalleVenta> detalles = venta.getDetalles();
        int numProductos = detalles != null ? detalles.size() : 0;
        String horaStr = formatearHora(venta.getFecha());

        if (lblSubtituloHoraProductos != null) {
            lblSubtituloHoraProductos.setText(horaStr + " · "
                    + (numProductos == 1 ? "1 producto" : numProductos + " productos"));
        }

        if (boxDetalleItems != null) {
            boxDetalleItems.getChildren().clear();
            if (detalles != null) {
                for (DetalleVenta dv : detalles) {
                    HBox fila = new HBox(8);
                    fila.setAlignment(Pos.CENTER_LEFT);
                    fila.getStyleClass().add("venta-reg-item-row");

                    Circle dot = new Circle(3.5, Color.web("#C88D55"));
                    Label lblNom = new Label(dv.getNombreProducto());
                    lblNom.getStyleClass().add("venta-reg-item-name");

                    Label lblCant = new Label("× " + dv.getCantidad());
                    lblCant.getStyleClass().add("venta-reg-item-qty");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label lblSub = new Label("$ " + formatearMoneda(dv.getSubtotal()));
                    lblSub.getStyleClass().add("venta-reg-item-subtotal");

                    fila.getChildren().addAll(dot, lblNom, lblCant, spacer, lblSub);
                    boxDetalleItems.getChildren().add(fila);
                }
            }
        }

        BigDecimal total = venta.getTotalVenta() != null ? venta.getTotalVenta() : BigDecimal.ZERO;
        if (lblTotalVenta != null) {
            lblTotalVenta.setText("$ " + formatearMoneda(total));
        }

        boolean esFiado = venta.isEsFiado();
        if (boxResumenContado != null) {
            boxResumenContado.setVisible(!esFiado);
            boxResumenContado.setManaged(!esFiado);
        }
        if (boxResumenFiado != null) {
            boxResumenFiado.setVisible(esFiado);
            boxResumenFiado.setManaged(esFiado);
        }

        if (!esFiado) {
            if (lblValorRecibido != null) {
                lblValorRecibido.setText("$ " + formatearMoneda(valorRecibido));
            }
            if (lblCambioDevolver != null) {
                lblCambioDevolver.setText("$ " + formatearMoneda(cambio));
            }
        } else {
            String nombreCl = venta.getNombreCliente() != null ? venta.getNombreCliente() : "Cliente";
            if (lblClienteFiado != null) {
                lblClienteFiado.setText(nombreCl);
            }
            BigDecimal nuevoSaldo = total;
            if (venta.getIdCliente() != null && venta.getIdCliente().intValue() > 0) {
                try {
                    Cliente cl = clienteDAO.obtenerPorId(venta.getIdCliente().intValue());
                    if (cl != null && cl.getSaldoPendiente() != null) {
                        nuevoSaldo = cl.getSaldoPendiente();
                    }
                } catch (SQLException ignored) {
                }
            }
            if (lblNuevoSaldoFiado != null) {
                lblNuevoSaldoFiado.setText("$ " + formatearMoneda(nuevoSaldo));
            }
        }
    }

    @FXML
    public void handleNuevaVenta() {
        ultimaVentaPendiente = null;
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/vender.fxml");
        }
    }

    @FXML
    public void handleIrAVentasHoy() {
        ultimaVentaPendiente = null;
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/ventas_hoy.fxml");
        }
    }

    @FXML
    public void handleVolverMenu() {
        ultimaVentaPendiente = null;
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarMenuDashboard();
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

    private String formatearHora(LocalDateTime fechaHora) {
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
