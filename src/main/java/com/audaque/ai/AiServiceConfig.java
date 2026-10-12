package com.audaque.ai;

import java.util.Objects;

/**
 * 单个 AI 提供方的服务配置。
 *
 * <p>集中承载统一 AI 接口管理层所需的全量参数：接口端点、密钥、模型、超时设置、默认采样参数与重试策略。
 * 由 {@link AiManager} 从 {@code ai-config.properties} 加载并组装，实例不可变。</p>
 */
public final class AiServiceConfig {

    /** 占位密钥特征串：包含该串的密钥被视为未配置。 */
    public static final String PLACEHOLDER_MARKER = "YOUR";

    private final String providerName;
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;
    private final int connectTimeoutSeconds;
    private final int readTimeoutSeconds;
    private final int writeTimeoutSeconds;
    private final RetryPolicy retryPolicy;

    private AiServiceConfig(Builder builder) {
        this.providerName = builder.providerName;
        this.endpoint = builder.endpoint;
        this.apiKey = builder.apiKey;
        this.model = builder.model;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.connectTimeoutSeconds = builder.connectTimeoutSeconds;
        this.readTimeoutSeconds = builder.readTimeoutSeconds;
        this.writeTimeoutSeconds = builder.writeTimeoutSeconds;
        this.retryPolicy = builder.retryPolicy;
    }

    /** @return 提供方名称 */
    public String providerName() {
        return providerName;
    }

    /** @return 接口端点 */
    public String endpoint() {
        return endpoint;
    }

    /** @return API 密钥 */
    public String apiKey() {
        return apiKey;
    }

    /** @return 默认模型名称 */
    public String model() {
        return model;
    }

    /** @return 默认采样温度 */
    public double temperature() {
        return temperature;
    }

    /** @return 默认最大 token 数 */
    public int maxTokens() {
        return maxTokens;
    }

    /** @return 连接超时（秒） */
    public int connectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    /** @return 读取超时（秒） */
    public int readTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    /** @return 写入超时（秒） */
    public int writeTimeoutSeconds() {
        return writeTimeoutSeconds;
    }

    /** @return 重试策略 */
    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    /**
     * 判断该提供方是否已完成有效配置（密钥非空且非占位值、端点非空）。
     *
     * <p>用于在密钥尚未接入时优雅降级，而非直接抛错。</p>
     *
     * @return 配置有效返回 true
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && !apiKey.contains(PLACEHOLDER_MARKER)
                && endpoint != null && !endpoint.isBlank();
    }

    /**
     * 创建指定提供方的配置构建器。
     *
     * @param providerName 提供方名称
     * @return 配置构建器
     */
    public static Builder builder(String providerName) {
        return new Builder(providerName);
    }

    /**
     * {@link AiServiceConfig} 构建器，内置合理默认值。
     */
    public static final class Builder {

        private final String providerName;
        private String endpoint = "";
        private String apiKey = "";
        private String model = "";
        private double temperature = 0.7;
        private int maxTokens = 2048;
        private int connectTimeoutSeconds = 10;
        private int readTimeoutSeconds = 60;
        private int writeTimeoutSeconds = 60;
        private RetryPolicy retryPolicy = RetryPolicy.defaults();

        private Builder(String providerName) {
            this.providerName = Objects.requireNonNull(providerName, "providerName 不能为 null");
        }

        /** @param endpoint 接口端点 @return 当前构建器 */
        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /** @param apiKey API 密钥 @return 当前构建器 */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /** @param model 模型名称 @return 当前构建器 */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /** @param temperature 采样温度 @return 当前构建器 */
        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        /** @param maxTokens 最大 token 数 @return 当前构建器 */
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /** @param connectTimeoutSeconds 连接超时（秒） @return 当前构建器 */
        public Builder connectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
            return this;
        }

        /** @param readTimeoutSeconds 读取超时（秒） @return 当前构建器 */
        public Builder readTimeoutSeconds(int readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
            return this;
        }

        /** @param writeTimeoutSeconds 写入超时（秒） @return 当前构建器 */
        public Builder writeTimeoutSeconds(int writeTimeoutSeconds) {
            this.writeTimeoutSeconds = writeTimeoutSeconds;
            return this;
        }

        /** @param retryPolicy 重试策略 @return 当前构建器 */
        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        /**
         * 构建配置对象。
         *
         * @return 配置对象
         * @throws IllegalArgumentException 超时或 token 配置非法时抛出
         */
        public AiServiceConfig build() {
            if (connectTimeoutSeconds <= 0 || readTimeoutSeconds <= 0 || writeTimeoutSeconds <= 0) {
                throw new IllegalArgumentException("HTTP 超时必须为正数");
            }
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("maxTokens 必须为正数");
            }
            return new AiServiceConfig(this);
        }
    }
}
