package com.audaque.ai;

import com.audaque.ai.provider.DeepSeekProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 统一 AI 接口管理层（全局唯一入口）。
 *
 * <p>职责：</p>
 * <ul>
 *     <li>集中加载并管理所有 AI 提供方的配置（模型参数、超时、重试策略）；</li>
 *     <li>维护提供方注册表，按名称装配 {@link AiClient}，屏蔽底层实现差异；</li>
 *     <li>作为全局单例，向上层提供默认客户端与按名称获取客户端的标准入口。</li>
 * </ul>
 *
 * <p>配置来源为 classpath 下的 {@code ai-config.properties}。当配置文件缺失或密钥尚未接入时，
 * 管理层仍可正常初始化并对外提供默认配置，便于后续补充 API 密钥与服务地址。</p>
 */
public final class AiManager {

    private static final Logger logger = LoggerFactory.getLogger(AiManager.class);

    private static final String CONFIG_RESOURCE = "ai-config.properties";
    private static final String DEFAULT_PROVIDER_KEY = "ai.default-provider";

    private static final AiManager INSTANCE = new AiManager();

    private final Map<String, AiProviderRegistration> registrations = new LinkedHashMap<>();
    private final Map<String, AiServiceConfig> configs = new LinkedHashMap<>();
    private String defaultProvider;

    private AiManager() {
        register(DeepSeekProvider.REGISTRATION);
        loadConfigs();
    }

    /**
     * 获取全局唯一的管理层实例。
     *
     * @return 单例实例
     */
    public static AiManager getInstance() {
        return INSTANCE;
    }

    /**
     * 注册一个 AI 提供方。新增模型时调用本方法即可接入层内。
     *
     * @param registration 提供方注册信息
     */
    public void register(AiProviderRegistration registration) {
        registrations.put(registration.name(), registration);
        logger.info("Registered AI provider: {}", registration.name());
    }

    /**
     * 重新加载配置，用于运行期热更新密钥或参数。
     */
    public synchronized void reload() {
        configs.clear();
        loadConfigs();
        logger.info("AI 配置已重新加载");
    }

    /**
     * 获取指定提供方的配置。
     *
     * @param provider 提供方名称
     * @return 服务配置
     * @throws AiException 提供方未注册时抛出
     */
    public AiServiceConfig config(String provider) {
        AiServiceConfig config = configs.get(provider);
        if (config == null) {
            throw new AiException("未注册的 AI 提供方: " + provider
                    + "，已注册: " + configs.keySet());
        }
        return config;
    }

    /**
     * 获取默认提供方的配置。
     *
     * @return 默认服务配置
     */
    public AiServiceConfig defaultConfig() {
        return config(defaultProvider);
    }

    /**
     * 按名称创建客户端（每次调用创建新实例，可安全并发使用）。
     *
     * @param provider 提供方名称
     * @return 统一 AI 客户端
     * @throws AiException 提供方未注册时抛出
     */
    public AiClient client(String provider) {
        AiServiceConfig config = config(provider);
        AiProvider aiProvider = registrations.get(provider).factory().create(config);
        return new AiClient(config, aiProvider);
    }

    /**
     * 创建默认提供方的客户端。
     *
     * @return 统一 AI 客户端
     */
    public AiClient defaultClient() {
        return client(defaultProvider);
    }

    /**
     * 返回默认提供方名称。
     *
     * @return 默认提供方名称
     */
    public String defaultProvider() {
        return defaultProvider;
    }

    /**
     * 返回已注册（已加载配置）的提供方名称集合。
     *
     * @return 提供方名称集合，不可修改
     */
    public Set<String> providers() {
        return Collections.unmodifiableSet(configs.keySet());
    }

    private void loadConfigs() {
        Properties props = loadProperties();
        this.defaultProvider = props.getProperty(DEFAULT_PROVIDER_KEY, DeepSeekProvider.NAME).trim();

        for (AiProviderRegistration registration : registrations.values()) {
            configs.put(registration.name(), buildConfig(registration, props));
        }

        logger.info("AI 配置加载完成: default={}, providers={}", defaultProvider, configs.keySet());
        AiServiceConfig defaultConfig = configs.get(defaultProvider);
        if (defaultConfig != null && !defaultConfig.isConfigured()) {
            logger.warn("默认 AI 提供方 '{}' 尚未配置有效 API Key，AI 增强步骤将被跳过（见 {}）",
                    defaultProvider, CONFIG_RESOURCE);
        }
    }

    private AiServiceConfig buildConfig(AiProviderRegistration registration, Properties props) {
        String prefix = "ai." + registration.name() + ".";
        return AiServiceConfig.builder(registration.name())
                .endpoint(props.getProperty(prefix + "endpoint", registration.defaultEndpoint()).trim())
                .apiKey(props.getProperty(prefix + "api-key", "").trim())
                .model(props.getProperty(prefix + "model", registration.defaultModel()).trim())
                .temperature(parseDouble(props, prefix + "temperature", 0.7))
                .maxTokens(parseInt(props, prefix + "max-tokens", 2048))
                .connectTimeoutSeconds(parseInt(props, prefix + "connect-timeout-seconds", 10))
                .readTimeoutSeconds(parseInt(props, prefix + "read-timeout-seconds", 60))
                .writeTimeoutSeconds(parseInt(props, prefix + "write-timeout-seconds", 60))
                .retryPolicy(new RetryPolicy(
                        parseInt(props, prefix + "max-attempts", 3),
                        parseLong(props, prefix + "initial-backoff-millis", 1000L),
                        parseDouble(props, prefix + "backoff-multiplier", 2.0)))
                .build();
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = AiManager.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (input == null) {
                logger.warn("未找到 AI 配置文件 {}，将使用各提供方的内置默认配置", CONFIG_RESOURCE);
                return props;
            }
            props.load(input);
        } catch (IOException e) {
            logger.error("读取 AI 配置文件失败，将使用内置默认配置: {}", e.getMessage(), e);
        }
        return props;
    }

    private int parseInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("配置项 {}='{}' 不是合法整数，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private long parseLong(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("配置项 {}='{}' 不是合法整数，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private double parseDouble(Properties props, String key, double defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("配置项 {}='{}' 不是合法数字，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
