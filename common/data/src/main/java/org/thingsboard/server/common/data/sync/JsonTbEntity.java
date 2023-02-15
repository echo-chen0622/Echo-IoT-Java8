package org.thingsboard.server.common.data.sync;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "entityType", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
@JsonSubTypes({
        @Type(name = "DEVICE", value = Device.class),
        @Type(name = "RULE_CHAIN", value = RuleChain.class),
        @Type(name = "DEVICE_PROFILE", value = DeviceProfile.class),
        @Type(name = "ASSET_PROFILE", value = AssetProfile.class),
        @Type(name = "ASSET", value = Asset.class),
        @Type(name = "DASHBOARD", value = Dashboard.class),
        @Type(name = "CUSTOMER", value = Customer.class),
        @Type(name = "ENTITY_VIEW", value = EntityView.class),
        @Type(name = "WIDGETS_BUNDLE", value = WidgetsBundle.class)
})
@JsonIgnoreProperties(value = {"tenantId", "createdTime"}, ignoreUnknown = true)
public @interface JsonTbEntity {
}
