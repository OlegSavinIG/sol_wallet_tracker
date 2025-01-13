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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WalletService {
    private static final String RPC_URL = "https://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";
    private final TransactionClient transactionClient = ClientFactory.createTransactionClient(RPC_URL);
    private final SignatureClient signatureClient = ClientFactory.createSignatureClient(RPC_URL);
    private static final int MIN_TRANSACTION_COUNT = 2;
    private static final int MAX_TRANSACTION_COUNT = 55;
    private static final int MAX_TRANSACTION_AGE_HOURS = 24;
    private final Logger logger = LoggerFactory.getLogger(WalletService.class);

    public List<String> validateWallets(List<String> wallets) throws IOException {
        int batchSize = 40;
        List<String> validatedWallets = new ArrayList<>();

        for (int i = 0; i < wallets.size(); i += batchSize) {
            List<String> batchWallets = wallets.subList(i, Math.min(i + batchSize, wallets.size()));
            validatedWallets.addAll(validateBatch(batchWallets));
        }
        return validatedWallets;
    }

//    private List<String> validateBatch(List<String> wallets) throws IOException {
//        Map<String, SignaturesResponse> signaturesForWallets = signatureClient.getSignaturesForWallets(wallets);
//
//        return signaturesForWallets.entrySet().stream()
//                .filter(entry -> isTransactionsCountBelow80(entry.getValue()))
//                .filter(entry -> isTransactionTimeBefore24Hours(entry.getValue()))
//                .map(Map.Entry::getKey)
//                .collect(Collectors.toList());
//    }
//
//    private boolean isTransactionsCountBelow80(SignaturesResponse signaturesResponse) {
//        int transactionCount = signaturesResponse.result().size();
//        return transactionCount > 1 && transactionCount < 55;
//    }
//
//    private boolean isTransactionTimeBefore24Hours(SignaturesResponse signaturesResponse) {
//        try {
//            List<SignatureResponseResult> results = signaturesResponse.result();
//            if (results.isEmpty()) {
//                return false;
//            }
//
//            SignatureResponseResult latestSignature = results.get(results.size() - 1);
//            TransactionResponse transaction = transactionClient.getSingleTransaction(latestSignature.signature());
//
//            LocalDateTime transactionTime = LocalDateTime.ofEpochSecond(
//                    transaction.result().blockTime(), 0, ZoneOffset.UTC);
//            LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
//
//            return Duration.between(transactionTime, currentTime).toHours() <= 24;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }

        public List<String> validateBatch(List<String> wallets) throws IOException {
            Map<String, SignaturesResponse> signaturesForWallets = signatureClient.getSignaturesForWallets(wallets);

            List<CompletableFuture<String>> futures = signaturesForWallets.entrySet().stream()
                    .map(entry -> CompletableFuture.supplyAsync(() -> {
                        String wallet = entry.getKey();
                        SignaturesResponse signaturesResponse = entry.getValue();

                        if (isTransactionCountValid(signaturesResponse) && isTransactionRecent(signaturesResponse)) {
                            return wallet;
                        }
                        return null;
                    }))
                    .collect(Collectors.toList());

            // Ждем завершения всех CompletableFuture и собираем результаты
            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        private boolean isTransactionCountValid(SignaturesResponse signaturesResponse) {
            if (signaturesResponse == null || signaturesResponse.result() == null) {
                System.err.println("Signatures response or result is null");
                return false;
            }

            int transactionCount = signaturesResponse.result().size();
//            logger.info("Total signatures/transactions per wallet: " + transactionCount);

            return transactionCount >= MIN_TRANSACTION_COUNT && transactionCount <= MAX_TRANSACTION_COUNT;
        }

        private boolean isTransactionRecent(SignaturesResponse signaturesResponse) {
            if (signaturesResponse == null || signaturesResponse.result() == null || signaturesResponse.result().isEmpty()) {
                System.err.println("Signatures response or result is null/empty");
                return false;
            }

            try {
                List<SignatureResponseResult> results = signaturesResponse.result();
                SignatureResponseResult latestSignature = results.get(results.size() - 1);

                TransactionResponse transaction = transactionClient.getSingleTransaction(latestSignature.signature());
                if (transaction == null || transaction.result() == null) {
                    System.err.println("Transaction or block time is null for signature " + latestSignature.signature());
                    return false;
                }

                LocalDateTime transactionTime = LocalDateTime.ofEpochSecond(
                        transaction.result().blockTime(), 0, ZoneOffset.UTC);
                LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

                long hoursBetween = Duration.between(transactionTime, currentTime).toHours();
                return hoursBetween <= MAX_TRANSACTION_AGE_HOURS;
            } catch (IOException e) {
                System.err.println("Error fetching transaction details: " + e.getMessage());
                return false;
            }
        }


    public Map<String, Set<String>> getWalletsWithDefiUrl(
            Map<String, Set<String>> signaturesForWallets,
            List<String> defiUrls) throws IOException, InterruptedException {

        Set<String> rayJupWallets = new HashSet<>();
        Set<String> pumpWallets = new HashSet<>();
        Map<String, Set<String>> confirmedWallets = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : signaturesForWallets.entrySet()) {
            Thread.sleep(500);
            Set<String> loadedSignatures = SignatureRedis.loadWalletSignatures(entry.getKey());
            if (loadedSignatures.size() == entry.getValue().size()) {
                continue;
            }
            entry.getValue().removeAll(loadedSignatures);
            SignatureRedis.saveWalletSignatures(entry.getValue(), entry.getKey());

            List<TransactionResponse> transactions = transactionClient.getTransactions(entry.getValue());

            boolean isConfirmed = false;
            for (TransactionResponse transaction : transactions) {
                String logMessages = transaction.result().meta().logMessages().toString();
                if (logMessages.contains("6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P")) {
                    pumpWallets.add(entry.getKey());
                    isConfirmed = true;
                }
                if (isConfirmed) {
                    break;
                }
                for (String defiUrl : defiUrls) {
                    if (logMessages.contains(defiUrl)) {
                        rayJupWallets.add(entry.getKey());
                        isConfirmed = true;
                        break;
                    }
                }
                if (isConfirmed) {
                    break;
                }
            }
        }
        confirmedWallets.put("Pump", pumpWallets);
        confirmedWallets.put("Ray", rayJupWallets);
        return confirmedWallets;
    }
}

