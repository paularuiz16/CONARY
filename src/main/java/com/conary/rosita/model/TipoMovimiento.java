package com.conary.rosita.model;

/**
 * Tipos de movimientos financieros registrados en la tabla 'movimiento'.
 */
public enum TipoMovimiento {
    VENTA,
    COMPRA,
    ABONO_CLIENTE,
    PAGO_PROVEEDOR;

    public static TipoMovimiento fromString(String tipo) {
        if (tipo == null) return VENTA;
        try {
            return TipoMovimiento.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return VENTA;
        }
    }
}

