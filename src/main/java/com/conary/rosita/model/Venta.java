package com.conary.rosita.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una Venta efectuada en el negocio.
 * Hereda de {@link Movimiento} conservando trazabilidad, usuario y turno de caja.
 */
public class Venta extends Movimiento {

    private Integer idCliente;
    private boolean esFiado;
    private boolean anulada;
    private String motivoAnulacion;
    private boolean importada;

    // Lista de items de la venta
    private List<DetalleVenta> detalles;

    // Campos auxiliares para visualizacion en UI / vistas
    private String nombreCliente;
    private BigDecimal totalVenta;

    public Venta() {
        super();
        setTipo(TipoMovimiento.VENTA);
        this.esFiado = false;
        this.anulada = false;
        this.importada = false;
        this.detalles = new ArrayList<>();
        this.totalVenta = BigDecimal.ZERO;
    }

    public Venta(int idMovimiento, LocalDateTime fecha, String descripcion, int idUsuario, Integer idCaja,
                 Integer idCliente, boolean esFiado, boolean anulada, String motivoAnulacion, boolean importada) {
        super(idMovimiento, fecha, descripcion, TipoMovimiento.VENTA, idUsuario, idCaja);
        this.idCliente = idCliente;
        this.esFiado = esFiado;
        this.anulada = anulada;
        this.motivoAnulacion = motivoAnulacion;
        this.importada = importada;
        this.detalles = new ArrayList<>();
        this.totalVenta = BigDecimal.ZERO;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public boolean isEsFiado() {
        return esFiado;
    }

    public void setEsFiado(boolean esFiado) {
        this.esFiado = esFiado;
    }

    public boolean isAnulada() {
        return anulada;
    }

    public void setAnulada(boolean anulada) {
        this.anulada = anulada;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public boolean isImportada() {
        return importada;
    }

    public void setImportada(boolean importada) {
        this.importada = importada;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles != null ? detalles : new ArrayList<>();
        calcularTotal();
    }

    public void agregarDetalle(DetalleVenta detalle) {
        if (detalle != null) {
            this.detalles.add(detalle);
            calcularTotal();
        }
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public BigDecimal getTotalVenta() {
        return totalVenta;
    }

    public void setTotalVenta(BigDecimal totalVenta) {
        this.totalVenta = totalVenta != null ? totalVenta : BigDecimal.ZERO;
    }

    public BigDecimal calcularTotal() {
        BigDecimal suma = BigDecimal.ZERO;
        if (detalles != null) {
            for (DetalleVenta dv : detalles) {
                if (dv != null && dv.getSubtotal() != null) {
                    suma = suma.add(dv.getSubtotal());
                }
            }
        }
        this.totalVenta = suma;
        return this.totalVenta;
    }

    @Override
    public String toString() {
        return "Venta #" + getIdMovimiento() + " [" + (esFiado ? "FIADO" : "CONTADO") + "] - Total: $" + totalVenta;
    }
}

