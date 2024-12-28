package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;
import test.sol.defiwebsocket.queueprocessor.UnsubscribeWalletsQueue;
import test.sol.pojo.signature.SignatureResponseResult;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.redis.NotActivatedWalletsRedis;
import test.sol.redis.SignatureRedis;
import test.sol.service.signature.SignatureService;
import test.sol.service.signature.SignatureServiceImpl;
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
    public static final String WSS_PROVIDER_URL = "wss://attentive-dimensional-needle.solana-mainnet.quiknode.pro/dc0abb602a7a6e28b6c7e69eb336b565e8709d2a";
    private final SignatureClient signatureClient = ClientFactory.createSignatureClient(WSS_PROVIDER_URL);
    private final TransactionClient transactionClient = ClientFactory.createTransactionClient(WSS_PROVIDER_URL);
    private final SignatureService signatureService = new SignatureServiceImpl();
    private final ConcurrentHashMap<String, Long> lastProcessedTime = new ConcurrentHashMap<>();
    private static final long MIN_PROCESS_INTERVAL_MS = 4000; // Минимальный интервал обработки (5 секунд)

    private static final List<String> DEFI_URLS = List.of(
//            "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4",
//            "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8",
            "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
    );
    private static final Logger logger = LoggerFactory.getLogger(DefiNotificationHandler.class);

    public void handleNotification(Integer subscription) {
        String wallet = SubscriptionWalletStorage.getWalletBySubscription(subscription);
        if (wallet == null) {
            logger.warn("No wallet found for subscription: {}", subscription);
            return;
        }

//        long currentTime = System.currentTimeMillis();
//        Long lastTime = lastProcessedTime.compute(wallet, (key, value) -> {
//            if (value == null || (currentTime - value) >= MIN_PROCESS_INTERVAL_MS) {
//                return currentTime; // Обновляем время обработки
//            } else {
//                return value; // Оставляем старое значение
//            }
//        });

        if (!canProcessWallet(wallet)) {
            logger.info("Skipping notification for wallet: {} due to throttling.", wallet);
            return;
        }


        // Обновляем время последней обработки
//        lastProcessedTime.put(wallet, currentTime);

        try {
            logger.info("Start processing wallet: {}", wallet);
            processWallet(wallet);
        } catch (IOException e) {
            logger.error("Error processing wallet: {}", e.getMessage(), e);
            // Возможно, добавить кошелек в очередь на повторную обработку
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void processWallet(String wallet) throws IOException, InterruptedException {
        SignaturesResponse signaturesResponse = signatureClient.getSignaturesForOneWallet(wallet, 10);

        List<SignatureResponseResult> result = signaturesResponse.result();

        Set<String> signatures = signatureService.validateSignature(signaturesResponse);
        if (result.isEmpty()){
            logger.info("Result with signatures is empty!!!");
            logger.info("Response {}", signaturesResponse);
        }
        Set<String> signaturesFromRedis = SignatureRedis.loadWalletSignatures(wallet);
        signatures.removeAll(signaturesFromRedis);
        SignatureRedis.saveWalletSignatures(signatures, wallet);
        logger.info("Signatures for validation size {}", signatures.size());
        for (String signature : signatures) {
            Thread.sleep(800);
            logger.info("Validating transactions for wallet {} with signature {}", wallet, signature);
            TransactionResponse transaction = transactionClient.getSingleTransaction(signature);
            String logMessage = transaction.result().meta().logMessages().toString();
            logger.info("Validation finished");
            if (containsDefiUrl(logMessage)) {
                TelegramMessageHandler.sendToTelegram("Wallet activated: " + wallet);
                UnsubscribeWalletsQueue.addWallet(wallet);
                NotActivatedWalletsRedis.remove(List.of(wallet));
                break ;
            }
        }
    }

    public void handleSubscribeNotification(int result, int id) {
        SubscriptionWalletStorage.addSubscriptionWithId(result, id);
    }
    private boolean containsDefiUrl(String logMessage) {
        return DEFI_URLS.stream().anyMatch(logMessage::contains);
    }
    private boolean canProcessWallet(String wallet) {
        long currentTime = System.currentTimeMillis();
        return lastProcessedTime.compute(wallet, (key, value) -> {
            if (value == null || (currentTime - value) >= MIN_PROCESS_INTERVAL_MS) {
                return currentTime; // Обновляем время
            }
            return value; // Оставляем старое значение
        }) == currentTime;
    }
}
