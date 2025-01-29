package test.sol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.pojo.transaction.TransactionResult;
import test.sol.redis.ValidatedWalletsRedis;
import test.sol.service.accountinformation.AccountInformationService;
import test.sol.service.accountinformation.AccountInformationServiceImpl;
import test.sol.service.signature.SignatureService;
import test.sol.service.signature.SignatureServiceImplWithSignaturesCash;
import test.sol.service.transaction.TransactionService;
import test.sol.service.transaction.TransactionServiceImpl;
import test.sol.service.wallet.WalletService;
import test.sol.utils.ClientFactory;
import test.sol.utils.ConfigLoader;

import java.util.List;
import java.util.Set;

public class SolanaNewWalletScanner {
    private static final String RPC_URL = ConfigLoader.getString("RPC_URL");
    private static final SignatureClient signatureClient = ClientFactory.createSignatureClient(RPC_URL);
    private static final TransactionClient transactionClient = ClientFactory.createTransactionClient(RPC_URL);
    private static final SignatureService signatureServiceWithCash = new SignatureServiceImplWithSignaturesCash();
    private static final TransactionService transactionService = new TransactionServiceImpl();
    private static final AccountInformationService accountInformationService = new AccountInformationServiceImpl();
    private static final WalletService walletService = new WalletService();
    private static final Logger logger = LoggerFactory.getLogger(SolanaNewWalletScanner.class);

    public static void main(String[] args) {
        try {
            long startTime = System.nanoTime();
            logger.info("Получение подписей для SolanaScanner");
            SignaturesResponse signaturesResponse = signatureClient.getSignaturesForSystemProgram();
            Set<String> signatures = signatureServiceWithCash.validateSignature(signaturesResponse);
            logger.info("Validated sigantures - {}", signatures.size());

            List<TransactionResponse> transactions = transactionClient.getTransactions(signatures);
            List<TransactionResult> transactionsWithTransfer = transactionService.getTransactionsWithTransfer(transactions);
            logger.info("Transactions after validation with transfer - {}", transactionsWithTransfer.size());

            Set<String> wallets = transactionService.extractWalletsFromTransactions(transactionsWithTransfer);
            logger.info("Extracted wallets - {}", wallets.size());

            List<String> walletsPositiveBalance = accountInformationService.isPositiveBalance(wallets);
            logger.info("Wallets with positive balance - {}", walletsPositiveBalance.size());

            List<String> validatedWallets = walletService.validateWallets(walletsPositiveBalance);
            logger.info("Validated wallets {}", validatedWallets.size());
            ValidatedWalletsRedis.saveValidatedWalletsWithTTL(validatedWallets);

            long endTime = System.nanoTime();
            System.out.println("SolanaNewWalletScanner working time " + (endTime - startTime) / 1_000_000 + " ms");
        } catch (Exception e) {
            logger.error("Произошла ошибка при выполнении программы.", e);
        }
    }
}


