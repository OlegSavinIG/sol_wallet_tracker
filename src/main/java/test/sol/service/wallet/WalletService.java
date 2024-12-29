package test.sol.service.wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;
import test.sol.pojo.signature.SignatureResponseResult;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.redis.SignatureRedis;
import test.sol.utils.ClientFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WalletService {
    private static final String RPC_URL = "https://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";
    private final TransactionClient transactionClient = ClientFactory.createTransactionClient(RPC_URL);
    private final SignatureClient signatureClient = ClientFactory.createSignatureClient(RPC_URL);
    private final Logger logger = LoggerFactory.getLogger(WalletService.class);

    public List<String> validateWallets(List<String> wallets) throws IOException {
        int batchSize = 20;
        List<String> validatedWallets = new ArrayList<>();

        for (int i = 0; i < wallets.size(); i += batchSize) {
            List<String> batchWallets = wallets.subList(i, Math.min(i + batchSize, wallets.size()));
            validatedWallets.addAll(validateBatch(batchWallets));
        }

        return validatedWallets;
    }

    private List<String> validateBatch(List<String> wallets) throws IOException {
        Map<String, SignaturesResponse> signaturesForWallets = signatureClient.getSignaturesForWallets(wallets);

        return signaturesForWallets.entrySet().stream()
                .filter(entry -> isTransactionsCountBelow80(entry.getValue()))
                .filter(entry -> isTransactionTimeBefore24Hours(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private boolean isTransactionsCountBelow80(SignaturesResponse signaturesResponse) {
        int transactionCount = signaturesResponse.result().size();
        return transactionCount > 0 && transactionCount < 80;
    }

    private boolean isTransactionTimeBefore24Hours(SignaturesResponse signaturesResponse) {
        try {
            List<SignatureResponseResult> results = signaturesResponse.result();
            if (results.isEmpty()) {
                return false;
            }

            SignatureResponseResult latestSignature = results.get(results.size() - 1);
            TransactionResponse transaction = transactionClient.getSingleTransaction(latestSignature.signature());

            LocalDateTime transactionTime = LocalDateTime.ofEpochSecond(
                    transaction.result().blockTime(), 0, ZoneOffset.UTC);
            LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

            return Duration.between(transactionTime, currentTime).toHours() <= 24;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getWalletsWithDefiUrl(
            Map<String, Set<String>> signaturesForWallets,
            List<String> defiUrls) throws IOException, InterruptedException {
        long startTime = System.nanoTime();
        List<String> confirmedWallets = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : signaturesForWallets.entrySet()) {
            Thread.sleep(500);

            Set<String> loadedSignatures = SignatureRedis.loadWalletSignatures(entry.getKey());
            if (loadedSignatures.size() == entry.getValue().size()) {
                continue;
            }
            SignatureRedis.saveWalletSignatures(entry.getValue(), entry.getKey());
            entry.getValue().removeAll(loadedSignatures);

            List<TransactionResponse> transactions = transactionClient.getTransactions(entry.getValue());

            boolean isConfirmed = false;
            for (TransactionResponse transaction : transactions) {
                String logMessages = transaction.result().meta().logMessages().toString();

                for (String defiUrl : defiUrls) {
                    if (logMessages.contains(defiUrl)) {
                        confirmedWallets.add(entry.getKey());
                        isConfirmed = true;
                        break;
                    }
                }
                if (isConfirmed) {
                    break;
                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println("getWalletsWithDefiUrl working time - " + (endTime - startTime) / 1_000_000 + " ms");
        return confirmedWallets;
    }


}

