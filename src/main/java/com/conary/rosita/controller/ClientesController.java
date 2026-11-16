package com.conary.rosita.controller;

import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.model.Cliente;
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
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista A de la Tanda 1 del Modulo 3: Clientes y fiado.
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ClientesController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final int FILAS_POR_PAGINA = 5;
    private static final BigDecimal FACTOR_CERCA_DEL_TOPE = new BigDecimal("0.80");

    private static int idClienteSeleccionarAlCargar = -1;

    public static void setIdClienteSeleccionarAlCargar(int idCliente) {
        idClienteSeleccionarAlCargar = idCliente;
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverMenu;
    @FXML private Label lblUsuarioHeader;

    // Buscador, Pildoras de Filtro y Boton de Registro
    @FXML private TextField txtBuscarCliente;
    @FXML private ToggleButton tbFiltroTodos;
    @FXML private ToggleButton tbFiltroConDeuda;
    @FXML private ToggleButton tbFiltroConFiado;
    @FXML private ToggleButton tbFiltroDadosBaja;
    @FXML private Button btnRegistrarCliente;

    // Indicadores KPI Superiores
    @FXML private Label lblTotalDeudaActivos;
    @FXML private Label lblConteoFiadoAutorizado;

    // Tabla Principal de 6 Columnas
    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, Cliente> colNombre;
    @FXML private TableColumn<Cliente, Cliente> colTelefono;
    @FXML private TableColumn<Cliente, Cliente> colFiado;
    @FXML private TableColumn<Cliente, Cliente> colCupo;
    @FXML private TableColumn<Cliente, Cliente> colDebeAhora;
    @FXML private TableColumn<Cliente, Cliente> colAviso;

    // Pie de Tabla y Paginacion
    @FXML private Label lblConteoClientes;
    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPaginaSiguiente;

    // Barra Inferior de Seleccion
    @FXML private HBox barraSeleccionCliente;
    @FXML private Label lblClienteSeleccionadoInfo;
    @FXML private Button btnVerCuenta;
    @FXML private Button btnRegistrarAbono;
    @FXML private Button btnAutorizarFiado;
    @FXML private Button btnModificarCliente;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ToggleGroup tgFiltros = new ToggleGroup();
    private final List<Cliente> todosLosClientes = new ArrayList<Cliente>();
    private final List<Cliente> clientesFiltrados = new ArrayList<Cliente>();
    private final ObservableList<Cliente> itemsPaginaActual = FXCollections.observableArrayList();

    private int paginaActual = 0;
    private Cliente clienteSeleccionado;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarFiltrosYBusqueda();
        configurarTabla();
        configurarRestriccionesPorRol();
        cargarClientesDesdeMySQL();
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

        if (lblUsuarioHeader != null) {
            lblUsuarioHeader.setText(nombre + " · " + rol);
        }
    }

    private void configurarRestriccionesPorRol() {
        boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
        if (btnAutorizarFiado != null) {
            btnAutorizarFiado.setVisible(esAdmin);
            btnAutorizarFiado.setManaged(esAdmin);
        }
    }

    private void configurarFiltrosYBusqueda() {
        ToggleButton[] botones = new ToggleButton[]{
                tbFiltroTodos, tbFiltroConDeuda, tbFiltroConFiado, tbFiltroDadosBaja
        };
        for (ToggleButton tb : botones) {
            if (tb != null) {
                tb.setToggleGroup(tgFiltros);
            }
        }
        if (tbFiltroTodos != null) {
            tbFiltroTodos.setSelected(true);
        }

        tgFiltros.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
                return;
            }
            paginaActual = 0;
            aplicarFiltrosYPaginacion();
        });

        if (txtBuscarCliente != null) {
            txtBuscarCliente.textProperty().addListener((obs, oldVal, newVal) -> {
                paginaActual = 0;
                aplicarFiltrosYPaginacion();
            });
        }
    }

    private void configurarTabla() {
        tblClientes.setItems(itemsPaginaActual);
        tblClientes.setFixedCellSize(56.0);

        tblClientes.setRowFactory(tv -> new TableRow<Cliente>() {
            @Override
            protected void updateItem(Cliente item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(56.0);
                setMinHeight(56.0);
                setMaxHeight(56.0);
            }
        });

        // 1. Columna Nombre (con punto cafe ● en la fila seleccionada)
        colNombre.setCellValueFactory(data -> new SimpleObjectProperty<Cliente>(data.getValue()));
        colNombre.setCellFactory(col -> new TableCell<Cliente, Cliente>() {
            private final Circle dot = new Circle(4.2, Color.web("#6B3A19"));
            private final Label lblNom = new Label();
            private final HBox box = new HBox(8, dot, lblNom);

            {
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    TableRow<?> row = getTableRow();
                    boolean sel = row != null && row.isSelected();
                    dot.setVisible(sel);
                    dot.setManaged(sel);
                    lblNom.setText(c.getNombre());
                    lblNom.getStyleClass().removeAll("clientes-cell-nombre", "clientes-cell-nombre-selected");
                    lblNom.getStyleClass().add(sel ? "clientes-cell-nombre-selected" : "clientes-cell-nombre");
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // 2. Columna Telefono
        colTelefono.setCellValueFactory(data -> new SimpleObjectProperty<Cliente>(data.getValue()));
        colTelefono.setCellFactory(col -> new TableCell<Cliente, Cliente>() {
            @Override
            protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String tel = c.getTelefono() != null && !c.getTelefono().trim().isEmpty()
                            ? c.getTelefono().trim()
                            : "—";
                    setText(tel);
                    getStyleClass().removeAll("clientes-cell-telefono");
                    getStyleClass().add("clientes-cell-telefono");
                }
            }
        });

        // 3. Columna Fiado (pildora melocoton "Autorizado" o pildora gris "Sin fiado")
        colFiado.setCellValueFactory(data -> new SimpleObjectProperty<Cliente>(data.getValue()));
        colFiado.setCellFactory(col -> new TableCell<Cliente, Cliente>() {
            @Override
            protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label();
                    if (!c.isActivo()) {
                        pill.setText("Dado de baja");
                        pill.getStyleClass().add("clientes-pill-sin-fiado");
                    } else if (c.isFiadoAutorizado()) {
                        pill.setText("Autorizado");
                        pill.getStyleClass().add("clientes-pill-autorizado");
                    } else {
                        pill.setText("Sin fiado");
                        pill.getStyleClass().add("clientes-pill-sin-fiado");
                    }
                    HBox cont = new HBox(pill);
                    cont.setAlignment(Pos.CENTER);
                    setGraphic(cont);
                    setText(null);
                }
            }
        });

        // 4. Columna Cupo ($ 100.000 o "—")
        colCupo.setCellValueFactory(data -> new SimpleObjectProperty<Cliente>(data.getValue()));
        colCupo.setCellFactory(col -> new TableCell<Cliente, Cliente>() {
            @Override
            protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    if (c.isFiadoAutorizado()
                            && c.getLimiteCredito() != null
                            && c.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0) {
                        setText("$ " + formatearMoneda(c.getLimiteCredito()));
                        getStyleClass().removeAll("clientes-cell-cupo", "clientes-cell-dash");
                        getStyleClass().add("clientes-cell-cupo");
                    } else {
                        setText("—");
                        getStyleClass().removeAll("clientes-cell-cupo", "clientes-cell-dash");
                        getStyleClass().add("clientes-cell-dash");
                    }
                }
            }
        });

        // 5. Columna Debe ahora ($ 20.000 o $ 0)
        colDebeAhora.setCellValueFactory(data -> new SimpleObjectProperty<Cliente>(data.getValue()));
        colDebeAhora.setCellFactory(col -> new TableCell<Cliente, Cliente>() {
            @Override
            protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    BigDecimal deuda = c.getSaldoPendiente() != null ? c.getSaldoPendiente() : BigDecimal.ZERO;
                    setText("$ " + formatearMoneda(deuda));
                    getStyleClass().removeAll("clientes-cell-deuda-activa", "clientes-cell-deuda-cero");
                    getStyleClass().add(deuda.compareTo(BigDecimal.ZERO) > 0
                            ? "clientes-cell-deuda-activa"
                            : "clientes-cell-deuda-cero");
                }
            }
        });

        // 6. Columna Aviso (pildora ambar "⚠ Cerca del tope" o "—")
        colAviso.setCellValueFactory(data -> new SimpleObjectProperty<Cliente>(data.getValue()));
        colAviso.setCellFactory(col -> new TableCell<Cliente, Cliente>() {
            @Override
            protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER);
                    if (estaCercaDelTope(c)) {
                        Label lblAviso = new Label("⚠  Cerca\ndel tope");
                        lblAviso.setTextAlignment(TextAlignment.CENTER);
                        lblAviso.setAlignment(Pos.CENTER);
                        lblAviso.getStyleClass().add("clientes-pill-cerca-tope");
                        HBox box = new HBox(lblAviso);
                        box.setAlignment(Pos.CENTER);
                        setGraphic(box);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText("—");
                        getStyleClass().removeAll("clientes-cell-dash");
                        getStyleClass().add("clientes-cell-dash");
                    }
                }
            }
        });

        tblClientes.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            clienteSeleccionado = newSel;
            actualizarBarraSeleccion(newSel);
            tblClientes.refresh();
        });
    }

    private boolean estaCercaDelTope(Cliente c) {
        if (c == null || !c.isActivo() || !c.isFiadoAutorizado()) {
            return false;
        }
        BigDecimal cupo = c.getLimiteCredito() != null ? c.getLimiteCredito() : BigDecimal.ZERO;
        BigDecimal deuda = c.getSaldoPendiente() != null ? c.getSaldoPendiente() : BigDecimal.ZERO;
        if (cupo.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal umbral = cupo.multiply(FACTOR_CERCA_DEL_TOPE);
        return deuda.compareTo(umbral) >= 0;
    }

    public void cargarClientesDesdeMySQL() {
        todosLosClientes.clear();
        BigDecimal sumaDeudaTotal = BigDecimal.ZERO;
        int conteoConFiado = 0;

        try {
            List<Cliente> desdeBD = clienteDAO.listarTodos(false);
            if (desdeBD != null) {
                for (Cliente c : desdeBD) {
                    todosLosClientes.add(c);
                    if (c.isActivo()) {
                        BigDecimal saldo = c.getSaldoPendiente() != null ? c.getSaldoPendiente() : BigDecimal.ZERO;
                        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
                            sumaDeudaTotal = sumaDeudaTotal.add(saldo);
                        }
                        if (c.isFiadoAutorizado()
                                && c.getLimiteCredito() != null
                                && c.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0) {
                            conteoConFiado++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ClientesController] Error al consultar clientes en MySQL: " + e.getMessage());
        }

        if (lblTotalDeudaActivos != null) {
            lblTotalDeudaActivos.setText("$ " + formatearMoneda(sumaDeudaTotal));
        }
        if (lblConteoFiadoAutorizado != null) {
            lblConteoFiadoAutorizado.setText(String.valueOf(conteoConFiado));
        }

        aplicarFiltrosYPaginacion();
    }

    private void aplicarFiltrosYPaginacion() {
        String criterio = txtBuscarCliente != null && txtBuscarCliente.getText() != null
                ? txtBuscarCliente.getText().trim().toLowerCase(LOCALE_ES_CO)
                : "";
        Toggle filtroActivo = tgFiltros.getSelectedToggle();

        clientesFiltrados.clear();
        for (Cliente c : todosLosClientes) {
            if (!cumpleFiltroPildora(c, filtroActivo)) {
                continue;
            }
            if (!criterio.isEmpty()) {
                String nom = c.getNombre() != null ? c.getNombre().toLowerCase(LOCALE_ES_CO) : "";
                String tel = c.getTelefono() != null ? c.getTelefono().toLowerCase(LOCALE_ES_CO) : "";
                if (!nom.contains(criterio) && !tel.contains(criterio)) {
                    continue;
                }
            }
            clientesFiltrados.add(c);
        }

        int totalFiltrados = clientesFiltrados.size();
        int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / FILAS_POR_PAGINA));

        Cliente clienteObjetivo = null;
        if (idClienteSeleccionarAlCargar > 0) {
            for (int i = 0; i < clientesFiltrados.size(); i++) {
                Cliente cand = clientesFiltrados.get(i);
                if (cand.getIdCliente() == idClienteSeleccionarAlCargar) {
                    clienteObjetivo = cand;
                    paginaActual = i / FILAS_POR_PAGINA;
                    break;
                }
            }
            idClienteSeleccionarAlCargar = -1;
        }

        if (paginaActual >= totalPaginas) {
            paginaActual = totalPaginas - 1;
        }
        if (paginaActual < 0) {
            paginaActual = 0;
        }

        int desde = paginaActual * FILAS_POR_PAGINA;
        int hasta = Math.min(desde + FILAS_POR_PAGINA, totalFiltrados);

        itemsPaginaActual.clear();
        if (desde < hasta) {
            itemsPaginaActual.addAll(clientesFiltrados.subList(desde, hasta));
        }

        if (lblConteoClientes != null) {
            lblConteoClientes.setText("Mostrando " + itemsPaginaActual.size() + " de " + totalFiltrados + " clientes");
        }
        if (btnPaginaAnterior != null) {
            btnPaginaAnterior.setDisable(paginaActual <= 0);
        }
        if (btnPaginaSiguiente != null) {
            btnPaginaSiguiente.setDisable(paginaActual >= totalPaginas - 1);
        }

        if (clienteObjetivo != null && itemsPaginaActual.contains(clienteObjetivo)) {
            tblClientes.getSelectionModel().select(clienteObjetivo);
        } else if (!itemsPaginaActual.isEmpty()) {
            tblClientes.getSelectionModel().selectFirst();
        } else {
            tblClientes.getSelectionModel().clearSelection();
            actualizarBarraSeleccion(null);
        }
    }

    private boolean cumpleFiltroPildora(Cliente c, Toggle filtro) {
        if (filtro == tbFiltroDadosBaja) {
            return !c.isActivo();
        }
        if (!c.isActivo()) {
            return false;
        }
        if (filtro == tbFiltroConDeuda) {
            BigDecimal deuda = c.getSaldoPendiente() != null ? c.getSaldoPendiente() : BigDecimal.ZERO;
            return deuda.compareTo(BigDecimal.ZERO) > 0;
        }
        if (filtro == tbFiltroConFiado) {
            return c.isFiadoAutorizado();
        }
        return true; // Todos (Activos)
    }

    private void actualizarBarraSeleccion(Cliente sel) {
        boolean haySel = sel != null;
        boolean tieneDeuda = haySel
                && sel.getSaldoPendiente() != null
                && sel.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0;

        if (lblClienteSeleccionadoInfo != null) {
            if (!haySel) {
                lblClienteSeleccionadoInfo.setText("Seleccione un cliente de la tabla para ver sus opciones");
            } else if (!sel.isActivo()) {
                lblClienteSeleccionadoInfo.setText("Seleccionado: " + sel.getNombre() + " · Dado de baja");
            } else if (!sel.isFiadoAutorizado()) {
                lblClienteSeleccionadoInfo.setText("Seleccionado: " + sel.getNombre() + " · Sin fiado autorizado");
            } else {
                BigDecimal deuda = sel.getSaldoPendiente() != null ? sel.getSaldoPendiente() : BigDecimal.ZERO;
                BigDecimal disponible = sel.getCreditoDisponible();
                lblClienteSeleccionadoInfo.setText("Seleccionada: " + sel.getNombre()
                        + " · Debe $ " + formatearMoneda(deuda)
                        + " · Puede deber $ " + formatearMoneda(disponible) + " más");
            }
        }

        if (btnVerCuenta != null) {
            btnVerCuenta.setDisable(!haySel);
        }
        if (btnRegistrarAbono != null) {
            btnRegistrarAbono.setDisable(!tieneDeuda);
        }
        if (btnAutorizarFiado != null) {
            btnAutorizarFiado.setDisable(!haySel);
            if (haySel && sel.isFiadoAutorizado()) {
                btnAutorizarFiado.setText("✓   Cambiar cupo");
            } else {
                btnAutorizarFiado.setText("✓   Autorizar fiado");
            }
        }
        if (btnModificarCliente != null) {
            btnModificarCliente.setDisable(!haySel);
        }
    }

    @FXML
    public void handlePaginaAnterior() {
        if (paginaActual > 0) {
            paginaActual--;
            aplicarFiltrosYPaginacion();
        }
    }

    @FXML
    public void handlePaginaSiguiente() {
        int totalPaginas = Math.max(1, (int) Math.ceil((double) clientesFiltrados.size() / FILAS_POR_PAGINA));
        if (paginaActual < totalPaginas - 1) {
            paginaActual++;
            aplicarFiltrosYPaginacion();
        }
    }

    @FXML
    public void handleRegistrarClienteNuevo() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/registrar_cliente.fxml");
        }
    }

    @FXML
    public void handleVerCuenta() {
        if (clienteSeleccionado == null) {
            return;
        }
        EstadoCuentaClienteController.setClientePendienteCuenta(clienteSeleccionado);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            EstadoCuentaClienteController ctrl = menu.cargarVistaConControlador("/fxml/estado_cuenta_cliente.fxml");
            if (ctrl != null) {
                ctrl.setCliente(clienteSeleccionado);
            }
        }
    }

    @FXML
    public void handleRegistrarAbono() {
        if (clienteSeleccionado == null) {
            return;
        }
        BigDecimal deuda = clienteSeleccionado.getSaldoPendiente() != null
                ? clienteSeleccionado.getSaldoPendiente()
                : BigDecimal.ZERO;
        if (deuda.compareTo(BigDecimal.ZERO) <= 0) {
            AlertaUtil.mostrarInfo("Sin deuda pendiente",
                    "El cliente «" + clienteSeleccionado.getNombre() + "» no tiene deuda pendiente.");
            return;
        }

        try {
            FXMLLoader loader = crearFXMLLoader("/fxml/modal_registrar_abono.fxml");
            Parent root = loader.load();
            RegistrarAbonoController ctrl = loader.getController();
            ctrl.setCliente(clienteSeleccionado);

            Stage stage = crearStageModal("Registrar abono", root);
            stage.showAndWait();

            if (ctrl.isAbonoGuardado()) {
                idClienteSeleccionarAlCargar = clienteSeleccionado.getIdCliente();
                cargarClientesDesdeMySQL();
            }
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error de Interfaz",
                    "No se pudo abrir el modal de registro de abono: " + e.getMessage());
        }
    }

    @FXML
    public void handleAutorizarFiado() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede autorizar o modificar cupos de fiado.");
            return;
        }
        if (clienteSeleccionado == null) {
            return;
        }

        try {
            FXMLLoader loader = crearFXMLLoader("/fxml/modal_autorizar_fiado.fxml");
            Parent root = loader.load();
            AutorizarFiadoController ctrl = loader.getController();
            ctrl.setCliente(clienteSeleccionado);

            String titulo = clienteSeleccionado.isFiadoAutorizado() ? "Cambiar cupo de fiado" : "Autorizar fiado";
            Stage stage = crearStageModal(titulo, root);
            stage.showAndWait();

            if (ctrl.isCambiosGuardados()) {
                idClienteSeleccionarAlCargar = clienteSeleccionado.getIdCliente();
                cargarClientesDesdeMySQL();
            }
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error de Interfaz",
                    "No se pudo abrir el modal de fiado: " + e.getMessage());
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
    public void handleModificarCliente() {
        if (clienteSeleccionado == null) {
            return;
        }
        ModificarClienteController.setClientePendienteEdicion(clienteSeleccionado);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            ModificarClienteController ctrl = menu.cargarVistaConControlador("/fxml/modificar_cliente.fxml");
            if (ctrl != null) {
                ctrl.setCliente(clienteSeleccionado);
            }
        }
    }

    @FXML
    public void handleVolverMenu() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarMenuDashboard();
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
}
