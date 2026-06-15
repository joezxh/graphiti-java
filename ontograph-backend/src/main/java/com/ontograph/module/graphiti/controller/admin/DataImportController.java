package com.ontograph.module.graphiti.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.common.response.CommonResult;
import com.ontograph.framework.security.util.UserContext;
import com.ontograph.module.graphiti.service.BulkImportTaskService;
import com.ontograph.module.graphiti.service.DataImportService;
import com.ontograph.module.graphiti.vo.imports.AddDataBatchReqVO;
import com.ontograph.module.graphiti.vo.imports.AddDataReqVO;
import com.ontograph.module.graphiti.vo.imports.AddMessagesReqVO;
import com.ontograph.module.graphiti.vo.imports.FactTripleReqVO;
import com.ontograph.system.dal.dataobject.OperationLogDO;
import com.ontograph.system.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据导入控制器
 */
@Tag(name = "数据导入", description = "知识图谱数据导入相关接口")
@RestController
@RequestMapping("/api/v1/graph/data")
@Validated
@Slf4j
public class DataImportController {

    @Resource
    private DataImportService dataImportService;

    @Resource
    private BulkImportTaskService bulkImportTaskService;

    @Resource
    private OperationLogService operationLogService;

    @Resource
    private UserContext userContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录数据操作日志
     */
    private void saveDataOpLog(String operation, String method, String graphId,
                                Object params, int status, String errorMsg, long startTime) {
        try {
            OperationLogDO logDO = new OperationLogDO();
            logDO.setUsername(userContext.getCurrentUsername());
            logDO.setOperation(operation);
            logDO.setMethod(method);

            Map<String, Object> paramMap = new HashMap<>();
            if (graphId != null) {
                paramMap.put("graphId", graphId);
            }
            if (params != null) {
                paramMap.put("detail", params);
            }
            logDO.setParams(objectMapper.writeValueAsString(paramMap));
            logDO.setStatus(status);
            logDO.setErrorMsg(errorMsg);
            logDO.setDuration((int) (System.currentTimeMillis() - startTime));
            logDO.setCreateTime(LocalDateTime.now());
            operationLogService.saveLog(logDO);
        } catch (Exception e) {
            log.error("记录数据操作日志失败: operation={}, graphId={}", operation, graphId, e);
        }
    }

