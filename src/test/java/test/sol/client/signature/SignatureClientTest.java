package test.sol.client.signature;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import test.sol.pojo.signature.SignatureResponseResult;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.utils.RequestBuilder;
import test.sol.utils.RequestSender;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SignatureClientTest {

    private static final String RPC_URL = "http://localhost:8899";
    private RequestSender requestSender;
    private RequestBuilder requestBuilder;
    private SignatureClient signatureClient;

    @BeforeEach
    void setUp() {
        requestSender = mock(RequestSender.class);
        requestBuilder = mock(RequestBuilder.class);
        signatureClient = new SignatureClient(RPC_URL, requestSender, requestBuilder);
    }

    @Test
    void testGetSignaturesForSystemProgram() throws IOException {
        // Arrange
        String requestBody = "mockRequestBody";
        SignaturesResponse mockResponse = new SignaturesResponse(1, List.of(new SignatureResponseResult("sign", null)));
        when(requestBuilder.buildJsonRpcRequest(
                eq("getSignaturesForAddress"),
                eq("ComputeBudget111111111111111111111111111111"),
                eq(Map.of("limit", 1000)))).thenReturn(requestBody);
        when(requestSender.processRequestWithRetry(eq(requestBody), eq(RPC_URL), any(TypeReference.class)))
                .thenReturn(mockResponse);

        // Act
        SignaturesResponse response = signatureClient.getSignaturesForSystemProgram();

        // Assert
        assertNotNull(response);
        assertEquals(mockResponse, response);
        verify(requestBuilder).buildJsonRpcRequest(
                "getSignaturesForAddress",
                "ComputeBudget111111111111111111111111111111",
                Map.of("limit", 1000));
        verify(requestSender).processRequestWithRetry(eq(requestBody), eq(RPC_URL), any(TypeReference.class));
    }

    @Test
    void testGetSignaturesForOneWallet() throws IOException {
        // Arrange
        String wallet = "mockWallet";
        String requestBody = "mockRequestBody";
        SignatureResponseResult mockResponse = new SignatureResponseResult("sign", null);
        when(requestBuilder.buildJsonRpcRequest(
                eq("getSignaturesForAddress"),
                eq(wallet),
                eq(Map.of("limit", 85)))).thenReturn(requestBody);
        when(requestSender.processRequestWithRetry(eq(requestBody), eq(RPC_URL), any(TypeReference.class)))
                .thenReturn(mockResponse);

        // Act
        SignatureResponseResult response = signatureClient.getSignaturesForOneWallet(wallet);

        // Assert
        assertNotNull(response);
        assertEquals(mockResponse, response);
        verify(requestBuilder).buildJsonRpcRequest("getSignaturesForAddress", wallet, Map.of("limit", 85));
        verify(requestSender).processRequestWithRetry(eq(requestBody), eq(RPC_URL), any(TypeReference.class));
    }

    @Test
    void testGetSignaturesForWallets() throws IOException {
        // Arrange
        List<String> wallets = List.of("wallet1", "wallet2");
        String batchRequest = "mockBatchRequest";
        SignaturesResponse mockResponse1 = new SignaturesResponse(1, List.of(new SignatureResponseResult("sign1", null)));
        SignaturesResponse mockResponse2 = new SignaturesResponse(2, List.of(new SignatureResponseResult("sign2", null)));
        List<SignaturesResponse> mockResponses = List.of(mockResponse1, mockResponse2);

        when(requestBuilder.buildBatchRequestWithId(
                anyMap(),
                eq("getSignaturesForAddress"),
                eq(85))).thenReturn(batchRequest);
        when(requestSender.processRequestWithRetry(eq(batchRequest), eq(RPC_URL), any(TypeReference.class)))
                .thenReturn(mockResponses);

        // Act
        Map<String, SignaturesResponse> result = signatureClient.getSignaturesForWallets(wallets);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("wallet1"));
        assertTrue(result.containsKey("wallet2"));
        when(requestBuilder.buildBatchRequestWithId(anyMap(), eq("getSignaturesForAddress"), eq(85))).thenReturn(batchRequest);
        verify(requestSender).processRequestWithRetry(eq(batchRequest), eq(RPC_URL), any(TypeReference.class));
    }
}
