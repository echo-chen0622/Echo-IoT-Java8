package org.echoiot.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.Destroyable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class LwM2mTemperatureSensor extends BaseInstanceEnabler implements Destroyable {

    private static final String UNIT_CELSIUS = "cel";
    private double currentTemp = 20d;
    private double minMeasuredValue = currentTemp;
    private double maxMeasuredValue = currentTemp;
    protected static final Random RANDOM = new Random();
    private static final List<Integer> supportedResources = Arrays.asList(5601, 5602, 5700, 5701);

    public LwM2mTemperatureSensor() {

    }

    public LwM2mTemperatureSensor(ScheduledExecutorService executorService, @Nullable Integer id) {
        try {
            if (id != null) this.setId(id);
        executorService.scheduleWithFixedDelay(this::adjustTemperature, 2000, 2000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            log.error("[{}]Throwable", e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        log.info("Read on Temperature resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 5601:
                return ReadResponse.success(resourceId, getTwoDigitValue(minMeasuredValue));
            case 5602:
                return ReadResponse.success(resourceId, getTwoDigitValue(maxMeasuredValue));
            case 5700:
                return ReadResponse.success(resourceId, getTwoDigitValue(currentTemp));
            case 5701:
                return ReadResponse.success(resourceId, UNIT_CELSIUS);
            default:
                return super.read(identity, resourceId);
        }
    }

    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        log.info("Execute on Temperature resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        if (resourceId == 5605) {
            resetMinMaxMeasuredValues();
            return ExecuteResponse.success();
        }
        return super.execute(identity, resourceId, params);
    }

    private double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void adjustTemperature() {
        float delta = (RANDOM.nextInt(20) - 10) / 10f;
        currentTemp += delta;
        @Nullable Integer changedResource = adjustMinMaxMeasuredValue(currentTemp);
        fireResourceChange(5700);
        if (changedResource != null) {
            fireResourceChange(changedResource);
        }
    }

    @Nullable
    private synchronized Integer adjustMinMaxMeasuredValue(double newTemperature) {
        if (newTemperature > maxMeasuredValue) {
            maxMeasuredValue = newTemperature;
            return 5602;
        } else if (newTemperature < minMeasuredValue) {
            minMeasuredValue = newTemperature;
            return 5601;
        } else {
            return null;
        }
    }

    private void resetMinMaxMeasuredValues() {
        minMeasuredValue = currentTemp;
        maxMeasuredValue = currentTemp;
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    @Override
    public void destroy() {
    }


}
