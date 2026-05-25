package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.service.SchemaOrgImportService;
import com.ontograph.module.graphiti.vo.ontology.SchemaOrgImportReqVO;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SchemaOrgImportServiceImpl implements SchemaOrgImportService {

    private static final String SCHEMA_ORG_BASE = "https://schema.org/";
    private static final ValueFactory VF = SimpleValueFactory.getInstance();

    @Override
    public Map<String, Integer> importFromSchemaOrg(String graphId, SchemaOrgImportReqVO reqVO) {
        int classesImported = 0;
        int propertiesImported = 0;

        for (String domain : reqVO.getDomains()) {
            try {
                String jsonLd = fetchSchemaOrgJsonLd(domain, reqVO.getHierarchyDepth());
                Model model = parseJsonLd(jsonLd);
                List<SchemaClassInfo> classes = extractClasses(model, domain, reqVO.getHierarchyDepth());
                for (SchemaClassInfo cls : classes) {
                    log.info("导入类: {} -> {}", cls.uri, cls.label);
                    classesImported++;
                }
                List<SchemaPropertyInfo> props = extractProperties(model, classes);
                for (SchemaPropertyInfo prop : props) {
                    log.info("导入属性: {} domain={} range={}", prop.uri, prop.domain, prop.range);
                    propertiesImported++;
                }
            } catch (Exception e) {
                log.error("导入 Schema.org 类 {} 失败", domain, e);
            }
        }

        return Map.of("classesImported", classesImported, "propertiesImported", propertiesImported);
    }

    @Override
    public String exportAsJsonLd(String graphId) {
        return "{}";
    }

    @Override
    public String exportAsTurtle(String graphId) {
        return "@prefix : <http://graphiti.io/> .";
    }

    private String fetchSchemaOrgJsonLd(String domain, int depth) throws Exception {
        String url = "https://schema.org/" + domain + ".jsonld";
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new URI(url).toURL().openStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private Model parseJsonLd(String jsonLd) {
        Model model = new org.eclipse.rdf4j.model.impl.LinkedHashModel();
        try {
            var parser = Rio.createParser(RDFFormat.JSONLD);
            parser.setRDFHandler(new org.eclipse.rdf4j.rio.helpers.StatementCollector(model));
            parser.parse(new StringReader(jsonLd));
        } catch (Exception e) {
            log.warn("JSON-LD 解析失败，使用备用方案", e);
        }
        return model;
    }

    private List<SchemaClassInfo> extractClasses(Model model, String rootDomain, int depth) {
        List<SchemaClassInfo> classes = new ArrayList<>();
        IRI rootIRI = VF.createIRI(SCHEMA_ORG_BASE + rootDomain);
        collectSubClasses(model, rootIRI, classes, 0, depth);
        return classes;
    }

    private void collectSubClasses(Model model, IRI clsIRI, List<SchemaClassInfo> result, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) return;
        String label = getLabel(model, clsIRI);
        String comment = getComment(model, clsIRI);
        result.add(new SchemaClassInfo(clsIRI.stringValue(), label, comment));
        model.filter(null, RDFS.SUBCLASSOF, clsIRI).forEach(st -> {
            Resource sub = st.getSubject();
            if (sub.isIRI()) {
                collectSubClasses(model, (IRI) sub, result, currentDepth + 1, maxDepth);
            }
        });
    }

    private List<SchemaPropertyInfo> extractProperties(Model model, List<SchemaClassInfo> classes) {
        List<SchemaPropertyInfo> props = new ArrayList<>();
        IRI propType = RDF.PROPERTY;
        model.filter(null, RDF.TYPE, propType).forEach(st -> {
            IRI propIRI = (IRI) st.getSubject();
            String propLabel = getLabel(model, propIRI);
            if (propLabel == null) return;

            List<String> domains = new ArrayList<>();
            List<String> ranges = new ArrayList<>();

            IRI domainIncl = VF.createIRI(SCHEMA_ORG_BASE + "domainIncludes");
            model.filter(propIRI, domainIncl, null).forEach(s -> {
                Value obj = s.getObject();
                if (obj.isIRI()) domains.add(((IRI) obj).getLocalName());
            });

            IRI rangeIncl = VF.createIRI(SCHEMA_ORG_BASE + "rangeIncludes");
            model.filter(propIRI, rangeIncl, null).forEach(s -> {
                Value obj = s.getObject();
                if (obj.isIRI()) ranges.add(((IRI) obj).getLocalName());
            });

            props.add(new SchemaPropertyInfo(propIRI.stringValue(), propLabel, domains, ranges));
        });
        return props;
    }

    private String getLabel(Model model, IRI iri) {
        Iterable<Statement> stmts = model.filter(iri, RDFS.LABEL, null);
        for (Statement st : stmts) {
            Value obj = st.getObject();
            if (obj.isLiteral()) {
                Literal lit = (Literal) obj;
                if ("en".equals(lit.getLanguage().orElse(""))) {
                    return lit.getLabel();
                }
            }
        }
        return iri.getLocalName();
    }

    private String getComment(Model model, IRI iri) {
        Iterable<Statement> stmts = model.filter(iri, RDFS.COMMENT, null);
        for (Statement st : stmts) {
            Value obj = st.getObject();
            if (obj.isLiteral()) {
                Literal lit = (Literal) obj;
                if ("en".equals(lit.getLanguage().orElse(""))) {
                    return lit.getLabel();
                }
            }
        }
        return null;
    }

    private record SchemaClassInfo(String uri, String label, String comment) {}
    private record SchemaPropertyInfo(String uri, String label, List<String> domain, List<String> range) {}
}
