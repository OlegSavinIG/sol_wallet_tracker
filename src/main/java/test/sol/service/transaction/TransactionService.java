package test.sol.service.transaction;

import test.sol.pojo.signature.SignaturesResponse;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.pojo.transaction.TransactionResult;

import java.util.List;
import java.util.Set;

public interface TransactionService {
    List<TransactionResult> getTransactionsWithTransfer(List<TransactionResponse> transactions);
    Set<String> extractWalletsFromTransactions(List<TransactionResult> transactions);
}
