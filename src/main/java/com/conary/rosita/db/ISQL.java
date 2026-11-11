package com.conary.rosita.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interfaz que define los parametros de configuracion SQL y contratos base
 * de conexion para la base de datos de la Papeleria de Rosita en MySQL (XAMPP).
 * Disenada para JDBC puro, sin sobrecarga de memoria (apta para 2 GB de RAM).
 */
public interface ISQL {

    String HOST = "localhost";
    String PORT = "3306";
    String DATABASE = "papeleria_rosita";
    String USER = "root";
    String PASSWORD = "";

    // Driver moderno MySQL Connector/J
    String DRIVER = "com.mysql.cj.jdbc.Driver";

    // Parametros optimizados para bajo overhead, reconexion y codificacion UTF-8
    String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=America/Bogota"
            + "&characterEncoding=UTF-8"
            + "&useUnicode=true"
            + "&connectTimeout=5000"
            + "&socketTimeout=30000";

    /**
     * Obtiene una conexion activa a la base de datos.
     * @return Connection activa.
     * @throws SQLException si ocurre un error de conexion.
     */
    Connection obtenerConexion() throws SQLException;
}

