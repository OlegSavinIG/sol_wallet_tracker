package test.sol.utils;

import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;

public class ClientFactory {

//    private static final String RPC_URL = "https://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";
//public static final String WSS_PROVIDER_URL = "wss://attentive-dimensional-needle.solana-mainnet.quiknode.pro/dc0abb602a7a6e28b6c7e69eb336b565e8709d2a";

    private static final RequestSender requestSender = new RequestSender();
    private static final RequestBuilder requestBuilder = new RequestBuilder();

    public static SignatureClient createSignatureClient(String url) {
        return new SignatureClient(url ,requestSender, requestBuilder);
    }

    public static TransactionClient createTransactionClient(String url) {
        return new TransactionClient(url ,requestSender, requestBuilder);
    }
}
