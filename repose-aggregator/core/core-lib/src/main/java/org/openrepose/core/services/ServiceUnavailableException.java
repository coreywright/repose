package org.openrepose.core.services;

//TODO: Refactor this into a checked exception
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
