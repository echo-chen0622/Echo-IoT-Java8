package org.echoiot.rule.engine.metadata;

import lombok.Data;
import org.echoiot.rule.engine.util.EntityDetails;

import java.util.List;

@Data
public abstract class TbAbstractGetEntityDetailsNodeConfiguration {


    private List<EntityDetails> detailsList;

    private boolean addToMetadata;

}
