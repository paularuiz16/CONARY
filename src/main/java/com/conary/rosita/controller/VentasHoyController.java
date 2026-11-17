package com.conary.rosita.controller;

import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.VentaDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.DetalleVenta;
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
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Controlador de la Vista B de la Tanda 2 del Modulo 1: Historial "Ventas de hoy".
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class VentasHoyController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final DateTimeFormatter FMT_HORA_TABLA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<Integer> ventasConComprobanteEmitido = new HashSet<Integer>();

    public static void registrarComprobanteEmitido(int idMovimiento) {
        ventasConComprobanteEmitido.add(Integer.valueOf(idMovimiento));
    }

    public static boolean tieneComprobanteEmitido(int idMovimiento) {
        return ventasConComprobanteEmitido.contains(Integer.valueOf(idMovimiento));
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverVender;
    @FXML private Label lblFechaHoyHeader;
    @FXML private Circle circleEstadoCaja;
    @FXML private Label lblEstadoCaja;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRolBadge;

    // 3 Tarjetas de Resumen del Dia
    @FXML private Label lblActividadDia;
    @FXML private Label lblDineroEnCaja;
    @FXML private Label lblPendienteFiado;

    // Filtros y Buscador
    @FXML private ToggleButton tbFiltroTodas;
    @FXML private ToggleButton tbFiltroContado;
    @FXML private ToggleButton tbFiltroFiado;
    @FXML private ToggleButton tbFiltroAnuladas;
    @FXML private TextField txtBuscarVenta;

    // Tabla Principal
    @FXML private TableView<Venta> tblVentasHoy;
    @FXML private TableColumn<Venta, Venta> colHora;
    @FXML private TableColumn<Venta, Venta> colQueSeVendio;
    @FXML private TableColumn<Venta, Venta> colCliente;
    @FXML private TableColumn<Venta, Venta> colPago;
    @FXML private TableColumn<Venta, Venta> colTotal;
    @FXML private TableColumn<Venta, Venta> colEstado;

    // Barra Inferior de Seleccion
    @FXML private HBox barraSeleccionVenta;
    @FXML private Label lblVentaSeleccionadaInfo;
    @FXML private Button btnEmitirComprobante;
    @FXML private Button btnAnularVenta;

    private final VentaDAO ventaDAO = new VentaDAO();
    private final CajaDAO cajaDAO = new CajaDAO();

    private final ToggleGroup tgFiltros = new ToggleGroup();
    private final List<Venta> ventasDelDiaCompletas = new ArrayList<Venta>();
    private final ObservableList<Venta> ventasFiltradasTabla = FXCollections.observableArrayList();
    private final Map<Integer, String> resumenProductosPorVenta = new HashMap<Integer, String>();
    private final Map<Integer, String> textoBusquedaProductosPorVenta = new HashMap<Integer, String>();

    private Venta ventaSeleccionada;

    @FXML
    public void initialize() {
        configurarEncabezado();
        configurarFiltrosYBuscador();
        configurarTabla();
        cargarVentasDeHoyDesdeMySQL();
    }

    private void configurarEncabezado() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        String nombre = "Doña Rosita";
        String rolBadge = "ADMINISTRADORA";

        if (u != null) {
            String raw = u.getNombre() != null ? u.getNombre().trim() : "Usuario";
            if (raw.toLowerCase(LOCALE_ES_CO).contains("rosita")
                    && !raw.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
                nombre = "Doña " + raw;
            } else {
                nombre = raw;
            }
            rolBadge = u.esAdministrador() ? "ADMINISTRADORA" : "VENDEDORA";
        }

        if (lblUsuarioNombre != null) {
            lblUsuarioNombre.setText(nombre);
        }
        if (lblUsuarioRolBadge != null) {
            lblUsuarioRolBadge.setText(rolBadge);
        }
        if (lblFechaHoyHeader != null) {
            lblFechaHoyHeader.setText(formatearFechaLarga(LocalDate.now()));
        }

        try {
            Caja abierta = cajaDAO.obtenerCajaAbierta();
            SesionUsuario.setCajaActiva(abierta);
            boolean hayCajaAbierta = abierta != null;
            if (lblEstadoCaja != null) {
                lblEstadoCaja.setText(hayCajaAbierta ? "Caja abierta" : "Caja cerrada");
            }
            if (circleEstadoCaja != null) {
                circleEstadoCaja.setFill(Color.web(hayCajaAbierta ? "#1E7B42" : "#9E2A2B"));
            }
        } catch (SQLException e) {
            if (lblEstadoCaja != null) {
                lblEstadoCaja.setText("Caja abierta");
            }
        }
    }

    private void configurarFiltrosYBuscador() {
        ToggleButton[] botones = new ToggleButton[]{
                tbFiltroTodas, tbFiltroContado, tbFiltroFiado, tbFiltroAnuladas
        };
        for (ToggleButton tb : botones) {
            if (tb != null) {
                tb.setToggleGroup(tgFiltros);
            }
        }
        if (tbFiltroTodas != null) {
            tbFiltroTodas.setSelected(true);
        }

        tgFiltros.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
                return;
            }
            aplicarFiltrosYBusqueda();
        });

        if (txtBuscarVenta != null) {
            txtBuscarVenta.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltrosYBusqueda());
        }
    }

    private void configurarTabla() {
        tblVentasHoy.setItems(ventasFiltradasTabla);
        tblVentasHoy.setFixedCellSize(56.0);

        tblVentasHoy.setRowFactory(tv -> new TableRow<Venta>() {
            @Override
            protected void updateItem(Venta item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(56.0);
                setMinHeight(56.0);
                setMaxHeight(56.0);
            }
        });

        // 1. Columna HORA (con indicador '›' en la fila seleccionada)
        colHora.setCellValueFactory(data -> new SimpleObjectProperty<Venta>(data.getValue()));
        colHora.setCellFactory(col -> new TableCell<Venta, Venta>() {
            @Override
            protected void updateItem(Venta v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    TableRow<?> row = getTableRow();
                    boolean sel = row != null && row.isSelected();
                    String horaStr = v.getFecha() != null ? v.getFecha().format(FMT_HORA_TABLA) : "--:--";
                    setText(sel ? ("›  " + horaStr) : horaStr);
                    getStyleClass().removeAll("ventas-hoy-cell-hora", "ventas-hoy-cell-hora-selected", "ventas-hoy-cell-muted");
                    if (v.isAnulada()) {
                        getStyleClass().add("ventas-hoy-cell-muted");
                    } else {
                        getStyleClass().add(sel ? "ventas-hoy-cell-hora-selected" : "ventas-hoy-cell-hora");
                    }
                }
            }
        });

        // 2. Columna QUE SE VENDIO
        colQueSeVendio.setCellValueFactory(data -> new SimpleObjectProperty<Venta>(data.getValue()));
        colQueSeVendio.setCellFactory(col -> new TableCell<Venta, Venta>() {
            @Override
            protected void updateItem(Venta v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String resumen = resumenProductosPorVenta.get(Integer.valueOf(v.getIdMovimiento()));
                    if (resumen == null || resumen.isEmpty()) {
                        resumen = v.getDescripcion() != null ? v.getDescripcion() : ("Venta #" + v.getIdMovimiento());
                    }
                    setText(resumen);
                    getStyleClass().removeAll("ventas-hoy-cell-productos", "ventas-hoy-cell-muted");
                    getStyleClass().add(v.isAnulada() ? "ventas-hoy-cell-muted" : "ventas-hoy-cell-productos");
                }
            }
        });

        // 3. Columna CLIENTE
        colCliente.setCellValueFactory(data -> new SimpleObjectProperty<Venta>(data.getValue()));
        colCliente.setCellFactory(col -> new TableCell<Venta, Venta>() {
            @Override
            protected void updateItem(Venta v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String cli = obtenerNombreClienteLegible(v);
                    setText(cli);
                    getStyleClass().removeAll("ventas-hoy-cell-cliente", "ventas-hoy-cell-cliente-bold", "ventas-hoy-cell-muted");
                    if (v.isAnulada()) {
                        getStyleClass().add("ventas-hoy-cell-muted");
                    } else if (v.isEsFiado()) {
                        getStyleClass().add("ventas-hoy-cell-cliente-bold");
                    } else {
                        getStyleClass().add("ventas-hoy-cell-cliente");
                    }
                }
            }
        });

        // 4. Columna PAGO ("Contado" o pildora melocoton "Fiado")
        colPago.setCellValueFactory(data -> new SimpleObjectProperty<Venta>(data.getValue()));
        colPago.setCellFactory(col -> new TableCell<Venta, Venta>() {
            @Override
            protected void updateItem(Venta v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setGraphic(null);
                } else if (v.isEsFiado()) {
                    Label pill = new Label("Fiado");
                    pill.getStyleClass().add("ventas-hoy-pill-fiado");
                    HBox box = new HBox(pill);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText("Contado");
                    getStyleClass().removeAll("ventas-hoy-cell-cliente", "ventas-hoy-cell-muted");
                    getStyleClass().add(v.isAnulada() ? "ventas-hoy-cell-muted" : "ventas-hoy-cell-cliente");
                }
            }
        });

        // 5. Columna TOTAL ($ 10.000, o tachado en gris si esta anulada)
        colTotal.setCellValueFactory(data -> new SimpleObjectProperty<Venta>(data.getValue()));
        colTotal.setCellFactory(col -> new TableCell<Venta, Venta>() {
            @Override
            protected void updateItem(Venta v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    String precioStr = "$ " + formatearMoneda(v.getTotalVenta());
                    if (v.isAnulada()) {
                        Text tachado = new Text(precioStr);
                        tachado.setStrikethrough(true);
                        tachado.getStyleClass().add("ventas-hoy-total-strikethrough");
                        StackPane pane = new StackPane(tachado);
                        pane.setAlignment(Pos.CENTER_RIGHT);
                        setGraphic(pane);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(precioStr);
                        getStyleClass().removeAll("ventas-hoy-cell-total");
                        getStyleClass().add("ventas-hoy-cell-total");
                    }
                }
            }
        });

        // 6. Columna ESTADO (vacio si normal, "Con comprobante" o "Anulada")
        colEstado.setCellValueFactory(data -> new SimpleObjectProperty<Venta>(data.getValue()));
        colEstado.setCellFactory(col -> new TableCell<Venta, Venta>() {
            @Override
            protected void updateItem(Venta v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER);
                    if (v.isAnulada()) {
                        Label pill = new Label("Anulada");
                        pill.getStyleClass().add("ventas-hoy-pill-anulada");
                        HBox box = new HBox(pill);
                        box.setAlignment(Pos.CENTER);
                        setGraphic(box);
                        setText(null);
                    } else if (tieneComprobanteEmitido(v.getIdMovimiento())) {
                        Label pill = new Label("Con\ncomprobante");
                        pill.setTextAlignment(TextAlignment.CENTER);
                        pill.setAlignment(Pos.CENTER);
                        pill.getStyleClass().add("ventas-hoy-pill-comprobante");
                        HBox box = new HBox(pill);
                        box.setAlignment(Pos.CENTER);
                        setGraphic(box);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(null);
                    }
                }
            }
        });

        tblVentasHoy.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            ventaSeleccionada = newSel;
            actualizarBarraSeleccion(newSel);
            tblVentasHoy.refresh();
        });
    }

    public void cargarVentasDeHoyDesdeMySQL() {
        ventasDelDiaCompletas.clear();
        resumenProductosPorVenta.clear();
        textoBusquedaProductosPorVenta.clear();

        int conteoValidas = 0;
        BigDecimal totalContadoHoy = BigDecimal.ZERO;
        BigDecimal totalFiadoHoy = BigDecimal.ZERO;

        try {
            LocalDate hoy = LocalDate.now();
            List<Venta> listaBD = ventaDAO.listarVentas(hoy, hoy, null);
            if (listaBD != null) {
                for (Venta v : listaBD) {
                    if (v.getDescripcion() != null && v.getDescripcion().contains("[COMPROBANTE]")) {
                        registrarComprobanteEmitido(v.getIdMovimiento());
                    }
                    List<DetalleVenta> detalles = ventaDAO.obtenerDetallesVenta(v.getIdMovimiento());
                    v.setDetalles(detalles);
                    Integer key = Integer.valueOf(v.getIdMovimiento());
                    resumenProductosPorVenta.put(key, construirResumenQueSeVendio(detalles));
                    textoBusquedaProductosPorVenta.put(key, construirTextoBusquedaProductos(detalles));

                    ventasDelDiaCompletas.add(v);

                    if (!v.isAnulada()) {
                        conteoValidas++;
                        BigDecimal monto = v.getTotalVenta() != null ? v.getTotalVenta() : BigDecimal.ZERO;
                        if (v.isEsFiado()) {
                            totalFiadoHoy = totalFiadoHoy.add(monto);
                        } else {
                            totalContadoHoy = totalContadoHoy.add(monto);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[VentasHoyController] Error al consultar ventas de hoy en MySQL: " + e.getMessage());
        }

        if (lblActividadDia != null) {
            lblActividadDia.setText(conteoValidas == 1 ? "1 venta" : (conteoValidas + " ventas"));
        }
        if (lblDineroEnCaja != null) {
            lblDineroEnCaja.setText("Entró a la caja $ " + formatearMoneda(totalContadoHoy));
        }
        if (lblPendienteFiado != null) {
            lblPendienteFiado.setText("Quedó a fiado $ " + formatearMoneda(totalFiadoHoy));
        }

        aplicarFiltrosYBusqueda();
    }

    private void aplicarFiltrosYBusqueda() {
        String criterio = txtBuscarVenta != null && txtBuscarVenta.getText() != null
                ? txtBuscarVenta.getText().trim().toLowerCase(LOCALE_ES_CO)
                : "";
        Toggle filtroActivo = tgFiltros.getSelectedToggle();

        List<Venta> resultado = new ArrayList<Venta>();
        for (Venta v : ventasDelDiaCompletas) {
            if (filtroActivo == tbFiltroContado && (v.isEsFiado() || v.isAnulada())) {
                continue;
            }
            if (filtroActivo == tbFiltroFiado && (!v.isEsFiado() || v.isAnulada())) {
                continue;
            }
            if (filtroActivo == tbFiltroAnuladas && !v.isAnulada()) {
                continue;
            }

            if (!criterio.isEmpty()) {
                String cli = obtenerNombreClienteLegible(v).toLowerCase(LOCALE_ES_CO);
                String prods = textoBusquedaProductosPorVenta.get(Integer.valueOf(v.getIdMovimiento()));
                if (prods == null) {
                    prods = "";
                }
                if (!cli.contains(criterio) && !prods.contains(criterio)) {
                    continue;
                }
            }
            resultado.add(v);
        }

        ventasFiltradasTabla.setAll(resultado);
        if (!ventasFiltradasTabla.isEmpty()) {
            tblVentasHoy.getSelectionModel().selectFirst();
        } else {
            tblVentasHoy.getSelectionModel().clearSelection();
            actualizarBarraSeleccion(null);
        }
    }

    private void actualizarBarraSeleccion(Venta sel) {
        boolean haySel = sel != null;
        boolean activaNoAnulada = haySel && !sel.isAnulada();
        boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();

        if (lblVentaSeleccionadaInfo != null) {
            if (!haySel) {
                lblVentaSeleccionadaInfo.setText("Seleccione una venta de la tabla para ver sus opciones");
            } else {
                String horaStr = sel.getFecha() != null ? sel.getFecha().format(FMT_HORA_TABLA) : "--:--";
                String cliStr = obtenerNombreClienteLegible(sel);
                String totalStr = "$ " + formatearMoneda(sel.getTotalVenta());
                String sufijoAnulada = sel.isAnulada() ? " · (Anulada)" : "";
                lblVentaSeleccionadaInfo.setText("SELECCIONADA:  venta de las " + horaStr
                        + "  ·  " + cliStr + "  ·  " + totalStr + sufijoAnulada);
            }
        }

        if (btnEmitirComprobante != null) {
            btnEmitirComprobante.setDisable(!activaNoAnulada);
        }
        if (btnAnularVenta != null) {
            btnAnularVenta.setDisable(!activaNoAnulada || !esAdmin);
        }
    }

    private String construirResumenQueSeVendio(List<DetalleVenta> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return "Venta en mostrador";
        }
        DetalleVenta primero = detalles.get(0);
        String base = primero.getNombreProducto() != null ? primero.getNombreProducto() : "Producto";
        if (primero.getCantidad() > 1) {
            base = base + " ×" + primero.getCantidad();
        }
        int adicionales = detalles.size() - 1;
        if (adicionales > 0) {
            return base + " y " + adicionales + " más";
        }
        return base;
    }

    private String construirTextoBusquedaProductos(List<DetalleVenta> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (DetalleVenta d : detalles) {
            if (d.getNombreProducto() != null) {
                sb.append(d.getNombreProducto().toLowerCase(LOCALE_ES_CO)).append(" ");
            }
        }
        return sb.toString();
    }

    private String obtenerNombreClienteLegible(Venta v) {
        if (v == null || v.getNombreCliente() == null) {
            return "Público";
        }
        String raw = v.getNombreCliente().trim();
        if (raw.isEmpty()
                || "CLIENTE GENERAL".equalsIgnoreCase(raw)
                || raw.toLowerCase(LOCALE_ES_CO).contains("público")) {
            return "Público";
        }
        return raw;
    }

    @FXML
    public void handleEmitirComprobante() {
        if (ventaSeleccionada == null || ventaSeleccionada.isAnulada()) {
            return;
        }
        ComprobanteVentaController.setVentaPendienteComprobante(ventaSeleccionada);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            ComprobanteVentaController ctrl = menu.cargarVistaConControlador("/fxml/comprobante_venta.fxml");
            if (ctrl != null) {
                ctrl.setVenta(ventaSeleccionada);
            }
        }
    }

    @FXML
    public void handleAnularVenta() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede anular una venta registrada.");
            return;
        }
        if (ventaSeleccionada == null || ventaSeleccionada.isAnulada()) {
            return;
        }

        try {
            FXMLLoader loader = crearLoaderFXML("/fxml/modal_anular_venta.fxml");
            Parent modalRoot = loader.load();
            AnularVentaController ctrl = loader.getController();
            if (ctrl != null) {
                String resumenProds = resumenProductosPorVenta.get(Integer.valueOf(ventaSeleccionada.getIdMovimiento()));
                ctrl.setVenta(ventaSeleccionada, resumenProds);
            }

            Stage modalStage = crearStageModal("¿Anular esta venta?", modalRoot);
            modalStage.showAndWait();

            if (ctrl != null && ctrl.isAnulacionConfirmada()) {
                cargarVentasDeHoyDesdeMySQL();
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarError("Error de Ventana",
                    "No se pudo abrir la ventana para anular la venta: " + e.getMessage());
        }
    }

    private FXMLLoader crearLoaderFXML(String fxmlPath) throws IOException {
        URL url = getClass().getResource(fxmlPath);
        if (url == null) {
            File fallback = new File("src/main/resources" + fxmlPath);
            if (fallback.exists()) {
                url = fallback.toURI().toURL();
            }
        }
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
        if (cssUrl == null) {
            File cssFallback = new File("src/main/resources/css/styles.css");
            if (cssFallback.exists()) {
                try {
                    cssUrl = cssFallback.toURI().toURL();
                } catch (IOException ignored) {
                }
            }
        }
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        stage.setScene(scene);
        return stage;
    }

    @FXML
    public void handleVolverAVender() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/vender.fxml");
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

    private String formatearFechaLarga(LocalDate fecha) {
        LocalDate f = fecha != null ? fecha : LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", LOCALE_ES_CO);
        String txt = f.format(fmt);
        if (txt == null || txt.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(txt.charAt(0)) + txt.substring(1);
    }
}
