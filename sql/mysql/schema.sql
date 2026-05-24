-- ============================================================
-- Graphiti 数据库 Schema (MySQL 版本)
-- 版本: 2026-05-18
-- 说明: 完整的23张表结构定义，与当前DO类实现一致
-- 要求: MySQL 8.0+
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 第一步：删除已存在的表（严格按外键依赖倒序）
-- ============================================================

DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user_notification_settings;
DROP TABLE IF EXISTS sys_notification;
DROP TABLE IF EXISTS sys_search_history;
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS sys_system_config;
DROP TABLE IF EXISTS ont_mapping;
DROP TABLE IF EXISTS ont_class_inheritance;
DROP TABLE IF EXISTS ont_version_history;
DROP TABLE IF EXISTS ont_constraint;
DROP TABLE IF EXISTS ont_property;
DROP TABLE IF EXISTS ont_class;
DROP TABLE IF EXISTS ont_definition;
DROP TABLE IF EXISTS ont_draft;
DROP TABLE IF EXISTS prompt_version;
DROP TABLE IF EXISTS prompt_variable;
DROP TABLE IF EXISTS prompt_template;
DROP TABLE IF EXISTS custom_instruction;
DROP TABLE IF EXISTS graphiti_graph_metadata;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;


-- ============================================================
-- 第二步：系统管理模块表
-- ============================================================

-- 系统用户表
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
    nickname VARCHAR(50) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    mobile VARCHAR(32) COMMENT '手机号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_sys_user_status (status),
    INDEX idx_sys_user_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 系统角色表
CREATE TABLE sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_sys_role_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    INDEX idx_sys_user_role_user_id (user_id),
    INDEX idx_sys_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 系统菜单表
CREATE TABLE sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '菜单ID',
    name VARCHAR(50) NOT NULL COMMENT '菜单名称',
    permission VARCHAR(100) COMMENT '权限标识',
    url VARCHAR(200) COMMENT '菜单URL',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0-禁用，1-启用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_sys_menu_parent_id (parent_id),
    INDEX idx_sys_menu_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统菜单表';

-- 角色菜单关联表
CREATE TABLE sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE,
    INDEX idx_sys_role_menu_role_id (role_id),
    INDEX idx_sys_role_menu_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单关联表';

-- 系统操作日志表
CREATE TABLE sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation VARCHAR(100) COMMENT '操作名称',
    method VARCHAR(200) COMMENT '请求方法和路径',
    params TEXT COMMENT '请求参数(JSON)',
    ip VARCHAR(50) COMMENT 'IP地址',
    location VARCHAR(100) COMMENT '地理位置',
    status TINYINT COMMENT '0-失败 1-成功',
    error_msg VARCHAR(500) COMMENT '错误信息',
    duration INT COMMENT '耗时(毫秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sys_operation_log_username (username),
    INDEX idx_sys_operation_log_operation (operation),
    INDEX idx_sys_operation_log_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统操作日志表';

-- 系统配置表
CREATE TABLE sys_system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_name VARCHAR(100) COMMENT '配置名称',
    config_description VARCHAR(500) COMMENT '配置描述',
    config_type TINYINT DEFAULT 1 COMMENT '1-文本 2-数字 3-布尔 4-JSON',
    group_name VARCHAR(50) COMMENT '分组名称',
    sort_num INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志',
    INDEX idx_sys_system_config_group_name (group_name),
    INDEX idx_sys_system_config_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 搜索历史表
CREATE TABLE sys_search_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '搜索历史ID',
    user_id BIGINT COMMENT '用户ID',
    query VARCHAR(500) NOT NULL COMMENT '搜索词',
    mode VARCHAR(20) COMMENT '搜索模式',
    result_count INT DEFAULT 0 COMMENT '结果数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sys_search_history_user_id (user_id),
    INDEX idx_sys_search_history_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史记录表';

