package test.sol.wallettracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AccountSubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(AccountSubscriptionService.class);
    private final WebSocket webSocket;
    private final Map<Integer, String> subscriptionMap = new ConcurrentHashMap<>();
    private final AtomicInteger walletId = new AtomicInteger(1);

    public AccountSubscriptionService(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public void subscribeToAddresses(Set<String> wallets) {
        for (String wallet : wallets) {
            int id = walletId.getAndIncrement();
            subscriptionMap.put(id, wallet);
            SubscriptionWalletStorage.addWalletWithId(id, wallet);
            String subscriptionMessage = createAccountSubscriptionMessage(wallet, id);
            webSocket.sendText(subscriptionMessage, true);
            logger.info("🔔 Subscribed to wallet: {} with subscription ID: {}", wallet, id);
        }
    }
    public void subscribeToWallet(String wallet) {
        logger.info("Subscribe process activated with wallet {}", wallet);
        if (!SubscriptionWalletStorage.isContainsWallet(wallet)) {
            int id = walletId.getAndIncrement();
            subscriptionMap.put(id, wallet);
            SubscriptionWalletStorage.addWalletWithId(id, wallet);
            String subscriptionMessage = createAccountSubscriptionMessage(wallet, id);
            webSocket.sendText(subscriptionMessage, true);
            logger.info("🔔 Subscribed to wallet: {} with subscription ID: {}", wallet, id);
        } else {
            logger.info("⚠️ Wallet already subscribed: {}", wallet);
        }
    }


    private String createAccountSubscriptionMessage(String accountAddress, int id) {
        return String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"accountSubscribe\",\"params\":[\"%s\",{\"commitment\":\"confirmed\"}]}", id, accountAddress);
    }

    public void unsubscribeFromAddress(String wallet) {
        if (SubscriptionWalletStorage.isContainsWallet(wallet)) {
            int subscription = SubscriptionWalletStorage.getSubscriptionByWallet(wallet);
            String unsubscribeMessage = createAccountUnsubscriptionMessage(subscription);
            SubscriptionWalletStorage.removeAll(wallet);
            webSocket.sendText(unsubscribeMessage, true);
            logger.info("❎ Unsubscribed from address: {} with subscription ID: {}", wallet, subscription);
        } else {
            logger.warn("⚠️ No subscription found for wallet: {}", wallet);
        }
    }

    private String createAccountUnsubscriptionMessage(int subscriptionId) {
        return String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"accountUnsubscribe\",\"params\":[%d]}", subscriptionId, subscriptionId);
    }
}


