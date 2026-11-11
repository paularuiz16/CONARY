package com.conary.rosita.controller;

import com.conary.rosita.dao.AbonoClienteDAO;
import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.model.AbonoCliente;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Cliente;
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
 * Controlador para la administracion de clientes y cuentas fiadas.
 */
public class ClientesController {

    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkSoloDeudores;
    @FXML private Label lblTotalDeuda;

    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colDireccion;
    @FXML private TableColumn<Cliente, String> colFiado;
    @FXML private TableColumn<Cliente, String> colLimite;
    @FXML private TableColumn<Cliente, String> colSaldo;

    @FXML private Label lblTituloForm;
    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;
    @FXML private CheckBox chkFiadoAutorizado;
    @FXML private TextField txtLimiteCredito;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final AbonoClienteDAO abonoDAO = new AbonoClienteDAO();
    private final CajaDAO cajaDAO = new CajaDAO();

    private final ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private Cliente clienteSeleccionado;

    @FXML
    public void initialize() {
        configurarColumnas();
        cargarDatos();

        txtBuscar.textProperty().addListener((obs, viejo, nuevo) -> filtrarClientes());
        chkSoloDeudores.selectedProperty().addListener((obs, viejo, nuevo) -> filtrarClientes());

        tblClientes.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) -> {
            if (nuevo != null) {
                cargarClienteEnFormulario(nuevo);
            }
        });
    }

    private void configurarColumnas() {
        colId.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getIdCliente()));
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colTelefono.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTelefono() != null ? data.getValue().getTelefono() : "-"));
        colDireccion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDireccion() != null ? data.getValue().getDireccion() : "-"));
        colFiado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isFiadoAutorizado() ? "SÍ" : "NO"));
        colLimite.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getLimiteCredito())));
        colSaldo.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getSaldoPendiente())));

        colSaldo.setCellFactory(column -> new TableCell<Cliente, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Cliente c = getTableView().getItems().get(getIndex());
                    if (c.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
                        setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #15803d;");
                    }
                }
            }
        });

        tblClientes.setItems(listaClientes);
    }

    private void cargarDatos() {
        try {
            List<Cliente> lista = clienteDAO.listarTodos(false);
            listaClientes.setAll(lista);
            actualizarTotalCartera(lista);
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error", "No se pudo cargar la lista de clientes: " + e.getMessage());
        }
    }

    private void actualizarTotalCartera(List<Cliente> lista) {
        BigDecimal total = BigDecimal.ZERO;
        for (Cliente c : lista) {
            if (c.getSaldoPendiente() != null) {
                total = total.add(c.getSaldoPendiente());
            }
        }
        lblTotalDeuda.setText("Total Cartera Fiada: " + formatoMoneda.format(total));
    }

    private void filtrarClientes() {
        String criterio = txtBuscar.getText() != null ? txtBuscar.getText().trim() : "";
        boolean soloDeudores = chkSoloDeudores.isSelected();

        try {
            List<Cliente> lista;
            if (soloDeudores) {
                lista = clienteDAO.listarConSaldosPendientes();
            } else if (!criterio.isEmpty()) {
                lista = clienteDAO.buscar(criterio, false);
            } else {
                lista = clienteDAO.listarTodos(false);
            }
            listaClientes.setAll(lista);
            actualizarTotalCartera(lista);
        } catch (SQLException e) {
            System.err.println("[ClientesController] Error al filtrar: " + e.getMessage());
        }
    }

    private void cargarClienteEnFormulario(Cliente c) {
        clienteSeleccionado = c;
        lblTituloForm.setText("Editar Cliente #" + c.getIdCliente());
        txtNombre.setText(c.getNombre());
        txtTelefono.setText(c.getTelefono() != null ? c.getTelefono() : "");
        txtDireccion.setText(c.getDireccion() != null ? c.getDireccion() : "");
        chkFiadoAutorizado.setSelected(c.isFiadoAutorizado());
        txtLimiteCredito.setText(c.getLimiteCredito() != null ? c.getLimiteCredito().toString() : "0.00");
        chkActivo.setSelected(c.isActivo());
        btnGuardar.setText("Actualizar");
    }

    @FXML
    public void handleNuevoCliente() {
        handleLimpiarForm();
        txtNombre.requestFocus();
    }

    @FXML
    public void handleLimpiarForm() {
        clienteSeleccionado = null;
        lblTituloForm.setText("Nuevo Cliente");
        txtNombre.clear();
        txtTelefono.clear();
        txtDireccion.clear();
        chkFiadoAutorizado.setSelected(false);
        txtLimiteCredito.setText("0.00");
        chkActivo.setSelected(true);
        btnGuardar.setText("Guardar Cliente");
        tblClientes.getSelectionModel().clearSelection();
    }

    @FXML
    public void handleGuardarCliente() {
        String nombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String telefono = txtTelefono.getText() != null ? txtTelefono.getText().trim() : "";
        String direccion = txtDireccion.getText() != null ? txtDireccion.getText().trim() : "";

        if (nombre.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo Requerido", "El nombre del cliente es obligatorio.");
            txtNombre.requestFocus();
            return;
        }

        BigDecimal limite;
        try {
            limite = new BigDecimal(txtLimiteCredito.getText().trim().replace(",", "."));
            if (limite.compareTo(BigDecimal.ZERO) < 0) throw new Exception();
        } catch (Exception e) {
            AlertaUtil.mostrarAdvertencia("Límite Inválido", "Ingresa un número positivo válido para el límite de crédito.");
            return;
        }

        try {
            if (clienteSeleccionado == null) {
                Cliente nuevo = new Cliente(0, nombre, telefono, direccion, chkActivo.isSelected(), chkFiadoAutorizado.isSelected(), limite);
                boolean exito = clienteDAO.insertar(nuevo);
                if (exito) {
                    AlertaUtil.mostrarInfo("Cliente Registrado", "Se guardó a '" + nuevo.getNombre() + "' con éxito.");
                    cargarDatos();
                    handleLimpiarForm();
                }
            } else {
                clienteSeleccionado.setNombre(nombre);
                clienteSeleccionado.setTelefono(telefono);
                clienteSeleccionado.setDireccion(direccion);
                clienteSeleccionado.setFiadoAutorizado(chkFiadoAutorizado.isSelected());
                clienteSeleccionado.setLimiteCredito(limite);
                clienteSeleccionado.setActivo(chkActivo.isSelected());

                boolean exito = clienteDAO.actualizar(clienteSeleccionado);
                if (exito) {
                    AlertaUtil.mostrarInfo("Actualización Exitosa", "Se actualizaron los datos del cliente.");
                    cargarDatos();
                    handleLimpiarForm();
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error", "Error en base de datos: " + e.getMessage());
        }
    }

    @FXML
    public void handleRegistrarAbono() {
        Cliente seleccionado = tblClientes.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selecciona un Cliente", "Debes seleccionar de la tabla el cliente que va a abonar.");
            return;
        }

        if (seleccionado.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
            AlertaUtil.mostrarInfo("Sin Deuda", "El cliente '" + seleccionado.getNombre() + "' se encuentra a paz y salvo.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(seleccionado.getSaldoPendiente().toString());
        dialog.setTitle("Registrar Abono a Fiado");
        dialog.setHeaderText("Cliente: " + seleccionado.getNombre() + "\nDeuda pendiente: " + formatoMoneda.format(seleccionado.getSaldoPendiente()));
        dialog.setContentText("Monto a abonar ($):");

        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isPresent()) {
            try {
                BigDecimal monto = new BigDecimal(resultado.get().trim().replace(",", "."));
                if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                    AlertaUtil.mostrarAdvertencia("Monto Inválido", "El monto de abono debe ser mayor a 0.");
                    return;
                }

                Usuario usuario = SesionUsuario.getUsuarioLogueado();
                Integer idCaja = null;
                try {
                    Caja caja = cajaDAO.obtenerCajaAbierta();
                    if (caja != null) idCaja = caja.getIdCaja();
                } catch (Exception ignored) {}

                AbonoCliente abono = new AbonoCliente(
                        "Abono a deuda de " + seleccionado.getNombre(),
                        usuario != null ? usuario.getIdUsuario() : 1,
                        idCaja,
                        seleccionado.getIdCliente(),
                        monto
                );

                boolean exito = abonoDAO.registrarAbono(abono);
                if (exito) {
                    AlertaUtil.mostrarInfo("Abono Registrado", "Se registró el abono de " + formatoMoneda.format(monto) + " con éxito.");
                    cargarDatos();
                }
            } catch (Exception e) {
                AlertaUtil.mostrarError("Error", "Ocurrió un error al registrar el abono:\n" + e.getMessage());
            }
        }
    }
}

