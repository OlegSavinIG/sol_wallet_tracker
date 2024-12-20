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

    public SignatureResponseResult getSignaturesForOneWallet(String wallet) throws IOException {
        String requestBody = requestBuilder.buildJsonRpcRequest(
                "getSignaturesForAddress",
                wallet, Map.of("limit", 85));
        return requestSender.processRequestWithRetry(requestBody, RPC_URL, new TypeReference<SignatureResponseResult>() {
        });
    }

    public Map<String, SignaturesResponse> getSignaturesForWallets(List<String> wallets) throws IOException {
        Map<Integer, String> sortedWallets = new HashMap<>();
        Map<String, SignaturesResponse> result = new HashMap<>();
        int batchSize = 70;
        for (int i = 0; i < wallets.size(); i+=batchSize) {
            List<String> batchWallets = wallets.subList(i, Math.min(i + batchSize, wallets.size()));

            for (int j = 1; j < batchWallets.size() + 1; j++) {
                sortedWallets.put(j, batchWallets.get(j - 1));
            }

            String batchRequest = requestBuilder.buildBatchRequestWithId(
                    sortedWallets, "getSignaturesForAddress", 85);

            List<SignaturesResponse> signaturesResponses = requestSender.processRequestWithRetry(
                    batchRequest, RPC_URL, new TypeReference<List<SignaturesResponse>>() {
                    });

            for (SignaturesResponse response : signaturesResponses) {
                result.put(sortedWallets.get(response.id()), response);
            }
        }
        return result;
    }
}
