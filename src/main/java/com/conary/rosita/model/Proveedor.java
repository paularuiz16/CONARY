package com.conary.rosita.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad que representa un proveedor de insumos o mercancia.
 */
public class Proveedor implements Serializable {

    private int idProveedor;
    private String nombre;
    private String telefono;
    private String direccion;
    private String productos;
    private boolean activo;

    // Atributo derivado/calculado de la vista v_saldo_proveedor
    private BigDecimal saldoPendiente;

    public Proveedor() {
        this.activo = true;
        this.saldoPendiente = BigDecimal.ZERO;
    }

    public Proveedor(int idProveedor, String nombre, String telefono, String direccion, String productos, boolean activo) {
        this.idProveedor = idProveedor;
        this.nombre = nombre;
        this.telefono = telefono;
        this.direccion = direccion;
        this.productos = productos;
        this.activo = activo;
        this.saldoPendiente = BigDecimal.ZERO;
    }

    public Proveedor(String nombre, String telefono, String direccion, String productos) {
        this(0, nombre, telefono, direccion, productos, true);
    }

    public int getIdProveedor() {
        return idProveedor;
    }

    public void setIdProveedor(int idProveedor) {
        this.idProveedor = idProveedor;
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

    public String getProductos() {
        return productos;
    }

    public void setProductos(String productos) {
        this.productos = productos;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public BigDecimal getSaldoPendiente() {
        return saldoPendiente;
    }

    public void setSaldoPendiente(BigDecimal saldoPendiente) {
        this.saldoPendiente = saldoPendiente != null ? saldoPendiente : BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proveedor proveedor = (Proveedor) o;
        return idProveedor == proveedor.idProveedor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProveedor);
    }

    @Override
    public String toString() {
        return nombre;
    }
}

