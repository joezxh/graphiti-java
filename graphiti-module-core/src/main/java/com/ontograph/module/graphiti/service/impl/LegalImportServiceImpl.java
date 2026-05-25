package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.*;
import com.graphiti.module.graphiti.vo.legal.ImportLegalKGReqVO;
import com.graphiti.module.graphiti.vo.legal.LegalImportResultRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 法律知识图谱批量导入服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegalImportServiceImpl implements LegalImportService {

    private final NodeService nodeService;
    private final EdgeService edgeService;
    private final GraphNeo4jService graphNeo4jService;
    private final EmbedderService embedderService;

    private static final String GRAPH_ID = "legal-knowledge-graph";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LegalImportResultRespVO importLegalKG(ImportLegalKGReqVO reqVO) {
        String graphId = reqVO.getGraphId();
        List<Map<String, Object>> nodes = reqVO.getNodes();
        List<Map<String, Object>> edges = reqVO.getEdges();

        LegalImportResultRespVO result = new LegalImportResultRespVO();
        result.setGraphId(graphId);
        result.setNodeErrors(new ArrayList<>());
        result.setEdgeErrors(new ArrayList<>());

        // 1. 导入节点
        if (nodes != null && !nodes.isEmpty()) {
            int successCount = importLegalNodes(graphId, nodes);
            result.setNodeCount(successCount);
            result.setNodeErrors(collectErrors(nodes, successCount));
        } else {
            result.setNodeCount(0);
        }

        // 2. 导入边（需要先建立名称->UUID 映射）
        if (edges != null && !edges.isEmpty()) {
            int successCount = importLegalEdges(graphId, edges);
            result.setEdgeCount(successCount);
            result.setEdgeErrors(collectEdgeErrors(edges, successCount));
        } else {
            result.setEdgeCount(0);
        }

        log.info("法律图谱导入完成: graphId={}, nodes={}, edges={}",
                graphId, result.getNodeCount(), result.getEdgeCount());
        return result;
    }

    @Override
    public int importLegalNodes(String graphId, List<Map<String, Object>> nodes) {
        int successCount = 0;
        for (Map<String, Object> nodeData : nodes) {
            try {
                nodeService.createNode(graphId, nodeData);
                successCount++;
                log.debug("节点导入成功: name={}, type={}",
                        nodeData.get("name"), nodeData.get("type"));
            } catch (Exception e) {
                log.warn("节点导入失败: name={}, error={}",
                        nodeData.get("name"), e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    public int importLegalEdges(String graphId, List<Map<String, Object>> edges) {
        // 建立名称->UUID 映射
        Map<String, String> nameToUuid = buildNameToUuidMap(graphId);

        int successCount = 0;
        for (Map<String, Object> edgeData : edges) {
            try {
                String sourceName = (String) edgeData.get("sourceName");
                String targetName = (String) edgeData.get("targetName");
                String sourceUuid = nameToUuid.get(sourceName);
                String targetUuid = nameToUuid.get(targetName);

                if (sourceUuid == null || targetUuid == null) {
                    log.warn("边导入失败，节点未找到: source={}, target={}", sourceName, targetName);
                    continue;
                }

                Map<String, Object> props = new HashMap<>();
                if (edgeData.containsKey("properties")) {
                    Object propsObj = edgeData.get("properties");
                    if (propsObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> propsMap = (Map<String, Object>) propsObj;
                        props.putAll(propsMap);
                    }
                }

                String edgeType = (String) edgeData.get("type");
                String fact = (String) edgeData.getOrDefault("fact", "");
                if (fact.isEmpty()) {
                    fact = edgeType + ": " + sourceName + " -> " + targetName;
                }

                float[] embedding = embedderService.embed(fact);
                String edgeUuid = UUID.randomUUID().toString().replace("-", "");

                graphNeo4jService.createRelationship(
                        graphId, edgeUuid, sourceUuid, targetUuid,
                        edgeType, fact, embedding, props);
                successCount++;
                log.debug("边导入成功: {} -[{}]-> {}", sourceName, edgeType, targetName);
            } catch (Exception e) {
                log.warn("边导入失败: error={}", e.getMessage());
            }
        }
        return successCount;
    }

    @Override
    public int importCommercialMediationProvisions(String graphId) {
        List<Map<String, Object>> provisions = buildCommercialMediationProvisions();
        return importLegalNodes(graphId, provisions);
    }

    @Override
    public int importSampleCases(String graphId) {
        List<Map<String, Object>> cases = buildSampleCases();
        List<Map<String, Object>> parties = buildSampleParties();
        List<Map<String, Object>> courts = buildSampleCourts();
        List<Map<String, Object>> judges = buildSampleJudges();
        List<Map<String, Object>> lawyers = buildSampleLawyers();

        int count = 0;
        count += importLegalNodes(graphId, cases);
        count += importLegalNodes(graphId, parties);
        count += importLegalNodes(graphId, courts);
        count += importLegalNodes(graphId, judges);
        count += importLegalNodes(graphId, lawyers);
        return count;
    }

    @Override
    public Map<String, Object> exportLegalKG(String graphId) {
        Map<String, Object> result = new HashMap<>();
        result.put("graphId", graphId);

        // 导出节点
        List<Map<String, Object>> allNodes = graphNeo4jService.listNodes(graphId, 0, 10000);
        result.put("nodeCount", allNodes.size());
        result.put("nodes", allNodes);

        // 导出边（通过遍历查询）
        List<Map<String, Object>> allEdges = graphNeo4jService.listEdges(graphId, null, null, null, 0, 10000);
        result.put("edgeCount", allEdges.size());
        result.put("edges", allEdges);

        return result;
    }

    // ==================== 私有辅助方法 ====================

    private Map<String, String> buildNameToUuidMap(String graphId) {
        Map<String, String> map = new HashMap<>();
        List<Map<String, Object>> nodes = graphNeo4jService.listNodes(graphId, 0, 10000);
        for (Map<String, Object> node : nodes) {
            String name = (String) node.get("name");
            String uuid = (String) node.get("uuid");
            if (name != null && uuid != null) {
                map.put(name, uuid);
            }
        }
        return map;
    }

    private List<String> collectErrors(List<Map<String, Object>> nodes, int successCount) {
        // 简化：返回失败数量
        List<String> errors = new ArrayList<>();
        int failCount = nodes.size() - successCount;
        if (failCount > 0) {
            errors.add("共 " + failCount + " 个节点导入失败，请检查日志");
        }
        return errors;
    }

    private List<String> collectEdgeErrors(List<Map<String, Object>> edges, int successCount) {
        List<String> errors = new ArrayList<>();
        int failCount = edges.size() - successCount;
        if (failCount > 0) {
            errors.add("共 " + failCount + " 条边导入失败，请检查日志");
        }
        return errors;
    }

    // 商事调解条例完整33条
    private List<Map<String, Object>> buildCommercialMediationProvisions() {
        List<Map<String, Object>> provisions = new ArrayList<>();

        provisions.add(buildProvision("商事调解条例第一条", "第一条",
                "为了规范商事调解活动，有效解决商事争议，保护当事人合法权益，促进商事调解行业发展，优化营商环境，制定本条例。",
                "商事调解条例", "2026-05-01", "商事调解,目的,范围"));

        provisions.add(buildProvision("商事调解条例第二条", "第二条",
                "本条例所称商事调解活动，是指在商事调解组织主持下，当事人自愿友好协商解决贸易、投资、金融、运输、房地产、工程建设、知识产权等领域商事争议的活动。婚姻家庭、继承、监护、劳动人事、消费者权益争议以及依法应当以其他方式解决的争议，不适用商事调解。",
                "商事调解条例", "2026-05-01", "商事调解,定义,适用范围"));

        provisions.add(buildProvision("商事调解条例第三条", "第三条",
                "商事调解行业发展贯彻落实党和国家路线方针政策、决策部署，坚持为民服务宗旨，服务国家高质量发展和高水平对外开放。",
                "商事调解条例", "2026-05-01", "行业发展,国家政策"));

        provisions.add(buildProvision("商事调解条例第四条", "第四条",
                "国务院司法行政部门负责指导、规范全国商事调解工作，统筹规划商事调解行业发展。",
                "商事调解条例", "2026-05-01", "主管部门,司法行政"));

        provisions.add(buildProvision("商事调解条例第五条", "第五条",
                "商事调解行业自律组织依照法律法规和章程开展行业自律，接受司法行政部门的指导、监督。",
                "商事调解条例", "2026-05-01", "行业自律"));

        provisions.add(buildProvision("商事调解条例第六条", "第六条",
                "国家培育有国际影响力的商事调解组织，提升商事调解组织的国际竞争力。",
                "商事调解条例", "2026-05-01", "国际竞争力"));

        provisions.add(buildProvision("商事调解条例第七条", "第七条",
                "国家完善商事调解与诉讼、仲裁、公证等制度的衔接机制，畅通商事争议解决途径。",
                "商事调解条例", "2026-05-01", "制度衔接,争议解决"));

        provisions.add(buildProvision("商事调解条例第八条", "第八条",
                "设立商事调解组织，应当符合下列条件：（一）发起人为非营利法人；（二）有规范的名称，名称中含有\"商事调解\"字样；（三）有自己的住所和章程；（四）有30万元以上的资产；（五）有5名以上商事调解员和适当数量的专职工作人员。",
                "商事调解条例", "2026-05-01", "设立条件,商事调解组织"));

        provisions.add(buildProvision("商事调解条例第九条", "第九条",
                "设立商事调解组织，应当向所在地设区的市级人民政府司法行政部门提出申请，提交设立申请书及相关材料。",
                "商事调解条例", "2026-05-01", "申请程序"));

        provisions.add(buildProvision("商事调解条例第十条", "第十条",
                "商事调解组织变更名称、住所、章程等事项的，应当依法办理执业证书的变更手续。",
                "商事调解条例", "2026-05-01", "变更手续"));

        provisions.add(buildProvision("商事调解条例第十一条", "第十一条",
                "省、自治区、直辖市人民政府司法行政部门应当编制本行政区域内的商事调解组织名册，并向社会公布。",
                "商事调解条例", "2026-05-01", "名册管理"));

        provisions.add(buildProvision("商事调解条例第十二条", "第十二条",
                "商事调解组织聘任的商事调解员应当公道正派，具备良好的专业素质。商事调解员应当符合下列条件之一：（一）通过国家统一法律职业资格考试取得法律职业资格，从事调解工作满3年；（二）从事律师、仲裁、公证工作满3年或者曾任法官、检察官满3年；（三）具有法律、经济、科学技术等相关专业知识，从事法律、经济贸易等专业工作，并具有中级以上职称或者具有同等专业水平；（四）本条例施行前已从事商事调解工作满3年，并具有大学本科以上学历。",
                "商事调解条例", "2026-05-01", "调解员资格"));

        provisions.add(buildProvision("商事调解条例第十三条", "第十三条",
                "商事调解组织应当建立业务管理、利益冲突审查、投诉处理等内部管理制度。",
                "商事调解条例", "2026-05-01", "内部管理"));

        provisions.add(buildProvision("商事调解条例第十四条", "第十四条",
                "商事调解活动应当遵循自愿、合法、诚信、保密的原则。",
                "商事调解条例", "2026-05-01", "基本原则,自愿,合法,诚信,保密"));

        provisions.add(buildProvision("商事调解条例第十五条", "第十五条",
                "发生商事争议的，当事人可以向商事调解组织申请调解。当事人一方明确拒绝调解的，不得调解。当事人可以从商事调解组织的商事调解员名册中共同选定商事调解员进行调解，或者由当事人共同委托商事调解组织推荐商事调解员进行调解。",
                "商事调解条例", "2026-05-01", "申请调解,选择调解员"));

        provisions.add(buildProvision("商事调解条例第十六条", "第十六条",
                "商事调解组织可以收取商事调解费用。商事调解组织应当按照公平、合理的原则制定商事调解费用标准，并向社会公开。",
                "商事调解条例", "2026-05-01", "调解费用,收费标准"));

        provisions.add(buildProvision("商事调解条例第十七条", "第十七条",
                "商事调解员开展调解活动应当依照法律法规，可以适用行业规则、商业惯例、交易习惯等。商事调解员在调解过程中应当保持中立，勤勉尽责，遵守职业道德和执业行为规范，不得与当事人串通进行虚假调解活动。",
                "商事调解条例", "2026-05-01", "调解员职责,中立,诚信"));

        provisions.add(buildProvision("商事调解条例第十八条", "第十八条",
                "鼓励商事调解组织运用人工智能、大数据等技术手段，提高商事调解质量和效率。商事调解员与当事人协商采取在线方式进行调解的，与线下调解活动具有同等法律效力。",
                "商事调解条例", "2026-05-01", "技术手段,在线调解"));

        provisions.add(buildProvision("商事调解条例第十九条", "第十九条",
                "商事调解不公开进行。当事人约定公开的，可以公开进行，但涉及国家秘密、他人的商业秘密或者个人隐私的除外。",
                "商事调解条例", "2026-05-01", "保密原则"));

        provisions.add(buildProvision("商事调解条例第二十条", "第二十条",
                "商事调解员与争议事项有利害关系，或者存在其他可能导致当事人对其中立性、公正性产生合理怀疑情形的，该商事调解员应当及时向当事人披露，并退出调解。",
                "商事调解条例", "2026-05-01", "利益冲突,回避制度"));

        provisions.add(buildProvision("商事调解条例第二十一条", "第二十一条",
                "经商事调解无法达成协议，当事人不同意继续调解，或者存在当事人意图利用调解达到非法目的等情形的，应当终止调解。",
                "商事调解条例", "2026-05-01", "终止调解"));

        provisions.add(buildProvision("商事调解条例第二十二条", "第二十二条",
                "经商事调解达成协议的，除当事人另有约定外，应当制作商事调解协议，载明主要事实、争议事项和当事人达成协议的主要内容、履行方式与期限等。商事调解员应当在商事调解协议上签名并加盖商事调解组织的印章。",
                "商事调解条例", "2026-05-01", "调解协议,法律效力"));

        provisions.add(buildProvision("商事调解条例第二十三条", "第二十三条",
                "当事人可以就商事调解协议申请司法确认，具体依照《中华人民共和国民事诉讼法》有关规定办理。",
                "商事调解条例", "2026-05-01", "司法确认,民事诉讼法"));

        provisions.add(buildProvision("商事调解条例第二十四条", "第二十四条",
                "支持商事调解组织到境外设立业务机构，开展商事调解活动。",
                "商事调解条例", "2026-05-01", "境外机构"));

        provisions.add(buildProvision("商事调解条例第二十五条", "第二十五条",
                "鼓励商事调解组织、商事调解行业自律组织与境外商事调解组织、国际组织开展交流合作，积极参与国际商事调解规则制定，加强国际商事调解人才培养。",
                "商事调解条例", "2026-05-01", "国际合作"));

        provisions.add(buildProvision("商事调解条例第二十六条", "第二十六条",
                "支持粤港澳大湾区商事调解规则衔接、机制对接，促进粤港澳大湾区商事调解协同发展。",
                "商事调解条例", "2026-05-01", "粤港澳大湾区"));

        provisions.add(buildProvision("商事调解条例第二十七条", "第二十七条",
                "县级以上地方人民政府司法行政部门对商事调解组织开展商事调解活动进行监督管理，可以采取现场检查、查阅和复制有关资料、对有关情况进行调查、对有关人员进行约谈等措施。",
                "商事调解条例", "2026-05-01", "监督管理"));

        provisions.add(buildProvision("商事调解条例第二十八条", "第二十八条",
                "未经司法行政部门批准，擅自以商事调解组织名义开展本条例规定的商事调解活动的，由省、自治区、直辖市或者设区的市级人民政府司法行政部门责令改正，处10万元以上30万元以下的罚款；有违法所得的，没收违法所得。",
                "商事调解条例", "2026-05-01", "法律责任,处罚"));

        provisions.add(buildProvision("商事调解条例第二十九条", "第二十九条",
                "商事调解组织未按照本条例规定办理变更、注销手续，公开相关信息的，由县级以上人民政府司法行政部门责令改正，给予警告；拒不改正的，责令停业整顿，可以并处1万元以上5万元以下的罚款。",
                "商事调解条例", "2026-05-01", "法律责任"));

        provisions.add(buildProvision("商事调解条例第三十条", "第三十条",
                "商事调解员未履行保密义务造成严重后果，或者与当事人串通进行虚假调解活动，损害国家利益、社会公共利益和他人合法权益的，由县级以上人民政府司法行政部门责令改正，给予警告，并处1万元以上10万元以下的罚款；情节严重的，责令暂停1年以上3年以下商事调解业务；有违法所得的，没收违法所得。",
                "商事调解条例", "2026-05-01", "调解员责任"));

        provisions.add(buildProvision("商事调解条例第三十一条", "第三十一条",
                "国务院司法行政部门依照本条例制定商事调解组织管理的具体办法。",
                "商事调解条例", "2026-05-01", "实施细则"));

        provisions.add(buildProvision("商事调解条例第三十二条", "第三十二条",
                "行业协会商会等开展公益性调解活动，不适用本条例。",
                "商事调解条例", "2026-05-01", "例外规定"));

        provisions.add(buildProvision("商事调解条例第三十三条", "第三十三条",
                "本条例自2026年5月1日起施行。",
                "商事调解条例", "2026-05-01", "施行日期"));

        return provisions;
    }

    private Map<String, Object> buildProvision(String name, String articleNumber,
            String content, String lawName, String effectiveDate, String keywords) {
        Map<String, Object> props = new HashMap<>();
        props.put("provisionId", lawName + articleNumber);
        props.put("articleNumber", articleNumber);
        props.put("content", content);
        props.put("lawName", lawName);
        props.put("lawType", "行政法规");
        props.put("effectiveDate", effectiveDate);
        props.put("keywords", keywords);

        Map<String, Object> node = new HashMap<>();
        node.put("name", name);
        node.put("type", "LegalProvision");
        node.put("summary", lawName + " " + articleNumber + "：" + content.substring(0, Math.min(50, content.length())) + "...");
        node.put("properties", props);
        return node;
    }

    private List<Map<String, Object>> buildSampleCases() {
        List<Map<String, Object>> cases = new ArrayList<>();

        Map<String, Object> c1 = new HashMap<>();
        c1.put("name", "上海某某贸易公司诉某某物流公司货物运输合同纠纷案");
        c1.put("type", "Case");
        c1.put("summary", "货物运输合同纠纷，货物部分损毁，调解成功");
        Map<String, Object> c1p = new HashMap<>();
        c1p.put("caseNumber", "(2025)沪01商初1234号");
        c1p.put("caseType", "商事");
        c1p.put("caseStatus", "调解成功");
        c1p.put("filingDate", "2025-06-01");
        c1p.put("closedDate", "2025-08-15");
        c1p.put("amountInDispute", 580000);
        c1p.put("summary", "原告上海某某贸易公司与被告某某物流公司签订货物运输合同，约定被告将原告货物从上海运至广州。运输过程中，因被告管理不善导致货物部分损毁，原告遂提起诉讼，要求被告赔偿损失。案件经上海国际商事调解中心调解，双方达成调解协议。");
        c1.put("properties", c1p);
        cases.add(c1);

        return cases;
    }

    private List<Map<String, Object>> buildSampleParties() {
        List<Map<String, Object>> parties = new ArrayList<>();

        Map<String, Object> p1 = new HashMap<>();
        p1.put("name", "上海某某贸易有限公司");
        p1.put("type", "Party");
        p1.put("summary", "货物运输合同纠纷案原告");
        Map<String, Object> p1p = new HashMap<>();
        p1p.put("partyType", "法人");
        p1p.put("idNumber", "91310000MA1K4XYZ01");
        p1p.put("role", "原告");
        p1p.put("address", "上海市浦东新区世纪大道100号");
        p1p.put("isEnterprise", true);
        p1.put("properties", p1p);
        parties.add(p1);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("name", "某某物流（上海）有限公司");
        p2.put("type", "Party");
        p2.put("summary", "货物运输合同纠纷案被告");
        Map<String, Object> p2p = new HashMap<>();
        p2p.put("partyType", "法人");
        p2p.put("idNumber", "91310000MA1K5ABC02");
        p2p.put("role", "被告");
        p2p.put("address", "上海市嘉定区安亭镇工业园");
        p2p.put("isEnterprise", true);
        p2.put("properties", p2p);
        parties.add(p2);

        return parties;
    }

    private List<Map<String, Object>> buildSampleCourts() {
        List<Map<String, Object>> courts = new ArrayList<>();

        Map<String, Object> c = new HashMap<>();
        c.put("name", "上海市第一中级人民法院");
        c.put("type", "Court");
        c.put("summary", "上海市中级人民法院，管辖一审商事案件");
        Map<String, Object> cp = new HashMap<>();
        cp.put("level", "中级人民法院");
        cp.put("location", "上海市");
        cp.put("jurisdiction", "上海市辖区一审商事案件");
        cp.put("parentCourt", "上海市高级人民法院");
        c.put("properties", cp);
        courts.add(c);

        return courts;
    }

    private List<Map<String, Object>> buildSampleJudges() {
        List<Map<String, Object>> judges = new ArrayList<>();

        Map<String, Object> j = new HashMap<>();
        j.put("name", "陈建华");
        j.put("type", "Judge");
        j.put("summary", "上海市第一中级人民法院审判长");
        Map<String, Object> jp = new HashMap<>();
        jp.put("title", "审判长");
        jp.put("courtName", "上海市第一中级人民法院");
        jp.put("specialty", "商事审判,公司纠纷");
        j.put("properties", jp);
        judges.add(j);

        return judges;
    }

    private List<Map<String, Object>> buildSampleLawyers() {
        List<Map<String, Object>> lawyers = new ArrayList<>();

        Map<String, Object> l = new HashMap<>();
        l.put("name", "赵海涛");
        l.put("type", "Lawyer");
        l.put("summary", "上海海华律师事务所律师");
        Map<String, Object> lp = new HashMap<>();
        lp.put("licenseNumber", "3110119991000123");
        lp.put("firmName", "上海海华律师事务所");
        lp.put("specialty", "商事诉讼,国际贸易");
        l.put("properties", lp);
        lawyers.add(l);

        return lawyers;
    }
}
