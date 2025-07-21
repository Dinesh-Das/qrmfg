package com.cqs.qrmfg.exception;

public class QueryAlreadyResolvedException extends QueryException {
    public QueryAlreadyResolvedException(String message) {
        super(message);
    }
    
    public QueryAlreadyResolvedException(Long queryId) {
        super(String.format("Query with ID %d is already resolved", queryId));
    }
}