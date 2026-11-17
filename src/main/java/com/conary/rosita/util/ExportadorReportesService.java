package com.conary.rosita.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Servicio de exportacion real de reportes a Excel (.xlsx con Apache POI) y PDF 1.4 nativo en Java 8.
 */
public class ExportadorReportesService {

    public enum Formato {
        PDF,
        EXCEL
    }

    public static class DatosExportacion {
        private final String titulo;
        private final String subtitulo;
        private final List<String> encabezados;
        private final List<List<String>> filas;
        private final List<String> filaTotales;
        private final String notaPie;

        public DatosExportacion(String titulo, String subtitulo,
                                List<String> encabezados, List<List<String>> filas,
                                List<String> filaTotales, String notaPie) {
            this.titulo = titulo != null ? titulo : "Reporte";
            this.subtitulo = subtitulo != null ? subtitulo : "";
            this.encabezados = encabezados != null ? encabezados : Collections.<String>emptyList();
            this.filas = filas != null ? filas : Collections.<List<String>>emptyList();
            this.filaTotales = filaTotales;
            this.notaPie = notaPie;
        }

        public String getTitulo() { return titulo; }
        public String getSubtitulo() { return subtitulo; }
        public List<String> getEncabezados() { return encabezados; }
        public List<List<String>> getFilas() { return filas; }
        public List<String> getFilaTotales() { return filaTotales; }
        public String getNotaPie() { return notaPie; }
    }

    public static File obtenerCarpetaDocumentosPorDefecto() {
        String home = System.getProperty("user.home");
        File docs = new File(home, "Documents");
        if (!docs.exists()) {
            File docsEs = new File(home, "Documentos");
            if (docsEs.exists()) {
                return docsEs;
            }
        }
        return docs.exists() ? docs : new File(home);
    }

    public File exportar(DatosExportacion datos, Formato formato, File carpetaDestino) throws IOException {
        if (datos == null) {
            throw new IllegalArgumentException("No hay datos para exportar.");
        }
        if (carpetaDestino == null) {
            carpetaDestino = obtenerCarpetaDocumentosPorDefecto();
        }
        if (!carpetaDestino.exists() && !carpetaDestino.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta destino: " + carpetaDestino.getAbsolutePath());
        }

        String ext = (formato == Formato.EXCEL) ? "xlsx" : "pdf";
        File archivo = construirArchivoUnico(carpetaDestino, datos.getTitulo(), ext);

        if (formato == Formato.EXCEL) {
            generarExcel(datos, archivo);
        } else {
            generarPdf(datos, archivo);
        }
        return archivo;
    }

    private File construirArchivoUnico(File carpeta, String titulo, String ext) {
        String baseSlug = titulo.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (baseSlug.isEmpty()) {
            baseSlug = "Reporte";
        }
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        String nombre = "CONARY_" + baseSlug + "_" + fecha + "." + ext;
        File candidato = new File(carpeta, nombre);
        int idx = 2;
        while (candidato.exists()) {
            candidato = new File(carpeta, "CONARY_" + baseSlug + "_" + fecha + "_" + idx + "." + ext);
            idx++;
        }
        return candidato;
    }

    private void generarExcel(DatosExportacion datos, File archivo) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Reporte");

            Font fontTitulo = wb.createFont();
            fontTitulo.setBold(true);
            fontTitulo.setFontHeightInPoints((short) 14);

            Font fontSub = wb.createFont();
            fontSub.setFontHeightInPoints((short) 11);

            Font fontHeader = wb.createFont();
            fontHeader.setBold(true);
            fontHeader.setFontHeightInPoints((short) 11);
            fontHeader.setColor(IndexedColors.WHITE.getIndex());

            Font fontTotal = wb.createFont();
            fontTotal.setBold(true);
            fontTotal.setFontHeightInPoints((short) 11);

            CellStyle styleTitulo = wb.createCellStyle();
            styleTitulo.setFont(fontTitulo);

            CellStyle styleSub = wb.createCellStyle();
            styleSub.setFont(fontSub);

