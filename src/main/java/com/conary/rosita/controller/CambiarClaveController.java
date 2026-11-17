package com.conary.rosita.controller;

import com.conary.rosita.dao.UsuarioDAO;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Controlador del modal "Cambiar clave" (Paso 2B de Mas opciones).
 * Permite asignar una nueva clave cifrada con SHA-256 al usuario seleccionado.
 */
public class CambiarClaveController {

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

    @FXML private StackPane overlayRoot;
    @FXML private Label lblNombreUsuario;
    @FXML private Label lblUsername;
    @FXML private Label lblRolUsuario;

    @FXML private HBox boxAlertaClave;
    @FXML private Label lblAlertaClave;

    @FXML private HBox boxClaveNueva;
    @FXML private PasswordField txtClaveNueva;
    @FXML private TextField txtClaveNuevaVisible;
    @FXML private Button btnVerClaveNueva;
    @FXML private SVGPath svgOjoClaveNueva;

    @FXML private HBox boxRepetirClaveNueva;
    @FXML private PasswordField txtRepetirClaveNueva;
    @FXML private TextField txtRepetirClaveNuevaVisible;
    @FXML private Button btnVerRepetirClaveNueva;
    @FXML private SVGPath svgOjoRepetirClaveNueva;

    @FXML private Label lblNotaSeguridad;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardarClave;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    private Usuario usuarioObjetivo;
    private boolean claveVisible = false;
    private boolean repetirClaveVisible = false;
    private boolean claveActualizada = false;

    @FXML
    public void initialize() {
        txtClaveNuevaVisible.textProperty().bindBidirectional(txtClaveNueva.textProperty());
        txtRepetirClaveNuevaVisible.textProperty().bindBidirectional(txtRepetirClaveNueva.textProperty());

        txtClaveNueva.textProperty().addListener((obs, o, n) -> ocultarAlerta());
        txtRepetirClaveNueva.textProperty().addListener((obs, o, n) -> ocultarAlerta());

        configurarFocoCaja(boxClaveNueva, txtClaveNueva, txtClaveNuevaVisible);
        configurarFocoCaja(boxRepetirClaveNueva, txtRepetirClaveNueva, txtRepetirClaveNuevaVisible);

        overlayRoot.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                handleCancelar();
            }
        });
    }

    public void setUsuario(Usuario usuario) {
        this.usuarioObjetivo = usuario;
        if (usuario == null) {
            return;
        }

        String nombreMostrar = usuario.getNombre() != null ? usuario.getNombre().trim() : "";
        String username = usuario.getUsername() != null ? usuario.getUsername().trim() : "";
        boolean esAdmin = usuario.esAdministrador();

        lblNombreUsuario.setText(nombreMostrar);
        lblUsername.setText("@" + username);

        lblRolUsuario.getStyleClass().removeAll("modal-clave-badge-rol", "modal-clave-badge-rol-admin");
        if (esAdmin) {
            lblRolUsuario.setText("ADMINISTRADORA");
            lblRolUsuario.getStyleClass().add("modal-clave-badge-rol-admin");
        } else {
            lblRolUsuario.setText("VENDEDORA");
            lblRolUsuario.getStyleClass().add("modal-clave-badge-rol");
        }

        lblNotaSeguridad.setText(nombreMostrar + " entrará con la clave nueva. La anterior deja de servir.");
        ocultarAlerta();
    }

    public boolean isClaveActualizada() {
        return claveActualizada;
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
    public void handleToggleVerClaveNueva() {
        claveVisible = !claveVisible;
        alternarVisibilidad(txtClaveNueva, txtClaveNuevaVisible, svgOjoClaveNueva, claveVisible);
    }

    @FXML
    public void handleToggleVerRepetirClaveNueva() {
        repetirClaveVisible = !repetirClaveVisible;
        alternarVisibilidad(txtRepetirClaveNueva, txtRepetirClaveNuevaVisible, svgOjoRepetirClaveNueva, repetirClaveVisible);
    }

    private void alternarVisibilidad(PasswordField pf, TextField tf, SVGPath icono, boolean mostrar) {
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
        cerrarModal();
    }

    @FXML
    public void handleGuardarClave() {
        if (usuarioObjetivo == null) {
            cerrarModal();
            return;
        }

        String claveNueva = txtClaveNueva.getText() != null ? txtClaveNueva.getText() : "";
        String repetirClave = txtRepetirClaveNueva.getText() != null ? txtRepetirClaveNueva.getText() : "";

        if (claveNueva.length() < 6) {
            mostrarAlerta("La clave debe tener mínimo 6 caracteres.");
            if (claveVisible) {
                txtClaveNuevaVisible.requestFocus();
            } else {
                txtClaveNueva.requestFocus();
            }
            return;
        }

        if (!claveNueva.equals(repetirClave)) {
            mostrarAlerta("Las dos claves no coinciden.");
            if (repetirClaveVisible) {
                txtRepetirClaveNuevaVisible.requestFocus();
            } else {
                txtRepetirClaveNueva.requestFocus();
            }
            return;
        }

        try {
            boolean actualizado = usuarioDAO.actualizarPassword(usuarioObjetivo.getIdUsuario(), claveNueva);
            if (!actualizado) {
                mostrarAlerta("No se encontró el usuario en la base de datos.");
                return;
            }

            claveActualizada = true;
            String nombreMostrar = usuarioObjetivo.getNombre() != null
                    ? usuarioObjetivo.getNombre().trim()
                    : usuarioObjetivo.getUsername();

            cerrarModal();
            AlertaUtil.mostrarInfo("Clave actualizada",
                    "La nueva clave de «" + nombreMostrar + "» se guardó correctamente.");

        } catch (SQLException e) {
            mostrarAlerta("Error al guardar la clave en MySQL: " + e.getMessage());
        }
    }

    private void mostrarAlerta(String mensaje) {
        lblAlertaClave.setText(mensaje);
        boxAlertaClave.setVisible(true);
        boxAlertaClave.setManaged(true);
    }

    private void ocultarAlerta() {
        if (boxAlertaClave != null) {
            boxAlertaClave.setVisible(false);
            boxAlertaClave.setManaged(false);
        }
    }

    private void cerrarModal() {
        if (overlayRoot != null && overlayRoot.getScene() != null
                && overlayRoot.getScene().getWindow() instanceof Stage) {
            ((Stage) overlayRoot.getScene().getWindow()).close();
        }
    }
}
