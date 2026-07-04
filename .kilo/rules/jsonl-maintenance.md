# JSONL 维护规则

## 文件位置

项目根目录：`AI开发考核_csk_数据分析自动报告生成工具.jsonl`

## 每轮必须追加的记录字段

| 字段           | 类型   | 说明                                        |
| -------------- | ------ | ------------------------------------------- |
| round_id       | number | 数字，从 1 开始递增                               |
| prompt_content | string | 字符串，本轮向编程智能体输入的完整自然语言指令、需求描述 |
| modify_diff    | string | 字符串，本轮代码修改的完整 diff（新增/删除/修改行） |
| commit_hash    | string | 字符串，本轮对应 Git commit 的哈希值                |
| modify_time    | string | 字符串，格式：YYYY-MM-DD HH:MM:SS                   |
| agent_type     | string | 字符串，固定为 "Kilo Code"                          |
| dev_language   | string | 字符串，固定为 "Java"                               |

## JSONL格式样例
（可直接复制作为模板使用，每行一条JSON，无多余空行、无注释）
{
    "round_id":1,
    "prompt_content":"使用Go语言编写一个简易的文本行数统计工具，支持读取本地txt文件，统计文件总行数、空行数、有效代码行数",
    "modify_diff":"@@ -0,0 +1,45 \\npackage main\\n\\nimport (\\n\t\"bufio\"\\n\t\"fmt\"\\n\t\"os\"\\n\t\"strings\"\\n)\\n\\nfunc main() {\\n\t// 代码实现逻辑\\n}",
    "commit_hash":"a1b2c3d4e5f6",
    "modify_time":"2026-06-30 10:20:00",
    "agent_type":"Kilo Code",
    "dev_language":"Java"
}

## 执行流程

1. 完成代码修改后，执行 `git diff HEAD~1` 获取 diff 内容
2. 获取最新 commit hash：`git rev-parse HEAD`
3. 组装 JSON 对象
4. 追加到 JSONL 文件末尾（每行一条 JSON，无多余空行）

## 格式要求

- 编码：UTF-8
- 每行一条独立的 JSON 数据
- 禁止多条 JSON 合并为一行
- 禁止在 JSON 中添加注释
