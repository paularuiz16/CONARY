package com.conary.rosita;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.io.InputStream;

/**
 * Clase principal de inicio para la aplicacion de escritorio en JavaFX (Java
 * 8).
 * Optimizada para bajo consumo de memoria en Windows 8 (2 GB RAM).
 */
public class App extends Application {

    private static Stage primaryStage;
    private static Stage splashStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setTitle("Papelería de Rosita - Inicio de Sesión");
        cargarIconoVentana(primaryStage);

        mostrarSplash();
    }

    /**
     * Asigna el isotipo corporativo como icono de ventana del Stage indicado.
     */
    private static void cargarIconoVentana(Stage stage) {
        try {
            InputStream iconStream = App.class.getResourceAsStream("/images/conary_logo.png");
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            System.err.println("[App] Aviso: No se pudo cargar el icono de la ventana: " + e.getMessage());
        }
    }


    /**
     * Muestra la ventana de carga inicial sin barra de titulo (Splash Screen).
     */
    private static void mostrarSplash() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/splash.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 680, 400);
            scene.setFill(Color.TRANSPARENT);

            splashStage = new Stage(StageStyle.UNDECORATED);
            splashStage.setTitle("CONARY - Iniciando...");
            cargarIconoVentana(splashStage);
            splashStage.setScene(scene);
            splashStage.setResizable(false);
            splashStage.centerOnScreen();
            splashStage.show();
        } catch (IOException e) {
            System.err.println("[App] Aviso: No se pudo cargar el Splash Screen, abriendo Login directamente: " + e.getMessage());
            mostrarLoginDesdeSplash();
        }
    }

    /**
     * Cierra el Splash Screen y abre la ventana principal decorada en la vista de
     * Login.
     */
    public static void mostrarLoginDesdeSplash() {
        if (splashStage != null) {
            splashStage.close();
            splashStage = null;
        }
        cambiarEscena("/fxml/login.fxml", "Papelería de Rosita - Inicio de Sesión", 680, 660);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
    }

    /**
     * Reemplaza la escena activa en la ventana principal de forma fluida.
     *
     * @param fxmlPath Ruta al archivo .fxml en resources.
     * @param titulo   Titulo de la ventana.
     * @param width    Ancho inicial sugerido.
     * @param height   Alto inicial sugerido.
     */
    public static void cambiarEscena(String fxmlPath, String titulo, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);

            primaryStage.setTitle(titulo);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(400);
            primaryStage.setMinHeight(450);
            primaryStage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("[App] Error crítico al cargar la vista " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
