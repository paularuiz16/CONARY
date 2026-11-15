package com.conary.rosita.controller;

import com.conary.rosita.App;
import com.conary.rosita.dao.AbonoClienteDAO;
import com.conary.rosita.dao.CajaDAO;
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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
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
 * Controlador de Caja, Turnos y Arqueo Diario (Modulo 2).
 * Soporta los 4 estados dinamicos:
 * - Estado A: Bloqueo por turno anterior pendiente sin cerrar.
 * - Estado B: Apertura de turno y confirmacion "¡Caja abierta!".
 * - Estado C: Vista informativa "La caja de hoy ya fue abierta".
 * - Estado D: Vista operativa de Arqueo y Cierre, modal de confirmacion y resumen "¡Caja cerrada!".
 */
public class CajaController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static boolean modoEntradaSoloApertura = false;

    public static void setModoEntradaSoloApertura(boolean soloApertura) {
        modoEntradaSoloApertura = soloApertura;
    }

    // =========================================================================
    // Elementos @FXML originales preservados intactos
    // =========================================================================
    @FXML private Label lblEstadoTurno;
    @FXML private Label lblFechaApertura;
    @FXML private Label lblMontoBase;
    @FXML private Button btnRefrescar;

    @FXML private VBox panelApertura;
    @FXML private TextField txtMontoInicial;
    @FXML private Button btnAbrirCaja;

    @FXML private VBox panelCierre;
    @FXML private Label lblTotalTirilla;
    @FXML private TextField txtEfectivoContado;
    @FXML private Label lblDiferencia;
    @FXML private Button btnCerrarCaja;

    @FXML private TableView<Caja> tblHistorialCaja;
    @FXML private TableColumn<Caja, Integer> colIdCaja;
    @FXML private TableColumn<Caja, String> colApertura;
    @FXML private TableColumn<Caja, String> colCierre;
    @FXML private TableColumn<Caja, String> colBase;
    @FXML private TableColumn<Caja, String> colTirilla;
    @FXML private TableColumn<Caja, String> colContado;
    @FXML private TableColumn<Caja, String> colDiferencia;

    // =========================================================================
    // Elementos @FXML de la nueva interfaz (Estados A, B, C, D y Modales)
    // =========================================================================
    @FXML private Label lblTituloHeaderCaja;
    @FXML private Label lblSubtituloHeaderCaja;
    @FXML private Label lblUsuarioHeaderNombre;
    @FXML private Label lblUsuarioHeaderRol;
    @FXML private HBox boxFechaHeaderDerecha;
    @FXML private Label lblFechaHeaderDerecha;

    // Estado A: Bloqueo turno anterior
    @FXML private VBox paneBloqueoAnterior;
    @FXML private Label lblTurnoPendienteInfo;
    @FXML private Label lblPermisoAdminAyer;
    @FXML private Button btnSimularAdmin;
    @FXML private Button btnSimularVendedora;
    @FXML private Button btnCerrarCajaAyer;

    // Estado B1 y B2: Apertura y Confirmacion "¡Caja abierta!"
    @FXML private Label lblReferenciaCierreAnterior;
    @FXML private VBox paneExitoApertura;
    @FXML private Label lblExitoBaseInicial;
    @FXML private Label lblExitoHoraApertura;
    @FXML private Label lblExitoCajera;
    @FXML private Label lblExitoCodigoTurno;

    // Estado C: "La caja de hoy ya fue abierta"
    @FXML private VBox paneYaAbierta;
    @FXML private Label lblYaAbiertaResumen;
    @FXML private Label lblYaAbiertaPor;

    // Estado D: Arqueo y Cierre Operativo
    @FXML private Label lblBannerEstadoAbierta;
    @FXML private Label lblTituloVentasContado;
    @FXML private Label lblValorVentasContado;
    @FXML private Label lblValorAbonosRecibidos;
    @FXML private Label lblValorPagosProveedores;
    @FXML private Label lblValorVentasFiado;
    @FXML private VBox boxListaMovimientosHoy;
    @FXML private TextField txtTotalTirillaInput;
    @FXML private VBox boxCuadreDiferencia;
    @FXML private Label lblSubEstadoTirilla;
    @FXML private TextField txtObservacionesCierre;

    // Estado D Final: Resumen "¡Caja cerrada!"
    @FXML private VBox paneExitoCierre;
    @FXML private Label lblResumenCajaNumero;
    @FXML private Label lblResumenEsperadoFinal;
    @FXML private Label lblResumenContadoFinal;
    @FXML private Label lblResumenDiferenciaBadge;
    @FXML private Label lblResumenFechaUsuario;

    // Modales
    @FXML private StackPane modalConfirmarCierre;
    @FXML private Label lblModalEsperado;
    @FXML private Label lblModalContado;
    @FXML private StackPane modalHistorialTurnos;

    // DAOs
    private final CajaDAO cajaDAO = new CajaDAO();
    private final VentaDAO ventaDAO = new VentaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final AbonoClienteDAO abonoClienteDAO = new AbonoClienteDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final PagoProveedorDAO pagoProveedorDAO = new PagoProveedorDAO();

    private final ObservableList<Caja> listaHistorial = FXCollections.observableArrayList();
    private final DateTimeFormatter formatoFechaTabla = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private Caja cajaActiva;
    private BigDecimal totalTirillaCalculado = BigDecimal.ZERO;
    private BigDecimal totalVentasContadoDia = BigDecimal.ZERO;
    private BigDecimal efectivoContadoPendienteCierre = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        configurarCabeceraUsuarioYFecha();
        configurarTabla();

        if (txtEfectivoContado != null) {
            txtEfectivoContado.textProperty().addListener((obs, viejo, nuevo) -> recalcularDiferenciaEnVivo());
        }
        if (txtTotalTirillaInput != null) {
            txtTotalTirillaInput.textProperty().addListener((obs, viejo, nuevo) -> recalcularDiferenciaEnVivo());
        }
        if (txtMontoInicial != null) {
            txtMontoInicial.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    handleAbrirCaja();
                }
            });
        }

        handleRefrescarCaja();
    }

    private void configurarCabeceraUsuarioYFecha() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        String nombre = "Doña Rosita";
        String rol = "Administradora";
        if (u != null) {
            String raw = u.getNombre() != null ? u.getNombre().trim() : "Usuario";
            if (raw.toLowerCase(LOCALE_ES_CO).contains("rosita") && !raw.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
                nombre = "Doña " + raw;
            } else {
                nombre = raw;
            }
            rol = u.esAdministrador() ? "Administradora" : "Vendedora";
        }
        if (lblUsuarioHeaderNombre != null) lblUsuarioHeaderNombre.setText(nombre);
        if (lblUsuarioHeaderRol != null) lblUsuarioHeaderRol.setText(rol);

        String fechaHoy = formatearFechaLarga(LocalDate.now());
        if (lblSubtituloHeaderCaja != null) lblSubtituloHeaderCaja.setText(fechaHoy);
        if (lblFechaHeaderDerecha != null) lblFechaHeaderDerecha.setText(fechaHoy);
    }

    private void configurarTabla() {
        if (tblHistorialCaja == null) return;
        colIdCaja.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getIdCaja()));
        colApertura.setCellValueFactory(data -> {
            if (data.getValue().getFechaApertura() != null) {
                return new SimpleStringProperty(formatoFechaTabla.format(data.getValue().getFechaApertura()));
            }
            return new SimpleStringProperty("-");
        });
        colCierre.setCellValueFactory(data -> {
            if (data.getValue().getFechaCierre() != null) {
                return new SimpleStringProperty(formatoFechaTabla.format(data.getValue().getFechaCierre()));
            }
            return new SimpleStringProperty("ABIERTA ACTUALMENTE");
        });
        colBase.setCellValueFactory(data -> new SimpleStringProperty("$ " + formatearMiles(data.getValue().getMontoInicial())));
        colTirilla.setCellValueFactory(data -> new SimpleStringProperty("$ " + formatearMiles(data.getValue().getTotalTirilla())));
        colContado.setCellValueFactory(data -> new SimpleStringProperty("$ " + formatearMiles(data.getValue().getEfectivoContado())));
        colDiferencia.setCellValueFactory(data -> new SimpleStringProperty("$ " + formatearMiles(data.getValue().getDiferencia())));

        colDiferencia.setCellFactory(column -> new TableCell<Caja, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Caja c = getTableView().getItems().get(getIndex());
                    if (c.getDiferencia() != null) {
                        if (c.getDiferencia().compareTo(BigDecimal.ZERO) < 0) {
                            setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
                        } else if (c.getDiferencia().compareTo(BigDecimal.ZERO) > 0) {
                            setStyle("-fx-text-fill: #704214; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #15803d; -fx-font-weight: bold;");
                        }
                    }
                }
            }
        });

        tblHistorialCaja.setItems(listaHistorial);
    }

    @FXML
    public void handleRefrescarCaja() {
        try {
            cajaActiva = cajaDAO.obtenerCajaAbierta();
            SesionUsuario.setCajaActiva(cajaActiva);

            List<Caja> historial = cajaDAO.listarHistorial();
            listaHistorial.setAll(historial);

            actualizarReferenciaCierreAnterior(historial);

            if (cajaActiva == null) {
                // ESTADO B: Caja cerrada -> Mostrar formulario de apertura
                lblEstadoTurno.setText("CERRADA");
                lblFechaApertura.setText("Fecha Apertura: No iniciada");
                lblMontoBase.setText("$ 0");
                mostrarVistaUnica(panelApertura, "Abrir caja", false);
                Platform.runLater(() -> {
                    if (txtMontoInicial != null) txtMontoInicial.requestFocus();
                });
            } else {
                LocalDate fechaTurno = cajaActiva.getFechaApertura() != null
                        ? cajaActiva.getFechaApertura().toLocalDate()
                        : LocalDate.now();

                if (fechaTurno.isBefore(LocalDate.now())) {
                    // ESTADO A: Bloqueo por turno de un dia anterior sin cerrar
                    configurarVistaBloqueoAnterior(cajaActiva);
                    mostrarVistaUnica(paneBloqueoAnterior, "Abrir caja", false);
                } else if (modoEntradaSoloApertura) {
                    // ESTADO C: Entraron por "Abrir caja" pero ya esta abierta hoy
                    modoEntradaSoloApertura = false;
                    configurarVistaYaAbierta(cajaActiva);
                    mostrarVistaUnica(paneYaAbierta, "Abrir caja", false);
                } else {
                    // ESTADO D: Vista Operativa de Arqueo y Cierre del dia
                    cargarDatosOperativosArqueo(cajaActiva);
                    mostrarVistaUnica(panelCierre, "Caja y cierre del día", true);
                }
            }

        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Consultar Caja", "Detalle: " + e.getMessage());
        }
    }

    private void ocultarTodasLasVistas() {
        VBox[] paneles = new VBox[]{
            paneBloqueoAnterior, panelApertura, paneExitoApertura,
            paneYaAbierta, panelCierre, paneExitoCierre
        };
        for (VBox p : paneles) {
            if (p != null) {
                p.setVisible(false);
                p.setManaged(false);
            }
        }
    }

    private void mostrarVistaUnica(VBox vistaActiva, String tituloHeader, boolean mostrarFechaDerecha) {
        ocultarTodasLasVistas();
        if (vistaActiva != null) {
            vistaActiva.setVisible(true);
            vistaActiva.setManaged(true);
        }
        if (lblTituloHeaderCaja != null) {
            lblTituloHeaderCaja.setText(tituloHeader);
        }
        if (lblSubtituloHeaderCaja != null) {
            lblSubtituloHeaderCaja.setVisible(!mostrarFechaDerecha);
            lblSubtituloHeaderCaja.setManaged(!mostrarFechaDerecha);
        }
        if (boxFechaHeaderDerecha != null) {
            boxFechaHeaderDerecha.setVisible(mostrarFechaDerecha);
            boxFechaHeaderDerecha.setManaged(mostrarFechaDerecha);
        }
    }

    private void actualizarReferenciaCierreAnterior(List<Caja> historial) {
        if (lblReferenciaCierreAnterior == null) return;
        Caja ultimoCerrado = null;
        if (historial != null) {
            for (Caja c : historial) {
                if (c.getFechaCierre() != null) {
                    ultimoCerrado = c;
                    break;
                }
            }
        }
        if (ultimoCerrado != null) {
            lblReferenciaCierreAnterior.setText("El turno anterior se cerró con $ "
                    + formatearMiles(ultimoCerrado.getEfectivoContado()) + " contados.");
        } else {
            lblReferenciaCierreAnterior.setText("Primer turno de caja registrado en el sistema.");
        }
    }

    // =========================================================================
    // ESTADO A: Bloqueo por turno anterior sin cerrar
    // =========================================================================
    private void configurarVistaBloqueoAnterior(Caja pendiente) {
        if (lblTurnoPendienteInfo != null && pendiente.getFechaApertura() != null) {
            String fechaStr = formatearFechaLarga(pendiente.getFechaApertura().toLocalDate());
            String horaStr = formatearHora(pendiente.getFechaApertura());
            lblTurnoPendienteInfo.setText("⏱ Turno pendiente: " + fechaStr + " • Abierto a las " + horaStr
                    + " (Base: $ " + formatearMiles(pendiente.getMontoInicial()) + ").");
        }
        boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
        aplicarModoRolBloqueo(esAdmin);
    }

    private void aplicarModoRolBloqueo(boolean comoAdmin) {
        if (btnCerrarCajaAyer != null) {
            btnCerrarCajaAyer.setVisible(comoAdmin);
            btnCerrarCajaAyer.setManaged(comoAdmin);
        }
        if (lblPermisoAdminAyer != null) {
            if (comoAdmin) {
                lblPermisoAdminAyer.setText("✓ Como usted ha iniciado sesión como Doña Rosita (Administradora), puede realizar el cierre de inmediato aquí debajo.");
            } else {
                lblPermisoAdminAyer.setText("🔒 Solicite a Doña Rosita (Administradora) que ingrese con su clave para cerrar el turno pendiente.");
            }
        }
        if (btnSimularAdmin != null && btnSimularVendedora != null) {
            if (comoAdmin) {
                btnSimularAdmin.setStyle("-fx-background-color: #5C330F; -fx-text-fill: #FAF6F0; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 4px 10px; -fx-cursor: hand;");
                btnSimularVendedora.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #46220F; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-padding: 4px 10px; -fx-cursor: hand;");
            } else {
                btnSimularAdmin.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #46220F; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-padding: 4px 10px; -fx-cursor: hand;");
                btnSimularVendedora.setStyle("-fx-background-color: #5C330F; -fx-text-fill: #FAF6F0; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 4px 10px; -fx-cursor: hand;");
            }
        }
    }

    @FXML
    public void handleSimularVistaAdmin() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Permiso de Administradora Requerido",
                    "Actualmente está en la sesión de Gladys (Vendedora). Solo Doña Rosita puede cerrar un turno atrasado.");
            return;
        }
        aplicarModoRolBloqueo(true);
    }

    @FXML
    public void handleSimularVistaVendedora() {
        aplicarModoRolBloqueo(false);
    }

    @FXML
    public void handleIrACerrarCajaAnterior() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede realizar el arqueo de un turno de un día anterior.");
            return;
        }
        if (cajaActiva != null) {
            cargarDatosOperativosArqueo(cajaActiva);
            mostrarVistaUnica(panelCierre, "Caja y cierre del día", true);
        }
    }

    // =========================================================================
    // ESTADO B: Apertura de Turno + Confirmacion "¡Caja abierta!"
    // =========================================================================
    @FXML
    public void handleLimpiarMontoInicial() {
        if (txtMontoInicial != null) {
            txtMontoInicial.clear();
            txtMontoInicial.requestFocus();
        }
    }

    @FXML
    public void handleAbrirCaja() {
        String baseTexto = txtMontoInicial.getText() != null ? txtMontoInicial.getText().trim() : "";
        BigDecimal base = parsearMoneda(baseTexto);
        if (base == null || base.compareTo(BigDecimal.ZERO) < 0) {
            AlertaUtil.mostrarAdvertencia("Monto Inválido",
                    "Por favor escriba un monto inicial en efectivo válido (ej. 50000).");
            if (txtMontoInicial != null) txtMontoInicial.requestFocus();
            return;
        }

        try {
            Caja abierta = cajaDAO.abrirCaja(base);
            if (abierta != null) {
                cajaActiva = abierta;
                SesionUsuario.setCajaActiva(abierta);
                txtMontoInicial.clear();

                // Configurar pantalla de confirmacion "¡Caja abierta!" (Mockup 4)
                if (lblExitoBaseInicial != null) {
                    lblExitoBaseInicial.setText("$ " + formatearMiles(abierta.getMontoInicial()));
                }
                if (lblExitoHoraApertura != null) {
                    lblExitoHoraApertura.setText("• " + formatearHora(abierta.getFechaApertura()));
                }
                if (lblExitoCajera != null) {
                    Usuario u = SesionUsuario.getUsuarioLogueado();
                    lblExitoCajera.setText(u != null ? u.getNombre() : "Doña Rosita");
                }
                if (lblExitoCodigoTurno != null) {
                    lblExitoCodigoTurno.setText(String.format("TRN-%d-%04d",
                            LocalDate.now().getYear(), abierta.getIdCaja()));
                }

                mostrarVistaUnica(paneExitoApertura, "Abrir caja", false);
            }
        } catch (Exception e) {
            AlertaUtil.mostrarError("Error al Abrir Caja", e.getMessage());
        }
    }

    // =========================================================================
    // ESTADO C: "La caja de hoy ya fue abierta"
    // =========================================================================
    private void configurarVistaYaAbierta(Caja abierta) {
        String horaStr = formatearHora(abierta.getFechaApertura());
        String baseStr = formatearMiles(abierta.getMontoInicial());
        if (lblYaAbiertaResumen != null) {
            lblYaAbiertaResumen.setText("La caja ya está abierta desde las " + horaStr
                    + ", con base de $ " + baseStr + ".");
        }
        if (lblYaAbiertaPor != null) {
            Usuario u = SesionUsuario.getUsuarioLogueado();
            String nombre = (u != null && u.getNombre() != null) ? u.getNombre() : "Doña Rosita";
            lblYaAbiertaPor.setText("   👤 Abierta por: " + nombre + " (Turno #" + abierta.getIdCaja() + ")");
        }
    }

    @FXML
    public void handleIrAArqueo() {
        if (cajaActiva != null) {
            cargarDatosOperativosArqueo(cajaActiva);
            mostrarVistaUnica(panelCierre, "Caja y cierre del día", true);
        }
    }

    // =========================================================================
    // ESTADO D: Vista Operativa de Arqueo y Cierre
    // =========================================================================
    private static class ItemMovimientoTurno {
        final LocalDateTime fecha;
        final String etiqueta;
        final String montoFormateado;
        final boolean esNegativo;

        ItemMovimientoTurno(LocalDateTime fecha, String etiqueta, String montoFormateado, boolean esNegativo) {
            this.fecha = fecha != null ? fecha : LocalDateTime.now();
            this.etiqueta = etiqueta;
            this.montoFormateado = montoFormateado;
            this.esNegativo = esNegativo;
        }
    }

    private void cargarDatosOperativosArqueo(Caja activa) {
        try {
            int idCaja = activa.getIdCaja();
            BigDecimal base = activa.getMontoInicial() != null ? activa.getMontoInicial() : BigDecimal.ZERO;

            lblEstadoTurno.setText("ABIERTA (Turno #" + idCaja + ")");
            if (activa.getFechaApertura() != null) {
                lblFechaApertura.setText("Fecha Apertura: " + formatoFechaTabla.format(activa.getFechaApertura()));
            }
            lblMontoBase.setText("$ " + formatearMiles(base));

            if (lblBannerEstadoAbierta != null) {
                lblBannerEstadoAbierta.setText("Estado actual: CAJA ABIERTA • Apertura registrada a las "
                        + formatearHora(activa.getFechaApertura())
                        + " con fondo base inicial de $ " + formatearMiles(base));
            }

            // 1. Calcular Ventas de Contado y Ventas a Fiado del turno activo
            int numVentasContado = 0;
            BigDecimal sumaVentasContado = BigDecimal.ZERO;
            BigDecimal sumaVentasFiado = BigDecimal.ZERO;
            List<ItemMovimientoTurno> itemsMovimientos = new ArrayList<>();

            DateTimeFormatter fmtHoraCorta = DateTimeFormatter.ofPattern("HH:mm");

            List<Venta> ventasRecientes = ventaDAO.listarVentas(
                    activa.getFechaApertura() != null ? activa.getFechaApertura().toLocalDate().minusDays(1) : LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(1),
                    null
            );
            if (ventasRecientes != null) {
                for (Venta v : ventasRecientes) {
                    if (v.getIdCaja() != null && v.getIdCaja() == idCaja && !v.isAnulada()) {
                        BigDecimal totalV = v.getTotalVenta() != null ? v.getTotalVenta() : BigDecimal.ZERO;
                        if (v.isEsFiado()) {
                            sumaVentasFiado = sumaVentasFiado.add(totalV);
                        } else {
                            numVentasContado++;
                            sumaVentasContado = sumaVentasContado.add(totalV);
                            String horaM = v.getFecha() != null ? v.getFecha().format(fmtHoraCorta) : "--:--";
                            itemsMovimientos.add(new ItemMovimientoTurno(
                                    v.getFecha(),
                                    "Venta • " + horaM,
                                    "+ $ " + formatearMiles(totalV),
                                    false
                            ));
                        }
                    }
                }
            }

            // 2. Calcular Abonos de Clientes recibidos en este turno
            BigDecimal sumaAbonos = BigDecimal.ZERO;
            List<Cliente> todosClientes = clienteDAO.listarTodos(false);
            if (todosClientes != null) {
                for (Cliente cl : todosClientes) {
                    List<AbonoCliente> abonosCl = abonoClienteDAO.listarAbonosPorCliente(cl.getIdCliente());
                    if (abonosCl != null) {
                        for (AbonoCliente ab : abonosCl) {
                            if (ab.getIdCaja() != null && ab.getIdCaja() == idCaja) {
                                BigDecimal m = ab.getMonto() != null ? ab.getMonto() : BigDecimal.ZERO;
                                sumaAbonos = sumaAbonos.add(m);
                                String horaM = ab.getFecha() != null ? ab.getFecha().format(fmtHoraCorta) : "--:--";
                                itemsMovimientos.add(new ItemMovimientoTurno(
                                        ab.getFecha(),
                                        "Abono de " + cl.getNombre() + " • " + horaM,
                                        "+ $ " + formatearMiles(m),
                                        false
                                ));
                            }
                        }
                    }
                }
            }

            // 3. Calcular Pagos a Proveedores en efectivo en este turno
            BigDecimal sumaPagos = BigDecimal.ZERO;
            List<Proveedor> todosProveedores = proveedorDAO.listarTodos(false);
            if (todosProveedores != null) {
                for (Proveedor pr : todosProveedores) {
                    List<PagoProveedor> pagosPr = pagoProveedorDAO.listarPagosPorProveedor(pr.getIdProveedor());
                    if (pagosPr != null) {
                        for (PagoProveedor pp : pagosPr) {
                            if (pp.getIdCaja() != null && pp.getIdCaja() == idCaja) {
                                BigDecimal m = pp.getMonto() != null ? pp.getMonto() : BigDecimal.ZERO;
                                sumaPagos = sumaPagos.add(m);
                                String horaM = pp.getFecha() != null ? pp.getFecha().format(fmtHoraCorta) : "--:--";
                                itemsMovimientos.add(new ItemMovimientoTurno(
                                        pp.getFecha(),
                                        "Pago a proveedor (" + pr.getNombre() + ") • " + horaM,
                                        "– $ " + formatearMiles(m),
                                        true
                                ));
                            }
                        }
                    }
                }
            }

            totalVentasContadoDia = sumaVentasContado;
            totalTirillaCalculado = cajaDAO.calcularTotalTirilla(idCaja);

            // Actualizar etiquetas de la Columna Izquierda
            if (lblTituloVentasContado != null) {
                lblTituloVentasContado.setText("🛒  (+) Ventas de contado del día • "
                        + numVentasContado + (numVentasContado == 1 ? " venta:" : " ventas:"));
            }
            if (lblValorVentasContado != null) {
                lblValorVentasContado.setText("+ $ " + formatearMiles(sumaVentasContado));
            }
            if (lblValorAbonosRecibidos != null) {
                lblValorAbonosRecibidos.setText("+ $ " + formatearMiles(sumaAbonos));
            }
            if (lblValorPagosProveedores != null) {
                lblValorPagosProveedores.setText("– $ " + formatearMiles(sumaPagos));
            }
            if (lblTotalTirilla != null) {
                lblTotalTirilla.setText("$ " + formatearMiles(totalTirillaCalculado));
            }
            if (lblValorVentasFiado != null) {
                lblValorVentasFiado.setText("ⓘ  Ventas a fiado (no entran a la caja): $ " + formatearMiles(sumaVentasFiado));
            }

            // Poblar lista de Movimientos de hoy (maximo 4 mas recientes)
            poblarListaMovimientosHoy(itemsMovimientos);

            // Prellenar tirilla de caja registradora y efectivo contado sugerido
            if (txtTotalTirillaInput != null) {
                txtTotalTirillaInput.setText("$ " + formatearMiles(sumaVentasContado));
            }
            if (txtEfectivoContado != null && (txtEfectivoContado.getText() == null || txtEfectivoContado.getText().trim().isEmpty())) {
                txtEfectivoContado.setText(formatearMiles(totalTirillaCalculado));
            }
            recalcularDiferenciaEnVivo();

        } catch (SQLException e) {
            System.err.println("[CajaController] Error al cargar desglose de arqueo: " + e.getMessage());
        }
    }

    private void poblarListaMovimientosHoy(List<ItemMovimientoTurno> items) {
        if (boxListaMovimientosHoy == null) return;
        boxListaMovimientosHoy.getChildren().clear();

        if (items == null || items.isEmpty()) {
            Label vacio = new Label("Aún no se han registrado movimientos de efectivo en este turno.");
            vacio.setStyle("-fx-font-size: 12px; -fx-text-fill: #7A5C43; -fx-padding: 6px 4px;");
            boxListaMovimientosHoy.getChildren().add(vacio);
            return;
        }

        Collections.sort(items, new Comparator<ItemMovimientoTurno>() {
            @Override
            public int compare(ItemMovimientoTurno a, ItemMovimientoTurno b) {
                return b.fecha.compareTo(a.fecha);
            }
        });

        int limite = Math.min(3, items.size());
        for (int i = 0; i < limite; i++) {
            ItemMovimientoTurno item = items.get(i);
            HBox fila = new HBox();
            fila.setAlignment(Pos.CENTER_LEFT);
            fila.getStyleClass().add("caja-movimiento-item");

            Label lblDesc = new Label(item.etiqueta);
            lblDesc.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #46220F;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblMonto = new Label(item.montoFormateado);
            lblMonto.setStyle(item.esNegativo
                    ? "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #9E2A2B;"
                    : "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #46220F;");

            fila.getChildren().addAll(lblDesc, spacer, lblMonto);
            boxListaMovimientosHoy.getChildren().add(fila);
        }
    }

    private void recalcularDiferenciaEnVivo() {
        if (cajaActiva == null || lblDiferencia == null) return;

        String textoContado = txtEfectivoContado.getText() != null ? txtEfectivoContado.getText().trim() : "";
        BigDecimal contado = parsearMoneda(textoContado);

        if (contado == null) {
            if (boxCuadreDiferencia != null) {
                boxCuadreDiferencia.getStyleClass().removeAll("caja-diff-box-ok");
                if (!boxCuadreDiferencia.getStyleClass().contains("caja-diff-box-warn")) {
                    boxCuadreDiferencia.getStyleClass().add("caja-diff-box-warn");
                }
            }
            lblDiferencia.setText("⚠ Escriba el efectivo contado en el cajón (Esperado: $ "
                    + formatearMiles(totalTirillaCalculado) + ")");
            lblDiferencia.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #704214;");
            return;
        }

        BigDecimal dif = contado.subtract(totalTirillaCalculado);

        if (dif.compareTo(BigDecimal.ZERO) == 0) {
            if (boxCuadreDiferencia != null) {
                boxCuadreDiferencia.getStyleClass().removeAll("caja-diff-box-warn");
                if (!boxCuadreDiferencia.getStyleClass().contains("caja-diff-box-ok")) {
                    boxCuadreDiferencia.getStyleClass().add("caja-diff-box-ok");
                }
            }
            lblDiferencia.setText("✓ La caja cuadra. Esperado $ " + formatearMiles(totalTirillaCalculado)
                    + ", contado $ " + formatearMiles(contado));
            lblDiferencia.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #1E6B38;");
            if (lblSubEstadoTirilla != null) {
                lblSubEstadoTirilla.setText("✓ La tirilla coincide con el sistema");
                lblSubEstadoTirilla.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #1E6B38;");
            }
        } else if (dif.compareTo(BigDecimal.ZERO) < 0) {
            if (boxCuadreDiferencia != null) {
                boxCuadreDiferencia.getStyleClass().removeAll("caja-diff-box-ok");
                if (!boxCuadreDiferencia.getStyleClass().contains("caja-diff-box-warn")) {
                    boxCuadreDiferencia.getStyleClass().add("caja-diff-box-warn");
                }
            }
            BigDecimal faltante = dif.abs();
            lblDiferencia.setText("⚠ Faltan $ " + formatearMiles(faltante)
                    + " • Esperado $ " + formatearMiles(totalTirillaCalculado)
                    + ", contado $ " + formatearMiles(contado));
            lblDiferencia.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #704214;");
            if (lblSubEstadoTirilla != null) {
                lblSubEstadoTirilla.setText("✓ La tirilla coincide con el sistema ($ " + formatearMiles(totalVentasContadoDia) + ")");
                lblSubEstadoTirilla.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #46220F;");
            }
        } else {
            if (boxCuadreDiferencia != null) {
                boxCuadreDiferencia.getStyleClass().removeAll("caja-diff-box-ok");
                if (!boxCuadreDiferencia.getStyleClass().contains("caja-diff-box-warn")) {
                    boxCuadreDiferencia.getStyleClass().add("caja-diff-box-warn");
                }
            }
            lblDiferencia.setText("⚠ Sobran $ " + formatearMiles(dif)
                    + " • Esperado $ " + formatearMiles(totalTirillaCalculado)
                    + ", contado $ " + formatearMiles(contado));
            lblDiferencia.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #704214;");
            if (lblSubEstadoTirilla != null) {
                lblSubEstadoTirilla.setText("✓ La tirilla coincide con el sistema ($ " + formatearMiles(totalVentasContadoDia) + ")");
                lblSubEstadoTirilla.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #46220F;");
            }
        }
    }

    @FXML
    public void handleCerrarCaja() {
        if (cajaActiva == null) return;

        String contadoTexto = txtEfectivoContado.getText() != null ? txtEfectivoContado.getText().trim() : "";
        BigDecimal contado = parsearMoneda(contadoTexto);
        if (contado == null || contado.compareTo(BigDecimal.ZERO) < 0) {
            AlertaUtil.mostrarAdvertencia("Efectivo Contado Requerido",
                    "Por favor escriba el efectivo físico que contó en el cajón antes de cerrar la caja.");
            if (txtEfectivoContado != null) txtEfectivoContado.requestFocus();
            return;
        }

        efectivoContadoPendienteCierre = contado;
        if (lblModalEsperado != null) {
            lblModalEsperado.setText("$ " + formatearMiles(totalTirillaCalculado));
        }
        if (lblModalContado != null) {
            lblModalContado.setText("$ " + formatearMiles(contado));
        }
        if (modalConfirmarCierre != null) {
            modalConfirmarCierre.setVisible(true);
            modalConfirmarCierre.setManaged(true);
        }
    }

    @FXML
    public void handleCancelarModalCierre() {
        if (modalConfirmarCierre != null) {
            modalConfirmarCierre.setVisible(false);
            modalConfirmarCierre.setManaged(false);
        }
    }

    @FXML
    public void handleConfirmarCierreDefinitivo() {
        if (cajaActiva == null) return;
        handleCancelarModalCierre();

        try {
            int idCerrada = cajaActiva.getIdCaja();
            BigDecimal esperadoFinal = totalTirillaCalculado;
            BigDecimal contadoFinal = efectivoContadoPendienteCierre;
            BigDecimal difFinal = contadoFinal.subtract(esperadoFinal);

            boolean exito = cajaDAO.cerrarCaja(idCerrada, contadoFinal);
            if (exito) {
                cajaActiva = null;
                SesionUsuario.setCajaActiva(null);
                txtEfectivoContado.clear();

                // Configurar pantalla de resumen "¡Caja cerrada!" (Mockup 8)
                if (lblResumenCajaNumero != null) {
                    lblResumenCajaNumero.setText("Caja N° " + idCerrada);
                }
                if (lblResumenEsperadoFinal != null) {
                    lblResumenEsperadoFinal.setText("$ " + formatearMiles(esperadoFinal));
                }
                if (lblResumenContadoFinal != null) {
                    lblResumenContadoFinal.setText("$ " + formatearMiles(contadoFinal));
                }
                if (lblResumenDiferenciaBadge != null) {
                    if (difFinal.compareTo(BigDecimal.ZERO) == 0) {
                        lblResumenDiferenciaBadge.setText("✓ Cuadra exacto ($ 0)");
                    } else if (difFinal.compareTo(BigDecimal.ZERO) < 0) {
                        lblResumenDiferenciaBadge.setText("⚠ Faltan $ " + formatearMiles(difFinal.abs()));
                    } else {
                        lblResumenDiferenciaBadge.setText("⚠ Sobran $ " + formatearMiles(difFinal));
                    }
                }
                if (lblResumenFechaUsuario != null) {
                    Usuario u = SesionUsuario.getUsuarioLogueado();
                    String nombre = u != null ? u.getNombre() : "Doña Rosita";
                    lblResumenFechaUsuario.setText("🕒 " + formatearFechaLarga(LocalDate.now())
                            + " • " + formatearHora(LocalDateTime.now())
                            + " • Turno: " + nombre);
                }

                mostrarVistaUnica(paneExitoCierre, "Caja y cierre del día", false);
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Cerrar Caja", e.getMessage());
        }
    }

    // =========================================================================
    // Modales de Historial y Navegacion
    // =========================================================================
    @FXML
    public void handleVerHistorialModal() {
        try {
            listaHistorial.setAll(cajaDAO.listarHistorial());
        } catch (SQLException e) {
            // Conservar lista previa si falla
        }
        if (modalHistorialTurnos != null) {
            modalHistorialTurnos.setVisible(true);
            modalHistorialTurnos.setManaged(true);
        }
    }

    @FXML
    public void handleCerrarHistorialModal() {
        if (modalHistorialTurnos != null) {
            modalHistorialTurnos.setVisible(false);
            modalHistorialTurnos.setManaged(false);
        }
    }

    @FXML
    public void handleVolverMenu() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarMenuDashboard();
        } else {
            App.cambiarEscena("/fxml/menu_principal.fxml", "Papelería de Rosita - Sistema CONARY", 1180, 780);
        }
    }

    @FXML
    public void handleIrAVender() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarPuntoVenta();
        } else {
            App.cambiarEscena("/fxml/menu_principal.fxml", "Papelería de Rosita - Sistema CONARY", 1180, 780);
        }
    }

    // =========================================================================
    // Utilidades de Formato (Java 8)
    // =========================================================================
    private BigDecimal parsearMoneda(String raw) {
        if (raw == null) return null;
        String limpio = raw.replace("$", "").replace(" ", "").replace(".", "").replace(",", ".").trim();
        if (limpio.isEmpty()) return null;
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatearMiles(BigDecimal valor) {
        BigDecimal v = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(v);
    }

    private String formatearFechaLarga(LocalDate fecha) {
        LocalDate f = fecha != null ? fecha : LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", LOCALE_ES_CO);
        String s = f.format(fmt);
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String formatearHora(LocalDateTime fechaHora) {
        LocalDateTime fh = fechaHora != null ? fechaHora : LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
        return fh.format(fmt).toLowerCase(LOCALE_ES_CO).replace("am", "a. m.").replace("pm", "p. m.");
    }
}
