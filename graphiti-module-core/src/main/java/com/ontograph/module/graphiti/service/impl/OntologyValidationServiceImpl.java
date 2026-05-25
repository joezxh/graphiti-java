package com.ontograph.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntConstraintDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import com.ontograph.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntConstraintMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntPropertyMapper;
import com.ontograph.module.graphiti.service.OntologyValidationService;
import com.ontograph.module.graphiti.service.validator.DomainRuleValidator;
import com.ontograph.module.graphiti.vo.ontology.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyValidationServiceImpl implements OntologyValidationService {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntConstraintMapper constraintMapper;
    private final ObjectMapper objectMapper;
    private final DomainRuleValidator domainRuleValidator;

    private static final String ERR_TYPE_NOT_FOUND      = "ONT001";
    private static final String ERR_REQUIRED_MISSING    = "ONT002";
    private static final String ERR_TYPE_MISMATCH       = "ONT003";
    private static final String ERR_CONSTRAINT_VIOLATED = "ONT004";

    @Override
    public ValidationResultVO validateNode(String graphId, String nodeType, Map<String, Object> properties) {
        if (!hasOntology(graphId)) {
            return ValidationResultVO.pass();
        }

        Long defId = resolveDefinitionId(graphId);
        if (defId == null) {
            return ValidationResultVO.pass();
        }

        List<ValidationErrorVO> errors = new ArrayList<>();
        List<ValidationWarningVO> warnings = new ArrayList<>();

        // Layer 1: 类型存在性
        OntClassDO classDef = findClassByLocalName(defId, nodeType);
        if (classDef == null) {
            errors.add(ValidationErrorVO.of(1, ERR_TYPE_NOT_FOUND,
                "节点类型未在本体中定义: " + nodeType, "type", nodeType));
            return ValidationResultVO.fail(1, errors);
        }

        // 获取该类及其父类的所有属性定义
        List<OntPropertyDO> allProps = collectPropertiesForClass(defId, classDef);
        Map<String, Object> enriched = new HashMap<>(properties != null ? properties : Map.of());

        // Layer 2: 必填属性校验
        errors.addAll(checkRequiredProperties(allProps, properties));

        // Layer 3: 数据类型校验
        errors.addAll(checkDataTypes(allProps, properties));

        // Layer 4: 约束规则校验
        errors.addAll(checkConstraints(defId, classDef, properties));

        // Layer 5: 领域规则校验
        errors.addAll(domainRuleValidator.validate(defId, classDef, properties));

        if (!errors.isEmpty()) {
            return ValidationResultVO.fail(4, errors);
        }

        // 注入默认值
        enriched = injectDefaults(allProps, enriched);

        return errors.isEmpty() && warnings.isEmpty()
            ? ValidationResultVO.pass()
            : ValidationResultVO.builder()
                .passed(true).level(0).warnings(warnings)
                .enrichedProperties(enriched)
                .build();
    }

    @Override
    public ValidationResultVO validateEdge(String graphId, String edgeType, Map<String, Object> properties) {
        if (!hasOntology(graphId)) {
            return ValidationResultVO.pass();
        }

        Long defId = resolveDefinitionId(graphId);
        if (defId == null) {
            return ValidationResultVO.pass();
        }

        List<ValidationErrorVO> errors = new ArrayList<>();

        // Layer 1: 边类型存在性（边存储在 ont_property 表）
        OntPropertyDO propDef = propertyMapper.selectByUri(defId, edgeType).orElse(null);
        if (propDef == null) {
            List<OntPropertyDO> allProps = propertyMapper.selectByDefinitionId(defId);
            propDef = allProps.stream()
                .filter(p -> edgeType.equals(p.getLocalName()) || edgeType.equals(p.getPropertyUri()))
                .findFirst().orElse(null);
        }

        if (propDef == null) {
            // 边类型未定义，允许通过（向后兼容），但给出警告
            return ValidationResultVO.passWithWarnings(List.of(
                new ValidationWarningVO(1, "边类型未在本体中定义（已允许通过）: " + edgeType,
                    "建议在本体中添加边类型定义")
            ));
        }

        // Layer 2-4: 属性校验
        List<OntPropertyDO> allProps = List.of(propDef);
        errors.addAll(checkRequiredProperties(allProps, properties));
        errors.addAll(checkDataTypes(allProps, properties));

        if (!errors.isEmpty()) {
            return ValidationResultVO.fail(4, errors);
        }

        return ValidationResultVO.pass();
    }

    @Override
    public BatchValidationRespVO validateBatch(String graphId, BatchValidationReqVO reqVO) {
        List<ValidationResultVO> nodeResults = new ArrayList<>();
        if (reqVO.getNodes() != null) {
            for (BatchValidationReqVO.NodeValidationItem item : reqVO.getNodes()) {
                nodeResults.add(validateNode(graphId, item.getNodeType(), item.getProperties()));
            }
        }

        List<ValidationResultVO> edgeResults = new ArrayList<>();
        if (reqVO.getEdges() != null) {
            for (BatchValidationReqVO.EdgeValidationItem item : reqVO.getEdges()) {
                edgeResults.add(validateEdge(graphId, item.getEdgeType(), item.getProperties()));
            }
        }

        int validNodes = (int) nodeResults.stream().filter(ValidationResultVO::isPassed).count();
        int validEdges = (int) edgeResults.stream().filter(ValidationResultVO::isPassed).count();

        return BatchValidationRespVO.builder()
            .totalNodes(nodeResults.size())
            .validNodes(validNodes)
            .totalEdges(edgeResults.size())
            .validEdges(validEdges)
            .nodeResults(nodeResults)
            .edgeResults(edgeResults)
            .build();
    }

    @Override
    public boolean hasOntology(String graphId) {
        return resolveDefinitionId(graphId) != null;
    }

    // ==================== 私有方法 ====================

    private Long resolveDefinitionId(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        OntDefinitionDO def = definitionMapper.selectOne(w);
        return def != null ? def.getId() : null;
    }

    private OntClassDO findClassByLocalName(Long defId, String localName) {
        LambdaQueryWrapper<OntClassDO> w = new LambdaQueryWrapper<>();
        w.eq(OntClassDO::getDefinitionId, defId);
        w.and(q -> q.eq(OntClassDO::getLocalName, localName)
            .or().eq(OntClassDO::getClassUri, localName));
        return classMapper.selectOne(w);
    }

    private List<OntPropertyDO> collectPropertiesForClass(Long defId, OntClassDO classDef) {
        Set<Long> classIds = new HashSet<>();
        collectClassAndAncestors(classIds, defId, classDef);

        LambdaQueryWrapper<OntPropertyDO> w = new LambdaQueryWrapper<>();
        w.eq(OntPropertyDO::getDefinitionId, defId);
        if (classIds.isEmpty()) {
            w.isNull(OntPropertyDO::getDomainClassId);
        } else {
            w.in(OntPropertyDO::getDomainClassId, classIds);
        }
        return propertyMapper.selectList(w);
    }

    private void collectClassAndAncestors(Set<Long> ids, Long defId, OntClassDO cls) {
        if (cls == null || ids.contains(cls.getId())) return;
        ids.add(cls.getId());
        if (cls.getParentClassId() != null) {
            OntClassDO parent = classMapper.selectById(cls.getParentClassId());
            collectClassAndAncestors(ids, defId, parent);
        }
    }

    private List<ValidationErrorVO> checkRequiredProperties(List<OntPropertyDO> props, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        Map<String, Object> propsMap = properties != null ? properties : Map.of();
        for (OntPropertyDO prop : props) {
            if (Boolean.TRUE.equals(prop.getIsRequired())) {
                String key = prop.getLocalName();
                Object val = propsMap.get(key);
                if (val == null || (val instanceof String s && s.isBlank())) {
                    errors.add(ValidationErrorVO.of(2, ERR_REQUIRED_MISSING,
                        "缺少必需属性: " + key, key, null));
                }
            }
        }
        return errors;
    }

    private List<ValidationErrorVO> checkDataTypes(List<OntPropertyDO> props, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        if (properties == null) return errors;
        for (OntPropertyDO prop : props) {
            String key = prop.getLocalName();
            Object val = properties.get(key);
            if (val == null) continue;

            String dataType = prop.getRangeDataType();
            if (dataType == null || dataType.isBlank()) continue;

            if (!checkValueType(val, dataType)) {
                errors.add(ValidationErrorVO.of(3, ERR_TYPE_MISMATCH,
                    "属性 '" + key + "' 类型应为 " + dataType, key, val));
            }
        }
        return errors;
    }

    private boolean checkValueType(Object value, String expectedType) {
        if (value == null) return true;
        return switch (expectedType.toLowerCase()) {
            case "string", "str" -> value instanceof String;
            case "integer", "int", "long" -> value instanceof Integer || value instanceof Long;
            case "float", "double", "number", "numeric", "decimal" -> value instanceof Number;
            case "boolean", "bool" -> value instanceof Boolean;
            case "date", "datetime", "timestamp" -> value instanceof java.time.temporal.Temporal
                || (value instanceof String s && !s.isBlank());
            case "json", "object", "map" -> value instanceof Map || value instanceof List;
            default -> true;
        };
    }

    private List<ValidationErrorVO> checkConstraints(Long defId, OntClassDO classDef, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        if (properties == null) return errors;

        LambdaQueryWrapper<OntConstraintDO> cw = new LambdaQueryWrapper<>();
        cw.eq(OntConstraintDO::getDefinitionId, defId);
        cw.eq(OntConstraintDO::getClassId, classDef.getId());
        List<OntConstraintDO> constraints = constraintMapper.selectList(cw);

        for (OntConstraintDO constraint : constraints) {
            errors.addAll(evaluateConstraint(constraint, properties));
        }
        return errors;
    }

    private List<ValidationErrorVO> evaluateConstraint(OntConstraintDO constraint, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        String constraintType = constraint.getConstraintType();
        String valueJson = constraint.getValue();
        String errorMsg = constraint.getErrorMessage() != null
            ? constraint.getErrorMessage()
            : "违反约束: " + constraintType;

        if (constraint.getPropertyId() == null) return errors;
        OntPropertyDO prop = propertyMapper.selectById(constraint.getPropertyId());
        if (prop == null) return errors;

        Object propValue = properties.get(prop.getLocalName());
        if (propValue == null) return errors;

        try {
            Map<String, Object> valueMap = objectMapper.readValue(valueJson, Map.class);

            switch (constraintType.toUpperCase()) {
                case "PATTERN" -> {
                    String pattern = (String) valueMap.get("pattern");
                    if (pattern != null && propValue instanceof String s && !Pattern.matches(pattern, s)) {
                        errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                            errorMsg + " (pattern: " + pattern + ")", prop.getLocalName(), propValue));
                    }
                }
                case "RANGE" -> {
                    if (propValue instanceof Number num) {
                        Object minObj = valueMap.get("min");
                        Object maxObj = valueMap.get("max");
                        if (minObj instanceof Number minNum && num.doubleValue() < minNum.doubleValue()) {
                            errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                                errorMsg + " (min: " + minObj + ")", prop.getLocalName(), propValue));
                        }
                        if (maxObj instanceof Number maxNum && num.doubleValue() > maxNum.doubleValue()) {
                            errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                                errorMsg + " (max: " + maxObj + ")", prop.getLocalName(), propValue));
                        }
                    }
                }
                case "ENUM" -> {
                    @SuppressWarnings("unchecked")
                    List<String> allowed = (List<String>) valueMap.get("values");
                    if (allowed != null && !allowed.contains(String.valueOf(propValue))) {
                        errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                            errorMsg + " (allowed: " + allowed + ")", prop.getLocalName(), propValue));
                    }
                }
                case "CARDINALITY" -> {
                    if (propValue instanceof List<?> list) {
                        int min = ((Number) valueMap.getOrDefault("min", 0)).intValue();
                        int max = ((Number) valueMap.getOrDefault("max", Integer.MAX_VALUE)).intValue();
                        if (list.size() < min || list.size() > max) {
                            errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED,
                                errorMsg + " (count: " + list.size() + ", expected: [" + min + "," + max + "])",
                                prop.getLocalName(), propValue));
                        }
                    }
                }
                case "NOT_NULL" -> {
                    if (propValue == null || (propValue instanceof String s && s.isBlank())) {
                        errors.add(ValidationErrorVO.of(4, ERR_CONSTRAINT_VIOLATED, errorMsg, prop.getLocalName(), propValue));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析约束值失败: constraintId={}", constraint.getId(), e);
        }
        return errors;
    }

    private Map<String, Object> injectDefaults(List<OntPropertyDO> props, Map<String, Object> enriched) {
        for (OntPropertyDO prop : props) {
            String key = prop.getLocalName();
            if (!enriched.containsKey(key) && prop.getDefaultValue() != null) {
                enriched.put(key, parseDefaultValue(prop.getDefaultValue(), prop.getRangeDataType()));
            }
        }
        return enriched;
    }

    private Object parseDefaultValue(String defaultValue, String dataType) {
        if (defaultValue == null) return null;
        return switch (dataType != null ? dataType.toLowerCase() : "string") {
            case "integer", "int" -> Integer.parseInt(defaultValue);
            case "long" -> Long.parseLong(defaultValue);
            case "float", "double", "number", "numeric", "decimal" -> new BigDecimal(defaultValue);
            case "boolean", "bool" -> Boolean.parseBoolean(defaultValue);
            default -> defaultValue;
        };
    }
}
