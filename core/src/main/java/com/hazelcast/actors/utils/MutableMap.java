package com.hazelcast.actors.utils;

import java.util.HashMap;
import java.util.Map;

public final class MutableMap {

    public static Map<String, Object> of() {
        return new HashMap<String, Object>();
    }

    public static Map<String, Object> of(String key1, Object value1) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key1, value1);
        return map;
    }

    public static Map<String, Object> map(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    public static Map<String, Object> of(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }
}
