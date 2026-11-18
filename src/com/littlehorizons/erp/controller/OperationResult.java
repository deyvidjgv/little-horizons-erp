package com.littlehorizons.erp.controller;

/** Answer of a controller to an operation that changes data: whether it worked and the message for the user. */
public record OperationResult(boolean success, String message) {

    public static OperationResult ok(String message) {
        return new OperationResult(true, message);
    }

    public static OperationResult error(String message) {
        return new OperationResult(false, message);
    }
}