            CellStyle styleHeader = wb.createCellStyle();
            styleHeader.setFont(fontHeader);
            styleHeader.setFillForegroundColor(IndexedColors.BROWN.getIndex());
            styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleHeader.setAlignment(HorizontalAlignment.LEFT);
            styleHeader.setBorderBottom(BorderStyle.THIN);

            CellStyle styleBody = wb.createCellStyle();
            styleBody.setBorderBottom(BorderStyle.THIN);
            styleBody.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

            CellStyle styleTotal = wb.createCellStyle();
            styleTotal.setFont(fontTotal);
            styleTotal.setFillForegroundColor(IndexedColors.TAN.getIndex());
            styleTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleTotal.setBorderTop(BorderStyle.MEDIUM);
            styleTotal.setBorderBottom(BorderStyle.MEDIUM);

            int rowIdx = 0;
            Row r0 = sheet.createRow(rowIdx++);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("Papelería de Rosita — CONARY v1.0");
            c0.setCellStyle(styleTitulo);

            Row r1 = sheet.createRow(rowIdx++);
            Cell c1 = r1.createCell(0);
            c1.setCellValue(datos.getTitulo() + (datos.getSubtitulo().isEmpty() ? "" : " · " + datos.getSubtitulo()));
            c1.setCellStyle(styleSub);

            rowIdx++; // fila en blanco

