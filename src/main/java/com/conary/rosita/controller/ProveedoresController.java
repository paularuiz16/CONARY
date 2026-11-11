package com.conary.rosita.controller;

import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.PagoProveedorDAO;
import com.conary.rosita.dao.ProveedorDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.PagoProveedor;
import com.conary.rosita.model.Proveedor;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controlador para la gestion de proveedores y cuentas por pagar.
 */
public class ProveedoresController {

    @FXML
    private TextField txtBuscar;
    @FXML
    private Label lblTotalDeuda;

    @FXML
    private TableView<Proveedor> tblProveedores;
    @FXML
    private TableColumn<Proveedor, Integer> colId;
    @FXML
    private TableColumn<Proveedor, String> colNombre;
    @FXML
    private TableColumn<Proveedor, String> colTelefono;
    @FXML
    private TableColumn<Proveedor, String> colDireccion;
    @FXML
    private TableColumn<Proveedor, String> colProductos;
    @FXML
    private TableColumn<Proveedor, String> colSaldo;

    @FXML
    private Label lblTituloForm;
    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtTelefono;
    @FXML
    private TextField txtDireccion;
    @FXML
    private TextArea txtProductos;
    @FXML
    private CheckBox chkActivo;
    @FXML
    private Button btnGuardar;

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final PagoProveedorDAO pagoDAO = new PagoProveedorDAO();
    private final CajaDAO cajaDAO = new CajaDAO();

    private final ObservableList<Proveedor> listaProveedores = FXCollections.observableArrayList();
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private Proveedor proveedorSeleccionado;

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarDatos();

        txtBuscar.textProperty().addListener((obs, viejo, nuevo) -> filtrarProveedores());

