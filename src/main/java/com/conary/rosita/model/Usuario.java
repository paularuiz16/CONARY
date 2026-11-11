package com.conary.rosita.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad que representa un usuario del sistema (Administrador o Vendedor).
 */
public class Usuario implements Serializable {

    private int idUsuario;
    private String nombre;
    private String username;
    private String passwordHash;
    private RolUsuario rol;
    private boolean activo;

    public Usuario() {
        this.rol = RolUsuario.VENDEDOR;
        this.activo = true;
    }

    public Usuario(int idUsuario, String nombre, String username, String passwordHash, RolUsuario rol, boolean activo) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol != null ? rol : RolUsuario.VENDEDOR;
        this.activo = activo;
    }

    public Usuario(String nombre, String username, String passwordHash, RolUsuario rol, boolean activo) {
        this(0, nombre, username, passwordHash, rol, activo);
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean esAdministrador() {
        return this.rol == RolUsuario.ADMINISTRADOR;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return idUsuario == usuario.idUsuario;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario);
    }

    @Override
    public String toString() {
        return nombre + " (" + username + " - " + rol + ")";
    }
}

