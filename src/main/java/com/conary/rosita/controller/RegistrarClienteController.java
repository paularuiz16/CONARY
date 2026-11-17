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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la Vista B de la Tanda 1 del Modulo 3: Registrar cliente.
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class RegistrarClienteController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

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

    @FXML private Button btnCancelar;
    @FXML private Button btnGuardarCliente;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final List<Cliente> clientesExistentes = new ArrayList<Cliente>();

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        cargarClientesExistentesParaValidacion();
        configurarListenersValidacion();

        Platform.runLater(() -> {
            if (txtNombreCliente != null) {
                txtNombreCliente.requestFocus();
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

    private void cargarClientesExistentesParaValidacion() {
        clientesExistentes.clear();
        try {
            List<Cliente> desdeBD = clienteDAO.listarTodos(false);
            if (desdeBD != null) {
                clientesExistentes.addAll(desdeBD);
            }
        } catch (SQLException e) {
            System.err.println("[RegistrarClienteController] Error al cargar clientes existentes: " + e.getMessage());
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
                    handleGuardarCliente();
                }
            });
        }
    }

    private Cliente buscarClienteDuplicado(String nombreInput, String telefonoInput) {
        String nomNorm = normalizarTexto(nombreInput);
        String telNorm = normalizarTelefono(telefonoInput);

        for (Cliente c : clientesExistentes) {
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
        Cliente dup = buscarClienteDuplicado(nom, tel);

        if (dup != null) {
            mostrarBannerDuplicado(dup);
        } else if (boxAlertaDuplicado != null) {
            boxAlertaDuplicado.setVisible(false);
            boxAlertaDuplicado.setManaged(false);
        }
    }

    private void mostrarBannerDuplicado(Cliente existente) {
        if (boxAlertaDuplicado == null || lblTextoDuplicado == null || existente == null) {
            return;
        }
        String telStr = existente.getTelefono() != null && !existente.getTelefono().trim().isEmpty()
                ? existente.getTelefono().trim()
                : "Sin teléfono";
        lblTextoDuplicado.setText("Ya hay un cliente con este nombre: "
                + existente.getNombre() + " · " + telStr
                + ". Cambie el nombre o vuelva a la lista.");
        boxAlertaDuplicado.setVisible(true);
        boxAlertaDuplicado.setManaged(true);
    }

    @FXML
    public void handleGuardarCliente() {
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

        cargarClientesExistentesParaValidacion();
        Cliente duplicado = buscarClienteDuplicado(nombre, telefono);
        if (duplicado != null) {
            mostrarBannerDuplicado(duplicado);
            return;
        }

        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setNombre(nombre);
        nuevoCliente.setTelefono(telefono);
        nuevoCliente.setDireccion(null);
        nuevoCliente.setActivo(true);
        nuevoCliente.setFiadoAutorizado(false);
        nuevoCliente.setLimiteCredito(BigDecimal.ZERO);
        nuevoCliente.setSaldoPendiente(BigDecimal.ZERO);

        try {
            boolean insertado = clienteDAO.insertar(nuevoCliente);
            if (insertado) {
                ClientesController.setIdClienteSeleccionarAlCargar(nuevoCliente.getIdCliente());
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                    menu.cargarVistaEnContenido("/fxml/clientes.fxml");
                }
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo guardar el cliente en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleVolverAClientes() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/clientes.fxml");
        }
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
