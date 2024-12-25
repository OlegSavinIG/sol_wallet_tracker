package test.sol.defiwebsocket;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.pojo.notification.RpcResponse;
import test.sol.wallettracker.NotificationHandler;
import test.sol.wallettracker.SolanaWebSocketListener;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class DefiSolanaWebSocketListener implements WebSocket.Listener {
    private static final Logger logger = LoggerFactory.getLogger(SolanaWebSocketListener.class);
    private final DefiNotificationHandler notificationHandler = new DefiNotificationHandler();
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
                logger.info("🔔 Notification received: subscription={}", subscription);
                notificationHandler.handleNotification(subscription);
            } else {
                logger.warn("⚠️ Unexpected JSON structure: {}", data);
            }
        } catch (JsonSyntaxException | IllegalStateException | IOException e) {
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
            logger.info("🔄 Attempting to reconnect... (Attempt {}/{})", reconnectAttempts, MAX_RECONNECT_ATTEMPTS);

            try {
                TimeUnit.MILLISECONDS.sleep(RECONNECT_DELAY_MS * reconnectAttempts);
            } catch (InterruptedException e) {
                logger.error("❌ Reconnection interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }

            HttpClient client = HttpClient.newHttpClient();
            client.newWebSocketBuilder()
                    .buildAsync(URI.create(test.sol.SolanaAccountNotifier.WSS_PROVIDER_URL), new SolanaWebSocketListener());
        } else {
            logger.error("❌ Maximum reconnect attempts reached. Unable to reconnect.");
        }
    }

}
