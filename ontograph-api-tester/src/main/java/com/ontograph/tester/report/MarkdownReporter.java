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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Markdown报告生成器
 * 生成易读的Markdown格式测试报告
 */
@Slf4j
public class MarkdownReporter implements TestReporter {

    private final TesterConfig.ReportConfig reportConfig;
    private final String outputDir;
    private final String outputFile;
    private final ObjectMapper mapper;
    private StringBuilder sb;
    private final List<TestResult> allResults = new ArrayList<>();

    public MarkdownReporter(TesterConfig.ReportConfig reportConfig) {
        this.reportConfig = reportConfig;
        this.outputDir = reportConfig.getOutputDirectory();
        this.outputFile = reportConfig.getMarkdown().getOutputPath();
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
    }

    @Override
    public void onTestRunStart() {
        sb = new StringBuilder();
        writeHeader();
    }

    private void writeHeader() {
        sb.append("# OntoGraph API 测试报告\n\n");
        sb.append("> 生成时间: `").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("`\n\n");
    }

    @Override
    public void onPhaseStart(TestCase.TestPhase phase, int caseCount) {
        sb.append("\n## ").append(getPhaseEmoji(phase)).append(" ").append(getPhaseName(phase))
                .append(" (`").append(caseCount).append("` 个用例)\n\n");
        sb.append("| # | 用例名称 | 模块 | 方法 | 路径 | 状态 | 耗时 | 详情 |\n");
        sb.append("|---|---------|------|------|------|------|------|------|\n");
    }

    @Override
    public void onTestResult(TestResult result) {
        allResults.add(result);

        String idx = String.valueOf(allResults.size());
        String name = escapeMarkdown(result.getCaseName());
        String module = result.getModule() != null ? result.getModule() : "-";
        String method = result.getHttpMethod() != null ? result.getHttpMethod() : "-";
        String path = truncateUrl(result.getRequestUrl());
        String status = getStatusMarkdown(result.getStatus());
        String duration = result.getResponseTimeMs() + "ms";
        String detail = result.getFailureReason() != null ? escapeMarkdown(result.getFailureReason()) : "";

        sb.append("| ").append(idx)
                .append(" | ").append(name)
                .append(" | ").append(module)
                .append(" | ").append(badge(method))
                .append(" | ").append(code(path))
                .append(" | ").append(status)
                .append(" | ").append(duration)
                .append(" | ").append(detail)
                .append(" |\n");
    }

    @Override
    public void onPhaseComplete(TestCase.TestPhase phase) {
        sb.append("\n");
        // 阶段小计由 onTestRunComplete 统一汇总
    }

    @Override
    public void onTestRunComplete(TestReport report) {
        writeSummary(report);
        writeModuleSummary(report);
        writeFailureDetails(report);
        writeFooter(report);

        // 写入文件
        ensureOutputDir();
        File outputPath = new File(outputDir, outputFile);
        try (PrintWriter pw = new PrintWriter(new FileWriter(outputPath))) {
            pw.print(sb.toString());
            log.info("Markdown报告已生成: {}", outputPath.getAbsolutePath());
        } catch (IOException e) {
            log.error("写入Markdown报告失败: {}", e.getMessage());
        }
    }

    private void writeSummary(TestReport report) {
        sb.append("---\n\n");
        sb.append("## 📊 测试概览\n\n");

        String passRate = String.format("%.2f%%", report.getSuccessRate());
        String rateEmoji = report.getSuccessRate() >= 90 ? "🟢" : report.getSuccessRate() >= 70 ? "🟡" : "🔴";

        sb.append("| 指标 | 值 |\n");
        sb.append("|------|----|\n");
        sb.append("| 目标服务 | `").append(report.getTargetUrl()).append("` |\n");
        sb.append("| 测试环境 | `").append(report.getEnvironment()).append("` |\n");
        sb.append("| 总用例数 | **").append(report.getTotalCases()).append("** |\n");
        sb.append("| ").append(emojiForStatus(TestResult.Status.PASSED)).append(" 通过 | `").append(report.getPassed()).append("` |\n");
        if (report.getFailed() > 0) {
            sb.append("| ").append(emojiForStatus(TestResult.Status.FAILED)).append(" 失败 | `").append(report.getFailed()).append("` |\n");
        }
        if (report.getError() > 0) {
            sb.append("| ").append(emojiForStatus(TestResult.Status.ERROR)).append(" 错误 | `").append(report.getError()).append("` |\n");
        }
        if (report.getSkipped() > 0) {
            sb.append("| ").append(emojiForStatus(TestResult.Status.SKIPPED)).append(" 跳过 | `").append(report.getSkipped()).append("` |\n");
        }
        sb.append("| 成功率 | **").append(rateEmoji).append(" `").append(passRate).append("`** |\n");
        sb.append("| 总耗时 | `").append(report.getDurationString()).append("` |\n");
        sb.append("| 平均响应 | `").append(String.format("%.2fms", report.getAvgResponseTimeMs())).append("` |\n");
        sb.append("\n");
    }

