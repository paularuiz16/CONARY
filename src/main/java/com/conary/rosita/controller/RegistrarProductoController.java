package com.conary.rosita.controller;

import com.conary.rosita.dao.ProductoDAO;
import com.conary.rosita.model.Producto;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.AlertaUtil;
import com.conary.rosita.util.SesionUsuario;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Controlador para la vista Registrar producto (Modulo 4 — Productos y precios).
 * Compatible estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class RegistrarProductoController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");

    @FXML private Button btnVolverProductos;
    @FXML private Label lblUsuarioHeader;

    @FXML private HBox boxAlertaDuplicado;
    @FXML private Label lblAlertaDuplicado;

    @FXML private TextField txtNombre;

    @FXML private ToggleButton btnCatCuadernos;
    @FXML private ToggleButton btnCatEscritura;
    @FXML private ToggleButton btnCatPapel;
    @FXML private ToggleButton btnCatOficina;
    @FXML private ToggleButton btnCatColores;
    @FXML private ToggleButton btnCatOtros;

    @FXML private Label lblAyudaCodigo;

    @FXML private TextField txtPrecioCompra;
    @FXML private TextField txtPrecioVenta;
    @FXML private TextField txtExistencias;
    @FXML private TextField txtStockMinimo;

    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final ToggleGroup tgCategoria = new ToggleGroup();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final List<Producto> productosExistentes = new ArrayList<Producto>();
    private List<ToggleButton> botonesCategoria;
    private boolean modoModal = false;
    private Producto productoCreadoModal;

    public void setModoModal(boolean modoModal) {
        this.modoModal = modoModal;
        if (modoModal && btnVolverProductos != null) {
            btnVolverProductos.setText("←   Volver a la compra");
        }
    }

    public Producto getProductoCreadoModal() {
        return productoCreadoModal;
    }

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarSelectorCategorias();
        cargarProductosExistentesYProximoCodigo();
        configurarValidacionEnTiempoReal();
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

    private void configurarSelectorCategorias() {
        botonesCategoria = Arrays.asList(
                btnCatCuadernos,
                btnCatEscritura,
                btnCatPapel,
                btnCatOficina,
                btnCatColores,
                btnCatOtros
        );

        if (btnCatCuadernos != null) btnCatCuadernos.setUserData("Cuadernos");
        if (btnCatEscritura != null) btnCatEscritura.setUserData("Escritura");
        if (btnCatPapel != null) btnCatPapel.setUserData("Papel");
        if (btnCatOficina != null) btnCatOficina.setUserData("Oficina");
        if (btnCatColores != null) btnCatColores.setUserData("Colores");
        if (btnCatOtros != null) btnCatOtros.setUserData("Otros");

        for (ToggleButton btn : botonesCategoria) {
            if (btn != null) {
                btn.setToggleGroup(tgCategoria);
            }
        }
        if (btnCatCuadernos != null) {
            btnCatCuadernos.setSelected(true);
        }

        tgCategoria.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                // Evita que quede ninguna categoria seleccionada
                oldToggle.setSelected(true);
                return;
            }
            actualizarTextosCategorias();
        });
        actualizarTextosCategorias();
    }

    private void actualizarTextosCategorias() {
        if (botonesCategoria == null) return;
        for (ToggleButton btn : botonesCategoria) {
            if (btn == null || btn.getUserData() == null) continue;
            String nombreBase = String.valueOf(btn.getUserData());
            if (btn.isSelected()) {
                btn.setText("\u2713  " + nombreBase);
            } else {
                btn.setText(nombreBase);
            }
        }
    }

    private String obtenerCategoriaSeleccionada() {
        if (tgCategoria != null && tgCategoria.getSelectedToggle() != null) {
            Toggle seleccionado = tgCategoria.getSelectedToggle();
            if (seleccionado.getUserData() != null) {
                return String.valueOf(seleccionado.getUserData());
            }
        }
        return "Cuadernos";
    }

    private void cargarProductosExistentesYProximoCodigo() {
        productosExistentes.clear();
        int maxId = 0;
        try {
            List<Producto> lista = productoDAO.listarTodos(false);
            if (lista != null) {
                productosExistentes.addAll(lista);
                for (Producto p : lista) {
                    if (p.getIdProducto() > maxId) {
                        maxId = p.getIdProducto();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[RegistrarProductoController] Error consultando productos existentes: " + e.getMessage());
        }

        String proximoCodigo = formatearCodigo(maxId + 1);
        if (lblAyudaCodigo != null) {
            lblAyudaCodigo.setText("El sistema le dará un código automático como " + proximoCodigo);
        }
    }

    private void configurarValidacionEnTiempoReal() {
        if (txtNombre != null) {
            txtNombre.textProperty().addListener((obs, oldVal, newVal) -> {
                Producto duplicado = buscarDuplicadoEnCache(newVal);
                if (duplicado != null) {
                    mostrarBannerDuplicado(duplicado);
                } else {
                    ocultarBannerDuplicado();
                }
            });
        }
    }

    private Producto buscarDuplicadoEnCache(String nombreCandidato) {
        if (nombreCandidato == null || nombreCandidato.trim().isEmpty()) {
            return null;
        }
        String buscado = nombreCandidato.trim();
        for (Producto p : productosExistentes) {
            if (p.getNombre() != null && p.getNombre().trim().equalsIgnoreCase(buscado)) {
                return p;
            }
        }
        return null;
    }

    private Producto buscarDuplicadoEnMySQL(String nombreCandidato) throws SQLException {
        if (nombreCandidato == null || nombreCandidato.trim().isEmpty()) {
            return null;
        }
        String buscado = nombreCandidato.trim();
        List<Producto> todos = productoDAO.listarTodos(false);
        productosExistentes.clear();
        if (todos != null) {
            productosExistentes.addAll(todos);
            for (Producto p : todos) {
                if (p.getNombre() != null && p.getNombre().trim().equalsIgnoreCase(buscado)) {
                    return p;
                }
            }
        }
        return null;
    }

    private void mostrarBannerDuplicado(Producto existente) {
        if (boxAlertaDuplicado == null || lblAlertaDuplicado == null || existente == null) {
            return;
        }
        String codigoReal = formatearCodigo(existente.getIdProducto());
        lblAlertaDuplicado.setText("Ya existe un producto con este nombre: "
                + existente.getNombre() + " (" + codigoReal + "). Cambie el nombre o vuelva a la lista.");
        boxAlertaDuplicado.setVisible(true);
        boxAlertaDuplicado.setManaged(true);
    }

    private void ocultarBannerDuplicado() {
        if (boxAlertaDuplicado != null) {
            boxAlertaDuplicado.setVisible(false);
            boxAlertaDuplicado.setManaged(false);
        }
    }

    @FXML
    public void handleGuardarProducto() {
        String nombre = txtNombre != null && txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String strPrecioCompra = txtPrecioCompra != null && txtPrecioCompra.getText() != null ? txtPrecioCompra.getText().trim() : "";
        String strPrecioVenta = txtPrecioVenta != null && txtPrecioVenta.getText() != null ? txtPrecioVenta.getText().trim() : "";
        String strExistencias = txtExistencias != null && txtExistencias.getText() != null ? txtExistencias.getText().trim() : "";
        String strStockMinimo = txtStockMinimo != null && txtStockMinimo.getText() != null ? txtStockMinimo.getText().trim() : "";

        // 1. Validar que los campos requeridos no esten vacios
        if (nombre.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Falta el nombre",
                    "Por favor escriba el nombre del producto antes de guardar.");
            if (txtNombre != null) txtNombre.requestFocus();
            return;
        }
        if (strPrecioVenta.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Falta el precio de venta",
                    "Por favor ingrese el precio de venta al cliente.");
            if (txtPrecioVenta != null) txtPrecioVenta.requestFocus();
            return;
        }
        if (strExistencias.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Faltan las existencias",
                    "Por favor indique cuántas unidades hay hoy del producto.");
            if (txtExistencias != null) txtExistencias.requestFocus();
            return;
        }
        if (strStockMinimo.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Falta el aviso de existencias",
                    "Por favor indique con cuántas unidades desea que el sistema le avise.");
            if (txtStockMinimo != null) txtStockMinimo.requestFocus();
            return;
        }

        // 2. Validar valores numericos
        BigDecimal precioCompra = BigDecimal.ZERO;
        if (!strPrecioCompra.isEmpty()) {
            precioCompra = parsearMontoColombiano(strPrecioCompra);
            if (precioCompra == null || precioCompra.compareTo(BigDecimal.ZERO) < 0) {
                AlertaUtil.mostrarAdvertencia("Precio de compra inválido",
                        "Ingrese un número válido mayor o igual a 0 en el precio de compra.");
                if (txtPrecioCompra != null) txtPrecioCompra.requestFocus();
                return;
            }
        }

        BigDecimal precioVenta = parsearMontoColombiano(strPrecioVenta);
        if (precioVenta == null || precioVenta.compareTo(BigDecimal.ZERO) <= 0) {
            AlertaUtil.mostrarAdvertencia("Precio de venta inválido",
                    "El precio de venta debe ser un número mayor a 0.");
            if (txtPrecioVenta != null) txtPrecioVenta.requestFocus();
            return;
        }

        Integer existencias = parsearEntero(strExistencias);
        if (existencias == null || existencias.intValue() < 0) {
            AlertaUtil.mostrarAdvertencia("Existencias inválidas",
                    "Las existencias actuales deben ser un número entero mayor o igual a 0.");
            if (txtExistencias != null) txtExistencias.requestFocus();
            return;
        }

        Integer stockMinimo = parsearEntero(strStockMinimo);
        if (stockMinimo == null || stockMinimo.intValue() < 0) {
            AlertaUtil.mostrarAdvertencia("Cantidad de aviso inválida",
                    "El campo 'Avisar cuando queden' debe ser un número entero mayor o igual a 0.");
            if (txtStockMinimo != null) txtStockMinimo.requestFocus();
            return;
        }

        // 3. Verificar duplicado en MySQL antes del INSERT
        try {
            Producto duplicado = buscarDuplicadoEnMySQL(nombre);
            if (duplicado != null) {
                mostrarBannerDuplicado(duplicado);
                if (txtNombre != null) txtNombre.requestFocus();
                return;
            }

            ocultarBannerDuplicado();

            // 4. Insertar producto nuevo en MySQL
            String categoria = obtenerCategoriaSeleccionada();
            Producto nuevo = new Producto(
                    0,
                    nombre,
                    categoria,
                    precioCompra,
                    precioVenta,
                    existencias.intValue(),
                    stockMinimo.intValue(),
                    true
            );

            boolean insertado = productoDAO.insertar(nuevo);
            if (insertado) {
                productoCreadoModal = nuevo;
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                }
                if (modoModal) {
                    cerrarSiEsModal();
                } else if (menu != null) {
                    menu.mostrarInventario();
                }
            } else {
                AlertaUtil.mostrarError("No se pudo guardar",
                        "La base de datos no confirmó el registro del producto.");
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "Ocurrió un problema al guardar el producto en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        if (modoModal) {
            productoCreadoModal = null;
            cerrarSiEsModal();
            return;
        }
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarInventario();
        }
    }

    private void cerrarSiEsModal() {
        if (btnCancelar != null && btnCancelar.getScene() != null
                && btnCancelar.getScene().getWindow() instanceof javafx.stage.Stage) {
            ((javafx.stage.Stage) btnCancelar.getScene().getWindow()).close();
        }
    }

    private BigDecimal parsearMontoColombiano(String texto) {
        if (texto == null) return null;
        String limpio = texto.replace("$", "").replace(" ", "").trim();
        if (limpio.isEmpty()) return null;
        // Si contiene puntos de miles como "3.200" o "18.500", los removemos
        limpio = limpio.replace(".", "").replace(",", ".");
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parsearEntero(String texto) {
        if (texto == null) return null;
        String limpio = texto.replace(".", "").replace(" ", "").trim();
        if (limpio.isEmpty()) return null;
        try {
            return Integer.valueOf(Integer.parseInt(limpio));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatearCodigo(int idProducto) {
        return String.format("PRD-%03d", Integer.valueOf(idProducto));
    }
}
