package com.eskcti.algashop.billing.presentation;

public class BadGatewayException extends RuntimeException {
    public BadGatewayException() {
    }

    public BadGatewayException(String message) {
        super(message);
    }

    public BadGatewayException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class ServerErrorException extends BadGatewayException {
        public ServerErrorException() {
        }

        public ServerErrorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
