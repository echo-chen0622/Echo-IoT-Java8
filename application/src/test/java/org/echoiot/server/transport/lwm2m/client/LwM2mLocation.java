package org.echoiot.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.response.ReadResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Destroyable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LwM2mLocation extends BaseInstanceEnabler implements Destroyable {

    private float latitude;
    private float longitude;
    private float scaleFactor;
    private Date timestamp;
    protected static final Random RANDOM = new Random();
    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 5);

    public LwM2mLocation() {
        this(null, null, 1.0f);
    }

    public LwM2mLocation(@Nullable Float latitude, @Nullable Float longitude, float scaleFactor) {

        if (latitude != null) {
            this.latitude = latitude + 90f;
        } else {
            this.latitude = RANDOM.nextInt(180);
        }
        if (longitude != null) {
            this.longitude = longitude + 180f;
        } else {
            this.longitude = RANDOM.nextInt(360);
        }
        this.scaleFactor = scaleFactor;
        timestamp = new Date();
    }

    public LwM2mLocation(@Nullable Float latitude, @Nullable Float longitude, float scaleFactor, @NotNull ScheduledExecutorService executorService, @Nullable Integer id) {
        try {
            if (id != null) this.setId(id);
            if (latitude != null) {
                this.latitude = latitude + 90f;
            } else {
                this.latitude = RANDOM.nextInt(180);
            }
            if (longitude != null) {
                this.longitude = longitude + 180f;
            } else {
                this.longitude = RANDOM.nextInt(360);
            }
            this.scaleFactor = scaleFactor;
            timestamp = new Date();
            executorService.scheduleWithFixedDelay(() -> {
                fireResourceChange(0);
                fireResourceChange(1);
            }, 10000, 10000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            log.error("[{}]Throwable", e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceId) {
        log.info("Read on Location resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 0:
                return ReadResponse.success(resourceId, getLatitude());
            case 1:
                return ReadResponse.success(resourceId, getLongitude());
            case 5:
                return ReadResponse.success(resourceId, getTimestamp());
            default:
                return super.read(identity, resourceId);
        }
    }

    public void moveLocation(@NotNull String nextMove) {
        switch (nextMove.charAt(0)) {
            case 'w':
                moveLatitude(1.0f);
                break;
            case 'a':
                moveLongitude(-1.0f);
                break;
            case 's':
                moveLatitude(-1.0f);
                break;
            case 'd':
                moveLongitude(1.0f);
                break;
        }
    }

    private void moveLatitude(float delta) {
        latitude = latitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourceChange(0);
        fireResourceChange(5);
    }

    private void moveLongitude(float delta) {
        longitude = longitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourceChange(1);
        fireResourceChange(5);

    }

    public float getLatitude() {
        return latitude - 90.0f;
    }

    public float getLongitude() {
        return longitude - 180.f;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

}
