-- ============================================================
-- 本体管理系统 - 数据库表结构 (PostgreSQL)
-- 版本: V5
-- 描述: 完整的本体定义系统，支持类、属性、约束、版本历史
-- 创建时间: 2026-05-12
-- ============================================================

-- ----------------------------------------------------------
-- 1. ont_definition - 本体定义表
-- ----------------------------------------------------------
-- 存储每个图谱的本体定义版本，是整个本体系统的根节点
-- 一个图谱可以有多个版本的本体定义，通过 parent_version_id 形成版本链

CREATE TABLE ont_definition (
    id BIGSERIAL PRIMARY KEY,
    graph_id VARCHAR(64) NOT NULL,
    namespace VARCHAR(255) DEFAULT 'http://legal-ai.cc/ontology',
    name VARCHAR(128) NOT NULL,
    version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    parent_version_id BIGINT,
    created_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ont_def_graph_version UNIQUE (graph_id, version)
);

CREATE INDEX idx_ont_def_graph_id ON ont_definition(graph_id);
CREATE INDEX idx_ont_def_status ON ont_definition(status);

COMMENT ON TABLE ont_definition IS '本体定义表 - 存储每个图谱的本体定义版本信息，是整个本体系统的根入口';
COMMENT ON COLUMN ont_definition.id IS '主键ID，自增序列';
COMMENT ON COLUMN ont_definition.graph_id IS '所属图谱的唯一标识符，对应 graph_metadata 表的 graph_id';
COMMENT ON COLUMN ont_definition.namespace IS '本体命名空间，默认 http://legal-ai.cc/ontology，可自定义扩展';
COMMENT ON COLUMN ont_definition.name IS '本体名称，如"法律知识图谱本体"、"医疗本体"等';
COMMENT ON COLUMN ont_definition.version IS '语义化版本号，格式为 major.minor.patch，如 1.0.0、2.1.3';
COMMENT ON COLUMN ont_definition.status IS '本体状态: ACTIVE-正在使用、DEPRECATED-已弃用待删除、ARCHIVED-已归档不显示';
COMMENT ON COLUMN ont_definition.description IS '本体的详细描述，包括设计目的、适用范围、使用场景等';
COMMENT ON COLUMN ont_definition.parent_version_id IS '父版本ID，指向前一个版本，用于构建版本历史链';
COMMENT ON COLUMN ont_definition.created_by IS '创建该版本的用户ID，用于审计追溯';
COMMENT ON COLUMN ont_definition.created_at IS '本体定义创建时间';
COMMENT ON COLUMN ont_definition.updated_at IS '本体定义最后修改时间';


-- ----------------------------------------------------------
-- 2. ont_class - 本体类表
-- ----------------------------------------------------------
-- 定义本体中的类（Class），对应知识图谱中的实体类型
-- 类可以形成继承关系（parent_class_id），支持单继承
-- 类之间可以声明互斥关系（disjoint_with）

CREATE TABLE ont_class (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    class_uri VARCHAR(512) NOT NULL,
    local_name VARCHAR(128) NOT NULL,
    parent_class_id BIGINT,
    equivalent_to TEXT,
    disjoint_with TEXT,
    description TEXT,
    example TEXT,
    domain_hint VARCHAR(32),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ont_class_uri UNIQUE (definition_id, class_uri),
    CONSTRAINT uk_ont_class_local_name UNIQUE (definition_id, local_name),
    CONSTRAINT fk_ont_class_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_class_parent FOREIGN KEY (parent_class_id) REFERENCES ont_class(id) ON DELETE SET NULL
);

CREATE INDEX idx_ont_class_definition ON ont_class(definition_id);
CREATE INDEX idx_ont_class_parent ON ont_class(parent_class_id);
CREATE INDEX idx_ont_class_domain ON ont_class(domain_hint);

