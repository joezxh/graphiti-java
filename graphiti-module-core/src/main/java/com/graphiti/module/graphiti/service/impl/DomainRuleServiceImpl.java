package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDomainRuleDO;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDomainRuleMapper;
import com.graphiti.module.graphiti.service.DomainRuleService;
import com.graphiti.module.graphiti.vo.ontology.DomainRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainRuleServiceImpl implements DomainRuleService {

    private final OntDomainRuleMapper domainRuleMapper;
    private final ExpressionParser spelParser = new SpelExpressionParser();

    @Override
    public List<DomainRuleVO> listRules(Long definitionId) {
        List<OntDomainRuleDO> rules = domainRuleMapper.selectEnabledByDefinitionId(definitionId);
        return rules.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DomainRuleVO createRule(DomainRuleVO reqVO) {
        OntDomainRuleDO entity = new OntDomainRuleDO();
        entity.setDefinitionId(reqVO.getDefinitionId());
        entity.setRuleName(reqVO.getRuleName());
        entity.setRuleCode(reqVO.getRuleCode());
        entity.setSpelExpression(reqVO.getSpelExpression());
        entity.setSeverity(reqVO.getSeverity());
        entity.setErrorMessage(reqVO.getErrorMessage());
        entity.setDescription(reqVO.getDescription());
        entity.setEnabled(reqVO.getEnabled() != null ? reqVO.getEnabled() : true);
        entity.setApplicableClassIds(reqVO.getApplicableClassIds());
        
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        
        domainRuleMapper.insert(entity);
        log.info("创建域规则成功: ruleId={}, ruleCode={}", entity.getId(), entity.getRuleCode());
        
        return toVO(entity);
    }

    @Override
    @Transactional
    public DomainRuleVO updateRule(Long ruleId, DomainRuleVO reqVO) {
        OntDomainRuleDO existing = domainRuleMapper.selectById(ruleId);
        if (existing == null) {
            throw new BusinessException(1002, "域规则不存在: ruleId=" + ruleId);
        }
        
        if (reqVO.getRuleName() != null) existing.setRuleName(reqVO.getRuleName());
        if (reqVO.getRuleCode() != null) existing.setRuleCode(reqVO.getRuleCode());
        if (reqVO.getSpelExpression() != null) existing.setSpelExpression(reqVO.getSpelExpression());
        if (reqVO.getSeverity() != null) existing.setSeverity(reqVO.getSeverity());
        if (reqVO.getErrorMessage() != null) existing.setErrorMessage(reqVO.getErrorMessage());
        if (reqVO.getDescription() != null) existing.setDescription(reqVO.getDescription());
        if (reqVO.getApplicableClassIds() != null) {
            existing.setApplicableClassIds(reqVO.getApplicableClassIds());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
        domainRuleMapper.updateById(existing);
        log.info("更新域规则成功: ruleId={}", ruleId);
        
        return toVO(existing);
    }

    @Override
    @Transactional
    public void deleteRule(Long ruleId) {
        OntDomainRuleDO existing = domainRuleMapper.selectById(ruleId);
        if (existing == null) {
            throw new BusinessException(1002, "域规则不存在: ruleId=" + ruleId);
        }
        
        domainRuleMapper.deleteById(ruleId);
        log.info("删除域规则成功: ruleId={}", ruleId);
    }

    @Override
    @Transactional
    public void toggleRule(Long ruleId, Boolean enabled) {
        OntDomainRuleDO existing = domainRuleMapper.selectById(ruleId);
        if (existing == null) {
            throw new BusinessException(1002, "域规则不存在: ruleId=" + ruleId);
        }
        
        existing.setEnabled(enabled);
        existing.setUpdatedAt(LocalDateTime.now());
        domainRuleMapper.updateById(existing);
        log.info("切换域规则状态: ruleId={}, enabled={}", ruleId, enabled);
    }

    @Override
    public Map<String, Object> testRule(String spelExpression, Map<String, Object> testProperties) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Expression expression = spelParser.parseExpression(spelExpression);
            StandardEvaluationContext context = new StandardEvaluationContext();
            
            // 将测试属性注入 SpEL 上下文
            testProperties.forEach(context::setVariable);
            
            Object evalResult = expression.getValue(context);
            boolean passed = Boolean.TRUE.equals(evalResult);
            
            result.put("passed", passed);
            result.put("result", evalResult);
            result.put("error", null);
            
        } catch (Exception e) {
            result.put("passed", false);
            result.put("result", null);
            result.put("error", e.getMessage());
            log.warn("SpEL 表达式测试失败: expression={}, error={}", spelExpression, e.getMessage());
        }
        
        return result;
    }

    private DomainRuleVO toVO(OntDomainRuleDO entity) {
        DomainRuleVO vo = new DomainRuleVO();
        vo.setId(entity.getId());
        vo.setDefinitionId(entity.getDefinitionId());
        vo.setRuleName(entity.getRuleName());
        vo.setRuleCode(entity.getRuleCode());
        vo.setSpelExpression(entity.getSpelExpression());
        vo.setSeverity(entity.getSeverity());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setDescription(entity.getDescription());
        vo.setEnabled(entity.getEnabled());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        
        // applicableClassIds 现在由 PgJsonbTypeHandler 自动处理，无须额外解析
        vo.setApplicableClassIds(entity.getApplicableClassIds());
        
        return vo;
    }
}
