package com.conary.rosita.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad que representa un cliente de la papeleria con soporte para credito/fiado.
 */
public class Cliente implements Serializable {

    private int idCliente;
    private String nombre;
    private String telefono;
    private String direccion;
    private boolean activo;
    private boolean fiadoAutorizado;
    private BigDecimal limiteCredito;

    // Atributo derivado/calculado de la vista v_saldo_cliente
    private BigDecimal saldoPendiente;

    public Cliente() {
        this.activo = true;
        this.fiadoAutorizado = false;
        this.limiteCredito = BigDecimal.ZERO;
        this.saldoPendiente = BigDecimal.ZERO;
    }

    public Cliente(int idCliente, String nombre, String telefono, String direccion, boolean activo, boolean fiadoAutorizado, BigDecimal limiteCredito) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.activo = activo;
        this.fiadoAutorizado = fiadoAutorizado;
        this.limiteCredito = limiteCredito != null ? limiteCredito : BigDecimal.ZERO;
        this.saldoPendiente = BigDecimal.ZERO;
    }

    public Cliente(String nombre, String telefono, String direccion, boolean fiadoAutorizado, BigDecimal limiteCredito) {
        this(0, nombre, telefono, direccion, true, fiadoAutorizado, limiteCredito);
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isFiadoAutorizado() {
        return fiadoAutorizado;
    }

    public void setFiadoAutorizado(boolean fiadoAutorizado) {
        this.fiadoAutorizado = fiadoAutorizado;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(BigDecimal limiteCredito) {
        this.limiteCredito = limiteCredito != null ? limiteCredito : BigDecimal.ZERO;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente != null ? saldoPendiente : BigDecimal.ZERO;
    }

    public BigDecimal getCreditoDisponible() {
        if (!fiadoAutorizado) return BigDecimal.ZERO;
        BigDecimal disponible = limiteCredito.subtract(saldoPendiente != null ? saldoPendiente : BigDecimal.ZERO);
        return disponible.compareTo(BigDecimal.ZERO) > 0 ? disponible : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return idCliente == cliente.idCliente;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }

    @Override
    public String toString() {
        return nombre + (telefono != null && !telefono.isEmpty() ? " (" + telefono + ")" : "");
    }
}

