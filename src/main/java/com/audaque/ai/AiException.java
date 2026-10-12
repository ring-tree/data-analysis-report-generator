package com.audaque.ai;

/**
 * 统一 AI 调用异常。
 *
 * <p>统一 AI 接口管理层的所有失败场景（配置缺失、网络异常、响应解析失败、重试耗尽等）
 * 均以本异常对外暴露，屏蔽底层具体实现细节。</p>
 */
public class AiException extends RuntimeException {

    /**
     * 使用错误信息构造异常。
     *
     * @param message 错误信息
     */
    public AiException(String message) {
        super(message);
    }

    /**
     * 使用错误信息和根因构造异常。
     *
     * @param message 错误信息
     * @param cause   根因异常
     */
    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
