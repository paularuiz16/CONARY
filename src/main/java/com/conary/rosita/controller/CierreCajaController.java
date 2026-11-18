package com.conary.rosita.controller;

import com.conary.rosita.dao.AbonoClienteDAO;
import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.dao.CajaDAO.ResumenVentasUsuarioTurno;
import com.conary.rosita.dao.CajaDAO.VentaOrdenTirillaTurno;
import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.dao.PagoProveedorDAO;
import com.conary.rosita.dao.ProveedorDAO;
import com.conary.rosita.dao.VentaDAO;
import com.conary.rosita.model.AbonoCliente;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Cliente;
import com.conary.rosita.model.PagoProveedor;
import com.conary.rosita.model.Proveedor;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.model.Venta;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.application.Platform;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del Bloque B del Modulo 2: Arqueo, Confirmacion Irreversible y Cierre de Turno.
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class CierreCajaController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    private static class ItemMovimientoTurno {
        final LocalDateTime fecha;
        final String descripcionConHora;
        final String montoFormateado;
        final boolean esSalida;

        ItemMovimientoTurno(LocalDateTime fecha, String descripcionConHora, String montoFormateado, boolean esSalida) {
            this.fecha = fecha != null ? fecha : LocalDateTime.now();
            this.descripcionConHora = descripcionConHora;
            this.montoFormateado = montoFormateado;
            this.esSalida = esSalida;
        }
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverHeader;
    @FXML private Label lblTituloHeader;
    @FXML private Label lblSubtituloFechaResumen;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;
    @FXML private HBox boxFechaHeaderDerecha;
    @FXML private Label lblFechaTurnoHeader;

    // Vista 1: Arqueo y Cierre Operativo
    @FXML private VBox panelArqueoOperativo;
    @FXML private Label lblBannerEstadoAbierta;
    @FXML private Label lblFondoBase;
    @FXML private Label lblEtiquetaVentasContado;
    @FXML private Label lblMontoVentasContado;
    @FXML private Label lblMontoAbonos;
    @FXML private Label lblMontoPagosProveedores;
    @FXML private Label lblTotalEsperado;
    @FXML private Label lblNotaVentasFiado;
    @FXML private VBox boxSeccionMovimientosHoy;
    @FXML private Button btnVerTodosMovimientos;
    @FXML private VBox boxMovimientosHoy;
    @FXML private VBox boxParaEncontrarDiferencia;
    @FXML private VBox boxVentasPorPersona;
    @FXML private ListView<VentaOrdenTirillaTurno> lvOrdenTirilla;

    @FXML private TextField txtTotalTirilla;
    @FXML private TextField txtEfectivoContado;
    @FXML private VBox boxResultadoArqueo;
    @FXML private Label lblEstadoCuadreCaja;
    @FXML private Label lblEstadoCuadreTirilla;
    @FXML private TextField txtObservaciones;
    @FXML private Button btnCerrarCaja;
    @FXML private Label lblNotaInferiorBoton;

    // Vista 2: Resumen Final "¡Caja cerrada!"
    @FXML private VBox panelCajaCerradaExito;
    @FXML private Label lblResumenCajaNumero;
    @FXML private Label lblResumenTotalEsperado;
    @FXML private Label lblResumenEfectivoContado;
    @FXML private Label lblResumenDiferenciaBadge;
    @FXML private Label lblResumenFechaTurno;
    @FXML private Button btnVolverMenuFinal;

    private final CajaDAO cajaDAO = new CajaDAO();
    private final VentaDAO ventaDAO = new VentaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final AbonoClienteDAO abonoClienteDAO = new AbonoClienteDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final PagoProveedorDAO pagoProveedorDAO = new PagoProveedorDAO();

    private Caja cajaActiva;
    private int cantidadVentasContadoTurno = 0;
    private BigDecimal totalVentasContadoTurno = BigDecimal.ZERO;
    private BigDecimal totalEsperadoTurno = BigDecimal.ZERO;
    private final List<ItemMovimientoTurno> movimientosDelTurno = new ArrayList<ItemMovimientoTurno>();
    private final List<ResumenVentasUsuarioTurno> ventasPorPersonaTurno = new ArrayList<ResumenVentasUsuarioTurno>();
    private final ObservableList<VentaOrdenTirillaTurno> ventasOrdenTirilla = FXCollections.observableArrayList();
    private boolean mostrandoTodosMovimientos = false;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarListaOrdenTirilla();
        configurarListenersEnTiempoReal();
        cargarDatosTurnoActivo();
    }

    private void configurarListaOrdenTirilla() {
        if (lvOrdenTirilla == null) {
            return;
        }
        final DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");
        lvOrdenTirilla.setItems(ventasOrdenTirilla);
        lvOrdenTirilla.setFixedCellSize(56.0);
        lvOrdenTirilla.setCellFactory(listView -> new ListCell<VentaOrdenTirillaTurno>() {
            @Override
            protected void updateItem(VentaOrdenTirillaTurno item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String hora = item.getFecha() != null ? item.getFecha().format(fmtHora) : "--:--";
                    setText(hora + "  ·  $ " + formatearMoneda(item.getTotalVenta()));
                    setGraphic(null);
                }
            }
        });
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

        if (lblUsuarioNombre != null) {
            lblUsuarioNombre.setText(nombre);
        }
        if (lblUsuarioRol != null) {
            lblUsuarioRol.setText(rol);
        }

        String fechaHoy = formatearFechaLarga(LocalDate.now());
        if (lblFechaTurnoHeader != null) {
            lblFechaTurnoHeader.setText(fechaHoy);
        }
        if (lblSubtituloFechaResumen != null) {
            lblSubtituloFechaResumen.setText(fechaHoy);
        }
    }

    private void configurarListenersEnTiempoReal() {
        if (txtEfectivoContado != null) {
            txtEfectivoContado.textProperty().addListener((obs, oldVal, newVal) -> recalcularValidacionEnTiempoReal());
        }
        if (txtTotalTirilla != null) {
            txtTotalTirilla.textProperty().addListener((obs, oldVal, newVal) -> recalcularValidacionEnTiempoReal());
        }

        if (rootPane != null) {
            rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER
                        && panelCajaCerradaExito != null
                        && panelCajaCerradaExito.isVisible()) {
                    event.consume();
                    handleVolverMenu();
                }
            });
        }
    }

    public void cargarDatosTurnoActivo() {
        try {
            cajaActiva = cajaDAO.obtenerCajaAbierta();
            SesionUsuario.setCajaActiva(cajaActiva);

            if (cajaActiva == null) {
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null && rootPane != null && rootPane.getScene() != null) {
                    menu.cargarVistaEnContenido("/fxml/abrir_caja.fxml");
                    return;
                }
                return;
            }

            int idCaja = cajaActiva.getIdCaja();
            BigDecimal base = cajaActiva.getMontoInicial() != null ? cajaActiva.getMontoInicial() : BigDecimal.ZERO;
            LocalDate fechaAperturaDate = cajaActiva.getFechaApertura() != null
                    ? cajaActiva.getFechaApertura().toLocalDate()
                    : LocalDate.now();
            boolean esDeHoy = fechaAperturaDate.isEqual(LocalDate.now());

            // Control de roles: si el turno pendiente es de un dia anterior, solo Administradora puede cerrarlo
            boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
            if (!esDeHoy && !esAdmin && btnCerrarCaja != null) {
                btnCerrarCaja.setDisable(true);
                if (lblNotaInferiorBoton != null) {
                    lblNotaInferiorBoton.setText("Solo Doña Rosita (Administradora) puede cerrar un turno de un día anterior.");
                }
            }

            if (lblFechaTurnoHeader != null) {
                lblFechaTurnoHeader.setText(formatearFechaLarga(fechaAperturaDate));
            }
            if (lblSubtituloFechaResumen != null) {
                lblSubtituloFechaResumen.setText(formatearFechaLarga(fechaAperturaDate));
            }

            if (lblBannerEstadoAbierta != null) {
                String cuando = esDeHoy ? "hoy" : ("el " + formatearFechaLarga(fechaAperturaDate).toLowerCase(LOCALE_ES_CO));
                lblBannerEstadoAbierta.setText("Estado actual: CAJA ABIERTA · Apertura registrada "
                        + cuando + " a las " + formatearHoraBanner(cajaActiva.getFechaApertura())
                        + " con fondo base inicial de $ " + formatearMoneda(base));
            }

            if (lblFondoBase != null) {
                lblFondoBase.setText("$ " + formatearMoneda(base));
            }

            // 1. Consultar Ventas de contado y Ventas a fiado del turno activo
            int cantidadVentasContado = 0;
            BigDecimal sumaVentasContado = BigDecimal.ZERO;
            BigDecimal sumaVentasFiado = BigDecimal.ZERO;
            movimientosDelTurno.clear();

            DateTimeFormatter fmtHoraMov = DateTimeFormatter.ofPattern("HH:mm");

            List<Venta> ventas = ventaDAO.listarVentas(
                    fechaAperturaDate.minusDays(1),
                    LocalDate.now().plusDays(1),
                    null
            );
            if (ventas != null) {
                for (Venta v : ventas) {
                    if (v.getIdCaja() != null && v.getIdCaja().intValue() == idCaja && !v.isAnulada()) {
                        BigDecimal montoVenta = v.getTotalVenta() != null ? v.getTotalVenta() : BigDecimal.ZERO;
                        if (v.isEsFiado()) {
                            sumaVentasFiado = sumaVentasFiado.add(montoVenta);
                        } else {
                            cantidadVentasContado++;
                            sumaVentasContado = sumaVentasContado.add(montoVenta);
                            String horaStr = v.getFecha() != null ? v.getFecha().format(fmtHoraMov) : "--:--";
                            movimientosDelTurno.add(new ItemMovimientoTurno(
                                    v.getFecha(),
                                    "Venta  ·  " + horaStr,
                                    "+ $ " + formatearMoneda(montoVenta),
                                    false
                            ));
                        }
                    }
                }
            }

            // 2. Consultar Abonos de clientes recibidos en este turno
            BigDecimal sumaAbonos = BigDecimal.ZERO;
            List<Cliente> clientes = clienteDAO.listarTodos(false);
            if (clientes != null) {
                for (Cliente cl : clientes) {
                    List<AbonoCliente> abonos = abonoClienteDAO.listarAbonosPorCliente(cl.getIdCliente());
                    if (abonos != null) {
                        for (AbonoCliente ab : abonos) {
                            if (ab.getIdCaja() != null && ab.getIdCaja().intValue() == idCaja) {
                                BigDecimal montoAb = ab.getMonto() != null ? ab.getMonto() : BigDecimal.ZERO;
                                sumaAbonos = sumaAbonos.add(montoAb);
                                String horaStr = ab.getFecha() != null ? ab.getFecha().format(fmtHoraMov) : "--:--";
                                movimientosDelTurno.add(new ItemMovimientoTurno(
                                        ab.getFecha(),
                                        "Abono de " + cl.getNombre() + "  ·  " + horaStr,
                                        "+ $ " + formatearMoneda(montoAb),
                                        false
                                ));
                            }
                        }
                    }
                }
            }

            // 3. Consultar Pagos a proveedores en efectivo en este turno
            BigDecimal sumaPagosCaja = BigDecimal.ZERO;
            List<Proveedor> proveedores = proveedorDAO.listarTodos(false);
            if (proveedores != null) {
                for (Proveedor pr : proveedores) {
                    List<PagoProveedor> pagos = pagoProveedorDAO.listarPagosPorProveedor(pr.getIdProveedor());
                    if (pagos != null) {
                        for (PagoProveedor pp : pagos) {
                            if (pp.getIdCaja() != null && pp.getIdCaja().intValue() == idCaja) {
                                BigDecimal montoPg = pp.getMonto() != null ? pp.getMonto() : BigDecimal.ZERO;
                                sumaPagosCaja = sumaPagosCaja.add(montoPg);
                                String horaStr = pp.getFecha() != null ? pp.getFecha().format(fmtHoraMov) : "--:--";
                                movimientosDelTurno.add(new ItemMovimientoTurno(
                                        pp.getFecha(),
                                        "Pago a proveedor (" + pr.getNombre() + ")  ·  " + horaStr,
                                        "– $ " + formatearMoneda(montoPg),
                                        true
                                ));
                            }
                        }
                    }
                }
            }

            cantidadVentasContadoTurno = cantidadVentasContado;
            totalVentasContadoTurno = sumaVentasContado;
            totalEsperadoTurno = base.add(sumaVentasContado).add(sumaAbonos).subtract(sumaPagosCaja);

            // Actualizar textos de la columna izquierda
            if (lblEtiquetaVentasContado != null) {
                lblEtiquetaVentasContado.setText("🛒   (+) Ventas de contado del día · "
                        + cantidadVentasContado + (cantidadVentasContado == 1 ? " venta:" : " ventas:"));
            }
            if (lblMontoVentasContado != null) {
                lblMontoVentasContado.setText("+ $ " + formatearMoneda(sumaVentasContado));
            }
            if (lblMontoAbonos != null) {
                lblMontoAbonos.setText("+ $ " + formatearMoneda(sumaAbonos));
            }
            if (lblMontoPagosProveedores != null) {
                lblMontoPagosProveedores.setText("– $ " + formatearMoneda(sumaPagosCaja));
            }
            if (lblTotalEsperado != null) {
                lblTotalEsperado.setText("$ " + formatearMoneda(totalEsperadoTurno));
            }
            if (lblNotaVentasFiado != null) {
                lblNotaVentasFiado.setText("ⓘ   Ventas a fiado (no entran a la caja): $ " + formatearMoneda(sumaVentasFiado));
            }

            // Cargar desglose para diferencia (Ventas por persona + Orden de la tirilla)
            ventasPorPersonaTurno.clear();
            List<ResumenVentasUsuarioTurno> porPersona = cajaDAO.listarVentasContadoPorPersonaTurno(idCaja);
            if (porPersona != null) {
                ventasPorPersonaTurno.addAll(porPersona);
            }

            List<VentaOrdenTirillaTurno> ordenTirilla = cajaDAO.listarVentasContadoOrdenTirillaTurno(idCaja);
            if (ordenTirilla != null) {
                ventasOrdenTirilla.setAll(ordenTirilla);
            } else {
                ventasOrdenTirilla.clear();
            }
            renderizarDesgloseDiferencia();

            // Ordenar y renderizar lista de movimientos
            Collections.sort(movimientosDelTurno, new Comparator<ItemMovimientoTurno>() {
                @Override
                public int compare(ItemMovimientoTurno a, ItemMovimientoTurno b) {
                    return b.fecha.compareTo(a.fecha);
                }
            });
            renderizarListaMovimientos();

            // Precargar campos de la columna derecha
            if (txtTotalTirilla != null) {
                txtTotalTirilla.setText("$ " + formatearMoneda(totalVentasContadoTurno));
            }
            if (txtEfectivoContado != null) {
                txtEfectivoContado.setText("$ " + formatearMoneda(totalEsperadoTurno));
            }

            recalcularValidacionEnTiempoReal();

        } catch (SQLException e) {
            System.err.println("[CierreCajaController] Error al consultar datos del arqueo en MySQL: " + e.getMessage());
            if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                AlertaUtil.mostrarError("Error de Base de Datos",
                        "No se pudo consultar el desglose del turno en MySQL: " + e.getMessage());
            }
        }
    }

    private void renderizarListaMovimientos() {
        if (boxMovimientosHoy == null) {
            return;
        }
        boxMovimientosHoy.getChildren().clear();

        if (movimientosDelTurno.isEmpty()) {
            Label lblVacio = new Label("No se han registrado ventas al contado, abonos ni pagos en efectivo en este turno.");
            lblVacio.getStyleClass().add("cierre-caja-mov-empty");
            lblVacio.setWrapText(true);
            boxMovimientosHoy.getChildren().add(lblVacio);
            if (btnVerTodosMovimientos != null) {
                btnVerTodosMovimientos.setDisable(true);
            }
            return;
        }

        if (btnVerTodosMovimientos != null) {
            btnVerTodosMovimientos.setDisable(movimientosDelTurno.size() <= 3);
            btnVerTodosMovimientos.setText(mostrandoTodosMovimientos ? "Ver recientes" : "Ver todos");
        }

        int cantidadMostrar = mostrandoTodosMovimientos
                ? movimientosDelTurno.size()
                : Math.min(3, movimientosDelTurno.size());

        for (int i = 0; i < cantidadMostrar; i++) {
            ItemMovimientoTurno item = movimientosDelTurno.get(i);
            HBox fila = new HBox(10);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("cierre-caja-mov-row");

            Label lblDesc = new Label(item.descripcionConHora);
            lblDesc.getStyleClass().add("cierre-caja-mov-desc");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblMonto = new Label(item.montoFormateado);
            lblMonto.getStyleClass().add(item.esSalida ? "cierre-caja-mov-amount-neg" : "cierre-caja-mov-amount-pos");

            fila.getChildren().addAll(lblDesc, spacer, lblMonto);
            boxMovimientosHoy.getChildren().add(fila);
        }
    }

    @FXML
    public void handleVerTodosMovimientos() {
        mostrandoTodosMovimientos = !mostrandoTodosMovimientos;
        renderizarListaMovimientos();
    }

    private void renderizarDesgloseDiferencia() {
        if (boxVentasPorPersona == null) {
            return;
        }
        boxVentasPorPersona.getChildren().clear();

        if (ventasPorPersonaTurno.isEmpty()) {
            Label lblVacio = new Label("No hay ventas de contado registradas por usuario en este turno.");
            lblVacio.getStyleClass().add("cierre-caja-diff-empty");
            lblVacio.setWrapText(true);
            boxVentasPorPersona.getChildren().add(lblVacio);
            return;
        }

        int sumaCantidad = 0;
        BigDecimal sumaTotal = BigDecimal.ZERO;

        for (ResumenVentasUsuarioTurno r : ventasPorPersonaTurno) {
            sumaCantidad += r.getCantidadVentas();
            sumaTotal = sumaTotal.add(r.getTotalVentas());

            HBox fila = new HBox(8);
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("cierre-caja-diff-person-row");

            String sufijoVentas = r.getCantidadVentas() == 1 ? " venta" : " ventas";
            Label lblPersona = new Label("👤  " + r.getNombreUsuario() + " · "
                    + r.getCantidadVentas() + sufijoVentas + " · $ " + formatearMoneda(r.getTotalVentas()));
            lblPersona.getStyleClass().add("cierre-caja-diff-person-text");
            lblPersona.setWrapText(true);

            fila.getChildren().add(lblPersona);
            boxVentasPorPersona.getChildren().add(fila);
        }

        int totalVentasMostrar = Math.max(cantidadVentasContadoTurno, sumaCantidad);
        BigDecimal montoTotalMostrar = totalVentasContadoTurno.compareTo(BigDecimal.ZERO) > 0
                ? totalVentasContadoTurno
                : sumaTotal;

        HBox filaTotal = new HBox(8);
        filaTotal.setAlignment(Pos.CENTER_LEFT);
        filaTotal.getStyleClass().add("cierre-caja-diff-total-row");

        Label lblTotal = new Label("Total · " + totalVentasMostrar
                + (totalVentasMostrar == 1 ? " venta" : " ventas")
                + " · $ " + formatearMoneda(montoTotalMostrar));
        lblTotal.getStyleClass().add("cierre-caja-diff-person-text");
        filaTotal.getChildren().add(lblTotal);
        boxVentasPorPersona.getChildren().add(filaTotal);
    }

    private void recalcularValidacionEnTiempoReal() {
        if (boxResultadoArqueo == null || lblEstadoCuadreCaja == null || lblEstadoCuadreTirilla == null) {
            return;
        }

        BigDecimal contado = parsearMonto(txtEfectivoContado != null ? txtEfectivoContado.getText() : null);
        BigDecimal tirilla = parsearMonto(txtTotalTirilla != null ? txtTotalTirilla.getText() : null);
        boolean hayDiferencia = contado != null
                && contado.compareTo(BigDecimal.ZERO) >= 0
                && contado.compareTo(totalEsperadoTurno) != 0;

        // 1. Evaluar cuadre del efectivo fisico vs total esperado
        if (contado == null || contado.compareTo(BigDecimal.ZERO) < 0) {
            aplicarEstiloCajaValidacion(false);
            lblEstadoCuadreCaja.setText("⚠  Escriba el efectivo contado en el cajón (Esperado $ "
                    + formatearMoneda(totalEsperadoTurno) + ")");
        } else {
            BigDecimal diferencia = contado.subtract(totalEsperadoTurno);
            if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
                aplicarEstiloCajaValidacion(true);
                lblEstadoCuadreCaja.setText("✓  La caja cuadra. Esperado $ "
                        + formatearMoneda(totalEsperadoTurno) + ", contado $ " + formatearMoneda(contado));
            } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
                aplicarEstiloCajaValidacion(false);
                lblEstadoCuadreCaja.setText("⚠  Faltan $ " + formatearMoneda(diferencia.abs())
                        + " · Esperado $ " + formatearMoneda(totalEsperadoTurno)
                        + ", contado $ " + formatearMoneda(contado));
            } else {
                aplicarEstiloCajaValidacion(false);
                lblEstadoCuadreCaja.setText("⚠  Sobran $ " + formatearMoneda(diferencia)
                        + " · Esperado $ " + formatearMoneda(totalEsperadoTurno)
                        + ", contado $ " + formatearMoneda(contado));
            }
        }

        // 2. Segunda linea de tirilla (oculta por completo cuando hay diferencia en efectivo)
        if (hayDiferencia) {
            setVisibilidadNodo(lblEstadoCuadreTirilla, false);
        } else {
            setVisibilidadNodo(lblEstadoCuadreTirilla, true);
            if (tirilla == null) {
                lblEstadoCuadreTirilla.setText("✓  La tirilla coincide con el sistema");
            } else {
                BigDecimal diffTirilla = tirilla.subtract(totalVentasContadoTurno);
                if (diffTirilla.compareTo(BigDecimal.ZERO) == 0) {
                    lblEstadoCuadreTirilla.setText("✓  La tirilla coincide con el sistema");
                } else {
                    lblEstadoCuadreTirilla.setText("⚠  La tirilla difiere por $ " + formatearMoneda(diffTirilla.abs()));
                }
            }
        }

        // 3. Alternar bloque izquierdo: "Movimientos de hoy" vs "Para encontrar la diferencia"
        setVisibilidadNodo(boxSeccionMovimientosHoy, !hayDiferencia);
        setVisibilidadNodo(boxParaEncontrarDiferencia, hayDiferencia);
    }

    private void setVisibilidadNodo(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void aplicarEstiloCajaValidacion(boolean cuadraExacto) {
        boxResultadoArqueo.getStyleClass().removeAll("cierre-caja-validation-ok", "cierre-caja-validation-warn");
        lblEstadoCuadreCaja.getStyleClass().removeAll("cierre-caja-validation-main-ok", "cierre-caja-validation-main-warn");
        lblEstadoCuadreTirilla.getStyleClass().removeAll("cierre-caja-validation-sub-ok", "cierre-caja-validation-sub-warn");

        if (cuadraExacto) {
            boxResultadoArqueo.getStyleClass().add("cierre-caja-validation-ok");
            lblEstadoCuadreCaja.getStyleClass().add("cierre-caja-validation-main-ok");
            lblEstadoCuadreTirilla.getStyleClass().add("cierre-caja-validation-sub-ok");
        } else {
            boxResultadoArqueo.getStyleClass().add("cierre-caja-validation-warn");
            lblEstadoCuadreCaja.getStyleClass().add("cierre-caja-validation-main-warn");
            lblEstadoCuadreTirilla.getStyleClass().add("cierre-caja-validation-sub-warn");
        }
    }

    @FXML
    public void handleAbrirModalConfirmarCierre() {
        if (cajaActiva == null) {
            return;
        }

        BigDecimal contado = parsearMonto(txtEfectivoContado != null ? txtEfectivoContado.getText() : null);
        if (contado == null || contado.compareTo(BigDecimal.ZERO) < 0) {
            AlertaUtil.mostrarAdvertencia("Efectivo Contado Inválido",
                    "Por favor ingrese un valor numérico válido mayor o igual a $ 0 en 'Efectivo que hay contado'.");
            if (txtEfectivoContado != null) {
                txtEfectivoContado.requestFocus();
            }
            return;
        }

        boolean esTurnoAnterior = cajaActiva.getFechaApertura() != null
                && cajaActiva.getFechaApertura().toLocalDate().isBefore(LocalDate.now());
        if (esTurnoAnterior && SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Permiso de Administradora Requerido",
                    "Solo Doña Rosita (Administradora) puede cerrar una caja pendiente de días anteriores.");
            return;
        }

        try {
            FXMLLoader loader = crearLoaderFXML("/fxml/modal_confirmar_cierre.fxml");
            Parent modalRoot = loader.load();
            ConfirmarCierreCajaController modalCtrl = loader.getController();
            if (modalCtrl != null) {
                modalCtrl.configurarDatos(totalEsperadoTurno, contado, esTurnoAnterior);
            }

            Stage modalStage = crearStageModal("¿Cerrar la caja de hoy?", modalRoot);
            modalStage.showAndWait();

            if (modalCtrl != null && modalCtrl.isCierreConfirmado()) {
                ejecutarCierreDefinitivoEnMySQL(contado);
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarError("Error de Ventana",
                    "No se pudo abrir la ventana de confirmación de cierre: " + e.getMessage());
        }
    }

    private void ejecutarCierreDefinitivoEnMySQL(BigDecimal efectivoContado) {
        if (cajaActiva == null) {
            return;
        }
        try {
            int idCerrada = cajaActiva.getIdCaja();
            BigDecimal esperado = totalEsperadoTurno;
            BigDecimal diferencia = efectivoContado.subtract(esperado);

            boolean cerrado = cajaDAO.cerrarCaja(idCerrada, efectivoContado);
            if (cerrado) {
                cajaActiva = null;
                SesionUsuario.setCajaActiva(null);

                // Configurar vista final "¡Caja cerrada!" (Imagen 4)
                if (lblResumenCajaNumero != null) {
                    lblResumenCajaNumero.setText("Caja N° " + idCerrada);
                }
                if (lblResumenTotalEsperado != null) {
                    lblResumenTotalEsperado.setText("$ " + formatearMoneda(esperado));
                }
                if (lblResumenEfectivoContado != null) {
                    lblResumenEfectivoContado.setText("$ " + formatearMoneda(efectivoContado));
                }
                if (lblResumenDiferenciaBadge != null) {
                    if (diferencia.compareTo(BigDecimal.ZERO) == 0) {
                        lblResumenDiferenciaBadge.setText("✓ Cuadra exacto ($ 0)");
                    } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
                        lblResumenDiferenciaBadge.setText("⚠ Faltan $ " + formatearMoneda(diferencia.abs()));
                    } else {
                        lblResumenDiferenciaBadge.setText("⚠ Sobran $ " + formatearMoneda(diferencia));
                    }
                }
                if (lblResumenFechaTurno != null) {
                    String usuarioCierre = lblUsuarioNombre != null ? lblUsuarioNombre.getText() : "Doña Rosita";
                    lblResumenFechaTurno.setText("🕒  " + formatearFechaLarga(LocalDate.now())
                            + " · " + formatearHoraResumen(LocalDateTime.now())
                            + " · Turno: " + usuarioCierre);
                }

                // Alternar encabezado y mostrar panelCajaCerradaExito
                if (boxFechaHeaderDerecha != null) {
                    boxFechaHeaderDerecha.setVisible(false);
                    boxFechaHeaderDerecha.setManaged(false);
                }
                if (lblSubtituloFechaResumen != null) {
                    lblSubtituloFechaResumen.setVisible(true);
                    lblSubtituloFechaResumen.setManaged(true);
                }
                if (panelArqueoOperativo != null) {
                    panelArqueoOperativo.setVisible(false);
                    panelArqueoOperativo.setManaged(false);
                }
                if (panelCajaCerradaExito != null) {
                    panelCajaCerradaExito.setVisible(true);
                    panelCajaCerradaExito.setManaged(true);
                }

                Platform.runLater(() -> {
                    if (btnVolverMenuFinal != null) {
                        btnVolverMenuFinal.requestFocus();
                    }
                });
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Cerrar Caja",
                    "No se pudo guardar el cierre de caja en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleVolverMenu() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarMenuDashboard();
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

    private String formatearFechaLarga(LocalDate fecha) {
        LocalDate f = fecha != null ? fecha : LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", LOCALE_ES_CO);
        String txt = f.format(fmt);
        if (txt == null || txt.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(txt.charAt(0)) + txt.substring(1);
    }

    private String formatearHoraBanner(LocalDateTime fechaHora) {
        LocalDateTime dt = fechaHora != null ? fechaHora : LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
        return dt.format(fmt);
    }

    private String formatearHoraResumen(LocalDateTime fechaHora) {
        LocalDateTime dt = fechaHora != null ? fechaHora : LocalDateTime.now();
        int h24 = dt.getHour();
        int min = dt.getMinute();
        int h12 = h24 % 12;
        if (h12 == 0) {
            h12 = 12;
        }
        String sufijo = h24 < 12 ? "a. m." : "p. m.";
        return String.format("%d:%02d %s", Integer.valueOf(h12), Integer.valueOf(min), sufijo);
    }
}
