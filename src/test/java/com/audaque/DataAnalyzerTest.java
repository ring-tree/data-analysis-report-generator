package com.audaque;

import com.audaque.analysis.DataAnalyzer;
import com.audaque.model.ColumnStat;
import com.audaque.model.DataSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link DataAnalyzer} 的单元测试。
 */
class DataAnalyzerTest {

    private DataAnalyzer analyzer;
    private List<List<Double>> sampleData;
    private List<String> columnNames;

    @BeforeEach
    void setUp() {
        analyzer = new DataAnalyzer();
        columnNames = Arrays.asList("Score", "Age", "GPA");
        sampleData = Arrays.asList(
                Arrays.asList(85.5, 20.0, 3.6),
                Arrays.asList(90.0, 21.0, 3.8),
                Arrays.asList(76.5, 19.0, 3.2),
                Arrays.asList(88.0, 22.0, 3.9),
                Arrays.asList(92.5, 20.0, 4.0),
                Arrays.asList(null, 18.0, 3.1),
                Arrays.asList(81.0, 21.0, null),
                Arrays.asList(79.5, 23.0, 3.4),
                Arrays.asList(95.0, 20.0, 3.7),
                Arrays.asList(87.0, null, 3.3),
                Collections.emptyList(),
                Arrays.asList(91.5, 22.0, 3.5)
        );
    }

    @Test
    @DisplayName("基本统计量计算应返回正确列数")
    void calculateBasicStatsShouldReturnCorrectColumnCount() {
        List<ColumnStat> stats = analyzer.calculateBasicStats(sampleData);

        assertNotNull(stats);
        assertEquals(3, stats.size(), "应有 3 列的统计结果");
    }

    @Test
    @DisplayName("均值计算应正确")
    void shouldCalculateMeanCorrectly() {
        List<ColumnStat> stats = analyzer.calculateBasicStats(sampleData);

        ColumnStat scoreStat = stats.get(0);
        assertTrue(scoreStat.mean() > 80 && scoreStat.mean() < 100,
                "Score 均值应在合理范围内，实际: " + scoreStat.mean());
        assertTrue(scoreStat.count() > 0);
    }

    @Test
    @DisplayName("缺失值计数应正确")
    void shouldCountMissingValuesCorrectly() {
        List<ColumnStat> stats = analyzer.calculateBasicStats(sampleData);

        ColumnStat scoreStat = stats.get(0);
        assertEquals(2, scoreStat.missingCount(), "Score 列应有 2 个缺失值（1 个显式 null + 1 个空行）");

        ColumnStat ageStat = stats.get(1);
        assertEquals(2, ageStat.missingCount(), "Age 列应有 2 个缺失值（1 个显式 null + 1 个空行）");

        ColumnStat gpaStat = stats.get(2);
        assertEquals(2, gpaStat.missingCount(), "GPA 列应有 2 个缺失值（1 个显式 null + 1 个空行）");
    }

    @Test
    @DisplayName("应检测出 IQR 异常值")
    void shouldDetectOutliers() {
        List<Double> column = Arrays.asList(10.0, 12.0, 11.0, 13.0, 12.5, 100.0, 11.5, -50.0, 12.0, 10.5);

        List<Integer> outliers = analyzer.detectOutliers(column);

        assertNotNull(outliers);
        assertFalse(outliers.isEmpty(), "应检测到异常值 100.0 和 -50.0");
        assertTrue(outliers.contains(5), "索引 5（值 100.0）应为异常值");
        assertTrue(outliers.contains(7), "索引 7（值 -50.0）应为异常值");
    }

    @Test
    @DisplayName("正常数据应无异常值")
    void shouldReturnEmptyForNormalData() {
        List<Double> column = Arrays.asList(10.0, 12.0, 11.0, 13.0, 12.5);

        List<Integer> outliers = analyzer.detectOutliers(column);

        assertTrue(outliers.isEmpty(), "正态分布数据应无异常值");
    }

    @Test
    @DisplayName("数据不足时应返回空列表")
    void shouldReturnEmptyForInsufficientOutlierData() {
        List<Double> column = Arrays.asList(10.0, 12.0, 11.0);

        List<Integer> outliers = analyzer.detectOutliers(column);

        assertTrue(outliers.isEmpty(), "数据点不足 4 个时应返回空列表");
    }

    @Test
    @DisplayName("应正确生成完整 DataSummary")
    void shouldGenerateCorrectSummary() {
        DataSummary summary = analyzer.generateSummary(sampleData, columnNames);

        assertNotNull(summary);
        assertEquals(12, summary.totalRows(), "总行数应为 12");
        assertEquals(1, summary.emptyRows(), "应有 1 行空行（第 11 行）");
        assertEquals(11, summary.validRows(), "应有 11 行有效行");
        assertEquals(3, summary.columnCount(), "应有 3 列");
        assertEquals(columnNames, summary.columnNames(), "列名应与输入一致");
        assertNotNull(summary.columnStats());
        assertEquals(3, summary.columnStats().size(), "应有 3 列的统计信息");

        assertTrue(summary.columnStats().containsKey("Score"));
        assertTrue(summary.columnStats().containsKey("Age"));
        assertTrue(summary.columnStats().containsKey("GPA"));
    }

    @Test
    @DisplayName("空数据应正确处理")
    void shouldHandleEmptyData() {
        DataSummary summary = analyzer.generateSummary(
                Collections.emptyList(),
                columnNames);

        assertNotNull(summary);
        assertEquals(0, summary.totalRows());
        assertEquals(0, summary.emptyRows());
        assertEquals(0, summary.validRows());
    }

    @Test
    @DisplayName("单行单列数据应正确处理")
    void shouldHandleSingleRowSingleColumn() {
        List<List<Double>> singleData = Collections.singletonList(
                Collections.singletonList(42.0));
        List<String> singleCol = Collections.singletonList("Value");

        DataSummary summary = analyzer.generateSummary(singleData, singleCol);

        assertNotNull(summary);
        assertEquals(1, summary.totalRows());
        assertEquals(1, summary.validRows());
        assertEquals(1, summary.columnCount());

        ColumnStat stat = summary.columnStats().get("Value");
        assertNotNull(stat);
        assertEquals(42.0, stat.mean(), 0.001);
        assertEquals(42.0, stat.median(), 0.001);
        assertEquals(42.0, stat.min(), 0.001);
        assertEquals(42.0, stat.max(), 0.001);
        assertEquals(0.0, stat.stdDev(), 0.001);
        assertEquals(0, stat.missingCount());
        assertEquals(0, stat.outlierCount());
    }

    @Test
    @DisplayName("全空列应正确返回零值统计")
    void shouldHandleAllNullColumn() {
        List<List<Double>> data = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            data.add(Arrays.asList(null, (double) i));
        }

        List<ColumnStat> stats = analyzer.calculateBasicStats(data);

        assertEquals(2, stats.size());
        ColumnStat nullCol = stats.get(0);
        assertEquals(0, nullCol.count());
        assertEquals(5, nullCol.missingCount());
        assertEquals(0.0, nullCol.mean(), 0.001);
    }
}
