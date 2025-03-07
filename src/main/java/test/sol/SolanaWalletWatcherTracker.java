package test.sol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import test.sol.redis.TrackWalletsRedis;
import test.sol.telegram.WalletWatcherTrackerBot;
import test.sol.utils.ConfigLoader;
import test.sol.wallettracker.AccountSubscriptionService;
import test.sol.wallettracker.SubscriptionWalletStorage;
import test.sol.wallettracker.queuelistener.RemoveWalletProcessor;
import test.sol.wallettracker.SolanaWebSocketListener;
import test.sol.wallettracker.queuelistener.AddWalletProcessor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Set;

public class SolanaWalletWatcherTracker {

    private static final Logger logger = LoggerFactory.getLogger(SolanaWalletWatcherTracker.class);
    public static final String WSS_PROVIDER_URL = ConfigLoader.getString("WSS_PROVIDER_URL");
    public static void main(String[] args) throws TelegramApiException {
        logger.info("🔔 Starting Solana Account Notifier...");

        Set<String> wallets = TrackWalletsRedis.loadWallets();

        HttpClient client = HttpClient.newHttpClient();
        WebSocket webSocket = client.newWebSocketBuilder()
                .buildAsync(URI.create(WSS_PROVIDER_URL), new SolanaWebSocketListener())
                .join();

        AccountSubscriptionService subscriptionService = new AccountSubscriptionService(webSocket);
        subscriptionService.subscribeToWallets(wallets);

        AddWalletProcessor addWalletProcessor = new AddWalletProcessor(subscriptionService);
        RemoveWalletProcessor removeWalletProcessor = new RemoveWalletProcessor(subscriptionService);

        removeWalletProcessor.startProcessing();
        addWalletProcessor.startProcessing();

        WalletWatcherTrackerBot bot = new WalletWatcherTrackerBot();
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            addWalletProcessor.stopProcessing();
            removeWalletProcessor.stopProcessing();

            TrackWalletsRedis.saveWallets(SubscriptionWalletStorage.getAllWallets());
            logger.info("✅ Application stopped.");
        }));
    }
}







