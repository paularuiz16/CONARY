package com.conary.rosita.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Utilidad para abrir ventanas modales (APPLICATION_MODAL) centradas y con fondo atenuado.
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public final class ModalUtil {

    private ModalUtil() {
    }

    /**
     * Carga el FXML indicado, permite inicializar su controlador y lo muestra como modal bloqueante.
     *
     * @return el controlador del modal una vez cerrado (o null si no tiene controlador).
     */
    public static <T> T abrirModal(String fxmlPath, String titulo, Node ownerNode, Consumer<T> inicializador)
            throws IOException {
        URL url = ModalUtil.class.getResource(fxmlPath);
        if (url == null) {
            File fallback = new File("src/main/resources" + fxmlPath);
            if (fallback.exists()) {
                url = fallback.toURI().toURL();
            }
        }
        if (url == null) {
            throw new IOException("No se encontró el recurso FXML: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        T controlador = loader.getController();
        if (inicializador != null && controlador != null) {
            inicializador.accept(controlador);
        }

        final Stage stage = new Stage();
        stage.setTitle(titulo);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        if (ownerNode != null && ownerNode.getScene() != null && ownerNode.getScene().getWindow() != null) {
            final Window owner = ownerNode.getScene().getWindow();
            stage.initOwner(owner);
            final Node ownerRoot = ownerNode.getScene().getRoot();
            final ColorAdjust dim = new ColorAdjust(0, 0, -0.28, 0);
            stage.setOnShown(ev -> {
                if (ownerRoot != null) {
                    ownerRoot.setEffect(dim);
                }
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2.0);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2.0);
            });
            stage.setOnHidden(ev -> {
                if (ownerRoot != null) {
                    ownerRoot.setEffect(null);
                }
            });
        }

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        URL cssUrl = ModalUtil.class.getResource("/css/styles.css");
        if (cssUrl == null) {
            File cssFallback = new File("src/main/resources/css/styles.css");
            if (cssFallback.exists()) {
                cssUrl = cssFallback.toURI().toURL();
            }
        }
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setScene(scene);
        stage.showAndWait();
        return controlador;
    }
}
