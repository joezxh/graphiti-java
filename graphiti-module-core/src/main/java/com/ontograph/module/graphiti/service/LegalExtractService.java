package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.legal.LegalExtractResultVO;
import java.util.List;
import java.util.Map;

/**
 * 法律知识图谱 LLM 提取服务接口（增强版）
 * 支持使用配置化的提示词模板
 */
public interface LegalExtractService {

    /**
     * 从 JSON 内容中提取法律知识图谱
     *
     * @param graphId       图谱ID
     * @param jsonContent   JSON 文件内容
     * @param fieldMapping  字段映射关系（JSON字段 -> 本体字段）
     * @param sourceFileName 原始文件名
     * @return 提取结果
     */
    LegalExtractResultVO extractFromJson(String graphId, String jsonContent,
                                        Map<String, String> fieldMapping, String sourceFileName);

    /**
     * 从 JSON 内容中提取法律知识图谱，并自动导入到图谱
     *
     * @param graphId       图谱ID
     * @param jsonContent   JSON 文件内容
     * @param fieldMapping  字段映射关系
     * @param sourceFileName 原始文件名
     * @return 提取结果（包含导入统计）
     */
    LegalExtractResultVO extractAndImport(String graphId, String jsonContent,
                                          Map<String, String> fieldMapping, String sourceFileName);

    /**
     * 使用配置化的提示词模板提取法律实体
     *
     * @param graphId        图谱ID
     * @param jsonContent    JSON 内容
     * @param templateCode   提示词模板编码
     * @param sourceFileName 原始文件名
     * @return 提取结果
     */
    LegalExtractResultVO extractWithTemplate(String graphId, String jsonContent,
                                           String templateCode, String sourceFileName);

    /**
     * 提取法律实体（不导入）
     *
     * @param jsonContent  JSON 内容
     * @param templateCode 提示词模板编码（可选）
     * @return 提取结果
     */
    LegalExtractResultVO extractOnly(String jsonContent, String templateCode);

    /**
     * 获取可用的法律提取模板列表
     *
     * @return 模板编码列表
     */
    List<String> getAvailableTemplates();

    /**
     * 获取默认的法律提取提示词
     *
     * @return 系统提示词
     */
    String getDefaultPrompt();
}
