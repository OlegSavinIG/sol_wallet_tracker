package test.sol.wallettracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SubscriptionWalletStorage {
    private static final Map<Integer, String> idWalletMap = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> subscriptionIdMap = new ConcurrentHashMap<>();
    private static final Map<String, Integer> walletSubscriptionMap = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionWalletStorage.class);
    public static Set<String> getAllWallets(){
       return new HashSet<>(walletSubscriptionMap.keySet());
    }
    public static boolean isContainsWallet(String wallet) {
        return idWalletMap.containsValue(wallet);
    }

    public static void addWalletWithId(int id, String wallet) {
        logger.info("wallet added {}", wallet);
        idWalletMap.put(id, wallet);
    }

    public static void removeAll(String wallet) {
        Integer subscription = walletSubscriptionMap.get(wallet);
        walletSubscriptionMap.remove(wallet);
        Integer id = subscriptionIdMap.get(subscription);
        subscriptionIdMap.remove(id);
        idWalletMap.remove(id);
    }

    public static String getWalletWithId(int id) {
        return idWalletMap.get(id);
    }

    public static void removeWalletWithId(int id) {
        idWalletMap.remove(id);
    }

    public static int getSubscriptionByWallet(String wallet) {
        return walletSubscriptionMap.get(wallet);
    }

    public static void removeWalletWithSubscription(String wallet) {
        walletSubscriptionMap.remove(wallet);
    }

    public static void addSubscriptionWithId(int subscription, int id) {
        logger.info("Added subs ID to map {} with ID {}", subscription, id);
        subscriptionIdMap.put(subscription, id);
        walletSubscriptionMap.put(idWalletMap.get(id), subscription);
    }

    public static Integer getSubscriptionWithId(int id) {
        return subscriptionIdMap.get(id);
    }

    public static void removeSubscriptionWithId(int id) {
        subscriptionIdMap.remove(id);
    }

    public static String getWalletBySubscription(int subscription) {
        return idWalletMap.get(subscriptionIdMap.get(subscription));
    }
}
