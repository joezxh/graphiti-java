package com.ontograph.module.graphiti.dto.batch;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of bulk import operation
 */
@Data
@Builder
public class BulkImportResult {

    private int totalItems;
    private int processedItems;
    private int failedItems;
    private int entitiesCreated;
    private int relationsCreated;

    @Builder.Default
    private List<String> errorDetails = new ArrayList<>();

    private long durationMs;
}
