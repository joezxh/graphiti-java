package com.graphiti.module.graphiti.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Prompt 模板加载器
 * 负责从 classpath 加载 prompt 模板文件，并支持变量替换
 */
@Slf4j
@Component
public class PromptTemplateLoader {

    private static final String PROMPTS_DIR = "prompts/";

    /**
     * 加载指定名称的 prompt 模板
     *
     * @param templateName 模板文件名（不含路径）
     * @return 模板内容
     */
    public String loadTemplate(String templateName) {
        String path = PROMPTS_DIR + templateName;
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new IllegalArgumentException("Prompt template not found: " + path);
            }
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            log.debug("Loaded prompt template: {}", path);
            return content;
        } catch (IOException e) {
            log.error("Failed to load prompt template: {}", path, e);
            throw new RuntimeException("Failed to load prompt template: " + path, e);
        }
    }

    /**
     * 加载模板并替换变量
     *
     * @param templateName 模板文件名
     * @param variables    变量映射表
     * @return 渲染后的 prompt 内容
     */
    public String render(String templateName, Map<String, String> variables) {
        String template = loadTemplate(templateName);
        return renderTemplate(template, variables);
    }

    /**
     * 渲染模板内容，替换 {{key}} 格式的变量
     *
     * @param template  模板内容
     * @param variables 变量映射表
     * @return 渲染后的内容
     */
    public String renderTemplate(String template, Map<String, String> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    /**
     * 预定义模板名称常量
     */
    public static class Templates {
        public static final String SYSTEM_PROMPT = "system_prompt.txt";
        public static final String EXTRACT_ENTITIES = "extract_entities.txt";
        public static final String EXTRACT_RELATIONS = "extract_relations.txt";
        public static final String SUMMARIZE_NODE = "summarize_node.txt";
        public static final String SUMMARIZE_COMMUNITY = "summarize_community.txt";
        public static final String SUMMARIZE_SAGA = "summarize_saga.txt";
    }
}
