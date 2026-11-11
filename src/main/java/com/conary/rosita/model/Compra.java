package com.conary.rosita.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una compra a proveedor.
 * Hereda de {@link Movimiento}.
 */
public class Compra extends Movimiento {

    private int idProveedor;
    private String numeroFacturaProveedor;
    private LocalDate fechaPagoAcordada;

    // Lista de items de la compra
    private List<DetalleCompra> detalles;

    // Auxiliares para visualizacion en UI / vistas
    private String nombreProveedor;
    private BigDecimal totalCompra;

    public Compra() {
        super();
        setTipo(TipoMovimiento.COMPRA);
        this.detalles = new ArrayList<>();
        this.totalCompra = BigDecimal.ZERO;
    }

    public Compra(int idMovimiento, LocalDateTime fecha, String descripcion, int idUsuario, Integer idCaja,
                  int idProveedor, String numeroFacturaProveedor, LocalDate fechaPagoAcordada) {
        super(idMovimiento, fecha, descripcion, TipoMovimiento.COMPRA, idUsuario, idCaja);
        this.idProveedor = idProveedor;
        this.numeroFacturaProveedor = numeroFacturaProveedor;
        this.fechaPagoAcordada = fechaPagoAcordada;
        this.detalles = new ArrayList<>();
        this.totalCompra = BigDecimal.ZERO;
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public String getNumeroFacturaProveedor() {
        return numeroFacturaProveedor;
    }

    public void setNumeroFacturaProveedor(String numeroFacturaProveedor) {
        this.numeroFacturaProveedor = numeroFacturaProveedor;
    }

    public LocalDate getFechaPagoAcordada() {
        return fechaPagoAcordada;
    }

    public void setFechaPagoAcordada(LocalDate fechaPagoAcordada) {
        this.fechaPagoAcordada = fechaPagoAcordada;
    }

    public List<DetalleCompra> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompra> detalles) {
        this.detalles = detalles != null ? detalles : new ArrayList<>();
        calcularTotal();
    }

    public void agregarDetalle(DetalleCompra detalle) {
        if (detalle != null) {
            this.detalles.add(detalle);
            calcularTotal();
        }
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    public BigDecimal getTotalCompra() {
        return totalCompra;
    }

    public void setTotalCompra(BigDecimal totalCompra) {
        this.totalCompra = totalCompra != null ? totalCompra : BigDecimal.ZERO;
    }

    public BigDecimal calcularTotal() {
        BigDecimal suma = BigDecimal.ZERO;
        if (detalles != null) {
            for (DetalleCompra dc : detalles) {
                if (dc != null && dc.getSubtotal() != null) {
                    suma = suma.add(dc.getSubtotal());
                }
            }
        }
        this.totalCompra = suma;
        return this.totalCompra;
    }

    @Override
    public String toString() {
        return "Compra #" + getIdMovimiento() + " [" + (nombreProveedor != null ? nombreProveedor : "Prov #" + idProveedor) + "] - Total: $" + totalCompra;
    }
}

