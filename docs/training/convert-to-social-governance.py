#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将法律知识图谱培训文档转换为社会治理领域培训文档
"""

import re

# 读取原文档
with open('ontology-training-guide.md', 'r', encoding='utf-8') as f:
    content = f.read()

# 定义替换规则（按优先级排序）
replacements = [
    # 文档元信息
    ("OntoGraph 知识图谱与本体论全栈技术培训", "OntoGraph 知识图谱与本体论全栈技术培训（社会治理版）"),
    ("2026-05-21", "2026-05-25"),
    
    # 主线案例替换
    ("徐某骥与上海某物业管理有限公司公司解散纠纷案", "张某新与某网络借贷平台网贷利率纠纷案"),
    ("徐某骥与王某芬家庭纠纷案", "张某新与某网络借贷平台网贷利率纠纷案"),
    ("徐某骥", "张某新"),
    ("上海某物业管理有限公司", "某网络借贷平台"),
    ("公司解散纠纷", "网贷利率纠纷"),
    
    # 当事人相关
    ("原告", "申请人"),
    ("被告", "被申请人"),
    ("partyRole.*?原告", "partyRole\": \"申请人\""),
    
    # 法院/调解员相关
    ("上海市长宁区人民法院", "深圳市南山区人民调解委员会"),
    ("上海市第一中级人民法院", "深圳市中级人民法院"),
    ("法院", "调解组织"),
    ("Court", "Mediator"),
    ("审判机关", "调解组织"),
    ("法官", "调解员"),
    ("Judge", "Mediator"),
    
    # 案件/纠纷相关
    ("Case", "Dispute"),
    ("案件", "纠纷"),
    ("caseNumber", "disputeNumber"),
    ("caseType", "disputeCategory"),
    ("filingDate", "filingDate"),
    ("（2022）沪0105民初21387号", "SZ2024-0001"),
    ("（2023）沪01民终11293号", "SZ2024-0002"),
    
    # 法律条文
    ("LegalProvision", "LegalProvision"),
    ("《公司法》第182条", "《民法典》第680条"),
    ("《民法典》第580条", "《民法典》第686条"),
    ("公司法", "民法典"),
    
    # URI命名空间
    ("http://legal-ai.cc/ontology", "http://social-gov.cc/ontology"),
    ("legal-ai.cc", "social-gov.cc"),
    
    # Graph ID
    ("legal-kg", "social-gov-kg"),
    ("legal-knowledge-graph", "social-governance-graph"),
    
    # 领域提示
    ("法律", "社会治理"),
    ("LEGAL", "GOVERNANCE"),
    ("KNOWLEDGE", "GOVERNANCE"),
    
    # 业务术语
    ("裁判文书", "调解协议书"),
    ("JUDGMENT_DOCUMENT", "MEDIATION_AGREEMENT"),
    ("COURT_JUDGMENT", "MEDIATION_RECORD"),
    ("司法程序", "调解程序"),
    ("一审判决", "调解协议"),
    ("二审判决", "调解复查"),
    ("立案", "受理"),
    ("裁判", "调解"),
    ("类案检索", "类案推荐"),
    ("裁判规律", "调解规律"),
    
    # 纠纷类型
    ("民事案件", "民事纠纷"),
    ("刑事案件", "刑事纠纷"),
    ("行政案件", "行政纠纷"),
    
    # 约束示例中的法律术语
    ("当事人类型必须是:自然人、法人或非法人组织", "当事人类型必须是:自然人、法人或非法人组织"),
    ("当事人诉讼角色枚举约束", "当事人纠纷角色枚举约束"),
    
    # 社区相关
    ("案件簇", "纠纷簇"),
    ("法规簇", "法条簇"),
    ("案件社区", "纠纷社区"),
    
    # 关系类型
    ("CASE_PARTY", "DISPUTE_PARTY"),
    ("CASE_COURT", "DISPUTE_MEDIATOR"),
    ("CASE_JUDGE", "DISPUTE_MEDIATOR"),
    ("CASE_LEGAL_BASIS", "DISPUTE_LEGAL_BASIS"),
    ("CASE_EVIDENCE", "DISPUTE_EVIDENCE"),
    ("APPEALED_CASE", "MEDIATION_AGREEMENT"),
    
    # 描述文本更新
    ("法律知识图谱", "社会治理知识图谱"),
    ("法律领域", "社会治理领域"),
    ("公司法领域", "社会治理领域"),
    ("中国民商事法律", "社会矛盾纠纷调解"),
]

# 执行替换
for old, new in replacements:
    content = content.replace(old, new)

# 更新文档导览
content = content.replace(
    "系统讲解知识图谱与本体论的理论知识及Graphiti-Java项目的完整实现",
    "系统讲解知识图谱与本体论的理论知识及OntoGraph项目在社会治理领域的完整实现",
)

# 更新适用对象行后的业务领域说明
content = content.replace(
    "**技术栈**: Java 21 + Spring Boot 3.x + Apache Jena + Neo4j 5.x + Vue 3 + TypeScript  \n> **更新日期**",
    "**技术栈**: Java 21 + Spring Boot 3.x + Apache Jena + Neo4j 5.x + Vue 3 + TypeScript  \n> **业务领域**: 社会综合治理（矛盾纠纷调解）  \n> **更新日期**"
)

# 写入新文档
with open('social-governance-training-guide.md', 'w', encoding='utf-8') as f:
    f.write(content)

print("✓ 社会治理培训文档生成成功！")
print(f"✓ 原文档行数: {content.count(chr(10)) + 1}")
print("✓ 新文档已保存为: social-governance-training-guide.md")