    @PostMapping("/add")
    @Operation(summary = "添加单条数据", description = "添加单条数据并自动提取实体和关系",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> addData(@Valid @RequestBody AddDataReqVO reqVO) {
        long start = System.currentTimeMillis();
        try {
            dataImportService.addData(reqVO);
            saveDataOpLog("添加单条数据", "POST /graph/data/add", reqVO.getGraphId(),
                          Map.of("sourceType", reqVO.getSourceType()), 1, null, start);
            return CommonResult.success(true);
        } catch (Exception e) {
            saveDataOpLog("添加单条数据", "POST /graph/data/add", reqVO.getGraphId(),
                          Map.of("sourceType", reqVO.getSourceType()), 0, e.getMessage(), start);
            throw e;
        }
    }

    @PostMapping("/batch")
    @Operation(summary = "批量添加数据（异步）", description = "批量导入数据到图谱，立即返回 taskId",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<String> addDataBatch(@Valid @RequestBody AddDataBatchReqVO reqVO) {
        long start = System.currentTimeMillis();
        try {
            if (reqVO.getContentChunkSize() == null) {
                reqVO.setContentChunkSize(50);
            }
            if (reqVO.getNeo4jChunkSize() == null) {
                reqVO.setNeo4jChunkSize(200);
            }

            String taskId = bulkImportTaskService.executeAsync(reqVO);
            saveDataOpLog("批量添加数据(异步)", "POST /graph/data/batch",
                          reqVO.getGraphId(),
                          Map.of("taskId", taskId, "itemCount", reqVO.getItems() != null ? reqVO.getItems().size() : 0),
                          1, null, start);
            return CommonResult.success(taskId);
        } catch (Exception e) {
            saveDataOpLog("批量添加数据(异步)", "POST /graph/data/batch",
                          reqVO.getGraphId(),
                          Map.of("itemCount", reqVO.getItems() != null ? reqVO.getItems().size() : 0),
                          0, e.getMessage(), start);
            throw e;
        }
    }

    @PostMapping("/messages")
    @Operation(summary = "添加消息", description = "添加对话历史消息到图谱",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> addMessages(@Valid @RequestBody AddMessagesReqVO reqVO) {
        long start = System.currentTimeMillis();
        String graphId = reqVO.getGraphId();
        try {
            dataImportService.addMessages(reqVO);
            saveDataOpLog("添加消息", "POST /graph/data/messages", graphId, null, 1, null, start);
            return CommonResult.success(true);
        } catch (Exception e) {
            saveDataOpLog("添加消息", "POST /graph/data/messages", graphId, null, 0, e.getMessage(), start);
            throw e;
        }
    }

    @PostMapping("/fact-triple")
    @Operation(summary = "添加事实三元组", description = "直接添加事实三元组到图谱",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> addFactTriple(@Valid @RequestBody FactTripleReqVO reqVO) {
        long start = System.currentTimeMillis();
        try {
            dataImportService.addFactTriple(reqVO);
            saveDataOpLog("添加事实三元组", "POST /graph/data/fact-triple", reqVO.getGraphId(),
                          Map.of("relationType", reqVO.getRelationType()), 1, null, start);
            return CommonResult.success(true);
        } catch (Exception e) {
            saveDataOpLog("添加事实三元组", "POST /graph/data/fact-triple", reqVO.getGraphId(),
                          Map.of("relationType", reqVO.getRelationType()), 0, e.getMessage(), start);
            throw e;
        }
    }

    @PostMapping("/entity-node")
    @Operation(summary = "添加实体节点", description = "直接写入实体节点，不经过LLM提取",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> addEntityNode(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody @Valid java.util.Map<String, Object> nodeData) {
        long start = System.currentTimeMillis();
        try {
            dataImportService.addEntityNode(graphId, nodeData);
            saveDataOpLog("添加实体节点", "POST /graph/data/entity-node", graphId,
                          Map.of("nodeName", nodeData.get("name")), 1, null, start);
            return CommonResult.success(true);
        } catch (Exception e) {
            saveDataOpLog("添加实体节点", "POST /graph/data/entity-node", graphId,
                          Map.of("nodeName", nodeData.get("name")), 0, e.getMessage(), start);
            throw e;
        }
    }

    // ==================== 删除操作（对齐 Python /ingest/ 路由） ====================

    @DeleteMapping("/entity-edge/{uuid}")
    @Operation(summary = "删除实体边", description = "根据边UUID删除实体边",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> deleteEntityEdge(
            @PathVariable("uuid") @Parameter(description = "边UUID", required = true) String uuid) {
        long start = System.currentTimeMillis();
        try {
            dataImportService.deleteEntityEdge(uuid);
            saveDataOpLog("删除实体边", "DELETE /graph/data/entity-edge/{uuid}", null,
                          Map.of("edgeUuid", uuid), 1, null, start);
            return CommonResult.success(null);
        } catch (Exception e) {
            saveDataOpLog("删除实体边", "DELETE /graph/data/entity-edge/{uuid}", null,
                          Map.of("edgeUuid", uuid), 0, e.getMessage(), start);
            throw e;
        }
    }

    @DeleteMapping("/group/{graphId}")
    @Operation(summary = "删除图谱数据", description = "删除图谱中的所有数据（含节点、边、Episode）",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> deleteGroup(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        long start = System.currentTimeMillis();
        try {
            dataImportService.deleteGroup(graphId);
            saveDataOpLog("删除图谱数据", "DELETE /graph/data/group/{graphId}", graphId, null, 1, null, start);
            return CommonResult.success(null);
        } catch (Exception e) {
            saveDataOpLog("删除图谱数据", "DELETE /graph/data/group/{graphId}", graphId, null, 0, e.getMessage(), start);
            throw e;
        }
    }

    @DeleteMapping("/episode/{uuid}")
    @Operation(summary = "删除 Episode", description = "根据 Episode UUID 删除事件",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> deleteEpisode(
            @PathVariable("uuid") @Parameter(description = "Episode UUID", required = true) String uuid) {
        long start = System.currentTimeMillis();
        try {
            dataImportService.deleteEpisode(uuid);
            saveDataOpLog("删除Episode", "DELETE /graph/data/episode/{uuid}", null,
                          Map.of("episodeUuid", uuid), 1, null, start);
            return CommonResult.success(null);
        } catch (Exception e) {
            saveDataOpLog("删除Episode", "DELETE /graph/data/episode/{uuid}", null,
                          Map.of("episodeUuid", uuid), 0, e.getMessage(), start);
            throw e;
        }
    }

    @PostMapping("/clear")
    @Operation(summary = "清空所有图谱数据", description = "清空所有图谱中的数据（全局操作）",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> clearAll() {
        long start = System.currentTimeMillis();
        try {
            // 清空操作需要特殊处理，暂时返回不支持
            throw new com.ontograph.common.exception.BusinessException(
                501, "请使用 DELETE /graph/{graphId}/clear 清空指定图谱数据");
        } catch (Exception e) {
            saveDataOpLog("清空所有图谱数据", "POST /graph/data/clear", null, null, 0, e.getMessage(), start);
            throw e;
        }
    }
}
