package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.dto.DomainInferenceResult;
import com.graphiti.module.graphiti.dto.SubTypeInferenceResult;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntCommunityTypeDO;

import java.util.List;

public interface DomainInferenceService {
    /**
     * 两阶段领域推断。
     * 第一阶段：推断顶层领域（DOMAIN_LEGAL/FINANCE/ENTERPRISE/MEDICAL/SOCIAL_GOV）
     * 第二阶段：在该领域内推断 subDomainType、region、scenarioType
     *
     * @param communityName 社区名称
     * @param memberContents 成员实体的内容摘要列表（用于上下文分析）
     * @param availableTypes 可用的 ont_community_type 配置
     * @return 推断结果（含置信度）
     */
    DomainInferenceResult infer(String communityName,
                                List<String> memberContents,
                                List<OntCommunityTypeDO> availableTypes);

    /**
     * 第一阶段：仅推断顶层领域
     */
    String inferTopLevelDomain(String communityName, List<String> memberContents);

    /**
     * 第二阶段：在已知领域内推断子类型
     *
     * @param topLevelDomain 顶层领域代码
     * @param communityName 社区名称
     * @param memberContents 成员内容摘要
     * @param availableTypes 可用的 ont_community_type 配置
     * @return 子类型推断结果
     */
    SubTypeInferenceResult inferSubTypes(String topLevelDomain,
                                         String communityName,
                                         List<String> memberContents,
                                         List<OntCommunityTypeDO> availableTypes);
}
