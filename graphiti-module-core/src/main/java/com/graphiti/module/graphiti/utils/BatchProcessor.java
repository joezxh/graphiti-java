package com.graphiti.module.graphiti.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 批量处理工具类
 */
@Slf4j
public class BatchProcessor {

    public static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * 分批处理列表
     */
    public static <T> void processBatch(List<T> items, int batchSize, Consumer<List<T>> processor) {
        for (int i = 0; i < items.size(); i += batchSize) {
            List<T> batch = items.subList(i, Math.min(i + batchSize, items.size()));
            processor.accept(batch);
            log.debug("已处理批次 {}/{}，大小 {}", i / batchSize + 1, (items.size() + batchSize - 1) / batchSize, batch.size());
        }
    }

    /**
     * 并行分批处理
     */
    public static <T> void processBatchParallel(List<T> items, int batchSize, Consumer<List<T>> processor) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            List<T> batch = items.subList(i, Math.min(i + batchSize, items.size()));
            futures.add(CompletableFuture.runAsync(() -> processor.accept(batch)));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
