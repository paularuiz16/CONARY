package com.conary.rosita.util;

import com.conary.rosita.db.ConexionDB;
import com.conary.rosita.db.ISQL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Servicio real de respaldo y restauracion de la base de datos MySQL (XAMPP)
 * en Java SE 8 puro, con soporte nativo para mysqldump/mysql de XAMPP y respaldo JDBC.
 */
public class RespaldoService {

    public static final String CARPETA_RESPALDOS = "respaldos";
    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final DateTimeFormatter FMT_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter FMT_FECHA_CORTA = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_ES_CO);
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("h:mm a", LOCALE_ES_CO);

    private static final String RUTA_XAMPP_MYSQLDUMP = "C:\\xampp\\mysql\\bin\\mysqldump.exe";
    private static final String RUTA_XAMPP_MYSQL = "C:\\xampp\\mysql\\bin\\mysql.exe";

    /**
     * Representa un archivo .sql listado en el historial local de copias.
     */
    public static class ArchivoRespaldo {
        private final File archivo;
        private final String nombreArchivo;
        private final LocalDateTime fechaModificacion;
        private final String fechaCorta;
        private final String fechaHoraLegible;
        private final String tamanoLegible;

        public ArchivoRespaldo(File archivo) {
            this.archivo = archivo;
            this.nombreArchivo = archivo.getName();
            this.fechaModificacion = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(archivo.lastModified()), ZoneId.systemDefault());
            this.fechaCorta = RespaldoService.formatearFechaCorta(this.fechaModificacion);
            this.fechaHoraLegible = RespaldoService.formatearFechaHoraCompleta(this.fechaModificacion);
            this.tamanoLegible = RespaldoService.formatearTamano(archivo.length());
        }

        public File getArchivo() {
            return archivo;
        }

        public String getNombreArchivo() {
            return nombreArchivo;
        }

        public LocalDateTime getFechaModificacion() {
            return fechaModificacion;
        }

        public String getFechaCorta() {
            return fechaCorta;
        }

        public String getFechaHoraLegible() {
            return fechaHoraLegible;
        }

        public String getTamanoLegible() {
            return tamanoLegible;
        }
    }

    public File obtenerCarpetaRespaldosLocal() {
        File dir = new File(CARPETA_RESPALDOS);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Lista los archivos .sql dentro de la carpeta local respaldos/ ordenados del mas reciente al mas antiguo.
     */
    public List<ArchivoRespaldo> listarCopiasLocales() {
        List<ArchivoRespaldo> copias = new ArrayList<>();
        File dir = obtenerCarpetaRespaldosLocal();
        File[] archivos = dir.listFiles();
        if (archivos != null) {
            Arrays.sort(archivos, new Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return Long.compare(b.lastModified(), a.lastModified());
                }
            });
            for (File f : archivos) {
                if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".sql") && f.length() > 0) {
                    copias.add(new ArchivoRespaldo(f));
                }
            }
        }
        return copias;
    }

    /**
     * Busca el respaldo .sql mas reciente en respaldos/ (o backups/ heredada) para mostrar "Ultima copia".
     */
    public File buscarUltimoRespaldo() {
        File masReciente = null;
        File[] carpetas = new File[]{
                new File(CARPETA_RESPALDOS),
                new File("backups")
        };
        for (File dir : carpetas) {
            if (dir.exists() && dir.isDirectory()) {
                File[] archivos = dir.listFiles();
                if (archivos != null) {
                    for (File f : archivos) {
                        if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".sql") && f.length() > 0) {
                            if (masReciente == null || f.lastModified() > masReciente.lastModified()) {
                                masReciente = f;
                            }
                        }
                    }
                }
            }
        }
        return masReciente;
    }

    /**
     * Devuelve el texto formateado para la cabecera:
     * "🕒 Última copia: hoy, 4:30 p. m." o "🕒 Última copia: Sin copias registradas aún".
     */
    public String obtenerTextoUltimaCopia() {
        File ultimo = buscarUltimoRespaldo();
        if (ultimo == null) {
            return "🕒  Última copia: Sin copias registradas aún";
        }
        LocalDateTime fechaMod = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(ultimo.lastModified()), ZoneId.systemDefault());
        String hora = normalizarAmPm(fechaMod.format(FMT_HORA));
        if (fechaMod.toLocalDate().equals(LocalDate.now())) {
            return "🕒  Última copia: hoy, " + hora;
        }
        String dia = fechaMod.format(FMT_FECHA_CORTA);
        return "🕒  Última copia: " + dia + ", " + hora;
    }

    /**
     * Crea una copia de seguridad .sql dentro de la carpeta local respaldos/.
     */
    public File crearCopiaLocal() throws Exception {
        return crearCopiaEnCarpeta(obtenerCarpetaRespaldosLocal(), "copia_" + ISQL.DATABASE + "_");
    }

    /**
     * Crea una copia de seguridad .sql en la carpeta seleccionada (por ejemplo memoria USB)
     * y sincroniza una copia en respaldos/ para actualizar el historial y "Ultima copia".
     */
    public File crearCopiaEnCarpetaExterna(File carpetaDestino) throws Exception {
        if (carpetaDestino == null || !carpetaDestino.isDirectory()) {
            throw new IllegalArgumentException("La carpeta seleccionada no es válida.");
        }
        File archivoExterno = crearCopiaEnCarpeta(carpetaDestino, "copia_" + ISQL.DATABASE + "_");
        File carpetaLocal = obtenerCarpetaRespaldosLocal();
        if (!carpetaDestino.getAbsoluteFile().equals(carpetaLocal.getAbsoluteFile())) {
            File copiaLocal = new File(carpetaLocal, archivoExterno.getName());
            Files.copy(archivoExterno.toPath(), copiaLocal.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return archivoExterno;
    }

    public File crearCopiaEnCarpeta(File carpetaDestino, String prefijo) throws Exception {
        if (!carpetaDestino.exists() && !carpetaDestino.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta destino: " + carpetaDestino.getAbsolutePath());
        }
        String nombre = prefijo + LocalDateTime.now().format(FMT_ARCHIVO) + ".sql";
        File archivoSalida = new File(carpetaDestino, nombre);

        boolean generadoConMysqldump = intentarMysqldump(archivoSalida);
        if (!generadoConMysqldump) {
            exportarPorJDBC(archivoSalida);
        }

        if (!archivoSalida.exists() || archivoSalida.length() == 0) {
            throw new IOException("El archivo de respaldo generado quedó vacío.");
        }
        return archivoSalida;
    }

    /**
     * Restaura un archivo .sql sobre la base de datos configurada en ISQL,
     * creando antes un respaldo automatico preventivo en respaldos/.
     */
    public File restaurarCopia(File archivoSql) throws Exception {
        if (archivoSql == null || !archivoSql.exists() || !archivoSql.isFile() || archivoSql.length() == 0) {
            throw new IllegalArgumentException("El archivo de copia seleccionado no existe o está vacío.");
        }

        // Paso de seguridad: guardar copia preventiva antes de restaurar
        File respaldoPreventivo = crearCopiaEnCarpeta(
                obtenerCarpetaRespaldosLocal(), "respaldo_previo_restauracion_");

        boolean restauradoConMysqlCli = intentarMysqlCli(archivoSql);
        if (!restauradoConMysqlCli) {
            restaurarPorJDBC(archivoSql);
        }
        return respaldoPreventivo;
    }

    private boolean intentarMysqldump(File archivoSalida) {
        File exe = new File(RUTA_XAMPP_MYSQLDUMP);
        if (!exe.exists() || !exe.isFile()) {
            return false;
        }
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(exe.getAbsolutePath());
            cmd.add("--host=" + ISQL.HOST);
            cmd.add("--port=" + ISQL.PORT);
            cmd.add("--user=" + ISQL.USER);
            if (ISQL.PASSWORD != null && !ISQL.PASSWORD.isEmpty()) {
                cmd.add("--password=" + ISQL.PASSWORD);
            }
            cmd.add("--default-character-set=utf8mb4");
            cmd.add("--single-transaction");
            cmd.add("--routines");
            cmd.add("--triggers");
            cmd.add(ISQL.DATABASE);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectOutput(archivoSalida);
            Process proceso = pb.start();
            leerFlujoDescartado(proceso.getErrorStream());
            int exit = proceso.waitFor();
            return exit == 0 && archivoSalida.exists() && archivoSalida.length() > 50;
        } catch (Exception e) {
            System.err.println("[RespaldoService] mysqldump no disponible o falló, usando respaldo JDBC: " + e.getMessage());
            return false;
        }
    }

    private void exportarPorJDBC(File archivoSalida) throws SQLException, IOException {
        try (Connection conn = ConexionDB.getConnection();
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(new FileOutputStream(archivoSalida), StandardCharsets.UTF_8))) {

            writer.write("-- Respaldo CONARY v1.0 - Papeleria de Rosita\n");
            writer.write("-- Base de datos: " + ISQL.DATABASE + "\n");
            writer.write("-- Generado: " + LocalDateTime.now().toString() + "\n\n");
            writer.write("SET NAMES utf8mb4;\n");
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");

            List<String> tablas = listarTablasBase(conn);
            for (String tabla : tablas) {
                writer.write("-- --------------------------------------------------------\n");
                writer.write("-- Estructura de tabla para `" + tabla + "`\n");
                writer.write("-- --------------------------------------------------------\n");
                writer.write("DROP TABLE IF EXISTS `" + tabla + "`;\n");

                try (Statement st = conn.createStatement();
                     ResultSet rsCreate = st.executeQuery("SHOW CREATE TABLE `" + tabla + "`")) {
                    if (rsCreate.next()) {
                        writer.write(rsCreate.getString(2));
                        writer.write(";\n\n");
                    }
                }

                exportarDatosTabla(conn, tabla, writer);
            }

            writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
            writer.flush();
        }
    }

    private List<String> listarTablasBase(Connection conn) throws SQLException {
        List<String> tablas = new ArrayList<>();
        String sql = "SHOW FULL TABLES FROM `" + ISQL.DATABASE + "` WHERE Table_type = 'BASE TABLE'";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                tablas.add(rs.getString(1));
            }
        }
        return tablas;
    }

    private void exportarDatosTabla(Connection conn, String tabla, BufferedWriter writer)
            throws SQLException, IOException {
        String selectSql = "SELECT * FROM `" + tabla + "`";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(selectSql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            if (colCount == 0) {
                return;
            }

            StringBuilder columnasHeader = new StringBuilder();
            columnasHeader.append("INSERT INTO `").append(tabla).append("` (");
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) {
                    columnasHeader.append(", ");
                }
                columnasHeader.append("`").append(meta.getColumnName(i)).append("`");
            }
            columnasHeader.append(") VALUES\n");

            int lote = 0;
            boolean hayFilas = false;
            while (rs.next()) {
                if (!hayFilas) {
                    writer.write("-- Volcado de datos para la tabla `" + tabla + "`\n");
                    hayFilas = true;
                }
                if (lote == 0) {
                    writer.write(columnasHeader.toString());
                } else {
                    writer.write(",\n");
                }

                writer.write("(");
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) {
                        writer.write(", ");
                    }
                    writer.write(formatearValorSQL(rs, meta.getColumnType(i), i));
                }
                writer.write(")");

                lote++;
                if (lote >= 100) {
                    writer.write(";\n");
                    lote = 0;
                }
            }
            if (lote > 0) {
                writer.write(";\n\n");
            } else if (hayFilas) {
                writer.write("\n");
            }
        }
    }

    private String formatearValorSQL(ResultSet rs, int sqlType, int colIndex) throws SQLException {
        Object obj = rs.getObject(colIndex);
        if (obj == null) {
            return "NULL";
        }
        if (obj instanceof Boolean) {
            return ((Boolean) obj) ? "1" : "0";
        }
        if (obj instanceof Number && sqlType != Types.BIT) {
            return obj.toString();
        }
        if (obj instanceof byte[]) {
            byte[] bytes = (byte[]) obj;
            StringBuilder hex = new StringBuilder("0x");
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        String texto = rs.getString(colIndex);
        if (texto == null) {
            return "NULL";
        }
        return "'" + escaparCadenaSQL(texto) + "'";
    }

    private String escaparCadenaSQL(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\'':
                    sb.append("\\'");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\0':
                    sb.append("\\0");
                    break;
                case '\u001a':
                    sb.append("\\Z");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    private boolean intentarMysqlCli(File archivoSql) {
        File exe = new File(RUTA_XAMPP_MYSQL);
        if (!exe.exists() || !exe.isFile()) {
            return false;
        }
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(exe.getAbsolutePath());
            cmd.add("--host=" + ISQL.HOST);
            cmd.add("--port=" + ISQL.PORT);
            cmd.add("--user=" + ISQL.USER);
            if (ISQL.PASSWORD != null && !ISQL.PASSWORD.isEmpty()) {
                cmd.add("--password=" + ISQL.PASSWORD);
            }
            cmd.add("--default-character-set=utf8mb4");
            cmd.add(ISQL.DATABASE);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectInput(archivoSql);
            Process proceso = pb.start();
            leerFlujoDescartado(proceso.getInputStream());
            String error = leerFlujoTexto(proceso.getErrorStream());
            int exit = proceso.waitFor();
            if (exit != 0) {
                System.err.println("[RespaldoService] mysql.exe falló (" + exit + "): " + error);
                return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println("[RespaldoService] No se pudo usar mysql.exe, usando restauración JDBC: " + e.getMessage());
            return false;
        }
    }

    private void restaurarPorJDBC(File archivoSql) throws SQLException, IOException {
        String contenido = leerArchivoUTF8(archivoSql);
        List<String> sentencias = dividirSentenciasSQL(contenido);

        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String sql : sentencias) {
                    String limpia = sql.trim();
                    if (!limpia.isEmpty() && !esDirectivaIgnorable(limpia)) {
                        st.execute(limpia);
                    }
                }
            } finally {
                st.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    private boolean esDirectivaIgnorable(String sql) {
        String upper = sql.toUpperCase(Locale.ROOT);
        return upper.startsWith("DELIMITER ")
                || upper.startsWith("LOCK TABLES ")
                || upper.equals("UNLOCK TABLES");
    }

    private List<String> dividirSentenciasSQL(String script) {
        List<String> sentencias = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enComillaSimple = false;
        boolean enComillaDoble = false;
        boolean enBacktick = false;
        boolean enComentarioLinea = false;
        boolean enComentarioBloque = false;

        int len = script.length();
        for (int i = 0; i < len; i++) {
            char c = script.charAt(i);
            char siguiente = (i + 1 < len) ? script.charAt(i + 1) : '\0';

            if (enComentarioLinea) {
                if (c == '\n') {
                    enComentarioLinea = false;
                }
                continue;
            }

            if (enComentarioBloque) {
                if (c == '*' && siguiente == '/') {
                    enComentarioBloque = false;
                    i++;
                }
                continue;
            }

            if (!enComillaSimple && !enComillaDoble && !enBacktick) {
                if (c == '-' && siguiente == '-' && (i + 2 >= len || Character.isWhitespace(script.charAt(i + 2)))) {
                    enComentarioLinea = true;
                    i++;
                    continue;
                }
                if (c == '#') {
                    enComentarioLinea = true;
                    continue;
                }
                if (c == '/' && siguiente == '*') {
                    enComentarioBloque = true;
                    i++;
                    continue;
                }
            }

            if (c == '\'' && !enComillaDoble && !enBacktick) {
                if (enComillaSimple && siguiente == '\'') {
                    actual.append("''");
                    i++;
                    continue;
                }
                if (!estaEscapado(script, i)) {
                    enComillaSimple = !enComillaSimple;
                }
            } else if (c == '"' && !enComillaSimple && !enBacktick) {
                if (!estaEscapado(script, i)) {
                    enComillaDoble = !enComillaDoble;
                }
            } else if (c == '`' && !enComillaSimple && !enComillaDoble) {
                enBacktick = !enBacktick;
            }

            if (c == ';' && !enComillaSimple && !enComillaDoble && !enBacktick) {
                String stmt = actual.toString().trim();
                if (!stmt.isEmpty()) {
                    sentencias.add(stmt);
                }
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }

        String remanente = actual.toString().trim();
        if (!remanente.isEmpty()) {
            sentencias.add(remanente);
        }
        return sentencias;
    }

    private boolean estaEscapado(String s, int pos) {
        int backslashes = 0;
        for (int j = pos - 1; j >= 0 && s.charAt(j) == '\\'; j--) {
            backslashes++;
        }
        return (backslashes % 2) != 0;
    }

    private String leerArchivoUTF8(File archivo) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            char[] buf = new char[4096];
            int leidos;
            while ((leidos = reader.read(buf)) != -1) {
                sb.append(buf, 0, leidos);
            }
        }
        return sb.toString();
    }

    private void leerFlujoDescartado(InputStream is) throws IOException {
        byte[] buf = new byte[1024];
        while (is.read(buf) != -1) {
            // Descartar salida para evitar bloqueo del subproceso
        }
    }

    private String leerFlujoTexto(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    public static String formatearFechaCorta(LocalDateTime dt) {
        return dt.format(FMT_FECHA_CORTA);
    }

    public static String formatearFechaHoraCompleta(LocalDateTime dt) {
        String dia = dt.format(FMT_FECHA_CORTA);
        String hora = normalizarAmPm(dt.format(FMT_HORA));
        return dia + ", " + hora;
    }

    private static String normalizarAmPm(String horaFormateada) {
        if (horaFormateada == null) {
            return "";
        }
        String res = horaFormateada.toLowerCase(LOCALE_ES_CO).trim();
        res = res.replace("a.m.", "a. m.").replace("p.m.", "p. m.");
        res = res.replaceAll("\\bam\\b", "a. m.").replaceAll("\\bpm\\b", "p. m.");
        return res;
    }

    public static String formatearTamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024.0) {
            return String.format(LOCALE_ES_CO, "%.0f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format(LOCALE_ES_CO, "%.1f MB", mb);
    }
}
