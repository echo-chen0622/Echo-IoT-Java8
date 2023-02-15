package org.thingsboard.server.common.data.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonTsValue {

    private TsValue current;
    private TsValue previous;
}
