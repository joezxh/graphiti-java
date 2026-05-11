/**
 * 法律知识图谱批量导入脚本
 * Legal Knowledge Graph Batch Import Script
 *
 * 使用方法:
 *   npx ts-node scripts/import-legal-kg.ts
 *   或:  curl -X POST http://localhost:8080/api/v1/graph/legal/import -H "Content-Type: application/json" -d @scripts/legal-seed-data.json
 *
 * 说明:
 *   本脚本通过 graphiti-java REST API 批量导入法律领域数据。
 *   导入顺序: 本体定义 -> 节点 -> 边
 */

export const LEGAL_GRAPH_ID = 'legal-knowledge-graph';

// ============================================================
// 本体定义 (Ontology)
// ============================================================
export const LEGAL_ENTITIES = [
  {
    name: 'Case',
    displayName: '案件',
    description: '案件基类，包含所有案件的公共属性',
    extends: 'Entity',
    properties: {
      caseNumber: { type: 'string', required: true, description: '案件编号' },
      caseName: { type: 'string', required: true, description: '案件名称' },
      caseType: { type: 'string', required: true, description: '案件类型', enum: ['民事', '刑事', '行政', '商事', '执行', '赔偿'] },
      caseStatus: { type: 'string', description: '案件状态', enum: ['立案', '审理中', '调解中', '判决', '上诉中', '结案', '撤销'] },
      filingDate: { type: 'date', description: '立案日期' },
      closedDate: { type: 'date', description: '结案日期' },
      amountInDispute: { type: 'decimal', description: '争议金额(元)' },
      summary: { type: 'text', description: '案件摘要' }
    }
  },
  {
    name: 'CommercialCase',
    displayName: '商事案件',
    description: '商事纠纷案件',
    extends: 'Case',
    properties: {
      disputeType: { type: 'string', description: '纠纷类型' },
      mediationAttempted: { type: 'boolean', description: '是否经过调解' }
    }
  },
  {
    name: 'Party',
    displayName: '当事人',
    description: '案件中的当事人（原告、被告、第三人等）',
    properties: {
      name: { type: 'string', required: true, description: '姓名或名称' },
      partyType: { type: 'string', required: true, description: '当事人类型', enum: ['自然人', '法人', '非法人组织'] },
      idNumber: { type: 'string', description: '身份证号/统一社会信用代码' },
      role: { type: 'string', required: true, description: '诉讼角色', enum: ['原告', '被告', '第三人', '上诉人', '被上诉人', '申请人', '被申请人'] },
      address: { type: 'string', description: '住所地' },
      contact: { type: 'string', description: '联系方式' },
      isEnterprise: { type: 'boolean', description: '是否企业' }
    }
  },
  {
    name: 'Court',
    displayName: '法院',
    description: '审判机关',
    properties: {
      name: { type: 'string', required: true, description: '法院名称' },
      level: { type: 'string', description: '法院级别', enum: ['最高人民法院', '高级人民法院', '中级人民法院', '基层人民法院', '专门法院'] },
      location: { type: 'string', description: '所在地' },
      jurisdiction: { type: 'string', description: '管辖范围' },
      parentCourt: { type: 'string', description: '上级法院名称' }
    }
  },
  {
    name: 'Judge',
    displayName: '法官',
    description: '案件审判人员',
    properties: {
      name: { type: 'string', required: true, description: '法官姓名' },
      title: { type: 'string', description: '职务', enum: ['审判长', '审判员', '人民陪审员', '书记员', '副院长', '院长'] },
      courtName: { type: 'string', description: '所属法院' },
      specialty: { type: 'string', description: '专业领域' }
    }
  },
  {
    name: 'LegalProvision',
    displayName: '法律条文',
    description: '法律、行政法规、司法解释等条文',
    extends: 'Entity',
    properties: {
      provisionId: { type: 'string', required: true, description: '条文编号' },
      articleNumber: { type: 'string', required: true, description: '条款序号' },
      content: { type: 'text', required: true, description: '条文内容' },
      lawName: { type: 'string', required: true, description: '所属法律名称' },
      lawType: { type: 'string', description: '法律类型', enum: ['法律', '行政法规', '司法解释', '部门规章', '地方性法规'] },
      chapter: { type: 'string', description: '所属章节' },
      effectiveDate: { type: 'date', description: '生效日期' },
      abolishedDate: { type: 'date', description: '废止日期' },
      keywords: { type: 'string', description: '关键词标签' }
    }
  },
  {
    name: 'Lawyer',
    displayName: '律师',
    description: '执业律师',
    properties: {
      name: { type: 'string', required: true, description: '律师姓名' },
      licenseNumber: { type: 'string', required: true, description: '律师执业证号' },
      firmName: { type: 'string', description: '所属律师事务所' },
      specialty: { type: 'string', description: '专业领域' },
      contact: { type: 'string', description: '联系方式' }
    }
  },
  {
    name: 'Evidence',
    displayName: '证据',
    description: '案件证据材料',
    properties: {
      evidenceNumber: { type: 'string', required: true, description: '证据编号' },
      evidenceType: { type: 'string', description: '证据类型', enum: ['书证', '物证', '视听资料', '电子数据', '证人证言', '当事人陈述', '鉴定意见', '勘验笔录'] },
      content: { type: 'text', required: true, description: '证据内容摘要' },
      submittedBy: { type: 'string', description: '提交方' },
      submissionDate: { type: 'date', description: '提交日期' },
      purpose: { type: 'string', description: '证明目的' }
    }
  },
  {
    name: 'JudgmentDocument',
    displayName: '裁判文书',
    description: '法院制作的裁判文书',
    properties: {
      documentNumber: { type: 'string', required: true, description: '文书编号' },
      documentType: { type: 'string', description: '文书类型', enum: ['判决书', '裁定书', '调解书', '决定书', '裁决书'] },
      issueDate: { type: 'date', required: true, description: '作出日期' },
      mainContent: { type: 'text', description: '主要内容摘要' },
      judgmentResult: { type: 'string', description: '判决结果' },
      legalBasis: { type: 'text', description: '法律依据' }
    }
  },
  {
    name: 'LegalOrganization',
    displayName: '法律组织',
    description: '调解组织、仲裁机构、公证机构等',
    properties: {
      name: { type: 'string', required: true, description: '组织名称' },
      orgType: { type: 'string', description: '组织类型', enum: ['商事调解组织', '人民调解组织', '仲裁机构', '公证机构', '法律援助中心'] },
      location: { type: 'string', description: '所在地' },
      licenseNumber: { type: 'string', description: '执业证书编号' },
      establishedDate: { type: 'date', description: '设立日期' },
      contact: { type: 'string', description: '联系方式' }
    }
  },
  {
    name: 'Mediator',
    displayName: '调解员',
    description: '商事调解员',
    properties: {
      name: { type: 'string', required: true, description: '调解员姓名' },
      qualification: { type: 'string', description: '资质类型', enum: ['法律职业资格', '律师', '仲裁员', '公证员', '原法官/检察官', '专业职称'] },
      licenseNumber: { type: 'string', description: '资质证书编号' },
      organizationName: { type: 'string', description: '所属组织' },
      specialty: { type: 'string', description: '专业领域' },
      yearsExperience: { type: 'integer', description: '从业年限' }
    }
  },
  {
    name: 'MediationAgreement',
    displayName: '调解协议',
    description: '商事调解达成的协议',
    properties: {
      agreementNumber: { type: 'string', required: true, description: '协议编号' },
      mainFacts: { type: 'text', description: '主要事实' },
      disputeItems: { type: 'text', description: '争议事项' },
      agreementContent: { type: 'text', required: true, description: '协议主要内容' },
      performanceMethod: { type: 'string', description: '履行方式' },
      performanceDeadline: { type: 'date', description: '履行期限' },
      signDate: { type: 'date', description: '签订日期' },
      judiciallyConfirmed: { type: 'boolean', description: '是否经司法确认' }
    }
  }
];

