package com.conary.rosita.model;

/**
 * Roles disponibles en el sistema de acuerdo al esquema de la base de datos.
 */
public enum RolUsuario {
    ADMINISTRADOR,
    VENDEDOR;

    public static RolUsuario fromString(String rol) {
        if (rol == null) return VENDEDOR;
        try {
            return RolUsuario.valueOf(rol.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return VENDEDOR;
        }
    }
}

