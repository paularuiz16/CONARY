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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Controlador para la vista Modificar producto (Modulo 4 — Productos y precios).
 * Cumple estrictamente con Java SE 8 y JavaFX 8 nativo.
 */
public class ModificarProductoController {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static Producto productoPendienteEdicion;

    public static void setProductoPendienteEdicion(Producto producto) {
        productoPendienteEdicion = producto;
    }

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

    @FXML private Label lblCodigoProducto;

    @FXML private TextField txtPrecioCompra;
    @FXML private TextField txtPrecioVenta;
    @FXML private Label lblExistenciasActuales;
    @FXML private TextField txtStockMinimo;

    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final ToggleGroup tgCategoria = new ToggleGroup();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final List<Producto> productosExistentes = new ArrayList<Producto>();
    private List<ToggleButton> botonesCategoria;
    private Producto productoActual;

    @FXML
    public void initialize() {
        configurarEncabezadoUsuario();
        configurarSelectorCategorias();
        cargarProductosExistentes();
        configurarValidacionEnTiempoReal();

        if (productoPendienteEdicion != null) {
            Producto temp = productoPendienteEdicion;
            productoPendienteEdicion = null;
            setProducto(temp);
        }
    }

    /**
     * Recibe el producto seleccionado desde ProductosController y rellena todos los campos.
     */
    public void setProducto(Producto producto) {
        if (producto == null) {
            return;
        }
        this.productoActual = producto;

        if (txtNombre != null) {
            txtNombre.setText(producto.getNombre() != null ? producto.getNombre() : "");
        }
        seleccionarBotonCategoria(producto.getCategoria());

        if (lblCodigoProducto != null) {
            lblCodigoProducto.setText(formatearCodigo(producto.getIdProducto()));
        }
        if (txtPrecioCompra != null) {
            txtPrecioCompra.setText(formatearMoneda(producto.getPrecioCompra()));
        }
        if (txtPrecioVenta != null) {
            txtPrecioVenta.setText(formatearMoneda(producto.getPrecioVenta()));
        }
        if (lblExistenciasActuales != null) {
            int stock = producto.getStockActual();
            lblExistenciasActuales.setText(stock + (stock == 1 ? "\nunidad" : "\nunidades"));
        }
        if (txtStockMinimo != null) {
            txtStockMinimo.setText(String.valueOf(producto.getStockMinimo()));
        }

        ocultarBannerDuplicado();
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

        tgCategoria.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
                return;
            }
            actualizarTextosCategorias();
        });
        actualizarTextosCategorias();
    }

    private void seleccionarBotonCategoria(String categoriaRaw) {
        if (botonesCategoria == null) return;
        String cat = categoriaRaw != null ? categoriaRaw.trim() : "";

        ToggleButton encontrado = null;
        for (ToggleButton btn : botonesCategoria) {
            if (btn != null && btn.getUserData() != null) {
                String nombreBoton = String.valueOf(btn.getUserData());
                if (nombreBoton.equalsIgnoreCase(cat)) {
                    encontrado = btn;
                    break;
                }
            }
        }
        if (encontrado == null) {
            encontrado = btnCatOtros != null ? btnCatOtros : btnCatCuadernos;
        }
        if (encontrado != null) {
            encontrado.setSelected(true);
        }
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
        if (tgCategoria.getSelectedToggle() != null) {
            Toggle seleccionado = tgCategoria.getSelectedToggle();
            if (seleccionado.getUserData() != null) {
                return String.valueOf(seleccionado.getUserData());
            }
        }
        return "Otros";
    }

    private void cargarProductosExistentes() {
        productosExistentes.clear();
        try {
            List<Producto> lista = productoDAO.listarTodos(false);
            if (lista != null) {
                productosExistentes.addAll(lista);
            }
        } catch (SQLException e) {
            System.err.println("[ModificarProductoController] Error consultando productos: " + e.getMessage());
        }
    }

    private void configurarValidacionEnTiempoReal() {
        if (txtNombre != null) {
            txtNombre.textProperty().addListener((obs, oldVal, newVal) -> {
                Producto conflicto = buscarOtroProductoConMismoNombreEnCache(newVal);
                if (conflicto != null) {
                    mostrarBannerDuplicado(conflicto);
                } else {
                    ocultarBannerDuplicado();
                }
            });
        }
    }

    private Producto buscarOtroProductoConMismoNombreEnCache(String nombreCandidato) {
        if (nombreCandidato == null || nombreCandidato.trim().isEmpty()) {
            return null;
        }
        String buscado = nombreCandidato.trim();
        int idActual = productoActual != null ? productoActual.getIdProducto() : -1;

        for (Producto p : productosExistentes) {
            if (p.getIdProducto() != idActual
                    && p.getNombre() != null
                    && p.getNombre().trim().equalsIgnoreCase(buscado)) {
                return p;
            }
        }
        return null;
    }

    private Producto buscarOtroProductoConMismoNombreEnMySQL(String nombreCandidato) throws SQLException {
        if (nombreCandidato == null || nombreCandidato.trim().isEmpty()) {
            return null;
        }
        String buscado = nombreCandidato.trim();
        int idActual = productoActual != null ? productoActual.getIdProducto() : -1;

        List<Producto> todos = productoDAO.listarTodos(false);
        productosExistentes.clear();
        if (todos != null) {
            productosExistentes.addAll(todos);
            for (Producto p : todos) {
                if (p.getIdProducto() != idActual
                        && p.getNombre() != null
                        && p.getNombre().trim().equalsIgnoreCase(buscado)) {
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
    public void handleGuardarCambios() {
        if (productoActual == null) {
            AlertaUtil.mostrarError("Sin producto seleccionado",
                    "No se encontró el producto a modificar. Vuelva a la lista e inténtelo de nuevo.");
            return;
        }

        String nombre = txtNombre != null && txtNombre.getText() != null ? txtNombre.getText().trim() : "";
        String strPrecioCompra = txtPrecioCompra != null && txtPrecioCompra.getText() != null ? txtPrecioCompra.getText().trim() : "";
        String strPrecioVenta = txtPrecioVenta != null && txtPrecioVenta.getText() != null ? txtPrecioVenta.getText().trim() : "";
        String strStockMinimo = txtStockMinimo != null && txtStockMinimo.getText() != null ? txtStockMinimo.getText().trim() : "";

        // 1. Validar que Nombre, Precio de venta y Avisar cuando queden no esten vacios
        if (nombre.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Falta el nombre",
                    "Por favor escriba el nombre del producto antes de guardar los cambios.");
            if (txtNombre != null) txtNombre.requestFocus();
            return;
        }
        if (strPrecioVenta.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Falta el precio de venta",
                    "Por favor ingrese el precio de venta al cliente.");
            if (txtPrecioVenta != null) txtPrecioVenta.requestFocus();
            return;
        }
        if (strStockMinimo.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Falta el aviso de existencias",
                    "Por favor indique con cuántas unidades desea que el sistema le avise.");
            if (txtStockMinimo != null) txtStockMinimo.requestFocus();
            return;
        }

        // 2. Validar valores numericos (Precio de compra >= 0, Precio de venta > 0, Stock minimo >= 0)
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

        Integer stockMinimo = parsearEntero(strStockMinimo);
        if (stockMinimo == null || stockMinimo.intValue() < 0) {
            AlertaUtil.mostrarAdvertencia("Cantidad de aviso inválida",
                    "El campo 'Avisar cuando queden' debe ser un número entero mayor o igual a 0.");
            if (txtStockMinimo != null) txtStockMinimo.requestFocus();
            return;
        }

        // 3. Verificar en MySQL que el nombre no choque con otro producto distinto (id != productoActual.getIdProducto())
        try {
            Producto conflicto = buscarOtroProductoConMismoNombreEnMySQL(nombre);
            if (conflicto != null) {
                mostrarBannerDuplicado(conflicto);
                if (txtNombre != null) txtNombre.requestFocus();
                return;
            }

            ocultarBannerDuplicado();

            // 4. Regla de integridad: preservar codigo (id_producto), existencias (stock_actual) y estado (activo) reales de BD
            Producto desdeBD = productoDAO.obtenerPorId(productoActual.getIdProducto());
            int stockActualIntacto = desdeBD != null ? desdeBD.getStockActual() : productoActual.getStockActual();
            boolean activoIntacto = desdeBD != null ? desdeBD.isActivo() : productoActual.isActivo();

            Producto actualizado = new Producto(
                    productoActual.getIdProducto(),
                    nombre,
                    obtenerCategoriaSeleccionada(),
                    precioCompra,
                    precioVenta,
                    stockActualIntacto,
                    stockMinimo.intValue(),
                    activoIntacto
            );

            boolean exito = productoDAO.actualizar(actualizado);
            if (exito) {
                ProductosController.setIdProductoSeleccionarAlCargar(productoActual.getIdProducto());
                MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
                if (menu != null) {
                    menu.refrescarAlertasReales();
                    menu.mostrarInventario();
                }
            } else {
                AlertaUtil.mostrarError("No se pudo actualizar",
                        "La base de datos no confirmó los cambios del producto.");
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarError("Error de Base de Datos",
                    "Ocurrió un problema al guardar los cambios en MySQL: " + e.getMessage());
        }
    }

    @FXML
    public void handleCancelar() {
        if (productoActual != null) {
            ProductosController.setIdProductoSeleccionarAlCargar(productoActual.getIdProducto());
        }
        MenuPrincipalController menu = MenuPrincipalController.getInstanciaActiva();
        if (menu != null) {
            menu.mostrarInventario();
        }
    }

    private BigDecimal parsearMontoColombiano(String texto) {
        if (texto == null) return null;
        String limpio = texto.replace("$", "").replace(" ", "").trim();
        if (limpio.isEmpty()) return null;
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

    private String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(monto);
    }
}
