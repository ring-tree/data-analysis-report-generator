package com.audaque.ai.provider;

import com.audaque.ai.AiException;
import com.audaque.ai.AiMessage;
import com.audaque.ai.AiProvider;
import com.audaque.ai.AiRequest;
import com.audaque.ai.AiResponse;
import com.audaque.ai.AiServiceConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议的 AI 提供方抽象基类。
 *
 * <p>绝大多数主流 AI 服务（含 DeepSeek）均兼容 OpenAI 的 {@code /chat/completions} 协议，
 * 本类把「请求体构建 + 鉴权头 + 响应解析」等公共逻辑沉淀在这里，供各提供方子类复用；
 * 子类只需提供名称与差异化钩子，从而屏蔽底层实现差异。</p>
 */
public abstract class AbstractOpenAiCompatibleProvider implements AiProvider {

    private static final Logger logger = LoggerFactory.getLogger(AbstractOpenAiCompatibleProvider.class);

    /** 统一的 JSON 媒体类型。 */
    protected static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 该提供方的服务配置。 */
    protected final AiServiceConfig config;

    /** 依据配置超时构建的 HTTP 客户端。 */
    protected final OkHttpClient httpClient;

    /**
     * 构造提供方实例。
     *
     * @param config 提供方服务配置
     */
    protected AbstractOpenAiCompatibleProvider(AiServiceConfig config) {
        this.config = config;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(config.connectTimeoutSeconds()))
                .readTimeout(Duration.ofSeconds(config.readTimeoutSeconds()))
                .writeTimeout(Duration.ofSeconds(config.writeTimeoutSeconds()))
                .build();
    }

    /**
     * 依据配置执行一次 HTTP 对话调用（单次请求，不含重试）。
     *
     * @param request 统一请求
     * @return 统一响应
     * @throws AiException 网络失败、HTTP 非 2xx 或解析失败
     */
    @Override
    public AiResponse chat(AiRequest request) throws AiException {
        String requestBody = buildRequestBody(request);

        Request httpRequest = new Request.Builder()
                .url(config.endpoint())
                .header("Authorization", "Bearer " + config.apiKey())
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            ResponseBody body = response.body();
            String responseJson = body != null ? body.string() : "";

            if (!response.isSuccessful()) {
                throw new AiException("AI 服务返回 HTTP " + response.code() + ": " + responseJson);
            }
            logger.debug("AI 提供方 {} 调用成功，响应长度 {}", name(), responseJson.length());
            return parseResponse(responseJson);
        } catch (IOException e) {
            throw new AiException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 OpenAI 兼容的请求体 JSON。
     *
     * @param request 统一请求
     * @return 请求体 JSON 字符串
     * @throws AiException 序列化失败
     */
    protected String buildRequestBody(AiRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", resolveModel(request));
        payload.put("messages", toMessagePayload(request.messages()));
        payload.put("temperature", request.temperature() != null ? request.temperature() : config.temperature());
        payload.put("max_tokens", request.maxTokens() != null ? request.maxTokens() : config.maxTokens());
        payload.put("stream", false);
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new AiException("构建 AI 请求体失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 OpenAI 兼容的响应 JSON。
     *
     * @param responseJson 响应 JSON 字符串
     * @return 统一响应
     * @throws AiException 结构不符合预期时抛出
     */
    protected AiResponse parseResponse(String responseJson) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(responseJson);
        } catch (JsonProcessingException e) {
            throw new AiException("解析 AI 响应失败: " + e.getMessage(), e);
        }

        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new AiException("AI 响应缺少 choices 字段: " + abbreviate(responseJson));
        }

        JsonNode firstChoice = choices.get(0);
        String content = extractContent(firstChoice.path("message"));
        if (content == null || content.isBlank()) {
            throw new AiException("AI 响应缺少有效的消息内容: " + abbreviate(responseJson));
        }

        String model = root.path("model").asText(config.model());
        String finishReason = firstChoice.path("finish_reason").asText(null);
        return AiResponse.of(content.trim(), model, finishReason);
    }

    /**
     * 提取消息内容，子类可覆写以兼容特定字段（如推理模型的 reasoning_content）。
     *
     * @param messageNode 响应中的 message 节点
     * @return 文本内容，缺失时返回 null
     */
    protected String extractContent(JsonNode messageNode) {
        return messageNode.path("content").asText(null);
    }

    /**
     * 解析最终使用的模型名称。
     *
     * @param request 统一请求
     * @return 模型名称
     */
    protected String resolveModel(AiRequest request) {
        return request.model() != null && !request.model().isBlank() ? request.model() : config.model();
    }

    private List<Map<String, String>> toMessagePayload(List<AiMessage> messages) {
        List<Map<String, String>> payload = new ArrayList<>(messages.size());
        for (AiMessage message : messages) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("role", message.role());
            item.put("content", message.content());
            payload.add(item);
        }
        return payload;
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "(null)";
        }
        return text.length() <= 500 ? text : text.substring(0, 500) + "...";
    }
}
