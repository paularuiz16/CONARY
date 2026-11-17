package com.conary.rosita.controller;

import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.model.Producto;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.ExportadorReportesService.DatosExportacion;
import com.conary.rosita.util.SesionUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Window;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista Resultado "Inventario" (Paso 1B del Modulo 6).
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ResultadoReporteInventarioController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final int FILAS_POR_PAGINA = 5;

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverReportes;
    @FXML private Label lblSubtituloFecha;
    @FXML private Button btnGuardarReporte;
    @FXML private Label lblUsuarioHeader;

    @FXML private Label lblConteoActivos;
    @FXML private Label lblConteoPocasExistencias;
    @FXML private Label lblConteoAgotados;

    @FXML private ToggleButton tbFiltroTodos;
    @FXML private ToggleButton tbFiltroPocas;
    @FXML private ToggleButton tbFiltroAgotados;

    @FXML private TableView<Producto> tblInventario;
    @FXML private TableColumn<Producto, Producto> colProducto;
    @FXML private TableColumn<Producto, Producto> colCategoria;
    @FXML private TableColumn<Producto, Producto> colExistencias;
    @FXML private TableColumn<Producto, Producto> colStockMinimo;
    @FXML private TableColumn<Producto, Producto> colAviso;

    @FXML private Label lblConteoMostrado;
    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPaginaSiguiente;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final ToggleGroup tgFiltros = new ToggleGroup();
    private final List<Producto> productosActivos = new ArrayList<Producto>();
    private final List<Producto> productosFiltrados = new ArrayList<Producto>();
    private final ObservableList<Producto> itemsPaginaActual = FXCollections.observableArrayList();

    private int paginaActual = 0;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        if (lblSubtituloFecha != null) {
            lblSubtituloFecha.setText("Cómo está el estante hoy, "
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", LOCALE_ES_CO)));
        }
        configurarFiltros();
        configurarTabla();
        cargarProductosActivosDesdeMySQL();
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

    private void configurarFiltros() {
        if (tbFiltroTodos != null) tbFiltroTodos.setToggleGroup(tgFiltros);
        if (tbFiltroPocas != null) tbFiltroPocas.setToggleGroup(tgFiltros);
        if (tbFiltroAgotados != null) tbFiltroAgotados.setToggleGroup(tgFiltros);
        if (tbFiltroTodos != null) tbFiltroTodos.setSelected(true);

        tgFiltros.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }
            paginaActual = 0;
            aplicarFiltroYPaginacion();
        });
    }

    private void configurarTabla() {
        tblInventario.setItems(itemsPaginaActual);
        tblInventario.setFixedCellSize(54.0);
        tblInventario.setRowFactory(tv -> new TableRow<Producto>() {
            @Override
            protected void updateItem(Producto item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(54.0);
                setMinHeight(54.0);
                setMaxHeight(54.0);
            }
        });

        colProducto.setCellValueFactory(d -> new SimpleObjectProperty<Producto>(d.getValue()));
        colProducto.setCellFactory(col -> new TableCell<Producto, Producto>() {
            private final Circle dot = new Circle(3.6);
            private final Label lbl = new Label();
            private final HBox box = new HBox(8, dot, lbl);

            {
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (p.getStockActual() == 0) {
                        dot.setFill(Color.web("#B91C1C"));
                    } else if (p.getStockActual() <= p.getStockMinimo()) {
                        dot.setFill(Color.web("#8C4A27"));
                    } else {
                        dot.setFill(Color.web("#E6C2A5"));
                    }
                    lbl.setText(p.getNombre());
                    lbl.getStyleClass().removeAll("clientes-cell-nombre");
                    lbl.getStyleClass().add("clientes-cell-nombre");
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        colCategoria.setCellValueFactory(d -> new SimpleObjectProperty<Producto>(d.getValue()));
        colCategoria.setCellFactory(col -> new TableCell<Producto, Producto>() {
            @Override
            protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    setText(p.getCategoria() != null ? p.getCategoria() : "GENERAL");
                }
            }
        });

        colExistencias.setCellValueFactory(d -> new SimpleObjectProperty<Producto>(d.getValue()));
        colExistencias.setCellFactory(col -> new TableCell<Producto, Producto>() {
            @Override
            protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER);
                    setText(String.valueOf(p.getStockActual()));
                    getStyleClass().removeAll("rep-inv-stock-zero", "rep-inv-stock-low", "rep-inv-stock-ok");
                    if (p.getStockActual() == 0) {
                        getStyleClass().add("rep-inv-stock-zero");
                    } else if (p.getStockActual() <= p.getStockMinimo()) {
                        getStyleClass().add("rep-inv-stock-low");
                    } else {
                        getStyleClass().add("rep-inv-stock-ok");
                    }
                }
            }
        });

        colStockMinimo.setCellValueFactory(d -> new SimpleObjectProperty<Producto>(d.getValue()));
        colStockMinimo.setCellFactory(col -> new TableCell<Producto, Producto>() {
            @Override
            protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER);
                    setText(String.valueOf(p.getStockMinimo()));
                }
            }
        });

        colAviso.setCellValueFactory(d -> new SimpleObjectProperty<Producto>(d.getValue()));
        colAviso.setCellFactory(col -> new TableCell<Producto, Producto>() {
            @Override
            protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER);
                    if (p.getStockActual() == 0) {
                        Label pill = new Label("⊗  Agotado");
                        pill.getStyleClass().add("rep-inv-pill-agotado");
                        HBox box = new HBox(pill);
                        box.setAlignment(Pos.CENTER);
                        setGraphic(box);
                        setText(null);
                    } else if (p.getStockActual() <= p.getStockMinimo()) {
                        Label pill = new Label("⚠  Pocas existencias");
                        pill.getStyleClass().add("rep-inv-pill-bajo");
                        HBox box = new HBox(pill);
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
    }

    private void cargarProductosActivosDesdeMySQL() {
        productosActivos.clear();
        int pocas = 0;
        int agotados = 0;

        try {
            List<Producto> activos = productoDAO.listarTodos(true);
            if (activos != null) {
                for (Producto p : activos) {
                    productosActivos.add(p);
                    if (p.getStockActual() == 0) {
                        agotados++;
                    } else if (p.getStockActual() <= p.getStockMinimo()) {
                        pocas++;
                    }
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo consultar el inventario en MySQL: " + e.getMessage());
        }

        int total = productosActivos.size();
        if (lblConteoActivos != null) lblConteoActivos.setText(String.valueOf(total));
        if (lblConteoPocasExistencias != null) lblConteoPocasExistencias.setText(String.valueOf(pocas));
        if (lblConteoAgotados != null) lblConteoAgotados.setText(String.valueOf(agotados));

        if (tbFiltroTodos != null) tbFiltroTodos.setText("Todos (" + total + ")");
        if (tbFiltroPocas != null) tbFiltroPocas.setText("⚠   Con pocas existencias (" + pocas + ")");
        if (tbFiltroAgotados != null) tbFiltroAgotados.setText("⊘   Agotados (" + agotados + ")");

        aplicarFiltroYPaginacion();
    }

    private void aplicarFiltroYPaginacion() {
        Toggle sel = tgFiltros.getSelectedToggle();
        productosFiltrados.clear();

        for (Producto p : productosActivos) {
            if (sel == tbFiltroPocas) {
                if (p.getStockActual() > 0 && p.getStockActual() <= p.getStockMinimo()) {
                    productosFiltrados.add(p);
                }
            } else if (sel == tbFiltroAgotados) {
                if (p.getStockActual() == 0) {
                    productosFiltrados.add(p);
                }
            } else {
                productosFiltrados.add(p);
            }
        }

        int totalFiltrados = productosFiltrados.size();
        int totalPaginas = Math.max(1, (int) Math.ceil((double) totalFiltrados / FILAS_POR_PAGINA));
        if (paginaActual >= totalPaginas) paginaActual = totalPaginas - 1;
        if (paginaActual < 0) paginaActual = 0;

        int desde = paginaActual * FILAS_POR_PAGINA;
        int hasta = Math.min(desde + FILAS_POR_PAGINA, totalFiltrados);
        itemsPaginaActual.clear();
        if (desde < hasta) {
            itemsPaginaActual.addAll(productosFiltrados.subList(desde, hasta));
        }

        if (lblConteoMostrado != null) {
            lblConteoMostrado.setText("Mostrando " + itemsPaginaActual.size() + " de " + totalFiltrados + " productos");
        }
        if (btnPaginaAnterior != null) btnPaginaAnterior.setDisable(paginaActual <= 0);
        if (btnPaginaSiguiente != null) btnPaginaSiguiente.setDisable(paginaActual >= totalPaginas - 1);
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
        int totalPaginas = Math.max(1, (int) Math.ceil((double) productosFiltrados.size() / FILAS_POR_PAGINA));
        if (paginaActual < totalPaginas - 1) {
            paginaActual++;
            aplicarFiltroYPaginacion();
        }
    }

    @FXML
    public void handleGuardarReporte() {
        List<String> headers = Arrays.asList("Producto", "Categoría", "Existencias", "Avisar al llegar a", "Aviso");
        List<List<String>> filas = new ArrayList<List<String>>();

        for (Producto p : productosFiltrados) {
            String aviso = p.getStockActual() == 0
                    ? "Agotado"
                    : (p.getStockActual() <= p.getStockMinimo() ? "Pocas existencias" : "-");
            filas.add(Arrays.asList(
                    p.getNombre(),
                    p.getCategoria() != null ? p.getCategoria() : "GENERAL",
                    String.valueOf(p.getStockActual()),
                    String.valueOf(p.getStockMinimo()),
                    aviso
            ));
        }

        String sub = lblSubtituloFecha != null ? lblSubtituloFecha.getText() : "";
        DatosExportacion datos = new DatosExportacion(
                "Inventario",
                sub,
                headers,
                filas,
                null,
                "No incluye los productos dados de baja."
        );

        Window owner = rootPane != null && rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
        GuardarReporteController.abrirModal(owner, datos);
    }

    @FXML
    public void handleVolverAReportes() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/reportes.fxml");
        }
    }
}