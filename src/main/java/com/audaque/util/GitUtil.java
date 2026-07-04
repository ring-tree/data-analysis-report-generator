package com.audaque.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Git 操作工具类，提供获取 commit hash 和 diff 内容的静态方法。
 */
public class GitUtil {

    private static final Logger logger = LoggerFactory.getLogger(GitUtil.class);

    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath();

    /**
     * 获取当前 HEAD 的完整 commit hash。
     *
     * @return 完整 hash 字符串，非 git 仓库时返回空字符串
     */
    public static String getCurrentCommitHash() {
        try {
            String result = execute("git", "rev-parse", "HEAD");
            return result.trim();
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to get current commit hash: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return "";
        }
    }

    /**
     * 获取最近一次提交的完整 diff 内容。
     *
     * @return diff 内容字符串，首次提交时返回空字符串
     */
    public static String getLastDiff() {
        try {
            execute("git", "rev-parse", "HEAD~1");
        } catch (IOException | InterruptedException e) {
            logger.info("No previous commit found, returning empty diff");
            return "";
        }

        try {
            return execute("git", "diff", "HEAD~1", "HEAD");
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to get last diff: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return "";
        }
    }

    private static String execute(String... command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(PROJECT_ROOT.toFile());
        builder.redirectErrorStream(false);

        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!output.isEmpty()) {
                    output.append(System.lineSeparator());
                }
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errReader.readLine()) != null) {
                    errorOutput.append(line);
                }
            }
            throw new IOException("Git command failed with exit code " + exitCode + ": " + errorOutput);
        }

        return output.toString();
    }

}
