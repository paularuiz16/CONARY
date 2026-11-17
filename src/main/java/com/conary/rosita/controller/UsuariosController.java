package com.conary.rosita.controller;

import com.conary.rosita.dao.UsuarioDAO;
import com.conary.rosita.model.RolUsuario;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controlador de la Vista B del Paso 2A: "Usuarios y claves".
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class UsuariosController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static int idUsuarioSeleccionarAlCargar = -1;

    public static void setIdUsuarioSeleccionarAlCargar(int idUsuario) {
        idUsuarioSeleccionarAlCargar = idUsuario;
    }

    @FXML private BorderPane rootPane;
    @FXML private Button btnVolverMasOpciones;
    @FXML private Label lblUsuarioHeader;

    @FXML private ToggleButton tbFiltroActivos;
    @FXML private ToggleButton tbFiltroDadosBaja;
    @FXML private Button btnRegistrarUsuario;

    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, Usuario> colNombre;
    @FXML private TableColumn<Usuario, Usuario> colUsuario;
    @FXML private TableColumn<Usuario, Usuario> colRol;
    @FXML private TableColumn<Usuario, Usuario> colEstado;

    @FXML private Label lblTotalRegistrados;
    @FXML private HBox barraGestionUsuario;
    @FXML private Label lblUsuarioSeleccionadoInfo;
    @FXML private Button btnCambiarClave;
    @FXML private Button btnModificarUsuario;
    @FXML private Button btnDarBajaUsuario;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final ToggleGroup tgFiltros = new ToggleGroup();
    private final List<Usuario> todosLosUsuarios = new ArrayList<Usuario>();
    private final ObservableList<Usuario> usuariosVisibles = FXCollections.observableArrayList();

    private Usuario usuarioSeleccionado;

    @FXML
    public void initialize() {
        if (SesionUsuario.haySesionActiva() && !SesionUsuario.esAdministrador()) {
            AlertaUtil.mostrarAdvertencia("Acceso Restringido",
                    "Solo Doña Rosita (Administradora) puede gestionar usuarios y claves.");
            MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
            if (menu != null) {
                menu.mostrarMenuDashboard();
            }
            return;
        }
        configurarEncabezadoUsuario();
        configurarFiltros();
        configurarTabla();
        cargarUsuariosDesdeMySQL();
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

        if (lblUsuarioHeader != null) {
            lblUsuarioHeader.setText(nombre + " · " + rol);
        }
    }

    private void configurarFiltros() {
        if (tbFiltroActivos != null) tbFiltroActivos.setToggleGroup(tgFiltros);
        if (tbFiltroDadosBaja != null) tbFiltroDadosBaja.setToggleGroup(tgFiltros);
        if (tbFiltroActivos != null) tbFiltroActivos.setSelected(true);

        tgFiltros.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
                return;
            }
            actualizarTextoFiltros();
            aplicarFiltro();
        });
    }

    private void configurarTabla() {
        tblUsuarios.setItems(usuariosVisibles);
        tblUsuarios.setFixedCellSize(56.0);
        tblUsuarios.setRowFactory(tv -> new TableRow<Usuario>() {
            @Override
            protected void updateItem(Usuario item, boolean empty) {
                super.updateItem(item, empty);
                setPrefHeight(56.0);
                setMinHeight(56.0);
                setMaxHeight(56.0);
            }
        });

        // 1. Nombre (con punto cafe ● en la fila seleccionada)
        colNombre.setCellValueFactory(d -> new SimpleObjectProperty<Usuario>(d.getValue()));
        colNombre.setCellFactory(col -> new TableCell<Usuario, Usuario>() {
            private final Circle dot = new Circle(4.2, Color.web("#6B3A19"));
            private final Label lblNom = new Label();
            private final HBox box = new HBox(10, dot, lblNom);

            {
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Usuario u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    TableRow<?> row = getTableRow();
                    boolean sel = row != null && row.isSelected();
                    dot.setVisible(sel);
                    dot.setManaged(sel);
                    lblNom.setText(formatearNombreMostrar(u));
                    lblNom.getStyleClass().removeAll("clientes-cell-nombre", "clientes-cell-dash");
                    lblNom.getStyleClass().add(u.isActivo() ? "clientes-cell-nombre" : "clientes-cell-dash");
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // 2. Usuario (username en estilo monoespaciado)
        colUsuario.setCellValueFactory(d -> new SimpleObjectProperty<Usuario>(d.getValue()));
        colUsuario.setCellFactory(col -> new TableCell<Usuario, Usuario>() {
            @Override
            protected void updateItem(Usuario u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setText(null);
                } else {
                    setText(u.getUsername());
                    getStyleClass().removeAll("usuarios-cell-username", "usuarios-cell-username-inactivo");
                    getStyleClass().add(u.isActivo() ? "usuarios-cell-username" : "usuarios-cell-username-inactivo");
                }
            }
        });

        // 3. Que puede hacer (pildora cafe Administradora / pildora melocoton Vendedora)
        colRol.setCellValueFactory(d -> new SimpleObjectProperty<Usuario>(d.getValue()));
        colRol.setCellFactory(col -> new TableCell<Usuario, Usuario>() {
            @Override
            protected void updateItem(Usuario u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label pill = new Label(u.esAdministrador() ? "Administradora" : "Vendedora");
                    pill.getStyleClass().add(u.esAdministrador() ? "usuarios-pill-admin" : "usuarios-pill-vendedor");
                    if (!u.isActivo()) {
                        pill.setOpacity(0.65);
                    }
                    HBox box = new HBox(pill);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // 4. Estado ("—" si activo, pildora "Dado de baja" si inactivo)
        colEstado.setCellValueFactory(d -> new SimpleObjectProperty<Usuario>(d.getValue()));
        colEstado.setCellFactory(col -> new TableCell<Usuario, Usuario>() {
            @Override
            protected void updateItem(Usuario u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER);
                    if (u.isActivo()) {
                        setGraphic(null);
                        setText("—");
                        getStyleClass().removeAll("clientes-cell-dash");
                        getStyleClass().add("clientes-cell-dash");
                    } else {
                        Label pill = new Label("Dado de baja");
                        pill.getStyleClass().add("usuarios-pill-baja");
                        HBox box = new HBox(pill);
                        box.setAlignment(Pos.CENTER);
                        setGraphic(box);
                        setText(null);
                    }
                }
            }
        });

        tblUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            usuarioSeleccionado = newSel;
            actualizarBarraGestion(newSel);
            tblUsuarios.refresh();
        });
    }

    public void cargarUsuariosDesdeMySQL() {
        todosLosUsuarios.clear();
        try {
            List<Usuario> desdeBD = usuarioDAO.listarTodos(false);
            if (desdeBD != null) {
                todosLosUsuarios.addAll(desdeBD);
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo consultar la lista de usuarios en MySQL: " + e.getMessage());
        }

        actualizarTextoFiltros();
        if (lblTotalRegistrados != null) {
            int n = todosLosUsuarios.size();
            lblTotalRegistrados.setText(n == 1 ? "Total: 1 persona registrada" : ("Total: " + n + " personas registradas"));
        }
        aplicarFiltro();
    }

    private void actualizarTextoFiltros() {
        int activos = 0;
        int inactivos = 0;
        for (Usuario u : todosLosUsuarios) {
            if (u.isActivo()) activos++;
            else inactivos++;
        }

        Toggle sel = tgFiltros.getSelectedToggle();
        if (tbFiltroActivos != null) {
            tbFiltroActivos.setText((sel == tbFiltroActivos ? "✓   " : "") + "Activos (" + activos + ")");
        }
        if (tbFiltroDadosBaja != null) {
            tbFiltroDadosBaja.setText("📦   Dados de baja (" + inactivos + ")");
        }
    }

    private void aplicarFiltro() {
        boolean verActivos = tgFiltros.getSelectedToggle() != tbFiltroDadosBaja;
        usuariosVisibles.clear();
        for (Usuario u : todosLosUsuarios) {
            if (u.isActivo() == verActivos) {
                usuariosVisibles.add(u);
            }
        }

        Usuario objetivo = null;
        if (idUsuarioSeleccionarAlCargar > 0) {
            for (Usuario cand : usuariosVisibles) {
                if (cand.getIdUsuario() == idUsuarioSeleccionarAlCargar) {
                    objetivo = cand;
                    break;
                }
            }
            idUsuarioSeleccionarAlCargar = -1;
        }

        if (objetivo != null) {
            tblUsuarios.getSelectionModel().select(objetivo);
        } else if (!usuariosVisibles.isEmpty()) {
            tblUsuarios.getSelectionModel().selectFirst();
        } else {
            tblUsuarios.getSelectionModel().clearSelection();
            actualizarBarraGestion(null);
        }
    }

    private void actualizarBarraGestion(Usuario sel) {
        boolean haySel = sel != null;
        if (lblUsuarioSeleccionadoInfo != null) {
            if (!haySel) {
                lblUsuarioSeleccionadoInfo.setText("Seleccione una persona de la lista para gestionar su cuenta");
            } else {
                String rolTexto = sel.esAdministrador() ? "Administradora" : "Vendedora";
                String estadoTexto = sel.isActivo() ? "" : " · Dado de baja";
                lblUsuarioSeleccionadoInfo.setText("Seleccionado: " + formatearNombreMostrar(sel) + " · " + rolTexto + estadoTexto);
            }
        }

        if (btnCambiarClave != null) btnCambiarClave.setDisable(!haySel);
        if (btnModificarUsuario != null) btnModificarUsuario.setDisable(!haySel);
        if (btnDarBajaUsuario != null) {
            btnDarBajaUsuario.setDisable(!haySel);
            btnDarBajaUsuario.getStyleClass().removeAll("usuarios-btn-baja", "usuarios-btn-reactivar");
            if (haySel && !sel.isActivo()) {
                btnDarBajaUsuario.setText("🔄   Volver a activar");
                btnDarBajaUsuario.getStyleClass().add("usuarios-btn-reactivar");
            } else {
                btnDarBajaUsuario.setText("📦   Dar de baja");
                btnDarBajaUsuario.getStyleClass().add("usuarios-btn-baja");
            }
        }
    }

    @FXML
    public void handleRegistrarUsuarioNuevo() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/registrar_usuario.fxml");
        }
    }

    @FXML
    public void handleCambiarClave() {
        if (usuarioSeleccionado == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/modal_cambiar_clave.fxml"));
            Parent root = loader.load();

            CambiarClaveController controller = loader.getController();
            controller.setUsuario(usuarioSeleccionado);

            Stage modalStage = new Stage(StageStyle.TRANSPARENT);
            modalStage.initModality(Modality.APPLICATION_MODAL);

            Window owner = rootPane != null && rootPane.getScene() != null
                    ? rootPane.getScene().getWindow()
                    : null;
            if (owner != null) {
                modalStage.initOwner(owner);
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            if (rootPane != null && rootPane.getScene() != null) {
                scene.getStylesheets().addAll(rootPane.getScene().getStylesheets());
            } else {
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            }

            modalStage.setScene(scene);
            if (owner != null) {
                modalStage.setX(owner.getX());
                modalStage.setY(owner.getY());
                modalStage.setWidth(owner.getWidth());
                modalStage.setHeight(owner.getHeight());
            }
            modalStage.showAndWait();

            if (controller.isClaveActualizada()) {
                idUsuarioSeleccionarAlCargar = usuarioSeleccionado.getIdUsuario();
                cargarUsuariosDesdeMySQL();
            }
        } catch (IOException e) {
            AlertaUtil.mostrarError("Error al abrir modal",
                    "No se pudo cargar la ventana para cambiar la clave: " + e.getMessage());
        }
    }

    @FXML
    public void handleModificarUsuario() {
        if (usuarioSeleccionado == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle("Modificar datos del usuario");
        dialog.setHeaderText("Editar información de acceso de «" + usuarioSeleccionado.getUsername() + "»");

        ButtonType btnGuardar = new ButtonType("Guardar cambios", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGuardar, btnCancelar);

        TextField txtNombre = new TextField(usuarioSeleccionado.getNombre());
        txtNombre.setPromptText("Nombre completo");
        txtNombre.setPrefWidth(320);

        ComboBox<RolUsuario> cmbRol = new ComboBox<RolUsuario>();
        cmbRol.getItems().addAll(RolUsuario.ADMINISTRADOR, RolUsuario.VENDEDOR);
        cmbRol.setValue(usuarioSeleccionado.getRol());
        cmbRol.setMaxWidth(Double.MAX_VALUE);
        cmbRol.setCellFactory(lv -> new ListCell<RolUsuario>() {
            @Override
            protected void updateItem(RolUsuario r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : (r == RolUsuario.ADMINISTRADOR ? "Administradora" : "Vendedora"));
            }
        });
        cmbRol.setButtonCell(new ListCell<RolUsuario>() {
            @Override
            protected void updateItem(RolUsuario r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : (r == RolUsuario.ADMINISTRADOR ? "Administradora" : "Vendedora"));
            }
        });

        VBox content = new VBox(10);
        content.setPadding(new Insets(10, 10, 4, 10));
        content.getChildren().addAll(
                new Label("Nombre completo:"), txtNombre,
                new Label("Qué puede hacer (Rol):"), cmbRol
        );
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> res = dialog.showAndWait();
        if (!res.isPresent() || res.get() != btnGuardar) {
            return;
        }

        String nuevoNombre = txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        RolUsuario nuevoRol = cmbRol.getValue();
        if (nuevoNombre.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Nombre requerido", "El nombre del usuario no puede quedar vacío.");
            return;
        }

        // Regla de seguridad: no degradar a la sesion actual ni al ultimo administrador activo
        Usuario enSesion = SesionUsuario.getUsuarioLogueado();
        if (usuarioSeleccionado.esAdministrador() && nuevoRol != RolUsuario.ADMINISTRADOR) {
            if (enSesion != null && enSesion.getIdUsuario() == usuarioSeleccionado.getIdUsuario()) {
                AlertaUtil.mostrarAdvertencia("Acción no permitida",
                        "No puedes cambiar tu propio rol de Administradora mientras estás en sesión.");
                return;
            }
            try {
                if (usuarioSeleccionado.isActivo() && usuarioDAO.contarAdministradoresActivos() <= 1) {
                    AlertaUtil.mostrarAdvertencia("Acción no permitida",
                            "Debe existir al menos un usuario Administrador activo en el sistema.");
                    return;
                }
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Error de Base de Datos", e.getMessage());
                return;
            }
        }

        try {
            if (usuarioDAO.actualizarDatosBasicos(usuarioSeleccionado.getIdUsuario(), nuevoNombre, nuevoRol)) {
                if (enSesion != null && enSesion.getIdUsuario() == usuarioSeleccionado.getIdUsuario()) {
                    enSesion.setNombre(nuevoNombre);
                    configurarEncabezadoUsuario();
                }
                idUsuarioSeleccionarAlCargar = usuarioSeleccionado.getIdUsuario();
                cargarUsuariosDesdeMySQL();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error al Actualizar",
                    "No se pudieron guardar los cambios en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleDarBajaUsuario() {
        if (usuarioSeleccionado == null) {
            return;
        }

        if (!usuarioSeleccionado.isActivo()) {
            boolean ok = AlertaUtil.mostrarConfirmacion("Volver a activar usuario",
                    "¿Desea volver a activar la cuenta de «" + formatearNombreMostrar(usuarioSeleccionado) + "»?");
            if (!ok) {
                return;
            }
            try {
                if (usuarioDAO.cambiarEstado(usuarioSeleccionado.getIdUsuario(), true)) {
                    idUsuarioSeleccionarAlCargar = usuarioSeleccionado.getIdUsuario();
                    cargarUsuariosDesdeMySQL();
                }
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Error de Base de Datos",
                        "No se pudo activar el usuario en MySQL: " + e.getMessage());
            }
            return;
        }

        // Regla 1: No dar de baja al usuario en sesion actual
        Usuario enSesion = SesionUsuario.getUsuarioLogueado();
        if (enSesion != null && enSesion.getIdUsuario() == usuarioSeleccionado.getIdUsuario()) {
            AlertaUtil.mostrarAdvertencia("No se puede dar de baja",
                    "No puedes dar de baja tu propio usuario mientras estás usando el sistema.");
            return;
        }

        // Regla 2: No dar de baja al ultimo administrador activo
        if (usuarioSeleccionado.esAdministrador()) {
            try {
                if (usuarioDAO.contarAdministradoresActivos() <= 1) {
                    AlertaUtil.mostrarAdvertencia("Administrador único protegido",
                            "No puedes dar de baja al último administrador activo de la papelería.");
                    return;
                }
            } catch (SQLException e) {
                AlertaUtil.mostrarError("Error de Base de Datos", e.getMessage());
                return;
            }
        }

        boolean confirmar = AlertaUtil.mostrarConfirmacion("¿Dar de baja a este usuario?",
                "«" + formatearNombreMostrar(usuarioSeleccionado) + "» (" + usuarioSeleccionado.getUsername()
                        + ") ya no podrá iniciar sesión.\n\nSu historial de ventas y movimientos se conservará.");
        if (!confirmar) {
            return;
        }

        try {
            if (usuarioDAO.cambiarEstado(usuarioSeleccionado.getIdUsuario(), false)) {
                cargarUsuariosDesdeMySQL();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "No se pudo dar de baja al usuario en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleVolverAMasOpciones() {
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.cargarVistaEnContenido("/fxml/mas_opciones.fxml");
        }
    }

    private String formatearNombreMostrar(Usuario u) {
        if (u == null || u.getNombre() == null) {
            return "";
        }
        String raw = u.getNombre().trim();
        if (raw.toLowerCase(LOCALE_ES_CO).contains("rosita")
                && !raw.toLowerCase(LOCALE_ES_CO).startsWith("doña")) {
            return "Doña " + raw;
        }
        return raw;
    }
}