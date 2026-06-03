package com.ontograph.module.graphiti.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(RedissonClient.class)
public class EmbeddingCacheService {

    private final EmbedderService embedderService;
    private final RedissonClient redissonClient;

    private static final String CACHE_PREFIX = "emb:";
    private static final long CACHE_TTL_SECONDS = 86400;

    public List<float[]> getOrComputeBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<float[]> results = new ArrayList<>(Collections.nCopies(texts.size(), null));
        List<String> uncached = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();

        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null || text.isBlank()) {
                results.set(i, new float[embedderService.getDimensions()]);
                continue;
            }
            String cacheKey = CACHE_PREFIX + md5(text);
            try {
                String cached = (String) redissonClient.getBucket(cacheKey).get();
                if (cached != null) {
                    results.set(i, deserialize(cached));
                } else {
                    uncached.add(text);
                    uncachedIndices.add(i);
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed for key {}: {}", cacheKey, e.getMessage());
                uncached.add(text);
                uncachedIndices.add(i);
            }
        }

        if (!uncached.isEmpty()) {
            List<float[]> computed = embedderService.embed(uncached);
            for (int i = 0; i < uncached.size(); i++) {
                int idx = uncachedIndices.get(i);
                float[] embedding = computed.get(i);
                results.set(idx, embedding);
                String text = uncached.get(i);
                try {
                    String cacheKey = CACHE_PREFIX + md5(text);
                    redissonClient.getBucket(cacheKey).set(serialize(embedding), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Redis cache write failed: {}", e.getMessage());
                }
            }
        }

        return results;
    }

    public void invalidate(String text) {
        if (text == null || text.isBlank()) return;
        try {
            String cacheKey = CACHE_PREFIX + md5(text);
            redissonClient.getBucket(cacheKey).delete();
        } catch (Exception e) {
            log.warn("Redis cache delete failed: {}", e.getMessage());
        }
    }

    public void invalidateBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return;
        for (String text : texts) {
            if (text != null && !text.isBlank()) {
                invalidate(text);
            }
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private String serialize(float[] embedding) {
        if (embedding == null) return "";
        byte[] bytes = new byte[embedding.length * 4];
        ByteBuffer.wrap(bytes).asFloatBuffer().put(embedding);
        StringBuilder sb = new StringBuilder(embedding.length * 8);
        for (float v : embedding) {
            if (sb.length() > 0) sb.append(',');
            sb.append(Float.toString(v));
        }
        return sb.toString();
    }

    private float[] deserialize(String data) {
        if (data == null || data.isBlank()) return null;
        String[] parts = data.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
