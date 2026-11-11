package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.model.PagoProveedor;
import com.conary.rosita.model.TipoMovimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para registro y consulta de pagos efectuados a proveedores.
 */
public class PagoProveedorDAO {

    public boolean registrarPago(PagoProveedor pago) throws SQLException {
        if (pago == null || pago.getMonto() == null || pago.getIdProveedor() <= 0) {
            throw new IllegalArgumentException("Datos de pago incompletos o invalidos.");
        }

        String sqlMovimiento = "INSERT INTO movimiento (fecha, descripcion, tipo, id_usuario, id_caja) VALUES (?, ?, ?, ?, ?)";
        String sqlPago = "INSERT INTO pago_proveedor (id_movimiento, id_proveedor, monto) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            int idMovimientoGenerado = 0;

            // 1. Insertar movimiento
            try (PreparedStatement psMov = conn.prepareStatement(sqlMovimiento, Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime ahora = pago.getFecha() != null ? pago.getFecha() : LocalDateTime.now();
                psMov.setTimestamp(1, Timestamp.valueOf(ahora));
                psMov.setString(2, pago.getDescripcion() != null ? pago.getDescripcion() : "Pago a proveedor");
                psMov.setString(3, TipoMovimiento.PAGO_PROVEEDOR.name());
                psMov.setInt(4, pago.getIdUsuario());
                if (pago.getIdCaja() != null) {
                    psMov.setInt(5, pago.getIdCaja());
                } else {
                    psMov.setNull(5, Types.INTEGER);
                }

                psMov.executeUpdate();
                try (ResultSet rs = psMov.getGeneratedKeys()) {
                    if (rs.next()) {
                        idMovimientoGenerado = rs.getInt(1);
                        pago.setIdMovimiento(idMovimientoGenerado);
                    } else {
                        throw new SQLException("No se genero ID para el movimiento de pago a proveedor.");
                    }
                }
            }

            // 2. Insertar pago_proveedor
            try (PreparedStatement psPago = conn.prepareStatement(sqlPago)) {
                psPago.setInt(1, idMovimientoGenerado);
                psPago.setInt(2, pago.getIdProveedor());
                psPago.setBigDecimal(3, pago.getMonto());
                psPago.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[PagoProveedorDAO] Error en rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[PagoProveedorDAO] Error al cerrar conexion: " + e.getMessage());
                }
            }
        }
    }

    public List<PagoProveedor> listarPagosPorProveedor(int idProveedor) throws SQLException {
        List<PagoProveedor> lista = new ArrayList<>();
        String sql = "SELECT m.id_movimiento, m.fecha, m.descripcion, m.id_usuario, u.nombre AS nombre_usuario, "
                   + "m.id_caja, pp.id_proveedor, p.nombre AS nombre_proveedor, pp.monto "
                   + "FROM pago_proveedor pp "
                   + "INNER JOIN movimiento m ON pp.id_movimiento = m.id_movimiento "
                   + "INNER JOIN usuario u ON m.id_usuario = u.id_usuario "
                   + "INNER JOIN proveedor p ON pp.id_proveedor = p.id_proveedor "
                   + "WHERE pp.id_proveedor = ? ORDER BY m.id_movimiento DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idProveedor);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PagoProveedor pag = new PagoProveedor();
                    pag.setIdMovimiento(rs.getInt("id_movimiento"));
                    Timestamp ts = rs.getTimestamp("fecha");
                    if (ts != null) {
                        pag.setFecha(ts.toLocalDateTime());
                    }
                    pag.setDescripcion(rs.getString("descripcion"));
                    pag.setIdUsuario(rs.getInt("id_usuario"));
                    pag.setNombreUsuario(rs.getString("nombre_usuario"));
                    int idCaja = rs.getInt("id_caja");
                    if (!rs.wasNull()) {
                        pag.setIdCaja(idCaja);
                    }
                    pag.setIdProveedor(rs.getInt("id_proveedor"));
                    pag.setNombreProveedor(rs.getString("nombre_proveedor"));
                    pag.setMonto(rs.getBigDecimal("monto"));
                    lista.add(pag);
                }
            }
        }
        return lista;
    }
}

