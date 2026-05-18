// ============================================================
// Graphiti Neo4j 初始化脚本
// 版本: 2026-05-18
// 说明: 法律知识图谱完整节点和关系示例数据
// 要求: Neo4j 5.x (APOC plugin recommended)
// ============================================================

// ============================================================
// 图谱ID参数配置
// ============================================================
// 所有节点和关系均携带 graph_id 属性，用于多图谱隔离
// 可通过参数注入: :param graphId = 'legal-knowledge-graph'

// ============================================================
// 第一部分: 约束与索引
// ============================================================

// 唯一性约束
CREATE CONSTRAINT case_number_unique_v2 IF NOT EXISTS
FOR (n:Case) REQUIRE n.caseNumber IS UNIQUE;

CREATE CONSTRAINT court_name_unique_v2 IF NOT EXISTS
FOR (n:Court) REQUIRE n.courtName IS UNIQUE;

CREATE CONSTRAINT party_name_unique_v2 IF NOT EXISTS
FOR (n:Party) REQUIRE n.name IS UNIQUE;

CREATE CONSTRAINT provision_unique_v2 IF NOT EXISTS
FOR (n:LegalProvision) REQUIRE (n.lawName, n.articleNumber) IS UNIQUE;

CREATE CONSTRAINT judge_unique_v2 IF NOT EXISTS
FOR (n:Judge) REQUIRE (n.judgeName, n.courtName) IS UNIQUE;

CREATE CONSTRAINT judgment_unique_v2 IF NOT EXISTS
FOR (n:JudgmentDocument) REQUIRE n.documentNumber IS UNIQUE;

CREATE CONSTRAINT agreement_unique_v2 IF NOT EXISTS
FOR (n:MediationAgreement) REQUIRE n.agreementNumber IS UNIQUE;

CREATE CONSTRAINT mediation_org_unique_v2 IF NOT EXISTS
FOR (n:CommercialMediationOrganization) REQUIRE n.name IS UNIQUE;

CREATE CONSTRAINT mediator_unique_v2 IF NOT EXISTS
FOR (n:Mediator) REQUIRE n.name IS UNIQUE;

CREATE CONSTRAINT evidence_unique_v2 IF NOT EXISTS
FOR (n:Evidence) REQUIRE n.evidenceNumber IS UNIQUE;

CREATE CONSTRAINT reason_unique_v2 IF NOT EXISTS
FOR (n:CaseReasoning) REQUIRE n.reasoning IS UNIQUE;

CREATE CONSTRAINT fact_unique_v2 IF NOT EXISTS
FOR (n:CaseFact) REQUIRE n.factDescription IS UNIQUE;

// 属性索引
CREATE INDEX case_type_v2 IF NOT EXISTS FOR (n:Case) ON (n.caseType);
CREATE INDEX case_status_v2 IF NOT EXISTS FOR (n:Case) ON (n.caseStatus);
CREATE INDEX case_court_level_v2 IF NOT EXISTS FOR (n:Case) ON (n.courtLevel);
CREATE INDEX party_role_v2 IF NOT EXISTS FOR (n:Party) ON (n.partyRole);
CREATE INDEX provision_law_v2 IF NOT EXISTS FOR (n:LegalProvision) ON (n.lawName);
CREATE INDEX provision_lawtype_v2 IF NOT EXISTS FOR (n:LegalProvision) ON (n.lawType);
CREATE INDEX evidence_type_v2 IF NOT EXISTS FOR (n:Evidence) ON (n.evidenceType);
CREATE INDEX judgment_type_v2 IF NOT EXISTS FOR (n:JudgmentDocument) ON (n.documentType);
CREATE INDEX org_type_v2 IF NOT EXISTS FOR (n:CommercialMediationOrganization) ON (n.orgType);
CREATE INDEX court_level_v2 IF NOT EXISTS FOR (n:Court) ON (n.courtLevel);
CREATE INDEX court_location_v2 IF NOT EXISTS FOR (n:Court) ON (n.location);

// 文本索引 (中文分词)
CREATE TEXT INDEX case_name_text_v2 IF NOT EXISTS
FOR (n:Case) ON (n.caseName);

CREATE TEXT INDEX provision_content_text_v2 IF NOT EXISTS
FOR (n:LegalProvision) ON (n.provisionContent);

CREATE TEXT INDEX party_name_text_v2 IF NOT EXISTS
FOR (n:Party) ON (n.name);


// ============================================================
// 第二部分: 法院节点 (Court)
// ============================================================

MERGE (c1:Court {
  uuid: 'court-supreme-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  courtName: '中华人民共和国最高人民法院',
  courtLevel: '最高人民法院',
  location: '北京市',
  jurisdiction: '全国范围内的重大案件、最高人民法院直接管辖的案件',
  parentCourt: NULL,
  metadata: '{"icon": "supreme", "color": "#B71C1C"}',
  created_at: datetime()
});

MERGE (c2:Court {
  uuid: 'court-shanghai-high-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  courtName: '上海市高级人民法院',
  courtLevel: '高级人民法院',
  location: '上海市',
  jurisdiction: '上海市辖区内的重大案件',
  parentCourt: '中华人民共和国最高人民法院',
  metadata: '{"icon": "high", "color": "#1565C0"}',
  created_at: datetime()
});

MERGE (c3:Court {
  uuid: 'court-shanghai-1-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  courtName: '上海市第一中级人民法院',
  courtLevel: '中级人民法院',
  location: '上海市',
  jurisdiction: '上海市第一中级人民法院管辖范围内的案件',
  parentCourt: '上海市高级人民法院',
  metadata: '{"icon": "intermediate", "color": "#0D47A1"}',
  created_at: datetime()
});

MERGE (c4:Court {
  uuid: 'court-shanghai-changning-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  courtName: '上海市长宁区人民法院',
  courtLevel: '基层人民法院',
  location: '上海市长宁区',
  jurisdiction: '上海市长宁区管辖范围内的第一审案件',
  parentCourt: '上海市第一中级人民法院',
  metadata: '{"icon": "grassroots", "color": "#1976D2"}',
  created_at: datetime()
});

