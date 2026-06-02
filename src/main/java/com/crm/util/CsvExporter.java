package com.crm.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CsvExporter {

    public static InputStream build(String[] headers, Iterable<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(joinRow(headers)).append("\r\n");
        for (String[] row : rows) {
            sb.append(joinRow(row)).append("\r\n");
        }
        return new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String joinRow(String[] cells) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) row.append(',');
            row.append(escape(cells[i]));
        }
        return row.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static String str(Object value) {
        return value == null ? "" : value.toString();
    }
}
