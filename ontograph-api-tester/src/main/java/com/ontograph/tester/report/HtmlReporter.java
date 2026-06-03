package com.ontograph.tester.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ontograph.tester.config.TesterConfig;
import com.ontograph.tester.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HTML报告生成器
 * 生成美观的HTML格式测试报告，可直接在浏览器中打开
 */
@Slf4j
public class HtmlReporter implements TestReporter {

    private final TesterConfig.ReportConfig reportConfig;
    private final String outputDir;
    private final String outputFile;
    private final List<TestResult> allResults = new ArrayList<>();
    private final Map<TestCase.TestPhase, List<TestResult>> phaseResults = new LinkedHashMap<>();
    private TestReport finalReport;

    public HtmlReporter(TesterConfig.ReportConfig reportConfig) {
        this.reportConfig = reportConfig;
        this.outputDir = reportConfig.getOutputDirectory();
        this.outputFile = reportConfig.getHtml().getOutputPath();
    }

    @Override
    public void onTestRunStart() {
        allResults.clear();
        phaseResults.clear();
    }

    @Override
    public void onPhaseStart(TestCase.TestPhase phase, int caseCount) {
        phaseResults.put(phase, new ArrayList<>());
    }

    @Override
    public void onTestResult(TestResult result) {
        allResults.add(result);
        phaseResults.computeIfAbsent(result.getPhase(), k -> new ArrayList<>()).add(result);
    }

    @Override
    public void onPhaseComplete(TestCase.TestPhase phase) {
        // 阶段性完成，不需要额外处理
    }

    @Override
    public void onTestRunComplete(TestReport report) {
        this.finalReport = report;
        generateHtml(report);
    }

    private void generateHtml(TestReport report) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-CN\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>OntoGraph API 测试报告 - ").append(getTimestamp()).append("</title>\n");
        html.append(getStyles());
        html.append("</head>\n");
        html.append("<body>\n");

        // 顶部导航
        html.append(getNav());

        // Hero区域
        html.append(getHero(report));

        // 概览统计
        html.append(getOverviewSection(report));

        // 模块统计
        if (!report.getModuleSummaries().isEmpty()) {
            html.append(getModuleSection(report));
        }

        // 测试详情
        html.append(getDetailSection(report));

        // 失败详情
        if (!report.getFailures().isEmpty()) {
            html.append(getFailureSection(report));
        }

        // 页脚
        html.append(getFooter(report));

        html.append("</body>\n");
        html.append("</html>");

