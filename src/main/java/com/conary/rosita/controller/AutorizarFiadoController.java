package com.conary.rosita.controller;

import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.model.Cliente;
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
 * Controlador del Modal "Autorizar fiado" / "Cambiar cupo de fiado" (Tanda 3 del Modulo 3).
 * Solo la Administradora (Doña Rosita) puede autorizar, cambiar o quitar cupos.
 */
public class AutorizarFiadoController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private Label lblTitulo;
    @FXML private Label lblSubtitulo;
    @FXML private Label lblResumenNombre;
    @FXML private Label lblResumenDerecha;
    @FXML private Label lblResumenDetalle;
    @FXML private HBox boxAlertaCupo;
    @FXML private Label lblAlertaCupo;
    @FXML private HBox boxInputCupo;
    @FXML private TextField txtCupoFiado;
    @FXML private HBox boxConfirmacionCupo;
    @FXML private Label lblConfirmacionCupo;
    @FXML private HBox boxQuitarFiado;
    @FXML private Button btnQuitarFiado;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardarCupo;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private Cliente cliente;
    private BigDecimal deudaActual = BigDecimal.ZERO;
    private boolean modoCambiarCupo = false;
    private boolean cambiosGuardados = false;

    @FXML
    public void initialize() {
        if (txtCupoFiado != null) {
            txtCupoFiado.textProperty().addListener((obs, oldVal, newVal) -> validarEnTiempoReal());
            txtCupoFiado.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    ev.consume();
                    handleGuardarCupo();
                } else if (ev.getCode() == KeyCode.ESCAPE) {
                    ev.consume();
                    handleCancelar();
                }
            });
        }
        validarEnTiempoReal();
    }

    public void setCliente(Cliente c) {
        this.cliente = c;
        if (c == null) {
            return;
        }
        try {
            Cliente fresco = clienteDAO.obtenerPorId(c.getIdCliente());
            if (fresco != null) {
                this.cliente = fresco;
            }
        } catch (SQLException ignored) {
        }

        deudaActual = cliente.getSaldoPendiente() != null ? cliente.getSaldoPendiente() : BigDecimal.ZERO;
        BigDecimal cupoActual = cliente.getLimiteCredito() != null ? cliente.getLimiteCredito() : BigDecimal.ZERO;
        modoCambiarCupo = cliente.isFiadoAutorizado();
        String tel = cliente.getTelefono() != null && !cliente.getTelefono().trim().isEmpty()
                ? cliente.getTelefono().trim()
                : "Sin teléfono";

        if (modoCambiarCupo) {
            lblTitulo.setText("Cambiar cupo de fiado");
            lblSubtitulo.setText("Modificación de límite de crédito para cliente habitual");
            lblResumenNombre.setText(cliente.getNombre());
            lblResumenDerecha.setText("Tel: " + tel);
            lblResumenDerecha.getStyleClass().removeAll("modal-fiado-pill-registrado");
            lblResumenDerecha.getStyleClass().add("modal-fiado-tel");
            lblResumenDetalle.setText("Cupo actual: $ " + formatearMoneda(cupoActual)
                    + "   ·   Debe ahora: $ " + formatearMoneda(deudaActual));
            txtCupoFiado.setText(formatearMoneda(cupoActual));
            btnGuardarCupo.setText("✓   Guardar cupo");
            btnCancelar.setText("Cancelar");
            boxQuitarFiado.setVisible(true);
            boxQuitarFiado.setManaged(true);
        } else {
            lblTitulo.setText("Autorizar fiado");
            lblSubtitulo.setText("Defina el cupo de crédito disponible para este cliente");
            lblResumenNombre.setText("👤  " + cliente.getNombre() + " · " + tel);
            lblResumenDerecha.setText("Cliente Registrado");
            lblResumenDetalle.setText("Todavía no tiene fiado. Hoy debe $ " + formatearMoneda(deudaActual) + ".");
            txtCupoFiado.setText("");
            btnGuardarCupo.setText("✓   Autorizar fiado");
            btnCancelar.setText("✕   Cancelar");
            boxQuitarFiado.setVisible(false);
            boxQuitarFiado.setManaged(false);
        }

        validarEnTiempoReal();
        Platform.runLater(() -> {
            txtCupoFiado.requestFocus();
            txtCupoFiado.positionCaret(txtCupoFiado.getText().length());
        });
    }

    public boolean isCambiosGuardados() {
        return cambiosGuardados;
    }

    private String validarEnTiempoReal() {
        BigDecimal cupo = parsearMonto(txtCupoFiado != null ? txtCupoFiado.getText() : null);
        String error = null;
        if (cupo == null || cupo.compareTo(BigDecimal.ZERO) <= 0) {
            error = modoCambiarCupo
                    ? "⚠  Escriba un valor mayor a cero."
                    : "⚠  Escriba un valor mayor a cero para activar el cupo.";
        } else if (cupo.compareTo(deudaActual) < 0) {
            error = "⚠  El cupo no puede ser menor a lo que debe hoy: $ " + formatearMoneda(deudaActual) + ".";
        }

        boolean hayError = error != null;
        if (boxAlertaCupo != null) {
            boxAlertaCupo.setVisible(hayError);
            boxAlertaCupo.setManaged(hayError);
        }
        if (lblAlertaCupo != null && hayError) {
            lblAlertaCupo.setText(error);
        }
        if (boxInputCupo != null) {
            boxInputCupo.getStyleClass().removeAll("modal-abono-input-box-error");
            if (hayError && cupo != null) {
                boxInputCupo.getStyleClass().add("modal-abono-input-box-error");
            }
        }
        if (boxConfirmacionCupo != null) {
            boxConfirmacionCupo.setVisible(!hayError);
            boxConfirmacionCupo.setManaged(!hayError);
        }
        if (lblConfirmacionCupo != null && !hayError) {
            lblConfirmacionCupo.setText("✓  Podrá deber hasta $ " + formatearMoneda(cupo) + ".");
        }
        if (btnGuardarCupo != null) {
            btnGuardarCupo.setDisable(hayError);
        }
        return error;
    }

    private boolean verificarPermisoAdministradora() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede autorizar, cambiar o quitar cupos de fiado.");
            return false;
        }
        return true;
    }

    @FXML
    public void handleGuardarCupo() {
        if (cliente == null || !verificarPermisoAdministradora()) {
            return;
        }
        if (validarEnTiempoReal() != null) {
            return;
        }
        BigDecimal nuevoCupo = parsearMonto(txtCupoFiado.getText());

        cliente.setFiadoAutorizado(true);
        cliente.setLimiteCredito(nuevoCupo);
        try {
            if (clienteDAO.actualizar(cliente)) {
                cambiosGuardados = true;
                cerrarVentana();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo guardar el cupo de fiado en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleQuitarFiado() {
        if (cliente == null || !verificarPermisoAdministradora()) {
            return;
        }
        String detalleDeuda = deudaActual.compareTo(BigDecimal.ZERO) > 0
                ? "\n\nSu deuda actual de $ " + formatearMoneda(deudaActual) + " se conserva y podrá seguir abonando."
                : "";
        boolean confirmar = AlertaUtil.mostrarConfirmacion("¿Quitar el fiado?",
                "A «" + cliente.getNombre() + "» ya no se le podrá fiar." + detalleDeuda);
        if (!confirmar) {
            return;
        }

        cliente.setFiadoAutorizado(false);
        cliente.setLimiteCredito(BigDecimal.ZERO);
        try {
            if (clienteDAO.actualizar(cliente)) {
                cambiosGuardados = true;
                cerrarVentana();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo quitar el fiado en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        cambiosGuardados = false;
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
