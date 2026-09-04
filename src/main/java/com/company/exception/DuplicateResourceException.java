package com.company.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public static class CustomerHasActiveLoanException extends RuntimeException {
        public CustomerHasActiveLoanException(String message) {
            super(message);
        }
    }
}