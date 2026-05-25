package com.ontograph.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.ontograph.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.ontograph.module.graphiti.service.EpisodeTypeInferenceService;
import com.ontograph.module.graphiti.vo.ontology.InferredTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeTypeInferenceServiceImpl implements EpisodeTypeInferenceService {

    private final OntClassMapper classMapper;
    private final OntDefinitionMapper definitionMapper;

    @Override
    public List<InferredTypeVO> inferEntityTypes(String graphId, String content, String domainHint) {
        if (content == null || content.isBlank()) return List.of();

        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();

        List<String> keywords = extractKeywords(content);
        List<OntClassDO> allClasses = classMapper.selectByDefinitionId(defId);
        List<MatchCandidate> candidates = new ArrayList<>();

        for (OntClassDO cls : allClasses) {
            if (domainHint != null && !domainHint.isBlank()
                    && !domainHint.equalsIgnoreCase(cls.getDomainHint())) {
                continue;
            }
            double score = calculateMatchScore(cls, keywords);
            if (score > 0.0) {
                candidates.add(new MatchCandidate(cls, score));
            }
        }

        candidates.sort((a, b) -> Double.compare(b.score, a.score));
        List<InferredTypeVO> results = new ArrayList<>();

        for (int i = 0; i < Math.min(5, candidates.size()); i++) {
            MatchCandidate mc = candidates.get(i);
            results.add(new InferredTypeVO(
                mc.cls.getLocalName(),
                mc.cls.getClassUri(),
                Math.round(mc.score * 100.0) / 100.0,
                buildReason(mc.cls, keywords)
            ));
        }
        return results;
    }

    @Override
    public Map<String, List<InferredTypeVO>> inferBatch(String graphId, List<String> episodeIds) {
        return Map.of();
    }

    private Long resolveDefinitionId(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        OntDefinitionDO def = definitionMapper.selectOne(w);
        return def != null ? def.getId() : null;
    }

    private List<String> extractKeywords(String content) {
        if (content == null) return List.of();
        String cleaned = content.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", " ");
        return Arrays.stream(cleaned.split("\\s+"))
            .filter(w -> w.length() > 2)
            .distinct()
            .limit(20)
            .collect(Collectors.toList());
    }

    private double calculateMatchScore(OntClassDO cls, List<String> keywords) {
        String localName = cls.getLocalName() != null ? cls.getLocalName().toLowerCase() : "";
        String description = cls.getDescription() != null ? cls.getDescription().toLowerCase() : "";

        double score = 0.0;
        for (String kw : keywords) {
            if (localName.contains(kw)) score += 0.5;
            if (description.contains(kw)) score += 0.2;
        }
        if (!keywords.isEmpty()) score = score / keywords.size();
        return Math.min(score, 1.0);
    }

    private String buildReason(OntClassDO cls, List<String> keywords) {
        List<String> matched = keywords.stream()
            .filter(kw -> cls.getLocalName().toLowerCase().contains(kw)
                || (cls.getDescription() != null && cls.getDescription().toLowerCase().contains(kw)))
            .limit(3)
            .collect(Collectors.toList());
        return "keyword match: " + String.join(", ", matched);
    }

    private record MatchCandidate(OntClassDO cls, double score) {}
}
