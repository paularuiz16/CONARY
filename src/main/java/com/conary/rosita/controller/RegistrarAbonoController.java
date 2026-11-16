package com.conary.rosita.controller;

import com.conary.rosita.dao.AbonoClienteDAO;
import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.model.AbonoCliente;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Cliente;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Controlador del Modal "Registrar abono" (Tanda 3 del Modulo 3: Clientes y fiado).
 * El saldo del cliente se recalcula automaticamente en la vista v_saldo_cliente al registrar el abono.
 */
public class RegistrarAbonoController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private Label lblNombreCliente;
    @FXML private Label lblDeudaActual;
    @FXML private HBox boxAlertaMonto;
    @FXML private Label lblAlertaMonto;
    @FXML private HBox boxInputMonto;
    @FXML private TextField txtMontoAbono;
    @FXML private HBox boxSaldoRestante;
    @FXML private Label lblSaldoRestante;
    @FXML private Label lblNotaCaja;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardarAbono;

    private final AbonoClienteDAO abonoClienteDAO = new AbonoClienteDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final CajaDAO cajaDAO = new CajaDAO();

    private Cliente cliente;
    private BigDecimal deudaActual = BigDecimal.ZERO;
    private Caja cajaAbierta;
    private boolean abonoGuardado = false;

    @FXML
    public void initialize() {
        if (txtMontoAbono != null) {
            txtMontoAbono.textProperty().addListener((obs, oldVal, newVal) -> recalcularEnTiempoReal());
            txtMontoAbono.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    ev.consume();
                    handleGuardarAbono();
                } else if (ev.getCode() == KeyCode.ESCAPE) {
                    ev.consume();
                    handleCancelar();
                }
            });
        }
        consultarCajaAbierta();
        recalcularEnTiempoReal();
    }

    private void consultarCajaAbierta() {
        try {
            cajaAbierta = cajaDAO.obtenerCajaAbierta();
        } catch (SQLException e) {
            cajaAbierta = null;
            System.err.println("[RegistrarAbonoController] Error al consultar caja abierta: " + e.getMessage());
        }
        if (lblNotaCaja != null) {
            lblNotaCaja.setText(cajaAbierta != null
                    ? "Este dinero entra a la caja de hoy."
                    : "No hay caja abierta: el abono se guardará en la cuenta del cliente, pero no sumará en la caja.");
        }
    }

    public void setCliente(Cliente c) {
        this.cliente = c;
        if (c == null) {
            return;
        }
        // Refrescar saldo real desde MySQL
        try {
            Cliente fresco = clienteDAO.obtenerPorId(c.getIdCliente());
            if (fresco != null) {
                this.cliente = fresco;
            }
        } catch (SQLException ignored) {
        }

        deudaActual = cliente.getSaldoPendiente() != null ? cliente.getSaldoPendiente() : BigDecimal.ZERO;
        if (lblNombreCliente != null) {
            lblNombreCliente.setText(cliente.getNombre());
        }
        if (lblDeudaActual != null) {
            lblDeudaActual.setText("$ " + formatearMoneda(deudaActual));
        }
        recalcularEnTiempoReal();
        Platform.runLater(() -> {
            if (txtMontoAbono != null) {
                txtMontoAbono.requestFocus();
            }
        });
    }

    public boolean isAbonoGuardado() {
        return abonoGuardado;
    }

    private void recalcularEnTiempoReal() {
        String texto = txtMontoAbono != null ? txtMontoAbono.getText() : null;
        boolean vacio = texto == null || texto.trim().isEmpty();
        BigDecimal monto = parsearMonto(texto);

        String mensajeError = null;
        if (!vacio) {
            if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                mensajeError = "⚠  Escriba un valor mayor a cero.";
            } else if (monto.compareTo(deudaActual) > 0) {
                mensajeError = "⚠  No puede pagar más de lo que debe: $ " + formatearMoneda(deudaActual) + ".";
            }
        }

        boolean hayError = mensajeError != null;
        if (boxAlertaMonto != null) {
            boxAlertaMonto.setVisible(hayError);
            boxAlertaMonto.setManaged(hayError);
        }
        if (lblAlertaMonto != null && hayError) {
            lblAlertaMonto.setText(mensajeError);
        }
        if (boxInputMonto != null) {
            boxInputMonto.getStyleClass().removeAll("modal-abono-input-box-error");
            if (hayError) {
                boxInputMonto.getStyleClass().add("modal-abono-input-box-error");
            }
        }

        BigDecimal montoValido = (!vacio && !hayError && monto != null) ? monto : BigDecimal.ZERO;
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

        if (btnGuardarAbono != null) {
            btnGuardarAbono.setDisable(vacio || hayError || deudaActual.compareTo(BigDecimal.ZERO) <= 0);
        }
    }

    @FXML
    public void handleGuardarAbono() {
        if (cliente == null) {
            return;
        }
        BigDecimal monto = parsearMonto(txtMontoAbono != null ? txtMontoAbono.getText() : null);
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0 || monto.compareTo(deudaActual) > 0) {
            recalcularEnTiempoReal();
            return;
        }

        consultarCajaAbierta();
        Usuario u = SesionUsuario.getUsuarioLogueado();
        int idUsuario = u != null && u.getIdUsuario() > 0 ? u.getIdUsuario() : 1;
        Integer idCaja = cajaAbierta != null ? Integer.valueOf(cajaAbierta.getIdCaja()) : null;

        BigDecimal saldoNuevo = deudaActual.subtract(monto);
        String descripcion = "Abono de " + cliente.getNombre()
                + " · Debía $ " + formatearMoneda(deudaActual)
                + " · Queda $ " + formatearMoneda(saldoNuevo);
        if (descripcion.length() > 250) {
            descripcion = descripcion.substring(0, 250);
        }

        AbonoCliente abono = new AbonoCliente(descripcion, idUsuario, idCaja, cliente.getIdCliente(), monto);

        try {
            // Transaccion ACID: inserta movimiento + abono_cliente (commit / rollback en el DAO)
            if (abonoClienteDAO.registrarAbono(abono)) {
                abonoGuardado = true;
                cerrarVentana();
            }
        } catch (Exception e) {
            AlertaUtil.mostrarError("Error al Registrar Abono",
                    "No se pudo guardar el abono en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        abonoGuardado = false;
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
