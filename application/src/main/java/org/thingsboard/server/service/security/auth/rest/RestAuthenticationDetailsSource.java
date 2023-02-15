package org.thingsboard.server.service.security.auth.rest;

import org.springframework.security.authentication.AuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

public class RestAuthenticationDetailsSource implements
        AuthenticationDetailsSource<HttpServletRequest, RestAuthenticationDetails> {

    public RestAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new RestAuthenticationDetails(context);
    }
}
