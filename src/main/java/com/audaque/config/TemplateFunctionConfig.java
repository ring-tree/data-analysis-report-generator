package com.audaque.config;

import com.audaque.model.TemplateFunction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 模板函数配置加载器，负责从 classpath 下的 template_functions.json 加载模板函数配置。
 */
public class TemplateFunctionConfig {

    private static final Logger logger = LoggerFactory.getLogger(TemplateFunctionConfig.class);

    private static final String CONFIG_RESOURCE = "template_functions.json";

    private final List<TemplateFunction> functions;

    /**
     * 构造并加载模板函数配置。
     */
    public TemplateFunctionConfig() {
        this.functions = load();
    }

    /**
     * 从 classpath 加载 template_functions.json 并解析为模板函数列表。
     *
     * @return 模板函数列表，加载失败时返回空列表
     */
    private List<TemplateFunction> load() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (input == null) {
                logger.error("Template functions config file not found: {}", CONFIG_RESOURCE);
                return Collections.emptyList();
            }
            List<TemplateFunction> loaded = mapper.readValue(input, new TypeReference<List<TemplateFunction>>() {});
            logger.info("Loaded {} template functions from {}", loaded.size(), CONFIG_RESOURCE);
            return loaded;
        } catch (IOException e) {
            logger.error("Failed to load template functions config: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取所有已加载的模板函数。
     *
     * @return 模板函数列表，不可修改
     */
    public List<TemplateFunction> getAll() {
        return Collections.unmodifiableList(functions);
    }

    /**
     * 按分类获取模板函数。
     *
     * @param category 分类名称，如 "数据总览"
     * @return 匹配分类的模板函数列表
     */
    public List<TemplateFunction> getByCategory(String category) {
        return functions.stream()
                .filter(f -> f.category().equals(category))
                .collect(Collectors.toList());
    }

    /**
     * 按 ID 查找模板函数。
     *
     * @param id 模板函数唯一标识
     * @return 匹配的模板函数，不存在时返回 Optional.empty()
     */
    public Optional<TemplateFunction> getById(String id) {
        return functions.stream()
                .filter(f -> f.id().equals(id))
                .findFirst();
    }
}
