package org.echoiot.server.service.security.auth.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.echoiot.server.cluster.TbClusterService;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.DashboardId;
import org.echoiot.server.common.data.id.IdBased;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.oauth2.OAuth2MapperConfig;
import org.echoiot.server.common.data.oauth2.OAuth2Registration;
import org.echoiot.server.common.data.page.PageData;
import org.echoiot.server.common.data.page.PageLink;
import org.echoiot.server.common.data.plugin.ComponentLifecycleEvent;
import org.echoiot.server.common.data.security.Authority;
import org.echoiot.server.common.data.security.UserCredentials;
import org.echoiot.server.dao.customer.CustomerService;
import org.echoiot.server.dao.dashboard.DashboardService;
import org.echoiot.server.dao.oauth2.OAuth2User;
import org.echoiot.server.dao.tenant.TbTenantProfileCache;
import org.echoiot.server.dao.tenant.TenantService;
import org.echoiot.server.dao.user.UserService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.echoiot.server.common.data.Customer;
import org.echoiot.server.common.data.DashboardInfo;
import org.echoiot.server.common.data.StringUtils;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.User;
import org.echoiot.server.service.entitiy.user.TbUserService;
import org.echoiot.server.service.install.InstallScripts;
import org.echoiot.server.service.security.model.SecurityUser;
import org.echoiot.server.service.security.model.UserPrincipal;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class AbstractOAuth2ClientMapper {
    private static final int DASHBOARDS_REQUEST_LIMIT = 10;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private UserService userService;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private TenantService tenantService;

    @Resource
    private CustomerService customerService;

    @Resource
    private DashboardService dashboardService;

    @Resource
    private InstallScripts installScripts;

    @Resource
    private TbUserService tbUserService;

    @Resource
    protected TbTenantProfileCache tenantProfileCache;

    @Resource
    protected TbClusterService tbClusterService;

    @Value("${edges.enabled}")
    @Getter
    private boolean edgesEnabled;

    private final Lock userCreationLock = new ReentrantLock();

    protected SecurityUser getOrCreateSecurityUserFromOAuth2User(@NotNull OAuth2User oauth2User, @NotNull OAuth2Registration registration) {

        OAuth2MapperConfig config = registration.getMapperConfig();

        @NotNull UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, oauth2User.getEmail());

        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, oauth2User.getEmail());

        if (user == null && !config.isAllowUserCreation()) {
            throw new UsernameNotFoundException("User not found: " + oauth2User.getEmail());
        }

        if (user == null) {
            userCreationLock.lock();
            try {
                user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, oauth2User.getEmail());
                if (user == null) {
                    user = new User();
                    if (oauth2User.getCustomerId() == null && StringUtils.isEmpty(oauth2User.getCustomerName())) {
                        user.setAuthority(Authority.TENANT_ADMIN);
                    } else {
                        user.setAuthority(Authority.CUSTOMER_USER);
                    }
                    TenantId tenantId = oauth2User.getTenantId() != null ?
                            oauth2User.getTenantId() : getTenantId(oauth2User.getTenantName());
                    user.setTenantId(tenantId);
                    @Nullable CustomerId customerId = oauth2User.getCustomerId() != null ?
                            oauth2User.getCustomerId() : getCustomerId(user.getTenantId(), oauth2User.getCustomerName());
                    user.setCustomerId(customerId);
                    user.setEmail(oauth2User.getEmail());
                    user.setFirstName(oauth2User.getFirstName());
                    user.setLastName(oauth2User.getLastName());

                    ObjectNode additionalInfo = objectMapper.createObjectNode();

                    if (!StringUtils.isEmpty(oauth2User.getDefaultDashboardName())) {
                        @NotNull Optional<DashboardId> dashboardIdOpt =
                                user.getAuthority() == Authority.TENANT_ADMIN ?
                                        getDashboardId(tenantId, oauth2User.getDefaultDashboardName())
                                        : getDashboardId(tenantId, customerId, oauth2User.getDefaultDashboardName());
                        if (dashboardIdOpt.isPresent()) {
                            additionalInfo.put("defaultDashboardFullscreen", oauth2User.isAlwaysFullScreen());
                            additionalInfo.put("defaultDashboardId", dashboardIdOpt.get().getId().toString());
                        }
                    }

                    if (registration.getAdditionalInfo() != null &&
                            registration.getAdditionalInfo().has("providerName")) {
                        additionalInfo.put("authProviderName", registration.getAdditionalInfo().get("providerName").asText());
                    }

                    user.setAdditionalInfo(additionalInfo);

                    user = tbUserService.save(tenantId, customerId, user, false, null, null);
                    if (config.isActivateUser()) {
                        UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
                        userService.activateUserCredentials(user.getTenantId(), userCredentials.getActivateToken(), passwordEncoder.encode(""));
                    }
                }
            } catch (Exception e) {
                log.error("Can't get or create security user from oauth2 user", e);
                throw new RuntimeException("Can't get or create security user from oauth2 user", e);
            } finally {
                userCreationLock.unlock();
            }
        }

        try {
            @NotNull SecurityUser securityUser = new SecurityUser(user, true, principal);
            return (SecurityUser) new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()).getPrincipal();
        } catch (Exception e) {
            log.error("Can't get or create security user from oauth2 user", e);
            throw new RuntimeException("Can't get or create security user from oauth2 user", e);
        }
    }

    private TenantId getTenantId(String tenantName) throws IOException {
        List<Tenant> tenants = tenantService.findTenants(new PageLink(1, 0, tenantName)).getData();
        Tenant tenant;
        if (tenants == null || tenants.isEmpty()) {
            tenant = new Tenant();
            tenant.setTitle(tenantName);
            tenant = tenantService.saveTenant(tenant);
            installScripts.createDefaultRuleChains(tenant.getId());
            installScripts.createDefaultEdgeRuleChains(tenant.getId());
            tenantProfileCache.evict(tenant.getId());
            tbClusterService.onTenantChange(tenant, null);
            tbClusterService.broadcastEntityStateChangeEvent(tenant.getId(), tenant.getId(),
                                                             ComponentLifecycleEvent.CREATED);
        } else {
            tenant = tenants.get(0);
        }
        return tenant.getTenantId();
    }

    @Nullable
    private CustomerId getCustomerId(TenantId tenantId, String customerName) {
        if (StringUtils.isEmpty(customerName)) {
            return null;
        }
        Optional<Customer> customerOpt = customerService.findCustomerByTenantIdAndTitle(tenantId, customerName);
        if (customerOpt.isPresent()) {
            return customerOpt.get().getId();
        } else {
            @NotNull Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setTitle(customerName);
            return customerService.saveCustomer(customer).getId();
        }
    }

    @NotNull
    private Optional<DashboardId> getDashboardId(TenantId tenantId, String dashboardName) {
        return Optional.ofNullable(dashboardService.findFirstDashboardInfoByTenantIdAndName(tenantId, dashboardName)).map(IdBased::getId);
    }

    @NotNull
    private Optional<DashboardId> getDashboardId(TenantId tenantId, CustomerId customerId, @NotNull String dashboardName) {
        PageData<DashboardInfo> dashboardsPage;
        @Nullable PageLink pageLink = null;
        do {
            pageLink = pageLink == null ? new PageLink(DASHBOARDS_REQUEST_LIMIT) : pageLink.nextPageLink();
            dashboardsPage = dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            @NotNull Optional<DashboardInfo> dashboardInfoOpt = dashboardsPage.getData().stream()
                                                                              .filter(dashboardInfo -> dashboardName.equals(dashboardInfo.getName()))
                                                                              .findAny();
            if (dashboardInfoOpt.isPresent()) {
                return dashboardInfoOpt.map(DashboardInfo::getId);
            }
        } while (dashboardsPage.hasNext());
        return Optional.empty();
    }
}
