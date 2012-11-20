package io.brooklyn.locations;

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

public class SshMachineLocationTest {

    private Properties properties;

    @Before
    public void setUp() throws IOException {
        properties = new Properties();
        File brooklynProperties = new File(System.getProperty("user.home"), ".brooklyn/brooklyn.properties");
        properties.load(new FileInputStream(brooklynProperties));

    }

    @Test
    public void testRun() {
        SshMachineLocation location = new SshMachineLocation("localhost");
        location.setUserName(System.getProperty("user.name"));
        location.setPrivateKey((String) properties.get("brooklyn.jclouds.private-key-file"));
        System.out.println("publicKey:" + location.getPrivateKey());
        location.run("ls");
    }

    @Test
    public void testRunScript() throws IOException {
        SshMachineLocation location = new SshMachineLocation("localhost");
        location.setUserName(System.getProperty("user.name"));
        location.setPrivateKey((String) properties.get("brooklyn.jclouds.private-key-file"));
        System.out.println("publicKey:" + location.getPrivateKey());

        File tmpFile = File.createTempFile("foo","sh");
        Files.write("function yes(){\necho hello\n}", tmpFile, Charset.defaultCharset());

        location.runScriptFunction(tmpFile.getAbsolutePath(),"yes");
    }
}
