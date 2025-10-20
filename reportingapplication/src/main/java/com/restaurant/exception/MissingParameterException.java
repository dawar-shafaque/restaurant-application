package com.restaurant.exception;


public class MissingParameterException extends RuntimeException{

    private final String parameterName;
    private final String parameterType;

    public MissingParameterException(String parameterName, String parameterType) {
        super(String.format("Required parameter '%s' of type '%s' is missing", parameterName, parameterType));
        this.parameterName = parameterName;
        this.parameterType = parameterType;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getParameterType() {
        return parameterType;
    }
}
