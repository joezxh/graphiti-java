package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.OntologyDO;
import com.graphiti.module.graphiti.dal.mysql.OntologyMapper;
import com.graphiti.module.graphiti.service.OntologyService;
import com.graphiti.module.graphiti.vo.ontology.OntologyRespVO;
import com.graphiti.module.graphiti.vo.ontology.SetOntologyReqVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 本体管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyServiceImpl implements OntologyService {
    
    private final OntologyMapper ontologyMapper;
    private final ObjectMapper objectMapper;

    @Override
    public OntologyRespVO getOntology(String graphId) {
        OntologyDO entity = getOntologyByGraphId(graphId);
        return convertToOntologyRespVO(entity);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OntologyRespVO setOntology(String graphId, SetOntologyReqVO reqVO) {
        // 查询是否已存在本体定义
        LambdaQueryWrapper<OntologyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OntologyDO::getGraphId, graphId);
        wrapper.eq(OntologyDO::getDeleted, false);
        OntologyDO entity = ontologyMapper.selectOne(wrapper);
        
        if (entity == null) {
            // 新建
            entity = new OntologyDO();
            entity.setGraphId(graphId);
            entity.setEntities(reqVO.getEntities());
            entity.setEdges(reqVO.getEdges());
            entity.setIsDefault(reqVO.getIsDefault());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            entity.setDeleted(false);
            ontologyMapper.insert(entity);
        } else {
            // 更新
            entity.setEntities(reqVO.getEntities());
            entity.setEdges(reqVO.getEdges());
            entity.setIsDefault(reqVO.getIsDefault());
            entity.setUpdateTime(LocalDateTime.now());
            ontologyMapper.updateById(entity);
        }
        
        return convertToOntologyRespVO(entity);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 根据 graphId 查询本体定义
     * @param graphId 图谱ID
     * @return OntologyDO
     * @throws BusinessException 如果本体未定义
     */
    private OntologyDO getOntologyByGraphId(String graphId) {
        LambdaQueryWrapper<OntologyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OntologyDO::getGraphId, graphId);
        wrapper.eq(OntologyDO::getDeleted, false);
        OntologyDO entity = ontologyMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(1002, "本体未定义");
        }
        return entity;
    }
    
    /**
     * 转换为 OntologyRespVO
     * @param entity OntologyDO
     * @return OntologyRespVO
     */
    private OntologyRespVO convertToOntologyRespVO(OntologyDO entity) {
        OntologyRespVO respVO = new OntologyRespVO();
        respVO.setGraphId(entity.getGraphId());
        respVO.setEntities(entity.getEntities());
        respVO.setEdges(entity.getEdges());
        respVO.setIsDefault(entity.getIsDefault());
        respVO.setCreatedAt(entity.getCreateTime());
        respVO.setUpdatedAt(entity.getUpdateTime());
        return respVO;
    }

    @Override
    public Map<String, Object> validateNode(String graphId, String nodeType, Map<String, Object> properties) {
        List<Map<String, Object>> fields = getFieldDefinitions(graphId, nodeType);
        return validateProperties(fields, properties);
    }

    @Override
    public Map<String, Object> validateEdge(String graphId, String edgeType, Map<String, Object> properties) {
        List<Map<String, Object>> fields = getFieldDefinitions(graphId, edgeType);
        return validateProperties(fields, properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFieldDefinitions(String graphId, String typeName) {
        try {
            OntologyDO entity = getOntologyByGraphId(graphId);
            String entitiesJson = entity.getEntities();
            if (entitiesJson == null || entitiesJson.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> entityDefs = objectMapper.readValue(entitiesJson, List.class);
            for (Map<String, Object> def : entityDefs) {
                if (typeName.equals(def.get("name"))) {
                    Object fields = def.get("fields");
                    if (fields instanceof List) {
                        return (List<Map<String, Object>>) fields;
                    }
                    return new ArrayList<>();
                }
            }
        } catch (Exception e) {
            log.warn("获取字段定义失败：graphId={}, typeName={}", graphId, typeName, e);
        }
        return new ArrayList<>();
    }

    private Map<String, Object> validateProperties(List<Map<String, Object>> fields, Map<String, Object> properties) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (Map<String, Object> field : fields) {
            String fieldName = (String) field.get("name");
            Boolean required = (Boolean) field.get("required");
            String fieldType = (String) field.get("type");

            Object value = properties != null ? properties.get(fieldName) : null;

            if (Boolean.TRUE.equals(required) && (value == null || value.toString().isEmpty())) {
                errors.add("字段 '" + fieldName + "' 为必填项");
            }

            if (value != null && fieldType != null) {
                if (!checkType(value, fieldType)) {
                    warnings.add("字段 '" + fieldName + "' 类型应为 " + fieldType);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("warnings", warnings);
        return result;
    }

    private boolean checkType(Object value, String expectedType) {
        return switch (expectedType.toLowerCase()) {
            case "string" -> value instanceof String;
            case "integer", "int" -> value instanceof Integer || value instanceof Long;
            case "number", "float", "double" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            default -> true;
        };
    }
}
