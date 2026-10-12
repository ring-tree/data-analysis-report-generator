# 统一 AI 接口管理层 — 模块说明 / 接口定义 / 配置指引

> 本文档描述 `com.audaque.ai` 统一 AI 接口管理层的整体结构、对外接口定义与配置接入方式。

## 1. 设计目标

| 目标 | 落地方式 |
|------|----------|
| 集中管理 AI 配置 | 唯一的 `ai-config.properties` + `AiServiceConfig` + 全局单例 `AiManager` |
| 标准化调用入口 | `AiClient.chat(...)` 唯一出口，隐藏协议细节 |
| 屏蔽底层差异 | `AiProvider` 适配接口 + `AbstractOpenAiCompatibleProvider` 公共基类 |
| 内置 DeepSeek v4.1 flash 预设 | `DeepSeekProvider` 内置端点、模型、响应解析 |
| 预留 API 接入点 | 配置文件中标注【接入点 1/2/3】，密钥未接入时优雅降级 |
| 可扩展 | 注册表 + 工厂：新增模型只需实现 `AiProvider` 并 `register` |

## 2. 模块结构

```
com.audaque.ai
├── AiManager                      # 全局唯一入口：加载配置 + 提供方注册 + 产出客户端
├── AiClient                       # 统一调用客户端：默认参数补齐 + 重试编排
├── AiProvider                     # 提供方适配接口（name + chat）
├── AiProviderFactory              # 提供方工厂（函数式接口）
├── AiProviderRegistration         # 提供方注册信息（名称/默认端点/默认模型/工厂）
├── AiServiceConfig                # 提供方服务配置（端点/密钥/模型/超时/重试/默认采样参数）
├── AiRequest / AiResponse / AiMessage   # 统一请求 / 响应 / 消息模型
├── RetryPolicy                    # 重试策略（指数退避）
├── AiException                    # 统一调用异常
├── provider/
│   ├── AbstractOpenAiCompatibleProvider   # OpenAI 兼容协议基类（请求构建/鉴权/响应解析）
│   └── DeepSeekProvider                   # DeepSeek v4.1 flash 适配器（内置预设）
└── service/
    └── AiAnalysisService          # 报告领域服务：仅依赖 AiClient 编排领域提示词
```

### 分层与调用关系

```
ReportGenerator
   │  创建
   ▼
AiAnalysisService ── 使用 ──► AiClient ── 委托 ──► AiProvider(DeepSeekProvider)
                                   │                      │
                                   │ 重试编排              │ HTTP / OpenAI 兼容协议
                                   ▼                      ▼
                              RetryPolicy          DeepSeek / 其他服务

AiManager（单例）：ai-config.properties ──► AiServiceConfig ──► 装配 AiProvider ──► AiClient
```

## 3. 接口定义

### 3.1 `AiManager`（全局唯一入口）

| 方法 | 说明 |
|------|------|
| `static AiManager getInstance()` | 获取全局单例 |
| `void register(AiProviderRegistration)` | 注册新的 AI 提供方 |
| `AiClient defaultClient()` | 获取默认提供方客户端 |
| `AiClient client(String provider)` | 按名称获取客户端 |
| `AiServiceConfig defaultConfig()` / `config(String)` | 获取提供方配置 |
| `String defaultProvider()` / `Set<String> providers()` | 查询默认提供方 / 已注册提供方 |
| `void reload()` | 重新加载配置（运行期热更新） |

### 3.2 `AiClient`（统一调用入口）

| 方法 | 说明 |
|------|------|
| `AiResponse chat(AiRequest request)` | 标准调用，失败按 `RetryPolicy` 重试 |
| `AiResponse chat(String systemPrompt, String userPrompt)` | 快捷调用 |
| `boolean isConfigured()` | 是否已配置有效密钥 |

### 3.3 `AiProvider`（提供方适配接口）

```java
public interface AiProvider {
    String name();                          // 提供方唯一标识
    AiResponse chat(AiRequest request);      // 仅执行一次网络调用，不含重试
}
```

