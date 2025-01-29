package test.sol.utils;

import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;

public class ClientFactory {

    private static final RequestSender requestSender = new RequestSender();
    private static final RequestBuilder requestBuilder = new RequestBuilder();

    public static SignatureClient createSignatureClient(String url) {
        return new SignatureClient(url ,requestSender, requestBuilder);
    }

    public static TransactionClient createTransactionClient(String url) {
        return new TransactionClient(url ,requestSender, requestBuilder);
    }
}
