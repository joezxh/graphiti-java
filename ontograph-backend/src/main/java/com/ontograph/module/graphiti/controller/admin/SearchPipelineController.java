package com.ontograph.module.graphiti.controller.admin;

import com.ontograph.common.response.CommonResult;
import com.ontograph.module.graphiti.model.search.*;
import com.ontograph.module.graphiti.service.SearchPipelineService;
import com.ontograph.module.graphiti.vo.search.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Search Pipeline 控制器
 *
 * <p>参考 Python：server/graph_service/routers/graph.py:325-378
 *
 * <p>提供与 Python graphiti 完全对齐的新版搜索 API。
 */
@Tag(name = "Search Pipeline", description = "新版搜索 Pipeline API，对齐 Python graphiti")
@RestController
@RequestMapping("/api/v1/graph/search/pipeline")
@Slf4j
public class SearchPipelineController {

    @Resource
    private SearchPipelineService searchPipelineService;

    @PostMapping("/search")
    @Operation(summary = "Pipeline 搜索", description = "并行执行 Edge/Node/Episode/Community 四 Scope 搜索",
            security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<SearchPipelineRespVO> search(@RequestBody SearchPipelineReqVO reqVO) {
        long start = System.currentTimeMillis();

        // 构建配置
        SearchConfig config = buildSearchConfig(reqVO);
        SearchFilters filters = SearchFilters.empty();

        // 执行搜索
        SearchResults results = executePipeline(reqVO, config, filters);

        // 转换结果
        SearchPipelineRespVO resp = toPipelineResp(results);

        long elapsed = System.currentTimeMillis() - start;
        resp.setElapsedMs(elapsed);
        log.info("Pipeline 搜索完成: query={}, graphId={}, elapsed={}ms",
                reqVO.getQuery(), reqVO.getGraphId(), elapsed);

        return CommonResult.success(resp);
    }

    @PostMapping("/parallel")
    @Operation(summary = "并行 Scope 搜索", description = "并行执行多 Scope 搜索，返回各 Scope 独立结果",
            security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<SearchPipelineRespVO> parallelSearch(@RequestBody SearchPipelineReqVO reqVO) {
        // parallel 和 search 内部逻辑相同，只是语义区分
        return search(reqVO);
    }

    @PostMapping("/rerank")
    @Operation(summary = "重排接口", description = "对已有候选项进行重排",
            security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<SearchPipelineRespVO> rerank(@RequestBody RerankReqVO reqVO) {
        if (reqVO == null || reqVO.getQuery() == null) {
            return CommonResult.success(new SearchPipelineRespVO());
        }

        // 转换边候选项
        List<SearchResults.EdgeResult> edgeCandidates = new ArrayList<>();
        if (reqVO.getEdges() != null) {
            for (RerankReqVO.EdgeCandidateVO e : reqVO.getEdges()) {
                SearchResults.EdgeResult edge = new SearchResults.EdgeResult();
                edge.setUuid(e.getUuid());
                edge.setName(e.getName());
                edge.setFact(e.getFact());
                edge.setSourceNodeUuid(e.getSourceNodeUuid());
                edge.setTargetNodeUuid(e.getTargetNodeUuid());
                edge.setScore(e.getScore());
                edgeCandidates.add(edge);
            }
        }

        // 转换节点候选项
        List<SearchResults.NodeResult> nodeCandidates = new ArrayList<>();
        if (reqVO.getNodes() != null) {
            for (RerankReqVO.NodeCandidateVO n : reqVO.getNodes()) {
                SearchResults.NodeResult node = new SearchResults.NodeResult();
                node.setUuid(n.getUuid());
                node.setName(n.getName());
                node.setSummary(n.getSummary());
                node.setLabels(n.getLabels());
                node.setScore(n.getScore());
                nodeCandidates.add(node);
            }
        }

        // 执行重排
        SearchResults results = searchPipelineService.rerank(
                reqVO.getQuery(),
                reqVO.getReranker(),
                reqVO.getMmrLambda(),
                reqVO.getCenterNodeUuid(),
                reqVO.getLimit(),
                edgeCandidates,
                nodeCandidates
        );

        return CommonResult.success(toPipelineResp(results));
    }

    // ==================== 配置转换 ====================

    private SearchConfig buildSearchConfig(SearchPipelineReqVO reqVO) {
        SearchConfig config = SearchConfig.builder()
                .limit(reqVO.getLimit() != null ? reqVO.getLimit() : 10)
                .rerankerMinScore(reqVO.getRerankerMinScore() != null ? reqVO.getRerankerMinScore() : 0.0)
                .build();

        // Edge 配置
        if (reqVO.getEdgeConfig() != null) {
            config.setEdgeConfig(buildEdgeConfig(reqVO.getEdgeConfig()));
        }

        // Node 配置
        if (reqVO.getNodeConfig() != null) {
            config.setNodeConfig(buildNodeConfig(reqVO.getNodeConfig()));
        }

        // Episode 配置
        if (reqVO.getEpisodeConfig() != null) {
            config.setEpisodeConfig(buildEpisodeConfig(reqVO.getEpisodeConfig()));
        }

        // Community 配置
        if (reqVO.getCommunityConfig() != null) {
            config.setCommunityConfig(buildCommunityConfig(reqVO.getCommunityConfig()));
        }

        return config;
    }

    private EdgeSearchConfig buildEdgeConfig(SearchPipelineReqVO.EdgeSearchConfigVO vo) {
        EdgeSearchConfig cfg = new EdgeSearchConfig();
        if (vo.getSearchMethods() != null) {
            List<EdgeSearchMethod> methods = vo.getSearchMethods().stream()
                    .map(m -> EdgeSearchMethod.valueOf(m.toLowerCase()))
                    .collect(Collectors.toList());
            cfg.setSearchMethods(methods);
        }
        if (vo.getReranker() != null) {
            cfg.setReranker(RerankerType.valueOf(vo.getReranker().toLowerCase()));
        }
        cfg.setSimMinScore(vo.getSimMinScore() != null ? vo.getSimMinScore() : 0.6);
        cfg.setMmrLambda(vo.getMmrLambda() != null ? vo.getMmrLambda() : 0.5);
        cfg.setBfsMaxDepth(vo.getBfsMaxDepth() != null ? vo.getBfsMaxDepth() : 2);
        return cfg;
    }

    private NodeSearchConfig buildNodeConfig(SearchPipelineReqVO.NodeSearchConfigVO vo) {
        NodeSearchConfig cfg = new NodeSearchConfig();
        if (vo.getSearchMethods() != null) {
            List<NodeSearchMethod> methods = vo.getSearchMethods().stream()
                    .map(m -> NodeSearchMethod.valueOf(m.toLowerCase()))
                    .collect(Collectors.toList());
            cfg.setSearchMethods(methods);
        }
        if (vo.getReranker() != null) {
            cfg.setReranker(RerankerType.valueOf(vo.getReranker().toLowerCase()));
        }
        cfg.setSimMinScore(vo.getSimMinScore() != null ? vo.getSimMinScore() : 0.6);
        cfg.setMmrLambda(vo.getMmrLambda() != null ? vo.getMmrLambda() : 0.5);
        cfg.setBfsMaxDepth(vo.getBfsMaxDepth() != null ? vo.getBfsMaxDepth() : 2);
        return cfg;
    }

    private EpisodeSearchConfig buildEpisodeConfig(SearchPipelineReqVO.EpisodeSearchConfigVO vo) {
        EpisodeSearchConfig cfg = new EpisodeSearchConfig();
        if (vo.getSearchMethods() != null) {
            List<EpisodeSearchMethod> methods = vo.getSearchMethods().stream()
                    .map(m -> EpisodeSearchMethod.valueOf(m.toLowerCase()))
                    .collect(Collectors.toList());
            cfg.setSearchMethods(methods);
        }
        if (vo.getReranker() != null) {
            cfg.setReranker(RerankerType.valueOf(vo.getReranker().toLowerCase()));
        }
        return cfg;
    }

    private CommunitySearchConfig buildCommunityConfig(SearchPipelineReqVO.CommunitySearchConfigVO vo) {
        CommunitySearchConfig cfg = new CommunitySearchConfig();
        if (vo.getSearchMethods() != null) {
            List<CommunitySearchMethod> methods = vo.getSearchMethods().stream()
                    .map(m -> CommunitySearchMethod.valueOf(m.toLowerCase()))
                    .collect(Collectors.toList());
            cfg.setSearchMethods(methods);
        }
        if (vo.getReranker() != null) {
            cfg.setReranker(RerankerType.valueOf(vo.getReranker().toLowerCase()));
        }
        return cfg;
    }

    private SearchResults executePipeline(SearchPipelineReqVO reqVO, SearchConfig config, SearchFilters filters) {
        return searchPipelineService.search(
                reqVO.getQuery(),
                reqVO.getGraphId(),
                config,
                filters,
                reqVO.getCenterNodeUuid(),
                reqVO.getBfsOriginUuids()
        );
    }

    // ==================== 结果转换 ====================

    private SearchPipelineRespVO toPipelineResp(SearchResults results) {
        SearchPipelineRespVO resp = new SearchPipelineRespVO();

        if (results.getEdges() != null) {
            resp.setEdges(results.getEdges().stream()
                    .map(this::toFactVO)
                    .collect(Collectors.toList()));
            if (results.getEdgeRerankerScores() != null) {
                resp.setEdgeScores(results.getEdgeRerankerScores());
            }
        } else {
            resp.setEdges(new ArrayList<>());
            resp.setEdgeScores(new ArrayList<>());
        }

        if (results.getNodes() != null) {
            resp.setNodes(results.getNodes().stream()
                    .map(this::toNodeVO)
                    .collect(Collectors.toList()));
            if (results.getNodeRerankerScores() != null) {
                resp.setNodeScores(results.getNodeRerankerScores());
            }
        } else {
            resp.setNodes(new ArrayList<>());
            resp.setNodeScores(new ArrayList<>());
        }

        if (results.getEpisodes() != null) {
            resp.setEpisodes(results.getEpisodes().stream()
                    .map(this::toEpisodeVO)
                    .collect(Collectors.toList()));
        } else {
            resp.setEpisodes(new ArrayList<>());
        }

        if (results.getCommunities() != null) {
            resp.setCommunities(results.getCommunities().stream()
                    .map(this::toCommunityVO)
                    .collect(Collectors.toList()));
        } else {
            resp.setCommunities(new ArrayList<>());
        }

        return resp;
    }

    private FactResultVO toFactVO(SearchResults.EdgeResult r) {
        FactResultVO vo = new FactResultVO();
        vo.setUuid(r.getUuid());
        vo.setName(r.getName());
        vo.setFact(r.getFact());
        vo.setSourceNodeUuid(r.getSourceNodeUuid());
        vo.setTargetNodeUuid(r.getTargetNodeUuid());
        vo.setGroupId(r.getGroupId());
        vo.setScore(r.getScore());
        return vo;
    }

    private NodeResultVO toNodeVO(SearchResults.NodeResult n) {
        NodeResultVO vo = new NodeResultVO();
        vo.setUuid(n.getUuid());
        vo.setName(n.getName());
        vo.setSummary(n.getSummary());
        vo.setLabels(n.getLabels());
        vo.setScore(n.getScore());
        return vo;
    }

    private SearchPipelineRespVO.EpisodeResultVO toEpisodeVO(SearchResults.EpisodeResult e) {
        SearchPipelineRespVO.EpisodeResultVO vo = new SearchPipelineRespVO.EpisodeResultVO();
        vo.setUuid(e.getUuid());
        vo.setName(e.getName());
        vo.setContent(e.getContent());
        vo.setSource(e.getSource());
        vo.setSourceDescription(e.getSourceDescription());
        vo.setScore(e.getScore());
        return vo;
    }

    private SearchPipelineRespVO.CommunityResultVO toCommunityVO(SearchResults.CommunityResult c) {
        SearchPipelineRespVO.CommunityResultVO vo = new SearchPipelineRespVO.CommunityResultVO();
        vo.setUuid(c.getUuid());
        vo.setName(c.getName());
        vo.setSummary(c.getSummary());
        vo.setScore(c.getScore());
        return vo;
    }
}
