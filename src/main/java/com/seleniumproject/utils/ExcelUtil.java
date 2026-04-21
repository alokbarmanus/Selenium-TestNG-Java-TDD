package com.seleniumproject.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ExcelUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ExcelUtil.class);

    private static final String RUN_MODE_COLUMN = "runMode";
    private static final String TEST_CASE_ID_COLUMN = "testCaseId";
    private static final String YES_VALUE = "yes";

    private ExcelUtil() {
    }

    public static List<Map<String, String>> getDataFromExcel(String dataFile, String sheetName, String tableKey) {
        Objects.requireNonNull(dataFile, "dataFile must not be null");
        Objects.requireNonNull(sheetName, "sheetName must not be null");
        Objects.requireNonNull(tableKey, "tableKey must not be null");

        Path filePath = Path.of(dataFile);
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Excel data file not found: " + dataFile);
        }

        try (InputStream inputStream = Files.newInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet not found: " + sheetName + " in file: " + dataFile);
            }

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            TableBounds bounds = findTableBounds(sheet, tableKey, formatter, evaluator);
            Map<Integer, String> headers = extractHeaders(sheet, bounds, formatter, evaluator);
            validateRequiredColumns(headers, tableKey);

            List<Map<String, String>> records = new ArrayList<>();
            for (int rowIndex = bounds.headerRowIndex + 1; rowIndex < bounds.endRowIndex; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                Map<String, String> rowData = new LinkedHashMap<>();
                boolean hasValue = false;

                for (Map.Entry<Integer, String> header : headers.entrySet()) {
                    String value = readCellValue(row.getCell(header.getKey()), formatter, evaluator);
                    if (!value.isBlank()) {
                        hasValue = true;
                    }
                    rowData.put(header.getValue(), value);
                }

                if (hasValue) {
                    records.add(rowData);
                }
            }

            LOG.info("Loaded {} row(s) from Excel table '{}' in sheet '{}'", records.size(), tableKey, sheetName);
            return Collections.unmodifiableList(records);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read Excel file: " + dataFile, e);
        }
    }

    public static List<Map<String, String>> getRunnableDataFromExcel(String dataFile, String sheetName, String tableKey) {
        List<Map<String, String>> allRecords = getDataFromExcel(dataFile, sheetName, tableKey);
        List<Map<String, String>> runnableRecords = new ArrayList<>();

        for (Map<String, String> record : allRecords) {
            String runMode = record.getOrDefault(RUN_MODE_COLUMN, "");
            if (YES_VALUE.equalsIgnoreCase(runMode.trim())) {
                runnableRecords.add(record);
            }
        }

        LOG.info("Filtered {} runnable row(s) from Excel table '{}'", runnableRecords.size(), tableKey);
        return Collections.unmodifiableList(runnableRecords);
    }

    public static Map<String, String> getRecordByTestCaseId(
        String dataFile, String sheetName, String tableKey, String testCaseId) {

        for (Map<String, String> record : getDataFromExcel(dataFile, sheetName, tableKey)) {
            if (testCaseId.equalsIgnoreCase(record.getOrDefault(TEST_CASE_ID_COLUMN, "").trim())) {
                return Collections.unmodifiableMap(new LinkedHashMap<>(record));
            }
        }

        throw new IllegalArgumentException(
            "No Excel record found for testCaseId '" + testCaseId + "' in table '" + tableKey + "'.");
    }

    public static Object[][] getDataFromXlsx(String dataFile, String sheetName, String tableKey) {
        List<Map<String, String>> runnableRecords = getRunnableDataFromExcel(dataFile, sheetName, tableKey);
        Object[][] dataProvider = new Object[runnableRecords.size()][1];

        for (int index = 0; index < runnableRecords.size(); index++) {
            dataProvider[index][0] = runnableRecords.get(index);
        }

        return dataProvider;
    }

    public static Object[][] getDataFromxlsx(String dataFile, String sheetName, String tableKey) {
        return getDataFromXlsx(dataFile, sheetName, tableKey);
    }

    private static TableBounds findTableBounds(
        Sheet sheet, String tableKey, DataFormatter formatter, FormulaEvaluator evaluator) {

        Cell startCell = null;
        Cell endCell = null;

        for (Row row : sheet) {
            for (Cell cell : row) {
                String value = readCellValue(cell, formatter, evaluator);
                if (tableKey.equalsIgnoreCase(value.trim())) {
                    if (startCell == null) {
                        startCell = cell;
                    } else {
                        endCell = cell;
                    }
                }
            }
        }

        if (startCell == null || endCell == null) {
            throw new IllegalArgumentException(
                "Table key '" + tableKey + "' must appear exactly twice in sheet '" + sheet.getSheetName() + "'.");
        }

        if (endCell.getRowIndex() <= startCell.getRowIndex() || endCell.getColumnIndex() <= startCell.getColumnIndex()) {
            throw new IllegalArgumentException(
                "Invalid Excel table bounds for key '" + tableKey + "'. Ensure the second marker is bottom-right of the first.");
        }

        return new TableBounds(
            startCell.getRowIndex(),
            startCell.getColumnIndex(),
            endCell.getRowIndex(),
            endCell.getColumnIndex());
    }

    private static Map<Integer, String> extractHeaders(
        Sheet sheet, TableBounds bounds, DataFormatter formatter, FormulaEvaluator evaluator) {

        Row headerRow = sheet.getRow(bounds.headerRowIndex);
        if (headerRow == null) {
            throw new IllegalArgumentException("Header row not found for Excel table.");
        }

        Map<Integer, String> headers = new LinkedHashMap<>();
        for (int columnIndex = bounds.startColumnIndex + 1; columnIndex < bounds.endColumnIndex; columnIndex++) {
            String header = readCellValue(headerRow.getCell(columnIndex), formatter, evaluator).trim();
            if (header.isBlank()) {
                throw new IllegalArgumentException(
                    "Blank header found at column index " + columnIndex + " for Excel table.");
            }
            headers.put(columnIndex, header);
        }

        return headers;
    }

    private static void validateRequiredColumns(Map<Integer, String> headers, String tableKey) {
        List<String> headerValues = new ArrayList<>(headers.values());
        boolean hasRunMode = headerValues.stream().anyMatch(name -> RUN_MODE_COLUMN.equalsIgnoreCase(name));
        boolean hasTestCaseId = headerValues.stream().anyMatch(name -> TEST_CASE_ID_COLUMN.equalsIgnoreCase(name));

        if (!hasRunMode || !hasTestCaseId) {
            throw new IllegalArgumentException(
                "Excel table '" + tableKey + "' must contain 'runMode' and 'testCaseId' columns.");
        }
    }

    private static String readCellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private static final class TableBounds {
        private final int headerRowIndex;
        private final int startColumnIndex;
        private final int endRowIndex;
        private final int endColumnIndex;

        private TableBounds(int headerRowIndex, int startColumnIndex, int endRowIndex, int endColumnIndex) {
            this.headerRowIndex = headerRowIndex;
            this.startColumnIndex = startColumnIndex;
            this.endRowIndex = endRowIndex;
            this.endColumnIndex = endColumnIndex;
        }
    }
}