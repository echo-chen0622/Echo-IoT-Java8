package org.echoiot.server.common.transport.config.ssl;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.echoiot.server.common.data.ResourceUtils;
import org.echoiot.server.common.data.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

@Data
@EqualsAndHashCode(callSuper = false)
public class KeystoreSslCredentials extends AbstractSslCredentials {

    private String type;
    private String storeFile;
    private String storePassword;
    private String keyPassword;
    private String keyAlias;

    @Override
    protected boolean canUse() {
        return ResourceUtils.resourceExists(this, this.storeFile);
    }

    @NotNull
    @Override
    protected KeyStore loadKeyStore(boolean trustsOnly, char[] keyPasswordArray) throws IOException, GeneralSecurityException {
        String keyStoreType = StringUtils.isEmpty(this.type) ? KeyStore.getDefaultType() : this.type;
        @NotNull KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        try (@NotNull InputStream tsFileInputStream = ResourceUtils.getInputStream(this, this.storeFile)) {
            keyStore.load(tsFileInputStream, StringUtils.isEmpty(this.storePassword) ? new char[0] : this.storePassword.toCharArray());
        }
        return keyStore;
    }

    @Override
    protected void updateKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }
}
