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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DefiNotificationHandler {
    public static final String WSS_PROVIDER_URL = "wss://attentive-dimensional-needle.solana-mainnet.quiknode.pro/dc0abb602a7a6e28b6c7e69eb336b565e8709d2a";
    private final SignatureClient signatureClient = ClientFactory.createSignatureClient(WSS_PROVIDER_URL);
    private final TransactionClient transactionClient = ClientFactory.createTransactionClient(WSS_PROVIDER_URL);
    private final SignatureService signatureService = new SignatureServiceImpl();
    private final ConcurrentHashMap<String, Long> lastProcessedTime = new ConcurrentHashMap<>();
    private static final long MIN_PROCESS_INTERVAL_MS = 3500; // Минимальный интервал обработки (5 секунд)

    private static final List<String> DEFI_URLS = List.of(
//            "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4",
//            "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8",
            "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
    );
    private static final List<String> UNSUB_URLS = List.of(
            "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4",
            "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8"
//            "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
    );
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
        Thread.sleep(1200);
        SignaturesResponse signaturesResponse = signatureClient.getSignaturesForOneWallet(wallet, 10);

        Set<String> signatures = signatureService.validateSignature(signaturesResponse);
        Set<String> signaturesFromRedis = SignatureRedis.loadWalletSignatures(wallet);
        signatures.removeAll(signaturesFromRedis);
        SignatureRedis.saveWalletSignatures(signatures, wallet);
        logger.info("Signatures before validation {}", signatures.size());

        for (String signature : signatures) {
            Thread.sleep(500);
            logger.info("Validating transactions for wallet {} with signature {}", wallet, signature);
            TransactionResponse transaction = transactionClient.getSingleTransaction(signature);
            String logMessage = transaction.result().meta().logMessages().toString();
            logger.info("Validation finished");
            if (containsDefiUrl(logMessage)) {
                TelegramInformationMessageHandler.sendToTelegram("Wallet activated: https://gmgn.ai/sol/address/" + wallet);
                UnsubscribeWalletsQueue.addWallet(wallet);
                NotActivatedWalletsRedis.remove(List.of(wallet));
                break;
            }
            if (containsUnsubUrl(logMessage)) {
                logger.warn("Wallet log contains unsub url {}", wallet);
                UnsubscribeWalletsQueue.addWallet(wallet);
                NotActivatedWalletsRedis.remove(List.of(wallet));
                break;
            }
        }
    }

//    private void processWallet(String wallet) throws IOException, InterruptedException {
//        Thread.sleep(1500);
//        SignaturesResponse signaturesResponse = signatureClient.getSignaturesForOneWallet(wallet, 10);
//
//        // Извлечение уникальных подписей
//        Set<String> signatures = signatureService.validateSignature(signaturesResponse);
//        Set<String> signaturesFromRedis = SignatureRedis.loadWalletSignatures(wallet);
//
//        logger.info("Signatures before Redis filtering: {}", signatures.size());
//
//        // Использование Stream API для фильтрации, сохранения и обработки подписей
//        signatures.stream()
//                .filter(signature -> !signaturesFromRedis.contains(signature)) // Фильтрация новых подписей
//                .peek(signature -> SignatureRedis.saveWalletSignatures(Set.of(signature), wallet)) // Сохранение новых подписей в Redis
//                .forEach(signature -> validateAndProcessSignature(wallet, signature)); // Обработка подписей
//
//        logger.info("Processing completed for wallet: {}", wallet);
//    }

    public void handleSubscribeNotification(int result, int id) {
        SubscriptionWebSocketStorage.addSubscriptionWithId(result, id);
    }

//    private void validateAndProcessSignature(String wallet, String signature) {
//        try {
//            logger.info("Validating transaction for wallet {} with signature {}", wallet, signature);
//            Thread.sleep(500);
//            TransactionResponse transaction = transactionClient.getSingleTransaction(signature);
//
//            String logMessage = transaction.result().meta().logMessages().toString();
//
//            if (containsDefiUrl(logMessage)) {
//                logger.info("DeFi URL found in wallet {} log, notifying and unsubscribing.", wallet);
//                TelegramInformationMessageHandler.sendToTelegram("Wallet activated: " + wallet);
//                UnsubscribeWalletsQueue.addWallet(wallet);
//                NotActivatedWalletsRedis.remove(List.of(wallet));
//            } else if (containsUnsubUrl(logMessage)) {
//                logger.warn("Unsubscribe URL found in wallet {} log.", wallet);
//                UnsubscribeWalletsQueue.addWallet(wallet);
//                NotActivatedWalletsRedis.remove(List.of(wallet));
//            }
//        } catch (IOException e) {
//            logger.error("Error fetching transaction for signature {}: {}", signature, e.getMessage(), e);
//        } catch (Exception e) {
//            logger.error("Unexpected error processing signature {}: {}", signature, e.getMessage(), e);
//        }
//    }

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
