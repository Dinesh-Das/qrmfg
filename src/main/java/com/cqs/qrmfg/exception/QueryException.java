package com.cqs.qrmfg.exception;

public class QueryException extends WorkflowException {
    public QueryException(String message) {
        super(message);
    }
    
    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}