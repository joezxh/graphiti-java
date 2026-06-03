package com.ontograph.tester.assertion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.ontograph.tester.model.HttpResponse;
import com.ontograph.tester.model.TestCase;
import com.ontograph.tester.model.TestResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 断言引擎
 * 执行JSON Path断言、状态码断言、响应时间断言等
 */
@Slf4j
public class AssertionEngine {

    private final ObjectMapper mapper;
    private final Configuration jsonPathConfig;
    private final int maxResponseBodyLength;

    public AssertionEngine(int maxResponseBodyLength) {
        this.maxResponseBodyLength = maxResponseBodyLength;
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();

        this.jsonPathConfig = Configuration.builder()
                .options(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS)
                .build();
    }

    /**
     * 执行所有断言
     */
    public List<TestResult.AssertionResult> assertAll(HttpResponse response, List<TestCase.Assertion> assertions) {
        List<TestResult.AssertionResult> results = new ArrayList<>();

        if (assertions == null || assertions.isEmpty()) {
            return results;
        }

        for (TestCase.Assertion assertion : assertions) {
            TestResult.AssertionResult result = executeAssertion(response, assertion);
            results.add(result);

            if (!result.isPassed()) {
                log.debug("断言失败 [{}]: {}", assertion.getType(), assertion.getMessage());
            }
        }

        return results;
    }

    /**
     * 执行单个断言
     */
    public TestResult.AssertionResult executeAssertion(HttpResponse response, TestCase.Assertion assertion) {
        long start = System.currentTimeMillis();

        try {
            return switch (assertion.getType()) {
                case STATUS_CODE -> assertStatusCode(response, assertion);
                case JSON_PATH -> assertJsonPath(response, assertion);
                case JSON_PATH_EXISTS -> assertJsonPathExists(response, assertion);
                case JSON_PATH_NOT_NULL -> assertJsonPathNotNull(response, assertion);
                case HEADER_EXISTS -> assertHeaderExists(response, assertion);
                case HEADER_VALUE -> assertHeaderValue(response, assertion);
                case RESPONSE_TIME -> assertResponseTime(response, assertion);
                case BODY_NOT_EMPTY -> assertBodyNotEmpty(response, assertion);
                case BODY_CONTAINS -> assertBodyContains(response, assertion);
                case CUSTOM -> executeCustomAssertion(response, assertion);
            };
        } catch (Exception e) {
            log.trace("断言执行异常: {}", e.getMessage());
            return TestResult.AssertionResult.fail(
                    assertion.getType().name(),
                    assertion.getJsonPath(),
                    assertion.getExpectedValue(),
                    null,
                    "断言执行异常: " + e.getMessage()
            );
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.trace("断言 {} 执行耗时: {}ms", assertion.getType(), elapsed);
        }
    }

    // ===== 状态码断言 =====

    private TestResult.AssertionResult assertStatusCode(HttpResponse response, TestCase.Assertion assertion) {
        int expected = asInt(assertion.getExpectedValue());
        int actual = response.getStatusCode();

        boolean passed = assertion.getOperator() == TestCase.Assertion.ComparisonOperator.EQUALS
                ? actual == expected
                : actual != expected;

        return TestResult.AssertionResult.builder()
                .assertionType("STATUS_CODE")
                .expected(expected)
                .actual(actual)
                .operator(assertion.getOperator().name())
                .passed(passed)
                .message(assertion.getMessage() != null ? assertion.getMessage()
                        : String.format("期望状态码 %s %d，实际 %d",
                                assertion.getOperator(), expected, actual))
                .build();
    }

    // ===== JSON Path 断言 =====

    private TestResult.AssertionResult assertJsonPath(HttpResponse response, TestCase.Assertion assertion) {
        String jsonPath = assertion.getJsonPath();
        if (jsonPath == null || jsonPath.isBlank()) {
            return TestResult.AssertionResult.fail("JSON_PATH", null,
                    assertion.getExpectedValue(), null, "JSON路径为空");
        }

        Object actualValue = extractJsonPath(response.getBody(), jsonPath);
        Object expected = assertion.getExpectedValue();
        boolean passed = compare(actualValue, expected, assertion.getOperator());

        return TestResult.AssertionResult.builder()
                .assertionType("JSON_PATH")
                .jsonPath(jsonPath)
                .expected(expected)
                .actual(actualValue)
                .operator(assertion.getOperator().name())
                .passed(passed)
                .message(assertion.getMessage() != null ? assertion.getMessage()
                        : String.format("JSON路径 %s 期望 %s %s，实际 %s",
                                jsonPath, assertion.getOperator(), expected, actualValue))
                .build();
    }

