# OntoGraph

<p align="center">
  <strong>Where ontology becomes living structure.</strong><br>
  <em>A production-ready knowledge graph system for ontology modeling and semantic relationships, powered by Java, Neo4j, and LLMs.</em>
</p>

<p align="center">
  <a href="README_CN.md">🇨🇳 中文文档</a> •
  <a href="README.md">🇺🇸 English</a> •
  <a href="docs/manual/快速开始.md">📖 User Guide</a>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#api-documentation">API</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#documentation">Documentation</a>
</p>

---

## Overview

OntoGraph is a production-ready knowledge graph backend system that brings the power of temporal knowledge graphs to the Java ecosystem. It automatically extracts entities and relationships from unstructured text using Large Language Models (LLM), stores them in Neo4j with vector embeddings, and provides advanced hybrid search capabilities combining full-text, semantic, and graph traversal.

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
│                              ontograph-web                                   │
│                    Vue 3 + Vite + Ant Design Vue                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ontograph-server                                │
│                        Spring Boot 3.5.5 (Entry)                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            ▼                         ▼                         ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│ ontograph-module-core│  │ontograph-module-sys │   │ ontograph-framework  │
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
ontograph-java/
├── ontograph-server/              # Spring Boot entry point
│   └── src/main/resources/
│       ├── application.yml       # Base configuration
│       └── application-dev.yml   # Development profile
│
├── ontograph-module-core/         # Core business module
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
├── ontograph-module-system/       # System management module
│   └── User/Role/Menu/Auth controllers & services
│
├── ontograph-framework/           # Framework infrastructure
│   ├── graphiti-common/          # Common utilities & exceptions
│   ├── graphiti-spring-boot-starter-security/  # JWT security
│   ├── graphiti-spring-boot-starter-mybatis/   # MyBatis config
│   └── graphiti-spring-boot-starter-redis/     # Redis config
│
├── ontograph-web/                 # Frontend (Vue 3)
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
cd ontograph-java
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

Edit `ontograph-server/src/main/resources/application-dev.yml`:

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

ontograph:
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
cd ontograph-server
mvn spring-boot:run
```

The backend will start at `http://localhost:8080`.
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 5. Run Frontend

```bash
cd ontograph-web
pnpm install
pnpm dev
```

The frontend will start at `http://localhost:5173`.

---

## Docker Deployment

### Quick Start with Docker Compose

The easiest way to run the complete OntoGraph service stack is using Docker Compose:

```bash
# 1. Clone the repository
git clone <repository-url>
cd ontograph-java

# 2. Configure environment variables
cp .env.example .env
# Edit .env file and fill in your actual values (see Configuration section below)

# 3. Start all services
docker-compose up -d

# 4. View logs
docker-compose logs -f ontograph-java

# 5. Stop services
docker-compose down
```

After startup, services will be available at:
- **Backend API**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **PostgreSQL**: `localhost:5432`
- **Redis**: `localhost:6379`

### Docker Compose Service Stack

The `docker-compose.yml` includes the following services:

| Service | Port | Description |
|---------|------|-------------|
| ontograph-java | 8080 | Spring Boot application |
| postgres | 5432 | PostgreSQL 16 (metadata & system data) |
| redis | 6379 | Redis 7 (cache & session) |

> **Note**: Neo4j is not included in docker-compose.yml as it's typically deployed separately. Configure the `NEO4J_URI` environment variable to point to your Neo4j instance.

### Environment Variables Configuration

Create a `.env` file from the template:

```bash
cp .env.example .env
```

Key environment variables to configure:

#### Database Configuration
```bash
# PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password_here

# Neo4j (external service)
NEO4J_URI=bolt://localhost:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=your_neo4j_password
```

