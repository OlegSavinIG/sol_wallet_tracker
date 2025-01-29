package test.sol.defiwebsocket;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.defiwebsocket.queueprocessor.UnsubscribeWalletsQueue;
import test.sol.pojo.notification.RpcResponse;
import test.sol.redis.NotActivatedWalletsRedis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DefiWebSocketListener implements WebSocket.Listener {
    private static final Logger logger = LoggerFactory.getLogger(DefiWebSocketListener.class);
    private final DefiNotificationHandler notificationHandler = new DefiNotificationHandler();
    private final Map<Integer, AtomicInteger> subscriptionCounter = new ConcurrentHashMap<>();
    private static final int NOTIFICATION_THRESHOLD = 5;
    private static final Gson gson = new Gson();
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 2000;
    private static final int MAX_RECONNECT_DELAY_MS = 30000;
    private int reconnectAttempts = 0;
    private final WebSocketManager webSocketManager;
    public DefiWebSocketListener(WebSocketManager webSocketManager) {
        this.webSocketManager = webSocketManager;
    }
    @Override
    public void onOpen(WebSocket webSocket) {
        logger.info("✅ WebSocket connection reopened.");
        reconnectAttempts = 0;
        webSocketManager.restoreSubscriptions();
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
                subscriptionCounter.compute(subscription, (key, counter) -> {
                    if (counter == null) {
                        return new AtomicInteger(1); // Если ключ отсутствует, создаем новый счетчик
                    } else {
                        int newValue = counter.incrementAndGet();
                        if (newValue > NOTIFICATION_THRESHOLD) {
                            logger.warn("⚠️ Too many notifications for subscription={}, unsubscribing...", subscription);
                            String wallet = SubscriptionWebSocketStorage.getWalletBySubscription(subscription);
                            UnsubscribeWalletsQueue.addWallet(wallet);
                            NotActivatedWalletsRedis.remove(List.of(wallet));
                            return null; // Удаляем ключ, чтобы остановить дальнейшие уведомления
                        }
                        return counter; // Обновляем счетчик
                    }
                });

                AtomicInteger updatedCounter = subscriptionCounter.get(subscription);
                if (updatedCounter != null) {
                    int count = updatedCounter.get();
                    logger.info("🔔 Notification received: subscription={}, count={}", subscription, count);
                    notificationHandler.handleNotification(subscription); // Вызов обработчика
                }
            } else {
                logger.warn("⚠️ Unexpected JSON structure: {}", data);
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            logger.error("❌ Error parsing JSON: {}", e.getMessage());
        }

        webSocket.request(1);
        return WebSocket.Listener.super.onText(webSocket, data, last); // Указывает, что обработка завершена;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        logger.error("❌ WebSocket error: {}", error.getMessage(), error);
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing due to error").thenRun(() -> {
            logger.info("✅ WebSocket closed gracefully.");
            webSocketManager.scheduleReconnect(); // Переподключаемся
        });
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        logger.error("❎ WebSocket closed. Status: {}, Reason: {}", statusCode, reason);
        webSocketManager.scheduleReconnect();
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }
}
