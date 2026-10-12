package com.audaque.ai.provider;

import com.audaque.ai.AiProviderRegistration;
import com.audaque.ai.AiServiceConfig;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * DeepSeek 官方模型提供方适配器。
 *
 * <p>内置 DeepSeek 官方 v4.1 flash 模型的基础预设：接口端点、模型名称与响应解析逻辑。
 * 使用 OpenAI 兼容协议，鉴权沿用 Bearer Token。</p>
 *
 * <p>本类的 {@link #REGISTRATION} 已由统一 AI 接口管理层自动装配；后续如需新增其他模型，
 * 参照本类实现新的 {@link com.audaque.ai.AiProvider} 并注册即可。</p>
 */
public class DeepSeekProvider extends AbstractOpenAiCompatibleProvider {

    /** 提供方名称。 */
    public static final String NAME = "deepseek";

    /** DeepSeek 官方 OpenAI 兼容接口端点（预设）。 */
    public static final String DEFAULT_ENDPOINT = "https://api.deepseek.com/v1/chat/completions";

    /** 默认模型：DeepSeek 官方 v4.1 flash。 */
    public static final String DEFAULT_MODEL = "deepseek-v4.1-flash";

    /** 注册信息，供 {@link com.audaque.ai.AiManager} 统一装配。 */
    public static final AiProviderRegistration REGISTRATION =
            new AiProviderRegistration(NAME, DEFAULT_ENDPOINT, DEFAULT_MODEL, DeepSeekProvider::new);

    /**
     * 构造 DeepSeek 提供方。
     *
     * @param config 服务配置
     */
    public DeepSeekProvider(AiServiceConfig config) {
        super(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return NAME;
    }

    /**
     * DeepSeek 推理类模型可能在 {@code reasoning_content} 中返回内容，
     * 当 {@code content} 为空时回退使用该字段。
     *
     * @param messageNode 响应 message 节点
     * @return 文本内容
     */
    @Override
    protected String extractContent(JsonNode messageNode) {
        String content = super.extractContent(messageNode);
        if (content == null || content.isBlank()) {
            content = messageNode.path("reasoning_content").asText(null);
        }
        return content;
    }
}
