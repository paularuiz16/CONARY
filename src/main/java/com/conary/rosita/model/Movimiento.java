package com.conary.rosita.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad base que representa un movimiento financiero en el sistema
 * (Venta, Compra, Abono de Cliente o Pago a Proveedor).
 */
public class Movimiento implements Serializable {

    private int idMovimiento;
    private LocalDateTime fecha;
    private String descripcion;
    private TipoMovimiento tipo;
    private int idUsuario;
    private Integer idCaja;

    // Campos auxiliares para visualizacion en UI
    private String nombreUsuario;

    public Movimiento() {
        this.fecha = LocalDateTime.now();
        this.tipo = TipoMovimiento.VENTA;
    }

    public Movimiento(int idMovimiento, LocalDateTime fecha, String descripcion,
                      TipoMovimiento tipo, int idUsuario, Integer idCaja) {
        this.idMovimiento = idMovimiento;
        this.fecha = fecha != null ? fecha : LocalDateTime.now();
        this.descripcion = descripcion;
        this.tipo = tipo != null ? tipo : TipoMovimiento.VENTA;
        this.idUsuario = idUsuario;
        this.idCaja = idCaja;
    }

    public Movimiento(String descripcion, TipoMovimiento tipo, int idUsuario, Integer idCaja) {
        this(0, LocalDateTime.now(), descripcion, tipo, idUsuario, idCaja);
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdCaja() {
        return idCaja;
    }

    public void setIdCaja(Integer idCaja) {
        this.idCaja = idCaja;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movimiento that = (Movimiento) o;
        return idMovimiento == that.idMovimiento;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMovimiento);
    }

    @Override
    public String toString() {
        return "Movimiento #" + idMovimiento + " [" + tipo + "] - " + descripcion;
    }
}

