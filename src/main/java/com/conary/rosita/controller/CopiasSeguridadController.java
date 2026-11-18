package com.conary.rosita.controller;

import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.RespaldoService;
import com.conary.rosita.util.RespaldoService.ArchivoRespaldo;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Controlador de la vista "Copias de seguridad" (Paso 2C de Mas opciones).
 * Permite crear respaldos locales o en USB, consultar el historial de respaldos .sql
 * y lanzar el modal de confirmacion para restaurar una copia anterior.
 */
public class CopiasSeguridadController {

    private static final String SVG_ESCUDO_FILA =
            "M 9 1.5 L 2.5 4.2 L 2.5 9.2 C 2.5 13.3 5.3 16.9 9 18.2 C 12.7 16.9 15.5 13.3 15.5 9.2 L 15.5 4.2 Z";

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverMasOpciones;
    @FXML private Label lblUltimaCopia;
    @FXML private Label lblUsuarioHeader;

    @FXML private Button btnCrearCopiaAhora;
    @FXML private Button btnGuardarEnUsb;

    @FXML private HBox boxCopiaExitosa;
    @FXML private Label lblCopiaExitosa;

    @FXML private Label lblConteoCopias;
    @FXML private VBox vboxListaCopias;

    private final RespaldoService respaldoService = new RespaldoService();

    @FXML
    public void initialize() {
        cargarCabeceraUsuario();

        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso restringido",
                    "Solo Doña Rosita (Administradora) puede gestionar las copias de seguridad.");
            MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
            if (menu != null) {
                menu.mostrarMenuDashboard();
            }
            return;
        }

        ocultarBannerExito();
        refrescarVista();
    }

    private void cargarCabeceraUsuario() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        if (u != null && lblUsuarioHeader != null) {
            String rolTexto = u.esAdministrador() ? "Administradora" : "Vendedora";
            lblUsuarioHeader.setText(u.getNombre() + " · " + rolTexto);
        }
    }

    private void refrescarVista() {
        if (lblUltimaCopia != null) {
            lblUltimaCopia.setText(respaldoService.obtenerTextoUltimaCopia());
        }
        cargarHistorialCopias();
    }

    private void cargarHistorialCopias() {
        vboxListaCopias.getChildren().clear();
        List<ArchivoRespaldo> copias = respaldoService.listarCopiasLocales();

        int total = copias.size();
        if (lblConteoCopias != null) {
            lblConteoCopias.setText(total == 1
                    ? "1 copia guardada en este equipo"
                    : total + " copias guardadas en este equipo");
        }

        if (copias.isEmpty()) {
            Label lblVacio = new Label(
                    "Todavía no hay copias registradas en la carpeta «" + RespaldoService.CARPETA_RESPALDOS
                            + "/». Pulse «Crear copia ahora» para guardar la primera.");
            lblVacio.setWrapText(true);
            lblVacio.getStyleClass().add("copias-vacio-texto");
            vboxListaCopias.getChildren().add(lblVacio);
            return;
        }

        for (ArchivoRespaldo copia : copias) {
            vboxListaCopias.getChildren().add(construirFilaCopia(copia));
        }
    }

    private HBox construirFilaCopia(final ArchivoRespaldo copia) {
        HBox fila = new HBox(14);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("copias-row");

        StackPane iconBox = new StackPane();
        iconBox.getStyleClass().add("copias-row-icon-box");
        SVGPath svg = new SVGPath();
        svg.setContent(SVG_ESCUDO_FILA);
        svg.setStyle("-fx-fill: #704214;");
        iconBox.getChildren().add(svg);

        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblFecha = new Label(copia.getFechaHoraLegible());
        lblFecha.getStyleClass().add("copias-row-fecha");

        Label lblMeta = new Label(copia.getNombreArchivo() + "  ·  " + copia.getTamanoLegible());
        lblMeta.getStyleClass().add("copias-row-meta");

        infoBox.getChildren().addAll(lblFecha, lblMeta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnRestaurar = new Button("🔄   Restaurar");
        btnRestaurar.getStyleClass().add("copias-btn-restaurar");
        btnRestaurar.setOnAction(e -> abrirModalRestaurar(copia));

        fila.getChildren().addAll(iconBox, infoBox, spacer, btnRestaurar);
        return fila;
    }

    @FXML
    public void handleCrearCopiaAhora() {
        ocultarBannerExito();
        try {
            File archivoGenerado = respaldoService.crearCopiaLocal();
            mostrarBannerExito("✓ Copia de seguridad guardada correctamente (" + archivoGenerado.getName() + ").");
            refrescarVista();
        } catch (Exception e) {
            AlertaUtil.mostrarError("Error al crear copia",
                    "No se pudo generar la copia de seguridad: " + e.getMessage());
        }
    }

    @FXML
    public void handleGuardarEnUsb() {
        ocultarBannerExito();

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Elegir memoria USB o carpeta para guardar la copia");
        String userHome = System.getProperty("user.home");
        File inicial = userHome != null ? new File(userHome) : new File(".");
        if (inicial.exists() && inicial.isDirectory()) {
            chooser.setInitialDirectory(inicial);
        }

        Window owner = rootPane != null && rootPane.getScene() != null
                ? rootPane.getScene().getWindow()
                : null;
        File carpetaElegida = chooser.showDialog(owner);
        if (carpetaElegida == null) {
            return;
        }

        try {
            File archivoGenerado = respaldoService.crearCopiaEnCarpetaExterna(carpetaElegida);
            mostrarBannerExito("✓ Copia de seguridad guardada correctamente (" + archivoGenerado.getName()
                    + ") en «" + carpetaElegida.getName() + "».");
            refrescarVista();
        } catch (Exception e) {
            AlertaUtil.mostrarError("Error al guardar en USB / carpeta",
                    "No se pudo guardar la copia en la carpeta elegida: " + e.getMessage());
        }
    }

    private void abrirModalRestaurar(ArchivoRespaldo copia) {
        if (copia == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/modal_restaurar_copia.fxml"));
            Parent root = loader.load();

            RestaurarCopiaController controller = loader.getController();
            controller.setCopiaSeleccionada(copia);

            Stage modalStage = new Stage(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Window owner = rootPane != null && rootPane.getScene() != null
                    ? rootPane.getScene().getWindow()
                    : null;
            if (owner != null) {
                modalStage.initOwner(owner);
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            if (rootPane != null && rootPane.getScene() != null) {
                scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
            } else {
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            }

            modalStage.setScene(scene);
            if (owner != null) {
                modalStage.setX(owner.getX());
                modalStage.setY(owner.getY());
                modalStage.setWidth(owner.getWidth());
                modalStage.setHeight(owner.getHeight());
            }
            modalStage.showAndWait();

            if (controller.isRestauracionCompletada()) {
                mostrarBannerExito("✓ Información restaurada correctamente a la copia del "
                        + copia.getFechaHoraLegible() + ".");
                refrescarVista();
            }
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error al abrir confirmación",
                    "No se pudo abrir la ventana de restauración: " + e.getMessage());
        }
    }

    @FXML
    public void handleVolverAMasOpciones() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/mas_opciones.fxml");
        }
    }

    private void mostrarBannerExito(String mensaje) {
        if (lblCopiaExitosa != null && boxCopiaExitosa != null) {
            lblCopiaExitosa.setText(mensaje);
            boxCopiaExitosa.setVisible(true);
            boxCopiaExitosa.setManaged(true);
        }
    }

    private void ocultarBannerExito() {
        if (boxCopiaExitosa != null) {
            boxCopiaExitosa.setVisible(false);
            boxCopiaExitosa.setManaged(false);
        }
    }
}
