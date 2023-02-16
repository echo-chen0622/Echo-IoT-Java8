package org.echoiot.server.dao.service;

import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.dao.exception.DataValidationException;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.hibernate.validator.cfg.ConstraintMapping;
import org.echoiot.server.common.data.validation.Length;
import org.echoiot.server.common.data.validation.NoXss;

import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ConstraintValidator {

    private static Validator fieldsValidator;

    static {
        initializeValidators();
    }

    public static void validateFields(Object data) {
        validateFields(data, "Validation error: ");
    }

    public static void validateFields(Object data, String errorPrefix) {
        List<String> constraintsViolations = getConstraintsViolations(data);
        if (!constraintsViolations.isEmpty()) {
            throw new DataValidationException(errorPrefix + String.join(", ", constraintsViolations));
        }
    }

    public static List<String> getConstraintsViolations(Object data) {
        return fieldsValidator.validate(data).stream()
                .map(constraintViolation -> {
                    String property;
                    if (constraintViolation.getConstraintDescriptor().getAttributes().containsKey("fieldName")) {
                        property = constraintViolation.getConstraintDescriptor().getAttributes().get("fieldName").toString();
                    } else {
                        Path propertyPath = constraintViolation.getPropertyPath();
                        property = Iterators.getLast(propertyPath.iterator()).toString();
                    }
                    return property + " " + constraintViolation.getMessage();
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private static void initializeValidators() {
        HibernateValidatorConfiguration validatorConfiguration = Validation.byProvider(HibernateValidator.class).configure();

        ConstraintMapping constraintMapping = validatorConfiguration.createConstraintMapping();
        constraintMapping.constraintDefinition(NoXss.class).validatedBy(NoXssValidator.class);
        constraintMapping.constraintDefinition(Length.class).validatedBy(StringLengthValidator.class);
        validatorConfiguration.addMapping(constraintMapping);

        fieldsValidator = validatorConfiguration.buildValidatorFactory().getValidator();
    }

}
