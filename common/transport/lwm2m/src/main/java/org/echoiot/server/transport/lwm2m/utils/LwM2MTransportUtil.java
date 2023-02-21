package org.echoiot.server.transport.lwm2m.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.DeviceProfile;
import org.echoiot.server.common.data.DeviceTransportType;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.echoiot.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.echoiot.server.common.data.lwm2m.LwM2mConstants;
import org.echoiot.server.common.data.ota.OtaPackageKey;
import org.echoiot.server.common.transport.util.JsonUtils;
import org.echoiot.server.transport.lwm2m.config.TbLwM2mVersion;
import org.echoiot.server.transport.lwm2m.server.LwM2mOtaConvert;
import org.echoiot.server.transport.lwm2m.server.client.ResourceValue;
import org.echoiot.server.transport.lwm2m.server.downlink.HasVersionedId;
import org.echoiot.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService;
import org.echoiot.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateResult;
import org.echoiot.server.transport.lwm2m.server.ota.firmware.FirmwareUpdateState;
import org.echoiot.server.transport.lwm2m.server.ota.software.SoftwareUpdateResult;
import org.echoiot.server.transport.lwm2m.server.ota.software.SoftwareUpdateState;
import org.echoiot.server.transport.lwm2m.server.uplink.DefaultLwM2mUplinkMsgHandler;
import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.request.SimpleDownlinkRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.registration.Registration;
import org.echoiot.common.util.JacksonUtil;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.leshan.core.attributes.Attribute.DIMENSION;
import static org.eclipse.leshan.core.attributes.Attribute.GREATER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.LESSER_THAN;
import static org.eclipse.leshan.core.attributes.Attribute.MAXIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.MINIMUM_PERIOD;
import static org.eclipse.leshan.core.attributes.Attribute.OBJECT_VERSION;
import static org.eclipse.leshan.core.attributes.Attribute.STEP;
import static org.eclipse.leshan.core.model.ResourceModel.Type.BOOLEAN;
import static org.eclipse.leshan.core.model.ResourceModel.Type.FLOAT;
import static org.eclipse.leshan.core.model.ResourceModel.Type.INTEGER;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OBJLNK;
import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.core.model.ResourceModel.Type.STRING;
import static org.eclipse.leshan.core.model.ResourceModel.Type.TIME;

@Slf4j
public class LwM2MTransportUtil {

    public static final String LWM2M_OBJECT_VERSION_DEFAULT = "1.0";

    public static final String LOG_LWM2M_TELEMETRY = "transportLog";
    public static final String LOG_LWM2M_INFO = "info";
    public static final String LOG_LWM2M_ERROR = "error";
    public static final String LOG_LWM2M_WARN = "warn";
    public static final int BOOTSTRAP_DEFAULT_SHORT_ID = 0;

    public enum LwM2MClientStrategy {
        CLIENT_STRATEGY_1(1, "Read only resources marked as observation"),
        CLIENT_STRATEGY_2(2, "Read all client resources");

        public int code;
        public String type;

        LwM2MClientStrategy(int code, String type) {
            this.code = code;
            this.type = type;
        }

