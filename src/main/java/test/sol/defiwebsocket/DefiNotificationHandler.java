package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;
import test.sol.defiwebsocket.queueprocessor.UnsubscribeWalletsQueue;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.redis.NotActivatedWalletsRedis;
import test.sol.redis.SignatureRedis;
import test.sol.service.signature.SignatureService;
import test.sol.service.signature.SignatureServiceImpl;
import test.sol.telegram.TelegramInformationMessageHandler;
import test.sol.utils.ClientFactory;
import test.sol.utils.ConfigLoader;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DefiNotificationHandler {
    public static final String WSS_PROVIDER_URL = ConfigLoader.getString("SECOND_WSS_PROVIDER_URL");
    private final SignatureClient signatureClient = ClientFactory.createSignatureClient(WSS_PROVIDER_URL);
    private final TransactionClient transactionClient = ClientFactory.createTransactionClient(WSS_PROVIDER_URL);
    private final SignatureService signatureService = new SignatureServiceImpl();
    private final ConcurrentHashMap<String, Long> lastProcessedTime = new ConcurrentHashMap<>();
    private static final long MIN_PROCESS_INTERVAL_MS = 3500;
    private static final List<String> DEFI_URLS = ConfigLoader.getList("DEFI_URLS_FOR_SUBSCRIPTION");
    private static final List<String> UNSUB_URLS = ConfigLoader.getList("UNSUB_URLS_FOR_SUBSCRIPTION");
    private static final Logger logger = LoggerFactory.getLogger(DefiNotificationHandler.class);

    public void handleNotification(Integer subscription) {
        String wallet = SubscriptionWebSocketStorage.getWalletBySubscription(subscription);
        if (wallet == null) {
            logger.warn("No wallet found for subscription: {}", subscription);
            return;
        }
        if (!canProcessWallet(wallet)) {
            logger.info("Skipping notification for wallet: {} due to throttling.", wallet);
            return;
        }
        try {
            logger.info("Start processing wallet: {}", wallet);
            processWallet(wallet);
        } catch (IOException e) {
            logger.error("Error processing wallet: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void processWallet(String wallet) throws IOException, InterruptedException {
        Set<String> signatures = getSignaturesAfterValidation(wallet);
        logger.info("Signatures before validation {}", signatures.size());
        if (signatures.isEmpty()) {
            Thread.sleep(500);
            signatures = getSignaturesAfterValidation(wallet);
            logger.info("Signatures second try {}", signatures.size());
        }
        for (String signature : signatures) {
            Thread.sleep(500);
            logger.info("Validating transactions for wallet {} with signature {}", wallet, signature);
            TransactionResponse transaction = transactionClient.getSingleTransaction(signature);
            String logMessage = transaction.result().meta().logMessages().toString();
            logger.info("Validation finished");
            if (containsUnsubUrl(logMessage)) {
                logger.warn("Wallet log contains unsub url {}", wallet);
                UnsubscribeWalletsQueue.addWallet(wallet);
                NotActivatedWalletsRedis.remove(List.of(wallet));
                break;
            }
            if (containsDefiUrl(logMessage)) {
                TelegramInformationMessageHandler.sendToTelegram("Wallet activated: https://gmgn.ai/sol/address/" + wallet);
                UnsubscribeWalletsQueue.addWallet(wallet);
                NotActivatedWalletsRedis.remove(List.of(wallet));
                break;
            }
        }
    }

    private Set<String> getSignaturesAfterValidation(String wallet) throws InterruptedException, IOException {
        Thread.sleep(1800);
        SignaturesResponse signaturesResponse = signatureClient.getSignaturesForOneWallet(wallet, 10);

        Set<String> signatures = signatureService.validateSignature(signaturesResponse);
        Set<String> signaturesFromRedis = SignatureRedis.loadWalletSignatures(wallet);
        signatures.removeAll(signaturesFromRedis);
        if (!signatures.isEmpty()) {
            SignatureRedis.saveWalletSignatures(signatures, wallet);
        }
        return signatures;
    }

    public void handleSubscribeNotification(int result, int id) {
        SubscriptionWebSocketStorage.addSubscriptionWithId(result, id);
    }

    private boolean containsDefiUrl(String logMessage) {
        return DEFI_URLS.stream().anyMatch(logMessage::contains);
    }

    private boolean containsUnsubUrl(String logMessage) {
        return UNSUB_URLS.stream().anyMatch(logMessage::contains);
    }

    private boolean canProcessWallet(String wallet) {
        long currentTime = System.currentTimeMillis();
        return lastProcessedTime.compute(wallet, (key, value) -> {
            if (value == null || (currentTime - value) >= MIN_PROCESS_INTERVAL_MS) {
                return currentTime;
            }
            return value;
        }) == currentTime;
    }
}
