package example.hazelcast.moop;

public interface Moop {

    String get(String key);

    void put(String key, String value);

    int size();

    int localSize();
}
