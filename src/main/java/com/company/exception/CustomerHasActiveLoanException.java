package com.company.exception;

public class CustomerHasActiveLoanException extends RuntimeException {
    public CustomerHasActiveLoanException(String message) {
        super(message);
    }
}