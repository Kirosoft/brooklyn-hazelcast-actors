package io.brooklyn.entity.web;

import io.brooklyn.entity.softwareprocess.SoftwareProcessDriver;

public interface TomcatDriver extends SoftwareProcessDriver {

    void deploy(String url);

    void undeploy();
}
