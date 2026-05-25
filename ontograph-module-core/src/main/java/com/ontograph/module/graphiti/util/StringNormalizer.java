package com.ontograph.module.graphiti.util;

/**
 * 字符串规范化工具类
 * 用于实体去重的字符串标准化处理
 */
public class StringNormalizer {

    private StringNormalizer() {}

    /**
     * 精确规范化：转小写 + 合并空格
     * 用于 Tier 1 精确匹配去重
     *
     * @param input 输入字符串
     * @return 规范化后的字符串
     */
    public static String normalizeExact(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return input.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    /**
     * 计算字符串的香农熵
     * 用于判断是否适合模糊匹配（熵值 >= 1.5 才进行 MinHash 匹配）
     *
     * @param input 输入字符串
     * @return 香农熵值
     */
    public static double calculateEntropy(String input) {
        if (input == null || input.isEmpty()) {
            return 0.0;
        }

        int[] charCount = new int[256];
        int length = 0;

        for (char c : input.toCharArray()) {
            if (c < 256) {
                charCount[c]++;
            }
            length++;
        }

        if (length == 0) {
            return 0.0;
        }

        double entropy = 0.0;
        for (int count : charCount) {
            if (count > 0) {
                double p = (double) count / length;
                entropy -= p * (Math.log(p) / Math.log(2));
            }
        }

        return entropy;
    }

    /**
     * 检查字符串是否足够长以进行模糊匹配
     *
     * @param input 输入字符串
     * @return true 如果长度 >= 6
     */
    public static boolean isLongEnough(String input) {
        return input != null && input.length() >= 6;
    }

    /**
     * 检查字符串是否有足够的字符类型多样性（用于模糊匹配）
     *
     * @param input 输入字符串
     * @return true 如果熵值 >= 1.5 且长度 >= 6
     */
    public static boolean hasEnoughEntropy(String input) {
        return isLongEnough(input) && calculateEntropy(input) >= 1.5;
    }
}
