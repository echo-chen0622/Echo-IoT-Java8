package org.echoiot.server.config;

import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.msg.tools.TbRateLimits;
import org.echoiot.server.common.msg.tools.TbRateLimitsException;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.exception.EchoiotErrorResponseHandler;
import org.echoiot.server.service.security.model.SecurityUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class RateLimitProcessingFilter extends OncePerRequestFilter {

    @Resource
    private EchoiotErrorResponseHandler errorResponseHandler;

    @Resource
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    private final ConcurrentMap<TenantId, TbRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private final ConcurrentMap<CustomerId, TbRateLimits> perCustomerLimits = new ConcurrentHashMap<>();

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        @Nullable SecurityUser user = getCurrentUser();
        if (user != null && !user.isSystemAdmin()) {
            @Nullable var profile = tenantProfileCache.get(user.getTenantId());
            if (profile == null) {
                log.debug("[{}] Failed to lookup tenant profile", user.getTenantId());
                errorResponseHandler.handle(new BadCredentialsException("Failed to lookup tenant profile"), response);
                return;
            }
            @Nullable var profileConfiguration = profile.getDefaultProfileConfiguration();
            if (!checkRateLimits(user.getTenantId(), profileConfiguration.getTenantServerRestLimitsConfiguration(), perTenantLimits, response)) {
                return;
            }
            if (user.isCustomerUser()) {
                if (!checkRateLimits(user.getCustomerId(), profileConfiguration.getCustomerServerRestLimitsConfiguration(), perCustomerLimits, response)) {
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private <I extends EntityId> boolean checkRateLimits(@NotNull I ownerId, @NotNull String rateLimitConfig, @NotNull Map<I, TbRateLimits> rateLimitsMap, ServletResponse response) {
        if (StringUtils.isNotEmpty(rateLimitConfig)) {
            TbRateLimits rateLimits = rateLimitsMap.get(ownerId);
            if (rateLimits == null || !rateLimits.getConfiguration().equals(rateLimitConfig)) {
                rateLimits = new TbRateLimits(rateLimitConfig);
                rateLimitsMap.put(ownerId, rateLimits);
            }

            if (!rateLimits.tryConsume()) {
                errorResponseHandler.handle(new TbRateLimitsException(ownerId.getEntityType()), (HttpServletResponse) response);
                return false;
            }
        } else {
            rateLimitsMap.remove(ownerId);
        }

        return true;
    }

    @Nullable
    protected SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            return null;
        }
    }

}
