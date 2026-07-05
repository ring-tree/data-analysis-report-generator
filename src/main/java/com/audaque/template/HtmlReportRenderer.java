package com.audaque.template;

import com.audaque.model.DataSummary;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * HTML 报告渲染器，使用 jte 模板引擎将数据分析结果渲染为 HTML 文件。
 */
public class HtmlReportRenderer {

    private static final Logger logger = LoggerFactory.getLogger(HtmlReportRenderer.class);

    private static final String TEMPLATE_DIR = "src/main/jte";
    private static final String TEMPLATE_NAME = "report.jte";
    private static final String DEFAULT_OUTPUT_DIR = "output";
    private static final String DEFAULT_OUTPUT_FILE = "report.html";

    private final TemplateEngine templateEngine;

    /**
     * 构造 HTML 报告渲染器，初始化 jte 模板引擎。
     */
    public HtmlReportRenderer() {
        Path templatePath = Paths.get(TEMPLATE_DIR);
        if (!Files.isDirectory(templatePath)) {
            logger.warn("Template directory not found: {}. Templates must be available at runtime.", templatePath.toAbsolutePath());
        }
        DirectoryCodeResolver codeResolver = new DirectoryCodeResolver(templatePath);
        this.templateEngine = TemplateEngine.create(codeResolver, ContentType.Html);
        logger.info("HtmlReportRenderer initialized with template dir: {}", templatePath.toAbsolutePath());
    }

    /**
     * 渲染数据分析报告为 HTML 字符串。
     *
     * @param summary      数据总览统计对象
     * @param analysisText DeepSeek 生成的分析结论文本（可为 null）
     * @return 渲染后的完整 HTML 字符串
     * @throws IOException 模板编译或渲染失败
     */
    public String render(DataSummary summary, String analysisText) throws IOException {
        String safeAnalysisText = formatAnalysisText(analysisText);

        TemplateOutput output = new StringOutput();
        templateEngine.render(TEMPLATE_NAME,
                Map.of("summary", summary, "analysisText", safeAnalysisText),
                output);

        String html = output.toString();
        logger.info("Rendered HTML report ({} chars)", html.length());
        return html;
    }

    /**
     * 渲染数据分析报告并写入到默认输出文件 output/report.html。
     *
     * @param summary      数据总览统计对象
     * @param analysisText DeepSeek 生成的分析结论文本（可为 null）
     * @throws IOException 模板渲染或文件写入失败
     */
    public void renderToFile(DataSummary summary, String analysisText) throws IOException {
        renderToFile(summary, analysisText, DEFAULT_OUTPUT_DIR, DEFAULT_OUTPUT_FILE);
    }

    /**
     * 渲染数据分析报告并写入到指定输出文件。
     *
     * @param summary      数据总览统计对象
     * @param analysisText DeepSeek 生成的分析结论文本（可为 null）
     * @param outputDir    输出目录
     * @param outputFile   输出文件名
     * @throws IOException 模板渲染或文件写入失败
     */
    public void renderToFile(DataSummary summary, String analysisText, String outputDir, String outputFile)
            throws IOException {
        String html = render(summary, analysisText);

        Path dir = Paths.get(outputDir);
        if (!Files.isDirectory(dir)) {
            Files.createDirectories(dir);
            logger.info("Created output directory: {}", dir.toAbsolutePath());
        }

        Path filePath = dir.resolve(outputFile);
        Files.writeString(filePath, html);
        logger.info("Report written to: {}", filePath.toAbsolutePath());
    }

    private String formatAnalysisText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String escaped = text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");

        escaped = escaped.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        escaped = escaped.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        escaped = escaped.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");
        escaped = escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        escaped = escaped.replaceAll("(?m)^- (.+)$", "<li>$1</li>");
        escaped = escaped.replaceAll("`(.+?)`", "<code>$1</code>");

        return escaped;
    }
}
