package com.ontograph.module.graphiti.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.module.graphiti.dal.dataobject.metadata.OntCommunityTypeDO;
import com.ontograph.module.graphiti.dto.DomainInferenceResult;
import com.ontograph.module.graphiti.dto.SubTypeInferenceResult;
import com.ontograph.module.graphiti.service.DomainInferenceService;
import com.ontograph.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainInferenceServiceImpl implements DomainInferenceService {

    private final LlmClientService llmClientService;

    private static final List<String> TOP_LEVEL_DOMAINS = List.of(
        "DOMAIN_LEGAL", "DOMAIN_FINANCE", "DOMAIN_ENTERPRISE", "DOMAIN_MEDICAL", "DOMAIN_SOCIAL_GOV"
    );

    @Override
    public DomainInferenceResult infer(String communityName,
                                        List<String> memberContents,
                                        List<OntCommunityTypeDO> availableTypes) {
        String topLevelDomain = inferTopLevelDomain(communityName, memberContents);
        SubTypeInferenceResult subTypes = inferSubTypes(topLevelDomain, communityName, memberContents, availableTypes);

        return DomainInferenceResult.builder()
            .domainType(topLevelDomain)
            .subDomainType(subTypes.getDomainType())
            .region(subTypes.getRegion() != null ? subTypes.getRegion() : "REGION_CN")
            .scenarioType(subTypes.getScenarioType())
            .confidence(subTypes.getConfidence())
            .reasoning(subTypes.getReasoning())
            .userOverridden(false)
            .build();
    }

    @Override
    public String inferTopLevelDomain(String communityName, List<String> memberContents) {
        if (communityName == null && (memberContents == null || memberContents.isEmpty())) {
            return "DOMAIN_ROOT";
        }

        String prompt = buildTopLevelDomainPrompt(communityName, memberContents);
        String llmResponse = llmClientService.chat(prompt);

        for (String domain : TOP_LEVEL_DOMAINS) {
            if (llmResponse.contains(domain)) {
                return domain;
            }
        }
        return "DOMAIN_ROOT";  // 兜底
    }

    @Override
    public SubTypeInferenceResult inferSubTypes(String topLevelDomain,
                                                 String communityName,
                                                 List<String> memberContents,
                                                 List<OntCommunityTypeDO> availableTypes) {
        if (topLevelDomain == null || "DOMAIN_ROOT".equals(topLevelDomain)) {
            return SubTypeInferenceResult.builder()
                .region("REGION_ROOT")
                .scenarioType("SCENARIO_ROOT")
                .confidence(0.3)
                .reasoning("顶层领域为 ROOT，返回通用默认值")
                .build();
        }

        String prompt = buildSubTypePrompt(topLevelDomain, communityName, memberContents, availableTypes);
        String llmResponse = llmClientService.chat(prompt);

        return parseSubTypeResult(llmResponse, topLevelDomain);
    }

    private String buildTopLevelDomainPrompt(String communityName, List<String> memberContents) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个领域分类专家。请根据以下社区信息，判断该社区属于哪个领域。\n\n");
        sb.append("已知顶层领域：\n");
        sb.append("- DOMAIN_LEGAL（法律）：处理法律纠纷、诉讼、合规、判决等\n");
        sb.append("- DOMAIN_FINANCE（金融）：处理银行、证券、保险、风险、信贷等\n");
        sb.append("- DOMAIN_ENTERPRISE（企业管理）：处理人力资源、财务、合规、治理等\n");
        sb.append("- DOMAIN_MEDICAL（医疗）：处理诊疗、药品、公共卫生等\n");
        sb.append("- DOMAIN_SOCIAL_GOV（社会治理）：处理社会综合治理事件，包括婚恋家庭纠纷、劳动人事争议纠纷、侵权责任纠纷、邻里关系纠纷、房屋物业纠纷、山林土地水利纠纷、消费服务纠纷、经济金融活动纠纷、行政纠纷与信访维权、咨询与公证服务等\n\n");
        sb.append("社区名称：").append(communityName != null ? communityName : "无").append("\n");
        sb.append("社区成员内容摘要：\n");
        if (memberContents != null && !memberContents.isEmpty()) {
            for (int i = 0; i < Math.min(memberContents.size(), 5); i++) {
                sb.append("- ").append(memberContents.get(i)).append("\n");
            }
        } else {
            sb.append("无\n");
        }
        sb.append("\n请从上述五个领域中选择最匹配的一个，返回领域代码（如 DOMAIN_LEGAL）。只返回一个代码，不需要解释。");
        return sb.toString();
    }

    private String buildSubTypePrompt(String topLevelDomain,
                                      String communityName,
                                      List<String> memberContents,
                                      List<OntCommunityTypeDO> availableTypes) {
        StringBuilder sb = new StringBuilder();
        sb.append("已知该社区属于领域 ").append(topLevelDomain).append("。请推断其具体子类型。\n\n");

        // 将 availableTypes 按 parent 分类，过滤出该 topLevelDomain 的子类型
        List<OntCommunityTypeDO> subTypes = availableTypes.stream()
            .filter(t -> topLevelDomain.equals(t.getParentTypeCode()))
            .collect(Collectors.toList());

        sb.append("该领域下的子类型（type_code | type_name | category）：\n");
        for (OntCommunityTypeDO t : subTypes) {
            sb.append("- ").append(t.getTypeCode()).append(" | ").append(t.getTypeName())
              .append(" | ").append(t.getCategory()).append("\n");
        }

        sb.append("\n同时请从以下区域中选择最匹配的（默认 REGION_CN）：\n");
        sb.append("- REGION_ROOT（通用）\n- REGION_CN（中国）\n- REGION_US（美国）\n- REGION_EU（欧洲）\n");
        sb.append("\n请从以下场景中选择最匹配的（默认 SCENARIO_ROOT）：\n");
        sb.append("- SCENARIO_ROOT（通用场景）\n- SCENARIO_LAW_REGULATE（依法调解）\n- SCENARIO_FEEDBACK（反馈处置）\n- SCENARIO_GOVERNANCE（综合治理）\n- SCENARIO_PREVENTION（预防预警）\n");
        sb.append("- SCENARIO_JUDICIAL（司法实践）\n- SCENARIO_COMPLIANCE（合规管理）\n- SCENARIO_RISK（风险管控）\n\n");

        sb.append("社区名称：").append(communityName != null ? communityName : "无").append("\n");
        if (memberContents != null && !memberContents.isEmpty()) {
            sb.append("成员内容摘要：\n");
            for (int i = 0; i < Math.min(memberContents.size(), 3); i++) {
                sb.append("- ").append(memberContents.get(i)).append("\n");
            }
        }

        sb.append("\n请输出 JSON，格式：{\"domainType\":\"子领域代码\",\"region\":\"区域代码\",\"scenarioType\":\"场景代码\",\"confidence\":0.0~1.0,\"reasoning\":\"推断理由\"}");
        return sb.toString();
    }

    private SubTypeInferenceResult parseSubTypeResult(String llmResponse, String topLevelDomain) {
        try {
            int jsonStart = llmResponse.indexOf('{');
            int jsonEnd = llmResponse.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String jsonStr = llmResponse.substring(jsonStart, jsonEnd + 1);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(jsonStr);
                return SubTypeInferenceResult.builder()
                    .domainType(node.has("domainType") && !node.get("domainType").isNull() ? node.get("domainType").asText() : null)
                    .region(node.has("region") && !node.get("region").isNull() ? node.get("region").asText() : "REGION_CN")
                    .scenarioType(node.has("scenarioType") && !node.get("scenarioType").isNull() ? node.get("scenarioType").asText() : "SCENARIO_ROOT")
                    .confidence(node.has("confidence") && !node.get("confidence").isNull() ? node.get("confidence").asDouble() : 0.5)
                    .reasoning(node.has("reasoning") && !node.get("reasoning").isNull() ? node.get("reasoning").asText() : "")
                    .build();
            }
        } catch (Exception e) {
            log.warn("LLM 响应解析失败: {}", e.getMessage());
        }
        // 解析失败时返回默认值
        return SubTypeInferenceResult.builder()
            .region("REGION_CN")
            .scenarioType("SCENARIO_ROOT")
            .confidence(0.3)
            .reasoning("LLM 解析失败，使用默认值")
            .build();
    }
}
