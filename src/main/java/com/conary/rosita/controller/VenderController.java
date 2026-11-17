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
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controlador del Modulo 1: Vender (Mostrador Principal) — Tanda 1:
 * Catalogo rapido, Carrito de venta actual y Formas de Pago (Contado y Fiado).
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class VenderController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverMenu;
    @FXML private Label lblEstadoCajaHeader;
    @FXML private Button btnHistorialHoy;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRolBadge;

    // Columna Izquierda: Buscador, Categorias y Catalogo
    @FXML private TextField txtBuscarProducto;
    @FXML private Button btnLimpiarBusqueda;
    @FXML private ToggleButton tbCatTodos;
    @FXML private ToggleButton tbCatCuadernos;
    @FXML private ToggleButton tbCatEscritura;
    @FXML private ToggleButton tbCatPapel;
    @FXML private ToggleButton tbCatOficina;
    @FXML private ToggleButton tbCatOtros;
    @FXML private Label lblTituloProductosEncontrados;
    @FXML private VBox boxListaProductos;

    // Columna Derecha: Carrito (Venta actual) y Forma de Pago
    @FXML private Label lblBadgeCantidadCarrito;
    @FXML private Button btnVaciarVenta;
    @FXML private VBox boxItemsCarrito;

    @FXML private ToggleButton tbPagoContado;
    @FXML private ToggleButton tbPagoFiado;

    // Modo Contado
    @FXML private VBox panelPagoContado;
    @FXML private Label lblTotalCobrarContado;
    @FXML private TextField txtValorRecibido;
    @FXML private HBox boxCambioDevolver;
    @FXML private Label lblTituloCambio;
    @FXML private Label lblValorCambio;
    @FXML private Button btnTerminarVentaContado;

    // Modo Fiado
    @FXML private VBox panelPagoFiado;
    @FXML private TextField txtBuscarClienteFiado;
    @FXML private VBox boxSugerenciasClientes;
    @FXML private VBox cardClienteFiado;
    @FXML private Label lblNombreClienteFiado;
    @FXML private Label lblBadgeEstadoFiado;
    @FXML private Label lblCupoDisponible;
    @FXML private Label lblDeudaActual;
    @FXML private Label lblDeudaDespuesVenta;
    @FXML private Label lblAlertaCupoFiado;
    @FXML private Label lblTotalCobrarFiado;
    @FXML private Button btnTerminarVentaFiado;

    private final CajaDAO cajaDAO = new CajaDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    private final ToggleGroup tgCategorias = new ToggleGroup();
    private final ToggleGroup tgFormaPago = new ToggleGroup();

    private final List<Producto> catalogoActivos = new ArrayList<Producto>();
    private final List<Cliente> clientesActivos = new ArrayList<Cliente>();
    private final Map<Integer, DetalleVenta> carrito = new LinkedHashMap<Integer, DetalleVenta>();
    private final Map<Integer, Producto> mapaProductosPorId = new LinkedHashMap<Integer, Producto>();

    private Caja cajaAbiertaHoy;
    private Cliente clienteFiadoSeleccionado;
    private BigDecimal totalVentaActual = BigDecimal.ZERO;
    private boolean actualizandoBuscadorCliente = false;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarGruposSeleccion();
        configurarListeners();
        if (!verificarCajaAbiertaHoy()) {
            return;
        }
        recargarDatosDesdeMySQL();
    }

    private void configurarEncabezadoUsuario() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        String nombre = "Doña Rosita";
        String badgeRol = "ADMIN";

        if (u != null) {
            String raw = u.getNombre() != null ? u.getNombre().trim() : "Usuario";
            if (raw.toLowerCase(LOCALE_ES_CO).contains("rosita")
                    && !raw.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
                nombre = "Doña " + raw;
            } else {
                nombre = raw;
            }
            badgeRol = u.esAdministrador() ? "ADMIN" : "VENDEDORA";
        }

        if (lblUsuarioNombre != null) {
            lblUsuarioNombre.setText(nombre);
        }
        if (lblUsuarioRolBadge != null) {
            lblUsuarioRolBadge.setText(badgeRol);
        }
    }

    private void configurarGruposSeleccion() {
        // 1. Grupo de Pildoras de Categoria
        ToggleButton[] botonesCat = new ToggleButton[]{
                tbCatTodos, tbCatCuadernos, tbCatEscritura, tbCatPapel, tbCatOficina, tbCatOtros
        };
        for (ToggleButton tb : botonesCat) {
            if (tb != null) {
                tb.setToggleGroup(tgCategorias);
            }
        }
        if (tbCatTodos != null) {
            tbCatTodos.setSelected(true);
        }

        tgCategorias.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
                return;
            }
            filtrarYRenderizarCatalogo();
        });

        // 2. Grupo de Forma de Pago (Contado vs Fiado)
        if (tbPagoContado != null) {
            tbPagoContado.setToggleGroup(tgFormaPago);
        }
        if (tbPagoFiado != null) {
            tbPagoFiado.setToggleGroup(tgFormaPago);
        }
        if (tbPagoContado != null) {
            tbPagoContado.setSelected(true);
        }

        tgFormaPago.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
                return;
            }
            actualizarVisibilidadFormaPago();
        });
    }

    private void configurarListeners() {
        if (txtBuscarProducto != null) {
            txtBuscarProducto.textProperty().addListener((obs, oldVal, newVal) -> filtrarYRenderizarCatalogo());
        }
        if (txtValorRecibido != null) {
            txtValorRecibido.textProperty().addListener((obs, oldVal, newVal) -> recalcularTotalesYValidaciones());
        }
        if (txtBuscarClienteFiado != null) {
            txtBuscarClienteFiado.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!actualizandoBuscadorCliente) {
                    filtrarClientesFiado(newVal);
                }
            });
        }
    }

    private boolean verificarCajaAbiertaHoy() {
        try {
            Caja abierta = cajaDAO.obtenerCajaAbierta();
            SesionUsuario.setCajaActiva(abierta);
            if (abierta == null
                    || (abierta.getFechaApertura() != null
                        && abierta.getFechaApertura().toLocalDate().isBefore(LocalDate.now()))) {
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null && rootPane != null && rootPane.getScene() != null) {
                    menu.cargarVistaEnContenido("/fxml/abrir_caja.fxml");
                    return false;
                }
            }
            cajaAbiertaHoy = abierta;
            if (lblEstadoCajaHeader != null) {
                lblEstadoCajaHeader.setText("Caja abierta");
            }
            return true;
        } catch (SQLException e) {
            System.err.println("[VenderController] Error al verificar caja abierta en MySQL: " + e.getMessage());
            renderizarCarritoYFormaPago();
            return true;
        }
    }

    public void recargarDatosDesdeMySQL() {
        catalogoActivos.clear();
        mapaProductosPorId.clear();
        clientesActivos.clear();

        try {
            List<Producto> productosBD = productoDAO.listarTodos(true);
            if (productosBD != null) {
                for (Producto p : productosBD) {
                    if (p.isActivo()) {
                        catalogoActivos.add(p);
                        mapaProductosPorId.put(Integer.valueOf(p.getIdProducto()), p);
                    }
                }
            }

            List<Cliente> clientesBD = clienteDAO.listarTodos(true);
            if (clientesBD != null) {
                clientesActivos.addAll(clientesBD);
            }

            // Seleccionar por defecto el primer cliente con fiado autorizado si aun no hay uno seleccionado
            if (clienteFiadoSeleccionado != null) {
                Cliente actualizado = clienteDAO.obtenerPorId(clienteFiadoSeleccionado.getIdCliente());
                clienteFiadoSeleccionado = actualizado;
            }
            if (clienteFiadoSeleccionado == null) {
                for (Cliente c : clientesActivos) {
                    if (c.isFiadoAutorizado()) {
                        seleccionarClienteFiado(c);
                        break;
                    }
                }
            } else {
                seleccionarClienteFiado(clienteFiadoSeleccionado);
            }

        } catch (SQLException e) {
            System.err.println("[VenderController] Error al cargar catalogo y clientes desde MySQL: " + e.getMessage());
        }

        filtrarYRenderizarCatalogo();
        renderizarCarritoYFormaPago();
    }

    // =========================================================================
    // COLUMNA IZQUIERDA: CATALOGO RAPIDO DE PRODUCTOS ACTIVOS
    // =========================================================================
    @FXML
    public void handleLimpiarBusqueda() {
        if (txtBuscarProducto != null) {
            txtBuscarProducto.clear();
            txtBuscarProducto.requestFocus();
        }
    }

    private void filtrarYRenderizarCatalogo() {
        if (boxListaProductos == null) {
            return;
        }
        boxListaProductos.getChildren().clear();

        String criterio = txtBuscarProducto != null && txtBuscarProducto.getText() != null
                ? txtBuscarProducto.getText().trim().toLowerCase(LOCALE_ES_CO)
                : "";
        String categoriaFiltro = obtenerCategoriaSeleccionada();

        List<Producto> filtrados = new ArrayList<Producto>();
        for (Producto p : catalogoActivos) {
            if (!p.isActivo()) {
                continue;
            }
            if (!cumpleCategoria(p, categoriaFiltro)) {
                continue;
            }
            if (!criterio.isEmpty() && !cumpleBusquedaProducto(p, criterio)) {
                continue;
            }
            filtrados.add(p);
        }

        if (lblTituloProductosEncontrados != null) {
            lblTituloProductosEncontrados.setText("Productos encontrados (" + filtrados.size() + ")");
        }

        if (filtrados.isEmpty()) {
            VBox boxVacio = new VBox(6);
            boxVacio.setAlignment(Pos.CENTER);
            boxVacio.getStyleClass().add("vender-empty-box");
            Label lbl1 = new Label("No se encontraron productos con ese filtro.");
            lbl1.getStyleClass().add("vender-empty-title");
            Label lbl2 = new Label("Pruebe buscando otro nombre o seleccione la categoría «Todos».");
            lbl2.getStyleClass().add("vender-empty-subtitle");
            boxVacio.getChildren().addAll(lbl1, lbl2);
            boxListaProductos.getChildren().add(boxVacio);
            return;
        }

        for (Producto prod : filtrados) {
            boxListaProductos.getChildren().add(crearTarjetaProductoCatalogo(prod));
        }
    }

    private String obtenerCategoriaSeleccionada() {
        Toggle sel = tgCategorias.getSelectedToggle();
        if (sel == tbCatCuadernos) return "cuadernos";
        if (sel == tbCatEscritura) return "escritura";
        if (sel == tbCatPapel) return "papel";
        if (sel == tbCatOficina) return "oficina";
        if (sel == tbCatOtros) return "otros";
        return "todos";
    }

    private boolean cumpleCategoria(Producto p, String catFiltro) {
        if ("todos".equals(catFiltro)) {
            return true;
        }
        String catProd = p.getCategoria() != null ? p.getCategoria().trim().toLowerCase(LOCALE_ES_CO) : "";
        if ("otros".equals(catFiltro)) {
            return !catProd.contains("cuaderno")
                    && !catProd.contains("escritura")
                    && !catProd.contains("papel")
                    && !catProd.contains("oficina");
        }
        return catProd.contains(catFiltro);
    }

    private boolean cumpleBusquedaProducto(Producto p, String criterio) {
        String nombre = p.getNombre() != null ? p.getNombre().toLowerCase(LOCALE_ES_CO) : "";
        String codigo = formatearCodigoProducto(p.getIdProducto()).toLowerCase(LOCALE_ES_CO);
        String idSimple = String.valueOf(p.getIdProducto());
        return nombre.contains(criterio) || codigo.contains(criterio) || idSimple.equals(criterio);
    }

    private HBox crearTarjetaProductoCatalogo(final Producto prod) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        boolean agotado = prod.getStockActual() <= 0;
        card.getStyleClass().add(agotado ? "vender-prod-card-agotado" : "vender-prod-card");

        // 1. Insignia de Codigo (PRD-001)
        Label lblCodigo = new Label(formatearCodigoProducto(prod.getIdProducto()));
        lblCodigo.getStyleClass().add(agotado ? "vender-prod-code-badge-disabled" : "vender-prod-code-badge");

        // 2. Nombre + Insignia de estado de existencias
        VBox boxInfo = new VBox(5);
        boxInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(boxInfo, Priority.ALWAYS);

        Label lblNombre = new Label(prod.getNombre());
        lblNombre.setWrapText(true);
        lblNombre.getStyleClass().add(agotado ? "vender-prod-name-disabled" : "vender-prod-name");

        Label lblBadgeStock = new Label();
        if (agotado) {
            lblBadgeStock.setText("Agotado (0 disponibles)");
            lblBadgeStock.getStyleClass().add("vender-stock-badge-agotado");
        } else if (prod.isStockBajo()) {
            lblBadgeStock.setText("⚠ Pocas existencias (" + prod.getStockActual() + " disp.)");
            lblBadgeStock.getStyleClass().add("vender-stock-badge-bajo");
        } else {
            lblBadgeStock.setText("● " + prod.getStockActual() + " disponibles");
            lblBadgeStock.getStyleClass().add("vender-stock-badge-ok");
        }

        boxInfo.getChildren().addAll(lblNombre, lblBadgeStock);

        // 3. Precio unitario (tachado si esta agotado)
        StackPane panePrecio = new StackPane();
        panePrecio.setAlignment(Pos.CENTER_RIGHT);
        String textoPrecio = "$ " + formatearMoneda(prod.getPrecioVenta());
        if (agotado) {
            Text txtTachado = new Text(textoPrecio);
            txtTachado.setStrikethrough(true);
            txtTachado.getStyleClass().add("vender-prod-price-strikethrough");
            panePrecio.getChildren().add(txtTachado);
        } else {
            Label lblPrecio = new Label(textoPrecio);
            lblPrecio.getStyleClass().add("vender-prod-price");
            panePrecio.getChildren().add(lblPrecio);
        }

        // 4. Boton de accion ("+ Agregar" o "⊘ Agotado")
        Button btnAccion = new Button();
        if (agotado) {
            btnAccion.setText("⊘  Agotado");
            btnAccion.setDisable(true);
            btnAccion.getStyleClass().add("vender-btn-agotado");
        } else {
            btnAccion.setText("⊕  + Agregar");
            btnAccion.getStyleClass().add("vender-btn-agregar");
            btnAccion.setOnAction(ev -> agregarProductoAlCarrito(prod));
        }

        card.getChildren().addAll(lblCodigo, boxInfo, panePrecio, btnAccion);
        return card;
    }

    // =========================================================================
    // COLUMNA DERECHA: CARRITO ("Venta actual") Y CONTROLES DE CANTIDAD
    // =========================================================================
    private void agregarProductoAlCarrito(Producto prod) {
        if (prod == null || prod.getStockActual() <= 0) {
            return;
        }

        Integer key = Integer.valueOf(prod.getIdProducto());
        DetalleVenta existente = carrito.get(key);
        int cantidadActual = existente != null ? existente.getCantidad() : 0;

        if (cantidadActual + 1 > prod.getStockActual()) {
            AlertaUtil.mostrarAdvertencia("Existencias Insuficientes",
                    "Solo hay " + prod.getStockActual() + " unidades disponibles de «" + prod.getNombre() + "».");
            return;
        }

        if (existente == null) {
            DetalleVenta nuevo = new DetalleVenta(
                    prod.getIdProducto(),
                    prod.getNombre(),
                    1,
                    prod.getPrecioVenta()
            );
            carrito.put(key, nuevo);
        } else {
            existente.setCantidad(cantidadActual + 1);
        }

        renderizarCarritoYFormaPago();
    }

    private void cambiarCantidadItemCarrito(int idProducto, int delta) {
        Integer key = Integer.valueOf(idProducto);
        DetalleVenta item = carrito.get(key);
        if (item == null) {
            return;
        }

        int nuevaCantidad = item.getCantidad() + delta;
        if (nuevaCantidad <= 0) {
            carrito.remove(key);
            renderizarCarritoYFormaPago();
            return;
        }

        Producto prod = mapaProductosPorId.get(key);
        int stockMaximo = prod != null ? prod.getStockActual() : nuevaCantidad;
        if (nuevaCantidad > stockMaximo) {
            AlertaUtil.mostrarAdvertencia("Existencias Insuficientes",
                    "No puede agregar más de " + stockMaximo + " unidades de «" + item.getNombreProducto() + "».");
            return;
        }

        item.setCantidad(nuevaCantidad);
        renderizarCarritoYFormaPago();
    }

    private void quitarItemCarrito(int idProducto) {
        carrito.remove(Integer.valueOf(idProducto));
        renderizarCarritoYFormaPago();
    }

    @FXML
    public void handleVaciarVenta() {
        if (carrito.isEmpty()) {
            return;
        }
        carrito.clear();
        if (txtValorRecibido != null) {
            txtValorRecibido.clear();
        }
        renderizarCarritoYFormaPago();
    }

    @FXML
    public void handleCancelarVenta() {
        handleVaciarVenta();
    }

    private void renderizarCarritoYFormaPago() {
        if (boxItemsCarrito == null) {
            return;
        }
        boxItemsCarrito.getChildren().clear();

        int numProductosDistintos = carrito.size();
        if (lblBadgeCantidadCarrito != null) {
            lblBadgeCantidadCarrito.setText(numProductosDistintos == 1
                    ? "1 producto"
                    : numProductosDistintos + " productos");
        }
        if (btnVaciarVenta != null) {
            btnVaciarVenta.setDisable(carrito.isEmpty());
        }

        BigDecimal sumaTotal = BigDecimal.ZERO;

        if (carrito.isEmpty()) {
            VBox vacio = new VBox(4);
            vacio.setAlignment(Pos.CENTER);
            vacio.getStyleClass().add("vender-cart-empty-box");
            Label lbl = new Label("Aún no ha agregado productos a la venta.");
            lbl.getStyleClass().add("vender-empty-title");
            Label sub = new Label("Pulse «+ Agregar» en cualquier producto de la izquierda.");
            sub.getStyleClass().add("vender-empty-subtitle");
            vacio.getChildren().addAll(lbl, sub);
            boxItemsCarrito.getChildren().add(vacio);
        } else {
            for (final DetalleVenta item : carrito.values()) {
                sumaTotal = sumaTotal.add(item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO);
                boxItemsCarrito.getChildren().add(crearFilaItemCarrito(item));
            }
        }

        totalVentaActual = sumaTotal;
        recalcularTotalesYValidaciones();
    }

    private HBox crearFilaItemCarrito(final DetalleVenta item) {
        HBox fila = new HBox(10);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("vender-cart-item-row");

        // Nombre y precio unitario
        VBox boxTexto = new VBox(2);
        boxTexto.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(boxTexto, Priority.ALWAYS);

        Label lblNom = new Label(item.getNombreProducto());
        lblNom.setWrapText(true);
        lblNom.getStyleClass().add("vender-cart-item-name");

        Label lblUnit = new Label("$ " + formatearMoneda(item.getValorUnitario()) + " c/u");
        lblUnit.getStyleClass().add("vender-cart-item-unit");
        boxTexto.getChildren().addAll(lblNom, lblUnit);

        // Selector − [cantidad] +
        HBox boxStepper = new HBox(4);
        boxStepper.setAlignment(Pos.CENTER);
        boxStepper.getStyleClass().add("vender-stepper-box");

        Button btnMenos = new Button("−");
        btnMenos.getStyleClass().add("vender-stepper-btn");
        btnMenos.setOnAction(ev -> cambiarCantidadItemCarrito(item.getIdProducto(), -1));

        Label lblCant = new Label(String.valueOf(item.getCantidad()));
        lblCant.setMinWidth(24);
        lblCant.setAlignment(Pos.CENTER);
        lblCant.getStyleClass().add("vender-stepper-qty");

        Button btnMas = new Button("+");
        btnMas.getStyleClass().add("vender-stepper-btn");
        btnMas.setOnAction(ev -> cambiarCantidadItemCarrito(item.getIdProducto(), 1));

        boxStepper.getChildren().addAll(btnMenos, lblCant, btnMas);

        // Subtotal de linea
        Label lblSub = new Label("$ " + formatearMoneda(item.getSubtotal()));
        lblSub.setMinWidth(74);
        lblSub.setAlignment(Pos.CENTER_RIGHT);
        lblSub.getStyleClass().add("vender-cart-item-subtotal");

        // Boton rojo suave "✕" para quitar el item
        Button btnQuitar = new Button("✕");
        btnQuitar.getStyleClass().add("vender-btn-remove-item");
        btnQuitar.setOnAction(ev -> quitarItemCarrito(item.getIdProducto()));

        fila.getChildren().addAll(boxTexto, boxStepper, lblSub, btnQuitar);
        return fila;
    }

    // =========================================================================
    // SELECTOR DE FORMA DE PAGO: CONTADO VS FIADO
    // =========================================================================
    @FXML
    public void handleCambiarAFiado() {
        if (tbPagoFiado != null) {
            tbPagoFiado.setSelected(true);
        }
    }

    private boolean esModoFiado() {
        return tbPagoFiado != null && tbPagoFiado.isSelected();
    }

    private void actualizarVisibilidadFormaPago() {
        boolean fiado = esModoFiado();
        if (panelPagoContado != null) {
            panelPagoContado.setVisible(!fiado);
            panelPagoContado.setManaged(!fiado);
        }
        if (panelPagoFiado != null) {
            panelPagoFiado.setVisible(fiado);
            panelPagoFiado.setManaged(fiado);
        }
        recalcularTotalesYValidaciones();
    }

    private void filtrarClientesFiado(String texto) {
        if (boxSugerenciasClientes == null) {
            return;
        }
        boxSugerenciasClientes.getChildren().clear();

        String criterio = texto != null ? texto.trim().toLowerCase(LOCALE_ES_CO) : "";
        if (criterio.startsWith("cliente:")) {
            criterio = criterio.substring("cliente:".length()).trim();
        }

        List<Cliente> coincidencias = new ArrayList<Cliente>();
        for (Cliente c : clientesActivos) {
            if (!c.isActivo() || !c.isFiadoAutorizado()) {
                continue;
            }
            String nom = c.getNombre() != null ? c.getNombre().toLowerCase(LOCALE_ES_CO) : "";
            String tel = c.getTelefono() != null ? c.getTelefono().toLowerCase(LOCALE_ES_CO) : "";
            if (criterio.isEmpty() || nom.contains(criterio) || tel.contains(criterio)) {
                coincidencias.add(c);
            }
        }

        if (coincidencias.isEmpty()) {
            boxSugerenciasClientes.setVisible(false);
            boxSugerenciasClientes.setManaged(false);
            return;
        }

        // Si el usuario esta escribiendo, mostrar hasta 3 sugerencias rapidas clicables
        int maxSug = Math.min(3, coincidencias.size());
        for (int i = 0; i < maxSug; i++) {
            final Cliente cand = coincidencias.get(i);
            Button btnSug = new Button("👤 " + cand.getNombre() + "  ·  Cupo disp.: $ "
                    + formatearMoneda(cand.getCreditoDisponible()));
            btnSug.setMaxWidth(Double.MAX_VALUE);
            btnSug.setAlignment(Pos.CENTER_LEFT);
            btnSug.getStyleClass().add("vender-cliente-sugerencia-btn");
            btnSug.setOnAction(ev -> {
                seleccionarClienteFiado(cand);
                boxSugerenciasClientes.setVisible(false);
                boxSugerenciasClientes.setManaged(false);
            });
            boxSugerenciasClientes.getChildren().add(btnSug);
        }

        boolean mostrarLista = !criterio.isEmpty() && (clienteFiadoSeleccionado == null
                || !criterio.equalsIgnoreCase(clienteFiadoSeleccionado.getNombre()));
        boxSugerenciasClientes.setVisible(mostrarLista);
        boxSugerenciasClientes.setManaged(mostrarLista);

        // Seleccionar automaticamente la primera coincidencia mientras busca
        seleccionarClienteFiadoSinSobrescribirInput(coincidencias.get(0));
    }

    private void seleccionarClienteFiado(Cliente c) {
        this.clienteFiadoSeleccionado = c;
        if (txtBuscarClienteFiado != null && c != null) {
            actualizandoBuscadorCliente = true;
            txtBuscarClienteFiado.setText("Cliente: " + c.getNombre());
            actualizandoBuscadorCliente = false;
        }
        recalcularTotalesYValidaciones();
    }

    private void seleccionarClienteFiadoSinSobrescribirInput(Cliente c) {
        this.clienteFiadoSeleccionado = c;
        recalcularTotalesYValidaciones();
    }

    @FXML
    public void handleLimpiarClienteFiado() {
        this.clienteFiadoSeleccionado = null;
        if (txtBuscarClienteFiado != null) {
            actualizandoBuscadorCliente = true;
            txtBuscarClienteFiado.clear();
            actualizandoBuscadorCliente = false;
            txtBuscarClienteFiado.requestFocus();
        }
        filtrarClientesFiado("");
        recalcularTotalesYValidaciones();
    }

    private void recalcularTotalesYValidaciones() {
        String totalFormateado = "$ " + formatearMoneda(totalVentaActual);
        if (lblTotalCobrarContado != null) {
            lblTotalCobrarContado.setText(totalFormateado);
        }
        if (lblTotalCobrarFiado != null) {
            lblTotalCobrarFiado.setText(totalFormateado);
        }

        boolean carritoVacio = carrito.isEmpty();

        // 1. Calculo en tiempo real para Modo Contado
        BigDecimal recibido = parsearMonto(txtValorRecibido != null ? txtValorRecibido.getText() : null);
        boolean recibidoSuficiente = true;

        if (recibido == null || (txtValorRecibido != null && txtValorRecibido.getText().trim().isEmpty())) {
            if (lblTituloCambio != null) {
                lblTituloCambio.setText("🔄  Cambio a devolver:");
            }
            if (lblValorCambio != null) {
                lblValorCambio.setText("$ 0");
            }
        } else if (recibido.compareTo(totalVentaActual) < 0) {
            recibidoSuficiente = false;
            BigDecimal faltante = totalVentaActual.subtract(recibido);
            if (lblTituloCambio != null) {
                lblTituloCambio.setText("⚠  Falta por recibir:");
            }
            if (lblValorCambio != null) {
                lblValorCambio.setText("$ " + formatearMoneda(faltante));
            }
        } else {
            BigDecimal cambio = recibido.subtract(totalVentaActual);
            if (lblTituloCambio != null) {
                lblTituloCambio.setText("🔄  Cambio a devolver:");
            }
            if (lblValorCambio != null) {
                lblValorCambio.setText("$ " + formatearMoneda(cambio));
            }
        }

        if (btnTerminarVentaContado != null) {
            btnTerminarVentaContado.setDisable(carritoVacio || !recibidoSuficiente);
        }

        // 2. Calculo y regla estricta de negocio para Modo Fiado
        if (clienteFiadoSeleccionado == null) {
            if (lblNombreClienteFiado != null) {
                lblNombreClienteFiado.setText("👤  Seleccione un cliente autorizado");
            }
            if (lblBadgeEstadoFiado != null) {
                lblBadgeEstadoFiado.setText("Sin seleccionar");
            }
            if (lblCupoDisponible != null) {
                lblCupoDisponible.setText("$ 0");
            }
            if (lblDeudaActual != null) {
                lblDeudaActual.setText("$ 0");
            }
            if (lblDeudaDespuesVenta != null) {
                lblDeudaDespuesVenta.setText(totalFormateado);
            }
            if (lblAlertaCupoFiado != null) {
                lblAlertaCupoFiado.setText("⚠ Seleccione un cliente con fiado autorizado para continuar.");
                lblAlertaCupoFiado.setVisible(true);
                lblAlertaCupoFiado.setManaged(true);
            }
            if (btnTerminarVentaFiado != null) {
                btnTerminarVentaFiado.setDisable(true);
            }
        } else {
            BigDecimal cupoDisponible = clienteFiadoSeleccionado.getCreditoDisponible();
            BigDecimal deudaActual = clienteFiadoSeleccionado.getSaldoPendiente() != null
                    ? clienteFiadoSeleccionado.getSaldoPendiente()
                    : BigDecimal.ZERO;
            BigDecimal deudaFutura = deudaActual.add(totalVentaActual);
            boolean autorizado = clienteFiadoSeleccionado.isFiadoAutorizado();
            boolean superaCupo = totalVentaActual.compareTo(cupoDisponible) > 0;

            if (lblNombreClienteFiado != null) {
                lblNombreClienteFiado.setText("👤  " + clienteFiadoSeleccionado.getNombre());
            }
            if (lblBadgeEstadoFiado != null) {
                lblBadgeEstadoFiado.setText(autorizado && !superaCupo ? "Autorizado" : "Cupo insuficiente");
            }
            if (lblCupoDisponible != null) {
                lblCupoDisponible.setText("$ " + formatearMoneda(cupoDisponible));
            }
            if (lblDeudaActual != null) {
                lblDeudaActual.setText("$ " + formatearMoneda(deudaActual));
            }
            if (lblDeudaDespuesVenta != null) {
                lblDeudaDespuesVenta.setText("$ " + formatearMoneda(deudaFutura));
            }

            if (!autorizado) {
                if (lblAlertaCupoFiado != null) {
                    lblAlertaCupoFiado.setText("⚠ Este cliente no tiene fiado autorizado. Cambie a Contado o elija otro cliente.");
                    lblAlertaCupoFiado.setVisible(true);
                    lblAlertaCupoFiado.setManaged(true);
                }
                if (btnTerminarVentaFiado != null) {
                    btnTerminarVentaFiado.setDisable(true);
                }
            } else if (superaCupo) {
                if (lblAlertaCupoFiado != null) {
                    lblAlertaCupoFiado.setText("⚠ El total de la venta ($ " + formatearMoneda(totalVentaActual)
                            + ") supera el cupo disponible del cliente ($ " + formatearMoneda(cupoDisponible) + ").");
                    lblAlertaCupoFiado.setVisible(true);
                    lblAlertaCupoFiado.setManaged(true);
                }
                if (btnTerminarVentaFiado != null) {
                    btnTerminarVentaFiado.setDisable(true);
                }
            } else {
                if (lblAlertaCupoFiado != null) {
                    lblAlertaCupoFiado.setVisible(false);
                    lblAlertaCupoFiado.setManaged(false);
                }
                if (btnTerminarVentaFiado != null) {
                    btnTerminarVentaFiado.setDisable(carritoVacio);
                }
            }
        }
    }

    // =========================================================================
    // GUARDADO TRANSACCIONAL EN MYSQL (VentaDAO)
    // =========================================================================
    @FXML
    public void handleTerminarVenta() {
        if (carrito.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Venta Vacía",
                    "Agregue al menos un producto antes de terminar la venta.");
            return;
        }

        try {
            Caja abierta = cajaDAO.obtenerCajaAbierta();
            if (abierta == null) {
                AlertaUtil.mostrarAdvertencia("Caja Cerrada",
                        "No hay una caja abierta actualmente. Debe abrir la caja antes de registrar ventas.");
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.cargarVistaEnContenido("/fxml/abrir_caja.fxml");
                }
                return;
            }
            cajaAbiertaHoy = abierta;
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo verificar el estado de la caja en MySQL: " + e.getMessage());
            return;
        }

        boolean esFiado = esModoFiado();
        BigDecimal valorRecibido = totalVentaActual;
        BigDecimal cambio = BigDecimal.ZERO;

        if (esFiado) {
            if (clienteFiadoSeleccionado == null || !clienteFiadoSeleccionado.isFiadoAutorizado()) {
                AlertaUtil.mostrarAdvertencia("Cliente Requerido",
                        "Seleccione un cliente con fiado autorizado para registrar la venta a fiado.");
                return;
            }
            BigDecimal cupo = clienteFiadoSeleccionado.getCreditoDisponible();
            if (totalVentaActual.compareTo(cupo) > 0) {
                AlertaUtil.mostrarAdvertencia("Cupo Disponible Insuficiente",
                        "La venta ($ " + formatearMoneda(totalVentaActual)
                                + ") supera el cupo disponible de " + clienteFiadoSeleccionado.getNombre()
                                + " ($ " + formatearMoneda(cupo) + ").");
                return;
            }
            valorRecibido = BigDecimal.ZERO;
            cambio = BigDecimal.ZERO;
        } else {
            BigDecimal recibidoInput = parsearMonto(txtValorRecibido != null ? txtValorRecibido.getText() : null);
            if (recibidoInput != null && !txtValorRecibido.getText().trim().isEmpty()) {
                if (recibidoInput.compareTo(totalVentaActual) < 0) {
                    AlertaUtil.mostrarAdvertencia("Valor Recibido Insuficiente",
                            "El dinero recibido ($ " + formatearMoneda(recibidoInput)
                                    + ") es menor al total a cobrar ($ " + formatearMoneda(totalVentaActual) + ").");
                    return;
                }
                valorRecibido = recibidoInput;
                cambio = valorRecibido.subtract(totalVentaActual);
            }
        }

        Usuario u = SesionUsuario.getUsuarioLogueado();
        int idUsuario = u != null && u.getIdUsuario() > 0 ? u.getIdUsuario() : 1;

        Venta nuevaVenta = new Venta();
        nuevaVenta.setFecha(LocalDateTime.now());
        nuevaVenta.setIdUsuario(idUsuario);
        nuevaVenta.setIdCaja(Integer.valueOf(cajaAbiertaHoy.getIdCaja()));
        nuevaVenta.setEsFiado(esFiado);
        if (esFiado && clienteFiadoSeleccionado != null) {
            nuevaVenta.setIdCliente(Integer.valueOf(clienteFiadoSeleccionado.getIdCliente()));
            nuevaVenta.setNombreCliente(clienteFiadoSeleccionado.getNombre());
            nuevaVenta.setDescripcion("Venta a fiado - " + clienteFiadoSeleccionado.getNombre());
        } else {
            nuevaVenta.setIdCliente(null);
            nuevaVenta.setNombreCliente("Venta General (Público mostrador)");
            nuevaVenta.setDescripcion("Venta de contado en mostrador");
        }

        for (DetalleVenta item : carrito.values()) {
            nuevaVenta.agregarDetalle(new DetalleVenta(
                    item.getIdProducto(),
                    item.getNombreProducto(),
                    item.getCantidad(),
                    item.getValorUnitario()
            ));
        }

        try {
            boolean guardada = ventaDAO.registrarVenta(nuevaVenta);
            if (guardada) {
                mostrarConfirmacionVenta(nuevaVenta, valorRecibido, cambio);
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Registrar Venta",
                    "Ocurrió un problema al guardar la venta en MySQL (se revirtieron los cambios): " + e.getMessage());
        }
    }

    /**
     * Navega inmediatamente a la pantalla de confirmacion "¡Venta registrada!".
     */
    protected void mostrarConfirmacionVenta(Venta ventaRegistrada, BigDecimal valorRecibido, BigDecimal cambio) {
        carrito.clear();
        if (txtValorRecibido != null) {
            txtValorRecibido.clear();
        }

        VentaRegistradaController.prepararDatosConfirmacion(ventaRegistrada, valorRecibido, cambio);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.refrescarAlertasReales();
            VentaRegistradaController ctrl = menu.cargarVistaConControlador("/fxml/venta_registrada.fxml");
            if (ctrl != null) {
                ctrl.mostrarDatosVenta(ventaRegistrada, valorRecibido, cambio);
            }
        }
    }

    @FXML
    public void handleAbrirHistorialHoy() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/ventas_hoy.fxml");
        }
    }

    @FXML
    public void handleVolverMenu() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarMenuDashboard();
        }
    }

    private String formatearCodigoProducto(int idProducto) {
        return String.format("PRD-%03d", Integer.valueOf(idProducto));
    }

    private BigDecimal parsearMonto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpio = texto.replace("$", "")
                .replace(".", "")
                .replace(",", ".")
                .replace(" ", "")
                .trim();
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
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(monto);
    }
}
