package com.conary.rosita.util;

import com.conary.rosita.db.ConexionDB;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Servicio para leer archivos Excel (.xlsx / .xls) o CSV de ventas anteriores,
 * validar cada fila y guardarlas en MySQL sin alterar el inventario actual ni la caja.
 */
public class ImportadorExcelService {

    private static final Locale LOCALE_ES_CO = new Locale("es", "CO");
    private static final DateTimeFormatter FMT_FECHA_MOSTRAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static class FilaVentaExcel {
        private final int numeroFila;
        private final LocalDateTime fecha;
        private final String fechaTexto;
        private final String nombreProducto;
        private final BigDecimal valorUnitario;
        private final int cantidad;
        private final BigDecimal valorTotal;

        public FilaVentaExcel(int numeroFila, LocalDateTime fecha, String nombreProducto,
                              BigDecimal valorUnitario, int cantidad, BigDecimal valorTotal) {
            this.numeroFila = numeroFila;
            this.fecha = fecha;
            this.fechaTexto = fecha != null ? fecha.format(FMT_FECHA_MOSTRAR) : "";
            this.nombreProducto = nombreProducto;
            this.valorUnitario = valorUnitario;
            this.cantidad = cantidad;
            this.valorTotal = valorTotal;
        }

        public int getNumeroFila() { return numeroFila; }
        public LocalDateTime getFecha() { return fecha; }
        public String getFechaTexto() { return fechaTexto; }
        public String getNombreProducto() { return nombreProducto; }
        public BigDecimal getValorUnitario() { return valorUnitario; }
        public int getCantidad() { return cantidad; }
        public BigDecimal getValorTotal() { return valorTotal; }
    }

    public static class FilaProblema {
        private final int numeroFila;
        private final String mensaje;

        public FilaProblema(int numeroFila, String mensaje) {
            this.numeroFila = numeroFila;
            this.mensaje = mensaje;
        }

        public int getNumeroFila() { return numeroFila; }
        public String getMensaje() { return mensaje; }
    }

    public static class ResultadoRevision {
        private final String nombreArchivo;
        private final int totalFilasLeidas;
        private final List<FilaVentaExcel> filasValidas;
        private final List<FilaProblema> filasConProblemas;
        private final int cantidadProductosNuevos;

        public ResultadoRevision(String nombreArchivo, int totalFilasLeidas,
                                 List<FilaVentaExcel> filasValidas,
                                 List<FilaProblema> filasConProblemas,
                                 int cantidadProductosNuevos) {
            this.nombreArchivo = nombreArchivo;
            this.totalFilasLeidas = totalFilasLeidas;
            this.filasValidas = filasValidas;
            this.filasConProblemas = filasConProblemas;
            this.cantidadProductosNuevos = cantidadProductosNuevos;
        }

        public String getNombreArchivo() { return nombreArchivo; }
        public int getTotalFilasLeidas() { return totalFilasLeidas; }
        public List<FilaVentaExcel> getFilasValidas() { return filasValidas; }
        public List<FilaProblema> getFilasConProblemas() { return filasConProblemas; }
        public int getCantidadProductosNuevos() { return cantidadProductosNuevos; }
    }

    public static class ResultadoMigracion {
        private final int ventasPasadas;
        private final int productosNuevosCreados;
        private final List<FilaProblema> filasNoPasadas;

        public ResultadoMigracion(int ventasPasadas, int productosNuevosCreados,
                                  List<FilaProblema> filasNoPasadas) {
            this.ventasPasadas = ventasPasadas;
            this.productosNuevosCreados = productosNuevosCreados;
            this.filasNoPasadas = filasNoPasadas != null ? filasNoPasadas : new ArrayList<FilaProblema>();
        }

        public int getVentasPasadas() { return ventasPasadas; }
        public int getProductosNuevosCreados() { return productosNuevosCreados; }
        public List<FilaProblema> getFilasNoPasadas() { return filasNoPasadas; }
    }

    private static class FilaCruda {
        final int numeroFila;
        final LocalDateTime fechaNativa;
        final String rawFecha;
        final String rawProducto;
        final String rawValorUnitario;
        final String rawCantidad;
        final String rawValorTotal;

