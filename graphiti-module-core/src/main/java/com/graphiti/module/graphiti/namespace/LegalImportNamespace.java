package com.graphiti.module.graphiti.namespace;

import com.graphiti.module.graphiti.service.LegalImportService;
import com.graphiti.module.graphiti.vo.legal.ImportLegalKGReqVO;
import com.graphiti.module.graphiti.vo.legal.LegalImportResultRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 法律知识图谱导入命名空间
 * 对应 Python: graphiti.namespaces.legal
 *
 * <p>封装法律领域知识图谱的批量导入操作，包括：
 * <ul>
 *   <li>批量导入法律节点（案件、当事人、法院、法官、法律条文等）</li>
 *   <li>批量导入法律边（案件-法条关系、当事人-律师关系等）</li>
 *   <li>一键导入商事调解条例全文</li>
 *   <li>导入示例案例数据</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class LegalImportNamespace {

    private final LegalImportService legalImportService;

    /**
     * 批量导入法律图谱数据（节点+边）
     */
    public LegalImportResultRespVO importLegalKG(ImportLegalKGReqVO reqVO) {
        log.debug("LegalImportNamespace.importLegalKG: graphId={}", reqVO.getGraphId());
        return legalImportService.importLegalKG(reqVO);
    }

    /**
     * 批量导入法律节点
     * @return 成功导入数量
     */
    public int importLegalNodes(String graphId, List<Map<String, Object>> nodes) {
        log.debug("LegalImportNamespace.importLegalNodes: graphId={}, count={}", graphId, nodes.size());
        return legalImportService.importLegalNodes(graphId, nodes);
    }

    /**
     * 批量导入法律边
     * @return 成功导入数量
     */
    public int importLegalEdges(String graphId, List<Map<String, Object>> edges) {
        log.debug("LegalImportNamespace.importLegalEdges: graphId={}, count={}", graphId, edges.size());
        return legalImportService.importLegalEdges(graphId, edges);
    }

    /**
     * 一键导入商事调解条例全文（33条）
     * @return 导入法条数量
     */
    public int importCommercialMediationProvisions(String graphId) {
        log.info("LegalImportNamespace.importCommercialMediationProvisions: graphId={}", graphId);
        return legalImportService.importCommercialMediationProvisions(graphId);
    }

    /**
     * 导入示例案例数据
     * @return 导入案例数量
     */
    public int importSampleCases(String graphId) {
        log.info("LegalImportNamespace.importSampleCases: graphId={}", graphId);
        return legalImportService.importSampleCases(graphId);
    }

    /**
     * 导出法律图谱数据
     */
    public Map<String, Object> exportLegalKG(String graphId) {
        log.debug("LegalImportNamespace.exportLegalKG: graphId={}", graphId);
        return legalImportService.exportLegalKG(graphId);
    }
}
