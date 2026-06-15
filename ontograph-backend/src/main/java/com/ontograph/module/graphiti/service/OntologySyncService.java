package com.ontograph.module.graphiti.service;

public interface OntologySyncService {

    void syncToNeo4j(String graphId);

    void syncIncremental(String graphId, Long fromClassId, Long toClassId);

    void clearNeo4jOntology(String graphId);
}
