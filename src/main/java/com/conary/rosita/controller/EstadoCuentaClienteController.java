package com.conary.rosita.controller;

import com.conary.rosita.dao.AbonoClienteDAO;
import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.dao.VentaDAO;
import com.conary.rosita.model.AbonoCliente;
import com.conary.rosita.model.Cliente;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.model.Venta;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista B de la Tanda 2 del Modulo 3: Estado de Cuenta del Cliente ("Cuenta de [Nombre]").
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class EstadoCuentaClienteController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static Cliente clientePendienteCuenta;

    public static void setClientePendienteCuenta(Cliente cliente) {
        clientePendienteCuenta = cliente;
    }

    public static class ItemMovimientoCuenta {
        private final int idMovimiento;
        private final LocalDateTime fecha;
        private final boolean esAbono;
        private final BigDecimal valor;
        private BigDecimal debiaDespues;

        public ItemMovimientoCuenta(int idMovimiento, LocalDateTime fecha, boolean esAbono, BigDecimal valor) {
            this.idMovimiento = idMovimiento;
            this.fecha = fecha != null ? fecha : LocalDateTime.now();
            this.esAbono = esAbono;
            this.valor = valor != null ? valor : BigDecimal.ZERO;
            this.debiaDespues = BigDecimal.ZERO;
        }

        public int getIdMovimiento() {
            return idMovimiento;
        }

        public LocalDateTime getFecha() {
            return fecha;
        }

        public boolean isEsAbono() {
            return esAbono;
        }

        public BigDecimal getValor() {
            return valor;
        }

        public BigDecimal getDebiaDespues() {
            return debiaDespues;
        }

        public void setDebiaDespues(BigDecimal debiaDespues) {
            this.debiaDespues = debiaDespues != null ? debiaDespues : BigDecimal.ZERO;
        }
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverTop;
    @FXML private Label lblTituloCuenta;
    @FXML private Label lblSubtituloTelefono;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;

    // 3 Tarjetas de Resumen Financiero
    @FXML private Label lblCupoFiado;
    @FXML private Label lblDebeAhora;
    @FXML private Label lblPuedeDeber;

    // Tabla de Movimientos
    @FXML private Label lblConteoMovimientos;
    @FXML private TableView<ItemMovimientoCuenta> tblMovimientosCliente;
    @FXML private TableColumn<ItemMovimientoCuenta, ItemMovimientoCuenta> colFecha;
    @FXML private TableColumn<ItemMovimientoCuenta, ItemMovimientoCuenta> colQueFue;
    @FXML private TableColumn<ItemMovimientoCuenta, ItemMovimientoCuenta> colValor;
    @FXML private TableColumn<ItemMovimientoCuenta, ItemMovimientoCuenta> colDebiaDespues;

    // Botones Inferiores
    @FXML private Button btnAutorizarFiado;
    @FXML private Button btnRegistrarAbono;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final VentaDAO ventaDAO = new VentaDAO();
    private final AbonoClienteDAO abonoClienteDAO = new AbonoClienteDAO();

    private final ObservableList<ItemMovimientoCuenta> listaMovimientos = FXCollections.observableArrayList();
    private Cliente clienteActual;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarTablaMovimientos();
        configurarRestriccionesPorRol();

        if (clientePendienteCuenta != null) {
            setCliente(clientePendienteCuenta);
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

        if (lblUsuarioNombre != null) {
            lblUsuarioNombre.setText(nombre);
        }
        if (lblUsuarioRol != null) {
            lblUsuarioRol.setText(rol);
        }
    }

    private void configurarRestriccionesPorRol() {
        boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
        if (btnAutorizarFiado != null) {
            btnAutorizarFiado.setVisible(esAdmin);
            btnAutorizarFiado.setManaged(esAdmin);
        }
    }

    private void configurarTablaMovimientos() {
        tblMovimientosCliente.setItems(listaMovimientos);
        tblMovimientosCliente.setFixedCellSize(56.0);

        tblMovimientosCliente.setRowFactory(tv -> new TableRow<ItemMovimientoCuenta>() {
            @Override
            protected void updateItem(ItemMovimientoCuenta item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(56.0);
                setMinHeight(56.0);
                setMaxHeight(56.0);
            }
        });

        // 1. Columna FECHA (ej. "10 de octubre")
        colFecha.setCellValueFactory(data -> new SimpleObjectProperty<ItemMovimientoCuenta>(data.getValue()));
        colFecha.setCellFactory(col -> new TableCell<ItemMovimientoCuenta, ItemMovimientoCuenta>() {
            @Override
            protected void updateItem(ItemMovimientoCuenta item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(formatearDiaMes(item.getFecha()));
                    getStyleClass().removeAll("cuenta-cliente-cell-fecha");
                    getStyleClass().add("cuenta-cliente-cell-fecha");
                }
            }
        });

        // 2. Columna QUE FUE (pildora melocoton "↓ Abono" o pildora crema "📄 Venta a fiado")
        colQueFue.setCellValueFactory(data -> new SimpleObjectProperty<ItemMovimientoCuenta>(data.getValue()));
        colQueFue.setCellFactory(col -> new TableCell<ItemMovimientoCuenta, ItemMovimientoCuenta>() {
            @Override
            protected void updateItem(ItemMovimientoCuenta item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label pill = new Label();
                    if (item.isEsAbono()) {
                        pill.setText("↓  Abono");
                        pill.getStyleClass().add("cuenta-cliente-pill-abono");
                    } else {
                        pill.setText("📄  Venta a fiado");
                        pill.getStyleClass().add("cuenta-cliente-pill-venta");
                    }
                    HBox box = new HBox(pill);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // 3. Columna VALOR ("− $ 10.000" o "+ $ 30.000")
        colValor.setCellValueFactory(data -> new SimpleObjectProperty<ItemMovimientoCuenta>(data.getValue()));
        colValor.setCellFactory(col -> new TableCell<ItemMovimientoCuenta, ItemMovimientoCuenta>() {
            @Override
            protected void updateItem(ItemMovimientoCuenta item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    String montoStr = formatearMoneda(item.getValor());
                    if (item.isEsAbono()) {
                        setText("− $ " + montoStr);
                        getStyleClass().removeAll("cuenta-cliente-cell-abono", "cuenta-cliente-cell-venta");
                        getStyleClass().add("cuenta-cliente-cell-abono");
                    } else {
                        setText("+ $ " + montoStr);
                        getStyleClass().removeAll("cuenta-cliente-cell-abono", "cuenta-cliente-cell-venta");
                        getStyleClass().add("cuenta-cliente-cell-venta");
                    }
                }
            }
        });

        // 4. Columna DEBIA DESPUES ("$ 20.000")
        colDebiaDespues.setCellValueFactory(data -> new SimpleObjectProperty<ItemMovimientoCuenta>(data.getValue()));
        colDebiaDespues.setCellFactory(col -> new TableCell<ItemMovimientoCuenta, ItemMovimientoCuenta>() {
            @Override
            protected void updateItem(ItemMovimientoCuenta item, boolean empty) {
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

    public void setCliente(Cliente cliente) {
        this.clienteActual = cliente;
        if (cliente == null) {
            return;
        }
        recargarEstadoCuentaDesdeMySQL();
    }

    public void recargarEstadoCuentaDesdeMySQL() {
        if (clienteActual == null) {
            return;
        }

        try {
            Cliente fresco = clienteDAO.obtenerPorId(clienteActual.getIdCliente());
            if (fresco != null) {
                clienteActual = fresco;
            }
        } catch (SQLException ignored) {
        }

        if (lblTituloCuenta != null) {
            lblTituloCuenta.setText("Cuenta de " + clienteActual.getNombre());
        }
        if (lblSubtituloTelefono != null) {
            String tel = clienteActual.getTelefono() != null && !clienteActual.getTelefono().trim().isEmpty()
                    ? clienteActual.getTelefono().trim()
                    : "Sin teléfono registrado";
            String dir = clienteActual.getDireccion() != null && !clienteActual.getDireccion().trim().isEmpty()
                    ? (" · " + clienteActual.getDireccion().trim())
                    : "";
            lblSubtituloTelefono.setText(tel + dir);
        }

        BigDecimal cupo = clienteActual.getLimiteCredito() != null ? clienteActual.getLimiteCredito() : BigDecimal.ZERO;
        BigDecimal deudaActual = clienteActual.getSaldoPendiente() != null ? clienteActual.getSaldoPendiente() : BigDecimal.ZERO;
        BigDecimal disponible = clienteActual.getCreditoDisponible();

        if (lblCupoFiado != null) {
            if (clienteActual.isFiadoAutorizado() && cupo.compareTo(BigDecimal.ZERO) > 0) {
                lblCupoFiado.setText("$ " + formatearMoneda(cupo));
            } else {
                lblCupoFiado.setText("Sin fiado");
            }
        }
        if (lblDebeAhora != null) {
            lblDebeAhora.setText("$ " + formatearMoneda(deudaActual));
        }
        if (lblPuedeDeber != null) {
            if (clienteActual.isFiadoAutorizado() && cupo.compareTo(BigDecimal.ZERO) > 0) {
                lblPuedeDeber.setText("$ " + formatearMoneda(disponible) + " más");
            } else {
                lblPuedeDeber.setText("$ 0");
            }
        }

        if (btnRegistrarAbono != null) {
            btnRegistrarAbono.setDisable(deudaActual.compareTo(BigDecimal.ZERO) <= 0);
        }
        if (btnAutorizarFiado != null) {
            btnAutorizarFiado.setText(clienteActual.isFiadoAutorizado() ? "☑   Cambiar cupo" : "☑   Autorizar fiado");
        }

        // Consultar y combinar movimientos reales de ventas a fiado y abonos en MySQL
        List<ItemMovimientoCuenta> cronologicoAsc = new ArrayList<ItemMovimientoCuenta>();
        int idCliente = clienteActual.getIdCliente();

        try {
            List<Venta> ventasFiadas = ventaDAO.listarVentas(null, null, Boolean.TRUE);
            if (ventasFiadas != null) {
                for (Venta v : ventasFiadas) {
                    if (v.getIdCliente() != null
                            && v.getIdCliente().intValue() == idCliente
                            && !v.isAnulada()) {
                        cronologicoAsc.add(new ItemMovimientoCuenta(
                                v.getIdMovimiento(),
                                v.getFecha(),
                                false,
                                v.getTotalVenta()
                        ));
                    }
                }
            }

            List<AbonoCliente> abonos = abonoClienteDAO.listarAbonosPorCliente(idCliente);
            if (abonos != null) {
                for (AbonoCliente ab : abonos) {
                    cronologicoAsc.add(new ItemMovimientoCuenta(
                            ab.getIdMovimiento(),
                            ab.getFecha(),
                            true,
                            ab.getMonto()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[EstadoCuentaClienteController] Error al consultar movimientos del cliente: " + e.getMessage());
        }

        // 1. Ordenar de mas antiguo a mas nuevo para calcular "DEBIA DESPUES"
        Collections.sort(cronologicoAsc, new Comparator<ItemMovimientoCuenta>() {
            @Override
            public int compare(ItemMovimientoCuenta a, ItemMovimientoCuenta b) {
                int cmpFecha = a.getFecha().compareTo(b.getFecha());
                if (cmpFecha != 0) {
                    return cmpFecha;
                }
                return Integer.compare(a.getIdMovimiento(), b.getIdMovimiento());
            }
        });

        BigDecimal saldoAcumulado = BigDecimal.ZERO;
        for (ItemMovimientoCuenta mov : cronologicoAsc) {
            if (mov.isEsAbono()) {
                saldoAcumulado = saldoAcumulado.subtract(mov.getValor());
                if (saldoAcumulado.compareTo(BigDecimal.ZERO) < 0) {
                    saldoAcumulado = BigDecimal.ZERO;
                }
            } else {
                saldoAcumulado = saldoAcumulado.add(mov.getValor());
            }
            mov.setDebiaDespues(saldoAcumulado);
        }

        // 2. Invertir la lista para mostrarla "Del mas nuevo al mas antiguo"
        Collections.reverse(cronologicoAsc);
        listaMovimientos.setAll(cronologicoAsc);

        if (lblConteoMovimientos != null) {
            int n = listaMovimientos.size();
            lblConteoMovimientos.setText(n == 1 ? "Mostrando 1 registro" : ("Mostrando " + n + " registros"));
        }
    }

    @FXML
    public void handleAutorizarFiado() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede autorizar o cambiar el cupo de fiado.");
            return;
        }
        if (clienteActual == null) {
            return;
        }

        try {
            FXMLLoader loader = crearFXMLLoader("/fxml/modal_autorizar_fiado.fxml");
            Parent root = loader.load();
            AutorizarFiadoController ctrl = loader.getController();
            ctrl.setCliente(clienteActual);

            String titulo = clienteActual.isFiadoAutorizado() ? "Cambiar cupo de fiado" : "Autorizar fiado";
            Stage stage = crearStageModal(titulo, root);
            stage.showAndWait();

            if (ctrl.isCambiosGuardados()) {
                recargarEstadoCuentaDesdeMySQL();
            }
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error de Interfaz",
                    "No se pudo abrir el modal de fiado: " + e.getMessage());
        }
    }

    @FXML
    public void handleRegistrarAbono() {
        if (clienteActual == null) {
            return;
        }
        BigDecimal deuda = clienteActual.getSaldoPendiente() != null
                ? clienteActual.getSaldoPendiente()
                : BigDecimal.ZERO;
        if (deuda.compareTo(BigDecimal.ZERO) <= 0) {
            AlertaUtil.mostrarInfo("Sin deuda pendiente",
                    "El cliente «" + clienteActual.getNombre() + "» no tiene deuda pendiente.");
            return;
        }

        try {
            FXMLLoader loader = crearFXMLLoader("/fxml/modal_registrar_abono.fxml");
            Parent root = loader.load();
            RegistrarAbonoController ctrl = loader.getController();
            ctrl.setCliente(clienteActual);

            Stage stage = crearStageModal("Registrar abono", root);
            stage.showAndWait();

            if (ctrl.isAbonoGuardado()) {
                recargarEstadoCuentaDesdeMySQL();
            }
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error de Interfaz",
                    "No se pudo abrir el modal de registro de abono: " + e.getMessage());
        }
    }

    private FXMLLoader crearFXMLLoader(String fxmlPath) throws IOException {
        URL url = getClass().getResource(fxmlPath);
        if (url == null) {
            throw new IOException("No se encontró el recurso FXML: " + fxmlPath);
        }
        return new FXMLLoader(url);
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
    public void handleVolverAClientes() {
        if (clienteActual != null) {
            ClientesController.setIdClienteSeleccionarAlCargar(clienteActual.getIdCliente());
        }
        clientePendienteCuenta = null;
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/clientes.fxml");
        }
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(monto);
    }

    private String formatearDiaMes(LocalDateTime fechaHora) {
        LocalDateTime dt = fechaHora != null ? fechaHora : LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_ES_CO);
        return dt.toLocalDate().format(fmt);
    }
}
