package com.hazelcast.actors.api.exceptions;

public class UnprocessedException extends RuntimeException{

    public UnprocessedException(String message) {
        super(message);
    }
}
