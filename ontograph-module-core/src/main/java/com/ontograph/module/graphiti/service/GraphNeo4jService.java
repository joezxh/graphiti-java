package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.dto.batch.EntityBatchDTO;
import com.ontograph.module.graphiti.dto.batch.EpisodeBatchDTO;
import com.ontograph.module.graphiti.dto.batch.RelationBatchDTO;
import org.neo4j.driver.Driver;

import java.util.List;
import java.util.Map;

/**
 * Neo4j 数据访问服务接口
 * 提供节点和关系的 CRUD 操作
 */
public interface GraphNeo4jService {

    Driver getNeo4jDriver();

    /**
     * 创建实体节点（带嵌入向量）
     */
    Map<String, Object> createEntityNode(String graphId, String uuid, String name, String type,
                                        String summary, float[] embedding, Map<String, Object> properties);

    /**
     * 创建关系
     */
    Map<String, Object> createRelationship(String graphId, String edgeUuid, String sourceUuid,
                                           String targetUuid, String type, String fact, float[] embedding,
                                           Map<String, Object> properties);

    /**
     * 创建关系（重载，不带嵌入向量）
     */
    Map<String, Object> createRelationship(String graphId, String edgeUuid, String sourceUuid,
                                           String targetUuid, String type, String fact);

    /**
     * 获取实体节点
     */
    Map<String, Object> getEntityNode(String graphId, String uuid);

    /**
     * 列出节点
     */
    List<Map<String, Object>> listNodes(String graphId, long skip, long limit);

    /**
     * 列出边
     */
    List<Map<String, Object>> listEdges(String graphId, String type, String source, String target, long skip, long limit);

    /**
     * 根据 UUID 获取边
     */
    Map<String, Object> getEdgeByUuid(String graphId, String uuid);

    /**
     * 更新节点嵌入向量
     */
    void updateNodeEmbedding(String graphId, String uuid, float[] embedding);

    /**
     * 更新边嵌入向量
     */
    void updateEdgeEmbedding(String graphId, String uuid, float[] embedding);

    /**
     * 删除边
     */
    void deleteEdge(String graphId, String uuid);

    /**
     * 按图谱ID统计事件数量
     */
    long countEpisodesByGraphId(String graphId);
    
    /**
     * 统计指定图谱和类型的 Episode 数量
     * @param graphId 图谱ID
     * @param episodeType 剧集类型代码
     * @return Episode 数量
     */
    long countEpisodesByType(String graphId, String episodeType);

    /**
     * 按类型获取图谱的事件列表（分页）
     * @param graphId 图谱ID
     * @param episodeType 剧集类型代码
     * @param limit 限制数量
     * @param offset 偏移量
     * @return Episode 列表
     */
    List<Map<String, Object>> getEpisodesByType(String graphId, String episodeType, int limit, int offset);

    /**
     * 获取图谱的事件列表
     */
    List<Map<String, Object>> getEpisodesByGraphId(String graphId, int limit, int offset);

    /**
     * 根据 UUID 获取事件详情
     */
    Map<String, Object> getEpisodeByUuid(String graphId, String episodeUuid);

    /**
     * 获取事件提及的节点和边
     */
    Map<String, List<Map<String, Object>>> getEpisodeMentions(String episodeUuid);

    /**
     * 创建事件
     */
    Map<String, Object> createEpisode(String graphId, String uuid, String name,
                                     String source, String sourceDescription, String content,
                                     Map<String, Object> properties);

    /**
     * 删除事件
     */
    void deleteEpisode(String graphId, String episodeUuid);

    /**
     * 获取图谱统计信息
     */
    Map<String, Long> getGraphStats(String graphId);

    /**
     * 删除实体节点
     */
    void deleteEntityNode(String graphId, String uuid);

    /**
     * 清除图谱数据
     */
    void clearGraphData(String graphId);

    /**
     * 全文搜索边
     */
    List<Map<String, Object>> searchEdgesByFulltext(String query, String graphId, int limit);

    /**
     * 全文搜索节点
     */
    List<Map<String, Object>> searchNodesByFulltext(String query, String graphId, int limit);

