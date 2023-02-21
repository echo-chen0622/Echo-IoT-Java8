package org.echoiot.server.service.security.auth.rest;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

public class RestAuthenticationDetailsSource implements
        AuthenticationDetailsSource<HttpServletRequest, RestAuthenticationDetails> {

    @NotNull
    public RestAuthenticationDetails buildDetails(@NotNull HttpServletRequest context) {
        return new RestAuthenticationDetails(context);
    }
}