        // 写入文件
        ensureOutputDir();
        File outputPath = new File(outputDir, outputFile);
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputPath))) {
            pw.print(html.toString());
            log.info("HTML报告已生成: {}", outputPath.getAbsolutePath());
        } catch (IOException e) {
            log.error("写入HTML报告失败: {}", e.getMessage());
        }
    }

    private String getStyles() {
        return """
            <style>
                :root {
                    --bg-primary: #0f172a;
                    --bg-secondary: #1e293b;
                    --bg-tertiary: #334155;
                    --text-primary: #f1f5f9;
                    --text-secondary: #94a3b8;
                    --text-muted: #64748b;
                    --accent-blue: #3b82f6;
                    --accent-green: #22c55e;
                    --accent-red: #ef4444;
                    --accent-yellow: #eab308;
                    --accent-purple: #a855f7;
                    --accent-cyan: #06b6d4;
                    --border-color: #334155;
                    --card-bg: #1e293b;
                    --success-bg: rgba(34,197,94,0.1);
                    --error-bg: rgba(239,68,68,0.1);
                    --warning-bg: rgba(234,179,8,0.1);
                }

                * { margin: 0; padding: 0; box-sizing: border-box; }

                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
                    background: var(--bg-primary);
                    color: var(--text-primary);
                    line-height: 1.6;
                    min-height: 100vh;
                }

                .container { max-width: 1400px; margin: 0 auto; padding: 0 24px; }

                /* Navigation */
                nav {
                    position: sticky;
                    top: 0;
                    background: rgba(15,23,42,0.95);
                    backdrop-filter: blur(12px);
                    border-bottom: 1px solid var(--border-color);
                    padding: 16px 0;
                    z-index: 100;
                }
                nav .container { display: flex; align-items: center; justify-content: space-between; }
                .nav-brand { font-size: 18px; font-weight: 700; color: var(--accent-cyan); }
                .nav-brand span { color: var(--text-secondary); font-weight: 400; }
                .nav-links { display: flex; gap: 24px; list-style: none; }
                .nav-links a {
                    color: var(--text-secondary);
                    text-decoration: none;
                    font-size: 14px;
                    transition: color 0.2s;
                }
                .nav-links a:hover { color: var(--accent-cyan); }

                /* Hero */
                .hero {
                    padding: 64px 0 48px;
                    text-align: center;
                    background: linear-gradient(180deg, rgba(6,182,212,0.05) 0%, transparent 100%);
                }
                .hero h1 {
                    font-size: 36px;
                    font-weight: 700;
                    margin-bottom: 12px;
                    background: linear-gradient(135deg, var(--accent-cyan), var(--accent-purple));
                    -webkit-background-clip: text;
                    -webkit-text-fill-color: transparent;
                }
                .hero .subtitle {
                    color: var(--text-secondary);
                    font-size: 16px;
                    margin-bottom: 8px;
                }
                .hero .timestamp {
                    color: var(--text-muted);
                    font-size: 14px;
                }

                /* Overview Cards */
                .overview {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                    gap: 16px;
                    margin: -32px 0 48px;
                }
                .card {
                    background: var(--card-bg);
                    border: 1px solid var(--border-color);
                    border-radius: 16px;
                    padding: 24px;
                    transition: transform 0.2s, box-shadow 0.2s;
                }
                .card:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 8px 24px rgba(0,0,0,0.3);
                }
                .card-label {
                    font-size: 13px;
                    color: var(--text-muted);
                    margin-bottom: 8px;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .card-value {
                    font-size: 32px;
                    font-weight: 700;
                    line-height: 1;
                    margin-bottom: 8px;
                }
                .card-sub { font-size: 13px; color: var(--text-secondary); }
                .card-total .card-value { color: var(--text-primary); }
                .card-pass .card-value { color: var(--accent-green); }
                .card-fail .card-value { color: var(--accent-red); }
                .card-rate .card-value { color: var(--accent-cyan); }
                .card-duration .card-value { color: var(--accent-purple); }

                /* Section */
                .section {
                    margin-bottom: 48px;
                }
                .section-title {
                    font-size: 20px;
                    font-weight: 600;
                    margin-bottom: 24px;
                    padding-bottom: 12px;
                    border-bottom: 1px solid var(--border-color);
                    display: flex;
                    align-items: center;
                    gap: 10px;
                }
                .section-title .icon { font-size: 24px; }

                /* Module Table */
                .module-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
                    gap: 16px;
                }
                .module-card {
                    background: var(--card-bg);
                    border: 1px solid var(--border-color);
                    border-radius: 12px;
                    padding: 20px;
                }
                .module-name {
                    font-size: 16px;
                    font-weight: 600;
                    margin-bottom: 16px;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                .module-badge {
                    font-size: 12px;
                    padding: 4px 10px;
                    border-radius: 20px;
                    font-weight: 600;
                }
                .module-stats {
                    display: grid;
                    grid-template-columns: repeat(4, 1fr);
                    gap: 12px;
                    font-size: 13px;
                }
                .module-stat { text-align: center; }
                .module-stat .val { font-size: 18px; font-weight: 700; }
                .module-stat .lbl { color: var(--text-muted); font-size: 11px; }
                .module-stat.passed .val { color: var(--accent-green); }
                .module-stat.failed .val { color: var(--accent-red); }
                .module-stat.avg .val { color: var(--accent-cyan); }

                /* Results Table */
                .results-table {
                    width: 100%;
                    border-collapse: collapse;
                    background: var(--card-bg);
                    border-radius: 12px;
                    overflow: hidden;
                    border: 1px solid var(--border-color);
                }
                .results-table thead {
                    background: var(--bg-tertiary);
                }
                .results-table th {
                    text-align: left;
                    padding: 14px 16px;
                    font-size: 12px;
                    font-weight: 600;
                    color: var(--text-secondary);
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    border-bottom: 1px solid var(--border-color);
                }
                .results-table td {
                    padding: 12px 16px;
                    font-size: 14px;
                    border-bottom: 1px solid var(--border-color);
                    vertical-align: middle;
                }
                .results-table tbody tr:hover {
                    background: rgba(255,255,255,0.02);
                }
                .results-table tbody tr:last-child td {
                    border-bottom: none;
                }

                .status-badge {
                    display: inline-flex;
                    align-items: center;
                    gap: 4px;
                    padding: 4px 10px;
                    border-radius: 20px;
                    font-size: 12px;
                    font-weight: 600;
                }
                .status-pass { background: var(--success-bg); color: var(--accent-green); }
                .status-fail { background: var(--error-bg); color: var(--accent-red); }
                .status-error { background: var(--error-bg); color: var(--accent-red); }
                .status-skip { background: var(--warning-bg); color: var(--accent-yellow); }

                .method-badge {
                    display: inline-block;
                    padding: 2px 8px;
                    border-radius: 4px;
                    font-size: 11px;
                    font-weight: 700;
                    font-family: monospace;
                }
                .method-get { background: rgba(59,130,246,0.15); color: #60a5fa; }
                .method-post { background: rgba(34,197,94,0.15); color: #4ade80; }
                .method-put { background: rgba(234,179,8,0.15); color: #facc15; }
                .method-delete { background: rgba(239,68,68,0.15); color: #f87171; }
                .method-patch { background: rgba(168,85,247,0.15); color: #c084fc; }

                .path-text {
                    font-family: 'Consolas', 'Monaco', monospace;
                    font-size: 13px;
                    color: var(--text-secondary);
                    max-width: 300px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .duration-text {
                    font-family: monospace;
                    font-size: 13px;
                }

                .phase-group {
                    margin-bottom: 32px;
                }
                .phase-header {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    margin-bottom: 16px;
                    font-size: 16px;
                    font-weight: 600;
                    color: var(--accent-cyan);
                }
                .phase-header .count {
                    font-size: 13px;
                    color: var(--text-muted);
                    font-weight: 400;
                }

                /* Failure Section */
                .failure-card {
                    background: var(--card-bg);
                    border: 1px solid var(--border-color);
                    border-radius: 12px;
                    margin-bottom: 16px;
                    overflow: hidden;
                }
                .failure-header {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 16px 20px;
                    background: rgba(239,68,68,0.05);
                    border-bottom: 1px solid var(--border-color);
                }
                .failure-title {
                    font-weight: 600;
                    font-size: 15px;
                }
                .failure-body { padding: 16px 20px; }
                .failure-row {
                    display: flex;
                    gap: 32px;
                    margin-bottom: 12px;
                    font-size: 14px;
                }
                .failure-row dt {
                    color: var(--text-muted);
                    width: 80px;
                    flex-shrink: 0;
                }
                .failure-row dd { color: var(--text-secondary); }
                .failure-reason {
                    color: var(--accent-red);
                    font-size: 14px;
                    padding: 12px 16px;
                    background: var(--error-bg);
                    border-radius: 8px;
                    margin-top: 12px;
                    font-family: monospace;
                }
                .failure-stack {
                    font-size: 12px;
                    color: var(--text-muted);
                    background: var(--bg-secondary);
                    padding: 12px;
                    border-radius: 8px;
                    margin-top: 8px;
                    max-height: 150px;
                    overflow-y: auto;
                    font-family: monospace;
                    white-space: pre-wrap;
                    word-break: break-all;
                }

                /* Footer */
                footer {
                    text-align: center;
                    padding: 32px 0;
                    color: var(--text-muted);
                    font-size: 13px;
                    border-top: 1px solid var(--border-color);
                    margin-top: 48px;
                }

                /* Responsive */
                @media (max-width: 768px) {
                    .overview { grid-template-columns: repeat(2, 1fr); }
                    .module-grid { grid-template-columns: 1fr; }
                    .results-table { font-size: 12px; }
                    .results-table th, .results-table td { padding: 10px 12px; }
                }

                /* Dark scrollbar */
                ::-webkit-scrollbar { width: 8px; height: 8px; }
                ::-webkit-scrollbar-track { background: var(--bg-secondary); }
                ::-webkit-scrollbar-thumb { background: var(--bg-tertiary); border-radius: 4px; }
                ::-webkit-scrollbar-thumb:hover { background: var(--text-muted); }
            </style>
            """;
    }

    private String getNav() {
        return """
            <nav>
                <div class="container">
                    <div class="nav-brand">OntoGraph <span>API Tester</span></div>
                    <ul class="nav-links">
                        <li><a href="#overview">概览</a></li>
                        <li><a href="#modules">模块</a></li>
                        <li><a href="#results">详情</a></li>
                        <li><a href="#failures">失败</a></li>
                    </ul>
                </div>
            </nav>
            """;
    }

    private String getHero(TestReport report) {
        String passRateStr = String.format("%.2f%%", report.getSuccessRate());
        String rateClass = report.getSuccessRate() >= 90 ? "var(--accent-green)"
                : report.getSuccessRate() >= 70 ? "var(--accent-yellow)" : "var(--accent-red)";

        return """
            <div class="hero">
                <div class="container">
                    <h1>API 测试报告</h1>
                    <div class="subtitle">%s | %s</div>
                    <div class="timestamp">%s</div>
                </div>
            </div>
            """.formatted(
                report.getTargetUrl(),
                report.getEnvironment(),
                getTimestamp()
        );
    }

    private String getOverviewSection(TestReport report) {
        return """
            <div class="container">
                <div class="overview">
                    <div class="card card-total">
                        <div class="card-label">总用例数</div>
                        <div class="card-value">%d</div>
                        <div class="card-sub">个测试用例</div>
                    </div>
                    <div class="card card-pass">
                        <div class="card-label">通过</div>
                        <div class="card-value">%d</div>
                        <div class="card-sub">测试通过</div>
                    </div>
                    <div class="card card-fail">
                        <div class="card-label">失败</div>
                        <div class="card-value">%d</div>
                        <div class="card-sub">%d错误</div>
                    </div>
                    <div class="card card-rate">
                        <div class="card-label">通过率</div>
                        <div class="card-value">%s</div>
                        <div class="card-sub">成功率</div>
                    </div>
                    <div class="card card-duration">
                        <div class="card-label">总耗时</div>
                        <div class="card-value">%s</div>
                        <div class="card-sub">平均 %.1fms</div>
                    </div>
                </div>
            </div>
            """.formatted(
                report.getTotalCases(),
                report.getPassed(),
                report.getFailed(),
                report.getError(),
                String.format("%.2f%%", report.getSuccessRate()),
                report.getDurationString(),
                report.getAvgResponseTimeMs()
        );
    }

    private String getModuleSection(TestReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"container\">\n");
        sb.append("<div id=\"modules\" class=\"section\">\n");
        sb.append("  <h2 class=\"section-title\"><span class=\"icon\">📁</span> 按模块统计</h2>\n");
        sb.append("  <div class=\"module-grid\">\n");

        for (TestReport.ModuleSummary mod : report.getModuleSummaries().values()) {
            String rateStr = String.format("%.0f%%", mod.getPassRate());
            String rateClass = mod.getPassRate() >= 90 ? "passed" : mod.getPassRate() >= 70 ? "failed" : "failed";

            sb.append("""
                <div class="module-card">
                    <div class="module-name">
                        %s
                        <span class="module-badge" style="background:%s;color:%s">%s</span>
                    </div>
                    <div class="module-stats">
                        <div class="module-stat">
                            <div class="val">%d</div>
                            <div class="lbl">总计</div>
                        </div>
                        <div class="module-stat passed">
                            <div class="val">%d</div>
                            <div class="lbl">通过</div>
                        </div>
                        <div class="module-stat failed">
                            <div class="val">%d</div>
                            <div class="lbl">失败</div>
                        </div>
                        <div class="module-stat avg">
                            <div class="val">%dms</div>
                            <div class="lbl">平均</div>
                        </div>
                    </div>
                </div>
                """.formatted(
                    mod.getModuleName(),
                    mod.getPassRate() >= 90 ? "rgba(34,197,94,0.15)" : "rgba(239,68,68,0.15)",
                    mod.getPassRate() >= 90 ? "var(--accent-green)" : "var(--accent-red)",
                    rateStr,
                    mod.getTotal(),
                    mod.getPassed(),
                    mod.getFailed(),
                    mod.getAvgResponseTimeMs()
            ));
        }

        sb.append("  </div>\n</div>\n");
        sb.append("</div>\n");
        return sb.toString();
    }

    private String getDetailSection(TestReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"container\">\n");
        sb.append("<div id=\"results\" class=\"section\">\n");
        sb.append("  <h2 class=\"section-title\"><span class=\"icon\">📋</span> 测试详情</h2>\n");

        for (Map.Entry<TestCase.TestPhase, List<TestResult>> entry : phaseResults.entrySet()) {
            List<TestResult> results = entry.getValue();
            if (results.isEmpty()) continue;

            sb.append("<div class=\"phase-group\">\n");
            sb.append("  <div class=\"phase-header\">");
            sb.append(getPhaseIcon(entry.getKey())).append(" ");
            sb.append(getPhaseName(entry.getKey()));
            sb.append(" <span class=\"count\">(").append(results.size()).append(")</span>");
            sb.append("</div>\n");

            sb.append("  <table class=\"results-table\">\n");
            sb.append("    <thead>\n");
            sb.append("      <tr><th>状态</th><th>用例名称</th><th>模块</th><th>方法</th><th>路径</th><th>耗时</th></tr>\n");
            sb.append("    </thead>\n");
            sb.append("    <tbody>\n");

            for (TestResult r : results) {
                String statusClass = switch (r.getStatus()) {
                    case PASSED -> "status-pass";
                    case FAILED -> "status-fail";
                    case ERROR -> "status-error";
                    case SKIPPED -> "status-skip";
                    default -> "";
                };
                String statusText = r.getStatus().getDescription();

                String methodClass = switch (r.getHttpMethod()) {
                    case "GET" -> "method-get";
                    case "POST" -> "method-post";
                    case "PUT" -> "method-put";
                    case "DELETE" -> "method-delete";
                    case "PATCH" -> "method-patch";
                    default -> "";
                };

                String path = r.getRequestUrl() != null
                        ? r.getRequestUrl().replace("http://localhost:8080", "")
                        : "-";

                sb.append("      <tr>\n");
                sb.append("        <td><span class=\"status-badge ").append(statusClass).append("\">")
                        .append(statusText).append("</span></td>\n");
                sb.append("        <td>").append(escHtml(r.getCaseName())).append("</td>\n");
                sb.append("        <td>").append(escHtml(r.getModule())).append("</td>\n");
                sb.append("        <td><span class=\"method-badge ").append(methodClass).append("\">")
                        .append(r.getHttpMethod()).append("</span></td>\n");
                sb.append("        <td><span class=\"path-text\" title=\"").append(escHtml(path))
                        .append("\">").append(escHtml(path)).append("</span></td>\n");
                sb.append("        <td><span class=\"duration-text\">").append(r.getResponseTimeMs())
                        .append("ms</span></td>\n");
                sb.append("      </tr>\n");
            }

            sb.append("    </tbody>\n");
            sb.append("  </table>\n");
            sb.append("</div>\n");
        }

        sb.append("</div>\n");
        sb.append("</div>\n");
        return sb.toString();
    }

    private String getFailureSection(TestReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"container\">\n");
        sb.append("<div id=\"failures\" class=\"section\">\n");
        sb.append("  <h2 class=\"section-title\"><span class=\"icon\">❌</span> 失败用例详情 (")
                .append(report.getFailures().size()).append(")</h2>\n");

        for (int i = 0; i < report.getFailures().size(); i++) {
            TestResult r = report.getFailures().get(i);
            String path = r.getRequestUrl() != null
                    ? r.getRequestUrl().replace("http://localhost:8080", "") : "-";

            sb.append("""
                <div class="failure-card">
                    <div class="failure-header">
                        <div class="failure-title">%d. %s</div>
                        <span class="status-badge status-fail">%s</span>
                    </div>
                    <div class="failure-body">
                        <dl class="failure-row">
                            <dt>模块</dt><dd>%s</dd>
                            <dt>方法</dt><dd>%s %s</dd>
                            <dt>耗时</dt><dd>%dms</dd>
                            <dt>HTTP</dt><dd>%d</dd>
                        </dl>
            """.formatted(
                i + 1,
                escHtml(r.getCaseName()),
                r.getStatus().getDescription(),
                escHtml(r.getModule()),
                r.getHttpMethod(),
                escHtml(path),
                r.getResponseTimeMs(),
                r.getHttpStatusCode()
            ));

            if (r.getFailureReason() != null) {
                sb.append("      <div class=\"failure-reason\">").append(escHtml(r.getFailureReason())).append("</div>\n");
            }

            if (r.getStackTrace() != null && reportConfig.getConsole().isShowStackTrace()) {
                sb.append("      <div class=\"failure-stack\">").append(escHtml(r.getStackTrace())).append("</div>\n");
            }

            sb.append("    </div>\n");
            sb.append("  </div>\n");
        }

        sb.append("</div>\n");
        sb.append("</div>\n");
        return sb.toString();
    }

    private String getFooter(TestReport report) {
        return """
            <footer>
                <div class="container">
                    <p>由 <strong>OntoGraph API Tester</strong> 自动生成 | 测试时间: %s</p>
                    <p>目标服务: %s | 环境: %s</p>
                </div>
            </footer>
            """.formatted(
                getTimestamp(),
                report.getTargetUrl(),
                report.getEnvironment()
        );
    }

    private void ensureOutputDir() {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String getPhaseIcon(TestCase.TestPhase phase) {
        return switch (phase) {
            case SETUP -> "🔐";
            case CREATE -> "➕";
            case READ -> "🔍";
            case UPDATE -> "✏️";
            case DELETE -> "🗑️";
            case CLEANUP -> "🧹";
            case INDEPENDENT -> "⚡";
        };
    }

    private String getPhaseName(TestCase.TestPhase phase) {
        return switch (phase) {
            case SETUP -> "认证与准备";
            case CREATE -> "创建资源";
            case READ -> "查询资源";
            case UPDATE -> "更新资源";
            case DELETE -> "删除资源";
            case CLEANUP -> "清理数据";
            case INDEPENDENT -> "独立测试";
        };
    }

    private String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String escHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @Override
    public String getReportPath() {
        return new File(outputDir, outputFile).getAbsolutePath();
    }
}
