package com.audaque.util;

/**
 * JSONL 考核记录模型，与考核规范字段一一对应。
 *
 * @param roundId        轮次编号，从 1 开始递增
 * @param promptContent  本轮向编程智能体输入的完整自然语言指令
 * @param modifyDiff     本轮代码修改的完整 diff
 * @param commitHash     本轮对应 Git commit 的哈希值
 * @param modifyTime     修改时间，格式 YYYY-MM-DD HH:MM:SS
 * @param agentType      智能体类型，固定为 "Kilo Code"
 * @param devLanguage    开发语言，固定为 "Java"
 */
public record JsonlRecord(
        int roundId,
        String promptContent,
        String modifyDiff,
        String commitHash,
        String modifyTime,
        String agentType,
        String devLanguage) {
}
