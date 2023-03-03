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

/**
 * Xss 校验器, 用于校验字符串是否包含 XSS 攻击的内容
 */
@Slf4j
public class NoXssValidator implements ConstraintValidator<NoXss, Object> {
    /**
     * 扫描器
     */
    private static final AntiSamy xssChecker = new AntiSamy();
    /**
     * 校验规则
     */
    private static Policy xssPolicy;


    /**
     * 初始化校验器
     *
     * @param constraintAnnotation
     */
    @Override
    public void initialize(NoXss constraintAnnotation) {
        if (xssPolicy == null) {
            // 加载校验策略
            xssPolicy = Optional.ofNullable(getClass().getClassLoader().getResourceAsStream("xss-policy.xml")).map(inputStream -> {
                try {
                    // 加载校验规则
                    return Policy.getInstance(inputStream);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).orElseThrow(() -> new IllegalStateException("找不到 XSS 校验规则文件"));
        }
    }

    /**
     * 校验字符串是否包含 XSS 攻击的内容
     *
     * @param value                      待校验的字符串
     * @param constraintValidatorContext 校验上下文
     *
     * @return true: 不包含 XSS 攻击的内容, false: 包含 XSS 攻击的内容
     */
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
            // 扫描字符串是否包含 XSS 攻击的内容
            return xssChecker.scan(stringValue, xssPolicy).getNumberOfErrors() == 0;
        } catch (ScanException | PolicyException e) {
            // 如果扫描出现异常, 则认为字符串包含 XSS 攻击的内容
            return false;
        }
    }
}
