package com.conary.rosita.controller;

import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.model.Producto;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Controlador del Modal A: Ajustar existencias (Modulo 4 — Productos y precios).
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class AjustarExistenciasController {

    @FXML private Label lblProductoResumen;
    @FXML private Label lblStockSistema;
    @FXML private TextField txtCantidadReal;

    @FXML private HBox boxResumenDiferencia;
    @FXML private Label lblIconoDiferencia;
    @FXML private Label lblTextoDiferencia;

    @FXML private ToggleButton btnMotivoInventario;
    @FXML private ToggleButton btnMotivoDanado;
    @FXML private ToggleButton btnMotivoPerdido;
    @FXML private ToggleButton btnMotivoOtro;

    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final ToggleGroup tgMotivo = new ToggleGroup();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private List<ToggleButton> botonesMotivo;
    private Producto productoActual;
    private boolean ajusteGuardado = false;

    @FXML
    public void initialize() {
        configurarSelectorMotivos();

        if (txtCantidadReal != null) {
            txtCantidadReal.textProperty().addListener((obs, oldVal, newVal) -> recalcularDiferencia());
        }
    }

    public void setProducto(Producto producto) {
        if (producto == null) {
            return;
        }
        this.productoActual = producto;
        int stockActual = producto.getStockActual();

        if (lblProductoResumen != null) {
            lblProductoResumen.setText(producto.getNombre() + " · " + formatearCodigo(producto.getIdProducto()));
        }
        if (lblStockSistema != null) {
            lblStockSistema.setText(stockActual + (stockActual == 1 ? " unidad" : " unidades"));
        }
        if (txtCantidadReal != null) {
            txtCantidadReal.setText(String.valueOf(stockActual));
            Platform.runLater(() -> {
                txtCantidadReal.requestFocus();
                txtCantidadReal.selectAll();
            });
        }
        recalcularDiferencia();
    }

    public boolean isAjusteGuardado() {
        return ajusteGuardado;
    }

    private void configurarSelectorMotivos() {
        botonesMotivo = Arrays.asList(
                btnMotivoInventario,
                btnMotivoDanado,
                btnMotivoPerdido,
                btnMotivoOtro
        );

        if (btnMotivoInventario != null) btnMotivoInventario.setUserData("Conté el inventario");
        if (btnMotivoDanado != null) btnMotivoDanado.setUserData("Se dañó");
        if (btnMotivoPerdido != null) btnMotivoPerdido.setUserData("Se perdió");
        if (btnMotivoOtro != null) btnMotivoOtro.setUserData("Otro motivo");

        for (ToggleButton btn : botonesMotivo) {
            if (btn != null) {
                btn.setToggleGroup(tgMotivo);
            }
        }
        if (btnMotivoInventario != null) {
            btnMotivoInventario.setSelected(true);
        }

        tgMotivo.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
                return;
            }
            actualizarTextosMotivos();
        });
        actualizarTextosMotivos();
    }

    private void actualizarTextosMotivos() {
        if (botonesMotivo == null) return;
        for (ToggleButton btn : botonesMotivo) {
            if (btn == null || btn.getUserData() == null) continue;
            String base = String.valueOf(btn.getUserData());
            if (btn.isSelected()) {
                btn.setText("\u2713   " + base);
            } else {
                btn.setText("\u2022   " + base);
            }
        }
    }

    private String obtenerMotivoSeleccionado() {
        if (tgMotivo.getSelectedToggle() != null) {
            Toggle t = tgMotivo.getSelectedToggle();
            if (t.getUserData() != null) {
                return String.valueOf(t.getUserData());
            }
        }
        return null;
    }

    private void recalcularDiferencia() {
        if (productoActual == null || lblTextoDiferencia == null || lblIconoDiferencia == null) {
            return;
        }
        int stockActual = productoActual.getStockActual();
        String raw = txtCantidadReal != null && txtCantidadReal.getText() != null
                ? txtCantidadReal.getText().trim()
                : "";

        Integer cantidadRealObj = parsearEntero(raw);
        if (cantidadRealObj == null || cantidadRealObj.intValue() < 0) {
            lblIconoDiferencia.setText("\u26A0");
            lblTextoDiferencia.setText("Escriba un número entero de unidades mayor o igual a 0.");
            return;
        }

        int cantidadReal = cantidadRealObj.intValue();
        int diferencia = cantidadReal - stockActual;

        if (diferencia > 0) {
            lblIconoDiferencia.setText("\u26A0");
            lblTextoDiferencia.setText("Se sumarán " + diferencia
                    + (diferencia == 1 ? " unidad. Quedarán " : " unidades. Quedarán ")
                    + cantidadReal + ".");
        } else if (diferencia < 0) {
            int restar = Math.abs(diferencia);
            lblIconoDiferencia.setText("\u26A0");
            lblTextoDiferencia.setText("Se restarán " + restar
                    + (restar == 1 ? " unidad. Quedarán " : " unidades. Quedarán ")
                    + cantidadReal + ".");
        } else {
            lblIconoDiferencia.setText("\u2713");
            lblTextoDiferencia.setText("El conteo coincide con el sistema (" + cantidadReal
                    + (cantidadReal == 1 ? " unidad)." : " unidades)."));
        }
    }

    @FXML
    public void handleGuardarAjuste() {
        if (productoActual == null) {
            cerrarVentana();
            return;
        }

        String raw = txtCantidadReal != null && txtCantidadReal.getText() != null
                ? txtCantidadReal.getText().trim()
                : "";
        Integer cantidadRealObj = parsearEntero(raw);
        if (cantidadRealObj == null || cantidadRealObj.intValue() < 0) {
            AlertaUtil.mostrarAdvertencia("Cantidad inválida",
                    "Por favor escriba un número entero válido mayor o igual a 0.");
            if (txtCantidadReal != null) txtCantidadReal.requestFocus();
            return;
        }

        String motivo = obtenerMotivoSeleccionado();
        if (motivo == null || motivo.trim().isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Seleccione un motivo",
                    "Por favor elija por qué cambia la cantidad en inventario.");
            return;
        }

        int nuevaCantidad = cantidadRealObj.intValue();
        try {
            Producto enBD = productoDAO.obtenerPorId(productoActual.getIdProducto());
            Producto base = enBD != null ? enBD : productoActual;

            Producto actualizado = new Producto(
                    base.getIdProducto(),
                    base.getNombre(),
                    base.getCategoria(),
                    base.getPrecioCompra(),
                    base.getPrecioVenta(),
                    nuevaCantidad,
                    base.getStockMinimo(),
                    base.isActivo()
            );

            boolean exito = productoDAO.actualizar(actualizado);
            if (exito) {
                Usuario u = SesionUsuario.getUsuarioLogueado();
                String responsable = u != null ? u.getNombre() : "Administradora";
                System.out.println("[AjustarExistencias] Producto " + formatearCodigo(base.getIdProducto())
                        + " ajustado de " + base.getStockActual() + " a " + nuevaCantidad
                        + " unidades. Motivo: " + motivo + " | Usuario: " + responsable);
                this.ajusteGuardado = true;
                cerrarVentana();
            } else {
                AlertaUtil.mostrarError("No se pudo guardar",
                        "La base de datos no confirmó la actualización de existencias.");
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "Ocurrió un error al actualizar las existencias en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        this.ajusteGuardado = false;
        cerrarVentana();
    }

    private void cerrarVentana() {
        if (btnCancelar != null && btnCancelar.getScene() != null && btnCancelar.getScene().getWindow() instanceof Stage) {
            ((Stage) btnCancelar.getScene().getWindow()).close();
        }
    }

    private Integer parsearEntero(String texto) {
        if (texto == null) return null;
        String limpio = texto.replace(".", "").replace(" ", "").trim();
        if (limpio.isEmpty()) return null;
        try {
            return Integer.valueOf(Integer.parseInt(limpio));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatearCodigo(int idProducto) {
        return String.format("PRD-%03d", Integer.valueOf(idProducto));
    }
}
