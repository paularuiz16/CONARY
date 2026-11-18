package com.conary.rosita.controller;

import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.ImportadorExcelService;
import com.conary.rosita.util.ImportadorExcelService.FilaProblema;
import com.conary.rosita.util.ImportadorExcelService.FilaVentaExcel;
import com.conary.rosita.util.ImportadorExcelService.ResultadoMigracion;
import com.conary.rosita.util.ImportadorExcelService.ResultadoRevision;
import com.conary.rosita.util.SesionUsuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/**
 * Controlador de la pantalla "Pasar datos del Excel".
 * Permite seleccionar el Excel o CSV historico, revisar filas validas y omitidas,
 * y migrarlas en una transaccion unica a MySQL sin alterar stock ni caja.
 */
public class PasarDatosExcelController {

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverMasOpciones;
    @FXML private Label lblUsuarioHeader;

    @FXML private Button btnElegirArchivo;
    @FXML private HBox boxArchivoSeleccionado;
    @FXML private Label lblArchivoSeleccionado;

    @FXML private Label lblFilasLeidas;
    @FXML private Label lblFilasBien;
    @FXML private Label lblConProblemas;
    @FXML private Label lblProductosNuevos;

    @FXML private TableView<FilaVentaExcel> tblVistaPrevia;
    @FXML private TableColumn<FilaVentaExcel, String> colFecha;
    @FXML private TableColumn<FilaVentaExcel, String> colProducto;
    @FXML private TableColumn<FilaVentaExcel, String> colValorUnitario;
    @FXML private TableColumn<FilaVentaExcel, String> colCantidad;
    @FXML private TableColumn<FilaVentaExcel, String> colTotal;

    @FXML private VBox boxErroresRevision;
    @FXML private VBox boxListaErrores;

    @FXML private Button btnPasarDatos;
    @FXML private Button btnCancelar;

    private final ImportadorExcelService importadorService = new ImportadorExcelService();
    private final ObservableList<FilaVentaExcel> filasPreview = FXCollections.observableArrayList();
    private ResultadoRevision revisionActual;

    @FXML
    public void initialize() {
        cargarCabeceraUsuario();

        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede pasar ventas anteriores al sistema.");
            MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
            if (menu != null) {
                menu.mostrarMenuDashboard();
            }
            return;
        }

