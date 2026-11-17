package com.conary.rosita.controller;

import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.CompraDAO;
import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.dao.ProveedorDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Compra;
import com.conary.rosita.model.DetalleCompra;
import com.conary.rosita.model.Producto;
import com.conary.rosita.model.Proveedor;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista A de la Tanda 2 del Modulo 5: Registrar compra.
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class RegistrarCompraController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static Proveedor proveedorInicialPendiente;

    public static void setProveedorInicial(Proveedor proveedor) {
        proveedorInicialPendiente = proveedor;
    }

    private static class ItemLineaCompra {
        private final Producto producto;
        private int cantidad;
        private BigDecimal costoUnitario;

        ItemLineaCompra(Producto producto, int cantidad, BigDecimal costoUnitario) {
            this.producto = producto;
            this.cantidad = Math.max(1, cantidad);
            this.costoUnitario = costoUnitario != null ? costoUnitario : BigDecimal.ZERO;
        }

        BigDecimal getSubtotal() {
            return costoUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverProveedores;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;

    // Paso 1: Proveedor y Factura
    @FXML private ComboBox<Proveedor> cmbProveedor;
    @FXML private Button btnNuevoProveedor;
    @FXML private TextField txtNumeroFactura;

    // Paso 2: Catalogo de productos
    @FXML private TextField txtBuscarProductoCompra;
    @FXML private Button btnLimpiarBusquedaProd;
    @FXML private VBox vboxResultadosCatalogo;
    @FXML private Button btnRegistrarProductoNuevo;

    // Columna derecha: Lo que compro y Paso 3
    @FXML private Label lblConteoProductosCargados;
    @FXML private VBox vboxItemsCompra;
    @FXML private Label lblTotalCompra;

    @FXML private ToggleButton tbPagoDespues;
    @FXML private ToggleButton tbPagoYa;
    @FXML private VBox vboxCredito;
    @FXML private DatePicker dpFechaLimitePago;

    @FXML private VBox vboxContado;
    @FXML private ToggleButton tbOrigenCaja;
    @FXML private ToggleButton tbOrigenOtro;
    @FXML private HBox boxAlertaCajaCerrada;
    @FXML private Label lblAlertaCajaCerrada;
    @FXML private Label lblNotaOrigenPago;

    @FXML private Button btnCancelarCompra;
    @FXML private Button btnGuardarCompra;

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CompraDAO compraDAO = new CompraDAO();
    private final CajaDAO cajaDAO = new CajaDAO();

    private final ToggleGroup tgFormaPago = new ToggleGroup();
    private final ToggleGroup tgOrigenPago = new ToggleGroup();

    private final List<Producto> catalogoProductos = new ArrayList<Producto>();
    private final List<ItemLineaCompra> lineasCompra = new ArrayList<ItemLineaCompra>();
    private Caja cajaAbiertaHoy;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarComboProveedores();
        configurarBuscadorProductos();
        configurarFormaDePago();
        cargarProveedoresDesdeMySQL();
        cargarCatalogoProductosDesdeMySQL();
        renderizarLineasCompra();

        if (proveedorInicialPendiente != null) {
            precargarProveedor(proveedorInicialPendiente);
            proveedorInicialPendiente = null;
        }
    }

    public void precargarProveedor(Proveedor prov) {
        if (prov == null || cmbProveedor == null) {
            return;
        }
        for (Proveedor item : cmbProveedor.getItems()) {
            if (item.getIdProveedor() == prov.getIdProveedor()) {
                cmbProveedor.getSelectionModel().select(item);
                return;
            }
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

    private void configurarComboProveedores() {
        if (cmbProveedor == null) {
            return;
        }
        cmbProveedor.setCellFactory(lv -> new ListCell<Proveedor>() {
            @Override
            protected void updateItem(Proveedor p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getNombre());
            }
        });
        cmbProveedor.setButtonCell(new ListCell<Proveedor>() {
            @Override
            protected void updateItem(Proveedor p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "Seleccione un proveedor..." : p.getNombre());
            }
        });
    }

    private void cargarProveedoresDesdeMySQL() {
        if (cmbProveedor == null) {
            return;
        }
        Proveedor actual = cmbProveedor.getValue();
        try {
            List<Proveedor> activos = proveedorDAO.listarTodos(true);
            cmbProveedor.getItems().setAll(activos);
            if (actual != null) {
                precargarProveedor(actual);
            }
        } catch (SQLException e) {
            System.err.println("[RegistrarCompraController] Error cargando proveedores: " + e.getMessage());
        }
    }

    private void configurarBuscadorProductos() {
        if (txtBuscarProductoCompra != null) {
            txtBuscarProductoCompra.textProperty().addListener((obs, oldVal, newVal) -> filtrarCatalogo());
        }
    }

    private void cargarCatalogoProductosDesdeMySQL() {
        catalogoProductos.clear();
        try {
            List<Producto> activos = productoDAO.listarTodos(true);
            if (activos != null) {
                catalogoProductos.addAll(activos);
            }
        } catch (SQLException e) {
            System.err.println("[RegistrarCompraController] Error cargando productos: " + e.getMessage());
        }
        filtrarCatalogo();
    }

    private void filtrarCatalogo() {
        if (vboxResultadosCatalogo == null) {
            return;
        }
        vboxResultadosCatalogo.getChildren().clear();

        String criterio = txtBuscarProductoCompra != null && txtBuscarProductoCompra.getText() != null
                ? txtBuscarProductoCompra.getText().trim().toLowerCase(LOCALE_ES_CO)
                : "";

        int mostrados = 0;
        for (final Producto prod : catalogoProductos) {
            String codigo = String.format("PRD-%03d", Integer.valueOf(prod.getIdProducto())).toLowerCase(LOCALE_ES_CO);
            String nombre = prod.getNombre() != null ? prod.getNombre().toLowerCase(LOCALE_ES_CO) : "";
            String categoria = prod.getCategoria() != null ? prod.getCategoria().toLowerCase(LOCALE_ES_CO) : "";

            if (!criterio.isEmpty()
                    && !nombre.contains(criterio)
                    && !codigo.contains(criterio)
                    && !categoria.contains(criterio)) {
                continue;
            }

            vboxResultadosCatalogo.getChildren().add(crearFilaCatalogo(prod));
            mostrados++;
            if (mostrados >= 25) {
                break;
            }
        }

        if (mostrados == 0) {
            Label vacio = new Label("No se encontraron productos con ese criterio.");
            vacio.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #7A5C43; -fx-padding: 14;");
            vboxResultadosCatalogo.getChildren().add(vacio);
        }
    }

    private HBox crearFilaCatalogo(final Producto prod) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("compra-catalog-row");

        VBox info = new VBox(2);
        Label lblNom = new Label(prod.getNombre());
        lblNom.setStyle("-fx-font-size: 14.5px; -fx-font-weight: bold; -fx-text-fill: #3E1E0B;");
        String codigo = String.format("PRD-%03d", Integer.valueOf(prod.getIdProducto()));
        Label lblStock = new Label(codigo + "  ·  Existencias actuales: " + prod.getStockActual() + " unidades");
        lblStock.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #6B4C35;");
        info.getChildren().addAll(lblNom, lblStock);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAgregar = new Button("+ Agregar");
        btnAgregar.getStyleClass().add("compra-btn-agregar");
        btnAgregar.setOnAction(ev -> agregarProductoALaCompra(prod));

        row.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                agregarProductoALaCompra(prod);
            }
        });

        row.getChildren().addAll(info, spacer, btnAgregar);
        return row;
    }

    private void agregarProductoALaCompra(Producto prod) {
        if (prod == null) {
            return;
        }
        for (ItemLineaCompra linea : lineasCompra) {
            if (linea.producto.getIdProducto() == prod.getIdProducto()) {
                linea.cantidad++;
                renderizarLineasCompra();
                return;
            }
        }
        BigDecimal costoInicial = prod.getPrecioCompra() != null && prod.getPrecioCompra().compareTo(BigDecimal.ZERO) > 0
                ? prod.getPrecioCompra()
                : BigDecimal.ZERO;
        lineasCompra.add(new ItemLineaCompra(prod, 1, costoInicial));
        renderizarLineasCompra();
    }

    private void renderizarLineasCompra() {
        if (vboxItemsCompra == null) {
            return;
        }
        vboxItemsCompra.getChildren().clear();

        if (lineasCompra.isEmpty()) {
            Label vacio = new Label("Todavía no ha agregado productos. Busque a la izquierda y pulse '+ Agregar'.");
            vacio.setWrapText(true);
            vacio.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #7A5C43; -fx-padding: 18;");
            vboxItemsCompra.getChildren().add(vacio);
        } else {
            for (final ItemLineaCompra linea : lineasCompra) {
                vboxItemsCompra.getChildren().add(crearTarjetaItemCompra(linea));
            }
        }

        actualizarTotales();
    }

    private VBox crearTarjetaItemCompra(final ItemLineaCompra linea) {
        VBox card = new VBox(8);
        card.getStyleClass().add("compra-item-card");

        // Fila 1: Nombre del producto + boton Quitar
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label lblNombre = new Label(linea.producto.getNombre());
        lblNombre.getStyleClass().add("compra-item-name");
        lblNombre.setWrapText(true);
        Region spTop = new Region();
        HBox.setHgrow(spTop, Priority.ALWAYS);
        Button btnQuitar = new Button("✕ Quitar");
        btnQuitar.getStyleClass().add("compra-item-quitar");
        btnQuitar.setOnAction(ev -> {
            lineasCompra.remove(linea);
            renderizarLineasCompra();
        });
        topRow.getChildren().addAll(lblNombre, spTop, btnQuitar);

        // Fila 2: Cantidad (- [cant] +), Costo c/u editable, Subtotal
        HBox bottomRow = new HBox(12);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        // Controles de cantidad
        HBox qtyBox = new HBox(4);
        qtyBox.setAlignment(Pos.CENTER_LEFT);
        qtyBox.getStyleClass().add("compra-qty-box");
        Button btnMenos = new Button("−");
        btnMenos.getStyleClass().add("compra-qty-btn");
        final TextField txtCant = new TextField(String.valueOf(linea.cantidad));
        txtCant.getStyleClass().add("compra-qty-input");
        Button btnMas = new Button("+");
        btnMas.getStyleClass().add("compra-qty-btn");
        qtyBox.getChildren().addAll(btnMenos, txtCant, btnMas);

        // Campo Costo c/u
        VBox costoBox = new VBox(2);
        Label lblCostoTitle = new Label("Costo c/u");
        lblCostoTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #7A5C43;");
        HBox costoInputRow = new HBox(4);
        costoInputRow.setAlignment(Pos.CENTER_LEFT);
        Label lblPeso = new Label("$");
        lblPeso.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #46220F;");
        final TextField txtCosto = new TextField(formatearMoneda(linea.costoUnitario));
        txtCosto.getStyleClass().add("compra-cost-input");
        costoInputRow.getChildren().addAll(lblPeso, txtCosto);
        costoBox.getChildren().addAll(lblCostoTitle, costoInputRow);

        Region spBottom = new Region();
        HBox.setHgrow(spBottom, Priority.ALWAYS);

        // Subtotal
        VBox subBox = new VBox(2);
        subBox.setAlignment(Pos.CENTER_RIGHT);
        Label lblSubTitle = new Label("Subtotal");
        lblSubTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #7A5C43;");
        final Label lblSubVal = new Label("$ " + formatearMoneda(linea.getSubtotal()));
        lblSubVal.setStyle("-fx-font-family: \"Georgia\", \"Times New Roman\", serif; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3E1E0B;");
        subBox.getChildren().addAll(lblSubTitle, lblSubVal);

        // Eventos en vivo de cantidad y costo
        btnMenos.setOnAction(ev -> {
            if (linea.cantidad > 1) {
                linea.cantidad--;
                txtCant.setText(String.valueOf(linea.cantidad));
                lblSubVal.setText("$ " + formatearMoneda(linea.getSubtotal()));
                actualizarTotales();
            }
        });
        btnMas.setOnAction(ev -> {
            linea.cantidad++;
            txtCant.setText(String.valueOf(linea.cantidad));
            lblSubVal.setText("$ " + formatearMoneda(linea.getSubtotal()));
            actualizarTotales();
        });
        txtCant.textProperty().addListener((obs, oldVal, newVal) -> {
            String digitos = newVal != null ? newVal.replaceAll("[^0-9]", "") : "";
            if (!digitos.isEmpty()) {
                try {
                    int val = Integer.parseInt(digitos);
                    if (val > 0) {
                        linea.cantidad = val;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            lblSubVal.setText("$ " + formatearMoneda(linea.getSubtotal()));
            actualizarTotales();
        });
        txtCosto.textProperty().addListener((obs, oldVal, newVal) -> {
            BigDecimal parsed = parsearMonto(newVal);
            linea.costoUnitario = parsed != null && parsed.compareTo(BigDecimal.ZERO) >= 0 ? parsed : BigDecimal.ZERO;
            lblSubVal.setText("$ " + formatearMoneda(linea.getSubtotal()));
            actualizarTotales();
        });

        bottomRow.getChildren().addAll(qtyBox, costoBox, spBottom, subBox);
        card.getChildren().addAll(topRow, bottomRow);
        return card;
    }

    private void actualizarTotales() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemLineaCompra l : lineasCompra) {
            total = total.add(l.getSubtotal());
        }
        if (lblTotalCompra != null) {
            lblTotalCompra.setText("$ " + formatearMoneda(total));
        }
        if (lblConteoProductosCargados != null) {
            int n = lineasCompra.size();
            lblConteoProductosCargados.setText(n == 1 ? "1 producto cargado" : (n + " productos cargados"));
        }
    }

    private void configurarFormaDePago() {
        if (tbPagoDespues != null) tbPagoDespues.setToggleGroup(tgFormaPago);
        if (tbPagoYa != null) tbPagoYa.setToggleGroup(tgFormaPago);
        if (tbPagoDespues != null) tbPagoDespues.setSelected(true);

        if (tbOrigenCaja != null) tbOrigenCaja.setToggleGroup(tgOrigenPago);
        if (tbOrigenOtro != null) tbOrigenOtro.setToggleGroup(tgOrigenPago);

        if (dpFechaLimitePago != null) {
            dpFechaLimitePago.setValue(LocalDate.now().plusDays(30));
        }

        tgFormaPago.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }
            actualizarVisibilidadPago();
        });

        tgOrigenPago.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }
            actualizarNotaOrigenPago();
        });

        actualizarVisibilidadPago();
    }

    private void actualizarVisibilidadPago() {
        Toggle sel = tgFormaPago.getSelectedToggle();
        boolean esCredito = sel == tbPagoDespues;

        if (vboxCredito != null) {
            vboxCredito.setVisible(esCredito);
            vboxCredito.setManaged(esCredito);
        }
        if (vboxContado != null) {
            vboxContado.setVisible(!esCredito);
            vboxContado.setManaged(!esCredito);
        }
        if (!esCredito) {
            verificarCajaAbiertaHoy();
        }
    }

    private void verificarCajaAbiertaHoy() {
        cajaAbiertaHoy = null;
        try {
            Caja abierta = cajaDAO.obtenerCajaAbierta();
            if (abierta != null && abierta.getFechaApertura() != null
                    && !abierta.getFechaApertura().toLocalDate().isBefore(LocalDate.now())) {
                cajaAbiertaHoy = abierta;
            }
        } catch (SQLException e) {
            System.err.println("[RegistrarCompraController] Error verificando caja: " + e.getMessage());
        }

        boolean hayCajaHoy = cajaAbiertaHoy != null;
        if (tbOrigenCaja != null) {
            tbOrigenCaja.setDisable(!hayCajaHoy);
            if (!hayCajaHoy && tbOrigenCaja.isSelected() && tbOrigenOtro != null) {
                tbOrigenOtro.setSelected(true);
            } else if (hayCajaHoy && tgOrigenPago.getSelectedToggle() == null) {
                tbOrigenCaja.setSelected(true);
            }
        }
        if (!hayCajaHoy && tbOrigenOtro != null && tgOrigenPago.getSelectedToggle() == null) {
            tbOrigenOtro.setSelected(true);
        }

        if (boxAlertaCajaCerrada != null) {
            boxAlertaCajaCerrada.setVisible(!hayCajaHoy);
            boxAlertaCajaCerrada.setManaged(!hayCajaHoy);
        }
        actualizarNotaOrigenPago();
    }

    private void actualizarNotaOrigenPago() {
        if (lblNotaOrigenPago == null) {
            return;
        }
        if (tgOrigenPago.getSelectedToggle() == tbOrigenCaja) {
            lblNotaOrigenPago.setText("El monto se descontará automáticamente del efectivo esperado al cerrar la caja de hoy.");
        } else {
            lblNotaOrigenPago.setText("Pago externo (transferencia o dinero aparte): salda la factura sin tocar el efectivo de la caja.");
        }
    }

    @FXML
    public void handleLimpiarBusqueda() {
        if (txtBuscarProductoCompra != null) {
            txtBuscarProductoCompra.clear();
            txtBuscarProductoCompra.requestFocus();
        }
    }

    @FXML
    public void handleNuevoProveedor() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            boolean ok = AlertaUtil.mostrarConfirmacion("Ir a Registrar Proveedor",
                    "Se abrirá el formulario de nuevo proveedor y se perderán los datos actuales de esta compra.\n¿Desea continuar?");
            if (ok) {
                menu.cargarVistaEnContenido("/fxml/registrar_proveedor.fxml");
            }
        }
    }

    @FXML
    public void handleRegistrarProductoNuevo() {
        try {
            URL url = getClass().getResource("/fxml/registrar_producto.fxml");
            if (url == null) {
                throw new IOException("No se encontró /fxml/registrar_producto.fxml");
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            RegistrarProductoController ctrl = loader.getController();
            ctrl.setModoModal(true);

            Stage stage = new Stage();
            stage.setTitle("Registrar producto nuevo");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.DECORATED);
            stage.setWidth(920);
            stage.setHeight(680);

            if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                Window owner = rootPane.getScene().getWindow();
                stage.initOwner(owner);
                Node ownerRoot = rootPane.getScene().getRoot();
                ColorAdjust dim = new ColorAdjust(0, 0, -0.22, 0);
                stage.setOnShown(ev -> { if (ownerRoot != null) ownerRoot.setEffect(dim); });
                stage.setOnHidden(ev -> { if (ownerRoot != null) ownerRoot.setEffect(null); });
            }

            Scene scene = new Scene(root);
            URL cssUrl = getClass().getResource("/css/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            stage.setScene(scene);
            stage.showAndWait();

            Producto creado = ctrl.getProductoCreadoModal();
            cargarCatalogoProductosDesdeMySQL();
            if (creado != null) {
                agregarProductoALaCompra(creado);
            }
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error de Interfaz",
                    "No se pudo abrir el formulario de producto nuevo: " + e.getMessage());
        }
    }

    @FXML
    public void handleGuardarCompra() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede registrar compras.");
            return;
        }

        Proveedor prov = cmbProveedor != null ? cmbProveedor.getValue() : null;
        if (prov == null) {
            AlertaUtil.mostrarAdvertencia("Falta el proveedor", "Seleccione el proveedor al que le realizó la compra.");
            if (cmbProveedor != null) cmbProveedor.requestFocus();
            return;
        }

        if (lineasCompra.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin productos", "Agregue al menos un producto a la factura de compra.");
            return;
        }

        for (ItemLineaCompra l : lineasCompra) {
            if (l.cantidad <= 0) {
                AlertaUtil.mostrarAdvertencia("Cantidad inválida",
                        "La cantidad de «" + l.producto.getNombre() + "» debe ser mayor a 0.");
                return;
            }
            if (l.costoUnitario == null || l.costoUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                AlertaUtil.mostrarAdvertencia("Costo inválido",
                        "Escriba un costo unitario mayor a $ 0 para «" + l.producto.getNombre() + "».");
                return;
            }
        }

        boolean esCredito = tgFormaPago.getSelectedToggle() == tbPagoDespues;
        LocalDate fechaLimite = null;
        boolean pagadaDeContado = !esCredito;
        Integer idCajaPago = null;

        if (esCredito) {
            fechaLimite = dpFechaLimitePago != null ? dpFechaLimitePago.getValue() : null;
            if (fechaLimite == null) {
                AlertaUtil.mostrarAdvertencia("Falta fecha límite",
                        "Seleccione la fecha límite de pago para esta compra a crédito.");
                if (dpFechaLimitePago != null) dpFechaLimitePago.requestFocus();
                return;
            }
        } else {
            if (tgOrigenPago.getSelectedToggle() == tbOrigenCaja) {
                verificarCajaAbiertaHoy();
                if (cajaAbiertaHoy == null) {
                    AlertaUtil.mostrarAdvertencia("Caja cerrada",
                            "No hay una caja abierta hoy. Abra la caja primero o seleccione 'De otro lado'.");
                    return;
                }
                idCajaPago = Integer.valueOf(cajaAbiertaHoy.getIdCaja());
            }
        }

        Usuario u = SesionUsuario.getUsuarioLogueado();
        int idUsuario = u != null && u.getIdUsuario() > 0 ? u.getIdUsuario() : 1;
        String numFactura = txtNumeroFactura != null && txtNumeroFactura.getText() != null
                ? txtNumeroFactura.getText().trim()
                : "";

        Compra compra = new Compra();
        compra.setFecha(LocalDateTime.now());
        compra.setIdUsuario(idUsuario);
        compra.setIdCaja(idCajaPago);
        compra.setIdProveedor(prov.getIdProveedor());
        compra.setNombreProveedor(prov.getNombre());
        compra.setNumeroFacturaProveedor(numFactura.isEmpty() ? null : numFactura);
        compra.setFechaPagoAcordada(fechaLimite);
        compra.setDescripcion("Compra a " + prov.getNombre()
                + (numFactura.isEmpty() ? "" : " · Fact. " + numFactura));

        for (ItemLineaCompra l : lineasCompra) {
            compra.agregarDetalle(new DetalleCompra(
                    l.producto.getIdProducto(),
                    l.producto.getNombre(),
                    l.cantidad,
                    l.costoUnitario
            ));
        }

        try {
            if (compraDAO.registrarCompraConPago(compra, pagadaDeContado, idCajaPago)) {
                ProveedoresController.setIdProveedorSeleccionarAlCargar(prov.getIdProveedor());
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                    menu.cargarVistaEnContenido("/fxml/proveedores.fxml");
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Guardar Compra",
                    "No se pudo registrar la compra en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/proveedores.fxml");
        }
    }

    private BigDecimal parsearMonto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpio = texto.replace("$", "").replace(".", "").replace(" ", "").replace(",", ".").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols).format(monto);
    }
}