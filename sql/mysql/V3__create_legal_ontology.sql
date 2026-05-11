-- ============================================================
-- 法律知识图谱 SQL 初始化脚本
-- MySQL 8.0+ / Neo4j 5.x
-- 适用于 graphiti-java 应用
--
-- 执行顺序:
--   1. schema.sql        (系统表)
--   2. V1__create_ontology_tables.sql (本体表, PostgreSQL only)
--   3. V3__create_legal_ontology.sql  (法律本体定义 + Neo4j 约束)
--   4. V4__seed_legal_data.sql       (法律数据)
--   5. V5__seed_legal_neo4j.sql     (Neo4j 节点和边)
-- ============================================================

-- ----------------------------------------------------------
-- 第一部分：Neo4j 约束和索引（通过 Neo4j Driver 执行）
-- ----------------------------------------------------------

-- 注意: 以下 Cypher 语句需要通过 Neo4j Browser 或 neo4j-admin 执行
-- 执行方式: COPY 以下内容到 Neo4j Browser 执行

-- Neo4j 约束和索引
-- ============================================================

-- [1] 唯一性约束（防止重复节点）
CREATE CONSTRAINT case_number_unique IF NOT EXISTS
FOR (n:Case) REQUIRE n.caseNumber IS UNIQUE;

CREATE CONSTRAINT court_name_unique IF NOT EXISTS
FOR (n:Court) REQUIRE n.name IS UNIQUE;

CREATE CONSTRAINT judge_name_unique IF NOT EXISTS
FOR (n:Judge) REQUIRE n.name IS UNIQUE;

CREATE CONSTRAINT lawyer_license_unique IF NOT EXISTS
FOR (n:Lawyer) REQUIRE n.licenseNumber IS UNIQUE;

CREATE CONSTRAINT provision_unique IF NOT EXISTS
FOR (n:LegalProvision) REQUIRE (n.lawName, n.articleNumber) IS UNIQUE;

CREATE CONSTRAINT mediator_unique IF NOT EXISTS
FOR (n:Mediator) REQUIRE n.name IS UNIQUE;

CREATE CONSTRAINT org_unique IF NOT EXISTS
FOR (n:LegalOrganization) REQUIRE n.name IS UNIQUE;

CREATE CONSTRAINT agreement_unique IF NOT EXISTS
FOR (n:MediationAgreement) REQUIRE n.agreementNumber IS UNIQUE;

CREATE CONSTRAINT party_unique IF NOT EXISTS
FOR (n:Party) REQUIRE n.name IS UNIQUE;

-- [2] 标签索引（加速按类型查询）
CREATE INDEX case_type_index IF NOT EXISTS
FOR (n:Case) ON (n.caseType);

CREATE INDEX case_status_index IF NOT EXISTS
FOR (n:Case) ON (n.caseStatus);

CREATE INDEX case_filing_date_index IF NOT EXISTS
FOR (n:Case) ON (n.filingDate);

CREATE INDEX party_name_index IF NOT EXISTS
FOR (n:Party) ON (n.name);

CREATE INDEX party_role_index IF NOT EXISTS
FOR (n:Party) ON (n.role);

CREATE INDEX court_level_index IF NOT EXISTS
FOR (n:Court) ON (n.level);

CREATE INDEX judge_court_index IF NOT EXISTS
FOR (n:Judge) ON (n.courtName);

CREATE INDEX provision_lawname_index IF NOT EXISTS
FOR (n:LegalProvision) ON (n.lawName);

CREATE INDEX provision_lawtype_index IF NOT EXISTS
FOR (n:LegalProvision) ON (n.lawType);

CREATE INDEX evidence_type_index IF NOT EXISTS
FOR (n:Evidence) ON (n.evidenceType);

CREATE INDEX judgment_type_index IF NOT EXISTS
FOR (n:JudgmentDocument) ON (n.documentType);

CREATE INDEX org_type_index IF NOT EXISTS
FOR (n:LegalOrganization) ON (n.orgType);

