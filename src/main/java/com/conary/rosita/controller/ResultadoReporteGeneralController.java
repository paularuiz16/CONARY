package com.conary.rosita.controller;

import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.dao.ProveedorDAO;
import com.conary.rosita.dao.ReporteDAO;
import com.conary.rosita.dao.ReporteDAO.FilaCierreCajaReporte;
import com.conary.rosita.model.Cliente;
import com.conary.rosita.model.Proveedor;
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
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

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
 * Controlador reutilizable para los reportes generales:
 * 1) Lo que me deben (Clientes con fiado)
 * 2) Lo que debo a repartidores (Proveedores)
 * 3) Cierres de caja diarios
 */
public class ResultadoReporteGeneralController {

    public enum TipoReporte {
        FIADOS,
        PROVEEDORES,
        CAJA
    }

    public static class FilaGeneral {
        private final String[] valores = new String[6];
        private final boolean alerta;

        public FilaGeneral(String c0, String c1, String c2, String c3, String c4, String c5, boolean alerta) {
            valores[0] = c0 != null ? c0 : "";
            valores[1] = c1 != null ? c1 : "";
            valores[2] = c2 != null ? c2 : "";
            valores[3] = c3 != null ? c3 : "";
            valores[4] = c4 != null ? c4 : "";
            valores[5] = c5 != null ? c5 : "";
            this.alerta = alerta;
        }

        public String get(int idx) {
            return idx >= 0 && idx < valores.length ? valores[idx] : "";
        }

        public boolean isAlerta() {
            return alerta;
        }
    }

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final BigDecimal FACTOR_CERCA_TOPE = new BigDecimal("0.80");

    private static TipoReporte tipoPendiente = TipoReporte.FIADOS;
    private static LocalDate desdePendiente = LocalDate.now();
    private static LocalDate hastaPendiente = LocalDate.now();