    private void writeModuleSummary(TestReport report) {
        if (report.getModuleSummaries().isEmpty()) return;

        sb.append("\n## 📁 按模块统计\n\n");
        sb.append("| 模块 | 总计 | 通过 | 失败 | 错误 | 跳过 | 通过率 | 平均耗时 |\n");
        sb.append("|------|------|------|------|------|------|--------|----------|\n");

        // 按通过率排序
        List<TestReport.ModuleSummary> sorted = report.getModuleSummaries().values().stream()
                .sorted(Comparator.comparingDouble(TestReport.ModuleSummary::getPassRate))
                .toList();

        for (TestReport.ModuleSummary mod : sorted) {
            String rate = String.format("%.1f%%", mod.getPassRate());
            String rateColor = mod.getPassRate() >= 90 ? "🟢" : mod.getPassRate() >= 70 ? "🟡" : "🔴";
            sb.append("| ").append(mod.getModuleName())
                    .append(" | ").append(mod.getTotal())
                    .append(" | ").append(mod.getPassed())
                    .append(" | ").append(mod.getFailed())
                    .append(" | ").append(mod.getError())
                    .append(" | ").append(mod.getSkipped())
                    .append(" | ").append(rateColor).append(" ").append(rate)
                    .append(" | ").append(mod.getAvgResponseTimeMs()).append("ms")
                    .append(" |\n");
        }
        sb.append("\n");
    }

    private void writeFailureDetails(TestReport report) {
        if (report.getFailures().isEmpty()) return;

        sb.append("\n## ❌ 失败用例详情\n\n");

        for (int i = 0; i < report.getFailures().size(); i++) {
            TestResult r = report.getFailures().get(i);
            sb.append("### ").append(i + 1).append(". ").append(escapeMarkdown(r.getCaseName())).append("\n\n");
            sb.append("| 属性 | 值 |\n");
            sb.append("|------|----|\n");
            sb.append("| 模块 | `").append(r.getModule()).append("` |\n");
            sb.append("| 阶段 | `").append(getPhaseName(r.getPhase())).append("` |\n");
            sb.append("| 方法 | `").append(r.getHttpMethod()).append("` |\n");
            sb.append("| 路径 | `").append(r.getRequestUrl()).append("` |\n");
            sb.append("| HTTP状态 | `").append(r.getHttpStatusCode()).append("` |\n");
            sb.append("| 耗时 | `").append(r.getResponseTimeMs()).append("ms` |\n");
            if (r.getFailureReason() != null) {
                sb.append("| 失败原因 | ").append(escapeMarkdown(r.getFailureReason())).append(" |\n");
            }
            if (r.getStackTrace() != null) {
                sb.append("| 堆栈 | ```\n").append(r.getStackTrace()).append("\n``` |\n");
            }

            // 失败的断言详情
            List<TestResult.AssertionResult> failedAssertions = r.getAssertionResults().stream()
                    .filter(a -> !a.isPassed())
                    .toList();
            if (!failedAssertions.isEmpty()) {
                sb.append("\n**失败断言:**\n\n");
                for (TestResult.AssertionResult ar : failedAssertions) {
                    sb.append("- `").append(ar.getAssertionType()).append("`");
                    if (ar.getJsonPath() != null) {
                        sb.append(" (路径: `").append(ar.getJsonPath()).append("`)");
                    }
                    sb.append(": 期望 `").append(ar.getExpected()).append("`, 实际 `").append(ar.getActual()).append("`\n");
                }
            }
            sb.append("\n");
        }
    }

    private void writeFooter(TestReport report) {
        sb.append("---\n\n");
        sb.append("*本报告由 OntoGraph API Tester 自动生成*\n");
        sb.append("> 报告生成时间: ").append(report.getTimestampString()).append("\n");
    }

    private void ensureOutputDir() {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // ===== 辅助方法 =====

    private String getPhaseEmoji(TestCase.TestPhase phase) {
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

    private String emojiForStatus(TestResult.Status status) {
        return switch (status) {
            case PASSED -> "🟢";
            case FAILED -> "🔴";
            case SKIPPED -> "🟡";
            case ERROR -> "⚠️";
            default -> "⚪";
        };
    }

    private String getStatusMarkdown(TestResult.Status status) {
        return switch (status) {
            case PASSED -> "🟢 PASS";
            case FAILED -> "🔴 FAIL";
            case SKIPPED -> "🟡 SKIP";
            case ERROR -> "⚠️ ERROR";
            default -> "⚪ UNKNOWN";
        };
    }

    private String badge(String text) {
        return "`" + text + "`";
    }

    private String code(String text) {
        return "`" + text + "`";
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("`", "\\`")
                .replace("[", "\\[")
                .replace("]", "\\]");
    }

    private String truncateUrl(String url) {
        if (url == null) return "-";
        String base = url.replace("http://localhost:8080", "");
        if (base.length() > 40) {
            return base.substring(0, 37) + "...";
        }
        return base.isEmpty() ? "/" : base;
    }

    @Override
    public String getReportPath() {
        return new File(outputDir, outputFile).getAbsolutePath();
    }
}
