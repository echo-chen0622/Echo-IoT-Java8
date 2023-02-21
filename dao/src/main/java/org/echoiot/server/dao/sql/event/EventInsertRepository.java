package org.echoiot.server.dao.sql.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.echoiot.server.common.data.event.ErrorEvent;
import org.echoiot.server.common.data.event.Event;
import org.echoiot.server.common.data.event.EventType;
import org.echoiot.server.common.data.event.LifecycleEvent;
import org.echoiot.server.common.data.event.RuleChainDebugEvent;
import org.echoiot.server.common.data.event.RuleNodeDebugEvent;
import org.echoiot.server.common.data.event.StatisticsEvent;
import org.echoiot.server.dao.util.SqlDao;

import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Repository
@Transactional
@SqlDao
public class EventInsertRepository {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));

    private static final String EMPTY_STR = "";

    private final Map<EventType, String> insertStmtMap = new ConcurrentHashMap<>();

    @Resource
    protected JdbcTemplate jdbcTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("${sql.remove_null_chars:true}")
    private boolean removeNullChars;

    @PostConstruct
    public void init() {
        insertStmtMap.put(EventType.ERROR, "INSERT INTO " + EventType.ERROR.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_method, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.LC_EVENT, "INSERT INTO " + EventType.LC_EVENT.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_type, e_success, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.STATS, "INSERT INTO " + EventType.STATS.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_messages_processed, e_errors_occurred) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.DEBUG_RULE_NODE, "INSERT INTO " + EventType.DEBUG_RULE_NODE.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_type, e_entity_id, e_entity_type, e_msg_id, e_msg_type, e_data_type, e_relation_type, e_data, e_metadata, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.DEBUG_RULE_CHAIN, "INSERT INTO " + EventType.DEBUG_RULE_CHAIN.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_message, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
    }

    public void save(@NotNull List<Event> entities) {
        @NotNull Map<EventType, List<Event>> eventsByType = entities.stream().collect(Collectors.groupingBy(Event::getType, Collectors.toList()));
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (@NotNull var entry : eventsByType.entrySet()) {
                    jdbcTemplate.batchUpdate(insertStmtMap.get(entry.getKey()), getStatementSetter(entry.getKey(), entry.getValue()));
                }
            }
        });
    }

    @NotNull
    private BatchPreparedStatementSetter getStatementSetter(@NotNull EventType eventType, @NotNull List<Event> events) {
        switch (eventType) {
            case ERROR:
                return getErrorEventSetter(events);
            case LC_EVENT:
                return getLcEventSetter(events);
            case STATS:
                return getStatsEventSetter(events);
            case DEBUG_RULE_NODE:
                return getRuleNodeEventSetter(events);
            case DEBUG_RULE_CHAIN:
                return getRuleChainEventSetter(events);
            default:
                throw new RuntimeException(eventType + " support is not implemented!");
        }
    }

    @NotNull
    private BatchPreparedStatementSetter getErrorEventSetter(@NotNull List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ErrorEvent event = (ErrorEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getMethod());
                safePutString(ps, 7, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    @NotNull
    private BatchPreparedStatementSetter getLcEventSetter(@NotNull List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                LifecycleEvent event = (LifecycleEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getLcEventType());
                ps.setBoolean(7, event.isSuccess());
                safePutString(ps, 8, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    @NotNull
    private BatchPreparedStatementSetter getStatsEventSetter(@NotNull List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                StatisticsEvent event = (StatisticsEvent) events.get(i);
                setCommonEventFields(ps, event);
                ps.setLong(6, event.getMessagesProcessed());
                ps.setLong(7, event.getErrorsOccurred());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    @NotNull
    private BatchPreparedStatementSetter getRuleNodeEventSetter(@NotNull List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RuleNodeDebugEvent event = (RuleNodeDebugEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getEventType());
                safePutUUID(ps, 7, event.getEventEntity() != null ? event.getEventEntity().getId() : null);
                safePutString(ps, 8, event.getEventEntity() != null ? event.getEventEntity().getEntityType().name() : null);
                safePutUUID(ps, 9, event.getMsgId());
                safePutString(ps, 10, event.getMsgType());
                safePutString(ps, 11, event.getDataType());
                safePutString(ps, 12, event.getRelationType());
                safePutString(ps, 13, event.getData());
                safePutString(ps, 14, event.getMetadata());
                safePutString(ps, 15, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    @NotNull
    private BatchPreparedStatementSetter getRuleChainEventSetter(@NotNull List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RuleChainDebugEvent event = (RuleChainDebugEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getMessage());
                safePutString(ps, 7, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    void safePutString(@NotNull PreparedStatement ps, int parameterIdx, @Nullable String value) throws SQLException {
        if (value != null) {
            ps.setString(parameterIdx, replaceNullChars(value));
        } else {
            ps.setNull(parameterIdx, Types.VARCHAR);
        }
    }

    void safePutUUID(@NotNull PreparedStatement ps, int parameterIdx, @Nullable UUID value) throws SQLException {
        if (value != null) {
            ps.setObject(parameterIdx, value);
        } else {
            ps.setNull(parameterIdx, Types.OTHER);
        }
    }

    private void setCommonEventFields(@NotNull PreparedStatement ps, @NotNull Event event) throws SQLException {
        ps.setObject(1, event.getId().getId());
        ps.setObject(2, event.getTenantId().getId());
        ps.setLong(3, event.getCreatedTime());
        ps.setObject(4, event.getEntityId());
        ps.setString(5, event.getServiceId());
    }

    @Nullable
    private String replaceNullChars(@Nullable String strValue) {
        if (removeNullChars && strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }
}
