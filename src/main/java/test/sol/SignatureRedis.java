package test.sol;

import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SignatureRedis {
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;
    private static final String SIGNATURES_KEY = "signature_";

    public static Set<String> loadWalletSignatures(String wallet) {
        Set<String> signatures = new HashSet<>();
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            signatures.addAll(jedis.smembers(SIGNATURES_KEY + wallet));
        } catch (Exception e) {
            System.err.println("Ошибка подключения к Redis: " + e.getMessage());
        }
        return signatures;
    }

    public static void saveWalletSignatures(Set<String> signatures, String wallet) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String signature : signatures) {
                jedis.sadd(SIGNATURES_KEY + wallet, signature);
            }
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунтов в Redis: " + e.getMessage());
        }
    }

    public static void removeWalletSignatures(List<String> wallets) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String wallet : wallets) {
                jedis.del(SIGNATURES_KEY + wallet);
            }
        } catch (Exception e) {
            System.err.println("Ошибка удаления аккаунта из Redis: " + e.getMessage());
        }
    }
}
