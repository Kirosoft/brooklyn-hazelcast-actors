package io.brooklyn.locations;

import com.hazelcast.actors.utils.Util;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

import static com.hazelcast.actors.utils.Util.notNull;

public class SshMachineLocation implements Location {

    private String hostName;
    private String userName;
    private String privateKey;

    public SshMachineLocation(String hostname) {
        this.hostName = notNull(hostname, "hostname");
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getHostName() {
        return hostName;
    }

    public void run(String command) {
        SSHClient client = new SSHClient();
        try {
            final Session session = createSession(client);
            new LoggerThread(session.getInputStream(), false).start();

            session.exec(command);
            session.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.disconnect();
            } catch (IOException ignore) {
            }
        }
    }

    private Session createSession(SSHClient client) throws IOException {
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.setTimeout(10000000);
        client.setConnectTimeout(10000000);
        client.loadKnownHosts();
        client.connect(hostName);
        //todo: we need to fix the key..
        client.authPublickey(userName, "/Users/alarmnummer/.ssh/id_rsa");

        return client.startSession();
    }

    public Session runScriptFunction(String localScriptPath, String function) {
        SSHClient client = new SSHClient();
        try {
            final Session session = createSession(client);
            new LoggerThread(session.getInputStream(), false).start();
            String remotePath = "/tmp/" + UUID.randomUUID().toString().replace("-", "") + ".sh";
            client.newSCPFileTransfer().upload(localScriptPath, remotePath);
            String command = String.format("chmod +x %s && . %s && %s", remotePath, remotePath, function);
            Session.Command c = session.exec(command);
            new LoggerThread(c.getInputStream(), false).start();
            new LoggerThread(c.getErrorStream(), true).start();
            //todo: we need to fix the waiting..
            Util.sleep(5000);
            session.close();
            return session;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.disconnect();
            } catch (IOException ignore) {
            }
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
}
