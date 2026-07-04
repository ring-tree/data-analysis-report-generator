package com.audaque.model;

import java.util.List;

/**
 * 模板函数定义模型，描述一个可用于数据分析的模板函数。
 *
 * @param id             唯一标识，如 "total_rows"
 * @param name           显示名称，如 "统计总行数"
 * @param category       分类：数据总览 / 数据清洗 / 数据处理 / 数据分析
 * @param description    函数功能详细描述
 * @param parameters     参数列表
 * @param implementation 该函数的伪代码或 Java 实现逻辑
 */
public record TemplateFunction(
        String id,
        String name,
        String category,
        String description,
        List<FunctionParameter> parameters,
        String implementation) {
}
