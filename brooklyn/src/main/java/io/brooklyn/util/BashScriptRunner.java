package io.brooklyn.util;

import brooklyn.location.basic.SshMachineLocation;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BashScriptRunner {

    private String rawScript;
    private final Map<String, Object> environmentVariables = new HashMap();
    private SshMachineLocation sshMachineLocation;

    public BashScriptRunner(String rawScript, SshMachineLocation sshMachineLocation) {
        this.rawScript = rawScript;
        this.sshMachineLocation = sshMachineLocation;
    }

    public void addEnvironmentVariable(String variable, Object value) {
        this.environmentVariables.put(variable, value);
    }

    public int run() {
        return run(null);
    }

    public void runWithoutFailure(String function) {
        int exitCode = run(function);
        if (exitCode != 0) {
            throw new IllegalStateException("exitcode:" + exitCode);
        }
    }

    public int run(String function) {
        File file = new File(rawScript);

        try {
            File script = buildFinalScript(file);
            String remotePath = "/tmp/" + UUID.randomUUID().toString().replace("-", "") + ".sh";
            System.out.println("Running script:"+remotePath);
            sshMachineLocation.copyTo(script.getAbsoluteFile(), remotePath);
            sshMachineLocation.run("chmod +x " + remotePath);

            String commandString;
            if (function != null) {
                commandString = String.format(". %s && %s", remotePath, function);
            } else {
                commandString = remotePath;
            }

            return sshMachineLocation.run(commandString);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private File buildFinalScript(File file) throws IOException {
        StringBuffer envVariables = new StringBuffer();

        for (Map.Entry<String, Object> entry : environmentVariables.entrySet()) {
            envVariables.append(entry.getKey() + "=" + entry.getValue());
            envVariables.append("\n");
        }

        System.out.println(envVariables);

        String origin = Files.toString(file, Charset.defaultCharset());
        String result = origin.replace("${EnvironmentVariables}", envVariables);

        File tmpFile = File.createTempFile(getClass().getSimpleName(), ".sh");
        Files.write(result, tmpFile, Charset.defaultCharset());
        tmpFile.setExecutable(true);
        return tmpFile;
    }
}
