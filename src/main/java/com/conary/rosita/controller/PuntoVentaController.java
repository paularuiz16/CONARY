package com.conary.rosita.controller;

import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.dao.VentaDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Cliente;
import com.conary.rosita.model.DetalleVenta;
import com.conary.rosita.model.Producto;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.model.Venta;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del Punto de Venta (Facturacion en mostrador y control de fiados).
 */
public class PuntoVentaController {

    @FXML private ComboBox<Producto> cmbProductos;
    @FXML private TextField txtCantidad;
    @FXML private Button btnAgregarItem;

    @FXML private TableView<DetalleVenta> tblCarrito;
    @FXML private TableColumn<DetalleVenta, String> colProducto;
    @FXML private TableColumn<DetalleVenta, String> colPrecio;
    @FXML private TableColumn<DetalleVenta, Integer> colCantidad;
    @FXML private TableColumn<DetalleVenta, String> colSubtotal;
    @FXML private TableColumn<DetalleVenta, Void> colAccion;

    @FXML private RadioButton rbContado;
    @FXML private RadioButton rbFiado;
    @FXML private ComboBox<Cliente> cmbClientes;
    @FXML private Label lblInfoCredito;
    @FXML private Label lblTotalVenta;
    @FXML private Button btnCobrar;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final VentaDAO ventaDAO = new VentaDAO();
    private final CajaDAO cajaDAO = new CajaDAO();

    private final ObservableList<DetalleVenta> itemsCarrito = FXCollections.observableArrayList();
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        configurarTabla();
        configurarRadioButtons();
        cargarProductos();
        cargarClientes();

