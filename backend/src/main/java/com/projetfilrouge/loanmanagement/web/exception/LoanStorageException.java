package com.projetfilrouge.loanmanagement.web.exception;

public class LoanStorageException extends RuntimeException {

    public LoanStorageException(String message) {
        super(message);
    }

    public LoanStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
