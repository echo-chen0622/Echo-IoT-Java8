package org.thingsboard.server.common.data.audit;

import lombok.Getter;

@Getter
public enum ActionType {
    ADDED(false), // log entity
    DELETED(false), // log string id
    UPDATED(false), // log entity
    ATTRIBUTES_UPDATED(false), // log attributes/values
    ATTRIBUTES_DELETED(false), // log attributes
    TIMESERIES_UPDATED(false), // log timeseries update
    TIMESERIES_DELETED(false), // log timeseries
    RPC_CALL(false), // log method and params
    CREDENTIALS_UPDATED(false), // log new credentials
    ASSIGNED_TO_CUSTOMER(false), // log customer name
    UNASSIGNED_FROM_CUSTOMER(false), // log customer name
    ACTIVATED(false), // log string id
    SUSPENDED(false), // log string id
    CREDENTIALS_READ(true), // log device id
    ATTRIBUTES_READ(true), // log attributes
    RELATION_ADD_OR_UPDATE(false),
    RELATION_DELETED(false),
    RELATIONS_DELETED(false),
    ALARM_ACK(false),
    ALARM_CLEAR(false),
    ALARM_DELETE(false),
    LOGIN(false),
    LOGOUT(false),
    LOCKOUT(false),
    ASSIGNED_FROM_TENANT(false),
    ASSIGNED_TO_TENANT(false),
    PROVISION_SUCCESS(false),
    PROVISION_FAILURE(false),
    ASSIGNED_TO_EDGE(false), // log edge name
    UNASSIGNED_FROM_EDGE(false);

    private final boolean isRead;

    ActionType(boolean isRead) {
        this.isRead = isRead;
    }
}
