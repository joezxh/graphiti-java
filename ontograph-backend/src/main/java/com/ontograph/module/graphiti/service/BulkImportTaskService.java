package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.dto.batch.*;
import com.ontograph.module.graphiti.dal.repository.ImportTaskRepository;
import com.ontograph.module.graphiti.service.EntityDedupService;
import com.ontograph.module.graphiti.vo.dedup.DedupResultVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedEntityVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedRelationVO;
import com.ontograph.module.graphiti.vo.imports.AddDataBatchReqVO;
import com.ontograph.module.graphiti.vo.imports.BatchDataItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkImportTaskService {

    @Value("${graphiti.batch.content-chunk-size:50}")
    private int contentChunkSize = 50;

    @Value("${graphiti.batch.neo4j-chunk-size:200}")
    private int neo4jChunkSize = 200;

    @Value("${graphiti.batch.llm-concurrency:20}")
    private int llmConcurrency = 20;

    private final ExecutorService taskExecutor =
        Executors.newFixedThreadPool(10);

    private final GraphNeo4jService graphNeo4jService;
    private final LlmClientService llmClientService;
    private final EmbedderService embedderService;
    private final TemporalService temporalService;
    private final EntityDedupService entityDedupService;
    private final ImportTaskRepository importTaskRepository;

    @Autowired(required = false)
    private EmbeddingCacheService embeddingCacheService;

    public String executeAsync(AddDataBatchReqVO reqVO) {
        String taskId = UUID.randomUUID().toString();
        log.info("Submitting bulk import task: taskId={}, graphId={}, items={}",
                 taskId, reqVO.getGraphId(), reqVO.getItems().size());

        importTaskRepository.save(taskId, reqVO.getGraphId(), reqVO.getItems().size());

        taskExecutor.submit(() -> {
            try {
                BulkImportResult result = executeInternal(taskId, reqVO);
                importTaskRepository.updateResult(taskId, result);
                log.info("Bulk import task completed: taskId={}, result={}", taskId, result);
            } catch (Exception e) {
                log.error("Bulk import task failed: taskId={}", taskId, e);
                importTaskRepository.updateFailed(taskId, e.getMessage());
            }
        });

        return taskId;
    }

    private BulkImportResult executeInternal(String taskId, AddDataBatchReqVO reqVO) {
        long startTime = System.currentTimeMillis();
        String graphId = reqVO.getGraphId();
        List<BatchDataItemVO> items = reqVO.getItems();

        if (contentChunkSize <= 0) contentChunkSize = 50;
        if (neo4jChunkSize <= 0) neo4jChunkSize = 200;

        // ===== Phase 1: LLM concurrent extraction =====
        List<String> contents = items.stream()
            .map(BatchDataItemVO::getContent)
            .filter(c -> c != null && !c.isBlank())
            .toList();

        List<ExtractedEntityVO> allEntities = new ArrayList<>();
        List<ExtractedRelationVO> allRelations = new ArrayList<>();

        List<List<String>> contentChunks = partition(contents, contentChunkSize);
        ExecutorService llmExecutor = Executors.newFixedThreadPool(llmConcurrency);
        try {
            List<ChunkLLMResult> chunkResults = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(contentChunks.size());

            for (int i = 0; i < contentChunks.size(); i++) {
                final int chunkIdx = i;
                final List<String> chunk = contentChunks.get(i);
                llmExecutor.submit(() -> {
                    try {
                        ChunkLLMResult r = extractEntitiesAndRelations(chunk, chunkIdx);
                        chunkResults.add(r);
                    } catch (Exception e) {
                        log.warn("LLM extraction chunk[{}] failed: {}", chunkIdx, e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean finished = false;
            try {
                finished = latch.await(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("LLM extraction interrupted: taskId={}", taskId);
            }
            if (!finished) {
                log.warn("LLM extraction timed out: taskId={}", taskId);
            }

            for (ChunkLLMResult cr : chunkResults) {
                if (cr.getEntities() != null) allEntities.addAll(cr.getEntities());
                if (cr.getRelations() != null) allRelations.addAll(cr.getRelations());
            }
        } finally {
            llmExecutor.shutdown();
        }

        log.info("Phase 1 complete: taskId={}, entities={}, relations={}",
                 taskId, allEntities.size(), allRelations.size());

        if (allEntities.isEmpty()) {
            return BulkImportResult.builder()
                .totalItems(items.size()).processedItems(0).failedItems(0)
                .entitiesCreated(0).relationsCreated(0)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
        }

        // ===== Phase 2: Three-tier deduplication =====
        List<Map<String, Object>> entityMaps = allEntities.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getName());
            m.put("type", e.getType());
            m.put("summary", e.getSummary());
            m.put("attributes", e.getAttributes());
            return m;
        }).toList();

        List<Map<String, Object>> existingNodes = graphNeo4jService.getValidNodes(graphId);
        DedupResultVO dedupResult = entityDedupService.deduplicate(graphId, entityMaps, existingNodes);

        Map<String, String> uuidMapping = new HashMap<>();
        if (dedupResult.getUuidMapping() != null) {
            uuidMapping.putAll(dedupResult.getUuidMapping());
        }

        for (Map<String, Object> newNode : dedupResult.getNewNodes()) {
            String name = (String) newNode.get("name");
            String uuid = UUID.randomUUID().toString().replace("-", "");
            uuidMapping.put(name, uuid);
        }

        // ===== Phase 3: Batch embedding generation =====
        List<String> entityEmbedTexts = allEntities.stream()
            .map(e -> e.getName() + (e.getSummary() != null ? " " + e.getSummary() : ""))
            .toList();
        List<float[]> entityEmbeddings = getOrComputeEmbeddings(entityEmbedTexts);

        // ===== Phase 4: UNWIND sub-chunk writes =====
        int totalEntities = 0, totalRelations = 0, totalProcessed = 0;
        List<String> errors = new ArrayList<>();

        List<EpisodeBatchDTO> episodes = new ArrayList<>();
        for (BatchDataItemVO item : items) {
            episodes.add(EpisodeBatchDTO.builder()
                .uuid(UUID.randomUUID().toString().replace("-", ""))
                .name(item.getName() != null ? item.getName() : "Episode-" + System.currentTimeMillis())
                .source(item.getSourceType() != null ? item.getSourceType() : "text")
                .sourceDescription(item.getSourceDescription())
                .content(item.getContent())
                .properties(new HashMap<>())
                .build());
        }

        List<List<EntityBatchDTO>> entityChunks = partitionEntity(
            buildEntityDTOs(allEntities, entityEmbeddings, uuidMapping), neo4jChunkSize);

        List<List<RelationBatchDTO>> relChunks = partitionRelation(
            buildRelationDTOs(allRelations, uuidMapping), neo4jChunkSize);

        int maxChunks = Math.max(entityChunks.size(), relChunks.size());

        for (int i = 0; i < maxChunks; i++) {
            List<EntityBatchDTO> eChunk = i < entityChunks.size() ? entityChunks.get(i) : List.of();
            List<RelationBatchDTO> rChunk = i < relChunks.size() ? relChunks.get(i) : List.of();

            try {
                List<String> names = eChunk.stream().map(EntityBatchDTO::getName).toList();
                if (!names.isEmpty()) {
                    temporalService.invalidateFacts(graphId, names);
                }

                graphNeo4jService.batchAddNodesAndEdges(graphId, episodes, eChunk, rChunk);
                totalEntities += eChunk.size();
                totalRelations += rChunk.size();
                totalProcessed += Math.min(neo4jChunkSize, items.size() - i * neo4jChunkSize);
            } catch (Exception e) {
                log.error("Sub-chunk write failed: chunk[{}]: {}", i, e.getMessage());
                errors.add(String.format("chunk[%d]: %s", i, e.getMessage()));
            }
        }

        return BulkImportResult.builder()
            .totalItems(items.size())
            .processedItems(totalProcessed)
            .failedItems(items.size() - totalProcessed)
            .entitiesCreated(totalEntities)
            .relationsCreated(totalRelations)
            .errorDetails(errors)
            .durationMs(System.currentTimeMillis() - startTime)
            .build();
    }

    private ChunkLLMResult extractEntitiesAndRelations(List<String> contents, int chunkIdx) {
        String merged = String.join("\n---\n", contents);
        List<ExtractedEntityVO> entities = llmClientService.extractEntities(merged);
        List<ExtractedRelationVO> relations = llmClientService.extractRelations(merged);
        return ChunkLLMResult.builder()
            .chunkIndex(chunkIdx)
            .entities(entities != null ? entities : List.of())
            .relations(relations != null ? relations : List.of())
            .build();
    }

    private List<EntityBatchDTO> buildEntityDTOs(List<ExtractedEntityVO> entities,
            List<float[]> embeddings, Map<String, String> uuidMapping) {
        List<EntityBatchDTO> dtos = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            ExtractedEntityVO e = entities.get(i);
            String uuid = uuidMapping.getOrDefault(e.getName(),
                UUID.randomUUID().toString().replace("-", ""));
            dtos.add(EntityBatchDTO.builder()
                .uuid(uuid)
                .name(e.getName())
                .type(e.getType() != null ? e.getType() : "Entity")
                .summary(e.getSummary() != null ? e.getSummary() : "")
                .embedding(i < embeddings.size() ? embeddings.get(i) : null)
                .properties(e.getAttributes() != null ? e.getAttributes() : new HashMap<>())
                .build());
        }
        return dtos;
    }

    private List<RelationBatchDTO> buildRelationDTOs(List<ExtractedRelationVO> relations,
            Map<String, String> uuidMapping) {
        List<RelationBatchDTO> dtos = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<ExtractedRelationVO> validRels = new ArrayList<>();

        for (ExtractedRelationVO r : relations) {
            String su = uuidMapping.get(r.getSource());
            String tu = uuidMapping.get(r.getTarget());
            if (su == null || tu == null) continue;
            validRels.add(r);
            texts.add(r.getFact() != null && !r.getFact().isBlank() ? r.getFact() : r.getType());
        }

        if (validRels.isEmpty()) return dtos;

        List<float[]> relEmbeddings = getOrComputeEmbeddings(texts);

        for (int i = 0; i < validRels.size(); i++) {
            ExtractedRelationVO r = validRels.get(i);
            dtos.add(RelationBatchDTO.builder()
                .edgeUuid(UUID.randomUUID().toString().replace("-", ""))
                .sourceUuid(uuidMapping.get(r.getSource()))
                .targetUuid(uuidMapping.get(r.getTarget()))
                .type(r.getType())
                .fact(r.getFact() != null ? r.getFact() : "")
                .embedding(i < relEmbeddings.size() ? relEmbeddings.get(i) : null)
                .properties(new HashMap<>())
                .build());
        }
        return dtos;
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    private <T> List<List<T>> partitionEntity(List<T> list, int size) {
        return partition(list, size);
    }

    private <T> List<List<T>> partitionRelation(List<T> list, int size) {
        return partition(list, size);
    }

    private List<float[]> getOrComputeEmbeddings(List<String> texts) {
        if (embeddingCacheService != null) {
            return embeddingCacheService.getOrComputeBatch(texts);
        }
        return embedderService.embed(texts);
    }
}
