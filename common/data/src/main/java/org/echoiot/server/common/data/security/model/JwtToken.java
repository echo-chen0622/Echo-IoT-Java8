package org.echoiot.server.common.data.security.model;

import java.io.Serializable;

public interface JwtToken extends Serializable {
    String getToken();
}