export const LEGAL_EDGES = [
  {
    name: 'CASE_PARTY',
    displayName: '案件-当事人关系',
    sourceType: 'Case',
    targetType: 'Party',
    description: '案件与当事人之间的参与关系',
    properties: {
      role: { type: 'string', required: true, description: '角色', enum: ['原告', '被告', '第三人', '上诉人', '被上诉人', '申请人', '被申请人'] },
      representationType: { type: 'string', description: '代理类型', enum: ['本人', '委托代理', '法定代理'] }
    }
  },
  {
    name: 'CASE_JUDGE',
    displayName: '案件-法官关系',
    sourceType: 'Case',
    targetType: 'Judge',
    description: '案件审判人员关系',
    properties: {
      role: { type: 'string', required: true, description: '角色', enum: ['审判长', '审判员', '人民陪审员', '书记员'] }
    }
  },
  {
    name: 'CASE_COURT',
    displayName: '案件-法院关系',
    sourceType: 'Case',
    targetType: 'Court',
    description: '案件与法院的管辖关系',
    properties: {
      courtRole: { type: 'string', required: true, description: '法院角色', enum: ['立案法院', '一审法院', '二审法院', '再审法院', '执行法院'] }
    }
  },
  {
    name: 'CASE_LEGAL_PROVISION',
    displayName: '案件-法条关系',
    sourceType: 'Case',
    targetType: 'LegalProvision',
    description: '案件适用的法律条文',
    properties: {
      usageType: { type: 'string', required: true, description: '使用方式', enum: ['适用', '参照', '援引', '参考', '分析'] },
      articleText: { type: 'text', description: '引用条文的具体文字' },
      reasoning: { type: 'text', description: '适用理由' }
    }
  },
  {
    name: 'CASE_EVIDENCE',
    displayName: '案件-证据关系',
    sourceType: 'Case',
    targetType: 'Evidence',
    description: '案件与证据的关联',
    properties: {
      evidenceRole: { type: 'string', description: '证据角色', enum: ['原告证据', '被告证据', '法院调取', '鉴定意见'] },
      admissibility: { type: 'string', description: '采信情况', enum: ['采纳', '不予采纳', '部分采纳'] }
    }
  },
  {
    name: 'CASE_JUDGMENT',
    displayName: '案件-裁判文书关系',
    sourceType: 'Case',
    targetType: 'JudgmentDocument',
    description: '案件与其裁判文书的关联',
    properties: {
      documentRole: { type: 'string', description: '文书角色', enum: ['一审判决', '二审判决', '再审判决', '裁定', '调解书'] }
    }
  },
  {
    name: 'PARTY_LAWYER',
    displayName: '当事人-律师关系',
    sourceType: 'Party',
    targetType: 'Lawyer',
    description: '当事人与代理律师的关系',
    properties: {
      representationType: { type: 'string', description: '代理类型', enum: ['一般代理', '特别授权', '法律援助'] },
      startDate: { type: 'date', description: '委托起始日期' },
      endDate: { type: 'date', description: '委托终止日期' }
    }
  },
  {
    name: 'LEGAL_PROVISION_RELATED',
    displayName: '法条-法条关系',
    sourceType: 'LegalProvision',
    targetType: 'LegalProvision',
    description: '法律条文之间的关联关系',
    properties: {
      relationType: { type: 'string', required: true, description: '关系类型', enum: ['引用', '修订', '替代', '废止', '配套', '补充', '参照'] }
    }
  },
  {
    name: 'CASE_RELATED',
    displayName: '关联案件关系',
    sourceType: 'Case',
    targetType: 'Case',
    description: '案件之间的关联关系',
    properties: {
      relationType: { type: 'string', required: true, description: '关联类型', enum: ['同一事实', '共同被告', '第三人参加', '先决关系', '上诉关系', '执行关联'] }
    }
  },
  {
    name: 'ORG_MEDIATOR',
    displayName: '组织-调解员关系',
    sourceType: 'LegalOrganization',
    targetType: 'Mediator',
    description: '法律组织与调解员的聘用关系',
    properties: {
      employmentType: { type: 'string', description: '聘用类型', enum: ['专职', '兼职', '特聘'] },
      hireDate: { type: 'date', description: '聘用日期' }
    }
  },
  {
    name: 'CASE_MEDIATION_ORG',
    displayName: '案件-调解组织关系',
    sourceType: 'Case',
    targetType: 'LegalOrganization',
    description: '案件与调解组织的参与关系',
    properties: {
      mediationStage: { type: 'string', description: '调解阶段', enum: ['诉前调解', '诉中调解', '执行调解'] },
      mediationResult: { type: 'string', description: '调解结果', enum: ['调解成功', '调解终止', '转入诉讼'] }
    }
  },
  {
    name: 'CASE_MEDIATION_AGREEMENT',
    displayName: '案件-调解协议关系',
    sourceType: 'Case',
    targetType: 'MediationAgreement',
    description: '案件与调解协议的关联',
    properties: {
      agreementRole: { type: 'string', description: '协议角色', enum: ['调解达成', '司法确认'] }
    }
  }
];

