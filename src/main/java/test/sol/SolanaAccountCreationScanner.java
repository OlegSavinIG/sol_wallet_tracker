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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SolanaAccountCreationScanner {

    private static final String RPC_URL = "https://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient();
    private static final Logger logger = LoggerFactory.getLogger(SolanaAccountCreationScanner.class);

    public static void main(String[] args) {
        try {
            logger.info("Получение подписей для SystemProgram...");
            Set<String> signatures = getSignaturesForSystemProgram();
            logger.info("Получено {} подписей.", signatures.size());

            logger.info("Обработка транзакций...");
            Set<String> createdAccounts = processTransactions(signatures);
            List<String> wallets = processWallets(createdAccounts);

            logger.info("Завершено. Найдено {} новых аккаунтов", wallets.size());
            wallets.forEach(account -> logger.info("Аккаунт: {}", account));
        } catch (Exception e) {
            logger.error("Произошла ошибка при выполнении программы.", e);
        }
    }

    private static Set<String> getSignaturesForSystemProgram() throws IOException {
        String requestBody = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":1,"
                + "\"method\":\"getSignaturesForAddress\","
                + "\"params\":["
                + "\"ComputeBudget111111111111111111111111111111\","
                + "{\"limit\":1000}"
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

        return signatures;
    }

    private static Set<String> processTransactions(Set<String> signatures) throws IOException {
        Set<String> createdAccounts = new HashSet<>();

        for (String signature : signatures) {
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
                List<Map<String, Object>> instructions = extractInstructions(transaction);

                if (containsTransfer(instructions)) {
                    String wallet = extractWallet(instructions);
                    if (wallet != null) {
                        createdAccounts.add(wallet);
                    }
                }
            }
        }

        return createdAccounts;
    }

    private static boolean containsTransfer(List<Map<String, Object>> instructions) {
        for (Map<String, Object> instruction : instructions) {
            try {
                Map<String, Object> parsed = (Map<String, Object>) instruction.get("parsed");
                if (parsed != null) {
                    String type = (String) parsed.get("type");
                    if ("transfer".equals(type)) {
                        return true;
                    }
                }
            } catch (ClassCastException e) {
                logger.error("ClassCastException while processing instruction: {}", instruction, e);
            } catch (Exception e) {
                logger.error("Unexpected exception while processing instruction: {}", instruction, e);
            }
        }
        return false;
    }


    private static List<String> processWallets(Set<String> wallets) throws IOException {
        List<String> correctWallets = new ArrayList<>();
        List<String> positiveBalance = hasPositiveBalance(wallets);
        logger.info("Wallet with positive balance {}", positiveBalance.size());
        for (String wallet : positiveBalance) {
            if (hasTransactions(wallet)) {
                correctWallets.add(wallet);
            }
        }
        return correctWallets;
    }

    private static List<String> hasPositiveBalance(Set<String> wallets) throws IOException {
        List<String> positiveWallets = new ArrayList<>();
        List<String> walletList = new ArrayList<>(wallets);

        logger.info("Balance check for {} wallets", wallets.size());

        int batchSize = 5;
        for (int i = 0; i < walletList.size(); i += batchSize) {
            List<String> batch = walletList.subList(i, Math.min(walletList.size(), i + batchSize));
            String walletArray = batch.stream()
                    .map(wallet -> "\"" + wallet + "\"")
                    .collect(Collectors.joining(",", "[", "]"));

            String requestBody = "{"
                    + "\"jsonrpc\":\"2.0\","
                    + "\"id\":1,"
                    + "\"method\":\"getMultipleAccounts\","
                    + "\"params\": ["
                    + walletArray + ","
                    + "{\"encoding\":\"jsonParsed\"}"
                    + "]"
                    + "}";

            Map<String, Object> jsonResponse = processRequest(requestBody);
            Map<String, Object> result = (Map<String, Object>) jsonResponse.get("result");
            List<Map<String, Object>> accounts = (List<Map<String, Object>>) result.get("value");

            for (int j = 0; j < batch.size(); j++) {
                Map<String, Object> account = accounts.get(j);
                if (account != null) {
                    Number lamportsNumber = (Number) account.get("lamports");
                    if (lamportsNumber != null) {
                        long lamports = lamportsNumber.longValue();
                        logger.info("Lamports for wallet {}: {}", batch.get(j), lamports);
                        if (lamports > 0) {
                            positiveWallets.add(batch.get(j));
                        }
                    } else {
                        logger.warn("Lamports missing for wallet {}", batch.get(j));
                    }
                } else {
                    logger.warn("Null account object for wallet {}", batch.get(j));
                }
            }
        }

        return positiveWallets;
    }

    private static boolean hasTransactions(String wallet) throws IOException {
        String requestBody = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":1,"
                + "\"method\":\"getSignaturesForAddress\","
                + "\"params\": ["
                + "\"" + wallet + "\","
                + "{\"limit\":85}"
                + "]"
                + "}";

        Map<String, Object> jsonResponse = processRequest(requestBody);
        List<Map<String, Object>> result = (List<Map<String, Object>>) jsonResponse.get("result");

        if (result.size() >= 80 || result.size() == 0) {
            return false;
        } else {
            Map<String, Object> firstTransaction = result.get(result.size() - 1);
            Object signature = firstTransaction.get("signature");
            if (signature != null) {
                if (signature instanceof String) {
                    return hoursChecker(signature.toString());
                }
            }
        }
        return true;
    }


    private static boolean hoursChecker(String signature) throws IOException {
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
            Number blockTimeNumber = (Number) transaction.get("blockTime");

            if (blockTimeNumber != null) {
                LocalDateTime blockDateTime = LocalDateTime.ofEpochSecond(blockTimeNumber.longValue(), 0, ZoneOffset.UTC);
                LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);

                if (Duration.between(blockDateTime, currentTime).toHours() <= 24) {
                    return true;
                }
            }
        }
        return false;
    }


    private static String extractWallet(List<Map<String, Object>> instructions) {
        for (Map<String, Object> instruction : instructions) {
            try {
                Map<String, Object> parsed = (Map<String, Object>) instruction.get("parsed");
                if (parsed != null) {
                    Map<String, Object> info = (Map<String, Object>) parsed.get("info");
                    return (String) info.get("destination");
                }
            } catch (ClassCastException e) {
                logger.error("ClassCastException while processing instruction: {}", instruction, e);
            } catch (Exception e) {
                logger.error("Unexpected exception while processing instruction: {}", instruction, e);
            }
        }
        return null;
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

    private static List<Map<String, Object>> extractInstructions(Map<String, Object> transaction) {
        Map<String, Object> transactionMap = (Map<String, Object>) transaction.get("transaction");
        Map<String, Object> message = (Map<String, Object>) transactionMap.get("message");
        List<Map<String, Object>> instructions = (List<Map<String, Object>>) message.get("instructions");
        return instructions;
    }
}


