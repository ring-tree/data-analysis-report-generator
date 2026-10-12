package com.audaque.ai;

/**
 * 统一 AI 响应模型。
 *
 * <p>各提供方的原始响应结构差异由对应的 {@link AiProvider} 适配器消化，向上层仅暴露标准化字段。</p>
 *
 * @param content      模型返回的文本内容
 * @param model        实际使用的模型名称
 * @param finishReason 结束原因，可能为 null
 */
public record AiResponse(String content, String model, String finishReason) {

    /**
     * 构造响应对象。
     *
     * @param content      文本内容
     * @param model        模型名称
     * @param finishReason 结束原因
     * @return 统一响应对象
     */
    public static AiResponse of(String content, String model, String finishReason) {
        return new AiResponse(content, model, finishReason);
    }
}
