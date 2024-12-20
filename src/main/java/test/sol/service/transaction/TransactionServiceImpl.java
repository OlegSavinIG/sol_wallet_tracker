package test.sol.service.transaction;

import test.sol.pojo.transaction.TransactionResponse;
import test.sol.pojo.transaction.TransactionResult;
import test.sol.redis.ProcessedWalletsRedis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TransactionServiceImpl implements TransactionService {
    @Override
    public List<TransactionResult> getTransactionsWithTransfer(List<TransactionResponse> transactions) {
        return transactions.stream()
                .map(TransactionResponse::result)
                .filter(result -> result != null)
                .filter(result -> result.transaction() != null)
                .filter(result -> result.transaction()
                        .message()
                        .instructions().stream()
                        .filter(instruction -> instruction.parsed() != null)
                        .anyMatch(instruction -> "transfer".equals(instruction.parsed().type()))
                )
                .collect(Collectors.toList());
    }


    @Override
    public Set<String> extractWalletsFromTransactions(List<TransactionResult> transactions) {
        Set<String> wallets = new HashSet<>();
        for (TransactionResult transaction : transactions) {
            if (transaction.transaction() != null) {
                transaction.transaction()
                        .message()
                        .instructions().stream()
                        .filter(instruction -> instruction.parsed() != null)
                        .filter(instruction -> "transfer".equals(instruction.parsed().type()))
                        .map(instruction -> instruction.parsed().info().destination())
                        .forEach(wallet -> {
                            if (wallet != null && !ProcessedWalletsRedis.isWalletProcessed(wallet)) {
                                wallets.add(wallet);
                                ProcessedWalletsRedis.saveProcessedWallets(wallet);
                            }
                        });
            }
        }
        return wallets;
    }
}
