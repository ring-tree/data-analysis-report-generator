package com.audaque;

import com.audaque.ai.DeepSeekOptimizer;
import com.audaque.analysis.DataAnalyzer;
import com.audaque.config.TemplateFunctionConfig;
import com.audaque.model.DataSummary;
import com.audaque.model.TemplateFunction;
import com.audaque.parser.CsvDataParser;
import com.audaque.template.HtmlReportRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据分析自动报告生成工具主入口，编排完整的分析报告生成流程。
 */
public class ReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

    /**
     * 主方法，编排 10 步分析报告生成流程。
     *
     * @param args 命令行参数，args[0] 为 CSV 文件路径
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java -jar data-analysis-report-generator.jar <csv-file-path>");
            System.err.println("  csv-file-path: path to the input CSV data file");
            System.exit(1);
        }

        String csvPath = args[0];
        logger.info("=== Data Analysis Report Generator started ===");
        logger.info("Input CSV: {}", csvPath);

        try {
            Path outputPath = execute(csvPath);
            System.out.println("Report generated successfully: " + outputPath.toAbsolutePath());
            logger.info("=== Report generation completed ===");
        } catch (Exception e) {
            logger.error("Report generation failed: {}", e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static Path execute(String csvPath) throws IOException {
        logger.info("[Step 1] Parsing CSV file: {}", csvPath);
        CsvDataParser parser = new CsvDataParser();
        List<List<Double>> data = parser.parse(csvPath);
        List<String> columnNames = parser.getColumnNames();
        logger.info("[Step 1] Parsed {} rows, {} columns: {}", data.size(), columnNames.size(), columnNames);

        logger.info("[Step 2] Generating data summary...");
        DataAnalyzer analyzer = new DataAnalyzer();
        DataSummary summary = analyzer.generateSummary(data, columnNames);
        logger.info("[Step 2] Summary: totalRows={}, validRows={}, emptyRows={}, columns={}",
                summary.totalRows(), summary.validRows(), summary.emptyRows(), summary.columnCount());

        logger.info("[Step 3] Loading template function configuration...");
        TemplateFunctionConfig config = new TemplateFunctionConfig();
        List<TemplateFunction> allFunctions = config.getAll();
        logger.info("[Step 3] Loaded {} template functions in {} categories",
                allFunctions.size(),
                allFunctions.stream().map(TemplateFunction::category).distinct().count());

        logger.info("[Step 4] Initializing DeepSeek optimizer...");
        DeepSeekOptimizer optimizer;
        try {
            optimizer = new DeepSeekOptimizer();
        } catch (ExceptionInInitializerError e) {
            Throwable cause = e.getCause();
            String message = cause != null ? cause.getMessage() : e.getMessage();
            throw new IllegalStateException("DeepSeek optimizer init failed: " + message, e);
        }
        logger.info("[Step 4] DeepSeek optimizer ready");

        logger.info("[Step 5] Selecting applicable template functions...");
        List<String> selectedIds = optimizer.selectFunctions(summary);
        logger.info("[Step 5] Selected {} functions: {}", selectedIds.size(), selectedIds);

        logger.info("[Step 6] Optimizing {} selected template functions...", selectedIds.size());
        List<TemplateFunction> optimizedFunctions = new ArrayList<>();
        for (String id : selectedIds) {
            TemplateFunction func = config.getById(id).orElse(null);
            if (func == null) {
                logger.warn("[Step 6] Function '{}' not found in config, skipping", id);
                continue;
            }
            try {
                String optimizedImpl = optimizer.optimizeTemplate(func, summary);
                TemplateFunction optimized = new TemplateFunction(
                        func.id(), func.name(), func.category(),
                        func.description(), func.parameters(), optimizedImpl);
                optimizedFunctions.add(optimized);
                logger.info("[Step 6] Optimized: {} ({})", func.name(), func.id());
            } catch (IOException e) {
                logger.error("[Step 6] Failed to optimize '{}': {}", func.name(), e.getMessage());
                optimizedFunctions.add(func);
            }
        }
        logger.info("[Step 6] Optimized {}/{} functions", optimizedFunctions.size(), selectedIds.size());

        logger.info("[Step 7] Generating report prompt...");
        String reportPrompt = optimizer.generateReportPrompt(summary, selectedIds);
        logger.info("[Step 7] Report prompt generated ({} chars)", reportPrompt.length());

        logger.info("[Step 8] Generating final analysis text via DeepSeek API...");
        String analysisText = optimizer.generateAnalysisText(reportPrompt);
        logger.info("[Step 8] Analysis text generated ({} chars)", analysisText.length());

        logger.info("[Step 9] Rendering HTML report...");
        HtmlReportRenderer renderer = new HtmlReportRenderer();
        renderer.renderToFile(summary, analysisText);
        Path outputPath = Paths.get("output", "report.html").toAbsolutePath();
        logger.info("[Step 9] HTML report rendered to: {}", outputPath);

        return outputPath;
    }
}
