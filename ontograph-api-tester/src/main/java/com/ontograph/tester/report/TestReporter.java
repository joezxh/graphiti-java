package com.ontograph.tester.report;

import com.ontograph.tester.model.TestCase;
import com.ontograph.tester.model.TestReport;
import com.ontograph.tester.model.TestResult;

/**
 * 测试报告器接口
 * 支持多种报告输出方式（控制台、Markdown、HTML等）
 */
public interface TestReporter {

    /**
     * 测试运行开始
     */
    default void onTestRunStart() {}

    /**
     * 测试阶段开始
     */
    default void onPhaseStart(TestCase.TestPhase phase, int caseCount) {}

    /**
     * 单个测试结果
     */
    default void onTestResult(TestResult result) {}

    /**
     * 测试阶段完成
     */
    default void onPhaseComplete(TestCase.TestPhase phase) {}

    /**
     * 测试运行完成（生成最终报告）
     */
    void onTestRunComplete(TestReport report);

    /**
     * 获取报告文件路径
     */
    String getReportPath();
}
