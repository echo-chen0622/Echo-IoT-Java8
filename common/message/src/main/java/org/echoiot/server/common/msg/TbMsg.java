package org.echoiot.server.common.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.EntityType;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.EntityIdFactory;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.msg.gen.MsgProtos;
import org.echoiot.server.common.msg.queue.TbMsgCallback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by Echo on 13.01.18.
 */
@Data
@Slf4j
public final class TbMsg implements Serializable {

    private final String queueName;
    private final UUID id;
    private final long ts;
    private final String type;
    private final EntityId originator;
    @Nullable
    private final CustomerId customerId;
    private final TbMsgMetaData metaData;
    private final TbMsgDataType dataType;
    private final String data;
    private final RuleChainId ruleChainId;
    private final RuleNodeId ruleNodeId;
    @NotNull
    @Getter(value = AccessLevel.NONE)
    @JsonIgnore
    //This field is not serialized because we use queues and there is no need to do it
    private final TbMsgProcessingCtx ctx;

    //This field is not serialized because we use queues and there is no need to do it
    @NotNull
    @JsonIgnore
    transient private final TbMsgCallback callback;

    public int getAndIncrementRuleNodeCounter() {
        return ctx.getAndIncrementRuleNodeCounter();
    }

    @NotNull
    public static TbMsg newMsg(String queueName, String type, EntityId originator, @NotNull TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return newMsg(queueName, type, originator, null, metaData, data, ruleChainId, ruleNodeId);
    }

    @NotNull
    public static TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, @NotNull TbMsgMetaData metaData, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    @NotNull
    public static TbMsg newMsg(String type, EntityId originator, @NotNull TbMsgMetaData metaData, String data) {
        return newMsg(type, originator, null, metaData, data);
    }

    @NotNull
    public static TbMsg newMsg(String type, EntityId originator, CustomerId customerId, @NotNull TbMsgMetaData metaData, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    // REALLY NEW MSG

    @NotNull
    public static TbMsg newMsg(String queueName, String type, EntityId originator, @NotNull TbMsgMetaData metaData, String data) {
        return newMsg(queueName, type, originator, null, metaData, data);
    }

    @NotNull
    public static TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, @NotNull TbMsgMetaData metaData, String data) {
        return new TbMsg(queueName, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, TbMsgCallback.EMPTY);
    }

