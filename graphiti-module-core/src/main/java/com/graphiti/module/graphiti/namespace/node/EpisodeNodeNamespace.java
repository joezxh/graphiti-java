package com.graphiti.module.graphiti.namespace.node;

import com.graphiti.module.graphiti.service.EpisodeService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.vo.episode.EpisodeInfoRespVO;
import com.graphiti.module.graphiti.vo.episode.EpisodeListRespVO;
import com.graphiti.module.graphiti.vo.episode.EpisodeMentionsRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

/**
 * 事件节点命名空间
 * 对应 Python: graphiti.nodes.episode
 *
 * <p>Episode 是时间线组织的基本单元，记录原始数据来源，
 * 通过 MENTIONS 关系关联实体节点。
 */
@Slf4j
@RequiredArgsConstructor
public class EpisodeNodeNamespace {

    private final EpisodeService episodeService;
    private final GraphNeo4jService graphNeo4jService;

    /**
     * 保存 Episode
     */
    public EpisodeInfoRespVO save(String graphId, Map<String, Object> episodeData) {
        log.debug("EpisodeNodeNamespace.save: graphId={}", graphId);
        return episodeService.createEpisode(graphId, episodeData);
    }

    /**
     * 批量保存 Episode
     */
    public List<EpisodeInfoRespVO> saveBulk(String graphId, List<Map<String, Object>> episodesData) {
        log.debug("EpisodeNodeNamespace.saveBulk: graphId={}, count={}", graphId, episodesData.size());
        return episodesData.stream()
                .map(data -> episodeService.createEpisode(graphId, data))
                .toList();
    }

    /**
     * 按图谱ID获取 Episode 列表
     */
    public EpisodeListRespVO getByGraphId(String graphId, int limit, int offset) {
        return episodeService.listEpisodes(graphId, limit, offset);
    }

    /**
     * 获取最近的 N 个 Episode
     */
    public List<Map<String, Object>> retrieveRecent(String graphId, int lastN) {
        return graphNeo4jService.getRecentEpisodes(graphId, lastN);
    }

    /**
     * 按 UUID 获取 Episode 详情
     */
    public EpisodeInfoRespVO getByUuid(String graphId, String episodeUuid) {
        return episodeService.getEpisodeDetail(graphId, episodeUuid);
    }

    /**
     * 获取 Episode 提及的节点和边
     */
    public EpisodeMentionsRespVO getMentions(String graphId, String episodeUuid) {
        return episodeService.getEpisodeMentions(graphId, episodeUuid);
    }

    /**
     * 删除 Episode
     */
    public void delete(String graphId, String episodeUuid) {
        episodeService.deleteEpisode(graphId, episodeUuid);
    }
}
