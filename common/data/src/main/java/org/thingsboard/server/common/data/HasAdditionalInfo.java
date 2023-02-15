package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;

public interface HasAdditionalInfo {

    JsonNode getAdditionalInfo();

}
