package org.echoiot.server.dao.util.mapping;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.jetbrains.annotations.NotNull;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JsonStringSqlTypeDescriptor
        extends AbstractJsonSqlTypeDescriptor {

    public static final JsonStringSqlTypeDescriptor INSTANCE =
            new JsonStringSqlTypeDescriptor();

    @NotNull
    @Override
    public <X> ValueBinder<X> getBinder(
            @NotNull final JavaTypeDescriptor<X> javaTypeDescriptor) {
        return new BasicBinder<X>(javaTypeDescriptor, this) {
            @Override
            protected void doBind(
                    @NotNull PreparedStatement st,
                    X value,
                    int index,
                    WrapperOptions options) throws SQLException {
                st.setString(index,
                        javaTypeDescriptor.unwrap(value, String.class, options)
                );
            }

            @Override
            protected void doBind(
                    @NotNull CallableStatement st,
                    X value,
                    String name,
                    WrapperOptions options)
                    throws SQLException {
                st.setString(name,
                        javaTypeDescriptor.unwrap(value, String.class, options
                        ));
            }
        };
    }
}
