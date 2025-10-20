package com.restaurant.exception;

public class TypeMismatchException extends RuntimeException {

    private final String parameterName;
    private final String requiredType;
    private final String providedValue;

    public TypeMismatchException(String parameterName, String requiredType, String providedValue) {
        super(String.format("Parameter '%s' should be of type '%s', but value '%s' was provided",
                parameterName, requiredType, providedValue));
        this.parameterName = parameterName;
        this.requiredType = requiredType;
        this.providedValue = providedValue;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getRequiredType() {
        return requiredType;
    }

    public String getProvidedValue() {
        return providedValue;
    }
}