package org.echoiot.server.dao.sql.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.alarm.AlarmStatus;
import org.echoiot.server.common.data.id.AlarmId;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.dao.alarm.AlarmDao;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.echoiot.server.dao.AbstractJpaDaoTest;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 5/21/2017.
 */
@Slf4j
public class JpaAlarmDaoTest extends AbstractJpaDaoTest {

    @Resource
    private AlarmDao alarmDao;


    @Test
    public void testFindLatestByOriginatorAndType() throws ExecutionException, InterruptedException, TimeoutException {
        log.info("Current system time in millis = {}", System.currentTimeMillis());
        @NotNull UUID tenantId = UUID.fromString("d4b68f40-3e96-11e7-a884-898080180d6b");
        @NotNull UUID originator1Id = UUID.fromString("d4b68f41-3e96-11e7-a884-898080180d6b");
        @NotNull UUID originator2Id = UUID.fromString("d4b68f42-3e96-11e7-a884-898080180d6b");
        @NotNull UUID alarm1Id = UUID.fromString("d4b68f43-3e96-11e7-a884-898080180d6b");
        @NotNull UUID alarm2Id = UUID.fromString("d4b68f44-3e96-11e7-a884-898080180d6b");
        @NotNull UUID alarm3Id = UUID.fromString("d4b68f45-3e96-11e7-a884-898080180d6b");
        int alarmCountBeforeSave = alarmDao.find(TenantId.fromUUID(tenantId)).size();
        saveAlarm(alarm1Id, tenantId, originator1Id, "TEST_ALARM");
        //The timestamp of the startTime should be different in order for test to always work
        Thread.sleep(1);
        saveAlarm(alarm2Id, tenantId, originator1Id, "TEST_ALARM");
        saveAlarm(alarm3Id, tenantId, originator2Id, "TEST_ALARM");
        int alarmCountAfterSave = alarmDao.find(TenantId.fromUUID(tenantId)).size();
        assertEquals(3, alarmCountAfterSave - alarmCountBeforeSave);
        ListenableFuture<Alarm> future = alarmDao
                .findLatestByOriginatorAndTypeAsync(TenantId.fromUUID(tenantId), new DeviceId(originator1Id), "TEST_ALARM");
        Alarm alarm = future.get(30, TimeUnit.SECONDS);
        assertNotNull(alarm);
        Assert.assertEquals(alarm2Id, alarm.getId().getId());
    }

    private void saveAlarm(UUID id, UUID tenantId, UUID deviceId, String type) {
        @NotNull Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(id));
        alarm.setTenantId(TenantId.fromUUID(tenantId));
        alarm.setOriginator(new DeviceId(deviceId));
        alarm.setType(type);
        alarm.setPropagate(true);
        alarm.setStartTs(System.currentTimeMillis());
        alarm.setEndTs(System.currentTimeMillis());
        alarm.setStatus(AlarmStatus.ACTIVE_UNACK);
        alarmDao.save(TenantId.fromUUID(tenantId), alarm);
    }
}
