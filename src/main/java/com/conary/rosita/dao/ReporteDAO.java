package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para consultas agregadas del Modulo 6: Reportes del Negocio.
 * Compatible estrictamente con Java SE 8 y el esquema MySQL de CONARY.
 */
public class ReporteDAO {

    public static class ResumenVentas {
        private final int cantidadVentasValidas;
        private final int cantidadVentasAnuladas;
        private final BigDecimal totalVendido;
        private final BigDecimal totalContado;
        private final BigDecimal totalFiado;

        public ResumenVentas(int cantidadVentasValidas, int cantidadVentasAnuladas,
                             BigDecimal totalVendido, BigDecimal totalContado, BigDecimal totalFiado) {
            this.cantidadVentasValidas = cantidadVentasValidas;
            this.cantidadVentasAnuladas = cantidadVentasAnuladas;
            this.totalVendido = totalVendido != null ? totalVendido : BigDecimal.ZERO;
            this.totalContado = totalContado != null ? totalContado : BigDecimal.ZERO;
            this.totalFiado = totalFiado != null ? totalFiado : BigDecimal.ZERO;
        }

        public int getCantidadVentasValidas() { return cantidadVentasValidas; }
        public int getCantidadVentasAnuladas() { return cantidadVentasAnuladas; }
        public BigDecimal getTotalVendido() { return totalVendido; }
        public BigDecimal getTotalContado() { return totalContado; }
        public BigDecimal getTotalFiado() { return totalFiado; }
    }

    public static class FilaVentaPorDia {
        private final LocalDate dia;
        private final int cantidadVentas;
        private final BigDecimal totalContado;
        private final BigDecimal totalFiado;
        private final BigDecimal total;

        public FilaVentaPorDia(LocalDate dia, int cantidadVentas,
                               BigDecimal totalContado, BigDecimal totalFiado, BigDecimal total) {
            this.dia = dia;
            this.cantidadVentas = cantidadVentas;
            this.totalContado = totalContado != null ? totalContado : BigDecimal.ZERO;
            this.totalFiado = totalFiado != null ? totalFiado : BigDecimal.ZERO;
            this.total = total != null ? total : BigDecimal.ZERO;
        }

        public LocalDate getDia() { return dia; }
        public int getCantidadVentas() { return cantidadVentas; }
        public BigDecimal getTotalContado() { return totalContado; }
        public BigDecimal getTotalFiado() { return totalFiado; }
        public BigDecimal getTotal() { return total; }
    }

    public static class FilaVentaPorProducto {
        private final String nombreProducto;
        private final int unidades;
        private final BigDecimal totalContado;
        private final BigDecimal totalFiado;
        private final BigDecimal total;

        public FilaVentaPorProducto(String nombreProducto, int unidades,
                                    BigDecimal totalContado, BigDecimal totalFiado, BigDecimal total) {
            this.nombreProducto = nombreProducto != null ? nombreProducto : "Producto";
            this.unidades = unidades;
            this.totalContado = totalContado != null ? totalContado : BigDecimal.ZERO;
            this.totalFiado = totalFiado != null ? totalFiado : BigDecimal.ZERO;
            this.total = total != null ? total : BigDecimal.ZERO;
        }

        public String getNombreProducto() { return nombreProducto; }
        public int getUnidades() { return unidades; }
        public BigDecimal getTotalContado() { return totalContado; }
        public BigDecimal getTotalFiado() { return totalFiado; }
        public BigDecimal getTotal() { return total; }
    }

    public static class FilaCierreCajaReporte {
        private final int idCaja;
        private final LocalDate fecha;
        private final String responsable;
        private final BigDecimal baseInicial;
        private final BigDecimal ventasContado;
        private final BigDecimal esperado;
        private final BigDecimal contado;
        private final BigDecimal diferencia;

        public FilaCierreCajaReporte(int idCaja, LocalDate fecha, String responsable,
                                     BigDecimal baseInicial, BigDecimal ventasContado,
                                     BigDecimal esperado, BigDecimal contado, BigDecimal diferencia) {
            this.idCaja = idCaja;
            this.fecha = fecha;
            this.responsable = responsable != null && !responsable.trim().isEmpty() ? responsable.trim() : "Doña Rosita";
            this.baseInicial = baseInicial != null ? baseInicial : BigDecimal.ZERO;
            this.ventasContado = ventasContado != null ? ventasContado : BigDecimal.ZERO;
            this.esperado = esperado != null ? esperado : BigDecimal.ZERO;
            this.contado = contado != null ? contado : BigDecimal.ZERO;
            this.diferencia = diferencia != null ? diferencia : BigDecimal.ZERO;
        }

        public int getIdCaja() { return idCaja; }
        public LocalDate getFecha() { return fecha; }
        public String getResponsable() { return responsable; }
        public BigDecimal getBaseInicial() { return baseInicial; }
        public BigDecimal getVentasContado() { return ventasContado; }
        public BigDecimal getEsperado() { return esperado; }
        public BigDecimal getContado() { return contado; }
        public BigDecimal getDiferencia() { return diferencia; }
    }

