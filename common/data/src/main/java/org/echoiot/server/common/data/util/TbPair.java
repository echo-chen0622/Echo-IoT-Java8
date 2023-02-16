package org.echoiot.server.common.data.util;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TbPair<S, T> {
    private S first;
    private T second;
}
