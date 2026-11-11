package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.model.AbonoCliente;
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
 * DAO para registro y consulta de abonos a cuentas por cobrar (fiados).
 */
public class AbonoClienteDAO {

    public boolean registrarAbono(AbonoCliente abono) throws SQLException {
        if (abono == null || abono.getMonto() == null || abono.getIdCliente() <= 0) {
            throw new IllegalArgumentException("Datos de abono incompletos o invalidos.");
        }

        String sqlMovimiento = "INSERT INTO movimiento (fecha, descripcion, tipo, id_usuario, id_caja) VALUES (?, ?, ?, ?, ?)";
        String sqlAbono = "INSERT INTO abono_cliente (id_movimiento, id_cliente, monto) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            int idMovimientoGenerado = 0;

            // 1. Insertar movimiento
            try (PreparedStatement psMov = conn.prepareStatement(sqlMovimiento, Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime ahora = abono.getFecha() != null ? abono.getFecha() : LocalDateTime.now();
                psMov.setTimestamp(1, Timestamp.valueOf(ahora));
                psMov.setString(2, abono.getDescripcion() != null ? abono.getDescripcion() : "Abono de cliente");
                psMov.setString(3, TipoMovimiento.ABONO_CLIENTE.name());
                psMov.setInt(4, abono.getIdUsuario());
                if (abono.getIdCaja() != null) {
                    psMov.setInt(5, abono.getIdCaja());
                } else {
                    psMov.setNull(5, Types.INTEGER);
                }

                psMov.executeUpdate();
                try (ResultSet rs = psMov.getGeneratedKeys()) {
                    if (rs.next()) {
                        idMovimientoGenerado = rs.getInt(1);
                        abono.setIdMovimiento(idMovimientoGenerado);
                    } else {
                        throw new SQLException("No se genero ID para el movimiento de abono.");
                    }
                }
            }

            // 2. Insertar abono_cliente
            try (PreparedStatement psAbono = conn.prepareStatement(sqlAbono)) {
                psAbono.setInt(1, idMovimientoGenerado);
                psAbono.setInt(2, abono.getIdCliente());
                psAbono.setBigDecimal(3, abono.getMonto());
                psAbono.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[AbonoClienteDAO] Error en rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[AbonoClienteDAO] Error al cerrar conexion: " + e.getMessage());
                }
            }
        }
    }

    public List<AbonoCliente> listarAbonosPorCliente(int idCliente) throws SQLException {
        List<AbonoCliente> lista = new ArrayList<>();
        String sql = "SELECT m.id_movimiento, m.fecha, m.descripcion, m.id_usuario, u.nombre AS nombre_usuario, "
                   + "m.id_caja, ac.id_cliente, c.nombre AS nombre_cliente, ac.monto "
                   + "FROM abono_cliente ac "
                   + "INNER JOIN movimiento m ON ac.id_movimiento = m.id_movimiento "
                   + "INNER JOIN usuario u ON m.id_usuario = u.id_usuario "
                   + "INNER JOIN cliente c ON ac.id_cliente = c.id_cliente "
                   + "WHERE ac.id_cliente = ? ORDER BY m.id_movimiento DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AbonoCliente a = new AbonoCliente();
                    a.setIdMovimiento(rs.getInt("id_movimiento"));
                    Timestamp ts = rs.getTimestamp("fecha");
                    if (ts != null) {
                        a.setFecha(ts.toLocalDateTime());
                    }
                    a.setDescripcion(rs.getString("descripcion"));
                    a.setIdUsuario(rs.getInt("id_usuario"));
                    a.setNombreUsuario(rs.getString("nombre_usuario"));
                    int idCaja = rs.getInt("id_caja");
                    if (!rs.wasNull()) {
                        a.setIdCaja(idCaja);
                    }
                    a.setIdCliente(rs.getInt("id_cliente"));
                    a.setNombreCliente(rs.getString("nombre_cliente"));
                    a.setMonto(rs.getBigDecimal("monto"));
                    lista.add(a);
                }
            }
        }
        return lista;
    }
}

