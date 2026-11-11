package com.conary.rosita.util;

import com.conary.rosita.model.Caja;
import com.conary.rosita.model.Usuario;

/**
 * Gestor de sesion en memoria para el usuario y turno de caja activo.
 * Mantiene el estado de la sesion sin consumo de memoria adicional.
 */
public final class SesionUsuario {

    private static Usuario usuarioLogueado;
    private static Caja cajaActiva;

    private SesionUsuario() {
    }

    public static Usuario getUsuarioLogueado() {
        return usuarioLogueado;
    }

    public static void setUsuarioLogueado(Usuario usuario) {
        usuarioLogueado = usuario;
    }

    public static Caja getCajaActiva() {
        return cajaActiva;
    }

    public static void setCajaActiva(Caja caja) {
        cajaActiva = caja;
    }

    public static boolean haySesionActiva() {
        return usuarioLogueado != null;
    }

    public static boolean esAdministrador() {
        return usuarioLogueado != null && usuarioLogueado.esAdministrador();
    }

    public static void cerrarSesion() {
        usuarioLogueado = null;
        cajaActiva = null;
    }
}

