package com.conary.rosita.controller;

import com.conary.rosita.dao.UsuarioDAO;
import com.conary.rosita.model.RolUsuario;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

import java.sql.SQLException;
import java.util.Locale;

/**
 * Controlador de la vista "Registrar usuario" (Paso 2B de Mas opciones).
 * Permite crear un nuevo usuario activo (Vendedora o Administradora) en MySQL
 * con validaciones amigables y clave cifrada mediante SHA-256.
 */
public class RegistrarUsuarioController {

    private static final String MENSAJE_DEFECTO =
            "Recuerde verificar que las dos claves coincidan antes de guardar el usuario.";

    private static final String SVG_EYE_OPEN =
            "M 10 2 C 5.45 2 1.57 4.83 0 8.8 C 1.57 12.77 5.45 15.6 10 15.6 "
                    + "C 14.55 15.6 18.43 12.77 20 8.8 C 18.43 4.83 14.55 2 10 2 Z "
                    + "M 10 13.3 C 7.52 13.3 5.5 11.28 5.5 8.8 C 5.5 6.32 7.52 4.3 10 4.3 "
                    + "C 12.48 4.3 14.5 6.32 14.5 8.8 C 14.5 11.28 12.48 13.3 10 13.3 Z "
                    + "M 10 6.1 C 8.51 6.1 7.3 7.31 7.3 8.8 C 7.3 10.29 8.51 11.5 10 11.5 "
                    + "C 11.49 11.5 12.7 10.29 12.7 8.8 C 12.7 7.31 11.49 6.1 10 6.1 Z";

    private static final String SVG_EYE_CLOSED =
            "M 10 4.3 C 12.48 4.3 14.5 6.32 14.5 8.8 C 14.5 9.38 14.38 9.93 14.18 10.44 "
                    + "L 16.82 13.08 C 18.18 11.94 19.29 10.48 20 8.8 C 18.43 4.83 14.55 2 10 2 "
                    + "C 8.73 2 7.52 2.22 6.4 2.63 L 8.36 4.59 C 8.88 4.41 9.43 4.3 10 4.3 Z "
                    + "M 1.8 1.5 L 0.5 2.8 L 3.46 5.76 C 1.98 6.84 0.78 8.25 0 8.8 "
                    + "C 1.57 12.77 5.45 15.6 10 15.6 C 11.52 15.6 12.97 15.29 14.28 14.74 "
                    + "L 17.2 17.66 L 18.5 16.36 L 1.8 1.5 Z "
                    + "M 5.5 8.8 C 5.5 8.05 5.69 7.35 6.02 6.73 L 7.45 8.16 "
                    + "C 7.36 8.36 7.3 8.57 7.3 8.8 C 7.3 10.29 8.51 11.5 10 11.5 "
                    + "C 10.23 11.5 10.44 11.44 10.64 11.35 L 12.07 12.78 "
                    + "C 11.45 13.11 10.75 13.3 10 13.3 C 7.52 13.3 5.5 11.28 5.5 8.8 Z";

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverUsuarios;
    @FXML private Label lblUsuarioHeader;

    @FXML private HBox boxAlertaUsuario;
    @FXML private SVGPath svgAlertaUsuario;
    @FXML private Label lblAlertaUsuario;

    @FXML private TextField txtNombreCompleto;
    @FXML private TextField txtNombreUsuario;

    @FXML private HBox boxClave;
    @FXML private PasswordField txtClave;
    @FXML private TextField txtClaveVisible;
    @FXML private Button btnVerClave;
    @FXML private SVGPath svgOjoClave;

    @FXML private HBox boxRepetirClave;
    @FXML private PasswordField txtRepetirClave;
    @FXML private TextField txtRepetirClaveVisible;
    @FXML private Button btnVerRepetirClave;
    @FXML private SVGPath svgOjoRepetirClave;

    @FXML private ToggleButton tbRolVendedora;
    @FXML private ToggleButton tbRolAdministradora;

    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final ToggleGroup rolToggleGroup = new ToggleGroup();

    private boolean claveVisible = false;
    private boolean repetirClaveVisible = false;

