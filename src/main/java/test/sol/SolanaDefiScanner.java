package test.sol;

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

    public static void main(String[] args) throws IOException {
        logger.info("---SolanaDefiScanner работает");
        Set<String> wallets = AccountRedis.loadProcessedAccounts();
        List<String> confirmedWallets = checkWallets(wallets);
        if (!confirmedWallets.isEmpty()) {
            confirmedWallets.forEach(System.out::println);
            AccountRedis.removeSavedWallets(confirmedWallets);
            AccountRedis.saveConfirmedWallets(confirmedWallets);
        }
    }

    private static List<String> checkWallets(Set<String> wallets) throws IOException {
        List<String> confirmedWallets = new ArrayList<>();
        for (String wallet : wallets) {
            String requestBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"id\":1,"
                    + "\"method\":\"getSignaturesForAddress\","
                    + "\"params\": ["
                    + "\"" + wallet + "\","
                    + "{\"limit\":150}"
                    + "]"
                    + "}";

            Map<String, Object> jsonResponse = processRequest(requestBody);
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

    private static boolean processWalletSignatures(Set<String> actualSignatures, String wallet) throws IOException {
        Set<String> loadedSignatures = AccountRedis.loadWalletSignatures(wallet);
        if (!(loadedSignatures.size() == actualSignatures.size())) {
            actualSignatures.removeAll(loadedSignatures);
            boolean contain = validateSignatures(actualSignatures);
            return contain;
        }
        return false;
    }

    private static boolean validateSignatures(Set<String> actualSignatures) throws IOException {
        for (String signature : actualSignatures) {
            String requestBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"id\":1,"
                    + "\"method\":\"getTransaction\","
                    + "\"params\":["
                    + "\"" + signature + "\","
                    + "{\"encoding\":\"jsonParsed\",\"maxSupportedTransactionVersion\":0}"
                    + "]"
                    + "}";

            Map<String, Object> jsonResponse = processRequest(requestBody);
            if (jsonResponse.containsKey("result")) {
                Map<String, Object> transaction = (Map<String, Object>) jsonResponse.get("result");
                Map<String, Object> meta = (Map<String, Object>) transaction.get("meta");
                if (meta != null) {
                    String logMessage = (String) meta.get("logMessage");
                    logger.info("LOG---- {}", logMessage);
                    for (String defiUrl : DEFI_URLS) {
                        return logMessage.contains(defiUrl);
                    }
                }
            }
        }
        return false;
    }

    private static Map<String, Object> processRequest(String requestBody) throws IOException {
        Request request = new Request.Builder()
                .url(RPC_URL)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .build();

        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();

        Map<String, Object> jsonResponse = objectMapper.readValue(responseBody, Map.class);
        return jsonResponse;
    }
}
