package io.brooklyn.example;

import io.brooklyn.activeobject.AbstractActiveObject;

public class Echoer extends AbstractActiveObject {
    public void echo(String echo) {
        System.out.println("echo:"+echo);
    }
}
