package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.model.Proveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la gestion de proveedores y cuentas por pagar.
 * Integra la vista 'v_saldo_proveedor'.
 */
public class ProveedorDAO {

    public List<Proveedor> listarTodos(boolean soloActivos) throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT p.id_proveedor, p.nombre, p.telefono, p.direccion, p.productos, p.activo, "
                   + "COALESCE(vp.saldo_pendiente, 0.00) AS saldo_pendiente "
                   + "FROM proveedor p "
                   + "LEFT JOIN v_saldo_proveedor vp ON p.id_proveedor = vp.id_proveedor "
                   + (soloActivos ? "WHERE p.activo = TRUE " : "")
                   + "ORDER BY p.nombre ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearProveedor(rs));
            }
        }
        return lista;
    }

    public Proveedor obtenerPorId(int idProveedor) throws SQLException {
        String sql = "SELECT p.id_proveedor, p.nombre, p.telefono, p.direccion, p.productos, p.activo, "
                   + "COALESCE(vp.saldo_pendiente, 0.00) AS saldo_pendiente "
                   + "FROM proveedor p "
                   + "LEFT JOIN v_saldo_proveedor vp ON p.id_proveedor = vp.id_proveedor "
                   + "WHERE p.id_proveedor = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idProveedor);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearProveedor(rs);
                }
            }
        }
        return null;
    }

    public List<Proveedor> listarConSaldosPendientes() throws SQLException {
        List<Proveedor> lista = new ArrayList<>();
        String sql = "SELECT p.id_proveedor, p.nombre, p.telefono, p.direccion, p.productos, p.activo, "
                   + "vp.saldo_pendiente "
                   + "FROM proveedor p "
                   + "INNER JOIN v_saldo_proveedor vp ON p.id_proveedor = vp.id_proveedor "
                   + "WHERE vp.saldo_pendiente > 0 AND p.activo = TRUE "
                   + "ORDER BY vp.saldo_pendiente DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearProveedor(rs));
            }
        }
        return lista;
    }

    public boolean insertar(Proveedor p) throws SQLException {
        String sql = "INSERT INTO proveedor (nombre, telefono, direccion, productos, activo) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getNombre().trim());
            ps.setString(2, p.getTelefono() != null ? p.getTelefono().trim() : null);
            ps.setString(3, p.getDireccion() != null ? p.getDireccion().trim() : null);
            ps.setString(4, p.getProductos() != null ? p.getProductos().trim() : null);
            ps.setBoolean(5, p.isActivo());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        p.setIdProveedor(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean actualizar(Proveedor p) throws SQLException {
        String sql = "UPDATE proveedor SET nombre = ?, telefono = ?, direccion = ?, productos = ?, activo = ? "
                   + "WHERE id_proveedor = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNombre().trim());
            ps.setString(2, p.getTelefono() != null ? p.getTelefono().trim() : null);
            ps.setString(3, p.getDireccion() != null ? p.getDireccion().trim() : null);
            ps.setString(4, p.getProductos() != null ? p.getProductos().trim() : null);
            ps.setBoolean(5, p.isActivo());
            ps.setInt(6, p.getIdProveedor());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int idProveedor, boolean activo) throws SQLException {
        String sql = "UPDATE proveedor SET activo = ? WHERE id_proveedor = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, activo);
            ps.setInt(2, idProveedor);
            return ps.executeUpdate() > 0;
        }
    }

    private Proveedor mapearProveedor(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setIdProveedor(rs.getInt("id_proveedor"));
        p.setNombre(rs.getString("nombre"));
        p.setTelefono(rs.getString("telefono"));
        p.setDireccion(rs.getString("direccion"));
        p.setProductos(rs.getString("productos"));
        p.setActivo(rs.getBoolean("activo"));
        p.setSaldoPendiente(rs.getBigDecimal("saldo_pendiente"));
        return p;
    }
}

