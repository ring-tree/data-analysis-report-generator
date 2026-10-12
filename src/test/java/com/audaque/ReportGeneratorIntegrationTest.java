package com.audaque;

import com.audaque.ai.AiClient;
import com.audaque.ai.AiManager;
import com.audaque.ai.AiServiceConfig;
import com.audaque.ai.provider.DeepSeekProvider;
import com.audaque.analysis.DataAnalyzer;
import com.audaque.config.TemplateFunctionConfig;
import com.audaque.model.ColumnStat;
import com.audaque.model.DataSummary;
import com.audaque.model.TemplateFunction;
import com.audaque.parser.CsvDataParser;
import com.audaque.template.HtmlReportRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 集成测试，验证从 CSV 输入到 HTML 输出的完整流程及各组件协作。
 */
class ReportGeneratorIntegrationTest {

    private Path sampleCsvPath;

    @BeforeEach
    void setUp() throws URISyntaxException {
        sampleCsvPath = Path.of(Objects.requireNonNull(
                getClass().getClassLoader().getResource("sample.csv")).toURI());
    }

    @Test
    @DisplayName("CSV 解析 → 数据总览 → 模板配置加载 完整集成链路")
    void testCsvToDataSummaryPipeline() throws IOException {
        CsvDataParser parser = new CsvDataParser();
        List<List<Double>> data = parser.parse(sampleCsvPath.toString());
        List<String> columnNames = parser.getColumnNames();

        assertEquals(25, data.size(), "应解析出 25 行数据");
        assertEquals(6, columnNames.size(), "应有 6 列");
        assertEquals("ID", columnNames.get(0));
        assertEquals("Hours", columnNames.get(5));

        DataAnalyzer analyzer = new DataAnalyzer();
        DataSummary summary = analyzer.generateSummary(data, columnNames);

        assertEquals(25, summary.totalRows());
        assertEquals(6, summary.columnCount());
        assertEquals(columnNames, summary.columnNames());
        assertEquals(6, summary.columnStats().size());
        assertTrue(summary.validRows() >= 20, "有效行数应不少于 20");

        ColumnStat scoreStat = summary.columnStats().get("Score");
        assertNotNull(scoreStat);
        assertTrue(scoreStat.max() > 100, "Score 应有异常大值 999.0");
        assertTrue(scoreStat.min() < 10, "Score 应有异常小值 0.5");
        assertTrue(scoreStat.outlierCount() > 0, "Score 列应检测到异常值");

        TemplateFunctionConfig config = new TemplateFunctionConfig();
        List<TemplateFunction> functions = config.getAll();
        assertEquals(12, functions.size(), "应加载 12 个模板函数");
        assertTrue(config.getById("basic_stats").isPresent());
        assertTrue(config.getById("percentile").isPresent());
    }

    @Test
    @DisplayName("统一 AI 接口管理层应加载 DeepSeek 提供方配置")
    void testAiManagerDefaultConfig() {
        AiManager manager = AiManager.getInstance();

        assertTrue(manager.providers().contains(DeepSeekProvider.NAME),
                "应注册 DeepSeek 提供方");
        assertEquals(DeepSeekProvider.NAME, manager.defaultProvider(),
                "默认提供方应为 deepseek");

        AiServiceConfig config = manager.defaultConfig();
        assertEquals(DeepSeekProvider.NAME, config.providerName());
        assertFalse(config.endpoint().isBlank(), "端点不应为空");
        assertFalse(config.model().isBlank(), "模型不应为空");

        AiClient client = manager.defaultClient();
        assertEquals(config.isConfigured(), client.isConfigured(),
                "客户端配置状态应与服务配置一致");
    }

    @Test
    @DisplayName("HTML 报告渲染器应正确生成 HTML 输出文件")
    void testHtmlReportRendering() throws IOException {
        CsvDataParser parser = new CsvDataParser();
        List<List<Double>> data = parser.parse(sampleCsvPath.toString());
        DataAnalyzer analyzer = new DataAnalyzer();
        DataSummary summary = analyzer.generateSummary(data, parser.getColumnNames());

        HtmlReportRenderer renderer = new HtmlReportRenderer();
        String analysisText = "基于数据分析，该数据集包含 25 条记录和 6 个特征列。"
                + "Score 列存在一个极端异常值 999.0，建议进行异常值清洗。"
                + "Age 列在正常范围内 (18-24)，无明显异常。"
                + "数据整体质量良好，有效记录占比超过 80%。";
        renderer.renderToFile(summary, analysisText);

        Path outputPath = Paths.get("output", "report.html");
        assertTrue(Files.exists(outputPath), "应在 output/report.html 生成报告文件");
        assertTrue(Files.size(outputPath) > 1000, "HTML 文件应包含实质性内容");

        String htmlContent = Files.readString(outputPath);
        assertTrue(htmlContent.contains("数据分析报告"), "HTML 应包含报告标题");
        assertTrue(htmlContent.contains("数据总览"), "HTML 应包含数据总览部分");
        assertTrue(htmlContent.contains("各列统计信息"), "HTML 应包含统计信息表格");
        assertTrue(htmlContent.contains("数据质量报告"), "HTML 应包含数据质量报告");
        assertTrue(htmlContent.contains("Score"), "HTML 应包含列名 Score");
        assertTrue(htmlContent.contains("分析结论"), "HTML 应包含分析结论部分");
    }

    @Test
    @DisplayName("execute 主流程端到端测试（AI 未配置时优雅降级，仍产出统计报告）")
    void testExecuteEndToEnd() throws Exception {
        String csvPath = sampleCsvPath.toString();

        Path result = ReportGenerator.execute(csvPath);

        assertNotNull(result, "execute 应返回输出路径");
        assertTrue(Files.exists(result), "输出文件应存在：" + result);
        assertTrue(Files.size(result) > 500, "输出文件应有内容");

        String html = Files.readString(result);
        assertTrue(html.contains("数据分析报告"), "应生成报告标题");
        assertTrue(html.contains("分析结论"), "应包含分析结论区块");

        if (!AiManager.getInstance().defaultConfig().isConfigured()) {
            assertTrue(html.contains("暂无分析结论"), "AI 未配置时应展示占位结论");
        }
    }
}
