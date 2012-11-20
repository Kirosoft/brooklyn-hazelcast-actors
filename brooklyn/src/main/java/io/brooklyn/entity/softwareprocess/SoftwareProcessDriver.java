package io.brooklyn.entity.softwareprocess;

public interface SoftwareProcessDriver {

    void install();

    void customize();

    void launch();

    void stop();

    boolean isRunning();
}
