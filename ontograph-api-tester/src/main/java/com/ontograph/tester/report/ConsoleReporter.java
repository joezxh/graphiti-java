package com.ontograph.tester.report;

import com.ontograph.tester.config.TesterConfig;
import com.ontograph.tester.model.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 控制台报告器
 * 实时打印带颜色高亮的测试进度和统计信息
 */
@Slf4j
public class ConsoleReporter implements TestReporter {

    private final TesterConfig.ReportConfig reportConfig;
    private final String baseUrl;
    private final boolean colored;
    private final int total;
    private final AtomicInteger completed = new AtomicInteger(0);
    private final AtomicInteger passed = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);
    private final AtomicInteger skipped = new AtomicInteger(0);
    private final AtomicInteger error = new AtomicInteger(0);
    private final long startMs;
    private String currentPhase = "";
    private int phaseTotal = 0;
    private final List<TestResult> recentFailures = new ArrayList<>();
    private static final int MAX_RECENT_FAILURES = 5;

    // ANSI颜色代码
    private static final String RESET = "\033[0m";
    private static final String BOLD = "\033[1m";
    private static final String DIM = "\033[2m";
    private static final String RED = "\033[31m";
    private static final String GREEN = "\033[32m";
    private static final String YELLOW = "\033[33m";
    private static final String BLUE = "\033[34m";
    private static final String CYAN = "\033[36m";
    private static final String WHITE = "\033[37m";
    private static final String BG_RED = "\033[41m";
    private static final String BG_GREEN = "\033[42m";

    public ConsoleReporter(TesterConfig.ReportConfig reportConfig, String baseUrl) {
        this.reportConfig = reportConfig;
        this.baseUrl = baseUrl;
        this.colored = reportConfig.getConsole().isColored() && isColorSupported();
        this.startMs = System.currentTimeMillis();
        this.total = 0;
    }

    private boolean isColorSupported() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            // Windows: check if ConEmu/AnsiWeather or similar
            String term = System.getenv("TERM");
            if (term != null && term.contains("256")) return true;
            // Check if Windows Terminal or modern console
            String wtSession = System.getenv("WT_SESSION");
            if (wtSession != null) return true;
            return false;
        }
        return true;
    }

    private String color(String text, String color) {
        return colored ? color + text + RESET : text;
    }

    private String bold(String text) { return color(text, BOLD); }
    private String red(String text) { return color(text, RED); }
    private String green(String text) { return color(text, GREEN); }
    private String yellow(String text) { return color(text, YELLOW); }
    private String blue(String text) { return color(text, BLUE); }
    private String cyan(String text) { return color(text, CYAN); }
    private String dim(String text) { return color(text, DIM); }
    private String bgGreen(String text) { return color(text, BG_GREEN); }
    private String bgRed(String text) { return color(text, BG_RED); }

    @Override
    public void onTestRunStart() {
        printBanner();
    }

    private void printBanner() {
        String banner = """
                
                ╔══════════════════════════════════════════════════════╗
                ║       OntoGraph API Tester  -  API自动化测试工具        ║
                ╚══════════════════════════════════════════════════════╝
                """;
        System.out.println(bold(blue(banner)));
    }

    @Override
    public void onPhaseStart(TestCase.TestPhase phase, int caseCount) {
        this.currentPhase = getPhaseName(phase);
        this.phaseTotal = caseCount;
        this.completed.set(0);
        this.passed.set(0);
        this.failed.set(0);
        this.skipped.set(0);
        this.error.set(0);

        System.out.println();
        System.out.println(bold(cyan("━━━ " + currentPhase + " (" + caseCount + "用例) ━━━")));
    }

    @Override
    public void onTestResult(TestResult result) {
        int idx = completed.incrementAndGet();
        String statusIcon;
        String statusText;
        String statusColor;

        switch (result.getStatus()) {
            case PASSED -> {
                passed.incrementAndGet();
                statusIcon = bold(green("[PASS]"));
                statusText = green("通过");
                statusColor = GREEN;
            }
            case FAILED -> {
                failed.incrementAndGet();
                statusIcon = bold(red("[FAIL]"));
                statusText = red("失败");
                statusColor = RED;
                synchronized (recentFailures) {
                    if (recentFailures.size() < MAX_RECENT_FAILURES) {
                        recentFailures.add(result);
                    }
                }
            }
            case SKIPPED -> {
                skipped.incrementAndGet();
                statusIcon = bold(yellow("[SKIP]"));
                statusText = yellow("跳过");
                statusColor = YELLOW;
            }
            case ERROR -> {
                error.incrementAndGet();
                statusIcon = bold(red("[ERR!]"));
                statusText = red("错误");
                statusColor = RED;
            }
            default -> {
                statusIcon = bold(dim("[----]"));
                statusText = dim("未知");
                statusColor = DIM;
            }
        }

        String method = bold(result.getHttpMethod() != null ? result.getHttpMethod() : "---");
        String path = truncate(result.getRequestUrl() != null
                ? result.getRequestUrl().replace(baseUrl, "") : "---", 50);
        String duration = result.getResponseTimeMs() < 1000
                ? green(result.getResponseTimeMs() + "ms")
                : yellow(result.getResponseTimeMs() + "ms");

        String statusBar = String.format("%s %s %s %s",
                statusIcon, method, path, duration);

        // 失败时显示原因
        if (result.getStatus() == TestResult.Status.FAILED && result.getFailureReason() != null) {
            String reason = truncate("  原因: " + result.getFailureReason(), 100);
            System.out.println(statusBar);
            System.out.println(dim(reason));
        } else if (result.getStatus() == TestResult.Status.ERROR && result.getFailureReason() != null) {
            System.out.println(statusBar);
            System.out.println(red("  错误: " + result.getFailureReason()));
        } else {
            System.out.println(statusBar);
        }

        // 实时进度条（每20个用例更新一次或最后更新）
        if (idx % 20 == 0 || idx == phaseTotal) {
            printProgress(idx, phaseTotal);
        }
    }

    private void printProgress(int done, int total) {
        int barWidth = 40;
        float progress = total > 0 ? (float) done / total : 0;
        int filled = (int) (barWidth * progress);
        StringBuilder bar = new StringBuilder();
        bar.append(dim("["));
        for (int i = 0; i < barWidth; i++) {
            if (i < filled) {
                bar.append(green("█"));
            } else {
                bar.append(dim("░"));
            }
        }
        bar.append(dim("]"));
        bar.append(String.format(" %d/%d ", done, total));
        bar.append(String.format("%.0f%%", progress * 100));
        bar.append(" | ");
        bar.append(green("P:" + passed.get()));
        bar.append(" ");
        if (failed.get() > 0) bar.append(red("F:" + failed.get()));
        else bar.append("F:0");
        bar.append(" ");
        if (error.get() > 0) bar.append(red("E:" + error.get()));
        else bar.append("E:0");
        bar.append(" ");
        bar.append(dim("S:" + skipped.get()));

        System.out.print("\r" + bar);
        if (done >= total) {
            System.out.println();
        }
    }

    @Override
    public void onPhaseComplete(TestCase.TestPhase phase) {
        System.out.println();
        String summary = String.format("  %s: %d通过 %s%d失败 %s%d错误",
                bold(currentPhase),
                green(String.valueOf(passed.get())),
                failed.get() > 0 ? red(String.valueOf(failed.get())) : "0",
                error.get() > 0 ? red(String.valueOf(error.get())) : "0");
        System.out.println(dim("─".repeat(60)));
        System.out.println(summary);
        System.out.println();
    }

    @Override
    public void onTestRunComplete(TestReport report) {
        long totalDuration = System.currentTimeMillis() - startMs;

        System.out.println();
        System.out.println(bold(cyan("════════════════════════════════════════════════════")));
        System.out.println(bold(cyan("                    测试报告摘要")));
        System.out.println(bold(cyan("════════════════════════════════════════════════════")));

        // 总体统计
        System.out.println();
        String passRateStr = String.format("%.1f%%", report.getSuccessRate());
        String rateColor = report.getSuccessRate() >= 90 ? GREEN
                : report.getSuccessRate() >= 70 ? YELLOW : RED;

        System.out.println("  " + bold("测试时间: ") + report.getTimestampString());
        System.out.println("  " + bold("目标服务: ") + blue(report.getTargetUrl()));
        System.out.println("  " + bold("环境: ") + report.getEnvironment());
        System.out.println();
        System.out.println("  " + bold("总计用例: ") + report.getTotalCases());
        System.out.print("  " + bold("通过: ") + bgGreen(" " + green(String.format("%3d", report.getPassed())) + " "));
        if (report.getFailed() > 0) {
            System.out.print("  " + bgRed(" " + red(String.format("%3d", report.getFailed())) + " "));
        } else {
            System.out.print("  " + bold("失败: ") + "  0");
        }
        if (report.getError() > 0) {
            System.out.print("  " + bgRed(" " + red(String.format("%3d", report.getError())) + " "));
        } else {
            System.out.print("  " + bold("错误: ") + "  0");
        }
        System.out.print("  " + bold("跳过: ") + yellow(String.format("%3d", report.getSkipped())));
        System.out.println();
        System.out.println();
        System.out.println("  " + bold("成功率: ") + color(passRateStr, rateColor));
        System.out.println("  " + bold("总耗时: ") + report.getDurationString());
        System.out.println("  " + bold("平均响应: ") + String.format("%.2fms", report.getAvgResponseTimeMs()));

        // 按模块统计
        if (!report.getModuleSummaries().isEmpty()) {
            System.out.println();
            System.out.println(bold("  按模块统计:"));
            for (TestReport.ModuleSummary mod : report.getModuleSummaries().values()) {
                String modRate = mod.getPassRate() >= 90 ? green(String.format("%.0f%%", mod.getPassRate()))
                        : mod.getPassRate() >= 70 ? yellow(String.format("%.0f%%", mod.getPassRate()))
                        : red(String.format("%.0f%%", mod.getPassRate()));

                String modSummary = String.format("    %-20s %4d用例 | P:%s%d | F:%s%d | 平均: %dms",
                        mod.getModuleName(),
                        mod.getTotal(),
                        green(String.valueOf(mod.getPassed())),
                        mod.getFailed() > 0 ? red(String.valueOf(mod.getFailed())) : "0",
                        modRate,
                        mod.getAvgResponseTimeMs());
                System.out.println(modSummary);
            }
        }

        // 失败用例详情
        if (!report.getFailures().isEmpty()) {
            System.out.println();
            System.out.println(bold(red("  失败用例详情 (" + report.getFailures().size() + "个):")));
            for (int i = 0; i < Math.min(10, report.getFailures().size()); i++) {
                TestResult r = report.getFailures().get(i);
                System.out.println();
                System.out.println(red("  " + (i + 1) + ". [" + r.getModule() + "] " + r.getCaseName()));
                System.out.println(dim("     " + r.getHttpMethod() + " " + truncate(r.getRequestUrl(), 80)));
                if (r.getFailureReason() != null) {
                    System.out.println(red("     原因: " + truncate(r.getFailureReason(), 100)));
                }
                if (reportConfig.getConsole().isShowStackTrace() && r.getStackTrace() != null) {
                    System.out.println(dim("     堆栈: " + truncate(r.getStackTrace(), 200)));
                }
            }
            if (report.getFailures().size() > 10) {
                System.out.println(dim("  ... 还有 " + (report.getFailures().size() - 10) + " 个失败用例"));
            }
        }

        System.out.println();
        if (report.isAllPassed()) {
            System.out.println(bold(green("  ✓ 全部测试通过！")));
        } else {
            System.out.println(bold(red("  ✗ 存在 " + (report.getFailed() + report.getError()) + " 个失败用例")));
        }
        System.out.println();
        System.out.println(bold(cyan("════════════════════════════════════════════════════")));
    }

    private TesterConfig.ReportConfig getReportConfig() {
        return reportConfig;
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

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }

    @Override
    public String getReportPath() {
        return null; // 控制台报告不写文件
    }
}
