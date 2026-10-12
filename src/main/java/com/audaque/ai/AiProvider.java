package com.audaque.ai;

/**
 * AI 服务提供方适配接口。
 *
 * <p>不同 AI 服务（DeepSeek、OpenAI 兼容服务等）通过实现本接口接入统一 AI 接口管理层，
 * 由适配器负责各自的鉴权方式、请求格式与响应解析，向上层屏蔽实现差异。</p>
 *
 * <p>实现约定：{@link #chat(AiRequest)} 仅执行「一次」网络调用，超时与重试由
 * {@link AiClient} 统一编排，以保持重试策略的集中管理。</p>
 */
public interface AiProvider {

    /**
     * 提供方唯一标识，如 "deepseek"。
     *
     * @return 提供方名称
     */
    String name();

    /**
     * 执行一次 AI 对话调用（单次网络请求，不含重试）。
     *
     * @param request 统一请求对象
     * @return 统一响应对象
     * @throws AiException 调用或解析失败
     */
    AiResponse chat(AiRequest request) throws AiException;
}
