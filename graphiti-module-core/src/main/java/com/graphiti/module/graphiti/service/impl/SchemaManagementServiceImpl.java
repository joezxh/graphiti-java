package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.dal.dataobject.metadata.OntEpisodeTypeDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassInheritanceDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntEpisodeTypeMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassInheritanceMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntPropertyMapper;
import com.graphiti.module.graphiti.service.SchemaManagementService;
import com.graphiti.module.graphiti.vo.ide.SchemaChangeValidateReqVO;
import com.graphiti.module.graphiti.vo.ide.SchemaChangeValidateRespVO;
import com.graphiti.module.graphiti.vo.ide.SchemaClassRespVO;
import com.graphiti.module.graphiti.vo.ide.SchemaPropertyRespVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Schema 管理服务实现类
 *
 * <p>V3.1.0: getEpisodeTypes / getEpisodeHierarchy 方法实现 Neo4j 图数据库与
 * ont_episode_type 元数据表双写读取。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaManagementServiceImpl implements SchemaManagementService {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntClassInheritanceMapper inheritanceMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntEpisodeTypeMapper episodeTypeMapper;
    private final Driver neo4jDriver;

    // ==================== 类相关方法 ====================

    @Override
    public List<SchemaClassRespVO> getClasses(String graphId) {
        OntDefinitionDO definition = getOrCreateDefinition(graphId);

        List<OntClassDO> classes = classMapper.selectList(
                new LambdaQueryWrapper<OntClassDO>()
                        .eq(OntClassDO::getDefinitionId, definition.getId())
                        .orderByAsc(OntClassDO::getLocalName)
        );

        List<Long> classIds = classes.stream().map(OntClassDO::getId).collect(Collectors.toList());

        List<OntClassInheritanceDO> inheritances = classIds.isEmpty() ? Collections.emptyList()
                : inheritanceMapper.selectList(
                        new LambdaQueryWrapper<OntClassInheritanceDO>()
                                .in(OntClassInheritanceDO::getClassId, classIds)
        );

        List<OntPropertyDO> properties = propertyMapper.selectList(
                new LambdaQueryWrapper<OntPropertyDO>()
                        .eq(OntPropertyDO::getDefinitionId, definition.getId())
        );
        Map<Long, Long> propertyCountMap = properties.stream()
                .collect(Collectors.groupingBy(OntPropertyDO::getDomainClassId, Collectors.counting()));

        return classes.stream().map(cls -> {
            SchemaClassRespVO vo = new SchemaClassRespVO();
            vo.setId(cls.getId());
            vo.setDefinitionId(cls.getDefinitionId());
            vo.setClassUri(cls.getClassUri());
            vo.setLocalName(cls.getLocalName());
            vo.setDescription(cls.getDescription());
            vo.setPropertyCount(propertyCountMap.getOrDefault(cls.getId(), 0L).intValue());

            List<Long> parentIds = inheritances.stream()
                    .filter(inh -> inh.getClassId().equals(cls.getId()))
                    .map(OntClassInheritanceDO::getParentClassId)
                    .collect(Collectors.toList());
            vo.setParentClassIds(parentIds);

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public SchemaClassRespVO getClassDetail(String graphId, Long classId) {
        OntClassDO cls = classMapper.selectById(classId);
        if (cls == null) {
            throw new RuntimeException("Class not found: " + classId);
        }

        SchemaClassRespVO vo = new SchemaClassRespVO();
        vo.setId(cls.getId());
        vo.setDefinitionId(cls.getDefinitionId());
        vo.setClassUri(cls.getClassUri());
        vo.setLocalName(cls.getLocalName());
        vo.setDescription(cls.getDescription());

        List<OntClassInheritanceDO> inheritances = inheritanceMapper.selectList(
                new LambdaQueryWrapper<OntClassInheritanceDO>()
                        .eq(OntClassInheritanceDO::getClassId, classId)
        );
        vo.setParentClassIds(inheritances.stream()
                .map(OntClassInheritanceDO::getParentClassId)
                .collect(Collectors.toList()));

        Long propertyCount = propertyMapper.selectCount(
                new LambdaQueryWrapper<OntPropertyDO>()
                        .eq(OntPropertyDO::getDomainClassId, classId)
        );
        vo.setPropertyCount(propertyCount.intValue());

        return vo;
    }

    @Override
    @Transactional
    public SchemaClassRespVO createClass(String graphId, Map<String, Object> classData) {
        OntDefinitionDO definition = getOrCreateDefinition(graphId);

        String localName = (String) classData.get("localName");
        String description = (String) classData.get("description");
        @SuppressWarnings("unchecked")
        List<Integer> parentClassIds = (List<Integer>) classData.getOrDefault("parentClassIds", new ArrayList<>());

        OntClassDO existing = classMapper.selectOne(
                new LambdaQueryWrapper<OntClassDO>()
                        .eq(OntClassDO::getDefinitionId, definition.getId())
                        .eq(OntClassDO::getLocalName, localName)
        );
        if (existing != null) {
            throw new RuntimeException("Class already exists: " + localName);
        }

        OntClassDO cls = new OntClassDO();
        cls.setDefinitionId(definition.getId());
        cls.setClassUri(definition.getNamespace() + "/" + localName);
        cls.setLocalName(localName);
        cls.setDescription(description);
        classMapper.insert(cls);

        for (Integer parentId : parentClassIds) {
            if (parentId != null && parentId > 0) {
                OntClassInheritanceDO inheritance = new OntClassInheritanceDO();
                inheritance.setClassId(cls.getId());
                inheritance.setParentClassId(parentId.longValue());
                inheritanceMapper.insert(inheritance);
            }
        }

        SchemaClassRespVO vo = new SchemaClassRespVO();
        vo.setId(cls.getId());
        vo.setDefinitionId(cls.getDefinitionId());
        vo.setClassUri(cls.getClassUri());
        vo.setLocalName(cls.getLocalName());
        vo.setDescription(cls.getDescription());
        vo.setParentClassIds(parentClassIds.stream().map(Integer::longValue).collect(Collectors.toList()));
        vo.setPropertyCount(0);

        return vo;
    }

    @Override
    @Transactional
    public SchemaClassRespVO updateClass(String graphId, Long classId, Map<String, Object> classData) {
        OntClassDO cls = classMapper.selectById(classId);
        if (cls == null) {
            throw new RuntimeException("Class not found: " + classId);
        }

        if (classData.containsKey("localName")) {
            cls.setLocalName((String) classData.get("localName"));
        }
        if (classData.containsKey("description")) {
            cls.setDescription((String) classData.get("description"));
        }

        classMapper.updateById(cls);

        if (classData.containsKey("parentClassIds")) {
            @SuppressWarnings("unchecked")
            List<Integer> parentClassIds = (List<Integer>) classData.get("parentClassIds");

            inheritanceMapper.delete(
                    new LambdaQueryWrapper<OntClassInheritanceDO>()
                            .eq(OntClassInheritanceDO::getClassId, classId)
            );

            for (Integer parentId : parentClassIds) {
                if (parentId != null && parentId > 0) {
                    OntClassInheritanceDO inheritance = new OntClassInheritanceDO();
                    inheritance.setClassId(cls.getId());
                    inheritance.setParentClassId(parentId.longValue());
                    inheritanceMapper.insert(inheritance);
                }
            }
        }

        return getClassDetail(graphId, classId);
    }

    @Override
    @Transactional
    public void deleteClass(String graphId, Long classId) {
        inheritanceMapper.delete(
                new LambdaQueryWrapper<OntClassInheritanceDO>()
                        .eq(OntClassInheritanceDO::getClassId, classId)
                        .or()
                        .eq(OntClassInheritanceDO::getParentClassId, classId)
        );

        propertyMapper.delete(
                new LambdaQueryWrapper<OntPropertyDO>()
                        .eq(OntPropertyDO::getDomainClassId, classId)
        );

        classMapper.deleteById(classId);
    }

    // ==================== 属性相关方法 ====================

    @Override
    public List<SchemaPropertyRespVO> getClassProperties(String graphId, Long classId) {
        List<OntPropertyDO> properties = propertyMapper.selectList(
                new LambdaQueryWrapper<OntPropertyDO>()
                        .eq(OntPropertyDO::getDomainClassId, classId)
                        .orderByAsc(OntPropertyDO::getLocalName)
        );

        return properties.stream().map(this::convertToPropertyVO).collect(Collectors.toList());
    }

    @Override
    public SchemaPropertyRespVO createProperty(String graphId, Long classId, Map<String, Object> propertyData) {
        OntClassDO cls = classMapper.selectById(classId);
        if (cls == null) {
            throw new RuntimeException("Class not found: " + classId);
        }

        OntDefinitionDO definition = definitionMapper.selectById(cls.getDefinitionId());

        OntPropertyDO property = new OntPropertyDO();
        property.setDefinitionId(definition.getId());
        property.setDomainClassId(classId);
        property.setLocalName((String) propertyData.get("localName"));
        property.setPropertyType((String) propertyData.getOrDefault("propertyType", "DATATYPE"));
        property.setRangeDataType((String) propertyData.getOrDefault("rangeDataType", "string"));

        if (propertyData.containsKey("isRequired")) {
            property.setIsRequired((Boolean) propertyData.get("isRequired"));
        }
        if (propertyData.containsKey("defaultValue")) {
            property.setDefaultValue((String) propertyData.get("defaultValue"));
        }
        if (propertyData.containsKey("pattern")) {
            property.setPattern((String) propertyData.get("pattern"));
        }
        if (propertyData.containsKey("minValue")) {
            property.setMinValue(new java.math.BigDecimal(propertyData.get("minValue").toString()));
        }
        if (propertyData.containsKey("maxValue")) {
            property.setMaxValue(new java.math.BigDecimal(propertyData.get("maxValue").toString()));
        }

        propertyMapper.insert(property);

        return convertToPropertyVO(property);
    }

    @Override
    public SchemaPropertyRespVO updateProperty(String graphId, Long classId, Long propertyId, Map<String, Object> propertyData) {
        OntPropertyDO property = propertyMapper.selectById(propertyId);
        if (property == null) {
            throw new RuntimeException("Property not found: " + propertyId);
        }

        if (propertyData.containsKey("localName")) {
            property.setLocalName((String) propertyData.get("localName"));
        }
        if (propertyData.containsKey("propertyType")) {
            property.setPropertyType((String) propertyData.get("propertyType"));
        }
        if (propertyData.containsKey("rangeDataType")) {
            property.setRangeDataType((String) propertyData.get("rangeDataType"));
        }
        if (propertyData.containsKey("isRequired")) {
            property.setIsRequired((Boolean) propertyData.get("isRequired"));
        }
        if (propertyData.containsKey("defaultValue")) {
            property.setDefaultValue((String) propertyData.get("defaultValue"));
        }
        if (propertyData.containsKey("pattern")) {
            property.setPattern((String) propertyData.get("pattern"));
        }
        if (propertyData.containsKey("minValue")) {
            property.setMinValue(propertyData.get("minValue") != null
                    ? new java.math.BigDecimal(propertyData.get("minValue").toString()) : null);
        }
        if (propertyData.containsKey("maxValue")) {
            property.setMaxValue(propertyData.get("maxValue") != null
                    ? new java.math.BigDecimal(propertyData.get("maxValue").toString()) : null);
        }

        propertyMapper.updateById(property);

        return convertToPropertyVO(property);
    }

    @Override
    public void deleteProperty(String graphId, Long classId, Long propertyId) {
        propertyMapper.deleteById(propertyId);
    }

    // ==================== Schema 变更验证 ====================

    @Override
    public SchemaChangeValidateRespVO validateSchemaChange(String graphId, SchemaChangeValidateReqVO request) {
        String changeType = request.getType();
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            switch (changeType) {
                case "UPDATE_CLASS":
                    violations = validateClassUpdate(session, graphId, request);
                    break;
                case "UPDATE_PROPERTY":
                    violations = validatePropertyUpdate(session, graphId, request);
                    break;
                case "DELETE_PROPERTY":
                    violations = validatePropertyDelete(session, graphId, request);
                    break;
                case "ADD_REQUIRED_PROPERTY":
                    violations = validateAddRequiredProperty(session, graphId, request);
                    break;
                default:
                    break;
            }
        }

        if (violations.isEmpty()) {
            return SchemaChangeValidateRespVO.compatible();
        } else {
            return SchemaChangeValidateRespVO.incompatible(violations.size(), violations);
        }
    }

    private List<SchemaChangeValidateRespVO.Violation> validateClassUpdate(
            Session session, String graphId, SchemaChangeValidateReqVO request) {
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        if (request.getClassId() == null || request.getChanges() == null) {
            return violations;
        }

        OntClassDO cls = classMapper.selectById(request.getClassId());
        if (cls == null) {
            return violations;
        }

        String cypher = "MATCH (n:Entity) WHERE n.graph_id = $graphId AND n.type = $className " +
                        "AND n.invalid_at IS NULL RETURN count(n) as cnt LIMIT 1";
        Result result = session.run(cypher,
                org.neo4j.driver.Values.parameters(
                        "graphId", graphId,
                        "className", cls.getLocalName()
                ));

        if (result.hasNext()) {
            long count = result.next().get("cnt").asLong();
            if (count > 0 && request.getChanges().getNewLocalName() != null) {
                violations.add(SchemaChangeValidateRespVO.Violation.builder()
                        .violationType("CLASS_RENAME")
                        .reason("类名变更将影响 " + count + " 个节点的 type 属性")
                        .expectedValue(request.getChanges().getNewLocalName())
                        .currentValue(cls.getLocalName())
                        .build());
            }
        }

        return violations;
    }

    private List<SchemaChangeValidateRespVO.Violation> validatePropertyUpdate(
            Session session, String graphId, SchemaChangeValidateReqVO request) {
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        if (request.getPropertyId() == null || request.getChanges() == null) {
            return violations;
        }

        OntPropertyDO property = propertyMapper.selectById(request.getPropertyId());
        if (property == null) {
            return violations;
        }

        OntClassDO cls = classMapper.selectById(property.getDomainClassId());
        if (cls == null) {
            return violations;
        }

        String propName = property.getLocalName();
        var changes = request.getChanges();

        if (changes.getNewIsRequired() != null && changes.getNewIsRequired()
                && (changes.getOldIsRequired() == null || !changes.getOldIsRequired())) {
            String cypher = "MATCH (n:Entity) WHERE n.graph_id = $graphId AND n.type = $className " +
                            "AND n.invalid_at IS NULL AND (NOT exists(n." + propName + ") OR n." + propName + " IS NULL) " +
                            "RETURN n.uuid as uuid, n.type as type, n." + propName + " as value, " +
                            "n.courtName as courtName, n.partyName as partyName, " +
                            "n.caseName as caseName, n.caseNumber as caseNumber, " +
                            "n.articleNumber as articleNumber, n.lawName as lawName, " +
                            "n.judgeName as judgeName, n.documentNumber as documentNumber, " +
                            "n.agreementNumber as agreementNumber, n.evidenceNumber as evidenceNumber, " +
                            "n.reasoning as reasoning, n.factDescription as factDescription, " +
                            "n.name as genericName, n.summary as summary " +
                            "LIMIT 100";

            Result result = session.run(cypher,
                    org.neo4j.driver.Values.parameters(
                            "graphId", graphId,
                            "className", cls.getLocalName()
                    ));

            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("courtName", record.get("courtName").isNull() ? null : record.get("courtName").asString());
                nodeData.put("partyName", record.get("partyName").isNull() ? null : record.get("partyName").asString());
                nodeData.put("caseName", record.get("caseName").isNull() ? null : record.get("caseName").asString());
                nodeData.put("caseNumber", record.get("caseNumber").isNull() ? null : record.get("caseNumber").asString());
                nodeData.put("articleNumber", record.get("articleNumber").isNull() ? null : record.get("articleNumber").asString());
                nodeData.put("lawName", record.get("lawName").isNull() ? null : record.get("lawName").asString());
                nodeData.put("judgeName", record.get("judgeName").isNull() ? null : record.get("judgeName").asString());
                nodeData.put("documentNumber", record.get("documentNumber").isNull() ? null : record.get("documentNumber").asString());
                nodeData.put("agreementNumber", record.get("agreementNumber").isNull() ? null : record.get("agreementNumber").asString());
                nodeData.put("evidenceNumber", record.get("evidenceNumber").isNull() ? null : record.get("evidenceNumber").asString());
                nodeData.put("reasoning", record.get("reasoning").isNull() ? null : record.get("reasoning").asString());
                nodeData.put("factDescription", record.get("factDescription").isNull() ? null : record.get("factDescription").asString());
                nodeData.put("name", record.get("genericName").isNull() ? null : record.get("genericName").asString());
                nodeData.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());

                violations.add(SchemaChangeValidateRespVO.Violation.builder()
                        .nodeUuid(record.get("uuid").asString())
                        .nodeName(extractNodeName(record.get("type").asString(), nodeData))
                        .violationType("MISSING_REQUIRED")
                        .reason("节点缺少必填属性 '" + propName + "'")
                        .currentValue(null)
                        .expectedValue("非空值")
                        .build());
            }
        }

        if (changes.getNewPattern() != null && !changes.getNewPattern().equals(property.getPattern())) {
            String cypher = "MATCH (n:Entity) WHERE n.graph_id = $graphId AND n.type = $className " +
                            "AND n.invalid_at IS NULL AND exists(n." + propName + ") AND n." + propName + " IS NOT NULL " +
                            "RETURN n.uuid as uuid, n.type as type, n." + propName + " as value, " +
                            "n.courtName as courtName, n.partyName as partyName, " +
                            "n.caseName as caseName, n.caseNumber as caseNumber, " +
                            "n.articleNumber as articleNumber, n.lawName as lawName, " +
                            "n.judgeName as judgeName, n.documentNumber as documentNumber, " +
                            "n.agreementNumber as agreementNumber, n.evidenceNumber as evidenceNumber, " +
                            "n.reasoning as reasoning, n.factDescription as factDescription, " +
                            "n.name as genericName, n.summary as summary " +
                            "LIMIT 100";

            Result result = session.run(cypher,
                    org.neo4j.driver.Values.parameters(
                            "graphId", graphId,
                            "className", cls.getLocalName()
                    ));

            java.util.regex.Pattern newPattern;
            try {
                newPattern = java.util.regex.Pattern.compile(changes.getNewPattern());
            } catch (java.util.regex.PatternSyntaxException e) {
                return violations;
            }

            while (result.hasNext()) {
                Record record = result.next();
                String value = record.get("value").asString();
                if (value != null && !newPattern.matcher(value).matches()) {
                    Map<String, Object> nodeData = new HashMap<>();
                    nodeData.put("courtName", record.get("courtName").isNull() ? null : record.get("courtName").asString());
                    nodeData.put("partyName", record.get("partyName").isNull() ? null : record.get("partyName").asString());
                    nodeData.put("caseName", record.get("caseName").isNull() ? null : record.get("caseName").asString());
                    nodeData.put("caseNumber", record.get("caseNumber").isNull() ? null : record.get("caseNumber").asString());
                    nodeData.put("articleNumber", record.get("articleNumber").isNull() ? null : record.get("articleNumber").asString());
                    nodeData.put("lawName", record.get("lawName").isNull() ? null : record.get("lawName").asString());
                    nodeData.put("judgeName", record.get("judgeName").isNull() ? null : record.get("judgeName").asString());
                    nodeData.put("documentNumber", record.get("documentNumber").isNull() ? null : record.get("documentNumber").asString());
                    nodeData.put("agreementNumber", record.get("agreementNumber").isNull() ? null : record.get("agreementNumber").asString());
                    nodeData.put("evidenceNumber", record.get("evidenceNumber").isNull() ? null : record.get("evidenceNumber").asString());
                    nodeData.put("reasoning", record.get("reasoning").isNull() ? null : record.get("reasoning").asString());
                    nodeData.put("factDescription", record.get("factDescription").isNull() ? null : record.get("factDescription").asString());
                    nodeData.put("name", record.get("genericName").isNull() ? null : record.get("genericName").asString());
                    nodeData.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());

                    violations.add(SchemaChangeValidateRespVO.Violation.builder()
                            .nodeUuid(record.get("uuid").asString())
                            .nodeName(extractNodeName(record.get("type").asString(), nodeData))
                            .violationType("PATTERN_VIOLATION")
                            .reason("节点属性值不满足新正则表达式: " + changes.getNewPattern())
                            .currentValue(value)
                            .expectedValue("匹配模式: " + changes.getNewPattern())
                            .build());
                }
            }
        }

        return violations;
    }

    private List<SchemaChangeValidateRespVO.Violation> validatePropertyDelete(
            Session session, String graphId, SchemaChangeValidateReqVO request) {
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        if (request.getPropertyId() == null) {
            return violations;
        }

        OntPropertyDO property = propertyMapper.selectById(request.getPropertyId());
        if (property == null) {
            return violations;
        }

        OntClassDO cls = classMapper.selectById(property.getDomainClassId());
        if (cls == null) {
            return violations;
        }

        String cypher = "MATCH (n:Entity) WHERE n.graph_id = $graphId AND n.type = $className " +
                        "AND n.invalid_at IS NULL AND exists(n." + property.getLocalName() + ") " +
                        "RETURN count(n) as cnt LIMIT 1";

        Result result = session.run(cypher,
                org.neo4j.driver.Values.parameters(
                        "graphId", graphId,
                        "className", cls.getLocalName()
                ));

        if (result.hasNext()) {
            long count = result.next().get("cnt").asLong();
            if (count > 0) {
                violations.add(SchemaChangeValidateRespVO.Violation.builder()
                        .violationType("PROPERTY_DELETE")
                        .reason("删除属性将丢失 " + count + " 个节点的属性数据")
                        .currentValue("属性: " + property.getLocalName())
                        .expectedValue("将被删除")
                        .build());
            }
        }

        return violations;
    }

    private List<SchemaChangeValidateRespVO.Violation> validateAddRequiredProperty(
            Session session, String graphId, SchemaChangeValidateReqVO request) {
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        if (request.getClassId() == null || request.getPropertyId() == null) {
            return violations;
        }

        OntClassDO cls = classMapper.selectById(request.getClassId());
        if (cls == null) {
            return violations;
        }

        OntPropertyDO property = propertyMapper.selectById(request.getPropertyId());
        if (property == null) {
            return violations;
        }

        String cypher = "MATCH (n:Entity) WHERE n.graph_id = $graphId AND n.type = $className " +
                        "AND n.invalid_at IS NULL AND (NOT exists(n." + property.getLocalName() + ") OR n." + property.getLocalName() + " IS NULL) " +
                        "RETURN n.uuid as uuid, n.type as type, " +
                        "n.courtName as courtName, n.partyName as partyName, " +
                        "n.caseName as caseName, n.caseNumber as caseNumber, " +
                        "n.articleNumber as articleNumber, n.lawName as lawName, " +
                        "n.judgeName as judgeName, n.documentNumber as documentNumber, " +
                        "n.agreementNumber as agreementNumber, n.evidenceNumber as evidenceNumber, " +
                        "n.reasoning as reasoning, n.factDescription as factDescription, " +
                        "n.name as genericName, n.summary as summary " +
                        "LIMIT 100";

        Result result = session.run(cypher,
                org.neo4j.driver.Values.parameters(
                        "graphId", graphId,
                        "className", cls.getLocalName()
                ));

        while (result.hasNext()) {
            Record record = result.next();
            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("courtName", record.get("courtName").isNull() ? null : record.get("courtName").asString());
            nodeData.put("partyName", record.get("partyName").isNull() ? null : record.get("partyName").asString());
            nodeData.put("caseName", record.get("caseName").isNull() ? null : record.get("caseName").asString());
            nodeData.put("caseNumber", record.get("caseNumber").isNull() ? null : record.get("caseNumber").asString());
            nodeData.put("articleNumber", record.get("articleNumber").isNull() ? null : record.get("articleNumber").asString());
            nodeData.put("lawName", record.get("lawName").isNull() ? null : record.get("lawName").asString());
            nodeData.put("judgeName", record.get("judgeName").isNull() ? null : record.get("judgeName").asString());
            nodeData.put("documentNumber", record.get("documentNumber").isNull() ? null : record.get("documentNumber").asString());
            nodeData.put("agreementNumber", record.get("agreementNumber").isNull() ? null : record.get("agreementNumber").asString());
            nodeData.put("evidenceNumber", record.get("evidenceNumber").isNull() ? null : record.get("evidenceNumber").asString());
            nodeData.put("reasoning", record.get("reasoning").isNull() ? null : record.get("reasoning").asString());
            nodeData.put("factDescription", record.get("factDescription").isNull() ? null : record.get("factDescription").asString());
            nodeData.put("name", record.get("genericName").isNull() ? null : record.get("genericName").asString());
            nodeData.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());

            violations.add(SchemaChangeValidateRespVO.Violation.builder()
                    .nodeUuid(record.get("uuid").asString())
                    .nodeName(extractNodeName(record.get("type").asString(), nodeData))
                    .violationType("MISSING_REQUIRED")
                    .reason("新必填属性 '" + property.getLocalName() + "' 将在这些节点上缺失")
                    .currentValue(null)
                    .expectedValue("需要提供值")
                    .build());
        }

        return violations;
    }

    // ==================== 类实例 ====================

    @Override
    public Map<String, Object> getClassInstances(String graphId, String classType, Integer page, Integer pageSize, String keyword) {
        int skip = (page - 1) * pageSize;

        try (Session session = neo4jDriver.session()) {
            StringBuilder cypherBuilder = new StringBuilder();
            cypherBuilder.append("MATCH (n:Entity {graph_id: $graphId, type: $classType}) ");

            Map<String, Object> params = new HashMap<>();
            params.put("graphId", graphId);
            params.put("classType", classType);

            if (keyword != null && !keyword.trim().isEmpty()) {
                cypherBuilder.append("AND (");
                cypherBuilder.append("n.courtName CONTAINS $keyword OR ");
                cypherBuilder.append("n.partyName CONTAINS $keyword OR ");
                cypherBuilder.append("n.caseName CONTAINS $keyword OR ");
                cypherBuilder.append("n.caseNumber CONTAINS $keyword OR ");
                cypherBuilder.append("n.lawName CONTAINS $keyword OR ");
                cypherBuilder.append("n.articleNumber CONTAINS $keyword OR ");
                cypherBuilder.append("n.judgeName CONTAINS $keyword OR ");
                cypherBuilder.append("n.documentNumber CONTAINS $keyword OR ");
                cypherBuilder.append("n.agreementNumber CONTAINS $keyword OR ");
                cypherBuilder.append("n.evidenceNumber CONTAINS $keyword OR ");
                cypherBuilder.append("n.name CONTAINS $keyword OR ");
                cypherBuilder.append("n.summary CONTAINS $keyword");
                cypherBuilder.append(") ");
                params.put("keyword", keyword.trim());
            }

            String countCypher = cypherBuilder.toString() + "RETURN count(n) as total";
            Result countResult = session.run(countCypher, params);
            long total = countResult.single().get("total").asLong();

            String dataCypher = cypherBuilder.toString()
                    + "RETURN n.uuid as uuid, n.type as type, n.summary as summary, "
                    + "n.created_at as createdAt, n.updated_at as updatedAt, "
                    + "n.courtName as courtName, n.partyName as partyName, "
                    + "n.caseName as caseName, n.caseNumber as caseNumber, "
                    + "n.articleNumber as articleNumber, n.lawName as lawName, "
                    + "n.judgeName as judgeName, n.documentNumber as documentNumber, "
                    + "n.agreementNumber as agreementNumber, n.evidenceNumber as evidenceNumber, "
                    + "n.reasoning as reasoning, n.factDescription as factDescription, "
                    + "n.name as genericName "
                    + "ORDER BY n.created_at DESC "
                    + "SKIP $skip LIMIT $limit";
            params.put("skip", skip);
            params.put("limit", pageSize);

            Result dataResult = session.run(dataCypher, params);

            List<Map<String, Object>> instances = new ArrayList<>();
            while (dataResult.hasNext()) {
                Record record = dataResult.next();
                Map<String, Object> instance = new HashMap<>();
                instance.put("uuid", record.get("uuid").asString());
                instance.put("type", record.get("type").asString());

                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("courtName", record.get("courtName").isNull() ? null : record.get("courtName").asString());
                nodeData.put("partyName", record.get("partyName").isNull() ? null : record.get("partyName").asString());
                nodeData.put("caseName", record.get("caseName").isNull() ? null : record.get("caseName").asString());
                nodeData.put("caseNumber", record.get("caseNumber").isNull() ? null : record.get("caseNumber").asString());
                nodeData.put("articleNumber", record.get("articleNumber").isNull() ? null : record.get("articleNumber").asString());
                nodeData.put("lawName", record.get("lawName").isNull() ? null : record.get("lawName").asString());
                nodeData.put("judgeName", record.get("judgeName").isNull() ? null : record.get("judgeName").asString());
                nodeData.put("documentNumber", record.get("documentNumber").isNull() ? null : record.get("documentNumber").asString());
                nodeData.put("agreementNumber", record.get("agreementNumber").isNull() ? null : record.get("agreementNumber").asString());
                nodeData.put("evidenceNumber", record.get("evidenceNumber").isNull() ? null : record.get("evidenceNumber").asString());
                nodeData.put("reasoning", record.get("reasoning").isNull() ? null : record.get("reasoning").asString());
                nodeData.put("factDescription", record.get("factDescription").isNull() ? null : record.get("factDescription").asString());
                nodeData.put("name", record.get("genericName").isNull() ? null : record.get("genericName").asString());
                nodeData.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());
                instance.put("name", extractNodeName(record.get("type").asString(), nodeData));

                if (record.get("summary").isNull()) {
                    instance.put("summary", null);
                } else {
                    instance.put("summary", record.get("summary").asString());
                }

                if (record.get("createdAt").isNull()) {
                    instance.put("createdAt", null);
                } else {
                    instance.put("createdAt", record.get("createdAt").asString());
                }

                if (record.get("updatedAt").isNull()) {
                    instance.put("updatedAt", null);
                } else {
                    instance.put("updatedAt", record.get("updatedAt").asString());
                }

                String propCypher = "MATCH (n:Entity {uuid: $uuid}) RETURN properties(n) as props";
                Result propResult = session.run(propCypher, Map.of("uuid", record.get("uuid").asString()));
                if (propResult.hasNext()) {
                    Record propRecord = propResult.next();
                    if (!propRecord.get("props").isNull()) {
                        instance.put("properties", propRecord.get("props").asMap());
                    }
                }

                instances.add(instance);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("data", instances);
            result.put("total", total);
            return result;
        } catch (Exception e) {
            log.error("获取类实例失败: graphId={}, classType={}", graphId, classType, e);
            throw new RuntimeException("获取类实例失败: " + e.getMessage(), e);
        }
    }

    // ==================== Episode 元数据（双写读取）====================

    /**
     * V3.1.0: 获取 Episode 类型元数据列表
     * 优先从 ont_episode_type 元数据表读取定义；若 Neo4j 中存在表中未录入的类型，
     * 则从 Neo4j 补充统计数据（fallback）。
     */
    @Override
    public List<Map<String, Object>> getEpisodeTypes(String graphId) {
        Long definitionId = resolveDefinitionId(graphId);

        // 1. 从元数据表读取所有已定义的 Episode 类型
        List<OntEpisodeTypeDO> metaTypes = episodeTypeMapper.selectActiveByDefinitionId(definitionId);
        Map<String, OntEpisodeTypeDO> metaMap = metaTypes.stream()
                .collect(Collectors.toMap(OntEpisodeTypeDO::getTypeCode, Function.identity(), (a, b) -> a));

        // 2. 从 Neo4j 统计各 episode_type 的实例数量
        Map<String, Long> neo4jCounts = countNeo4jEpisodesByType(graphId);

        // 3. 组装结果：元数据定义为主，Neo4j 计数为辅
        List<Map<String, Object>> result = new ArrayList<>();

        for (OntEpisodeTypeDO meta : metaTypes) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("typeCode", meta.getTypeCode());
            row.put("typeName", meta.getTypeName());
            row.put("processType", resolveProcessType(meta));
            row.put("stageLabel", meta.getStageLabel());
            row.put("stageLevel", meta.getStageLevel());
            row.put("isReviewStage", meta.getIsReviewStage());
            row.put("description", meta.getDescription());
            row.put("metadata", meta.getMetadata());
            // Neo4j 实例数量
            Long neo4jCount = neo4jCounts.getOrDefault(meta.getTypeCode(), 0L);
            row.put("count", neo4jCount);
            row.put("source", "metadata");
            result.add(row);
        }

        // 4. 补充：Neo4j 中存在但元数据表中没有的类型（fallback）
        for (Map.Entry<String, Long> entry : neo4jCounts.entrySet()) {
            String typeCode = entry.getKey();
            if (!metaMap.containsKey(typeCode)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("typeCode", typeCode);
                row.put("typeName", typeCode);
                row.put("processType", null);
                row.put("stageLabel", null);
                row.put("stageLevel", null);
                row.put("isReviewStage", null);
                row.put("description", null);
                row.put("metadata", null);
                row.put("count", entry.getValue());
                row.put("source", "neo4j_only");
                result.add(row);
            }
        }

        return result;
    }

    /**
     * V3.1.0: 获取 Episode 层级树（用于 IDE 左侧树形菜单）
     * 从 ont_episode_type 元数据表和 Neo4j 图数据库双写读取数据。
     * 按 process_type 一级分组，stage_label 二级分组，每组返回 count。
     */
    @Override
    public List<Map<String, Object>> getEpisodeHierarchy(String graphId) {
        Long definitionId = resolveDefinitionId(graphId);

        // 1. 从元数据表加载所有 Episode 类型定义
        List<OntEpisodeTypeDO> metaTypes = episodeTypeMapper.selectActiveByDefinitionId(definitionId);

        // 2. 按 process_type 分组，构建元数据层级的树结构
        Map<String, Map<String, Object>> processMap = new LinkedHashMap<>();

        for (OntEpisodeTypeDO meta : metaTypes) {
            String processType = resolveProcessType(meta);
            if (processType == null || processType.isEmpty()) {
                processType = "未分类";
            }
            final String finalProcessType = processType;

            processMap.computeIfAbsent(finalProcessType, k -> {
                Map<String, Object> processNode = new LinkedHashMap<>();
                processNode.put("processType", finalProcessType);
                processNode.put("name", finalProcessType);
                processNode.put("count", 0L);
                processNode.put("children", new ArrayList<Map<String, Object>>());
                processNode.put("metadata", buildProcessTypeMetadata(meta));
                return processNode;
            });

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stageChildren = (List<Map<String, Object>>) processMap.get(processType).get("children");

            Map<String, Object> stageNode = new LinkedHashMap<>();
            stageNode.put("stageLabel", meta.getStageLabel() != null ? meta.getStageLabel() : "其他");
            stageNode.put("name", meta.getTypeName() != null ? meta.getTypeName() : meta.getStageLabel());
            stageNode.put("processType", processType);
            stageNode.put("typeCode", meta.getTypeCode());
            stageNode.put("typeName", meta.getTypeName());
            stageNode.put("stageLevel", meta.getStageLevel());
            stageNode.put("isReviewStage", meta.getIsReviewStage());
            stageNode.put("description", meta.getDescription());
            stageNode.put("metadata", meta.getMetadata());
            stageNode.put("count", 0L);
            stageNode.put("source", "metadata");
            stageChildren.add(stageNode);
        }

        // 3. 从 Neo4j 统计各 typeCode 的实例数量
        Map<String, Long> neo4jCounts = countNeo4jEpisodesByType(graphId);

        // 4. 用 Neo4j 实际计数更新树节点
        for (Map<String, Object> processNode : processMap.values()) {
            long processTotalCount = 0L;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stageChildren = (List<Map<String, Object>>) processNode.get("children");

            for (Map<String, Object> stageNode : stageChildren) {
                String typeCode = (String) stageNode.get("typeCode");
                long typeCount = neo4jCounts.getOrDefault(typeCode, 0L);
                stageNode.put("count", typeCount);
                processTotalCount += typeCount;
            }

            // 累加未在元数据中定义但Neo4j中存在的类型计数
            for (Map.Entry<String, Long> entry : neo4jCounts.entrySet()) {
                String typeCode = entry.getKey();
                boolean found = stageChildren.stream()
                        .anyMatch(s -> typeCode.equals(s.get("typeCode")));
                if (!found) {
                    // Neo4j 中存在但不在元数据定义中，按 typeCode 前缀匹配 processType
                    String processType = (String) processNode.get("processType");
                    if (typeCode.toLowerCase().contains(processType.toLowerCase()) ||
                            (processType.equals("未分类") && !stageChildren.isEmpty())) {
                        processTotalCount += entry.getValue();
                    }
                }
            }

            processNode.put("count", processTotalCount);
        }

        return new ArrayList<>(processMap.values());
    }

    /**
     * V3.0.0: 获取关系类型元数据
     */
    @Override
    public List<Map<String, Object>> getRelationshipMetadata(String graphId) {
        String cypher =
            "MATCH (a)-[r]->(b) " +
            "WHERE a.graph_id = $graphId AND r.invalid_at IS NULL " +
            "RETURN type(r) as relationshipType, labels(a)[0] as sourceType, " +
            "       labels(b)[0] as targetType, count(*) as count, " +
            "       head(collect(r.fact)) as sampleFact " +
            "ORDER BY count DESC " +
            "LIMIT 50";

        List<Map<String, Object>> result = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result rs = session.run(cypher, org.neo4j.driver.Values.parameters("graphId", graphId));
            while (rs.hasNext()) {
                Record record = rs.next();
                Map<String, Object> row = new HashMap<>();
                row.put("relationshipType", record.get("relationshipType").asString());
                row.put("sourceType", record.get("sourceType").isNull() ? null : record.get("sourceType").asString());
                row.put("targetType", record.get("targetType").isNull() ? null : record.get("targetType").asString());
                row.put("count", record.get("count").asLong());
                row.put("sampleFact", record.get("sampleFact").isNull() ? null : record.get("sampleFact").asString());
                result.add(row);
            }
        }
        return result;
    }

    // ==================== 辅助方法 ====================

    private OntDefinitionDO getOrCreateDefinition(String graphId) {
        List<OntDefinitionDO> definitions = definitionMapper.selectList(
                new LambdaQueryWrapper<OntDefinitionDO>()
                        .eq(OntDefinitionDO::getGraphId, graphId)
                        .orderByDesc(OntDefinitionDO::getId)
                        .last("LIMIT 1")
        );

        if (!definitions.isEmpty()) {
            return definitions.get(0);
        }

        OntDefinitionDO definition = new OntDefinitionDO();
        definition.setGraphId(graphId);
        definition.setName("Default Ontology");
        definition.setNamespace("http://example.org/ontology");
        definition.setVersion("1.0.0");
        definition.setStatus("ACTIVE");
        definitionMapper.insert(definition);

        return definition;
    }

    private Long resolveDefinitionId(String graphId) {
        List<OntDefinitionDO> definitions = definitionMapper.selectList(
                new LambdaQueryWrapper<OntDefinitionDO>()
                        .eq(OntDefinitionDO::getGraphId, graphId)
                        .last("LIMIT 1")
        );
        return definitions.isEmpty() ? 1L : definitions.get(0).getId();
    }

    private SchemaPropertyRespVO convertToPropertyVO(OntPropertyDO property) {
        SchemaPropertyRespVO vo = new SchemaPropertyRespVO();
        vo.setId(property.getId());
        vo.setDefinitionId(property.getDefinitionId());
        vo.setLocalName(property.getLocalName());
        vo.setPropertyType(property.getPropertyType());
        vo.setRangeDataType(property.getRangeDataType());
        vo.setDomainClassId(property.getDomainClassId());
        vo.setRangeClassId(property.getRangeClassId());
        vo.setIsRequired(property.getIsRequired() != null && property.getIsRequired());
        vo.setIsMultiple(property.getIsMultiple() != null && property.getIsMultiple());
        vo.setDefaultValue(property.getDefaultValue());
        vo.setPattern(property.getPattern());
        vo.setMinValue(property.getMinValue() != null ? property.getMinValue().doubleValue() : null);
        vo.setMaxValue(property.getMaxValue() != null ? property.getMaxValue().doubleValue() : null);
        vo.setDescription(property.getDescription());
        return vo;
    }

    /**
     * 提取节点名称（根据节点类型）
     */
    private String extractNodeName(String type, Map<String, Object> nodeMap) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case "Court" -> (String) nodeMap.get("courtName");
            case "Party" -> (String) nodeMap.get("partyName");
            case "Case" -> (String) nodeOrDefault(nodeMap.get("caseName"), nodeMap.get("caseNumber"));
            case "LegalProvision" -> {
                String articleNumber = (String) nodeMap.get("articleNumber");
                String lawName = (String) nodeMap.get("lawName");
                yield articleNumber != null && lawName != null
                    ? lawName + " " + articleNumber
                    : articleNumber != null ? articleNumber : lawName;
            }
            case "Judge" -> (String) nodeMap.get("judgeName");
            case "JudgmentDocument" -> (String) nodeMap.get("documentNumber");
            case "MediationAgreement" -> (String) nodeMap.get("agreementNumber");
            case "CommercialMediationOrganization", "Mediator" -> (String) nodeMap.get("name");
            case "Evidence" -> (String) nodeMap.get("evidenceNumber");
            case "CaseReasoning" -> truncate((String) nodeMap.get("reasoning"), 50);
            case "CaseFact" -> truncate((String) nodeMap.get("factDescription"), 50);
            default -> {
                String name = (String) nodeMap.get("name");
                if (name == null || name.isBlank()) {
                    String summary = (String) nodeMap.get("summary");
                    yield truncate(summary, 50);
                }
                yield name;
            }
        };
    }

    private String nodeOrDefault(Object primary, Object fallback) {
        return primary != null ? (String) primary : (String) fallback;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return null;
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 解析 process_type：优先使用新字段 process_type，fallback 到 legal_process
     */
    private String resolveProcessType(OntEpisodeTypeDO meta) {
        if (meta.getProcessType() != null && !meta.getProcessType().isEmpty()) {
            return meta.getProcessType();
        }
        return meta.getLegalProcess();
    }

    /**
     * 构建 process_type 的元数据信息
     */
    private Map<String, Object> buildProcessTypeMetadata(OntEpisodeTypeDO meta) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("definitionId", meta.getDefinitionId());
        metadata.put("stageLevel", meta.getStageLevel());
        metadata.put("isReviewStage", meta.getIsReviewStage());
        return metadata;
    }

    /**
     * 从 Neo4j 统计各 episode_type 的实例数量
     */
    private Map<String, Long> countNeo4jEpisodesByType(String graphId) {
        Map<String, Long> counts = new HashMap<>();
        String cypher =
            "MATCH (e:Episode {graph_id: $graphId}) " +
            "WHERE e.episode_type IS NOT NULL " +
            "RETURN e.episode_type as typeCode, count(e) as cnt";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, org.neo4j.driver.Values.parameters("graphId", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                String typeCode = record.get("typeCode").asString();
                long cnt = record.get("cnt").asLong();
                counts.put(typeCode, cnt);
            }
        } catch (Exception e) {
            log.warn("从 Neo4j 统计 Episode 类型数量失败: graphId={}", graphId, e);
        }
        return counts;
    }
}
