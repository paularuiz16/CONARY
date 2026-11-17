package com.conary.rosita.controller;

import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.PagoProveedorDAO;
import com.conary.rosita.dao.ProveedorDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.PagoProveedor;
import com.conary.rosita.model.Proveedor;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Controlador del Modal "Registrar pago a proveedor" (Tanda 3 del Modulo 5).
 * Registra el movimiento PAGO_PROVEEDOR en transaccion ACID en MySQL.
 */
public class RegistrarPagoProveedorController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private Label lblNombreProveedor;
    @FXML private Label lblDeudaActual;
    @FXML private HBox boxAlertaMonto;
    @FXML private Label lblAlertaMonto;
    @FXML private HBox boxInputMonto;
    @FXML private TextField txtMontoPago;
    @FXML private HBox boxSaldoRestante;
    @FXML private Label lblSaldoRestante;
    @FXML private ToggleButton rbCajaHoy;
    @FXML private ToggleButton rbOtroLado;
    @FXML private HBox boxAlertaCaja;
    @FXML private Label lblAlertaCaja;
    @FXML private Label lblNotaOrigen;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardarPago;

    private final PagoProveedorDAO pagoProveedorDAO = new PagoProveedorDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final CajaDAO cajaDAO = new CajaDAO();
    private final ToggleGroup tgOrigen = new ToggleGroup();

    private Proveedor proveedor;
    private BigDecimal deudaActual = BigDecimal.ZERO;
    private Caja cajaAbiertaHoy;
    private boolean pagoGuardado = false;

    @FXML
    public void initialize() {
        if (rbCajaHoy != null) rbCajaHoy.setToggleGroup(tgOrigen);
        if (rbOtroLado != null) rbOtroLado.setToggleGroup(tgOrigen);
        if (rbCajaHoy != null) rbCajaHoy.setSelected(true);

        tgOrigen.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }
            consultarCajaAbiertaHoy();
            recalcularEnTiempoReal();
        });

        if (txtMontoPago != null) {
            txtMontoPago.textProperty().addListener((obs, oldVal, newVal) -> recalcularEnTiempoReal());
            txtMontoPago.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    ev.consume();
                    handleGuardarPago();
                } else if (ev.getCode() == KeyCode.ESCAPE) {
                    ev.consume();
                    handleCancelar();
                }
            });
        }

        consultarCajaAbiertaHoy();
        recalcularEnTiempoReal();
    }

    private void consultarCajaAbiertaHoy() {
        cajaAbiertaHoy = null;
        try {
            Caja abierta = cajaDAO.obtenerCajaAbierta();
            if (abierta != null && abierta.getFechaApertura() != null
                    && !abierta.getFechaApertura().toLocalDate().isBefore(LocalDate.now())) {
                cajaAbiertaHoy = abierta;
            }
        } catch (SQLException e) {
            System.err.println("[RegistrarPagoProveedorController] Error consultando caja: " + e.getMessage());
        }

        boolean origenCaja = tgOrigen.getSelectedToggle() == rbCajaHoy;
        boolean sinCajaParaCajaHoy = origenCaja && cajaAbiertaHoy == null;

        if (boxAlertaCaja != null) {
            boxAlertaCaja.setVisible(sinCajaParaCajaHoy);
            boxAlertaCaja.setManaged(sinCajaParaCajaHoy);
        }
        if (lblNotaOrigen != null) {
            if (origenCaja) {
                lblNotaOrigen.setText("💵 Este pago se restará del efectivo esperado en el cierre de caja de hoy.");
            } else {
                lblNotaOrigen.setText("🏦 Dinero externo o transferencia. No afecta el efectivo de la caja registradora.");
            }
        }
    }

    public void setProveedor(Proveedor p) {
        this.proveedor = p;
        if (p == null) {
            return;
        }
        try {
            Proveedor fresco = proveedorDAO.obtenerPorId(p.getIdProveedor());
            if (fresco != null) {
                this.proveedor = fresco;
            }
        } catch (SQLException ignored) {
        }

        deudaActual = proveedor.getSaldoPendiente() != null ? proveedor.getSaldoPendiente() : BigDecimal.ZERO;
        if (lblNombreProveedor != null) {
            lblNombreProveedor.setText(proveedor.getNombre());
        }
        if (lblDeudaActual != null) {
            lblDeudaActual.setText("$ " + formatearMoneda(deudaActual));
        }

        consultarCajaAbiertaHoy();
        recalcularEnTiempoReal();
        Platform.runLater(() -> {
            if (txtMontoPago != null) {
                txtMontoPago.requestFocus();
            }
        });
    }

    public boolean isPagoGuardado() {
        return pagoGuardado;
    }

    private void recalcularEnTiempoReal() {
        String texto = txtMontoPago != null ? txtMontoPago.getText() : null;
        boolean vacio = texto == null || texto.trim().isEmpty();
        BigDecimal monto = parsearMonto(texto);

        String mensajeError = null;
        if (!vacio) {
            if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                mensajeError = "⚠  Escriba un valor mayor a cero.";
            } else if (monto.compareTo(deudaActual) > 0) {
                mensajeError = "⚠  No puede pagar más de lo que le debe: $ " + formatearMoneda(deudaActual) + ".";
            }
        }

        boolean hayErrorMonto = mensajeError != null;
        if (boxAlertaMonto != null) {
            boxAlertaMonto.setVisible(hayErrorMonto);
            boxAlertaMonto.setManaged(hayErrorMonto);
        }
        if (lblAlertaMonto != null && hayErrorMonto) {
            lblAlertaMonto.setText(mensajeError);
        }
        if (boxInputMonto != null) {
            boxInputMonto.getStyleClass().removeAll("modal-abono-input-box-error");
            if (hayErrorMonto) {
                boxInputMonto.getStyleClass().add("modal-abono-input-box-error");
            }
        }

        BigDecimal montoValido = (!vacio && !hayErrorMonto && monto != null) ? monto : BigDecimal.ZERO;
        BigDecimal saldoRestante = deudaActual.subtract(montoValido);
        if (saldoRestante.compareTo(BigDecimal.ZERO) < 0) {
            saldoRestante = BigDecimal.ZERO;
        }
        if (lblSaldoRestante != null) {
            if (saldoRestante.compareTo(BigDecimal.ZERO) == 0) {
                lblSaldoRestante.setText("Quedará al día ($ 0).");
            } else {
                lblSaldoRestante.setText("Quedará debiendo $ " + formatearMoneda(saldoRestante) + ".");
            }
        }

        boolean origenCajaSinAbrir = (tgOrigen.getSelectedToggle() == rbCajaHoy) && (cajaAbiertaHoy == null);
        if (btnGuardarPago != null) {
            btnGuardarPago.setDisable(vacio || hayErrorMonto || deudaActual.compareTo(BigDecimal.ZERO) <= 0 || origenCajaSinAbrir);
        }
    }

    @FXML
    public void handleGuardarPago() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede registrar pagos a proveedores.");
            return;
        }
        if (proveedor == null) {
            return;
        }

        BigDecimal monto = parsearMonto(txtMontoPago != null ? txtMontoPago.getText() : null);
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0 || monto.compareTo(deudaActual) > 0) {
            recalcularEnTiempoReal();
            return;
        }

        boolean origenCaja = tgOrigen.getSelectedToggle() == rbCajaHoy;
        consultarCajaAbiertaHoy();
        if (origenCaja && cajaAbiertaHoy == null) {
            recalcularEnTiempoReal();
            return;
        }

        Usuario u = SesionUsuario.getUsuarioLogueado();
        int idUsuario = u != null && u.getIdUsuario() > 0 ? u.getIdUsuario() : 1;
        Integer idCaja = origenCaja ? Integer.valueOf(cajaAbiertaHoy.getIdCaja()) : null;

        BigDecimal saldoNuevo = deudaActual.subtract(monto);
        String descripcion = "Pago a " + proveedor.getNombre()
                + " · Debía $ " + formatearMoneda(deudaActual)
                + " · Queda $ " + formatearMoneda(saldoNuevo)
                + " · Origen: " + (origenCaja ? "CAJA" : "OTRO");
        if (descripcion.length() > 250) {
            descripcion = descripcion.substring(0, 250);
        }

        PagoProveedor pago = new PagoProveedor(descripcion, idUsuario, idCaja, proveedor.getIdProveedor(), monto);

        try {
            if (pagoProveedorDAO.registrarPago(pago)) {
                pagoGuardado = true;
                cerrarVentana();
            }
        } catch (Exception e) {
            AlertaUtil.mostrarError("Error al Registrar Pago",
                    "No se pudo guardar el pago en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        pagoGuardado = false;
        cerrarVentana();
    }

    private void cerrarVentana() {
        if (btnCancelar != null && btnCancelar.getScene() != null) {
            Stage stage = (Stage) btnCancelar.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    private BigDecimal parsearMonto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpio = texto.replace("$", "").replace(".", "").replace(" ", "").replace(",", ".").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols).format(monto);
    }
}