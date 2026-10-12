package com.audaque.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 统一 AI 请求模型。
 *
 * <p>屏蔽各提供方请求参数差异，只暴露通用字段：消息列表、模型覆盖、采样温度与最大 token。
 * 未显式指定的参数将由 {@link AiServiceConfig} 中的默认值补齐。</p>
 */
public final class AiRequest {

    private final List<AiMessage> messages;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;

    private AiRequest(Builder builder) {
        this.messages = Collections.unmodifiableList(new ArrayList<>(builder.messages));
        this.model = builder.model;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
    }

    /**
     * 返回消息列表。
     *
     * @return 不可变消息列表
     */
    public List<AiMessage> messages() {
        return messages;
    }

    /**
     * 返回模型覆盖值。
     *
     * @return 模型名称，未覆盖时为 null
     */
    public String model() {
        return model;
    }

    /**
     * 返回采样温度覆盖值。
     *
     * @return 温度，未覆盖时为 null
     */
    public Double temperature() {
        return temperature;
    }

    /**
     * 返回最大 token 覆盖值。
     *
     * @return 最大 token，未覆盖时为 null
     */
    public Integer maxTokens() {
        return maxTokens;
    }

    /**
     * 创建构建器。
     *
     * @return 新的构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 以系统提示词 + 用户提示词快速构造请求。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return 请求对象
     */
    public static AiRequest of(String systemPrompt, String userPrompt) {
        return builder()
                .addMessage(AiMessage.system(systemPrompt))
                .addMessage(AiMessage.user(userPrompt))
                .build();
    }

    /**
     * {@link AiRequest} 构建器。
     */
    public static final class Builder {

        private final List<AiMessage> messages = new ArrayList<>();
        private String model;
        private Double temperature;
        private Integer maxTokens;

        private Builder() {
        }

        /**
         * 追加一条消息。
         *
         * @param message 消息
         * @return 当前构建器
         */
        public Builder addMessage(AiMessage message) {
            this.messages.add(Objects.requireNonNull(message, "message 不能为 null"));
            return this;
        }

        /**
         * 覆盖请求使用的模型。
         *
         * @param model 模型名称
         * @return 当前构建器
         */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /**
         * 覆盖采样温度。
         *
         * @param temperature 温度
         * @return 当前构建器
         */
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * 覆盖最大 token 数。
         *
         * @param maxTokens 最大 token
         * @return 当前构建器
         */
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * 构建请求对象。
         *
         * @return 请求对象
         * @throws IllegalArgumentException 消息列表为空时抛出
         */
        public AiRequest build() {
            if (messages.isEmpty()) {
                throw new IllegalArgumentException("AiRequest 至少需要一条消息");
            }
            return new AiRequest(this);
        }
    }
}
