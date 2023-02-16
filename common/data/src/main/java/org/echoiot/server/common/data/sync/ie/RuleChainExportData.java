package org.echoiot.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.echoiot.server.common.data.rule.RuleChain;
import org.echoiot.server.common.data.rule.RuleChainMetaData;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
public class RuleChainExportData extends EntityExportData<RuleChain> {

    @JsonProperty(index = 3)
    @JsonIgnoreProperties("ruleChainId")
    private RuleChainMetaData metaData;

}