        FilaCruda(int numeroFila, LocalDateTime fechaNativa, String rawFecha, String rawProducto,
                  String rawValorUnitario, String rawCantidad, String rawValorTotal) {
            this.numeroFila = numeroFila;
            this.fechaNativa = fechaNativa;
            this.rawFecha = rawFecha;
            this.rawProducto = rawProducto;
            this.rawValorUnitario = rawValorUnitario;
            this.rawCantidad = rawCantidad;
            this.rawValorTotal = rawValorTotal;
        }
    }

    /**
     * Lee el archivo (.xlsx / .xls / .csv), valida cada fila y calcula productos nuevos.
     */
    public ResultadoRevision leerYValidar(File archivo) throws Exception {
        if (archivo == null || !archivo.exists() || !archivo.isFile()) {
            throw new IllegalArgumentException("No se encontró el archivo seleccionado.");
        }

        String nombreLower = archivo.getName().toLowerCase(LOCALE_ES_CO);
        List<FilaCruda> crudas;
        if (nombreLower.endsWith(".csv")) {
            crudas = leerCsv(archivo);
        } else {
            crudas = leerExcelPoi(archivo);
        }

        List<FilaVentaExcel> validas = new ArrayList<FilaVentaExcel>();
        List<FilaProblema> problemas = new ArrayList<FilaProblema>();
        Set<String> productosExistentes = consultarNombresProductosExistentes();
        Set<String> nuevosDetectados = new HashSet<String>();

        for (FilaCruda raw : crudas) {
            LocalDateTime fecha = raw.fechaNativa != null ? raw.fechaNativa : parsearFechaTexto(raw.rawFecha);
            if (fecha == null) {
                boolean sinDatoFecha = raw.rawFecha == null || raw.rawFecha.trim().isEmpty();
                String motivo = sinDatoFecha ? "no tiene fecha" : "la fecha no se entiende";
                problemas.add(new FilaProblema(raw.numeroFila, "Fila " + raw.numeroFila + ": " + motivo));
                continue;
            }

            String nombreProd = raw.rawProducto != null ? raw.rawProducto.trim().replaceAll("\\s+", " ") : "";
            if (nombreProd.isEmpty()) {
                problemas.add(new FilaProblema(raw.numeroFila, "Fila " + raw.numeroFila + ": no tiene producto"));
                continue;
            }

            BigDecimal valorUnitario = parsearMoneda(raw.rawValorUnitario);
            if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) <= 0) {
                problemas.add(new FilaProblema(raw.numeroFila,
                        "Fila " + raw.numeroFila + ": el valor unitario no es válido"));
                continue;
            }

            Integer cantidad = parsearCantidadEntera(raw.rawCantidad);
            if (cantidad == null || cantidad.intValue() <= 0) {
                problemas.add(new FilaProblema(raw.numeroFila,
                        "Fila " + raw.numeroFila + ": la cantidad no es válida"));
                continue;
            }

            BigDecimal valorTotal = parsearMoneda(raw.rawValorTotal);
            if (valorTotal == null) {
                problemas.add(new FilaProblema(raw.numeroFila,
                        "Fila " + raw.numeroFila + ": falta el valor total"));
                continue;
            }

            BigDecimal esperado = valorUnitario.multiply(BigDecimal.valueOf(cantidad.intValue()))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalEscala = valorTotal.setScale(2, RoundingMode.HALF_UP);
            if (esperado.compareTo(totalEscala) != 0) {
                problemas.add(new FilaProblema(raw.numeroFila,
                        "Fila " + raw.numeroFila + ": el total no coincide con valor × cantidad"));
                continue;
            }

            validas.add(new FilaVentaExcel(
                    raw.numeroFila,
                    fecha,
                    nombreProd,
                    valorUnitario.setScale(2, RoundingMode.HALF_UP),
                    cantidad.intValue(),
                    totalEscala
            ));

