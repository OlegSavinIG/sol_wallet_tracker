package test.sol.client.transaction;

import com.fasterxml.jackson.core.type.TypeReference;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.pojo.transaction.TransactionResult;
import test.sol.utils.RequestBuilder;
import test.sol.utils.RequestSender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransactionClient {
    private final String RPC_URL;
    private final RequestSender requestSender;
    private final RequestBuilder requestBuilder;

    public TransactionClient(String RPC_URL, RequestSender requestSender, RequestBuilder requestBuilder) {
        this.RPC_URL = RPC_URL;
        this.requestSender = requestSender;
        this.requestBuilder = requestBuilder;
    }

    public List<TransactionResponse> getTransactions(Set<String> signatures) throws IOException, InterruptedException {
        List<String> signatureList = new ArrayList<>(signatures);
        List<TransactionResponse> transactions = new ArrayList<>();
        int batchSize = 40;

        for (int i = 0; i < signatureList.size(); i += batchSize) {
            Thread.sleep(1000);
            List<String> batchSignatures = signatureList.subList(i, Math.min(i + batchSize, signatureList.size()));
            String batchRequestBody = requestBuilder.buildBatchRequest(batchSignatures, "getTransaction");

            List<TransactionResponse> transactionResponses = requestSender.processRequestWithRetry(
                    batchRequestBody, RPC_URL,
                    new TypeReference<List<TransactionResponse>>() {
                    });
            if (transactionResponses != null && !transactionResponses.isEmpty()) {
                transactions.addAll(transactionResponses);
            }
        }
        return transactions;
    }
    public TransactionResponse getSingleTransaction(String signature) throws IOException {
        Map<String, Object> options = Map.of(
                "encoding", "jsonParsed",
                "maxSupportedTransactionVersion", 0
        );
        String requestBody = requestBuilder.buildJsonRpcRequest("getTransaction", signature, options);
        return requestSender.processRequestWithRetry(
                requestBody, RPC_URL, new TypeReference<TransactionResponse>() {
                });
    }

}
