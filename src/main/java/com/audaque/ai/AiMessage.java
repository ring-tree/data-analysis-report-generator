package com.audaque.ai;

/**
 * 统一 AI 消息模型。
 *
 * <p>用于屏蔽不同 AI 服务提供方在消息结构上的差异，统一使用 {@code role + content} 描述一轮对话。</p>
 *
 * @param role    消息角色，取值参见 {@link #ROLE_SYSTEM}、{@link #ROLE_USER}、{@link #ROLE_ASSISTANT}
 * @param content 消息内容
 */
public record AiMessage(String role, String content) {

    /** 系统角色。 */
    public static final String ROLE_SYSTEM = "system";
    /** 用户角色。 */
    public static final String ROLE_USER = "user";
    /** 助手角色。 */
    public static final String ROLE_ASSISTANT = "assistant";

    /**
     * 构造系统消息。
     *
     * @param content 消息内容
     * @return 系统角色消息
     */
    public static AiMessage system(String content) {
        return new AiMessage(ROLE_SYSTEM, content);
    }

    /**
     * 构造用户消息。
     *
     * @param content 消息内容
     * @return 用户角色消息
     */
    public static AiMessage user(String content) {
        return new AiMessage(ROLE_USER, content);
    }

    /**
     * 构造助手消息。
     *
     * @param content 消息内容
     * @return 助手角色消息
     */
    public static AiMessage assistant(String content) {
        return new AiMessage(ROLE_ASSISTANT, content);
    }
}
