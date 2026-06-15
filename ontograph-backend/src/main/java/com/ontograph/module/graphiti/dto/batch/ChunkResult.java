package com.ontograph.module.graphiti.dto.batch;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result of processing a single chunk in batch operations
 */
@Data
@Builder
public class ChunkResult {

    private int chunkIndex;
    private int itemCount;
    private boolean success;
    private int entitiesCreated;
    private int relationsCreated;
    private String errorMessage;
    private List<Integer> failedItemIndices;
}
