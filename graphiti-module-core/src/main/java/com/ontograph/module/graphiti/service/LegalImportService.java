package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.legal.ImportLegalKGReqVO;
import com.ontograph.module.graphiti.vo.legal.LegalImportResultRespVO;

import java.util.List;
import java.util.Map;

/**
 * 法律知识图谱导入服务接口
 */
public interface LegalImportService {

    /**
     * 批量导入法律图谱数据（节点+边）
     */
    LegalImportResultRespVO importLegalKG(ImportLegalKGReqVO reqVO);

    /**
     * 批量导入法律节点
     * @return 成功导入数量
     */
    int importLegalNodes(String graphId, List<Map<String, Object>> nodes);

    /**
     * 批量导入法律边
     * @return 成功导入数量
     */
    int importLegalEdges(String graphId, List<Map<String, Object>> edges);

    /**
     * 一键导入商事调解条例全文（33条）
     * @return 导入法条数量
     */
    int importCommercialMediationProvisions(String graphId);

    /**
     * 导入示例案例数据
     * @return 导入案例数量
     */
    int importSampleCases(String graphId);

    /**
     * 导出法律图谱数据
     */
    Map<String, Object> exportLegalKG(String graphId);
}
