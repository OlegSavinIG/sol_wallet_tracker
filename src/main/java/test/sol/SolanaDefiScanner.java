package test.sol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.defiwebsocket.queueprocessor.NotActivatedWalletsQueue;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.redis.ConfirmedWalletsRedis;
import test.sol.redis.NotActivatedWalletsRedis;
import test.sol.redis.SignatureRedis;
import test.sol.redis.ValidatedWalletsRedis;
import test.sol.service.signature.SignatureService;
import test.sol.service.signature.SignatureServiceImpl;
import test.sol.service.wallet.WalletService;
import test.sol.telegram.TelegramMessageHandler;
import test.sol.utils.ClientFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SolanaDefiScanner {
    private static final String RPC_URL = "https://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";
    private static final SignatureClient signatureClient = ClientFactory.createSignatureClient(RPC_URL);
    private static final SignatureService signatureService = new SignatureServiceImpl();
    private static final WalletService walletService = new WalletService();
    private static final Logger logger = LoggerFactory.getLogger(SolanaDefiScanner.class);
    private static final List<String> DEFI_URLS = List.of(
//            "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4",
//            "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8",
            "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
    );

    public static void main(String[] args) throws IOException, InterruptedException {
        long startTime = System.nanoTime();
        logger.info("SolanaDefiScanner работает");

        try {
            List<String> wallets = ValidatedWalletsRedis.loadValidatedAccounts();
            if (wallets.isEmpty()) {
                logger.warn("No wallets loaded from Redis. Exiting...");
                return;
            }
            ValidatedWalletsRedis.removeValidatedWallets(wallets);
            logger.info("Loaded wallets from Redis: {}", wallets.size());

            Map<String, SignaturesResponse> signaturesForWallets = signatureClient.getSignaturesForWallets(wallets);

            Map<String, Set<String>> validatedSignatures = signaturesForWallets.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> signatureService.validateSignature(entry.getValue())
                    ));
            logger.info("Total signatures for all wallets {}", validatedSignatures.values().stream()
                    .mapToInt(Set::size)
                    .sum());

            List<String> confirmedWallets = walletService.getWalletsWithDefiUrl(validatedSignatures, DEFI_URLS);
            logger.info("Confirmed wallets: {}", confirmedWallets.size());

            if (!confirmedWallets.isEmpty()) {
                wallets.removeAll(confirmedWallets);
            }
            if (!wallets.isEmpty()) {
                NotActivatedWalletsRedis.saveWithTTL(wallets);
            }
            Thread.sleep(700);
            NotActivatedWalletsQueue.addWallets(wallets);

            if (!confirmedWallets.isEmpty()) {
                String message = "Wallets found "
                        + confirmedWallets.size()
                        + " : - \n" + String.join(" - \n", confirmedWallets);
                TelegramMessageHandler.sendToTelegram(message);
                confirmedWallets.forEach(wallet -> logger.info("Confirmed wallet: {}", wallet));
                ConfirmedWalletsRedis.saveConfirmedWallets(confirmedWallets);
                SignatureRedis.removeWalletSignatures(confirmedWallets);
            }
        } catch (IOException e) {
            logger.error("IOException occurred in SolanaDefiScanner: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            logger.error("InterruptedException occurred in SolanaDefiScanner: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("An unexpected error occurred in SolanaDefiScanner: {}", e.getMessage(), e);
        } finally {
            long endTime = System.nanoTime();
            logger.info("DefiScanner working time: {} ms", (endTime - startTime) / 1_000_000);
        }
    }
}