        configurarTablaVistaPrevia();
        limpiarRevision();
    }

    private void cargarCabeceraUsuario() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        if (u != null && lblUsuarioHeader != null) {
            String rolTexto = u.esAdministrador() ? "Administradora" : "Vendedora";
            lblUsuarioHeader.setText(u.getNombre() + " · " + rolTexto);
        }
    }

    private void configurarTablaVistaPrevia() {
        tblVistaPrevia.setItems(filasPreview);
        tblVistaPrevia.setFixedCellSize(56.0);
        tblVistaPrevia.setRowFactory(tv -> new TableRow<FilaVentaExcel>() {
            @Override
            protected void updateItem(FilaVentaExcel item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(56.0);
                setMinHeight(56.0);
                setMaxHeight(56.0);
            }
        });

        colFecha.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFechaTexto()));
        colProducto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombreProducto()));
        colValorUnitario.setCellValueFactory(data -> new SimpleStringProperty(
                "$ " + ImportadorExcelService.formatearMoneda(data.getValue().getValorUnitario())));
        colCantidad.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getCantidad())));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(
                "$ " + ImportadorExcelService.formatearMoneda(data.getValue().getValorTotal())));

        configurarAlineacionDerecha(colValorUnitario);
        configurarAlineacionCentro(colCantidad);
        configurarAlineacionDerecha(colTotal);
    }

    private void configurarAlineacionDerecha(TableColumn<FilaVentaExcel, String> col) {
        col.setCellFactory(c -> new TableCell<FilaVentaExcel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
    }

    private void configurarAlineacionCentro(TableColumn<FilaVentaExcel, String> col) {
        col.setCellFactory(c -> new TableCell<FilaVentaExcel, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setAlignment(Pos.CENTER);
            }
        });
    }

    @FXML
    public void handleElegirArchivo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir hoja de cálculo con ventas anteriores");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Hojas de cálculo y listas (*.xlsx, *.xls, *.csv)",
                        "*.xlsx", "*.xls", "*.csv"));

        String home = System.getProperty("user.home");
        if (home != null) {
            File dirHome = new File(home);
            if (dirHome.exists() && dirHome.isDirectory()) {
                chooser.setInitialDirectory(dirHome);
            }
        }

        Window owner = rootPane != null && rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
        File archivo = chooser.showOpenDialog(owner);
        if (archivo == null) {
            return;
        }

        try {
            ResultadoRevision revision = importadorService.leerYValidar(archivo);
            mostrarRevision(revision);
        } catch (Exception e) {
            limpiarRevision();
            AlertaUtil.mostrarError("No se pudo leer el archivo",
                    "Verifique que el archivo no esté abierto en otro programa y tenga las columnas esperadas.\n\nDetalle: "
                            + e.getMessage());
        }
    }

    private void mostrarRevision(ResultadoRevision rev) {
        this.revisionActual = rev;

        if (lblArchivoSeleccionado != null && boxArchivoSeleccionado != null) {
            lblArchivoSeleccionado.setText("✓ " + rev.getNombreArchivo());
            boxArchivoSeleccionado.setVisible(true);
            boxArchivoSeleccionado.setManaged(true);
        }

        lblFilasLeidas.setText("Filas leídas: " + rev.getTotalFilasLeidas());
        lblFilasBien.setText("Bien: " + rev.getFilasValidas().size());
        lblConProblemas.setText("⚠ Con problemas: " + rev.getFilasConProblemas().size());
        lblProductosNuevos.setText("Productos nuevos: " + rev.getCantidadProductosNuevos());

        filasPreview.setAll(rev.getFilasValidas());

        boxListaErrores.getChildren().clear();
        if (rev.getFilasConProblemas().isEmpty()) {
            boxErroresRevision.setVisible(false);
            boxErroresRevision.setManaged(false);
        } else {
            for (FilaProblema prob : rev.getFilasConProblemas()) {
                Label lbl = new Label("• " + prob.getMensaje());
                lbl.setWrapText(true);
                lbl.getStyleClass().add("excel-errores-item");
                boxListaErrores.getChildren().add(lbl);
            }
            boxErroresRevision.setVisible(true);
            boxErroresRevision.setManaged(true);
        }

        btnPasarDatos.setDisable(rev.getFilasValidas().isEmpty());
    }

    private void limpiarRevision() {
        revisionActual = null;
        filasPreview.clear();
        if (boxArchivoSeleccionado != null) {
            boxArchivoSeleccionado.setVisible(false);
            boxArchivoSeleccionado.setManaged(false);
        }
        if (lblFilasLeidas != null) lblFilasLeidas.setText("Filas leídas: 0");
        if (lblFilasBien != null) lblFilasBien.setText("Bien: 0");
        if (lblConProblemas != null) lblConProblemas.setText("⚠ Con problemas: 0");
        if (lblProductosNuevos != null) lblProductosNuevos.setText("Productos nuevos: 0");
        if (boxListaErrores != null) boxListaErrores.getChildren().clear();
        if (boxErroresRevision != null) {
            boxErroresRevision.setVisible(false);
            boxErroresRevision.setManaged(false);
        }
        if (btnPasarDatos != null) btnPasarDatos.setDisable(true);
    }

    @FXML
    public void handlePasarDatos() {
        if (revisionActual == null || revisionActual.getFilasValidas().isEmpty()) {
            return;
        }

        Usuario u = SesionUsuario.getUsuarioLogueado();
        int idUsuario = u != null && u.getIdUsuario() > 0 ? u.getIdUsuario() : 1;

        try {
            ResultadoMigracion resultado = importadorService.pasarDatosAMySQL(revisionActual, idUsuario);
            ResultadoPasarExcelController.setResultadoPendiente(resultado);
            MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
            if (menu != null) {
                ResultadoPasarExcelController ctrl = menu.cargarVistaConControlador("/fxml/resultado_pasar_excel.fxml");
                if (ctrl != null) {
                    ctrl.mostrarResultado(resultado);
                }
            }
        } catch (Exception e) {
            AlertaUtil.mostrarError("No se pudieron pasar los datos",
                    "Ocurrió un error al guardar en la base de datos (no se aplicó ningún cambio): " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/mas_opciones.fxml");
        }
    }
}
