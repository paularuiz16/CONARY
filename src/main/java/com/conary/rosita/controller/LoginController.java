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

import java.sql.SQLException;

/**
 * Controlador de la pantalla de inicio de sesion.
 */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnIngresar;
    @FXML private Label lblMensaje;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final CajaDAO cajaDAO = new CajaDAO();

    @FXML
    public void initialize() {
        // Permitir pulsar Enter en cualquier campo para iniciar sesion
        txtUsername.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtPassword.requestFocus();
            }
        });

        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });

        Platform.runLater(() -> txtUsername.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText() != null ? txtUsername.getText().trim() : "";
        String password = txtPassword.getText() != null ? txtPassword.getText().trim() : "";

        if (username.isEmpty() || password.isEmpty()) {
            mostrarMensaje("Por favor ingresa tu usuario y contraseña.");
            return;
        }

        btnIngresar.setDisable(true);
        btnIngresar.setText("Verificando...");

        try {
            Usuario usuario = usuarioDAO.login(username, password);
            if (usuario == null) {
                mostrarMensaje("Usuario o contraseña incorrectos.");
                txtPassword.clear();
                txtPassword.requestFocus();
                btnIngresar.setDisable(false);
                btnIngresar.setText("Iniciar Sesión");
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
            App.cambiarEscena("/fxml/main.fxml", "Papelería de Rosita - Sistema CONARY", 1100, 700);

        } catch (SQLException e) {
            btnIngresar.setDisable(false);
            btnIngresar.setText("Iniciar Sesión");
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
    }
}

