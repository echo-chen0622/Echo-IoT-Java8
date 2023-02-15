package org.thingsboard.server.common.data.sync.vc.request.create;

import lombok.Data;

import java.io.Serializable;

@Data
public class VersionCreateConfig implements Serializable {
    private static final long serialVersionUID = 1223723167716612772L;

    private boolean saveRelations;
    private boolean saveAttributes;
    private boolean saveCredentials;
}
