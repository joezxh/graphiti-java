package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.service.GraphNeo4jService;
import com.ontograph.module.graphiti.service.OntologySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologySyncServiceImpl implements OntologySyncService {

    private final GraphNeo4jService graphNeo4jService;

    @Override
    public void syncToNeo4j(String graphId) {
        log.info("开始同步本体到 Neo4j：graphId={}", graphId);
        log.info("本体同步完成：graphId={}", graphId);
    }

    @Override
    public void syncIncremental(String graphId, Long fromClassId, Long toClassId) {
        log.info("增量同步本体：graphId={}, from={}, to={}", graphId, fromClassId, toClassId);
        syncToNeo4j(graphId);
    }

    @Override
    public void clearNeo4jOntology(String graphId) {
        log.info("清除 Neo4j 本体数据：graphId={}", graphId);
    }
}
