package com.conary.rosita.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestor de conexion JDBC directo y ultraligero para MySQL (XAMPP).
 * Implementa {@link ISQL} con metodos estaticos de utilidad y cierre seguro
 * de recursos para garantizar cero fugas de memoria en equipos de bajos recursos.
 */
public class ConexionDB implements ISQL {

    private static ConexionDB instancia;

    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            try {
                // Compatibilidad con drivers legacy MySQL 5.x comunes en XAMPP clasico
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                System.err.println("[ConexionDB] Error: Driver MySQL no encontrado en el classpath: " + e.getMessage());
            }
        }
    }

    private ConexionDB() {
        // Constructor privado para patron singleton ligero
    }

    /**
     * Obtiene la instancia singleton de ConexionDB.
     */
    public static synchronized ConexionDB getInstancia() {
        if (instancia == null) {
            instancia = new ConexionDB();
        }
        return instancia;
    }

    /**
     * Abre y retorna una nueva conexion JDBC directa.
     * Al usar JDBC puro, cada DAO puede emplear try-with-resources para liberar
     * memoria inmediatamente sin saturar el Heap del sistema.
     *
     * @return Connection lista para operaciones SQL.
     * @throws SQLException si falla la conexion con XAMPP / MySQL.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    @Override
    public Connection obtenerConexion() throws SQLException {
        return getConnection();
    }

    /**
     * Prueba rapida de conectividad con el servidor MySQL de XAMPP.
     *
     * @return true si la conexion fue exitosa, false en caso contrario.
     */
    public static boolean probarConexion() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("[ConexionDB] Fallo la prueba de conexion: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cierra de forma segura uno o multiples recursos JDBC (ResultSet, PreparedStatement, Connection)
     * evitando null pointers y excepciones no controladas.
     *
     * @param recursos Uno o mas objetos que implementen AutoCloseable.
     */
    public static void cerrar(AutoCloseable... recursos) {
        if (recursos == null) {
            return;
        }
        for (AutoCloseable recurso : recursos) {
            if (recurso != null) {
                try {
                    recurso.close();
                } catch (Exception e) {
                    System.err.println("[ConexionDB] Error al cerrar recurso JDBC: " + e.getMessage());
                }
            }
        }
    }
}

