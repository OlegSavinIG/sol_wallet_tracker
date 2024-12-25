package test.sol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import test.sol.redis.TrackWalletsRedis;
import test.sol.telegram.WalletTrackerBot;
import test.sol.wallettracker.AccountSubscriptionService;
import test.sol.wallettracker.SubscriptionWalletStorage;
import test.sol.wallettracker.queuelistener.RemoveWalletProcessor;
import test.sol.wallettracker.SolanaWebSocketListener;
import test.sol.wallettracker.queuelistener.AddWalletProcessor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Set;

public class SolanaAccountNotifier {

    private static final Logger logger = LoggerFactory.getLogger(SolanaAccountNotifier.class);
    public static final String WSS_PROVIDER_URL = "wss://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";

    public static void main(String[] args) throws TelegramApiException {
        logger.info("🔔 Starting Solana Account Notifier...");

        Set<String> wallets = TrackWalletsRedis.loadWallets();

        HttpClient client = HttpClient.newHttpClient();
        WebSocket webSocket = client.newWebSocketBuilder()
                .buildAsync(URI.create(WSS_PROVIDER_URL), new SolanaWebSocketListener())
                .join();

        AccountSubscriptionService subscriptionService = new AccountSubscriptionService(webSocket);
        subscriptionService.subscribeToAddresses(wallets);

        AddWalletProcessor addWalletProcessor = new AddWalletProcessor(subscriptionService);
        RemoveWalletProcessor removeWalletProcessor = new RemoveWalletProcessor(subscriptionService);

        removeWalletProcessor.startProcessing();
        addWalletProcessor.startProcessing();

        WalletTrackerBot bot = new WalletTrackerBot();
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







