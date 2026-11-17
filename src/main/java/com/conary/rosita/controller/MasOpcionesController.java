package com.conary.rosita.controller;

import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
        if (lblUltimaCopia == null) {
            return;
        }
        File ultimo = buscarUltimoRespaldoLocal();
        if (ultimo == null) {
            lblUltimaCopia.setText("🕒  Última copia: Sin copias registradas aún");
            return;
        }
        LocalDateTime fechaMod = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(ultimo.lastModified()), ZoneId.systemDefault());
        String hora = fechaMod.format(DateTimeFormatter.ofPattern("hh:mm a", LOCALE_ES_CO)).toLowerCase(LOCALE_ES_CO);
        if (fechaMod.toLocalDate().equals(LocalDate.now())) {
            lblUltimaCopia.setText("🕒  Última copia: hoy, " + hora);
        } else {
            String dia = fechaMod.format(DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_ES_CO));
            lblUltimaCopia.setText("🕒  Última copia: " + dia + ", " + hora);
        }
    }

    private File buscarUltimoRespaldoLocal() {
        String home = System.getProperty("user.home");
        File[] carpetas = new File[]{
                new File("backups"),
                new File("respaldos"),
                new File(home + File.separator + "Documents" + File.separator + "CONARY_Backups"),
                new File(home + File.separator + "Documentos" + File.separator + "CONARY_Backups")
        };

        File masReciente = null;
        for (File dir : carpetas) {
            if (dir.exists() && dir.isDirectory()) {
                File[] archivos = dir.listFiles();
                if (archivos != null) {
                    for (File f : archivos) {
                        if (f.isFile() && (f.getName().endsWith(".sql") || f.getName().endsWith(".zip"))) {
                            if (masReciente == null || f.lastModified() > masReciente.lastModified()) {
                                masReciente = f;
                            }
                        }
                    }
                }
            }
        }
        return masReciente;
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
        AlertaUtil.mostrarInfo("Copias de seguridad",
                "La vista de copias de seguridad y restauración se conectará en el Paso 2C del submódulo Más opciones.");
    }

    @FXML
    public void handleVolverMenu() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarMenuDashboard();
        }
    }
}