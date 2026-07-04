package com.audaque.model;

/**
 * 模板函数的参数定义。
 *
 * @param name         参数名称（如 "columnName"、"threshold"）
 * @param type         参数类型，仅限：STRING、NUMBER、BOOLEAN、COLUMN_REF
 * @param description  参数含义描述
 * @param defaultValue 默认值（可为 null）
 * @param required     是否必填
 */
public record FunctionParameter(
        String name,
        String type,
        String description,
        String defaultValue,
        boolean required) {
}
