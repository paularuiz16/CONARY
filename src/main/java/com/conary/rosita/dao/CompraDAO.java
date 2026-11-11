package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.model.Compra;
import com.conary.rosita.model.DetalleCompra;
import com.conary.rosita.model.TipoMovimiento;

import java.sql.Connection;
import java.sql.Date;
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
 * DAO para registro de compras de mercancia y entrada a inventario.
 */
public class CompraDAO {

    private final ProductoDAO productoDAO = new ProductoDAO();

    public boolean registrarCompra(Compra compra) throws SQLException {
        if (compra == null || compra.getDetalles() == null || compra.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La compra no tiene productos asignados.");
        }

        String sqlMovimiento = "INSERT INTO movimiento (fecha, descripcion, tipo, id_usuario, id_caja) VALUES (?, ?, ?, ?, ?)";
        String sqlCompra = "INSERT INTO compra (id_movimiento, id_proveedor, numero_factura_proveedor, fecha_pago_acordada) VALUES (?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_compra (id_movimiento, id_producto, cantidad, valor_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";
        String sqlActualizarPrecioCompra = "UPDATE producto SET precio_compra = ? WHERE id_producto = ?";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Transaccion atomica

            int idMovimientoGenerado = 0;

            // 1. Insertar movimiento
            try (PreparedStatement psMov = conn.prepareStatement(sqlMovimiento, Statement.RETURN_GENERATED_KEYS)) {
                LocalDateTime ahora = compra.getFecha() != null ? compra.getFecha() : LocalDateTime.now();
                psMov.setTimestamp(1, Timestamp.valueOf(ahora));
                psMov.setString(2, compra.getDescripcion() != null ? compra.getDescripcion() : "Compra de mercancia");
                psMov.setString(3, TipoMovimiento.COMPRA.name());
                psMov.setInt(4, compra.getIdUsuario());
                if (compra.getIdCaja() != null) {
                    psMov.setInt(5, compra.getIdCaja());
                } else {
                    psMov.setNull(5, Types.INTEGER);
                }

                psMov.executeUpdate();
                try (ResultSet rs = psMov.getGeneratedKeys()) {
                    if (rs.next()) {
                        idMovimientoGenerado = rs.getInt(1);
                        compra.setIdMovimiento(idMovimientoGenerado);
                    } else {
                        throw new SQLException("No se pudo obtener el ID del movimiento de compra.");
                    }
                }
            }

            // 2. Insertar compra
            try (PreparedStatement psCompra = conn.prepareStatement(sqlCompra)) {
                psCompra.setInt(1, idMovimientoGenerado);
                psCompra.setInt(2, compra.getIdProveedor());
                psCompra.setString(3, compra.getNumeroFacturaProveedor());
                if (compra.getFechaPagoAcordada() != null) {
                    psCompra.setDate(4, Date.valueOf(compra.getFechaPagoAcordada()));
                } else {
                    psCompra.setNull(4, Types.DATE);
                }

                psCompra.executeUpdate();
            }

            // 3. Insertar detalles y sumar stock (+cantidad) y refrescar precio de compra
            try (PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psPrecio = conn.prepareStatement(sqlActualizarPrecioCompra)) {

                for (DetalleCompra item : compra.getDetalles()) {
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

                    // Aumentar stock (+cantidad)
                    productoDAO.modificarStockTransaccional(item.getIdProducto(), item.getCantidad(), conn);

                    // Actualizar ultimo precio de compra
                    if (item.getValorUnitario() != null) {
                        psPrecio.setBigDecimal(1, item.getValorUnitario());
                        psPrecio.setInt(2, item.getIdProducto());
                        psPrecio.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[CompraDAO] Error al hacer rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[CompraDAO] Error al cerrar conexion: " + e.getMessage());
                }
            }
        }
    }

    public Compra obtenerCompraPorId(int idMovimiento) throws SQLException {
        String sql = "SELECT id_movimiento, fecha, descripcion, id_usuario, nombre_usuario, id_caja, "
                   + "id_proveedor, nombre_proveedor, numero_factura_proveedor, fecha_pago_acordada, total_compra "
                   + "FROM v_total_compra WHERE id_movimiento = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idMovimiento);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Compra c = mapearCompraResumen(rs);
                    c.setDetalles(obtenerDetallesCompra(idMovimiento));
                    return c;
                }
            }
        }
        return null;
    }

    public List<Compra> listarCompras(LocalDate desde, LocalDate hasta) throws SQLException {
        List<Compra> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT id_movimiento, fecha, descripcion, id_usuario, nombre_usuario, id_caja, "
          + "id_proveedor, nombre_proveedor, numero_factura_proveedor, fecha_pago_acordada, total_compra "
          + "FROM v_total_compra WHERE 1=1 "
        );

        if (desde != null) {
            sql.append("AND fecha >= ? ");
        }
        if (hasta != null) {
            sql.append("AND fecha <= ? ");
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

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCompraResumen(rs));
                }
            }
        }
        return lista;
    }

    public List<DetalleCompra> obtenerDetallesCompra(int idMovimiento) throws SQLException {
        List<DetalleCompra> lista = new ArrayList<>();
        String sql = "SELECT dc.id_detalle, dc.id_movimiento, dc.id_producto, p.nombre AS nombre_producto, "
                   + "dc.cantidad, dc.valor_unitario, dc.subtotal "
                   + "FROM detalle_compra dc "
                   + "INNER JOIN producto p ON dc.id_producto = p.id_producto "
                   + "WHERE dc.id_movimiento = ? ORDER BY dc.id_detalle ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idMovimiento);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleCompra d = new DetalleCompra();
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

    private Compra mapearCompraResumen(ResultSet rs) throws SQLException {
        Compra c = new Compra();
        c.setIdMovimiento(rs.getInt("id_movimiento"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) {
            c.setFecha(ts.toLocalDateTime());
        }
        c.setDescripcion(rs.getString("descripcion"));
        c.setIdUsuario(rs.getInt("id_usuario"));
        c.setNombreUsuario(rs.getString("nombre_usuario"));
        int idCaja = rs.getInt("id_caja");
        if (!rs.wasNull()) {
            c.setIdCaja(idCaja);
        }
        c.setIdProveedor(rs.getInt("id_proveedor"));
        c.setNombreProveedor(rs.getString("nombre_proveedor"));
        c.setNumeroFacturaProveedor(rs.getString("numero_factura_proveedor"));
        Date dPago = rs.getDate("fecha_pago_acordada");
        if (dPago != null) {
            c.setFechaPagoAcordada(dPago.toLocalDate());
        }
        c.setTotalCompra(rs.getBigDecimal("total_compra"));
        return c;
    }
}

