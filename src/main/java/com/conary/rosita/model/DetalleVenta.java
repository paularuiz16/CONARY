package com.conary.rosita.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad que representa una linea de producto dentro de una venta.
 */
public class DetalleVenta implements Serializable {

    private int idDetalle;
    private int idMovimiento;
    private int idProducto;
    private int cantidad;
    private BigDecimal valorUnitario;
    private BigDecimal subtotal;

    // Auxiliar para interfaz de usuario
    private String nombreProducto;

    public DetalleVenta() {
        this.cantidad = 1;
        this.valorUnitario = BigDecimal.ZERO;
        this.subtotal = BigDecimal.ZERO;
    }

    public DetalleVenta(int idDetalle, int idMovimiento, int idProducto, int cantidad, BigDecimal valorUnitario, BigDecimal subtotal) {
        this.idDetalle = idDetalle;
        this.idMovimiento = idMovimiento;
        this.idProducto = idProducto;
        this.cantidad = cantidad;
        this.valorUnitario = valorUnitario != null ? valorUnitario : BigDecimal.ZERO;
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public DetalleVenta(int idProducto, String nombreProducto, int cantidad, BigDecimal valorUnitario) {
        this(0, 0, idProducto, cantidad, valorUnitario, valorUnitario.multiply(BigDecimal.valueOf(cantidad)));
        this.nombreProducto = nombreProducto;
    }

    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        recalcularSubtotal();
    }

    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    public void setValorUnitario(BigDecimal valorUnitario) {
        this.valorUnitario = valorUnitario != null ? valorUnitario : BigDecimal.ZERO;
        recalcularSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public void recalcularSubtotal() {
        if (this.valorUnitario != null) {
            this.subtotal = this.valorUnitario.multiply(BigDecimal.valueOf(this.cantidad));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetalleVenta that = (DetalleVenta) o;
        return idDetalle == that.idDetalle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idDetalle);
    }

    @Override
    public String toString() {
        return (nombreProducto != null ? nombreProducto : "Prod #" + idProducto) + " x" + cantidad + " ($" + subtotal + ")";
    }
}

