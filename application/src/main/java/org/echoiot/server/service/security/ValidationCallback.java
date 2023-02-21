package org.echoiot.server.service.security;

import com.google.common.util.concurrent.FutureCallback;
import org.echoiot.server.service.telemetry.exception.AccessDeniedException;
import org.echoiot.server.service.telemetry.exception.EntityNotFoundException;
import org.echoiot.server.service.telemetry.exception.InternalErrorException;
import org.echoiot.server.service.telemetry.exception.UnauthorizedException;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Echo on 31.03.18.
 */
public class ValidationCallback<T> implements FutureCallback<ValidationResult> {

    private final T response;
    private final FutureCallback<T> action;

    public ValidationCallback(T response, FutureCallback<T> action) {
        this.response = response;
        this.action = action;
    }

    @Override
    public void onSuccess(@NotNull ValidationResult result) {
        if (result.getResultCode() == ValidationResultCode.OK) {
            action.onSuccess(response);
        } else {
            onFailure(getException(result));
        }
    }

    @Override
    public void onFailure(Throwable e) {
        action.onFailure(e);
    }

    @NotNull
    public static Exception getException(@NotNull ValidationResult result) {
        ValidationResultCode resultCode = result.getResultCode();
        Exception e;
        switch (resultCode) {
            case ENTITY_NOT_FOUND:
                e = new EntityNotFoundException(result.getMessage());
                break;
            case UNAUTHORIZED:
                e = new UnauthorizedException(result.getMessage());
                break;
            case ACCESS_DENIED:
                e = new AccessDeniedException(result.getMessage());
                break;
            case INTERNAL_ERROR:
                e = new InternalErrorException(result.getMessage());
                break;
            default:
                e = new UnauthorizedException("Permission denied.");
                break;
        }
        return e;
    }

}
