package io.brooklyn.web;

import com.google.common.io.Files;
import io.brooklyn.SoftwareProcessDriver;
import io.brooklyn.util.BashScriptRunner;
import io.brooklyn.util.JmxConnection;

import java.io.File;

public class TomcatDriver implements SoftwareProcessDriver {
    private final Tomcat tomcat;
    private final File runDir = Files.createTempDir();

    public TomcatDriver(Tomcat tomcat) {
        this.tomcat = tomcat;
    }

    private String findScript() {
        ClassLoader classLoader = getClass().getClassLoader();
        String name = "drivers/" + getClass().getName().replace(".", "/") + ".sh";
        return classLoader.getResource(name).getFile();
    }

    private BashScriptRunner buildBashScriptRunner() {
        BashScriptRunner runner = new BashScriptRunner(findScript());
        runner.addEnvironmentVariable("RUN_DIR", runDir.getAbsolutePath());
        runner.addEnvironmentVariable("HTTP_PORT", tomcat.httPort.get());
        runner.addEnvironmentVariable("JMX_PORT", tomcat.jmxPort.get());
        runner.addEnvironmentVariable("SHUTDOWN_PORT", tomcat.shutdownPort.get());
        return runner;
    }

    public void deploy(String url) {
        buildBashScriptRunner().runZeroExitCode("deploy");
    }

    public void undeploy() {
        buildBashScriptRunner().runZeroExitCode("undeploy");
    }

    @Override
    public void customize() {
        buildBashScriptRunner().runZeroExitCode("customize");
    }

    @Override
    public void install() {
        buildBashScriptRunner().runZeroExitCode("install");
    }

    @Override
    public void launch() {
        buildBashScriptRunner().runZeroExitCode("launch");
    }

    public JmxConnection getJmxConnection() {
        return new JmxConnection("localhost",tomcat.jmxPort.get());
    }

    @Override
    public void stop() {
        buildBashScriptRunner().run("stop");
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