    private TestResult.AssertionResult assertJsonPathExists(HttpResponse response, TestCase.Assertion assertion) {
        String jsonPath = assertion.getJsonPath();
        if (jsonPath == null || jsonPath.isBlank()) {
            return TestResult.AssertionResult.fail("JSON_PATH_EXISTS", null,
                    "exists", null, "JSON路径为空");
        }

        Object value = extractJsonPath(response.getBody(), jsonPath);
        boolean passed = value != null;

        return TestResult.AssertionResult.builder()
                .assertionType("JSON_PATH_EXISTS")
                .jsonPath(jsonPath)
                .expected("exists")
                .actual(passed ? "exists" : "not found")
                .passed(passed)
                .message(assertion.getMessage() != null ? assertion.getMessage()
                        : String.format("JSON路径 %s 应存在", jsonPath))
                .build();
    }

    private TestResult.AssertionResult assertJsonPathNotNull(HttpResponse response, TestCase.Assertion assertion) {
        String jsonPath = assertion.getJsonPath();
        if (jsonPath == null || jsonPath.isBlank()) {
            return TestResult.AssertionResult.fail("JSON_PATH_NOT_NULL", null,
                    "not null", null, "JSON路径为空");
        }

        Object value = extractJsonPath(response.getBody(), jsonPath);
        boolean passed = value != null && !isNullValue(value);

        return TestResult.AssertionResult.builder()
                .assertionType("JSON_PATH_NOT_NULL")
                .jsonPath(jsonPath)
                .expected("not null")
                .actual(value == null ? "null" : value)
                .passed(passed)
                .message(assertion.getMessage() != null ? assertion.getMessage()
                        : String.format("JSON路径 %s 不应为null", jsonPath))
                .build();
    }

    // ===== 响应头断言 =====

    private TestResult.AssertionResult assertHeaderExists(HttpResponse response, TestCase.Assertion assertion) {
        String headerName = assertion.getJsonPath(); // 复用jsonPath存header名
        if (headerName == null || headerName.isBlank()) {
            return TestResult.AssertionResult.fail("HEADER_EXISTS", null,
                    "exists", null, "响应头名称为空");
        }

        boolean exists = response.getHeaders().containsKey(headerName);

        return TestResult.AssertionResult.builder()
                .assertionType("HEADER_EXISTS")
                .jsonPath(headerName)
                .expected("exists")
                .actual(exists ? "exists" : "not found")
                .passed(exists)
                .message(assertion.getMessage() != null ? assertion.getMessage()
                        : String.format("响应头 %s 应存在", headerName))
                .build();
    }

    private TestResult.AssertionResult assertHeaderValue(HttpResponse response, TestCase.Assertion assertion) {
        String headerName = assertion.getJsonPath();
        String expected = assertion.getExpectedValue() != null ? assertion.getExpectedValue().toString() : "";
        String actual = response.getHeaders().get(headerName);

        boolean passed = expected.equals(actual);

        return TestResult.AssertionResult.builder()
                .assertionType("HEADER_VALUE")
                .jsonPath(headerName)
                .expected(expected)
                .actual(actual)
                .passed(passed)
                .message(assertion.getMessage() != null ? assertion.getMessage()
                        : String.format("响应头 %s 期望 %s，实际 %s", headerName, expected, actual))
                .build();
    }

    // ===== 响应时间断言 =====

    private TestResult.AssertionResult assertResponseTime(HttpResponse response, TestCase.Assertion assertion) {
        long expectedMs = asLong(assertion.getExpectedValue());
        long actualMs = response.getResponseTimeMs();

        boolean passed = switch (assertion.getOperator()) {
            case LESS_THAN -> actualMs < expectedMs;
            case LESS_THAN_OR_EQUALS -> actualMs <= expectedMs;
            case GREATER_THAN -> actualMs > expectedMs;
            case GREATER_THAN_OR_EQUALS -> actualMs >= expectedMs;
            default -> actualMs <= expectedMs;
        };

        return TestResult.AssertionResult.builder()
                .assertionType("RESPONSE_TIME")
                .expected(expectedMs)
                .actual(actualMs)
                .operator(assertion.getOperator().name())
                .passed(passed)
                .message(assertion.getMessage() != null ? assertion.getMessage()
                        : String.format("响应时间 %s %dms，实际 %dms",
                                assertion.getOperator(), expectedMs, actualMs))
                .build();
    }

    // ===== 响应体断言 =====

    private TestResult.AssertionResult assertBodyNotEmpty(HttpResponse response, TestCase.Assertion assertion) {
        boolean passed = response.getBody() != null
                && !response.getBody().isBlank()
                && !response.getBody().equals("null");

        return TestResult.AssertionResult.builder()
                .assertionType("BODY_NOT_EMPTY")
                .expected("non-empty")
                .actual(passed ? "non-empty" : "empty")
                .passed(passed)
                .message(assertion.getMessage() != null ? assertion.getMessage()
                        : "响应体不应为空")
                .build();
    }

