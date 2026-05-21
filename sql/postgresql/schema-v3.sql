-- ============================================================
-- 法律知识图谱 V3.0.0: PostgreSQL Schema 增量扩展（通用化改造）
-- 版本: 2026-05-20
-- 说明: 通用化改造：支持法律、金融、企业管理、医疗、社会治理五大领域
--       字段重命名：legal_domain→domain_type, jurisdiction→region, practice_type→scenario_type
-- 要求: PostgreSQL 13+
-- ============================================================

-- ============================================================
-- ont_episode_type: 剧集类型维度表 — 字段重命名（通用化）
-- ============================================================

-- legal_process → process_type
ALTER TABLE ont_episode_type RENAME COLUMN legal_process TO process_type;

-- court_level → stage_level
ALTER TABLE ont_episode_type RENAME COLUMN court_level TO stage_level;

-- is_trial_stage → is_review_stage
ALTER TABLE ont_episode_type RENAME COLUMN is_trial_stage TO is_review_stage;

-- 更新注释
COMMENT ON COLUMN ont_episode_type.process_type IS
  '业务流程类型：business_process(业务流程)|lifecycle(生命周期)|workflow(工作流)|mediation(调解)|arbitration(仲裁)|execution(执行)';
COMMENT ON COLUMN ont_episode_type.stage_level IS
  '阶段级别（通用化，原法律领域 court_level，可为空）';
COMMENT ON COLUMN ont_episode_type.is_review_stage IS
  '是否审查/评议阶段（原 is_trial_stage）';

-- ============================================================
-- ont_community_type: 社区类型维度表 — 新增字段
-- ============================================================

ALTER TABLE ont_community_type
    ADD COLUMN IF NOT EXISTS community_uuid VARCHAR(64),
    ADD COLUMN IF NOT EXISTS graph_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS region VARCHAR(32),
    ADD COLUMN IF NOT EXISTS scenario_type VARCHAR(32);

COMMENT ON TABLE ont_community_type IS '社区类型维度表 — 定义知识图谱中社区的分类体系，支持多领域通用分类（领域、区域、场景三个正交维度）';
COMMENT ON COLUMN ont_community_type.category IS '分类维度: domain(领域分类)|region(区域)|scenario(应用场景)';
COMMENT ON COLUMN ont_community_type.region IS '区域/管辖区：REGION_CN(中国)|REGION_US(美国)|REGION_EU(欧洲)|REGION_ROOT(通用)';
COMMENT ON COLUMN ont_community_type.scenario_type IS '应用场景：SCENARIO_JUDICIAL(司法)|SCENARIO_COMPLIANCE(合规)|SCENARIO_RISK(风险)|SCENARIO_ROOT(通用)';
COMMENT ON COLUMN ont_community_type.community_uuid IS '关联的图数据库社区节点 uuid（实例级关联键）';

-- ============================================================
-- 初始数据：五领域 + 社会治理分类体系
-- ============================================================

-- ---------- 顶层领域（domain）----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, parent_type_code, sort_order, status, metadata)
VALUES
  (1, 'DOMAIN_ROOT', '知识领域', 'Knowledge Domain', 'domain', NULL, 0, 'ACTIVE',
   '{"icon": "global", "color": "#37474F"}'),
  (1, 'DOMAIN_LEGAL', '法律', 'Legal', 'domain', 'DOMAIN_ROOT', 1, 'ACTIVE',
   '{"icon": "scale", "color": "#2E7D32"}'),
  (1, 'DOMAIN_FINANCE', '金融', 'Finance', 'domain', 'DOMAIN_ROOT', 10, 'ACTIVE',
   '{"icon": "trending_up", "color": "#1565C0"}'),
  (1, 'DOMAIN_ENTERPRISE', '企业管理', 'Enterprise', 'domain', 'DOMAIN_ROOT', 20, 'ACTIVE',
   '{"icon": "business", "color": "#6A1B9A"}'),
  (1, 'DOMAIN_MEDICAL', '医疗', 'Medical', 'domain', 'DOMAIN_ROOT', 30, 'ACTIVE',
   '{"icon": "local_hospital", "color": "#C62828"}'),
  (1, 'DOMAIN_SOCIAL_GOV', '社会综合治理', 'Social Governance', 'domain', 'DOMAIN_ROOT', 40, 'ACTIVE',
   '{"icon": "account_balance", "color": "#E65100"}')
