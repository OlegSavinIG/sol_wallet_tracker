package test.sol.defiwebsocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UnsubscribeWalletProcessor {
        private final WalletsSubscriptionService subscriptionService;
        private final ExecutorService executorService;

    public UnsubscribeWalletProcessor(WalletsSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

        public void startProcessing() {
        executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (UnsubscribeWalletsQueue.hasWallets()) {
                    String wallet = UnsubscribeWalletsQueue.pollWallet();
                    System.out.println("PROCESSOR: Unsubscribed from wallet " + wallet);
                    if (wallet != null) {
                        System.out.println("Unsubscribe processor -> send to service ");
                        subscriptionService.unsubscribeFromWallet(wallet);
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
