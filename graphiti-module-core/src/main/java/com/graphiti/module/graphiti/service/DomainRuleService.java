package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.DomainRuleVO;
import java.util.List;
import java.util.Map;

public interface DomainRuleService {

    /**
     * 查询指定本体的所有域规则
     */
    List<DomainRuleVO> listRules(Long definitionId);

    /**
     * 创建域规则
     */
    DomainRuleVO createRule(DomainRuleVO reqVO);

    /**
     * 更新域规则
     */
    DomainRuleVO updateRule(Long ruleId, DomainRuleVO reqVO);

    /**
     * 删除域规则
     */
    void deleteRule(Long ruleId);

    /**
     * 启用/禁用域规则
     */
    void toggleRule(Long ruleId, Boolean enabled);

    /**
     * 测试域规则 SpEL 表达式
     * @param spelExpression SpEL 表达式
     * @param testProperties 测试属性数据
     * @return 测试结果（是否通过、错误信息）
     */
    Map<String, Object> testRule(String spelExpression, Map<String, Object> testProperties);
}