-- [3] 文本索引（支持 CONTAINS 模糊查询）
CREATE INDEX case_name_text_index IF NOT EXISTS
FOR (n:Case) ON (n.caseName)
OPTIONS {indexConfig: {
  `parser`: 'Chinese',
  `entity`: 'token',
  `tokenizer`: 'JieBa',
  `type`: 'text'}};

CREATE INDEX party_name_text_index IF NOT EXISTS
FOR (n:Party) ON (n.name)
OPTIONS {indexConfig: {
  `parser`: 'Chinese',
  `entity`: 'token',
  `tokenizer`: 'JieBa',
  `type`: 'text'}};

CREATE INDEX provision_content_text_index IF NOT EXISTS
FOR (n:LegalProvision) ON (n.content)
OPTIONS {indexConfig: {
  `parser`: 'Chinese',
  `entity`: 'token',
  `tokenizer`: 'JieBa',
  `type`: 'text'}};

-- [4] 向量索引（用于语义检索，indexConfig 根据实际嵌入维度调整）
-- 以下为示例配置，实际维度根据 EmbedderService 配置确定（通常 1024/1536/3072）
-- CREATE VECTOR INDEX case_embedding_index IF NOT EXISTS
-- FOR (n:Case) ON (n.embedding)
-- OPTIONS {indexConfig: {
--   `vector.dimensions`: 1024,
--   `vector.similarity_function`: 'cosine'}};
-- 
-- CREATE VECTOR INDEX legal_provision_embedding_index IF NOT EXISTS
-- FOR (n:LegalProvision) ON (n.embedding)
-- OPTIONS {indexConfig: {
--   `vector.dimensions`: 1024,
--   `vector.similarity_function`: 'cosine'}};

-- ----------------------------------------------------------
-- 第二部分：Neo4j 示例数据（节点 + 关系）
-- 执行方式: neo4j cypher-shell < V5__seed_legal_neo4j.sql
-- 或通过应用 API 接口导入
-- ----------------------------------------------------------

-- 图谱 ID（法律知识图谱）
-- GRAPH_ID = 'legal-knowledge-graph'

-- [1] 创建法律条文节点
-- ---------------------------------------------

-- 商事调解条例 - 第1条
CREATE (n:LegalProvision {
    uuid: 'lp-00001',
    name: '商事调解条例第一条',
    type: 'LegalProvision',
    provisionId: '商事调解条例第一条',
    articleNumber: '第一条',
    content: '为了规范商事调解活动，有效解决商事争议，保护当事人合法权益，促进商事调解行业发展，优化营商环境，制定本条例。',
    lawName: '商事调解条例',
    lawType: '行政法规',
    effectiveDate: '2026-05-01',
    keywords: '商事调解,目的,范围'
});

-- 商事调解条例 - 第2条
CREATE (n:LegalProvision {
    uuid: 'lp-00002',
    name: '商事调解条例第二条',
    type: 'LegalProvision',
    provisionId: '商事调解条例第二条',
    articleNumber: '第二条',
    content: '本条例所称商事调解活动，是指在商事调解组织主持下，当事人自愿友好协商解决贸易、投资、金融、运输、房地产、工程建设、知识产权等领域商事争议的活动。婚姻家庭、继承、监护、劳动人事、消费者权益争议以及依法应当以其他方式解决的争议，不适用商事调解。',
    lawName: '商事调解条例',
    lawType: '行政法规',
    effectiveDate: '2026-05-01',
    keywords: '商事调解,定义,适用范围'
});

-- 商事调解条例 - 第8条
CREATE (n:LegalProvision {
    uuid: 'lp-00008',
    name: '商事调解条例第八条',
    type: 'LegalProvision',
    provisionId: '商事调解条例第八条',
    articleNumber: '第八条',
    content: '设立商事调解组织，应当符合下列条件：（一）发起人为非营利法人；（二）有规范的名称，名称中含有"商事调解"字样；（三）有自己的住所和章程；（四）有30万元以上的资产；（五）有5名以上商事调解员和适当数量的专职工作人员。',
    lawName: '商事调解条例',
    lawType: '行政法规',
    effectiveDate: '2026-05-01',
    keywords: '设立条件,商事调解组织'
});

