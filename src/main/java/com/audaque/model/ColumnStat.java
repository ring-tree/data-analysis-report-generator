package com.audaque.model;

/**
 * 单列的详细统计信息。
 *
 * @param columnName   列名
 * @param count        有效数值个数（非空）
 * @param mean         均值
 * @param median       中位数
 * @param stdDev       标准差
 * @param min          最小值
 * @param max          最大值
 * @param missingCount 缺失值个数
 * @param outlierCount 异常值个数（基于 IQR 方法）
 */
public record ColumnStat(
        String columnName,
        long count,
        double mean,
        double median,
        double stdDev,
        double min,
        double max,
        long missingCount,
        long outlierCount) {
}
