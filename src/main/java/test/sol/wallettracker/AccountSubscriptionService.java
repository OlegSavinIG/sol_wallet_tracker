package test.sol.wallettracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AccountSubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(AccountSubscriptionService.class);
    private final WebSocket webSocket;
    private final Map<Integer, String> subscriptionMap = new ConcurrentHashMap<>();
    private final AtomicInteger subscriptionIdCounter = new AtomicInteger(1);

    public AccountSubscriptionService(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public void subscribeToAddresses(List<String> addresses) {
        for (String address : addresses) {
            int subscriptionId = subscriptionIdCounter.getAndIncrement();
            subscriptionMap.put(subscriptionId, address);
            String subscriptionMessage = createAccountSubscriptionMessage(address, subscriptionId);
            webSocket.sendText(subscriptionMessage, true);
            logger.info("🔔 Subscribed to address: {} with subscription ID: {}", address, subscriptionId);
        }
    }
    public void subscribeToNewWallets() {
        WalletStorage.getWallets().forEach(address -> {
            if (!subscriptionMap.containsValue(address)) {
                int subscriptionId = subscriptionIdCounter.getAndIncrement();
                subscriptionMap.put(subscriptionId, address);
                String subscriptionMessage = createAccountSubscriptionMessage(address, subscriptionId);
                webSocket.sendText(subscriptionMessage, true);
                logger.info("🔔 Subscribed to new address: {} with subscription ID: {}", address, subscriptionId);
            }
        });
    }

    private String createAccountSubscriptionMessage(String accountAddress, int id) {
        return String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"accountSubscribe\",\"params\":[\"%s\",{\"commitment\":\"confirmed\"}]}", id, accountAddress);
    }

    public void unsubscribeFromAddress(int subscriptionId) {
        String address = subscriptionMap.remove(subscriptionId);
        if (address != null) {
            String unsubscribeMessage = createAccountUnsubscriptionMessage(subscriptionId);
            webSocket.sendText(unsubscribeMessage, true);
            logger.info("❎ Unsubscribed from address: {} with subscription ID: {}", address, subscriptionId);
        } else {
            logger.warn("⚠️ No subscription found for ID: {}", subscriptionId);
        }
    }

    private String createAccountUnsubscriptionMessage(int subscriptionId) {
        return String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"accountUnsubscribe\",\"params\":[%d]}", subscriptionId, subscriptionId);
    }
}


