# Ontology and Data Import Pipeline

## 1. System Architecture: Dual Storage

The graphiti-java system uses a dual-storage architecture:

```
MySQL (Metadata)                          Neo4j (Graph Data)
─────────────────────                    ────────────────────────────────────────
graphiti_graph_metadata                   Entity 节点 (type -> ontClassId)
graphiti_ontology                         Episode 节点 (原始数据容器)
ont_definition                            RELATES_TO 边 (fact -> embedding)
ont_class                                 ───────────────────────────────────────
ont_property                              groupId 隔离，同一 graphId 下的数据
ont_constraint
ont_version_history                       Embedding 向量存储 (用于语义检索)
```

- **MySQL**: Stores graph metadata, ontology definitions, version history, and system configuration
- **Neo4j**: Stores actual graph data (nodes, edges, episodes) with vector embeddings for semantic search

The two stores are linked via the `graphId` field present in both systems.

---

## 2. Ontology Data Model

### 2.1 Core Tables (MySQL)

#### `graphiti_graph_metadata` — Graph Metadata

Maps to `GraphMetadataDO`. Represents a knowledge graph instance.

| Field | Type | Description |
|-------|------|-------------|
| `graph_id` | VARCHAR(36) | Primary key, UUID |
| `name` | VARCHAR(255) | Graph name |
| `description` | TEXT | Graph description |
| `node_count` | INT | Current node count |
| `edge_count` | INT | Current edge count |
| `ont_version_id` | BIGINT | FK to `ont_definition.id` |

#### `graphiti_ontology` — Simple Ontology (JSON-based)

Maps to `OntologyDO`. A lightweight, JSON-based ontology with entities/edges as JSON arrays.

| Field | Type | Description |
|-------|------|-------------|
| `graph_id` | VARCHAR(36) | FK to `graphiti_graph_metadata.graph_id` |
| `entities` | JSON | Entity type definitions as JSON array |
| `edges` | JSON | Relation type definitions as JSON array |

#### `ont_definition` — Versioned Ontology Container

Maps to `OntDefinitionDO`. Groups ontology classes and properties into a versioned namespace.

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key, auto-increment |
| `graph_id` | VARCHAR(36) | FK to graph metadata |
| `namespace` | VARCHAR(512) | Ontology namespace URI |
| `version` | VARCHAR(50) | Semantic version (e.g., `1.0.0`) |
| `status` | ENUM | DRAFT / ACTIVE / DEPRECATED / ARCHIVED |
| `created_at` | DATETIME | Creation timestamp |
| `updated_at` | DATETIME | Last update timestamp |

#### `ont_class` — Class Definitions

Maps to `OntClassDO`. Represents an ontology class (equivalent to an OWL Class).

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `definition_id` | BIGINT | FK to `ont_definition.id` |
| `class_uri` | VARCHAR(512) | Full URI of the class |
| `local_name` | VARCHAR(255) | Short name used in Neo4j `type` field |
| `description` | TEXT | Class description |
| `parent_class_id` | BIGINT | FK to `ont_class.id` for inheritance |
| `equivalent_to` | TEXT | OWL equivalentTo expression |
| `disjoint_with` | TEXT | OWL disjointWith class URIs |
| `created_at` | DATETIME | Creation timestamp |

#### `ont_property` — Property Definitions

Maps to `OntPropertyDO`. Represents an ontology property (equivalent to an OWL Property).

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `definition_id` | BIGINT | FK to `ont_definition.id` |
| `property_uri` | VARCHAR(512) | Full URI of the property |
| `local_name` | VARCHAR(255) | Short name |
| `property_type` | ENUM | OBJECT / DATATYPE / ANNOTATION |
| `domain_class_id` | BIGINT | FK to `ont_class.id` (subject class) |
| `range_class_id` | BIGINT | FK to `ont_class.id` (object class, for OBJECT type) |
| `range_data_type` | VARCHAR(100) | Data type for DATATYPE properties |
| `is_required` | BOOLEAN | Whether the property is mandatory |
| `is_inherited` | BOOLEAN | Whether the property is inherited from parent class |
| `cardinality` | INT | Min/max cardinality |
| `inverse_of` | BIGINT | FK to `ont_property.id` (inverse property) |
| `created_at` | DATETIME | Creation timestamp |

