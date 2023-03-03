package org.echoiot.server.dao.service;

import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.validation.Length;
import org.echoiot.server.common.data.validation.NoXss;
import org.echoiot.server.dao.exception.DataValidationException;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;

import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 校验器，底层运用 Hibernate 的校验器进行校验
 *
 * @author Echo
 */
@Slf4j
public class ConstraintValidator {

    private static Validator fieldsValidator;

    static {
        initializeValidators();
    }

    public static void validateFields(Object data) {
        validateFields(data, "数据验证错误: ");
    }

    /**
     * 校验数据
     */
    public static void validateFields(Object data, String errorPrefix) {
        // 执行校验，获取校验错误信息
        List<String> constraintsViolations = getConstraintsViolations(data);
        // 如果校验错误信息不为空，则抛出异常
        if (!constraintsViolations.isEmpty()) {
            throw new DataValidationException(errorPrefix + String.join(", ", constraintsViolations));
        }
    }

    /**
     * 执行校验，获取校验错误信息
     */
    public static List<String> getConstraintsViolations(Object data) {
        return fieldsValidator.validate(data).stream().map(constraintViolation -> {
            String property;
            // 如果校验器中有 fieldName 属性，则使用 fieldName 作为属性名
            if (constraintViolation.getConstraintDescriptor().getAttributes().containsKey("fieldName")) {
                property = constraintViolation.getConstraintDescriptor().getAttributes().get("fieldName").toString();
            } else {
                // 否则使用属性路径的最后一个节点作为属性名
                Path propertyPath = constraintViolation.getPropertyPath();
                property = Iterators.getLast(propertyPath.iterator()).toString();
            }
            // 返回属性名 + 错误信息
            return property + " " + constraintViolation.getMessage();
        }).distinct().collect(Collectors.toList());
    }

    /**
     * 初始化校验器
     */
    private static void initializeValidators() {
        // 初始化 Hibernate 校验器
        HibernateValidatorConfiguration validatorConfiguration = Validation.byProvider(HibernateValidator.class).configure();

        // 添加自定义校验器
        ConstraintMapping constraintMapping = validatorConfiguration.createConstraintMapping();
        // 添加 NoXss 校验器
        constraintMapping.constraintDefinition(NoXss.class).validatedBy(NoXssValidator.class);
        // 添加 Length 校验器
        constraintMapping.constraintDefinition(Length.class).validatedBy(StringLengthValidator.class);
        validatorConfiguration.addMapping(constraintMapping);

        // 构建校验器
        fieldsValidator = validatorConfiguration.buildValidatorFactory().getValidator();
    }

}