    private TestResult.AssertionResult assertBodyContains(HttpResponse response, TestCase.Assertion assertion) {
        String expectedText = assertion.getExpectedValue() != null
                ? assertion.getExpectedValue().toString() : "";
        String body = response.getBody() != null ? response.getBody() : "";
        boolean passed = body.contains(expectedText);

        return TestResult.AssertionResult.builder()
                .assertionType("BODY_CONTAINS")
                .expected(expectedText)
                .actual(passed ? "contains" : "not found")
                .passed(passed)
                .message(assertion.getMessage() != null ? assertion.getMessage()
                        : String.format("响应体应包含: %s", expectedText))
                .build();
    }

    // ===== 自定义断言 =====

    private TestResult.AssertionResult executeCustomAssertion(HttpResponse response, TestCase.Assertion assertion) {
        // 自定义断言可通过扩展点实现
        return TestResult.AssertionResult.builder()
                .assertionType("CUSTOM")
                .expected(assertion.getExpectedValue())
                .actual(null)
                .passed(false)
                .message(assertion.getMessage() != null ? assertion.getMessage() : "自定义断言未实现")
                .build();
    }

    // ===== 辅助方法 =====

    /**
     * 从JSON字符串提取JSON Path值
     */
    @SuppressWarnings("unchecked")
    private Object extractJsonPath(String json, String jsonPath) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            Object document = JsonPath.using(jsonPathConfig).parse(json).read(jsonPath);

            if (document == null) {
                return null;
            }

            // 处理列表：如果返回列表且期望单个值，取第一个
            if (document instanceof List) {
                List<?> list = (List<?>) document;
                return list.isEmpty() ? null : list.get(0);
            }

            return document;
        } catch (PathNotFoundException e) {
            return null;
        } catch (Exception e) {
            log.trace("JSON Path提取失败 [{}]: {}", jsonPath, e.getMessage());
            return null;
        }
    }

    /**
     * 比较两个值
     */
    private boolean compare(Object actual, Object expected, TestCase.Assertion.ComparisonOperator operator) {
        if (actual == null && expected == null) {
            return operator == TestCase.Assertion.ComparisonOperator.EQUALS;
        }
        if (actual == null) {
            return operator == TestCase.Assertion.ComparisonOperator.NOT_EQUALS;
        }

        String actualStr = String.valueOf(actual).trim();
        String expectedStr = expected != null ? String.valueOf(expected).trim() : "";

        return switch (operator) {
            case EQUALS -> {
                // 特殊处理：比较数值
                if (isNumeric(actual) && isNumeric(expected)) {
                    yield compareNumbers(actual, expected) == 0;
                }
                yield actualStr.equals(expectedStr);
            }
            case NOT_EQUALS -> !actualStr.equals(expectedStr);
            case GREATER_THAN -> isNumeric(actual) && isNumeric(expected)
                    && compareNumbers(actual, expected) > 0;
            case LESS_THAN -> isNumeric(actual) && isNumeric(expected)
                    && compareNumbers(actual, expected) < 0;
            case GREATER_THAN_OR_EQUALS -> isNumeric(actual) && isNumeric(expected)
                    && compareNumbers(actual, expected) >= 0;
            case LESS_THAN_OR_EQUALS -> isNumeric(actual) && isNumeric(expected)
                    && compareNumbers(actual, expected) <= 0;
            case CONTAINS -> actualStr.contains(expectedStr);
            case NOT_CONTAINS -> !actualStr.contains(expectedStr);
            case MATCHES -> Pattern.matches(expectedStr, actualStr);
            case IN -> expected != null && expectedStr.contains(actualStr);
        };
    }

    private boolean isNumeric(Object value) {
        if (value instanceof Number) return true;
        if (value instanceof String) {
            try {
                Double.parseDouble((String) value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private int compareNumbers(Object a, Object b) {
        double ai = a instanceof Number ? ((Number) a).doubleValue()
                : Double.parseDouble(String.valueOf(a));
        double bi = b instanceof Number ? ((Number) b).doubleValue()
                : Double.parseDouble(String.valueOf(b));
        return Double.compare(ai, bi);
    }

    private boolean isNullValue(Object value) {
        if (value == null) return true;
        String str = String.valueOf(value).trim().toLowerCase();
        return str.equals("null") || str.equals("none") || str.isEmpty();
    }

    private int asInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try {
                return (int) Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private long asLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return (long) Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 判断所有断言是否通过
     */
    public boolean allPassed(List<TestResult.AssertionResult> results) {
        return results != null && !results.isEmpty()
                && results.stream().allMatch(TestResult.AssertionResult::isPassed);
    }

    /**
     * 获取失败的断言
     */
    public List<TestResult.AssertionResult> getFailed(List<TestResult.AssertionResult> results) {
        if (results == null) return List.of();
        return results.stream().filter(r -> !r.isPassed()).toList();
    }
}
