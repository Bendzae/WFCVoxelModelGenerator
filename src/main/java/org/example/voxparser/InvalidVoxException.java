package org.example.voxparser;

public class InvalidVoxException extends RuntimeException {
    public InvalidVoxException(String message) {
        super(message);
    }

    public InvalidVoxException(String message, Throwable cause) {
        super(message, cause);
    }
}