COMMENT ON TABLE ont_class IS '本体类表 - 定义知识图谱中的实体类型，如 Person(人)、Company(公司)、Case(案件)等';
COMMENT ON COLUMN ont_class.id IS '主键ID，自增序列';
COMMENT ON COLUMN ont_class.definition_id IS '所属本体定义的ID，同一个类只能属于一个本体定义';
COMMENT ON COLUMN ont_class.class_uri IS '类的完整 URI，由命名空间+本地名组成，保证全局唯一性';
COMMENT ON COLUMN ont_class.local_name IS '类的本地名称，在同一本体内唯一，如 Person、Invoice、Judgment';
COMMENT ON COLUMN ont_class.parent_class_id IS '父类ID，支持单继承，形成类层次树结构，如 Vehicle -> Car -> SportsCar';
COMMENT ON COLUMN ont_class.equivalent_to IS '等价类列表，JSON数组，表示与这些类在语义上等价，可用于知识融合';
COMMENT ON COLUMN ont_class.disjoint_with IS '互斥类列表，JSON数组，声明这些类不能有共同实例，如 Person 与 Organization';
COMMENT ON COLUMN ont_class.description IS '类的详细描述文档，帮助用户理解类的含义和使用方式';
COMMENT ON COLUMN ont_class.example IS '类的使用示例，展示该类典型实例的属性值';
COMMENT ON COLUMN ont_class.domain_hint IS '领域分类标记，用于分类检索和智能推荐: 金融/医疗/电商/通用/法律';
COMMENT ON COLUMN ont_class.metadata IS '扩展元数据，JSON格式，可存储: 图标路径、颜色配置、显示优先级、标签列表等';


-- ----------------------------------------------------------
-- 3. ont_property - 本体属性表
-- ----------------------------------------------------------
-- 定义类和实体具有的属性，分为对象属性和数据属性
-- 对象属性连接两个类，数据属性连接类与字面值

