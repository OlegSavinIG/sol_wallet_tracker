package test.sol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.redis.ConfirmedWalletsRedis;
import test.sol.redis.SignatureRedis;
import test.sol.redis.ValidatedWalletsRedis;
import test.sol.service.signature.SignatureService;
import test.sol.service.signature.SignatureServiceImpl;
import test.sol.service.wallet.WalletService;
import test.sol.telegram.TelegramMessageSandler;
import test.sol.utils.ClientFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SolanaDefiScanner {
    private static final SignatureClient signatureClient = ClientFactory.createSignatureClient();
    private static final SignatureService signatureService = new SignatureServiceImpl();
    private static final WalletService walletService = new WalletService();
    private static final Logger logger = LoggerFactory.getLogger(SolanaDefiScanner.class);
    private static final List<String> DEFI_URLS = List.of(
            "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4",
            "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8",
            "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
    );

    public static void main(String[] args) throws IOException, InterruptedException {
        long startTime = System.nanoTime();
        logger.info("SolanaDefiScanner работает");
        List<String> wallets = ValidatedWalletsRedis.loadValidatedAccounts();
        logger.info("Loaded wallets from Redis {}", wallets.size());

        Map<String, SignaturesResponse> signaturesForWallets = signatureClient.getSignaturesForWallets(wallets);
        Map<String, Set<String>> validatedSignatures = signaturesForWallets.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> signatureService.validateSignature(entry.getValue())
                ));

        List<String> confirmedWallets = walletService.getWalletsWithDefiUrl(validatedSignatures, DEFI_URLS);
        logger.info("Confirmed wallets {}", confirmedWallets.size());

        long endTime = System.nanoTime();
        System.out.println("DefiScanner working time - " + (endTime - startTime) / 1_000_000 + " ms");

        if (!confirmedWallets.isEmpty()) {
            String message = "Wallets found "
                    + confirmedWallets.size()
                    + " : - \n" + String.join(" - \n", confirmedWallets);
            TelegramMessageSandler.sendToTelegram(message);
            confirmedWallets.forEach(wallet -> logger.info("Confirmed wallet: {}", wallet));
            ValidatedWalletsRedis.removeValidatedWallets(confirmedWallets);
            ConfirmedWalletsRedis.saveConfirmedWallets(confirmedWallets);
            SignatureRedis.removeWalletSignatures(confirmedWallets);
        }
    }
}
