package com.conary.rosita;

import com.conary.rosita.db.ConexionDB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Clase principal de inicio para la aplicacion de escritorio en JavaFX (Java 8).
 * Optimizada para bajo consumo de memoria en Windows 8 (2 GB RAM).
 */
public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Papelería de Rosita - Inicio de Sesión");

        // Prueba rapida en segundo plano de la conexion a base de datos
        new Thread(() -> {
            boolean ok = ConexionDB.probarConexion();
            if (ok) {
                System.out.println("[App] Conexión con base de datos MySQL papeleria_rosita: OK");
            } else {
                System.err.println("[App] Aviso: No se detectó base de datos papeleria_rosita en ejecución.");
            }
        }).start();

        cambiarEscena("/fxml/login.fxml", "Papelería de Rosita - Acceso al Sistema", 420, 500);
        primaryStage.show();
    }

    /**
     * Reemplaza la escena activa en la ventana principal de forma fluida.
     *
     * @param fxmlPath Ruta al archivo .fxml en resources.
     * @param titulo Titulo de la ventana.
     * @param width Ancho inicial sugerido.
     * @param height Alto inicial sugerido.
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

