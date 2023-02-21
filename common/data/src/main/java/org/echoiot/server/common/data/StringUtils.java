package org.echoiot.server.common.data;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.apache.commons.lang3.StringUtils.repeat;

public class StringUtils {
    public static final String EMPTY = "";

    public static final int INDEX_NOT_FOUND = -1;

    public static boolean isEmpty(@Nullable String source) {
        return source == null || source.isEmpty();
    }

    public static boolean isBlank(@Nullable String source) {
        return source == null || source.isEmpty() || source.trim().isEmpty();
    }

    public static boolean isNotEmpty(@Nullable String source) {
        return source != null && !source.isEmpty();
    }

    public static boolean isNotBlank(@Nullable String source) {
        return source != null && !source.isEmpty() && !source.trim().isEmpty();
    }

    @Nullable
    public static String removeStart(@NotNull final String str, @NotNull final String remove) {
        if (isEmpty(str) || isEmpty(remove)) {
            return str;
        }
        if (str.startsWith(remove)) {
            return str.substring(remove.length());
        }
        return str;
    }

    @Nullable
    public static String substringBefore(@NotNull final String str, @Nullable final String separator) {
        if (isEmpty(str) || separator == null) {
            return str;
        }
        if (separator.isEmpty()) {
            return EMPTY;
        }
        final int pos = str.indexOf(separator);
        if (pos == INDEX_NOT_FOUND) {
            return str;
        }
        return str.substring(0, pos);
    }

    public static String substringBetween(@Nullable final String str, @Nullable final String open, @Nullable final String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        final int start = str.indexOf(open);
        if (start != INDEX_NOT_FOUND) {
            final int end = str.indexOf(close, start + open.length());
            if (end != INDEX_NOT_FOUND) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    @NotNull
    public static String obfuscate(@NotNull String input, int seenMargin, char obfuscationChar,
                                   int startIndexInclusive, int endIndexExclusive) {

        @NotNull String part = input.substring(startIndexInclusive, endIndexExclusive);
        String obfuscatedPart;
        if (part.length() <= seenMargin * 2) {
            obfuscatedPart = repeat(obfuscationChar, part.length());
        } else {
            obfuscatedPart = part.substring(0, seenMargin)
                    + repeat(obfuscationChar, part.length() - seenMargin * 2)
                    + part.substring(part.length() - seenMargin);
        }
        return input.substring(0, startIndexInclusive) + obfuscatedPart + input.substring(endIndexExclusive);
    }

    @NotNull
    public static Iterable<String> split(@NotNull String value, int maxPartSize) {
        return Splitter.fixedLength(maxPartSize).split(value);
    }

    public static boolean equalsIgnoreCase(@Nullable String str1, @Nullable String str2) {
        return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
    }

    public static String join(String[] keyArray, String lwm2mSeparatorPath) {
        return org.apache.commons.lang3.StringUtils.join(keyArray, lwm2mSeparatorPath);
    }

    public static String trimToNull(String toString) {
        return org.apache.commons.lang3.StringUtils.trimToNull(toString);
    }

    public static boolean isNoneEmpty(String str) {
        return org.apache.commons.lang3.StringUtils.isNoneEmpty(str);
    }

    public static boolean endsWith(String str, String suffix) {
        return org.apache.commons.lang3.StringUtils.endsWith(str, suffix);
    }

    public static boolean hasLength(String str) {
        return org.springframework.util.StringUtils.hasLength(str);
    }

    public static boolean isNoneBlank(String... str) {
        return org.apache.commons.lang3.StringUtils.isNoneBlank(str);
    }

    public static boolean hasText(String str) {
        return org.springframework.util.StringUtils.hasText(str);
    }

    public static String defaultString(String s, String defaultValue) {
        return org.apache.commons.lang3.StringUtils.defaultString(s, defaultValue);
    }

    public static boolean isNumeric(String str) {
        return org.apache.commons.lang3.StringUtils.isNumeric(str);
    }

    public static boolean equals(String str1, String str2) {
        return org.apache.commons.lang3.StringUtils.equals(str1, str2);
    }

    public static String substringAfterLast(String str, String sep) {
        return org.apache.commons.lang3.StringUtils.substringAfterLast(str, sep);
    }

    public static boolean containedByAny(@Nullable String searchString, @NotNull String... strings) {
        if (searchString == null) return false;
        for (@Nullable String string : strings) {
            if (string != null && string.contains(searchString)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(final CharSequence seq, final CharSequence searchSeq) {
        return org.apache.commons.lang3.StringUtils.contains(seq, searchSeq);
    }

    @NotNull
    public static String randomNumeric(int length) {
        return RandomStringUtils.randomNumeric(length);
    }

    @NotNull
    public static String random(int length) {
        return RandomStringUtils.random(length);
    }

    @NotNull
    public static String random(int length, String chars) {
        return RandomStringUtils.random(length, chars);
    }

    @NotNull
    public static String randomAlphanumeric(int count) {
        return RandomStringUtils.randomAlphanumeric(count);
    }

    @NotNull
    public static String randomAlphabetic(int count) {
        return RandomStringUtils.randomAlphabetic(count);
    }

}
