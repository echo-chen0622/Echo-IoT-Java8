package org.echoiot.server.dao.util.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.jetbrains.annotations.NotNull;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class JsonBinarySqlTypeDescriptor extends AbstractJsonSqlTypeDescriptor {

    public static final JsonBinarySqlTypeDescriptor INSTANCE = new JsonBinarySqlTypeDescriptor();

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @NotNull
    @Override
    public <X> ValueBinder<X> getBinder(@NotNull final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicBinder<X>(javaTypeDescriptor, this) {
            @Override
            protected void doBind(@NotNull PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
                st.setObject(index, javaTypeDescriptor.unwrap(value, JsonNode.class, options), getSqlType());
            }

            @Override
            protected void doBind(@NotNull CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
                st.setObject(name, javaTypeDescriptor.unwrap(value, JsonNode.class, options), getSqlType());
            }
        };
    }
}
