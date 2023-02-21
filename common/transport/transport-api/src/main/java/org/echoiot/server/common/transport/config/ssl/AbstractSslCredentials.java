package org.echoiot.server.common.transport.config.ssl;

import org.echoiot.server.common.data.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public abstract class AbstractSslCredentials implements SslCredentials {

    private char[] keyPasswordArray;

    private KeyStore keyStore;

    private PrivateKey privateKey;

    private PublicKey publicKey;

    private X509Certificate[] chain;

    private X509Certificate[] trusts;

    @Override
    public void init(boolean trustsOnly) throws IOException, GeneralSecurityException {
        String keyPassword = getKeyPassword();
        if (StringUtils.isEmpty(keyPassword)) {
            this.keyPasswordArray = new char[0];
        } else {
            this.keyPasswordArray = keyPassword.toCharArray();
        }
        this.keyStore = this.loadKeyStore(trustsOnly, this.keyPasswordArray);
        @NotNull Set<X509Certificate> trustedCerts = getTrustedCerts(this.keyStore, trustsOnly);
        this.trusts = trustedCerts.toArray(new X509Certificate[0]);
        if (!trustsOnly) {
            @Nullable PrivateKeyEntry privateKeyEntry = null;
            String keyAlias = this.getKeyAlias();
            if (!StringUtils.isEmpty(keyAlias)) {
                privateKeyEntry = tryGetPrivateKeyEntry(this.keyStore, keyAlias, this.keyPasswordArray);
            } else {
                for (Enumeration<String> e = this.keyStore.aliases(); e.hasMoreElements(); ) {
                    String alias = e.nextElement();
                    privateKeyEntry = tryGetPrivateKeyEntry(this.keyStore, alias, this.keyPasswordArray);
                    if (privateKeyEntry != null) {
                        this.updateKeyAlias(alias);
                        break;
                    }
                }
            }
            if (privateKeyEntry == null) {
                throw new IllegalArgumentException("Failed to get private key from the keystore or pem files. " +
                        "Please check if the private key exists in the keystore or pem files and if the provided private key password is valid.");
            }
            this.chain = asX509Certificates(privateKeyEntry.getCertificateChain());
            this.privateKey = privateKeyEntry.getPrivateKey();
            if (this.chain.length > 0) {
                this.publicKey = this.chain[0].getPublicKey();
            }
        }
    }

    @Override
    public KeyStore getKeyStore() {
        return this.keyStore;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    @Override
    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    @Override
    public X509Certificate[] getCertificateChain() {
        return this.chain;
    }

    @Override
    public X509Certificate[] getTrustedCertificates() {
        return this.trusts;
    }

    @NotNull
    @Override
    public TrustManagerFactory createTrustManagerFactory() throws NoSuchAlgorithmException, KeyStoreException {
        @NotNull TrustManagerFactory tmFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmFactory.init(this.keyStore);
        return tmFactory;
    }

    @NotNull
    @Override
    public KeyManagerFactory createKeyManagerFactory() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        @NotNull KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(this.keyStore, this.keyPasswordArray);
        return kmf;
    }

    @Nullable
    @Override
    public String getValueFromSubjectNameByKey(@NotNull String subjectName, String key) {
        @NotNull String[] dns = subjectName.split(",");
        @NotNull Optional<String> cn = (Arrays.stream(dns).filter(dn -> dn.contains(key + "="))).findFirst();
        @Nullable String value = cn.isPresent() ? cn.get().replace(key + "=", "") : null;
        return StringUtils.isNotEmpty(value) ? value : null;
    }

    protected abstract boolean canUse();

    protected abstract KeyStore loadKeyStore(boolean isPrivateKeyRequired, char[] keyPasswordArray) throws IOException, GeneralSecurityException;

    protected abstract void updateKeyAlias(String keyAlias);

    @NotNull
    private static X509Certificate[] asX509Certificates(@NotNull Certificate[] certificates) {
        if (null == certificates || 0 == certificates.length) {
            throw new IllegalArgumentException("certificates missing!");
        }
        @NotNull X509Certificate[] x509Certificates = new X509Certificate[certificates.length];
        for (int index = 0; certificates.length > index; ++index) {
            if (null == certificates[index]) {
                throw new IllegalArgumentException("[" + index + "] is null!");
            }
            try {
                x509Certificates[index] = (X509Certificate) certificates[index];
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("[" + index + "] is not a x509 certificate! Instead it's a "
                        + certificates[index].getClass().getName());
            }
        }
        return x509Certificates;
    }

    @Nullable
    private static PrivateKeyEntry tryGetPrivateKeyEntry(@NotNull KeyStore keyStore, @NotNull String alias, char[] pwd) {
        @Nullable PrivateKeyEntry entry = null;
        try {
            if (keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                try {
                    entry = (KeyStore.PrivateKeyEntry) keyStore
                            .getEntry(alias, new KeyStore.PasswordProtection(pwd));
                } catch (UnsupportedOperationException e) {
                    PrivateKey key = (PrivateKey) keyStore.getKey(alias, pwd);
                    Certificate[] certs = keyStore.getCertificateChain(alias);
                    entry = new KeyStore.PrivateKeyEntry(key, certs);
                }
            }
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException ignored) {}
        return entry;
    }

    @NotNull
    private static Set<X509Certificate> getTrustedCerts(@NotNull KeyStore ks, boolean trustsOnly) {
        @NotNull Set<X509Certificate> set = new HashSet<>();
        try {
            for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                if (ks.isCertificateEntry(alias)) {
                    Certificate cert = ks.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        if (trustsOnly) {
                            // is CA certificate
                            if (((X509Certificate) cert).getBasicConstraints()>=0) {
                                set.add((X509Certificate) cert);
                            }
                        } else {
                            set.add((X509Certificate) cert);
                        }
                    }
                } else if (ks.isKeyEntry(alias)) {
                    Certificate[] certs = ks.getCertificateChain(alias);
                    if ((certs != null) && (certs.length > 0) &&
                            (certs[0] instanceof X509Certificate)) {
                        if (trustsOnly) {
                            for (@NotNull Certificate cert : certs) {
                                // is CA certificate
                                if (((X509Certificate) cert).getBasicConstraints()>=0) {
                                    set.add((X509Certificate) cert);
                                }
                            }
                        } else {
                            set.add((X509Certificate)certs[0]);
                        }
                    }
                }
            }
        } catch (KeyStoreException ignored) {}
        return Collections.unmodifiableSet(set);
    }
}
