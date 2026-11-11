package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.model.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la gestion de inventario y catalogo de productos.
 * JDBC puro para maximo rendimiento en 2 GB de RAM.
 */
public class ProductoDAO {

    public List<Producto> listarTodos(boolean soloActivos) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT id_producto, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo, activo "
                   + "FROM producto "
                   + (soloActivos ? "WHERE activo = TRUE " : "")
                   + "ORDER BY nombre ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }
        }
        return lista;
    }

    public Producto obtenerPorId(int idProducto) throws SQLException {
        String sql = "SELECT id_producto, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo, activo "
                   + "FROM producto WHERE id_producto = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idProducto);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearProducto(rs);
                }
            }
        }
        return null;
    }

    public List<Producto> buscar(String criterio, boolean soloActivos) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT id_producto, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo, activo "
                   + "FROM producto "
                   + "WHERE (nombre LIKE ? OR categoria LIKE ?) "
                   + (soloActivos ? "AND activo = TRUE " : "")
                   + "ORDER BY nombre ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String term = "%" + (criterio != null ? criterio.trim() : "") + "%";
            ps.setString(1, term);
            ps.setString(2, term);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearProducto(rs));
                }
            }
        }
        return lista;
    }

    public List<Producto> listarStockBajo() throws SQLException {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT id_producto, nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo, activo "
                   + "FROM producto "
                   + "WHERE stock_actual <= stock_minimo AND activo = TRUE "
                   + "ORDER BY stock_actual ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearProducto(rs));
            }
        }
        return lista;
    }

    public boolean insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO producto (nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo, activo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getNombre().trim());
            ps.setString(2, p.getCategoria() != null ? p.getCategoria().trim() : "GENERAL");
            ps.setBigDecimal(3, p.getPrecioCompra());
            ps.setBigDecimal(4, p.getPrecioVenta());
            ps.setInt(5, p.getStockActual());
            ps.setInt(6, p.getStockMinimo());
            ps.setBoolean(7, p.isActivo());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setIdProducto(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET nombre = ?, categoria = ?, precio_compra = ?, precio_venta = ?, "
                   + "stock_actual = ?, stock_minimo = ?, activo = ? WHERE id_producto = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNombre().trim());
            ps.setString(2, p.getCategoria() != null ? p.getCategoria().trim() : "GENERAL");
            ps.setBigDecimal(3, p.getPrecioCompra());
            ps.setBigDecimal(4, p.getPrecioVenta());
            ps.setInt(5, p.getStockActual());
            ps.setInt(6, p.getStockMinimo());
            ps.setBoolean(7, p.isActivo());
            ps.setInt(8, p.getIdProducto());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Modifica el stock de forma atomica dentro de una transaccion de venta o compra.
     * @param idProducto Identificador del producto.
     * @param delta Cantidad a sumar (positivo para compras o devoluciones, negativo para ventas).
     * @param conn Conexion activa con transaccion manual.
     */
    public boolean modificarStockTransaccional(int idProducto, int delta, Connection conn) throws SQLException {
        String sql = "UPDATE producto SET stock_actual = stock_actual + (?) WHERE id_producto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, idProducto);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int idProducto, boolean activo) throws SQLException {
        String sql = "UPDATE producto SET activo = ? WHERE id_producto = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, activo);
            ps.setInt(2, idProducto);
            return ps.executeUpdate() > 0;
        }
    }

    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("id_producto"));
        p.setNombre(rs.getString("nombre"));
        p.setCategoria(rs.getString("categoria"));
        p.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setActivo(rs.getBoolean("activo"));
        return p;
    }
}

