package com.conary.rosita.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Controlador del modal irreversible de confirmacion de cierre de caja ("¿Cerrar la caja de hoy?").
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ConfirmarCierreCajaController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private Label lblTituloModal;
    @FXML private Label lblTotalEsperado;
    @FXML private Label lblEfectivoContado;
    @FXML private Button btnConfirmarCierre;
    @FXML private Button btnCancelar;

    private boolean cierreConfirmado = false;

    public void configurarDatos(BigDecimal totalEsperado, BigDecimal efectivoContado, boolean esTurnoAnterior) {
        if (lblTituloModal != null) {
            lblTituloModal.setText(esTurnoAnterior ? "¿Cerrar la caja pendiente?" : "¿Cerrar la caja de hoy?");
        }
        if (lblTotalEsperado != null) {
            lblTotalEsperado.setText("$ " + formatearMoneda(totalEsperado));
        }
        if (lblEfectivoContado != null) {
            lblEfectivoContado.setText("$ " + formatearMoneda(efectivoContado));
        }
    }

    public boolean isCierreConfirmado() {
        return cierreConfirmado;
    }

    @FXML
    public void handleConfirmarCierre() {
        this.cierreConfirmado = true;
        cerrarVentana();
    }

    @FXML
    public void handleCancelar() {
        this.cierreConfirmado = false;
        cerrarVentana();
    }

    private void cerrarVentana() {
        if (btnConfirmarCierre != null && btnConfirmarCierre.getScene() != null) {
            Stage stage = (Stage) btnConfirmarCierre.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
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
}
