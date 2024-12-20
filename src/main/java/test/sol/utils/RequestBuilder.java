package test.sol.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestBuilder {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String buildJsonRpcRequest(String method, Object... params) {
        validateParams(method, params);
        try {
            return objectMapper.writeValueAsString(createBaseRequest(method, params));
        } catch (IOException e) {
            throw new IllegalStateException("Error building JSON RPC request", e);
        }
    }
    public String buildBatchRequest(List<String> signatures, String method) {
        if (signatures == null || signatures.isEmpty()) {
            throw new IllegalArgumentException("Signatures list cannot be null or empty");
        }

        StringBuilder batchRequestBody = new StringBuilder("[");
        for (int i = 0; i < signatures.size(); i++) {
            String signature = signatures.get(i);
            batchRequestBody.append("{")
                    .append("\"jsonrpc\":\"2.0\",")
                    .append("\"id\":").append(i).append(",")
                    .append("\"method\":\"").append(method).append("\",")
                    .append("\"params\":[\"").append(signature).append("\",")
                    .append("{\"encoding\":\"jsonParsed\",\"maxSupportedTransactionVersion\":0}")
                    .append("]}");
            if (i < signatures.size() - 1) {
                batchRequestBody.append(",");
            }
        }
        batchRequestBody.append("]");
        return batchRequestBody.toString();
    }
    public String buildBatchRequestWithId(Map<Integer, String> params, String method, Integer limit) {
        validateParams(method, params);
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("Limit must be a positive integer");
        }

        try {
            List<Map<String, Object>> batchRequests = new ArrayList<>();

            for (Map.Entry<Integer, String> entry : params.entrySet()) {
                Map<String, Object> request = createBaseRequest(
                        method,
                        entry.getValue(),
                        Map.of("limit", limit)
                );
                request.put("id", entry.getKey());
                batchRequests.add(request);
            }

            return objectMapper.writeValueAsString(batchRequests);
        } catch (IOException e) {
            throw new IllegalStateException("Error building batch JSON RPC request", e);
        }
    }

    private Map<String, Object> createBaseRequest(String method, Object... params) {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", method);
        request.put("params", params);
        return request;
    }


    private void validateParams(String method, Object... params) {
        if (method == null || method.isEmpty()) {
            throw new IllegalArgumentException("Method cannot be null or empty");
        }
        if (params == null) {
            throw new IllegalArgumentException("Params cannot be null");
        }
    }
}
