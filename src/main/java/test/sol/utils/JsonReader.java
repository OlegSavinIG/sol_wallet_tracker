package test.sol.utils;

import java.util.Map;

public class JsonReader {
    @SuppressWarnings("unchecked")
    public static <T> T extractField(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(key);
        }
        return (T) current;
    }
}
