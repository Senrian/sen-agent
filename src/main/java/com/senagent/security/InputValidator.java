package com.senagent.security;

import lombok.extern.slf4j;

import java.util.regex.Pattern;

/**
 * 输入校验器 - 安全合规
 */
@Slf4j
public class InputValidator {

    private static final Pattern SAFE_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\.\\s]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[^\\s]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    /**
     * 校验字符串
     */
    public static boolean isValid(String input, int maxLength) {
        if (input == null || input.isEmpty()) return false;
        if (input.length() > maxLength) return false;
        return true;
    }

    /**
     * 校验安全字符串(字母数字下划线)
     */
    public static boolean isSafeString(String input) {
        if (input == null) return false;
        return SAFE_PATTERN.matcher(input).matches();
    }

    /**
     * 校验URL
     */
    public static boolean isValidUrl(String url) {
        if (url == null) return false;
        return URL_PATTERN.matcher(url).matches();
    }

    /**
     * 校验邮箱
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 校验数字范围
     */
    public static boolean isInRange(Number value, Number min, Number max) {
        if (value == null) return false;
        double v = value.doubleValue();
        return v >= min.doubleValue() && v <= max.doubleValue();
    }

    /**
     * SQL注入检测
     */
    public static boolean containsSqlInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("union select") ||
               lower.contains("drop table") ||
               lower.contains("delete from") ||
               lower.contains("insert into") ||
               lower.contains("--");
    }

    /**
     * XSS检测
     */
    public static boolean containsXss(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return lower.contains("<script") ||
               lower.contains("javascript:") ||
               lower.contains("onerror=") ||
               lower.contains("onload=");
    }

    /**
     * 路径遍历检测
     */
    public static boolean containsPathTraversal(String input) {
        if (input == null) return false;
        return input.contains("../") ||
               input.contains("..\\") ||
               input.startsWith("/etc") ||
               input.startsWith("C:\\");
    }

    /**
     * HTML转义
     */
    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    /**
     * 校验并返回结果
     */
    public static ValidationResult validate(String input, int maxLength) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        if (!isValid(input, maxLength)) {
            result.setValid(false);
            result.addError("Input too long or empty");
        }

        if (containsSqlInjection(input)) {
            result.setValid(false);
            result.addError("SQL injection detected");
        }

        if (containsXss(input)) {
            result.setValid(false);
            result.addError("XSS detected");
        }

        return result;
    }

    public static class ValidationResult {
        private boolean valid = true;
        private final java.util.List<String> errors = new java.util.ArrayList<>();

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public java.util.List<String> getErrors() { return errors; }
        public void addError(String error) { errors.add(error); }
    }
}