    public static void setParametrosPendientes(TipoReporte tipo, LocalDate desde, LocalDate hasta) {
        tipoPendiente = tipo != null ? tipo : TipoReporte.FIADOS;
        desdePendiente = desde != null ? desde : LocalDate.now();
        hastaPendiente = hasta != null ? hasta : LocalDate.now();
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverReportes;
    @FXML private Label lblTituloReporte;
    @FXML private Label lblSubtituloReporte;
    @FXML private Button btnGuardarReporte;
    @FXML private Label lblUsuarioHeader;

    @FXML private Label lblKpi1Titulo;
    @FXML private Label lblKpi1Valor;
    @FXML private Label lblKpi2Titulo;
    @FXML private Label lblKpi2Valor;
    @FXML private Label lblKpi3Titulo;
    @FXML private Label lblKpi3Valor;

    @FXML private TableView<FilaGeneral> tblReporteGeneral;
    @FXML private TableColumn<FilaGeneral, FilaGeneral> col0;
    @FXML private TableColumn<FilaGeneral, FilaGeneral> col1;
    @FXML private TableColumn<FilaGeneral, FilaGeneral> col2;
    @FXML private TableColumn<FilaGeneral, FilaGeneral> col3;
    @FXML private TableColumn<FilaGeneral, FilaGeneral> col4;
    @FXML private TableColumn<FilaGeneral, FilaGeneral> col5;

    @FXML private Label lblTot0;
    @FXML private Label lblTot1;
    @FXML private Label lblTot2;
    @FXML private Label lblTot3;
    @FXML private Label lblTot4;
    @FXML private Label lblTot5;
    @FXML private Label lblNotaInferior;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final ReporteDAO reporteDAO = new ReporteDAO();

    private final ObservableList<FilaGeneral> filasTabla = FXCollections.observableArrayList();
    private List<String> encabezadosActuales = new ArrayList<String>();
    private List<String> filaTotalesExport = new ArrayList<String>();
    private String tituloActual = "Reporte";
    private String subtituloActual = "";
    private String notaActual = "";

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarTablaBase();

        if (tipoPendiente != null) {
            cargarReporte(tipoPendiente, desdePendiente, hastaPendiente);
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

    private void configurarTablaBase() {
        tblReporteGeneral.setItems(filasTabla);
        tblReporteGeneral.setFixedCellSize(52.0);
        tblReporteGeneral.setRowFactory(tv -> new TableRow<FilaGeneral>() {
            @Override
            protected void updateItem(FilaGeneral item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(52.0);
                setMinHeight(52.0);
                setMaxHeight(52.0);
            }
        });
    }

    private void configurarColumna(TableColumn<FilaGeneral, FilaGeneral> col, final int idx,
                                   String titulo, final Pos alineacion, final boolean resaltarAlerta) {
        col.setText(titulo);
        col.setCellValueFactory(d -> new SimpleObjectProperty<FilaGeneral>(d.getValue()));
        col.setCellFactory(c -> new TableCell<FilaGeneral, FilaGeneral>() {
            @Override
            protected void updateItem(FilaGeneral item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setAlignment(alineacion);
                    String val = item.get(idx);
                    if (resaltarAlerta && item.isAlerta() && !val.equals("—")) {
                        Label pill = new Label(val);
                        pill.getStyleClass().add("rep-inv-pill-bajo");
                        setGraphic(pill);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(val);
                        if (idx == 0) {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #3E1E0B;");
                        } else if (alineacion == Pos.CENTER_RIGHT) {
                            setStyle("-fx-font-family: \"Georgia\", \"Times New Roman\", serif; -fx-font-weight: bold; -fx-text-fill: #3E1E0B;");
                        } else {
                            setStyle("-fx-text-fill: #3E1E0B;");
                        }
                    }
                }
            }
        });
    }

    public void cargarReporte(TipoReporte tipo, LocalDate desde, LocalDate hasta) {
        filasTabla.clear();
        LocalDate fDesde = desde != null ? desde : LocalDate.now();
        LocalDate fHasta = hasta != null ? hasta : LocalDate.now();

        if (tipo == TipoReporte.FIADOS) {
            cargarReporteFiados();
        } else if (tipo == TipoReporte.PROVEEDORES) {
            cargarReporteProveedores();
        } else {
            cargarReporteCierresCaja(fDesde, fHasta);
        }
    }

    private void cargarReporteFiados() {
        tituloActual = "Lo que me deben";
        subtituloActual = "Situación acumulada hasta hoy, " + formatearFechaLarga(LocalDate.now());
        notaActual = "Incluye clientes activos con saldo pendiente.";
        encabezadosActuales = Arrays.asList("Cliente", "Teléfono", "Cupo", "Debe ahora", "Disponible", "Aviso");

        configurarColumna(col0, 0, "Cliente", Pos.CENTER_LEFT, false);
        configurarColumna(col1, 1, "Teléfono", Pos.CENTER_LEFT, false);
        configurarColumna(col2, 2, "Cupo", Pos.CENTER_RIGHT, false);
        configurarColumna(col3, 3, "Debe ahora", Pos.CENTER_RIGHT, false);
        configurarColumna(col4, 4, "Disponible", Pos.CENTER_RIGHT, false);
        configurarColumna(col5, 5, "Aviso", Pos.CENTER, true);

        BigDecimal totalCobrar = BigDecimal.ZERO;
        int clientesConDeuda = 0;
        int cercaDelTope = 0;

        try {
            List<Cliente> activos = clienteDAO.listarTodos(true);
            if (activos != null) {
                for (Cliente c : activos) {
                    BigDecimal deuda = c.getSaldoPendiente() != null ? c.getSaldoPendiente() : BigDecimal.ZERO;
                    if (deuda.compareTo(BigDecimal.ZERO) <= 0 && !c.isFiadoAutorizado()) {
                        continue;
                    }
                    BigDecimal cupo = c.getLimiteCredito() != null ? c.getLimiteCredito() : BigDecimal.ZERO;
                    BigDecimal disp = c.getCreditoDisponible();
                    boolean cerca = c.isFiadoAutorizado()
                            && cupo.compareTo(BigDecimal.ZERO) > 0
                            && deuda.compareTo(cupo.multiply(FACTOR_CERCA_TOPE)) >= 0;

                    if (deuda.compareTo(BigDecimal.ZERO) > 0) {
                        clientesConDeuda++;
                        totalCobrar = totalCobrar.add(deuda);
                    }
                    if (cerca) cercaDelTope++;

                    filasTabla.add(new FilaGeneral(
                            c.getNombre(),
                            c.getTelefono() != null && !c.getTelefono().trim().isEmpty() ? c.getTelefono().trim() : "—",
                            c.isFiadoAutorizado() && cupo.compareTo(BigDecimal.ZERO) > 0 ? "$ " + formatearMoneda(cupo) : "Sin fiado",
                            "$ " + formatearMoneda(deuda),
                            c.isFiadoAutorizado() ? "$ " + formatearMoneda(disp) : "$ 0",
                            cerca ? "⚠ Cerca del tope" : "—",
                            cerca
                    ));
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos", "No se pudo consultar cartera de clientes: " + e.getMessage());
        }

        actualizarCabeceraYKpis(
                "Total por cobrar", "$ " + formatearMoneda(totalCobrar),
                "Clientes con deuda", String.valueOf(clientesConDeuda),
                "Cerca del tope", String.valueOf(cercaDelTope)
        );
        aplicarTotales("Total", "", "", "$ " + formatearMoneda(totalCobrar), "", "");
    }

    private void cargarReporteProveedores() {
        tituloActual = "Lo que debo a repartidores";
        subtituloActual = "Cuentas por pagar vigentes a hoy, " + formatearFechaLarga(LocalDate.now());
        notaActual = "Incluye proveedores activos registrados en el sistema.";
        encabezadosActuales = Arrays.asList("Proveedor", "Teléfono", "Qué le compra", "Próximo pago", "Le debo", "Aviso");

        configurarColumna(col0, 0, "Proveedor", Pos.CENTER_LEFT, false);
        configurarColumna(col1, 1, "Teléfono", Pos.CENTER_LEFT, false);
        configurarColumna(col2, 2, "Qué le compra", Pos.CENTER_LEFT, false);
        configurarColumna(col3, 3, "Próximo pago", Pos.CENTER_LEFT, false);
        configurarColumna(col4, 4, "Le debo", Pos.CENTER_RIGHT, false);
        configurarColumna(col5, 5, "Aviso", Pos.CENTER, true);

        BigDecimal totalPagar = BigDecimal.ZERO;
        int provConSaldo = 0;
        int vencenSemana = 0;
        LocalDate limite = LocalDate.now().plusDays(7);

        try {
            List<Proveedor> activos = proveedorDAO.listarTodos(true);
            if (activos != null) {
                for (Proveedor p : activos) {
                    BigDecimal deuda = p.getSaldoPendiente() != null ? p.getSaldoPendiente() : BigDecimal.ZERO;
                    boolean tieneDeuda = deuda.compareTo(BigDecimal.ZERO) > 0;
                    boolean vence = tieneDeuda && p.getProximoPago() != null && !p.getProximoPago().isAfter(limite);

                    if (tieneDeuda) {
                        provConSaldo++;
                        totalPagar = totalPagar.add(deuda);
                    }
                    if (vence) vencenSemana++;

                    filasTabla.add(new FilaGeneral(
                            p.getNombre(),
                            p.getTelefono() != null && !p.getTelefono().trim().isEmpty() ? p.getTelefono().trim() : "—",
                            p.getProductos() != null && !p.getProductos().trim().isEmpty() ? p.getProductos().trim() : "—",
                            (tieneDeuda && p.getProximoPago() != null) ? formatearDiaMes(p.getProximoPago()) : "—",
                            "$ " + formatearMoneda(deuda),
                            vence ? "⚠ Vence esta semana" : "—",
                            vence
                    ));
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos", "No se pudo consultar cuentas de proveedores: " + e.getMessage());
        }

        actualizarCabeceraYKpis(
                "Total por pagar", "$ " + formatearMoneda(totalPagar),
                "Proveedores con saldo", String.valueOf(provConSaldo),
                "Vencen en 7 días", String.valueOf(vencenSemana)
        );
        aplicarTotales("Total", "", "", "", "$ " + formatearMoneda(totalPagar), "");
    }

    private void cargarReporteCierresCaja(LocalDate desde, LocalDate hasta) {
        tituloActual = "Cierres de caja diarios";
        subtituloActual = "Del " + formatearFechaLarga(desde) + " al " + formatearFechaLarga(hasta);
        notaActual = "Muestra los turnos de caja cerrados dentro del período seleccionado.";
        encabezadosActuales = Arrays.asList("Fecha", "Responsable", "Base inicial", "Esperado", "Contado", "Diferencia");

        configurarColumna(col0, 0, "Fecha", Pos.CENTER_LEFT, false);
        configurarColumna(col1, 1, "Responsable", Pos.CENTER_LEFT, false);
        configurarColumna(col2, 2, "Base inicial", Pos.CENTER_RIGHT, false);
        configurarColumna(col3, 3, "Esperado", Pos.CENTER_RIGHT, false);
        configurarColumna(col4, 4, "Contado", Pos.CENTER_RIGHT, false);
        configurarColumna(col5, 5, "Diferencia", Pos.CENTER_RIGHT, false);

        int turnosCerrados = 0;
        BigDecimal totalVentasContado = BigDecimal.ZERO;
        BigDecimal sumBase = BigDecimal.ZERO;
        BigDecimal sumEsperado = BigDecimal.ZERO;
        BigDecimal sumContado = BigDecimal.ZERO;
        BigDecimal sumDiferencia = BigDecimal.ZERO;

        try {
            List<FilaCierreCajaReporte> cierres = reporteDAO.listarCierresCaja(desde, hasta);
            if (cierres != null) {
                for (FilaCierreCajaReporte c : cierres) {
                    turnosCerrados++;
                    totalVentasContado = totalVentasContado.add(c.getVentasContado());
                    sumBase = sumBase.add(c.getBaseInicial());
                    sumEsperado = sumEsperado.add(c.getEsperado());
                    sumContado = sumContado.add(c.getContado());
                    sumDiferencia = sumDiferencia.add(c.getDiferencia());

                    filasTabla.add(new FilaGeneral(
                            formatearDiaMes(c.getFecha()),
                            c.getResponsable(),
                            "$ " + formatearMoneda(c.getBaseInicial()),
                            "$ " + formatearMoneda(c.getEsperado()),
                            "$ " + formatearMoneda(c.getContado()),
                            "$ " + formatearMoneda(c.getDiferencia()),
                            false
                    ));
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos", "No se pudo consultar el historial de caja: " + e.getMessage());
        }

        actualizarCabeceraYKpis(
                "Turnos cerrados", String.valueOf(turnosCerrados),
                "Total ventas contado", "$ " + formatearMoneda(totalVentasContado),
                "Diferencia acumulada", "$ " + formatearMoneda(sumDiferencia)
        );
        aplicarTotales(
                "Total", "",
                "$ " + formatearMoneda(sumBase),
                "$ " + formatearMoneda(sumEsperado),
                "$ " + formatearMoneda(sumContado),
                "$ " + formatearMoneda(sumDiferencia)
        );
    }

    private void actualizarCabeceraYKpis(String k1T, String k1V, String k2T, String k2V, String k3T, String k3V) {
        if (lblTituloReporte != null) lblTituloReporte.setText(tituloActual);
        if (lblSubtituloReporte != null) lblSubtituloReporte.setText(subtituloActual);
        if (lblKpi1Titulo != null) lblKpi1Titulo.setText(k1T);
        if (lblKpi1Valor != null) lblKpi1Valor.setText(k1V);
        if (lblKpi2Titulo != null) lblKpi2Titulo.setText(k2T);
        if (lblKpi2Valor != null) lblKpi2Valor.setText(k2V);
        if (lblKpi3Titulo != null) lblKpi3Titulo.setText(k3T);
        if (lblKpi3Valor != null) lblKpi3Valor.setText(k3V);
        if (lblNotaInferior != null) lblNotaInferior.setText("ℹ  " + notaActual);
    }

    private void aplicarTotales(String t0, String t1, String t2, String t3, String t4, String t5) {
        filaTotalesExport = Arrays.asList(t0, t1, t2, t3, t4, t5);
        if (lblTot0 != null) lblTot0.setText(t0);
        if (lblTot1 != null) lblTot1.setText(t1);
        if (lblTot2 != null) lblTot2.setText(t2);
        if (lblTot3 != null) lblTot3.setText(t3);
        if (lblTot4 != null) lblTot4.setText(t4);
        if (lblTot5 != null) lblTot5.setText(t5);
    }

    @FXML
    public void handleGuardarReporte() {
        List<List<String>> filas = new ArrayList<List<String>>();
        for (FilaGeneral fg : filasTabla) {
            filas.add(Arrays.asList(fg.get(0), fg.get(1), fg.get(2), fg.get(3), fg.get(4), fg.get(5)));
        }

        DatosExportacion datos = new DatosExportacion(
                tituloActual,
                subtituloActual,
                encabezadosActuales,
                filas,
                filaTotalesExport,
                notaActual
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