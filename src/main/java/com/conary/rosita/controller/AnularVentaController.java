package com.conary.rosita.controller;

import com.conary.rosita.dao.VentaDAO;
import com.conary.rosita.model.DetalleVenta;
import com.conary.rosita.model.Venta;
import com.conary.rosita.util.AlertaUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del Modal Irreversible "¿Anular esta venta?" (Tanda 3 del Modulo 1: Vender).
 * Ejecuta la transaccion ACID en VentaDAO devolviendo existencias al inventario y descontando saldo/caja.
 */
public class AnularVentaController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private Label lblResumenEncabezado;
    @FXML private Label lblResumenProductos;
    @FXML private Label lblConsecuenciaPago;
    @FXML private TextField txtMotivoAnulacion;
    @FXML private Label lblAyudaMotivo;
    @FXML private Button btnVolver;
    @FXML private Button btnConfirmarAnulacion;

    private final VentaDAO ventaDAO = new VentaDAO();
    private Venta ventaActual;
    private boolean anulacionConfirmada = false;

    @FXML
    public void initialize() {
        if (txtMotivoAnulacion != null) {
            txtMotivoAnulacion.textProperty().addListener((obs, oldVal, newVal) -> {
                txtMotivoAnulacion.getStyleClass().removeAll("modal-anular-input-error");
                if (lblAyudaMotivo != null) {
                    lblAyudaMotivo.setText("Escriba el motivo para guardar en el historial");
                    lblAyudaMotivo.getStyleClass().removeAll("modal-anular-help-error");
                }
            });

            txtMotivoAnulacion.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    ev.consume();
                    handleConfirmarAnulacion();
                } else if (ev.getCode() == KeyCode.ESCAPE) {
                    ev.consume();
                    handleCancelar();
                }
            });
        }
    }

    public void setVenta(Venta venta, String resumenProductos) {
        this.ventaActual = venta;
        if (venta == null) {
            return;
        }

        String horaStr = formatearHora(venta.getFecha());
        String clienteStr = obtenerNombreClienteLegible(venta);
        String tipoPagoStr = venta.isEsFiado() ? "A fiado" : "De contado";
        String totalStr = "$ " + formatearMoneda(venta.getTotalVenta());

        if (lblResumenEncabezado != null) {
            lblResumenEncabezado.setText(horaStr + " · " + clienteStr + " · " + tipoPagoStr + " · " + totalStr);
        }

        if (lblResumenProductos != null) {
            String detalleTexto = resumenProductos;
            if (detalleTexto == null || detalleTexto.trim().isEmpty()) {
                detalleTexto = construirDetalleProductos(venta.getDetalles());
            }
            lblResumenProductos.setText(detalleTexto);
        }

        if (lblConsecuenciaPago != null) {
            if (venta.isEsFiado()) {
                lblConsecuenciaPago.setText("Se descuenta de la cuenta de " + clienteStr + ".");
            } else {
                lblConsecuenciaPago.setText("Se descuenta del dinero esperado en la caja de hoy.");
            }
        }

        Platform.runLater(() -> {
            if (txtMotivoAnulacion != null) {
                txtMotivoAnulacion.requestFocus();
            }
        });
    }

    public boolean isAnulacionConfirmada() {
        return anulacionConfirmada;
    }

    @FXML
    public void handleConfirmarAnulacion() {
        if (ventaActual == null) {
            return;
        }

        String motivo = txtMotivoAnulacion != null && txtMotivoAnulacion.getText() != null
                ? txtMotivoAnulacion.getText().trim()
                : "";

        if (motivo.isEmpty()) {
            if (txtMotivoAnulacion != null) {
                if (!txtMotivoAnulacion.getStyleClass().contains("modal-anular-input-error")) {
                    txtMotivoAnulacion.getStyleClass().add("modal-anular-input-error");
                }
                txtMotivoAnulacion.requestFocus();
            }
            if (lblAyudaMotivo != null) {
                lblAyudaMotivo.setText("⚠ Debe escribir el motivo por el cual anula la venta.");
                if (!lblAyudaMotivo.getStyleClass().contains("modal-anular-help-error")) {
                    lblAyudaMotivo.getStyleClass().add("modal-anular-help-error");
                }
            }
            return;
        }

        try {
            boolean exito = ventaDAO.anularVenta(ventaActual.getIdMovimiento(), motivo);
            if (exito) {
                this.anulacionConfirmada = true;
                cerrarVentana();
            }
        } catch (Exception e) {
            AlertaUtil.mostrarError("Error al Anular Venta",
                    "No se pudo anular la venta en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        this.anulacionConfirmada = false;
        cerrarVentana();
    }

    private void cerrarVentana() {
        if (btnVolver != null && btnVolver.getScene() != null) {
            Stage stage = (Stage) btnVolver.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }

    private String construirDetalleProductos(List<DetalleVenta> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return "Productos de mostrador";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < detalles.size(); i++) {
            DetalleVenta d = detalles.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(d.getNombreProducto()).append(" × ").append(d.getCantidad());
        }
        return sb.toString();
    }

    private String obtenerNombreClienteLegible(Venta v) {
        if (v == null || v.getNombreCliente() == null) {
            return "Público";
        }
        String raw = v.getNombreCliente().trim();
        if (raw.isEmpty()
                || "CLIENTE GENERAL".equalsIgnoreCase(raw)
                || raw.toLowerCase(LOCALE_ES_CO).contains("público")) {
            return "Público";
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
