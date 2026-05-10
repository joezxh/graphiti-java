package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.llm.ExtractedEntityVO;
import com.graphiti.module.graphiti.vo.llm.ExtractedRelationVO;

import java.util.List;

/**
 * LLM客户端服务接口
 */
public interface LlmClientService {

    /**
     * 从文本中提取实体
     */
    List<ExtractedEntityVO> extractEntities(String text, List<String> entityTypes);

    /**
     * 从文本中提取关系
     */
    List<ExtractedRelationVO> extractRelations(String text, List<ExtractedEntityVO> entities);

    /**
     * 生成内容摘要
     */
    String generateSummary(String content);

    /**
     * 生成社区摘要
     */
    String generateCommunitySummary(List<String> nodeSummaries);
}
