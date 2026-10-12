package com.audaque.ai;

/**
 * AI 提供方工厂，用于根据配置创建具体提供方适配器实例。
 *
 * <p>通过该函数式接口，统一 AI 接口管理层可以按名称动态装配不同的提供方实现，
 * 新增模型时只需注册对应的工厂方法即可。</p>
 */
@FunctionalInterface
public interface AiProviderFactory {

    /**
     * 创建提供方实例。
     *
     * @param config 该提供方的服务配置
     * @return 提供方适配器实例
     */
    AiProvider create(AiServiceConfig config);
}
