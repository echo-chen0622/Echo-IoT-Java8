package org.echoiot.server.service.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class ValidationResult<V> {

    @NotNull
    private final ValidationResultCode resultCode;
    @NotNull
    private final String message;
    @NotNull
    private final V v;

    @NotNull
    public static <V> ValidationResult<V> ok(V v) {
        return new ValidationResult<>(ValidationResultCode.OK, "Ok", v);
    }

    @NotNull
    public static <V> ValidationResult<V> accessDenied(String message) {
        return new ValidationResult<>(ValidationResultCode.ACCESS_DENIED, message, null);
    }

    @NotNull
    public static <V> ValidationResult<V> entityNotFound(String message) {
        return new ValidationResult<>(ValidationResultCode.ENTITY_NOT_FOUND, message, null);
    }

    @NotNull
    public static <V> ValidationResult<V> unauthorized(String message) {
        return new ValidationResult<>(ValidationResultCode.UNAUTHORIZED, message, null);
    }

    @NotNull
    public static <V> ValidationResult<V> internalError(String message) {
        return new ValidationResult<>(ValidationResultCode.INTERNAL_ERROR, message, null);
    }

}
