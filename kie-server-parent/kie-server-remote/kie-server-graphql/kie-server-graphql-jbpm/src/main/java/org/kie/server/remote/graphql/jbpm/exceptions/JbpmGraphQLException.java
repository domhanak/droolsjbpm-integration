package org.kie.server.remote.graphql.jbpm.exceptions;

public class JbpmGraphQLException extends RuntimeException {

    public JbpmGraphQLException() {
    }

    public JbpmGraphQLException(String message) {
        super(message);
    }

    public JbpmGraphQLException(String message, Throwable cause) {
        super(message, cause);
    }

    public JbpmGraphQLException(Throwable cause) {
        super(cause);
    }

    public JbpmGraphQLException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