MERGE (c5:Court {
  uuid: 'court-chongqing-high-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  courtName: '重庆市高级人民法院',
  courtLevel: '高级人民法院',
  location: '重庆市',
  jurisdiction: '重庆市范围内的重大案件',
  parentCourt: '中华人民共和国最高人民法院',
  metadata: '{"icon": "high", "color": "#6A1B9A"}',
  created_at: datetime()
});

MERGE (c6:Court {
  uuid: 'court-chongqing-nanan-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  courtName: '重庆市南岸区人民法院',
  courtLevel: '基层人民法院',
  location: '重庆市南岸区',
  jurisdiction: '重庆市南岸区管辖范围内的第一审案件',
  parentCourt: '重庆市第五中级人民法院',
  metadata: '{"icon": "grassroots", "color": "#7B1FA2"}',
  created_at: datetime()
});

MERGE (c7:Court {
  uuid: 'court-chongqing-jiangbei-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  courtName: '重庆市江北区人民法院',
  courtLevel: '基层人民法院',
  location: '重庆市江北区',
  jurisdiction: '重庆市江北区管辖范围内的第一审案件',
  parentCourt: '重庆市第五中级人民法院',
  metadata: '{"icon": "grassroots", "color": "#8E24AA"}',
  created_at: datetime()
});


// ============================================================
// 第三部分: 法律条文节点 (LegalProvision)
// ============================================================

