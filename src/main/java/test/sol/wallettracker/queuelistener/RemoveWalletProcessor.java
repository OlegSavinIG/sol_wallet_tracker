package test.sol.wallettracker.queuelistener;

import test.sol.wallettracker.AccountSubscriptionService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoveWalletProcessor {
    private final AccountSubscriptionService subscriptionService;
    private final ExecutorService executorService;

    public RemoveWalletProcessor(AccountSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startProcessing() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (RemoveWalletQueue.hasWallets()) {
                    String wallet = RemoveWalletQueue.pollWallet();
                    if (wallet != null) {
                        subscriptionService.unsubscribeFromAddress(wallet);
                    }
                } else {
                    try {
                        Thread.sleep(500); // Ждем новые запросы на удаление
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public void stopProcessing() {
        executorService.shutdownNow();
    }
}

