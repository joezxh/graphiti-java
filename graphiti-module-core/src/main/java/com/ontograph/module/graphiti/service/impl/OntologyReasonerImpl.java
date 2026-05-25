package com.ontograph.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import com.ontograph.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntConstraintMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntPropertyMapper;
import com.ontograph.module.graphiti.service.OntologyReasoner;
import com.ontograph.module.graphiti.vo.ontology.ConsistencyResultVO;
import com.ontograph.module.graphiti.vo.ontology.InferredTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyReasonerImpl implements OntologyReasoner {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntConstraintMapper constraintMapper;

    private final Map<String, InfModel> infModelCache = new ConcurrentHashMap<>();
    private final Map<String, OntModel> ontModelCache = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    private ReentrantReadWriteLock getLock(String graphId) {
        return locks.computeIfAbsent(graphId, k -> new ReentrantReadWriteLock());
    }

    private Long resolveDefinitionId(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        OntDefinitionDO def = definitionMapper.selectOne(w);
        return def != null ? def.getId() : null;
    }

    @Override
    public void warmUp(String graphId) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.writeLock().lock();
        try {
            if (infModelCache.containsKey(graphId)) return;
            log.info("推理机预热中：graphId={}", graphId);

            Long defId = resolveDefinitionId(graphId);
            if (defId == null) {
                log.warn("图谱无活跃本体定义，跳过预热：graphId={}", graphId);
                return;
            }

            OntModel baseModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            String ns = "http://graphiti.io/ontology/" + graphId + "/";
            baseModel.setNsPrefix("gt", ns);
            baseModel.setNsPrefix("rdfs", RDFS.getURI());
            baseModel.setNsPrefix("owl", OWL.getURI());
            baseModel.setNsPrefix("rdf", RDF.getURI());

            List<OntClassDO> classes = classMapper.selectList(
                new LambdaQueryWrapper<OntClassDO>().eq(OntClassDO::getDefinitionId, defId));
            Map<Long, OntClass> classMap = new HashMap<>();
            for (OntClassDO cls : classes) {
                OntClass ontClass = baseModel.createClass(cls.getClassUri());
                classMap.put(cls.getId(), ontClass);
            }
            for (OntClassDO cls : classes) {
                if (cls.getParentClassId() != null && classMap.containsKey(cls.getParentClassId())) {
                    classMap.get(cls.getId()).addSuperClass(classMap.get(cls.getParentClassId()));
                }
            }

            List<OntPropertyDO> props = propertyMapper.selectList(
                new LambdaQueryWrapper<OntPropertyDO>().eq(OntPropertyDO::getDefinitionId, defId));
            for (OntPropertyDO prop : props) {
                if ("OBJECT".equals(prop.getPropertyType())) {
                    ObjectProperty op = baseModel.createObjectProperty(prop.getPropertyUri());
                    if (prop.getDomainClassId() != null && classMap.containsKey(prop.getDomainClassId())) {
                        op.addDomain(classMap.get(prop.getDomainClassId()));
                    }
                    if (prop.getRangeClassId() != null && classMap.containsKey(prop.getRangeClassId())) {
                        op.addRange(classMap.get(prop.getRangeClassId()));
                    }
                } else {
                    DatatypeProperty dp = baseModel.createDatatypeProperty(prop.getPropertyUri());
                    if (prop.getDomainClassId() != null && classMap.containsKey(prop.getDomainClassId())) {
                        dp.addDomain(classMap.get(prop.getDomainClassId()));
                    }
                }
            }

            Reasoner reasoner = ReasonerRegistry.getOWLReasoner().bindSchema(baseModel);
            InfModel infModel = ModelFactory.createInfModel(reasoner, baseModel);

            infModelCache.put(graphId, infModel);
            ontModelCache.put(graphId, baseModel);
            log.info("推理机预热完成：graphId={}", graphId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void shutdown(String graphId) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.writeLock().lock();
        try {
            InfModel removed = infModelCache.remove(graphId);
            ontModelCache.remove(graphId);
            if (removed != null) {
                removed.removeAll();
                log.info("推理机已关闭：graphId={}", graphId);
            }
        } finally {
            lock.writeLock().unlock();
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
    public List<InferredTypeVO> inferTypes(String graphId, Map<String, Object> properties) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.readLock().lock();
        try {
            InfModel infModel = infModelCache.get(graphId);
            if (infModel == null || properties == null || properties.isEmpty()) {
                return List.of();
            }

            Map<String, Integer> classMatchCount = new HashMap<>();
            for (String propertyUri : properties.keySet()) {
                Property prop = infModel.getProperty(propertyUri);
                if (prop == null) {
                    continue;
                }

                StmtIterator it = infModel.listStatements(prop, RDFS.domain, (RDFNode) null);
                while (it.hasNext()) {
                    RDFNode domain = it.nextStatement().getObject();
                    if (domain.isResource()) {
                        String domainUri = domain.asResource().getURI();
                        if (domainUri != null) {
                            classMatchCount.merge(domainUri, 1, Integer::sum);
                            Set<String> ancestors = new HashSet<>();
                            collectAncestors(infModel, domain.asResource(), ancestors);
                            for (String ancestorUri : ancestors) {
                                if (!ancestorUri.equals(domainUri)) {
                                    classMatchCount.merge(ancestorUri, 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
            }

            return classMatchCount.entrySet().stream()
                .map(e -> InferredTypeVO.builder()
                    .classUri(e.getKey())
                    .confidence(e.getValue() * 1.0)
                    .reason("匹配属性数量: " + e.getValue())
                    .build())
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<String> getPropertyDomains(String graphId, String propertyUri) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.readLock().lock();
        try {
            InfModel infModel = infModelCache.get(graphId);
            if (infModel == null || propertyUri == null) {
                return List.of();
            }

            Property prop = infModel.getProperty(propertyUri);
            if (prop == null) {
                return List.of();
            }

            Set<String> domains = new LinkedHashSet<>();
            StmtIterator it = infModel.listStatements(prop, RDFS.domain, (RDFNode) null);
            while (it.hasNext()) {
                RDFNode obj = it.nextStatement().getObject();
                if (obj.isResource()) {
                    String uri = obj.asResource().getURI();
                    if (uri != null) {
                        domains.add(uri);
                    }
                }
            }
            return new ArrayList<>(domains);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<String> getPropertyRanges(String graphId, String propertyUri) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.readLock().lock();
        try {
            InfModel infModel = infModelCache.get(graphId);
            if (infModel == null || propertyUri == null) {
                return List.of();
            }

            Property prop = infModel.getProperty(propertyUri);
            if (prop == null) {
                return List.of();
            }

            Set<String> ranges = new LinkedHashSet<>();
            StmtIterator it = infModel.listStatements(prop, RDFS.range, (RDFNode) null);
            while (it.hasNext()) {
                RDFNode obj = it.nextStatement().getObject();
                if (obj.isResource()) {
                    String uri = obj.asResource().getURI();
                    if (uri != null) {
                        ranges.add(uri);
                    }
                }
            }
            return new ArrayList<>(ranges);
        } finally {
            lock.readLock().unlock();
        }
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