    @FXML
    public void initialize() {
        cargarCabeceraUsuario();

        if (!SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso restringido",
                    "Solo la administradora puede registrar nuevos usuarios.");
            navegarA("/fxml/menu_principal.fxml");
            return;
        }

        configurarRoles();
        configurarCampos();
        restaurarBannerPorDefecto();
    }

    private void cargarCabeceraUsuario() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        if (u != null && lblUsuarioHeader != null) {
            String rolTexto = u.esAdministrador() ? "Administradora" : "Vendedora";
            lblUsuarioHeader.setText(u.getNombre() + " · " + rolTexto);
        }
    }

    private void configurarRoles() {
        tbRolVendedora.setToggleGroup(rolToggleGroup);
        tbRolAdministradora.setToggleGroup(rolToggleGroup);
        tbRolVendedora.setSelected(true);

        rolToggleGroup.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                if (anterior != null) {
                    anterior.setSelected(true);
                } else {
                    tbRolVendedora.setSelected(true);
                }
            }
        });
    }

    private void configurarCampos() {
        // Fuerza minusculas y elimina espacios automaticamente en "Nombre para entrar"
        txtNombreUsuario.textProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo != null) {
                String normalizado = nuevo.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
                if (!normalizado.equals(nuevo)) {
                    txtNombreUsuario.setText(normalizado);
                }
            }
            restaurarBannerPorDefecto();
        });

        txtNombreCompleto.textProperty().addListener((obs, o, n) -> restaurarBannerPorDefecto());

        // Vinculacion bidireccional entre PasswordField y TextField visible
        txtClaveVisible.textProperty().bindBidirectional(txtClave.textProperty());
        txtRepetirClaveVisible.textProperty().bindBidirectional(txtRepetirClave.textProperty());

        txtClave.textProperty().addListener((obs, o, n) -> restaurarBannerPorDefecto());
        txtRepetirClave.textProperty().addListener((obs, o, n) -> restaurarBannerPorDefecto());

        configurarFocoCaja(boxClave, txtClave, txtClaveVisible);
        configurarFocoCaja(boxRepetirClave, txtRepetirClave, txtRepetirClaveVisible);
    }

    private void configurarFocoCaja(HBox caja, PasswordField pf, TextField tf) {
        pf.focusedProperty().addListener((obs, o, focused) -> actualizarClaseFoco(caja, focused || tf.isFocused()));
        tf.focusedProperty().addListener((obs, o, focused) -> actualizarClaseFoco(caja, focused || pf.isFocused()));
    }

    private void actualizarClaseFoco(HBox caja, boolean focused) {
        if (focused) {
            if (!caja.getStyleClass().contains("reg-usuario-password-box-focused")) {
                caja.getStyleClass().add("reg-usuario-password-box-focused");
            }
        } else {
            caja.getStyleClass().remove("reg-usuario-password-box-focused");
        }
    }

    @FXML
    public void handleToggleVerClave() {
        claveVisible = !claveVisible;
        alternarVisibilidadPassword(txtClave, txtClaveVisible, svgOjoClave, claveVisible);
    }

    @FXML
    public void handleToggleVerRepetirClave() {
        repetirClaveVisible = !repetirClaveVisible;
        alternarVisibilidadPassword(txtRepetirClave, txtRepetirClaveVisible, svgOjoRepetirClave, repetirClaveVisible);
    }

    private void alternarVisibilidadPassword(PasswordField pf, TextField tf, SVGPath icono, boolean mostrar) {
        tf.setVisible(mostrar);
        tf.setManaged(mostrar);
        pf.setVisible(!mostrar);
        pf.setManaged(!mostrar);
        icono.setContent(mostrar ? SVG_EYE_CLOSED : SVG_EYE_OPEN);

        if (mostrar) {
            tf.requestFocus();
            tf.positionCaret(tf.getText() != null ? tf.getText().length() : 0);
        } else {
            pf.requestFocus();
            pf.positionCaret(pf.getText() != null ? pf.getText().length() : 0);
        }
    }

    @FXML
    public void handleCancelar() {
        navegarA("/fxml/usuarios.fxml");
    }

    @FXML
    public void handleGuardarUsuario() {
        String nombreCompleto = txtNombreCompleto.getText() != null ? txtNombreCompleto.getText().trim() : "";
        String username = txtNombreUsuario.getText() != null
                ? txtNombreUsuario.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        String clave = txtClave.getText() != null ? txtClave.getText() : "";
        String repetirClave = txtRepetirClave.getText() != null ? txtRepetirClave.getText() : "";

        if (nombreCompleto.isEmpty()) {
            mostrarErrorBanner("Escriba el nombre completo de la persona antes de guardar.");
            txtNombreCompleto.requestFocus();
            return;
        }

        if (username.isEmpty()) {
            mostrarErrorBanner("Escriba el nombre para entrar al sistema (sin espacios).");
            txtNombreUsuario.requestFocus();
            return;
        }

        if (username.contains(" ")) {
            mostrarErrorBanner("El nombre para entrar no puede llevar espacios.");
            txtNombreUsuario.requestFocus();
            return;
        }

        if (clave.length() < 6) {
            mostrarErrorBanner("La clave debe tener mínimo 6 caracteres.");
            enfocarCampoClave(claveVisible, txtClaveVisible, txtClave);
            return;
        }

        if (!clave.equals(repetirClave)) {
            mostrarErrorBanner("Las dos claves no coinciden. Revíselas antes de guardar.");
            enfocarCampoClave(repetirClaveVisible, txtRepetirClaveVisible, txtRepetirClave);
            return;
        }

        try {
            if (usuarioDAO.existeUsername(username)) {
                mostrarErrorBanner("Ya existe una cuenta con el usuario «" + username + "». Elija otro nombre para entrar.");
                txtNombreUsuario.requestFocus();
                return;
            }

            RolUsuario rolSeleccionado = tbRolAdministradora.isSelected()
                    ? RolUsuario.ADMINISTRADOR
                    : RolUsuario.VENDEDOR;

            Usuario nuevo = new Usuario();
            nuevo.setNombre(nombreCompleto);
            nuevo.setUsername(username);
            nuevo.setRol(rolSeleccionado);
            nuevo.setActivo(true);

            usuarioDAO.insertar(nuevo, clave);
            UsuariosController.setIdUsuarioSeleccionarAlCargar(nuevo.getIdUsuario());

            AlertaUtil.mostrarInfo("Usuario registrado",
                    "La cuenta de «" + nombreCompleto + "» (" + username + ") se guardó correctamente.");
            navegarA("/fxml/usuarios.fxml");

        } catch (SQLException e) {
            mostrarErrorBanner("No se pudo guardar el usuario en la base de datos: " + e.getMessage());
        }
    }

    private void enfocarCampoClave(boolean visible, TextField tf, PasswordField pf) {
        if (visible) {
            tf.requestFocus();
        } else {
            pf.requestFocus();
        }
    }

    private void restaurarBannerPorDefecto() {
        if (boxAlertaUsuario == null || lblAlertaUsuario == null) {
            return;
        }
        boxAlertaUsuario.getStyleClass().remove("reg-usuario-alerta-error");
        if (!boxAlertaUsuario.getStyleClass().contains("reg-usuario-alerta")) {
            boxAlertaUsuario.getStyleClass().add("reg-usuario-alerta");
        }
        if (svgAlertaUsuario != null) {
            svgAlertaUsuario.setStyle("-fx-fill: #704214;");
        }
        lblAlertaUsuario.setText(MENSAJE_DEFECTO);
    }

    private void mostrarErrorBanner(String mensaje) {
        boxAlertaUsuario.getStyleClass().remove("reg-usuario-alerta");
        if (!boxAlertaUsuario.getStyleClass().contains("reg-usuario-alerta-error")) {
            boxAlertaUsuario.getStyleClass().add("reg-usuario-alerta-error");
        }
        if (svgAlertaUsuario != null) {
            svgAlertaUsuario.setStyle("-fx-fill: #9A342B;");
        }
        lblAlertaUsuario.setText(mensaje);
    }

    private void navegarA(String fxmlPath) {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido(fxmlPath);
        }
    }
}
