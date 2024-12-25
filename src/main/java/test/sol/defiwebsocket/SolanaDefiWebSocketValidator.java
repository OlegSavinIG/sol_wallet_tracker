package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import test.sol.SolanaAccountNotifier;
import test.sol.redis.NotActivatedWalletsRedis;
import test.sol.redis.TrackWalletsRedis;
import test.sol.telegram.WalletTrackerBot;
import test.sol.wallettracker.AccountSubscriptionService;
import test.sol.wallettracker.SolanaWebSocketListener;
import test.sol.wallettracker.SubscriptionWalletStorage;
import test.sol.wallettracker.queuelistener.RemoveWalletProcessor;
import test.sol.wallettracker.queuelistener.WalletProcessor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Set;

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
        subscriptionService.subscribeToAddresses(wallets);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            logger.info("✅ Application stopped.");
        }));
    }
}
