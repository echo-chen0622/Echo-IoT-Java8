package org.thingsboard.server.common.data.ota;

import lombok.Getter;

public enum OtaPackageKey {

    TITLE("title"), VERSION("version"), TS("ts"), STATE("state"), SIZE("size"), CHECKSUM("checksum"), CHECKSUM_ALGORITHM("checksum_algorithm"), URL("url"), TAG("tag");

    @Getter
    private final String value;

    OtaPackageKey(String value) {
        this.value = value;
    }
}
