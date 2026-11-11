package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.model.DetalleVenta;
import com.conary.rosita.model.TipoMovimiento;
import com.conary.rosita.model.Venta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para registro atomico y trazabilidad de ventas.
 * Manejo transaccional puro con JDBC (ACID) y reversión de inventario.
 */
public class VentaDAO {

    private final ProductoDAO productoDAO = new ProductoDAO();

    /**
     * Registra una venta completa con sus detalles y disminuye el stock en una sola transaccion ACID.
     */
    public boolean registrarVenta(Venta venta) throws SQLException {
        if (venta == null || venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La venta no contiene productos.");
        }

        String sqlMovimiento = "INSERT INTO movimiento (fecha, descripcion, tipo, id_usuario, id_caja) VALUES (?, ?, ?, ?, ?)";
        String sqlVenta = "INSERT INTO venta (id_movimiento, id_cliente, es_fiado, anulada, motivo_anulacion, importada) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_venta (id_movimiento, id_producto, cantidad, valor_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Iniciar transaccion atomica

            int idMovimientoGenerado = 0;

            // 1. Insertar movimiento
            try (PreparedStatement psMov = conn.prepareStatement(sqlMovimiento, Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime ahora = venta.getFecha() != null ? venta.getFecha() : LocalDateTime.now();
                psMov.setTimestamp(1, Timestamp.valueOf(ahora));
                psMov.setString(2, venta.getDescripcion() != null ? venta.getDescripcion() : "Venta en mostrador");
                psMov.setString(3, TipoMovimiento.VENTA.name());
                psMov.setInt(4, venta.getIdUsuario());
                if (venta.getIdCaja() != null) {
                    psMov.setInt(5, venta.getIdCaja());
                } else {
                    psMov.setNull(5, Types.INTEGER);
                }

                psMov.executeUpdate();
                try (ResultSet rs = psMov.getGeneratedKeys()) {
                    if (rs.next()) {
                        idMovimientoGenerado = rs.getInt(1);
                        venta.setIdMovimiento(idMovimientoGenerado);
                    } else {
                        throw new SQLException("No se genero ID para el movimiento de venta.");
                    }
                }
            }

            // 2. Insertar venta
            try (PreparedStatement psVenta = conn.prepareStatement(sqlVenta)) {
                psVenta.setInt(1, idMovimientoGenerado);
                if (venta.getIdCliente() != null && venta.getIdCliente() > 0) {
                    psVenta.setInt(2, venta.getIdCliente());
                } else {
                    psVenta.setNull(2, Types.INTEGER);
                }
                psVenta.setBoolean(3, venta.isEsFiado());
                psVenta.setBoolean(4, false); // anulada = false
                psVenta.setString(5, null);
                psVenta.setBoolean(6, venta.isImportada());

                psVenta.executeUpdate();
            }

            // 3. Insertar detalles y descontar stock
            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS)) {
                for (DetalleVenta item : venta.getDetalles()) {
                    psDetalle.setInt(1, idMovimientoGenerado);
                    psDetalle.setInt(2, item.getIdProducto());
                    psDetalle.setInt(3, item.getCantidad());
                    psDetalle.setBigDecimal(4, item.getValorUnitario());
                    psDetalle.setBigDecimal(5, item.getSubtotal());
                    psDetalle.executeUpdate();

                    try (ResultSet rs = psDetalle.getGeneratedKeys()) {
                        if (rs.next()) {
                            item.setIdDetalle(rs.getInt(1));
                        }
                    }

                    // Disminuir stock (-cantidad) de forma atomica
                    productoDAO.modificarStockTransaccional(item.getIdProducto(), -item.getCantidad(), conn);
                }
            }

