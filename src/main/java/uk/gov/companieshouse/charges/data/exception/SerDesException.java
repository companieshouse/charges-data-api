package uk.gov.companieshouse.charges.data.exception;

public class SerDesException extends RuntimeException {

    public SerDesException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
