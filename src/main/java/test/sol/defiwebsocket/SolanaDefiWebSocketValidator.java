package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.redis.NotActivatedWalletsRedis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;

public class SolanaDefiWebSocketValidator {
    private static final Logger logger = LoggerFactory.getLogger(SolanaDefiWebSocketValidator.class);
    public static final String WSS_PROVIDER_URL = "wss://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";

    public static void main(String[] args) {
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
