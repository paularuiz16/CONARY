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