// 商事调解条例
MERGE (lp1:LegalProvision {
  uuid: 'prov-shangshi-tiao1',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'SMTL-001',
  articleNumber: '第1条',
  provisionContent: '为了规范商事调解活动，有效解决商事争议，保护当事人合法权益，促进商事调解行业发展，优化营商环境，制定本条例。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '商事调解,立法目的,营商环境',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp2:LegalProvision {
  uuid: 'prov-shangshi-tiao2',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'SMTL-002',
  articleNumber: '第2条',
  provisionContent: '本条例所称商事调解活动，是指在商事调解组织主持下，当事人自愿友好协商解决贸易、投资、金融、运输、房地产、工程建设、知识产权等领域商事争议的活动。婚姻家庭、继承、监护、劳动人事、消费者权益争议以及依法应当以其他方式解决的争议，不适用商事调解。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '商事调解定义,适用范围,除外情形',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp3:LegalProvision {
  uuid: 'prov-shangshi-tiao8',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'SMTL-008',
  articleNumber: '第8条',
  provisionContent: '设立商事调解组织，应当符合下列条件：（一）发起人为非营利法人；（二）有规范的名称，名称中含有"商事调解"字样；（三）有自己的住所和章程；（四）有30万元以上的资产；（五）有5名以上商事调解员和适当数量的专职工作人员。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '商事调解组织设立条件,非营利法人,注册资本',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp4:LegalProvision {
  uuid: 'prov-shangshi-tiao12',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'SMTL-012',
  articleNumber: '第12条',
  provisionContent: '商事调解组织聘任的商事调解员应当公道正派，具备良好的专业素质。商事调解员应当符合下列条件之一：（一）通过国家统一法律职业资格考试取得法律职业资格，从事调解工作满3年；（二）从事律师、仲裁、公证工作满3年或者曾任法官、检察官满3年；（三）具有法律、经济、科学技术等相关专业知识，从事法律、经济贸易等专业工作，并具有中级以上职称或者具有同等专业水平；（四）本条例施行前已从事商事调解工作满3年，并具有大学本科以上学历。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '调解员资质,法律职业资格,专业素质',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp5:LegalProvision {
  uuid: 'prov-shangshi-tiao14',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'SMTL-014',
  articleNumber: '第14条',
  provisionContent: '商事调解活动应当遵循自愿、合法、诚信、保密的原则。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '调解原则,自愿,合法,诚信,保密',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp6:LegalProvision {
  uuid: 'prov-shangshi-tiao22',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'SMTL-022',
  articleNumber: '第22条',
  provisionContent: '经商事调解达成协议的，除当事人另有约定外，应当制作商事调解协议，载明主要事实、争议事项和当事人达成协议的主要内容、履行方式与期限等。商事调解员应当在商事调解协议上签名并加盖商事调解组织的印章。商事调解协议的内容不得损害国家利益、社会公共利益和他人合法权益，不得违反法律、行政法规的强制性规定，不得违背公序良俗。商事调解协议具有法律约束力，当事人应当履行。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '调解协议,效力,履行,限制',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

// 民法典
MERGE (lp7:LegalProvision {
  uuid: 'prov-minfadian-69',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'MFD-069',
  articleNumber: '第69条',
  provisionContent: '法人解散的，除合并或者分立的情形外，清算义务人应当及时组成清算组进行清算。法人的董事、理事等执行机构或者决策机构的成员为清算义务人。法律、行政法规另有规定的，依照其规定。清算义务人未及时履行清算义务，造成损害的，应当承担民事责任；主管机关或者利害关系人可以申请人民法院指定有关人员组成清算组进行清算。',
  lawName: '中华人民共和国民法典',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '法人解散,清算,清算义务人,民事责任',
  metadata: '{"source": "中华人民共和国主席令第45号", "category": "民商事"}',
  created_at: datetime()
});

MERGE (lp8:LegalProvision {
  uuid: 'prov-minfadian-70',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'MFD-070',
  articleNumber: '第70条',
  provisionContent: '法人解散后的清算程序和清算组职权，依照有关法律规定。没有法律规定或者当事人没有约定的，可以参照适用民法典的有关规定。',
  lawName: '中华人民共和国民法典',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '法人清算,程序,清算组职权',
  metadata: '{"source": "中华人民共和国主席令第45号", "category": "民商事"}',
  created_at: datetime()
});

// 公司法司法解释（二）
MERGE (lp9:LegalProvision {
  uuid: 'prov-gongsifa-shi2-tiao1',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'GSF2-001',
  articleNumber: '第1条',
  provisionContent: '单独或者合计持有公司全部股东表决权百分之十以上的股东，以下列事由之一提起解散公司诉讼，并符合公司法第一百八十二条规定的，人民法院应当受理：（一）公司持续两年以上无法召开股东会或者股东大会，公司经营管理发生严重困难的；（二）股东表决时无法达到法定或者公司章程规定的比例，持续两年以上不能做出有效的股东会或者股东大会决议，公司经营管理发生严重困难的；（三）公司董事长期冲突，且无法通过股东会或者股东大会解决，公司经营管理发生严重困难的；（四）经营管理发生其他严重困难，公司继续存续会使股东利益受到重大损失的情形。',
  lawName: '最高人民法院关于适用《中华人民共和国公司法》若干问题的规定（二）',
  lawType: '司法解释',
  effectiveDate: date('2021-01-01'),
  keywords: '公司解散诉讼,股东表决权,经营管理困难,公司僵局',
  metadata: '{"source": "法释〔2008〕6号（2020年修正）", "category": "公司法"}',
  created_at: datetime()
});

// 民事诉讼法
MERGE (lp10:LegalProvision {
  uuid: 'prov-minshisusongfa-22',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'MSSSF-022',
  articleNumber: '第22条',
  provisionContent: '对公民提起的民事诉讼，由被告住所地人民法院管辖；被告住所地与经常居住地不一致的，由经常居住地人民法院管辖。',
  lawName: '中华人民共和国民事诉讼法',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '管辖,被告住所地,经常居住地',
  metadata: '{"source": "2023年修正", "category": "民事诉讼"}',
  created_at: datetime()
});

MERGE (lp11:LegalProvision {
  uuid: 'prov-minshisusongfa-23',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'MSSSF-023',
  articleNumber: '第23条',
  provisionContent: '下列民事诉讼，由原告住所地人民法院管辖；原告住所地与经常居住地不一致的，由原告经常居住地人民法院管辖：（一）对不在中华人民共和国领域内居住的人提起的有关身份关系的诉讼；（二）对下落不明或者宣告失踪的人提起的有关身份关系的诉讼；（三）对被采取强制性教育措施的人提起的诉讼；（四）对被监禁的人提起的诉讼。',
  lawName: '中华人民共和国民事诉讼法',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '管辖,原告住所地,被监禁,特殊情形',
  metadata: '{"source": "2023年修正", "category": "民事诉讼"}',
  created_at: datetime()
});

MERGE (lp12:LegalProvision {
  uuid: 'prov-minshisusongfa-24',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  provisionId: 'MSSSF-024',
  articleNumber: '第24条',
  provisionContent: '因合同纠纷提起的诉讼，由被告住所地或者合同履行地人民法院管辖。',
  lawName: '中华人民共和国民事诉讼法',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '合同纠纷,管辖,被告住所地,合同履行地',
  metadata: '{"source": "2023年修正", "category": "民事诉讼"}',
  created_at: datetime()
});


// ============================================================
// 第四部分: 案件节点 (Case)
// ============================================================

// 案例一: 公司解散纠纷
MERGE (ca1:Case:CommercialCase {
  uuid: 'case-xj-company-dissolution-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  caseNumber: '（2023）沪01民终11293号',
  caseName: '徐某骥诉上海某物业管理有限公司等公司解散纠纷案',
  caseType: '民事',
  caseStatus: '结案',
  filingDate: date('2022-01-01'),
  closedDate: date('2023-10-24'),
  amountInDispute: 0,
  caseSummary: '原告作为被告公司持股39.54%的股东，因公司经营管理僵局诉请解散公司。法院认定公司运营良好、股东矛盾可通过其他途径解决，判决驳回解散诉请。',
  disputeType: '公司解散',
  mediationAttempted: false,
  courtLevel: '中级人民法院',
  originalCaseNumber: '（2022）沪0105民初21387号',
  globalCaseNum: '2025-08-2-283-001',
  source: '人民法院案例库',
  crawlingDate: '2025-11-07',
  url: 'https://rmfyalk.court.gov.cn/view/content.html?id=iJabnFfgJmNRHEI4TPDmHOU6Syvay+hVbocBjlk58zU=',
  metadata: '{"caseIndex": 10, "pageNum": 10, "apiData": {"caseSortName": "民事", "sortName": "公司解散纠纷", "courtName": "上海市第一中级人民法院"}}',
  created_at: datetime()
});

// 案例二: 债权人撤销权纠纷
MERGE (ca2:Case:CivilCase {
  uuid: 'case-tan-creditor-revocation-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  caseNumber: '（2020）渝民辖188号',
  caseName: '谭某诉吴某、雒某债权人撤销权纠纷案',
  caseType: '民事',
  caseStatus: '结案',
  filingDate: date('2020-06-08'),
  closedDate: date('2020-12-28'),
  amountInDispute: 0,
  caseSummary: '债权人谭某诉请撤销债务人吴某与雒某离婚时分割房屋的行为。案件经历管辖权异议移送，最终由重庆市高级人民法院指定管辖回原法院。核心争点：共同被告中仅部分被告被监禁时，是否适用原告住所地管辖。',
  courtLevel: '高级人民法院',
  disputeType: '债权人撤销权',
  mediationAttempted: false,
  globalCaseNum: '2025-01-2-078-001',
  source: '人民法院案例库',
  crawlingDate: '2025-11-07',
  url: 'https://rmfyalk.court.gov.cn/view/content.html?id=1XcOaaovBlmK500dPh8CVj2zfisBUPFvvMAeqeqFCWQ=',
  metadata: '{"caseIndex": 4, "pageNum": 10, "apiData": {"caseSortName": "民事", "sortName": "债权人撤销权纠纷", "courtName": "重庆市高级人民法院"}}',
  created_at: datetime()
});

// 案例三: 演示商事调解案件
MERGE (ca_demo:Case:CommercialCase {
  uuid: 'case-demo-mediation-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  caseNumber: '（2024）沪0115商初1234号',
  caseName: '某科技公司诉某投资公司技术合同纠纷案',
  caseType: '民事',
  caseStatus: '调解中',
  filingDate: date('2024-03-01'),
  closedDate: NULL,
  amountInDispute: 1500000,
  caseSummary: '技术合同款项支付争议，经上海国际商事调解中心调解，双方达成和解协议。',
  disputeType: '合同纠纷',
  mediationAttempted: true,
  courtLevel: '基层人民法院',
  metadata: '{"mediationOrg": "上海国际商事调解中心", "mediationDate": "2024-06-15"}',
  created_at: datetime()
});


// ============================================================
// 第五部分: 当事人节点 (Party)
// ============================================================

// 案例一当事人
MERGE (p1:Party {
  uuid: 'party-xu-jiji-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  partyName: '徐某骥',
  partyType: '自然人',
  partyRole: '原告/上诉人',
  idNumber: NULL,
  address: NULL,
  contact: NULL,
  isEnterprise: false,
  metadata: '{"roleType": "股东", "shareholdingRatio": "39.54%"}',
  created_at: datetime()
});

MERGE (p2:Party:LegalPerson {
  uuid: 'party-shanghai-property-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  partyName: '上海某物业管理有限公司',
  partyType: '法人',
  partyRole: '被告',
  unifiedSocialCreditCode: '91310000MA1FXXXXX',
  address: '上海市',
  isEnterprise: true,
  metadata: '{"companyType": "有限责任公司", "businessScope": "物业管理"}',
  created_at: datetime()
});

MERGE (p3:Party {
  uuid: 'party-sun-jin-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  partyName: '孙某瑾',
  partyType: '自然人',
  partyRole: '第三人',
  idNumber: NULL,
  isEnterprise: false,
  metadata: '{"roleType": "公司董事长兼总经理", "shareholdingRatio": "38.37%"}',
  created_at: datetime()
});

MERGE (p4:Party {
  uuid: 'party-zhang-zhizi-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  partyName: '张某之',
  partyType: '自然人',
  partyRole: '第三人',
  idNumber: NULL,
  isEnterprise: false,
  metadata: '{"roleType": "公司股东", "shareholdingRatio": "22.09%"}',
  created_at: datetime()
});

// 案例二当事人
MERGE (p5:Party {
  uuid: 'party-tan-moumou-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  partyName: '谭某',
  partyType: '自然人',
  partyRole: '原告',
  idNumber: NULL,
  isEnterprise: false,
  metadata: '{"roleType": "债权人"}',
  created_at: datetime()
});

MERGE (p6:Party {
  uuid: 'party-wu-moumou-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  partyName: '吴某',
  partyType: '自然人',
  partyRole: '被告',
  idNumber: NULL,
  isEnterprise: false,
  metadata: '{"roleType": "债务人", "note": "起诉时被羁押于重庆市永川监狱"}',
  created_at: datetime()
});

MERGE (p7:Party {
  uuid: 'party-luo-moumou-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  partyName: '雒某',
  partyType: '自然人',
  partyRole: '被告',
  idNumber: NULL,
  address: '重庆市九龙坡区（户籍）/ 重庆市南岸区（经常居住地）',
  isEnterprise: false,
  metadata: '{"roleType": "债务人前配偶"}',
  created_at: datetime()
});


// ============================================================
// 第六部分: 裁判文书节点 (JudgmentDocument)
// ============================================================

MERGE (jd1:JudgmentDocument {
  uuid: 'judgment-c1-first-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  documentNumber: '（2022）沪0105民初21387号',
  documentType: '民事判决书',
  issueDate: date('2023-05-04'),
  judgmentResult: '驳回原告徐某骥的全部诉讼请求',
  legalBasis: '《中华人民共和国民法典》第69条，《最高人民法院关于适用〈中华人民共和国公司法〉若干问题的规定（二）》第1条',
  mainContent: '原告徐某骥作为被告公司持股39.54%的股东，以公司经营管理发生严重困难、继续存续会使股东利益遭受重大损失为由，请求解散公司上海某物业管理有限公司。法院认为：（1）原告与第三人就公司经营、资产处理等有过协商并达成一致；（2）被告公司尚在经营并处于盈利状态；（3）原告可通过转让股权等途径解决。',
  courtName: '上海市长宁区人民法院',
  judgeNames: NULL,
  metadata: '{"caseLevel": "一审", "decisionType": "驳回诉请"}',
  created_at: datetime()
});

MERGE (jd2:JudgmentDocument {
  uuid: 'judgment-c1-second-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  documentNumber: '（2023）沪01民终11293号',
  documentType: '民事判决书',
  issueDate: date('2023-10-24'),
  judgmentResult: '驳回上诉，维持原判',
  legalBasis: '《中华人民共和国民法典》第69条，《最高人民法院关于适用〈中华人民共和国公司法〉若干问题的规定（二）》第1条',
  mainContent: '上诉人徐某骥不服上海市长宁区人民法院（2022）沪0105民初21387号民事判决，提起上诉。上海市第一中级人民法院经审理认为，原审法院认定事实清楚，适用法律正确，判决驳回上诉，维持原判。',
  courtName: '上海市第一中级人民法院',
  judgeNames: NULL,
  metadata: '{"caseLevel": "二审", "decisionType": "维持原判"}',
  created_at: datetime()
});

MERGE (jd3:JudgmentDocument {
  uuid: 'judgment-c2-jurisdiction-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  documentNumber: '（2020）渝民辖188号',
  documentType: '民事裁定书',
  issueDate: date('2020-12-28'),
  judgmentResult: '一、撤销重庆市南岸区人民法院（2020）渝0108民初637号民事裁定；二、本案由重庆市南岸区人民法院审理。',
  legalBasis: '《中华人民共和国民事诉讼法》第22条、第23条',
  mainContent: '重庆市南岸区人民法院认为被告吴某被监禁应移送原告住所地管辖，移送至重庆市江北区人民法院。重庆市高级人民法院认为共同被告中仅部分被监禁不适用民事诉讼法第23条关于原告住所地管辖的规定，撤销一审裁定，指定由重庆市南岸区人民法院审理。',
  courtName: '重庆市高级人民法院',
  judgeNames: NULL,
  metadata: '{"caseLevel": "指定管辖", "decisionType": "撤销移送"}',
  created_at: datetime()
});


// ============================================================
// 第七部分: 法官节点 (Judge)
// ============================================================

MERGE (j1:Judge {
  uuid: 'judge-c1-first-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  judgeName: '某法官（未公开）',
  judgeTitle: '审判长',
  courtName: '上海市长宁区人民法院',
  specialty: '民商事审判',
  metadata: '{"caseNumber": "（2022）沪0105民初21387号"}',
  created_at: datetime()
});

MERGE (j2:Judge {
  uuid: 'judge-c1-second-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  judgeName: '某法官（未公开）',
  judgeTitle: '审判长',
  courtName: '上海市第一中级人民法院',
  specialty: '民商事上诉审判',
  metadata: '{"caseNumber": "（2023）沪01民终11293号"}',
  created_at: datetime()
});

MERGE (j3:Judge {
  uuid: 'judge-c2-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  judgeName: '某法官（未公开）',
  judgeTitle: '审判长',
  courtName: '重庆市高级人民法院',
  specialty: '管辖权审判',
  metadata: '{"caseNumber": "（2020）渝民辖188号"}',
  created_at: datetime()
});


// ============================================================
// 第八部分: 证据节点 (Evidence)
// ============================================================

MERGE (e1:Evidence {
  uuid: 'evidence-c1-share-transfer-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  evidenceNumber: '证据001',
  evidenceType: '书证',
  content: '股权转让协议（2020年3月30日签署），证明原告受让被告公司五位股东持有的全部股权',
  submittedBy: '原告',
  submissionDate: NULL,
  purpose: '证明原告合法取得被告公司股东资格及持股比例',
  admissibility: '采纳',
  metadata: '{}',
  created_at: datetime()
});

MERGE (e2:Evidence {
  uuid: 'evidence-c1-wechat-record-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  evidenceNumber: '证据002',
  evidenceType: '电子数据',
  content: '微信聊天记录（2022年6月），记录原告与第三人孙某瑾就财务章、分红、办公室等事项的沟通',
  submittedBy: '原告',
  submissionDate: date('2022-06-24'),
  purpose: '证明原告曾要求第三人召开股东会解决纠纷',
  admissibility: '采纳',
  metadata: '{}',
  created_at: datetime()
});

MERGE (e3:Evidence {
  uuid: 'evidence-c1-meeting-minutes-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  evidenceNumber: '证据003',
  evidenceType: '书证',
  content: '被告公司股东会决议及会议纪要（2020年4月至2022年1月），证明被告多次召开股东会并正常运营',
  submittedBy: '被告',
  submissionDate: NULL,
  purpose: '证明被告公司正常经营，原告多次获得分红',
  admissibility: '采纳',
  metadata: '{}',
  created_at: datetime()
});

MERGE (e4:Evidence {
  uuid: 'evidence-c1-profit-record-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  evidenceNumber: '证据004',
  evidenceType: '书证',
  content: '银行转账记录，证明被告公司向原告累计支付约1223070元分红款项',
  submittedBy: '被告',
  submissionDate: NULL,
  purpose: '证明原告已获得分红，不存在不分红的公司僵局',
  admissibility: '采纳',
  metadata: '{}',
  created_at: datetime()
});


// ============================================================
// 第九部分: 商事调解相关节点
// ============================================================

MERGE (mo:CommercialMediationOrganization {
  uuid: 'mediation-org-shanghai-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  name: '上海国际商事调解中心',
  orgType: '商事调解组织',
  location: '上海市',
  licenseNumber: '沪商调证字2024001号',
  establishedDate: date('2024-01-01'),
  contact: NULL,
  assetAmount: 500000,
  mediatorCount: 15,
  metadata: '{"supervisingAuthority": "上海市司法局", "international": false}',
  created_at: datetime()
});

MERGE (m:Mediator {
  uuid: 'mediator-li-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  name: '李某',
  qualification: '法律职业资格+5年调解经验',
  licenseNumber: 'MD2024001',
  organizationName: '上海国际商事调解中心',
  specialty: '公司法务、合同纠纷',
  yearsExperience: 5,
  metadata: '{}',
  created_at: datetime()
});

MERGE (ma:MediationAgreement {
  uuid: 'agreement-ma-demo-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  agreementNumber: 'MA20240001',
  mainFacts: '某科技公司与某投资公司就技术合同款项支付产生争议，经上海国际商事调解中心主持调解',
  disputeItems: '技术合同款项支付、项目验收标准',
  agreementContent: '被申请人于2024年12月31日前分期支付合同款项共计人民币150万元',
  performanceMethod: '银行转账',
  performanceDeadline: date('2024-12-31'),
  signDate: date('2024-06-15'),
  judiciallyConfirmed: true,
  judiciallyConfirmDate: date('2024-06-20'),
  judiciallyConfirmCourt: '上海市浦东新区人民法院',
  judiciallyConfirmNumber: '（2024）沪0115民调确字第1234号',
  metadata: '{"fees": "双方各承担50%", "confidentiality": "调解过程保密"}',
  created_at: datetime()
});


// ============================================================
// 第十部分: 案件事实与裁判要旨
// ============================================================

MERGE (f1:CaseFact {
  uuid: 'fact-c1-shareholder-dispute-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  factDescription: '2020年3月30日，原告受让被告五位股东持有的股权，成为上海某物业管理有限公司股东，持股39.54%',
  factCategory: '股权转让',
  factImportance: 'high',
  created_at: datetime()
});

MERGE (f2:CaseFact {
  uuid: 'fact-c1-company-profitable-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  factDescription: '被告公司正常经营，自2020年4月至2022年1月多次召开股东会协商分红、办公场地等事宜，原告累计获得利润分配约1223070元',
  factCategory: '公司经营状况',
  factImportance: 'high',
  created_at: datetime()
});

MERGE (f3:CaseFact {
  uuid: 'fact-c1-shareholder-dispute-002',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  factDescription: '原告称第三人孙某瑾把持公司、拒绝分红、拒绝提供办公室，双方就公司经营产生纠纷',
  factCategory: '股东矛盾',
  factImportance: 'medium',
  created_at: datetime()
});

MERGE (r1:CaseReasoning {
  uuid: 'reasoning-c1-dissolution-standard-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  reasoning: '公司解散纠纷是股东在穷尽公司自治或其他途径，均不能解决公司僵局状况下的救济途径。公司经营管理是否发生困难、是否已经存在公司僵局是判断公司应否解散的重要标准。实践中，应当审慎适用公司解散这一使企业退出市场的救济途径。对于公司运营良好，自我调整机制未失灵，不存在公司存续会使股东利益受到重大损害情形的，不能认定公司经营管理发生困难或存在公司僵局，不应当判令解散公司。',
  guidanceLevel: '参考',
  keywords: '公司解散,公司僵局,判断标准,审慎适用,司法救济',
  applicableScenario: '股东诉请解散公司时，公司运营良好且股东矛盾可通过其他途径解决的',
  created_at: datetime()
});

MERGE (r2:CaseReasoning {
  uuid: 'reasoning-c2-jurisdiction-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  reasoning: '共同被告中仅有部分被告被监禁的民事诉讼，不属于《中华人民共和国民事诉讼法》第二十三条规定的"对被监禁的人提起诉讼由原告住所地人民法院管辖"情形，应当按照法律的其他管辖规定确定管辖法院。',
  guidanceLevel: '参考',
  keywords: '管辖权,被监禁被告,共同被告,原告住所地,债权人撤销权',
  applicableScenario: '债权人撤销权诉讼中，共同被告之一被监禁但另一被告未监禁的情形',
  created_at: datetime()
});


// ============================================================
// 第十一部分: 关系创建
// ============================================================

// 法条关联关系
MATCH (a:LegalProvision {provisionId: 'SMTL-001', graph_id: 'legal-knowledge-graph'})
MATCH (b:LegalProvision {provisionId: 'SMTL-002', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-smtl-1-2',
  graph_id: 'legal-knowledge-graph',
  relationType: '配套',
  description: '第1条定义立法目的，第2条界定适用范围'
}]->(b);

MATCH (a:LegalProvision {provisionId: 'SMTL-012', graph_id: 'legal-knowledge-graph'})
MATCH (b:LegalProvision {provisionId: 'SMTL-008', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-smtl-12-8',
  graph_id: 'legal-knowledge-graph',
  relationType: '配套',
  description: '调解员资格条件与调解组织设立条件的配套规定'
}]->(b);

MATCH (a:LegalProvision {provisionId: 'SMTL-022', graph_id: 'legal-knowledge-graph'})
MATCH (b:LegalProvision {provisionId: 'SMTL-014', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-smtl-22-14',
  graph_id: 'legal-knowledge-graph',
  relationType: '配套',
  description: '调解协议效力与调解原则的关联'
}]->(b);

MATCH (a:LegalProvision {provisionId: 'MFD-069', graph_id: 'legal-knowledge-graph'})
MATCH (b:LegalProvision {provisionId: 'GSF2-001', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-mfd-gsf2',
  graph_id: 'legal-knowledge-graph',
  relationType: '配套',
  description: '民法典第69条规定法人解散一般规则，公司法解释（二）第1条规定公司解散诉讼的具体情形'
}]->(b);

MATCH (a:LegalProvision {provisionId: 'MSSSF-023', graph_id: 'legal-knowledge-graph'})
MATCH (b:LegalProvision {provisionId: 'MSSSF-022', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-msssf-23-22',
  graph_id: 'legal-knowledge-graph',
  relationType: '补充',
  description: '第23条是对第22条一般管辖规定的特别补充'
}]->(b);

// 案件-当事人关系
MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Party {partyName: '徐某骥', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c1-p1-party',
  graph_id: 'legal-knowledge-graph',
  role: '上诉人',
  representationType: '本人',
  caseLevel: '二审',
  fact: '原告持股39.54%诉请解散公司'
}]->(p);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Party {partyName: '上海某物业管理有限公司', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c1-p2-party',
  graph_id: 'legal-knowledge-graph',
  role: '被上诉人',
  representationType: '委托代理',
  caseLevel: '二审',
  fact: '被告公司经营管理权由第三人孙某瑾行使'
}]->(p);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Party {partyName: '孙某瑾', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c1-p3-party',
  graph_id: 'legal-knowledge-graph',
  role: '第三人',
  representationType: '本人',
  caseLevel: '二审',
  fact: '被告公司董事长兼总经理，第三人孙某瑾持股38.37%'
}]->(p);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Party {partyName: '张某之', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c1-p4-party',
  graph_id: 'legal-knowledge-graph',
  role: '第三人',
  representationType: '本人',
  caseLevel: '二审',
  fact: '被告公司股东，持股22.09%'
}]->(p);