ON CONFLICT (definition_id, type_code) DO UPDATE
  SET type_name = EXCLUDED.type_name,
      parent_type_code = EXCLUDED.parent_type_code,
      metadata = EXCLUDED.metadata,
      updated_at = NOW();

-- ---------- 区域分类（region）----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, sort_order, status, metadata)
VALUES
  (1, 'REGION_ROOT', '全球/通用', 'Global', 'region', 0, 'ACTIVE',
   '{"icon": "public", "color": "#78909C"}'),
  (1, 'REGION_CN', '中国', 'China', 'region', 1, 'ACTIVE',
   '{"icon": "flag", "color": "#C62828"}'),
  (1, 'REGION_US', '美国', 'United States', 'region', 2, 'ACTIVE',
   '{"icon": "flag", "color": "#1565C0"}'),
  (1, 'REGION_EU', '欧洲', 'Europe', 'region', 3, 'ACTIVE',
   '{"icon": "flag", "color": "#1565C0"}'),
  (1, 'REGION_INTERNATIONAL', '国际组织', 'International', 'region', 4, 'ACTIVE',
   '{"icon": "public", "color": "#78909C"}')
ON CONFLICT (definition_id, type_code) DO UPDATE
  SET type_name = EXCLUDED.type_name, metadata = EXCLUDED.metadata, updated_at = NOW();

-- ---------- 场景分类（scenario）----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, sort_order, status, metadata)
VALUES
  (1, 'SCENARIO_ROOT', '通用场景', 'General', 'scenario', 0, 'ACTIVE',
   '{"icon": "category", "color": "#78909C"}'),
  (1, 'SCENARIO_JUDICIAL', '司法实践', 'Judicial', 'scenario', 1, 'ACTIVE',
   '{"icon": "gavel", "color": "#2E7D32"}'),
  (1, 'SCENARIO_COMPLIANCE', '合规管理', 'Compliance', 'scenario', 2, 'ACTIVE',
   '{"icon": "verified_user", "color": "#F57F17"}'),
  (1, 'SCENARIO_RISK', '风险管控', 'Risk Management', 'scenario', 3, 'ACTIVE',
   '{"icon": "warning", "color": "#D84315"}'),
  (1, 'SCENARIO_LIFECYCLE', '生命周期', 'Lifecycle', 'scenario', 4, 'ACTIVE',
   '{"icon": "autorenew", "color": "#00838F"}'),
  (1, 'SCENARIO_LAW_REGULATE', '依法调解', 'Law Regulation', 'scenario', 5, 'ACTIVE',
   '{"icon": "balance", "color": "#5D4037"}'),
  (1, 'SCENARIO_FEEDBACK', '反馈处置', 'Feedback', 'scenario', 6, 'ACTIVE',
   '{"icon": "feedback", "color": "#AD1457"}'),
  (1, 'SCENARIO_GOVERNANCE', '综合治理', 'Governance', 'scenario', 7, 'ACTIVE',
   '{"icon": "governance", "color": "#E65100"}'),
  (1, 'SCENARIO_PREVENTION', '预防预警', 'Prevention', 'scenario', 8, 'ACTIVE',
   '{"icon": "shield", "color": "#37474F"}')
ON CONFLICT (definition_id, type_code) DO UPDATE
  SET type_name = EXCLUDED.type_name, metadata = EXCLUDED.metadata, updated_at = NOW();