    @NotNull
    public static TbMsg newMsg(String type, EntityId originator, CustomerId customerId, @NotNull TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, customerId,
                metaData.copy(), dataType, data, null, null, null, TbMsgCallback.EMPTY);
    }

    @NotNull
    public static TbMsg newMsg(String type, EntityId originator, @NotNull TbMsgMetaData metaData, TbMsgDataType dataType, String data) {
        return newMsg(type, originator, null, metaData, dataType, data);
    }

    // For Tests only

    @NotNull
    public static TbMsg newMsg(String type, EntityId originator, @NotNull TbMsgMetaData metaData, TbMsgDataType dataType, String data, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, null,
                metaData.copy(), dataType, data, ruleChainId, ruleNodeId, null, TbMsgCallback.EMPTY);
    }

    @NotNull
    public static TbMsg newMsg(String type, EntityId originator, @NotNull TbMsgMetaData metaData, String data, TbMsgCallback callback) {
        return new TbMsg(null, UUID.randomUUID(), System.currentTimeMillis(), type, originator, null,
                metaData.copy(), TbMsgDataType.JSON, data, null, null, null, callback);
    }

    @NotNull
    public static TbMsg transformMsg(@NotNull TbMsg tbMsg, String type, EntityId originator, @NotNull TbMsgMetaData metaData, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, type, originator, tbMsg.customerId, metaData.copy(), tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.callback);
    }

    @NotNull
    public static TbMsg transformMsgData(@NotNull TbMsg tbMsg, String data) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    @NotNull
    public static TbMsg transformMsg(@NotNull TbMsg tbMsg, @NotNull TbMsgMetaData metadata) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.type, tbMsg.originator, tbMsg.customerId, metadata.copy(), tbMsg.dataType,
                tbMsg.data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    @NotNull
    public static TbMsg transformMsg(@NotNull TbMsg tbMsg, CustomerId customerId) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.type, tbMsg.originator, customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, tbMsg.ruleChainId, tbMsg.ruleNodeId, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    @NotNull
    public static TbMsg transformMsg(@NotNull TbMsg tbMsg, RuleChainId ruleChainId) {
        return new TbMsg(tbMsg.queueName, tbMsg.id, tbMsg.ts, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, ruleChainId, null, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    @NotNull
    public static TbMsg transformMsg(@NotNull TbMsg tbMsg, String queueName) {
        return new TbMsg(queueName, tbMsg.id, tbMsg.ts, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, tbMsg.getRuleChainId(), null, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    @NotNull
    public static TbMsg transformMsg(@NotNull TbMsg tbMsg, RuleChainId ruleChainId, String queueName) {
        return new TbMsg(queueName, tbMsg.id, tbMsg.ts, tbMsg.type, tbMsg.originator, tbMsg.customerId, tbMsg.metaData, tbMsg.dataType,
                tbMsg.data, ruleChainId, null, tbMsg.ctx.copy(), tbMsg.getCallback());
    }

    //used for enqueueForTellNext
    @NotNull
    public static TbMsg newMsg(@NotNull TbMsg tbMsg, String queueName, RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        return new TbMsg(queueName, UUID.randomUUID(), tbMsg.getTs(), tbMsg.getType(), tbMsg.getOriginator(), tbMsg.customerId, tbMsg.getMetaData().copy(),
                tbMsg.getDataType(), tbMsg.getData(), ruleChainId, ruleNodeId, tbMsg.ctx.copy(), TbMsgCallback.EMPTY);
    }

    private TbMsg(String queueName, UUID id, long ts, String type, @Nullable EntityId originator, @Nullable CustomerId customerId, TbMsgMetaData metaData, TbMsgDataType dataType, String data,
                  RuleChainId ruleChainId, RuleNodeId ruleNodeId, @Nullable TbMsgProcessingCtx ctx, @Nullable TbMsgCallback callback) {
        this.id = id;
        this.queueName = queueName;
        if (ts > 0) {
            this.ts = ts;
        } else {
            this.ts = System.currentTimeMillis();
        }
        this.type = type;
        this.originator = originator;
        if (customerId == null || customerId.isNullUid()) {
            if (originator != null && originator.getEntityType() == EntityType.CUSTOMER) {
                this.customerId = (CustomerId) originator;
            } else {
                this.customerId = null;
            }
        } else {
            this.customerId = customerId;
        }
        this.metaData = metaData;
        this.dataType = dataType;
        this.data = data;
        this.ruleChainId = ruleChainId;
        this.ruleNodeId = ruleNodeId;
        this.ctx = ctx != null ? ctx : new TbMsgProcessingCtx();
        if (callback != null) {
            this.callback = callback;
        } else {
            this.callback = TbMsgCallback.EMPTY;
        }
    }

    @NotNull
    public static ByteString toByteString(@NotNull TbMsg msg) {
        return ByteString.copyFrom(toByteArray(msg));
    }

    public static byte[] toByteArray(@NotNull TbMsg msg) {
        MsgProtos.TbMsgProto.Builder builder = MsgProtos.TbMsgProto.newBuilder();
        builder.setId(msg.getId().toString());
        builder.setTs(msg.getTs());
        builder.setType(msg.getType());
        builder.setEntityType(msg.getOriginator().getEntityType().name());
        builder.setEntityIdMSB(msg.getOriginator().getId().getMostSignificantBits());
        builder.setEntityIdLSB(msg.getOriginator().getId().getLeastSignificantBits());

        if (msg.getCustomerId() != null) {
            builder.setCustomerIdMSB(msg.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(msg.getCustomerId().getId().getLeastSignificantBits());
        }

        if (msg.getRuleChainId() != null) {
            builder.setRuleChainIdMSB(msg.getRuleChainId().getId().getMostSignificantBits());
            builder.setRuleChainIdLSB(msg.getRuleChainId().getId().getLeastSignificantBits());
        }

        if (msg.getRuleNodeId() != null) {
            builder.setRuleNodeIdMSB(msg.getRuleNodeId().getId().getMostSignificantBits());
            builder.setRuleNodeIdLSB(msg.getRuleNodeId().getId().getLeastSignificantBits());
        }

        if (msg.getMetaData() != null) {
            builder.setMetaData(MsgProtos.TbMsgMetaDataProto.newBuilder().putAllData(msg.getMetaData().getData()).build());
        }

        builder.setDataType(msg.getDataType().ordinal());
        builder.setData(msg.getData());

        builder.setCtx(msg.ctx.toProto());
        return builder.build().toByteArray();
    }

    @NotNull
    public static TbMsg fromBytes(String queueName, byte[] data, TbMsgCallback callback) {
        try {
            MsgProtos.TbMsgProto proto = MsgProtos.TbMsgProto.parseFrom(data);
            @NotNull TbMsgMetaData metaData = new TbMsgMetaData(proto.getMetaData().getDataMap());
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
            @Nullable CustomerId customerId = null;
            @Nullable RuleChainId ruleChainId = null;
            @Nullable RuleNodeId ruleNodeId = null;
            if (proto.getCustomerIdMSB() != 0L && proto.getCustomerIdLSB() != 0L) {
                customerId = new CustomerId(new UUID(proto.getCustomerIdMSB(), proto.getCustomerIdLSB()));
            }
            if (proto.getRuleChainIdMSB() != 0L && proto.getRuleChainIdLSB() != 0L) {
                ruleChainId = new RuleChainId(new UUID(proto.getRuleChainIdMSB(), proto.getRuleChainIdLSB()));
            }
            if (proto.getRuleNodeIdMSB() != 0L && proto.getRuleNodeIdLSB() != 0L) {
                ruleNodeId = new RuleNodeId(new UUID(proto.getRuleNodeIdMSB(), proto.getRuleNodeIdLSB()));
            }

            TbMsgProcessingCtx ctx;
            if (proto.hasCtx()) {
                ctx = TbMsgProcessingCtx.fromProto(proto.getCtx());
            } else {
                // Backward compatibility with unprocessed messages fetched from queue after update.
                ctx = new TbMsgProcessingCtx(proto.getRuleNodeExecCounter());
            }

            TbMsgDataType dataType = TbMsgDataType.values()[proto.getDataType()];
            return new TbMsg(queueName, UUID.fromString(proto.getId()), proto.getTs(), proto.getType(), entityId, customerId,
                    metaData, dataType, proto.getData(), ruleChainId, ruleNodeId, ctx, callback);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException("Could not parse protobuf for TbMsg", e);
        }
    }

    @NotNull
    public TbMsg copyWithRuleChainId(RuleChainId ruleChainId) {
        return copyWithRuleChainId(ruleChainId, this.id);
    }

    @NotNull
    public TbMsg copyWithRuleChainId(RuleChainId ruleChainId, UUID msgId) {
        return new TbMsg(this.queueName, msgId, this.ts, this.type, this.originator, this.customerId,
                this.metaData, this.dataType, this.data, ruleChainId, null, this.ctx, callback);
    }

    @NotNull
    public TbMsg copyWithRuleNodeId(RuleChainId ruleChainId, RuleNodeId ruleNodeId, UUID msgId) {
        return new TbMsg(this.queueName, msgId, this.ts, this.type, this.originator, this.customerId,
                this.metaData, this.dataType, this.data, ruleChainId, ruleNodeId, this.ctx, callback);
    }

    @NotNull
    public TbMsgCallback getCallback() {
        // May be null in case of deserialization;
        if (callback != null) {
            return callback;
        } else {
            return TbMsgCallback.EMPTY;
        }
    }

    public void pushToStack(RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        ctx.push(ruleChainId, ruleNodeId);
    }

    @Nullable
    public TbMsgProcessingStackItem popFormStack() {
        return ctx.pop();
    }

    /**
     * Checks if the message is still valid for processing. May be invalid if the message pack is timed-out or canceled.
     * @return 'true' if message is valid for processing, 'false' otherwise.
     */
    public boolean isValid() {
        return getCallback().isMsgValid();
    }

    public long getMetaDataTs() {
        String tsStr = metaData.getValue("ts");
        if (!StringUtils.isEmpty(tsStr)) {
            try {
                return Long.parseLong(tsStr);
            } catch (NumberFormatException ignored) {
            }
        }
        return ts;
    }
}
