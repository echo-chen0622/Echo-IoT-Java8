package org.echoiot.server.dao.util.mapping;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

public class JsonBinaryType extends AbstractSingleColumnStandardBasicType<Object> implements DynamicParameterizedType {

    public JsonBinaryType() {
        super(
                JsonBinarySqlTypeDescriptor.INSTANCE,
                new JsonTypeDescriptor()
        );
    }

    @NotNull
    public String getName() {
        return "jsonb";
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }

    @Override
    public void setParameterValues(@NotNull Properties parameters) {
        ((JsonTypeDescriptor) getJavaTypeDescriptor())
                .setParameterValues(parameters);
    }
}