-- ---------- 法律子领域（domain_type）----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, parent_type_code, sort_order, status, metadata)
VALUES
  (1, 'DOMAIN_CIVIL', '民商事', 'Civil & Commercial', 'domain', 'DOMAIN_LEGAL', 1, 'ACTIVE',
   '{"icon": "account_balance", "color": "#388E3C"}'),
  (1, 'DOMAIN_CRIMINAL', '刑事法律', 'Criminal Law', 'domain', 'DOMAIN_LEGAL', 2, 'ACTIVE',
   '{"icon": "security", "color": "#C62828"}'),
  (1, 'DOMAIN_ADMIN', '行政法律', 'Administrative Law', 'domain', 'DOMAIN_LEGAL', 3, 'ACTIVE',
   '{"icon": "admin_panel_settings", "color": "#F57F17"}'),
  (1, 'DOMAIN_IP', '知识产权', 'Intellectual Property', 'domain', 'DOMAIN_LEGAL', 4, 'ACTIVE',
   '{"icon": "lightbulb", "color": "#1565C0"}'),
  (1, 'DOMAIN_LABOR', '劳动法律', 'Labor Law', 'domain', 'DOMAIN_LEGAL', 5, 'ACTIVE',
   '{"icon": "work", "color": "#00838F"}')
ON CONFLICT (definition_id, type_code) DO UPDATE
  SET type_name = EXCLUDED.type_name, parent_type_code = EXCLUDED.parent_type_code,
      metadata = EXCLUDED.metadata, updated_at = NOW();

-- ---------- 金融子领域（domain_type）----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, parent_type_code, sort_order, status, metadata)
VALUES
  (1, 'DOMAIN_BANKING', '银行与信贷', 'Banking & Credit', 'domain', 'DOMAIN_FINANCE', 1, 'ACTIVE',
   '{"icon": "account_balance", "color": "#0277BD"}'),
  (1, 'DOMAIN_SECURITIES', '证券与投资', 'Securities & Investment', 'domain', 'DOMAIN_FINANCE', 2, 'ACTIVE',
   '{"icon": "show_chart", "color": "#00838F"}'),
  (1, 'DOMAIN_INSURANCE', '保险业务', 'Insurance', 'domain', 'DOMAIN_FINANCE', 3, 'ACTIVE',
   '{"icon": "health_and_safety", "color": "#2E7D32"}'),
  (1, 'DOMAIN_RISK', '风险管控', 'Risk Management', 'domain', 'DOMAIN_FINANCE', 4, 'ACTIVE',
   '{"icon": "warning", "color": "#D84315"}')
ON CONFLICT (definition_id, type_code) DO UPDATE
  SET type_name = EXCLUDED.type_name, parent_type_code = EXCLUDED.parent_type_code,
      metadata = EXCLUDED.metadata, updated_at = NOW();

-- ---------- 企业管理子领域（domain_type）----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, parent_type_code, sort_order, status, metadata)
VALUES
  (1, 'DOMAIN_HR', '人力资源', 'Human Resources', 'domain', 'DOMAIN_ENTERPRISE', 1, 'ACTIVE',
   '{"icon": "people", "color": "#6A1B9A"}'),
  (1, 'DOMAIN_FINANCE_MGMT', '财务管理', 'Financial Management', 'domain', 'DOMAIN_ENTERPRISE', 2, 'ACTIVE',
   '{"icon": "payments", "color": "#0277BD"}'),
  (1, 'DOMAIN_COMPLIANCE', '企业合规', 'Compliance', 'domain', 'DOMAIN_ENTERPRISE', 3, 'ACTIVE',
   '{"icon": "verified_user", "color": "#F57F17"}'),
  (1, 'DOMAIN_GOVERNANCE', '公司治理', 'Corporate Governance', 'domain', 'DOMAIN_ENTERPRISE', 4, 'ACTIVE',
   '{"icon": "business_center", "color": "#37474F"}')
ON CONFLICT (definition_id, type_code) DO UPDATE
  SET type_name = EXCLUDED.type_name, parent_type_code = EXCLUDED.parent_type_code,
      metadata = EXCLUDED.metadata, updated_at = NOW();

