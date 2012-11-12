package example.hazelcast.brooklyn;

public interface SoftwareProcessDriver {
    void install();

    void customize();

    void launch();

    void stop();

    boolean isRunning();
}
