package com.conary.rosita.controller;

import com.conary.rosita.dao.ProveedorDAO;
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
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista A de la Tanda 1 del Modulo 5: Compras y proveedores.
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ProveedoresController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static int idProveedorSeleccionarAlCargar = -1;

    public static void setIdProveedorSeleccionarAlCargar(int idProveedor) {
        idProveedorSeleccionarAlCargar = idProveedor;
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverMenu;
    @FXML private Label lblUsuarioHeader;

    // Buscador, 3 pildoras y 2 botones superiores
    @FXML private TextField txtBuscarProveedor;
    @FXML private ToggleButton tbFiltroTodos;
    @FXML private ToggleButton tbFiltroLesDebo;
    @FXML private ToggleButton tbFiltroDadosBaja;
    @FXML private Button btnRegistrarProveedor;
    @FXML private Button btnRegistrarCompraTop;

    // 3 Tarjetas KPI superiores
    @FXML private Label lblTotalDeudaProveedores;
    @FXML private Label lblConteoVencenSemana;
    @FXML private Label lblSubVencenSemana;
    @FXML private Label lblConteoProveedoresActivos;

    // Tabla principal de 6 columnas
    @FXML private TableView<Proveedor> tblProveedores;
    @FXML private TableColumn<Proveedor, Proveedor> colProveedor;
    @FXML private TableColumn<Proveedor, Proveedor> colTelefono;
    @FXML private TableColumn<Proveedor, Proveedor> colProductos;
    @FXML private TableColumn<Proveedor, Proveedor> colLeDebo;
    @FXML private TableColumn<Proveedor, Proveedor> colProximoPago;
    @FXML private TableColumn<Proveedor, Proveedor> colAviso;

    // Pie de tabla y barra inferior fija
    @FXML private Label lblConteoProveedores;
    @FXML private HBox barraSeleccionProveedor;
    @FXML private Label lblProveedorSeleccionadoInfo;
    @FXML private Button btnVerCuenta;
    @FXML private Button btnRegistrarPago;
    @FXML private Button btnRegistrarCompra;
    @FXML private Button btnModificarDatos;

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    private final ToggleGroup tgFiltros = new ToggleGroup();
    private final List<Proveedor> todosLosProveedores = new ArrayList<Proveedor>();
    private final ObservableList<Proveedor> proveedoresFiltrados = FXCollections.observableArrayList();

    private Proveedor proveedorSeleccionado;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarFiltrosYBusqueda();
        configurarTabla();
        configurarRestriccionesPorRol();
        cargarProveedoresDesdeMySQL();
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
        Button[] botonesSoloAdmin = new Button[]{
                btnRegistrarProveedor, btnRegistrarCompraTop, btnRegistrarPago, btnRegistrarCompra, btnModificarDatos
        };
        for (Button btn : botonesSoloAdmin) {
            if (btn != null) {
                btn.setVisible(esAdmin);
                btn.setManaged(esAdmin);
            }
        }
    }

    private boolean verificarPermisoAdministradora() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede registrar o modificar proveedores, compras y pagos.");
            return false;
        }
        return true;
    }

    private void configurarFiltrosYBusqueda() {
        ToggleButton[] botones = new ToggleButton[]{
                tbFiltroTodos, tbFiltroLesDebo, tbFiltroDadosBaja
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
            aplicarFiltros();
        });

        if (txtBuscarProveedor != null) {
            txtBuscarProveedor.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
        }
    }

    private void configurarTabla() {
        tblProveedores.setItems(proveedoresFiltrados);
        tblProveedores.setFixedCellSize(56.0);

        tblProveedores.setRowFactory(tv -> new TableRow<Proveedor>() {
            @Override
            protected void updateItem(Proveedor item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(56.0);
                setMinHeight(56.0);
                setMaxHeight(56.0);
            }
        });

        // 1. Columna Proveedor (con radio cafe en fila seleccionada / aro gris en no seleccionada)
        colProveedor.setCellValueFactory(data -> new SimpleObjectProperty<Proveedor>(data.getValue()));
        colProveedor.setCellFactory(col -> new TableCell<Proveedor, Proveedor>() {
            private final Circle outerCircle = new Circle(6.5, Color.WHITE);
            private final Circle innerDot = new Circle(3.3, Color.web("#6B3A19"));
            private final StackPane radioPane = new StackPane(outerCircle, innerDot);
            private final Label lblNom = new Label();
            private final HBox box = new HBox(10, radioPane, lblNom);

            {
                outerCircle.setStrokeWidth(1.5);
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Proveedor p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    TableRow<?> row = getTableRow();
                    boolean sel = row != null && row.isSelected();
                    outerCircle.setStroke(Color.web(sel ? "#6B3A19" : "#C5B6A6"));
                    innerDot.setVisible(sel);
                    lblNom.setText(p.getNombre());
                    lblNom.getStyleClass().removeAll("clientes-cell-nombre", "clientes-cell-nombre-selected");
                    lblNom.getStyleClass().add(sel ? "clientes-cell-nombre-selected" : "clientes-cell-nombre");
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // 2. Columna Telefono
        colTelefono.setCellValueFactory(data -> new SimpleObjectProperty<Proveedor>(data.getValue()));
        colTelefono.setCellFactory(col -> new TableCell<Proveedor, Proveedor>() {
            @Override
            protected void updateItem(Proveedor p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String tel = p.getTelefono() != null && !p.getTelefono().trim().isEmpty()
                            ? p.getTelefono().trim()
                            : "—";
                    setText(tel);
                    getStyleClass().removeAll("clientes-cell-telefono");
                    getStyleClass().add("clientes-cell-telefono");
                }
            }
        });

        // 3. Columna Que le compra
        colProductos.setCellValueFactory(data -> new SimpleObjectProperty<Proveedor>(data.getValue()));
        colProductos.setCellFactory(col -> new TableCell<Proveedor, Proveedor>() {
            @Override
            protected void updateItem(Proveedor p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String prod = p.getProductos() != null && !p.getProductos().trim().isEmpty()
                            ? p.getProductos().trim()
                            : "—";
                    setText(prod);
                    getStyleClass().removeAll("clientes-cell-telefono");
                    getStyleClass().add("clientes-cell-telefono");
                }
            }
        });

        // 4. Columna Le debo ($ 480.000 o $ 0)
        colLeDebo.setCellValueFactory(data -> new SimpleObjectProperty<Proveedor>(data.getValue()));
        colLeDebo.setCellFactory(col -> new TableCell<Proveedor, Proveedor>() {
            @Override
            protected void updateItem(Proveedor p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    BigDecimal deuda = p.getSaldoPendiente() != null ? p.getSaldoPendiente() : BigDecimal.ZERO;
                    setText("$ " + formatearMoneda(deuda));
                    getStyleClass().removeAll("clientes-cell-deuda-activa", "clientes-cell-deuda-cero");
                    getStyleClass().add(deuda.compareTo(BigDecimal.ZERO) > 0
                            ? "clientes-cell-deuda-activa"
                            : "clientes-cell-deuda-cero");
                }
            }
        });

        // 5. Columna Proximo pago ("8 de noviembre" o "—")
        colProximoPago.setCellValueFactory(data -> new SimpleObjectProperty<Proveedor>(data.getValue()));
        colProximoPago.setCellFactory(col -> new TableCell<Proveedor, Proveedor>() {
            @Override
            protected void updateItem(Proveedor p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    BigDecimal deuda = p.getSaldoPendiente() != null ? p.getSaldoPendiente() : BigDecimal.ZERO;
                    getStyleClass().removeAll("proveedores-cell-fecha", "proveedores-cell-fecha-alerta", "clientes-cell-dash");
                    if (deuda.compareTo(BigDecimal.ZERO) > 0 && p.getProximoPago() != null) {
                        setText(formatearDiaMes(p.getProximoPago()));
                        getStyleClass().add(venceEnSieteDias(p)
                                ? "proveedores-cell-fecha-alerta"
                                : "proveedores-cell-fecha");
                    } else {
                        setText("—");
                        getStyleClass().add("clientes-cell-dash");
                    }
                }
            }
        });

        // 6. Columna Aviso (pildora ambar "⚠ Vence esta semana" o "—")
        colAviso.setCellValueFactory(data -> new SimpleObjectProperty<Proveedor>(data.getValue()));
        colAviso.setCellFactory(col -> new TableCell<Proveedor, Proveedor>() {
            @Override
            protected void updateItem(Proveedor p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER);
                    if (venceEnSieteDias(p)) {
                        Label lblAviso = new Label("⚠  Vence esta\nsemana");
                        lblAviso.setTextAlignment(TextAlignment.CENTER);
                        lblAviso.setAlignment(Pos.CENTER);
                        lblAviso.getStyleClass().add("proveedores-pill-vence");
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

        tblProveedores.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            proveedorSeleccionado = newSel;
            actualizarBarraSeleccion(newSel);
            tblProveedores.refresh();
        });
    }

    private boolean venceEnSieteDias(Proveedor p) {
        if (p == null || !p.isActivo()) {
            return false;
        }
        BigDecimal deuda = p.getSaldoPendiente() != null ? p.getSaldoPendiente() : BigDecimal.ZERO;
        if (deuda.compareTo(BigDecimal.ZERO) <= 0 || p.getProximoPago() == null) {
            return false;
        }
        LocalDate limite = LocalDate.now().plusDays(7);
        return !p.getProximoPago().isAfter(limite);
    }

    public void cargarProveedoresDesdeMySQL() {
        todosLosProveedores.clear();
        BigDecimal sumaDeudaTotal = BigDecimal.ZERO;
        int conteoVencenSemana = 0;
        int conteoActivos = 0;

        try {
            List<Proveedor> desdeBD = proveedorDAO.listarTodos(false);
            if (desdeBD != null) {
                for (Proveedor p : desdeBD) {
                    todosLosProveedores.add(p);
                    if (p.isActivo()) {
                        conteoActivos++;
                        BigDecimal saldo = p.getSaldoPendiente() != null ? p.getSaldoPendiente() : BigDecimal.ZERO;
                        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
                            sumaDeudaTotal = sumaDeudaTotal.add(saldo);
                        }
                        if (venceEnSieteDias(p)) {
                            conteoVencenSemana++;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProveedoresController] Error al consultar proveedores en MySQL: " + e.getMessage());
        }

        if (lblTotalDeudaProveedores != null) {
            lblTotalDeudaProveedores.setText("$ " + formatearMoneda(sumaDeudaTotal));
        }
        if (lblConteoVencenSemana != null) {
            lblConteoVencenSemana.setText(String.valueOf(conteoVencenSemana));
        }
        if (lblSubVencenSemana != null) {
            lblSubVencenSemana.setText(conteoVencenSemana == 1
                    ? "PROVEEDOR PENDIENTE"
                    : "PROVEEDORES PENDIENTES");
        }
        if (lblConteoProveedoresActivos != null) {
            lblConteoProveedoresActivos.setText(String.valueOf(conteoActivos));
        }

        aplicarFiltros();
    }

    private void aplicarFiltros() {
        String criterio = txtBuscarProveedor != null && txtBuscarProveedor.getText() != null
                ? txtBuscarProveedor.getText().trim().toLowerCase(LOCALE_ES_CO)
                : "";
        Toggle filtroActivo = tgFiltros.getSelectedToggle();

        List<Proveedor> filtrados = new ArrayList<Proveedor>();
        int totalEnCategoria = 0;
        for (Proveedor p : todosLosProveedores) {
            if (!cumpleFiltroPildora(p, filtroActivo)) {
                continue;
            }
            totalEnCategoria++;
            if (!criterio.isEmpty()) {
                String nom = p.getNombre() != null ? p.getNombre().toLowerCase(LOCALE_ES_CO) : "";
                String prod = p.getProductos() != null ? p.getProductos().toLowerCase(LOCALE_ES_CO) : "";
                if (!nom.contains(criterio) && !prod.contains(criterio)) {
                    continue;
                }
            }
            filtrados.add(p);
        }

        proveedoresFiltrados.setAll(filtrados);

        if (lblConteoProveedores != null) {
            lblConteoProveedores.setText("Mostrando " + filtrados.size() + " de " + totalEnCategoria + " proveedores");
        }

        Proveedor objetivo = null;
        if (idProveedorSeleccionarAlCargar > 0) {
            for (Proveedor cand : filtrados) {
                if (cand.getIdProveedor() == idProveedorSeleccionarAlCargar) {
                    objetivo = cand;
                    break;
                }
            }
            idProveedorSeleccionarAlCargar = -1;
        }

        if (objetivo != null) {
            tblProveedores.getSelectionModel().select(objetivo);
        } else if (!proveedoresFiltrados.isEmpty()) {
            tblProveedores.getSelectionModel().selectFirst();
        } else {
            tblProveedores.getSelectionModel().clearSelection();
            actualizarBarraSeleccion(null);
        }
    }

    private boolean cumpleFiltroPildora(Proveedor p, Toggle filtro) {
        if (filtro == tbFiltroDadosBaja) {
            return !p.isActivo();
        }
        if (!p.isActivo()) {
            return false;
        }
        if (filtro == tbFiltroLesDebo) {
            BigDecimal deuda = p.getSaldoPendiente() != null ? p.getSaldoPendiente() : BigDecimal.ZERO;
            return deuda.compareTo(BigDecimal.ZERO) > 0;
        }
        return true; // Todos (activos)
    }

    private void actualizarBarraSeleccion(Proveedor sel) {
        boolean haySel = sel != null;
        BigDecimal deuda = haySel && sel.getSaldoPendiente() != null ? sel.getSaldoPendiente() : BigDecimal.ZERO;
        boolean tieneDeuda = haySel && deuda.compareTo(BigDecimal.ZERO) > 0;

        if (lblProveedorSeleccionadoInfo != null) {
            if (!haySel) {
                lblProveedorSeleccionadoInfo.setText("Seleccione un proveedor de la tabla para ver sus opciones");
            } else if (!sel.isActivo()) {
                lblProveedorSeleccionadoInfo.setText("Seleccionado: " + sel.getNombre() + " · Dado de baja");
            } else if (tieneDeuda) {
                lblProveedorSeleccionadoInfo.setText("Seleccionado: " + sel.getNombre()
                        + " · Le debe $ " + formatearMoneda(deuda));
            } else {
                lblProveedorSeleccionadoInfo.setText("Seleccionado: " + sel.getNombre() + " · Al día ($ 0)");
            }
        }

        if (btnVerCuenta != null) {
            btnVerCuenta.setDisable(!haySel);
        }
        if (btnRegistrarPago != null) {
            btnRegistrarPago.setDisable(!tieneDeuda);
        }
        if (btnRegistrarCompra != null) {
            btnRegistrarCompra.setDisable(!haySel || !sel.isActivo());
        }
        if (btnModificarDatos != null) {
            btnModificarDatos.setDisable(!haySel);
        }
    }

    @FXML
    public void handleRegistrarProveedorNuevo() {
        if (!verificarPermisoAdministradora()) {
            return;
        }
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/registrar_proveedor.fxml");
        }
    }

    @FXML
    public void handleRegistrarCompra() {
        if (!verificarPermisoAdministradora()) {
            return;
        }
        RegistrarCompraController.setProveedorInicial(proveedorSeleccionado);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            RegistrarCompraController ctrl = menu.cargarVistaConControlador("/fxml/registrar_compra.fxml");
            if (ctrl != null && proveedorSeleccionado != null) {
                ctrl.precargarProveedor(proveedorSeleccionado);
            }
        }
    }

    @FXML
    public void handleVerCuenta() {
        if (proveedorSeleccionado == null) {
            return;
        }
        CuentaProveedorController.setProveedorPendienteCuenta(proveedorSeleccionado);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            CuentaProveedorController ctrl = menu.cargarVistaConControlador("/fxml/cuenta_proveedor.fxml");
            if (ctrl != null) {
                ctrl.setProveedor(proveedorSeleccionado);
            }
        }
    }

    @FXML
    public void handleModificarDatos() {
        if (!verificarPermisoAdministradora()) {
            return;
        }
        if (proveedorSeleccionado == null) {
            return;
        }
        ModificarProveedorController.setProveedorPendienteEdicion(proveedorSeleccionado);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            ModificarProveedorController ctrl = menu.cargarVistaConControlador("/fxml/modificar_proveedor.fxml");
            if (ctrl != null) {
                ctrl.setProveedor(proveedorSeleccionado);
            }
        }
    }

    @FXML
    public void handleRegistrarPago() {
        if (!verificarPermisoAdministradora()) {
            return;
        }
        if (proveedorSeleccionado == null) {
            return;
        }
        BigDecimal deuda = proveedorSeleccionado.getSaldoPendiente() != null
                ? proveedorSeleccionado.getSaldoPendiente()
                : BigDecimal.ZERO;
        if (deuda.compareTo(BigDecimal.ZERO) <= 0) {
            AlertaUtil.mostrarInfo("Sin deuda pendiente", "No hay cuentas pendientes por pagar con este proveedor.");
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
            ctrl.setProveedor(proveedorSeleccionado);

            Stage stage = crearStageModal("Registrar pago a proveedor", root);
            stage.showAndWait();

            if (ctrl.isPagoGuardado()) {
                idProveedorSeleccionarAlCargar = proveedorSeleccionado.getIdProveedor();
                cargarProveedoresDesdeMySQL();
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