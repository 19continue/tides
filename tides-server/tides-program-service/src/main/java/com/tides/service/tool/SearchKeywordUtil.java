package com.tides.service.tool;

import com.tides.util.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 搜索关键词处理工具，支持组合词和 x 通配输入。
 */
public final class SearchKeywordUtil {

    private static final int MIN_FRAGMENT_LENGTH = 2;

    private static final int MAX_FRAGMENT_LENGTH = 10;

    private static final int MAX_FRAGMENT_SOURCE_LENGTH = 18;

    private static final char WILDCARD = '*';

    private SearchKeywordUtil() {
    }

    public static boolean validContent(String content) {
        return concreteLength(normalizeCompact(content)) >= MIN_FRAGMENT_LENGTH;
    }

    public static boolean hasLooseClue(String content) {
        String keyword = normalizeCompact(content);
        return keyword.indexOf(WILDCARD) >= 0 || keyword.length() > 4;
    }

    public static List<String> fragments(String content) {
        Set<String> result = new LinkedHashSet<>();
        String compact = normalizeCompact(content);
        if (concreteLength(compact) >= MIN_FRAGMENT_LENGTH) {
            result.add(compact);
            addSubFragments(result, compact);
        }
        for (String token : normalizeTokenText(content).split("\\s+")) {
            if (concreteLength(token) >= MIN_FRAGMENT_LENGTH) {
                result.add(token);
                addSubFragments(result, token);
            }
        }
        return new ArrayList<>(result);
    }

    public static int fieldScore(String value, String content, int fullMatchScore) {
        String fieldValue = normalizeCompact(value);
        String keyword = normalizeCompact(content);
        if (StringUtil.isEmpty(fieldValue) || concreteLength(keyword) < MIN_FRAGMENT_LENGTH) {
            return 0;
        }
        if (fieldValue.indexOf(WILDCARD) < 0 && keyword.indexOf(WILDCARD) < 0) {
            if (fieldValue.contains(keyword)) {
                return fullMatchScore;
            }
            if (fieldValue.length() >= MIN_FRAGMENT_LENGTH && keyword.contains(fieldValue)) {
                return fullMatchScore;
            }
        }

        int score = 0;
        for (String fragment : fragments(content)) {
            int concreteLength = concreteLength(fragment);
            if (concreteLength < MIN_FRAGMENT_LENGTH || !matchesFragment(fieldValue, fragment)) {
                continue;
            }
            int base = containsWildcard(fragment) ? fullMatchScore / 2 : fullMatchScore / 3;
            int lengthBonus = Math.min(fullMatchScore / 2, concreteLength * 4);
            score = Math.max(score, base + lengthBonus);
        }
        return score;
    }

    public static String toEsWildcardPattern(String fragment) {
        return ("*" + normalizeCompact(fragment) + "*").replaceAll("\\*+", "*");
    }

    public static boolean containsWildcard(String value) {
        return StringUtil.isNotEmpty(value) && normalizeCompact(value).indexOf(WILDCARD) >= 0;
    }

    public static String normalizeCompact(String value) {
        if (StringUtil.isEmpty(value)) {
            return "";
        }
        String text = stripHighlight(value);
        StringBuilder builder = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (isSeparator(ch)) {
                continue;
            } else if (isWildcardSymbol(ch)) {
                builder.append(WILDCARD);
            } else {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString();
    }

    private static String normalizeTokenText(String value) {
        if (StringUtil.isEmpty(value)) {
            return "";
        }
        String text = stripHighlight(value);
        StringBuilder builder = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (isSeparator(ch)) {
                builder.append(' ');
            } else if (isWildcardSymbol(ch)) {
                builder.append(WILDCARD);
            } else {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString().trim();
    }

    private static String stripHighlight(String value) {
        return value.replace("<em>", "").replace("</em>", "");
    }

    private static void addSubFragments(Set<String> result, String source) {
        if (source.length() > MAX_FRAGMENT_SOURCE_LENGTH) {
            return;
        }
        int maxLength = Math.min(MAX_FRAGMENT_LENGTH, source.length());
        for (int length = maxLength; length >= MIN_FRAGMENT_LENGTH; length--) {
            for (int start = 0; start + length <= source.length(); start++) {
                String fragment = source.substring(start, start + length);
                if (concreteLength(fragment) >= MIN_FRAGMENT_LENGTH) {
                    result.add(fragment);
                }
            }
        }
    }

    private static boolean matchesFragment(String fieldValue, String fragment) {
        if (!containsWildcard(fragment)) {
            return fieldValue.contains(fragment);
        }
        return wildcardContains(fieldValue, fragment);
    }

    private static boolean wildcardContains(String fieldValue, String fragment) {
        if (concreteLength(fragment) < MIN_FRAGMENT_LENGTH) {
            return false;
        }
        StringBuilder regex = new StringBuilder(".*");
        StringBuilder literal = new StringBuilder();
        for (int index = 0; index < fragment.length(); index++) {
            char ch = fragment.charAt(index);
            if (ch == WILDCARD) {
                appendLiteral(regex, literal);
                regex.append(".*");
            } else {
                literal.append(ch);
            }
        }
        appendLiteral(regex, literal);
        regex.append(".*");
        return Pattern.matches(regex.toString(), fieldValue);
    }

    private static void appendLiteral(StringBuilder regex, StringBuilder literal) {
        if (literal.isEmpty()) {
            return;
        }
        regex.append(Pattern.quote(literal.toString()));
        literal.setLength(0);
    }

    private static int concreteLength(String value) {
        if (StringUtil.isEmpty(value)) {
            return 0;
        }
        int length = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) != WILDCARD) {
                length++;
            }
        }
        return length;
    }

    private static boolean isSeparator(char ch) {
        return Character.isWhitespace(ch)
                || "，。、“”‘’：:；;,.!?！？（）()【】[]《》<>·|/\\-_+&".indexOf(ch) >= 0;
    }

    private static boolean isWildcardSymbol(char ch) {
        return ch == 'x' || ch == 'X' || ch == '*' || ch == '＊' || ch == '?' || ch == '？';
    }
}
