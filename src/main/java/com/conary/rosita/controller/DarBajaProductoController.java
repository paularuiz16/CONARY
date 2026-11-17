package com.conary.rosita.controller;

import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.model.Producto;
import com.conary.rosita.util.AlertaUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Controlador del Modal B: ¿Dar de baja este producto? (Modulo 4 — Productos y precios).
 * Cumple estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class DarBajaProductoController {

    @FXML private Label lblProductoResumen;
    @FXML private HBox boxAvisoStock;
    @FXML private Label lblAvisoStock;

    @FXML private Button btnNoVolver;
    @FXML private Button btnConfirmarBaja;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private Producto productoActual;
    private boolean bajaConfirmada = false;

    @FXML
    public void initialize() {
        // Estado inicial limpio hasta recibir el producto mediante setProducto(...)
    }

    public void setProducto(Producto producto) {
        if (producto == null) {
            return;
        }
        this.productoActual = producto;

        if (lblProductoResumen != null) {
            lblProductoResumen.setText(producto.getNombre() + " · " + formatearCodigo(producto.getIdProducto()));
        }

        int stock = producto.getStockActual();
        boolean tieneExistencias = stock > 0;
        if (boxAvisoStock != null) {
            boxAvisoStock.setVisible(tieneExistencias);
            boxAvisoStock.setManaged(tieneExistencias);
        }
        if (lblAvisoStock != null && tieneExistencias) {
            lblAvisoStock.setText("Todavía " + (stock == 1 ? "queda 1 unidad" : "quedan " + stock + " unidades")
                    + " en el inventario.");
        }
    }

    public boolean isBajaConfirmada() {
        return bajaConfirmada;
    }

    @FXML
    public void handleConfirmarBaja() {
        if (productoActual == null) {
            cerrarVentana();
            return;
        }

        try {
            // Regla estricta de integridad: borrado logico (activo = false), nunca DELETE
            boolean exito = productoDAO.cambiarEstado(productoActual.getIdProducto(), false);
            if (exito) {
                ReactivarProductoController.registrarFechaBaja(productoActual.getIdProducto());
                this.bajaConfirmada = true;
                cerrarVentana();
            } else {
                AlertaUtil.mostrarError("No se pudo dar de baja",
                        "La base de datos no confirmó el cambio de estado del producto.");
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "Ocurrió un problema al dar de baja el producto en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        this.bajaConfirmada = false;
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
}
