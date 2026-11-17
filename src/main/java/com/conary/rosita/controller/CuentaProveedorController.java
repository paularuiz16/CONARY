package com.conary.rosita.controller;

import com.conary.rosita.dao.CompraDAO;
import com.conary.rosita.dao.PagoProveedorDAO;
import com.conary.rosita.dao.ProveedorDAO;
import com.conary.rosita.model.Compra;
import com.conary.rosita.model.PagoProveedor;
import com.conary.rosita.model.Proveedor;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista B de la Tanda 2 del Modulo 5: Cuenta del Proveedor ("Cuenta de [Nombre]").
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class CuentaProveedorController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static Proveedor proveedorPendienteCuenta;

    public static void setProveedorPendienteCuenta(Proveedor proveedor) {
        proveedorPendienteCuenta = proveedor;
    }

    public static class ItemMovimientoProveedor {
        private final int idMovimiento;
        private final LocalDateTime fecha;
        private final boolean esPago;
        private final String factura;
        private final BigDecimal valor;
        private BigDecimal debiaDespues;

        public ItemMovimientoProveedor(int idMovimiento, LocalDateTime fecha, boolean esPago,
                                       String factura, BigDecimal valor) {
            this.idMovimiento = idMovimiento;
            this.fecha = fecha != null ? fecha : LocalDateTime.now();
            this.esPago = esPago;
            this.factura = factura;
            this.valor = valor != null ? valor : BigDecimal.ZERO;
            this.debiaDespues = BigDecimal.ZERO;
        }

        public int getIdMovimiento() { return idMovimiento; }
        public LocalDateTime getFecha() { return fecha; }
        public boolean isEsPago() { return esPago; }
        public String getFactura() { return factura; }
        public BigDecimal getValor() { return valor; }
        public BigDecimal getDebiaDespues() { return debiaDespues; }
        public void setDebiaDespues(BigDecimal d) { this.debiaDespues = d != null ? d : BigDecimal.ZERO; }
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverTop;
    @FXML private Label lblTituloCuenta;
    @FXML private Label lblSubtituloProveedor;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;

    @FXML private Label lblLeDebo;
    @FXML private Label lblProximoPago;
    @FXML private Label lblCompradoAnio;

    @FXML private Label lblConteoMovimientos;
    @FXML private TableView<ItemMovimientoProveedor> tblMovimientosProveedor;
    @FXML private TableColumn<ItemMovimientoProveedor, ItemMovimientoProveedor> colFecha;
    @FXML private TableColumn<ItemMovimientoProveedor, ItemMovimientoProveedor> colQueFue;
    @FXML private TableColumn<ItemMovimientoProveedor, ItemMovimientoProveedor> colFactura;
    @FXML private TableColumn<ItemMovimientoProveedor, ItemMovimientoProveedor> colValor;
    @FXML private TableColumn<ItemMovimientoProveedor, ItemMovimientoProveedor> colDebiaDespues;

    @FXML private Button btnVolverBottom;
    @FXML private Button btnRegistrarCompra;
    @FXML private Button btnRegistrarPago;

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final CompraDAO compraDAO = new CompraDAO();
    private final PagoProveedorDAO pagoProveedorDAO = new PagoProveedorDAO();

    private final ObservableList<ItemMovimientoProveedor> listaMovimientos = FXCollections.observableArrayList();
    private Proveedor proveedorActual;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarTablaMovimientos();
        configurarRestriccionesPorRol();

        if (proveedorPendienteCuenta != null) {
            setProveedor(proveedorPendienteCuenta);
        }
    }

    private void configurarEncabezadoUsuario() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        String nombre = "Doña Rosita";
        String rol = "Administradora";

        if (u != null) {
            String raw = u.getNombre() != null ? u.getNombre().trim() : "Usuario";
            if (raw.toLowerCase(LOCALE_ES_CO).contains("rosita")
                    && !raw.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
                nombre = "Doña " + raw;
            } else {
                nombre = raw;
            }
            rol = u.esAdministrador() ? "Administradora" : "Vendedora";
        }

        if (lblUsuarioNombre != null) lblUsuarioNombre.setText(nombre);
        if (lblUsuarioRol != null) lblUsuarioRol.setText(rol);
    }

    private void configurarRestriccionesPorRol() {
        boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
        if (btnRegistrarCompra != null) {
            btnRegistrarCompra.setVisible(esAdmin);
            btnRegistrarCompra.setManaged(esAdmin);
        }
        if (btnRegistrarPago != null) {
            btnRegistrarPago.setVisible(esAdmin);
            btnRegistrarPago.setManaged(esAdmin);
        }
    }

    private void configurarTablaMovimientos() {
        tblMovimientosProveedor.setItems(listaMovimientos);
        tblMovimientosProveedor.setFixedCellSize(56.0);

        tblMovimientosProveedor.setRowFactory(tv -> new TableRow<ItemMovimientoProveedor>() {
            @Override
            protected void updateItem(ItemMovimientoProveedor item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(56.0);
                setMinHeight(56.0);
                setMaxHeight(56.0);
            }
        });

        colFecha.setCellValueFactory(data -> new SimpleObjectProperty<ItemMovimientoProveedor>(data.getValue()));
        colFecha.setCellFactory(col -> new TableCell<ItemMovimientoProveedor, ItemMovimientoProveedor>() {
            @Override
            protected void updateItem(ItemMovimientoProveedor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(formatearDiaMes(item.getFecha().toLocalDate()));
                    getStyleClass().removeAll("cuenta-cliente-cell-fecha");
                    getStyleClass().add("cuenta-cliente-cell-fecha");
                }
            }
        });

        colQueFue.setCellValueFactory(data -> new SimpleObjectProperty<ItemMovimientoProveedor>(data.getValue()));
        colQueFue.setCellFactory(col -> new TableCell<ItemMovimientoProveedor, ItemMovimientoProveedor>() {
            @Override
            protected void updateItem(ItemMovimientoProveedor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label pill = new Label(item.isEsPago() ? "💵  Pago" : "🚚  Compra");
                    pill.getStyleClass().add("cuenta-proveedor-pill-mov");
                    HBox box = new HBox(pill);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        colFactura.setCellValueFactory(data -> new SimpleObjectProperty<ItemMovimientoProveedor>(data.getValue()));
        colFactura.setCellFactory(col -> new TableCell<ItemMovimientoProveedor, ItemMovimientoProveedor>() {
            @Override
            protected void updateItem(ItemMovimientoProveedor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String fac = item.getFactura() != null && !item.getFactura().trim().isEmpty()
                            ? item.getFactura().trim()
                            : "—";
                    setText(fac);
                    getStyleClass().removeAll("clientes-cell-telefono");
                    getStyleClass().add("clientes-cell-telefono");
                }
            }
        });

        colValor.setCellValueFactory(data -> new SimpleObjectProperty<ItemMovimientoProveedor>(data.getValue()));
        colValor.setCellFactory(col -> new TableCell<ItemMovimientoProveedor, ItemMovimientoProveedor>() {
            @Override
            protected void updateItem(ItemMovimientoProveedor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    String montoStr = formatearMoneda(item.getValor());
                    setText((item.isEsPago() ? "− $ " : "+ $ ") + montoStr);
                    getStyleClass().removeAll("cuenta-cliente-cell-abono", "cuenta-cliente-cell-venta");
                    getStyleClass().add(item.isEsPago() ? "cuenta-cliente-cell-abono" : "cuenta-cliente-cell-venta");
                }
            }
        });

        colDebiaDespues.setCellValueFactory(data -> new SimpleObjectProperty<ItemMovimientoProveedor>(data.getValue()));
        colDebiaDespues.setCellFactory(col -> new TableCell<ItemMovimientoProveedor, ItemMovimientoProveedor>() {
            @Override
            protected void updateItem(ItemMovimientoProveedor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    setText("$ " + formatearMoneda(item.getDebiaDespues()));
                    getStyleClass().removeAll("cuenta-cliente-cell-saldo");
                    getStyleClass().add("cuenta-cliente-cell-saldo");
                }
            }
        });
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedorActual = proveedor;
        if (proveedor == null) {
            return;
        }
        recargarCuentaDesdeMySQL();
    }

    public void recargarCuentaDesdeMySQL() {
        if (proveedorActual == null) {
            return;
        }

        try {
            Proveedor fresco = proveedorDAO.obtenerPorId(proveedorActual.getIdProveedor());
            if (fresco != null) {
                proveedorActual = fresco;
            }
        } catch (SQLException ignored) {
        }

        if (lblTituloCuenta != null) {
            lblTituloCuenta.setText("Cuenta de " + proveedorActual.getNombre());
        }
        if (lblSubtituloProveedor != null) {
            String tel = proveedorActual.getTelefono() != null && !proveedorActual.getTelefono().trim().isEmpty()
                    ? proveedorActual.getTelefono().trim()
                    : "Sin teléfono";
            String prod = proveedorActual.getProductos() != null && !proveedorActual.getProductos().trim().isEmpty()
                    ? proveedorActual.getProductos().trim()
                    : "Varios";
            lblSubtituloProveedor.setText("Teléfono: " + tel + " · Le compra: " + prod);
        }

        BigDecimal deudaActual = proveedorActual.getSaldoPendiente() != null
                ? proveedorActual.getSaldoPendiente()
                : BigDecimal.ZERO;

        if (lblLeDebo != null) {
            lblLeDebo.setText("$ " + formatearMoneda(deudaActual));
        }
        if (lblProximoPago != null) {
            if (deudaActual.compareTo(BigDecimal.ZERO) > 0 && proveedorActual.getProximoPago() != null) {
                lblProximoPago.setText(formatearDiaMes(proveedorActual.getProximoPago()));
            } else {
                lblProximoPago.setText("—");
            }
        }
        if (btnRegistrarPago != null) {
            btnRegistrarPago.setDisable(deudaActual.compareTo(BigDecimal.ZERO) <= 0);
        }

        List<ItemMovimientoProveedor> cronologicoAsc = new ArrayList<ItemMovimientoProveedor>();
        int anioActual = LocalDate.now().getYear();
        int facturasAnio = 0;

        try {
            List<Compra> compras = compraDAO.listarComprasPorProveedor(proveedorActual.getIdProveedor());
            if (compras != null) {
                for (Compra c : compras) {
                    if (c.getFecha() != null && c.getFecha().getYear() == anioActual) {
                        facturasAnio++;
                    }
                    cronologicoAsc.add(new ItemMovimientoProveedor(
                            c.getIdMovimiento(),
                            c.getFecha(),
                            false,
                            c.getNumeroFacturaProveedor(),
                            c.getTotalCompra()
                    ));
                }
            }

            List<PagoProveedor> pagos = pagoProveedorDAO.listarPagosPorProveedor(proveedorActual.getIdProveedor());
            if (pagos != null) {
                for (PagoProveedor pg : pagos) {
                    cronologicoAsc.add(new ItemMovimientoProveedor(
                            pg.getIdMovimiento(),
                            pg.getFecha(),
                            true,
                            null,
                            pg.getMonto()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CuentaProveedorController] Error consultando movimientos: " + e.getMessage());
        }

        if (lblCompradoAnio != null) {
            lblCompradoAnio.setText(facturasAnio == 1 ? "1 factura" : (facturasAnio + " facturas"));
        }

        // Ordenar de mas antiguo a mas nuevo para calcular "Le debia despues"
        Collections.sort(cronologicoAsc, new Comparator<ItemMovimientoProveedor>() {
            @Override
            public int compare(ItemMovimientoProveedor a, ItemMovimientoProveedor b) {
                int cmpFecha = a.getFecha().compareTo(b.getFecha());
                if (cmpFecha != 0) {
                    return cmpFecha;
                }
                return Integer.compare(a.getIdMovimiento(), b.getIdMovimiento());
            }
        });

        BigDecimal saldoAcumulado = BigDecimal.ZERO;
        for (ItemMovimientoProveedor mov : cronologicoAsc) {
            if (mov.isEsPago()) {
                saldoAcumulado = saldoAcumulado.subtract(mov.getValor());
                if (saldoAcumulado.compareTo(BigDecimal.ZERO) < 0) {
                    saldoAcumulado = BigDecimal.ZERO;
                }
            } else {
                saldoAcumulado = saldoAcumulado.add(mov.getValor());
            }
            mov.setDebiaDespues(saldoAcumulado);
        }

        Collections.reverse(cronologicoAsc);
        listaMovimientos.setAll(cronologicoAsc);

        if (lblConteoMovimientos != null) {
            int n = listaMovimientos.size();
            lblConteoMovimientos.setText(n == 1 ? "Último 1 registro" : ("Últimos " + n + " registros"));
        }
    }

    @FXML
    public void handleRegistrarCompra() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede registrar compras.");
            return;
        }
        if (proveedorActual == null) {
            return;
        }
        RegistrarCompraController.setProveedorInicial(proveedorActual);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            RegistrarCompraController ctrl = menu.cargarVistaConControlador("/fxml/registrar_compra.fxml");
            if (ctrl != null) {
                ctrl.precargarProveedor(proveedorActual);
            }
        }
    }

    @FXML
    public void handleRegistrarPago() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede registrar pagos a proveedores.");
            return;
        }
        if (proveedorActual == null) {
            return;
        }
        BigDecimal deuda = proveedorActual.getSaldoPendiente() != null
                ? proveedorActual.getSaldoPendiente()
                : BigDecimal.ZERO;
        if (deuda.compareTo(BigDecimal.ZERO) <= 0) {
            AlertaUtil.mostrarInfo("Al día", "No hay saldo pendiente con este proveedor.");
            return;
        }

        try {
            URL url = getClass().getResource("/fxml/modal_registrar_pago_proveedor.fxml");
            if (url == null) {
                throw new IOException("No se encontró /fxml/modal_registrar_pago_proveedor.fxml");
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            RegistrarPagoProveedorController ctrl = loader.getController();
            ctrl.setProveedor(proveedorActual);

            Stage stage = crearStageModal("Registrar pago a proveedor", root);
            stage.showAndWait();

            if (ctrl.isPagoGuardado()) {
                recargarCuentaDesdeMySQL();
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                }
            }
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error de Interfaz",
                    "No se pudo abrir el modal de pago a proveedor: " + e.getMessage());
        }
    }

    private Stage crearStageModal(String titulo, Parent root) {
        Stage stage = new Stage();
        stage.setTitle(titulo);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);

        if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            final Window owner = rootPane.getScene().getWindow();
            stage.initOwner(owner);
            final Node ownerRoot = rootPane.getScene().getRoot();
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
        URL cssUrl = getClass().getResource("/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setScene(scene);
        return stage;
    }

    @FXML
    public void handleVolverAProveedores() {
        if (proveedorActual != null) {
            ProveedoresController.setIdProveedorSeleccionarAlCargar(proveedorActual.getIdProveedor());
        }
        proveedorPendienteCuenta = null;
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/proveedores.fxml");
        }
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols).format(monto);
    }

    private String formatearDiaMes(LocalDate fecha) {
        if (fecha == null) {
            return "—";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_ES_CO);
        return fecha.format(fmt);
    }
}