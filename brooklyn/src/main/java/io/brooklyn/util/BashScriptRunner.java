package io.brooklyn.util;


import com.google.common.io.Files;
import io.brooklyn.locations.SshMachineLocation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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
            sshMachineLocation.runScriptFunction(script.getAbsolutePath(), function);

            int exitCode = 0;//process.waitFor();
            if (exitCode != 0) {
                System.out.println("exited with " + exitCode);
            }
            return exitCode;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private class LoggerThread extends Thread {
        InputStream in;
        private final boolean error;

        private LoggerThread(InputStream in, boolean error) {
            this.in = in;
            this.error = error;
        }

        public void run() {
            try {
                BufferedReader is = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = is.readLine()) != null) {
                    if (error) {
                        System.out.println("err " + line);
                    } else {
                        System.out.println("    " + line);

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File buildFinalScript(File file) throws IOException {
        StringBuffer envVariables = new StringBuffer();

        for (Map.Entry<String, Object> entry : environmentVariables.entrySet()) {
            envVariables.append(entry.getKey() + "=" + entry.getValue());
            envVariables.append("\n");
        }

        String origin = Files.toString(file, Charset.defaultCharset());
        String result = origin.replace("${EnvironmentVariables}", envVariables);

        File tmpFile = File.createTempFile(getClass().getSimpleName(), ".sh");
        Files.write(result, tmpFile, Charset.defaultCharset());
        tmpFile.setExecutable(true);
        return tmpFile;
    }
}
