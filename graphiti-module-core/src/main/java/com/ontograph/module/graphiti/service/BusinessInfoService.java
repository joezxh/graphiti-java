package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.business.*;
import java.util.List;

/**
 * 业务信息管理服务接口
 */
public interface BusinessInfoService {

    // --- Feature 1: 本体定义生成 ---
    GenerateOntologyRespVO generateOntology(String graphId, GenerateOntologyReqVO reqVO);

    void saveAsDraft(String graphId, GenerateOntologyReqVO reqVO);

    // --- Feature 2: 描述优化 ---
    OptimizeDescRespVO optimizeDescription(OptimizeDescReqVO reqVO);

    OptimizeDescRespVO optimizeBatch(OptimizeDescReqVO reqVO);

    // --- Feature 3: 数据模拟生成 ---
    GenerateDataRespVO generateMockData(String graphId, GenerateDataReqVO reqVO);

    GenerateDataRespVO generateFromDraft(String graphId, Long draftId, GenerateDataReqVO reqVO);
}
