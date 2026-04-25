package com.wuxiaoya.techstart.core.codec.tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class MapTagObject implements TagObject {
    private static final MapTagObject EMPTY = new MapTagObject(Map.of());

    private final Map<String, ?> values;

    public MapTagObject(Map<String, ?> values) {
        this.values = values == null ? Map.of() : values;
    }

    public static MapTagObject of(Map<String, ?> values) {
        return new MapTagObject(values);
    }

    public static MapTagObject empty() {
        return EMPTY;
    }

    @Override
    public boolean has(String key) {
        return values.containsKey(key);
    }

    @Override
    public String getString(String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = values.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string.trim());
        }
        return defaultValue;
    }

    @Override
    public TagObject getObject(String key) {
        Object value = values.get(key);
        if (value instanceof TagObject tagObject) {
            return tagObject;
        }
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> casted = (Map<String, ?>) map;
            return new MapTagObject(casted);
        }
        return EMPTY;
    }

    @Override
    public List<String> getStringList(String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>(list.size());
        for (Object entry : list) {
            if (entry != null) {
                result.add(String.valueOf(entry).trim());
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<Integer> getIntList(String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>(list.size());
        for (Object entry : list) {
            if (entry instanceof Number number) {
                result.add(number.intValue());
            } else if (entry instanceof String string) {
                try {
                    result.add(Integer.parseInt(string.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<TagObject> getObjectList(String key) {
        Object value = values.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<TagObject> result = new ArrayList<>(list.size());
        for (Object entry : list) {
            if (entry instanceof TagObject tagObject) {
                result.add(tagObject);
            } else if (entry instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, ?> casted = (Map<String, ?>) map;
                result.add(new MapTagObject(casted));
            }
        }
        return Collections.unmodifiableList(result);
    }
}

