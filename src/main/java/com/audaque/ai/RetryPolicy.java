package com.audaque.ai;

/**
 * 统一的 AI 调用重试策略。
 *
 * <p>由统一 AI 接口管理层集中配置，采用指数退避方式计算每次重试前的等待时间。</p>
 *
 * @param maxAttempts          最大尝试次数（含首次调用），必须大于等于 1
 * @param initialBackoffMillis 首次重试的等待时间（毫秒）
 * @param backoffMultiplier    退避倍数，必须大于等于 1.0
 */
public record RetryPolicy(int maxAttempts, long initialBackoffMillis, double backoffMultiplier) {

    /**
     * 紧凑构造器，校验参数合法性。
     */
    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts 必须大于等于 1，实际: " + maxAttempts);
        }
        if (initialBackoffMillis < 0) {
            throw new IllegalArgumentException("initialBackoffMillis 不能为负数，实际: " + initialBackoffMillis);
        }
        if (backoffMultiplier < 1.0) {
            throw new IllegalArgumentException("backoffMultiplier 必须大于等于 1.0，实际: " + backoffMultiplier);
        }
    }

    /**
     * 返回默认的退避策略：最多尝试 3 次，首次等待 1s，退避倍数 2.0（1s → 2s → 4s）。
     *
     * @return 默认重试策略
     */
    public static RetryPolicy defaults() {
        return new RetryPolicy(3, 1000L, 2.0);
    }

    /**
     * 计算第 N 次重试前的等待时间。
     *
     * @param attemptIndex 已失败的尝试序号（从 1 开始）
     * @return 等待时间（毫秒）
     */
    public long backoffMillis(int attemptIndex) {
        double factor = Math.pow(backoffMultiplier, Math.max(0, attemptIndex - 1));
        return (long) (initialBackoffMillis * factor);
    }
}
