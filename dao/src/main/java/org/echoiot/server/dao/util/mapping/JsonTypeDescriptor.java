package org.echoiot.server.dao.util.mapping;

import org.echoiot.common.util.JacksonUtil;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.usertype.DynamicParameterizedType;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;

/**
 * Created by Valerii Sosliuk on 5/12/2017.
 */
public class JsonTypeDescriptor
        extends AbstractTypeDescriptor<Object>
        implements DynamicParameterizedType {

    private Class<?> jsonObjectClass;

    @Override
    public void setParameterValues(Properties parameters) {
        jsonObjectClass = ( (ParameterType) parameters.get( PARAMETER_TYPE ) )
                .getReturnedClass();

    }

    public JsonTypeDescriptor() {
        super( Object.class, new MutableMutabilityPlan<Object>() {
            @Override
            protected Object deepCopyNotNull(Object value) {
                return JacksonUtil.clone(value);
            }
        });
    }

    @Override
    public boolean areEqual(@Nullable Object one, @Nullable Object another) {
        if ( one == another ) {
            return true;
        }
        if ( one == null || another == null ) {
            return false;
        }
        return JacksonUtil.toJsonNode(JacksonUtil.toString(one)).equals(
                JacksonUtil.toJsonNode(JacksonUtil.toString(another)));
    }

    @Nullable
    @Override
    public String toString(Object value) {
        return JacksonUtil.toString(value);
    }

    @Nullable
    @Override
    public Object fromString(String string) {
        return JacksonUtil.fromString(string, jsonObjectClass);
    }

    @Nullable
    @SuppressWarnings({"unchecked" })
    @Override
    public <X> X unwrap(@Nullable Object value, Class<X> type, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( String.class.isAssignableFrom( type ) ) {
            return (X) toString(value);
        }
        if ( Object.class.isAssignableFrom( type ) ) {
            return (X) JacksonUtil.toJsonNode(toString(value));
        }
        throw unknownUnwrap( type );
    }

    @Nullable
    @Override
    public <X> Object wrap(@Nullable X value, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        return fromString(value.toString());
    }

}