#### `ont_constraint` — Constraint Rules

Maps to `OntConstraintDO`. Defines validation constraints per class or property.

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `class_id` | BIGINT | FK to `ont_class.id` (nullable) |
| `property_id` | BIGINT | FK to `ont_property.id` (nullable) |
| `constraint_type` | ENUM | CARDINALITY / PATTERN / RANGE / ENUM / NOT_NULL |
| `constraint_value` | TEXT | Constraint value (e.g., regex, min/max, enum list) |

Supported constraint types:

| Type | Value Format | Description |
|------|-------------|-------------|
| CARDINALITY | `min:max` (e.g., `1:10`) | Number of property values must be in range |
| PATTERN | Java regex | Value must match the regex |
| RANGE | `min:max` | Numeric value must be in range |
| ENUM | comma-separated values | Value must be one of the listed options |
| NOT_NULL | `true` | Property value cannot be null |

#### `ont_version_history` — Version History

Maps to `OntVersionHistoryDO`. Tracks all changes to an ontology definition.

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `definition_id` | BIGINT | FK to `ont_definition.id` |
| `version` | VARCHAR(50) | Version that was applied |
| `change_type` | ENUM | CREATE / UPDATE / ACTIVATE / DEPRECATE / ARCHIVE |
| `changed_by` | VARCHAR(100) | User or system that made the change |
| `change_summary` | TEXT | Human-readable change description |
| `created_at` | DATETIME | When the change occurred |

---

## 3. Data Import Pipeline

### Pipeline 1: Schema.org Ontology Import

Imports standard ontologies from schema.org into the MySQL ontology tables.

```
schema.org CDN (JSON-LD)
        │
        ▼
SchemaOrgImportServiceImpl
        │
        ├── HTTP GET https://schema.org/version/latest/schemaorg-all-http.rdf
        │
        ├── Parse RDF using Eclipse RDF4J (Rio Parser)
        │
        ├── Extract Classes ──► OntClassDO
        │       - Recursive parent traversal (N levels)
        │       - parentClassId linked to parent OntClassDO
        │
        ├── Extract Properties ──► OntPropertyDO
        │       - domainIncludes ──► domainClassId
        │       - rangeIncludes ──► rangeClassId / rangeDataType
        │
        └── Persist to ont_definition / ont_class / ont_property
```

Key files:
- `graphiti-module-core/.../service/SchemaOrgImportService.java`
- `graphiti-module-core/.../service/impl/SchemaOrgImportServiceImpl.java`
- `graphiti-module-core/.../controller/admin/OntologyController.java` (REST endpoint: `POST /api/v1/ontology/{graphId}/import/schemaorg`)

**Note**: The current `SchemaOrgImportServiceImpl` implementation parses the RDF but does not yet persist to the MySQL tables (marked TODO). It logs the extracted schema information.

### Pipeline 2: Node/Edge Creation with Ontology Validation

Full pipeline with 4-layer validation before writing to Neo4j.

```
REST: NodeServiceImpl.createNode() / EdgeServiceImpl.createEdge()
        │
        ▼
L1: Type Existence Check
        │  Query ont_class by graphId + localName
        │  If not found -> OntologyValidationException
        ▼
L2: Required Properties Check
        │  Query ont_property where isRequired=true AND domainClassId=classId
        │  If any required property missing -> OntologyValidationException
        ▼
L3: Data Type Check
        │  For each property, verify value type matches rangeDataType
        │  If type mismatch -> OntologyValidationException
        ▼
L4: Constraint Validation
        │  Query ont_constraint for this class/property
        │  Evaluate CARDINALITY / PATTERN / RANGE / ENUM / NOT_NULL
        │  If constraint violated -> OntologyValidationException
        ▼
Generate UUID + Timestamp
        │
        ▼
EmbedderService.embed(text)
        │  Embed name + summary into a vector
        │  Supports: Ollama, OpenAI, Qwen embedders (configurable)
        ▼
GraphNeo4jService (Cypher execution)
        │
        ├── createEntityNode()  -> CREATE (n:Entity {...})
        └── createRelationship() -> CREATE (a)-[r:RELATES_TO]->(b)
        │
        ▼
GraphMetadataService.incNodeCount() / incEdgeCount()
```

