package com.conary.rosita.controller;

import com.conary.rosita.App;
import com.conary.rosita.db.ConexionDB;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

/**
 * Controlador de la pantalla de carga inicial (Splash Screen).
 * Ejecuta la verificacion de entorno y base de datos en segundo plano (Java 8 Task).
 */
public class SplashController {

    @FXML private Label lblEstado;
    @FXML private ProgressBar progressBar;

    @FXML
    public void initialize() {
        Task<Void> cargaTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                boolean dbVerificada = false;

                for (int i = 0; i <= 100; i++) {
                    if (i <= 35) {
                        updateMessage("Iniciando entorno CONARY...");
                    } else if (i <= 75) {
                        updateMessage("Verificando base de datos...");
                        if (!dbVerificada) {
                            dbVerificada = true;
                            long inicio = System.currentTimeMillis();
                            boolean ok = ConexionDB.probarConexion();
                            if (ok) {
                                System.out.println("[Splash] Conexión con base de datos MySQL papeleria_rosita: OK");
                            } else {
                                System.err.println("[Splash] Aviso: No se detectó base de datos papeleria_rosita en ejecución.");
                            }
                            long transcurrido = System.currentTimeMillis() - inicio;
                            int pasosConsumidos = (int) Math.min(25, transcurrido / 22);
                            i = Math.min(70, i + pasosConsumidos);
                        }
                    } else {
                        updateMessage("Cargando interfaz...");
                    }

                    updateProgress(i, 100);
                    Thread.sleep(22);
                }
                return null;
            }
        };

        lblEstado.textProperty().bind(cargaTask.messageProperty());
        progressBar.progressProperty().bind(cargaTask.progressProperty());

        cargaTask.setOnSucceeded(event -> {
            lblEstado.textProperty().unbind();
            progressBar.progressProperty().unbind();
            App.mostrarLoginDesdeSplash();
        });

        cargaTask.setOnFailed(event -> {
            lblEstado.textProperty().unbind();
            progressBar.progressProperty().unbind();
            App.mostrarLoginDesdeSplash();
        });

        Thread hiloCarga = new Thread(cargaTask, "Conary-Splash-Loader");
        hiloCarga.setDaemon(true);
        hiloCarga.start();
    }
}
