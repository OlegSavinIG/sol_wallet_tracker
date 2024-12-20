package test.sol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.client.signature.SignatureClient;
import test.sol.client.transaction.TransactionClient;
import test.sol.pojo.signature.SignaturesResponse;
import test.sol.pojo.transaction.TransactionResponse;
import test.sol.pojo.transaction.TransactionResult;
import test.sol.redis.ProcessedWalletsRedis;
import test.sol.redis.ValidatedWalletsRedis;
import test.sol.service.accountinformation.AccountInformationService;
import test.sol.service.accountinformation.AccountInformationServiceImpl;
import test.sol.service.signature.SignatureService;
import test.sol.service.signature.SignatureServiceImpl;
import test.sol.service.transaction.TransactionService;
import test.sol.service.transaction.TransactionServiceImpl;
import test.sol.service.wallet.WalletService;
import test.sol.utils.ClientFactory;

import java.util.List;
import java.util.Set;

public class SolanaAccountCreationScanner {
    //    https://mainnet.helius-rpc.com/?api-key=d528e83e-fd04-44de-b13b-2a1839229b5b
//    https://cool-long-sky.solana-mainnet.quiknode.pro/11f11504b987da4fa32dbb3ab4c8bfe913db4ee2

    private static final SignatureClient signatureClient = ClientFactory.createSignatureClient();
    private static final TransactionClient transactionClient = ClientFactory.createTransactionClient();
    private static final SignatureService signatureService = new SignatureServiceImpl();
    private static final TransactionService transactionService = new TransactionServiceImpl();
    private static final AccountInformationService accountInformationService = new AccountInformationServiceImpl();
    private static final WalletService walletService = new WalletService();
    private static final Logger logger = LoggerFactory.getLogger(SolanaAccountCreationScanner.class);

    public static void main(String[] args) {
        try {
            long startTime = System.nanoTime();
            logger.info("Получение подписей для SolanaScanner");
            SignaturesResponse signaturesResponse = signatureClient.getSignaturesForSystemProgram();
            Set<String> signatures = signatureService.validateSignature(signaturesResponse);
            logger.info("Validated sigantures - {}", signatures.size());

            List<TransactionResponse> transactions = transactionClient.getTransactions(signatures);
            logger.info("Transactions before validation - {}", transactions.size());

            List<TransactionResult> transactionsWithTransfer = transactionService.getTransactionsWithTransfer(transactions);
            logger.info("Transactions after validations - {}", transactionsWithTransfer.size());

            Set<String> wallets = transactionService.extractWalletsFromTransactions(transactionsWithTransfer);
            logger.info("Extracted wallets - {}", wallets.size());

            List<String> walletsPositiveBalance = accountInformationService.isPositiveBalance(wallets);
            logger.info("Wallets with positive balance - {}", walletsPositiveBalance.size());

            List<String> validatedWallets = walletService.validateWallets(walletsPositiveBalance);
            logger.info("Validated wallets {}", validatedWallets.size());
            ValidatedWalletsRedis.saveValidatedWalletsWithTTL(validatedWallets);

            validatedWallets.forEach(System.out::println);
            long endTime = System.nanoTime();
            System.out.println("Проверка заняла " + (endTime - startTime) / 1_000_000 + " ms");
        } catch (Exception e) {
            logger.error("Произошла ошибка при выполнении программы.", e);
        }
    }
}


