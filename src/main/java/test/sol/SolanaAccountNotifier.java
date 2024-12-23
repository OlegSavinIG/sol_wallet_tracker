package test.sol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.wallettracker.AccountSubscriptionService;
import test.sol.wallettracker.SolanaWebSocketListener;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;

public class SolanaAccountNotifier {

    private static final Logger logger = LoggerFactory.getLogger(SolanaAccountNotifier.class);
    public static final String WSS_PROVIDER_URL = "wss://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";
    public static final List<String> ACCOUNT_ADDRESSES = List.of(
            "BnjdLi46v7yeFgbihCwepdDfGozpF6PAhNVnUGz9aM1x",
            "726jN6T9SRf3q6UFb71vcRodQobaPb2sHLsMzs55gZkk",
            "HynDvCWAzvPMS4pHQFqBb6CGoFrX8AvZMdyEtQ46zEXk",
            "J1T4XuoLm97mMxLZvSse12AYCTprgCTEsMLDeGh2dHcM"
    );

    public static void main(String[] args) {
        logger.info("🔔 Starting Solana Account Notifier...");

        HttpClient client = HttpClient.newHttpClient();
        WebSocket webSocket = client.newWebSocketBuilder()
                .buildAsync(URI.create(WSS_PROVIDER_URL), new SolanaWebSocketListener())
                .join();

        AccountSubscriptionService subscriptionService = new AccountSubscriptionService(webSocket);
        subscriptionService.subscribeToAddresses(ACCOUNT_ADDRESSES);

        while (true) {
            subscriptionService.subscribeToNewWallets();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("❌ Main thread interrupted: {}", e.getMessage(), e);
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}







