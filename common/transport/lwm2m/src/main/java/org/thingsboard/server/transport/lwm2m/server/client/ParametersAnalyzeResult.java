package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ParametersAnalyzeResult {
    Set<String> pathPostParametersAdd;
    Set<String> pathPostParametersDel;

    public ParametersAnalyzeResult() {
        this.pathPostParametersAdd = ConcurrentHashMap.newKeySet();
        this.pathPostParametersDel = ConcurrentHashMap.newKeySet();
    }
}
