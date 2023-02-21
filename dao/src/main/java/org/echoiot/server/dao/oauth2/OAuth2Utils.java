package org.echoiot.server.dao.oauth2;

import org.echoiot.server.common.data.BaseData;
import org.echoiot.server.common.data.id.OAuth2ParamsId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.oauth2.*;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class OAuth2Utils {
    public static final String OAUTH2_AUTHORIZATION_PATH_TEMPLATE = "/oauth2/authorization/%s";

    @NotNull
    public static OAuth2ClientInfo toClientInfo(@NotNull OAuth2Registration registration) {
        @NotNull OAuth2ClientInfo client = new OAuth2ClientInfo();
        client.setName(registration.getLoginButtonLabel());
        client.setUrl(String.format(OAUTH2_AUTHORIZATION_PATH_TEMPLATE, registration.getUuidId().toString()));
        client.setIcon(registration.getLoginButtonIcon());
        return client;
    }

    @NotNull
    public static OAuth2ParamsInfo toOAuth2ParamsInfo(@NotNull List<OAuth2Registration> registrations, @NotNull List<OAuth2Domain> domains, @NotNull List<OAuth2Mobile> mobiles) {
        @NotNull OAuth2ParamsInfo oauth2ParamsInfo = new OAuth2ParamsInfo();
        oauth2ParamsInfo.setClientRegistrations(registrations.stream().sorted(Comparator.comparing(BaseData::getUuidId)).map(OAuth2Utils::toOAuth2RegistrationInfo).collect(Collectors.toList()));
        oauth2ParamsInfo.setDomainInfos(domains.stream().sorted(Comparator.comparing(BaseData::getUuidId)).map(OAuth2Utils::toOAuth2DomainInfo).collect(Collectors.toList()));
        oauth2ParamsInfo.setMobileInfos(mobiles.stream().sorted(Comparator.comparing(BaseData::getUuidId)).map(OAuth2Utils::toOAuth2MobileInfo).collect(Collectors.toList()));
        return oauth2ParamsInfo;
    }

    public static OAuth2RegistrationInfo toOAuth2RegistrationInfo(@NotNull OAuth2Registration registration) {
        return OAuth2RegistrationInfo.builder()
                .mapperConfig(registration.getMapperConfig())
                .clientId(registration.getClientId())
                .clientSecret(registration.getClientSecret())
                .authorizationUri(registration.getAuthorizationUri())
                .accessTokenUri(registration.getAccessTokenUri())
                .scope(registration.getScope())
                .platforms(registration.getPlatforms())
                .userInfoUri(registration.getUserInfoUri())
                .userNameAttributeName(registration.getUserNameAttributeName())
                .jwkSetUri(registration.getJwkSetUri())
                .clientAuthenticationMethod(registration.getClientAuthenticationMethod())
                .loginButtonLabel(registration.getLoginButtonLabel())
                .loginButtonIcon(registration.getLoginButtonIcon())
                .additionalInfo(registration.getAdditionalInfo())
                .build();
    }

    public static OAuth2DomainInfo toOAuth2DomainInfo(@NotNull OAuth2Domain domain) {
        return OAuth2DomainInfo.builder()
                .name(domain.getDomainName())
                .scheme(domain.getDomainScheme())
                .build();
    }

    public static OAuth2MobileInfo toOAuth2MobileInfo(@NotNull OAuth2Mobile mobile) {
        return OAuth2MobileInfo.builder()
                .pkgName(mobile.getPkgName())
                .appSecret(mobile.getAppSecret())
                .build();
    }

    @NotNull
    public static OAuth2Params infoToOAuth2Params(@NotNull OAuth2Info oauth2Info) {
        @NotNull OAuth2Params oauth2Params = new OAuth2Params();
        oauth2Params.setEnabled(oauth2Info.isEnabled());
        oauth2Params.setTenantId(TenantId.SYS_TENANT_ID);
        return oauth2Params;
    }

    @NotNull
    public static OAuth2Registration toOAuth2Registration(OAuth2ParamsId oauth2ParamsId, @NotNull OAuth2RegistrationInfo registrationInfo) {
        @NotNull OAuth2Registration registration = new OAuth2Registration();
        registration.setOauth2ParamsId(oauth2ParamsId);
        registration.setMapperConfig(registrationInfo.getMapperConfig());
        registration.setClientId(registrationInfo.getClientId());
        registration.setClientSecret(registrationInfo.getClientSecret());
        registration.setAuthorizationUri(registrationInfo.getAuthorizationUri());
        registration.setAccessTokenUri(registrationInfo.getAccessTokenUri());
        registration.setScope(registrationInfo.getScope());
        registration.setPlatforms(registrationInfo.getPlatforms());
        registration.setUserInfoUri(registrationInfo.getUserInfoUri());
        registration.setUserNameAttributeName(registrationInfo.getUserNameAttributeName());
        registration.setJwkSetUri(registrationInfo.getJwkSetUri());
        registration.setClientAuthenticationMethod(registrationInfo.getClientAuthenticationMethod());
        registration.setLoginButtonLabel(registrationInfo.getLoginButtonLabel());
        registration.setLoginButtonIcon(registrationInfo.getLoginButtonIcon());
        registration.setAdditionalInfo(registrationInfo.getAdditionalInfo());
        return registration;
    }

    @NotNull
    public static OAuth2Domain toOAuth2Domain(OAuth2ParamsId oauth2ParamsId, @NotNull OAuth2DomainInfo domainInfo) {
        @NotNull OAuth2Domain domain = new OAuth2Domain();
        domain.setOauth2ParamsId(oauth2ParamsId);
        domain.setDomainName(domainInfo.getName());
        domain.setDomainScheme(domainInfo.getScheme());
        return domain;
    }

    @NotNull
    public static OAuth2Mobile toOAuth2Mobile(OAuth2ParamsId oauth2ParamsId, @NotNull OAuth2MobileInfo mobileInfo) {
        @NotNull OAuth2Mobile mobile = new OAuth2Mobile();
        mobile.setOauth2ParamsId(oauth2ParamsId);
        mobile.setPkgName(mobileInfo.getPkgName());
        mobile.setAppSecret(mobileInfo.getAppSecret());
        return mobile;
    }
}
