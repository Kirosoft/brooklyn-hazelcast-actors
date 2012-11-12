package io.brooklyn.web;

import io.brooklyn.SoftwareProcessDriver;

public class TomcatDriver implements SoftwareProcessDriver {
    private final Tomcat tomcat;

    public TomcatDriver(Tomcat tomcat){
        this.tomcat = tomcat;
    }

    public void deploy() {
    }

    public void undeploy() {
    }

    @Override
    public void customize() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void install() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void launch() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stop() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRunning() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
