package org.thingsboard.server.common.data.sync.vc.request.load;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityTypeVersionLoadConfig extends VersionLoadConfig {

    private boolean removeOtherEntities;
    private boolean findExistingEntityByName;

}