-- ---------- 医疗子领域（domain_type）----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, parent_type_code, sort_order, status, metadata)
VALUES
  (1, 'DOMAIN_CLINICAL', '临床诊疗', 'Clinical Practice', 'domain', 'DOMAIN_MEDICAL', 1, 'ACTIVE',
   '{"icon": "medical_services", "color": "#C62828"}'),
  (1, 'DOMAIN_DRUG', '药品与器械', 'Pharma & Device', 'domain', 'DOMAIN_MEDICAL', 2, 'ACTIVE',
   '{"icon": "medication", "color": "#6A1B9A"}'),
  (1, 'DOMAIN_PUBLIC_HEALTH', '公共卫生', 'Public Health', 'domain', 'DOMAIN_MEDICAL', 3, 'ACTIVE',
   '{"icon": "coronavirus", "color": "#00838F"}')
ON CONFLICT (definition_id, type_code) DO UPDATE
  SET type_name = EXCLUDED.type_name, parent_type_code = EXCLUDED.parent_type_code,
      metadata = EXCLUDED.metadata, updated_at = NOW();

-- ---------- 社会治理一级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, parent_type_code, sort_order, status, metadata)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', '婚恋家庭纠纷', 'Marriage & Family Disputes', 'domain', 'DOMAIN_SOCIAL_GOV', 1, 'ACTIVE',
   '{"icon": "family_restroom", "color": "#AD1457"}'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR', '劳动人事争议纠纷', 'Labor & Employment Disputes', 'domain', 'DOMAIN_SOCIAL_GOV', 2, 'ACTIVE',
   '{"icon": "work", "color": "#E65100"}'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT', '侵权责任纠纷', 'Tort Liability Disputes', 'domain', 'DOMAIN_SOCIAL_GOV', 3, 'ACTIVE',
   '{"icon": "gavel", "color": "#C62828"}'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', '邻里关系纠纷', 'Neighbor Disputes', 'domain', 'DOMAIN_SOCIAL_GOV', 4, 'ACTIVE',
   '{"icon": "home", "color": "#5D4037"}'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', '房屋物业纠纷', 'Property & Real Estate Disputes', 'domain', 'DOMAIN_SOCIAL_GOV', 5, 'ACTIVE',
   '{"icon": "apartment", "color": "#1565C0"}'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND', '山林土地水利纠纷', 'Land & Water Disputes', 'domain', 'DOMAIN_SOCIAL_GOV', 6, 'ACTIVE',
   '{"icon": "terrain", "color": "#2E7D32"}'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', '消费服务纠纷', 'Consumer Service Disputes', 'domain', 'DOMAIN_SOCIAL_GOV', 7, 'ACTIVE',
   '{"icon": "shopping_cart", "color": "#F57F17"}'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', '经济金融活动纠纷', 'Economic & Financial Disputes', 'domain', 'DOMAIN_SOCIAL_GOV', 8, 'ACTIVE',
   '{"icon": "trending_up", "color": "#00838F"}'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION', '行政纠纷与信访维权', 'Administrative & Petition Disputes', 'domain', 'DOMAIN_SOCIAL_GOV', 9, 'ACTIVE',
   '{"icon": "admin_panel_settings", "color": "#6A1B9A"}'),
  (1, 'DOMAIN_SOCIAL_CONSULT_SERVICE', '咨询与公证服务', 'Consultation & Notary Services', 'domain', 'DOMAIN_SOCIAL_GOV', 10, 'ACTIVE',
   '{"icon": "help", "color": "#37474F"}')
ON CONFLICT (definition_id, type_code) DO UPDATE
  SET type_name = EXCLUDED.type_name, parent_type_code = EXCLUDED.parent_type_code,
      metadata = EXCLUDED.metadata, updated_at = NOW();

