package com.rhcommons.json;

import java.util.HashMap;
import java.util.Optional;

public class JsonObject {
    private HashMap<String, Object> data = new HashMap<>();

    public Object getValue(String key) {
        return data.get(key);
    }
    
    public Optional<String> getStringValue(String key) {
        if(data.get(key) instanceof String stringValue) {
            return Optional.of(stringValue);
        }
        return Optional.empty();
    }

    public void set(String key, Object value) {
        if(value instanceof  String strValue) {
            if(strValue.startsWith("\"") && strValue.endsWith("\"")) {
                data.put(key, strValue.substring(1,strValue.length()-1));
            } else if("true".equals(strValue)) {
                data.put(key, Boolean.TRUE);
            } else if ("false".equals(strValue)) {
                data.put(key, Boolean.FALSE);
            } else {
                data.put(key, strValue);
            }
        } else {
            data.put(key, value);
        }
    }

    public JsonObject getJsonObject(String key) {
        Object value = data.get(key);
        if(value instanceof JsonObject jsonObject) {
            return jsonObject;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(var entry : data.entrySet()) {
            sb.append(entry.getKey() + " = <"+entry.getValue()+">").append("\n");
        }
        return sb.toString();
    }
}
