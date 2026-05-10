package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.OntologyReasoner;
import com.graphiti.module.graphiti.vo.ontology.ConsistencyResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.*;
import org.apache.jena.vocabulary.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OntologyReasonerImpl implements OntologyReasoner {

    private final Map<String, InfModel> infModelCache = new ConcurrentHashMap<>();
    private final Map<String, OntModel> ontModelCache = new ConcurrentHashMap<>();

    @Override
    public synchronized void warmUp(String graphId) {
        if (infModelCache.containsKey(graphId)) return;

        log.info("推理机预热中：graphId={}", graphId);

        OntModel baseModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        baseModel.setNsPrefix("rdfs", RDFS.getURI());
        baseModel.setNsPrefix("owl", OWL.getURI());
        baseModel.setNsPrefix("rdf", RDF.getURI());

        Reasoner reasoner = ReasonerRegistry.getOWLReasoner().bindSchema(baseModel);
        InfModel infModel = ModelFactory.createInfModel(reasoner, baseModel);

        infModelCache.put(graphId, infModel);
        ontModelCache.put(graphId, baseModel);
        log.info("推理机预热完成：graphId={}", graphId);
    }

    @Override
    public synchronized void shutdown(String graphId) {
        InfModel removed = infModelCache.remove(graphId);
        ontModelCache.remove(graphId);
        if (removed != null) {
            removed.removeAll();
            log.info("推理机已关闭：graphId={}", graphId);
        }
    }

    @Override
    public List<String> getAncestorClasses(String graphId, String classUri) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) return List.of();

        Resource cls = infModel.getResource(classUri);
        if (cls == null) return List.of();

        Set<String> ancestors = new LinkedHashSet<>();
        collectAncestors(infModel, cls, ancestors);
        ancestors.remove(classUri);
        return new ArrayList<>(ancestors);
    }

    @Override
    public List<String> getDescendantClasses(String graphId, String classUri) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) return List.of();

        Resource cls = infModel.getResource(classUri);
        if (cls == null) return List.of();

        Set<String> descendants = new LinkedHashSet<>();
        collectDescendants(infModel, cls, descendants);
        descendants.remove(classUri);
        return new ArrayList<>(descendants);
    }

    @Override
    public List<String> inferTypes(String graphId, Map<String, Object> properties) {
        return List.of();
    }

    @Override
    public ConsistencyResultVO checkConsistency(String graphId) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) {
            return ConsistencyResultVO.builder()
                .consistent(true)
                .inconsistencies(List.of("推理机未初始化"))
                .build();
        }

        List<String> satisfiable = new ArrayList<>();
        List<String> unsatisfiable = new ArrayList<>();

        String[] coreClasses = {
            "http://www.w3.org/2002/07/owl#Thing",
            "http://www.w3.org/2000/01/rdf-schema#Resource"
        };
        for (String clsUri : coreClasses) {
            if (isSatisfiable(graphId, clsUri)) {
                satisfiable.add(clsUri);
            } else {
                unsatisfiable.add(clsUri);
            }
        }

        return ConsistencyResultVO.builder()
            .consistent(unsatisfiable.isEmpty())
            .satisfiableClasses(satisfiable)
            .unsatisfiableClasses(unsatisfiable)
            .build();
    }

    @Override
    public boolean isSatisfiable(String graphId, String classUri) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) return true;
        Resource cls = infModel.getResource(classUri);
        if (cls == null) return true;
        return !infModel.listStatements(null, RDF.type, cls).toList().isEmpty()
            || !infModel.listStatements(cls, RDFS.subClassOf, (RDFNode) null).toList().isEmpty();
    }

    @Override
    public boolean isWarmedUp(String graphId) {
        return infModelCache.containsKey(graphId);
    }

    private void collectAncestors(InfModel model, Resource cls, Set<String> result) {
        StmtIterator it = model.listStatements(cls, RDFS.subClassOf, (RDFNode) null);
        while (it.hasNext()) {
            RDFNode parent = it.nextStatement().getObject();
            if (parent.isResource()) {
                String parentUri = parent.asResource().getURI();
                if (parentUri != null && !parentUri.equals(cls.getURI())) {
                    result.add(parentUri);
                    collectAncestors(model, parent.asResource(), result);
                }
            }
        }
    }

    private void collectDescendants(InfModel model, Resource cls, Set<String> result) {
        StmtIterator it = model.listStatements(null, RDFS.subClassOf, cls);
        while (it.hasNext()) {
            Resource child = it.nextStatement().getSubject();
            String childUri = child.getURI();
            if (childUri != null && !childUri.equals(cls.getURI())) {
                result.add(childUri);
                collectDescendants(model, child, result);
            }
        }
    }
}