-- 商事调解条例 - 第14条
CREATE (n:LegalProvision {
    uuid: 'lp-00014',
    name: '商事调解条例第十四条',
    type: 'LegalProvision',
    provisionId: '商事调解条例第十四条',
    articleNumber: '第十四条',
    content: '商事调解活动应当遵循自愿、合法、诚信、保密的原则。',
    lawName: '商事调解条例',
    lawType: '行政法规',
    effectiveDate: '2026-05-01',
    keywords: '基本原则,自愿,合法,诚信,保密'
});

-- 商事调解条例 - 第15条
CREATE (n:LegalProvision {
    uuid: 'lp-00015',
    name: '商事调解条例第十五条',
    type: 'LegalProvision',
    provisionId: '商事调解条例第十五条',
    articleNumber: '第十五条',
    content: '发生商事争议的，当事人可以向商事调解组织申请调解。当事人一方明确拒绝调解的，不得调解。当事人可以从商事调解组织的商事调解员名册中共同选定商事调解员进行调解，或者由当事人共同委托商事调解组织推荐商事调解员进行调解。',
    lawName: '商事调解条例',
    lawType: '行政法规',
    effectiveDate: '2026-05-01',
    keywords: '申请调解,选择调解员'
});

-- 商事调解条例 - 第16条
CREATE (n:LegalProvision {
    uuid: 'lp-00016',
    name: '商事调解条例第十六条',
    type: 'LegalProvision',
    provisionId: '商事调解条例第十六条',
    articleNumber: '第十六条',
    content: '商事调解组织可以收取商事调解费用。商事调解组织应当按照公平、合理的原则制定商事调解费用标准，并向社会公开。',
    lawName: '商事调解条例',
    lawType: '行政法规',
    effectiveDate: '2026-05-01',
    keywords: '调解费用,收费标准'
});

-- 商事调解条例 - 第17条
CREATE (n:LegalProvision {
    uuid: 'lp-00017',
    name: '商事调解条例第十七条',
    type: 'LegalProvision',
    provisionId: '商事调解条例第十七条',
    articleNumber: '第十七条',
    content: '商事调解员开展调解活动应当依照法律法规，可以适用行业规则、商业惯例、交易习惯等。商事调解员在调解过程中应当保持中立，勤勉尽责，遵守职业道德和执业行为规范，不得与当事人串通进行虚假调解活动。',
    lawName: '商事调解条例',
    lawType: '行政法规',
    effectiveDate: '2026-05-01',
    keywords: '调解员职责,中立,诚信'
});

-- 商事调解条例 - 第22条
CREATE (n:LegalProvision {
    uuid: 'lp-00022',
    name: '商事调解条例第二十二条',
    type: 'LegalProvision',
    provisionId: '商事调解条例第二十二条',
    articleNumber: '第二十二条',
    content: '经商事调解达成协议的，除当事人另有约定外，应当制作商事调解协议，载明主要事实、争议事项和当事人达成协议的主要内容、履行方式与期限等。商事调解员应当在商事调解协议上签名并加盖商事调解组织的印章。',
    lawName: '商事调解条例',
    lawType: '行政法规',
    effectiveDate: '2026-05-01',
    keywords: '调解协议,法律效力'
});

-- 商事调解条例 - 第23条
CREATE (n:LegalProvision {
    uuid: 'lp-00023',
    name: '商事调解条例第二十三条',
    type: 'LegalProvision',
    provisionId: '商事调解条例第二十三条',
    articleNumber: '第二十三条',
    content: '当事人可以就商事调解协议申请司法确认，具体依照《中华人民共和国民事诉讼法》有关规定办理。',
    lawName: '商事调解条例',
    lawType: '行政法规',
    effectiveDate: '2026-05-01',
    keywords: '司法确认,民事诉讼法'
});

-- [2] 创建法律组织节点
-- ---------------------------------------------

