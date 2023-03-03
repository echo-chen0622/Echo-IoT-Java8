package org.echoiot.server.common.data.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * NoXss注解，用于校验用户输入的字符串是否包含XSS攻击的字符
 *
 * @author Echo
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Constraint(validatedBy = {})
public @interface NoXss {
    String message() default "格式不正确";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
