package com.graphiti.module.graphiti.namespace;

import com.graphiti.module.graphiti.service.*;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Graphiti 命名空间工厂
 *
 * <p>三层架构的入口点，提供所有命名空间的访问入口。
 *
 * <pre>
 * // 使用示例：
 * GraphitiNamespaceFactory factory;
 *
 * // 节点操作
 * factory.getNodes().getEntity().save(graphId, nodeData);
 * factory.getNodes().getEpisode().getByGraphId(graphId, 20, 0);
 *
 * // 边操作
 * factory.getEdges().getEntity().save(graphId, edgeData);
 *
 * // 图谱操作
 * factory.getGraphs().getMetadata().create(reqVO);
 * factory.getGraphs().getOntology().get(graphId);
 *
 * // 检索
 * factory.getRetrieve().search("query", graphId, 10);
 *
 * // 摄取
 * factory.getIngest().addMessages(reqVO);
 *
 * // 法律知识图谱导入
 * factory.getLegalImport().importLegalKG(reqVO);
 * </pre>
 */
@Getter
@Component
public class GraphitiNamespaceFactory {

    private final NodeNamespace nodes;
    private final EdgeNamespace edges;
    private final GraphNamespace graphs;
    private final RetrieveNamespace retrieve;
    private final IngestNamespace ingest;
    private final CustomInstructionNamespace customInstructions;
    private final LegalImportNamespace legalImport;

    public GraphitiNamespaceFactory(
            // Node services
            NodeService nodeService,
            EmbedderService embedderService,
            GraphNeo4jService graphNeo4jService,
            EpisodeService episodeService,
            CommunityService communityService,
            // Edge services
            EdgeService edgeService,
            // Graph services
            GraphitiService graphitiService,
            OntologyClassService ontologyClassService,
            // Search & ingest
            SearchService searchService,
            DataImportService dataImportService,
            // Custom instructions
            CustomInstructionService customInstructionService,
            // Legal import
            LegalImportService legalImportService) {

        // 初始化节点命名空间
        this.nodes = new NodeNamespace(
                nodeService, embedderService, graphNeo4jService,
                episodeService, communityService);

        // 初始化边命名空间
        this.edges = new EdgeNamespace(edgeService, embedderService, graphNeo4jService);

        // 初始化图谱命名空间
        this.graphs = new GraphNamespace(graphitiService, ontologyClassService);

        // 初始化检索命名空间
        this.retrieve = new RetrieveNamespace(searchService);

        // 初始化摄取命名空间
        this.ingest = new IngestNamespace(
                dataImportService, nodeService, edgeService,
                episodeService, graphNeo4jService);

        // 初始化自定义指令命名空间
        this.customInstructions = new CustomInstructionNamespace(customInstructionService);

        // 初始化法律知识图谱导入命名空间
        this.legalImport = new LegalImportNamespace(legalImportService);
    }

    /**
     * 获取节点命名空间入口
     */
    public NodeNamespace getNodes() {
        return nodes;
    }

    /**
     * 获取边命名空间入口
     */
    public EdgeNamespace getEdges() {
        return edges;
    }

    /**
     * 获取图谱命名空间入口
     */
    public GraphNamespace getGraphs() {
        return graphs;
    }

    /**
     * 获取检索命名空间入口
     */
    public RetrieveNamespace getRetrieve() {
        return retrieve;
    }

    /**
     * 获取摄取命名空间入口
     */
    public IngestNamespace getIngest() {
        return ingest;
    }

    /**
     * 获取自定义指令命名空间入口
     */
    public CustomInstructionNamespace getCustomInstructions() {
        return customInstructions;
    }

    /**
     * 获取法律知识图谱导入命名空间入口
     */
    public LegalImportNamespace getLegalImport() {
        return legalImport;
    }
}
