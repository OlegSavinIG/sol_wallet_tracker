package test.sol.redis;

import redis.clients.jedis.Jedis;

import java.util.Set;
import java.util.stream.Collectors;

public class TrackWalletsRedis {
    private static final String REDIS_KEY = "tracked_wallets";
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;

    public static void saveWallets(Set<String> wallets) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) { // Укажите хост и порт Redis
            jedis.sadd(REDIS_KEY, wallets.toArray(new String[0]));
            System.out.println("✅ Wallets saved to Redis.");
        }
    }

    public static Set<String> loadWallets() {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            return jedis.smembers(REDIS_KEY).stream().collect(Collectors.toSet());
        }
    }

    public static void clearWallets() {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            jedis.del(REDIS_KEY);
            System.out.println("✅ Redis wallet storage cleared.");
        }
    }
}
