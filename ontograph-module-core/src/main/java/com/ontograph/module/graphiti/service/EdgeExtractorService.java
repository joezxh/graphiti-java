package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.extractor.ExtractedEdgeVO;
import com.ontograph.module.graphiti.vo.extractor.ExtractedEntityVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 关系提取服务接口
 * 参考 Graphiti 项目的 extract_edges.py 实现
 */
public interface EdgeExtractorService {

    /**
     * 从文本中提取关系
     *
     * @param content 文本内容
     * @param entities 已提取的实体列表
     * @param referenceTime 参考时间（用于解析相对时间）
     * @param edgeTypesConfig 关系类型配置
     * @param customInstructions 自定义提取指令
     * @return 提取到的关系列表
     */
    List<ExtractedEdgeVO> extractFromText(String content, List<ExtractedEntityVO> entities,
                                          LocalDateTime referenceTime, String edgeTypesConfig,
                                          String customInstructions);

    /**
     * 从消息中提取关系
     *
     * @param content 当前消息内容
     * @param entities 已提取的实体列表
     * @param previousEpisodes 历史 episodes
     * @param referenceTime 参考时间
     * @param edgeTypesConfig 关系类型配置
     * @param customInstructions 自定义提取指令
     * @return 提取到的关系列表
     */
    List<ExtractedEdgeVO> extractFromMessage(String content, List<ExtractedEntityVO> entities,
                                            List<Map<String, Object>> previousEpisodes,
                                            LocalDateTime referenceTime, String edgeTypesConfig,
                                            String customInstructions);

    /**
     * 使用配置化的提示词模板提取关系
     *
     * @param content 待处理的内容
     * @param entities 实体列表
     * @param sourceType 数据源类型
     * @param templateCode 提示词模板编码
     * @param variables 变量映射
     * @return 提取到的关系列表
     */
    List<ExtractedEdgeVO> extractWithTemplate(String content, List<ExtractedEntityVO> entities,
                                             String sourceType, String templateCode,
                                             Map<String, Object> variables);

    /**
     * 获取默认的关系类型配置
     */
    String getDefaultEdgeTypes();
}
