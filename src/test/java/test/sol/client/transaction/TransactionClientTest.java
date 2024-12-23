package test.sol.client.transaction;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.utils.RequestBuilder;
import test.sol.utils.RequestSender;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionClientTest {

    private static final String RPC_URL = "http://localhost:8899";
    private RequestSender requestSender;
    private RequestBuilder requestBuilder;
    private TransactionClient transactionClient;

    @BeforeEach
    void setUp() {
        requestSender = mock(RequestSender.class);
        requestBuilder = mock(RequestBuilder.class);
        transactionClient = new TransactionClient(RPC_URL, requestSender, requestBuilder);
    }

    @Test
    void testGetTransactions() throws IOException, InterruptedException {
        // Arrange
        Set<String> signatures = Set.of("sig1", "sig2", "sig3", "sig4", "sig5");
        String batchRequestBody = "mockBatchRequest";
        TransactionResponse response1 = new TransactionResponse(null, 1);
        TransactionResponse response2 = new TransactionResponse(null, 2);
        List<TransactionResponse> mockResponses = List.of(response1, response2);

        when(requestBuilder.buildBatchRequest(anyList(), "getTransaction")).thenReturn(batchRequestBody);
        when(requestSender.processRequestWithRetry(eq(batchRequestBody), eq(RPC_URL), any(TypeReference.class)))
                .thenReturn(mockResponses);

        // Act
        List<TransactionResponse> transactions = transactionClient.getTransactions(signatures);

        // Assert
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
        verify(requestBuilder, atLeastOnce()).buildBatchRequest(anyList(), "getTransaction");
        verify(requestSender, atLeastOnce()).processRequestWithRetry(eq(batchRequestBody), eq(RPC_URL), any(TypeReference.class));
    }

    @Test
    void testGetTransactions_withEmptySignatures_returnsEmptyList() throws IOException, InterruptedException {
        // Arrange
        Set<String> signatures = Set.of();

        // Act
        List<TransactionResponse> transactions = transactionClient.getTransactions(signatures);

        // Assert
        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
        verifyNoInteractions(requestBuilder, requestSender);
    }

    @Test
    void testGetSingleTransaction() throws IOException {
        // Arrange
        String signature = "mockSignature";
        String requestBody = "mockRequestBody";
        TransactionResponse mockResponse = new TransactionResponse(null,1);

        when(requestBuilder.buildJsonRpcRequest(
                eq("getTransaction"),
                eq(signature),
                eq(Map.of("encoding", "jsonParsed")),
                eq(Map.of("maxSupportedTransactionVersion", 0))
        )).thenReturn(requestBody);
        when(requestSender.processRequestWithRetry(eq(requestBody), eq(RPC_URL), any(TypeReference.class)))
                .thenReturn(mockResponse);

        // Act
        TransactionResponse response = transactionClient.getSingleTransaction(signature);

        // Assert
        assertNotNull(response);
        assertEquals(mockResponse, response);
        verify(requestBuilder).buildJsonRpcRequest(
                "getTransaction",
                signature,
                Map.of("encoding", "jsonParsed"),
                Map.of("maxSupportedTransactionVersion", 0)
        );
        verify(requestSender).processRequestWithRetry(eq(requestBody), eq(RPC_URL), any(TypeReference.class));
    }
}
