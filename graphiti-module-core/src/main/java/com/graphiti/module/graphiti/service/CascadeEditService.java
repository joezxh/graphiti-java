package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ide.CascadeExecuteReqVO;
import com.graphiti.module.graphiti.vo.ide.CascadeExecuteRespVO;
import com.graphiti.module.graphiti.vo.ide.CascadeFilterReqVO;
import com.graphiti.module.graphiti.vo.ide.CascadePreviewRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 级联编辑服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CascadeEditService {

    private final Driver neo4jDriver;

    /**
     * 预览级联编辑影响范围
     */
    public CascadePreviewRespVO preview(String graphId, CascadeFilterReqVO filter) {
        try (Session session = neo4jDriver.session()) {
            // 构建 WHERE 条件
            StringBuilder whereClause = new StringBuilder("WHERE n.group_id = $graphId AND n.invalid_at IS NULL ");
            Map<String, Object> params = new HashMap<>();
            params.put("graphId", graphId);
            
            // 添加类型条件
            if (filter.getClassType() != null && !filter.getClassType().isBlank()) {
                whereClause.append("AND n.type = $classType ");
                params.put("classType", filter.getClassType());
            }
            
            // 添加属性条件
            if (filter.getConditions() != null && !filter.getConditions().isEmpty()) {
                int index = 0;
                for (CascadeFilterReqVO.PropertyCondition condition : filter.getConditions()) {
                    String propName = condition.getPropertyName();
                    String operator = condition.getOperator();
                    Object value = condition.getValue();
                    
                    String conditionStr = buildConditionClause("n", propName, operator, "p" + index, index == 0);
                    if (conditionStr != null) {
                        whereClause.append(conditionStr);
                        if (value != null && !isNoValueOperator(operator)) {
                            params.put("p" + index, convertValue(value));
                        }
                    }
                    index++;
                }
            }
            
            // 统计总数
            String countCypher = "MATCH (n:Entity) " + whereClause + " RETURN count(n) as total";
            Result countResult = session.run(countCypher, params);
            long total = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;
            
            // 获取分布
            List<CascadePreviewRespVO.DistributionItem> distribution = new ArrayList<>();
            
            // 按某个属性分组统计（这里使用一个常见属性演示）
            String distCypher = "MATCH (n:Entity) " + whereClause + 
                    " RETURN n.city as value, count(n) as count ORDER BY count DESC LIMIT 10";
            
            // 尝试不同的分组属性
            String[] groupByProps = {"city", "status", "company", "type"};
            for (String prop : groupByProps) {
                String groupByCypher = "MATCH (n:Entity) " + whereClause + 
                        " WHERE n." + prop + " IS NOT NULL " +
                        " RETURN n." + prop + " as value, count(n) as count ORDER BY count DESC LIMIT 5";
                
                Result distResult = session.run(groupByCypher, params);
                boolean hasResults = false;
                
                while (distResult.hasNext()) {
                    hasResults = true;
                    Record record = distResult.next();
                    Object val = record.get("value").asObject();
                    if (val != null) {
                        distribution.add(CascadePreviewRespVO.DistributionItem.builder()
                                .groupBy(prop)
                                .value(String.valueOf(val))
                                .count(record.get("count").asLong())
                                .build());
                    }
                }
                
                if (hasResults) break;
            }
            
            CascadePreviewRespVO response = new CascadePreviewRespVO();
            response.setTotalMatch(total);
            response.setDistribution(distribution);
            
            return response;
        }
    }

    /**
     * 执行级联编辑
     */
    public CascadeExecuteRespVO execute(String graphId, CascadeExecuteReqVO executeReq) {
        try (Session session = neo4jDriver.session()) {
            // 构建 WHERE 条件
            StringBuilder whereClause = new StringBuilder("WHERE n.group_id = $graphId AND n.invalid_at IS NULL ");
            Map<String, Object> params = new HashMap<>();
            params.put("graphId", graphId);
            
            // 添加类型条件
            if (executeReq.getClassType() != null && !executeReq.getClassType().isBlank()) {
                whereClause.append("AND n.type = $classType ");
                params.put("classType", executeReq.getClassType());
            }
            
            // 添加属性条件
            if (executeReq.getConditions() != null && !executeReq.getConditions().isEmpty()) {
                int index = 0;
                for (CascadeFilterReqVO.PropertyCondition condition : executeReq.getConditions()) {
                    String propName = condition.getPropertyName();
                    String operator = condition.getOperator();
                    Object value = condition.getValue();
                    
                    String conditionStr = buildConditionClause("n", propName, operator, "p" + index, index == 0);
                    if (conditionStr != null) {
                        whereClause.append(conditionStr);
                        if (value != null && !isNoValueOperator(operator)) {
                            params.put("p" + index, convertValue(value));
                        }
                    }
                    index++;
                }
            }
            
            // 构建更新语句
            Map<String, Object> updates = executeReq.getUpdates();
            if (updates == null || updates.isEmpty()) {
                CascadeExecuteRespVO response = new CascadeExecuteRespVO();
                response.setSuccess(false);
                response.setAffectedCount(0L);
                response.setFailedCount(0L);
                response.setErrors(List.of("No updates specified"));
                return response;
            }
            
            // 构建 SET 子句
            List<String> setClauses = new ArrayList<>();
            int updateIndex = 0;
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String propName = entry.getKey();
                Object value = entry.getValue();
                setClauses.add("n." + propName + " = $update" + updateIndex);
                params.put("update" + updateIndex, value);
                updateIndex++;
            }
            
            // 执行更新
            String updateCypher = "MATCH (n:Entity) " + whereClause + " SET " + String.join(", ", setClauses);
            
            // 先统计匹配数量
            String countCypher = "MATCH (n:Entity) " + whereClause + " RETURN count(n) as total";
            Result countResult = session.run(countCypher, params);
            long matchedCount = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;
            
            // 执行更新
            session.run(updateCypher, params);
            
            CascadeExecuteRespVO response = new CascadeExecuteRespVO();
            response.setSuccess(true);
            response.setAffectedCount(matchedCount);
            response.setFailedCount(0L);
            response.setErrors(Collections.emptyList());
            
            return response;
        } catch (Exception e) {
            log.error("Cascade edit failed", e);
            CascadeExecuteRespVO response = new CascadeExecuteRespVO();
            response.setSuccess(false);
            response.setAffectedCount(0L);
            response.setFailedCount(0L);
            response.setErrors(List.of(e.getMessage()));
            return response;
        }
    }

    /**
     * 构建条件子句
     */
    private String buildConditionClause(String prefix, String propName, String operator, String paramName, boolean isFirst) {
        String andOr = isFirst ? "" : " AND ";
        
        switch (operator) {
            case "eq":
                return andOr + prefix + "." + propName + " = $" + paramName;
            case "ne":
                return andOr + prefix + "." + propName + " <> $" + paramName;
            case "gt":
                return andOr + prefix + "." + propName + " > $" + paramName;
            case "lt":
                return andOr + prefix + "." + propName + " < $" + paramName;
            case "gte":
                return andOr + prefix + "." + propName + " >= $" + paramName;
            case "lte":
                return andOr + prefix + "." + propName + " <= $" + paramName;
            case "contains":
                return andOr + prefix + "." + propName + " CONTAINS $" + paramName;
            case "not_contains":
                return andOr + "NOT " + prefix + "." + propName + " CONTAINS $" + paramName;
            case "in":
                return andOr + prefix + "." + propName + " IN $" + paramName;
            case "not_in":
                return andOr + prefix + "." + propName + " NOT IN $" + paramName;
            case "is_null":
                return andOr + prefix + "." + propName + " IS NULL";
            case "is_not_null":
                return andOr + prefix + "." + propName + " IS NOT NULL";
            default:
                return null;
        }
    }

    /**
     * 判断是否为不需要值的操作符
     */
    private boolean isNoValueOperator(String operator) {
        return "is_null".equals(operator) || "is_not_null".equals(operator);
    }

    /**
     * 转换值类型
     */
    private Object convertValue(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.toArray();
        }
        if (value instanceof Number) {
            return value;
        }
        return String.valueOf(value);
    }
}
