package com.restaurant.exception;


public class ExceptionErrorResponse {

    private String message;
    public ExceptionErrorResponse() {
    }

    public ExceptionErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