#### LLM Provider Configuration
```bash
# Select LLM provider: openai | qwen | ollama | anthropic | mistral
GRAPHTI_AI_LLM_PROVIDER=openai
GRAPHTI_AI_EMBEDDING_PROVIDER=openai

# OpenAI configuration
SPRING_AI_OPENAI_API_KEY=your_openai_api_key_here
SPRING_AI_OPENAI_BASE_URL=  # Leave empty for official API, or set for private deployment

# Ollama local deployment (optional)
# SPRING_AI_OLLAMA_BASE_URL=http://host.docker.internal:11434
```

#### JWT Security
```bash
JWT_SECRET=your_secure_jwt_secret_here_min_512_bits
JWT_EXPIRATION=86400
```

#### Logging
```bash
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_GRAPHTI=DEBUG
```

### Database Initialization

After starting the services, initialize the databases:

```bash
# PostgreSQL initialization
docker exec -i graphiti-postgres psql -U postgres -d graphiti < sql/postgresql/schema.sql
docker exec -i graphiti-postgres psql -U postgres -d graphiti < sql/postgresql/init-data.sql

# Neo4j initialization (connect to your Neo4j instance)
# Execute: sql/neo4j/init.cypher
```

### Common Issues & Troubleshooting

#### 1. Port Conflicts

If ports 8080, 5432, or 6379 are already in use, modify the port mappings in `docker-compose.yml`:

```yaml
ports:
  - "8081:8080"  # Change host port to 8081
```

#### 2. Neo4j Connection Issues

Ensure Neo4j is accessible from the Docker container:

- **Local Neo4j**: Use `NEO4J_URI=bolt://host.docker.internal:7687` (Docker Desktop)
- **Remote Neo4j**: Use `NEO4J_URI=bolt://your-neo4j-host:7687`

#### 3. LLM API Connection Issues

- Check that your API key is correct in `.env`
- For private deployments, ensure the `BASE_URL` is accessible from the container
- Use `host.docker.internal` instead of `localhost` for services on the host machine

#### 4. Container Health Check Failures

```bash
# Check container status
docker-compose ps

# View detailed logs
docker-compose logs ontograph-java

# Restart specific service
docker-compose restart ontograph-java
```

#### 5. Database Connection Refused

PostgreSQL may not be ready when the application starts. The docker-compose.yml includes health checks, but if issues persist:

```bash
# Wait for PostgreSQL to be ready
docker-compose up -d postgres
sleep 10
docker-compose up -d ontograph-java
```

#### 6. Data Persistence

Data is persisted to the `./data` directory:
- `./data/postgres` - PostgreSQL data
- `./data/redis` - Redis data

To reset all data:
```bash
docker-compose down
rm -rf ./data
```

### Production Deployment

For production environments:

1. **Use production profile**:
   ```bash
   SPRING_PROFILES_ACTIVE=prod docker-compose up -d
   ```

2. **Configure proper secrets**:
   - Use strong passwords for PostgreSQL and Neo4j
   - Set a secure JWT secret (minimum 512 bits)
   - Never commit `.env` file to version control

3. **Resource limits** (add to docker-compose.yml):
   ```yaml
   services:
     ontograph-java:
       deploy:
         resources:
           limits:
             memory: 2G
             cpus: '2.0'
   ```

4. **Use Docker secrets** for sensitive data instead of environment variables

5. **Enable HTTPS** with a reverse proxy (Nginx/Traefik)

### Custom Docker Build

To build a custom Docker image:

```bash
# Build image
docker build -t ontograph-java:latest -f docker/Dockerfile .

# Run container
docker run -d \
  --name ontograph-java \
  -p 8080:8080 \
  --env-file .env \
  ontograph-java:latest
```

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

## Documentation

### Quick Start Guide

- **[User Guide (Quick Start)](docs/manual/快速开始.md)** - Complete installation and setup guide

### Design & Architecture

- [Project Overview (docs/01-项目概述.md)](docs/01-项目概述.md)
- [Java vs Python Comparison (docs/ontograph-java-vs-python-comparison.md)](docs/ontograph-java-vs-python-comparison.md)
- [Implementation Summary (docs/implementation-summary.md)](docs/implementation-summary.md)
- [Design Document (DESIGN.md)](DESIGN.md)

