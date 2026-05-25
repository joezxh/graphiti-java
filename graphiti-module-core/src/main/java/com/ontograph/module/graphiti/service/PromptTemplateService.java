package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.dal.dataobject.PromptTemplateDO;
import com.ontograph.module.graphiti.dal.dataobject.PromptVariableDO;
import com.ontograph.module.graphiti.vo.prompt.CreatePromptTemplateReqVO;
import com.ontograph.module.graphiti.vo.prompt.PromptTemplateVO;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 提示词模板服务接口
 */
public interface PromptTemplateService {

    // ========== 模板管理 ==========

    /**
     * 创建提示词模板
     */
    PromptTemplateVO createTemplate(CreatePromptTemplateReqVO reqVO);

    /**
     * 更新提示词模板
     */
    PromptTemplateVO updateTemplate(CreatePromptTemplateReqVO reqVO);

    /**
     * 删除提示词模板
     */
    void deleteTemplate(Long id);

    /**
     * 根据ID获取模板
     */
    Optional<PromptTemplateVO> getTemplate(Long id);

    /**
     * 根据编码获取模板
     */
    Optional<PromptTemplateVO> getTemplateByCode(String code);

    /**
     * 获取所有模板
     */
    List<PromptTemplateVO> listAllTemplates();

    /**
     * 根据类型获取模板列表
     */
    List<PromptTemplateVO> listTemplatesByType(String type);

    /**
     * 获取模板变量列表
     */
    List<PromptVariableDO> getTemplateVariables(Long templateId);

    /**
     * 启用/禁用模板
     */
    void toggleTemplate(Long id, boolean enabled);

    // ========== 提示词渲染 ==========

    /**
     * 渲染提示词（替换变量）
     * @param templateCode 模板编码
     * @param variables 变量值映射
     * @return 渲染后的完整提示词
     */
    RenderedPrompt renderPrompt(String templateCode, Map<String, Object> variables);

    /**
     * 渲染提示词（基于模板对象）
     */
    RenderedPrompt renderPrompt(PromptTemplateDO template, Map<String, Object> variables);

    /**
     * 获取提示词的系统提示词和用户提示词
     */
    RenderedPrompt getRenderedPrompt(Long templateId, Map<String, Object> variables);

    // ========== 提示词版本 ==========

    /**
     * 创建新版本
     */
    void createVersion(Long templateId, String description);

    /**
     * 获取版本历史
     */
    List<com.ontograph.module.graphiti.dal.dataobject.PromptVersionDO> getVersionHistory(Long templateId);

    /**
     * 回滚到指定版本
     */
    void rollbackToVersion(Long templateId, Integer version);

    /**
     * 渲染后的提示词结果
     */
    record RenderedPrompt(
        String systemPrompt,
        String userPrompt,
        String responseFormat,
        String model
    ) {}
}
