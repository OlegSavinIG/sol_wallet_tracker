package test.sol.defiwebsocket.queueprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.sol.defiwebsocket.DefiNotificationHandler;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NotActivatedWalletsQueue {
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private static final Logger logger = LoggerFactory.getLogger(NotActivatedWalletsQueue.class);
    public static void addWallet(String walletAddress) {
        try {
            queue.put(walletAddress); // Блокируется, если очередь заполнена
            logger.info("Wallet added to queue {}", walletAddress);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Failed to add wallet to queue: {}", e.getMessage());
        }
    }

    public static String pollWallet() {
        try {
            return queue.poll(10, TimeUnit.SECONDS); // Ждет появления данных
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Failed to poll wallet: {}", e.getMessage());
            return null;
        }
    }


    public static boolean hasWallets() {
        return !queue.isEmpty();
    }

    public static void addWallets(List<String> wallets) {
        System.out.println("wallets added to NotActiveQueue " + wallets.size());
        queue.addAll(wallets);
    }
}
