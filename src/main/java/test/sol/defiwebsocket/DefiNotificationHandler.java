package test.sol.defiwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;
import test.sol.pojo.notification.Value;
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
import java.net.http.WebSocket;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DefiNotificationHandler {
    private final SignatureClient signatureClient = ClientFactory.createSignatureClient();
    private final TransactionClient transactionClient = ClientFactory.createTransactionClient();
    private static final List<String> DEFI_URLS = List.of(
//            "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4",
//            "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8",
            "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
    );
    private static final Logger logger = LoggerFactory.getLogger(NotificationHandler.class);

    public void handleNotification(Integer subscription) {
        try {
            String wallet = SubscriptionWalletStorage.getWalletBySubscription(subscription);
            logger.info("Wallet: {}", wallet);
            SignaturesResponse signaturesResponse = signatureClient.getSignaturesForOneWallet(wallet, 10);

            List<SignatureResponseResult> result = signaturesResponse.result();
            Set<String> signatures = result.stream()
                    .map(SignatureResponseResult::signature)
                    .collect(Collectors.toSet());

            Set<String> signaturesFromRedis = SignatureRedis.loadWalletSignatures(wallet);
            logger.info("Signatures from Redis {} " +
                    "and actual signatures {} ", signaturesFromRedis.size(), signatures.size());
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
        } catch (IOException e) {
            logger.error("Error handling notification: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error in handleNotification: {}", e.getMessage(), e);
        }
    }

    private void processValue(String address, Value value) {
        logger.info("Processing value for address: {} with balance: {} lamports", address, value.getLamports());
    }

    public void handleSubscribeNotification(int result, int id) {
        SubscriptionWalletStorage.addSubscriptionWithId(result, id);
    }
}
