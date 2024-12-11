package test.sol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SolanaDefiScanner {
    private static final String RPC_URL = "https://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient();
    private static final Logger logger = LoggerFactory.getLogger(SolanaAccountCreationScanner.class);
    private static final Set<String> DEFI_URLS = Set.of(
            "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4",
            "675kPX9MHTjS2zt1qfr1NYHuzeLXfQM9H24wFSUt1Mp8",
            "6EF8rrecthR5Dkzon8Nwu78hRvfCKubJ14M5uBEwF6P"
    );

    public static void main(String[] args) throws IOException, InterruptedException {
        logger.info("---SolanaDefiScanner работает");
        List<String> wallets = ValidatedWalletsRedis.loadValidatedAccounts();
        logger.info("Loaded wallets {}", wallets.size());
        List<String> confirmedWallets = checkWallets(wallets);
        logger.info("Confirmed wallets {}", confirmedWallets.size());
        if (!confirmedWallets.isEmpty()) {
            confirmedWallets.forEach(System.out::println);
            ValidatedWalletsRedis.removeValidatedWallets(confirmedWallets);
            ConfirmedWalletsRedis.saveConfirmedWallets(confirmedWallets);
            SignatureRedis.removeWalletSignatures(confirmedWallets);
        }
    }

    private static List<String> checkWallets(List<String> wallets) throws IOException, InterruptedException {
        List<String> confirmedWallets = new ArrayList<>();
        for (String wallet : wallets) {
            Thread.sleep(1000);
            logger.info("Processing wallet {}", wallet);
            String requestBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"id\":1,"
                    + "\"method\":\"getSignaturesForAddress\","
                    + "\"params\": ["
                    + "\"" + wallet + "\","
                    + "{\"limit\":50}"
                    + "]"
                    + "}";

            Map<String, Object> jsonResponse = (Map<String, Object>) processRequest(requestBody);
            List<Map<String, Object>> result = (List<Map<String, Object>>) jsonResponse.get("result");

            Set<String> signatures = new HashSet<>();
            if (result != null) {
                for (Map<String, Object> entry : result) {
                    Object error = entry.get("err");
                    if (error == null) {
                        Object signatureObj = entry.get("signature");
                        if (signatureObj instanceof String) {
                            signatures.add((String) signatureObj);
                        }
                    }
                }
            }
            boolean isConfirmed = processWalletSignatures(signatures, wallet);
            if (isConfirmed) {
                confirmedWallets.add(wallet);
            }
        }
        return confirmedWallets;
    }

    private static boolean processWalletSignatures(Set<String> actualSignatures, String wallet) throws IOException, InterruptedException {
        Set<String> loadedSignatures = SignatureRedis.loadWalletSignatures(wallet);
        if (!(loadedSignatures.size() == actualSignatures.size())) {
            actualSignatures.removeAll(loadedSignatures);
            SignatureRedis.saveWalletSignatures(actualSignatures, wallet);
            return validateSignatures(actualSignatures);
        }
        return false;
    }

    private static boolean validateSignatures(Set<String> actualSignatures) throws IOException, InterruptedException {
        List<String> signaturesList = new ArrayList<>(actualSignatures);
        int batchSize = 5;

        for (int i = 0; i < signaturesList.size(); i += batchSize) {
            Thread.sleep(2000);
            List<String> batchSignatures = signaturesList.subList(i, Math.min(i + batchSize, signaturesList.size()));

            StringBuilder batchRequestBody = new StringBuilder("[");
            for (int j = 0; j < batchSignatures.size(); j++) {
                String signature = batchSignatures.get(j);
                batchRequestBody.append("{")
                        .append("\"jsonrpc\":\"2.0\",")
                        .append("\"id\":").append(j).append(",")
                        .append("\"method\":\"getTransaction\",")
                        .append("\"params\":[").append("\"").append(signature).append("\",")
                        .append("{\"encoding\":\"jsonParsed\",\"maxSupportedTransactionVersion\":0}")
                        .append("]}");
                if (j < batchSignatures.size() - 1) {
                    batchRequestBody.append(",");
                }
            }
            batchRequestBody.append("]");

            List<Map<String, Object>> jsonResponseList = (List<Map<String, Object>>) processRequest(batchRequestBody.toString());

            for (Map<String, Object> jsonResponse : jsonResponseList) {
                if (jsonResponse.containsKey("result")) {
                    Map<String, Object> transaction = (Map<String, Object>) jsonResponse.get("result");
                    Map<String, Object> meta = (Map<String, Object>) transaction.get("meta");
                    if (meta != null) {
                        List<String> logMessages = (List<String>) meta.get("logMessages");
                        if (logMessages != null) {
                            for (String logMessage : logMessages) {
                                for (String defiUrl : DEFI_URLS) {
                                    if (logMessage.contains(defiUrl)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static Object processRequest(String requestBody) throws IOException {
        Request request = new Request.Builder()
                .url(RPC_URL)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (responseBody.trim().startsWith("[")) {
                return objectMapper.readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {
                });
            } else {
                return objectMapper.readValue(responseBody, Map.class);
            }
        }
    }
}
