package test.sol;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

public class ValidatedWalletsRedis {
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;
    private static final String VALIDATED_WALLETS_KEY = "validated_wallets";

    public static List<String> loadValidatedAccounts() {
        List<String> accounts = new ArrayList<>();
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            accounts = jedis.lrange(VALIDATED_WALLETS_KEY, 0, -1);
        } catch (Exception e) {
            System.err.println("Ошибка подключения к Redis: " + e.getMessage());
        }
        return accounts;
    }
    public static void saveValidatedWallets(List<String> newAccounts) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String account : newAccounts) {
                jedis.rpush(VALIDATED_WALLETS_KEY, account);
            }
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунтов в Redis: " + e.getMessage());
        }
    }
    public static void removeValidatedWallets(List<String> wallets) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String wallet : wallets) {
                jedis.lrem(VALIDATED_WALLETS_KEY, 0, wallet);
            }
        } catch (Exception e) {
            System.err.println("Ошибка удаления аккаунта из Redis: " + e.getMessage());
        }
    }
}
