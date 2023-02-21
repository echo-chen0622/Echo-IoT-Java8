package org.echoiot.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.validation.NoXss;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Optional;

@Slf4j
public class NoXssValidator implements ConstraintValidator<NoXss, Object> {
    private static final AntiSamy xssChecker = new AntiSamy();
    private static Policy xssPolicy;

    @Override
    public void initialize(NoXss constraintAnnotation) {
        if (xssPolicy == null) {
            xssPolicy = Optional.ofNullable(getClass().getClassLoader().getResourceAsStream("xss-policy.xml"))
                    .map(inputStream -> {
                        try {
                            return Policy.getInstance(inputStream);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .orElseThrow(() -> new IllegalStateException("XSS policy file not found"));
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        String stringValue;
        if (value instanceof CharSequence || value instanceof JsonNode) {
            stringValue = value.toString();
        } else {
            return true;
        }
        if (stringValue.isEmpty()) {
            return true;
        }

        try {
            return xssChecker.scan(stringValue, xssPolicy).getNumberOfErrors() == 0;
        } catch (ScanException | PolicyException e) {
            return false;
        }
    }
}
