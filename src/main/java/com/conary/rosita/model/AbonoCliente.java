package com.conary.rosita.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que representa un abono de un cliente para amortizar su saldo pendiente fiado.
 * Hereda de {@link Movimiento}.
 */
public class AbonoCliente extends Movimiento {

    private int idCliente;
    private BigDecimal monto;

    // Auxiliar para la interfaz de usuario
    private String nombreCliente;

    public AbonoCliente() {
        super();
        setTipo(TipoMovimiento.ABONO_CLIENTE);
        this.monto = BigDecimal.ZERO;
    }

    public AbonoCliente(int idMovimiento, LocalDateTime fecha, String descripcion, int idUsuario,
                        Integer idCaja, int idCliente, BigDecimal monto) {
        super(idMovimiento, fecha, descripcion, TipoMovimiento.ABONO_CLIENTE, idUsuario, idCaja);
        this.idCliente = idCliente;
        this.monto = monto != null ? monto : BigDecimal.ZERO;
    }

    public AbonoCliente(String descripcion, int idUsuario, Integer idCaja, int idCliente, BigDecimal monto) {
        this(0, LocalDateTime.now(), descripcion, idUsuario, idCaja, idCliente, monto);
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto != null ? monto : BigDecimal.ZERO;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    @Override
    public String toString() {
        return "Abono #" + getIdMovimiento() + " [" + (nombreCliente != null ? nombreCliente : "Cliente #" + idCliente) + "] - Monto: $" + monto;
    }
}

