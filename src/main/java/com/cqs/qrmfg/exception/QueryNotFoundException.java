package com.cqs.qrmfg.exception;

public class QueryNotFoundException extends QueryException {
    public QueryNotFoundException(String message) {
        super(message);
    }
    
    public QueryNotFoundException(Long queryId) {
        super(String.format("Query not found with ID: %d", queryId));
    }
}