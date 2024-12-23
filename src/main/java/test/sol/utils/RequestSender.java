package test.sol.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

public class RequestSender {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final int CONNECT_TIMEOUT_SECONDS = 30;
    private static final int READ_TIMEOUT_SECONDS = 60;
    private static final int WRITE_TIMEOUT_SECONDS = 60;
    private static final int MAX_RETRIES = 3;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final Logger logger;

    public RequestSender() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();
        this.objectMapper = new ObjectMapper();
        this.logger = LoggerFactory.getLogger(RequestSender.class);
    }

    public <T> T processRequestWithRetry(String requestBody, String url, TypeReference<T> typeReference) throws IOException {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return processRequest(requestBody, url, typeReference);
            } catch (SocketTimeoutException e) {
                logger.warn("Attempt {} failed due to timeout: {}", attempt, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    logger.error("Max retries reached for request to {}", url);
                    throw e;
                }
            } catch (IOException e) {
                logger.error("Attempt {} failed due to I/O error: {}", attempt, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
            }
        }
        throw new IOException("Request failed after " + MAX_RETRIES + " attempts");
    }

    private <T> T processRequest(String requestBody, String url, TypeReference<T> typeReference) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestBody, JSON_MEDIA_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed request to {}: HTTP {} - {}", url, response.code(), response.message());
                throw new IOException("Request failed with HTTP code " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            if (responseBody.isEmpty()) {
                logger.error("Empty response body for request to {}", url);
                throw new IOException("Empty response body.");
            }

            try {
                return objectMapper.readValue(responseBody, typeReference);
            } catch (IOException e) {
                logger.error("Error parsing response from {}: {}", url, e.getMessage());
                throw e;
            }
        }
    }
}
