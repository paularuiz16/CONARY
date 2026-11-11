package com.conary.rosita.controller;

import com.conary.rosita.dao.CajaDAO;
import com.conary.rosita.model.Caja;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de Caja, Turnos y Arqueo Diario.
 */
public class CajaController {

    @FXML private Label lblEstadoTurno;
    @FXML private Label lblFechaApertura;
    @FXML private Label lblMontoBase;

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

    private final CajaDAO cajaDAO = new CajaDAO();
    private final ObservableList<Caja> listaHistorial = FXCollections.observableArrayList();
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private Caja cajaActiva;
    private BigDecimal totalTirillaCalculado = BigDecimal.ZERO;

    @FXML
    public void initialize() {
        configurarTabla();
        handleRefrescarCaja();

        txtEfectivoContado.textProperty().addListener((obs, viejo, nuevo) -> recalcularDiferenciaEnVivo());
    }

    private void configurarTabla() {
        colIdCaja.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getIdCaja()));
        colApertura.setCellValueFactory(data -> {
            if (data.getValue().getFechaApertura() != null) {
                return new SimpleStringProperty(formatoFecha.format(data.getValue().getFechaApertura()));
            }
            return new SimpleStringProperty("-");
        });
        colCierre.setCellValueFactory(data -> {
            if (data.getValue().getFechaCierre() != null) {
                return new SimpleStringProperty(formatoFecha.format(data.getValue().getFechaCierre()));
            }
            return new SimpleStringProperty("ABIERTA ACTUALMENTE");
        });
        colBase.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getMontoInicial())));
        colTirilla.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getTotalTirilla())));
        colContado.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getEfectivoContado())));
        colDiferencia.setCellValueFactory(data -> new SimpleStringProperty(formatoMoneda.format(data.getValue().getDiferencia())));

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
                            setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;"); // Faltante
                        } else if (c.getDiferencia().compareTo(BigDecimal.ZERO) > 0) {
                            setStyle("-fx-text-fill: #0369a1; -fx-font-weight: bold;"); // Sobrante
                        } else {
                            setStyle("-fx-text-fill: #15803d;"); // Exacto
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

            if (cajaActiva != null) {
                lblEstadoTurno.setText("ABIERTA (Turno #" + cajaActiva.getIdCaja() + ")");
                lblEstadoTurno.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10b981;");
                lblFechaApertura.setText("Fecha Apertura: " + formatoFecha.format(cajaActiva.getFechaApertura()));
                lblMontoBase.setText("Monto Base Inicial: " + formatoMoneda.format(cajaActiva.getMontoInicial()));

                panelApertura.setVisible(false);
                panelApertura.setManaged(false);

                panelCierre.setVisible(true);
                panelCierre.setManaged(true);

                // Calcular tirilla en tiempo real
                totalTirillaCalculado = cajaDAO.calcularTotalTirilla(cajaActiva.getIdCaja());
                lblTotalTirilla.setText(formatoMoneda.format(totalTirillaCalculado));
                recalcularDiferenciaEnVivo();

            } else {
                lblEstadoTurno.setText("CERRADA");
                lblEstadoTurno.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #ef4444;");
                lblFechaApertura.setText("Fecha Apertura: No iniciada");
                lblMontoBase.setText("Monto Base Inicial: $ 0.00");

                panelApertura.setVisible(true);
                panelApertura.setManaged(true);

                panelCierre.setVisible(false);
                panelCierre.setManaged(false);
            }

            // Cargar historial
            List<Caja> historial = cajaDAO.listarHistorial();
            listaHistorial.setAll(historial);

        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Consultar Caja", "Detalle: " + e.getMessage());
        }
    }

    private void recalcularDiferenciaEnVivo() {
        if (cajaActiva == null) return;
        String textoContado = txtEfectivoContado.getText() != null ? txtEfectivoContado.getText().trim() : "";
        if (textoContado.isEmpty()) {
            lblDiferencia.setText("Diferencia: $ 0.00");
            lblDiferencia.setStyle("-fx-text-fill: #64748b; -fx-font-weight: bold;");
            return;
        }

        try {
            BigDecimal contado = new BigDecimal(textoContado.replace(",", "."));
            BigDecimal dif = contado.subtract(totalTirillaCalculado);

            if (dif.compareTo(BigDecimal.ZERO) < 0) {
                lblDiferencia.setText("Faltante en Caja: " + formatoMoneda.format(dif));
                lblDiferencia.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 13px;");
            } else if (dif.compareTo(BigDecimal.ZERO) > 0) {
                lblDiferencia.setText("Sobrante en Caja: +" + formatoMoneda.format(dif));
                lblDiferencia.setStyle("-fx-text-fill: #0284c7; -fx-font-weight: bold; -fx-font-size: 13px;");
            } else {
                lblDiferencia.setText("Diferencia: Exacto ($ 0.00)");
                lblDiferencia.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 13px;");
            }
        } catch (Exception e) {
            lblDiferencia.setText("Formato de número incorrecto.");
            lblDiferencia.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    public void handleAbrirCaja() {
        String baseTexto = txtMontoInicial.getText() != null ? txtMontoInicial.getText().trim() : "";
        BigDecimal base = BigDecimal.ZERO;
        if (!baseTexto.isEmpty()) {
            try {
                base = new BigDecimal(baseTexto.replace(",", "."));
                if (base.compareTo(BigDecimal.ZERO) < 0) throw new Exception();
            } catch (Exception e) {
                AlertaUtil.mostrarAdvertencia("Monto Inválido", "Ingresa una cantidad de base inicial válida.");
                return;
            }
        }

        try {
            Caja abierta = cajaDAO.abrirCaja(base);
            if (abierta != null) {
                AlertaUtil.mostrarInfo("Turno Abierto", "Se inició el Turno de Caja #" + abierta.getIdCaja() + " con una base de " + formatoMoneda.format(base));
                txtMontoInicial.clear();
                handleRefrescarCaja();
            }
        } catch (Exception e) {
            AlertaUtil.mostrarError("Error al Abrir Caja", e.getMessage());
        }
    }

    @FXML
    public void handleCerrarCaja() {
        if (cajaActiva == null) return;

        String contadoTexto = txtEfectivoContado.getText() != null ? txtEfectivoContado.getText().trim() : "";
        if (contadoTexto.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Efectivo Requerido", "Debes ingresar el efectivo físico contado en el cajón para realizar el arqueo.");
            txtEfectivoContado.requestFocus();
            return;
        }

        BigDecimal contado;
        try {
            contado = new BigDecimal(contadoTexto.replace(",", "."));
            if (contado.compareTo(BigDecimal.ZERO) < 0) throw new Exception();
        } catch (Exception e) {
            AlertaUtil.mostrarAdvertencia("Valor Inválido", "Ingresa un valor positivo válido para el efectivo contado.");
            return;
        }

        BigDecimal diferencia = contado.subtract(totalTirillaCalculado);
        String mensajeConfirm = "¿Deseas cerrar el Turno #" + cajaActiva.getIdCaja() + "?\n\n"
                + "• Total Tirilla esperado: " + formatoMoneda.format(totalTirillaCalculado) + "\n"
                + "• Efectivo contado físico: " + formatoMoneda.format(contado) + "\n"
                + "• Cuadre / Diferencia: " + (diferencia.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + formatoMoneda.format(diferencia);

        boolean confirmacion = AlertaUtil.mostrarConfirmacion("Confirmar Cierre de Caja", mensajeConfirm);
        if (!confirmacion) return;

        try {
            boolean exito = cajaDAO.cerrarCaja(cajaActiva.getIdCaja(), contado);
            if (exito) {
                AlertaUtil.mostrarInfo("Caja Cerrada", "El arqueo del turno #" + cajaActiva.getIdCaja() + " fue guardado con éxito.");
                txtEfectivoContado.clear();
                handleRefrescarCaja();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Cerrar Caja", e.getMessage());
        }
    }
}