### AI & Memory

- [AI Chat Memory System (docs/ai-chat-memory.md)](docs/ai-chat-memory.md)

### Ontology & Data Pipeline

- [Ontology System (docs/ontology.md)](docs/ontology.md)
- [Data Pipeline (docs/pipeline.md)](docs/pipeline.md)
- [Legal Ontology Migration Guide (docs/legal-ontology-migration-guide.md)](docs/legal-ontology-migration-guide.md)

### Database

- [Database Migration Guide (docs/database-migration-guide.md)](docs/database-migration-guide.md)

### Planning & Specs

- [Backend Implementation Plan (docs/superpowers/plans/2026-05-08-graphiti-backend-implementation.md)](docs/superpowers/plans/2026-05-08-graphiti-backend-implementation.md)
- [Console Implementation Plan (docs/superpowers/plans/2026-05-08-graphiti-console-implementation.md)](docs/superpowers/plans/2026-05-08-graphiti-console-implementation.md)
- [MySQL to PostgreSQL Migration (docs/superpowers/plans/2026-05-08-mysql-to-postgresql-migration.md)](docs/superpowers/plans/2026-05-08-mysql-to-postgresql-migration.md)
- [Full Alignment Plan (docs/superpowers/plans/2026-05-10-ontograph-java-full-alignment-plan.md)](docs/superpowers/plans/2026-05-10-ontograph-java-full-alignment-plan.md)
- [Full Migration Plan (docs/superpowers/plans/2026-05-11-ontograph-java-full-migration-plan.md)](docs/superpowers/plans/2026-05-11-ontograph-java-full-migration-plan.md)
- [Ontology Phase 1 (docs/superpowers/plans/2026-05-10-ontology-phase1-schema-enforcement.md)](docs/superpowers/plans/2026-05-10-ontology-phase1-schema-enforcement.md)
- [Ontology Phase 2-4 (docs/superpowers/plans/2026-05-10-ontology-phase2-4-remaining.md)](docs/superpowers/plans/2026-05-10-ontology-phase2-4-remaining.md)
- [Legal Ontology Design (docs/superpowers/plans/2026-05-11-legal-ontology-design.md)](docs/superpowers/plans/2026-05-11-legal-ontology-design.md)
- [Neo4j Relation Type Consistency (docs/superpowers/plans/2026-05-11-neo4j-relation-type-consistency.md)](docs/superpowers/plans/2026-05-11-neo4j-relation-type-consistency.md)
- [Console Design Spec (docs/superpowers/specs/2026-05-08-graphiti-console-design.md)](docs/superpowers/specs/2026-05-08-graphiti-console-design.md)
- [Full Alignment Design Spec (docs/superpowers/specs/2026-05-10-ontograph-java-full-alignment-design.md)](docs/superpowers/specs/2026-05-10-ontograph-java-full-alignment-design.md)
- [Ontology Enhancement Design Spec (docs/superpowers/specs/2026-05-10-ontology-enhancement-design.md)](docs/superpowers/specs/2026-05-10-ontology-enhancement-design.md)
- [Backend API Implementation Design Spec (docs/superpowers/specs/2026-05-11-backend-api-impl-design.md)](docs/superpowers/specs/2026-05-11-backend-api-impl-design.md)
- [Full Migration Design Spec (docs/superpowers/specs/2026-05-11-ontograph-java-full-migration-design.md)](docs/superpowers/specs/2026-05-11-ontograph-java-full-migration-design.md)
- [AI Chat Memory Design Spec (docs/superpowers/specs/2026-05-13-ai-chat-memory-design.md)](docs/superpowers/specs/2026-05-13-ai-chat-memory-design.md)

---

## Contributing

We welcome contributions to OntoGraph! Please follow these guidelines:

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

OntoGraph (原 Graphiti) 灵感来源于 Zep AI 的原始 [Graphiti](https://github.com/getzep/graphiti) Python 库。

---

<p align="center">
  <sub>Built with ❤️ for the knowledge graph community</sub>
</p>
