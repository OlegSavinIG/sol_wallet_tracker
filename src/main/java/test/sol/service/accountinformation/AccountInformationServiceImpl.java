package test.sol.service.accountinformation;

import test.sol.client.account.AccountClient;
import test.sol.pojo.accountinformation.AccountInformationResponse;
import test.sol.pojo.accountinformation.AccountInformationValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AccountInformationServiceImpl implements AccountInformationService {
    private final AccountClient accountClient = new AccountClient();
    private static final Integer MIN_LAMPORTS = 500000000;

    @Override
    public List<String> isPositiveBalance(Set<String> wallets) throws IOException {
        List<String> positiveBalance = new ArrayList<>();
        List<String> walletsList = new ArrayList<>(wallets);

        int batchSize = 20;
        for (int i = 0; i < wallets.size(); i += batchSize) {
            List<String> batch = walletsList.subList(i, Math.min(wallets.size(), i + batchSize));
            AccountInformationResponse walletsInformation = accountClient.getWalletsInformation(batch);
            for (int j = 0; j < batch.size(); j++) {
                List<AccountInformationValue> value = walletsInformation.result().value();
                AccountInformationValue accountInformationValue = value.get(j);
                if (accountInformationValue != null){
                    long lamports = accountInformationValue.lamports();
                if (lamports > MIN_LAMPORTS) {
                    positiveBalance.add(batch.get(j));
                }
                }
            }
        }
        return positiveBalance;
    }
}
