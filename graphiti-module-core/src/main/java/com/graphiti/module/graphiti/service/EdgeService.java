package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.edge.EdgeFilterReqVO;
import com.graphiti.module.graphiti.vo.edge.EdgeInfoRespVO;
import com.graphiti.module.graphiti.vo.edge.EdgeListRespVO;
import java.util.List;
import java.util.Map;

/**
 * 边管理服务接口
 */
public interface EdgeService {
    /**
     * 获取边列表（支持过滤和分页）
     * @param graphId 图谱ID
     * @param filterReqVO 过滤条件
     * @return List<EdgeListRespVO>
     */
    List<EdgeListRespVO> listEdges(String graphId, EdgeFilterReqVO filterReqVO);
    
    /**
     * 获取边详情
     * @param graphId 图谱ID
     * @param edgeUuid 边UUID
     * @return EdgeInfoRespVO
     */
    EdgeInfoRespVO getEdgeDetail(String graphId, String edgeUuid);
    
    /**
     * 创建边
     * @param graphId 图谱ID
     * @param edgeData 边数据（包含 source, target, type, properties）
     * @return EdgeInfoRespVO
     */
    EdgeInfoRespVO createEdge(String graphId, Map<String, Object> edgeData);
    
    /**
     * 更新边
     * @param graphId 图谱ID
     * @param edgeUuid 边UUID
     * @param edgeData 更新的数据
     * @return EdgeInfoRespVO
     */
    EdgeInfoRespVO updateEdge(String graphId, String edgeUuid, Map<String, Object> edgeData);
    
    /**
     * 删除边
     * @param graphId 图谱ID
     * @param edgeUuid 边UUID
     */
    void deleteEdge(String graphId, String edgeUuid);
}
