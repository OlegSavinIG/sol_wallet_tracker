package test.sol.utils;

import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;

public class ClientFactory {

    private static final String RPC_URL = "https://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";

    private static final RequestSender requestSender = new RequestSender();
    private static final RequestBuilder requestBuilder = new RequestBuilder();

    public static SignatureClient createSignatureClient() {
        return new SignatureClient(RPC_URL ,requestSender, requestBuilder);
    }

    public static TransactionClient createTransactionClient() {
        return new TransactionClient(RPC_URL ,requestSender, requestBuilder);
    }
}
