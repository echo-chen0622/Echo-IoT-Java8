package org.thingsboard.server.common.data.sync.vc.request.load;

import lombok.Data;

@Data
public class VersionLoadConfig {

    private boolean loadRelations;
    private boolean loadAttributes;
    private boolean loadCredentials;

}
