package io.brooklyn.web;

import com.google.common.io.Files;
import io.brooklyn.SoftwareProcessDriver;
import io.brooklyn.util.BashScriptRunner;

import java.io.File;

public class TomcatDriver implements SoftwareProcessDriver {
    private final Tomcat tomcat;

    public TomcatDriver(Tomcat tomcat) {
        this.tomcat = tomcat;
        File runDir = Files.createTempDir();
        tomcat.runDir.set(runDir.getAbsolutePath());
    }

    private String findScript() {
        ClassLoader classLoader = getClass().getClassLoader();
        String name = "drivers/" + getClass().getName().replace(".", "/") + ".sh";
        return classLoader.getResource(name).getFile();
    }

    private BashScriptRunner buildBashScriptRunner() {
        BashScriptRunner runner = new BashScriptRunner(findScript());
        runner.addEnvironmentVariable("RUN_DIR", tomcat.runDir);
        runner.addEnvironmentVariable("HTTP_PORT", tomcat.httPort);
        runner.addEnvironmentVariable("JMX_PORT", tomcat.jmxPort);
        runner.addEnvironmentVariable("SHUTDOWN_PORT", tomcat.shutdownPort);
        runner.addEnvironmentVariable("VERSION", tomcat.version);
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
        //tomcat.getManagementContext().executeLocally(
        //        new Runnable() {
        //            public void run() {
        buildBashScriptRunner().runZeroExitCode("install");
        //                tomcat.send(new Callback() {
        //                    public void run() {
        //                        tomcat.state.set("foo");
        //                    }
        //                });
        //            }
        //        }
        //);
    }

    @Override
    public void launch() {
        buildBashScriptRunner().runZeroExitCode("launch");
        tomcat.jmxConnection.init(tomcat.location.get(), tomcat.jmxPort.get());
    }

    @Override
    public void stop() {
        buildBashScriptRunner().run("terminate");
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