Key files:
- `graphiti-module-core/.../service/impl/NodeServiceImpl.java`
- `graphiti-module-core/.../service/impl/EdgeServiceImpl.java`
- `graphiti-module-core/.../service/impl/OntologyValidationServiceImpl.java` (L1-L4 validator)
- `graphiti-module-core/.../service/impl/GraphNeo4jService.java` (Cypher execution)

### Pipeline 3: Raw Data Import

Lightweight import for raw data (documents, messages) without LLM extraction.

```
DataImportController REST endpoints
        │
        ├── POST /admin/graphiti/data/add
        │       └── DataImportService.addData() -> Episode 节点
        │
        ├── POST /admin/graphiti/data/batch
        │       └── DataImportService.addDataBatch() -> Multiple Episodes
        │
        ├── POST /admin/graphiti/data/messages
        │       └── DataImportService.addMessages() -> Episodes from message list
        │
        ├── POST /admin/graphiti/data/fact-triple
        │       └── DataImportService.addFactTriple() -> Entity + RELATES_TO edge
        │
        └── POST /admin/graphiti/data/entity-node
                └── DataImportService.addEntityNode() -> Direct Entity node
```

Key files:
- `graphiti-module-core/.../controller/admin/DataImportController.java`
- `graphiti-module-core/.../service/impl/DataImportServiceImpl.java`

**Note**: `DataImportServiceImpl` is a simplified implementation. LLM-based entity extraction is marked TODO — raw data is currently stored as-is in Episode nodes.

---

## 4. Graph-Ontology Association Mechanism

The association between Neo4j graph data and MySQL ontology definitions is established through three layers:

```
Layer 1: graphId 关联
Layer 2: ont_definition 版本容器
Layer 3: Neo4j type 字段
```

### Layer 1: graphId Association

Both the Neo4j graph and MySQL ontology tables share the `graphId` field as the primary grouping key:

```
graphiti_graph_metadata.graphId
        │
        ├── graphiti_ontology.graphId  (same value)
        ├── ont_definition.graphId     (same value)
        └── Neo4j Entity.groupId       (same value)
```

### Layer 2: ont_definition Version Container

The `ont_definition` table acts as a versioned namespace that groups all ontology classes and properties:

```
graphiti_graph_metadata.graphId
        │
        ▼
ont_definition.graphId  ──► version / status
        │
        ├── ont_class.definitionId  ──► class definitions
        │       └── ont_property.definitionId  ──► property definitions
        │
        └── graphiti_graph_metadata.ontVersionId  (active version FK)
```

### Layer 3: Neo4j Node type Field

Nodes in Neo4j store the ontology class name as the `type` property:

```cypher
CREATE (n:Entity {
    group_id: "uuid-of-graph",    -- same graphId
    uuid: "uuid-of-node",
    name: "Alice",
    type: "Person",               -- maps to ont_class.local_name
    summary: "A software engineer",
    embedding: [0.1, 0.2, ...],  -- vector from EmbedderService
    valid_at: 1715404800000,
    invalid_at: null
})
```

The `NodeServiceImpl` resolves this `type` field against `ont_class.local_name` during validation.

---

## 5. Neo4j Graph Data Model

### Node Labels

All graph nodes share a base label and optional type labels:

| Label | Description | Properties |
|-------|-------------|------------|
| `:Entity` | All entity nodes | groupId, uuid, name, type, summary, embedding, validAt, invalidAt |
| `:Episode` | Raw data containers | groupId, uuid, name, source, content, createdAt, validAt, processed |
| `:Community` | Community detection results | groupId, uuid, name, nodeCount |

### Relationship Types

| Type | Description | Properties |
|------|-------------|------------|
| `RELATES_TO` | Edges between entities | uuid, type, fact, embedding, validAt, invalidAt, groupId |
| `MENTIONS` | Episode references entity | uuid, groupId |
| `IN_COMMUNITY` | Entity belongs to community | uuid, groupId |

### Temporal Versioning

All nodes and edges use a bi-temporal design:

```
valid_at:    Timestamp when this fact became true
invalid_at:  Timestamp when this fact became false (null = currently true)
```

- **Insert**: Write with `valid_at = NOW()`, `invalid_at = null`
- **Update**: Set `invalid_at = NOW()` on old record, insert new record with new `valid_at`
- **Query valid state at time T**: `WHERE valid_at <= T AND (invalid_at > T OR invalid_at IS NULL)`

