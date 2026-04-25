package com.wuxiaoya.techstart.core.codec.tag;

import java.util.List;

public interface TagObject {
    boolean has(String key);

    String getString(String key);

    int getInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    TagObject getObject(String key);

    List<String> getStringList(String key);

    List<Integer> getIntList(String key);

    List<TagObject> getObjectList(String key);
}

