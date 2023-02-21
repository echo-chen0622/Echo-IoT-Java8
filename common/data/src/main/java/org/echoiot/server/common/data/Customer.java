package org.echoiot.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.echoiot.server.common.data.id.CustomerId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.validation.Length;
import org.echoiot.server.common.data.validation.NoXss;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
public class Customer extends ContactBased<CustomerId> implements HasTenantId, ExportableEntity<CustomerId> {

    private static final long serialVersionUID = -1599722990298929275L;

    @NoXss
    @Length(fieldName = "title")
    @ApiModelProperty(position = 3, value = "Title of the customer", example = "Company A")
    private String title;
    @ApiModelProperty(position = 5, required = true, value = "JSON object with Tenant Id")
    private TenantId tenantId;

    @Getter @Setter
    private CustomerId externalId;

    public Customer() {
        super();
    }

    public Customer(CustomerId id) {
        super(id);
    }

    public Customer(@NotNull Customer customer) {
        super(customer);
        this.tenantId = customer.getTenantId();
        this.title = customer.getTitle();
        this.externalId = customer.getExternalId();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ApiModelProperty(position = 1, value = "JSON object with the customer Id. " +
            "Specify this field to update the customer. " +
            "Referencing non-existing customer Id will cause error. " +
            "Omit this field to create new customer." )
    @Override
    public CustomerId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the customer creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @ApiModelProperty(position = 6, required = true, value = "Country", example = "US")
    @Override
    public String getCountry() {
        return super.getCountry();
    }

    @ApiModelProperty(position = 7, required = true, value = "State", example = "NY")
    @Override
    public String getState() {
        return super.getState();
    }

    @ApiModelProperty(position = 8, required = true, value = "City", example = "New York")
    @Override
    public String getCity() {
        return super.getCity();
    }

    @ApiModelProperty(position = 9, required = true, value = "Address Line 1", example = "42 Broadway Suite 12-400")
    @Override
    public String getAddress() {
        return super.getAddress();
    }

    @ApiModelProperty(position = 10, required = true, value = "Address Line 2", example = "")
    @Override
    public String getAddress2() {
        return super.getAddress2();
    }

    @ApiModelProperty(position = 11, required = true, value = "Zip code", example = "10004")
    @Override
    public String getZip() {
        return super.getZip();
    }

    @ApiModelProperty(position = 12, required = true, value = "Phone number", example = "+1(415)777-7777")
    @Override
    public String getPhone() {
        return super.getPhone();
    }

    @ApiModelProperty(position = 13, required = true, value = "Email", example = "example@company.com")
    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @ApiModelProperty(position = 14, value = "Additional parameters of the device", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    @JsonIgnore
    public boolean isPublic() {
        if (getAdditionalInfo() != null && getAdditionalInfo().has("isPublic")) {
            return getAdditionalInfo().get("isPublic").asBoolean();
        }

        return false;
    }

    @NotNull
    @JsonIgnore
    public ShortCustomerInfo toShortCustomerInfo() {
        return new ShortCustomerInfo(id, title, isPublic());
    }

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    @ApiModelProperty(position = 4, value = "Name of the customer. Read-only, duplicated from title for backward compatibility", example = "Company A", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public String getName() {
        return title;
    }

    @Override
    public String getSearchText() {
        return getTitle();
    }

    @NotNull
    @Override
    public String toString() {
        String builder = "Customer [title=" +
                         title +
                         ", tenantId=" +
                         tenantId +
                         ", additionalInfo=" +
                         getAdditionalInfo() +
                         ", country=" +
                         country +
                         ", state=" +
                         state +
                         ", city=" +
                         city +
                         ", address=" +
                         address +
                         ", address2=" +
                         address2 +
                         ", zip=" +
                         zip +
                         ", phone=" +
                         phone +
                         ", email=" +
                         email +
                         ", createdTime=" +
                         createdTime +
                         ", id=" +
                         id +
                         "]";
        return builder;
    }
}