            Row headerRow = sheet.createRow(rowIdx++);
            List<String> headers = datos.getEncabezados();
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers.get(col));
                cell.setCellStyle(styleHeader);
            }

            for (List<String> fila : datos.getFilas()) {
                Row r = sheet.createRow(rowIdx++);
                for (int col = 0; col < headers.size(); col++) {
                    Cell cell = r.createCell(col);
                    String val = col < fila.size() && fila.get(col) != null ? fila.get(col) : "";
                    cell.setCellValue(val);
                    cell.setCellStyle(styleBody);
                }
            }

            if (datos.getFilaTotales() != null && !datos.getFilaTotales().isEmpty()) {
                Row rt = sheet.createRow(rowIdx++);
                for (int col = 0; col < headers.size(); col++) {
                    Cell cell = rt.createCell(col);
                    String val = col < datos.getFilaTotales().size() && datos.getFilaTotales().get(col) != null
                            ? datos.getFilaTotales().get(col)
                            : "";
                    cell.setCellValue(val);
                    cell.setCellStyle(styleTotal);
                }
            }

            if (datos.getNotaPie() != null && !datos.getNotaPie().trim().isEmpty()) {
                rowIdx++;
                Row rn = sheet.createRow(rowIdx);
                Cell cn = rn.createCell(0);
                cn.setCellValue(datos.getNotaPie());
                cn.setCellStyle(styleSub);
            }

            for (int col = 0; col < headers.size(); col++) {
                sheet.autoSizeColumn(col);
                int width = sheet.getColumnWidth(col) + 900;
                sheet.setColumnWidth(col, Math.min(width, 14000));
            }

            try (FileOutputStream fos = new FileOutputStream(archivo)) {
                wb.write(fos);
            }
        }
    }

    /**
     * Genera un documento PDF 1.4 valido en Java 8 puro (sin librerias externas).
     */
    private void generarPdf(DatosExportacion datos, File archivo) throws IOException {
        List<String> headers = datos.getEncabezados();
        int cols = Math.max(1, headers.size());
        boolean landscape = cols >= 6;
        float pageW = landscape ? 792f : 612f;
        float pageH = landscape ? 612f : 792f;
        float margin = 38f;
        float usableW = pageW - (margin * 2f);
        float colW = usableW / cols;

        List<List<String>> todasLasFilas = new ArrayList<List<String>>(datos.getFilas());
        boolean tieneTotales = datos.getFilaTotales() != null && !datos.getFilaTotales().isEmpty();

        // Construir contenido por paginas
        List<String> streamsPagina = new ArrayList<String>();
        float rowHeight = 20f;
        float yBottomLimit = 62f;

        int filaIdx = 0;
        int pageNum = 1;
        while (filaIdx < todasLasFilas.size() || pageNum == 1) {
            StringBuilder sb = new StringBuilder();
            float y = pageH - margin;

            // Cabecera de pagina
            sb.append("0.275 0.133 0.059 rg\n");
            sb.append(pdfText("F2", 14, margin, y, "Papelería de Rosita — CONARY v1.0"));
            y -= 20f;
            sb.append("0.439 0.259 0.078 rg\n");
            sb.append(pdfText("F2", 12, margin, y, datos.getTitulo()));
            y -= 15f;
            sb.append("0.35 0.25 0.18 rg\n");
            if (!datos.getSubtitulo().isEmpty()) {
                sb.append(pdfText("F1", 10, margin, y, datos.getSubtitulo()));
            }
            y -= 10f;

            // Linea divisoria
            sb.append("0.85 0.78 0.70 RG 0.8 w ");
            sb.append(fmt(margin)).append(" ").append(fmt(y)).append(" m ")
              .append(fmt(pageW - margin)).append(" ").append(fmt(y)).append(" l S\n");
            y -= 18f;

            // Encabezado de tabla
            sb.append("0.976 0.922 0.867 rg ");
            sb.append(fmt(margin)).append(" ").append(fmt(y - 16f)).append(" ")
              .append(fmt(usableW)).append(" 20 re f\n");
            sb.append("0.275 0.133 0.059 rg\n");
            for (int c = 0; c < headers.size(); c++) {
                String h = recorte(headers.get(c), (int) (colW / 5.8f));
                sb.append(pdfText("F2", 9.5f, margin + (c * colW) + 4f, y - 10f, h));
            }
            y -= 22f;

            // Filas de datos en esta pagina
            if (todasLasFilas.isEmpty() && pageNum == 1) {
                sb.append("0.45 0.35 0.28 rg\n");
                sb.append(pdfText("F1", 10, margin + 4f, y - 12f, "No hay registros para mostrar en este reporte."));
                y -= rowHeight;
            }

            while (filaIdx < todasLasFilas.size() && (y - rowHeight) >= yBottomLimit) {
                List<String> row = todasLasFilas.get(filaIdx);
                sb.append("0.18 0.12 0.08 rg\n");
                for (int c = 0; c < headers.size(); c++) {
                    String val = c < row.size() && row.get(c) != null ? row.get(c) : "";
                    val = recorte(val, (int) (colW / 5.3f));
                    sb.append(pdfText("F1", 9f, margin + (c * colW) + 4f, y - 12f, val));
                }
                sb.append("0.92 0.88 0.84 RG 0.5 w ");
                sb.append(fmt(margin)).append(" ").append(fmt(y - rowHeight + 2f)).append(" m ")
                  .append(fmt(pageW - margin)).append(" ").append(fmt(y - rowHeight + 2f)).append(" l S\n");
                y -= rowHeight;
                filaIdx++;
            }

            // Si terminamos todas las filas, dibujar totales y nota si caben
            if (filaIdx >= todasLasFilas.size()) {
                if (tieneTotales && (y - rowHeight) >= yBottomLimit) {
                    sb.append("0.988 0.890 0.808 rg ");
                    sb.append(fmt(margin)).append(" ").append(fmt(y - 16f)).append(" ")
                      .append(fmt(usableW)).append(" 20 re f\n");
                    sb.append("0.243 0.118 0.043 rg\n");
                    for (int c = 0; c < headers.size(); c++) {
                        String val = c < datos.getFilaTotales().size() && datos.getFilaTotales().get(c) != null
                                ? datos.getFilaTotales().get(c) : "";
                        val = recorte(val, (int) (colW / 5.6f));
                        sb.append(pdfText("F2", 9.5f, margin + (c * colW) + 4f, y - 10f, val));
                    }
                    y -= 24f;
                    tieneTotales = false;
                }
                if (!tieneTotales && datos.getNotaPie() != null && !datos.getNotaPie().trim().isEmpty()) {
                    sb.append("0.42 0.30 0.21 rg\n");
                    sb.append(pdfText("F1", 8.5f, margin, Math.max(y - 12f, 44f), datos.getNotaPie()));
                }
            }

            // Pie de pagina
            sb.append("0.45 0.35 0.28 rg\n");
            sb.append(pdfText("F1", 8f, margin, 24f, "Generado por Papelería de Rosita — CONARY v1.0"));
            sb.append(pdfText("F1", 8f, pageW - margin - 52f, 24f, "Página " + pageNum));

            streamsPagina.add(sb.toString());
            pageNum++;
            if (filaIdx >= todasLasFilas.size() && !tieneTotales) {
                break;
            }
        }

        escribirDocumentoPdf14(streamsPagina, pageW, pageH, archivo);
    }

    private void escribirDocumentoPdf14(List<String> streamsPagina, float pageW, float pageH, File archivo) throws IOException {
        List<byte[]> objetos = new ArrayList<byte[]>();
        // Obj 1: Catalog, Obj 2: Pages, Obj 3: Font F1 (Helvetica), Obj 4: Font F2 (Helvetica-Bold)
        int numPages = streamsPagina.size();
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < numPages; i++) {
            int pageObjNum = 5 + (i * 2);
            kids.append(pageObjNum).append(" 0 R ");
        }

        objetos.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.US_ASCII));
        objetos.add(("<< /Type /Pages /Kids [ " + kids.toString() + "] /Count " + numPages + " >>")
                .getBytes(StandardCharsets.US_ASCII));
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"
                .getBytes(StandardCharsets.US_ASCII));
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"
                .getBytes(StandardCharsets.US_ASCII));

        for (int i = 0; i < numPages; i++) {
            int pageObjNum = 5 + (i * 2);
            int contentObjNum = pageObjNum + 1;
            String pageDict = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                    + fmt(pageW) + " " + fmt(pageH)
                    + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "
                    + contentObjNum + " 0 R >>";
            objetos.add(pageDict.getBytes(StandardCharsets.US_ASCII));

            byte[] streamBytes = streamsPagina.get(i).getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream contentObj = new ByteArrayOutputStream();
            contentObj.write(("<< /Length " + streamBytes.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII));
            contentObj.write(streamBytes);
            contentObj.write("\nendstream".getBytes(StandardCharsets.US_ASCII));
            objetos.add(contentObj.toByteArray());
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        List<Integer> offsets = new ArrayList<Integer>();
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(pdf.size());
            pdf.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
            pdf.write(objetos.get(i));
            pdf.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }

        int xrefOffset = pdf.size();
        pdf.write(("xref\n0 " + (objetos.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (Integer off : offsets) {
            pdf.write(String.format("%010d 00000 n \n", off).getBytes(StandardCharsets.US_ASCII));
        }
        pdf.write(("trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                + xrefOffset + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));

        try (FileOutputStream fos = new FileOutputStream(archivo)) {
            fos.write(pdf.toByteArray());
        }
    }

    private String pdfText(String fontAlias, float size, float x, float y, String texto) {
        String limpio = normalizarWinAnsi(texto)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
        return "BT /" + fontAlias + " " + fmt(size) + " Tf " + fmt(x) + " " + fmt(y)
                + " Td (" + limpio + ") Tj ET\n";
    }

    private String normalizarWinAnsi(String texto) {
        if (texto == null) return "";
        return texto
                .replace("\u2212", "-")
                .replace("\u2013", "-")
                .replace("\u2014", "-")
                .replace("\u2022", "*")
                .replace("\u2139", "i")
                .replace("\u26A0", "!")
                .replace("\u2713", "OK")
                .replace("\u2298", "-")
                .replace("\u2297", "X");
    }

    private String recorte(String texto, int maxChars) {
        if (texto == null) return "";
        int limit = Math.max(6, maxChars);
        if (texto.length() <= limit) return texto;
        return texto.substring(0, limit - 2) + "..";
    }

    private String fmt(float v) {
        return String.format(java.util.Locale.US, "%.2f", Float.valueOf(v));
    }
}