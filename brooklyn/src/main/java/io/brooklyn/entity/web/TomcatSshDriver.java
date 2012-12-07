package io.brooklyn.entity.web;

import brooklyn.location.basic.SshMachineLocation;
import com.google.common.io.Files;
import io.brooklyn.util.BashScriptRunner;

import java.io.File;
import java.net.URL;

import static java.lang.String.format;

/**
 * The Driver for Tomcat.
 * <p/>
 * Currently there is no system in place that selects a drivers for the right environment, e.g. ssh. Or even more
 * concrete, centos. That can all be added in the
 * {@link io.brooklyn.ManagementContext#newDriver(io.brooklyn.entity.softwareprocess.SoftwareProcess)} method. Since
 * we already know how to do it, we are not going to do it again.
 */
public class TomcatSshDriver implements TomcatDriver {
    private final Tomcat tomcat;
    private final SshMachineLocation sshLocation;

    public TomcatSshDriver(Tomcat tomcat, SshMachineLocation sshLocation) {
        this.tomcat = tomcat;
        this.sshLocation = sshLocation;
        File runDir = Files.createTempDir();
        tomcat.runDir.set(runDir.getAbsolutePath());
    }

    private String findScript() {
        ClassLoader classLoader = getClass().getClassLoader();
        String path = "drivers/" + getClass().getName().replace(".", "/") + ".sh";
        URL resource = classLoader.getResource(path);
        if (resource == null) {
            throw new RuntimeException(format("Can't find script: '%s'", path));
        }
        return resource.getFile();
    }

    //currently we manually copy the properties to the script. But perhaps some kind of auto copy would be nice
    //to prevent duplication.
    private BashScriptRunner buildBashScriptRunner() {
        BashScriptRunner runner = new BashScriptRunner(findScript(),sshLocation);
        runner.addEnvironmentVariable("RUN_DIR", tomcat.runDir);
        runner.addEnvironmentVariable("HTTP_PORT", tomcat.httPort);
        runner.addEnvironmentVariable("JMX_PORT", tomcat.jmxPort);
        runner.addEnvironmentVariable("SHUTDOWN_PORT", tomcat.shutdownPort);
        runner.addEnvironmentVariable("VERSION", tomcat.version);
        return runner;
    }

    @Override
    public void deploy(String url) {
        //todo:
        buildBashScriptRunner().runWithoutFailure("deploy");
    }

    @Override
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
        tomcat.jmxConnection.init(sshLocation.getAddress().getHostName(), tomcat.jmxPort.get());
    }

    @Override
    public void stop() {
        buildBashScriptRunner().run("exit");
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