            String claveNombre = normalizarClaveNombre(nombreProd);
            if (!productosExistentes.contains(claveNombre)) {
                nuevosDetectados.add(claveNombre);
            }
        }

        return new ResultadoRevision(
                archivo.getName(),
                crudas.size(),
                validas,
                problemas,
                nuevosDetectados.size()
        );
    }

    /**
     * Ejecuta una transaccion unica en MySQL para crear productos nuevos (stock 0)
     * y registrar las ventas historicas (id_caja = NULL, importada = TRUE, sin tocar existencias).
     */
    public ResultadoMigracion pasarDatosAMySQL(ResultadoRevision revision, int idUsuarioActual) throws SQLException {
        if (revision == null || revision.getFilasValidas().isEmpty()) {
            throw new IllegalArgumentException("No hay filas válidas para pasar al sistema.");
        }

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false);

            Map<String, Integer> mapaProductos = new HashMap<String, Integer>();
            String sqlBuscarProductos = "SELECT id_producto, nombre FROM producto";
            try (PreparedStatement ps = conn.prepareStatement(sqlBuscarProductos);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nom = rs.getString("nombre");
                    if (nom != null) {
                        mapaProductos.put(normalizarClaveNombre(nom), Integer.valueOf(rs.getInt("id_producto")));
                    }
                }
            }

            // 1. Crear productos nuevos unicos con existencias = 0 y precio de venta del archivo
            Map<String, FilaVentaExcel> productosPorCrear = new LinkedHashMap<String, FilaVentaExcel>();
            for (FilaVentaExcel fila : revision.getFilasValidas()) {
                String clave = normalizarClaveNombre(fila.getNombreProducto());
                if (!mapaProductos.containsKey(clave) && !productosPorCrear.containsKey(clave)) {
                    productosPorCrear.put(clave, fila);
                }
            }

            int productosNuevosCreados = 0;
            String sqlInsertProd = "INSERT INTO producto (nombre, categoria, precio_compra, precio_venta, stock_actual, stock_minimo, activo) "
                                 + "VALUES (?, ?, 0.00, ?, 0, 5, TRUE)";
            try (PreparedStatement psProd = conn.prepareStatement(sqlInsertProd, Statement.RETURN_GENERATED_KEYS)) {
                for (Map.Entry<String, FilaVentaExcel> entry : productosPorCrear.entrySet()) {
                    FilaVentaExcel muestra = entry.getValue();
                    psProd.setString(1, muestra.getNombreProducto());
                    psProd.setString(2, "GENERAL");
                    psProd.setBigDecimal(3, muestra.getValorUnitario());
                    psProd.executeUpdate();
                    try (ResultSet rs = psProd.getGeneratedKeys()) {
                        if (rs.next()) {
                            mapaProductos.put(entry.getKey(), Integer.valueOf(rs.getInt(1)));
                            productosNuevosCreados++;
                        }
                    }
                }
            }

            // 2. Registrar ventas historicas sin afectar stock_actual ni asociar a caja
            String sqlMov = "INSERT INTO movimiento (fecha, descripcion, tipo, id_usuario, id_caja) "
                          + "VALUES (?, ?, 'VENTA', ?, ?)";
            String sqlVenta = "INSERT INTO venta (id_movimiento, id_cliente, es_fiado, anulada, motivo_anulacion, importada) "
                            + "VALUES (?, NULL, FALSE, FALSE, NULL, TRUE)";
            String sqlDet = "INSERT INTO detalle_venta (id_movimiento, id_producto, cantidad, valor_unitario, subtotal) "
                          + "VALUES (?, ?, ?, ?, ?)";

            int ventasPasadas = 0;
            try (PreparedStatement psMov = conn.prepareStatement(sqlMov, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psVenta = conn.prepareStatement(sqlVenta);
                 PreparedStatement psDet = conn.prepareStatement(sqlDet)) {

                for (FilaVentaExcel fila : revision.getFilasValidas()) {
                    Integer idProducto = mapaProductos.get(normalizarClaveNombre(fila.getNombreProducto()));
                    if (idProducto == null) {
                        throw new SQLException("No se pudo obtener el identificador del producto: " + fila.getNombreProducto());
                    }

                    psMov.setTimestamp(1, Timestamp.valueOf(fila.getFecha()));
                    psMov.setString(2, "Venta anterior pasada desde Excel - " + fila.getNombreProducto());
                    psMov.setInt(3, idUsuarioActual > 0 ? idUsuarioActual : 1);
                    psMov.setNull(4, Types.INTEGER);
                    psMov.executeUpdate();

                    int idMovimiento = 0;
                    try (ResultSet rsKey = psMov.getGeneratedKeys()) {
                        if (rsKey.next()) {
                            idMovimiento = rsKey.getInt(1);
                        } else {
                            throw new SQLException("No se generó el movimiento de venta histórica.");
                        }
                    }

                    psVenta.setInt(1, idMovimiento);
                    psVenta.executeUpdate();

                    psDet.setInt(1, idMovimiento);
                    psDet.setInt(2, idProducto.intValue());
                    psDet.setInt(3, fila.getCantidad());
                    psDet.setBigDecimal(4, fila.getValorUnitario());
                    psDet.setBigDecimal(5, fila.getValorTotal());
                    psDet.executeUpdate();

                    ventasPasadas++;
                }
            }

            conn.commit();
            return new ResultadoMigracion(ventasPasadas, productosNuevosCreados, revision.getFilasConProblemas());

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[ImportadorExcelService] Error en rollback: " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("[ImportadorExcelService] Error al cerrar conexión: " + e.getMessage());
                }
            }
        }
    }

    private Set<String> consultarNombresProductosExistentes() throws SQLException {
        Set<String> nombres = new HashSet<String>();
        String sql = "SELECT nombre FROM producto";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String nom = rs.getString("nombre");
                if (nom != null && !nom.trim().isEmpty()) {
                    nombres.add(normalizarClaveNombre(nom));
                }
            }
        }
        return nombres;
    }

    private List<FilaCruda> leerExcelPoi(File archivo) throws Exception {
        List<FilaCruda> lista = new ArrayList<FilaCruda>();
        DataFormatter formatter = new DataFormatter(LOCALE_ES_CO);

        try (FileInputStream fis = new FileInputStream(archivo);
             Workbook workbook = WorkbookFactory.create(fis)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("El archivo elegido no contiene ninguna hoja con datos.");
            }
            Sheet sheet = workbook.getSheetAt(0);
            int firstRow = sheet.getFirstRowNum();
            int lastRow = sheet.getLastRowNum();

            int[] cols = new int[]{0, 1, 2, 3, 4};
            int startDataRow = firstRow;

            // Detectar fila de encabezados si existe en las primeras filas no vacias
            for (int r = firstRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || esFilaPoiVacia(row, formatter)) {
                    continue;
                }
                int[] mapeoDetectado = detectarColumnasEncabezadoPoi(row, formatter);
                if (mapeoDetectado != null) {
                    cols = mapeoDetectado;
                    startDataRow = r + 1;
                } else {
                    startDataRow = r;
                }
                break;
            }

            for (int r = startDataRow; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || esFilaPoiVacia(row, formatter)) {
                    continue;
                }
                Cell cellFecha = row.getCell(cols[0]);
                LocalDateTime fechaNativa = extraerFechaExcel(cellFecha);
                String rawFecha = leerTextoCelda(cellFecha, formatter);
                String rawProd = leerTextoCelda(row.getCell(cols[1]), formatter);
                String rawUnit = leerTextoNumericoCelda(row.getCell(cols[2]), formatter);
                String rawCant = leerTextoNumericoCelda(row.getCell(cols[3]), formatter);
                String rawTotal = leerTextoNumericoCelda(row.getCell(cols[4]), formatter);

                if (estaTodoVacio(rawFecha, rawProd, rawUnit, rawCant, rawTotal) && fechaNativa == null) {
                    continue;
                }
                lista.add(new FilaCruda(r + 1, fechaNativa, rawFecha, rawProd, rawUnit, rawCant, rawTotal));
            }
        }
        return lista;
    }

    private List<FilaCruda> leerCsv(File archivo) throws IOException {
        List<FilaCruda> lista = new ArrayList<FilaCruda>();
        List<String> lineas = new ArrayList<String>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineas.add(line);
            }
        }

        if (lineas.isEmpty()) {
            return lista;
        }

        char separador = detectarSeparadorCsv(lineas);
        int[] cols = new int[]{0, 1, 2, 3, 4};
        int startIndex = 0;

        for (int i = 0; i < lineas.size(); i++) {
            String linea = limpiarBom(lineas.get(i));
            if (linea.trim().isEmpty()) {
                continue;
            }
            List<String> celdas = parsearLineaCsv(linea, separador);
            int[] detectadas = detectarColumnasEncabezadoLista(celdas);
            if (detectadas != null) {
                cols = detectadas;
                startIndex = i + 1;
            } else {
                startIndex = i;
            }
            break;
        }

        for (int i = startIndex; i < lineas.size(); i++) {
            String linea = limpiarBom(lineas.get(i));
            if (linea.trim().isEmpty()) {
                continue;
            }
            List<String> celdas = parsearLineaCsv(linea, separador);
            String rawFecha = obtenerCeldaLista(celdas, cols[0]);
            String rawProd = obtenerCeldaLista(celdas, cols[1]);
            String rawUnit = obtenerCeldaLista(celdas, cols[2]);
            String rawCant = obtenerCeldaLista(celdas, cols[3]);
            String rawTotal = obtenerCeldaLista(celdas, cols[4]);

            if (estaTodoVacio(rawFecha, rawProd, rawUnit, rawCant, rawTotal)) {
                continue;
            }
            lista.add(new FilaCruda(i + 1, null, rawFecha, rawProd, rawUnit, rawCant, rawTotal));
        }

        return lista;
    }

    private int[] detectarColumnasEncabezadoPoi(Row row, DataFormatter formatter) {
        short maxCol = row.getLastCellNum();
        List<String> valores = new ArrayList<String>();
        for (int c = 0; c < maxCol; c++) {
            valores.add(leerTextoCelda(row.getCell(c), formatter));
        }
        return detectarColumnasEncabezadoLista(valores);
    }

    private int[] detectarColumnasEncabezadoLista(List<String> encabezados) {
        int colFecha = -1;
        int colProd = -1;
        int colUnit = -1;
        int colCant = -1;
        int colTotal = -1;

        for (int i = 0; i < encabezados.size(); i++) {
            String norm = quitarAcentos(encabezados.get(i)).toLowerCase(Locale.ROOT).trim();
            if (norm.isEmpty()) {
                continue;
            }
            if (colFecha < 0 && (norm.contains("fecha") || norm.equals("dia"))) {
                colFecha = i;
            } else if (colProd < 0 && (norm.contains("producto") || norm.contains("articulo")
                    || norm.contains("nombre") || norm.contains("descripcion"))) {
                colProd = i;
            } else if (colUnit < 0 && (norm.contains("unitario") || norm.equals("precio")
                    || norm.equals("valor"))) {
                colUnit = i;
            } else if (colCant < 0 && (norm.contains("cantidad") || norm.equals("cant")
                    || norm.contains("unidades"))) {
                colCant = i;
            } else if (colTotal < 0 && (norm.contains("total") || norm.contains("subtotal"))) {
                colTotal = i;
            }
        }

        int encontrados = 0;
        if (colFecha >= 0) encontrados++;
        if (colProd >= 0) encontrados++;
        if (colUnit >= 0) encontrados++;
        if (colCant >= 0) encontrados++;
        if (colTotal >= 0) encontrados++;

        if (encontrados >= 3) {
            return new int[]{
                    colFecha >= 0 ? colFecha : 0,
                    colProd >= 0 ? colProd : 1,
                    colUnit >= 0 ? colUnit : 2,
                    colCant >= 0 ? colCant : 3,
                    colTotal >= 0 ? colTotal : 4
            };
        }
        return null;
    }

    private boolean esFilaPoiVacia(Row row, DataFormatter formatter) {
        short last = row.getLastCellNum();
        for (int c = 0; c < last; c++) {
            String val = leerTextoCelda(row.getCell(c), formatter);
            if (!val.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private LocalDateTime extraerFechaExcel(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType tipo = cell.getCellType();
        if (tipo == CellType.FORMULA) {
            tipo = cell.getCachedFormulaResultType();
        }
        if (tipo == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            if (date != null) {
                return Instant.ofEpochMilli(date.getTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            }
        }
        return null;
    }

    private String leerTextoCelda(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        String texto = formatter.formatCellValue(cell);
        return texto != null ? texto.trim() : "";
    }

    private String leerTextoNumericoCelda(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        CellType tipo = cell.getCellType();
        if (tipo == CellType.FORMULA) {
            tipo = cell.getCachedFormulaResultType();
        }
        if (tipo == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            BigDecimal bd = BigDecimal.valueOf(cell.getNumericCellValue());
            return bd.stripTrailingZeros().toPlainString();
        }
        return leerTextoCelda(cell, formatter);
    }

    private LocalDateTime parsearFechaTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpio = texto.trim();

        String[] patronesFechaHora = new String[]{
                "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm",
                "d/M/yyyy HH:mm:ss", "d/M/yyyy HH:mm",
                "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm",
                "dd-MM-yyyy HH:mm:ss", "dd-MM-yyyy HH:mm"
        };
        for (String p : patronesFechaHora) {
            try {
                return LocalDateTime.parse(limpio, DateTimeFormatter.ofPattern(p));
            } catch (DateTimeParseException ignored) {
            }
        }

        String[] patronesFecha = new String[]{
                "dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "d-M-yyyy", "yyyy/MM/dd"
        };
        for (String p : patronesFecha) {
            try {
                LocalDate d = LocalDate.parse(limpio, DateTimeFormatter.ofPattern(p));
                return d.atTime(12, 0);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private BigDecimal parsearMoneda(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String s = raw.trim()
                .replace("$", "")
                .replace("COP", "")
                .replaceAll("\\s+", "");
        if (s.isEmpty()) {
            return null;
        }

        boolean tienePunto = s.contains(".");
        boolean tieneComa = s.contains(",");

        if (tienePunto && tieneComa) {
            if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                s = s.replace(".", "").replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (tienePunto) {
            if (s.matches("^-?\\d{1,3}(\\.\\d{3})+$")) {
                s = s.replace(".", "");
            }
        } else if (tieneComa) {
            if (s.matches("^-?\\d{1,3}(,\\d{3})+$")) {
                s = s.replace(",", "");
            } else {
                s = s.replace(',', '.');
            }
        }

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parsearCantidadEntera(String raw) {
        BigDecimal bd = parsearMoneda(raw);
        if (bd == null) {
            return null;
        }
        try {
            BigDecimal limpio = bd.stripTrailingZeros();
            if (limpio.scale() > 0) {
                return null;
            }
            return Integer.valueOf(limpio.intValueExact());
        } catch (ArithmeticException e) {
            return null;
        }
    }

    private char detectarSeparadorCsv(List<String> lineas) {
        for (String l : lineas) {
            String s = limpiarBom(l).trim();
            if (!s.isEmpty()) {
                int semi = s.split(";", -1).length;
                int comma = s.split(",", -1).length;
                return semi >= comma ? ';' : ',';
            }
        }
        return ',';
    }

    private List<String> parsearLineaCsv(String linea, char separador) {
        List<String> campos = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean enComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') {
                if (enComillas && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    enComillas = !enComillas;
                }
            } else if (c == separador && !enComillas) {
                campos.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        campos.add(sb.toString().trim());
        return campos;
    }

    private String obtenerCeldaLista(List<String> lista, int idx) {
        if (idx < 0 || idx >= lista.size()) {
            return "";
        }
        String v = lista.get(idx);
        return v != null ? v.trim() : "";
    }

    private boolean estaTodoVacio(String a, String b, String c, String d, String e) {
        return (a == null || a.trim().isEmpty())
                && (b == null || b.trim().isEmpty())
                && (c == null || c.trim().isEmpty())
                && (d == null || d.trim().isEmpty())
                && (e == null || e.trim().isEmpty());
    }

    private String limpiarBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s != null ? s : "";
    }

    private String normalizarClaveNombre(String nombre) {
        return nombre != null ? nombre.trim().replaceAll("\\s+", " ").toLowerCase(LOCALE_ES_CO) : "";
    }

    private String quitarAcentos(String input) {
        if (input == null) {
            return "";
        }
        String norm = Normalizer.normalize(input, Normalizer.Form.NFD);
        return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public static String formatearMoneda(BigDecimal valor) {
        BigDecimal monto = valor != null ? valor : BigDecimal.ZERO;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_ES_CO);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols).format(monto);
    }
}
