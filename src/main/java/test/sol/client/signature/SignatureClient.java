package test.sol.client.signature;

import com.fasterxml.jackson.core.type.TypeReference;
import test.sol.pojo.signature.SignatureResponseResult;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.utils.RequestBuilder;
import test.sol.utils.RequestSender;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SignatureClient {
    private final String RPC_URL;
    private final RequestSender requestSender;
    private final RequestBuilder requestBuilder;

    public SignatureClient(String RPC_URL, RequestSender requestSender, RequestBuilder requestBuilder) {
        this.RPC_URL = RPC_URL;
        this.requestSender = requestSender;
        this.requestBuilder = requestBuilder;
    }

    public SignaturesResponse getSignaturesForSystemProgram() throws IOException {
        String requestBody = requestBuilder.buildJsonRpcRequest(
                "getSignaturesForAddress",
                "ComputeBudget111111111111111111111111111111",
                Map.of("limit", 1000));

        SignaturesResponse response = requestSender.processRequestWithRetry(
                requestBody, RPC_URL, new TypeReference<SignaturesResponse>() {
                });
        return response;
    }

    public SignaturesResponse getSignaturesForOneWallet(String wallet, int limit) throws IOException {
        String requestBody = requestBuilder.buildJsonRpcRequest(
                "getSignaturesForAddress",
                wallet, Map.of("limit", limit));
        return requestSender.processRequestWithRetry(requestBody, RPC_URL, new TypeReference<SignaturesResponse>() {
        });
    }

    public Map<String, SignaturesResponse> getSignaturesForWallets(List<String> wallets) throws IOException {
        long startTime = System.nanoTime();
        Map<String, SignaturesResponse> result = new HashMap<>();
        int batchSize = 70;

        for (int i = 0; i < wallets.size(); i += batchSize) {
            List<String> batchWallets = wallets.subList(i, Math.min(i + batchSize, wallets.size()));
            Map<Integer, String> sortedWallets = IntStream.range(0, batchWallets.size())
                    .boxed()
                    .collect(Collectors.toMap(j -> j + 1, batchWallets::get));

            String batchRequest = requestBuilder.buildBatchRequestWithId(
                    sortedWallets, "getSignaturesForAddress", 85);

            List<SignaturesResponse> signaturesResponses = requestSender.processRequestWithRetry(
                    batchRequest, RPC_URL, new TypeReference<List<SignaturesResponse>>() {});

            for (SignaturesResponse response : signaturesResponses) {
                if (!sortedWallets.containsKey(response.id())) {
                    continue;
                }
                result.put(sortedWallets.get(response.id()), response);
            }
        }
        long endTime = System.nanoTime();
        System.out.println("getSignaturesForWallets working time - " + (endTime - startTime) / 1_000_000 + " ms");

        return result;
    }

}
