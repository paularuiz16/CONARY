package com.conary.rosita.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad que representa un producto del inventario de la papeleria.
 */
public class Producto implements Serializable {

    private int idProducto;
    private String nombre;
    private String categoria;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private int stockActual;
    private int stockMinimo;
    private boolean activo;

    public Producto() {
        this.categoria = "GENERAL";
        this.precioCompra = BigDecimal.ZERO;
        this.precioVenta = BigDecimal.ZERO;
        this.stockActual = 0;
        this.stockMinimo = 5;
        this.activo = true;
    }

    public Producto(int idProducto, String nombre, String categoria, BigDecimal precioCompra,
                    BigDecimal precioVenta, int stockActual, int stockMinimo, boolean activo) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.categoria = categoria != null && !categoria.trim().isEmpty() ? categoria : "GENERAL";
        this.precioCompra = precioCompra != null ? precioCompra : BigDecimal.ZERO;
        this.precioVenta = precioVenta != null ? precioVenta : BigDecimal.ZERO;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.activo = activo;
    }

    public Producto(String nombre, String categoria, BigDecimal precioCompra,
                    BigDecimal precioVenta, int stockActual, int stockMinimo) {
        this(0, nombre, categoria, precioCompra, precioVenta, stockActual, stockMinimo, true);
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra != null ? precioCompra : BigDecimal.ZERO;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta != null ? precioVenta : BigDecimal.ZERO;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int stockActual) {
        this.stockActual = stockActual;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isStockBajo() {
        return stockActual <= stockMinimo;
    }

    public BigDecimal getGananciaEstimada() {
        return precioVenta.subtract(precioCompra);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return idProducto == producto.idProducto;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idProducto);
    }

    @Override
    public String toString() {
        return nombre + " (Stock: " + stockActual + ")";
    }
}