            conn.commit(); // Confirmar cambios si todo salio perfecto
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Revertir en caso de cualquier error
                } catch (SQLException ex) {
                    System.err.println("[VentaDAO] Error al hacer rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[VentaDAO] Error al cerrar conexion: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Anula una venta previamente realizada y devuelve los productos al stock del inventario.
     */
    public boolean anularVenta(int idMovimiento, String motivoAnulacion) throws SQLException {
        Venta v = obtenerVentaPorId(idMovimiento);
        if (v == null) {
            throw new IllegalArgumentException("La venta no existe.");
        }
        if (v.isAnulada()) {
            throw new IllegalStateException("La venta #" + idMovimiento + " ya se encuentra anulada.");
        }

        List<DetalleVenta> detalles = obtenerDetallesVenta(idMovimiento);

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            // 1. Restaurar stock de cada producto (+cantidad)
            for (DetalleVenta item : detalles) {
                productoDAO.modificarStockTransaccional(item.getIdProducto(), item.getCantidad(), conn);
            }

            // 2. Marcar venta como anulada
            String sqlAnular = "UPDATE venta SET anulada = TRUE, motivo_anulacion = ? WHERE id_movimiento = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlAnular)) {
                ps.setString(1, motivoAnulacion != null ? motivoAnulacion.trim() : "Anulacion manual");
                ps.setInt(2, idMovimiento);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[VentaDAO] Error al hacer rollback en anulacion: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[VentaDAO] Error cerrando conexion: " + e.getMessage());
                }
            }
        }
    }

    public Venta obtenerVentaPorId(int idMovimiento) throws SQLException {
        String sql = "SELECT id_movimiento, fecha, descripcion, id_usuario, nombre_usuario, id_caja, "
                   + "id_cliente, nombre_cliente, es_fiado, anulada, motivo_anulacion, importada, total_venta "
                   + "FROM v_total_venta WHERE id_movimiento = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idMovimiento);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Venta v = mapearVentaResumen(rs);
                    v.setDetalles(obtenerDetallesVenta(idMovimiento));
                    return v;
                }
            }
        }
        return null;
    }

    public List<Venta> listarVentas(LocalDate desde, LocalDate hasta, Boolean soloFiados) throws SQLException {
        List<Venta> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT id_movimiento, fecha, descripcion, id_usuario, nombre_usuario, id_caja, "
          + "id_cliente, nombre_cliente, es_fiado, anulada, motivo_anulacion, importada, total_venta "
          + "FROM v_total_venta WHERE 1=1 "
        );

        if (desde != null) {
            sql.append("AND fecha >= ? ");
        }
        if (hasta != null) {
            sql.append("AND fecha <= ? ");
        }
        if (soloFiados != null) {
            sql.append("AND es_fiado = ? ");
        }
        sql.append("ORDER BY id_movimiento DESC");

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (desde != null) {
                ps.setTimestamp(paramIndex++, Timestamp.valueOf(desde.atStartOfDay()));
            }
            if (hasta != null) {
                ps.setTimestamp(paramIndex++, Timestamp.valueOf(hasta.atTime(LocalTime.MAX)));
            }
            if (soloFiados != null) {
                ps.setBoolean(paramIndex++, soloFiados);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearVentaResumen(rs));
                }
            }
        }
        return lista;
    }

    public List<DetalleVenta> obtenerDetallesVenta(int idMovimiento) throws SQLException {
        List<DetalleVenta> lista = new ArrayList<>();
        String sql = "SELECT dv.id_detalle, dv.id_movimiento, dv.id_producto, p.nombre AS nombre_producto, "
                   + "dv.cantidad, dv.valor_unitario, dv.subtotal "
                   + "FROM detalle_venta dv "
                   + "INNER JOIN producto p ON dv.id_producto = p.id_producto "
                   + "WHERE dv.id_movimiento = ? ORDER BY dv.id_detalle ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idMovimiento);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleVenta d = new DetalleVenta();
                    d.setIdDetalle(rs.getInt("id_detalle"));
                    d.setIdMovimiento(rs.getInt("id_movimiento"));
                    d.setIdProducto(rs.getInt("id_producto"));
                    d.setNombreProducto(rs.getString("nombre_producto"));
                    d.setCantidad(rs.getInt("cantidad"));
                    d.setValorUnitario(rs.getBigDecimal("valor_unitario"));
                    d.setSubtotal(rs.getBigDecimal("subtotal"));
                    lista.add(d);
                }
            }
        }
        return lista;
    }

    private Venta mapearVentaResumen(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setIdMovimiento(rs.getInt("id_movimiento"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) {
            v.setFecha(ts.toLocalDateTime());
        }
        v.setDescripcion(rs.getString("descripcion"));
        v.setIdUsuario(rs.getInt("id_usuario"));
        v.setNombreUsuario(rs.getString("nombre_usuario"));
        int idCaja = rs.getInt("id_caja");
        if (!rs.wasNull()) {
            v.setIdCaja(idCaja);
        }
        int idCliente = rs.getInt("id_cliente");
        if (!rs.wasNull()) {
            v.setIdCliente(idCliente);
        }
        v.setNombreCliente(rs.getString("nombre_cliente"));
        v.setEsFiado(rs.getBoolean("es_fiado"));
        v.setAnulada(rs.getBoolean("anulada"));
        v.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        v.setImportada(rs.getBoolean("importada"));
        v.setTotalVenta(rs.getBigDecimal("total_venta"));
        return v;
    }
}

