package test.sol.defiwebsocket.queueprocessor;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UnsubscribeWalletsQueue {
    private static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    public static void addWallet(String walletAddress) {
        queue.add(walletAddress);
        System.out.println("Wallet added to queue " + walletAddress);
    }

    public static String pollWallet() {
        System.out.println("Wallet polled from queue ");
        return queue.poll();
    }

    public static boolean hasWallets() {
        return !queue.isEmpty();
    }

    public static void addWallets(List<String> wallets) {
        queue.addAll(wallets);
    }
}
