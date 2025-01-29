package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.utils.WalletIdGenerator;

import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WalletsSubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(WalletsSubscriptionService.class);
    private WebSocket webSocket;
    private final Map<Integer, String> subscriptionMap = new ConcurrentHashMap<>();
public void setWebSocket(WebSocket webSocket) {
    this.webSocket = webSocket;
}
    public void subscribeToWallets(List<String> wallets) throws InterruptedException {
        if (!wallets.isEmpty()) {
            for (String wallet : wallets) {
                Thread.sleep(200);
                int id = WalletIdGenerator.getNextId();
                subscriptionMap.put(id, wallet);
                SubscriptionWebSocketStorage.addWalletWithId(id, wallet);
                String subscriptionMessage = createAccountSubscriptionMessage(wallet, id);
                webSocket.sendText(subscriptionMessage, true);
                logger.info("🔔 Subscribed to wallet: {} with subscription ID: {}", wallet, id);
            }
        }
    }

    public void subscribeToWallet(String wallet) throws InterruptedException {
        if (webSocket == null) {
            throw new IllegalStateException("WebSocket is not initialized.");
        }
        logger.info("Subscribe process activated with wallet {}", wallet);
        if (!SubscriptionWebSocketStorage.isContainsWallet(wallet)) {
            Thread.sleep(1000);
            int id = WalletIdGenerator.getNextId();
            subscriptionMap.put(id, wallet);
            SubscriptionWebSocketStorage.addWalletWithId(id, wallet);
            String subscriptionMessage = createAccountSubscriptionMessage(wallet, id);
            webSocket.sendText(subscriptionMessage, true);
            logger.info("🔔 Subscribed to wallet: {} with subscription ID: {}", wallet, id);
        } else {
            logger.info("⚠️ Wallet already subscribed: {}", wallet);
        }
    }

    public void unsubscribeFromWallet(String wallet) {
        if (SubscriptionWebSocketStorage.isContainsWallet(wallet)) {
            int subscription = SubscriptionWebSocketStorage.getSubscriptionByWallet(wallet);
            String unsubscribeMessage = createAccountUnsubscriptionMessage(subscription);
            SubscriptionWebSocketStorage.removeAll(wallet);
            webSocket.sendText(unsubscribeMessage, true);
            logger.info("❎ Unsubscribed from address: {} with subscription ID: {}", wallet, subscription);
        } else {
            logger.warn("⚠️ No subscription found for wallet: {}", wallet);
        }
    }

    private String createAccountSubscriptionMessage(String accountAddress, int id) {
        return String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"accountSubscribe\",\"params\":[\"%s\",{\"commitment\":\"confirmed\"}]}", id, accountAddress);
    }


    private String createAccountUnsubscriptionMessage(int subscriptionId) {
        return String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"accountUnsubscribe\",\"params\":[%d]}", subscriptionId, subscriptionId);
    }

}
