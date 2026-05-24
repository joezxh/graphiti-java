package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.episode.EpisodeInfoRespVO;
import com.graphiti.module.graphiti.vo.episode.EpisodeListRespVO;
import com.graphiti.module.graphiti.vo.episode.EpisodeMentionsRespVO;

import java.util.Map;

/**
 * 事件管理服务接口
 */
public interface EpisodeService {
    /**
     * 获取事件列表（分页）
     * @param graphId 图谱ID
     * @param limit 限制数量
     * @param offset 偏移量
     * @return EpisodeListRespVO
     */
    EpisodeListRespVO listEpisodes(String graphId, int limit, int offset);

    /**
     * 按类型获取事件列表（分页）
     * @param graphId 图谱ID
     * @param typeCode 剧集类型代码
     * @param page 页码（从1开始）
     * @param pageSize 每页数量
     * @return EpisodeListRespVO
     */
    EpisodeListRespVO listEpisodesByType(String graphId, String typeCode, int page, int pageSize);
    
    /**
     * 获取事件详情
     * @param graphId 图谱ID
     * @param episodeUuid 事件UUID
     * @return EpisodeInfoRespVO
     */
    EpisodeInfoRespVO getEpisodeDetail(String graphId, String episodeUuid);
    
    /**
     * 获取事件提及的节点和边
     * @param graphId 图谱ID
     * @param episodeUuid 事件UUID
     * @return EpisodeMentionsRespVO
     */
    EpisodeMentionsRespVO getEpisodeMentions(String graphId, String episodeUuid);
    
    /**
     * 创建事件
     * @param graphId 图谱ID
     * @param episodeData 事件数据
     * @return EpisodeInfoRespVO
     */
    EpisodeInfoRespVO createEpisode(String graphId, Map<String, Object> episodeData);
    
    /**
     * 删除事件
     * @param graphId 图谱ID
     * @param episodeUuid 事件UUID
     */
    void deleteEpisode(String graphId, String episodeUuid);
}
