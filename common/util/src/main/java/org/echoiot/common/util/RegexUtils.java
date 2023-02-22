package org.echoiot.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegexUtils {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");


    public static String replace(String s, Pattern pattern, UnaryOperator<String> replacer) {
        return pattern.matcher(s).replaceAll(matchResult -> {
            return replacer.apply(matchResult.group());
        });
    }

    public static boolean matches(String input, Pattern pattern) {
        return pattern.matcher(input).matches();
    }

}