-- 商事调解组织
CREATE (n:LegalOrganization {
    uuid: 'org-00001',
    name: '上海国际商事调解中心',
    type: 'LegalOrganization',
    orgType: '商事调解组织',
    location: '上海市浦东新区',
    licenseNumber: 'SMC-2025-00001',
    establishedDate: '2024-01-01',
    contact: '021-58880001',
    assetAmount: 500000.00,
    mediatorCount: 15
});

CREATE (n:LegalOrganization {
    uuid: 'org-00002',
    name: '中国国际贸易促进委员会调解中心',
    type: 'LegalOrganization',
    orgType: '商事调解组织',
    location: '北京市朝阳区',
    licenseNumber: 'CCPIT-MED-001',
    establishedDate: '1990-01-01',
    contact: '010-68042247',
    assetAmount: 2000000.00,
    mediatorCount: 50
});

-- [3] 创建调解员节点
-- ---------------------------------------------

CREATE (n:Mediator {
    uuid: 'med-00001',
    name: '张明远',
    type: 'Mediator',
    qualification: '律师',
    licenseNumber: 'MED-LAW-001',
    organizationName: '上海国际商事调解中心',
    specialty: '国际贸易,物流运输',
    yearsExperience: 12
});

CREATE (n:Mediator {
    uuid: 'med-00002',
    name: '李雅琴',
    type: 'Mediator',
    qualification: '法律职业资格',
    licenseNumber: 'MED-LAW-002',
    organizationName: '上海国际商事调解中心',
    specialty: '股权投资,知识产权',
    yearsExperience: 8
});

CREATE (n:Mediator {
    uuid: 'med-00003',
    name: '王志强',
    type: 'Mediator',
    qualification: '原法官',
    licenseNumber: 'MED-JUDGE-001',
    organizationName: '中国国际贸易促进委员会调解中心',
    specialty: '合同纠纷,金融借贷',
    yearsExperience: 20
});

-- [4] 创建法院节点
-- ---------------------------------------------

CREATE (n:Court {
    uuid: 'court-00001',
    name: '上海市第一中级人民法院',
    type: 'Court',
    level: '中级人民法院',
    location: '上海市',
    jurisdiction: '上海市辖区一审商事案件',
    parentCourt: '上海市高级人民法院'
});

CREATE (n:Court {
    uuid: 'court-00002',
    name: '上海市浦东新区人民法院',
    type: 'Court',
    level: '基层人民法院',
    location: '上海市浦东新区',
    jurisdiction: '浦东新区一审民商事案件',
    parentCourt: '上海市第一中级人民法院'
});

CREATE (n:Court {
    uuid: 'court-00003',
    name: '上海市高级人民法院',
    type: 'Court',
    level: '高级人民法院',
    location: '上海市',
    jurisdiction: '上海市辖区二审及再审案件',
    parentCourt: '最高人民法院'
});

-- [5] 创建法官节点
-- ---------------------------------------------

CREATE (n:Judge {
    uuid: 'judge-00001',
    name: '陈建华',
    type: 'Judge',
    title: '审判长',
    courtName: '上海市第一中级人民法院',
    specialty: '商事审判,公司纠纷'
});

CREATE (n:Judge {
    uuid: 'judge-00002',
    name: '周雪梅',
    type: 'Judge',
    title: '审判员',
    courtName: '上海市浦东新区人民法院',
    specialty: '合同纠纷,买卖合同'
});

CREATE (n:Judge {
    uuid: 'judge-00003',
    name: '刘德明',
    type: 'Judge',
    title: '副院长',
    courtName: '上海市高级人民法院',
    specialty: '金融纠纷,知识产权'
});

-- [6] 创建律师节点
-- ---------------------------------------------

CREATE (n:Lawyer {
    uuid: 'lawyer-00001',
    name: '赵海涛',
    type: 'Lawyer',
    licenseNumber: '3110119991000123',
    firmName: '上海海华律师事务所',
    specialty: '商事诉讼,国际贸易',
    contact: '13800001111'
});

