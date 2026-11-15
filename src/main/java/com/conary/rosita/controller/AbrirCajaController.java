package com.conary.rosita.controller;

import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controlador del Bloque A del Modulo 2: Apertura de Turno y Validaciones de Seguridad.
 * Gestiona los 4 estados dinamicos en Java SE 8 y JavaFX 8 nativo:
 * 1. Bloqueo por caja anterior pendiente sin cerrar (con control por rol Admin vs Vendedora).
 * 2. Turno de hoy ya abierto ("La caja de hoy ya fue abierta").
 * 3. Formulario de apertura de turno ("¿Con cuánto dinero empieza la caja hoy?").
 * 4. Confirmacion de apertura exitosa ("¡Caja abierta!").
 */
public class AbrirCajaController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverHeader;
    @FXML private Label lblTituloHeader;
    @FXML private Label lblFechaHeader;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;

    // Estado 1: Bloqueo por caja anterior sin cerrar
    @FXML private VBox panelCajaAnteriorPendiente;
    @FXML private Label lblTurnoPendienteDetalle;
    @FXML private Label lblIconoPermisoRol;
    @FXML private Label lblMensajePermisoRol;
    @FXML private Button btnModoAdmin;
    @FXML private Button btnModoVendedora;
    @FXML private Button btnCerrarCajaAyer;

    // Estado 2: La caja de hoy ya fue abierta
    @FXML private VBox panelCajaYaAbierta;
    @FXML private Label lblResumenCajaAbierta;
    @FXML private Label lblAbiertaPor;
    @FXML private Button btnIrAVenderYaAbierta;

    // Estado 3: Formulario de Apertura de Turno
    @FXML private VBox panelFormularioApertura;
    @FXML private TextField txtMontoInicial;
    @FXML private Button btnBorrarMonto;
    @FXML private Label lblErrorMonto;
    @FXML private Label lblUltimoCierreInfo;
    @FXML private Button btnConfirmarApertura;

    // Estado 4: Confirmacion de Apertura Exitosa
    @FXML private VBox panelAperturaExitosa;
    @FXML private Label lblExitoMontoBase;
    @FXML private Label lblExitoHora;
    @FXML private Label lblExitoCajera;
    @FXML private Label lblExitoTurnoCodigo;
    @FXML private Button btnIrAVenderExito;

    private final CajaDAO cajaDAO = new CajaDAO();
    private Caja cajaActivaActual;
    private static String ultimoUsuarioAperturaNombre = null;

    @FXML
    public void initialize() {
        configurarEncabezado();
        configurarAtajosTeclado();
        evaluarEstadoCajaEnBaseDeDatos();
    }

    private void configurarEncabezado() {
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
        if (lblFechaHeader != null) {
            lblFechaHeader.setText(formatearFechaLarga(LocalDate.now()));
        }
    }

    private void configurarAtajosTeclado() {
        if (txtMontoInicial != null) {
            txtMontoInicial.textProperty().addListener((obs, oldVal, newVal) -> {
                if (lblErrorMonto != null && lblErrorMonto.isVisible()) {
                    lblErrorMonto.setVisible(false);
                    lblErrorMonto.setManaged(false);
                }
            });

            txtMontoInicial.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    handleConfirmarApertura();
                }
            });
        }

        if (rootPane != null) {
            rootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    if ((panelCajaYaAbierta != null && panelCajaYaAbierta.isVisible())
                            || (panelAperturaExitosa != null && panelAperturaExitosa.isVisible())) {
                        event.consume();
                        handleIrAVender();
                    }
                }
            });
        }
    }

    public void evaluarEstadoCajaEnBaseDeDatos() {
        try {
            cajaActivaActual = cajaDAO.obtenerCajaAbierta();
            SesionUsuario.setCajaActiva(cajaActivaActual);

            List<Caja> historial = cajaDAO.listarHistorial();
            actualizarReferenciaUltimoCierre(historial);

            if (cajaActivaActual == null) {
                // Estado 3: Caja cerrada y sin turnos anteriores pendientes
                mostrarUnicoPanel(panelFormularioApertura);
                Platform.runLater(() -> {
                    if (txtMontoInicial != null) {
                        txtMontoInicial.requestFocus();
                    }
                });
            } else {
                LocalDate fechaApertura = cajaActivaActual.getFechaApertura() != null
                        ? cajaActivaActual.getFechaApertura().toLocalDate()
                        : LocalDate.now();

                if (fechaApertura.isBefore(LocalDate.now())) {
                    // Estado 1: Bloqueo por caja anterior sin cerrar
                    configurarEstadoBloqueoAnterior(cajaActivaActual);
                    mostrarUnicoPanel(panelCajaAnteriorPendiente);
                } else {
                    // Estado 2: La caja de hoy ya fue abierta
                    configurarEstadoCajaYaAbierta(cajaActivaActual);
                    mostrarUnicoPanel(panelCajaYaAbierta);
                    Platform.runLater(() -> {
                        if (btnIrAVenderYaAbierta != null) {
                            btnIrAVenderYaAbierta.requestFocus();
                        }
                    });
                }
            }
        } catch (SQLException e) {
            System.err.println("[AbrirCajaController] Error al consultar caja en MySQL: " + e.getMessage());
            mostrarUnicoPanel(panelFormularioApertura);
            if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
                AlertaUtil.mostrarError("Error de Base de Datos",
                        "No se pudo consultar el estado de la caja en MySQL: " + e.getMessage());
            }
        }
    }

    private void mostrarUnicoPanel(VBox panelActivo) {
        VBox[] todos = new VBox[]{
                panelCajaAnteriorPendiente,
                panelCajaYaAbierta,
                panelFormularioApertura,
                panelAperturaExitosa
        };
        for (VBox panel : todos) {
            if (panel != null) {
                boolean activo = panel == panelActivo;
                panel.setVisible(activo);
                panel.setManaged(activo);
            }
        }
    }

    private void actualizarReferenciaUltimoCierre(List<Caja> historial) {
        if (lblUltimoCierreInfo == null) {
            return;
        }
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
            String montoContado = formatearMoneda(ultimoCerrado.getEfectivoContado());
            lblUltimoCierreInfo.setText("Ayer la caja se cerró con $ " + montoContado + " contados.");
        } else {
            lblUltimoCierreInfo.setText("Primer turno de caja registrado en el sistema.");
        }
    }

    // =========================================================================
    // ESTADO 1: BLOQUEO POR CAJA ANTERIOR PENDIENTE
    // =========================================================================
    private void configurarEstadoBloqueoAnterior(Caja turnoPendiente) {
        if (lblTurnoPendienteDetalle != null && turnoPendiente.getFechaApertura() != null) {
            String fechaTexto = formatearFechaDiaMes(turnoPendiente.getFechaApertura().toLocalDate());
            String horaTexto = formatearHora(turnoPendiente.getFechaApertura());
            lblTurnoPendienteDetalle.setText("Turno pendiente: " + fechaTexto
                    + " · Registrado por última vez a las " + horaTexto + ".");
        }
        boolean esAdminReal = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
        aplicarPermisosVisualesBloqueo(esAdminReal);
    }

    private void aplicarPermisosVisualesBloqueo(boolean mostrarComoAdmin) {
        if (btnCerrarCajaAyer != null) {
            btnCerrarCajaAyer.setVisible(mostrarComoAdmin);
            btnCerrarCajaAyer.setManaged(mostrarComoAdmin);
            btnCerrarCajaAyer.setDisable(!mostrarComoAdmin);
        }
        if (lblIconoPermisoRol != null) {
            lblIconoPermisoRol.setText(mostrarComoAdmin ? "✓" : "🔒");
        }
        if (lblMensajePermisoRol != null) {
            if (mostrarComoAdmin) {
                lblMensajePermisoRol.setText("Como usted ha iniciado sesión como Doña Rosita (Administradora), puede realizar el cierre de inmediato aquí debajo.");
            } else {
                lblMensajePermisoRol.setText("Usted ha iniciado sesión como Gladys (Vendedora). Pida a Doña Rosita que ingrese para completar el arqueo pendiente.");
            }
        }
        if (btnModoAdmin != null && btnModoVendedora != null) {
            btnModoAdmin.getStyleClass().removeAll("abrir-caja-role-btn-active", "abrir-caja-role-btn-inactive");
            btnModoVendedora.getStyleClass().removeAll("abrir-caja-role-btn-active", "abrir-caja-role-btn-inactive");
            btnModoAdmin.getStyleClass().add(mostrarComoAdmin ? "abrir-caja-role-btn-active" : "abrir-caja-role-btn-inactive");
            btnModoVendedora.getStyleClass().add(mostrarComoAdmin ? "abrir-caja-role-btn-inactive" : "abrir-caja-role-btn-active");
        }
    }

    @FXML
    public void handleVerComoAdmin() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Ha iniciado sesión como Vendedora (Gladys). Solo Doña Rosita puede activar el cierre de un turno atrasado.");
            return;
        }
        aplicarPermisosVisualesBloqueo(true);
    }

    @FXML
    public void handleVerComoVendedora() {
        aplicarPermisosVisualesBloqueo(false);
    }

    @FXML
    public void handleCerrarCajaAyer() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Permiso Requerido",
                    "Solo Doña Rosita (Administradora) puede cerrar la caja pendiente de un día anterior.");
            return;
        }
        CajaController.setForzarArqueoTurnoPendiente(true);
        CajaController.setModoEntradaSoloApertura(false);
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/caja.fxml");
        }
    }

    // =========================================================================
    // ESTADO 2: LA CAJA DE HOY YA FUE ABIERTA
    // =========================================================================
    private void configurarEstadoCajaYaAbierta(Caja turnoAbierto) {
        String horaStr = formatearHora(turnoAbierto.getFechaApertura());
        String montoStr = formatearMoneda(turnoAbierto.getMontoInicial());

        if (lblResumenCajaAbierta != null) {
            lblResumenCajaAbierta.setText("La caja ya está abierta desde las " + horaStr
                    + ", con base de $ " + montoStr + ".");
        }
        if (lblAbiertaPor != null) {
            String quienAbrio = ultimoUsuarioAperturaNombre != null
                    ? ultimoUsuarioAperturaNombre
                    : obtenerNombreUsuarioActual();
            int hora = turnoAbierto.getFechaApertura() != null ? turnoAbierto.getFechaApertura().getHour() : 8;
            String jornada = hora < 12 ? "Apertura matutina" : (hora < 18 ? "Jornada de tarde" : "Jornada nocturna");
            lblAbiertaPor.setText("     👤  Abierta por: " + quienAbrio + " (" + jornada + ")");
        }
    }

    // =========================================================================
    // ESTADO 3 Y 4: FORMULARIO DE APERTURA Y CONFIRMACION "¡CAJA ABIERTA!"
    // =========================================================================
    @FXML
    public void handleLimpiarMontoInicial() {
        if (txtMontoInicial != null) {
            txtMontoInicial.clear();
            txtMontoInicial.requestFocus();
        }
        if (lblErrorMonto != null) {
            lblErrorMonto.setVisible(false);
            lblErrorMonto.setManaged(false);
        }
    }

    @FXML
    public void handleConfirmarApertura() {
        String raw = txtMontoInicial != null && txtMontoInicial.getText() != null
                ? txtMontoInicial.getText().trim()
                : "";

        BigDecimal montoInicial = parsearMonto(raw);
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            if (lblErrorMonto != null) {
                lblErrorMonto.setText("⚠ Ingrese un monto inicial válido en números (mayor o igual a $ 0).");
                lblErrorMonto.setVisible(true);
                lblErrorMonto.setManaged(true);
            }
            if (txtMontoInicial != null) {
                txtMontoInicial.requestFocus();
            }
            return;
        }

        try {
            Caja nuevaCaja = cajaDAO.abrirCaja(montoInicial);
            if (nuevaCaja != null) {
                cajaActivaActual = nuevaCaja;
                SesionUsuario.setCajaActiva(nuevaCaja);
                String cajera = obtenerNombreUsuarioActual();
                ultimoUsuarioAperturaNombre = cajera;

                if (lblExitoMontoBase != null) {
                    lblExitoMontoBase.setText("$ " + formatearMoneda(nuevaCaja.getMontoInicial()));
                }
                if (lblExitoHora != null) {
                    lblExitoHora.setText("· " + formatearHora(nuevaCaja.getFechaApertura()));
                }
                if (lblExitoCajera != null) {
                    lblExitoCajera.setText(cajera);
                }
                if (lblExitoTurnoCodigo != null) {
                    int anio = nuevaCaja.getFechaApertura() != null
                            ? nuevaCaja.getFechaApertura().getYear()
                            : LocalDate.now().getYear();
                    lblExitoTurnoCodigo.setText(String.format("TRN-%d-%04d", Integer.valueOf(anio), Integer.valueOf(nuevaCaja.getIdCaja())));
                }

                mostrarUnicoPanel(panelAperturaExitosa);
                Platform.runLater(() -> {
                    if (btnIrAVenderExito != null) {
                        btnIrAVenderExito.requestFocus();
                    }
                });
            }
        } catch (IllegalStateException ex) {
            evaluarEstadoCajaEnBaseDeDatos();
        } catch (SQLException ex) {
            AlertaUtil.mostrarError("Error al Abrir Caja",
                    "No se pudo guardar la apertura de caja en MySQL: " + ex.getMessage());
        }
    }

    // =========================================================================
    // NAVEGACION
    // =========================================================================
    @FXML
    public void handleIrAVender() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarPuntoVenta();
        }
    }

    @FXML
    public void handleVolverMenu() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarMenuDashboard();
        }
    }

    // =========================================================================
    // UTILIDADES DE FORMATO Y PARSEO
    // =========================================================================
    private String obtenerNombreUsuarioActual() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        if (u == null || u.getNombre() == null || u.getNombre().trim().isEmpty()) {
            return "Doña Rosita";
        }
        String raw = u.getNombre().trim();
        if (raw.toLowerCase(LOCALE_ES_CO).contains("rosita")
                && !raw.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
            return "Doña " + raw;
        }
        return raw;
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

    private String formatearFechaDiaMes(LocalDate fecha) {
        LocalDate f = fecha != null ? fecha : LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", LOCALE_ES_CO);
        String txt = f.format(fmt);
        if (txt == null || txt.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(txt.charAt(0)) + txt.substring(1);
    }

    private String formatearHora(LocalDateTime fechaHora) {
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
