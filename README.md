# Graphiti-Java

<p align="center">
  <strong>Temporal Knowledge Graph Backend System</strong><br>
  <em>Java implementation of the Graphiti knowledge graph with LLM-powered entity extraction, hybrid search, and temporal fact management.</em>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#api-documentation">API</a> •
  <a href="#configuration">Configuration</a>
</p>

---

## Overview

Graphiti-Java is a production-ready knowledge graph backend system that brings the power of temporal knowledge graphs to the Java ecosystem. It automatically extracts entities and relationships from unstructured text using Large Language Models (LLM), stores them in Neo4j with vector embeddings, and provides advanced hybrid search capabilities combining full-text, semantic, and graph traversal.

### Key Capabilities

- **LLM-Powered Data Ingestion**: Automatically extract entities and relationships from text, conversations, and documents
- **Temporal Fact Management**: Track facts over time with `valid_at`/`invalid_at` timestamps; auto-invalidate outdated facts
- **Hybrid Search**: Combine BM25 full-text, vector similarity, and BFS graph traversal with RRF fusion and MMR re-ranking
- **Multi-Provider LLM**: Support OpenAI, Anthropic Claude, Alibaba Qwen, Ollama, and private deployments via custom base URLs
- **Ontology Validation**: 6-layer validation engine with class inheritance, Domain/Range constraints, and pattern matching
- **Community Detection**: Label propagation algorithm with LLM-generated community summaries
- **Data Quality**: Automatic deduplication of nodes and edges, entity resolution

---

## Features

### Knowledge Graph Core

| Feature | Description | Status |
|---------|-------------|--------|
| Graph Lifecycle | Create, read, update, delete, clone, export graphs | ✅ |
| Node Management | CRUD operations with embedding vectors and ontology validation | ✅ |
| Edge Management | Relationship CRUD with custom relation types and fact tracking | ✅ |
| Episode Management | Temporal episodes with content and source tracking | ✅ |
| Ontology System | Class hierarchy, property constraints, 6-layer validation | ✅ |

### AI & Search

| Feature | Description | Status |
|---------|-------------|--------|
| LLM Entity Extraction | Extract entities/relations from text via Spring AI | ✅ |
| LLM Relation Extraction | Relationship extraction with factual statements | ✅ |
| Embedding Generation | Text-to-vector via OpenAI/Ollama embedding models | ✅ |
| Vector Index | Neo4j 5.x vector index for semantic search | ✅ |
| Hybrid Search | BM25 + Vector + BFS with RRF fusion | ✅ |
| MMR Re-ranking | Maximal Marginal Relevance for diversity | ✅ |
| Community Detection | Label propagation + LLM summary generation | ✅ |

### Data Quality & Temporal

| Feature | Description | Status |
|---------|-------------|--------|
| Temporal Management | `valid_at`/`invalid_at` with auto-invalidation | ✅ |
| Saga Management | Episode chaining via `NEXT_EPISODE` relationships | ✅ |
| Node Deduplication | Jaccard similarity-based merging | ✅ |
| Edge Deduplication | Duplicate relationship detection and removal | ✅ |
| Entity Resolution | `SAME_AS` relationships for name variations | ✅ |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              graphiti-web                                    │
│                    Vue 3 + Vite + Ant Design Vue                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              graphiti-server                                 │
│                        Spring Boot 3.5.5 (Entry)                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            ▼                         ▼                         ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│ graphiti-module-core│   │ graphiti-module-sys │   │ graphiti-framework  │
│  Knowledge Graph    │   │  User/Role/Menu/Auth│   │  Common/Security/   │
│  - Graph CRUD       │   │  - JWT Auth         │   │  - MyBatis Starter  │
│  - Search/Import    │   │  - RBAC             │   │  - Redis Starter    │
│  - Ontology/Community│  │  - Menu Management  │   │                     │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘
            │
    ┌───────┴───────┐
    ▼               ▼
