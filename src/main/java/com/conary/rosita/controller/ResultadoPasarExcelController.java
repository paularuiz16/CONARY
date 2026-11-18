package com.conary.rosita.controller;

import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.ImportadorExcelService.FilaProblema;
import com.conary.rosita.util.ImportadorExcelService.ResultadoMigracion;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.util.List;

/**
 * Controlador de la pantalla de resultado "¡Listo! Los datos ya están en el sistema".
 */
public class ResultadoPasarExcelController {

    private static ResultadoMigracion resultadoPendiente;

    public static void setResultadoPendiente(ResultadoMigracion resultado) {
        resultadoPendiente = resultado;
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverHeader;
    @FXML private Label lblUsuarioHeader;

    @FXML private Label lblVentasPasadas;
    @FXML private Label lblProductosNuevosCreados;
    @FXML private Label lblFilasNoPasadas;
    @FXML private Button btnVerCuales;

    @FXML private Button btnVerProductos;
    @FXML private Button btnVolverMasOpciones;

    private ResultadoMigracion resultadoActual;

    @FXML
    public void initialize() {
        cargarCabeceraUsuario();
        if (resultadoPendiente != null) {
            mostrarResultado(resultadoPendiente);
        }
    }

    private void cargarCabeceraUsuario() {
        Usuario u = SesionUsuario.getUsuarioLogueado();
        if (u != null && lblUsuarioHeader != null) {
            String rolTexto = u.esAdministrador() ? "Administradora" : "Vendedora";
            lblUsuarioHeader.setText(u.getNombre() + " · " + rolTexto);
        }
    }

    public void mostrarResultado(ResultadoMigracion resultado) {
        this.resultadoActual = resultado;
        resultadoPendiente = null;
        if (resultado == null) {
            return;
        }

        int ventas = resultado.getVentasPasadas();
        int nuevos = resultado.getProductosNuevosCreados();
        List<FilaProblema> omitidas = resultado.getFilasNoPasadas();
        int nOmitidas = omitidas != null ? omitidas.size() : 0;

        if (lblVentasPasadas != null) {
            lblVentasPasadas.setText(ventas == 1 ? "1 venta pasada" : ventas + " ventas pasadas");
        }
        if (lblProductosNuevosCreados != null) {
            lblProductosNuevosCreados.setText(nuevos == 1
                    ? "1 producto nuevo creado"
                    : nuevos + " productos nuevos creados");
        }
        if (lblFilasNoPasadas != null) {
            lblFilasNoPasadas.setText(nOmitidas == 1
                    ? "1 fila no se pasó"
                    : nOmitidas + " filas no se pasaron");
        }
        if (btnVerCuales != null) {
            boolean hayOmitidas = nOmitidas > 0;
            btnVerCuales.setVisible(hayOmitidas);
            btnVerCuales.setManaged(hayOmitidas);
        }
    }

    @FXML
    public void handleVerCuales() {
        if (resultadoActual == null || resultadoActual.getFilasNoPasadas().isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        List<FilaProblema> lista = resultadoActual.getFilasNoPasadas();
        int limite = Math.min(lista.size(), 40);
        for (int i = 0; i < limite; i++) {
            sb.append("• ").append(lista.get(i).getMensaje()).append("\n");
        }
        if (lista.size() > limite) {
            sb.append("\n... y ").append(lista.size() - limite).append(" filas más.");
        }
        AlertaUtil.mostrarInfo("Filas que no se pasaron", sb.toString());
    }

    @FXML
    public void handleVerProductos() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/productos.fxml");
        }
    }

    @FXML
    public void handleVolverMasOpciones() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/mas_opciones.fxml");
        }
    }
}
