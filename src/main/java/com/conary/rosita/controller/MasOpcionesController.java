package com.conary.rosita.controller;

import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.RespaldoService;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.util.Locale;

/**
 * Controlador de la Vista A del Paso 2A: Pantalla de entrada "Mas opciones".
 * Acceso exclusivo para Doña Rosita (Administradora).
 */
public class MasOpcionesController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverMenu;
    @FXML private Label lblUsuarioNombre;
    @FXML private Label lblUsuarioRol;
    @FXML private Button btnUsuariosClaves;
    @FXML private Button btnCopiasSeguridad;
    @FXML private Label lblUltimaCopia;

    private final RespaldoService respaldoService = new RespaldoService();

    @FXML
    public void initialize() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) tiene acceso a Más opciones.");
            handleVolverMenu();
            return;
        }
        configurarEncabezadoUsuario();
        actualizarUltimaCopia();
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

    private void actualizarUltimaCopia() {
        if (lblUltimaCopia != null) {
            lblUltimaCopia.setText(respaldoService.obtenerTextoUltimaCopia());
        }
    }

    @FXML
    public void handleAbrirUsuarios() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede gestionar usuarios y claves.");
            return;
        }
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/usuarios.fxml");
        }
    }

    @FXML
    public void handleAbrirCopiasSeguridad() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede gestionar copias de seguridad.");
            return;
        }
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/copias_seguridad.fxml");
        }
    }

    @FXML
    public void handleVolverMenu() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarMenuDashboard();
        }
    }
}