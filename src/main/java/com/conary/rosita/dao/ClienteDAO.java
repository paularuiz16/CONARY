package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.model.Cliente;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la gestion de clientes y cuentas por cobrar (fiados).
 * Integra la vista 'v_saldo_cliente'.
 */
public class ClienteDAO {

    public List<Cliente> listarTodos(boolean soloActivos) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, nombre, telefono, direccion, activo, fiado_autorizado, limite_credito, "
                   + "total_fiado, total_abonos, saldo_pendiente "
                   + "FROM v_saldo_cliente "
                   + (soloActivos ? "WHERE activo = TRUE " : "")
                   + "ORDER BY nombre ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearClienteConSaldo(rs));
            }
        }
        return lista;
    }

    public Cliente obtenerPorId(int idCliente) throws SQLException {
        String sql = "SELECT id_cliente, nombre, telefono, direccion, activo, fiado_autorizado, limite_credito, "
                   + "total_fiado, total_abonos, saldo_pendiente "
                   + "FROM v_saldo_cliente WHERE id_cliente = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearClienteConSaldo(rs);
                }
            }
        }
        return null;
    }

    public List<Cliente> buscar(String criterio, boolean soloActivos) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, nombre, telefono, direccion, activo, fiado_autorizado, limite_credito, "
                   + "total_fiado, total_abonos, saldo_pendiente "
                   + "FROM v_saldo_cliente "
                   + "WHERE (nombre LIKE ? OR telefono LIKE ?) "
                   + (soloActivos ? "AND activo = TRUE " : "")
                   + "ORDER BY nombre ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String term = "%" + (criterio != null ? criterio.trim() : "") + "%";
            ps.setString(1, term);
            ps.setString(2, term);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearClienteConSaldo(rs));
                }
            }
        }
        return lista;
    }

    public List<Cliente> listarConSaldosPendientes() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, nombre, telefono, direccion, activo, fiado_autorizado, limite_credito, "
                   + "total_fiado, total_abonos, saldo_pendiente "
                   + "FROM v_saldo_cliente "
                   + "WHERE saldo_pendiente > 0 AND activo = TRUE "
                   + "ORDER BY saldo_pendiente DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearClienteConSaldo(rs));
            }
        }
        return lista;
    }

    public boolean insertar(Cliente c) throws SQLException {
        String sql = "INSERT INTO cliente (nombre, telefono, direccion, activo, fiado_autorizado, limite_credito) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, c.getNombre().trim());
            ps.setString(2, c.getTelefono() != null ? c.getTelefono().trim() : null);
            ps.setString(3, c.getDireccion() != null ? c.getDireccion().trim() : null);
            ps.setBoolean(4, c.isActivo());
            ps.setBoolean(5, c.isFiadoAutorizado());
            ps.setBigDecimal(6, c.getLimiteCredito());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        c.setIdCliente(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean actualizar(Cliente c) throws SQLException {
        String sql = "UPDATE cliente SET nombre = ?, telefono = ?, direccion = ?, activo = ?, "
                   + "fiado_autorizado = ?, limite_credito = ? WHERE id_cliente = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getNombre().trim());
            ps.setString(2, c.getTelefono() != null ? c.getTelefono().trim() : null);
            ps.setString(3, c.getDireccion() != null ? c.getDireccion().trim() : null);
            ps.setBoolean(4, c.isActivo());
            ps.setBoolean(5, c.isFiadoAutorizado());
            ps.setBigDecimal(6, c.getLimiteCredito());
            ps.setInt(7, c.getIdCliente());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int idCliente, boolean activo) throws SQLException {
        String sql = "UPDATE cliente SET activo = ? WHERE id_cliente = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, activo);
            ps.setInt(2, idCliente);
            return ps.executeUpdate() > 0;
        }
    }

    private Cliente mapearClienteConSaldo(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setIdCliente(rs.getInt("id_cliente"));
        c.setNombre(rs.getString("nombre"));
        c.setTelefono(rs.getString("telefono"));
        c.setDireccion(rs.getString("direccion"));
        c.setActivo(rs.getBoolean("activo"));
        c.setFiadoAutorizado(rs.getBoolean("fiado_autorizado"));
        c.setLimiteCredito(rs.getBigDecimal("limite_credito"));
        c.setSaldoPendiente(rs.getBigDecimal("saldo_pendiente"));
        return c;
    }
}

