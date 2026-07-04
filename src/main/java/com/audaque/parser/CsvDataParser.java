package com.audaque.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CSV 数据解析器，负责将 CSV 文件解析为数值数据矩阵。
 */
public class CsvDataParser {

    private static final Logger logger = LoggerFactory.getLogger(CsvDataParser.class);

    private List<String> columnNames;

    /**
     * 构造解析器。
     */
    public CsvDataParser() {
        this.columnNames = Collections.emptyList();
    }

    /**
     * 解析 CSV 文件，自动检测并跳过表头行，将每行数据解析为 Double 列表。
     * 非数值单元格将被跳过，并记录警告日志。
     *
     * @param filePath CSV 文件路径
     * @return 所有有效数值行数据，每行为一个 Double 列表
     * @throws IOException 文件读取或解析失败
     */
    public List<List<Double>> parse(String filePath) throws IOException {
        CSVFormat format = CSVFormat.Builder.create()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        List<List<Double>> data = new ArrayList<>();

        try (Reader reader = new FileReader(filePath);
             CSVParser parser = format.parse(reader)) {

            this.columnNames = new ArrayList<>(parser.getHeaderNames());
            logger.info("Detected {} columns: {}", columnNames.size(), columnNames);

            int totalRows = 0;
            int validRows = 0;
            int skippedCells = 0;

            for (CSVRecord record : parser) {
                totalRows++;
                List<Double> row = new ArrayList<>();

                for (int i = 0; i < columnNames.size(); i++) {
                    String cellValue = record.get(i);
                    if (cellValue == null || cellValue.isBlank()) {
                        skippedCells++;
                        continue;
                    }
                    try {
                        row.add(Double.parseDouble(cellValue));
                    } catch (NumberFormatException e) {
                        skippedCells++;
                        logger.warn("Skipping non-numeric value '{}' in column '{}', row {}",
                                cellValue, columnNames.get(i), totalRows);
                    }
                }

                if (!row.isEmpty()) {
                    data.add(row);
                    validRows++;
                }
            }

            logger.info("Parsed {} total rows: {} valid numeric rows, {} non-numeric cells skipped",
                    totalRows, validRows, skippedCells);
        }

        return data;
    }

    /**
     * 获取最近一次解析得到的列名列表。
     *
     * @return 列名列表，未调用 parse 时返回空列表
     */
    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columnNames);
    }
}
