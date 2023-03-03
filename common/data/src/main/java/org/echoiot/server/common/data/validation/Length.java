package org.echoiot.server.common.data.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 长度校验引擎
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Constraint(validatedBy = {})
public @interface Length {
    String message() default "长度必须等于或小于 {max}";

    String fieldName();

    int max() default 255;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