CREATE TABLE ont_property (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    property_uri VARCHAR(512) NOT NULL,
    local_name VARCHAR(128) NOT NULL,
    property_type VARCHAR(20) NOT NULL DEFAULT 'DATATYPE',
    domain_class_id BIGINT,
    range_class_id BIGINT,
    range_data_type VARCHAR(32),
    min_cardinality INTEGER DEFAULT 0,
    max_cardinality INTEGER,
    default_value VARCHAR(512),
    allowed_values TEXT,
    parent_property_id BIGINT,
    equivalent_to TEXT,
    inverse_of_id BIGINT,
    is_required BOOLEAN DEFAULT FALSE,
    is_multiple BOOLEAN DEFAULT FALSE,
    pattern VARCHAR(256),
    min_value DECIMAL(20,6),
    max_value DECIMAL(20,6),
    description TEXT,
    example TEXT,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ont_prop_uri UNIQUE (definition_id, property_uri),
    CONSTRAINT fk_ont_prop_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_prop_domain FOREIGN KEY (domain_class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_prop_range FOREIGN KEY (range_class_id) REFERENCES ont_class(id) ON DELETE SET NULL,
    CONSTRAINT fk_ont_prop_parent FOREIGN KEY (parent_property_id) REFERENCES ont_property(id) ON DELETE SET NULL,
    CONSTRAINT fk_ont_prop_inverse FOREIGN KEY (inverse_of_id) REFERENCES ont_property(id) ON DELETE SET NULL
);

CREATE INDEX idx_ont_prop_definition ON ont_property(definition_id);
CREATE INDEX idx_ont_prop_domain ON ont_property(domain_class_id);
CREATE INDEX idx_ont_prop_range ON ont_property(range_class_id);
CREATE INDEX idx_ont_prop_type ON ont_property(property_type);

COMMENT ON TABLE ont_property IS '本体属性表 - 定义类具有的属性，包括对象属性(关联其他类)和数据属性(字面值)';
COMMENT ON COLUMN ont_property.id IS '主键ID';
COMMENT ON COLUMN ont_property.definition_id IS '所属本体定义ID';
COMMENT ON COLUMN ont_property.property_uri IS '属性完整URI，格式: {namespace}/property/{localName}';
COMMENT ON COLUMN ont_property.local_name IS '属性本地名称，如 hasName、worksAt、salary';
COMMENT ON COLUMN ont_property.property_type IS '属性类型: OBJECT-连接类/DATATYPE-连接字面值/ANNOTATION-注释/TRANSITIVE-传递/SYMMETRIC-对称/FUNCTIONAL-函数';
COMMENT ON COLUMN ont_property.domain_class_id IS '定义域，属性所属的类，如 hasName 的定义域是 Person';
COMMENT ON COLUMN ont_property.range_class_id IS '值域，OBJECT属性的目标类，如 worksAt 的值域是 Company';
COMMENT ON COLUMN ont_property.range_data_type IS '值域数据类型，DATATYPE属性的类型: string/integer/float/boolean/date/datetime/json';
COMMENT ON COLUMN ont_property.min_cardinality IS '最小出现次数，0表示可选，正整数表示必填';
COMMENT ON COLUMN ont_property.max_cardinality IS '最大出现次数，NULL表示无限制，1表示单值';
COMMENT ON COLUMN ont_property.default_value IS '默认值，当实例未指定时使用该值';
COMMENT ON COLUMN ont_property.allowed_values IS '允许值枚举，JSON数组，限制属性只能取这些值之一';
COMMENT ON COLUMN ont_property.parent_property_id IS '父属性ID，支持属性继承，子属性继承父属性的约束';
COMMENT ON COLUMN ont_property.equivalent_to IS '等价属性列表，JSON数组，表示语义上等价的其他属性';
COMMENT ON COLUMN ont_property.inverse_of_id IS '逆属性ID，如 hasParent 的逆是 hasChild';
COMMENT ON COLUMN ont_property.is_required IS '是否必填，true 等同于 min_cardinality >= 1';
COMMENT ON COLUMN ont_property.is_multiple IS '是否允许多值，true 等同于 max_cardinality > 1 或 NULL';
COMMENT ON COLUMN ont_property.pattern IS '正则表达式约束，字符串值必须匹配此正则，如邮箱格式: ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$';
COMMENT ON COLUMN ont_property.min_value IS '数值类型最小值约束';
COMMENT ON COLUMN ont_property.max_value IS '数值类型最大值约束';
COMMENT ON COLUMN ont_property.description IS '属性的详细说明文档';
COMMENT ON COLUMN ont_property.example IS '属性的使用示例，如 hasEmail: "user@example.com"';
COMMENT ON COLUMN ont_property.metadata IS '扩展元数据，JSON格式，可存储表单配置、显示设置等';


-- ----------------------------------------------------------
-- 4. ont_constraint - 本体约束表
-- ----------------------------------------------------------
-- 定义复杂的数据验证规则，比属性的简单约束更强大
-- 支持基数约束、正则约束、范围约束、枚举约束、自定义 SPARQL 约束

CREATE TABLE ont_constraint (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    class_id BIGINT,
    property_id BIGINT,
    constraint_type VARCHAR(32) NOT NULL,
    value TEXT NOT NULL,
    error_message VARCHAR(512),
    severity VARCHAR(10) NOT NULL DEFAULT 'ERROR',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ont_constraint_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_constraint_class FOREIGN KEY (class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_constraint_property FOREIGN KEY (property_id) REFERENCES ont_property(id) ON DELETE CASCADE
);

CREATE INDEX idx_ont_constraint_definition ON ont_constraint(definition_id);
CREATE INDEX idx_ont_constraint_class ON ont_constraint(class_id);
CREATE INDEX idx_ont_constraint_property ON ont_constraint(property_id);
CREATE INDEX idx_ont_constraint_type ON ont_constraint(constraint_type);

COMMENT ON TABLE ont_constraint IS '本体约束表 - 定义复杂的数据验证规则，用于保证数据质量';
COMMENT ON COLUMN ont_constraint.id IS '主键ID';
COMMENT ON COLUMN ont_constraint.definition_id IS '所属本体定义ID';
COMMENT ON COLUMN ont_constraint.class_id IS '约束应用的类ID，为空表示约束应用于所有类';
COMMENT ON COLUMN ont_constraint.property_id IS '约束应用的属性ID，为空表示类级别的约束';
COMMENT ON COLUMN ont_constraint.constraint_type IS '约束类型: CARDINALITY-基数/PATTERN-正则/RANGE-范围/ENUM-枚举/NOT_NULL-非空/CUSTOM_SPARQL-自定义查询/UNIQUE-唯一/LENGTH-长度';
COMMENT ON COLUMN ont_constraint.value IS '约束值，JSON格式: { "min": 1, "max": 5 } 或 { "pattern": "^[A-Z].*" } 或 ["value1", "value2"]';
COMMENT ON COLUMN ont_constraint.error_message IS '用户友好的错误提示，如"姓名长度必须在2-20个字符之间"';
COMMENT ON COLUMN ont_constraint.severity IS '严重级别: ERROR-阻止保存/WARNING-仅警告/INFO-信息提示';
COMMENT ON COLUMN ont_constraint.description IS '约束的业务说明，帮助理解约束的目的';


-- ----------------------------------------------------------
-- 5. ont_version_history - 本体版本历史表
-- ----------------------------------------------------------
-- 记录本体定义的每次变更，支持版本回滚和变更追溯
-- 记录变更前后的完整状态，便于理解演进过程

CREATE TABLE ont_version_history (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    version VARCHAR(32) NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    entity_id BIGINT,
    before_state TEXT,
    after_state TEXT,
    diff_summary VARCHAR(512),
    changed_by VARCHAR(64),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ont_version_def FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);

CREATE INDEX idx_ont_version_def ON ont_version_history(definition_id);
CREATE INDEX idx_ont_version_type ON ont_version_history(change_type);
CREATE INDEX idx_ont_version_time ON ont_version_history(changed_at DESC);

COMMENT ON TABLE ont_version_history IS '本体版本历史表 - 记录所有本体变更操作，支持版本追溯和回滚';
COMMENT ON COLUMN ont_version_history.id IS '主键ID';
COMMENT ON COLUMN ont_version_history.definition_id IS '关联的本体定义ID';
COMMENT ON COLUMN ont_version_history.version IS '变更时的版本号，如 1.0.0、1.0.1';
COMMENT ON COLUMN ont_version_history.change_type IS '变更操作类型: CLASS_ADDED-新增类/CLASS_MODIFIED-修改类/CLASS_DELETED-删除类/PROPERTY_*-属性变更/CONSTRAINT_*-约束变更';
COMMENT ON COLUMN ont_version_history.entity_type IS '被修改实体的类型: CLASS-类/PROPERTY-属性/CONSTRAINT-约束/DEFINITION-本体定义本身';
COMMENT ON COLUMN ont_version_history.entity_id IS '被修改实体的ID';
COMMENT ON COLUMN ont_version_history.before_state IS '变更前的完整状态JSON，用于查看历史或回滚操作';
COMMENT ON COLUMN ont_version_history.after_state IS '变更后的完整状态JSON';
COMMENT ON COLUMN ont_version_history.diff_summary IS '人类可读的变更摘要，如"将类 Person 的描述从10字更新为50字"';
COMMENT ON COLUMN ont_version_history.changed_by IS '执行本次变更的用户ID，用于审计';
COMMENT ON COLUMN ont_version_history.changed_at IS '变更发生的时间戳';


-- ----------------------------------------------------------
-- 6. ont_mapping - 本体映射表
-- ----------------------------------------------------------
-- 支持本体对齐和知识融合，可以映射到外部本体
-- 用于多本体协同和跨图谱知识共享

CREATE TABLE ont_mapping (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    source_ontology VARCHAR(256),
    source_type VARCHAR(64),
    mapped_class_uri VARCHAR(512) NOT NULL,
    mapping_type VARCHAR(32) NOT NULL DEFAULT 'EQUIVALENT',
    confidence DECIMAL(5,4) DEFAULT 1.0000,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ont_mapping_def FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT uk_ont_mapping_src_tgt UNIQUE (definition_id, source_ontology, source_type, mapped_class_uri)
);

CREATE INDEX idx_ont_mapping_def ON ont_mapping(definition_id);
CREATE INDEX idx_ont_mapping_source ON ont_mapping(source_ontology);
CREATE INDEX idx_ont_mapping_confidence ON ont_mapping(confidence);

COMMENT ON TABLE ont_mapping IS '本体映射表 - 存储本体之间的对齐关系，支持跨本体知识融合';
COMMENT ON COLUMN ont_mapping.id IS '主键ID';
COMMENT ON COLUMN ont_mapping.definition_id IS '所属本体定义ID';
COMMENT ON COLUMN ont_mapping.source_ontology IS '源本体URI，如 DBpedia、Schema.org 等外部本体的命名空间';
COMMENT ON COLUMN ont_mapping.source_type IS '源本体中的类型名称，如 DBpedia 的 Person';
COMMENT ON COLUMN ont_mapping.mapped_class_uri IS '映射到本地本体的类URI';
COMMENT ON COLUMN ont_mapping.mapping_type IS '映射类型: EQUIVALENT-完全等价/SUB_CLASS-是子类/SUPER_CLASS-是父类/RELATED-相关但不等价';
COMMENT ON COLUMN ont_mapping.confidence IS '映射置信度，1.0 表示完全确定，可由 AI 算法自动计算';
COMMENT ON COLUMN ont_mapping.metadata IS '扩展信息，如映射来源(手动/自动)、使用的算法、映射时间等';


-- ============================================================
-- 初始化完成
-- 表数量: 6
-- 预计创建时间: < 1秒
-- ============================================================
