package org.thingsboard.server.install;

import org.springframework.boot.ExitCodeGenerator;

public class ThingsboardInstallException extends RuntimeException implements ExitCodeGenerator {

    public ThingsboardInstallException(String message, Throwable cause) {
        super(message, cause);
    }

    public int getExitCode() {
        return 1;
    }

}
