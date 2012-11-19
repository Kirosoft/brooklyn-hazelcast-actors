package com.hazelcast.actors.utils;

import java.util.HashMap;
import java.util.Map;

public final class MutableMap {

    public static Map<String, Object> map() {
        return new HashMap<String, Object>();
    }

    public static Map<String, Object> map(String key1, Object value1) {
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

    public static Map<String, Object> map(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }

    public static Map<String, Object> map(String key1, Object value1, String key2, Object value2, String key3, Object value3,
                                          String key4, Object value4) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return map;
    }

    public static Map<String, Object> map(String key1, Object value1, String key2, Object value2, String key3, Object value3,
                                          String key4, Object value4, String key5, Object value5) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        return map;
    }

    public static Map<String, Object> map(String key1, Object value1, String key2, Object value2, String key3, Object value3,
                                          String key4, Object value4, String key5, Object value5, String key6, Object value6) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        return map;
    }

    public static Map<String, Object> map(String key1, Object value1, String key2, Object value2, String key3, Object value3,
                                          String key4, Object value4, String key5, Object value5, String key6, Object value6,
                                          String key7, Object value7) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        map.put(key7, value7);
        return map;
    }

    public static Map<String, Object> map(String key1, Object value1, String key2, Object value2, String key3, Object value3,
                                          String key4, Object value4, String key5, Object value5, String key6, Object value6,
                                          String key7, Object value7, String key8, Object value8) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        map.put(key7, value7);
        map.put(key8, value8);
        return map;
    }
}
