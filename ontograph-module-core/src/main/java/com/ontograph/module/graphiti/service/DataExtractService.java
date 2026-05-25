package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.extractor.DataExtractReqVO;
import com.ontograph.module.graphiti.vo.extractor.DataExtractResultVO;

/**
 * 数据提取服务接口
 * 参考 Graphiti 的 EpisodeProcessor.java 和 tianque-ai 的 EntityRelationExtractor.java
 */
public interface DataExtractService {

    /**
     * 完整的数据提取（实体 + 关系）
     *
     * @param reqVO 提取请求
     * @return 提取结果
     */
    DataExtractResultVO extract(DataExtractReqVO reqVO);

    /**
     * 仅提取实体
     *
     * @param reqVO 提取请求
     * @return 提取结果（仅包含实体）
     */
    DataExtractResultVO extractEntities(DataExtractReqVO reqVO);

    /**
     * 仅提取关系（需要先提供实体）
     *
     * @param reqVO 提取请求（需包含 existingEntities）
     * @return 提取结果（仅包含关系）
     */
    DataExtractResultVO extractEdges(DataExtractReqVO reqVO);
}
