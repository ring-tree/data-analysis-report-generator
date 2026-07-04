package com.audaque.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * JSONL 记录工具类，负责向考核 JSONL 文件追加操作记录。
 */
public class JsonlRecorder {

    private static final Logger logger = LoggerFactory.getLogger(JsonlRecorder.class);

    private static final String JSONL_FILENAME = "AI开发考核_csk_数据分析自动报告生成工具.jsonl";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 向项目根目录下的 JSONL 文件追加一条考核记录。
     *
     * @param record 待追加的考核记录
     * @throws IOException 若文件不可写
     */
    public static void append(JsonlRecord record) throws IOException {
        Path jsonlPath = Paths.get(JSONL_FILENAME);
        String jsonLine = MAPPER.writeValueAsString(record);
        String content = System.lineSeparator() + jsonLine;
        try {
            Files.writeString(jsonlPath, content, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            logger.info("Appended JSONL record for round {}", record.roundId());
        } catch (IOException e) {
            logger.error("Failed to append JSONL record for round {}: {}", record.roundId(), e.getMessage(), e);
            throw e;
        }
    }
}
