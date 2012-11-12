package com.hazelcast.actors.rest;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class ActorsResource extends ServerResource {


    @Get
    public String toString() {
        return "hello, world";
    }
}
