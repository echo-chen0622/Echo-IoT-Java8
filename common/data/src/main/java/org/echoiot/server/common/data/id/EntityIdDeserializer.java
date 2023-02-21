package org.echoiot.server.common.data.id;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Created by Echo on 11.05.17.
 */
public class EntityIdDeserializer extends JsonDeserializer<EntityId> {

    @Override
    public EntityId deserialize(@NotNull JsonParser jsonParser, DeserializationContext ctx) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        ObjectNode node = oc.readTree(jsonParser);
        if (node.has("entityType") && node.has("id")) {
            return EntityIdFactory.getByTypeAndId(node.get("entityType").asText(), node.get("id").asText());
        } else {
            throw new IOException("Missing entityType or id!");
        }
    }

}
