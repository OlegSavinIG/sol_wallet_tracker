package test.sol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.defiwebsocket.queueprocessor.NotActivatedWalletsQueue;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.redis.NotActivatedWalletsRedis;
import test.sol.redis.SignatureRedis;
import test.sol.redis.ValidatedWalletsRedis;
import test.sol.service.signature.SignatureService;
import test.sol.service.signature.SignatureServiceImpl;
import test.sol.service.wallet.WalletService;
import test.sol.telegram.TelegramInformationMessageHandler;
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
            "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4",
            "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8"
//            "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
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

            Map<String, Set<String>> confirmedWallets = walletService.getWalletsWithDefiUrl(validatedSignatures, DEFI_URLS);

            if (!confirmedWallets.get("Ray").isEmpty()) {
                wallets.removeAll(confirmedWallets.get("Ray"));
                SignatureRedis.removeWalletSignatures(confirmedWallets.get("Ray").stream().toList());
            }
            if (!confirmedWallets.get("Pump").isEmpty()) {
                wallets.removeAll(confirmedWallets.get("Pump"));
                SignatureRedis.removeWalletSignatures(confirmedWallets.get("Pump").stream().toList());
            }
            if (!wallets.isEmpty()) {
                NotActivatedWalletsRedis.saveWithTTL(wallets);
            }
            Thread.sleep(700);
            NotActivatedWalletsQueue.addWallets(wallets);

//            if (!confirmedWallets.get("Ray").isEmpty()) {
//                String message = "Ray or Jup wallets found "
//                        + confirmedWallets.get("Ray").size()
//                        + " : \n" + String.join(" - \n", confirmedWallets.get("Ray"));
//                TelegramInformationMessageHandler.sendToTelegram(message);
//            }
            if (!confirmedWallets.get("Ray").isEmpty()) {
                List<String> formattedWallets = confirmedWallets.get("Ray").stream()
                        .map(wallet -> "https://gmgn.ai/sol/address/" + wallet)
                        .toList();

                String message = "Ray or Jup wallets found "
                        + formattedWallets.size()
                        + " : \n" + String.join(" --- \n", formattedWallets);
                TelegramInformationMessageHandler.sendToTelegram(message);
            }

            if (!confirmedWallets.get("Pump").isEmpty()) {
                List<String> formattedWallets = confirmedWallets.get("Pump").stream()
                        .map(wallet -> "https://gmgn.ai/sol/address/" + wallet)
                        .toList();

                String pumpMessage = "Pump wallets found "
                        + confirmedWallets.get("Pump").size()
                        + " : \n" + String.join(" -- \n", formattedWallets);
                TelegramInformationMessageHandler.sendToTelegram(pumpMessage);
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

