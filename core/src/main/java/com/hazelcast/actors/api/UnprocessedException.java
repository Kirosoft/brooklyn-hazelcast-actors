package com.hazelcast.actors.api;

public class UnprocessedException extends RuntimeException{

    public UnprocessedException(String message) {
        super(message);
    }
}
