package com.payment.rag.service;

import com.payment.rag.model.RagUnit;
import com.payment.rag.model.SourceType;
import com.payment.rag.util.CharsetUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TabularRowChunker {

    public List<RagUnit> chunkCsv(InputStream input, String filename, SourceType sourceType) {
        try {
            byte[] bytes = input.readAllBytes();
            String rawText = readStringWithFallback(bytes);
            java.io.StringReader reader = new java.io.StringReader(rawText);
            try (CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {
                List<RowPayload> rows = new ArrayList<>();
                for (CSVRecord record : parser) {
                    List<String> values = new ArrayList<>();
                    record.forEach(values::add);
                    rows.add(new RowPayload(filename, record.getRecordNumber(), values));
                }
                return buildUnits(rows, filename, sourceType, false);
            }
        } catch (IOException e) {
            throw new RuntimeException("CSV 行切片失败: " + filename, e);
        }
    }

    private String readStringWithFallback(byte[] bytes) {
        return CharsetUtils.readStringWithFallback(bytes);
    }

    public List<RagUnit> chunkWorkbook(InputStream input, String filename, SourceType sourceType) {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            List<RagUnit> units = new ArrayList<>();
            int chunkIndex = 0;
            DataFormatter formatter = new DataFormatter();

            for (Sheet sheet : workbook) {
                List<RowPayload> rows = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> values = new ArrayList<>();
                    int lastCellNum = Math.max(row.getLastCellNum(), 0);
                    for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                        values.add(formatter.formatCellValue(row.getCell(cellIndex)));
                    }
                    rows.add(new RowPayload(sheet.getSheetName(), row.getRowNum() + 1L, values));
                }

                List<RagUnit> sheetUnits = buildUnits(rows, filename, sourceType, true);
                for (RagUnit unit : sheetUnits) {
                    unit.setChunkIndex(chunkIndex++);
                    units.add(unit);
                }
            }

            return units;
        } catch (Exception e) {
            throw new RuntimeException("Excel 行切片失败: " + filename, e);
        }
    }

    private List<RagUnit> buildUnits(List<RowPayload> rows,
                                     String filename,
                                     SourceType sourceType,
                                     boolean includeSheetName) {
        if (rows.isEmpty()) {
            return List.of();
        }

        List<String> headers = inferHeaders(rows);
        int dataStartIndex = rows.size() > 1 ? 1 : 0;
        List<RagUnit> units = new ArrayList<>();
        int chunkIndex = 0;

        for (int rowIndex = dataStartIndex; rowIndex < rows.size(); rowIndex++) {
            RowPayload payload = rows.get(rowIndex);
            String content = buildRowContent(payload, headers, filename, includeSheetName);
            if (content.isBlank()) {
                continue;
            }

            RagUnit unit = new RagUnit();
            unit.setSourceType(sourceType);
            unit.setTitle(payload.label() + " Row " + payload.rowNumber());
            unit.setContent(content);
            unit.setChunkIndex(chunkIndex++);
            units.add(unit);
        }

        return units;
    }

    private List<String> inferHeaders(List<RowPayload> rows) {
        if (rows.size() <= 1) {
            int width = rows.get(0).values().size();
            List<String> genericHeaders = new ArrayList<>();
            for (int index = 0; index < width; index++) {
                genericHeaders.add("column_" + (index + 1));
            }
            return genericHeaders;
        }

        List<String> headers = new ArrayList<>();
        List<String> firstRow = rows.get(0).values();
        for (int index = 0; index < firstRow.size(); index++) {
            String header = firstRow.get(index) == null ? "" : firstRow.get(index).trim();
            headers.add(header.isBlank() ? "column_" + (index + 1) : header);
        }
        return headers;
    }

    private String buildRowContent(RowPayload payload,
                                   List<String> headers,
                                   String filename,
                                   boolean includeSheetName) {
        List<String> fields = new ArrayList<>();
        List<String> values = payload.values();
        for (int index = 0; index < values.size(); index++) {
            String value = values.get(index) == null ? "" : values.get(index).trim();
            if (value.isBlank()) {
                continue;
            }
            String header = index < headers.size() ? headers.get(index) : "column_" + (index + 1);
            fields.add(header + ": " + value);
        }

        if (fields.isEmpty()) {
            return "";
        }

        StringBuilder content = new StringBuilder();
        content.append("来源文件: ").append(filename).append('\n');
        if (includeSheetName) {
            content.append("工作表: ").append(payload.label()).append('\n');
        }
        content.append("行号: ").append(payload.rowNumber()).append('\n');
        content.append(String.join("\n", fields));
        return content.toString();
    }

    private record RowPayload(String label, long rowNumber, List<String> values) {
    }
}