        tblProveedores.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null) {
                cargarProveedorEnFormulario(nuevo);
            }
        });
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getIdProveedor()));
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colTelefono.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTelefono() != null ? data.getValue().getTelefono() : "-"));
        colDireccion.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDireccion() != null ? data.getValue().getDireccion() : "-"));
        colProductos.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getProductos() != null ? data.getValue().getProductos() : "-"));
        colSaldo.setCellValueFactory(
                data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getSaldoPendiente())));

        colSaldo.setCellFactory(column -> new TableCell<Proveedor, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Proveedor p = getTableView().getItems().get(getIndex());
                    if (p.getSaldoPendiente() != null && p.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
                        setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #15803d;");
                    }
                }
            }
        });

        tblProveedores.setItems(listaProveedores);
    }

    private void cargarDatos() {
        try {
            List<Proveedor> lista = proveedorDAO.listarTodos(false);
            listaProveedores.setAll(lista);
            actualizarTotalCuentasPorPagar(lista);
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error", "No se pudo cargar la lista de proveedores: " + e.getMessage());
        }
    }

    private void actualizarTotalCuentasPorPagar(List<Proveedor> lista) {
        BigDecimal total = BigDecimal.ZERO;
        for (Proveedor p : lista) {
            if (p.getSaldoPendiente() != null) {
                total = total.add(p.getSaldoPendiente());
            }
        }
        lblTotalDeuda.setText("Total Cuentas por Pagar: " + formatoMoneda.format(total));
    }

    private void filtrarProveedores() {
        String criterio = txtBuscar.getText() != null ? txtBuscar.getText().trim().toLowerCase() : "";
        if (criterio.isEmpty()) {
            cargarDatos();
            return;
        }

        try {
            List<Proveedor> todos = proveedorDAO.listarTodos(false);
            ObservableList<Proveedor> filtrados = FXCollections.observableArrayList();
            for (Proveedor p : todos) {
                if (p.getNombre().toLowerCase().contains(criterio) ||
                        (p.getProductos() != null && p.getProductos().toLowerCase().contains(criterio))) {
                    filtrados.add(p);
                }
            }
            listaProveedores.setAll(filtrados);
            actualizarTotalCuentasPorPagar(filtrados);
        } catch (SQLException e) {
            System.err.println("[ProveedoresController] Error al filtrar: " + e.getMessage());
        }
    }

    private void cargarProveedorEnFormulario(Proveedor p) {
        proveedorSeleccionado = p;
        lblTituloForm.setText("Editar Proveedor #" + p.getIdProveedor());
        txtNombre.setText(p.getNombre());
        txtTelefono.setText(p.getTelefono() != null ? p.getTelefono() : "");
        txtDireccion.setText(p.getDireccion() != null ? p.getDireccion() : "");
        txtProductos.setText(p.getProductos() != null ? p.getProductos() : "");
        chkActivo.setSelected(p.isActivo());
        btnGuardar.setText("Actualizar");
    }

    @FXML
    public void handleNuevoProveedor() {
        handleLimpiarForm();
        txtNombre.requestFocus();
    }

    @FXML
    public void handleLimpiarForm() {
        proveedorSeleccionado = null;
        lblTituloForm.setText("Nuevo Proveedor");
        txtNombre.clear();
        txtTelefono.clear();
        txtDireccion.clear();
        txtProductos.clear();
        chkActivo.setSelected(true);
        btnGuardar.setText("Guardar Proveedor");
        tblProveedores.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleGuardarProveedor() {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String telefono = txtTelefono.getText() != null ? txtTelefono.getText().trim() : "";
        String direccion = txtDireccion.getText() != null ? txtDireccion.getText().trim() : "";
        String productos = txtProductos.getText() != null ? txtProductos.getText().trim() : "";

        if (nombre.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo Requerido", "El nombre del proveedor es obligatorio.");
            txtNombre.requestFocus();
            return;
        }

        try {
            if (proveedorSeleccionado == null) {
                Proveedor nuevo = new Proveedor(0, nombre, telefono, direccion, productos, chkActivo.isSelected());
                boolean exito = proveedorDAO.insertar(nuevo);
                if (exito) {
                    AlertaUtil.mostrarInfo("Proveedor Guardado",
                            "Se registró el proveedor '" + nuevo.getNombre() + "' con éxito.");
                    cargarDatos();
                    handleLimpiarForm();
                }
            } else {
                proveedorSeleccionado.setNombre(nombre);
                proveedorSeleccionado.setTelefono(telefono);
                proveedorSeleccionado.setDireccion(direccion);
                proveedorSeleccionado.setProductos(productos);
                proveedorSeleccionado.setActivo(chkActivo.isSelected());

                boolean exito = proveedorDAO.actualizar(proveedorSeleccionado);
                if (exito) {
                    AlertaUtil.mostrarInfo("Actualización Exitosa", "Se actualizaron los datos del proveedor.");
                    cargarDatos();
                    handleLimpiarForm();
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error", "Error en base de datos: " + e.getMessage());
        }
    }

    @FXML
    public void handleRegistrarPago() {
        Proveedor seleccionado = tblProveedores.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selecciona un Proveedor",
                    "Debes seleccionar de la tabla el proveedor al cual se va a pagar.");
            return;
        }

        if (seleccionado.getSaldoPendiente() == null
                || seleccionado.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
            AlertaUtil.mostrarInfo("Sin Deuda", "No hay cuentas pendientes por pagar con este proveedor.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(seleccionado.getSaldoPendiente().toString());
        dialog.setTitle("Registrar Pago a Proveedor");
        dialog.setHeaderText("Proveedor: " + seleccionado.getNombre() + "\nDeuda pendiente: "
                + formatoMoneda.format(seleccionado.getSaldoPendiente()));
        dialog.setContentText("Monto a pagar ($):");

        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isPresent()) {
            try {
                BigDecimal monto = new BigDecimal(resultado.get().trim().replace(",", "."));
                if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                    AlertaUtil.mostrarAdvertencia("Monto Inválido", "El monto a pagar debe ser mayor a 0.");
                    return;
                }

                Usuario usuario = SesionUsuario.getUsuarioLogueado();
                Integer idCaja = null;
                try {
                    Caja caja = cajaDAO.obtenerCajaAbierta();
                    if (caja != null)
                        idCaja = caja.getIdCaja();
                } catch (Exception ignored) {
                }

                PagoProveedor pago = new PagoProveedor(
                        "Pago a proveedor " + seleccionado.getNombre(),
                        usuario != null ? usuario.getIdUsuario() : 1,
                        idCaja,
                        seleccionado.getIdProveedor(),
                        monto);

                boolean exito = pagoDAO.registrarPago(pago);
                if (exito) {
                    AlertaUtil.mostrarInfo("Pago Registrado",
                            "Se registró el pago de " + formatoMoneda.format(monto) + " al proveedor.");
                    cargarDatos();
                }
            } catch (Exception e) {
                AlertaUtil.mostrarError("Error", "Ocurrió un error al registrar el pago:\n" + e.getMessage());
            }
        }
    }
}
