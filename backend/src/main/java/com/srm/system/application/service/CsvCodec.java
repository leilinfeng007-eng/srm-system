package com.srm.system.application.service;

import com.srm.common.exception.BusinessException;
import com.srm.common.exception.ErrorCode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class CsvCodec {
    record CsvData(List<String> headers, List<List<String>> rows) {}

    private CsvCodec() {}

    static CsvData parse(byte[] content) {
        final String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException invalidEncoding) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV must be UTF-8");
        }
        List<List<String>> all = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"'); i++;
                } else if (ch == '"') quoted = false;
                else field.append(ch);
            } else if (ch == '"' && field.length() == 0) quoted = true;
            else if (ch == ',') { row.add(field.toString()); field.setLength(0); }
            else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(field.toString()); field.setLength(0);
                if (!(row.size() == 1 && row.get(0).isBlank())) all.add(List.copyOf(row));
                row.clear();
            } else field.append(ch);
        }
        if (quoted) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV has an unclosed quote");
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            if (!(row.size() == 1 && row.get(0).isBlank())) all.add(List.copyOf(row));
        }
        if (all.isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV has no header");
        List<String> headers = new ArrayList<>(all.get(0));
        if (!headers.isEmpty() && headers.get(0).startsWith("\ufeff")) {
            headers.set(0, headers.get(0).substring(1));
        }
        if (headers.stream().anyMatch(String::isBlank) || headers.stream().distinct().count() != headers.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CSV header is blank or duplicated");
        }
        if (all.size() > 10001) throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "CSV exceeds the 10000 row limit");
        return new CsvData(List.copyOf(headers), all.subList(1, all.size()));
    }

    static byte[] encode(List<String> headers, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, headers);
        for (List<String> row : rows) appendRow(csv, row);
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder csv, List<String> row) {
        for (int i = 0; i < row.size(); i++) {
            if (i > 0) csv.append(',');
            String value = row.get(i) == null ? "" : row.get(i);
            boolean quote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                    || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
            if (quote) csv.append('"').append(value.replace("\"", "\"\"")).append('"');
            else csv.append(value);
        }
        csv.append('\n');
    }
}
