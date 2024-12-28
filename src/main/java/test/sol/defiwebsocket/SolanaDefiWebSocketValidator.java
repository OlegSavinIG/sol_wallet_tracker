package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.defiwebsocket.queueprocessor.NotActivatedWalletsProcessor;
import test.sol.defiwebsocket.queueprocessor.UnsubscribeWalletProcessor;
import test.sol.redis.NotActivatedWalletsRedis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;

public class SolanaDefiWebSocketValidator {
    private static final Logger logger = LoggerFactory.getLogger(SolanaDefiWebSocketValidator.class);
    public static final String WSS_PROVIDER_URL = "wss://attentive-dimensional-needle.solana-mainnet.quiknode.pro/dc0abb602a7a6e28b6c7e69eb336b565e8709d2a";

    public static void main(String[] args) throws InterruptedException {
        logger.info("🔔 Starting SolanaDefiWebSocketValidator...");

        List<String> wallets = NotActivatedWalletsRedis.load();

        HttpClient client = HttpClient.newHttpClient();
        WebSocket webSocket = client.newWebSocketBuilder()
                .buildAsync(URI.create(WSS_PROVIDER_URL), new DefiSolanaWebSocketListener())
                .join();

        WalletsSubscriptionService subscriptionService = new WalletsSubscriptionService(webSocket);
        subscriptionService.subscribeToWallets(wallets);

        NotActivatedWalletsProcessor walletsProcessor = new NotActivatedWalletsProcessor(subscriptionService);
        UnsubscribeWalletProcessor unsubscribeWalletProcessor = new UnsubscribeWalletProcessor(subscriptionService);
        unsubscribeWalletProcessor.startProcessing();
        walletsProcessor.startProcessing();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            walletsProcessor.stopProcessing();
            unsubscribeWalletProcessor.stopProcessing();
            logger.info("✅ Application stopped.");
        }));
    }
}