// ============================================================
// 节点数据
// ============================================================

export const LEGAL_NODES = [
  // --- 法律条文 ---
  {
    uuid: 'lp-00001',
    name: '商事调解条例第一条',
    type: 'LegalProvision',
    summary: '商事调解条例立法目的和适用范围',
    properties: {
      provisionId: '商事调解条例第一条',
      articleNumber: '第一条',
      content: '为了规范商事调解活动，有效解决商事争议，保护当事人合法权益，促进商事调解行业发展，优化营商环境，制定本条例。',
      lawName: '商事调解条例',
      lawType: '行政法规',
      effectiveDate: '2026-05-01',
      keywords: '商事调解,目的,范围'
    }
  },
  {
    uuid: 'lp-00002',
    name: '商事调解条例第二条',
    type: 'LegalProvision',
    summary: '商事调解的定义与适用范围',
    properties: {
      provisionId: '商事调解条例第二条',
      articleNumber: '第二条',
      content: '本条例所称商事调解活动，是指在商事调解组织主持下，当事人自愿友好协商解决贸易、投资、金融、运输、房地产、工程建设、知识产权等领域商事争议的活动。婚姻家庭、继承、监护、劳动人事、消费者权益争议以及依法应当以其他方式解决的争议，不适用商事调解。',
      lawName: '商事调解条例',
      lawType: '行政法规',
      effectiveDate: '2026-05-01',
      keywords: '商事调解,定义,适用范围'
    }
  },
  {
    uuid: 'lp-00008',
    name: '商事调解条例第八条',
    type: 'LegalProvision',
    summary: '商事调解组织的设立条件',
    properties: {
      provisionId: '商事调解条例第八条',
      articleNumber: '第八条',
      content: '设立商事调解组织，应当符合下列条件：（一）发起人为非营利法人；（二）有规范的名称，名称中含有"商事调解"字样；（三）有自己的住所和章程；（四）有30万元以上的资产；（五）有5名以上商事调解员和适当数量的专职工作人员。',
      lawName: '商事调解条例',
      lawType: '行政法规',
      effectiveDate: '2026-05-01',
      keywords: '设立条件,商事调解组织'
    }
  },
  {
    uuid: 'lp-00014',
    name: '商事调解条例第十四条',
    type: 'LegalProvision',
    summary: '商事调解的基本原则',
    properties: {
      provisionId: '商事调解条例第十四条',
      articleNumber: '第十四条',
      content: '商事调解活动应当遵循自愿、合法、诚信、保密的原则。',
      lawName: '商事调解条例',
      lawType: '行政法规',
      effectiveDate: '2026-05-01',
      keywords: '基本原则,自愿,合法,诚信,保密'
    }
  },
  {
    uuid: 'lp-00015',
    name: '商事调解条例第十五条',
    type: 'LegalProvision',
    summary: '商事调解的申请与调解员选定',
    properties: {
      provisionId: '商事调解条例第十五条',
      articleNumber: '第十五条',
      content: '发生商事争议的，当事人可以向商事调解组织申请调解。当事人一方明确拒绝调解的，不得调解。当事人可以从商事调解组织的商事调解员名册中共同选定商事调解员进行调解，或者由当事人共同委托商事调解组织推荐商事调解员进行调解。',
      lawName: '商事调解条例',
      lawType: '行政法规',
      effectiveDate: '2026-05-01',
      keywords: '申请调解,选择调解员'
    }
  },
  {
    uuid: 'lp-00016',
    name: '商事调解条例第十六条',
    type: 'LegalProvision',
    summary: '商事调解费用规定',
    properties: {
      provisionId: '商事调解条例第十六条',
      articleNumber: '第十六条',
      content: '商事调解组织可以收取商事调解费用。商事调解组织应当按照公平、合理的原则制定商事调解费用标准，并向社会公开。',
      lawName: '商事调解条例',
      lawType: '行政法规',
      effectiveDate: '2026-05-01',
      keywords: '调解费用,收费标准'
    }
  },
  {
    uuid: 'lp-00017',
    name: '商事调解条例第十七条',
    type: 'LegalProvision',
    summary: '调解员职责与行为规范',
    properties: {
      provisionId: '商事调解条例第十七条',
      articleNumber: '第十七条',
      content: '商事调解员开展调解活动应当依照法律法规，可以适用行业规则、商业惯例、交易习惯等。商事调解员在调解过程中应当保持中立，勤勉尽责，遵守职业道德和执业行为规范，不得与当事人串通进行虚假调解活动。',
      lawName: '商事调解条例',
      lawType: '行政法规',
      effectiveDate: '2026-05-01',
      keywords: '调解员职责,中立,诚信'
    }
  },
  {
    uuid: 'lp-00022',
    name: '商事调解条例第二十二条',
    type: 'LegalProvision',
    summary: '调解协议的制作与效力',
    properties: {
      provisionId: '商事调解条例第二十二条',
      articleNumber: '第二十二条',
      content: '经商事调解达成协议的，除当事人另有约定外，应当制作商事调解协议，载明主要事实、争议事项和当事人达成协议的主要内容、履行方式与期限等。商事调解员应当在商事调解协议上签名并加盖商事调解组织的印章。',
      lawName: '商事调解条例',
      lawType: '行政法规',
      effectiveDate: '2026-05-01',
      keywords: '调解协议,法律效力'
    }
  },
  {
    uuid: 'lp-00023',
    name: '商事调解条例第二十三条',
    type: 'LegalProvision',
    summary: '调解协议的司法确认',
    properties: {
      provisionId: '商事调解条例第二十三条',
      articleNumber: '第二十三条',
      content: '当事人可以就商事调解协议申请司法确认，具体依照《中华人民共和国民事诉讼法》有关规定办理。',
      lawName: '商事调解条例',
      lawType: '行政法规',
      effectiveDate: '2026-05-01',
      keywords: '司法确认,民事诉讼法'
    }
  },

  // --- 法律组织 ---
  {
    uuid: 'org-00001',
    name: '上海国际商事调解中心',
    type: 'LegalOrganization',
    summary: '上海国际商事调解中心，提供国际商事调解服务',
    properties: {
      orgType: '商事调解组织',
      location: '上海市浦东新区',
      licenseNumber: 'SMC-2025-00001',
      establishedDate: '2024-01-01',
      contact: '021-58880001'
    }
  },
  {
    uuid: 'org-00002',
    name: '中国国际贸易促进委员会调解中心',
    type: 'LegalOrganization',
    summary: '中国国际贸易促进委员会调解中心，从事涉外商事调解',
    properties: {
      orgType: '商事调解组织',
      location: '北京市朝阳区',
      licenseNumber: 'CCPIT-MED-001',
      establishedDate: '1990-01-01',
      contact: '010-68042247'
    }
  },

  // --- 调解员 ---
  {
    uuid: 'med-00001',
    name: '张明远',
    type: 'Mediator',
    summary: '上海国际商事调解中心专职调解员，擅长国际贸易和物流运输纠纷',
    properties: {
      qualification: '律师',
      licenseNumber: 'MED-LAW-001',
      organizationName: '上海国际商事调解中心',
      specialty: '国际贸易,物流运输',
      yearsExperience: 12
    }
  },
  {
    uuid: 'med-00002',
    name: '李雅琴',
    type: 'Mediator',
    summary: '上海国际商事调解中心兼职调解员，擅长股权投资和知识产权纠纷',
    properties: {
      qualification: '法律职业资格',
      licenseNumber: 'MED-LAW-002',
      organizationName: '上海国际商事调解中心',
      specialty: '股权投资,知识产权',
      yearsExperience: 8
    }
  },
  {
    uuid: 'med-00003',
    name: '王志强',
    type: 'Mediator',
    summary: '中国国际贸易促进委员会调解中心特聘调解员，原高级法官',
    properties: {
      qualification: '原法官',
      licenseNumber: 'MED-JUDGE-001',
      organizationName: '中国国际贸易促进委员会调解中心',
      specialty: '合同纠纷,金融借贷',
      yearsExperience: 20
    }
  },

  // --- 法院 ---
  {
    uuid: 'court-00001',
    name: '上海市第一中级人民法院',
    type: 'Court',
    summary: '上海市中级人民法院，管辖上海市一审商事案件',
    properties: {
      level: '中级人民法院',
      location: '上海市',
      jurisdiction: '上海市辖区一审商事案件',
      parentCourt: '上海市高级人民法院'
    }
  },
  {
    uuid: 'court-00002',
    name: '上海市浦东新区人民法院',
    type: 'Court',
    summary: '上海市基层人民法院，管辖浦东新区一审民商事案件',
    properties: {
      level: '基层人民法院',
      location: '上海市浦东新区',
      jurisdiction: '浦东新区一审民商事案件',
      parentCourt: '上海市第一中级人民法院'
    }
  },
  {
    uuid: 'court-00003',
    name: '上海市高级人民法院',
    type: 'Court',
    summary: '上海市高级人民法院，管辖上海市二审及再审案件',
    properties: {
      level: '高级人民法院',
      location: '上海市',
      jurisdiction: '上海市辖区二审及再审案件',
      parentCourt: '最高人民法院'
    }
  },

  // --- 法官 ---
  {
    uuid: 'judge-00001',
    name: '陈建华',
    type: 'Judge',
    summary: '上海市第一中级人民法院审判长，擅长商事审判',
    properties: {
      title: '审判长',
      courtName: '上海市第一中级人民法院',
      specialty: '商事审判,公司纠纷'
    }
  },
  {
    uuid: 'judge-00002',
    name: '周雪梅',
    type: 'Judge',
    summary: '上海市浦东新区人民法院审判员，擅长合同纠纷',
    properties: {
      title: '审判员',
      courtName: '上海市浦东新区人民法院',
      specialty: '合同纠纷,买卖合同'
    }
  },
  {
    uuid: 'judge-00003',
    name: '刘德明',
    type: 'Judge',
    summary: '上海市高级人民法院副院长，擅长金融纠纷',
    properties: {
      title: '副院长',
      courtName: '上海市高级人民法院',
      specialty: '金融纠纷,知识产权'
    }
  },

  // --- 律师 ---
  {
    uuid: 'lawyer-00001',
    name: '赵海涛',
    type: 'Lawyer',
    summary: '上海海华律师事务所律师，擅长商事诉讼',
    properties: {
      licenseNumber: '3110119991000123',
      firmName: '上海海华律师事务所',
      specialty: '商事诉讼,国际贸易',
      contact: '13800001111'
    }
  },
  {
    uuid: 'lawyer-00002',
    name: '孙丽娜',
    type: 'Lawyer',
    summary: '北京中伦律师事务所上海分所律师，擅长公司并购',
    properties: {
      licenseNumber: '3110120121000456',
      firmName: '北京中伦律师事务所上海分所',
      specialty: '公司并购,股权投资',
      contact: '13900002222'
    }
  },

  // --- 当事人 ---
  {
    uuid: 'party-00001',
    name: '上海某某贸易有限公司',
    type: 'Party',
    summary: '货物运输合同纠纷案原告',
    properties: {
      partyType: '法人',
      idNumber: '91310000MA1K4XYZ01',
      role: '原告',
      address: '上海市浦东新区世纪大道100号',
      contact: '021-58880001',
      isEnterprise: true
    }
  },
  {
    uuid: 'party-00002',
    name: '某某物流（上海）有限公司',
    type: 'Party',
    summary: '货物运输合同纠纷案被告',
    properties: {
      partyType: '法人',
      idNumber: '91310000MA1K5ABC02',
      role: '被告',
      address: '上海市嘉定区安亭镇工业园',
      contact: '021-69560001',
      isEnterprise: true
    }
  },

  // --- 案件 ---
  {
    uuid: 'case-00001',
    name: '上海某某贸易公司诉某某物流公司货物运输合同纠纷案',
    type: 'Case',
    summary: '原告上海某某贸易公司与被告某某物流公司签订货物运输合同，运输过程中货物部分损毁，案件经调解成功',
    properties: {
      caseNumber: '(2025)沪01商初1234号',
      caseType: '商事',
      caseStatus: '调解成功',
      filingDate: '2025-06-01',
      closedDate: '2025-08-15',
      amountInDispute: 580000,
      summary: '原告上海某某贸易公司与被告某某物流公司签订货物运输合同，约定被告将原告货物从上海运至广州。运输过程中，因被告管理不善导致货物部分损毁，原告遂提起诉讼，要求被告赔偿损失。案件经上海国际商事调解中心调解，双方达成调解协议。'
    }
  },
  {
    uuid: 'case-00002',
    name: '某某物流公司诉某某仓储公司仓储合同纠纷案',
    type: 'Case',
    summary: '仓储合同纠纷，货物受潮霉变，案件审理中',
    properties: {
      caseNumber: '(2025)沪浦商初5678号',
      caseType: '商事',
      caseStatus: '审理中',
      filingDate: '2025-09-01',
      amountInDispute: 1200000,
      summary: '原告某某物流公司将其客户的货物委托被告某某仓储公司保管，因被告仓库管理不当导致货物受潮霉变，双方就赔偿金额产生争议。'
    }
  },
  {
    uuid: 'case-00003',
    name: '某某投资有限公司诉某某科技公司股权投资纠纷案',
    type: 'Case',
    summary: '股权投资纠纷，对赌协议未完成，案件已结案',
    properties: {
      caseNumber: '(2025)沪01商初9012号',
      caseType: '商事',
      caseStatus: '结案',
      filingDate: '2025-03-01',
      closedDate: '2025-05-20',
      amountInDispute: 5000000,
      summary: '原告某某投资有限公司与被告某某科技公司签订股权投资协议，约定原告向被告投资500万元。被告未按约定完成业绩对赌目标，原告要求回购股权并支付违约金。'
    }
  },

  // --- 证据 ---
  {
    uuid: 'ev-00001',
    name: '货物运输合同',
    type: 'Evidence',
    summary: '原被告签订的货物运输合同，证明合同关系存在',
    properties: {
      evidenceNumber: '原告证据-001',
      evidenceType: '书证',
      content: '2025年3月15日原被告签订的货物运输合同，约定运输路线、费用及违约责任。',
      submittedBy: '原告',
      submissionDate: '2025-06-05',
      purpose: '证明原被告之间存在货物运输合同关系'
    }
  },
  {
    uuid: 'ev-00002',
    name: '货物损毁照片及鉴定报告',
    type: 'Evidence',
    summary: '第三方鉴定机构出具的货物损毁鉴定报告',
    properties: {
      evidenceNumber: '原告证据-002',
      evidenceType: '鉴定意见',
      content: '第三方鉴定机构出具的货物损毁鉴定报告，显示货物损失金额为48万元。',
      submittedBy: '原告',
      submissionDate: '2025-06-05',
      purpose: '证明货物损失的具体金额'
    }
  },
  {
    uuid: 'ev-00003',
    name: '入库单及仓储费发票',
    type: 'Evidence',
    summary: '被告出具的货物入库单据及仓储费发票',
    properties: {
      evidenceNumber: '原告证据-003',
      evidenceType: '书证',
      content: '被告出具的货物入库单据及相关仓储费用发票。',
      submittedBy: '原告',
      submissionDate: '2025-09-05',
      purpose: '证明货物已交付被告保管'
    }
  },

  // --- 裁判文书 ---
  {
    uuid: 'jd-00001',
    name: '民事调解书',
    type: 'JudgmentDocument',
    summary: '上海国际商事调解中心主持调解达成的民事调解书',
    properties: {
      documentNumber: '(2025)沪01商初1234号调',
      documentType: '调解书',
      issueDate: '2025-08-15',
      mainContent: '经上海国际商事调解中心主持调解，双方当事人自愿达成如下协议：1.被告于2025年9月30日前赔偿原告货物损失48万元；2.被告于2025年10月31日前支付逾期违约金12万元；3.双方就本案再无其他争议。',
      judgmentResult: '调解成功',
      legalBasis: '《中华人民共和国民法典》第八百三十二条、《商事调解条例》第十四条、第二十二条'
    }
  },

  // --- 调解协议 ---
  {
    uuid: 'ma-00001',
    name: '商事调解协议（2025）沪国贸调字第123号',
    type: 'MediationAgreement',
    summary: '货物运输合同纠纷调解协议，已司法确认',
    properties: {
      agreementNumber: '（2025）沪国贸调字第123号',
      mainFacts: '原告委托被告运输货物，被告在运输过程中造成货物损毁。',
      disputeItems: '货物损失金额认定、违约责任承担方式',
      agreementContent: '1.被告赔偿原告货物损失48万元；2.被告支付逾期违约金12万元；3.原告放弃其他诉讼请求。',
      performanceMethod: '一次性支付',
      performanceDeadline: '2025-10-31',
      signDate: '2025-08-10',
      judiciallyConfirmed: true
    }
  }
];

