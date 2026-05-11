package com.graphiti.module.graphiti.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.dal.dataobject.PromptTemplateDO;
import com.graphiti.module.graphiti.dal.dataobject.PromptVariableDO;
import com.graphiti.module.graphiti.dal.dataobject.PromptVersionDO;
import com.graphiti.module.graphiti.dal.mysql.PromptTemplateMapper;
import com.graphiti.module.graphiti.dal.mysql.PromptVariableMapper;
import com.graphiti.module.graphiti.dal.mysql.PromptVersionMapper;
import com.graphiti.module.graphiti.service.PromptTemplateService;
import com.graphiti.module.graphiti.vo.prompt.CreatePromptTemplateReqVO;
import com.graphiti.module.graphiti.vo.prompt.PromptTemplateVO;
import com.graphiti.module.graphiti.vo.prompt.PromptVariableVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 提示词模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final PromptTemplateMapper promptTemplateMapper;
    private final PromptVariableMapper promptVariableMapper;
    private final PromptVersionMapper promptVersionMapper;
    private final ObjectMapper objectMapper;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)}");

    // ========== 模板管理 ==========

    @Override
    @Transactional
    public PromptTemplateVO createTemplate(CreatePromptTemplateReqVO reqVO) {
        // 检查编码唯一性
        PromptTemplateDO existing = promptTemplateMapper.selectByCode(reqVO.getCode());
        if (existing != null) {
            throw new IllegalArgumentException("模板编码已存在: " + reqVO.getCode());
        }

        PromptTemplateDO template = new PromptTemplateDO();
        template.setCode(reqVO.getCode());
        template.setName(reqVO.getName());
        template.setDescription(reqVO.getDescription());
        template.setType(reqVO.getType());
        template.setSystemPrompt(reqVO.getSystemPrompt());
        template.setUserPromptTemplate(reqVO.getUserPromptTemplate());
        template.setResponseFormat(reqVO.getResponseFormat());
        template.setEnabled(reqVO.getEnabled() != null ? reqVO.getEnabled() : true);
        template.setModel(reqVO.getModel());
        template.setSort(reqVO.getSort() != null ? reqVO.getSort() : 0);
        template.setTags(serializeTags(reqVO.getTags()));
        template.setExtraConfig(reqVO.getExtraConfig());

        promptTemplateMapper.insert(template);

        // 保存变量
        if (reqVO.getVariables() != null) {
            for (int i = 0; i < reqVO.getVariables().size(); i++) {
                PromptVariableVO varVO = reqVO.getVariables().get(i);
                PromptVariableDO variable = new PromptVariableDO();
                variable.setTemplateId(template.getId());
                variable.setVariableName(varVO.getVariableName());
                variable.setDescription(varVO.getDescription());
                variable.setVariableType(varVO.getVariableType() != null ? varVO.getVariableType() : "string");
                variable.setRequired(varVO.getRequired() != null ? varVO.getRequired() : true);
                variable.setDefaultValue(varVO.getDefaultValue());
                variable.setSource(varVO.getSource() != null ? varVO.getSource() : "context");
                variable.setValidationRule(varVO.getValidationRule());
                variable.setSort(varVO.getSort() != null ? varVO.getSort() : i);
                variable.setRemark(varVO.getRemark());
                promptVariableMapper.insert(variable);
            }
        }

        // 创建初始版本
        createVersionInternal(template.getId(), template.getSystemPrompt(), template.getUserPromptTemplate(),
                template.getResponseFormat(), "初始版本", 0L);

        return getTemplate(template.getId()).orElseThrow();
    }

    @Override
    @Transactional
    public PromptTemplateVO updateTemplate(CreatePromptTemplateReqVO reqVO) {
        if (reqVO.getId() == null) {
            throw new IllegalArgumentException("模板ID不能为空");
        }

        PromptTemplateDO template = promptTemplateMapper.selectById(reqVO.getId());
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + reqVO.getId());
        }

        // 如果编码变更，检查唯一性
        if (!template.getCode().equals(reqVO.getCode())) {
            PromptTemplateDO existing = promptTemplateMapper.selectByCode(reqVO.getCode());
            if (existing != null) {
                throw new IllegalArgumentException("模板编码已存在: " + reqVO.getCode());
            }
        }

        template.setCode(reqVO.getCode());
        template.setName(reqVO.getName());
        template.setDescription(reqVO.getDescription());
        template.setType(reqVO.getType());
        template.setSystemPrompt(reqVO.getSystemPrompt());
        template.setUserPromptTemplate(reqVO.getUserPromptTemplate());
        template.setResponseFormat(reqVO.getResponseFormat());
        template.setEnabled(reqVO.getEnabled() != null ? reqVO.getEnabled() : true);
        template.setModel(reqVO.getModel());
        template.setSort(reqVO.getSort() != null ? reqVO.getSort() : 0);
        template.setTags(serializeTags(reqVO.getTags()));
        template.setExtraConfig(reqVO.getExtraConfig());

        promptTemplateMapper.updateById(template);

        // 更新变量 - 先删除旧的
        promptVariableMapper.deleteByTemplateId(template.getId());
        // 再插入新的
        if (reqVO.getVariables() != null) {
            for (int i = 0; i < reqVO.getVariables().size(); i++) {
                PromptVariableVO varVO = reqVO.getVariables().get(i);
                PromptVariableDO variable = new PromptVariableDO();
                variable.setTemplateId(template.getId());
                variable.setVariableName(varVO.getVariableName());
                variable.setDescription(varVO.getDescription());
                variable.setVariableType(varVO.getVariableType() != null ? varVO.getVariableType() : "string");
                variable.setRequired(varVO.getRequired() != null ? varVO.getRequired() : true);
                variable.setDefaultValue(varVO.getDefaultValue());
                variable.setSource(varVO.getSource() != null ? varVO.getSource() : "context");
                variable.setValidationRule(varVO.getValidationRule());
                variable.setSort(varVO.getSort() != null ? varVO.getSort() : i);
                variable.setRemark(varVO.getRemark());
                promptVariableMapper.insert(variable);
            }
        }

        return getTemplate(template.getId()).orElseThrow();
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        promptTemplateMapper.deleteById(id);
        // 删除关联的变量和版本
        promptVariableMapper.deleteByTemplateId(id);
        promptVersionMapper.deleteByTemplateId(id);
    }

    @Override
    public Optional<PromptTemplateVO> getTemplate(Long id) {
        PromptTemplateDO template = promptTemplateMapper.selectById(id);
        if (template == null) {
            return Optional.empty();
        }
        return Optional.of(toVO(template));
    }

    @Override
    public Optional<PromptTemplateVO> getTemplateByCode(String code) {
        PromptTemplateDO template = promptTemplateMapper.selectByCode(code);
        if (template == null) {
            return Optional.empty();
        }
        return Optional.of(toVO(template));
    }

    @Override
    public List<PromptTemplateVO> listAllTemplates() {
        return promptTemplateMapper.selectAllEnabled().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptTemplateVO> listTemplatesByType(String type) {
        return promptTemplateMapper.selectByType(type).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptVariableDO> getTemplateVariables(Long templateId) {
        return promptVariableMapper.selectByTemplateId(templateId);
    }

    @Override
    @Transactional
    public void toggleTemplate(Long id, boolean enabled) {
        PromptTemplateDO template = promptTemplateMapper.selectById(id);
        if (template != null) {
            template.setEnabled(enabled);
            promptTemplateMapper.updateById(template);
        }
    }

    // ========== 提示词渲染 ==========

    @Override
    public RenderedPrompt renderPrompt(String templateCode, Map<String, Object> variables) {
        PromptTemplateDO template = promptTemplateMapper.selectByCode(templateCode);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateCode);
        }
        return renderPrompt(template, variables);
    }

    @Override
    public RenderedPrompt renderPrompt(PromptTemplateDO template, Map<String, Object> variables) {
        String systemPrompt = replaceVariables(template.getSystemPrompt(), variables);
        String userPrompt = replaceVariables(template.getUserPromptTemplate(), variables);
        return new RenderedPrompt(systemPrompt, userPrompt, template.getResponseFormat(), template.getModel());
    }

    @Override
    public RenderedPrompt getRenderedPrompt(Long templateId, Map<String, Object> variables) {
        PromptTemplateDO template = promptTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }
        return renderPrompt(template, variables);
    }

    /**
     * 替换提示词中的变量占位符
     */
    private String replaceVariables(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);

        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);
            if (value != null) {
                String replacement = Matcher.quoteReplacement(value.toString());
                matcher.appendReplacement(result, replacement);
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    // ========== 版本管理 ==========

    @Override
    @Transactional
    public void createVersion(Long templateId, String description) {
        PromptTemplateDO template = promptTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }

        createVersionInternal(templateId, template.getSystemPrompt(), template.getUserPromptTemplate(),
                template.getResponseFormat(), description, null);
    }

    private void createVersionInternal(Long templateId, String systemPrompt, String userPrompt,
                                       String responseFormat, String description, Long createdBy) {
        // 获取当前最大版本号
        Optional<PromptVersionDO> latest = promptVersionMapper.selectLatestByTemplateId(templateId);
        int newVersion = latest.map(v -> v.getVersion() + 1).orElse(1);

        // 将之前的活跃版本设为非活跃
        promptVersionMapper.selectActiveByTemplateId(templateId).ifPresent(v -> {
            v.setActive(false);
            promptVersionMapper.updateById(v);
        });

        PromptVersionDO version = new PromptVersionDO();
        version.setTemplateId(templateId);
        version.setVersion(newVersion);
        version.setSystemPrompt(systemPrompt);
        version.setUserPromptTemplate(userPrompt);
        version.setResponseFormat(responseFormat);
        version.setDescription(description);
        version.setActive(true);
        version.setCreatedBy(createdBy);

        promptVersionMapper.insert(version);
    }

    @Override
    public List<PromptVersionDO> getVersionHistory(Long templateId) {
        return promptVersionMapper.selectByTemplateId(templateId);
    }

    @Override
    @Transactional
    public void rollbackToVersion(Long templateId, Integer version) {
        PromptVersionDO targetVersion = promptVersionMapper.selectByTemplateIdAndVersion(templateId, version)
                .orElseThrow(() -> new IllegalArgumentException("版本不存在: " + version));

        PromptTemplateDO template = promptTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }

        // 更新模板内容
        template.setSystemPrompt(targetVersion.getSystemPrompt());
        template.setUserPromptTemplate(targetVersion.getUserPromptTemplate());
        template.setResponseFormat(targetVersion.getResponseFormat());
        promptTemplateMapper.updateById(template);

        // 将目标版本设为活跃
        targetVersion.setActive(true);
        promptVersionMapper.updateById(targetVersion);

        // 将其他版本设为非活跃
        promptVersionMapper.selectByTemplateId(templateId).forEach(v -> {
            if (!v.getVersion().equals(version)) {
                v.setActive(false);
                promptVersionMapper.updateById(v);
            }
        });
    }

    // ========== 辅助方法 ==========

    private PromptTemplateVO toVO(PromptTemplateDO template) {
        PromptTemplateVO vo = new PromptTemplateVO();
        vo.setId(template.getId());
        vo.setCode(template.getCode());
        vo.setName(template.getName());
        vo.setDescription(template.getDescription());
        vo.setType(template.getType());
        vo.setSystemPrompt(template.getSystemPrompt());
        vo.setUserPromptTemplate(template.getUserPromptTemplate());
        vo.setResponseFormat(template.getResponseFormat());
        vo.setEnabled(template.getEnabled());
        vo.setModel(template.getModel());
        vo.setSort(template.getSort());
        vo.setTags(deserializeTags(template.getTags()));
        vo.setExtraConfig(template.getExtraConfig());
        vo.setCreatedAt(template.getCreatedAt());
        vo.setUpdatedAt(template.getUpdatedAt());

        // 加载变量
        List<PromptVariableDO> variables = promptVariableMapper.selectByTemplateId(template.getId());
        vo.setVariables(variables.stream().map(this::toVariableVO).collect(Collectors.toList()));

        return vo;
    }

    private PromptVariableVO toVariableVO(PromptVariableDO variable) {
        PromptVariableVO vo = new PromptVariableVO();
        vo.setId(variable.getId());
        vo.setTemplateId(variable.getTemplateId());
        vo.setVariableName(variable.getVariableName());
        vo.setDescription(variable.getDescription());
        vo.setVariableType(variable.getVariableType());
        vo.setRequired(variable.getRequired());
        vo.setDefaultValue(variable.getDefaultValue());
        vo.setSource(variable.getSource());
        vo.setValidationRule(variable.getValidationRule());
        vo.setSort(variable.getSort());
        vo.setRemark(variable.getRemark());
        return vo;
    }

    private List<String> deserializeTags(String tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(tags, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析标签失败: {}", tags);
            return new ArrayList<>();
        }
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (Exception e) {
            log.warn("序列化标签失败", e);
            return null;
        }
    }
}
