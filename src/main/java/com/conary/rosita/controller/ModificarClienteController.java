package com.conary.rosita.controller;

import com.conary.rosita.dao.ClienteDAO;
import com.conary.rosita.model.Cliente;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista A de la Tanda 2 del Modulo 3: Modificar datos del cliente.
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ModificarClienteController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static Cliente clientePendienteEdicion;

    public static void setClientePendienteEdicion(Cliente cliente) {
        clientePendienteEdicion = cliente;
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverClientes;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;

    @FXML private HBox boxAlertaDuplicado;
    @FXML private Label lblTextoDuplicado;

    @FXML private HBox boxInputNombre;
    @FXML private TextField txtNombreCliente;
    @FXML private Label lblAyudaNombre;

    @FXML private HBox boxInputTelefono;
    @FXML private TextField txtTelefonoCliente;
    @FXML private Label lblAyudaTelefono;

    @FXML private Label lblInfoFiadoActual;
    @FXML private Button btnDarBajaCliente;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardarCambios;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final List<Cliente> todosLosClientes = new ArrayList<Cliente>();
    private Cliente clienteActual;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        cargarCatalogoClientesParaValidacion();
        configurarListenersValidacion();

        if (clientePendienteEdicion != null) {
            setCliente(clientePendienteEdicion);
        }

        Platform.runLater(() -> {
            if (txtNombreCliente != null) {
                txtNombreCliente.requestFocus();
                txtNombreCliente.positionCaret(txtNombreCliente.getText() != null ? txtNombreCliente.getText().length() : 0);
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
    }

    private void cargarCatalogoClientesParaValidacion() {
        todosLosClientes.clear();
        try {
            List<Cliente> desdeBD = clienteDAO.listarTodos(false);
            if (desdeBD != null) {
                todosLosClientes.addAll(desdeBD);
            }
        } catch (SQLException e) {
            System.err.println("[ModificarClienteController] Error al cargar clientes: " + e.getMessage());
        }
    }

    private void configurarListenersValidacion() {
        if (txtNombreCliente != null) {
            txtNombreCliente.textProperty().addListener((obs, oldVal, newVal) -> {
                if (boxInputNombre != null) {
                    boxInputNombre.getStyleClass().removeAll("reg-cliente-input-box-error");
                }
                if (lblAyudaNombre != null) {
                    lblAyudaNombre.setText("❓  Como lo conoce usted");
                    lblAyudaNombre.getStyleClass().removeAll("reg-cliente-field-help-error");
                }
                verificarDuplicadoEnVivo();
            });

            txtNombreCliente.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER && txtTelefonoCliente != null) {
                    txtTelefonoCliente.requestFocus();
                }
            });
        }

        if (txtTelefonoCliente != null) {
            txtTelefonoCliente.textProperty().addListener((obs, oldVal, newVal) -> {
                if (boxInputTelefono != null) {
                    boxInputTelefono.getStyleClass().removeAll("reg-cliente-input-box-error");
                }
                if (lblAyudaTelefono != null) {
                    lblAyudaTelefono.setText("ⓘ  Para localizarla si debe dinero");
                    lblAyudaTelefono.getStyleClass().removeAll("reg-cliente-field-help-error");
                }
                verificarDuplicadoEnVivo();
            });

            txtTelefonoCliente.setOnKeyPressed(ev -> {
                if (ev.getCode() == KeyCode.ENTER) {
                    handleGuardarCambios();
                }
            });
        }
    }

    public void setCliente(Cliente cliente) {
        this.clienteActual = cliente;
        if (cliente == null) {
            return;
        }

        if (txtNombreCliente != null) {
            txtNombreCliente.setText(cliente.getNombre() != null ? cliente.getNombre() : "");
        }
        if (txtTelefonoCliente != null) {
            txtTelefonoCliente.setText(cliente.getTelefono() != null ? cliente.getTelefono() : "");
        }

        if (lblInfoFiadoActual != null) {
            if (cliente.isFiadoAutorizado()
                    && cliente.getLimiteCredito() != null
                    && cliente.getLimiteCredito().compareTo(BigDecimal.ZERO) > 0) {
                lblInfoFiadoActual.setText("Fiado: autorizado, cupo $ "
                        + formatearMoneda(cliente.getLimiteCredito())
                        + ". Para cambiarlo use Autorizar fiado.");
            } else {
                lblInfoFiadoActual.setText("Fiado: sin autorizar. Para habilitarlo use Autorizar fiado desde la lista.");
            }
        }

        if (btnDarBajaCliente != null) {
            boolean esAdmin = SesionUsuario.esAdministrador() || !SesionUsuario.haySesionActiva();
            btnDarBajaCliente.setVisible(esAdmin);
            btnDarBajaCliente.setManaged(esAdmin);
            if (!cliente.isActivo()) {
                btnDarBajaCliente.setText("↻   Volver a activar este cliente");
            } else {
                btnDarBajaCliente.setText("📦   Dar de baja a este cliente");
            }
        }

        if (boxAlertaDuplicado != null) {
            boxAlertaDuplicado.setVisible(false);
            boxAlertaDuplicado.setManaged(false);
        }
    }

    private Cliente buscarOtroClienteDuplicado(String nombreInput, String telefonoInput) {
        if (clienteActual == null) {
            return null;
        }
        String nomNorm = normalizarTexto(nombreInput);
        String telNorm = normalizarTelefono(telefonoInput);

        for (Cliente c : todosLosClientes) {
            if (c.getIdCliente() == clienteActual.getIdCliente()) {
                continue;
            }
            if (!nomNorm.isEmpty() && nomNorm.equals(normalizarTexto(c.getNombre()))) {
                return c;
            }
            if (!telNorm.isEmpty() && telNorm.length() >= 7 && telNorm.equals(normalizarTelefono(c.getTelefono()))) {
                return c;
            }
        }
        return null;
    }

    private void verificarDuplicadoEnVivo() {
        String nom = txtNombreCliente != null ? txtNombreCliente.getText() : "";
        String tel = txtTelefonoCliente != null ? txtTelefonoCliente.getText() : "";
        Cliente dup = buscarOtroClienteDuplicado(nom, tel);

        if (dup != null) {
            mostrarBannerDuplicado(dup);
        } else if (boxAlertaDuplicado != null) {
            boxAlertaDuplicado.setVisible(false);
            boxAlertaDuplicado.setManaged(false);
        }
    }

    private void mostrarBannerDuplicado(Cliente otro) {
        if (boxAlertaDuplicado == null || lblTextoDuplicado == null || otro == null) {
            return;
        }
        String telStr = otro.getTelefono() != null && !otro.getTelefono().trim().isEmpty()
                ? otro.getTelefono().trim()
                : "Sin teléfono";
        lblTextoDuplicado.setText("Ya hay un cliente con este nombre: "
                + otro.getNombre() + " · " + telStr
                + ". Cambie el nombre o vuelva a la lista.");
        boxAlertaDuplicado.setVisible(true);
        boxAlertaDuplicado.setManaged(true);
    }

    @FXML
    public void handleGuardarCambios() {
        if (clienteActual == null) {
            return;
        }

        String nombre = txtNombreCliente != null && txtNombreCliente.getText() != null
                ? txtNombreCliente.getText().trim()
                : "";
        String telefono = txtTelefonoCliente != null && txtTelefonoCliente.getText() != null
                ? txtTelefonoCliente.getText().trim()
                : "";

        boolean hayError = false;
        if (nombre.isEmpty()) {
            hayError = true;
            if (boxInputNombre != null && !boxInputNombre.getStyleClass().contains("reg-cliente-input-box-error")) {
                boxInputNombre.getStyleClass().add("reg-cliente-input-box-error");
            }
            if (lblAyudaNombre != null) {
                lblAyudaNombre.setText("⚠ Escriba el nombre completo del cliente.");
                if (!lblAyudaNombre.getStyleClass().contains("reg-cliente-field-help-error")) {
                    lblAyudaNombre.getStyleClass().add("reg-cliente-field-help-error");
                }
            }
        }

        if (telefono.isEmpty()) {
            hayError = true;
            if (boxInputTelefono != null && !boxInputTelefono.getStyleClass().contains("reg-cliente-input-box-error")) {
                boxInputTelefono.getStyleClass().add("reg-cliente-input-box-error");
            }
            if (lblAyudaTelefono != null) {
                lblAyudaTelefono.setText("⚠ Escriba el número de teléfono del cliente.");
                if (!lblAyudaTelefono.getStyleClass().contains("reg-cliente-field-help-error")) {
                    lblAyudaTelefono.getStyleClass().add("reg-cliente-field-help-error");
                }
            }
        }

        if (hayError) {
            return;
        }

        cargarCatalogoClientesParaValidacion();
        Cliente duplicado = buscarOtroClienteDuplicado(nombre, telefono);
        if (duplicado != null) {
            mostrarBannerDuplicado(duplicado);
            return;
        }

        clienteActual.setNombre(nombre);
        clienteActual.setTelefono(telefono);

        try {
            boolean actualizado = clienteDAO.actualizar(clienteActual);
            if (actualizado) {
                clientePendienteEdicion = null;
                ClientesController.setIdClienteSeleccionarAlCargar(clienteActual.getIdCliente());
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                    menu.cargarVistaEnContenido("/fxml/clientes.fxml");
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudieron guardar los cambios del cliente en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleDarBajaCliente() {
        if (clienteActual == null) {
            return;
        }
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede dar de baja o reactivar clientes.");
            return;
        }

        // Si ya esta dado de baja, permite reactivarlo
        if (!clienteActual.isActivo()) {
            boolean confirmarReactivar = AlertaUtil.mostrarConfirmacion(
                    "Volver a activar cliente",
                    "¿Desea volver a activar a «" + clienteActual.getNombre() + "» en la lista de clientes?");
            if (!confirmarReactivar) {
                return;
            }
            try {
                if (clienteDAO.cambiarEstado(clienteActual.getIdCliente(), true)) {
                    clientePendienteEdicion = null;
                    ClientesController.setIdClienteSeleccionarAlCargar(clienteActual.getIdCliente());
                    MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                    if (menu != null) {
                        menu.refrescarAlertasReales();
                        menu.cargarVistaEnContenido("/fxml/clientes.fxml");
                    }
                }
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Error de Base de Datos",
                        "No se pudo reactivar al cliente en MySQL: " + e.getMessage());
            }
            return;
        }

        // Regla de negocio: impedir dar de baja si tiene deuda pendiente (deuda_actual > 0)
        BigDecimal deudaActual = clienteActual.getSaldoPendiente() != null
                ? clienteActual.getSaldoPendiente()
                : BigDecimal.ZERO;
        if (deudaActual.compareTo(BigDecimal.ZERO) > 0) {
            AlertaUtil.mostrarAdvertencia(
                    "No se puede dar de baja todavía",
                    "«" + clienteActual.getNombre() + "» todavía debe $ "
                            + formatearMoneda(deudaActual)
                            + ".\n\nPrimero debe saldar su deuda registrando el abono correspondiente antes de darle de baja.");
            return;
        }

        boolean confirmarBaja = AlertaUtil.mostrarConfirmacion(
                "¿Dar de baja a este cliente?",
                "¿Está segura de que desea archivar a «" + clienteActual.getNombre()
                        + "»?\n\nSu historial de ventas y abonos se conservará y podrá volver a activarlo cuando lo necesite.");
        if (!confirmarBaja) {
            return;
        }

        try {
            if (clienteDAO.cambiarEstado(clienteActual.getIdCliente(), false)) {
                clientePendienteEdicion = null;
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                    menu.cargarVistaEnContenido("/fxml/clientes.fxml");
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo dar de baja al cliente en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleVolverAClientes() {
        if (clienteActual != null) {
            ClientesController.setIdClienteSeleccionarAlCargar(clienteActual.getIdCliente());
        }
        clientePendienteEdicion = null;
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/clientes.fxml");
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

    private String normalizarTexto(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(LOCALE_ES_CO).replaceAll("\\s+", " ");
    }

    private String normalizarTelefono(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("[^0-9]", "");
    }
}
