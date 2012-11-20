package io.brooklyn.util;


import com.google.common.io.Files;

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

    public BashScriptRunner(String rawScript) {
        this.rawScript = rawScript;
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
            System.out.println(script.getAbsoluteFile());

            String[] command;
            if (function != null) {
                command = new String[]{"/bin/sh", "-c", String.format("source %s && %s", script.getAbsolutePath(), function)};
            } else {
                command = new String[]{"/bin/sh", "-c", script.getAbsolutePath()};
            }

            StringBuffer sb = new StringBuffer();
            for (String s : command) {
                sb.append(s + " ");
            }
            System.out.println("===========================");
            System.out.println(sb);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream();
            Process process = processBuilder.start();
            new LoggerThread(process.getErrorStream(), true).start();
            new LoggerThread(process.getInputStream(), false).start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.out.println("exited with " + exitCode);
            }
            return exitCode;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
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
