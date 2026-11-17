package com.conary.rosita.controller;

import com.conary.rosita.dao.ProveedorDAO;
import com.conary.rosita.model.Proveedor;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista B de la Tanda 1 del Modulo 5: Registrar proveedor.
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class RegistrarProveedorController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverProveedores;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;

    @FXML private HBox boxAlertaDuplicado;
    @FXML private Label lblTextoDuplicado;

    @FXML private HBox boxInputNombre;
    @FXML private TextField txtNombreProveedor;
    @FXML private Label lblAyudaNombre;

    @FXML private HBox boxInputTelefono;
    @FXML private TextField txtTelefonoProveedor;

    @FXML private HBox boxInputProductos;
    @FXML private TextField txtProductosProveedor;

    @FXML private Button btnCancelar;
    @FXML private Button btnGuardarProveedor;

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final List<Proveedor> proveedoresExistentes = new ArrayList<Proveedor>();

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        cargarProveedoresExistentes();
        configurarListenersValidacion();

        Platform.runLater(() -> {
            if (txtNombreProveedor != null) {
                txtNombreProveedor.requestFocus();
            }
        });
    }

    private void configurarEncabezadoUsuario() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        String nombre = "Doña Rosita";
        String rol = "Administradora";

        if (u != null) {
            String raw = u.getNombre() != null ? u.getNombre().trim() : "Usuario";
            if (raw.toLowerCase(LOCALE_ES_CO).contains("rosita")
                    && !raw.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
                nombre = "Doña " + raw;
            } else {
                nombre = raw;
            }
            rol = u.esAdministrador() ? "Administradora" : "Vendedora";
        }

        if (lblUsuarioNombre != null) {
            lblUsuarioNombre.setText(nombre);
        }
        if (lblUsuarioRol != null) {
            lblUsuarioRol.setText(rol);
        }
    }

    private void cargarProveedoresExistentes() {
        proveedoresExistentes.clear();
        try {
            List<Proveedor> desdeBD = proveedorDAO.listarTodos(false);
            if (desdeBD != null) {
                proveedoresExistentes.addAll(desdeBD);
            }
        } catch (SQLException e) {
            System.err.println("[RegistrarProveedorController] Error al cargar proveedores existentes: " + e.getMessage());
        }
    }

    private void configurarListenersValidacion() {
        if (txtNombreProveedor != null) {
            txtNombreProveedor.textProperty().addListener((obs, oldVal, newVal) -> {
                if (boxInputNombre != null) {
                    boxInputNombre.getStyleClass().removeAll("reg-proveedor-input-box-error");
                }
                if (lblAyudaNombre != null) {
                    lblAyudaNombre.setText("ⓘ  Como lo conoce usted");
                    lblAyudaNombre.getStyleClass().removeAll("reg-cliente-field-help-error");
                }
                verificarDuplicadoEnVivo();
            });

            txtNombreProveedor.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER && txtTelefonoProveedor != null) {
                    txtTelefonoProveedor.requestFocus();
                }
            });
        }

        if (txtTelefonoProveedor != null) {
            txtTelefonoProveedor.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER && txtProductosProveedor != null) {
                    txtProductosProveedor.requestFocus();
                }
            });
        }

        if (txtProductosProveedor != null) {
            txtProductosProveedor.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    handleGuardarProveedor();
                }
            });
        }
    }

    private Proveedor buscarProveedorDuplicado(String nombreInput) {
        String nomNorm = normalizarTexto(nombreInput);
        if (nomNorm.isEmpty()) {
            return null;
        }
        for (Proveedor p : proveedoresExistentes) {
            if (nomNorm.equals(normalizarTexto(p.getNombre()))) {
                return p;
            }
        }
        return null;
    }

    private void verificarDuplicadoEnVivo() {
        String nom = txtNombreProveedor != null ? txtNombreProveedor.getText() : "";
        Proveedor dup = buscarProveedorDuplicado(nom);

        if (dup != null) {
            mostrarBannerDuplicado(dup);
        } else if (boxAlertaDuplicado != null) {
            boxAlertaDuplicado.setVisible(false);
            boxAlertaDuplicado.setManaged(false);
        }
    }

    private void mostrarBannerDuplicado(Proveedor existente) {
        if (boxAlertaDuplicado == null || lblTextoDuplicado == null || existente == null) {
            return;
        }
        lblTextoDuplicado.setText("Ya hay un proveedor con este nombre: "
                + existente.getNombre()
                + ". Cambie el nombre o vuelva a la lista.");
        boxAlertaDuplicado.setVisible(true);
        boxAlertaDuplicado.setManaged(true);
    }

    @FXML
    public void handleGuardarProveedor() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede registrar proveedores.");
            return;
        }

        String nombre = txtNombreProveedor != null && txtNombreProveedor.getText() != null
                ? txtNombreProveedor.getText().trim()
                : "";
        String telefono = txtTelefonoProveedor != null && txtTelefonoProveedor.getText() != null
                ? txtTelefonoProveedor.getText().trim()
                : "";
        String productos = txtProductosProveedor != null && txtProductosProveedor.getText() != null
                ? txtProductosProveedor.getText().trim()
                : "";

        if (nombre.isEmpty()) {
            if (boxInputNombre != null && !boxInputNombre.getStyleClass().contains("reg-proveedor-input-box-error")) {
                boxInputNombre.getStyleClass().add("reg-proveedor-input-box-error");
            }
            if (lblAyudaNombre != null) {
                lblAyudaNombre.setText("⚠  Escriba el nombre del proveedor.");
                if (!lblAyudaNombre.getStyleClass().contains("reg-cliente-field-help-error")) {
                    lblAyudaNombre.getStyleClass().add("reg-cliente-field-help-error");
                }
            }
            if (txtNombreProveedor != null) {
                txtNombreProveedor.requestFocus();
            }
            return;
        }

        cargarProveedoresExistentes();
        Proveedor duplicado = buscarProveedorDuplicado(nombre);
        if (duplicado != null) {
            mostrarBannerDuplicado(duplicado);
            if (txtNombreProveedor != null) {
                txtNombreProveedor.requestFocus();
            }
            return;
        }

        Proveedor nuevo = new Proveedor();
        nuevo.setNombre(nombre);
        nuevo.setTelefono(telefono.isEmpty() ? null : telefono);
        nuevo.setDireccion(null);
        nuevo.setProductos(productos.isEmpty() ? null : productos);
        nuevo.setActivo(true);
        nuevo.setSaldoPendiente(BigDecimal.ZERO);

        try {
            if (proveedorDAO.insertar(nuevo)) {
                ProveedoresController.setIdProveedorSeleccionarAlCargar(nuevo.getIdProveedor());
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                    menu.cargarVistaEnContenido("/fxml/proveedores.fxml");
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo guardar el proveedor en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleVolverAProveedores() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/proveedores.fxml");
        }
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.trim().toLowerCase(LOCALE_ES_CO).replaceAll("\\s+", " ");
    }
}