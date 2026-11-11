package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.model.Caja;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para control de turnos de caja y arqueos diarios.
 */
public class CajaDAO {

    /**
     * Retorna la sesion de caja actualmente abierta (sin fecha de cierre).
     * @return Objeto Caja abierta o null si no hay turno abierto.
     */
    public Caja obtenerCajaAbierta() throws SQLException {
        String sql = "SELECT id_caja, fecha_apertura, fecha_cierre, monto_inicial, total_tirilla, efectivo_contado, diferencia "
                   + "FROM caja WHERE fecha_cierre IS NULL ORDER BY id_caja DESC LIMIT 1";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return mapearCaja(rs);
            }
        }
        return null;
    }

    public Caja obtenerPorId(int idCaja) throws SQLException {
        String sql = "SELECT id_caja, fecha_apertura, fecha_cierre, monto_inicial, total_tirilla, efectivo_contado, diferencia "
                   + "FROM caja WHERE id_caja = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCaja);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCaja(rs);
                }
            }
        }
        return null;
    }

    /**
     * Abre un nuevo turno de caja con una base inicial.
     */
    public Caja abrirCaja(BigDecimal montoInicial) throws SQLException {
        Caja existente = obtenerCajaAbierta();
        if (existente != null) {
            throw new IllegalStateException("Ya existe un turno de caja abierto (Turno #" + existente.getIdCaja() + "). Debe cerrarlo primero.");
        }

        String sql = "INSERT INTO caja (fecha_apertura, monto_inicial, total_tirilla, efectivo_contado, diferencia) "
                   + "VALUES (NOW(), ?, 0.00, 0.00, 0.00)";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setBigDecimal(1, montoInicial != null ? montoInicial : BigDecimal.ZERO);

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        Caja caja = new Caja();
                        caja.setIdCaja(rs.getInt(1));
                        caja.setFechaApertura(LocalDateTime.now());
                        caja.setMontoInicial(montoInicial != null ? montoInicial : BigDecimal.ZERO);
                        return caja;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Calcula en tiempo real el efectivo teorico que deberia existir en el cajon (Tirilla):
     * Base inicial + Ventas al contado activas + Abonos de clientes - Pagos en efectivo a proveedores
     */
    public BigDecimal calcularTotalTirilla(int idCaja) throws SQLException {
        Caja caja = obtenerPorId(idCaja);
        if (caja == null) return BigDecimal.ZERO;

        BigDecimal base = caja.getMontoInicial() != null ? caja.getMontoInicial() : BigDecimal.ZERO;

        // 1. Ventas de contado del turno (no anuladas)
        BigDecimal ventasContado = BigDecimal.ZERO;
        String sqlVentas = "SELECT COALESCE(SUM(dv.subtotal), 0.00) "
                         + "FROM venta v "
                         + "INNER JOIN movimiento m ON v.id_movimiento = m.id_movimiento "
                         + "INNER JOIN detalle_venta dv ON v.id_movimiento = dv.id_movimiento "
                         + "WHERE m.id_caja = ? AND v.es_fiado = FALSE AND v.anulada = FALSE";

        // 2. Abonos de clientes en este turno
        BigDecimal abonos = BigDecimal.ZERO;
        String sqlAbonos = "SELECT COALESCE(SUM(ac.monto), 0.00) "
                         + "FROM abono_cliente ac "
                         + "INNER JOIN movimiento m ON ac.id_movimiento = m.id_movimiento "
                         + "WHERE m.id_caja = ?";

        // 3. Pagos a proveedores salidos de caja en este turno
        BigDecimal pagos = BigDecimal.ZERO;
        String sqlPagos = "SELECT COALESCE(SUM(pp.monto), 0.00) "
                        + "FROM pago_proveedor pp "
                        + "INNER JOIN movimiento m ON pp.id_movimiento = m.id_movimiento "
                        + "WHERE m.id_caja = ?";

        try (Connection conn = ConexionDB.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(sqlVentas)) {
                ps.setInt(1, idCaja);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) ventasContado = rs.getBigDecimal(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlAbonos)) {
                ps.setInt(1, idCaja);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) abonos = rs.getBigDecimal(1);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlPagos)) {
                ps.setInt(1, idCaja);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) pagos = rs.getBigDecimal(1);
                }
            }
        }

        return base.add(ventasContado).add(abonos).subtract(pagos);
    }

    /**
     * Cierra el turno de caja, registrando el efectivo contado fisico y calculando la diferencia.
     */
    public boolean cerrarCaja(int idCaja, BigDecimal efectivoContado) throws SQLException {
        BigDecimal totalTirilla = calcularTotalTirilla(idCaja);
        BigDecimal diferencia = (efectivoContado != null ? efectivoContado : BigDecimal.ZERO).subtract(totalTirilla);

        String sql = "UPDATE caja SET fecha_cierre = NOW(), total_tirilla = ?, efectivo_contado = ?, diferencia = ? "
                   + "WHERE id_caja = ? AND fecha_cierre IS NULL";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, totalTirilla);
            ps.setBigDecimal(2, efectivoContado != null ? efectivoContado : BigDecimal.ZERO);
            ps.setBigDecimal(3, diferencia);
            ps.setInt(4, idCaja);

            return ps.executeUpdate() > 0;
        }
    }

    public List<Caja> listarHistorial() throws SQLException {
        List<Caja> lista = new ArrayList<>();
        String sql = "SELECT id_caja, fecha_apertura, fecha_cierre, monto_inicial, total_tirilla, efectivo_contado, diferencia "
                   + "FROM caja ORDER BY id_caja DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearCaja(rs));
            }
        }
        return lista;
    }

    private Caja mapearCaja(ResultSet rs) throws SQLException {
        Caja c = new Caja();
        c.setIdCaja(rs.getInt("id_caja"));
        Timestamp tsApertura = rs.getTimestamp("fecha_apertura");
        if (tsApertura != null) {
            c.setFechaApertura(tsApertura.toLocalDateTime());
        }
        Timestamp tsCierre = rs.getTimestamp("fecha_cierre");
        if (tsCierre != null) {
            c.setFechaCierre(tsCierre.toLocalDateTime());
        }
        c.setMontoInicial(rs.getBigDecimal("monto_inicial"));
        c.setTotalTirilla(rs.getBigDecimal("total_tirilla"));
        c.setEfectivoContado(rs.getBigDecimal("efectivo_contado"));
        c.setDiferencia(rs.getBigDecimal("diferencia"));
        return c;
    }
}