> 约定：重试、超时由 `AiClient` / `AiServiceConfig` 统一编排，适配器只负责协议本身。

### 3.4 数据模型

- `AiMessage(String role, String content)`：`role ∈ {system, user, assistant}`，提供 `system()/user()/assistant()` 静态工厂。
- `AiRequest`：`messages`（必填）、`model` / `temperature` / `maxTokens`（可选覆盖，缺省用配置默认值），通过 `AiRequest.builder()` 构建。
- `AiResponse(String content, String model, String finishReason)`。
- `AiServiceConfig`：`providerName / endpoint / apiKey / model / temperature / maxTokens / connectTimeoutSeconds / readTimeoutSeconds / writeTimeoutSeconds / retryPolicy`。
- `RetryPolicy(maxAttempts, initialBackoffMillis, backoffMultiplier)`：退避时间 = `initialBackoffMillis × backoffMultiplier^(n-1)`。

## 4. 配置使用指引

配置文件：`src/main/resources/ai-config.properties`（AI 模块唯一配置入口）

命名规范：`ai.<provider>.<参数名>`，其中 `<provider>` 需与管理层注册名称一致。

```properties
ai.default-provider=deepseek

# 【接入点 1】API 密钥
ai.deepseek.api-key=sk-YOUR_DEEPSEEK_API_KEY_HERE
# 【接入点 2】服务地址
ai.deepseek.endpoint=https://api.deepseek.com/v1/chat/completions
# 【接入点 3】模型名称
ai.deepseek.model=deepseek-v4.1-flash
```

### 参数清单

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `ai.default-provider` | `deepseek` | 默认提供方 |
| `ai.<p>.api-key` | 占位值 | API 密钥（含 `YOUR` 视为未配置） |
| `ai.<p>.endpoint` | 提供方内置默认 | 接口端点 |
| `ai.<p>.model` | 提供方内置默认 | 模型名称 |
| `ai.<p>.temperature` | `0.7` | 默认采样温度 |
| `ai.<p>.max-tokens` | `2048` | 默认最大 token |
| `ai.<p>.connect-timeout-seconds` | `10` | 连接超时 |
| `ai.<p>.read-timeout-seconds` | `60` | 读取超时 |
| `ai.<p>.write-timeout-seconds` | `60` | 写入超时 |
| `ai.<p>.max-attempts` | `3` | 最大尝试次数 |
| `ai.<p>.initial-backoff-millis` | `1000` | 首次退避时间 |
| `ai.<p>.backoff-multiplier` | `2.0` | 退避倍数 |

> 所有参数均可缺省；缺省时使用各提供方内置默认值或框架默认值。非法数值会记录 WARN 并回退默认。

### 优雅降级

当密钥为空或仍为占位值（`AiServiceConfig.isConfigured() == false`）时：

- `AiManager` 正常初始化，不抛异常；
- 报告主流程跳过 AI 增强步骤，仍生成统计类报告；
- 直接调用 `AiClient.chat(...)` 会抛出明确的 `AiException`。

## 5. 扩展新模型

以新增 `openai` 提供方为例：

1. 实现适配器（若兼容 OpenAI 协议，继承基类即可）：

```java
public class OpenAiProvider extends AbstractOpenAiCompatibleProvider {
    public static final String NAME = "openai";
    public static final AiProviderRegistration REGISTRATION =
            new AiProviderRegistration(NAME,
                    "https://api.openai.com/v1/chat/completions",
                    "gpt-4o-mini", OpenAiProvider::new);

    public OpenAiProvider(AiServiceConfig config) { super(config); }

    @Override public String name() { return NAME; }
}
```

2. 在 `AiManager` 构造函数中注册：`register(OpenAiProvider.REGISTRATION);`
3. 在 `ai-config.properties` 增加 `ai.openai.*` 配置块，并将 `ai.default-provider` 指向 `openai`。

无需修改 `AiClient`、`AiAnalysisService` 等上层代码。
