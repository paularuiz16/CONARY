package com.conary.rosita.controller;

import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.model.Producto;
import com.conary.rosita.util.AlertaUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de Gestion de Inventario, Catalogo y Exportacion a Excel.
 */
public class InventarioController {

    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkSoloStockBajo;
    @FXML private Label lblTotalProductos;

    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colPrecioCompra;
    @FXML private TableColumn<Producto, String> colPrecioVenta;
    @FXML private TableColumn<Producto, Integer> colStockActual;
    @FXML private TableColumn<Producto, Integer> colStockMinimo;
    @FXML private TableColumn<Producto, String> colEstadoStock;

    @FXML private Label lblTituloForm;
    @FXML private TextField txtNombre;
    @FXML private TextField txtCategoria;
    @FXML private TextField txtPrecioCompra;
    @FXML private TextField txtPrecioVenta;
    @FXML private TextField txtStockActual;
    @FXML private TextField txtStockMinimo;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ObservableList<Producto> listaProductos = FXCollections.observableArrayList();
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private Producto productoSeleccionado;

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarDatos();

        txtBuscar.textProperty().addListener((obs, viejo, nuevo) -> filtrarProductos());
        chkSoloStockBajo.selectedProperty().addListener((obs, viejo, nuevo) -> filtrarProductos());

        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null) {
                cargarProductoEnFormulario(nuevo);
            }
        });
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getIdProducto()));
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria()));
        colPrecioCompra.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getPrecioCompra())));
        colPrecioVenta.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getPrecioVenta())));
        colStockActual.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStockActual()));
        colStockMinimo.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStockMinimo()));

        colEstadoStock.setCellValueFactory(data -> {
            if (data.getValue().isStockBajo()) {
                return new SimpleStringProperty("⚠️ BAJO");
            }
            return new SimpleStringProperty("✓ OK");
        });

        colEstadoStock.setCellFactory(column -> new TableCell<Producto, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("BAJO")) {
                        setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                    }
                }
            }
        });

        tblProductos.setItems(listaProductos);
    }

    private void cargarDatos() {
        try {
            List<Producto> lista = productoDAO.listarTodos(false);
            listaProductos.setAll(lista);
            lblTotalProductos.setText("Total productos: " + lista.size());
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error", "No se pudo cargar el inventario: " + e.getMessage());
        }
    }

    private void filtrarProductos() {
        String criterio = txtBuscar.getText() != null ? txtBuscar.getText().trim() : "";
        boolean soloBajo = chkSoloStockBajo.isSelected();

        try {
            List<Producto> lista;
            if (soloBajo) {
                lista = productoDAO.listarStockBajo();
            } else if (!criterio.isEmpty()) {
                lista = productoDAO.buscar(criterio, false);
            } else {
                lista = productoDAO.listarTodos(false);
            }
            listaProductos.setAll(lista);
            lblTotalProductos.setText("Total productos: " + lista.size());
        } catch (SQLException e) {
            System.err.println("[InventarioController] Error al filtrar: " + e.getMessage());
        }
    }

    private void cargarProductoEnFormulario(Producto p) {
        productoSeleccionado = p;
        lblTituloForm.setText("Editar Producto #" + p.getIdProducto());
        txtNombre.setText(p.getNombre());
        txtCategoria.setText(p.getCategoria());
        txtPrecioCompra.setText(p.getPrecioCompra() != null ? p.getPrecioCompra().toString() : "0.00");
        txtPrecioVenta.setText(p.getPrecioVenta() != null ? p.getPrecioVenta().toString() : "0.00");
        txtStockActual.setText(String.valueOf(p.getStockActual()));
        txtStockMinimo.setText(String.valueOf(p.getStockMinimo()));
        chkActivo.setSelected(p.isActivo());
        btnGuardar.setText("Actualizar");
    }

    @FXML
    public void handleNuevoProducto() {
        handleLimpiarForm();
        txtNombre.requestFocus();
    }

    @FXML
    public void handleLimpiarForm() {
        productoSeleccionado = null;
        lblTituloForm.setText("Nuevo Producto");
        txtNombre.clear();
        txtCategoria.setText("GENERAL");
        txtPrecioCompra.setText("0.00");
        txtPrecioVenta.setText("0.00");
        txtStockActual.setText("0");
        txtStockMinimo.setText("5");
        chkActivo.setSelected(true);
        btnGuardar.setText("Guardar Producto");
        tblProductos.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleGuardarProducto() {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String categoria = txtCategoria.getText() != null && !txtCategoria.getText().trim().isEmpty()
                ? txtCategoria.getText().trim().toUpperCase() : "GENERAL";

        if (nombre.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo Requerido", "El nombre del producto es obligatorio.");
            txtNombre.requestFocus();
            return;
        }

        BigDecimal pCompra;
        BigDecimal pVenta;
        int stock;
        int stockMin;

        try {
            pCompra = new BigDecimal(txtPrecioCompra.getText().trim().replace(",", "."));
            pVenta = new BigDecimal(txtPrecioVenta.getText().trim().replace(",", "."));
            stock = Integer.parseInt(txtStockActual.getText().trim());
            stockMin = Integer.parseInt(txtStockMinimo.getText().trim());

            if (pCompra.compareTo(BigDecimal.ZERO) < 0 || pVenta.compareTo(BigDecimal.ZERO) < 0) {
                AlertaUtil.mostrarAdvertencia("Valores Inválidos", "Los precios no pueden ser negativos.");
                return;
            }
        } catch (Exception e) {
            AlertaUtil.mostrarAdvertencia("Formato Incorrecto", "Por favor revisa los números de precios y cantidades de stock.");
            return;
        }

        try {
            if (productoSeleccionado == null) {
                // Nuevo producto
                Producto nuevo = new Producto(0, nombre, categoria, pCompra, pVenta, stock, stockMin, chkActivo.isSelected());
                boolean exito = productoDAO.insertar(nuevo);
                if (exito) {
                    AlertaUtil.mostrarInfo("Producto Guardado", "Se agregó '" + nuevo.getNombre() + "' al inventario.");
                    cargarDatos();
                    handleLimpiarForm();
                }
            } else {
                // Actualizar existente
                productoSeleccionado.setNombre(nombre);
                productoSeleccionado.setCategoria(categoria);
                productoSeleccionado.setPrecioCompra(pCompra);
                productoSeleccionado.setPrecioVenta(pVenta);
                productoSeleccionado.setStockActual(stock);
                productoSeleccionado.setStockMinimo(stockMin);
                productoSeleccionado.setActivo(chkActivo.isSelected());

                boolean exito = productoDAO.actualizar(productoSeleccionado);
                if (exito) {
                    AlertaUtil.mostrarInfo("Actualización Exitosa", "Se actualizaron los datos del producto.");
                    cargarDatos();
                    handleLimpiarForm();
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Guardar", "Error en base de datos: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportarExcel() {
        if (listaProductos.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin Datos", "No hay productos en la lista para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte de Inventario para Excel");
        fileChooser.setInitialFileName("Inventario_Papeleria_Rosita.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV para Excel (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // Escribir BOM UTF-8 para que Excel lo abra con tildes y caracteres correctos
            pw.write('\ufeff');

            // Encabezados separados por punto y coma (estandar de Excel en espanol)
            pw.println("ID;Nombre;Categoría;Precio Compra;Precio Venta;Stock Actual;Stock Mínimo;Estado");

            for (Producto p : listaProductos) {
                pw.printf(Locale.US, "%d;\"%s\";\"%s\";%.2f;%.2f;%d;%d;%s%n",
                        p.getIdProducto(),
                        p.getNombre().replace("\"", "\"\""),
                        p.getCategoria().replace("\"", "\"\""),
                        p.getPrecioCompra().doubleValue(),
                        p.getPrecioVenta().doubleValue(),
                        p.getStockActual(),
                        p.getStockMinimo(),
                        p.isActivo() ? "Activo" : "Inactivo"
                );
            }

            AlertaUtil.mostrarInfo("Exportación Exitosa", "El inventario fue exportado con éxito a:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            AlertaUtil.mostrarError("Error al Exportar", "Ocurrió un error al generar el archivo:\n" + e.getMessage());
        }
    }
}

