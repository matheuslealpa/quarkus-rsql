package io.github.matheuslealpa.rsql;

/**
 * Thrown when an RSQL filter can't be translated, for example because it is malformed,
 * references an unknown property or contains a value of the wrong type.
 */
public class RsqlException extends RuntimeException {

    public RsqlException(String message) {
        super(message);
    }

    public RsqlException(String message, Throwable cause) {
        super(message, cause);
    }

}
