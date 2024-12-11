package test.sol;

import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Set;

public class ProcessedWalletsRedis {

    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;
    private static final String PROCESSED_KEY = "processed_wallets";

    public static Set<String> loadProcessedWallets() {
        Set<String> accounts = new HashSet<>();
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            accounts.addAll(jedis.smembers(PROCESSED_KEY));
        } catch (Exception e) {
            System.err.println("Ошибка подключения к Redis: " + e.getMessage());
        }
        return accounts;
    }


    public static void saveProcessedWallets(String account) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            jedis.sadd(PROCESSED_KEY, account);
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунта в Redis: " + e.getMessage());
        }
    }

    public static void saveProcessedWalletsWithTime(String account) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            long timestamp = System.currentTimeMillis() / 1000; // Текущий Unix Timestamp
            jedis.zadd(PROCESSED_KEY, timestamp, account); // Добавляем аккаунт с временной меткой
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунта с меткой времени: " + e.getMessage());
        }
    }

    public static boolean isWalletProcessed(String account) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            return jedis.sismember(PROCESSED_KEY, account);
        } catch (Exception e) {
            System.err.println("Ошибка проверки аккаунта в Redis: " + e.getMessage());
        }
        return false;
    }

    public static void removeProcessedWallets(String account) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            jedis.srem(PROCESSED_KEY, account);
        } catch (Exception e) {
            System.err.println("Ошибка удаления аккаунта из Redis: " + e.getMessage());
        }
    }

    public static void clearProcessedWallets() {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            jedis.del(PROCESSED_KEY);
        } catch (Exception e) {
            System.err.println("Ошибка очистки обработанных аккаунтов в Redis: " + e.getMessage());
        }
    }
}
