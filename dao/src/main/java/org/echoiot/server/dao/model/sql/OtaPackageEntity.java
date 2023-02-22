package org.echoiot.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.OtaPackage;
import org.echoiot.server.common.data.id.DeviceProfileId;
import org.echoiot.server.common.data.id.OtaPackageId;
import org.echoiot.server.common.data.id.TenantId;
import org.echoiot.server.common.data.ota.ChecksumAlgorithm;
import org.echoiot.server.common.data.ota.OtaPackageType;
import org.echoiot.server.dao.model.BaseSqlEntity;
import org.echoiot.server.dao.model.ModelConstants;
import org.echoiot.server.dao.model.SearchTextEntity;
import org.echoiot.server.dao.util.mapping.JsonStringType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.nio.ByteBuffer;
import java.util.UUID;

import static org.echoiot.server.dao.model.ModelConstants.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = OTA_PACKAGE_TABLE_NAME)
public class OtaPackageEntity extends BaseSqlEntity<OtaPackage> implements SearchTextEntity<OtaPackage> {

    @Column(name = OTA_PACKAGE_TENANT_ID_COLUMN)
    private UUID tenantId;

    @Column(name = OTA_PACKAGE_DEVICE_PROFILE_ID_COLUMN)
    private UUID deviceProfileId;

    @Enumerated(EnumType.STRING)
    @Column(name = OTA_PACKAGE_TYPE_COLUMN)
    private OtaPackageType type;

    @Column(name = OTA_PACKAGE_TILE_COLUMN)
    private String title;

    @Column(name = OTA_PACKAGE_VERSION_COLUMN)
    private String version;

    @Column(name = OTA_PACKAGE_TAG_COLUMN)
    private String tag;

    @Column(name = OTA_PACKAGE_URL_COLUMN)
    private String url;

    @Column(name = OTA_PACKAGE_FILE_NAME_COLUMN)
    private String fileName;

    @Column(name = OTA_PACKAGE_CONTENT_TYPE_COLUMN)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = OTA_PACKAGE_CHECKSUM_ALGORITHM_COLUMN)
    private ChecksumAlgorithm checksumAlgorithm;

    @Column(name = OTA_PACKAGE_CHECKSUM_COLUMN)
    private String checksum;

    @Lob
    @Column(name = OTA_PACKAGE_DATA_COLUMN, columnDefinition = "BINARY")
    private byte[] data;

    @Column(name = OTA_PACKAGE_DATA_SIZE_COLUMN)
    private Long dataSize;

    @Type(type = "json")
    @Column(name = ModelConstants.OTA_PACKAGE_ADDITIONAL_INFO_COLUMN)
    private JsonNode additionalInfo;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    public OtaPackageEntity() {
        super();
    }

    public OtaPackageEntity(OtaPackage otaPackage) {
        this.createdTime = otaPackage.getCreatedTime();
        this.setUuid(otaPackage.getUuidId());
        this.tenantId = otaPackage.getTenantId().getId();
        if (otaPackage.getDeviceProfileId() != null) {
            this.deviceProfileId = otaPackage.getDeviceProfileId().getId();
        }
        this.type = otaPackage.getType();
        this.title = otaPackage.getTitle();
        this.version = otaPackage.getVersion();
        this.tag = otaPackage.getTag();
        this.url = otaPackage.getUrl();
        this.fileName = otaPackage.getFileName();
        this.contentType = otaPackage.getContentType();
        this.checksumAlgorithm = otaPackage.getChecksumAlgorithm();
        this.checksum = otaPackage.getChecksum();
        this.data = otaPackage.getData().array();
        this.dataSize = otaPackage.getDataSize();
        this.additionalInfo = otaPackage.getAdditionalInfo();
    }

    @Override
    public String getSearchTextSource() {
        return title;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public OtaPackage toData() {
        OtaPackage otaPackage = new OtaPackage(new OtaPackageId(id));
        otaPackage.setCreatedTime(createdTime);
        otaPackage.setTenantId(TenantId.fromUUID(tenantId));
        if (deviceProfileId != null) {
            otaPackage.setDeviceProfileId(new DeviceProfileId(deviceProfileId));
        }
        otaPackage.setType(type);
        otaPackage.setTitle(title);
        otaPackage.setVersion(version);
        otaPackage.setTag(tag);
        otaPackage.setUrl(url);
        otaPackage.setFileName(fileName);
        otaPackage.setContentType(contentType);
        otaPackage.setChecksumAlgorithm(checksumAlgorithm);
        otaPackage.setChecksum(checksum);
        otaPackage.setDataSize(dataSize);
        if (data != null) {
            otaPackage.setData(ByteBuffer.wrap(data));
            otaPackage.setHasData(true);
        }
        otaPackage.setAdditionalInfo(additionalInfo);
        return otaPackage;
    }
}
