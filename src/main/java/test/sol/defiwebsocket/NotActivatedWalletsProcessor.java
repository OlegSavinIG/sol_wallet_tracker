package test.sol.defiwebsocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotActivatedWalletsProcessor {
    private final WalletsSubscriptionService subscriptionService;
    private final ExecutorService executorService;

    public NotActivatedWalletsProcessor(WalletsSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startProcessing() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (NotActivatedWalletsQueue.hasWallets()) {
                    String wallet = NotActivatedWalletsQueue.pollWallet();
                    System.out.println("PROCESSOR: Found new wallet " + wallet);
                    if (wallet != null) {
                        System.out.println("Wallet processor -> send to service ");
                        subscriptionService.subscribeToWallet(wallet);
                    }
                } else {
                    try {
                        Thread.sleep(100); // Ждем новые кошельки
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
