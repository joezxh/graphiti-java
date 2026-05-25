package com.ontograph.module.graphiti.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加权标签传播算法实现
 *
 * <p>参考 Python 实现：graphiti_core/utils/maintenance/community_operations.py
 *
 * <p>算法步骤：
 * <ol>
 *   <li>初始化：每个节点独立社区</li>
 *   <li>迭代：统计邻居节点的加权票数</li>
 *   <li>票数计算：同一社区内的关系越多，权重越大</li>
 *   <li>平票处理：选择社区 ID 较大的</li>
 *   <li>收敛检测：直到无社区变化</li>
 * </ol>
 */
public class LabelPropagation {

    // 最大迭代次数，防止无限循环
    private static final int MAX_ITERATIONS = 100;

    // 收敛阈值：变化节点数低于此值时认为收敛
    private static final double CONVERGENCE_THRESHOLD = 0.001;

    /**
     * 邻居节点信息
     */
    public static class Neighbor {
        private final String nodeUuid;
        private final int edgeCount;

        public Neighbor(String nodeUuid, int edgeCount) {
            this.nodeUuid = nodeUuid;
            this.edgeCount = edgeCount;
        }

        public String getNodeUuid() {
            return nodeUuid;
        }

        public int getEdgeCount() {
            return edgeCount;
        }
    }

    /**
     * 图的邻居映射
     */
    public static class Graph {
        private final Map<String, List<Neighbor>> adjacencyList;

        public Graph() {
            this.adjacencyList = new HashMap<>();
        }

        public void addNode(String nodeId) {
            adjacencyList.putIfAbsent(nodeId, new ArrayList<>());
        }

        public void addEdge(String nodeA, String nodeB, int weight) {
            addNode(nodeA);
            addNode(nodeB);

            adjacencyList.computeIfAbsent(nodeA, k -> new ArrayList<>()).add(new Neighbor(nodeB, weight));
            adjacencyList.computeIfAbsent(nodeB, k -> new ArrayList<>()).add(new Neighbor(nodeA, weight));
        }

        public List<Neighbor> getNeighbors(String nodeId) {
            return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
        }

        public Set<String> getNodes() {
            return adjacencyList.keySet();
        }

        public int getDegree(String nodeId) {
            return getNeighbors(nodeId).size();
        }
    }

    /**
     * 社区检测结果
     */
    public static class CommunityResult {
        // 节点 UUID -> 社区 UUID
        private final Map<String, String> nodeCommunity;
        // 社区 UUID -> 节点集合
        private final Map<String, Set<String>> communityMembers;
        // 迭代次数
        private final int iterations;

        public CommunityResult(Map<String, String> nodeCommunity, int iterations) {
            this.nodeCommunity = new HashMap<>(nodeCommunity);
            this.communityMembers = new HashMap<>();

            // 构建反向索引
            for (Map.Entry<String, String> entry : nodeCommunity.entrySet()) {
                communityMembers.computeIfAbsent(entry.getValue(), k -> new HashSet<>()).add(entry.getKey());
            }

            this.iterations = iterations;
        }

        public Map<String, String> getNodeCommunity() {
            return nodeCommunity;
        }

        public Map<String, Set<String>> getCommunityMembers() {
            return communityMembers;
        }

        public int getIterationCount() {
            return iterations;
        }

        public int getCommunityCount() {
            return communityMembers.size();
        }
    }

    /**
     * 执行加权标签传播算法
     *
     * @param graph 图结构
     * @return 社区检测结果
     */
    public static CommunityResult detect(Graph graph) {
        Set<String> nodes = graph.getNodes();
        if (nodes.isEmpty()) {
            return new CommunityResult(Collections.emptyMap(), 0);
        }

        // 初始化：每个节点独立社区
        Map<String, String> communityMap = new ConcurrentHashMap<>();
        for (String node : nodes) {
            communityMap.put(node, node);
        }

        int iterations = 0;
        boolean changed = true;

        while (changed && iterations < MAX_ITERATIONS) {
            changed = false;
            iterations++;

            // 随机打乱节点顺序
            List<String> shuffledNodes = new ArrayList<>(nodes);
            Collections.shuffle(shuffledNodes);

            for (String nodeId : shuffledNodes) {
                List<Neighbor> neighbors = graph.getNeighbors(nodeId);

                if (neighbors.isEmpty()) {
                    continue;
                }

                // 统计邻居社区投票（加权）
                Map<String, Integer> votes = new HashMap<>();
                for (Neighbor neighbor : neighbors) {
                    String neighborCommunity = communityMap.get(neighbor.getNodeUuid());
                    if (neighborCommunity != null) {
                        // 加权：边的数量越多，票数越高
                        votes.merge(neighborCommunity, neighbor.getEdgeCount(), Integer::sum);
                    }
                }

                if (votes.isEmpty()) {
                    continue;
                }

                // 找出票数最多的社区
                String currentCommunity = communityMap.get(nodeId);
                Map.Entry<String, Integer> maxEntry = findMaxVote(votes, currentCommunity);

                if (maxEntry != null && !maxEntry.getKey().equals(currentCommunity)) {
                    communityMap.put(nodeId, maxEntry.getKey());
                    changed = true;
                }
            }
        }

        // 压缩社区 ID（使用连续的整数 ID）
        Map<String, String> compressedCommunityMap = compressCommunityIds(communityMap);

        return new CommunityResult(compressedCommunityMap, iterations);
    }

    /**
     * 找出票数最多的社区（平票时选择社区 ID 较大的）
     */
    private static Map.Entry<String, Integer> findMaxVote(Map<String, Integer> votes, String currentCommunity) {
        Map.Entry<String, Integer> maxEntry = null;

        for (Map.Entry<String, Integer> entry : votes.entrySet()) {
            if (maxEntry == null) {
                maxEntry = entry;
            } else if (entry.getValue() > maxEntry.getValue()) {
                maxEntry = entry;
            } else if (entry.getValue().equals(maxEntry.getValue())) {
                // 平票时，选择社区 ID 较大的
                if (entry.getKey().compareTo(maxEntry.getKey()) > 0) {
                    maxEntry = entry;
                }
            }
        }

        return maxEntry;
    }

    /**
     * 压缩社区 ID 为连续的整数
     */
    private static Map<String, String> compressCommunityIds(Map<String, String> communityMap) {
        Map<String, Integer> communityToIndex = new HashMap<>();
        int index = 0;

        for (String community : communityMap.values()) {
            if (!communityToIndex.containsKey(community)) {
                communityToIndex.put(community, index++);
            }
        }

        Map<String, String> compressed = new HashMap<>();
        for (Map.Entry<String, String> entry : communityMap.entrySet()) {
            int compressedId = communityToIndex.get(entry.getValue());
            compressed.put(entry.getKey(), String.format("community_%d", compressedId));
        }

        return compressed;
    }

    /**
     * 从边列表构建图
     *
     * @param edges 边列表，每条边格式：[sourceUuid, targetUuid, edgeCount]
     * @return 图结构
     */
    public static Graph buildGraphFromEdges(List<List<String>> edges) {
        Graph graph = new Graph();

        for (List<String> edge : edges) {
            if (edge.size() >= 2) {
                String source = edge.get(0);
                String target = edge.get(1);
                int weight = edge.size() > 2 ? Integer.parseInt(edge.get(2)) : 1;
                graph.addEdge(source, target, weight);
            }
        }

        return graph;
    }
}
