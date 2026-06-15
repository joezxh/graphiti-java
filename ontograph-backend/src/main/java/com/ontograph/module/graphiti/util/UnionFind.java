package com.ontograph.module.graphiti.util;

/**
 * Union-Find（并查集）数据结构
 * 用于批量实体去重的 UUID 规范映射
 *
 * <p>特性：
 * <ul>
 *   <li>路径压缩（Path Compression）：加速后续查找</li>
 *   <li>按秩合并（Union by Rank）：保持树平衡</li>
 * </ul>
 *
 * <p>参考 Python 实现：graphiti_core/utils/bulk_utils.py:584-621
 */
public class UnionFind<T> {

    private final java.util.Map<T, T> parent;
    private final java.util.Map<T, Integer> rank;

    public UnionFind(java.util.Collection<T> elements) {
        parent = new java.util.HashMap<>();
        rank = new java.util.HashMap<>();

        for (T element : elements) {
            parent.put(element, element);
            rank.put(element, 0);
        }
    }

    /**
     * 查找元素所在集合的根节点（带路径压缩）
     *
     * @param x 要查找的元素
     * @return 根节点
     */
    public T find(T x) {
        if (!parent.containsKey(x)) {
            throw new IllegalArgumentException("Element not found in UnionFind: " + x);
        }

        if (!parent.get(x).equals(x)) {
            // 路径压缩：将查找路径上的所有节点直接指向根节点
            parent.put(x, find(parent.get(x)));
        }
        return parent.get(x);
    }

    /**
     * 合并两个元素所在的集合（按秩合并）
     *
     * @param x 元素1
     * @param y 元素2
     * @return true 如果合并成功，false 如果已经在同一集合
     */
    public boolean union(T x, T y) {
        T rootX = find(x);
        T rootY = find(y);

        if (rootX.equals(rootY)) {
            return false;
        }

        // 按秩合并：小的秩合并到大的秩
        int rankX = rank.get(rootX);
        int rankY = rank.get(rootY);

        if (rankX < rankY) {
            parent.put(rootX, rootY);
        } else if (rankX > rankY) {
            parent.put(rootY, rootX);
        } else {
            // 秩相同，合并后秩 +1
            parent.put(rootY, rootX);
            rank.put(rootX, rankX + 1);
        }
        return true;
    }

    /**
     * 检查两个元素是否在同一个集合
     *
     * @param x 元素1
     * @param y 元素2
     * @return true 如果在同一集合
     */
    public boolean connected(T x, T y) {
        return find(x).equals(find(y));
    }

    /**
     * 获取所有不同的集合（分组）
     *
     * @return 每个根节点对应一个集合的 Map
     */
    public java.util.Map<T, java.util.Set<T>> getGroups() {
        java.util.Map<T, java.util.Set<T>> groups = new java.util.HashMap<>();

        for (T element : parent.keySet()) {
            T root = find(element);
            groups.computeIfAbsent(root, k -> new java.util.HashSet<>()).add(element);
        }

        return groups;
    }

    /**
     * 获取集合的数量
     *
     * @return 不同集合的数量
     */
    public int getGroupCount() {
        return getGroups().size();
    }

    /**
     * 获取指定元素的秩
     *
     * @param element 元素
     * @return 秩值
     */
    public int getRank(T element) {
        return rank.getOrDefault(find(element), 0);
    }
}
