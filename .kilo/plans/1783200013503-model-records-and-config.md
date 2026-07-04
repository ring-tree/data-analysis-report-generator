# 第 2 轮：创建 Record 模型类和模板函数配置

## 任务

1. 新建 `ColumnStat`、`FunctionParameter` 两个 Record 类
2. 修改 `DataSummary`、`TemplateFunction` 为 Record 类
3. 创建 `template_functions.json`（4 类别 12 个模板函数）
4. 实现 `TemplateFunctionConfig` 加载 JSON 配置
5. Git 提交 + JSONL 更新

## 执行步骤

### 步骤 1：新建 `ColumnStat.java`

- 路径：`src/main/java/com/audaque/model/ColumnStat.java`
- 类型：Java Record
- 字段（顺序固定）：`String columnName`, `long count`, `double mean`, `double median`, `double stdDev`, `double min`, `double max`, `long missingCount`, `long outlierCount`
- 添加 Javadoc

### 步骤 2：新建 `FunctionParameter.java`

- 路径：`src/main/java/com/audaque/model/FunctionParameter.java`
- 类型：Java Record
- 字段（顺序固定）：`String name`, `String type`, `String description`, `String defaultValue`, `boolean required`
- 添加 Javadoc

### 步骤 3：修改 `DataSummary.java`

- 路径：`src/main/java/com/audaque/model/DataSummary.java`
- 替换 class 为 Record
- 字段（顺序固定）：`long totalRows`, `long emptyRows`, `long validRows`, `int columnCount`, `List<String> columnNames`, `Map<String, ColumnStat> columnStats`
- 添加完整的 Javadoc 和字段说明

### 步骤 4：修改 `TemplateFunction.java`

- 路径：`src/main/java/com/audaque/model/TemplateFunction.java`
- 替换 class 为 Record
- 字段（顺序固定）：`String id`, `String name`, `String category`, `String description`, `List<FunctionParameter> parameters`, `String implementation`
- 添加完整的 Javadoc 和字段说明

### 步骤 5：创建 `template_functions.json`

- 路径：`src/main/resources/template_functions.json`
- 编码：UTF-8
- 结构：JSON 数组，每个元素包含 `id`, `name`, `category`, `description`, `parameters`, `implementation`
- 12 个模板函数分布在 4 个类别：

| 类别 | 函数 ID | 函数名 |
|------|---------|--------|
| 数据总览 | total_rows | 统计总行数 |
| 数据总览 | column_count | 统计列数 |
| 数据总览 | basic_stats | 基本统计量 |
| 数据清洗 | remove_nulls | 移除空值行 |
| 数据清洗 | remove_outliers | 移除异常值 |
| 数据清洗 | fill_missing | 填充缺失值 |
| 数据处理 | normalize | 归一化 |
| 数据处理 | standardize | 标准化 |
| 数据处理 | categorize | 分类编码 |
| 数据分析 | correlation | 相关性分析 |
| 数据分析 | group_by | 分组聚合 |
| 数据分析 | percentile | 百分位数计算 |

- 每个函数需要定义 `parameters` 数组（含 name, type, description, defaultValue, required）
- `implementation` 字段初始为占位伪代码

### 步骤 6：实现 `TemplateFunctionConfig.java`

- 路径：`src/main/java/com/audaque/config/TemplateFunctionConfig.java`
- 使用 Jackson `ObjectMapper` 从 classpath 加载 `template_functions.json`
- 解析为 `List<TemplateFunction>`
- 提供方法：
  - `load()` — 加载并解析 JSON
  - `getAll()` — 返回所有模板函数
  - `getByCategory(String category)` — 按分类筛选
  - `getById(String id)` — 按 ID 查找（返回 Optional）
- 使用 SLF4J 日志
- 异常处理：JSON 解析失败、文件不存在

### 步骤 7：Git 提交 + JSONL 更新

- commit message：`[轮次 2] 创建 Record 模型类和模板函数配置加载器`
- 更新 JSONL：round_id=2，记录 commit hash 和 diff

## 影响范围

- 修改 2 个现有文件（DataSummary.java, TemplateFunctionConfig.java）
- 新建 4 个文件（ColumnStat.java, FunctionParameter.java, TemplateFunction.java 替换, template_functions.json）
- 无其他文件受影响