### Indexes

Configured in `GraphNeo4jConfig` / `GraphNeo4jService.initVectorIndexes()`:

| Index Type | Fields | Purpose |
|------------|--------|---------|
| Vector | `Entity.embedding` | Semantic search ( cosine similarity ) |
| Fulltext | `Entity.name` | Keyword search |
| Fulltext | `RELATES_TO.fact` | Edge content search |
| Composite | `Entity.groupId, Entity.type` | Filtered queries |
| Composite | `Entity.groupId, Entity.validAt` | Temporal queries |

---

## 6. Key Service Interfaces

### OntologyService

High-level ontology operations:

```java
public interface OntologyService {
    OntologyVO getOntology(String graphId);
    void setOntology(String graphId, OntologyDTO ontology);
    List<OntClassVO> listClasses(String graphId, Long definitionId);
    void createClass(String graphId, OntClassDTO ontClass);
    void updateClass(String graphId, Long classId, OntClassDTO ontClass);
    void deleteClass(String graphId, Long classId);
    List<OntPropertyVO> listProperties(String graphId, Long definitionId);
    void createProperty(String graphId, OntPropertyDTO ontProperty);
    void updateProperty(String graphId, Long propertyId, OntPropertyDTO ontProperty);
    void deleteProperty(String graphId, Long propertyId);
    void importFromSchemaOrg(String graphId, SchemaOrgImportDTO importDTO);
    void warmUpReasoner(String graphId);
}
```

### OntologyValidationService

4-layer validation engine:

```java
public interface OntologyValidationService {
    void validateNode(String graphId, String nodeType, Map<String, Object> properties);
    void validateEdge(String graphId, String sourceNodeUuid, String targetNodeUuid,
                      String edgeType, Map<String, Object> properties);
    boolean hasOntology(String graphId);
}
```

### OntologyReasoner

OWL 2 RL inference:

```java
public interface OntologyReasoner {
    void initialize(String graphId);
    List<InferenceResult> infer(String graphId, String sourceNodeUri, String targetNodeUri);
    boolean isEntailed(String graphId, String triple);
    Set<String> getSubClasses(String graphId, String classUri);
    Set<String> getSuperClasses(String graphId, String classUri);
}
```

### EmbedderService

Text vectorization (pluggable implementation):

```java
public interface EmbedderService {
    float[] embed(String text);
    String getModelName();
}
```

Supported implementations:
- `OllamaEmbedderServiceImpl` — Local Ollama server
- `OpenAiEmbedderServiceImpl` — OpenAI text-embedding-3-small/large
- `QwenEmbedderServiceImpl` — Alibaba Qwen embedding API

---

## 7. REST API Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/ontology/{graphId}` | GET | Get ontology |
| `/api/v1/ontology/{graphId}` | PUT | Set ontology |
| `/api/v1/ontology/{graphId}/classes` | GET/POST | List/create classes |
| `/api/v1/ontology/{graphId}/classes/{classId}` | PUT/DELETE | Update/delete class |
| `/api/v1/ontology/{graphId}/properties` | GET/POST | List/create properties |
| `/api/v1/ontology/{graphId}/properties/{propertyId}` | PUT/DELETE | Update/delete property |
| `/api/v1/ontology/{graphId}/import/schemaorg` | POST | Import from Schema.org |
| `/api/v1/ontology/{graphId}/reasoner/warmup` | POST | Warm up OWL reasoner |
| `/admin/graphiti/data/add` | POST | Import single data record |
| `/admin/graphiti/data/batch` | POST | Batch import |
| `/admin/graphiti/data/messages` | POST | Import message list |
| `/admin/graphiti/data/fact-triple` | POST | Import fact triple |
| `/admin/graphiti/data/entity-node` | POST | Import entity node directly |
| `/api/v1/graph/{graphId}/nodes` | GET/POST | List/create nodes |
| `/api/v1/graph/{graphId}/nodes/{nodeId}` | GET/PUT/DELETE | Node CRUD |
| `/api/v1/graph/{graphId}/edges` | GET/POST | List/create edges |
| `/api/v1/graph/{graphId}/edges/{edgeId}` | GET/PUT/DELETE | Edge CRUD |
