package test.sol.wallettracker.queuelistener;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoveWalletQueue {
    private static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    public static void addWallet(String walletAddress) {
        queue.add(walletAddress);
    }

    public static String pollWallet() {
        return queue.poll();
    }

    public static boolean hasWallets() {
        return !queue.isEmpty();
    }
}

