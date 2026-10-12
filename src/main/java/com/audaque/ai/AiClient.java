package com.audaque.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一 AI 调用客户端。
 *
 * <p>对上层提供标准化的调用入口，负责把配置中的默认参数补齐到请求，并按 {@link RetryPolicy}
 * 统一编排重试与指数退避。具体协议差异由绑定的 {@link AiProvider} 消化。</p>
 */
public final class AiClient {

    private static final Logger logger = LoggerFactory.getLogger(AiClient.class);

    private final AiServiceConfig config;
    private final AiProvider provider;

    /**
     * 构造客户端。
     *
     * @param config   提供方服务配置
     * @param provider 提供方适配器
     */
    public AiClient(AiServiceConfig config, AiProvider provider) {
        this.config = config;
        this.provider = provider;
    }

    /**
     * 返回绑定的服务配置。
     *
     * @return 服务配置
     */
    public AiServiceConfig config() {
        return config;
    }

    /**
     * 返回提供方名称。
     *
     * @return 提供方名称
     */
    public String providerName() {
        return config.providerName();
    }

    /**
     * 判断当前客户端是否可调用（密钥与端点均已配置）。
     *
     * @return 可用返回 true
     */
    public boolean isConfigured() {
        return config.isConfigured();
    }

    /**
     * 发起一次对话调用，失败时按配置的重试策略自动重试。
     *
     * @param request 统一请求对象
     * @return 统一响应对象
     * @throws AiException 未配置或重试耗尽后仍失败
     */
    public AiResponse chat(AiRequest request) {
        if (!config.isConfigured()) {
            throw new AiException("AI 提供方 '" + config.providerName()
                    + "' 未配置有效 API Key，请在 ai-config.properties 中补全 api-key 与 endpoint");
        }

        RetryPolicy policy = config.retryPolicy();
        AiException lastError = null;

        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            try {
                return provider.chat(request);
            } catch (AiException e) {
                lastError = e;
                if (attempt < policy.maxAttempts()) {
                    long backoff = policy.backoffMillis(attempt);
                    logger.warn("AI 调用失败（第 {}/{} 次尝试），{}ms 后重试: {}",
                            attempt, policy.maxAttempts(), backoff, e.getMessage());
                    sleep(backoff);
                }
            }
        }

        throw new AiException("AI 调用在 " + policy.maxAttempts() + " 次尝试后仍失败", lastError);
    }

    /**
     * 以系统提示词 + 用户提示词快捷调用。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 统一响应对象
     * @throws AiException 未配置或调用失败
     */
    public AiResponse chat(String systemPrompt, String userPrompt) {
        return chat(AiRequest.of(systemPrompt, userPrompt));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("重试等待被中断", e);
        }
    }
}
