package test.sol.utils;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.Arrays;
import java.util.List;

public class ConfigLoader {
    private static final Dotenv dotenv = Dotenv.load();

    public static String getString(String key) {
        return dotenv.get(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(dotenv.get(key));
    }

    public static long getLong(String key) {
        return Long.parseLong(dotenv.get(key));
    }

    public static List<String> getList(String key) {
        String value = dotenv.get(key);
        return value != null ? Arrays.asList(value.split(",")) : List.of();
    }
}

