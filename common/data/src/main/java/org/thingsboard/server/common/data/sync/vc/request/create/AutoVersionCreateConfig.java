package org.thingsboard.server.common.data.sync.vc.request.create;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AutoVersionCreateConfig extends VersionCreateConfig {

    private static final long serialVersionUID = 8245450889383315551L;

    private String branch;

}
