package org.thingsboard.server.common.transport.config.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

public interface SslCredentials {

    void init(boolean trustsOnly) throws IOException, GeneralSecurityException;

    KeyStore getKeyStore();

    String getKeyPassword();

    String getKeyAlias();

    PrivateKey getPrivateKey();

    PublicKey getPublicKey();

    X509Certificate[] getCertificateChain();

    X509Certificate[] getTrustedCertificates();

    TrustManagerFactory createTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException;

    KeyManagerFactory createKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException;

    String getValueFromSubjectNameByKey(String subjectName, String key);
}
