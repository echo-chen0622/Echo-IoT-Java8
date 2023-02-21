package org.echoiot.rule.engine.api;

import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Data
@Builder
public class TbEmail {

    @NotNull
    private final String from;
    @NotNull
    private final String to;
    @NotNull
    private final String cc;
    @NotNull
    private final String bcc;
    @NotNull
    private final String subject;
    @NotNull
    private final String body;
    @NotNull
    private final Map<String, String> images;
    private final boolean html;

}
