package org.echoiot.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.ResourceType;
import org.echoiot.server.common.data.TbResource;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.lwm2m.LwM2mConstants;
import org.echoiot.server.queue.util.TbLwM2mTransportComponent;
import org.echoiot.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.eclipse.leshan.core.model.DefaultDDFFileValidator;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@TbLwM2mTransportComponent
public class LwM2mVersionedModelProvider implements LwM2mModelProvider {

    private final LwM2mClientContext lwM2mClientContext;
    private final LwM2mTransportServerHelper helper;
    private final LwM2mTransportContext context;
    @NotNull
    private final ConcurrentMap<TenantId, ConcurrentMap<String, ObjectModel>> models;

    public LwM2mVersionedModelProvider(@Lazy LwM2mClientContext lwM2mClientContext, LwM2mTransportServerHelper helper, LwM2mTransportContext context) {
        this.lwM2mClientContext = lwM2mClientContext;
        this.helper = helper;
        this.context = context;
        this.models = new ConcurrentHashMap<>();
    }

    private String getKeyIdVer(@Nullable Integer objectId, @Nullable String version) {
        return objectId != null ? objectId + LwM2mConstants.LWM2M_SEPARATOR_KEY + ((version == null || version.isEmpty()) ? ObjectModel.DEFAULT_VERSION : version) : null;
    }

    @NotNull
    @Override
    public LwM2mModel getObjectModel(@NotNull Registration registration) {
        return new DynamicModel(registration);
    }

    public void evict(@NotNull TenantId tenantId, String key) {
        if (tenantId.isNullUid()) {
            models.values().forEach(m -> m.remove(key));
        } else {
            models.get(tenantId).remove(key);
        }
    }

    private class DynamicModel implements LwM2mModel {
        private final Registration registration;
        private final TenantId tenantId;
        @NotNull
        private final Lock modelsLock;

        public DynamicModel(@NotNull Registration registration) {
            this.registration = registration;
            this.tenantId = lwM2mClientContext.getClientByEndpoint(registration.getEndpoint()).getTenantId();
            this.modelsLock = new ReentrantLock();
            if (tenantId != null) {
                models.computeIfAbsent(tenantId, t -> new ConcurrentHashMap<>());
            }
        }

        @Nullable
        @Override
        public ResourceModel getResourceModel(int objectId, int resourceId) {
            try {
                @Nullable ObjectModel objectModel = getObjectModel(objectId);
                if (objectModel != null)
                    return objectModel.resources.get(resourceId);
                else
                    log.trace("Tenant hasn't such the TbResources: Object model with id [{}/0/{}].", objectId, resourceId);
                return null;
            } catch (Exception e) {
                log.error("", e);
                return null;
            }
        }

        @Nullable
        @Override
        public ObjectModel getObjectModel(int objectId) {
            String version = registration.getSupportedVersion(objectId);
            if (version != null) {
                return this.getObjectModelDynamic(objectId, version);
            }
            return null;
        }

        @NotNull
        @Override
        public Collection<ObjectModel> getObjectModels() {
            Map<Integer, String> supportedObjects = this.registration.getSupportedObject();
            @NotNull Collection<ObjectModel> result = new ArrayList<>(supportedObjects.size());
            for (@NotNull Map.Entry<Integer, String> supportedObject : supportedObjects.entrySet()) {
                @Nullable ObjectModel objectModel = this.getObjectModelDynamic(supportedObject.getKey(), supportedObject.getValue());
                if (objectModel != null) {
                    result.add(objectModel);
                }
            }
            return result;
        }

        @Nullable
        private ObjectModel getObjectModelDynamic(Integer objectId, String version) {
            @Nullable String key = getKeyIdVer(objectId, version);
            @Nullable ObjectModel objectModel = tenantId != null ? models.get(tenantId).get(key) : null;
            if (tenantId != null && objectModel == null) {
                modelsLock.lock();
                try {
                    objectModel = models.get(tenantId).get(key);
                    if (objectModel == null) {
                        objectModel = getObjectModel(key);
                    }
                    if (objectModel != null) {
                        models.get(tenantId).put(key, objectModel);
                    } else {
                        log.error("Tenant hasn't such the resource: Object model with id [{}] version [{}].", objectId, version);
                    }
                } finally {
                    modelsLock.unlock();
                }
            }

            return objectModel;
        }

        @Nullable
        private ObjectModel getObjectModel(String key) {
            Optional<TbResource> tbResource = context.getTransportResourceCache().get(this.tenantId, ResourceType.LWM2M_MODEL, key);
            return tbResource.map(resource -> helper.parseFromXmlToObjectModel(
                    Base64.getDecoder().decode(resource.getData()),
                    key + ".xml",
                    new DefaultDDFFileValidator())).orElse(null);
        }
    }
}
