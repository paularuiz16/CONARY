package com.conary.rosita.dao;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.model.RolUsuario;
import com.conary.rosita.model.Usuario;
import com.conary.rosita.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para operaciones de persistencia y autenticacion sobre la tabla 'usuario'.
 * Disenado con JDBC puro sin sobrecarga de memoria (Java 8).
 */
public class UsuarioDAO {

    /**
     * Autentica un usuario verificando username y hash SHA-256 de la contrasena.
     * Solo permite ingreso si el usuario esta marcado como 'activo = TRUE'.
     */
    public Usuario login(String username, String passwordPlana) throws SQLException {
        if (username == null || passwordPlana == null) {
            return null;
        }

        String sql = "SELECT id_usuario, nombre, username, password_hash, rol, activo "
                   + "FROM usuario "
                   + "WHERE username = ? AND activo = TRUE LIMIT 1";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashAlmacenado = rs.getString("password_hash");
                    if (SecurityUtil.verificarPassword(passwordPlana, hashAlmacenado)) {
                        return mapearUsuario(rs);
                    }
                }
            }
        }
        return null;
    }

    public Usuario obtenerPorId(int idUsuario) throws SQLException {
        String sql = "SELECT id_usuario, nombre, username, password_hash, rol, activo "
                   + "FROM usuario WHERE id_usuario = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        }
        return null;
    }

    public List<Usuario> listarTodos(boolean soloActivos) throws SQLException {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id_usuario, nombre, username, password_hash, rol, activo FROM usuario "
                   + (soloActivos ? "WHERE activo = TRUE " : "")
                   + "ORDER BY nombre ASC";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        }
        return lista;
    }

    public boolean insertar(Usuario usuario, String passwordPlana) throws SQLException {
        String sql = "INSERT INTO usuario (nombre, username, password_hash, rol, activo) "
                   + "VALUES (?, ?, ?, ?, ?)";

        String hash = SecurityUtil.hashSHA256(passwordPlana);

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsername().trim());
            ps.setString(3, hash);
            ps.setString(4, usuario.getRol().name());
            ps.setBoolean(5, usuario.isActivo());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        usuario.setIdUsuario(rs.getInt(1));
                        usuario.setPasswordHash(hash);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean actualizar(Usuario usuario, String nuevaPasswordPlana) throws SQLException {
        boolean cambiaPassword = nuevaPasswordPlana != null && !nuevaPasswordPlana.trim().isEmpty();

        String sql = cambiaPassword
                ? "UPDATE usuario SET nombre = ?, username = ?, rol = ?, activo = ?, password_hash = ? WHERE id_usuario = ?"
                : "UPDATE usuario SET nombre = ?, username = ?, rol = ?, activo = ? WHERE id_usuario = ?";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, usuario.getNombre());
            ps.setString(2, usuario.getUsername().trim());
            ps.setString(3, usuario.getRol().name());
            ps.setBoolean(4, usuario.isActivo());

            if (cambiaPassword) {
                String nuevoHash = SecurityUtil.hashSHA256(nuevaPasswordPlana);
                ps.setString(5, nuevoHash);
                ps.setInt(6, usuario.getIdUsuario());
                usuario.setPasswordHash(nuevoHash);
            } else {
                ps.setInt(5, usuario.getIdUsuario());
            }

            return ps.executeUpdate() > 0;
        }
    }

    public boolean cambiarEstado(int idUsuario, boolean activo) throws SQLException {
        String sql = "UPDATE usuario SET activo = ? WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, activo);
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    public int contarAdministradoresActivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE rol = 'ADMINISTRADOR' AND activo = TRUE";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public boolean actualizarDatosBasicos(int idUsuario, String nombre, RolUsuario rol) throws SQLException {
        String sql = "UPDATE usuario SET nombre = ?, rol = ? WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre != null ? nombre.trim() : "");
            ps.setString(2, (rol != null ? rol : RolUsuario.VENDEDOR).name());
            ps.setInt(3, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Verifica si ya existe un usuario registrado con ese nombre para entrar (case-insensitive en MySQL).
     */
    public boolean existeUsername(String username) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM usuario WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Actualiza la clave de acceso de un usuario aplicando hash SHA-256.
     */
    public boolean actualizarPassword(int idUsuario, String nuevaPasswordPlana) throws SQLException {
        String sql = "UPDATE usuario SET password_hash = ? WHERE id_usuario = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, SecurityUtil.hashSHA256(nuevaPasswordPlana));
            ps.setInt(2, idUsuario);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Verifica si la clave ingresada coincide con la clave actual de un usuario activo.
     */
    public boolean verificarPasswordUsuario(int idUsuario, String passwordPlana) throws SQLException {
        if (passwordPlana == null || passwordPlana.isEmpty()) {
            return false;
        }
        String sql = "SELECT password_hash FROM usuario WHERE id_usuario = ? AND activo = TRUE LIMIT 1";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return SecurityUtil.verificarPassword(passwordPlana, rs.getString("password_hash"));
                }
            }
        }
        return false;
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombre(rs.getString("nombre"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRol(RolUsuario.fromString(rs.getString("rol")));
        u.setActivo(rs.getBoolean("activo"));
        return u;
    }
}