CREATE (n:Lawyer {
    uuid: 'lawyer-00002',
    name: '孙丽娜',
    type: 'Lawyer',
    licenseNumber: '3110120121000456',
    firmName: '北京中伦律师事务所上海分所',
    specialty: '公司并购,股权投资',
    contact: '13900002222'
});

-- [7] 创建当事人节点
-- ---------------------------------------------

CREATE (n:Party {
    uuid: 'party-00001',
    name: '上海某某贸易有限公司',
    type: 'Party',
    partyType: '法人',
    idNumber: '91310000MA1K4XYZ01',
    role: '原告',
    address: '上海市浦东新区世纪大道100号',
    contact: '021-58880001',
    isEnterprise: true
});

CREATE (n:Party {
    uuid: 'party-00002',
    name: '某某物流（上海）有限公司',
    type: 'Party',
    partyType: '法人',
    idNumber: '91310000MA1K5ABC02',
    role: '被告',
    address: '上海市嘉定区安亭镇工业园',
    contact: '021-69560001',
    isEnterprise: true
});

-- [8] 创建案件节点
-- ---------------------------------------------

CREATE (n:Case {
    uuid: 'case-00001',
    name: '上海某某贸易公司诉某某物流公司货物运输合同纠纷案',
    type: 'Case',
    caseNumber: '(2025)沪01商初1234号',
    caseType: '商事',
    caseStatus: '调解成功',
    filingDate: '2025-06-01',
    closedDate: '2025-08-15',
    amountInDispute: 580000.00,
    summary: '原告上海某某贸易公司与被告某某物流公司签订货物运输合同，约定被告将原告货物从上海运至广州。运输过程中，因被告管理不善导致货物部分损毁，原告遂提起诉讼，要求被告赔偿损失。案件经上海国际商事调解中心调解，双方达成调解协议。'
});

CREATE (n:Case {
    uuid: 'case-00002',
    name: '某某物流公司诉某某仓储公司仓储合同纠纷案',
    type: 'Case',
    caseNumber: '(2025)沪浦商初5678号',
    caseType: '商事',
    caseStatus: '审理中',
    filingDate: '2025-09-01',
    amountInDispute: 1200000.00,
    summary: '原告某某物流公司将其客户的货物委托被告某某仓储公司保管，因被告仓库管理不当导致货物受潮霉变，双方就赔偿金额产生争议。原告已申请调解。'
});

CREATE (n:Case {
    uuid: 'case-00003',
    name: '某某投资有限公司诉某某科技公司股权投资纠纷案',
    type: 'Case',
    caseNumber: '(2025)沪01商初9012号',
    caseType: '商事',
    caseStatus: '结案',
    filingDate: '2025-03-01',
    closedDate: '2025-05-20',
    amountInDispute: 5000000.00,
    summary: '原告某某投资有限公司与被告某某科技公司签订股权投资协议，约定原告向被告投资500万元。被告未按约定完成业绩对赌目标，原告要求回购股权并支付违约金。经中国国际贸易促进委员会调解中心调解，双方达成和解。'
});

-- [9] 创建证据节点
-- ---------------------------------------------

CREATE (n:Evidence {
    uuid: 'ev-00001',
    name: '货物运输合同',
    type: 'Evidence',
    evidenceNumber: '原告证据-001',
    evidenceType: '书证',
    content: '2025年3月15日原被告签订的货物运输合同，约定运输路线、费用及违约责任。',
    submittedBy: '原告',
    submissionDate: '2025-06-05',
    purpose: '证明原被告之间存在货物运输合同关系'
});

CREATE (n:Evidence {
    uuid: 'ev-00002',
    name: '货物损毁照片及鉴定报告',
    type: 'Evidence',
    evidenceNumber: '原告证据-002',
    evidenceType: '鉴定意见',
    content: '第三方鉴定机构出具的货物损毁鉴定报告，显示货物损失金额为48万元。',
    submittedBy: '原告',
    submissionDate: '2025-06-05',
    purpose: '证明货物损失的具体金额'
});

