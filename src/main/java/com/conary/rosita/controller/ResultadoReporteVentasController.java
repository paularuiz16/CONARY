package com.conary.rosita.controller;

import com.conary.rosita.dao.ReporteDAO;
import com.conary.rosita.dao.ReporteDAO.FilaVentaPorDia;
import com.conary.rosita.dao.ReporteDAO.FilaVentaPorProducto;
import com.conary.rosita.dao.ReporteDAO.ResumenVentas;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.ExportadorReportesService.DatosExportacion;
import com.conary.rosita.util.SesionUsuario;
import javafx.stage.Window;
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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista B del Paso 1 del Modulo 6: Resultado "Ventas" (Por dia / Por producto).
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ResultadoReporteVentasController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static LocalDate desdePendiente;
    private static LocalDate hastaPendiente;

    public static void setRangoPendiente(LocalDate desde, LocalDate hasta) {
        desdePendiente = desde;
        hastaPendiente = hasta;
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverReportes;
    @FXML private Label lblRangoSubtitulo;
    @FXML private Button btnGuardarReporte;
    @FXML private Label lblUsuarioHeader;

    // 4 Tarjetas KPI
    @FXML private Label lblTotalVendido;
    @FXML private Label lblTotalContado;
    @FXML private Label lblTotalFiado;
    @FXML private Label lblCantidadVentas;

    // Pestanas y Tablas
    @FXML private ToggleButton tbPorDia;
    @FXML private ToggleButton tbPorProducto;

    @FXML private TableView<FilaVentaPorDia> tblPorDia;
    @FXML private TableColumn<FilaVentaPorDia, FilaVentaPorDia> colDia;
    @FXML private TableColumn<FilaVentaPorDia, FilaVentaPorDia> colDiaVentas;
    @FXML private TableColumn<FilaVentaPorDia, FilaVentaPorDia> colDiaContado;
    @FXML private TableColumn<FilaVentaPorDia, FilaVentaPorDia> colDiaFiado;
    @FXML private TableColumn<FilaVentaPorDia, FilaVentaPorDia> colDiaTotal;

    @FXML private TableView<FilaVentaPorProducto> tblPorProducto;
    @FXML private TableColumn<FilaVentaPorProducto, FilaVentaPorProducto> colProducto;
    @FXML private TableColumn<FilaVentaPorProducto, FilaVentaPorProducto> colProdUnidades;
    @FXML private TableColumn<FilaVentaPorProducto, FilaVentaPorProducto> colProdContado;
    @FXML private TableColumn<FilaVentaPorProducto, FilaVentaPorProducto> colProdFiado;
    @FXML private TableColumn<FilaVentaPorProducto, FilaVentaPorProducto> colProdTotal;

    // Fila fija de totales y nota inferior
    @FXML private Label lblFilaTotalConteo;
    @FXML private Label lblFilaTotalContado;
    @FXML private Label lblFilaTotalFiado;
    @FXML private Label lblFilaTotalGeneral;
    @FXML private Label lblNotaAnuladas;

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final ToggleGroup tgVista = new ToggleGroup();
    private final ObservableList<FilaVentaPorDia> datosPorDia = FXCollections.observableArrayList();
    private final ObservableList<FilaVentaPorProducto> datosPorProducto = FXCollections.observableArrayList();

    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private ResumenVentas resumenActual = new ResumenVentas(0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    private int totalUnidadesProductos = 0;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarPestanas();
        configurarTablaPorDia();
        configurarTablaPorProducto();

        if (desdePendiente != null && hastaPendiente != null) {
            cargarReporte(desdePendiente, hastaPendiente);
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
            lblUsuarioHeader.setText(nombre + " · " + rol);
        }
    }

    private void configurarPestanas() {
        if (tbPorDia != null) tbPorDia.setToggleGroup(tgVista);
        if (tbPorProducto != null) tbPorProducto.setToggleGroup(tgVista);
        if (tbPorDia != null) tbPorDia.setSelected(true);

        tgVista.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }
            actualizarVistaActiva();
        });
    }

    private void actualizarVistaActiva() {
        Toggle sel = tgVista.getSelectedToggle();
        boolean verPorDia = sel == tbPorDia;

        if (tblPorDia != null) {
            tblPorDia.setVisible(verPorDia);
            tblPorDia.setManaged(verPorDia);
        }
        if (tblPorProducto != null) {
            tblPorProducto.setVisible(!verPorDia);
            tblPorProducto.setManaged(!verPorDia);
        }

        if (lblFilaTotalConteo != null) {
            lblFilaTotalConteo.setText(String.valueOf(
                    verPorDia ? resumenActual.getCantidadVentasValidas() : totalUnidadesProductos));
        }
    }

    private void configurarTablaPorDia() {
        tblPorDia.setItems(datosPorDia);
        tblPorDia.setFixedCellSize(52.0);
        tblPorDia.setRowFactory(tv -> new TableRow<FilaVentaPorDia>() {
            @Override
            protected void updateItem(FilaVentaPorDia item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(52.0);
                setMinHeight(52.0);
                setMaxHeight(52.0);
            }
        });

        colDia.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorDia>(d.getValue()));
        colDia.setCellFactory(col -> new TableCell<FilaVentaPorDia, FilaVentaPorDia>() {
            @Override
            protected void updateItem(FilaVentaPorDia f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setText(formatearDiaMes(f.getDia()));
                    getStyleClass().removeAll("cuenta-cliente-cell-fecha");
                    getStyleClass().add("cuenta-cliente-cell-fecha");
                }
            }
        });

        colDiaVentas.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorDia>(d.getValue()));
        colDiaVentas.setCellFactory(col -> new TableCell<FilaVentaPorDia, FilaVentaPorDia>() {
            @Override
            protected void updateItem(FilaVentaPorDia f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER);
                    setText(String.valueOf(f.getCantidadVentas()));
                }
            }
        });

        colDiaContado.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorDia>(d.getValue()));
        colDiaContado.setCellFactory(col -> new TableCell<FilaVentaPorDia, FilaVentaPorDia>() {
            @Override
            protected void updateItem(FilaVentaPorDia f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    setText("$ " + formatearMoneda(f.getTotalContado()));
                }
            }
        });

        colDiaFiado.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorDia>(d.getValue()));
        colDiaFiado.setCellFactory(col -> new TableCell<FilaVentaPorDia, FilaVentaPorDia>() {
            @Override
            protected void updateItem(FilaVentaPorDia f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    setText("$ " + formatearMoneda(f.getTotalFiado()));
                }
            }
        });

        colDiaTotal.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorDia>(d.getValue()));
        colDiaTotal.setCellFactory(col -> new TableCell<FilaVentaPorDia, FilaVentaPorDia>() {
            @Override
            protected void updateItem(FilaVentaPorDia f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    setText("$ " + formatearMoneda(f.getTotal()));
                    getStyleClass().removeAll("cuenta-cliente-cell-saldo");
                    getStyleClass().add("cuenta-cliente-cell-saldo");
                }
            }
        });
    }

    private void configurarTablaPorProducto() {
        tblPorProducto.setItems(datosPorProducto);
        tblPorProducto.setFixedCellSize(52.0);
        tblPorProducto.setRowFactory(tv -> new TableRow<FilaVentaPorProducto>() {
            @Override
            protected void updateItem(FilaVentaPorProducto item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(52.0);
                setMinHeight(52.0);
                setMaxHeight(52.0);
            }
        });

        colProducto.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorProducto>(d.getValue()));
        colProducto.setCellFactory(col -> new TableCell<FilaVentaPorProducto, FilaVentaPorProducto>() {
            @Override
            protected void updateItem(FilaVentaPorProducto f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setText(f.getNombreProducto());
                    getStyleClass().removeAll("cuenta-cliente-cell-fecha");
                    getStyleClass().add("cuenta-cliente-cell-fecha");
                }
            }
        });

        colProdUnidades.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorProducto>(d.getValue()));
        colProdUnidades.setCellFactory(col -> new TableCell<FilaVentaPorProducto, FilaVentaPorProducto>() {
            @Override
            protected void updateItem(FilaVentaPorProducto f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER);
                    setText(String.valueOf(f.getUnidades()));
                }
            }
        });

        colProdContado.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorProducto>(d.getValue()));
        colProdContado.setCellFactory(col -> new TableCell<FilaVentaPorProducto, FilaVentaPorProducto>() {
            @Override
            protected void updateItem(FilaVentaPorProducto f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    setText("$ " + formatearMoneda(f.getTotalContado()));
                }
            }
        });

        colProdFiado.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorProducto>(d.getValue()));
        colProdFiado.setCellFactory(col -> new TableCell<FilaVentaPorProducto, FilaVentaPorProducto>() {
            @Override
            protected void updateItem(FilaVentaPorProducto f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    setText("$ " + formatearMoneda(f.getTotalFiado()));
                }
            }
        });

        colProdTotal.setCellValueFactory(d -> new SimpleObjectProperty<FilaVentaPorProducto>(d.getValue()));
        colProdTotal.setCellFactory(col -> new TableCell<FilaVentaPorProducto, FilaVentaPorProducto>() {
            @Override
            protected void updateItem(FilaVentaPorProducto f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) {
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER_RIGHT);
                    setText("$ " + formatearMoneda(f.getTotal()));
                    getStyleClass().removeAll("cuenta-cliente-cell-saldo");
                    getStyleClass().add("cuenta-cliente-cell-saldo");
                }
            }
        });
    }

    public void cargarReporte(LocalDate desde, LocalDate hasta) {
        this.fechaDesde = desde != null ? desde : LocalDate.now();
        this.fechaHasta = hasta != null ? hasta : LocalDate.now();

        if (lblRangoSubtitulo != null) {
            lblRangoSubtitulo.setText("Del " + formatearFechaLarga(this.fechaDesde)
                    + " al " + formatearFechaLarga(this.fechaHasta));
        }

        try {
            resumenActual = reporteDAO.obtenerResumenVentas(this.fechaDesde, this.fechaHasta);
            List<FilaVentaPorDia> filasDia = reporteDAO.listarVentasPorDia(this.fechaDesde, this.fechaHasta);
            List<FilaVentaPorProducto> filasProd = reporteDAO.listarVentasPorProducto(this.fechaDesde, this.fechaHasta);

            datosPorDia.setAll(filasDia);
            datosPorProducto.setAll(filasProd);

            totalUnidadesProductos = 0;
            for (FilaVentaPorProducto fp : filasProd) {
                totalUnidadesProductos += fp.getUnidades();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Consultar Reporte",
                    "No se pudo consultar el reporte de ventas en MySQL: " + e.getMessage());
            resumenActual = new ResumenVentas(0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
            datosPorDia.clear();
            datosPorProducto.clear();
            totalUnidadesProductos = 0;
        }

        if (lblTotalVendido != null) lblTotalVendido.setText("$ " + formatearMoneda(resumenActual.getTotalVendido()));
        if (lblTotalContado != null) lblTotalContado.setText("$ " + formatearMoneda(resumenActual.getTotalContado()));
        if (lblTotalFiado != null) lblTotalFiado.setText("$ " + formatearMoneda(resumenActual.getTotalFiado()));
        if (lblCantidadVentas != null) lblCantidadVentas.setText(String.valueOf(resumenActual.getCantidadVentasValidas()));

        if (lblFilaTotalContado != null) lblFilaTotalContado.setText("$ " + formatearMoneda(resumenActual.getTotalContado()));
        if (lblFilaTotalFiado != null) lblFilaTotalFiado.setText("$ " + formatearMoneda(resumenActual.getTotalFiado()));
        if (lblFilaTotalGeneral != null) lblFilaTotalGeneral.setText("$ " + formatearMoneda(resumenActual.getTotalVendido()));

        if (lblNotaAnuladas != null) {
            int anuladas = resumenActual.getCantidadVentasAnuladas();
            lblNotaAnuladas.setText(anuladas == 1
                    ? "ℹ  No cuenta 1 venta anulada."
                    : ("ℹ  No cuentan " + anuladas + " ventas anuladas."));
        }

        actualizarVistaActiva();
    }

    @FXML
    public void handleGuardarReporte() {
        boolean verPorDia = tgVista.getSelectedToggle() == tbPorDia;
        List<String> headers;
        List<List<String>> filas = new ArrayList<List<String>>();

        if (verPorDia) {
            headers = Arrays.asList("Día", "Ventas", "De contado", "A fiado", "Total");
            for (FilaVentaPorDia f : datosPorDia) {
                filas.add(Arrays.asList(
                        formatearDiaMes(f.getDia()),
                        String.valueOf(f.getCantidadVentas()),
                        "$ " + formatearMoneda(f.getTotalContado()),
                        "$ " + formatearMoneda(f.getTotalFiado()),
                        "$ " + formatearMoneda(f.getTotal())
                ));
            }
        } else {
            headers = Arrays.asList("Producto", "Unidades", "De contado", "A fiado", "Total");
            for (FilaVentaPorProducto f : datosPorProducto) {
                filas.add(Arrays.asList(
                        f.getNombreProducto(),
                        String.valueOf(f.getUnidades()),
                        "$ " + formatearMoneda(f.getTotalContado()),
                        "$ " + formatearMoneda(f.getTotalFiado()),
                        "$ " + formatearMoneda(f.getTotal())
                ));
            }
        }

        List<String> filaTotales = Arrays.asList(
                "Total",
                String.valueOf(verPorDia ? resumenActual.getCantidadVentasValidas() : totalUnidadesProductos),
                "$ " + formatearMoneda(resumenActual.getTotalContado()),
                "$ " + formatearMoneda(resumenActual.getTotalFiado()),
                "$ " + formatearMoneda(resumenActual.getTotalVendido())
        );

        String sub = lblRangoSubtitulo != null ? lblRangoSubtitulo.getText() : "";
        String nota = lblNotaAnuladas != null ? lblNotaAnuladas.getText().replace("ℹ", "").trim() : "";
        DatosExportacion datos = new DatosExportacion(
                verPorDia ? "Ventas (Por día)" : "Ventas (Por producto)",
                sub,
                headers,
                filas,
                filaTotales,
                nota
        );

        Window owner = rootPane != null && rootPane.getScene() != null ? rootPane.getScene().getWindow() : null;
        GuardarReporteController.abrirModal(owner, datos);
    }

    @FXML
    public void handleVolverAReportes() {
        desdePendiente = null;
        hastaPendiente = null;
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/reportes.fxml");
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
        if (fecha == null) return "—";
        return fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_ES_CO));
    }

    private String formatearFechaLarga(LocalDate fecha) {
        if (fecha == null) return "";
        return fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", LOCALE_ES_CO));
    }
}