        @NotNull
        public static LwM2MClientStrategy fromStrategyClientByType(String type) {
            for (@NotNull LwM2MClientStrategy to : LwM2MClientStrategy.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client Strategy type  : %s", type));
        }

        @NotNull
        public static LwM2MClientStrategy fromStrategyClientByCode(int code) {
            for (@NotNull LwM2MClientStrategy to : LwM2MClientStrategy.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client Strategy code : %s", code));
        }
    }

    public static boolean equalsResourceValue(@NotNull Object valueOld, @NotNull Object valueNew, @NotNull ResourceModel.Type type, LwM2mPath
            resourcePath) throws CodecException {
        switch (type) {
            case BOOLEAN:
            case INTEGER:
            case FLOAT:
                return String.valueOf(valueOld).equals(String.valueOf(valueNew));
            case TIME:
                return ((Date) valueOld).getTime() == ((Date) valueNew).getTime();
            case STRING:
            case OBJLNK:
                return valueOld.equals(valueNew);
            case OPAQUE:
                return Arrays.equals(Hex.decodeHex(((String) valueOld).toCharArray()), Hex.decodeHex(((String) valueNew).toCharArray()));
            default:
                throw new CodecException("Invalid value type for resource %s, type %s", resourcePath, type);
        }
    }

    @NotNull
    public static LwM2mOtaConvert convertOtaUpdateValueToString(String pathIdVer, @NotNull Object value, ResourceModel.Type currentType) {
        @Nullable String path = fromVersionedIdToObjectId(pathIdVer);
        @NotNull LwM2mOtaConvert lwM2mOtaConvert = new LwM2mOtaConvert();
        if (path != null) {
            if (DefaultLwM2MOtaUpdateService.FW_STATE_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(FirmwareUpdateState.fromStateFwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (DefaultLwM2MOtaUpdateService.FW_RESULT_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(FirmwareUpdateResult.fromUpdateResultFwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (DefaultLwM2MOtaUpdateService.SW_STATE_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(SoftwareUpdateState.fromUpdateStateSwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            } else if (DefaultLwM2MOtaUpdateService.SW_RESULT_ID.equals(path)) {
                lwM2mOtaConvert.setCurrentType(STRING);
                lwM2mOtaConvert.setValue(SoftwareUpdateResult.fromUpdateResultSwByCode(((Long) value).intValue()).getType());
                return lwM2mOtaConvert;
            }
        }
        lwM2mOtaConvert.setCurrentType(currentType);
        lwM2mOtaConvert.setValue(value);
        return lwM2mOtaConvert;
    }

    @NotNull
    public static Lwm2mDeviceProfileTransportConfiguration toLwM2MClientProfile(@NotNull DeviceProfile deviceProfile) {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration.getType().equals(DeviceTransportType.LWM2M)) {
            return (Lwm2mDeviceProfileTransportConfiguration) transportConfiguration;
        } else {
            log.info("[{}] Received profile with invalid transport configuration: {}", deviceProfile.getId(), deviceProfile.getProfileData().getTransportConfiguration());
            throw new IllegalArgumentException("Received profile with invalid transport configuration: " + transportConfiguration.getType());
        }
    }

    public static List<LwM2MBootstrapServerCredential> getBootstrapParametersFromEchoiot(@NotNull DeviceProfile deviceProfile) {
        return toLwM2MClientProfile(deviceProfile).getBootstrap();
    }

    @Nullable
    public static String fromVersionedIdToObjectId(@Nullable String pathIdVer) {
        try {
            if (pathIdVer == null) {
                return null;
            }
            @NotNull String[] keyArray = pathIdVer.split(LwM2mConstants.LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1 && keyArray[1].split(LwM2mConstants.LWM2M_SEPARATOR_KEY).length == 2) {
                keyArray[1] = keyArray[1].split(LwM2mConstants.LWM2M_SEPARATOR_KEY)[0];
                return StringUtils.join(keyArray, LwM2mConstants.LWM2M_SEPARATOR_PATH);
            } else {
                return pathIdVer;
            }
        } catch (Exception e) {
            log.debug("Issue converting path with version [{}] to path without version: ", pathIdVer, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param path - pathId or pathIdVer
     * @return
     */
    @Nullable
    public static String getVerFromPathIdVerOrId(@NotNull String path) {
        try {
            @NotNull String[] keyArray = path.split(LwM2mConstants.LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1) {
                @NotNull String[] keyArrayVer = keyArray[1].split(LwM2mConstants.LWM2M_SEPARATOR_KEY);
                return keyArrayVer.length == 2 ? keyArrayVer[1] : LWM2M_OBJECT_VERSION_DEFAULT;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Nullable
    public static String validPathIdVer(@NotNull String pathIdVer, @NotNull Registration registration) throws
            IllegalArgumentException {
        if (!pathIdVer.contains(LwM2mConstants.LWM2M_SEPARATOR_PATH)) {
            throw new IllegalArgumentException("Error:");
        } else {
            @NotNull String[] keyArray = pathIdVer.split(LwM2mConstants.LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1 && keyArray[1].split(LwM2mConstants.LWM2M_SEPARATOR_KEY).length == 2) {
                return pathIdVer;
            } else {
                return convertObjectIdToVersionedId(pathIdVer, registration);
            }
        }
    }

    @Nullable
    public static String convertObjectIdToVersionedId(@NotNull String path, @NotNull Registration registration) {
        String ver = registration.getSupportedObject().get(new LwM2mPath(path).getObjectId());
        ver = ver != null ? ver : TbLwM2mVersion.VERSION_1_0.getVersion().toString();
        try {
            @NotNull String[] keyArray = path.split(LwM2mConstants.LWM2M_SEPARATOR_PATH);
            if (keyArray.length > 1) {
                keyArray[1] = keyArray[1] + LwM2mConstants.LWM2M_SEPARATOR_KEY + ver;
                return StringUtils.join(keyArray, LwM2mConstants.LWM2M_SEPARATOR_PATH);
            } else {
                return path;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static String validateObjectVerFromKey(@NotNull String key) {
        try {
            return (key.split(LwM2mConstants.LWM2M_SEPARATOR_PATH)[1].split(LwM2mConstants.LWM2M_SEPARATOR_KEY)[1]);
        } catch (Exception e) {
            return ObjectModel.DEFAULT_VERSION;
        }
    }

    /**
     * As example:
     * a)Write-Attributes/3/0/9?pmin=1 means the Battery Level value will be notified
     * to the Server with a minimum interval of 1sec;
     * this value is set at theResource level.
     * b)Write-Attributes/3/0/9?pmin means the Battery Level will be notified
     * to the Server with a minimum value (pmin) given by the default one
     * (resource 2 of Object Server ID=1),
     * or with another value if this Attribute has been set at another level
     * (Object or Object Instance: see section5.1.1).
     * c)Write-Attributes/3/0?pmin=10 means that all Resources of Instance 0 of the Object ‘Device (ID:3)’
     * will be notified to the Server with a minimum interval of 10 sec;
     * this value is set at the Object Instance level.
     * d)Write-Attributes /3/0/9?gt=45&st=10 means the Battery Level will be notified to the Server
     * when:
     * a.old value is 20 and new value is 35 due to step condition
     * b.old value is 45 and new value is 50 due to gt condition
     * c.old value is 50 and new value is 40 due to both gt and step conditions
     * d.old value is 35 and new value is 20 due to step conditione)
     * Write-Attributes /3/0/9?lt=20&gt=85&st=10 means the Battery Level will be notified to the Server
     * when:
     * a.old value is 17 and new value is 24 due to lt condition
     * b.old value is 75 and new value is 90 due to both gt and step conditions
     * String uriQueries = "pmin=10&pmax=60";
     * AttributeSet attributes = AttributeSet.parse(uriQueries);
     * WriteAttributesRequest request = new WriteAttributesRequest(target, attributes);
     * Attribute gt = new Attribute(GREATER_THAN, Double.valueOf("45"));
     * Attribute st = new Attribute(LESSER_THAN, Double.valueOf("10"));
     * Attribute pmax = new Attribute(MAXIMUM_PERIOD, "60");
     * Attribute [] attrs = {gt, st};
     */
    @Nullable
    public static SimpleDownlinkRequest createWriteAttributeRequest(@NotNull String target, Object params, @NotNull DefaultLwM2mUplinkMsgHandler serviceImpl) {
        @NotNull AttributeSet attrSet = new AttributeSet(createWriteAttributes(params, serviceImpl, target));
        return attrSet.getAttributes().size() > 0 ? new WriteAttributesRequest(target, attrSet) : null;
    }

    private static Attribute[] createWriteAttributes(Object params, @NotNull DefaultLwM2mUplinkMsgHandler serviceImpl, @NotNull String target) {
        @NotNull List<Attribute> attributeLists = new ArrayList<>();
        @Nullable Map<String, Object> map = JacksonUtil.convertValue(params, new TypeReference<>() {
        });
        map.forEach((k, v) -> {
            if (StringUtils.trimToNull(v.toString()) != null) {
                @Nullable Object attrValue = convertWriteAttributes(k, v, serviceImpl, target);
                if (attrValue != null) {
                    @Nullable Attribute attribute = createAttribute(k, attrValue);
                    if (attribute != null) {
                        attributeLists.add(new Attribute(k, attrValue));
                    }
                }
            }
        });
        return attributeLists.toArray(Attribute[]::new);
    }

    /**
     * "UNSIGNED_INTEGER":  // Number -> Integer Example:
     * Alarm Timestamp [32-bit unsigned integer]
     * Short Server ID, Object ID, Object Instance ID, Resource ID, Resource Instance ID
     * "CORELINK": // String used in Attribute
     */
    @Nullable
    public static ResourceModel.Type equalsResourceTypeGetSimpleName(@NotNull Object value) {
        switch (value.getClass().getSimpleName()) {
            case "Double":
                return FLOAT;
            case "Integer":
                return INTEGER;
            case "String":
                return STRING;
            case "Boolean":
                return BOOLEAN;
            case "byte[]":
                return OPAQUE;
            case "Date":
                return TIME;
            case "ObjectLink":
                return OBJLNK;
            default:
                return null;
        }
    }

    public static void validateVersionedId(@NotNull LwM2mClient client, @NotNull HasVersionedId request) {
        String msgExceptionStr = "";
        if (request.getObjectId() == null) {
            msgExceptionStr = "Specified object id is null!";
        } else {
            msgExceptionStr = client.isValidObjectVersion(request.getVersionedId());
        }
        if (!msgExceptionStr.isEmpty()) {
            throw new IllegalArgumentException(msgExceptionStr);
        }
    }

    @NotNull
    public static Map<Integer, Object> convertMultiResourceValuesFromRpcBody(Object value, ResourceModel.Type type, String versionedId) throws Exception {
            @Nullable String valueJsonStr = JacksonUtil.toString(value);
            JsonElement element = JsonUtils.parse(valueJsonStr);
            return convertMultiResourceValuesFromJson(element, type, versionedId);
    }

    @NotNull
    public static Map<Integer, Object> convertMultiResourceValuesFromJson(@NotNull JsonElement newValProto, ResourceModel.Type type, String versionedId) {
        @NotNull Map<Integer, Object> newValues = new HashMap<>();
        newValProto.getAsJsonObject().entrySet().forEach((obj) -> {
            newValues.put(Integer.valueOf(obj.getKey()), LwM2mValueConverterImpl.getInstance().convertValue(obj.getValue().getAsString(),
                    STRING, type, new LwM2mPath(fromVersionedIdToObjectId(versionedId))));
        });
        return newValues;
    }

    @Nullable
    public static Object convertWriteAttributes(@NotNull String type, @NotNull Object value, @NotNull DefaultLwM2mUplinkMsgHandler serviceImpl, @NotNull String target) {
        switch (type) {
            /** Integer [0:255]; */
            case DIMENSION:
                Long dim = (Long) serviceImpl.converter.convertValue(value, equalsResourceTypeGetSimpleName(value), INTEGER, new LwM2mPath(target));
                return dim >= 0 && dim <= 255 ? dim : null;
            /**String;*/
            case OBJECT_VERSION:
                return serviceImpl.converter.convertValue(value, equalsResourceTypeGetSimpleName(value), STRING, new LwM2mPath(target));
            /**INTEGER */
            case MINIMUM_PERIOD:
            case MAXIMUM_PERIOD:
                return serviceImpl.converter.convertValue(value, equalsResourceTypeGetSimpleName(value), INTEGER, new LwM2mPath(target));
            /**Float; */
            case GREATER_THAN:
            case LESSER_THAN:
            case STEP:
                if (value.getClass().getSimpleName().equals("String")) {
                    value = Double.valueOf((String) value);
                }
                return serviceImpl.converter.convertValue(value, equalsResourceTypeGetSimpleName(value), FLOAT, new LwM2mPath(target));
            default:
                return null;
        }
    }

    @Nullable
    private static Attribute createAttribute(@NotNull String key, @NotNull Object attrValue) {
        try {
            return new Attribute(key, attrValue);
        } catch (Exception e) {
            log.error("CreateAttribute, not valid parameter key: [{}], attrValue: [{}], error: [{}]", key, attrValue, e.getMessage());
            return null;
        }
    }

    /**
     * @param lwM2MClient -
     * @param path        -
     * @return - return value of Resource by idPath
     */
    @Nullable
    public static LwM2mResource getResourceValueFromLwM2MClient(@NotNull LwM2mClient lwM2MClient, String path) {
        @Nullable LwM2mResource lwm2mResourceValue = null;
        ResourceValue resourceValue = lwM2MClient.getResources().get(path);
        if (resourceValue != null) {
            if (new LwM2mPath(fromVersionedIdToObjectId(path)).isResource()) {
                lwm2mResourceValue = lwM2MClient.getResources().get(path).getLwM2mResource();
            }
        }
        return lwm2mResourceValue;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static Optional<String> contentToString(Object content) {
        try {
            @Nullable String value = null;
            @Nullable LwM2mResource resource = null;
            @Nullable String key = null;
            if (content instanceof Map) {
                @NotNull Map<Object, Object> contentAsMap = (Map<Object, Object>) content;
                if (contentAsMap.size() == 1) {
                    for (@NotNull Map.Entry<Object, Object> kv : contentAsMap.entrySet()) {
                        if (kv.getValue() instanceof LwM2mResource) {
                            key = kv.getKey().toString();
                            resource = (LwM2mResource) kv.getValue();
                        }
                    }
                }
            } else if (content instanceof LwM2mResource) {
                resource = (LwM2mResource) content;
            }
            if (resource != null && resource.getType() == OPAQUE) {
                value = opaqueResourceToString(resource, key);
            }
            value = value == null ? content.toString() : value;
            return Optional.of(value);
        } catch (Exception e) {
            log.debug("Failed to convert content " + content + " to string", e);
            return Optional.ofNullable(content != null ? content.toString() : null);
        }
    }

    @Nullable
    private static String opaqueResourceToString(LwM2mResource resource, @Nullable String key) {
        @Nullable String value = null;
        @NotNull StringBuilder builder = new StringBuilder();
        if (resource instanceof LwM2mSingleResource) {
            builder.append("LwM2mSingleResource");
            if (key == null) {
                builder.append(" id=").append(resource.getId());
            } else {
                builder.append(" key=").append(key);
            }
            builder.append(" value=").append(opaqueToString((byte[]) resource.getValue()));
            builder.append(" type=").append(OPAQUE);
            value = builder.toString();
        } else if (resource instanceof LwM2mMultipleResource) {
            builder.append("LwM2mMultipleResource");
            if (key == null) {
                builder.append(" id=").append(resource.getId());
            } else {
                builder.append(" key=").append(key);
            }
            builder.append(" values={");
            if (resource.getInstances().size() > 0) {
                builder.append(multiInstanceOpaqueToString((LwM2mMultipleResource) resource));
            }
            builder.append("}");
            builder.append(" type=").append(OPAQUE);
            value = builder.toString();
        }
        return value;
    }

    @NotNull
    private static String multiInstanceOpaqueToString(@NotNull LwM2mMultipleResource resource) {
        @NotNull StringBuilder builder = new StringBuilder();
        resource.getInstances().values()
                .forEach(v -> builder.append(" id=").append(v.getId()).append(" value=").append(Hex.encodeHexString((byte[]) v.getValue())).append(", "));
        int startInd = builder.lastIndexOf(", ");
        if (startInd > 0) {
            builder.delete(startInd, startInd + 2);
        }
        return builder.toString();
    }

    @NotNull
    private static String opaqueToString(@NotNull byte[] value) {
        @NotNull String opaque = Hex.encodeHexString(value);
        return opaque.length() > 1024 ? opaque.substring(0, 1024) : opaque;
    }

    @NotNull
    public static LwM2mModel createModelsDefault() {
        return new StaticModel(ObjectLoader.loadDefault());
    }

    public static boolean compareAttNameKeyOta(@NotNull String attrName) {
        for (@NotNull OtaPackageKey value : OtaPackageKey.values()) {
            if (attrName.contains(value.getValue())) return true;
        }
        return false;
    }

    public static boolean valueEquals(Object newValue, Object oldValue) {
        String newValueStr;
        String oldValueStr;
        if (oldValue instanceof byte[]) {
            oldValueStr = Hex.encodeHexString((byte[]) oldValue);
        } else {
            oldValueStr = oldValue.toString();
        }
        if (newValue instanceof byte[]) {
            newValueStr = Hex.encodeHexString((byte[]) newValue);
        } else {
            newValueStr = newValue.toString();
        }
        return newValueStr.equals(oldValueStr);
    }
}