MATCH (ca:Case {caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Party {partyName: '谭某', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c2-p5-party',
  graph_id: 'legal-knowledge-graph',
  role: '原告',
  representationType: '委托代理',
  fact: '债权人，主张撤销债务人转让财产的行为'
}]->(p);

MATCH (ca:Case {caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Party {partyName: '吴某', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c2-p6-party',
  graph_id: 'legal-knowledge-graph',
  role: '被告',
  representationType: '法定代理',
  fact: '债务人，起诉时被羁押于重庆市永川监狱'
}]->(p);

MATCH (ca:Case {caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Party {partyName: '雒某', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c2-p7-party',
  graph_id: 'legal-knowledge-graph',
  role: '被告',
  representationType: '委托代理',
  fact: '债务人前配偶，接收了离婚时分得的房屋'
}]->(p);

// 案件-法院关系
MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (c:Court {courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_COURT {
  uuid: 'rel-c1-court-second',
  graph_id: 'legal-knowledge-graph',
  courtRole: '二审法院',
  jurisdictionBasis: '上诉案件管辖',
  fact: '二审法院，驳回上诉，维持原判'
}]->(c);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (c:Court {courtName: '上海市长宁区人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_COURT {
  uuid: 'rel-c1-court-first',
  graph_id: 'legal-knowledge-graph',
  courtRole: '一审法院',
  jurisdictionBasis: '基层人民法院一审管辖',
  fact: '一审法院，驳回原告诉讼请求'
}]->(c);

MATCH (ca:Case {caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (c:Court {courtName: '重庆市高级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_COURT {
  uuid: 'rel-c2-court-supreme-cq',
  graph_id: 'legal-knowledge-graph',
  courtRole: '指定管辖法院',
  jurisdictionBasis: '管辖权争议指定管辖',
  fact: '重庆市高级人民法院，撤销一审裁定，指定由南岸区法院审理'
}]->(c);

// 案件-法官关系
MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (j:Judge {courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGE {
  uuid: 'rel-c1-judge-second',
  graph_id: 'legal-knowledge-graph',
  role: '审判长',
  caseLevel: '二审'
}]->(j);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (j:Judge {courtName: '上海市长宁区人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGE {
  uuid: 'rel-c1-judge-first',
  graph_id: 'legal-knowledge-graph',
  role: '审判长',
  caseLevel: '一审'
}]->(j);

MATCH (ca:Case {caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (j:Judge {courtName: '重庆市高级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGE {
  uuid: 'rel-c2-judge',
  graph_id: 'legal-knowledge-graph',
  role: '审判长',
  caseLevel: '管辖权审查'
}]->(j);

// 案件-法律条文关系
MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:LegalProvision {articleNumber: '第69条', lawName: '中华人民共和国民法典', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c1-prov-mfd69',
  graph_id: 'legal-knowledge-graph',
  usageType: '适用',
  articleText: '法人解散的，除合并或者分立的情形外，清算义务人应当及时组成清算组进行清算',
  reasoning: '法院引用民法典第69条关于法人解散的一般规定，作为本案法律适用的基础',
  importance: 'primary'
}]->(lp);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:LegalProvision {articleNumber: '第1条', provisionId: 'GSF2-001', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c1-prov-gsf2-1',
  graph_id: 'legal-knowledge-graph',
  usageType: '适用',
  articleText: '公司持续两年以上无法召开股东会或者股东大会，公司经营管理发生严重困难的...',
  reasoning: '法院引用公司法解释（二）第1条关于公司解散诉讼的具体情形，判断本案不符合解散条件',
  importance: 'primary'
}]->(lp);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:LegalProvision {articleNumber: '第22条', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c1-prov-msf-22',
  graph_id: 'legal-knowledge-graph',
  usageType: '参照',
  articleText: '对公民提起的民事诉讼，由被告住所地人民法院管辖',
  reasoning: '参照管辖规定确定本案的级别管辖',
  importance: 'secondary'
}]->(lp);

MATCH (ca:Case {caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:LegalProvision {articleNumber: '第22条', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c2-prov-22',
  graph_id: 'legal-knowledge-graph',
  usageType: '适用',
  articleText: '对公民提起的民事诉讼，由被告住所地人民法院管辖',
  reasoning: '确定债权人撤销权诉讼的一般管辖规则',
  importance: 'primary'
}]->(lp);

MATCH (ca:Case {caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:LegalProvision {articleNumber: '第23条', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c2-prov-23',
  graph_id: 'legal-knowledge-graph',
  usageType: '适用',
  articleText: '对被监禁的人提起的诉讼，由原告住所地人民法院管辖',
  reasoning: '争议焦点：共同被告中仅部分被监禁时是否适用本条。经法院解释，本条适用于所有被告均被监禁的情形',
  importance: 'primary'
}]->(lp);

// 案件-裁判文书关系
MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (jd:JudgmentDocument {documentNumber: '（2022）沪0105民初21387号', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGMENT {
  uuid: 'rel-c1-judgment-first',
  graph_id: 'legal-knowledge-graph',
  documentRole: '一审判决',
  fact: '一审判决驳回原告诉讼请求'
}]->(jd);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (jd:JudgmentDocument {documentNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGMENT {
  uuid: 'rel-c1-judgment-second',
  graph_id: 'legal-knowledge-graph',
  documentRole: '二审判决',
  fact: '二审判决驳回上诉，维持原判'
}]->(jd);

MATCH (ca:Case {caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (jd:JudgmentDocument {documentNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGMENT {
  uuid: 'rel-c2-judgment',
  graph_id: 'legal-knowledge-graph',
  documentRole: '管辖权裁定',
  fact: '撤销一审移送裁定，指定由南岸区法院审理'
}]->(jd);

// 案件-证据关系
MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (e:Evidence {evidenceNumber: '证据001', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_EVIDENCE {
  uuid: 'rel-c1-evidence-1',
  graph_id: 'legal-knowledge-graph',
  evidenceRole: '原告证据',
  admissibility: '采纳',
  fact: '证明原告股东资格和持股比例'
}]->(e);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (e:Evidence {evidenceNumber: '证据002', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_EVIDENCE {
  uuid: 'rel-c1-evidence-2',
  graph_id: 'legal-knowledge-graph',
  evidenceRole: '原告证据',
  admissibility: '采纳',
  fact: '证明原告曾要求召开股东会解决纠纷'
}]->(e);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (e:Evidence {evidenceNumber: '证据003', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_EVIDENCE {
  uuid: 'rel-c1-evidence-3',
  graph_id: 'legal-knowledge-graph',
  evidenceRole: '被告证据',
  admissibility: '采纳',
  fact: '证明被告多次召开股东会，正常运营'
}]->(e);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (e:Evidence {evidenceNumber: '证据004', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_EVIDENCE {
  uuid: 'rel-c1-evidence-4',
  graph_id: 'legal-knowledge-graph',
  evidenceRole: '被告证据',
  admissibility: '采纳',
  fact: '证明被告向原告累计支付约1223070元分红'
}]->(e);

// 案件-事实关系
MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (f:CaseFact {factDescription: '2020年3月30日，原告受让被告五位股东持有的股权，成为上海某物业管理有限公司股东，持股39.54%', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:HAS_CASE_FACT {
  uuid: 'rel-c1-f1',
  graph_id: 'legal-knowledge-graph',
  factRole: '背景事实',
  factNarrative: '股权转让完成，原告成为被告公司股东'
}]->(f);

MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (f:CaseFact {factDescription: '被告公司正常经营，自2020年4月至2022年1月多次召开股东会协商分红、办公场地等事宜，原告累计获得利润分配约1223070元', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:HAS_CASE_FACT {
  uuid: 'rel-c1-f2',
  graph_id: 'legal-knowledge-graph',
  factRole: '关键事实',
  factNarrative: '公司持续盈利并正常运营'
}]->(f);

// 案件-裁判要旨关系
MATCH (ca:Case {caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (r:CaseReasoning {graph_id: 'legal-knowledge-graph'})
WHERE r.reasoning STARTS WITH '公司解散纠纷是股东在穷尽公司自治'
MERGE (ca)-[rel:HAS_CASE_REASONING {
  uuid: 'rel-c1-r1',
  graph_id: 'legal-knowledge-graph',
  reasoningRole: '指导意义',
  reasoningSummary: '公司解散的判断标准：穷尽其他途径+经营管理严重困难'
}]->(r);

MATCH (ca:Case {caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (r:CaseReasoning {graph_id: 'legal-knowledge-graph'})
WHERE r.reasoning STARTS WITH '共同被告中仅有部分被告被监禁'
MERGE (ca)-[rel:HAS_CASE_REASONING {
  uuid: 'rel-c2-r2',
  graph_id: 'legal-knowledge-graph',
  reasoningRole: '指导意义',
  reasoningSummary: '部分被告被监禁时按一般管辖规定确定管辖法院'
}]->(r);

// 案件-调解组织关系
MATCH (ca:Case {caseNumber: '（2024）沪0115商初1234号', graph_id: 'legal-knowledge-graph'})
MATCH (mo:CommercialMediationOrganization {name: '上海国际商事调解中心', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_MEDIATION_ORG {
  uuid: 'rel-cdemo-mediation-org',
  graph_id: 'legal-knowledge-graph',
  mediationStage: '诉前调解',
  mediationResult: '调解成功',
  mediationStartDate: date('2024-05-01'),
  mediationEndDate: date('2024-06-15'),
  mediationFees: 15000,
  fact: '商事调解组织主持调解，双方达成和解'
}]->(mo);

MATCH (ca:Case {caseNumber: '（2024）沪0115商初1234号', graph_id: 'legal-knowledge-graph'})
MATCH (ma:MediationAgreement {agreementNumber: 'MA20240001', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_MEDIATION_AGREEMENT {
  uuid: 'rel-cdemo-mediation-agreement',
  graph_id: 'legal-knowledge-graph',
  agreementRole: '调解达成',
  fact: '双方签订调解协议，被申请人分期支付150万元'
}]->(ma);

// 法院层级关系
MATCH (supreme:Court {courtLevel: '最高人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (shanghaiHigh:Court {courtName: '上海市高级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (supreme)-[r:COURT_HIERARCHY {
  uuid: 'rel-court-hierarchy-shanghai-high',
  graph_id: 'legal-knowledge-graph',
  relationType: '上级法院'
}]->(shanghaiHigh);

MATCH (shanghaiHigh:Court {courtName: '上海市高级人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (shanghai1:Court {courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (shanghaiHigh)-[r:COURT_HIERARCHY {
  uuid: 'rel-court-hierarchy-shanghai-1',
  graph_id: 'legal-knowledge-graph',
  relationType: '上级法院'
}]->(shanghai1);

MATCH (shanghai1:Court {courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (changning:Court {courtName: '上海市长宁区人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (shanghai1)-[r:COURT_HIERARCHY {
  uuid: 'rel-court-hierarchy-shanghai-changning',
  graph_id: 'legal-knowledge-graph',
  relationType: '上级法院'
}]->(changning);

// 调解组织-调解员关系
MATCH (mo:CommercialMediationOrganization {name: '上海国际商事调解中心', graph_id: 'legal-knowledge-graph'})
MATCH (m:Mediator {name: '李某', graph_id: 'legal-knowledge-graph'})
MERGE (mo)-[r:ORG_MEDIATOR {
  uuid: 'rel-org-mediator-1',
  graph_id: 'legal-knowledge-graph',
  employmentType: '专职',
  hireDate: date('2024-01-15'),
  qualification: '法律职业资格+5年调解经验',
  fact: '具有法律职业资格，从事调解工作满5年'
}]->(m);

// 调解协议-司法确认关系
MATCH (ma:MediationAgreement {agreementNumber: 'MA20240001', graph_id: 'legal-knowledge-graph'})
MATCH (c:Court {courtName: '上海市浦东新区人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ma)-[r:AGREEMENT_JUDICIALLY_CONFIRMED {
  uuid: 'rel-agreement-judicial-confirm',
  graph_id: 'legal-knowledge-graph',
  confirmDate: date('2024-06-20'),
  confirmNumber: '（2024）沪0115民调确字第1234号',
  confirmResult: '确认有效',
  enforceability: '具有强制执行力',
  fact: '调解协议经司法确认后获得强制执行力'
}]->(c);


// ============================================================
// 验证查询
// ============================================================

// 查看所有节点类型及数量
MATCH (n) WHERE n.graph_id = 'legal-knowledge-graph'
WITH labels(n)[0] AS label, count(*) AS cnt
RETURN label, cnt ORDER BY cnt DESC;

// 查看关键案件节点
MATCH (ca:Case {graph_id: 'legal-knowledge-graph'})
RETURN ca.caseNumber, ca.caseName, ca.caseStatus, ca.courtLevel;

// 查看法条引用关系
MATCH (ca:Case {graph_id: 'legal-knowledge-graph'})-[r:CASE_LEGAL_PROVISION]->(lp:LegalProvision {graph_id: 'legal-knowledge-graph'})
RETURN ca.caseName, lp.lawName, lp.articleNumber, r.usageType, r.importance
ORDER BY ca.caseNumber, r.importance;

// 查看案件-当事人-法院完整图谱
MATCH (p:Party {graph_id: 'legal-knowledge-graph'})-[rp:CASE_PARTY]->(ca:Case {graph_id: 'legal-knowledge-graph'})-[rc:CASE_COURT]->(c:Court {graph_id: 'legal-knowledge-graph'})
RETURN p.partyName, rp.role, ca.caseName, c.courtName, rc.courtRole
ORDER BY ca.caseNumber;


// ============================================================
// 数据统计
// ============================================================
// 节点: 7个法院 + 12个法律条文 + 3个案件 + 4个案件事实 + 3个裁判要旨
//      + 7个当事人 + 3个裁判文书 + 4个证据 + 1个调解组织 + 1个调解员 + 1个调解协议
//      = 约46个节点
// 关系: 案件-当事人(7) + 案件-法院(4) + 案件-法官(3) + 案件-法条(5)
//      + 案件-裁判文书(3) + 案件-证据(4) + 案件-要旨(2) + 案件-调解组织(1)
//      + 案件-调解协议(1) + 法条-法条(4) + 法院层级(3) + 调解组织-调解员(1)
//      + 调解协议-司法确认(1) + 案件-事实(2)
//      = 约40条关系
// ============================================================
