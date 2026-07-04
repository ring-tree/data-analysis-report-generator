package com.audaque.ai;

import com.audaque.model.DataSummary;
import com.audaque.model.TemplateFunction;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek API 优化器，调用 deepseek-v4-flash 模型优化模板函数、推荐函数选择和生成报告 prompt。
 */
public class DeepSeekOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekOptimizer.class);

    private static final String CONFIG_FILE = "config.properties";
    private static final String PLACEHOLDER_KEY = "sk-YOUR_SHORT_LIVED_API_KEY_HERE";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String apiKey;
    private static String apiUrl;
    private static String model;
    private static OkHttpClient httpClient;

    static {
        loadConfig();
        initHttpClient();
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = DeepSeekOptimizer.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException("配置文件不存在");
            }
            props.load(input);
        } catch (IllegalStateException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalStateException("配置文件加载失败: " + e.getMessage(), e);
        }

        apiKey = props.getProperty("deepseek.api.key", "").trim();
        apiUrl = props.getProperty("deepseek.api.url", "https://api.deepseek.com/v1/chat/completions").trim();
        model = props.getProperty("deepseek.model", "deepseek-v4-flash").trim();

        if (apiKey.isEmpty() || PLACEHOLDER_KEY.equals(apiKey)) {
            throw new IllegalStateException(
                    "请将 config.properties 中的 deepseek.api.key 占位值替换为面试官提供的短期有效 Key");
        }

        logger.info("DeepSeek optimizer initialized: url={}, model={}", apiUrl, model);
    }

    private static void initHttpClient() {
        Properties props = new Properties();
        int connectTimeout = 10;
        int readTimeout = 60;
        try (InputStream input = DeepSeekOptimizer.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                props.load(input);
                connectTimeout = Integer.parseInt(props.getProperty("http.connect.timeout", "10"));
                readTimeout = Integer.parseInt(props.getProperty("http.read.timeout", "60"));
            }
        } catch (IOException | NumberFormatException e) {
            logger.warn("Failed to read HTTP timeout config, using defaults: {}", e.getMessage());
        }

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .readTimeout(Duration.ofSeconds(readTimeout))
                .writeTimeout(Duration.ofSeconds(readTimeout))
                .build();

        logger.info("HTTP client initialized: connect={}s, read={}s", connectTimeout, readTimeout);
    }

    /**
     * 调用 DeepSeek API 优化指定模板函数的实现。
     *
     * @param func    待优化的模板函数
     * @param summary 当前数据集的统计摘要
     * @return 优化后的实现代码字符串
     * @throws IOException API 调用失败
     */
    public String optimizeTemplate(TemplateFunction func, DataSummary summary) throws IOException {
        String userMessage = String.format("""
                请优化以下模板函数的实现，使其更高效或更健壮。

                函数名称：%s
                函数分类：%s
                函数描述：%s
                参数定义：%s
                当前实现：
                %s

                数据上下文：
                - 总行数：%d
                - 有效行数：%d
                - 列数：%d
                - 列名：%s

                请直接返回优化后的伪代码或 Java 代码实现，不要包含任何解释文字。
                """,
                func.name(), func.category(), func.description(),
                func.parameters(), func.implementation(),
                summary.totalRows(), summary.validRows(), summary.columnCount(),
                summary.columnNames());

        String systemPrompt = "你是一个数据分析和代码优化专家。请根据给定的数据上下文优化模板函数的实现。只返回优化后的代码，不要包含任何解释。";

        String response = callApi(systemPrompt, userMessage);
        logger.info("Optimized template function: {}", func.name());
        return response;
    }

    /**
     * 根据数据特征让模型推荐适用的函数 ID 列表。
     *
     * @param summary 当前数据集的统计摘要
     * @return 推荐使用的函数 ID 列表
     * @throws IOException API 调用失败
     */
    public List<String> selectFunctions(DataSummary summary) throws IOException {
        String userMessage = String.format("""
                请根据以下数据特征，从可用模板函数中推荐最适用的函数 ID 列表。

                数据总览：
                - 总行数：%d
                - 有效行数：%d
                - 列数：%d
                - 列名：%s
                - 缺失值情况：部分列存在缺失值

                可用函数：
                数据总览：total_rows, column_count, basic_stats
                数据清洗：remove_nulls, remove_outliers, fill_missing
                数据处理：normalize, standardize, categorize
                数据分析：correlation, group_by, percentile

                请返回一个 JSON 数组，只包含推荐使用的函数 ID 字符串。例如：["basic_stats", "remove_nulls", "normalize"]
                不要返回任何其他内容。
                """,
                summary.totalRows(), summary.validRows(), summary.columnCount(),
                summary.columnNames());

        String systemPrompt = "你是一个数据分析专家。请根据数据特征推荐最适用的数据分析函数。只返回 JSON 数组，不要包含任何解释。";

        String response = callApi(systemPrompt, userMessage);
        List<String> selectedIds = parseStringList(response);
        logger.info("Selected {} functions: {}", selectedIds.size(), selectedIds);
        return selectedIds;
    }

    /**
     * 根据数据总览和选定的函数生成最终报告的分析 prompt。
     *
     * @param summary     当前数据集的统计摘要
     * @param selectedIds 已选择的函数 ID 列表
     * @return 用于报告生成的分析 prompt
     * @throws IOException API 调用失败
     */
    public String generateReportPrompt(DataSummary summary, List<String> selectedIds) throws IOException {
        String userMessage = String.format("""
                请根据以下数据和选定的分析函数，生成一份数据分析报告的详细 prompt。

                数据总览：
                - 总行数：%d
                - 有效行数：%d
                - 列数：%d
                - 列名：%s

                选定的分析函数：%s

                请生成一份结构化的数据分析 prompt，包含：
                1. 数据概况描述
                2. 需要执行的具体分析步骤
                3. 每个步骤的输入输出说明
                4. 报告的组织结构建议
                """,
                summary.totalRows(), summary.validRows(), summary.columnCount(),
                summary.columnNames(), String.join(", ", selectedIds));

        String systemPrompt = "你是一个数据分析报告专家。请根据数据和函数选择生成一份详细的数据分析 prompt。";

        String response = callApi(systemPrompt, userMessage);
        logger.info("Generated report prompt (length={})", response.length());
        return response;
    }

    private String callApi(String systemPrompt, String userMessage) throws IOException {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "(empty)";
                        throw new IOException("API returned HTTP " + response.code() + ": " + errorBody);
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        throw new IOException("API returned empty response body");
                    }

                    String responseJson = body.string();
                    return parseApiResponse(responseJson);
                }
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long backoff = BASE_BACKOFF_MS * (1L << (attempt - 1));
                    logger.warn("API call failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt, MAX_RETRIES, backoff, e.getMessage());
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", interrupted);
                    }
                }
            }
        }

        throw new IOException("API call failed after " + MAX_RETRIES + " attempts", lastException);
    }

    private String parseApiResponse(String responseJson) throws IOException {
        ApiResponse apiResponse = objectMapper.readValue(responseJson, ApiResponse.class);
        if (apiResponse.choices == null || apiResponse.choices.isEmpty()) {
            throw new IOException("API returned no choices in response");
        }
        ApiChoice firstChoice = apiResponse.choices.get(0);
        if (firstChoice.message == null || firstChoice.message.content == null) {
            throw new IOException("API returned empty message content");
        }
        return firstChoice.message.content.trim();
    }

    /**
     * 根据报告 prompt 生成最终的数据分析结论文本。
     *
     * @param reportPrompt 由 generateReportPrompt 生成的分析 prompt
     * @return DeepSeek 生成的分析结论文本
     * @throws IOException API 调用失败
     */
    public String generateAnalysisText(String reportPrompt) throws IOException {
        String systemPrompt = "你是一个数据分析报告专家。请根据给定的分析prompt生成详细、专业的数据分析结论。";

        String response = callApi(systemPrompt, reportPrompt);
        logger.info("Generated analysis text (length={})", response.length());
        return response;
    }

    private List<String> parseStringList(String response) {
        try {
            return objectMapper.readValue(response, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            String cleaned = response.replaceAll("^```json\\s*", "")
                    .replaceAll("\\s*```$", "")
                    .trim();
            try {
                return objectMapper.readValue(cleaned, new TypeReference<List<String>>() {});
            } catch (IOException ex) {
                logger.warn("Failed to parse response as JSON array: {}", response);
                return List.of(response);
            }
        }
    }

    static class ApiResponse {
        @JsonProperty("choices")
        List<ApiChoice> choices;
    }

    static class ApiChoice {
        @JsonProperty("message")
        ApiMessage message;
    }

    static class ApiMessage {
        @JsonProperty("content")
        String content;
    }
}
