package test.sol.redis;

import redis.clients.jedis.Jedis;

import java.util.List;

public class ConfirmedWalletsRedis {
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;
    private static final String CONFIRMED_WALLET_KEY = "confirmed_wallets";
    public static void saveConfirmedWallets(List<String> confirmed) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String account : confirmed) {
                jedis.rpush(CONFIRMED_WALLET_KEY, account);
            }
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунтов в Redis: " + e.getMessage());
        }
    }

}
