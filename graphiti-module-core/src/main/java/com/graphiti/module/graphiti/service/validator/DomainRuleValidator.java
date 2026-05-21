package com.graphiti.module.graphiti.service.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDomainRuleDO;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDomainRuleMapper;
import com.graphiti.module.graphiti.vo.ontology.ValidationErrorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainRuleValidator {

    private final OntDomainRuleMapper domainRuleMapper;
    private final ObjectMapper objectMapper;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    public List<ValidationErrorVO> validate(Long defId, OntClassDO classDef, Map<String, Object> properties) {
        List<OntDomainRuleDO> rules = domainRuleMapper.selectEnabledByDefinitionId(defId);
        List<ValidationErrorVO> errors = new ArrayList<>();
        for (OntDomainRuleDO rule : rules) {
            if (!isApplicable(rule, classDef)) continue;
            errors.addAll(evaluateRule(rule, properties));
        }
        return errors;
    }

    private boolean isApplicable(OntDomainRuleDO rule, OntClassDO classDef) {
        if (rule.getApplicableClassIds() == null || rule.getApplicableClassIds().isBlank()) return true;
        try {
            List<Long> ids = objectMapper.readValue(rule.getApplicableClassIds(), List.class);
            return ids.contains(classDef.getId());
        } catch (Exception e) {
            return true;
        }
    }

    private List<ValidationErrorVO> evaluateRule(OntDomainRuleDO rule, Map<String, Object> properties) {
        List<ValidationErrorVO> errors = new ArrayList<>();
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (properties != null) properties.forEach(context::setVariable);
            Boolean passed = parser.parseExpression(rule.getSpelExpression()).getValue(context, Boolean.class);
            if (!Boolean.TRUE.equals(passed)) {
                errors.add(ValidationErrorVO.builder().layer(5).code("ONT005")
                    .message(rule.getErrorMessage() != null ? rule.getErrorMessage() : "违反领域规则: " + rule.getRuleName())
                    .build());
            }
        } catch (SpelEvaluationException e) {
            log.error("SpEL执行失败: rule={}", rule.getRuleCode(), e);
            errors.add(ValidationErrorVO.builder().layer(5).code("ONT005E")
                .message("领域规则表达式执行失败: " + rule.getRuleName()).build());
        }
        return errors;
    }
}
