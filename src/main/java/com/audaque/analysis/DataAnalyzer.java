package com.audaque.analysis;

import com.audaque.model.ColumnStat;
import com.audaque.model.DataSummary;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据统计分析器，提供基本统计量计算、异常值检测和数据总览生成功能。
 */
public class DataAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(DataAnalyzer.class);

    private static final double IQR_MULTIPLIER = 1.5;

    /**
     * 计算每列的基本统计量：均值、中位数、标准差、最小值、最大值、缺失值数量。
     *
     * @param data 数值数据，外层为行，内层为列值
     * @return 每列的统计信息列表，列名默认为 "column_N"
     */
    public List<ColumnStat> calculateBasicStats(List<List<Double>> data) {
        int columnCount = getMaxColumnCount(data);
        List<List<Double>> columns = transposeToColumns(data, columnCount);
        List<ColumnStat> stats = new ArrayList<>();

        for (int i = 0; i < columns.size(); i++) {
            List<Double> column = columns.get(i);
            DescriptiveStatistics descStats = new DescriptiveStatistics();

            for (Double value : column) {
                if (value != null) {
                    descStats.addValue(value);
                }
            }

            long totalValues = column.size();
            long validCount = descStats.getN();
            long missingCount = totalValues - validCount;

            double mean = validCount > 0 ? descStats.getMean() : 0.0;
            double stdDev = validCount > 1 ? descStats.getStandardDeviation() : 0.0;
            double min = validCount > 0 ? descStats.getMin() : 0.0;
            double max = validCount > 0 ? descStats.getMax() : 0.0;
            double median = validCount > 0 ? descStats.getPercentile(50) : 0.0;

            ColumnStat stat = new ColumnStat(
                    "column_" + i,
                    validCount,
                    mean,
                    median,
                    stdDev,
                    min,
                    max,
                    missingCount,
                    0
            );
            stats.add(stat);
        }

        logger.info("Calculated basic stats for {} columns", stats.size());
        return stats;
    }

    /**
     * 使用 IQR 方法检测指定列中的异常值。
     *
     * @param column 单列数值数据，null 值会被忽略
     * @return 异常值在原列表中的索引位置
     */
    public List<Integer> detectOutliers(List<Double> column) {
        List<Integer> outlierIndices = new ArrayList<>();

        DescriptiveStatistics descStats = new DescriptiveStatistics();
        for (Double value : column) {
            if (value != null) {
                descStats.addValue(value);
            }
        }

        if (descStats.getN() < 4) {
            logger.debug("Insufficient data for outlier detection (n={})", descStats.getN());
            return outlierIndices;
        }

        double q1 = descStats.getPercentile(25);
        double q3 = descStats.getPercentile(75);
        double iqr = q3 - q1;
        double lowerBound = q1 - IQR_MULTIPLIER * iqr;
        double upperBound = q3 + IQR_MULTIPLIER * iqr;

        for (int i = 0; i < column.size(); i++) {
            Double value = column.get(i);
            if (value != null && (value < lowerBound || value > upperBound)) {
                outlierIndices.add(i);
            }
        }

        logger.debug("Detected {} outliers in column (Q1={}, Q3={}, IQR={})",
                outlierIndices.size(), q1, q3, iqr);
        return outlierIndices;
    }

    /**
     * 生成完整的数据总览统计，返回 DataSummary 对象。
     *
     * @param data        数值数据，外层为行，内层为列值
     * @param columnNames 列名列表
     * @return 包含总行数、空行数、有效行数、列数、列名及每列统计信息的 DataSummary 对象
     */
    public DataSummary generateSummary(List<List<Double>> data, List<String> columnNames) {
        int columnCount = Math.max(columnNames.size(), getMaxColumnCount(data));
        long totalRows = data.size();
        long emptyRows = 0;
        long validRows = 0;

        for (List<Double> row : data) {
            boolean hasValue = false;
            if (row == null || row.isEmpty()) {
                emptyRows++;
                continue;
            }
            for (Double value : row) {
                if (value != null) {
                    hasValue = true;
                    break;
                }
            }
            if (hasValue) {
                validRows++;
            } else {
                emptyRows++;
            }
        }

        List<List<Double>> columns = transposeToColumns(data, columnCount);
        Map<String, ColumnStat> columnStats = new HashMap<>();

        for (int i = 0; i < columns.size(); i++) {
            String colName = i < columnNames.size() ? columnNames.get(i) : "column_" + i;
            List<Double> column = columns.get(i);

            DescriptiveStatistics descStats = new DescriptiveStatistics();
            for (Double value : column) {
                if (value != null) {
                    descStats.addValue(value);
                }
            }

            long totalValues = column.size();
            long validCount = descStats.getN();
            long missingCount = totalValues - validCount;

            double mean = validCount > 0 ? descStats.getMean() : 0.0;
            double stdDev = validCount > 1 ? descStats.getStandardDeviation() : 0.0;
            double min = validCount > 0 ? descStats.getMin() : 0.0;
            double max = validCount > 0 ? descStats.getMax() : 0.0;
            double median = validCount > 0 ? descStats.getPercentile(50) : 0.0;

            List<Integer> outliers = detectOutliers(column);
            long outlierCount = outliers.size();

            ColumnStat stat = new ColumnStat(
                    colName,
                    validCount,
                    mean,
                    median,
                    stdDev,
                    min,
                    max,
                    missingCount,
                    outlierCount
            );
            columnStats.put(colName, stat);
        }

        logger.info("Generated summary: totalRows={}, emptyRows={}, validRows={}, columns={}",
                totalRows, emptyRows, validRows, columnCount);

        return new DataSummary(
                totalRows,
                emptyRows,
                validRows,
                columnCount,
                List.copyOf(columnNames),
                Map.copyOf(columnStats)
        );
    }

    private int getMaxColumnCount(List<List<Double>> data) {
        int max = 0;
        for (List<Double> row : data) {
            if (row != null && row.size() > max) {
                max = row.size();
            }
        }
        return max;
    }

    private List<List<Double>> transposeToColumns(List<List<Double>> data, int columnCount) {
        List<List<Double>> columns = new ArrayList<>(columnCount);
        for (int c = 0; c < columnCount; c++) {
            List<Double> column = new ArrayList<>(data.size());
            for (List<Double> row : data) {
                if (row != null && c < row.size()) {
                    column.add(row.get(c));
                } else {
                    column.add(null);
                }
            }
            columns.add(column);
        }
        return columns;
    }
}
