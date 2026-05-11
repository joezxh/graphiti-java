package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.extractor.ExtractedEntityVO;
import java.util.List;
import java.util.Map;

/**
 * 实体提取服务接口
 * 参考 Graphiti 项目的 extract_nodes.py 实现
 */
public interface EntityExtractorService {

    /**
     * 从文本中提取实体
     *
     * @param content 待提取的文本内容
     * @param entityTypesConfig 实体类型配置
     * @param customInstructions 自定义提取指令
     * @return 提取到的实体列表
     */
    List<ExtractedEntityVO> extractFromText(String content, String entityTypesConfig, String customInstructions);

    /**
     * 从 JSON 中提取实体
     *
     * @param jsonContent 待提取的 JSON 内容
     * @param sourceDescription 数据源描述
     * @param entityTypesConfig 实体类型配置
     * @param customInstructions 自定义提取指令
     * @return 提取到的实体列表
     */
    List<ExtractedEntityVO> extractFromJson(String jsonContent, String sourceDescription, String entityTypesConfig, String customInstructions);

    /**
     * 从消息列表中提取实体
     *
     * @param content 消息内容
     * @param previousEpisodes 历史 episodes
     * @param entityTypesConfig 实体类型配置
     * @param customInstructions 自定义提取指令
     * @return 提取到的实体列表
     */
    List<ExtractedEntityVO> extractFromMessage(String content, List<Map<String, Object>> previousEpisodes,
                                               String entityTypesConfig, String customInstructions);

    /**
     * 使用配置化的提示词模板提取实体
     *
     * @param content 待提取的内容
     * @param sourceType 数据源类型
     * @param templateCode 提示词模板编码
     * @param variables 变量映射
     * @return 提取到的实体列表
     */
    List<ExtractedEntityVO> extractWithTemplate(String content, String sourceType, String templateCode,
                                                Map<String, Object> variables);

    /**
     * 获取默认的实体类型配置
     */
    String getDefaultEntityTypes();
}
