package com.conary.rosita.controller;

import com.conary.rosita.controller.ResultadoReporteGeneralController.TipoReporte;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/**
 * Controlador de la Vista A del Paso 1 del Modulo 6: Selector "Reportes del Negocio".
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ReportesController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final DateTimeFormatter FMT_CORTO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverMenu;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;

    // 5 temas del negocio
    @FXML private ToggleButton tbTemaVentas;
    @FXML private ToggleButton tbTemaInventario;
    @FXML private ToggleButton tbTemaFiados;
    @FXML private ToggleButton tbTemaProveedores;
    @FXML private ToggleButton tbTemaCaja;

    @FXML private StackPane iconBoxVentas;
    @FXML private StackPane iconBoxInventario;
    @FXML private StackPane iconBoxFiados;
    @FXML private StackPane iconBoxProveedores;
    @FXML private StackPane iconBoxCaja;

    @FXML private SVGPath svgVentas;
    @FXML private SVGPath svgInventario;
    @FXML private SVGPath svgFiados;
    @FXML private SVGPath svgProveedores;
    @FXML private SVGPath svgCaja;

    @FXML private Circle dotVentas;
    @FXML private Circle dotInventario;
    @FXML private Circle dotFiados;
    @FXML private Circle dotProveedores;
    @FXML private Circle dotCaja;

    @FXML private Label chkVentas;
    @FXML private Label chkInventario;
    @FXML private Label chkFiados;
    @FXML private Label chkProveedores;
    @FXML private Label chkCaja;

    // Selector de fechas
    @FXML private HBox boxAlertaFechas;
    @FXML private ToggleButton tbPeriodoHoy;
    @FXML private ToggleButton tbPeriodoSemana;
    @FXML private ToggleButton tbPeriodoMes;
    @FXML private ToggleButton tbPeriodoOtras;
    @FXML private Label lblRangoResumen;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private HBox boxNotaAcumulada;
    @FXML private Label lblNotaAcumulada;
    @FXML private Button btnVerReporte;

    private final ToggleGroup tgTemas = new ToggleGroup();
    private final ToggleGroup tgPeriodos = new ToggleGroup();
    private boolean actualizandoFechasProgramaticamente = false;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarTemas();
        configurarFechas();
        aplicarPeriodo("SEMANA");
        actualizarEstilosTemas();
        validarEstadoFechasYBoton();
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

    private void configurarTemas() {
        ToggleButton[] temas = new ToggleButton[]{
                tbTemaVentas, tbTemaInventario, tbTemaFiados, tbTemaProveedores, tbTemaCaja
        };
        for (ToggleButton tb : temas) {
            if (tb != null) {
                tb.setToggleGroup(tgTemas);
            }
        }
        if (tbTemaVentas != null) {
            tbTemaVentas.setSelected(true);
        }

        tgTemas.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }
            actualizarEstilosTemas();
            validarEstadoFechasYBoton();
        });
    }

    private void actualizarEstilosTemas() {
        Toggle sel = tgTemas.getSelectedToggle();
        actualizarVisualTarjeta(sel == tbTemaVentas, iconBoxVentas, svgVentas, dotVentas, chkVentas);
        actualizarVisualTarjeta(sel == tbTemaInventario, iconBoxInventario, svgInventario, dotInventario, chkInventario);
        actualizarVisualTarjeta(sel == tbTemaFiados, iconBoxFiados, svgFiados, dotFiados, chkFiados);
        actualizarVisualTarjeta(sel == tbTemaProveedores, iconBoxProveedores, svgProveedores, dotProveedores, chkProveedores);
        actualizarVisualTarjeta(sel == tbTemaCaja, iconBoxCaja, svgCaja, dotCaja, chkCaja);
    }

    private void actualizarVisualTarjeta(boolean seleccionada, StackPane iconBox, SVGPath svg, Circle dot, Label chk) {
        if (iconBox != null) {
            iconBox.getStyleClass().removeAll("reportes-topic-icon-box", "reportes-topic-icon-box-selected");
            iconBox.getStyleClass().add(seleccionada ? "reportes-topic-icon-box-selected" : "reportes-topic-icon-box");
        }
        if (svg != null) {
            svg.setStroke(Color.web(seleccionada ? "#FAF6F0" : "#704214"));
        }
        if (dot != null) {
            dot.setFill(Color.web(seleccionada ? "#5C300C" : "#FBE9DA"));
        }
        if (chk != null) {
            chk.setVisible(seleccionada);
        }
    }

    private void configurarFechas() {
        ToggleButton[] periodos = new ToggleButton[]{
                tbPeriodoHoy, tbPeriodoSemana, tbPeriodoMes, tbPeriodoOtras
        };
        for (ToggleButton tb : periodos) {
            if (tb != null) {
                tb.setToggleGroup(tgPeriodos);
            }
        }
        if (tbPeriodoSemana != null) {
            tbPeriodoSemana.setSelected(true);
        }

        StringConverter<LocalDate> converter = new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? FMT_CORTO.format(date) : "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string == null || string.trim().isEmpty()) {
                    return null;
                }
                try {
                    return LocalDate.parse(string.trim(), FMT_CORTO);
                } catch (Exception e) {
                    return null;
                }
            }
        };
        if (dpDesde != null) dpDesde.setConverter(converter);
        if (dpHasta != null) dpHasta.setConverter(converter);

        tgPeriodos.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }
            actualizarTextoBotonesPeriodo();
            if (newT == tbPeriodoHoy) {
                aplicarPeriodo("HOY");
            } else if (newT == tbPeriodoSemana) {
                aplicarPeriodo("SEMANA");
            } else if (newT == tbPeriodoMes) {
                aplicarPeriodo("MES");
            }
            validarEstadoFechasYBoton();
        });

        if (dpDesde != null) {
            dpDesde.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!actualizandoFechasProgramaticamente && tbPeriodoOtras != null) {
                    tbPeriodoOtras.setSelected(true);
                }
                validarEstadoFechasYBoton();
            });
        }
        if (dpHasta != null) {
            dpHasta.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!actualizandoFechasProgramaticamente && tbPeriodoOtras != null) {
                    tbPeriodoOtras.setSelected(true);
                }
                validarEstadoFechasYBoton();
            });
        }
    }

    private void actualizarTextoBotonesPeriodo() {
        Toggle sel = tgPeriodos.getSelectedToggle();
        if (tbPeriodoHoy != null) tbPeriodoHoy.setText(sel == tbPeriodoHoy ? "✓   Hoy" : "Hoy");
        if (tbPeriodoSemana != null) tbPeriodoSemana.setText(sel == tbPeriodoSemana ? "✓   Esta semana" : "Esta semana");
        if (tbPeriodoMes != null) tbPeriodoMes.setText(sel == tbPeriodoMes ? "✓   Este mes" : "Este mes");
        if (tbPeriodoOtras != null) tbPeriodoOtras.setText(sel == tbPeriodoOtras ? "✓   Otras fechas" : "Otras fechas");
    }

    private void aplicarPeriodo(String tipo) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy;
        LocalDate hasta = hoy;

        if ("SEMANA".equals(tipo)) {
            desde = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            hasta = hoy;
        } else if ("MES".equals(tipo)) {
            desde = hoy.withDayOfMonth(1);
            hasta = hoy;
        }

        actualizandoFechasProgramaticamente = true;
        if (dpDesde != null) dpDesde.setValue(desde);
        if (dpHasta != null) dpHasta.setValue(hasta);
        actualizandoFechasProgramaticamente = false;
    }

    private boolean temaUsaFechas() {
        Toggle sel = tgTemas.getSelectedToggle();
        return sel != tbTemaInventario && sel != tbTemaFiados;
    }

    private void validarEstadoFechasYBoton() {
        boolean usaFechas = temaUsaFechas();

        if (tbPeriodoHoy != null) tbPeriodoHoy.setDisable(!usaFechas);
        if (tbPeriodoSemana != null) tbPeriodoSemana.setDisable(!usaFechas);
        if (tbPeriodoMes != null) tbPeriodoMes.setDisable(!usaFechas);
        if (tbPeriodoOtras != null) tbPeriodoOtras.setDisable(!usaFechas);
        if (dpDesde != null) dpDesde.setDisable(!usaFechas);
        if (dpHasta != null) dpHasta.setDisable(!usaFechas);

        LocalDate desde = dpDesde != null ? dpDesde.getValue() : null;
        LocalDate hasta = dpHasta != null ? dpHasta.getValue() : null;
        boolean rangoInvalido = usaFechas && (desde == null || hasta == null || desde.isAfter(hasta));

        if (boxAlertaFechas != null) {
            boolean mostrarAlerta = usaFechas && desde != null && hasta != null && desde.isAfter(hasta);
            boxAlertaFechas.setVisible(mostrarAlerta);
            boxAlertaFechas.setManaged(mostrarAlerta);
        }

        if (lblRangoResumen != null) {
            if (!usaFechas) {
                lblRangoResumen.setText("📅  Situación acumulada hasta hoy (" + formatearFechaLarga(LocalDate.now()) + ")");
            } else if (desde != null && hasta != null) {
                lblRangoResumen.setText("📅  Del " + formatearFechaConDia(desde) + " al " + formatearFechaConDia(hasta));
            } else {
                lblRangoResumen.setText("📅  Seleccione ambas fechas en el calendario");
            }
        }

        if (btnVerReporte != null) {
            btnVerReporte.setDisable(rangoInvalido);
        }
    }

    @FXML
    public void handleVerReporte() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede consultar reportes del negocio.");
            return;
        }

        Toggle tema = tgTemas.getSelectedToggle();
        LocalDate desde = dpDesde != null && dpDesde.getValue() != null ? dpDesde.getValue() : LocalDate.now();
        LocalDate hasta = dpHasta != null && dpHasta.getValue() != null ? dpHasta.getValue() : LocalDate.now();

        if (temaUsaFechas() && desde.isAfter(hasta)) {
            validarEstadoFechasYBoton();
            return;
        }

        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu == null) {
            return;
        }

        if (tema == tbTemaVentas) {
            ResultadoReporteVentasController.setRangoPendiente(desde, hasta);
            ResultadoReporteVentasController ctrl = menu.cargarVistaConControlador("/fxml/resultado_reporte_ventas.fxml");
            if (ctrl != null) {
                ctrl.cargarReporte(desde, hasta);
            }
        } else if (tema == tbTemaInventario) {
            menu.cargarVistaEnContenido("/fxml/resultado_reporte_inventario.fxml");
        } else {
            TipoReporte tipoGeneral = TipoReporte.FIADOS;
            if (tema == tbTemaProveedores) tipoGeneral = TipoReporte.PROVEEDORES;
            else if (tema == tbTemaCaja) tipoGeneral = TipoReporte.CAJA;

            ResultadoReporteGeneralController.setParametrosPendientes(tipoGeneral, desde, hasta);
            ResultadoReporteGeneralController ctrl = menu.cargarVistaConControlador("/fxml/resultado_reporte_general.fxml");
            if (ctrl != null) {
                ctrl.cargarReporte(tipoGeneral, desde, hasta);
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

    private String formatearFechaConDia(LocalDate f) {
        if (f == null) return "";
        return f.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", LOCALE_ES_CO));
    }

    private String formatearFechaLarga(LocalDate f) {
        if (f == null) return "";
        return f.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", LOCALE_ES_CO));
    }
}