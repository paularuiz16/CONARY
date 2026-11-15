package com.conary.rosita.controller;

import com.conary.rosita.App;
import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.UsuarioDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

import java.sql.SQLException;

/**
 * Controlador de la pantalla de inicio de sesion.
 */
public class LoginController {

    private static final String TEXTO_BTN_INGRESAR = "INGRESAR AL SISTEMA   \u2794";
    private static final String SVG_EYE_OPEN = "M 10 2 C 5.45 2 1.57 4.83 0 8.8 C 1.57 12.77 5.45 15.6 10 15.6 C 14.55 15.6 18.43 12.77 20 8.8 C 18.43 4.83 14.55 2 10 2 Z M 10 13.3 C 7.52 13.3 5.5 11.28 5.5 8.8 C 5.5 6.32 7.52 4.3 10 4.3 C 12.48 4.3 14.5 6.32 14.5 8.8 C 14.5 11.28 12.48 13.3 10 13.3 Z M 10 6.1 C 8.51 6.1 7.3 7.31 7.3 8.8 C 7.3 10.29 8.51 11.5 10 11.5 C 11.49 11.5 12.7 10.29 12.7 8.8 C 12.7 7.31 11.49 6.1 10 6.1 Z";
    private static final String SVG_EYE_CLOSED = "M 10 4.3 C 12.48 4.3 14.5 6.32 14.5 8.8 C 14.5 9.38 14.38 9.93 14.18 10.44 L 16.82 13.08 C 18.18 11.94 19.29 10.48 20 8.8 C 18.43 4.83 14.55 2 10 2 C 8.73 2 7.52 2.22 6.4 2.63 L 8.36 4.59 C 8.88 4.41 9.43 4.3 10 4.3 Z M 1.8 1.5 L 0.5 2.8 L 3.46 5.76 C 1.98 6.84 0.78 8.25 0 8.8 C 1.57 12.77 5.45 15.6 10 15.6 C 11.52 15.6 12.97 15.29 14.28 14.74 L 17.2 17.66 L 18.5 16.36 L 1.8 1.5 Z M 5.5 8.8 C 5.5 8.05 5.69 7.35 6.02 6.73 L 7.45 8.16 C 7.36 8.36 7.3 8.57 7.3 8.8 C 7.3 10.29 8.51 11.5 10 11.5 C 10.23 11.5 10.44 11.44 10.64 11.35 L 12.07 12.78 C 11.45 13.11 10.75 13.3 10 13.3 C 7.52 13.3 5.5 11.28 5.5 8.8 Z";

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private TextField txtPasswordVisible;
    @FXML
    private Button btnTogglePassword;
    @FXML
    private StackPane boxPasswordContainer;
    @FXML
    private HBox boxError;
    @FXML
    private SVGPath svgEye;
    @FXML
    private Button btnIngresar;
    @FXML
    private Label lblMensaje;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final CajaDAO cajaDAO = new CajaDAO();
    private boolean mostrandoPassword = false;

    @FXML
    public void initialize() {
        if (txtPasswordVisible != null) {
            txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
            txtPasswordVisible.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    handleLogin();
                }
            });
        }

        if (boxPasswordContainer != null) {
            txtPassword.focusedProperty().addListener((obs, oldVal, newVal) -> actualizarFocoContenedorPassword());
            if (txtPasswordVisible != null) {
                txtPasswordVisible.focusedProperty()
                        .addListener((obs, oldVal, newVal) -> actualizarFocoContenedorPassword());
            }
        }

        // Permitir pulsar Enter en cualquier campo para iniciar sesion
        txtUsername.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (mostrandoPassword && txtPasswordVisible != null) {
                    txtPasswordVisible.requestFocus();
                } else {
                    txtPassword.requestFocus();
                }
            }
        });

        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });

        Platform.runLater(() -> txtUsername.requestFocus());
    }

    private void actualizarFocoContenedorPassword() {
        if (boxPasswordContainer == null) {
            return;
        }
        boolean enfocado = txtPassword.isFocused()
                || (txtPasswordVisible != null && txtPasswordVisible.isFocused());
        if (enfocado) {
            if (!boxPasswordContainer.getStyleClass().contains("login-password-container-focused")) {
                boxPasswordContainer.getStyleClass().add("login-password-container-focused");
            }
        } else {
            boxPasswordContainer.getStyleClass().remove("login-password-container-focused");
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        if (txtPasswordVisible == null) {
            return;
        }
        mostrandoPassword = !mostrandoPassword;
        if (mostrandoPassword) {
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
            txtPasswordVisible.requestFocus();
            txtPasswordVisible
                    .positionCaret(txtPasswordVisible.getText() != null ? txtPasswordVisible.getText().length() : 0);
            if (svgEye != null) {
                svgEye.setContent(SVG_EYE_CLOSED);
            }
        } else {
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText() != null ? txtPassword.getText().length() : 0);
            if (svgEye != null) {
                svgEye.setContent(SVG_EYE_OPEN);
            }
        }
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            mostrarMensaje("Por favor ingrese su usuario y contraseña.");
            return;
        }

        btnIngresar.setDisable(true);
        btnIngresar.setText("VERIFICANDO...");

        try {
            Usuario usuario = usuarioDAO.login(username, password);
            if (usuario == null) {
                mostrarMensaje("El usuario o la contraseña no coinciden. Inténtelo de nuevo.");
                txtPassword.clear();
                if (mostrandoPassword && txtPasswordVisible != null) {
                    txtPasswordVisible.requestFocus();
                } else {
                    txtPassword.requestFocus();
                }
                btnIngresar.setDisable(false);
                btnIngresar.setText(TEXTO_BTN_INGRESAR);
                return;
            }

            // Iniciar sesion y cargar turno de caja si existe
            SesionUsuario.setUsuarioLogueado(usuario);
            try {
                Caja cajaAbierta = cajaDAO.obtenerCajaAbierta();
                SesionUsuario.setCajaActiva(cajaAbierta);
            } catch (SQLException e) {
                System.err.println("[LoginController] Aviso: No se pudo verificar la caja: " + e.getMessage());
            }

            // Navegar a la pantalla principal
            App.cambiarEscena("/fxml/main.fxml", "Papelería de Rosita - Sistema CONARY", 1180, 780);

        } catch (SQLException e) {
            btnIngresar.setDisable(false);
            btnIngresar.setText(TEXTO_BTN_INGRESAR);
            mostrarMensaje("Error de conexión con MySQL (XAMPP): Asegúrate de que MySQL esté iniciado.");
            AlertaUtil.mostrarError("Error de Conexión",
                    "No se pudo conectar con la base de datos MySQL.\n\n"
                            + "Verifica en el Panel de Control de XAMPP que el servicio 'MySQL' esté en verde (Start).\n"
                            + "Detalle: " + e.getMessage());
        }
    }

    private void mostrarMensaje(String texto) {
        lblMensaje.setText(texto);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
        if (boxError != null) {
            boxError.setVisible(true);
            boxError.setManaged(true);
        }
    }
}
