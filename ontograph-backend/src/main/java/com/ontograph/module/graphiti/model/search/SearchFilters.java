package com.ontograph.module.graphiti.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 搜索过滤器
 *
 * <p>参考 Python 实现：graphiti_core/search/search_models.py:SearchFilters
 */
@Data
@Schema(description = "搜索过滤器")
public class SearchFilters implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "节点标签过滤（如 [Person, Organization]）")
    private List<String> nodeLabels;

    @Schema(description = "边类型过滤（如 [RELATES_TO, MENTIONS]）")
    private List<String> edgeTypes;

    @Schema(description = "有效时间过滤")
    private List<List<DateFilter>> validAt;

    @Schema(description = "失效时间过滤")
    private List<List<DateFilter>> invalidAt;

    @Schema(description = "创建时间过滤")
    private List<List<DateFilter>> createdAt;

    @Schema(description = "过期时间过滤")
    private List<List<DateFilter>> expiredAt;

    @Schema(description = "指定边 UUID 列表过滤")
    private List<String> edgeUuids;

    @Schema(description = "属性过滤器")
    private List<PropertyFilter> propertyFilters;

    /**
     * 日期过滤条件
     */
    @Data
    public static class DateFilter implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "比较操作符")
        private ComparisonOperator operator = ComparisonOperator.gte;

        @Schema(description = "日期值（ISO 8601）")
        private String value;
    }

    /**
     * 属性过滤器
     */
    @Data
    public static class PropertyFilter implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "属性名称")
        private String property;

        @Schema(description = "比较操作符")
        private ComparisonOperator operator = ComparisonOperator.eq;

        @Schema(description = "属性值")
        private Object value;
    }

    /**
     * 比较操作符枚举
     */
    public enum ComparisonOperator {
        eq,    // equals
        ne,    // not equals
        gt,    // greater than
        gte,   // greater than or equal
        lt,    // less than
        lte,   // less than or equal
        isNull,
        isNotNull
    }

    // ==================== 便捷工厂方法 ====================

    public static SearchFilters empty() {
        return new SearchFilters();
    }

    public static SearchFilters byNodeLabels(List<String> labels) {
        SearchFilters filters = new SearchFilters();
        filters.setNodeLabels(labels);
        return filters;
    }

    public static SearchFilters byEdgeTypes(List<String> types) {
        SearchFilters filters = new SearchFilters();
        filters.setEdgeTypes(types);
        return filters;
    }

    public static SearchFilters byGraphId(String graphId) {
        SearchFilters filters = new SearchFilters();
        PropertyFilter pf = new PropertyFilter();
        pf.setProperty("graph_id");
        pf.setOperator(ComparisonOperator.eq);
        pf.setValue(graphId);
        filters.setPropertyFilters(List.of(pf));
        return filters;
    }
}
