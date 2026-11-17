package com.conary.rosita.controller;

import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.ExportadorReportesService;
import com.conary.rosita.util.ExportadorReportesService.DatosExportacion;
import com.conary.rosita.util.ExportadorReportesService.Formato;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Controlador del Modal "Guardar reporte" (PDF y Excel reales en Java 8).
 */
public class GuardarReporteController {

    @FXML private Label lblResumenReporte;
    @FXML private HBox boxReporteGuardado;
    @FXML private Label lblMensajeGuardado;
    @FXML private ToggleButton tbFormatoPdf;
    @FXML private ToggleButton tbFormatoExcel;
    @FXML private Label lblRutaCarpeta;
    @FXML private Button btnCambiarCarpeta;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;

    private final ExportadorReportesService exportador = new ExportadorReportesService();
    private final ToggleGroup tgFormato = new ToggleGroup();

    private DatosExportacion datosExportacion;
    private File carpetaPorDefecto;
    private File carpetaSeleccionada;

    public static void abrirModal(Window owner, DatosExportacion datos) {
        try {
            URL url = GuardarReporteController.class.getResource("/fxml/modal_guardar_reporte.fxml");
            if (url == null) {
                throw new IOException("No se encontró /fxml/modal_guardar_reporte.fxml");
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            GuardarReporteController ctrl = loader.getController();
            ctrl.setDatosExportacion(datos);

            Stage stage = new Stage();
            stage.setTitle("Guardar reporte");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setResizable(false);

            if (owner != null) {
                stage.initOwner(owner);
                final Node ownerRoot = owner.getScene() != null ? owner.getScene().getRoot() : null;
                final ColorAdjust dim = new ColorAdjust(0, 0, -0.28, 0);
                stage.setOnShown(ev -> {
                    if (ownerRoot != null) ownerRoot.setEffect(dim);
                    stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2.0);
                    stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2.0);
                });
                stage.setOnHidden(ev -> {
                    if (ownerRoot != null) ownerRoot.setEffect(null);
                });
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            URL cssUrl = GuardarReporteController.class.getResource("/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error de Interfaz",
                    "No se pudo abrir el modal Guardar reporte: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        carpetaPorDefecto = ExportadorReportesService.obtenerCarpetaDocumentosPorDefecto();
        carpetaSeleccionada = carpetaPorDefecto;

        if (tbFormatoPdf != null) tbFormatoPdf.setToggleGroup(tgFormato);
        if (tbFormatoExcel != null) tbFormatoExcel.setToggleGroup(tgFormato);
        if (tbFormatoPdf != null) tbFormatoPdf.setSelected(true);

        tgFormato.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }
            actualizarEtiquetasFormato();
        });
        actualizarEtiquetasFormato();
        actualizarTextoCarpeta();
    }

    public void setDatosExportacion(DatosExportacion datos) {
        this.datosExportacion = datos;
        if (lblResumenReporte != null && datos != null) {
            String sub = datos.getSubtitulo() != null ? datos.getSubtitulo().trim() : "";
            lblResumenReporte.setText(sub.isEmpty() ? datos.getTitulo() : (datos.getTitulo() + "  ·  " + sub));
        }
    }

    private void actualizarEtiquetasFormato() {
        Toggle sel = tgFormato.getSelectedToggle();
        if (tbFormatoPdf != null) {
            tbFormatoPdf.setText(sel == tbFormatoPdf ? "📄   PDF   ✓" : "📄   PDF");
        }
        if (tbFormatoExcel != null) {
            tbFormatoExcel.setText(sel == tbFormatoExcel ? "📊   Excel   ✓" : "📊   Excel");
        }
    }

    private void actualizarTextoCarpeta() {
        if (lblRutaCarpeta == null || carpetaSeleccionada == null) {
            return;
        }
        if (carpetaPorDefecto != null && carpetaSeleccionada.equals(carpetaPorDefecto)) {
            lblRutaCarpeta.setText("Carpeta Documentos de este equipo");
        } else {
            lblRutaCarpeta.setText(carpetaSeleccionada.getAbsolutePath());
        }
    }

    @FXML
    public void handleCambiarCarpeta() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Elegir carpeta para guardar el reporte");
        if (carpetaSeleccionada != null && carpetaSeleccionada.exists()) {
            chooser.setInitialDirectory(carpetaSeleccionada);
        }
        Window owner = btnCambiarCarpeta != null && btnCambiarCarpeta.getScene() != null
                ? btnCambiarCarpeta.getScene().getWindow()
                : null;
        File elegida = chooser.showDialog(owner);
        if (elegida != null) {
            carpetaSeleccionada = elegida;
            actualizarTextoCarpeta();
            if (boxReporteGuardado != null) {
                boxReporteGuardado.setVisible(false);
                boxReporteGuardado.setManaged(false);
            }
        }
    }

    @FXML
    public void handleGuardar() {
        if (datosExportacion == null) {
            return;
        }
        Formato formato = tgFormato.getSelectedToggle() == tbFormatoExcel ? Formato.EXCEL : Formato.PDF;

        try {
            File generado = exportador.exportar(datosExportacion, formato, carpetaSeleccionada);
            String nombreCarpeta = carpetaPorDefecto != null && carpetaSeleccionada.equals(carpetaPorDefecto)
                    ? "Documentos"
                    : carpetaSeleccionada.getName();
            if (lblMensajeGuardado != null) {
                lblMensajeGuardado.setText("✓  Reporte guardado en " + nombreCarpeta + " (" + generado.getName() + ").");
            }
            if (boxReporteGuardado != null) {
                boxReporteGuardado.setVisible(true);
                boxReporteGuardado.setManaged(true);
            }
            if (btnCancelar != null) {
                btnCancelar.setText("Cerrar");
            }
        } catch (Exception e) {
            AlertaUtil.mostrarError("Error al Guardar Reporte",
                    "No se pudo generar el archivo del reporte: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        if (btnCancelar != null && btnCancelar.getScene() != null) {
            Stage stage = (Stage) btnCancelar.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
}