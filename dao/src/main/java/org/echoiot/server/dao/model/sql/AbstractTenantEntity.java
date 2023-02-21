package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.echoiot.server.common.data.Tenant;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.id.TenantProfileId;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.model.SearchTextEntity;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractTenantEntity<T extends Tenant> extends BaseSqlEntity<T> implements SearchTextEntity<T> {

    @Column(name = ModelConstants.TENANT_TITLE_PROPERTY)
    private String title;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = ModelConstants.TENANT_REGION_PROPERTY)
    private String region;

    @Column(name = ModelConstants.COUNTRY_PROPERTY)
    private String country;

    @Column(name = ModelConstants.STATE_PROPERTY)
    private String state;

    @Column(name = ModelConstants.CITY_PROPERTY)
    private String city;

    @Column(name = ModelConstants.ADDRESS_PROPERTY)
    private String address;

    @Column(name = ModelConstants.ADDRESS2_PROPERTY)
    private String address2;

    @Column(name = ModelConstants.ZIP_PROPERTY)
    private String zip;

    @Column(name = ModelConstants.PHONE_PROPERTY)
    private String phone;

    @Column(name = ModelConstants.EMAIL_PROPERTY)
    private String email;

    @Type(type = "json")
    @Column(name = ModelConstants.TENANT_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    @Column(name = ModelConstants.TENANT_TENANT_PROFILE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID tenantProfileId;

    public AbstractTenantEntity() {
        super();
    }

    public AbstractTenantEntity(@NotNull Tenant tenant) {
        if (tenant.getId() != null) {
            this.setUuid(tenant.getId().getId());
        }
        this.setCreatedTime(tenant.getCreatedTime());
        this.title = tenant.getTitle();
        this.region = tenant.getRegion();
        this.country = tenant.getCountry();
        this.state = tenant.getState();
        this.city = tenant.getCity();
        this.address = tenant.getAddress();
        this.address2 = tenant.getAddress2();
        this.zip = tenant.getZip();
        this.phone = tenant.getPhone();
        this.email = tenant.getEmail();
        this.additionalInfo = tenant.getAdditionalInfo();
        if (tenant.getTenantProfileId() != null) {
            this.tenantProfileId = tenant.getTenantProfileId().getId();
        }
    }

    public AbstractTenantEntity(@NotNull TenantEntity tenantEntity) {
        this.setId(tenantEntity.getId());
        this.setCreatedTime(tenantEntity.getCreatedTime());
        this.title = tenantEntity.getTitle();
        this.region = tenantEntity.getRegion();
        this.country = tenantEntity.getCountry();
        this.state = tenantEntity.getState();
        this.city = tenantEntity.getCity();
        this.address = tenantEntity.getAddress();
        this.address2 = tenantEntity.getAddress2();
        this.zip = tenantEntity.getZip();
        this.phone = tenantEntity.getPhone();
        this.email = tenantEntity.getEmail();
        this.additionalInfo = tenantEntity.getAdditionalInfo();
        this.tenantProfileId = tenantEntity.getTenantProfileId();
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

    @NotNull
    protected Tenant toTenant() {
        @NotNull Tenant tenant = new Tenant(TenantId.fromUUID(this.getUuid()));
        tenant.setCreatedTime(createdTime);
        tenant.setTitle(title);
        tenant.setRegion(region);
        tenant.setCountry(country);
        tenant.setState(state);
        tenant.setCity(city);
        tenant.setAddress(address);
        tenant.setAddress2(address2);
        tenant.setZip(zip);
        tenant.setPhone(phone);
        tenant.setEmail(email);
        tenant.setAdditionalInfo(additionalInfo);
        if (tenantProfileId != null) {
            tenant.setTenantProfileId(new TenantProfileId(tenantProfileId));
        }
        return tenant;
    }


}
