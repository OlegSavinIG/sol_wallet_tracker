package test.sol.wallettracker.queuelistener;

import test.sol.wallettracker.AccountSubscriptionService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddWalletProcessor {
    private final AccountSubscriptionService subscriptionService;
    private final ExecutorService executorService;

    public AddWalletProcessor(AccountSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startProcessing() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (AddWalletQueue.hasWallets()) {
                    String wallet = AddWalletQueue.pollWallet();
                    System.out.println("PROCESSOR: Found new wallet " + wallet);
                    if (wallet != null) {
                        System.out.println("Wallet processor -> send to service ");
                        subscriptionService.subscribeToWallet(wallet);
                    }
                } else {
                    try {
                        Thread.sleep(500); // Ждем новые кошельки
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

