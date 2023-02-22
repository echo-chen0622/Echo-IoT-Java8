package org.echoiot.server.dao.attributes;

import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.kv.AttributeKvEntry;
import org.echoiot.server.dao.exception.IncorrectParameterException;
import org.echoiot.server.dao.service.Validator;

public class AttributeUtils {
    public static void validate(EntityId id, String scope) {
        Validator.validateId(id.getId(), "Incorrect id " + id);
        Validator.validateString(scope, "Incorrect scope " + scope);
    }

    public static void validate(AttributeKvEntry kvEntry) {
        if (kvEntry == null) {
            throw new IncorrectParameterException("Key value entry can't be null");
        } else if (kvEntry.getDataType() == null) {
            throw new IncorrectParameterException("Incorrect kvEntry. Data type can't be null");
        } else {
            Validator.validateString(kvEntry.getKey(), "Incorrect kvEntry. Key can't be empty");
            Validator.validatePositiveNumber(kvEntry.getLastUpdateTs(), "Incorrect last update ts. Ts should be positive");
        }
    }
}
