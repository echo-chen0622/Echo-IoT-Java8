package org.echoiot.rule.engine.metadata;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.rule.engine.util.EntityDetails;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNode;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.ContactBased;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Map;

import static org.echoiot.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractGetEntityDetailsNode<C extends TbAbstractGetEntityDetailsNodeConfiguration> implements TbNode {

    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();
    private static final Type TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadGetEntityDetailsNodeConfiguration(configuration);
    }

    @Override
    public void onMsg(@NotNull TbContext ctx, TbMsg msg) {
        withCallback(getDetails(ctx, msg),
                ctx::tellSuccess,
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract C loadGetEntityDetailsNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException;

    protected abstract ListenableFuture<TbMsg> getDetails(TbContext ctx, TbMsg msg);

    protected abstract ListenableFuture<ContactBased> getContactBasedListenableFuture(TbContext ctx, TbMsg msg);

    @NotNull
    protected MessageData getDataAsJson(@NotNull TbMsg msg) {
        if (this.config.isAddToMetadata()) {
            return new MessageData(gson.toJsonTree(msg.getMetaData().getData(), TYPE), "metadata");
        } else {
            return new MessageData(jsonParser.parse(msg.getData()), "data");
        }
    }

    @NotNull
    protected ListenableFuture<TbMsg> getTbMsgListenableFuture(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull MessageData messageData, String prefix) {
        if (!this.config.getDetailsList().isEmpty()) {
            ListenableFuture<ContactBased> contactBasedListenableFuture = getContactBasedListenableFuture(ctx, msg);
            @NotNull ListenableFuture<JsonElement> resultObject = addContactProperties(messageData.getData(), contactBasedListenableFuture, prefix);
            return transformMsg(ctx, msg, resultObject, messageData);
        } else {
            return Futures.immediateFuture(msg);
        }
    }

    @NotNull
    private ListenableFuture<TbMsg> transformMsg(@NotNull TbContext ctx, @NotNull TbMsg msg, @NotNull ListenableFuture<JsonElement> propertiesFuture, @NotNull MessageData messageData) {
        return Futures.transformAsync(propertiesFuture, jsonElement -> {
            if (jsonElement != null) {
                if (messageData.getDataType().equals("metadata")) {
                    Map<String, String> metadataMap = gson.fromJson(jsonElement.toString(), TYPE);
                    return Futures.immediateFuture(ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), new TbMsgMetaData(metadataMap), msg.getData()));
                } else {
                    return Futures.immediateFuture(ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), gson.toJson(jsonElement)));
                }
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
    }

    @NotNull
    private ListenableFuture<JsonElement> addContactProperties(@NotNull JsonElement data, @NotNull ListenableFuture<ContactBased> entityFuture, String prefix) {
        return Futures.transformAsync(entityFuture, contactBased -> {
            if (contactBased != null) {
                @Nullable JsonElement jsonElement = null;
                for (@NotNull EntityDetails entityDetails : this.config.getDetailsList()) {
                    jsonElement = setProperties(contactBased, data, entityDetails, prefix);
                }
                return Futures.immediateFuture(jsonElement);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
    }

    private JsonElement setProperties(@NotNull ContactBased entity, @NotNull JsonElement data, @NotNull EntityDetails entityDetails, String prefix) {
        JsonObject dataAsObject = data.getAsJsonObject();
        switch (entityDetails) {
            case ID:
                dataAsObject.addProperty(prefix + "id", entity.getId().toString());
                break;
            case TITLE:
                dataAsObject.addProperty(prefix + "title", entity.getName());
                break;
            case ADDRESS:
                if (entity.getAddress() != null) {
                    dataAsObject.addProperty(prefix + "address", entity.getAddress());
                }
                break;
            case ADDRESS2:
                if (entity.getAddress2() != null) {
                    dataAsObject.addProperty(prefix + "address2", entity.getAddress2());
                }
                break;
            case CITY:
                if (entity.getCity() != null) dataAsObject.addProperty(prefix + "city", entity.getCity());
                break;
            case COUNTRY:
                if (entity.getCountry() != null)
                    dataAsObject.addProperty(prefix + "country", entity.getCountry());
                break;
            case STATE:
                if (entity.getState() != null) {
                    dataAsObject.addProperty(prefix + "state", entity.getState());
                }
                break;
            case EMAIL:
                if (entity.getEmail() != null) {
                    dataAsObject.addProperty(prefix + "email", entity.getEmail());
                }
                break;
            case PHONE:
                if (entity.getPhone() != null) {
                    dataAsObject.addProperty(prefix + "phone", entity.getPhone());
                }
                break;
            case ZIP:
                if (entity.getZip() != null) {
                    dataAsObject.addProperty(prefix + "zip", entity.getZip());
                }
                break;
            case ADDITIONAL_INFO:
                if (entity.getAdditionalInfo().hasNonNull("description")) {
                    dataAsObject.addProperty(prefix + "additionalInfo", entity.getAdditionalInfo().get("description").asText());
                }
                break;
        }
        return dataAsObject;
    }

    @Data
    @AllArgsConstructor
    private static class MessageData {
        private JsonElement data;
        private String dataType;
    }


}
