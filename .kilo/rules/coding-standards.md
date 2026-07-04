# Java 21 编码规范

## 语言版本

- 使用 Java 21 LTS 语法特性
- 优先使用 Records、Switch Expressions、Text Blocks 等新特性

## 包结构

- 基包：com.audaque
- config：配置类（模板函数配置加载）
- model：数据模型（POJO/Record）
- parser：数据解析（CSV 读取）
- analysis：数据分析（统计、清洗）
- template：报告渲染（HTML 生成）
- ai：DeepSeek API 调用
- util：工具类（JSONL 记录等）

## 代码风格

- 类名：PascalCase
- 方法名/变量名：camelCase
- 常量：UPPER_SNAKE_CASE
- 每个 public 类必须有 Javadoc
- 每个 public 方法必须有 Javadoc

## 异常处理

- 不使用 `catch (Exception e)` 吞掉所有异常
- 使用具体的异常类型
- 顶层入口捕获异常并记录日志后退出

## 资源管理

- 使用 Try-With-Resources 管理 IO 资源
- 确保所有流正确关闭