        txtCantidad.setOnAction(e -> handleAgregarItem());
    }

    private void configurarTabla() {
        colProducto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombreProducto()));
        colPrecio.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getValorUnitario())));
        colCantidad.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getCantidad()));
        colSubtotal.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getSubtotal())));

        colAccion.setCellFactory(col -> new TableCell<DetalleVenta, Void>() {
            private final Button btnEliminar = new Button("✕");
            {
                btnEliminar.getStyleClass().add("button-danger");
                btnEliminar.setStyle("-fx-padding: 3 8 3 8; -fx-font-size: 11px;");
                btnEliminar.setOnAction(e -> {
                    DetalleVenta item = getTableView().getItems().get(getIndex());
                    itemsCarrito.remove(item);
                    actualizarTotal();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnEliminar);
            }
        });

        tblCarrito.setItems(itemsCarrito);
    }

    private void configurarRadioButtons() {
        ToggleGroup grupoPago = new ToggleGroup();
        rbContado.setToggleGroup(grupoPago);
        rbFiado.setToggleGroup(grupoPago);

        grupoPago.selectedToggleProperty().addListener((obs, viejo, nuevo) -> {
            boolean esFiado = rbFiado.isSelected();
            if (esFiado) {
                actualizarInfoClienteFiado();
            } else {
                lblInfoCredito.setText("");
            }
        });

        cmbClientes.valueProperty().addListener((obs, viejo, nuevo) -> {
            if (rbFiado.isSelected()) {
                actualizarInfoClienteFiado();
            }
        });
    }

    private void cargarProductos() {
        try {
            List<Producto> lista = productoDAO.listarTodos(true);
            cmbProductos.setItems(FXCollections.observableArrayList(lista));
            cmbProductos.setConverter(new StringConverter<Producto>() {
                @Override
                public String toString(Producto p) {
                    if (p == null) return "";
                    return p.getNombre() + " (" + formatoMoneda.format(p.getPrecioVenta()) + " - Stock: " + p.getStockActual() + ")";
                }
                @Override
                public Producto fromString(String string) {
                    return null;
                }
            });
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error", "No se pudo cargar la lista de productos: " + e.getMessage());
        }
    }

    private void cargarClientes() {
        try {
            List<Cliente> lista = clienteDAO.listarTodos(true);
            cmbClientes.setItems(FXCollections.observableArrayList(lista));
            cmbClientes.setConverter(new StringConverter<Cliente>() {
                @Override
                public String toString(Cliente c) {
                    if (c == null) return "";
                    return c.getNombre() + (c.isFiadoAutorizado() ? " [CRÉDITO AUTORIZADO]" : "");
                }
                @Override
                public Cliente fromString(String string) {
                    return null;
                }
            });
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error", "No se pudo cargar la lista de clientes: " + e.getMessage());
        }
    }

    @FXML
    public void handleAgregarItem() {
        Producto producto = cmbProductos.getValue();
        if (producto == null) {
            AlertaUtil.mostrarAdvertencia("Atención", "Por favor selecciona un producto de la lista.");
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            AlertaUtil.mostrarAdvertencia("Cantidad Inválida", "Ingresa un número entero positivo mayor a 0.");
            txtCantidad.requestFocus();
            return;
        }

        // Validar stock disponible
        if (cantidad > producto.getStockActual()) {
            AlertaUtil.mostrarAdvertencia("Stock Insuficiente",
                    "El producto '" + producto.getNombre() + "' solo cuenta con " + producto.getStockActual() + " unidades en existencia.");
            return;
        }

        // Revisar si ya esta en el carrito para sumar la cantidad
        boolean encontrado = false;
        for (DetalleVenta item : itemsCarrito) {
            if (item.getIdProducto() == producto.getIdProducto()) {
                int nuevaCantidad = item.getCantidad() + cantidad;
                if (nuevaCantidad > producto.getStockActual()) {
                    AlertaUtil.mostrarAdvertencia("Stock Excedido",
                            "La cantidad acumulada en el carrito supera el stock actual disponible (" + producto.getStockActual() + ").");
                    return;
                }
                item.setCantidad(nuevaCantidad);
                item.recalcularSubtotal();
                tblCarrito.refresh();
                encontrado = true;
                break;
            }
        }

        if (!encontrado) {
            DetalleVenta detalle = new DetalleVenta(
                    producto.getIdProducto(),
                    producto.getNombre(),
                    cantidad,
                    producto.getPrecioVenta()
            );
            itemsCarrito.add(detalle);
        }

        txtCantidad.setText("1");
        cmbProductos.getSelectionModel().clearSelection();
        cmbProductos.requestFocus();

        actualizarTotal();
    }

    private void actualizarTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVenta item : itemsCarrito) {
            total = total.add(item.getSubtotal());
        }
        lblTotalVenta.setText(formatoMoneda.format(total));
    }

    private void actualizarInfoClienteFiado() {
        Cliente cliente = cmbClientes.getValue();
        if (cliente == null) {
            lblInfoCredito.setText("⚠️ Para ventas a crédito (fiado) debes seleccionar un cliente.");
            lblInfoCredito.setStyle("-fx-text-fill: #b45309; -fx-font-size: 11px;");
            return;
        }

        if (!cliente.isFiadoAutorizado()) {
            lblInfoCredito.setText("❌ El cliente '" + cliente.getNombre() + "' NO tiene crédito autorizado.");
            lblInfoCredito.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 11px;");
            return;
        }

        lblInfoCredito.setText("Límite: " + formatoMoneda.format(cliente.getLimiteCredito())
                + " | Deuda actual: " + formatoMoneda.format(cliente.getSaldoPendiente())
                + " | Disponible: " + formatoMoneda.format(cliente.getCreditoDisponible()));
        lblInfoCredito.setStyle("-fx-text-fill: #0369a1; -fx-font-size: 11px;");
    }

    @FXML
    public void handleRegistrarVenta() {
        if (itemsCarrito.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Carrito Vacío", "No hay productos en el carrito para registrar la venta.");
            return;
        }

        Usuario usuario = SesionUsuario.getUsuarioLogueado();
        if (usuario == null) {
            AlertaUtil.mostrarError("Sesión Inválida", "No hay un usuario activo. Reinicia la aplicación.");
            return;
        }

        boolean esFiado = rbFiado.isSelected();
        Cliente cliente = cmbClientes.getValue();

        // Validaciones de credito si es fiado
        if (esFiado) {
            if (cliente == null) {
                AlertaUtil.mostrarAdvertencia("Cliente Requerido", "Debes seleccionar un cliente para registrar una venta a crédito (fiado).");
                return;
            }
            if (!cliente.isFiadoAutorizado()) {
                AlertaUtil.mostrarAdvertencia("Fiado No Permitido", "El cliente seleccionado no tiene autorización para compras a crédito.");
                return;
            }

            BigDecimal totalVenta = BigDecimal.ZERO;
            for (DetalleVenta d : itemsCarrito) totalVenta = totalVenta.add(d.getSubtotal());

            if (totalVenta.compareTo(cliente.getCreditoDisponible()) > 0) {
                boolean autorizar = AlertaUtil.mostrarConfirmacion("Límite de Crédito Superado",
                        "El total de la venta (" + formatoMoneda.format(totalVenta) + ") supera el crédito disponible del cliente ("
                      + formatoMoneda.format(cliente.getCreditoDisponible()) + ").\n\n¿Deseas autorizar la venta de todas maneras?");
                if (!autorizar) return;
            }
        }

        // Obtener turno de caja abierto
        Integer idCaja = null;
        try {
            Caja cajaAbierta = cajaDAO.obtenerCajaAbierta();
            if (cajaAbierta != null) {
                idCaja = cajaAbierta.getIdCaja();
            } else if (!esFiado) {
                AlertaUtil.mostrarAdvertencia("Caja Cerrada",
                        "Actualmente no hay ningún turno de caja abierto.\n"
                      + "Se registrará la venta, pero recuerda abrir la caja para el control del arqueo.");
            }
        } catch (SQLException e) {
            System.err.println("[PuntoVentaController] Error verificando caja: " + e.getMessage());
        }

        // Construir venta
        Venta venta = new Venta();
        venta.setIdUsuario(usuario.getIdUsuario());
        venta.setIdCaja(idCaja);
        venta.setEsFiado(esFiado);
        if (cliente != null) {
            venta.setIdCliente(cliente.getIdCliente());
            venta.setNombreCliente(cliente.getNombre());
        }
        venta.setDescripcion(esFiado ? "Venta fiada a " + (cliente != null ? cliente.getNombre() : "") : "Venta contado mostrador");

        for (DetalleVenta item : itemsCarrito) {
            venta.agregarDetalle(item);
        }

        try {
            boolean exito = ventaDAO.registrarVenta(venta);
            if (exito) {
                AlertaUtil.mostrarInfo("Venta Exitosa",
                        "Venta #" + venta.getIdMovimiento() + " registrada correctamente.\nTotal: " + formatoMoneda.format(venta.getTotalVenta()));
                handleLimpiarVenta();
                cargarProductos(); // Recargar stock actualizado
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Registrar Venta", "Ocurrió un error en la base de datos:\n" + e.getMessage());
        }
    }

    @FXML
    public void handleLimpiarVenta() {
        itemsCarrito.clear();
        actualizarTotal();
        rbContado.setSelected(true);
        cmbClientes.getSelectionModel().clearSelection();
        lblInfoCredito.setText("");
        txtCantidad.setText("1");
    }
}

