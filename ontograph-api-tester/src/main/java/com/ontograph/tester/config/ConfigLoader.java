package com.ontograph.tester.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 配置文件加载器
 */
@Slf4j
public class ConfigLoader {

    private static final String DEFAULT_CONFIG_PATH = "api-tester.yml";
    private static final String[] CONFIG_LOCATIONS = {
            "api-tester.yml",
            "config/api-tester.yml",
            "src/main/resources/api-tester.yml",
            "./api-tester.yml"
    };

    private final ObjectMapper yamlMapper;

    public ConfigLoader() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.yamlMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    }

    /**
     * 加载配置文件
     * 优先级：命令行指定 > 当前目录 > classpath
     */
    public TesterConfig load(String... customPaths) {
        // 1. 先尝试命令行指定的路径
        for (String path : customPaths) {
            if (path != null && !path.isBlank()) {
                File file = new File(path);
                if (file.exists()) {
                    return loadFromFile(file);
                }
            }
        }

        // 2. 尝试当前工作目录
        for (String location : CONFIG_LOCATIONS) {
            File file = new File(location);
            if (file.exists()) {
                return loadFromFile(file);
            }
        }

        // 3. 尝试classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
            if (is != null) {
                return yamlMapper.readValue(is, TesterConfig.class);
            }
        } catch (Exception e) {
            log.debug("无法从classpath加载配置: {}", e.getMessage());
        }

        // 4. 返回默认配置
        log.warn("未找到配置文件，使用默认配置");
        return new TesterConfig();
    }

    private TesterConfig loadFromFile(File file) {
        try {
            TesterConfig config = yamlMapper.readValue(file, TesterConfig.class);
            log.info("从 {} 加载配置文件", file.getAbsolutePath());
            return config;
        } catch (Exception e) {
            log.error("加载配置文件失败: {}", e.getMessage());
            return new TesterConfig();
        }
    }

    /**
     * 从命令行参数解析配置
     * 格式: --key=value 或 -key=value
     */
    public static java.util.Map<String, String> parseArgs(String[] args) {
        java.util.Map<String, String> overrides = new java.util.HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") || arg.startsWith("-")) {
                String[] parts = arg.substring(arg.startsWith("--") ? 2 : 1).split("=", 2);
                if (parts.length == 2) {
                    overrides.put(parts[0], parts[1]);
                }
            }
        }
        return overrides;
    }

    /**
     * 应用命令行参数覆盖
     */
    public static TesterConfig applyOverrides(TesterConfig config, java.util.Map<String, String> overrides) {
        if (overrides.containsKey("server.url") || overrides.containsKey("server-url")) {
            String key = overrides.containsKey("server.url") ? "server.url" : "server-url";
            config.getServer().setBaseUrl(overrides.get(key));
        }
        if (overrides.containsKey("auth.username")) {
            config.getAuth().setUsername(overrides.get("auth.username"));
        }
        if (overrides.containsKey("auth.password")) {
            config.getAuth().setPassword(overrides.get("auth.password"));
        }
        if (overrides.containsKey("test.modules")) {
            // 已经在applyEnvironmentOverrides中处理
        }
        return config;
    }
}
