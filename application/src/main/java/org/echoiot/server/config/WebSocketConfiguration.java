package org.echoiot.server.config;

import org.echoiot.server.common.data.exception.EchoiotErrorCode;
import org.echoiot.server.common.data.exception.EchoiotException;
import org.echoiot.server.controller.plugin.TbWebSocketHandler;
import org.echoiot.server.queue.util.TbCoreComponent;
import org.echoiot.server.service.security.model.SecurityUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

@Configuration
@TbCoreComponent
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    public static final String WS_PLUGIN_PREFIX = "/api/ws/plugins/";
    private static final String WS_PLUGIN_MAPPING = WS_PLUGIN_PREFIX + "**";

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(32768);
        container.setMaxBinaryMessageBufferSize(32768);
        return container;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(wsHandler(), WS_PLUGIN_MAPPING).setAllowedOriginPatterns("*")
                .addInterceptors(new HttpSessionHandshakeInterceptor(), new HandshakeInterceptor() {

                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                                   Map<String, Object> attributes) throws Exception {
                        SecurityUser user = null;
                        try {
                            user = getCurrentUser();
                        } catch (EchoiotException ex) {
                        }
                        if (user == null) {
                            response.setStatusCode(HttpStatus.UNAUTHORIZED);
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                               Exception exception) {
                        //Do nothing
                    }
                });
    }

    @Bean
    public WebSocketHandler wsHandler() {
        return new TbWebSocketHandler();
    }

    protected SecurityUser getCurrentUser() throws EchoiotException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            throw new EchoiotException("You aren't authorized to perform this operation!", EchoiotErrorCode.AUTHENTICATION);
        }
    }
}
