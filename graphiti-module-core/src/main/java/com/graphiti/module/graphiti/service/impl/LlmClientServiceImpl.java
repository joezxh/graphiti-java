package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.LlmClientService;
import com.graphiti.module.graphiti.vo.llm.ExtractedEntityVO;
import com.graphiti.module.graphiti.vo.llm.ExtractedRelationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LlmClientServiceImpl implements LlmClientService {

    @Override
    public List<ExtractedEntityVO> extractEntities(String text, List<String> entityTypes) {
        log.warn("LLM实体提取尚未完全实现");
        return List.of();
    }

    @Override
    public List<ExtractedRelationVO> extractRelations(String text, List<ExtractedEntityVO> entities) {
        log.warn("LLM关系提取尚未完全实现");
        return List.of();
    }

    @Override
    public String generateSummary(String content) {
        log.warn("LLM摘要生成尚未完全实现");
        return content.substring(0, Math.min(content.length(), 100));
    }

    @Override
    public String generateCommunitySummary(List<String> nodeSummaries) {
        log.warn("LLM社区摘要生成尚未完全实现");
        return String.join(", ", nodeSummaries).substring(0, Math.min(200, String.join(", ", nodeSummaries).length()));
    }
}
