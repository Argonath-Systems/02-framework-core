package com.argonathsystems.framework.core.result;

/**
 * Exception thrown when accessing the value of a failed Result.
 */
public class ResultException extends RuntimeException {

    /**
     * Create a result exception.
     *
     * @param message The error message from the failed Result
     */
    public ResultException(String message) {
        super(message);
    }
}
