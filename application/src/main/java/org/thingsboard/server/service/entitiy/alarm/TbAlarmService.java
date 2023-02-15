package org.thingsboard.server.service.entitiy.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.exception.ThingsboardException;

public interface TbAlarmService {

    Alarm save(Alarm entity, User user) throws ThingsboardException;

    ListenableFuture<Void> ack(Alarm alarm, User user);

    ListenableFuture<Void> clear(Alarm alarm, User user);

    Boolean delete(Alarm alarm, User user);
}
