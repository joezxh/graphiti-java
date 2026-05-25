package com.ontograph.module.graphiti.controller.admin;

import com.ontograph.common.response.CommonResult;
import com.ontograph.module.graphiti.service.EpisodeService;
import com.ontograph.module.graphiti.vo.episode.EpisodeInfoRespVO;
import com.ontograph.module.graphiti.vo.episode.EpisodeListRespVO;
import com.ontograph.module.graphiti.vo.episode.EpisodeMentionsRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * 事件管理控制器
 */
@Tag(name = "事件管理", description = "知识图谱事件（Episode）管理接口")
@RestController
@RequestMapping("/api/v1/graph/episode")
@Validated
@Slf4j
public class EpisodeController {

    @Resource
    private EpisodeService episodeService;

    @GetMapping("/list/{graphId}")
    @Operation(summary = "获取事件列表", description = "分页获取指定图谱的事件列表", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<EpisodeListRespVO> listEpisodes(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam(defaultValue = "20") @Parameter(description = "限制数量", example = "20") Integer limit,
            @RequestParam(defaultValue = "0") @Parameter(description = "偏移量", example = "0") Integer offset) {
        return CommonResult.success(episodeService.listEpisodes(graphId, limit, offset));
    }

    @GetMapping("/{graphId}/{episodeUuid}")
    @Operation(summary = "获取事件详情", description = "根据事件UUID获取事件详细信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<EpisodeInfoRespVO> getEpisodeDetail(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("episodeUuid") @Parameter(description = "事件UUID", required = true) String episodeUuid) {
        return CommonResult.success(episodeService.getEpisodeDetail(graphId, episodeUuid));
    }

    @GetMapping("/{graphId}/{episodeUuid}/mentions")
    @Operation(summary = "获取事件提及", description = "获取事件提及的节点和边", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<EpisodeMentionsRespVO> getEpisodeMentions(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("episodeUuid") @Parameter(description = "事件UUID", required = true) String episodeUuid) {
        return CommonResult.success(episodeService.getEpisodeMentions(graphId, episodeUuid));
    }

    @PostMapping("/{graphId}")
    @Operation(summary = "创建事件", description = "在图谱中创建新的事件", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<EpisodeInfoRespVO> createEpisode(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody @Valid Map<String, Object> episodeData) {
        return CommonResult.success(episodeService.createEpisode(graphId, episodeData));
    }

    @DeleteMapping("/{graphId}/{episodeUuid}")
    @Operation(summary = "删除事件", description = "删除指定的事件", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> deleteEpisode(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("episodeUuid") @Parameter(description = "事件UUID", required = true) String episodeUuid) {
        episodeService.deleteEpisode(graphId, episodeUuid);
        return CommonResult.success(true);
    }
}
