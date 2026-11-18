package com.conary.rosita.controller;

import com.conary.rosita.dao.UsuarioDAO;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.RespaldoService;
import com.conary.rosita.util.RespaldoService.ArchivoRespaldo;
import com.conary.rosita.util.SesionUsuario;
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

import java.io.File;

/**
 * Controlador del modal "¿Volver a la copia del [fecha]?" (Paso 2C de Mas opciones).
 * Exige confirmacion con la clave del administrador en sesion, crea un respaldo preventivo
 * y restaura el archivo .sql seleccionado sobre MySQL.
 */
public class RestaurarCopiaController {

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
    @FXML private Label lblTituloModal;
    @FXML private Label lblAvisoFecha;

    @FXML private HBox boxClaveIncorrecta;
    @FXML private Label lblClaveIncorrecta;

    @FXML private HBox boxClaveConfirmar;
    @FXML private PasswordField txtClaveConfirmar;
    @FXML private TextField txtClaveConfirmarVisible;
    @FXML private Button btnVerClave;
    @FXML private SVGPath svgOjoClave;

    @FXML private Button btnNoVolver;
    @FXML private Button btnSiRestaurar;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final RespaldoService respaldoService = new RespaldoService();

    private ArchivoRespaldo copiaSeleccionada;
    private boolean claveVisible = false;
    private boolean restauracionCompletada = false;

    @FXML
    public void initialize() {
        txtClaveConfirmarVisible.textProperty().bindBidirectional(txtClaveConfirmar.textProperty());

        txtClaveConfirmar.textProperty().addListener((obs, o, n) -> ocultarError());

        txtClaveConfirmar.focusedProperty().addListener((obs, o, focused) ->
                actualizarClaseFoco(focused || txtClaveConfirmarVisible.isFocused()));
        txtClaveConfirmarVisible.focusedProperty().addListener((obs, o, focused) ->
                actualizarClaseFoco(focused || txtClaveConfirmar.isFocused()));

        overlayRoot.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                handleCancelar();
            }
        });
    }

    public void setCopiaSeleccionada(ArchivoRespaldo copia) {
        this.copiaSeleccionada = copia;
        if (copia == null) {
            return;
        }
        lblTituloModal.setText("¿Volver a la copia del " + copia.getFechaCorta() + "?");
        lblAvisoFecha.setText("Todo volverá a como estaba el " + copia.getFechaHoraLegible() + ".");
        ocultarError();
    }

    public boolean isRestauracionCompletada() {
        return restauracionCompletada;
    }

    private void actualizarClaseFoco(boolean focused) {
        if (focused) {
            if (!boxClaveConfirmar.getStyleClass().contains("reg-usuario-password-box-focused")) {
                boxClaveConfirmar.getStyleClass().add("reg-usuario-password-box-focused");
            }
        } else {
            boxClaveConfirmar.getStyleClass().remove("reg-usuario-password-box-focused");
        }
    }

    @FXML
    public void handleToggleVerClave() {
        claveVisible = !claveVisible;
        txtClaveConfirmarVisible.setVisible(claveVisible);
        txtClaveConfirmarVisible.setManaged(claveVisible);
        txtClaveConfirmar.setVisible(!claveVisible);
        txtClaveConfirmar.setManaged(!claveVisible);
        svgOjoClave.setContent(claveVisible ? SVG_EYE_CLOSED : SVG_EYE_OPEN);

        if (claveVisible) {
            txtClaveConfirmarVisible.requestFocus();
            String t = txtClaveConfirmarVisible.getText();
            txtClaveConfirmarVisible.positionCaret(t != null ? t.length() : 0);
        } else {
            txtClaveConfirmar.requestFocus();
            String t = txtClaveConfirmar.getText();
            txtClaveConfirmar.positionCaret(t != null ? t.length() : 0);
        }
    }

    @FXML
    public void handleCancelar() {
        cerrarModal();
    }

    @FXML
    public void handleConfirmarRestaurar() {
        if (copiaSeleccionada == null) {
            cerrarModal();
            return;
        }

        String claveIngresada = txtClaveConfirmar.getText() != null ? txtClaveConfirmar.getText() : "";
        if (claveIngresada.trim().isEmpty()) {
            mostrarError("La clave escrita no es correcta. Inténtelo de nuevo.");
            enfocarCampoClave();
            return;
        }

        Usuario usuarioActual = SesionUsuario.getUsuarioLogueado();
        if (usuarioActual == null || !usuarioActual.esAdministrador()) {
            mostrarError("Debe haber una sesión de administradora activa para restaurar una copia.");
            return;
        }

        try {
            boolean claveValida = usuarioDAO.verificarPasswordUsuario(usuarioActual.getIdUsuario(), claveIngresada);
            if (!claveValida) {
                mostrarError("La clave escrita no es correcta. Inténtelo de nuevo.");
                enfocarCampoClave();
                return;
            }

            File respaldoPreventivo = respaldoService.restaurarCopia(copiaSeleccionada.getArchivo());
            restauracionCompletada = true;

            cerrarModal();
            AlertaUtil.mostrarInfo("Copia restaurada",
                    "La información volvió al estado del " + copiaSeleccionada.getFechaHoraLegible()
                            + ".\n\nSe guardó antes un respaldo preventivo de hoy: "
                            + respaldoPreventivo.getName());

        } catch (Exception e) {
            mostrarError("No se pudo completar la restauración: " + e.getMessage());
        }
    }

    private void enfocarCampoClave() {
        if (claveVisible) {
            txtClaveConfirmarVisible.requestFocus();
        } else {
            txtClaveConfirmar.requestFocus();
        }
    }

    private void mostrarError(String mensaje) {
        lblClaveIncorrecta.setText(mensaje);
        boxClaveIncorrecta.setVisible(true);
        boxClaveIncorrecta.setManaged(true);
    }

    private void ocultarError() {
        if (boxClaveIncorrecta != null) {
            boxClaveIncorrecta.setVisible(false);
            boxClaveIncorrecta.setManaged(false);
        }
    }

    private void cerrarModal() {
        if (overlayRoot != null && overlayRoot.getScene() != null
                && overlayRoot.getScene().getWindow() instanceof Stage) {
            ((Stage) overlayRoot.getScene().getWindow()).close();
        }
    }
}
