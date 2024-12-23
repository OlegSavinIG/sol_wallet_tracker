package test.sol.wallettracker;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.SolanaAccountNotifier;
import test.sol.pojo.notification.Params;
import test.sol.pojo.notification.RpcResponse;
import test.sol.pojo.notification.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class SolanaWebSocketListener implements WebSocket.Listener {
    private static final Logger logger = LoggerFactory.getLogger(SolanaWebSocketListener.class);
    private final NotificationHandler notificationHandler = new NotificationHandler();
    private static final Gson gson = new Gson();
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 2000;
    private int reconnectAttempts = 0;

    @Override
    public void onOpen(WebSocket webSocket) {
        logger.info("✅ WebSocket connection reopened.");
        reconnectAttempts = 0; // Reset reconnect attempts on successful connection
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        logger.info("🔄 Change detected: {}", data);

        try {
            RpcResponse response = gson.fromJson(data.toString(), RpcResponse.class);
            if (response != null && response.getParams() != null) {
                Params params = response.getParams();
                if (params.getResult() != null && params.getResult().getValue() != null) {
                    Value value = params.getResult().getValue();
                    String address = value.getOwner();
                    notificationHandler.handleNotification(address, value);
                }
            }
        } catch (JsonSyntaxException e) {
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

