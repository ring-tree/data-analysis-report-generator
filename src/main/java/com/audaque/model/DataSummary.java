package com.audaque.model;

import java.util.List;
import java.util.Map;

/**
 * 数据总览统计模型，描述 CSV 数据的整体统计结果。
 *
 * @param totalRows   总行数
 * @param emptyRows   空行数（整行全为空）
 * @param validRows   有效行数（至少一个数值）
 * @param columnCount 总列数
 * @param columnNames 列名列表
 * @param columnStats 列名到该列详细统计信息的映射
 */
public record DataSummary(
        long totalRows,
        long emptyRows,
        long validRows,
        int columnCount,
        List<String> columnNames,
        Map<String, ColumnStat> columnStats) {
}
