package com.conary.rosita.controller;

import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.model.Producto;
import com.conary.rosita.util.AlertaUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Controlador del Modal de Confirmacion: ¿Volver a activar este producto? (Modulo 4 — Productos y precios).
 * Cumple estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ReactivarProductoController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final Map<Integer, LocalDate> FECHAS_BAJA_SESION = new HashMap<Integer, LocalDate>();

    public static void registrarFechaBaja(int idProducto) {
        FECHAS_BAJA_SESION.put(Integer.valueOf(idProducto), LocalDate.now());
    }

    @FXML private Label lblProductoResumen;
    @FXML private Label lblFechaBaja;
    @FXML private Label lblPrecioActualInfo;
    @FXML private HBox boxAvisoStockCero;

    @FXML private Button btnNoVolver;
    @FXML private Button btnConfirmarReactivar;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private Producto productoActual;
    private boolean reactivacionConfirmada = false;

    @FXML
    public void initialize() {
        // Inicializado al recibir el producto con setProducto(...)
    }

    public void setProducto(Producto producto) {
        if (producto == null) {
            return;
        }
        this.productoActual = producto;

        if (lblProductoResumen != null) {
            lblProductoResumen.setText(producto.getNombre() + " · " + formatearCodigo(producto.getIdProducto()));
        }

        if (lblFechaBaja != null) {
            LocalDate fecha = FECHAS_BAJA_SESION.get(Integer.valueOf(producto.getIdProducto()));
            if (fecha == null) {
                fecha = LocalDate.now();
            }
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", LOCALE_ES_CO);
            lblFechaBaja.setText("Dado de baja el " + fecha.format(fmt));
        }

        if (lblPrecioActualInfo != null) {
            lblPrecioActualInfo.setText("Precio actual: $ " + formatearMoneda(producto.getPrecioVenta())
                    + ". Si cambió, modifíquelo después.");
        }

        boolean stockCero = producto.getStockActual() <= 0;
        if (boxAvisoStockCero != null) {
            boxAvisoStockCero.setVisible(stockCero);
            boxAvisoStockCero.setManaged(stockCero);
        }
    }

    public boolean isReactivacionConfirmada() {
        return reactivacionConfirmada;
    }

    @FXML
    public void handleConfirmarReactivar() {
        if (productoActual == null) {
            cerrarVentana();
            return;
        }

        try {
            boolean exito = productoDAO.cambiarEstado(productoActual.getIdProducto(), true);
            if (exito) {
                FECHAS_BAJA_SESION.remove(Integer.valueOf(productoActual.getIdProducto()));
                this.reactivacionConfirmada = true;
                cerrarVentana();
            } else {
                AlertaUtil.mostrarError("No se pudo activar",
                        "La base de datos no confirmó la reactivación del producto.");
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "Ocurrió un problema al volver a activar el producto en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        this.reactivacionConfirmada = false;
        cerrarVentana();
    }

    private void cerrarVentana() {
        if (btnNoVolver != null && btnNoVolver.getScene() != null && btnNoVolver.getScene().getWindow() instanceof Stage) {
            ((Stage) btnNoVolver.getScene().getWindow()).close();
        }
    }

    private String formatearCodigo(int idProducto) {
        return String.format("PRD-%03d", Integer.valueOf(idProducto));
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
