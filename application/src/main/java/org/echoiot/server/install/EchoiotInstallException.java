package org.echoiot.server.install;

import org.springframework.boot.ExitCodeGenerator;

public class EchoiotInstallException extends RuntimeException implements ExitCodeGenerator {

    public EchoiotInstallException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getExitCode() {
        return 1;
    }

}
