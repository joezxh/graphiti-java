package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.node.NodeFilterReqVO;
import com.graphiti.module.graphiti.vo.node.NodeInfoRespVO;
import com.graphiti.module.graphiti.vo.node.NodeListRespVO;
import java.util.List;
import java.util.Map;

/**
 * 节点管理服务接口
 */
public interface NodeService {
    /**
     * 获取节点列表（支持过滤和分页）
     * @param graphId 图谱ID
     * @param filterReqVO 过滤条件
     * @return List<NodeListRespVO>
     */
    List<NodeListRespVO> listNodes(String graphId, NodeFilterReqVO filterReqVO);
    /**
     * 获取节点详情
     * @param graphId 图谱ID
     * @param nodeUuid 节点UUID
     * @return NodeInfoRespVO
     */
    NodeInfoRespVO getNodeDetail(String graphId, String nodeUuid);
    /**
     * 创建节点
     * @param graphId 图谱ID
     * @param nodeData 节点数据（包含 name, type, properties）
     * @return NodeInfoRespVO
     */
    NodeInfoRespVO createNode(String graphId, Map<String, Object> nodeData);
    /**
     * 更新节点
     * @param graphId 图谱ID
     * @param nodeUuid 节点UUID
     * @param nodeData 更新的数据
     * @return NodeInfoRespVO
     */
    NodeInfoRespVO updateNode(String graphId, String nodeUuid, Map<String, Object> nodeData);
    /**
     * 删除节点
     * @param graphId 图谱ID
     * @param nodeUuid 节点UUID
     */
    void deleteNode(String graphId, String nodeUuid);
}
