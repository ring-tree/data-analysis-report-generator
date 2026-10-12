package com.audaque.ai.service;

import com.audaque.ai.AiClient;
import com.audaque.ai.AiException;
import com.audaque.model.DataSummary;
import com.audaque.model.TemplateFunction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据分析领域 AI 服务。
 *
 * <p>承载报告生成流程中的领域提示词编排（函数选择、模板优化、报告 prompt 生成、结论文本生成），
 * 完全基于统一 AI 接口管理层的 {@link AiClient} 实现，不再直接依赖任何具体 AI 服务的 SDK 或协议。</p>
 */
public class AiAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(AiAnalysisService.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 可用模板函数清单（提示词中的静态说明）。 */
    private static final String AVAILABLE_FUNCTIONS = """
            数据总览：total_rows, column_count, basic_stats
            数据清洗：remove_nulls, remove_outliers, fill_missing
            数据处理：normalize, standardize, categorize
            数据分析：correlation, group_by, percentile""";

    /** 模型未返回可用结果时使用的默认函数集，保证后续流程不空转。 */
    private static final List<String> DEFAULT_FUNCTION_IDS =
            List.of("basic_stats", "remove_nulls", "normalize");

    /** 用于从模型响应文本中提取首个 JSON 数组片段。 */
    private static final Pattern JSON_ARRAY_PATTERN =
            Pattern.compile("\\[[^\\[\\]]*]", Pattern.DOTALL);

    private final AiClient aiClient;

    /**
     * 构造领域服务。
     *
     * @param aiClient 统一 AI 客户端
     */
    public AiAnalysisService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    /**
     * 判断 AI 能力是否可用（底层提供方是否已配置有效密钥）。
     *
     * @return 可用返回 true
     */
    public boolean isAvailable() {
        return aiClient.isConfigured();
    }

    /**
     * 根据数据特征让模型推荐适用的模板函数 ID 列表。
     *
     * @param summary 数据总览
     * @return 推荐使用的函数 ID 列表；模型未返回有效结果时回退到默认函数集
     * @throws AiException 调用失败
     */
    public List<String> selectFunctions(DataSummary summary) {
        String userMessage = String.format("""
                请根据以下数据特征，从可用模板函数中挑选最适用的函数。

                数据总览：
                - 总行数：%d
                - 有效行数：%d
                - 列数：%d
                - 列名：%s

                可用函数（只能从下列 ID 中选择）：
                %s

                输出要求：
                1. 只输出一个 JSON 字符串数组，数组元素为上述函数 ID；
                2. 不要输出解释、标题、Markdown 代码块或数组以外的任何字符；
                3. 输出必须以 [ 开头、以 ] 结尾。

                示例输出：
                ["basic_stats", "remove_nulls", "normalize"]
                """,
                summary.totalRows(), summary.validRows(), summary.columnCount(),
                summary.columnNames(), AVAILABLE_FUNCTIONS);

        String systemPrompt = "你是一个数据分析专家。你必须且只能输出一个 JSON 字符串数组，"
                + "不得输出任何解释、标题或 Markdown 代码块。";

        String response = aiClient.chat(systemPrompt, userMessage).content();
        List<String> selected = parseStringList(response);
        if (selected.isEmpty()) {
            logger.warn("模型未返回可用的函数 ID 列表，回退到默认函数集: {}", DEFAULT_FUNCTION_IDS);
            return DEFAULT_FUNCTION_IDS;
        }
        return selected;
    }

    /**
     * 优化指定模板函数的实现。
     *
     * @param func    待优化的模板函数
     * @param summary 数据总览
     * @return 优化后的实现代码
     * @throws AiException 调用失败
     */
    public String optimizeTemplate(TemplateFunction func, DataSummary summary) {
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
        return aiClient.chat(systemPrompt, userMessage).content();
    }

    /**
     * 生成数据分析报告的分析 prompt。
     *
     * @param summary     数据总览
     * @param selectedIds 已选择的函数 ID 列表
     * @return 结构化分析 prompt
     * @throws AiException 调用失败
     */
    public String generateReportPrompt(DataSummary summary, List<String> selectedIds) {
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
        return aiClient.chat(systemPrompt, userMessage).content();
    }

    /**
     * 根据报告 prompt 生成最终的数据分析结论文本。
     *
     * @param reportPrompt 由 {@link #generateReportPrompt} 生成的 prompt
     * @return 分析结论文本
     * @throws AiException 调用失败
     */
    public String generateAnalysisText(String reportPrompt) {
        String systemPrompt = "你是一个数据分析报告专家。请根据给定的分析prompt生成详细、专业的数据分析结论。";
        return aiClient.chat(systemPrompt, reportPrompt).content();
    }

    /**
     * 从模型响应中尽力解析出字符串数组。
     *
     * <p>依次尝试：直接解析 → 去除 Markdown 代码围栏后解析 → 从文本中提取首个 JSON 数组片段；
     * 任一步得到结果即返回，全部失败返回空列表。</p>
     *
     * @param response 模型响应文本
     * @return 解析出的字符串列表，失败时为空列表
     */
    private List<String> parseStringList(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        String trimmed = response.trim();
        List<String> direct = tryParseJsonArray(trimmed);
        if (direct != null) {
            return direct;
        }

        String cleaned = trimmed.replaceAll("```(?:json)?", "").trim();
        List<String> unfenced = tryParseJsonArray(cleaned);
        if (unfenced != null) {
            return unfenced;
        }

        Matcher matcher = JSON_ARRAY_PATTERN.matcher(cleaned);
        while (matcher.find()) {
            List<String> extracted = tryParseJsonArray(matcher.group());
            if (extracted != null && !extracted.isEmpty()) {
                return extracted;
            }
        }

        logger.warn("无法从模型响应中解析出 JSON 数组: {}", response);
        return List.of();
    }

    private List<String> tryParseJsonArray(String candidate) {
        try {
            return OBJECT_MAPPER.readValue(candidate, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            return null;
        }
    }
}
