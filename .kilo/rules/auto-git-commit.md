# 自动 Git 提交规则

## 触发条件

每次 Kilo Code 完成一个完整的任务轮次（代码生成、修改、重构完成）后，必须自动执行 Git 提交。

## 执行流程

1. 检查当前工作区是否有未暂存的变更：`git status --porcelain`
2. 如有变更，执行：`git add .`
3. 生成 commit message，格式：`[轮次 N] {任务简述}`
4. 执行：`git commit -m "{message}"`
5. 执行：`git push origin main`（如已配置远程仓库）

## Commit Message 规范

- 轮次 N：从 1 开始递增，与 JSONL 的 round_id 对应
- 任务简述：用中文简要描述本轮完成的工作
- 示例：`[轮次 3] 实现 CSV 数据解析器和数据总览统计功能`

## 异常处理

- 如无变更可提交，跳过本次提交
- 如有冲突，暂停并提示用户解决
