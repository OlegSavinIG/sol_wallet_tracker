package test.sol.redis;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

public class ValidatedWalletsRedis {
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;
    private static final String VALIDATED_WALLETS_KEY = "validated_wallets";

    public static void saveValidatedWalletsWithTTL(List<String> newAccounts) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String account : newAccounts) {
                String accountKey = VALIDATED_WALLETS_KEY + ":" + account;
                jedis.setex(accountKey, 57600, account); // 86400 секунд = 24 часа
            }
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунтов в Redis с TTL: " + e.getMessage());
        }
    }

    public static List<String> loadValidatedAccounts() {
        List<String> accounts = new ArrayList<>();
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String key : jedis.keys(VALIDATED_WALLETS_KEY + ":*")) {
                accounts.add(jedis.get(key));
            }
        } catch (Exception e) {
            System.err.println("Ошибка загрузки аккаунтов из Redis: " + e.getMessage());
        }
        return accounts;
    }

    public static void removeValidatedWallets(List<String> wallets) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String wallet : wallets) {
                String walletKey = VALIDATED_WALLETS_KEY + ":" + wallet;
                jedis.del(walletKey);
            }
        } catch (Exception e) {
            System.err.println("Ошибка удаления аккаунтов из Redis: " + e.getMessage());
        }
    }
}
