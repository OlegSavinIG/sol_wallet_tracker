package test.sol.service.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import test.sol.pojo.transaction.TransactionData;
import test.sol.pojo.transaction.TransactionInfo;
import test.sol.pojo.transaction.TransactionInstruction;
import test.sol.pojo.transaction.TransactionMessage;
import test.sol.pojo.transaction.TransactionParsed;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.pojo.transaction.TransactionResult;
import test.sol.redis.ProcessedWalletsRedis;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class TransactionServiceImplTest {

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl();
    }

    private TransactionResult createMockTransactionResult(String type, String account) {
        TransactionResult transactionResult = Mockito.mock(TransactionResult.class);
        TransactionData transactionData = Mockito.mock(TransactionData.class);
        TransactionMessage message = Mockito.mock(TransactionMessage.class);
        TransactionInstruction instructions = Mockito.mock(TransactionInstruction.class);
        TransactionParsed parsed = Mockito.mock(TransactionParsed.class);

        Mockito.when(transactionResult.transaction()).thenReturn(transactionData);
        Mockito.when(transactionData.message()).thenReturn(message);
        Mockito.when(message.instructions()).thenReturn(instructions);
        Mockito.when(instructions.parsed()).thenReturn(List.of(parsed));
        Mockito.when(parsed.type()).thenReturn(type);
        Mockito.when(parsed.info()).thenReturn(new TransactionInfo(account));

        return transactionResult;
    }

    @Test
    void testGetTransactionsWithTransfer() {
        // Arrange
        TransactionResult result1 = createMockTransactionResult("transfer", "account1");
        TransactionResult result2 = createMockTransactionResult("non-transfer", "account2");

        TransactionResponse response1 = new TransactionResponse(result1);
        TransactionResponse response2 = new TransactionResponse(result2);
        List<TransactionResponse> transactions = List.of(response1, response2);

        // Act
        List<TransactionResult> filteredResults = transactionService.getTransactionsWithTransfer(transactions);

        // Assert
        assertNotNull(filteredResults);
        assertEquals(1, filteredResults.size());
        assertTrue(filteredResults.contains(result1));
        assertFalse(filteredResults.contains(result2));
    }
}
