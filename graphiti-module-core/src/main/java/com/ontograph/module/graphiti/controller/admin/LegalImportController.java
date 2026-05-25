package com.ontograph.module.graphiti.controller.admin;

import com.ontograph.common.response.CommonResult;
import com.ontograph.module.graphiti.service.LegalImportService;
import com.ontograph.module.graphiti.vo.legal.ImportLegalKGReqVO;
import com.ontograph.module.graphiti.vo.legal.LegalImportResultRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 法律知识图谱导入控制器
 * Legal Knowledge Graph Import Controller
 *
 * <p>提供法律领域知识的批量导入接口，包括：
 * <ul>
 *   <li>批量导入法律节点（案件、当事人、法院、法官、法律条文等）</li>
 *   <li>批量导入法律边（案件-法条关系、当事人-律师关系等）</li>
 *   <li>一键导入完整法律知识图谱</li>
 *   <li>商事调解条例法条数据导入</li>
 * </ul>
 */
@Tag(name = "法律知识图谱导入", description = "法律领域知识图谱的批量导入接口")
@RestController
@RequestMapping("/api/v1/graph/legal")
@Validated
@Slf4j
public class LegalImportController {

    @Resource
    private LegalImportService legalImportService;

    /**
     * 批量导入法律图谱数据（节点+边）
     */
    @PostMapping("/import")
    @Operation(summary = "批量导入法律图谱", description = "一次性导入法律图谱的节点和边数据",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<LegalImportResultRespVO> importLegalKG(
            @RequestBody @Valid ImportLegalKGReqVO reqVO) {
        log.info("批量导入法律图谱: graphId={}, nodes={}, edges={}",
                reqVO.getGraphId(),
                reqVO.getNodes() != null ? reqVO.getNodes().size() : 0,
                reqVO.getEdges() != null ? reqVO.getEdges().size() : 0);
        LegalImportResultRespVO result = legalImportService.importLegalKG(reqVO);
        return CommonResult.success(result);
    }

    /**
     * 批量导入法律节点
     */
    @PostMapping("/nodes")
    @Operation(summary = "批量导入法律节点", description = "批量导入案件、当事人、法院、法官、法律条文等节点",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> importLegalNodes(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody @Valid List<Map<String, Object>> nodes) {
        log.info("批量导入法律节点: graphId={}, count={}", graphId, nodes.size());
        int successCount = legalImportService.importLegalNodes(graphId, nodes);
        return CommonResult.success(Map.of(
                "successCount", successCount,
                "totalCount", nodes.size()
        ));
    }

    /**
     * 批量导入法律边
     */
    @PostMapping("/edges")
    @Operation(summary = "批量导入法律边", description = "批量导入案件-法条关系、当事人-律师关系等边",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> importLegalEdges(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody @Valid List<Map<String, Object>> edges) {
        log.info("批量导入法律边: graphId={}, count={}", graphId, edges.size());
        int successCount = legalImportService.importLegalEdges(graphId, edges);
        return CommonResult.success(Map.of(
                "successCount", successCount,
                "totalCount", edges.size()
        ));
    }

    /**
     * 一键导入商事调解条例法条数据
     */
    @PostMapping("/provisions")
    @Operation(summary = "导入商事调解条例", description = "一键导入商事调解条例全文法条数据",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> importCommercialMediationProvisions(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId) {
        log.info("导入商事调解条例: graphId={}", graphId);
        int count = legalImportService.importCommercialMediationProvisions(graphId);
        return CommonResult.success(Map.of(
                "importedCount", count,
                "lawName", "商事调解条例"
        ));
    }

    /**
     * 批量导入预定义的法律案例数据
     */
    @PostMapping("/cases")
    @Operation(summary = "导入示例案例", description = "导入预定义的示例案件数据",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> importSampleCases(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId) {
        log.info("导入示例案例: graphId={}", graphId);
        int count = legalImportService.importSampleCases(graphId);
        return CommonResult.success(Map.of(
                "importedCount", count
        ));
    }

    /**
     * 导出法律图谱数据为 JSON
     */
    @GetMapping("/export")
    @Operation(summary = "导出法律图谱数据", description = "将法律图谱数据导出为 JSON 格式",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Map<String, Object>> exportLegalKG(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId) {
        log.info("导出法律图谱: graphId={}", graphId);
        Map<String, Object> data = legalImportService.exportLegalKG(graphId);
        return CommonResult.success(data);
    }
}
