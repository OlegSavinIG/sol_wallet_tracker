package test.sol.client.account;

import com.fasterxml.jackson.core.type.TypeReference;
import test.sol.pojo.accountinformation.AccountInformationResponse;
import test.sol.utils.RequestSender;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AccountClient {
    private static final String RPC_URL = "https://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2";
    private final RequestSender requestSender = new RequestSender();

    public AccountInformationResponse getWalletsInformation(List<String> wallets) throws IOException {

        String walletArray = wallets.stream()
                .map(wallet -> "\"" + wallet + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        String requestBody = "{"
                + "\"jsonrpc\":\"2.0\","
                + "\"id\":1,"
                + "\"method\":\"getMultipleAccounts\","
                + "\"params\": ["
                + walletArray + ","
                + "{\"encoding\":\"jsonParsed\"}"
                + "]"
                + "}";

        return requestSender.processRequestWithRetry(
                requestBody, RPC_URL,
                new TypeReference<AccountInformationResponse>() {
                });


    }
}
