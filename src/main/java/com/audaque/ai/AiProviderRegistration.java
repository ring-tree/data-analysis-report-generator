package com.audaque.ai;

import java.util.Objects;

/**
 * AI 提供方注册信息。
 *
 * <p>把「提供方名称 + 内置默认端点 + 内置默认模型 + 工厂方法」打包为一个注册项，
 * 供统一 AI 接口管理层在加载配置时补齐默认值并装配实例，是扩展新模型的统一入口。</p>
 *
 * @param name            提供方唯一标识，如 "deepseek"
 * @param defaultEndpoint 内置默认接口端点
 * @param defaultModel    内置默认模型名称
 * @param factory         提供方工厂方法
 */
public record AiProviderRegistration(
        String name,
        String defaultEndpoint,
        String defaultModel,
        AiProviderFactory factory) {

    /**
     * 紧凑构造器，校验必填字段。
     */
    public AiProviderRegistration {
        Objects.requireNonNull(name, "name 不能为 null");
        Objects.requireNonNull(defaultEndpoint, "defaultEndpoint 不能为 null");
        Objects.requireNonNull(defaultModel, "defaultModel 不能为 null");
        Objects.requireNonNull(factory, "factory 不能为 null");
    }
}
