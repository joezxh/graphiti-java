package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDraftDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDraftMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntPropertyMapper;
import com.graphiti.module.graphiti.service.OntologyMetadataService;
import com.graphiti.module.graphiti.vo.OntologyGraphVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyMetadataServiceImpl implements OntologyMetadataService {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntDraftMapper draftMapper;
    private final ObjectMapper objectMapper;

    private static final String COLOR_CLASS = "#00f0ff";
    private static final String COLOR_PROPERTY = "#bf5fff";
    private static final String COLOR_ENTITY = "#00ffcc";

    @Override
    public OntologyGraphVO getOntologyGraph(String graphId) {
        OntDefinitionDO definition = resolveDefinition(graphId);
        if (definition == null) {
            throw new BusinessException(1002, "本体未定义");
        }

        List<OntClassDO> classes = classMapper.selectByDefinitionId(definition.getId());
        List<OntPropertyDO> properties = propertyMapper.selectByDefinitionId(definition.getId());

        List<OntologyGraphVO.NodeVO> nodes = new ArrayList<>();
        List<OntologyGraphVO.EdgeVO> edges = new ArrayList<>();
        Set<String> relationTypes = new HashSet<>();
        Map<Long, String> classIdToUri = new HashMap<>();

        // 类节点
        for (OntClassDO cls : classes) {
            classIdToUri.put(cls.getId(), cls.getClassUri());
            nodes.add(OntologyGraphVO.NodeVO.builder()
                .id(cls.getClassUri())
                .label(cls.getLocalName())
                .type("CLASS")
                .category(cls.getDomainHint())
                .color(COLOR_CLASS)
                .description(cls.getDescription())
                .example(cls.getExample())
                .data(Map.of(
                    "localName", cls.getLocalName() != null ? cls.getLocalName() : "",
                    "example", cls.getExample() != null ? cls.getExample() : ""
                ))
                .build());
        }

        // 属性节点 + 边
        for (OntPropertyDO prop : properties) {
            nodes.add(OntologyGraphVO.NodeVO.builder()
                .id(prop.getPropertyUri())
                .label(prop.getLocalName())
                .type("PROPERTY")
                .color(COLOR_PROPERTY)
                .data(Map.of(
                    "localName", prop.getLocalName() != null ? prop.getLocalName() : "",
                    "propertyType", prop.getPropertyType() != null ? prop.getPropertyType() : "",
                    "rangeDataType", prop.getRangeDataType() != null ? prop.getRangeDataType() : "",
                    "isRequired", prop.getIsRequired() != null ? prop.getIsRequired() : false
                ))
                .build());

            String domainUri = classIdToUri.get(prop.getDomainClassId());
            String rangeUri = classIdToUri.get(prop.getRangeClassId());

            if (domainUri != null) {
                edges.add(OntologyGraphVO.EdgeVO.builder()
                    .id(prop.getId() + "-domain")
                    .source(domainUri)
                    .target(prop.getPropertyUri())
                    .label("定义域")
                    .type("HAS_PROPERTY")
                    .color("#00f0ff")
                    .build());

                if (rangeUri != null) {
                    edges.add(OntologyGraphVO.EdgeVO.builder()
                        .id(prop.getId() + "-range")
                        .source(prop.getPropertyUri())
                        .target(rangeUri)
                        .label("值域")
                        .type("HAS_RANGE")
                        .color("#00f0ff")
                        .build());
                }
            }
        }

        // 继承边
        for (OntClassDO cls : classes) {
            if (cls.getParentClassId() != null) {
                String parentUri = classIdToUri.get(cls.getParentClassId());
                if (parentUri != null) {
                    edges.add(OntologyGraphVO.EdgeVO.builder()
                        .id("inherit-" + cls.getId())
                        .source(cls.getClassUri())
                        .target(parentUri)
                        .label("继承")
                        .type("INHERITS")
                        .color(COLOR_PROPERTY)
                        .build());
                }
            }
        }

        // 关系类型统计
        Set<String> entityTypes = classes.stream()
            .map(OntClassDO::getDomainHint)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return OntologyGraphVO.builder()
            .nodes(nodes)
            .edges(edges)
            .meta(OntologyGraphVO.GraphMetaVO.builder()
                .nodeCount(nodes.size())
                .edgeCount(edges.size())
                .entityTypeCount(entityTypes.size())
                .relationTypeCount(relationTypes.size())
                .entityTypes(new ArrayList<>(entityTypes))
                .relationTypes(new ArrayList<>(relationTypes))
                .graphId(graphId)
                .ontologyName(definition.getName())
                .ontologyVersion(definition.getVersion())
                .build())
            .build();
    }

    @Override
    public OntologyGraphVO getMockDataGraph(String graphId, Long draftId) {
        OntDraftDO draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(2000, "草稿不存在");
        }
        if (draft.getMockData() == null || draft.getMockData().isBlank()) {
            throw new BusinessException(2007, "草稿中无模拟数据");
        }

        try {
            Map<String, Object> mockData = objectMapper.readValue(draft.getMockData(), Map.class);
            List<Map<String, Object>> entities = (List<Map<String, Object>>) mockData.getOrDefault("entities", List.of());
            List<Map<String, Object>> relationships = (List<Map<String, Object>>) mockData.getOrDefault("relationships", List.of());

            List<OntologyGraphVO.NodeVO> nodes = new ArrayList<>();
            List<OntologyGraphVO.EdgeVO> edges = new ArrayList<>();
            Set<String> entityTypes = new LinkedHashSet<>();
            Set<String> relationTypes = new LinkedHashSet<>();

            for (Map<String, Object> entity : entities) {
                String type = (String) entity.getOrDefault("type", "Unknown");
                entityTypes.add(type);
                nodes.add(OntologyGraphVO.NodeVO.builder()
                    .id((String) entity.getOrDefault("id", UUID.randomUUID().toString()))
                    .label((String) entity.getOrDefault("name", "Unknown"))
                    .type("ENTITY")
                    .category(type)
                    .color(COLOR_ENTITY)
                    .data(entity)
                    .build());
            }

            for (Map<String, Object> rel : relationships) {
                relationTypes.add((String) rel.getOrDefault("type", "UNKNOWN"));
                edges.add(OntologyGraphVO.EdgeVO.builder()
                    .id((String) rel.getOrDefault("id", UUID.randomUUID().toString()))
                    .source((String) rel.getOrDefault("source", ""))
                    .target((String) rel.getOrDefault("target", ""))
                    .label((String) rel.getOrDefault("type", ""))
                    .type("RELATES_TO")
                    .color("#ffe066")
                    .data(rel)
                    .build());
            }

            return OntologyGraphVO.builder()
                .nodes(nodes)
                .edges(edges)
                .meta(OntologyGraphVO.GraphMetaVO.builder()
                    .nodeCount(nodes.size())
                    .edgeCount(edges.size())
                    .entityTypeCount(entityTypes.size())
                    .relationTypeCount(relationTypes.size())
                    .entityTypes(new ArrayList<>(entityTypes))
                    .relationTypes(new ArrayList<>(relationTypes))
                    .graphId(graphId)
                    .ontologyName(draft.getDraftName())
                    .build())
                .build();
        } catch (Exception e) {
            log.error("解析模拟数据图失败: draftId={}", draftId, e);
            throw new BusinessException(2007, "解析模拟数据失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getGraphStats(String graphId) {
        OntologyGraphVO graph = getOntologyGraph(graphId);
        return Map.of(
            "nodeCount", graph.getMeta().getNodeCount(),
            "edgeCount", graph.getMeta().getEdgeCount(),
            "entityTypeCount", graph.getMeta().getEntityTypeCount(),
            "relationTypeCount", graph.getMeta().getRelationTypeCount(),
            "entityTypes", graph.getMeta().getEntityTypes(),
            "relationTypes", graph.getMeta().getRelationTypes()
        );
    }

    @Override
    public Map<String, Object> getMockDataStats(String graphId, Long draftId) {
        OntologyGraphVO graph = getMockDataGraph(graphId, draftId);
        return Map.of(
            "nodeCount", graph.getMeta().getNodeCount(),
            "edgeCount", graph.getMeta().getEdgeCount(),
            "entityTypeCount", graph.getMeta().getEntityTypeCount(),
            "relationTypeCount", graph.getMeta().getRelationTypeCount(),
            "entityTypes", graph.getMeta().getEntityTypes(),
            "relationTypes", graph.getMeta().getRelationTypes()
        );
    }

    private OntDefinitionDO resolveDefinition(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        return definitionMapper.selectOne(w);
    }
}
