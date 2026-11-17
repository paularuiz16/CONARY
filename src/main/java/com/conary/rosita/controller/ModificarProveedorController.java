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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista B de la Tanda 3 del Modulo 5: Modificar datos y Dar de baja proveedor.
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ModificarProveedorController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static Proveedor proveedorPendienteEdicion;

    public static void setProveedorPendienteEdicion(Proveedor proveedor) {
        proveedorPendienteEdicion = proveedor;
    }

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

    @FXML private Label lblInfoDeudaActual;
    @FXML private Button btnDarBajaProveedor;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardarCambios;

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final List<Proveedor> todosLosProveedores = new ArrayList<Proveedor>();
    private Proveedor proveedorActual;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        cargarCatalogoProveedores();
        configurarListenersValidacion();

        if (proveedorPendienteEdicion != null) {
            setProveedor(proveedorPendienteEdicion);
        }

        Platform.runLater(() -> {
            if (txtNombreProveedor != null) {
                txtNombreProveedor.requestFocus();
                String t = txtNombreProveedor.getText();
                txtNombreProveedor.positionCaret(t != null ? t.length() : 0);
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

        if (lblUsuarioNombre != null) lblUsuarioNombre.setText(nombre);
        if (lblUsuarioRol != null) lblUsuarioRol.setText(rol);
    }

    private void cargarCatalogoProveedores() {
        todosLosProveedores.clear();
        try {
            List<Proveedor> desdeBD = proveedorDAO.listarTodos(false);
            if (desdeBD != null) {
                todosLosProveedores.addAll(desdeBD);
            }
        } catch (SQLException e) {
            System.err.println("[ModificarProveedorController] Error cargando proveedores: " + e.getMessage());
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
                    handleGuardarCambios();
                }
            });
        }
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedorActual = proveedor;
        if (proveedor == null) {
            return;
        }

        try {
            Proveedor fresco = proveedorDAO.obtenerPorId(proveedor.getIdProveedor());
            if (fresco != null) {
                this.proveedorActual = fresco;
            }
        } catch (SQLException ignored) {
        }

        if (txtNombreProveedor != null) {
            txtNombreProveedor.setText(proveedorActual.getNombre() != null ? proveedorActual.getNombre() : "");
        }
        if (txtTelefonoProveedor != null) {
            txtTelefonoProveedor.setText(proveedorActual.getTelefono() != null ? proveedorActual.getTelefono() : "");
        }
        if (txtProductosProveedor != null) {
            txtProductosProveedor.setText(proveedorActual.getProductos() != null ? proveedorActual.getProductos() : "");
        }

        BigDecimal deuda = proveedorActual.getSaldoPendiente() != null
                ? proveedorActual.getSaldoPendiente()
                : BigDecimal.ZERO;
        if (lblInfoDeudaActual != null) {
            if (deuda.compareTo(BigDecimal.ZERO) > 0) {
                lblInfoDeudaActual.setText("Saldo pendiente actual: $ " + formatearMoneda(deuda)
                        + ". Para darle de baja primero debe saldar esta deuda.");
            } else {
                lblInfoDeudaActual.setText("Saldo pendiente actual: $ 0 (al día con este proveedor).");
            }
        }

        if (btnDarBajaProveedor != null) {
            boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
            btnDarBajaProveedor.setVisible(esAdmin);
            btnDarBajaProveedor.setManaged(esAdmin);
            if (!proveedorActual.isActivo()) {
                btnDarBajaProveedor.setText("↻   Volver a activar este proveedor");
            } else {
                btnDarBajaProveedor.setText("📦   Dar de baja a este proveedor");
            }
        }

        if (boxAlertaDuplicado != null) {
            boxAlertaDuplicado.setVisible(false);
            boxAlertaDuplicado.setManaged(false);
        }
    }

    private Proveedor buscarOtroProveedorDuplicado(String nombreInput) {
        if (proveedorActual == null) {
            return null;
        }
        String nomNorm = normalizarTexto(nombreInput);
        if (nomNorm.isEmpty()) {
            return null;
        }
        for (Proveedor p : todosLosProveedores) {
            if (p.getIdProveedor() == proveedorActual.getIdProveedor()) {
                continue;
            }
            if (nomNorm.equals(normalizarTexto(p.getNombre()))) {
                return p;
            }
        }
        return null;
    }

    private void verificarDuplicadoEnVivo() {
        String nom = txtNombreProveedor != null ? txtNombreProveedor.getText() : "";
        Proveedor dup = buscarOtroProveedorDuplicado(nom);

        if (dup != null) {
            mostrarBannerDuplicado(dup);
        } else if (boxAlertaDuplicado != null) {
            boxAlertaDuplicado.setVisible(false);
            boxAlertaDuplicado.setManaged(false);
        }
    }

    private void mostrarBannerDuplicado(Proveedor otro) {
        if (boxAlertaDuplicado == null || lblTextoDuplicado == null || otro == null) {
            return;
        }
        lblTextoDuplicado.setText("Ya hay otro proveedor con este nombre: "
                + otro.getNombre()
                + ". Cambie el nombre o vuelva a la lista.");
        boxAlertaDuplicado.setVisible(true);
        boxAlertaDuplicado.setManaged(true);
    }

    @FXML
    public void handleGuardarCambios() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede modificar proveedores.");
            return;
        }
        if (proveedorActual == null) {
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

        cargarCatalogoProveedores();
        Proveedor duplicado = buscarOtroProveedorDuplicado(nombre);
        if (duplicado != null) {
            mostrarBannerDuplicado(duplicado);
            if (txtNombreProveedor != null) {
                txtNombreProveedor.requestFocus();
            }
            return;
        }

        proveedorActual.setNombre(nombre);
        proveedorActual.setTelefono(telefono.isEmpty() ? null : telefono);
        proveedorActual.setProductos(productos.isEmpty() ? null : productos);

        try {
            if (proveedorDAO.actualizar(proveedorActual)) {
                proveedorPendienteEdicion = null;
                ProveedoresController.setIdProveedorSeleccionarAlCargar(proveedorActual.getIdProveedor());
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                    menu.cargarVistaEnContenido("/fxml/proveedores.fxml");
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudieron guardar los cambios del proveedor en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleDarBajaProveedor() {
        if (proveedorActual == null) {
            return;
        }
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede dar de baja o reactivar proveedores.");
            return;
        }

        if (!proveedorActual.isActivo()) {
            boolean confirmarReactivar = AlertaUtil.mostrarConfirmacion(
                    "Volver a activar proveedor",
                    "¿Desea volver a activar a «" + proveedorActual.getNombre() + "» en el directorio?");
            if (!confirmarReactivar) {
                return;
            }
            try {
                if (proveedorDAO.cambiarEstado(proveedorActual.getIdProveedor(), true)) {
                    proveedorPendienteEdicion = null;
                    ProveedoresController.setIdProveedorSeleccionarAlCargar(proveedorActual.getIdProveedor());
                    MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                    if (menu != null) {
                        menu.refrescarAlertasReales();
                        menu.cargarVistaEnContenido("/fxml/proveedores.fxml");
                    }
                }
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Error de Base de Datos",
                        "No se pudo reactivar al proveedor en MySQL: " + e.getMessage());
            }
            return;
        }

        BigDecimal deudaActual = proveedorActual.getSaldoPendiente() != null
                ? proveedorActual.getSaldoPendiente()
                : BigDecimal.ZERO;
        if (deudaActual.compareTo(BigDecimal.ZERO) > 0) {
            AlertaUtil.mostrarAdvertencia(
                    "No se puede dar de baja todavía",
                    "Todavía se le deben $ " + formatearMoneda(deudaActual)
                            + " a «" + proveedorActual.getNombre()
                            + "».\n\nPrimero debe saldar la deuda con el proveedor antes de darle de baja.");
            return;
        }

        boolean confirmarBaja = AlertaUtil.mostrarConfirmacion(
                "¿Dar de baja a este proveedor?",
                "¿Está segura de que desea archivar a «" + proveedorActual.getNombre()
                        + "»?\n\nSu historial de compras y pagos se conservará y podrá volver a activarlo cuando lo necesite.");
        if (!confirmarBaja) {
            return;
        }

        try {
            if (proveedorDAO.cambiarEstado(proveedorActual.getIdProveedor(), false)) {
                proveedorPendienteEdicion = null;
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                    menu.cargarVistaEnContenido("/fxml/proveedores.fxml");
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo dar de baja al proveedor en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleVolverAProveedores() {
        if (proveedorActual != null) {
            ProveedoresController.setIdProveedorSeleccionarAlCargar(proveedorActual.getIdProveedor());
        }
        proveedorPendienteEdicion = null;
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/proveedores.fxml");
        }
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols).format(monto);
    }

    private String normalizarTexto(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(LOCALE_ES_CO).replaceAll("\\s+", " ");
    }
}