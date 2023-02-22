package org.echoiot.server.service.install.migrate;

import com.datastax.oss.driver.api.core.cql.Row;
import lombok.Data;
import org.echoiot.server.common.data.UUIDConverter;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

@Data
public class CassandraToSqlColumn {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));
    private static final String EMPTY_STR = "";

    private int index;
    private int sqlIndex;
    private String cassandraColumnName;
    private String sqlColumnName;
    private CassandraToSqlColumnType type;
    private int sqlType;
    private int size;
    private Class<? extends Enum> enumClass;
    private boolean allowNullBoolean = false;

    public static CassandraToSqlColumn idColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.ID);
    }

    public static CassandraToSqlColumn stringColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.STRING);
    }

    public static CassandraToSqlColumn stringColumn(String cassandraColumnName, String sqlColumnName) {
        return new CassandraToSqlColumn(cassandraColumnName, sqlColumnName);
    }

    public static CassandraToSqlColumn bigintColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.BIGINT);
    }

    public static CassandraToSqlColumn doubleColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.DOUBLE);
    }

    public static CassandraToSqlColumn booleanColumn(String name) {
        return booleanColumn(name, false);
    }

    public static CassandraToSqlColumn booleanColumn(String name, boolean allowNullBoolean) {
        return new CassandraToSqlColumn(name, name, CassandraToSqlColumnType.BOOLEAN, null, allowNullBoolean);
    }

    public static CassandraToSqlColumn jsonColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.JSON);
    }

    public static CassandraToSqlColumn enumToIntColumn(String name, Class<? extends Enum> enumClass) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.ENUM_TO_INT, enumClass);
    }

    public CassandraToSqlColumn(String columnName) {
        this(columnName, columnName, CassandraToSqlColumnType.STRING, null, false);
    }

    public CassandraToSqlColumn(String columnName, CassandraToSqlColumnType type) {
        this(columnName, columnName, type, null, false);
    }

    public CassandraToSqlColumn(String columnName, CassandraToSqlColumnType type, Class<? extends Enum> enumClass) {
        this(columnName, columnName, type, enumClass, false);
    }

    public CassandraToSqlColumn(String cassandraColumnName, String sqlColumnName) {
        this(cassandraColumnName, sqlColumnName, CassandraToSqlColumnType.STRING, null, false);
    }

    public CassandraToSqlColumn(String cassandraColumnName, String sqlColumnName, CassandraToSqlColumnType type,
                                Class<? extends Enum> enumClass, boolean allowNullBoolean) {
        this.cassandraColumnName = cassandraColumnName;
        this.sqlColumnName = sqlColumnName;
        this.type = type;
        this.enumClass = enumClass;
        this.allowNullBoolean = allowNullBoolean;
    }

    @Nullable
    public String getColumnValue(Row row) {
        if (row.isNull(index)) {
            if (this.type == CassandraToSqlColumnType.BOOLEAN && !this.allowNullBoolean) {
                return Boolean.toString(false);
            } else {
                return null;
            }
        } else {
            switch (this.type) {
                case ID:
                    return UUIDConverter.fromTimeUUID(row.getUuid(index));
                case DOUBLE:
                    return Double.toString(row.getDouble(index));
                case INTEGER:
                    return Integer.toString(row.getInt(index));
                case FLOAT:
                    return Float.toString(row.getFloat(index));
                case BIGINT:
                    return Long.toString(row.getLong(index));
                case BOOLEAN:
                    return Boolean.toString(row.getBoolean(index));
                case STRING:
                case JSON:
                case ENUM_TO_INT:
                default:
                    @Nullable String value = row.getString(index);
                    return this.replaceNullChars(value);
            }
        }
    }

    public void setColumnValue(PreparedStatement sqlInsertStatement, @Nullable String value) throws SQLException {
        if (value == null) {
            sqlInsertStatement.setNull(this.sqlIndex, this.sqlType);
        } else {
            switch (this.type) {
                case DOUBLE:
                    sqlInsertStatement.setDouble(this.sqlIndex, Double.parseDouble(value));
                    break;
                case INTEGER:
                    sqlInsertStatement.setInt(this.sqlIndex, Integer.parseInt(value));
                    break;
                case FLOAT:
                    sqlInsertStatement.setFloat(this.sqlIndex, Float.parseFloat(value));
                    break;
                case BIGINT:
                    sqlInsertStatement.setLong(this.sqlIndex, Long.parseLong(value));
                    break;
                case BOOLEAN:
                    sqlInsertStatement.setBoolean(this.sqlIndex, Boolean.parseBoolean(value));
                    break;
                case ENUM_TO_INT:
                    @SuppressWarnings("unchecked")
                    Enum<?> enumVal = Enum.valueOf(this.enumClass, value);
                    int intValue = enumVal.ordinal();
                    sqlInsertStatement.setInt(this.sqlIndex, intValue);
                    break;
                case JSON:
                case STRING:
                case ID:
                default:
                    sqlInsertStatement.setString(this.sqlIndex, value);
                    break;
            }
        }
    }

    private String replaceNullChars(@Nullable String strValue) {
        if (strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }

}