CREATE (n:Evidence {
    uuid: 'ev-00003',
    name: '入库单及仓储费发票',
    type: 'Evidence',
    evidenceNumber: '原告证据-003',
    evidenceType: '书证',
    content: '被告出具的货物入库单据及相关仓储费用发票。',
    submittedBy: '原告',
    submissionDate: '2025-09-05',
    purpose: '证明货物已交付被告保管'
});

-- [10] 创建裁判文书节点
-- ---------------------------------------------

CREATE (n:JudgmentDocument {
    uuid: 'jd-00001',
    name: '民事调解书',
    type: 'JudgmentDocument',
    documentNumber: '(2025)沪01商初1234号调',
    documentType: '调解书',
    issueDate: '2025-08-15',
    mainContent: '经上海国际商事调解中心主持调解，双方当事人自愿达成如下协议：1.被告于2025年9月30日前赔偿原告货物损失48万元；2.被告于2025年10月31日前支付逾期违约金12万元；3.双方就本案再无其他争议。',
    judgmentResult: '调解成功',
    legalBasis: '《中华人民共和国民法典》第八百三十二条、《商事调解条例》第十四条、第二十二条'
});

-- [11] 创建调解协议节点
-- ---------------------------------------------

CREATE (n:MediationAgreement {
    uuid: 'ma-00001',
    name: '商事调解协议（2025）沪国贸调字第123号',
    type: 'MediationAgreement',
    agreementNumber: '（2025）沪国贸调字第123号',
    mainFacts: '原告委托被告运输货物，被告在运输过程中造成货物损毁。',
    disputeItems: '货物损失金额认定、违约责任承担方式',
    agreementContent: '1.被告赔偿原告货物损失48万元；2.被告支付逾期违约金12万元；3.原告放弃其他诉讼请求。',
    performanceMethod: '一次性支付',
    performanceDeadline: '2025-10-31',
    signDate: '2025-08-10',
    judiciallyConfirmed: true
});

-- [12] 创建关系
-- ---------------------------------------------

-- 案件-当事人关系
MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (p:Party {name: '上海某某贸易有限公司'})
CREATE (c)-[:CASE_PARTY {role: '原告'}]->(p);

MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (p:Party {name: '某某物流（上海）有限公司'})
CREATE (c)-[:CASE_PARTY {role: '被告'}]->(p);

-- 案件-法官关系
MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (j:Judge {name: '陈建华'})
CREATE (c)-[:CASE_JUDGE {role: '审判长'}]->(j);

-- 案件-法院关系
MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (ct:Court {name: '上海市第一中级人民法院'})
CREATE (c)-[:CASE_COURT {courtRole: '一审法院'}]->(ct);

-- 案件-法律条文关系
MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (lp:LegalProvision {articleNumber: '第十四条'})
CREATE (c)-[:CASE_LEGAL_PROVISION {usageType: '适用', reasoning: '案件调解活动遵循自愿、合法、诚信、保密原则'}]->(lp);

MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (lp:LegalProvision {articleNumber: '第二十二条'})
CREATE (c)-[:CASE_LEGAL_PROVISION {usageType: '适用', reasoning: '调解达成协议应制作调解协议，具有法律约束力'}]->(lp);

MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (lp:LegalProvision {articleNumber: '第二条'})
CREATE (c)-[:CASE_LEGAL_PROVISION {usageType: '分析', reasoning: '本案属于货物运输合同纠纷，属于商事调解适用范围'}]->(lp);

MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (lp:LegalProvision {articleNumber: '第十五条'})
CREATE (c)-[:CASE_LEGAL_PROVISION {usageType: '参照', reasoning: '当事人通过商事调解组织申请调解'}]->(lp);

-- 案件-证据关系
MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (e:Evidence {evidenceNumber: '原告证据-001'})
CREATE (c)-[:CASE_EVIDENCE {evidenceRole: '原告证据', admissibility: '采纳'}]->(e);

MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (e:Evidence {evidenceNumber: '原告证据-002'})
CREATE (c)-[:CASE_EVIDENCE {evidenceRole: '原告证据', admissibility: '采纳'}]->(e);

