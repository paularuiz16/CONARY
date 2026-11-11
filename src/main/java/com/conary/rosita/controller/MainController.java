package com.conary.rosita.controller;

import com.conary.rosita.App;
import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Controlador del marco principal de la aplicacion (Dashboard, Navegacion y Sesion).
 */
public class MainController {

    @FXML private Label lblUsuarioLogueado;
    @FXML private Label lblEstadoCaja;
    @FXML private StackPane contentArea;

    @FXML private Button btnNavVenta;
    @FXML private Button btnNavInventario;
    @FXML private Button btnNavClientes;
    @FXML private Button btnNavProveedores;
    @FXML private Button btnNavCaja;

    private final CajaDAO cajaDAO = new CajaDAO();
    private List<Button> botonesMenu;

    @FXML
    public void initialize() {
        botonesMenu = Arrays.asList(btnNavVenta, btnNavInventario, btnNavClientes, btnNavProveedores, btnNavCaja);

        refrescarDatosSesion();

        // Cargar por defecto la pantalla de Punto de Venta al iniciar
        mostrarPuntoVenta();
    }

    public void refrescarDatosSesion() {
        Usuario usuario = SesionUsuario.getUsuarioLogueado();
        if (usuario != null) {
            lblUsuarioLogueado.setText(usuario.getNombre() + " (" + usuario.getRol() + ")");
        } else {
            lblUsuarioLogueado.setText("Invitado");
        }

        refrescarEstadoCaja();
    }

    public void refrescarEstadoCaja() {
        try {
            Caja abierta = cajaDAO.obtenerCajaAbierta();
            SesionUsuario.setCajaActiva(abierta);
            if (abierta != null) {
                lblEstadoCaja.setText("ABIERTA (Turno #" + abierta.getIdCaja() + ")");
                lblEstadoCaja.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 12px;");
            } else {
                lblEstadoCaja.setText("CERRADA");
                lblEstadoCaja.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 12px;");
            }
        } catch (SQLException e) {
            lblEstadoCaja.setText("DESCONOCIDO");
            lblEstadoCaja.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        }
    }

    @FXML
    public void mostrarPuntoVenta() {
        activarBoton(btnNavVenta);
        cargarVistaEnContenido("/fxml/punto_venta.fxml");
    }

    @FXML
    public void mostrarInventario() {
        activarBoton(btnNavInventario);
        cargarVistaEnContenido("/fxml/inventario.fxml");
    }

    @FXML
    public void mostrarClientes() {
        activarBoton(btnNavClientes);
        cargarVistaEnContenido("/fxml/clientes.fxml");
    }

    @FXML
    public void mostrarProveedores() {
        activarBoton(btnNavProveedores);
        cargarVistaEnContenido("/fxml/proveedores.fxml");
    }

    @FXML
    public void mostrarCaja() {
        activarBoton(btnNavCaja);
        cargarVistaEnContenido("/fxml/caja.fxml");
    }

    @FXML
    public void handleCerrarSesion() {
        boolean confirmar = AlertaUtil.mostrarConfirmacion("Cerrar Sesión", "¿Estás seguro de que deseas salir del sistema?");
        if (confirmar) {
            SesionUsuario.cerrarSesion();
            App.cambiarEscena("/fxml/login.fxml", "Papelería de Rosita - Acceso al Sistema", 420, 500);
        }
    }

    private void activarBoton(Button botonActivo) {
        if (botonesMenu == null) return;
        for (Button btn : botonesMenu) {
            btn.getStyleClass().remove("sidebar-button-active");
        }
        if (!botonActivo.getStyleClass().contains("sidebar-button-active")) {
            botonActivo.getStyleClass().add("sidebar-button-active");
        }
    }

    private void cargarVistaEnContenido(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(node);
        } catch (IOException e) {
            System.err.println("[MainController] Error cargando vista: " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
            AlertaUtil.mostrarError("Error de Navegación", "No se pudo cargar la vista solicitada: " + fxmlPath);
        }
    }
}

