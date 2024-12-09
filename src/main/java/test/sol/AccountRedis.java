package test.sol;

import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccountRedis {

    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;
    private static final String PROCESSED_KEY = "processed_accounts";
    private static final String SAVED_WALLETS_KEY = "saved_wallets";
    private static final String SIGNATURES_KEY = "signature_";
    private static final String CONFIRMED_WALLET_KEY = "confirmed_wallets";


    public static Set<String> loadProcessedAccounts() {
        Set<String> accounts = new HashSet<>();
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            accounts.addAll(jedis.smembers(PROCESSED_KEY));
        } catch (Exception e) {
            System.err.println("Ошибка подключения к Redis: " + e.getMessage());
        }
        return accounts;
    }

    public static Set<String> loadWalletSignatures(String wallet) {
        Set<String> signatures = new HashSet<>();
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            signatures.addAll(jedis.smembers(SIGNATURES_KEY + wallet));
        } catch (Exception e) {
            System.err.println("Ошибка подключения к Redis: " + e.getMessage());
        }
        return signatures;
    }

    public static void saveWalletSignatures(List<String> signatures, String wallet) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String signature : signatures) {
                jedis.sadd(SIGNATURES_KEY + wallet, signature);
            }
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунтов в Redis: " + e.getMessage());
        }
    }

    public static void saveSavedWallets(List<String> newAccounts) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String account : newAccounts) {
                jedis.rpush(SAVED_WALLETS_KEY, account);
            }
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунтов в Redis: " + e.getMessage());
        }
    }
    public static void saveConfirmedWallets(List<String> confirmed) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String account : confirmed) {
                jedis.rpush(CONFIRMED_WALLET_KEY, account);
            }
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунтов в Redis: " + e.getMessage());
        }
    }

    public static void saveProcessedAccount(String account) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            jedis.sadd(PROCESSED_KEY, account);
        } catch (Exception e) {
            System.err.println("Ошибка сохранения аккаунта в Redis: " + e.getMessage());
        }
    }

    public static void saveProcessedAccountWithTime(String account) {
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

    public static void removeProcessedAccount(String account) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            jedis.srem(PROCESSED_KEY, account);
        } catch (Exception e) {
            System.err.println("Ошибка удаления аккаунта из Redis: " + e.getMessage());
        }
    }
    public static void removeSavedWallets(List<String> wallets) {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            for (String wallet : wallets) {
                jedis.srem(SAVED_WALLETS_KEY, wallet);
            }
        } catch (Exception e) {
            System.err.println("Ошибка удаления аккаунта из Redis: " + e.getMessage());
        }
    }

    public static void clearProcessedAccounts() {
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            jedis.del(PROCESSED_KEY);
        } catch (Exception e) {
            System.err.println("Ошибка очистки обработанных аккаунтов в Redis: " + e.getMessage());
        }
    }
}