// ============================================================
// 边数据（引用节点 UUID）
// ============================================================

export const LEGAL_EDGES_DATA = [
  // --- 案件-当事人 ---
  {
    source: 'case-00001',
    target: 'party-00001',
    type: 'CASE_PARTY',
    properties: { role: '原告' }
  },
  {
    source: 'case-00001',
    target: 'party-00002',
    type: 'CASE_PARTY',
    properties: { role: '被告' }
  },

  // --- 案件-法官 ---
  {
    source: 'case-00001',
    target: 'judge-00001',
    type: 'CASE_JUDGE',
    properties: { role: '审判长' }
  },

  // --- 案件-法院 ---
  {
    source: 'case-00001',
    target: 'court-00001',
    type: 'CASE_COURT',
    properties: { courtRole: '一审法院' }
  },

  // --- 案件-法律条文 ---
  {
    source: 'case-00001',
    target: 'lp-00014',
    type: 'CASE_LEGAL_PROVISION',
    properties: { usageType: '适用', reasoning: '案件调解活动遵循自愿、合法、诚信、保密原则' }
  },
  {
    source: 'case-00001',
    target: 'lp-00022',
    type: 'CASE_LEGAL_PROVISION',
    properties: { usageType: '适用', reasoning: '调解达成协议应制作调解协议，具有法律约束力' }
  },
  {
    source: 'case-00001',
    target: 'lp-00002',
    type: 'CASE_LEGAL_PROVISION',
    properties: { usageType: '分析', reasoning: '本案属于货物运输合同纠纷，属于商事调解适用范围' }
  },
  {
    source: 'case-00001',
    target: 'lp-00015',
    type: 'CASE_LEGAL_PROVISION',
    properties: { usageType: '参照', reasoning: '当事人通过商事调解组织申请调解' }
  },

  // --- 案件-证据 ---
  {
    source: 'case-00001',
    target: 'ev-00001',
    type: 'CASE_EVIDENCE',
    properties: { evidenceRole: '原告证据', admissibility: '采纳' }
  },
  {
    source: 'case-00001',
    target: 'ev-00002',
    type: 'CASE_EVIDENCE',
    properties: { evidenceRole: '原告证据', admissibility: '采纳' }
  },

  // --- 案件-裁判文书 ---
  {
    source: 'case-00001',
    target: 'jd-00001',
    type: 'CASE_JUDGMENT',
    properties: { documentRole: '调解书' }
  },

  // --- 案件-调解协议 ---
  {
    source: 'case-00001',
    target: 'ma-00001',
    type: 'CASE_MEDIATION_AGREEMENT',
    properties: { agreementRole: '调解达成' }
  },

  // --- 案件-调解组织 ---
  {
    source: 'case-00001',
    target: 'org-00001',
    type: 'CASE_MEDIATION_ORG',
    properties: { mediationStage: '诉前调解', mediationResult: '调解成功' }
  },

  // --- 当事人-律师 ---
  {
    source: 'party-00001',
    target: 'lawyer-00001',
    type: 'PARTY_LAWYER',
    properties: { representationType: '一般代理', startDate: '2025-06-01' }
  },
  {
    source: 'party-00002',
    target: 'lawyer-00002',
    type: 'PARTY_LAWYER',
    properties: { representationType: '一般代理', startDate: '2025-06-10' }
  },

  // --- 调解组织-调解员 ---
  {
    source: 'org-00001',
    target: 'med-00001',
    type: 'ORG_MEDIATOR',
    properties: { employmentType: '专职', hireDate: '2024-01-01' }
  },
  {
    source: 'org-00001',
    target: 'med-00002',
    type: 'ORG_MEDIATOR',
    properties: { employmentType: '兼职', hireDate: '2024-06-01' }
  },
  {
    source: 'org-00002',
    target: 'med-00003',
    type: 'ORG_MEDIATOR',
    properties: { employmentType: '特聘', hireDate: '2020-01-01' }
  },

  // --- 法条-法条 ---
  {
    source: 'lp-00023',
    target: 'lp-00001',
    type: 'LEGAL_PROVISION_RELATED',
    properties: { relationType: '引用' }
  },

  // --- 案件-案件 ---
  {
    source: 'case-00001',
    target: 'case-00003',
    type: 'CASE_RELATED',
    properties: { relationType: '同一当事人' }
  }
];