-- 系统通知表
CREATE TABLE sys_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    type TINYINT NOT NULL DEFAULT 1 COMMENT '通知类型: 1-系统通知 2-图谱通知 3-检索通知',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '已读状态: 0-未读 1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志',
    INDEX idx_sys_notification_user_id (user_id),
    INDEX idx_sys_notification_type (type),
    INDEX idx_sys_notification_is_read (is_read),
    INDEX idx_sys_notification_deleted (deleted),
    INDEX idx_sys_notification_create_time (create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- 用户通知设置表
CREATE TABLE sys_user_notification_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '设置ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    system_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '系统通知开关: 0-关闭 1-开启',
    graph_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '图谱通知开关: 0-关闭 1-开启',
    search_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '检索通知开关: 0-关闭 1-开启',
    email_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '邮件通知开关: 0-关闭 1-开启',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志',
    INDEX idx_sys_user_notification_settings_user_id (user_id),
    INDEX idx_sys_user_notification_settings_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知设置表';


-- ============================================================
-- 第三步：图谱管理模块表
-- ============================================================

-- 图谱元数据表
CREATE TABLE graphiti_graph_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    graph_id VARCHAR(64) NOT NULL COMMENT '图谱ID（UUID）',
    name VARCHAR(255) NOT NULL COMMENT '图谱名称',
    description TEXT COMMENT '图谱描述',
    node_count INT DEFAULT 0 COMMENT '节点数量',
    edge_count INT DEFAULT 0 COMMENT '边数量',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '图谱状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标志',
    UNIQUE KEY uk_graphiti_graph_metadata_graph_id (graph_id),
    INDEX idx_graphiti_graph_metadata_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图谱元数据表';


-- ============================================================
-- 第四步：本体管理模块表
-- ============================================================

-- 本体定义表
CREATE TABLE ont_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    graph_id VARCHAR(64) NOT NULL COMMENT '所属图谱的唯一标识符',
    namespace VARCHAR(255) DEFAULT 'http://legal-ai.cc/ontology' COMMENT '本体命名空间',
    name VARCHAR(128) NOT NULL COMMENT '本体名称',
    version VARCHAR(32) NOT NULL DEFAULT '1.0.0' COMMENT '语义化版本号',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '本体状态: DRAFT/ACTIVE/DEPRECATED/ARCHIVED',
    description TEXT COMMENT '本体的详细描述',
    parent_version_id BIGINT COMMENT '父版本ID',
    created_by VARCHAR(64) COMMENT '创建该版本的用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_ont_def_graph_version (graph_id, version),
    INDEX idx_ont_def_graph_id (graph_id),
    INDEX idx_ont_def_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本体定义表 - 存储每个图谱的本体定义版本信息';

-- 本体类表
CREATE TABLE ont_class (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    definition_id BIGINT NOT NULL COMMENT '所属本体定义的ID',
    class_uri VARCHAR(512) NOT NULL COMMENT '类的完整URI',
    local_name VARCHAR(128) NOT NULL COMMENT '类的本地名称',
    name_en VARCHAR(128) COMMENT '类的英文名称',
    parent_class_id BIGINT COMMENT '父类ID',
    equivalent_to TEXT COMMENT '等价类列表(JSON数组)',
    disjoint_with TEXT COMMENT '互斥类列表(JSON数组)',
    description TEXT COMMENT '类的详细描述',
    example TEXT COMMENT '类的使用示例',
    domain_hint VARCHAR(32) COMMENT '领域分类标记',
    metadata TEXT COMMENT '扩展元数据(JSON格式)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_ont_class_uri (definition_id, class_uri),
    UNIQUE KEY uk_ont_class_local_name (definition_id, local_name),
    CONSTRAINT fk_ont_class_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_class_parent FOREIGN KEY (parent_class_id) REFERENCES ont_class(id) ON DELETE SET NULL,
    INDEX idx_ont_class_definition (definition_id),
    INDEX idx_ont_class_parent (parent_class_id),
    INDEX idx_ont_class_domain (domain_hint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本体类表 - 定义知识图谱中的实体类型';

-- 本体类继承关系表（支持多继承）
CREATE TABLE ont_class_inheritance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    class_id BIGINT NOT NULL COMMENT '类ID',
    parent_class_id BIGINT NOT NULL COMMENT '父类ID',
    definition_id BIGINT NOT NULL COMMENT '所属本体定义ID',
    distance INT DEFAULT 1 COMMENT '继承距离（1=直接父类）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_inheritance_class FOREIGN KEY (class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_inheritance_parent FOREIGN KEY (parent_class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_inheritance_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    UNIQUE KEY uk_ont_inheritance_pair (class_id, parent_class_id),
    INDEX idx_ont_inheritance_class (class_id),
    INDEX idx_ont_inheritance_parent (parent_class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本体类继承关系表 - 支持多继承';

-- 本体属性表
CREATE TABLE ont_property (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    definition_id BIGINT NOT NULL COMMENT '所属本体定义ID',
    property_uri VARCHAR(512) NOT NULL COMMENT '属性完整URI',
    local_name VARCHAR(128) NOT NULL COMMENT '属性本地名称',
    property_type VARCHAR(20) NOT NULL DEFAULT 'DATATYPE' COMMENT '属性类型: OBJECT/DATATYPE/ANNOTATION/TRANSITIVE/SYMMETRIC/FUNCTIONAL',
    domain_class_id BIGINT COMMENT '定义域，属性所属的类',
    range_class_id BIGINT COMMENT '值域，OBJECT属性的目标类',
    range_data_type VARCHAR(32) COMMENT '值域数据类型: string/integer/float/boolean/date/datetime/json',
    min_cardinality INT DEFAULT 0 COMMENT '最小出现次数',
    max_cardinality INT COMMENT '最大出现次数',
    default_value VARCHAR(512) COMMENT '默认值',
    allowed_values TEXT COMMENT '允许值枚举(JSON数组)',
    parent_property_id BIGINT COMMENT '父属性ID',
    equivalent_to TEXT COMMENT '等价属性列表',
    inverse_of_id BIGINT COMMENT '逆属性ID',
    is_required TINYINT(1) DEFAULT 0 COMMENT '是否必填',
    is_multiple TINYINT(1) DEFAULT 0 COMMENT '是否允许多值',
    pattern VARCHAR(256) COMMENT '正则表达式约束',
    min_value DECIMAL(20,6) COMMENT '数值类型最小值约束',
    max_value DECIMAL(20,6) COMMENT '数值类型最大值约束',
    description TEXT COMMENT '属性详细说明',
    example TEXT COMMENT '属性使用示例',
    metadata TEXT COMMENT '扩展元数据(JSON格式)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_ont_prop_uri (definition_id, property_uri),
    CONSTRAINT fk_ont_prop_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_prop_domain FOREIGN KEY (domain_class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_prop_range FOREIGN KEY (range_class_id) REFERENCES ont_class(id) ON DELETE SET NULL,
    CONSTRAINT fk_ont_prop_parent FOREIGN KEY (parent_property_id) REFERENCES ont_property(id) ON DELETE SET NULL,
    CONSTRAINT fk_ont_prop_inverse FOREIGN KEY (inverse_of_id) REFERENCES ont_property(id) ON DELETE SET NULL,
    INDEX idx_ont_prop_definition (definition_id),
    INDEX idx_ont_prop_domain (domain_class_id),
    INDEX idx_ont_prop_range (range_class_id),
    INDEX idx_ont_prop_type (property_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本体属性表 - 定义类具有的属性';

-- 本体约束表
CREATE TABLE ont_constraint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    definition_id BIGINT NOT NULL COMMENT '所属本体定义ID',
    class_id BIGINT COMMENT '约束应用的类ID',
    property_id BIGINT COMMENT '约束应用的属性ID',
    constraint_type VARCHAR(32) NOT NULL COMMENT '约束类型: CARDINALITY/PATTERN/RANGE/ENUM/NOT_NULL/CUSTOM_SPARQL/UNIQUE/LENGTH',
    value TEXT NOT NULL COMMENT '约束值(JSON格式)',
    error_message VARCHAR(512) COMMENT '用户友好的错误提示',
    severity VARCHAR(10) NOT NULL DEFAULT 'ERROR' COMMENT '严重级别: ERROR/WARNING/INFO',
    description TEXT COMMENT '约束的业务说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_ont_constraint_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_constraint_class FOREIGN KEY (class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_constraint_property FOREIGN KEY (property_id) REFERENCES ont_property(id) ON DELETE CASCADE,
    INDEX idx_ont_constraint_definition (definition_id),
    INDEX idx_ont_constraint_class (class_id),
    INDEX idx_ont_constraint_property (property_id),
    INDEX idx_ont_constraint_type (constraint_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本体约束表 - 定义复杂的数据验证规则';

-- 本体版本历史表
CREATE TABLE ont_version_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    definition_id BIGINT NOT NULL COMMENT '关联的本体定义ID',
    version VARCHAR(32) NOT NULL COMMENT '变更时的版本号',
    change_type VARCHAR(32) NOT NULL COMMENT '变更操作类型: CREATED/UPDATED/DELETED/ACTIVATED/DEPRECATED',
    entity_type VARCHAR(20) NOT NULL COMMENT '被修改实体的类型: CLASS/PROPERTY/CONSTRAINT/DEFINITION',
    entity_id BIGINT COMMENT '被修改实体的ID',
    before_state TEXT COMMENT '变更前的完整状态JSON',
    after_state TEXT COMMENT '变更后的完整状态JSON',
    diff_summary VARCHAR(512) COMMENT '变更摘要',
    changed_by VARCHAR(64) COMMENT '执行变更的用户ID',
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    CONSTRAINT fk_ont_version_def FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    INDEX idx_ont_version_def (definition_id),
    INDEX idx_ont_version_type (change_type),
    INDEX idx_ont_version_time (changed_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本体版本历史表 - 记录所有本体变更操作';

-- 本体映射表
CREATE TABLE ont_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    definition_id BIGINT NOT NULL COMMENT '所属本体定义ID',
    source_ontology VARCHAR(256) COMMENT '源本体URI',
    source_type VARCHAR(64) COMMENT '源本体中的类型名称',
    mapped_class_uri VARCHAR(512) NOT NULL COMMENT '映射到本地本体的类URI',
    mapping_type VARCHAR(32) NOT NULL DEFAULT 'EQUIVALENT' COMMENT '映射类型: EQUIVALENT/SUB_CLASS/SUPER_CLASS/RELATED',
    confidence DECIMAL(5,4) DEFAULT 1.0000 COMMENT '映射置信度',
    metadata TEXT COMMENT '扩展信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_ont_mapping_def FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    UNIQUE KEY uk_ont_mapping_src_tgt (definition_id, source_ontology, source_type, mapped_class_uri),
    INDEX idx_ont_mapping_def (definition_id),
    INDEX idx_ont_mapping_source (source_ontology),
    INDEX idx_ont_mapping_confidence (confidence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本体映射表 - 存储本体之间的对齐关系';

-- 本体草稿表（LLM生成）
CREATE TABLE ont_draft (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    graph_id VARCHAR(64) NOT NULL COMMENT '所属图谱ID',
    draft_name VARCHAR(128) NOT NULL COMMENT '草稿名称',
    draft_type VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '草稿类型: DRAFT/OPTIMIZED/GENERATED',
    source_info TEXT COMMENT '原始业务信息(JSON)',
    generated_info TEXT COMMENT 'LLM生成的本体定义(JSON)',
    mock_data TEXT COMMENT '生成的模拟数据(JSON): 节点+边',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/APPLIED',
    created_by VARCHAR(64) COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_ont_draft_graph_id (graph_id),
    INDEX idx_ont_draft_status (status),
    INDEX idx_ont_draft_type (draft_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本体草稿表 - 存储LLM生成的本体定义草稿和模拟数据';


-- ============================================================
-- 第五步：提示词管理模块表
-- ============================================================

-- 提示词模板表
CREATE TABLE prompt_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    code VARCHAR(64) NOT NULL UNIQUE COMMENT '模板编码（唯一标识）',
    name VARCHAR(128) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    type VARCHAR(32) COMMENT '模板类型: entity_extract/edge_extract/dedupe/summary',
    system_prompt TEXT COMMENT '系统提示词',
    user_prompt_template TEXT COMMENT '用户提示词模板',
    response_format TEXT COMMENT '响应格式定义',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    model VARCHAR(64) COMMENT '所属模型',
    sort INT DEFAULT 0 COMMENT '排序值',
    tags TEXT COMMENT '标签（JSON数组）',
    extra_config TEXT COMMENT '额外配置（JSON格式）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    INDEX idx_prompt_template_type (type),
    INDEX idx_prompt_template_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词模板表';

-- 提示词变量表
CREATE TABLE prompt_variable (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '所属模板ID',
    variable_name VARCHAR(64) NOT NULL COMMENT '变量名称',
    description TEXT COMMENT '变量描述',
    variable_type VARCHAR(32) COMMENT '变量类型: string/list/json/text',
    required TINYINT(1) DEFAULT 1 COMMENT '是否必需',
    default_value VARCHAR(512) COMMENT '默认值',
    source VARCHAR(32) COMMENT '变量来源: context/static/llm',
    validation_rule VARCHAR(256) COMMENT '验证规则',
    sort INT DEFAULT 0 COMMENT '排序值',
    remark TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT fk_prompt_var_template FOREIGN KEY (template_id) REFERENCES prompt_template(id) ON DELETE CASCADE,
    INDEX idx_prompt_var_template (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词变量表';

-- 提示词版本表
CREATE TABLE prompt_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '所属模板ID',
    version INT NOT NULL COMMENT '版本号',
    system_prompt TEXT COMMENT '系统提示词',
    user_prompt_template TEXT COMMENT '用户提示词模板',
    response_format TEXT COMMENT '响应格式',
    description TEXT COMMENT '版本描述',
    active TINYINT(1) DEFAULT 0 COMMENT '是否为当前活跃版本',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_prompt_ver_template FOREIGN KEY (template_id) REFERENCES prompt_template(id) ON DELETE CASCADE,
    INDEX idx_prompt_ver_template (template_id),
    INDEX idx_prompt_ver_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提示词版本表';


-- ============================================================
-- 第六步：自定义指令模块表
-- ============================================================

-- 自定义抽取指令表
CREATE TABLE custom_instruction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    graph_id VARCHAR(64) COMMENT '图谱ID（null表示全局指令）',
    instruction TEXT NOT NULL COMMENT 'LLM抽取时的额外提示词',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_custom_instruction_graph_id (graph_id),
    INDEX idx_custom_instruction_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义抽取指令表';


SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 初始化完成
-- ============================================================