┌─────────┐   ┌──────────┐
│  Neo4j  │   │PostgreSQL│
│(GraphDB)│   │ (Metadata│
│         │   │  & System│
└─────────┘   └──────────┘
    │
    ▼
┌─────────┐
│  Redis  │
│ (Cache) │
└─────────┘
```

### Module Structure

```
graphiti-java/
├── graphiti-server/              # Spring Boot entry point
│   └── src/main/resources/
│       ├── application.yml       # Base configuration
│       └── application-dev.yml   # Development profile
│
├── graphiti-module-core/         # Core business module
│   ├── controller/admin/         # REST controllers
│   ├── service/                  # Business services
│   │   ├── impl/ai/              # LLM provider implementations
│   │   ├── impl/                 # Service implementations
│   │   ├── GraphNeo4jService.java # Neo4j graph operations
│   │   ├── SearchService.java    # Hybrid search
│   │   ├── DataImportService.java # LLM extraction & import
│   │   ├── OntologyValidationService.java # 6-layer validation
│   │   └── CommunityService.java # Community detection
│   ├── dal/
│   │   ├── dataobject/           # MyBatis-Plus entities
│   │   │   └── ont/              # Ontology entities (Class/Property/Constraint)
│   │   └── mysql/                # Mappers
│   ├── dal/neo4j/                # Neo4j repositories
│   │   ├── NodeRepository.java
│   │   ├── EdgeRepository.java
│   │   └── VectorIndexRepository.java
│   ├── vo/                       # View Objects
│   │   ├── llm/                  # LLM extraction VOs
│   │   ├── search/               # Search request/response VOs
│   │   ├── ontology/             # Ontology VOs
│   │   └── imports/              # Data import VOs
│   └── resources/prompts/        # LLM prompt templates
│       ├── extract_entities.txt
│       ├── extract_relations.txt
│       ├── summarize_node.txt
│       └── summarize_community.txt
│
├── graphiti-module-system/       # System management module
│   └── User/Role/Menu/Auth controllers & services
│
├── graphiti-framework/           # Framework infrastructure
│   ├── graphiti-common/          # Common utilities & exceptions
│   ├── graphiti-spring-boot-starter-security/  # JWT security
│   ├── graphiti-spring-boot-starter-mybatis/   # MyBatis config
│   └── graphiti-spring-boot-starter-redis/     # Redis config
│
├── graphiti-web/                 # Frontend (Vue 3)
│   ├── src/api/                  # API client modules
│   ├── src/views/                # Page components
│   └── src/components/           # Reusable components
│
└── sql/                          # Database initialization scripts
    ├── mysql/
    ├── postgresql/
    └── neo4j/
```

---

## Tech Stack

### Backend

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.5 |
| AI Framework | Spring AI | 1.1.2 |
| Data Access | MyBatis-Plus | 3.5.12 |
| Graph Database | Neo4j Java Driver | 5.26.0 |
| Security | Spring Security + JWT | - |
| Cache | Redisson | 3.37.0 |
| Documentation | SpringDoc OpenAPI | 2.8.5 |
| Utilities | Hutool | 5.8.37 |

### Frontend

| Technology | Version |
|-----------|---------|
| Vue | 3.4 |
| Vite | 5.2 |
| Vue Router | 4.3 |
| Pinia | 2.1 |
| Ant Design Vue | 4.2 |
| Axios | 1.7 |
| ECharts | 5.5 |

### Databases

| Database | Purpose | Version |
|----------|---------|---------|
| Neo4j | Knowledge graph storage | 5.26 |
| PostgreSQL | Metadata & system data | 15+ |
| MySQL | Alternative metadata storage | 8.0+ |
| Redis | Session & cache | 6+ |

### Supported LLM Providers

| Provider | Spring AI Starter | Custom Base URL |
|----------|-------------------|-----------------|
| OpenAI | `spring-ai-starter-model-openai` | ✅ |
| Anthropic Claude | `spring-ai-starter-model-anthropic` | ✅ |
| Alibaba Qwen | `spring-ai-starter-model-openai` (compatible) | ✅ |
| Ollama | `spring-ai-starter-model-ollama` | ✅ |
| Mistral AI | `spring-ai-starter-model-mistral-ai` | ✅ |
| Azure OpenAI | `spring-ai-starter-model-azure-openai` | ✅ |
| AWS Bedrock | `spring-ai-starter-model-bedrock` | ✅ |

---

## Quick Start

### Prerequisites

- JDK 21+
- Maven 3.9+
- Neo4j 5.26+
- PostgreSQL 15+ (or MySQL 8.0+)
- Redis 6+
- Node.js 18+ (for frontend)

### 1. Clone & Build

```bash
git clone <repository-url>
cd graphiti-java
mvn clean install -DskipTests
```

### 2. Database Setup

**Neo4j** (create vector indexes):
```bash
# Run the Neo4j initialization script
neo4j-shell -f sql/neo4j/init.cypher
```

**PostgreSQL**:
```bash
psql -U postgres -d graphiti -f sql/postgresql/schema.sql
psql -U postgres -d graphiti -f sql/postgresql/init-data.sql
```

**MySQL** (alternative):
```bash
mysql -u root -p graphiti < sql/mysql/schema.sql
mysql -u root -p graphiti < sql/mysql/init-data.sql
```

### 3. Configure Application

Edit `graphiti-server/src/main/resources/application-dev.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
      base-url: http://your-llm-deployment:8000/v1  # Private deployment
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:postgresql://localhost:5432/graphiti
          username: postgres
          password: your-password
  data:
    redis:
      host: localhost
      port: 6379

graphiti:
  ai:
    llm-provider: openai        # openai | anthropic | qwen | ollama | mistral
    embedding-provider: openai

neo4j:
  uri: bolt://localhost:7687
  username: neo4j
  password: your-neo4j-password
```

### 4. Run Backend

```bash
cd graphiti-server
mvn spring-boot:run
```

The backend will start at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 5. Run Frontend

```bash
cd graphiti-web
pnpm install
pnpm dev
```

The frontend will start at `http://localhost:5173`.

---

## Usage Examples

### Create a Graph

```bash
curl -X POST http://localhost:8080/api/v1/graph \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"name": "Tech Companies", "description": "Technology industry knowledge graph"}'
```

### Add Data with Auto Extraction

```bash
curl -X POST http://localhost:8080/api/v1/graph/data/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "graphId": "your-graph-id",
    "name": "Apple News",
    "content": "Apple Inc. was founded by Steve Jobs and Steve Wozniak in Cupertino, California in 1976.",
    "sourceType": "article"
  }'
```

The system will:
1. Create an Episode node
2. Extract entities: "Apple Inc." (Organization), "Steve Jobs" (Person), "Steve Wozniak" (Person), "Cupertino" (Location)
3. Extract relationships: FOUNDED_BY, LOCATED_IN
4. Create Entity nodes with embeddings
5. Create RELATES_TO edges with fact descriptions

### Hybrid Search

```bash
curl -X POST http://localhost:8080/api/v1/search/hybrid \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "graphId": "your-graph-id",
    "query": "Who founded Apple?",
    "config": {
      "limit": 10,
      "useBM25": true,
      "useVector": true,
      "useBFS": false,
      "reranker": "rrf"
    }
  }'
```

### Define Ontology

```bash
curl -X POST http://localhost:8080/api/v1/graph/your-graph-id/ontology \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "entities": [
      {
        "name": "Person",
        "fields": [
          {"name": "age", "type": "integer", "required": false},
          {"name": "email", "type": "string", "required": false}
        ]
      },
      {
        "name": "Organization",
        "parent": "Entity",
        "fields": [
          {"name": "industry", "type": "string", "required": true},
          {"name": "foundedYear", "type": "integer", "required": false}
        ]
      }
    ],
    "edges": [
      {"name": "FOUNDED_BY", "source": "Organization", "target": "Person"},
      {"name": "WORKS_AT", "source": "Person", "target": "Organization"}
    ]
  }'
```

### Build Communities

```bash
curl -X POST http://localhost:8080/api/v1/graph/your-graph-id/communities/build \
  -H "Authorization: Bearer <token>"
```

---

## API Documentation

### Swagger / OpenAPI

Once the application is running, access the interactive API documentation:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### API Groups

| Group | Base Path | Description |
|-------|-----------|-------------|
| Auth | `/api/v1/auth/**` | Login, register, token refresh |
| User | `/api/v1/users/**` | User management |
| Role | `/api/v1/roles/**` | Role management |
| Menu | `/api/v1/menus/**` | Menu management |
| Graph | `/api/v1/graph/**` | Graph CRUD, clone, export |
| Node | `/api/v1/nodes/**` | Node CRUD |
| Edge | `/api/v1/edges/**` | Edge/relationship CRUD |
| Episode | `/api/v1/episodes/**` | Episode management |
| Ontology | `/api/v1/graph/{graphId}/ontology/**` | Ontology definition & validation |
| Search | `/api/v1/search/**` | Hybrid, semantic, BFS search |
| Data Import | `/api/v1/graph/data/**` | LLM extraction & import |
| Maintenance | `/api/v1/maintenance/**` | Data quality operations |

---

## Configuration

### LLM Provider Selection

Set in `application-dev.yml`:

```yaml
graphiti:
  ai:
    llm-provider: openai      # Switch provider here
    embedding-provider: openai
```

Available providers: `openai`, `anthropic`, `qwen`, `ollama`, `mistral`

### Private Deployment Examples

**OpenAI-compatible (vLLM / LM Studio):**
```yaml
spring:
  ai:
    openai:
      api-key: any-key
      base-url: http://localhost:8000/v1
```

**Anthropic-compatible (LiteLLM proxy):**
```yaml
spring:
  ai:
    anthropic:
      api-key: any-key
      base-url: http://localhost:8080/v1
```

**Ollama:**
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
```

### Vector Index Configuration

Vector indexes are auto-created on startup via `VectorIndexRepository`:

```java
// Node vector index
CREATE VECTOR INDEX node_embedding_index IF NOT EXISTS
FOR (n:Entity) ON (n.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}}

