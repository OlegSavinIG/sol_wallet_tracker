package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;
import test.sol.pojo.signature.SignatureResponseResult;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.redis.NotActivatedWalletsRedis;
import test.sol.redis.SignatureRedis;
import test.sol.telegram.TelegramMessageHandler;
import test.sol.utils.ClientFactory;
import test.sol.wallettracker.NotificationHandler;
import test.sol.wallettracker.SubscriptionWalletStorage;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DefiNotificationHandler {
    private final SignatureClient signatureClient = ClientFactory.createSignatureClient();
    private final TransactionClient transactionClient = ClientFactory.createTransactionClient();
    private final ConcurrentHashMap<String, Long> lastProcessedTime = new ConcurrentHashMap<>();
    private static final long MIN_PROCESS_INTERVAL_MS = 5000; // Минимальный интервал обработки (5 секунд)

    private static final List<String> DEFI_URLS = List.of(
//            "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4",
//            "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8",
            "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
    );
    private static final Logger logger = LoggerFactory.getLogger(NotificationHandler.class);

    public void handleNotification(Integer subscription) {
        String wallet = SubscriptionWalletStorage.getWalletBySubscription(subscription);
        if (wallet == null) {
            logger.warn("No wallet found for subscription: {}", subscription);
            return;
        }

        long currentTime = System.currentTimeMillis();
        Long lastTime = lastProcessedTime.get(wallet);

        // Проверяем, прошло ли достаточно времени с последней обработки
        if (lastTime != null && (currentTime - lastTime) < MIN_PROCESS_INTERVAL_MS) {
            logger.info("Skipping notification for wallet: {} due to throttling", wallet);
            return;
        }

        // Обновляем время последней обработки
        lastProcessedTime.put(wallet, currentTime);

        try {
            logger.info("Start processing wallet: {}", wallet);
            processWallet(wallet);
        } catch (Exception e) {
            logger.error("Error processing wallet: {}", e.getMessage(), e);
        }
    }

    private void processWallet(String wallet) throws IOException {
        SignaturesResponse signaturesResponse = signatureClient.getSignaturesForOneWallet(wallet, 10);

        List<SignatureResponseResult> result = signaturesResponse.result();
        Set<String> signatures = result.stream()
                .map(SignatureResponseResult::signature)
                .collect(Collectors.toSet());

        Set<String> signaturesFromRedis = SignatureRedis.loadWalletSignatures(wallet);
        signatures.removeAll(signaturesFromRedis);
        SignatureRedis.saveWalletSignatures(signatures, wallet);

        outer:
        for (String signature : signatures) {
            logger.info("Validating transactions for wallet {} ", wallet);
            TransactionResponse transaction = transactionClient.getSingleTransaction(signature);
            String logMessage = transaction.result().meta().logMessages().toString();
            for (String defiUrl : DEFI_URLS) {
                if (logMessage.contains(defiUrl)) {
                    TelegramMessageHandler.sendToTelegram("Wallet activated: " + wallet);
                    UnsubscribeWalletsQueue.addWallet(wallet);
                    NotActivatedWalletsRedis.remove(List.of(wallet));
                    break outer;
                }
            }
        }
    }

    public void handleSubscribeNotification(int result, int id) {
        SubscriptionWalletStorage.addSubscriptionWithId(result, id);
    }
}
