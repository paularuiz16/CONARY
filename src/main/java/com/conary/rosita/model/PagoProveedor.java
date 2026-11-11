package com.conary.rosita.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa un pago efectuado a un proveedor para amortizar facturas de compras.
 * Hereda de {@link Movimiento}.
 */
public class PagoProveedor extends Movimiento {

    private int idProveedor;
    private BigDecimal monto;

    // Auxiliar para la interfaz de usuario
    private String nombreProveedor;

    public PagoProveedor() {
        super();
        setTipo(TipoMovimiento.PAGO_PROVEEDOR);
        this.monto = BigDecimal.ZERO;
    }

    public PagoProveedor(int idMovimiento, LocalDateTime fecha, String descripcion, int idUsuario,
                         Integer idCaja, int idProveedor, BigDecimal monto) {
        super(idMovimiento, fecha, descripcion, TipoMovimiento.PAGO_PROVEEDOR, idUsuario, idCaja);
        this.idProveedor = idProveedor;
        this.monto = monto != null ? monto : BigDecimal.ZERO;
    }

    public PagoProveedor(String descripcion, int idUsuario, Integer idCaja, int idProveedor, BigDecimal monto) {
        this(0, LocalDateTime.now(), descripcion, idUsuario, idCaja, idProveedor, monto);
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto != null ? monto : BigDecimal.ZERO;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public void setNombreProveedor(String nombreProveedor) {
        this.nombreProveedor = nombreProveedor;
    }

    @Override
    public String toString() {
        return "Pago Proveedor #" + getIdMovimiento() + " [" + (nombreProveedor != null ? nombreProveedor : "Prov #" + idProveedor) + "] - Monto: $" + monto;
    }
}

