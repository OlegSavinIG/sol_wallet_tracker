package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.utils.ConfigLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WebSocketManager {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);

    private static final String WSS_PROVIDER_URL = ConfigLoader.getString("SECOND_WSS_PROVIDER_URL");
//    private static final String WSS_PROVIDER_URL = "wss://attentive-dimensional-needle.solana-mainnet.quiknode.pro/dc0abb602a7a6e28b6c7e69eb336b565e8709d2a";
    private static final int RECONNECT_DELAY_SECONDS = 5;

    private WebSocket webSocket;
    private final HttpClient httpClient;
    private final List<String> wallets;
    private final WalletsSubscriptionService subscriptionService;

    public WebSocketManager(List<String> wallets, WalletsSubscriptionService subscriptionService) {
        this.httpClient = HttpClient.newHttpClient();
        this.wallets = wallets;
        this.subscriptionService = subscriptionService; // Сервис подписки
    }

    public void connect() {
        logger.info("🔗 Connecting to WebSocket...");

        CompletableFuture<WebSocket> futureWebSocket = httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(WSS_PROVIDER_URL), new DefiWebSocketListener(this));

        futureWebSocket.thenAccept(ws -> {
            this.webSocket = ws;
            logger.info("✅ WebSocket connected successfully.");
            restoreSubscriptions(); // Восстанавливаем подписки
        }).exceptionally(ex -> {
            logger.error("❌ Failed to connect: {}", ex.getMessage());
            scheduleReconnect();
            return null;
        });
    }

    public void restoreSubscriptions() {
        if (webSocket != null && wallets != null) {
            logger.info("🔄 Restoring subscriptions for wallets...");
            subscriptionService.setWebSocket(webSocket); // Обновляем веб-сокет в сервисе подписки
            try {
                subscriptionService.subscribeToWallets(wallets); // Подписываемся заново
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void reconnect() {
        logger.info("🔄 Reconnecting to WebSocket...");
        connect();
    }

    public void scheduleReconnect() {
        logger.info("⏳ Scheduling reconnect in {} seconds...", RECONNECT_DELAY_SECONDS);
        try {
            TimeUnit.SECONDS.sleep(RECONNECT_DELAY_SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        reconnect();
    }

    public void close() {
        if (webSocket != null) {
            webSocket.abort();
            logger.info("🔌 WebSocket connection closed.");
        }
    }
}

