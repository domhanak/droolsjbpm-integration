package org.kie.server.remote.graphql.jbpm.exceptions;

public class NoFilterMatchException extends RuntimeException {

    public NoFilterMatchException() {
    }

    public NoFilterMatchException(String message) {
        super(message);
    }

    public NoFilterMatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoFilterMatchException(Throwable cause) {
        super(cause);
    }

    public NoFilterMatchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
