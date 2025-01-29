package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.defiwebsocket.queueprocessor.NotActivatedWalletsProcessor;
import test.sol.defiwebsocket.queueprocessor.UnsubscribeWalletProcessor;
import test.sol.redis.NotActivatedWalletsRedis;

import java.util.List;

public class SolanaDefiWebSocketValidator {
    private static final Logger logger = LoggerFactory.getLogger(SolanaDefiWebSocketValidator.class);

    public static void main(String[] args) {
        logger.info("🔔 Starting SolanaDefiWebSocketValidator...");

        List<String> wallets = NotActivatedWalletsRedis.load();
        WalletsSubscriptionService subscriptionService = new WalletsSubscriptionService();
        WebSocketManager webSocketManager = new WebSocketManager(wallets, subscriptionService);


        NotActivatedWalletsProcessor walletsProcessor = new NotActivatedWalletsProcessor(subscriptionService);
        UnsubscribeWalletProcessor unsubscribeWalletProcessor = new UnsubscribeWalletProcessor(subscriptionService);
        unsubscribeWalletProcessor.startProcessing();
        walletsProcessor.startProcessing();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            walletsProcessor.stopProcessing();
            unsubscribeWalletProcessor.stopProcessing();
            webSocketManager.close();
            logger.info("✅ Application stopped.");
        }));
        webSocketManager.connect();
    }
}
