# 数据分析自动报告生成工具

基于 Java 21 + Maven 的数据分析自动报告生成工具，支持 CSV 数据解析、统计分析和 AI 驱动的报告生成。

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 构建 | Maven | 3.9.16 |
| CSV 解析 | Apache Commons CSV | 1.12.0 |
| JSON 处理 | Jackson Databind | 2.18.3 |
| 模板引擎 | jte | 3.1.15 |
| HTTP 客户端 | OkHttp | 4.12.0 |
| 统计计算 | Commons Math | 3.6.1 |
| 日志 | SLF4J + Logback | 2.0.16 / 1.5.16 |
| 测试 | JUnit Jupiter | 5.11.4 |
| AI 模型 | DeepSeek v4 Flash | - |

## 项目结构

```
data-analysis-report-generator/
├── src/main/java/com/audaque/
│   ├── ReportGenerator.java          # 主入口，流程编排
│   ├── config/
│   │   └── TemplateFunctionConfig.java  # 模板函数配置加载
│   ├── model/
│   │   ├── ColumnStat.java           # 列统计信息 Record
│   │   ├── DataSummary.java          # 数据总览 Record
│   │   ├── FunctionParameter.java    # 函数参数 Record
│   │   └── TemplateFunction.java     # 模板函数 Record
│   ├── parser/
│   │   └── CsvDataParser.java        # CSV 数据解析器
│   ├── analysis/
│   │   └── DataAnalyzer.java         # 统计分析与异常值检测
│   ├── ai/
│   │   └── DeepSeekOptimizer.java    # DeepSeek API 调用与优化
│   ├── template/
│   │   └── HtmlReportRenderer.java   # HTML 报告渲染器
│   └── util/
│       └── JsonlRecorder.java        # JSONL 记录工具
├── src/main/jte/
│   └── report.jte                    # jte 报告模板
├── src/main/resources/
│   ├── config.properties             # 应用配置
│   └── template_functions.json       # 模板函数定义（12 个函数，4 类别）
├── src/test/java/com/audaque/
│   ├── CsvDataParserTest.java        # CSV 解析器单元测试
│   ├── DataAnalyzerTest.java         # 统计分析单元测试
│   └── ReportGeneratorIntegrationTest.java  # 集成测试
├── pom.xml
└── output/
    └── report.html                   # 生成的 HTML 分析报告
```

## 功能特性

### 数据处理流程

1. **CSV 解析** — 自动检测表头，逐列解析数值，跳过非数值单元格并记录警告
2. **统计计算** — 均值、中位数、标准差、最小/最大值、缺失值计数
3. **异常值检测** — 基于 IQR 方法自动标识异常值
4. **AI 函数选择** — DeepSeek 根据数据特征推荐适用的分析函数
5. **模板优化** — DeepSeek 优化模板函数的实现代码
6. **提示词生成** — 自动生成结构化的分析提示词
7. **结论生成** — DeepSeek 生成详细的数据分析结论
8. **HTML 渲染** — jte 模板引擎渲染现代化数据分析报告

### 模板函数类别（12 个）

| 类别 | 函数 | 说明 |
|------|------|------|
| 数据总览 | total_rows, column_count, basic_stats | 数据统计概览 |
| 数据清洗 | remove_nulls, remove_outliers, fill_missing | 数据质量控制 |
| 数据处理 | normalize, standardize, categorize | 数据变换处理 |
| 数据分析 | correlation, group_by, percentile | 深度分析 |

## 快速开始

### 环境要求

- Java 21+
- Maven 3.9+

### 配置 API Key

编辑 `src/main/resources/config.properties`：

```properties
deepseek.api.key=sk-YOUR_DEEPSEEK_API_KEY
deepseek.api.url=https://api.deepseek.com/v1/chat/completions
deepseek.model=deepseek-v4-flash
```

### 构建

```bash
mvn clean package
```

### 运行

```bash
java -jar target/data-analysis-report-generator-1.0-SNAPSHOT.jar <csv-file-path>
```

生成的报告位于 `output/report.html`。

### 运行测试

```bash
mvn clean test
```

## 报告示例

生成的 HTML 报告包含 4 个模块：

- **数据总览** — 卡片式布局展示总行数、有效行数、列数等关键指标
- **各列统计信息** — 表格展示每列的均值、中位数、标准差、缺失值、异常值（带颜色标记）
- **数据质量报告** — 空行占比、有效数据占比、异常值检测结果
- **分析结论** — DeepSeek AI 生成的专业分析文本

报告使用现代化 CSS 设计，响应式布局，支持移动端查看。

## API 重试机制

所有 DeepSeek API 调用均内置重试逻辑：
- 最多重试 3 次
- 指数退避策略（1s → 2s → 4s）
- HTTP 连接超时 10s，读取超时 60s

## 许可证

MIT License
