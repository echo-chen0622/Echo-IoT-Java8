package org.echoiot.server.service.entitiy.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import org.echoiot.server.common.data.User;
import org.echoiot.server.common.data.alarm.Alarm;
import org.echoiot.server.common.data.exception.EchoiotException;

public interface TbAlarmService {

    Alarm save(Alarm entity, User user) throws EchoiotException;

    ListenableFuture<Void> ack(Alarm alarm, User user);

    ListenableFuture<Void> clear(Alarm alarm, User user);

    Boolean delete(Alarm alarm, User user);
}
