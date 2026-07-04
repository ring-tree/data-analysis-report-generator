package com.audaque;

import com.audaque.parser.CsvDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CsvDataParser} 的单元测试。
 */
class CsvDataParserTest {

    private CsvDataParser parser;
    private Path sampleCsvPath;

    @BeforeEach
    void setUp() throws URISyntaxException {
        parser = new CsvDataParser();
        sampleCsvPath = Path.of(Objects.requireNonNull(
                getClass().getClassLoader().getResource("sample.csv")).toURI());
    }

    @Test
    @DisplayName("应正确解析 CSV 文件并返回数值数据")
    void shouldParseCsvAndReturnNumericData() throws IOException {
        List<List<Double>> data = parser.parse(sampleCsvPath.toString());

        assertNotNull(data, "解析结果不应为 null");
        assertFalse(data.isEmpty(), "解析结果不应为空");
        assertTrue(data.size() >= 10, "应至少解析出 10 行有效数据");
    }

    @Test
    @DisplayName("应正确识别并返回列名列表")
    void shouldReturnCorrectColumnNames() throws IOException {
        parser.parse(sampleCsvPath.toString());
        List<String> columnNames = parser.getColumnNames();

        assertNotNull(columnNames);
        assertEquals(6, columnNames.size(), "应有 6 列");
        assertEquals("ID", columnNames.get(0));
        assertEquals("Score", columnNames.get(1));
        assertEquals("Age", columnNames.get(2));
        assertEquals("GPA", columnNames.get(3));
        assertEquals("Attendance", columnNames.get(4));
        assertEquals("Hours", columnNames.get(5));
    }

    @Test
    @DisplayName("应跳过表头行，不把列名当作数据")
    void shouldSkipHeaderRow() throws IOException {
        List<List<Double>> data = parser.parse(sampleCsvPath.toString());

        for (List<Double> row : data) {
            assertFalse(row.isEmpty(), "每行数据不应为空");
            for (Double value : row) {
                assertNotNull(value, "每个单元格值不应为 null");
            }
        }
    }

    @Test
    @DisplayName("应跳过非数值单元格并继续解析后续数据")
    void shouldSkipNonNumericCells() throws IOException {
        List<List<Double>> data = parser.parse(sampleCsvPath.toString());

        for (List<Double> row : data) {
            for (Double value : row) {
                assertDoesNotThrow(() -> {
                    double v = value;
                }, "每个值都应是合法的 Double");
            }
        }
    }

    @Test
    @DisplayName("未调用 parse 时 getColumnNames 应返回空列表")
    void shouldReturnEmptyColumnNamesBeforeParse() {
        List<String> columnNames = parser.getColumnNames();
        assertNotNull(columnNames);
        assertTrue(columnNames.isEmpty());
    }

    @Test
    @DisplayName("解析不存在的文件应抛出 IOException")
    void shouldThrowIOExceptionForNonExistentFile() {
        assertThrows(IOException.class, () -> {
            parser.parse("nonexistent_file.csv");
        }, "解析不存在的文件应抛出 IOException");
    }
}
