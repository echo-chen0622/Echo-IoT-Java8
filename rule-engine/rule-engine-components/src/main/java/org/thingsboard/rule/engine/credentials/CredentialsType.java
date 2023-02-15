package org.thingsboard.rule.engine.credentials;

public enum CredentialsType {
    ANONYMOUS("anonymous"),
    BASIC("basic"),
    SAS("sas"),
    CERT_PEM("cert.PEM");

    private final String label;

    CredentialsType(String label) {
        this.label = label;
    }
}