-- ---------- 婚恋家庭纠纷二级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_01', '夫妻关系矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_02', '离异夫妻矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_03', '未婚恋爱纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_04', '同居关系纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_05', '分家继承与赡养纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 5, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_06', '父母子女矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 6, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_07', '兄弟姐妹矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 7, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_08', '家庭其它成员矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 8, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_09', '婚姻自主权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 9, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_10', '宣告失踪死亡纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 10, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_11', '认定无民事行为能力纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 11, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- 劳动人事争议纠纷二级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_01', '劳动报酬追索纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_02', '经济补偿与赔偿纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_03', '福利待遇纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_04', '招聘录用纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_05', '人事任免纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 5, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_06', '劳动合同纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 6, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_07', '临时用工纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 7, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_08', '竞业限制纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 8, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- 侵权责任纠纷二级分类（部分） ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_01', '医疗医美损害责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_02', '人身安全与健康权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_03', '姓名权肖像权声音权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_04', '名誉权荣誉权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_05', '隐私和个人信息保护纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 5, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_06', '财物返还及损害赔偿纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 6, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_07', '网络侵权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 7, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_08', '群众性活动纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 8, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_09', '学校及教育机构纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 9, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_10', '交通事故责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 10, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_11', '医疗事故责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 11, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_12', '性骚扰损害责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 12, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_13', '生态环境责任纠纷及公益诉讼', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 13, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_14', '食品药品安全责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 14, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_15', '饲养动物损害责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 15, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_16', '国家赔偿纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 16, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_17', '口角琐事纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 17, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_18', '知识产权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 18, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- 邻里关系纠纷二级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_01', '相邻用水排水通行通风采光纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_02', '相邻土地利用与建筑物纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_03', '相邻污染损害防免纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_04', '高空抛物责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_05', '邻里口角琐事纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 5, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- 房屋物业纠纷二级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_01', '物业管理纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_02', '业主与业委会纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_03', '不动产登记纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_04', '车位车库使用权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_05', '居住权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 5, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_06', '房屋买卖与租赁纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 6, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_07', '建筑质量损害纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 7, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- 山林土地水利纠纷二级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_01', '土地承包经营权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_02', '宅基地使用权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_03', '取水养殖捕捞权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_04', '建设用地使用权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_05', '探矿权采矿权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 5, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_06', '侵害集体经济组织权益纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 6, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- 消费服务纠纷二级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_01', '商品买卖与质量纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_02', '交通出行服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_03', '住宿餐饮服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_04', '邮政快递与跑腿服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_05', '通信与网络服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 5, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_06', '公用事业服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 6, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_07', '旅游服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 7, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_08', '家政服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 8, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_09', '养老服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 9, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_10', '美容保健服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 10, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_11', '培训服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 11, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_12', '房地产服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 12, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- 经济金融活动纠纷二级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_01', '借贷担保纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_02', '储蓄存款纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_03', '投资信托理财纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_04', '证券基金期货纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_05', '保险理赔纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 5, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_06', '票据与信用证纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 6, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_07', '政府类债务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 7, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_08', '非法融资纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 8, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_09', '公司企业生产经营纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 9, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_10', '拖欠企业账款纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 10, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_11', '房地产纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 11, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_12', '涉众经济金融纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 12, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- 行政纠纷与信访维权二级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_01', '公安治安管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_02', '道路交通管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_03', '劳动和社会保障行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_04', '民政行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_05', '工商行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 5, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_06', '规划拆迁房屋登记等城乡建设行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 6, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_07', '教育行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 7, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_08', '卫生行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 8, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_09', '食品药品安全行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 9, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_10', '税务行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 10, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_11', '环境保护行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 11, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_12', '金融行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 12, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_13', '海关行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 13, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_14', '乡政府管理', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 14, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_15', '村社区居务管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 15, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_16', '行政复议纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 16, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_17', '纪检监察举报申诉', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 17, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_18', '综合行政执法举报投诉', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 18, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_19', '市场监督执法举报投诉', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 19, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_20', '涉诉涉法举报申诉', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 20, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_21', '其他投诉举报', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 21, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_22', '检举控告类事项', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 22, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_23', '建议意见类事项', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 23, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_24', '申诉求决类事项', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 24, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_25', '涉法涉诉信访', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 25, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_26', '涉军维权事项', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 26, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- 咨询与公证服务二级分类 ----------
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, status)
VALUES
  (1, 'DOMAIN_SOCIAL_CONSULT_LEGAL', '法律咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 1, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_PSYCH', '心理咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 2, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_PETITION', '信访咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 3, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_POLICE', '涉警咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 4, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_NOTARY', '公证咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 5, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_LEGAL_AID', '法律援助咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 6, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SERVICE_NOTARY', '公证服务', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 7, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SERVICE_APPRAISAL', '司法鉴定', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 8, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SERVICE_LEGAL_AID', '法律援助', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 9, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SERVICE_WORK_INJURY', '工伤认定', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 10, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_OTHER', '其他咨询与服务', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 11, 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SUGGESTION', '意见建议', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 12, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE SET type_name = EXCLUDED.type_name, updated_at = NOW();

-- ---------- ont_episode_type 通用化初始数据 ----------
INSERT INTO ont_episode_type (definition_id, type_code, type_name, type_name_en, process_type, stage_label, stage_level, is_review_stage, sort_order, status)
VALUES
  -- 通用生命周期类型
  (1, 'EP_INITIATION', '启动', 'Initiation', 'lifecycle', '启动', NULL, false, 1, 'ACTIVE'),
  (1, 'EP_EVALUATION', '评估审查', 'Evaluation', 'lifecycle', '审查', NULL, true, 2, 'ACTIVE'),
  (1, 'EP_EXECUTION', '执行实施', 'Execution', 'lifecycle', '执行', NULL, false, 3, 'ACTIVE'),
  (1, 'EP_RESOLUTION', '解决终结', 'Resolution', 'lifecycle', '终结', NULL, false, 4, 'ACTIVE'),
  -- 通用工作流类型
  (1, 'EP_WORKFLOW_START', '流程启动', 'Workflow Start', 'workflow', '启动', NULL, false, 10, 'ACTIVE'),
  (1, 'EP_WORKFLOW_NODE', '流程节点', 'Workflow Node', 'workflow', '流转', NULL, false, 11, 'ACTIVE'),
  (1, 'EP_WORKFLOW_END', '流程结束', 'Workflow End', 'workflow', '结束', NULL, false, 12, 'ACTIVE'),
  -- 社会治理类型
  (1, 'EP_REPORT_RECEIVE', '事件接收', 'Report Receive', 'lifecycle', '接收', NULL, false, 20, 'ACTIVE'),
  (1, 'EP_TRIAGE_ASSESS', '事件分流评估', 'Triage Assessment', 'workflow', '评估', NULL, true, 21, 'ACTIVE'),
  (1, 'EP_MEDIATION', '调解处理', 'Mediation', 'workflow', '调解', NULL, false, 22, 'ACTIVE'),
  (1, 'EP_COORDINATION', '协调处置', 'Coordination', 'workflow', '协调', NULL, false, 23, 'ACTIVE'),
  (1, 'EP_FEEDBACK', '结果反馈', 'Feedback', 'lifecycle', '反馈', NULL, false, 24, 'ACTIVE'),
  (1, 'EP_FOLLOW_UP', '跟踪回访', 'Follow Up', 'lifecycle', '回访', NULL, false, 25, 'ACTIVE'),
  (1, 'EP_CLOSE', '事件办结', 'Case Close', 'lifecycle', '办结', NULL, false, 26, 'ACTIVE'),
  -- 法律领域保留类型（标注为法律可选）
  (1, 'EP_FILING', '立案', 'Filing', 'business_process', '立案', NULL, false, 30, 'ACTIVE'),
  (1, 'EP_TRIAL', '庭审', 'Trial', 'business_process', '庭审', '一审', true, 31, 'ACTIVE'),
  (1, 'EP_JUDGMENT', '判决', 'Judgment', 'business_process', '判决', '一审', true, 32, 'ACTIVE'),
  (1, 'EP_APPEAL', '上诉', 'Appeal', 'business_process', '上诉', '二审', true, 33, 'ACTIVE'),
  (1, 'EP_EXECUTION', '执行', 'Execution', 'business_process', '执行', NULL, false, 34, 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO UPDATE
  SET type_name = EXCLUDED.type_name, process_type = EXCLUDED.process_type,
      stage_label = EXCLUDED.stage_label, updated_at = NOW();