-- 案件-裁判文书关系
MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (jd:JudgmentDocument {documentNumber: '(2025)沪01商初1234号调'})
CREATE (c)-[:CASE_JUDGMENT {documentRole: '调解书'}]->(jd);

-- 案件-调解协议关系
MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (ma:MediationAgreement {agreementNumber: '（2025）沪国贸调字第123号'})
CREATE (c)-[:CASE_MEDIATION_AGREEMENT {agreementRole: '调解达成'}]->(ma);

-- 案件-调解组织关系
MATCH (c:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (org:LegalOrganization {name: '上海国际商事调解中心'})
CREATE (c)-[:CASE_MEDIATION_ORG {mediationStage: '诉前调解', mediationResult: '调解成功'}]->(org);

-- 当事人-律师关系
MATCH (p:Party {name: '上海某某贸易有限公司'})
MATCH (l:Lawyer {name: '赵海涛'})
CREATE (p)-[:PARTY_LAWYER {representationType: '一般代理', startDate: '2025-06-01'}]->(l);

MATCH (p:Party {name: '某某物流（上海）有限公司'})
MATCH (l:Lawyer {name: '孙丽娜'})
CREATE (p)-[:PARTY_LAWYER {representationType: '一般代理', startDate: '2025-06-10'}]->(l);

-- 调解组织-调解员关系
MATCH (org:LegalOrganization {name: '上海国际商事调解中心'})
MATCH (m:Mediator {name: '张明远'})
CREATE (org)-[:ORG_MEDIATOR {employmentType: '专职', hireDate: '2024-01-01'}]->(m);

MATCH (org:LegalOrganization {name: '上海国际商事调解中心'})
MATCH (m:Mediator {name: '李雅琴'})
CREATE (org)-[:ORG_MEDIATOR {employmentType: '兼职', hireDate: '2024-06-01'}]->(m);

MATCH (org:LegalOrganization {name: '中国国际贸易促进委员会调解中心'})
MATCH (m:Mediator {name: '王志强'})
CREATE (org)-[:ORG_MEDIATOR {employmentType: '特聘', hireDate: '2020-01-01'}]->(m);

-- 法律条文-法律条文关系（法条引用）
MATCH (lp1:LegalProvision {articleNumber: '第二十三条'})
MATCH (lp2:LegalProvision {articleNumber: '第一条'})
CREATE (lp1)-[:LEGAL_PROVISION_RELATED {relationType: '引用'}]->(lp2);

-- 案件-案件关系（关联案件）
MATCH (c1:Case {caseNumber: '(2025)沪01商初1234号'})
MATCH (c2:Case {caseNumber: '(2025)沪01商初9012号'})
CREATE (c1)-[:CASE_RELATED {relationType: '同一当事人'}]->(c2);

-- [13] 将节点和边归属到图谱
-- ---------------------------------------------
-- 以下操作将所有节点和边标记归属到法律知识图谱
-- 在 graphiti-java 应用中，group_id 对应 graph_id

MATCH (n) WHERE labels(n)[0] IN ['Case', 'Party', 'Judge', 'Court', 'Lawyer',
    'LegalProvision', 'Evidence', 'JudgmentDocument', 'LegalOrganization',
    'Mediator', 'MediationAgreement']
SET n.group_id = 'legal-knowledge-graph';

MATCH ()-[r]->()
SET r.group_id = 'legal-knowledge-graph';

-- ============================================================
-- 完成
-- ============================================================
-- 数据统计:
-- 节点: 26 个
--   - LegalProvision: 9 个 (商事调解条例)
--   - LegalOrganization: 2 个
--   - Mediator: 3 个
--   - Court: 3 个
--   - Judge: 3 个
--   - Lawyer: 2 个
--   - Party: 2 个
--   - Case: 3 个
--   - Evidence: 3 个
--   - JudgmentDocument: 1 个
--   - MediationAgreement: 1 个
-- 关系: 26 条
-- ============================================================