// Edge vector index
CREATE VECTOR INDEX edge_embedding_index IF NOT EXISTS
FOR ()-[r:RELATES_TO]-() ON (r.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}}
```

---

## Contributing

We welcome contributions to Graphiti-Java! Please follow these guidelines:

### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Ensure code compiles: `mvn clean compile`
4. Run tests: `mvn test`
5. Submit a pull request

### Code Style

- Follow Java 21 conventions
- Use Lombok for boilerplate reduction
- Document public APIs with Javadoc
- Add Swagger annotations to controllers

### Commit Message Format

```
feat: add new feature
fix: fix a bug
docs: update documentation
refactor: code refactoring
test: add tests
chore: build/config changes
```

### Architecture Principles

- **Service Layer**: Business logic in `*Service` interfaces, implementations in `impl/`
- **Neo4j Operations**: Centralized in `GraphNeo4jService`
- **LLM Integration**: Provider-specific implementations in `impl/ai/`, unified via `LlmClientService`
- **Ontology Validation**: 6-layer validation in `OntologyValidationServiceImpl`

---

## License

[MIT License](LICENSE)

## Acknowledgements

Graphiti-Java is inspired by the original [Graphiti](https://github.com/getzep/graphiti) Python library by Zep AI.

---

<p align="center">
  <sub>Built with ❤️ for the knowledge graph community</sub>
</p>
