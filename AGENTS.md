# 数据分析自动报告生成工具 — Kilo Code 系统指令

## 项目概述

开发一个 Java 21 + Maven 3.19.6 的数据分析自动报告生成工具。

- 输入：仅包含数值类型的 CSV 数据表
- 处理：通用分析流程 + 模板函数（数据总览、数据清洗、数据处理、数据分析）
- 输出：HTML 格式的数据分析报告
- 外部模型：通过 DeepSeek 官方 API 调用 deepseek-v4-flash 模型优化模板函数

## 强制工作流（每次会话必须遵守）

### 步骤 1：理解需求

- 收到任何开发指令后，先解析需求，拆解为具体可执行任务
- 如需调用 DeepSeek API，明确输入输出格式

### 步骤 2：编写/修改代码

- 所有 Java 代码遵循 Java 21 语法规范
- 使用 Maven 3.19.6 管理依赖
- 代码必须包含完整的异常处理和日志记录

### 步骤 3：代码审核

- 生成代码后，自我审核：检查空指针、边界条件、资源释放
- 确认与现有代码风格一致

### 步骤 4：Git 提交（必须执行）

- 每次任务完成后，**必须**执行 Git 提交
- Commit message 格式：`[轮次 N] {简短描述}`
- 示例：`[轮次 1] 初始化项目结构和 Maven 配置`

### 步骤 5：更新 JSONL 记录（必须执行）

- 每次提交后，**必须**在 `AI开发考核_姓名_数据分析自动报告生成工具.jsonl` 中追加一行 JSON
- JSON 格式严格遵循考核规范，字段：round_id, prompt_content, modify_diff, commit_hash, modify_time, agent_type, dev_language
- 时间格式：YYYY-MM-DD HH:MM:SS
- agent_type: "Kilo Code"
- dev_language: "Java"

## 禁止行为

- 禁止跳过 Git 提交直接修改代码
- 禁止不更新 JSONL 文件就结束任务
- 禁止清空或修改已有的 Git 提交历史
- 禁止合并多条 JSONL 记录为一行
