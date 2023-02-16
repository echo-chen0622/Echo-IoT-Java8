package org.echoiot.server.controller;

import com.google.common.util.concurrent.FutureCallback;
import org.echoiot.server.service.security.ValidationCallback;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Created by Echo on 21.02.17.
 */
public class HttpValidationCallback extends ValidationCallback<DeferredResult<ResponseEntity>> {

    public HttpValidationCallback(DeferredResult<ResponseEntity> response, FutureCallback<DeferredResult<ResponseEntity>> action) {
       super(response, action);
    }

}
