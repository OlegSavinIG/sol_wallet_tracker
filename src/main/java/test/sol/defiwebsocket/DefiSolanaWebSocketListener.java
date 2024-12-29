package test.sol.defiwebsocket;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.SolanaWalletTracker;
import test.sol.defiwebsocket.queueprocessor.UnsubscribeWalletsQueue;
import test.sol.pojo.notification.RpcResponse;
import test.sol.redis.NotActivatedWalletsRedis;
import test.sol.wallettracker.SolanaWebSocketListener;
import test.sol.wallettracker.SubscriptionWalletStorage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DefiSolanaWebSocketListener implements WebSocket.Listener {
    private static final Logger logger = LoggerFactory.getLogger(SolanaWebSocketListener.class);
    private final DefiNotificationHandler notificationHandler = new DefiNotificationHandler();
    private final Map<Integer, Integer> subscriptionCounter = new ConcurrentHashMap<>();
    private static final int NOTIFICATION_THRESHOLD = 5;
    private static final Gson gson = new Gson();
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 2000;
    private int reconnectAttempts = 0;

    @Override
    public void onOpen(WebSocket webSocket) {
        logger.info("✅ WebSocket connection reopened.");
        reconnectAttempts = 0;
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        logger.info("🔄 Change detected: {}", data);
        try {
            RpcResponse response = gson.fromJson(data.toString(), RpcResponse.class);
            if (response.getResult() != null && response.getId() != null) {
                int result = response.getResult();
                int id = response.getId();
                logger.info("✅ Specific response received: result={}, id={}", result, id);
                notificationHandler.handleSubscribeNotification(result, id);
            } else if (response.getParams() != null) {
                int subscription = response.getParams().getSubscription();
                subscriptionCounter.merge(subscription, 1, Integer::sum);
                int notificationCount = subscriptionCounter.get(subscription);
                logger.info("\uD83D\uDD14 Notification received: subscription={}, count={}",
                        subscription, notificationCount);

                if (notificationCount > NOTIFICATION_THRESHOLD) {
                    logger.warn("\u26A0\uFE0F Too many notifications for subscription={}, unsubscribing...", subscription);
                    String wallet = SubscriptionWalletStorage.getWalletBySubscription(subscription);
                    UnsubscribeWalletsQueue.addWallet(wallet);
                    NotActivatedWalletsRedis.remove(List.of(wallet));
                    subscriptionCounter.remove(subscription);
                } else {
                    notificationHandler.handleNotification(subscription);
                }
            } else {
                logger.warn("⚠️ Unexpected JSON structure: {}", data);
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            logger.error("❌ Error parsing JSON: {}", e.getMessage());
        }

        webSocket.request(1);
        return null;
    }


    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        logger.error("❌ WebSocket error: {}", error.getMessage(), error);
        reconnect();
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        logger.info("❎ WebSocket closed. Status: {}, Reason: {}", statusCode, reason);
        reconnect();
        return null;
    }

    private void reconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            long delay = (long) Math.pow(2, reconnectAttempts) * RECONNECT_DELAY_MS;
            logger.info("🔄 Reconnecting... Attempt {}/{} in {} ms", reconnectAttempts, MAX_RECONNECT_ATTEMPTS, delay);

            try {
                TimeUnit.MILLISECONDS.sleep(delay);
            } catch (InterruptedException e) {
                logger.error("Reconnection interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }

            connectWebSocket(); // Повторное подключение
        } else {
            logger.error("❌ Maximum reconnect attempts reached.");
        }
    }

    private void connectWebSocket() {
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(SolanaWalletTracker.WSS_PROVIDER_URL), this);
    }


}
