package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.ide.SchemaChangeValidateReqVO;
import com.ontograph.module.graphiti.vo.ide.SchemaChangeValidateRespVO;
import com.ontograph.module.graphiti.vo.ide.SchemaClassRespVO;
import com.ontograph.module.graphiti.vo.ide.SchemaPropertyRespVO;

import java.util.List;
import java.util.Map;

/**
 * Schema 管理服务接口
 */
public interface SchemaManagementService {

    /**
     * 获取类的列表
     */
    List<SchemaClassRespVO> getClasses(String graphId);

    /**
     * 获取类详情
     */
    SchemaClassRespVO getClassDetail(String graphId, Long classId);

    /**
     * 创建类
     */
    SchemaClassRespVO createClass(String graphId, Map<String, Object> classData);

    /**
     * 更新类
     */
    SchemaClassRespVO updateClass(String graphId, Long classId, Map<String, Object> classData);

    /**
     * 删除类
     */
    void deleteClass(String graphId, Long classId);

    /**
     * 获取类的属性列表
     */
    List<SchemaPropertyRespVO> getClassProperties(String graphId, Long classId);

    /**
     * 创建属性
     */
    SchemaPropertyRespVO createProperty(String graphId, Long classId, Map<String, Object> propertyData);

    /**
     * 更新属性
     */
    SchemaPropertyRespVO updateProperty(String graphId, Long classId, Long propertyId, Map<String, Object> propertyData);

    /**
     * 删除属性
     */
    void deleteProperty(String graphId, Long classId, Long propertyId);

    /**
     * 验证 Schema 变更的影响
     */
    SchemaChangeValidateRespVO validateSchemaChange(String graphId, SchemaChangeValidateReqVO request);

    /**
     * 获取类的实例数据列表
     */
    Map<String, Object> getClassInstances(String graphId, String classType, Integer page, Integer pageSize, String keyword);

    /**
     * V3.1.0: 获取 Episode 类型元数据列表
     * 从 Neo4j 图数据库和 ont_episode_type 元数据表双写获取数据
     */
    List<Map<String, Object>> getEpisodeTypes(String graphId);

    /**
     * V3.1.0: 获取 Episode 层级树（用于 IDE 左侧树形菜单）
     * 从 Neo4j 图数据库和 ont_episode_type 元数据表双写获取数据
     * 按 process_type 一级分组，stage_label 二级分组，每组返回 count
     */
    List<Map<String, Object>> getEpisodeHierarchy(String graphId);

    /**
     * V3.0.0: 获取关系类型元数据
     */
    List<Map<String, Object>> getRelationshipMetadata(String graphId);
}
