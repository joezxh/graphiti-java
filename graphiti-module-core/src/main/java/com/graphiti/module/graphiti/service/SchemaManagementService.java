package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassInheritanceDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassInheritanceMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntPropertyMapper;
import com.graphiti.module.graphiti.vo.ide.SchemaClassRespVO;
import com.graphiti.module.graphiti.vo.ide.SchemaChangeValidateReqVO;
import com.graphiti.module.graphiti.vo.ide.SchemaChangeValidateRespVO;
import com.graphiti.module.graphiti.vo.ide.SchemaPropertyRespVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Schema 管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaManagementService {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntClassInheritanceMapper inheritanceMapper;
    private final OntPropertyMapper propertyMapper;
    private final org.neo4j.driver.Driver neo4jDriver;

    /**
     * 获取类的列表
     */
    public List<SchemaClassRespVO> getClasses(String graphId) {
        OntDefinitionDO definition = getOrCreateDefinition(graphId);
        
        List<OntClassDO> classes = classMapper.selectList(
                new LambdaQueryWrapper<OntClassDO>()
                        .eq(OntClassDO::getDefinitionId, definition.getId())
                        .orderByAsc(OntClassDO::getLocalName)
        );

        List<Long> classIds = classes.stream().map(OntClassDO::getId).collect(Collectors.toList());

        // 获取所有继承关系（仅在有类时才查询）
        List<OntClassInheritanceDO> inheritances = classIds.isEmpty() ? Collections.emptyList()
                : inheritanceMapper.selectList(
                        new LambdaQueryWrapper<OntClassInheritanceDO>()
                                .in(OntClassInheritanceDO::getClassId, classIds)
        );

        // 获取每个类的属性数量
        List<OntPropertyDO> properties = propertyMapper.selectList(
                new LambdaQueryWrapper<OntPropertyDO>()
                        .eq(OntPropertyDO::getDefinitionId, definition.getId())
        );
        Map<Long, Long> propertyCountMap = properties.stream()
                .collect(Collectors.groupingBy(OntPropertyDO::getDomainClassId, Collectors.counting()));
        
        // 转换为 VO
        return classes.stream().map(cls -> {
            SchemaClassRespVO vo = new SchemaClassRespVO();
            vo.setId(cls.getId());
            vo.setDefinitionId(cls.getDefinitionId());
            vo.setClassUri(cls.getClassUri());
            vo.setLocalName(cls.getLocalName());
            vo.setDescription(cls.getDescription());
            vo.setPropertyCount(propertyCountMap.getOrDefault(cls.getId(), 0L).intValue());
            
            // 获取父类 ID 列表
            List<Long> parentIds = inheritances.stream()
                    .filter(inh -> inh.getClassId().equals(cls.getId()))
                    .map(OntClassInheritanceDO::getParentClassId)
                    .collect(Collectors.toList());
            vo.setParentClassIds(parentIds);
            
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取类详情
     */
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
        
        // 获取父类
        List<OntClassInheritanceDO> inheritances = inheritanceMapper.selectList(
                new LambdaQueryWrapper<OntClassInheritanceDO>()
                        .eq(OntClassInheritanceDO::getClassId, classId)
        );
        vo.setParentClassIds(inheritances.stream()
                .map(OntClassInheritanceDO::getParentClassId)
                .collect(Collectors.toList()));
        
        // 获取属性数量
        Long propertyCount = propertyMapper.selectCount(
                new LambdaQueryWrapper<OntPropertyDO>()
                        .eq(OntPropertyDO::getDomainClassId, classId)
        );
        vo.setPropertyCount(propertyCount.intValue());
        
        return vo;
    }

    /**
     * 创建类
     */
    @Transactional
    public SchemaClassRespVO createClass(String graphId, Map<String, Object> classData) {
        OntDefinitionDO definition = getOrCreateDefinition(graphId);
        
        String localName = (String) classData.get("localName");
        String description = (String) classData.get("description");
        @SuppressWarnings("unchecked")
        List<Integer> parentClassIds = (List<Integer>) classData.getOrDefault("parentClassIds", new ArrayList<>());
        
        // 检查是否已存在
        OntClassDO existing = classMapper.selectOne(
                new LambdaQueryWrapper<OntClassDO>()
                        .eq(OntClassDO::getDefinitionId, definition.getId())
                        .eq(OntClassDO::getLocalName, localName)
        );
        if (existing != null) {
            throw new RuntimeException("Class already exists: " + localName);
        }
        
        // 创建类
        OntClassDO cls = new OntClassDO();
        cls.setDefinitionId(definition.getId());
        cls.setClassUri(definition.getNamespace() + "/" + localName);
        cls.setLocalName(localName);
        cls.setDescription(description);
        classMapper.insert(cls);
        
        // 创建继承关系
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

    /**
     * 更新类
     */
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
        
        // 更新继承关系
        if (classData.containsKey("parentClassIds")) {
            @SuppressWarnings("unchecked")
            List<Integer> parentClassIds = (List<Integer>) classData.get("parentClassIds");
            
            // 删除旧关系
            inheritanceMapper.delete(
                    new LambdaQueryWrapper<OntClassInheritanceDO>()
                            .eq(OntClassInheritanceDO::getClassId, classId)
            );
            
            // 创建新关系
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

    /**
     * 删除类
     */
    @Transactional
    public void deleteClass(String graphId, Long classId) {
        // 删除继承关系
        inheritanceMapper.delete(
                new LambdaQueryWrapper<OntClassInheritanceDO>()
                        .eq(OntClassInheritanceDO::getClassId, classId)
                        .or()
                        .eq(OntClassInheritanceDO::getParentClassId, classId)
        );
        
        // 删除属性
        propertyMapper.delete(
                new LambdaQueryWrapper<OntPropertyDO>()
                        .eq(OntPropertyDO::getDomainClassId, classId)
        );
        
        // 删除类
        classMapper.deleteById(classId);
    }

    /**
     * 获取类的属性列表
     */
    public List<SchemaPropertyRespVO> getClassProperties(String graphId, Long classId) {
        List<OntPropertyDO> properties = propertyMapper.selectList(
                new LambdaQueryWrapper<OntPropertyDO>()
                        .eq(OntPropertyDO::getDomainClassId, classId)
                        .orderByAsc(OntPropertyDO::getLocalName)
        );
        
        return properties.stream().map(this::convertToPropertyVO).collect(Collectors.toList());
    }

    /**
     * 创建属性
     */
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

    /**
     * 更新属性
     */
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

    /**
     * 删除属性
     */
    public void deleteProperty(String graphId, Long classId, Long propertyId) {
        propertyMapper.deleteById(propertyId);
    }

    /**
     * 验证 Schema 变更的影响
     */
    public SchemaChangeValidateRespVO validateSchemaChange(String graphId, SchemaChangeValidateReqVO request) {
        String changeType = request.getType();
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            switch (changeType) {
                case "UPDATE_CLASS":
                    // 类名变更可能影响节点
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
                    // 默认认为兼容
                    break;
            }
        }

        if (violations.isEmpty()) {
            return SchemaChangeValidateRespVO.compatible();
        } else {
            return SchemaChangeValidateRespVO.incompatible(violations.size(), violations);
        }
    }

    /**
     * 验证类更新变更
     */
    private List<SchemaChangeValidateRespVO.Violation> validateClassUpdate(
            Session session, String graphId, SchemaChangeValidateReqVO request) {
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        if (request.getClassId() == null || request.getChanges() == null) {
            return violations;
        }

        // 获取类的 localName
        OntClassDO cls = classMapper.selectById(request.getClassId());
        if (cls == null) {
            return violations;
        }

        // 检查是否有节点使用这个类
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
                // 类名变更会导致现有节点失去引用
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

    /**
     * 验证属性更新变更
     */
    private List<SchemaChangeValidateRespVO.Violation> validatePropertyUpdate(
            Session session, String graphId, SchemaChangeValidateReqVO request) {
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        if (request.getPropertyId() == null || request.getChanges() == null) {
            return violations;
        }

        // 获取属性定义
        OntPropertyDO property = propertyMapper.selectById(request.getPropertyId());
        if (property == null) {
            return violations;
        }

        // 获取对应的类
        OntClassDO cls = classMapper.selectById(property.getDomainClassId());
        if (cls == null) {
            return violations;
        }

        String propName = property.getLocalName();
        var changes = request.getChanges();

        // 检查必填属性变更
        if (changes.getNewIsRequired() != null && changes.getNewIsRequired()
                && (changes.getOldIsRequired() == null || !changes.getOldIsRequired())) {
            // 检查有多少节点缺少此属性
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

        // 检查正则表达式变更
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
                // 无效的正则表达式，不验证
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

    /**
     * 验证属性删除变更
     */
    private List<SchemaChangeValidateRespVO.Violation> validatePropertyDelete(
            Session session, String graphId, SchemaChangeValidateReqVO request) {
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        if (request.getPropertyId() == null) {
            return violations;
        }

        // 获取属性定义
        OntPropertyDO property = propertyMapper.selectById(request.getPropertyId());
        if (property == null) {
            return violations;
        }

        // 获取对应的类
        OntClassDO cls = classMapper.selectById(property.getDomainClassId());
        if (cls == null) {
            return violations;
        }

        // 检查有多少节点有该属性
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

    /**
     * 验证添加必填属性
     */
    private List<SchemaChangeValidateRespVO.Violation> validateAddRequiredProperty(
            Session session, String graphId, SchemaChangeValidateReqVO request) {
        List<SchemaChangeValidateRespVO.Violation> violations = new ArrayList<>();

        if (request.getClassId() == null || request.getPropertyId() == null) {
            return violations;
        }

        // 获取类定义
        OntClassDO cls = classMapper.selectById(request.getClassId());
        if (cls == null) {
            return violations;
        }

        // 获取属性定义
        OntPropertyDO property = propertyMapper.selectById(request.getPropertyId());
        if (property == null) {
            return violations;
        }

        // 检查有多少现有节点缺少此必填属性
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

    /**
     * 获取或创建本体定义
     */
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
        
        // 创建默认定义
        OntDefinitionDO definition = new OntDefinitionDO();
        definition.setGraphId(graphId);
        definition.setName("Default Ontology");
        definition.setNamespace("http://example.org/ontology");
        definition.setVersion("1.0.0");
        definition.setStatus("ACTIVE");
        definitionMapper.insert(definition);
        
        return definition;
    }

    /**
     * 转换为属性 VO
     */
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
     * 获取类的实例数据列表
     * 从 Neo4j 图数据库中查询指定类型的所有实体节点
     *
     * @param graphId 图谱ID
     * @param classType 类类型名称（localName）
     * @param page 页码
     * @param pageSize 每页数量
     * @param keyword 搜索关键词
     * @return 实例列表和总数
     */
    public Map<String, Object> getClassInstances(String graphId, String classType, Integer page, Integer pageSize, String keyword) {
        int skip = (page - 1) * pageSize;
        
        try (Session session = neo4jDriver.session()) {
            // 构建基础查询
            StringBuilder cypherBuilder = new StringBuilder();
            cypherBuilder.append("MATCH (n:Entity {graph_id: $graphId, type: $classType}) ");
            
            Map<String, Object> params = new HashMap<>();
            params.put("graphId", graphId);
            params.put("classType", classType);
            
            // 如果有关键词搜索，添加 WHERE 条件（搜索类型特定的名称字段）
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
            
            // 查询总数
            String countCypher = cypherBuilder.toString() + "RETURN count(n) as total";
            Result countResult = session.run(countCypher, params);
            long total = countResult.single().get("total").asLong();
            
            // 查询分页数据
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
                
                // 根据类型提取名称（需要显式转换为 Java 类型，避免 Neo4j Value 类型）
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
                
                // 获取节点属性
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
    
    /**
     * 根据节点类型提取对应的名称属性
     * 不同类型的节点使用不同的 name 属性
     */
    private String extractNodeName(String type, Map<String, Object> nodeMap) {
        if (type == null) {
            return null;
        }
        
        return switch (type) {
            case "Court" -> (String) nodeMap.get("courtName");
            case "Party" -> (String) nodeMap.get("partyName");
            case "Case" -> (String) nodeMap.getOrDefault("caseName", nodeMap.get("caseNumber"));
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
            case "CommercialMediationOrganization" -> (String) nodeMap.get("name");
            case "Mediator" -> (String) nodeMap.get("name");
            case "Evidence" -> (String) nodeMap.get("evidenceNumber");
            case "CaseReasoning" -> {
                String reasoning = (String) nodeMap.get("reasoning");
                yield reasoning != null && reasoning.length() > 50 
                    ? reasoning.substring(0, 50) + "..." 
                    : reasoning;
            }
            case "CaseFact" -> {
                String description = (String) nodeMap.get("factDescription");
                yield description != null && description.length() > 50 
                    ? description.substring(0, 50) + "..." 
                    : description;
            }
            default -> {
                String name = (String) nodeMap.get("name");
                if (name == null || name.isBlank()) {
                    String summary = (String) nodeMap.get("summary");
                    name = summary != null && summary.length() > 50 
                        ? summary.substring(0, 50) + "..." 
                        : summary;
                }
                yield name;
            }
        };
    }

    /**
     * V3.0.0: 获取 Episode 类型元数据列表
     * 从 Neo4j 查询已有 Episode 节点，按 V3 字段分组统计
     */
    public List<Map<String, Object>> getEpisodeTypes(String graphId) {
        String cypher = 
            "MATCH (e:Episode {graph_id: $graphId}) " +
            "WHERE e.episode_type IS NOT NULL " +
            "RETURN e.episode_type as typeCode, e.legal_process as legalProcess, " +
            "       e.stage_label as stageLabel, e.court_level as courtLevel, " +
            "       e.is_trial_stage as isTrialStage, count(*) as count " +
            "ORDER BY count DESC";
        
        List<Map<String, Object>> result = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result rs = session.run(cypher, org.neo4j.driver.Values.parameters("graphId", graphId));
            while (rs.hasNext()) {
                Record record = rs.next();
                Map<String, Object> row = new HashMap<>();
                row.put("typeCode", record.get("typeCode").isNull() ? null : record.get("typeCode").asString());
                row.put("legalProcess", record.get("legalProcess").isNull() ? null : record.get("legalProcess").asString());
                row.put("stageLabel", record.get("stageLabel").isNull() ? null : record.get("stageLabel").asString());
                row.put("courtLevel", record.get("courtLevel").isNull() ? null : record.get("courtLevel").asString());
                row.put("isTrialStage", record.get("isTrialStage").isNull() ? null : record.get("isTrialStage").asBoolean());
                row.put("count", record.get("count").asLong());
                result.add(row);
            }
        }
        return result;
    }

    /**
     * V3.0.0: 获取关系类型元数据
     * 从 Neo4j 查询已有关系的类型和统计信息
     */
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
}
