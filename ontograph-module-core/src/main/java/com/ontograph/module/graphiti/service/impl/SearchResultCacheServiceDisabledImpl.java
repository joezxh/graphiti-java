package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.service.SearchResultCacheService;
import com.ontograph.module.graphiti.vo.search.SearchResultsRespVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * 搜索结果缓存服务空实现（缓存禁用时）
 *
 * <p>当 graphiti.search.cache.enabled = false 时使用此实现
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "graphiti.search.cache", name = "enabled", havingValue = "false")
public class SearchResultCacheServiceDisabledImpl implements SearchResultCacheService {

    @Override
    public SearchResultsRespVO getOrCompute(String query, String graphId,
                                           Supplier<SearchResultsRespVO> computer) {
        log.debug("缓存禁用，直接执行搜索: query={}, graphId={}", query, graphId);
        return computer.get();
    }

    @Override
    public SearchResultsRespVO getOrCompute(String query, String graphId, String configKey,
                                           Supplier<SearchResultsRespVO> computer) {
        return computer.get();
    }

    @Override
    public void invalidateByGraphId(String graphId) {}

    @Override
    public void invalidate(String query, String graphId) {}

    @Override
    public void invalidateAll() {}

    @Override
    public String buildCacheKey(String query, String graphId) {
        return "";
    }

    @Override
    public String buildCacheKey(String query, String graphId, String configKey) {
        return "";
    }
}
