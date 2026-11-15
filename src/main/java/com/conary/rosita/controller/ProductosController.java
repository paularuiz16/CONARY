package com.conary.rosita.controller;

import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.model.Producto;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
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
 * Controlador del Modulo 4: Productos y precios — Paso 1 (Vista principal de catalogo, busqueda y seleccion).
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ProductosController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final int FILAS_POR_PAGINA = 7;

    public static final String FILTRO_ACTIVOS = "Estado: Todos (Activos)";
    public static final String FILTRO_POCAS_EXISTENCIAS = "Estado: Pocas existencias";
    public static final String FILTRO_DADOS_DE_BAJA = "Estado: Dados de baja";
    public static final String FILTRO_TODOS = "Estado: Todos";

    private static boolean iniciarConFiltroPocasExistencias = false;
    private static int idProductoSeleccionarAlCargar = -1;

    public static void setIniciarConFiltroPocasExistencias(boolean activar) {
        iniciarConFiltroPocasExistencias = activar;
    }

    public static void setIdProductoSeleccionarAlCargar(int idProducto) {
        idProductoSeleccionarAlCargar = idProducto;
    }

    @FXML private Button btnVolverMenu;
    @FXML private Label lblSubtituloFiltro;
    @FXML private Label lblUsuarioHeader;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFiltroEstado;
    @FXML private Button btnRegistrar;

    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, Producto> colCodigo;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colCategoria;
    @FXML private TableColumn<Producto, String> colPrecioVenta;
    @FXML private TableColumn<Producto, Producto> colExistencias;
    @FXML private TableColumn<Producto, Producto> colEstado;

    @FXML private Label lblConteo;
    @FXML private Button btnAnteriorPagina;
    @FXML private Button btnSiguientePagina;

    @FXML private HBox barraSeleccion;
    @FXML private Label lblProductoSeleccionado;
    @FXML private Button btnModificar;
    @FXML private Button btnAjustarStock;
    @FXML private Button btnDarBaja;
    @FXML private Button btnReactivar;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final List<Producto> catalogoCompleto = new ArrayList<Producto>();
    private final List<Producto> listaFiltrada = new ArrayList<Producto>();
    private final ObservableList<Producto> paginaActualItems = FXCollections.observableArrayList();

    private int paginaActual = 0;
    private Producto productoSeleccionado;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarRestriccionesPorRol();
        configurarFiltros();
        configurarTabla();
        cargarCatalogoDesdeBaseDeDatos();

        if (iniciarConFiltroPocasExistencias) {
            iniciarConFiltroPocasExistencias = false;
            cmbFiltroEstado.getSelectionModel().select(FILTRO_POCAS_EXISTENCIAS);
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
        if (lblUsuarioHeader != null) {
            lblUsuarioHeader.setText(nombre + "  •  " + rol);
        }
    }

    private void configurarRestriccionesPorRol() {
        boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
        if (btnRegistrar != null) {
            btnRegistrar.setVisible(esAdmin);
            btnRegistrar.setManaged(esAdmin);
            btnRegistrar.setDisable(!esAdmin);
        }
        if (btnModificar != null) {
            btnModificar.setVisible(esAdmin);
            btnModificar.setManaged(esAdmin);
        }
        if (btnAjustarStock != null) {
            btnAjustarStock.setVisible(esAdmin);
            btnAjustarStock.setManaged(esAdmin);
        }
        if (btnDarBaja != null) {
            btnDarBaja.setVisible(esAdmin);
            btnDarBaja.setManaged(esAdmin);
        }
        if (btnReactivar != null) {
            btnReactivar.setVisible(false);
            btnReactivar.setManaged(false);
            btnReactivar.setDisable(!esAdmin);
        }
    }

    private void configurarFiltros() {
        if (cmbFiltroEstado != null) {
            cmbFiltroEstado.setItems(FXCollections.observableArrayList(
                    FILTRO_ACTIVOS,
                    FILTRO_POCAS_EXISTENCIAS,
                    FILTRO_DADOS_DE_BAJA,
                    FILTRO_TODOS
            ));
            cmbFiltroEstado.getSelectionModel().select(FILTRO_ACTIVOS);
            cmbFiltroEstado.valueProperty().addListener((obs, oldVal, newVal) -> {
                paginaActual = 0;
                aplicarFiltroYPaginacion();
            });
        }

        if (txtBuscar != null) {
            txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> {
                paginaActual = 0;
                aplicarFiltroYPaginacion();
            });
        }
    }

    private void configurarTabla() {
        tblProductos.setItems(paginaActualItems);
        tblProductos.setFixedCellSize(56.0);

        tblProductos.setRowFactory(tv -> new TableRow<Producto>() {
            @Override
            protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(56.0);
                setMinHeight(56.0);
                setMaxHeight(56.0);
            }
        });

        // 1. Columna Codigo (con indicador circular en la fila seleccionada)
        colCodigo.setCellValueFactory(data -> new SimpleObjectProperty<Producto>(data.getValue()));
        colCodigo.setCellFactory(col -> new TableCell<Producto, Producto>() {
            private final Circle dot = new Circle(4.2, Color.web("#9E5827"));
            private final Label lblCode = new Label();
            private final HBox box = new HBox(8, dot, lblCode);

            {
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Producto prod, boolean empty) {
                super.updateItem(prod, empty);
                if (empty || prod == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    TableRow<?> row = getTableRow();
                    boolean filaSeleccionada = row != null && row.isSelected();
                    dot.setVisible(filaSeleccionada);
                    dot.setManaged(filaSeleccionada);
                    lblCode.setText(formatearCodigo(prod.getIdProducto()));
                    lblCode.getStyleClass().removeAll("productos-cell-codigo", "productos-cell-codigo-selected");
                    lblCode.getStyleClass().add(filaSeleccionada ? "productos-cell-codigo-selected" : "productos-cell-codigo");
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // 2. Columna Nombre del producto
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colNombre.setCellFactory(col -> new TableCell<Producto, String>() {
            @Override
            protected void updateItem(String nombre, boolean empty) {
                super.updateItem(nombre, empty);
                if (empty || nombre == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(nombre);
                    getStyleClass().removeAll("productos-cell-nombre");
                    getStyleClass().add("productos-cell-nombre");
                }
            }
        });

        // 3. Columna Categoria
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(formatearCategoria(data.getValue().getCategoria())));
        colCategoria.setCellFactory(col -> new TableCell<Producto, String>() {
            @Override
            protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) {
                    setText(null);
                } else {
                    setText(cat);
                    getStyleClass().removeAll("productos-cell-categoria");
                    getStyleClass().add("productos-cell-categoria");
                }
            }
        });

        // 4. Columna Precio de venta ($ 4.500)
        colPrecioVenta.setCellValueFactory(data -> new SimpleStringProperty("$ " + formatearMoneda(data.getValue().getPrecioVenta())));
        colPrecioVenta.setCellFactory(col -> new TableCell<Producto, String>() {
            @Override
            protected void updateItem(String precio, boolean empty) {
                super.updateItem(precio, empty);
                if (empty || precio == null) {
                    setText(null);
                } else {
                    setText(precio);
                    setAlignment(Pos.CENTER_RIGHT);
                    getStyleClass().removeAll("productos-cell-precio");
                    getStyleClass().add("productos-cell-precio");
                }
            }
        });

        // 5. Columna Existencias (solo numero entero)
        colExistencias.setCellValueFactory(data -> new SimpleObjectProperty<Producto>(data.getValue()));
        colExistencias.setCellFactory(col -> new TableCell<Producto, Producto>() {
            @Override
            protected void updateItem(Producto prod, boolean empty) {
                super.updateItem(prod, empty);
                if (empty || prod == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(prod.getStockActual()));
                    setAlignment(Pos.CENTER);
                    getStyleClass().removeAll("productos-cell-existencias", "productos-cell-existencias-bajo");
                    getStyleClass().add(prod.isStockBajo() ? "productos-cell-existencias-bajo" : "productos-cell-existencias");
                }
            }
        });

        // 6. Columna Estado (vacio si normal, badge ambar si pocas existencias, badge gris si dado de baja)
        colEstado.setCellValueFactory(data -> new SimpleObjectProperty<Producto>(data.getValue()));
        colEstado.setCellFactory(col -> new TableCell<Producto, Producto>() {
            @Override
            protected void updateItem(Producto prod, boolean empty) {
                super.updateItem(prod, empty);
                if (empty || prod == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                setAlignment(Pos.CENTER);
                if (!prod.isActivo()) {
                    Label text = new Label("Dado de baja");
                    text.getStyleClass().add("badge-dado-baja-text");
                    HBox pill = new HBox(text);
                    pill.setAlignment(Pos.CENTER);
                    pill.getStyleClass().add("badge-dado-baja");
                    pill.setMaxWidth(124);
                    setGraphic(pill);
                    setText(null);
                } else if (prod.isStockBajo()) {
                    Label icon = new Label("\u26A0");
                    icon.getStyleClass().add("badge-pocas-existencias-icon");
                    Label text = new Label("Pocas\nexistencias");
                    text.setTextAlignment(TextAlignment.CENTER);
                    text.setAlignment(Pos.CENTER);
                    text.getStyleClass().add("badge-pocas-existencias-text");
                    HBox pill = new HBox(6, icon, text);
                    pill.setAlignment(Pos.CENTER);
                    pill.getStyleClass().add("badge-pocas-existencias");
                    pill.setMaxWidth(128);
                    setGraphic(pill);
                    setText(null);
                } else {
                    setGraphic(null);
                    setText(null);
                }
            }
        });

        tblProductos.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            productoSeleccionado = newSel;
            actualizarBarraSeleccion(newSel);
            tblProductos.refresh();
        });
    }

    public void cargarCatalogoDesdeBaseDeDatos() {
        catalogoCompleto.clear();
        try {
            List<Producto> desdeBD = productoDAO.listarTodos(false);
            if (desdeBD != null) {
                catalogoCompleto.addAll(desdeBD);
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo consultar el catálogo de productos en MySQL: " + e.getMessage());
        }
        aplicarFiltroYPaginacion();
    }

    private void aplicarFiltroYPaginacion() {
        String textoBusqueda = txtBuscar != null && txtBuscar.getText() != null
                ? txtBuscar.getText().trim().toLowerCase(LOCALE_ES_CO)
                : "";
        String estadoSeleccionado = cmbFiltroEstado != null && cmbFiltroEstado.getValue() != null
                ? cmbFiltroEstado.getValue()
                : FILTRO_ACTIVOS;

        listaFiltrada.clear();
        for (Producto p : catalogoCompleto) {
            if (!cumpleFiltroEstado(p, estadoSeleccionado)) {
                continue;
            }
            if (!textoBusqueda.isEmpty() && !cumpleFiltroTexto(p, textoBusqueda)) {
                continue;
            }
            listaFiltrada.add(p);
        }

        int totalFiltrados = listaFiltrada.size();
        int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / FILAS_POR_PAGINA));

        Producto productoObjetivo = null;
        if (idProductoSeleccionarAlCargar > 0) {
            for (int i = 0; i < listaFiltrada.size(); i++) {
                Producto cand = listaFiltrada.get(i);
                if (cand.getIdProducto() == idProductoSeleccionarAlCargar) {
                    productoObjetivo = cand;
                    paginaActual = i / FILAS_POR_PAGINA;
                    break;
                }
            }
            idProductoSeleccionarAlCargar = -1;
        }

        if (paginaActual >= totalPaginas) {
            paginaActual = totalPaginas - 1;
        }
        if (paginaActual < 0) {
            paginaActual = 0;
        }

        int desde = paginaActual * FILAS_POR_PAGINA;
        int hasta = Math.min(desde + FILAS_POR_PAGINA, totalFiltrados);

        paginaActualItems.clear();
        if (desde < hasta) {
            paginaActualItems.addAll(listaFiltrada.subList(desde, hasta));
        }

        // Actualizar etiqueta de conteo en el pie de tabla
        boolean esVistaDadosBaja = FILTRO_DADOS_DE_BAJA.equals(estadoSeleccionado);
        if (lblSubtituloFiltro != null) {
            lblSubtituloFiltro.setVisible(esVistaDadosBaja);
            lblSubtituloFiltro.setManaged(esVistaDadosBaja);
        }
        if (lblConteo != null) {
            if (esVistaDadosBaja) {
                lblConteo.setText("Mostrando " + paginaActualItems.size() + " de " + totalFiltrados + " productos dados de baja");
            } else {
                lblConteo.setText("Mostrando " + paginaActualItems.size() + " de " + totalFiltrados + " productos");
            }
        }

        // Habilitar / deshabilitar botones de paginacion
        if (btnAnteriorPagina != null) {
            btnAnteriorPagina.setDisable(paginaActual <= 0);
        }
        if (btnSiguientePagina != null) {
            btnSiguientePagina.setDisable(paginaActual >= totalPaginas - 1);
        }

        // Seleccionar el producto objetivo o el primero de la pagina por defecto
        if (productoObjetivo != null && paginaActualItems.contains(productoObjetivo)) {
            tblProductos.getSelectionModel().select(productoObjetivo);
        } else if (!paginaActualItems.isEmpty()) {
            tblProductos.getSelectionModel().selectFirst();
        } else {
            tblProductos.getSelectionModel().clearSelection();
            actualizarBarraSeleccion(null);
        }
    }

    private boolean cumpleFiltroEstado(Producto p, String filtroEstado) {
        if (FILTRO_ACTIVOS.equals(filtroEstado)) {
            return p.isActivo();
        } else if (FILTRO_POCAS_EXISTENCIAS.equals(filtroEstado)) {
            return p.isActivo() && p.isStockBajo();
        } else if (FILTRO_DADOS_DE_BAJA.equals(filtroEstado)) {
            return !p.isActivo();
        }
        return true; // FILTRO_TODOS
    }

    private boolean cumpleFiltroTexto(Producto p, String criterio) {
        String codigo = formatearCodigo(p.getIdProducto()).toLowerCase(LOCALE_ES_CO);
        String nombre = p.getNombre() != null ? p.getNombre().toLowerCase(LOCALE_ES_CO) : "";
        String categoria = p.getCategoria() != null ? p.getCategoria().toLowerCase(LOCALE_ES_CO) : "";
        String idSimple = String.valueOf(p.getIdProducto());

        return nombre.contains(criterio)
                || codigo.contains(criterio)
                || categoria.contains(criterio)
                || idSimple.equals(criterio);
    }

    private void actualizarBarraSeleccion(Producto sel) {
        boolean haySeleccion = sel != null;
        boolean esInactivo = haySeleccion && !sel.isActivo();
        boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
        String estadoFiltro = cmbFiltroEstado != null && cmbFiltroEstado.getValue() != null
                ? cmbFiltroEstado.getValue()
                : FILTRO_ACTIVOS;
        boolean mostrarSubtituloInactivos = FILTRO_DADOS_DE_BAJA.equals(estadoFiltro) || esInactivo;

        if (lblSubtituloFiltro != null) {
            lblSubtituloFiltro.setVisible(mostrarSubtituloInactivos);
            lblSubtituloFiltro.setManaged(mostrarSubtituloInactivos);
        }

        if (lblProductoSeleccionado != null) {
            if (!haySeleccion) {
                lblProductoSeleccionado.setText("Seleccione un producto de la tabla para ver sus opciones");
            } else if (esInactivo) {
                String resumenInactivo = "Seleccionado: " + sel.getNombre()
                        + "  ·  " + formatearCodigo(sel.getIdProducto())
                        + "  ·  Dado de baja";
                lblProductoSeleccionado.setText(resumenInactivo);
            } else {
                String resumen = "Seleccionado: " + sel.getNombre()
                        + "  ·  " + formatearCodigo(sel.getIdProducto())
                        + "  ·  Precio actual $ " + formatearMoneda(sel.getPrecioVenta())
                        + "  ·  Existencias " + sel.getStockActual();
                lblProductoSeleccionado.setText(resumen);
            }
        }

        boolean mostrarBotonesActivos = esAdmin && !esInactivo;
        boolean mostrarBotonReactivar = esAdmin && esInactivo;

        if (btnModificar != null) {
            btnModificar.setVisible(mostrarBotonesActivos);
            btnModificar.setManaged(mostrarBotonesActivos);
            btnModificar.setDisable(!haySeleccion || !esAdmin);
        }
        if (btnAjustarStock != null) {
            btnAjustarStock.setVisible(mostrarBotonesActivos);
            btnAjustarStock.setManaged(mostrarBotonesActivos);
            btnAjustarStock.setDisable(!haySeleccion || !esAdmin);
        }
        if (btnDarBaja != null) {
            btnDarBaja.setVisible(mostrarBotonesActivos);
            btnDarBaja.setManaged(mostrarBotonesActivos);
            btnDarBaja.setDisable(!haySeleccion || !esAdmin);
            btnDarBaja.setText("  Dar de baja");
        }
        if (btnReactivar != null) {
            btnReactivar.setVisible(mostrarBotonReactivar);
            btnReactivar.setManaged(mostrarBotonReactivar);
            btnReactivar.setDisable(!haySeleccion || !esAdmin);
        }
    }

    @FXML
    public void handlePaginaAnterior() {
        if (paginaActual > 0) {
            paginaActual--;
            aplicarFiltroYPaginacion();
        }
    }

    @FXML
    public void handlePaginaSiguiente() {
        int totalPaginas = Math.max(1, (int) Math.ceil((double) listaFiltrada.size() / FILAS_POR_PAGINA));
        if (paginaActual < totalPaginas - 1) {
            paginaActual++;
            aplicarFiltroYPaginacion();
        }
    }

    @FXML
    public void handleRegistrarProducto() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede registrar productos nuevos.");
            return;
        }
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/registrar_producto.fxml");
        }
    }

    @FXML
    public void handleModificarProducto() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede modificar precios o datos de productos.");
            return;
        }
        Producto seleccionado = tblProductos != null
                ? tblProductos.getSelectionModel().getSelectedItem()
                : productoSeleccionado;
        if (seleccionado == null) {
            seleccionado = productoSeleccionado;
        }
        if (seleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Seleccione un producto",
                    "Primero toque o haga clic sobre un producto de la tabla.");
            return;
        }

        ModificarProductoController.setProductoPendienteEdicion(seleccionado);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            ModificarProductoController ctrl = menu.cargarVistaConControlador("/fxml/modificar_producto.fxml");
            if (ctrl != null) {
                ctrl.setProducto(seleccionado);
            }
        }
    }

    @FXML
    public void handleAjustarExistencias() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede ajustar existencias.");
            return;
        }
        Producto seleccionado = obtenerProductoSeleccionadoActual();
        if (seleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Seleccione un producto",
                    "Primero toque o haga clic sobre un producto de la tabla.");
            return;
        }

        try {
            FXMLLoader loader = crearLoaderFXML("/fxml/modal_ajustar_existencias.fxml");
            Parent root = loader.load();
            AjustarExistenciasController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setProducto(seleccionado);
            }

            Stage modalStage = crearStageModal("Ajustar existencias", root);
            modalStage.showAndWait();

            if (ctrl != null && ctrl.isAjusteGuardado()) {
                idProductoSeleccionarAlCargar = seleccionado.getIdProducto();
                cargarCatalogoDesdeBaseDeDatos();
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarError("Error de Ventana",
                    "No se pudo abrir la ventana de ajuste de existencias: " + e.getMessage());
        }
    }

    @FXML
    public void handleDarDeBaja() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede dar de baja o reactivar productos.");
            return;
        }
        Producto seleccionado = obtenerProductoSeleccionadoActual();
        if (seleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Seleccione un producto",
                    "Primero toque o haga clic sobre un producto de la tabla.");
            return;
        }

        // Si el producto ya esta inactivo, abre el modal de reactivacion
        if (!seleccionado.isActivo()) {
            handleReactivarProducto();
            return;
        }

        try {
            FXMLLoader loader = crearLoaderFXML("/fxml/modal_dar_baja.fxml");
            Parent root = loader.load();
            DarBajaProductoController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setProducto(seleccionado);
            }

            Stage modalStage = crearStageModal("¿Dar de baja este producto?", root);
            modalStage.showAndWait();

            if (ctrl != null && ctrl.isBajaConfirmada()) {
                idProductoSeleccionarAlCargar = -1;
                cargarCatalogoDesdeBaseDeDatos();
                if (tblProductos != null) {
                    tblProductos.getSelectionModel().clearSelection();
                }
                actualizarBarraSeleccion(null);

                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarError("Error de Ventana",
                    "No se pudo abrir la confirmación para dar de baja: " + e.getMessage());
        }
    }

    @FXML
    public void handleReactivarProducto() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede volver a activar productos.");
            return;
        }
        Producto seleccionado = obtenerProductoSeleccionadoActual();
        if (seleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Seleccione un producto",
                    "Primero toque o haga clic sobre un producto dado de baja en la tabla.");
            return;
        }

        try {
            FXMLLoader loader = crearLoaderFXML("/fxml/modal_reactivar_producto.fxml");
            Parent root = loader.load();
            ReactivarProductoController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setProducto(seleccionado);
            }

            Stage modalStage = crearStageModal("¿Volver a activar este producto?", root);
            modalStage.showAndWait();

            if (ctrl != null && ctrl.isReactivacionConfirmada()) {
                idProductoSeleccionarAlCargar = seleccionado.getIdProducto();
                cargarCatalogoDesdeBaseDeDatos();

                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarError("Error de Ventana",
                    "No se pudo abrir la ventana para volver a activar el producto: " + e.getMessage());
        }
    }

    private Producto obtenerProductoSeleccionadoActual() {
        Producto sel = tblProductos != null
                ? tblProductos.getSelectionModel().getSelectedItem()
                : productoSeleccionado;
        return sel != null ? sel : productoSeleccionado;
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

        if (tblProductos != null && tblProductos.getScene() != null && tblProductos.getScene().getWindow() != null) {
            final javafx.stage.Window owner = tblProductos.getScene().getWindow();
            stage.initOwner(owner);
            final javafx.scene.Node ownerRoot = tblProductos.getScene().getRoot();
            final javafx.scene.effect.ColorAdjust dim = new javafx.scene.effect.ColorAdjust(0, 0, -0.28, 0);
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
    public void volverAlMenuPrincipal() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarMenuDashboard();
        }
    }

    private String formatearCodigo(int idProducto) {
        return String.format("PRD-%03d", Integer.valueOf(idProducto));
    }

    private String formatearCategoria(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "General";
        }
        String limpia = raw.trim().toLowerCase(LOCALE_ES_CO);
        return Character.toUpperCase(limpia.charAt(0)) + limpia.substring(1);
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
