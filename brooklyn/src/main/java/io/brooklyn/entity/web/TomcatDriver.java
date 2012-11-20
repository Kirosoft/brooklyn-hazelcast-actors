package io.brooklyn.entity.web;

import com.google.common.io.Files;
import io.brooklyn.entity.softwareprocess.SoftwareProcessDriver;
import io.brooklyn.util.BashScriptRunner;

import java.io.File;

/**
 * The Driver for Tomcat.
 * <p/>
 * Currently there is no system in place that selects a drivers for the right environment, e.g. ssh. Or even more
 * concrete, centos. That can all be added in the
 * {@link io.brooklyn.ManagementContext#newDriver(io.brooklyn.entity.softwareprocess.SoftwareProcess)} method. Since
 * we already know how to do it, we are not going to do it again.
 */
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

    //currently we manually copy the properties to the script. But perhaps some kind of auto copy would be nice
    //to prevent duplication.
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
        //todo:
        buildBashScriptRunner().runWithoutFailure("deploy");
    }

    public void undeploy() {
        //todo:
        buildBashScriptRunner().runWithoutFailure("undeploy");
    }

    @Override
    public void customize() {
        buildBashScriptRunner().runWithoutFailure("customize");
    }

    @Override
    public void install() {
        //tomcat.getManagementContext().executeLocally(
        //        new Runnable() {
        //            public void run() {
        buildBashScriptRunner().runWithoutFailure("install");
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
        buildBashScriptRunner().runWithoutFailure("launch");
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
