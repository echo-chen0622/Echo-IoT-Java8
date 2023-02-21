package org.echoiot.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RegexUtils {

    public static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");


    public static String replace(@NotNull String s, @NotNull Pattern pattern, @NotNull UnaryOperator<String> replacer) {
        return pattern.matcher(s).replaceAll(matchResult -> {
            return replacer.apply(matchResult.group());
        });
    }

    public static boolean matches(@NotNull String input, @NotNull Pattern pattern) {
        return pattern.matcher(input).matches();
    }

}