    public ResumenVentas obtenerResumenVentas(LocalDate desde, LocalDate hasta) throws SQLException {
        String sql = "SELECT "
                   + "COUNT(CASE WHEN anulada = FALSE THEN 1 END) AS ventas_validas, "
                   + "COUNT(CASE WHEN anulada = TRUE THEN 1 END) AS ventas_anuladas, "
                   + "COALESCE(SUM(CASE WHEN anulada = FALSE THEN total_venta ELSE 0 END), 0.00) AS total_vendido, "
                   + "COALESCE(SUM(CASE WHEN anulada = FALSE AND es_fiado = FALSE THEN total_venta ELSE 0 END), 0.00) AS total_contado, "
                   + "COALESCE(SUM(CASE WHEN anulada = FALSE AND es_fiado = TRUE THEN total_venta ELSE 0 END), 0.00) AS total_fiado "
                   + "FROM v_total_venta "
                   + "WHERE fecha >= ? AND fecha <= ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(LocalTime.MAX)));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ResumenVentas(
                            rs.getInt("ventas_validas"),
                            rs.getInt("ventas_anuladas"),
                            rs.getBigDecimal("total_vendido"),
                            rs.getBigDecimal("total_contado"),
                            rs.getBigDecimal("total_fiado")
                    );
                }
            }
        }
        return new ResumenVentas(0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public List<FilaVentaPorDia> listarVentasPorDia(LocalDate desde, LocalDate hasta) throws SQLException {
        List<FilaVentaPorDia> lista = new ArrayList<FilaVentaPorDia>();
        String sql = "SELECT "
                   + "DATE(fecha) AS dia, "
                   + "COUNT(*) AS cantidad_ventas, "
                   + "COALESCE(SUM(CASE WHEN es_fiado = FALSE THEN total_venta ELSE 0 END), 0.00) AS total_contado, "
                   + "COALESCE(SUM(CASE WHEN es_fiado = TRUE THEN total_venta ELSE 0 END), 0.00) AS total_fiado, "
                   + "COALESCE(SUM(total_venta), 0.00) AS total_dia "
                   + "FROM v_total_venta "
                   + "WHERE anulada = FALSE AND fecha >= ? AND fecha <= ? "
                   + "GROUP BY DATE(fecha) "
                   + "ORDER BY DATE(fecha) DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(LocalTime.MAX)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("dia");
                    lista.add(new FilaVentaPorDia(
                            d != null ? d.toLocalDate() : desde,
                            rs.getInt("cantidad_ventas"),
                            rs.getBigDecimal("total_contado"),
                            rs.getBigDecimal("total_fiado"),
                            rs.getBigDecimal("total_dia")
                    ));
                }
            }
        }
        return lista;
    }

    public List<FilaVentaPorProducto> listarVentasPorProducto(LocalDate desde, LocalDate hasta) throws SQLException {
        List<FilaVentaPorProducto> lista = new ArrayList<FilaVentaPorProducto>();
        String sql = "SELECT "
                   + "p.nombre AS nombre_producto, "
                   + "COALESCE(SUM(dv.cantidad), 0) AS unidades, "
                   + "COALESCE(SUM(CASE WHEN v.es_fiado = FALSE THEN dv.subtotal ELSE 0 END), 0.00) AS total_contado, "
                   + "COALESCE(SUM(CASE WHEN v.es_fiado = TRUE THEN dv.subtotal ELSE 0 END), 0.00) AS total_fiado, "
                   + "COALESCE(SUM(dv.subtotal), 0.00) AS total_producto "
                   + "FROM detalle_venta dv "
                   + "INNER JOIN venta v ON dv.id_movimiento = v.id_movimiento "
                   + "INNER JOIN movimiento m ON v.id_movimiento = m.id_movimiento "
                   + "INNER JOIN producto p ON dv.id_producto = p.id_producto "
                   + "WHERE v.anulada = FALSE AND m.fecha >= ? AND m.fecha <= ? "
                   + "GROUP BY p.id_producto, p.nombre "
                   + "ORDER BY total_producto DESC, unidades DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(LocalTime.MAX)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new FilaVentaPorProducto(
                            rs.getString("nombre_producto"),
                            rs.getInt("unidades"),
                            rs.getBigDecimal("total_contado"),
                            rs.getBigDecimal("total_fiado"),
                            rs.getBigDecimal("total_producto")
                    ));
                }
            }
        }
        return lista;
    }

    public List<FilaCierreCajaReporte> listarCierresCaja(LocalDate desde, LocalDate hasta) throws SQLException {
        List<FilaCierreCajaReporte> lista = new ArrayList<FilaCierreCajaReporte>();
        String sql = "SELECT c.id_caja, DATE(COALESCE(c.fecha_cierre, c.fecha_apertura)) AS fecha_turno, "
                   + "c.monto_inicial, c.total_tirilla, c.efectivo_contado, c.diferencia, "
                   + "COALESCE((SELECT u.nombre FROM movimiento m INNER JOIN usuario u ON m.id_usuario = u.id_usuario "
                   + "          WHERE m.id_caja = c.id_caja ORDER BY m.id_movimiento DESC LIMIT 1), 'Doña Rosita') AS responsable, "
                   + "COALESCE((SELECT SUM(vt.total_venta) FROM v_total_venta vt "
                   + "          WHERE vt.id_caja = c.id_caja AND vt.es_fiado = FALSE AND vt.anulada = FALSE), 0.00) AS ventas_contado "
                   + "FROM caja c "
                   + "WHERE c.fecha_cierre IS NOT NULL "
                   + "AND c.fecha_apertura >= ? AND c.fecha_apertura <= ? "
                   + "ORDER BY c.id_caja DESC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(desde.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(hasta.atTime(LocalTime.MAX)));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate("fecha_turno");
                    lista.add(new FilaCierreCajaReporte(
                            rs.getInt("id_caja"),
                            d != null ? d.toLocalDate() : desde,
                            rs.getString("responsable"),
                            rs.getBigDecimal("monto_inicial"),
                            rs.getBigDecimal("ventas_contado"),
                            rs.getBigDecimal("total_tirilla"),
                            rs.getBigDecimal("efectivo_contado"),
                            rs.getBigDecimal("diferencia")
                    ));
                }
            }
        }
        return lista;
    }
}