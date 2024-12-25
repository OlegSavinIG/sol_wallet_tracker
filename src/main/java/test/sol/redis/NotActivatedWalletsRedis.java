package test.sol.redis;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

public class NotActivatedWalletsRedis {
    private static final String REDIS_KEY = "not_activated_wallets";
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;
    public static void saveWithTTL(List<String> newAccounts) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String account : newAccounts) {
                String accountKey = REDIS_KEY + ":" + account;
                jedis.setex(accountKey, 57600, account); // 86400 секунд = 24 часа
            }
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунтов в Redis с TTL: " + e.getMessage());
        }
    }

    public static List<String> load() {
        List<String> accounts = new ArrayList<>();
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String key : jedis.keys(REDIS_KEY + ":*")) {
                accounts.add(jedis.get(key));
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки аккаунтов из Redis: " + e.getMessage());
        }
        return accounts;
    }

    public static void remove(List<String> wallets) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String wallet : wallets) {
                String walletKey = REDIS_KEY + ":" + wallet;
                jedis.del(walletKey);
            }
        } catch (Exception e) {
            System.err.println("Ошибка удаления аккаунтов из Redis: " + e.getMessage());
        }
    }
}