    /**
     * 向量搜索节点
     */
    List<Map<String, Object>> searchNodesByVector(String graphId, float[] embedding, int limit);

    /**
     * 向量搜索边
     */
    List<Map<String, Object>> searchEdgesByVector(String graphId, float[] embedding, int limit);

    /**
     * 初始化向量索引
     */
    void initVectorIndexes(int nodeDimensions, int edgeDimensions);

    /**
     * 根据 UUID 获取节点
     */
    Map<String, Object> getNodeByUuid(String uuid);

    /**
     * 按名称失效节点
     */
    void invalidateNodesByName(String graphId, List<String> entityNames);

    /**
     * 根据节点列表失效边
     */
    void invalidateEdgesByNodes(String graphId, List<String> nodeUuids);

    /**
     * 获取有效节点列表
     */
    List<Map<String, Object>> getValidNodes(String graphId);

    /**
     * 获取指定时间点的有效节点
     */
    List<Map<String, Object>> getValidNodesAt(String graphId, long referenceTime);

    /**
     * 获取指定时间点的有效边
     */
    List<Map<String, Object>> getValidEdgesAt(String graphId, long referenceTime);

    /**
     * 获取事实版本
     */
    List<Map<String, Object>> getFactVersions(String graphId, String entityName);

    /**
     * 根据 UUID 获取边（不限制图谱）
     */
    Map<String, Object> getEdgeByUuidOnly(String uuid);

    /**
     * 克隆图谱数据
     */
    void cloneGraphData(String sourceGraphId, String targetGraphId);

    /**
     * 按图谱ID获取所有节点
     */
    List<Map<String, Object>> getNodesByGraphId(String graphId);

    /**
     * 按图谱ID获取所有边
     */
    List<Map<String, Object>> getEdgesByGraphId(String graphId);

    /**
     * 查找节点
     */
    List<Map<String, Object>> findNodes(String graphId, List<String> labels,
                                       Map<String, Object> properties, long skip, long limit);

    /**
     * 查找边
     */
    List<Map<String, Object>> findEdges(String graphId, List<String> edgeTypes,
                                       Map<String, Object> properties, long skip, long limit);

    /**
     * 获取两节点之间的边
     */
    List<Map<String, Object>> getEdgesBetweenNodes(String sourceUuid, String targetUuid);

    /**
     * 获取节点的所有边
     */
    List<Map<String, Object>> getNodeEdges(String nodeUuid, long skip, long limit);

    /**
     * 获取节点的所有事件
     */
    List<Map<String, Object>> getNodeEpisodes(String nodeUuid, long skip, long limit);

    /**
     * 获取最近的事件
     */
    List<Map<String, Object>> getRecentEpisodes(String graphId, int lastN);

    /**
     * 统计节点数量
     */
    long countNodes(String graphId, List<String> labels);

    /**
     * 统计边数量
     */
    long countEdges(String graphId, List<String> edgeTypes);

    /**
     * 统计社区节点数量
     */
    long countCommunitiesByGraphId(String graphId);

    /**
     * 删除图谱所有关系（RELATES_TO 和 MENTIONS）
     */
    void clearAllRelationships(String graphId);

    /**
     * 删除图谱所有社区节点
     */
    void deleteAllCommunities(String graphId);

    /**
     * 批量写入 Episodes（单事务 UNWIND）
     */
    void batchCreateEpisodes(String graphId, List<EpisodeBatchDTO> episodes);

    /**
     * 批量创建实体节点（单事务 UNWIND）
     */
    void batchCreateEntities(String graphId, List<EntityBatchDTO> entities);

    /**
     * 批量创建关系（单事务 UNWIND）
     */
    void batchCreateRelationships(String graphId, List<RelationBatchDTO> relations);

    /**
     * 原子性批量写入：Episodes + 实体 + 关系在同一个事务中
     */
    void batchAddNodesAndEdges(String graphId, List<EpisodeBatchDTO> episodes,
                               List<EntityBatchDTO> entities, List<RelationBatchDTO> relations);
}
