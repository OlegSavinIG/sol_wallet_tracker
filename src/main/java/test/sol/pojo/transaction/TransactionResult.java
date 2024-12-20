package test.sol.pojo.transaction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionResult(
         long blockTime,
         TransactionMeta meta,
         TransactionData transaction
) {
}

