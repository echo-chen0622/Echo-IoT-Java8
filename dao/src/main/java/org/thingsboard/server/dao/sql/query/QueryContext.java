package org.thingsboard.server.dao.sql.query;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.type.PostgresUUIDType;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class QueryContext implements SqlParameterSource {
    private static final PostgresUUIDType UUID_TYPE = new PostgresUUIDType();

    private final QuerySecurityContext securityCtx;
    private final StringBuilder query;
    private final Map<String, Parameter> params;

    public QueryContext(QuerySecurityContext securityCtx) {
        this.securityCtx = securityCtx;
        query = new StringBuilder();
        params = new HashMap<>();
    }

    void addParameter(String name, Object value, int type, String typeName) {
        Parameter newParam = new Parameter(value, type, typeName);
        Parameter oldParam = params.put(name, newParam);
        if (oldParam != null && oldParam.value != null && !oldParam.value.equals(newParam.value)) {
            throw new RuntimeException("Parameter with name: " + name + " was already registered!");
        }
        if(value == null){
            log.warn("[{}][{}][{}] Trying to set null value", getTenantId(), getCustomerId(), name);
        }
    }

    public void append(String s) {
        query.append(s);
    }

    @Override
    public boolean hasValue(String paramName) {
        return params.containsKey(paramName);
    }

    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
        return checkParameter(paramName).value;
    }

    @Override
    public int getSqlType(String paramName) {
        return checkParameter(paramName).type;
    }

    private Parameter checkParameter(String paramName) {
        Parameter param = params.get(paramName);
        if (param == null) {
            throw new RuntimeException("Parameter with name: " + paramName + " is not set!");
        }
        return param;
    }

    @Override
    public String getTypeName(String paramName) {
        return params.get(paramName).name;
    }

    @Override
    public String[] getParameterNames() {
        return params.keySet().toArray(new String[]{});
    }

    public void addUuidParameter(String name, UUID value) {
        addParameter(name, value, UUID_TYPE.sqlType(), UUID_TYPE.getName());
    }

    public void addStringParameter(String name, String value) {
        addParameter(name, value, Types.VARCHAR, "VARCHAR");
    }

    public void addDoubleParameter(String name, double value) {
        addParameter(name, value, Types.DOUBLE, "DOUBLE");
    }

    public void addLongParameter(String name, long value) {
        addParameter(name, value, Types.BIGINT, "BIGINT");
    }

    public void addStringListParameter(String name, List<String> value) {
        addParameter(name, value, Types.VARCHAR, "VARCHAR");
    }

    public void addBooleanParameter(String name, boolean value) {
        addParameter(name, value, Types.BOOLEAN, "BOOLEAN");
    }

    public void addUuidListParameter(String name, List<UUID> value) {
        addParameter(name, value, UUID_TYPE.sqlType(), UUID_TYPE.getName());
    }

    public String getQuery() {
        return query.toString();
    }


    public static class Parameter {
        private final Object value;
        private final int type;
        private final String name;

        public Parameter(Object value, int type, String name) {
            this.value = value;
            this.type = type;
            this.name = name;
        }
    }

    public TenantId getTenantId() {
        return securityCtx.getTenantId();
    }

    public CustomerId getCustomerId() {
        return securityCtx.getCustomerId();
    }

    public EntityType getEntityType() {
        return securityCtx.getEntityType();
    }

    public boolean isIgnorePermissionCheck() {
        return securityCtx.isIgnorePermissionCheck();
    }
}
